package com.radphonecamera.app.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Range
import com.radphonecamera.app.detector.DarkState
import com.radphonecamera.app.detector.DarkStateClassifier
import com.radphonecamera.app.detector.FrameStats
import com.radphonecamera.app.detector.FrameStatsCalculator
import java.util.concurrent.atomic.AtomicBoolean

data class ManualControlAttempt(
    val exposureLocked: Boolean,
    val sensitivityLocked: Boolean,
    val focusLocked: Boolean,
)

data class LumaFrameSnapshot(
    val width: Int,
    val height: Int,
    val luma: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LumaFrameSnapshot) return false
        return width == other.width &&
            height == other.height &&
            luma.contentEquals(other.luma)
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + luma.contentHashCode()
        return result
    }
}

data class FrameProbeResult(
    val cameraId: String,
    val framesAnalyzed: Int,
    val manualControlAttempt: ManualControlAttempt,
    val latestStats: FrameStats?,
    val latestDarkState: DarkState?,
    val latestSnapshot: LumaFrameSnapshot?,
    val error: String?,
)

interface FrameProbeListener {
    fun onProgress(result: FrameProbeResult)
    fun onCompleted(result: FrameProbeResult)
}

class FrameProbeSession internal constructor(
    private val stopAction: () -> Unit,
) {
    fun stop() {
        stopAction()
    }
}

class FrameProbe(
    private val context: Context,
) {
    private val cameraManager = context.getSystemService(CameraManager::class.java)

    fun runSingleCameraProbe(
        cameraId: String,
        durationMillis: Long = 3_000L,
        listener: FrameProbeListener,
    ): FrameProbeSession {
        if (context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            listener.onCompleted(emptyResult(cameraId, "Camera permission has not been granted."))
            return FrameProbeSession {}
        }

        val thread = HandlerThread("FrameProbe-$cameraId").apply { start() }
        val handler = Handler(thread.looper)
        val finished = AtomicBoolean(false)
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)

        var cameraDevice: CameraDevice? = null
        var session: CameraCaptureSession? = null
        var imageReader: ImageReader? = null
        var latestStats: FrameStats? = null
        var latestDarkState: DarkState? = null
        var latestSnapshot: LumaFrameSnapshot? = null
        var frameCount = 0
        var manualAttempt = ManualControlAttempt(
            exposureLocked = false,
            sensitivityLocked = false,
            focusLocked = false,
        )

        fun finish(error: String? = null) {
            if (!finished.compareAndSet(false, true)) return
            runCatching { session?.stopRepeating() }
            runCatching { session?.abortCaptures() }
            runCatching { session?.close() }
            runCatching { imageReader?.close() }
            runCatching { cameraDevice?.close() }
            thread.quitSafely()
            listener.onCompleted(
                FrameProbeResult(
                    cameraId = cameraId,
                    framesAnalyzed = frameCount,
                    manualControlAttempt = manualAttempt,
                    latestStats = latestStats,
                    latestDarkState = latestDarkState,
                    latestSnapshot = latestSnapshot,
                    error = error,
                ),
            )
        }

        try {
            cameraManager.openCamera(
                cameraId,
                object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        cameraDevice = camera
                        val size = chooseYuvSize(characteristics)
                        if (size == null) {
                            finish("Camera $cameraId does not expose YUV_420_888 output.")
                            return
                        }

                        imageReader = ImageReader.newInstance(
                            size.width,
                            size.height,
                            ImageFormat.YUV_420_888,
                            3,
                        )
                        imageReader?.setOnImageAvailableListener(
                            { reader ->
                                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                                try {
                                    val stats = FrameStatsCalculator.fromImage(image)
                                    val darkState = DarkStateClassifier.classify(stats)
                                    val snapshot = image.copyLumaSnapshot()
                                    latestStats = stats
                                    latestDarkState = darkState
                                    latestSnapshot = snapshot
                                    frameCount += 1
                                    listener.onProgress(
                                        FrameProbeResult(
                                            cameraId = cameraId,
                                            framesAnalyzed = frameCount,
                                            manualControlAttempt = manualAttempt,
                                            latestStats = stats,
                                            latestDarkState = darkState,
                                            latestSnapshot = snapshot,
                                            error = null,
                                        ),
                                    )
                                } finally {
                                    image.close()
                                }
                            },
                            handler,
                        )

                        val surface = imageReader?.surface
                        if (surface == null) {
                            finish("Could not create YUV analysis surface.")
                            return
                        }

                        val request = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                        request.addTarget(surface)
                        manualAttempt = applyManualControls(request, characteristics)

                        @Suppress("DEPRECATION")
                        camera.createCaptureSession(
                            listOf(surface),
                            object : CameraCaptureSession.StateCallback() {
                                override fun onConfigured(configuredSession: CameraCaptureSession) {
                                    session = configuredSession
                                    configuredSession.setRepeatingRequest(
                                        request.build(),
                                        null,
                                        handler,
                                    )
                                    handler.postDelayed({ finish() }, durationMillis)
                                }

                                override fun onConfigureFailed(configuredSession: CameraCaptureSession) {
                                    finish("Camera capture session configuration failed.")
                                }
                            },
                            handler,
                        )
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        finish("Camera disconnected.")
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        finish("Camera error code $error.")
                    }
                },
                handler,
            )
        } catch (securityException: SecurityException) {
            finish("Camera permission check failed: ${securityException.message}")
        } catch (exception: RuntimeException) {
            finish("Camera probe failed: ${exception.message}")
        }

        return FrameProbeSession {
            handler.post { finish("Stopped by user.") }
        }
    }

    private fun chooseYuvSize(characteristics: CameraCharacteristics): FrameSize? {
        val streamMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        return streamMap
            ?.getOutputSizes(ImageFormat.YUV_420_888)
            ?.map { FrameSize(it.width, it.height) }
            ?.filter { it.width >= 320 && it.height >= 240 }
            ?.minByOrNull { it.pixelCount }
            ?: streamMap
                ?.getOutputSizes(ImageFormat.YUV_420_888)
                ?.map { FrameSize(it.width, it.height) }
                ?.minByOrNull { it.pixelCount }
    }

    private fun applyManualControls(
        request: CaptureRequest.Builder,
        characteristics: CameraCharacteristics,
    ): ManualControlAttempt {
        val capabilities = characteristics
            .get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
            ?.toSet()
            .orEmpty()
        val hasManualSensor = capabilities.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR)
        val exposureRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        val sensitivityRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val focusModes = characteristics
            .get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
            ?.toSet()
            .orEmpty()

        var exposureLocked = false
        var sensitivityLocked = false

        if (hasManualSensor && exposureRange != null && sensitivityRange != null) {
            request.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            request.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureRange.clamp(20_000_000L))
            request.set(CaptureRequest.SENSOR_SENSITIVITY, sensitivityRange.clamp(400))
            exposureLocked = true
            sensitivityLocked = true
        }

        val focusLocked = if (focusModes.contains(CameraMetadata.CONTROL_AF_MODE_OFF)) {
            request.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF)
            true
        } else if (focusModes.contains(CameraMetadata.CONTROL_AF_MODE_AUTO)) {
            request.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO)
            true
        } else {
            false
        }

        request.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        request.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF)

        return ManualControlAttempt(
            exposureLocked = exposureLocked,
            sensitivityLocked = sensitivityLocked,
            focusLocked = focusLocked,
        )
    }

    private fun Range<Long>.clamp(value: Long): Long =
        value.coerceAtLeast(lower).coerceAtMost(upper)

    private fun Range<Int>.clamp(value: Int): Int =
        value.coerceAtLeast(lower).coerceAtMost(upper)

    private fun emptyResult(cameraId: String, error: String): FrameProbeResult =
        FrameProbeResult(
            cameraId = cameraId,
            framesAnalyzed = 0,
            manualControlAttempt = ManualControlAttempt(
                exposureLocked = false,
                sensitivityLocked = false,
                focusLocked = false,
            ),
            latestStats = null,
            latestDarkState = null,
            latestSnapshot = null,
            error = error,
        )

    private fun android.media.Image.copyLumaSnapshot(): LumaFrameSnapshot {
        val plane = planes.first()
        val buffer = plane.buffer.duplicate()
        val bytes = ByteArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val sourceIndex = y * plane.rowStride + x * plane.pixelStride
                if (sourceIndex < buffer.limit()) {
                    bytes[y * width + x] = buffer.get(sourceIndex)
                }
            }
        }
        return LumaFrameSnapshot(
            width = width,
            height = height,
            luma = bytes,
        )
    }
}
