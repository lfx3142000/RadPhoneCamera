package com.radphonecamera.app.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CameraManager
import android.os.Build
import com.radphonecamera.app.detector.DeviceQualification

class CameraRepository(
    context: Context,
) {
    private val cameraManager = context.getSystemService(CameraManager::class.java)

    fun discoverCameras(): DeviceCameraReport {
        val cameras = cameraManager.cameraIdList.map { cameraId ->
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val capability = characteristics.toCapability(cameraId)
            capability.copy(detectorScore = DeviceQualification.score(capability))
        }
        return DeviceCameraReport(cameras)
    }

    private fun CameraCharacteristics.toCapability(cameraId: String): CameraCapability {
        val capabilities = get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
            ?.toSet()
            .orEmpty()
        val streamMap = get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val outputFormats = streamMap?.outputFormats?.toSet().orEmpty()
        val yuvSizes = streamMap
            ?.getOutputSizes(ImageFormat.YUV_420_888)
            ?.map { FrameSize(it.width, it.height) }
            ?.sortedBy { it.pixelCount }
            .orEmpty()
        val focusModes = get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
            ?.toSet()
            .orEmpty()

        val baseCapability = CameraCapability(
            cameraId = cameraId,
            lensFacing = lensFacingLabel(get(CameraCharacteristics.LENS_FACING)),
            hardwareLevel = hardwareLevelLabel(get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)),
            supportsManualExposure = get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE) != null &&
                capabilities.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR),
            supportsManualSensitivity = get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE) != null &&
                capabilities.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR),
            supportsFocusLock = focusModes.any {
                it == CameraMetadata.CONTROL_AF_MODE_OFF ||
                    it == CameraMetadata.CONTROL_AF_MODE_AUTO ||
                    it == CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            },
            supportsYuv = outputFormats.contains(ImageFormat.YUV_420_888),
            supportsRaw = outputFormats.contains(ImageFormat.RAW_SENSOR),
            physicalCameraIds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                physicalCameraIds.toList().sorted()
            } else {
                emptyList()
            },
            suggestedYuvSize = yuvSizes.firstOrNull(),
            availableYuvSizes = yuvSizes,
            detectorScore = CameraDetectorScore(
                supportLevel = DetectorSupportLevel.NotSupported,
                score = 0,
                reasons = emptyList(),
            ),
        )

        return baseCapability
    }

    private fun lensFacingLabel(value: Int?): String = when (value) {
        CameraCharacteristics.LENS_FACING_BACK -> "Back"
        CameraCharacteristics.LENS_FACING_FRONT -> "Front"
        CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
        else -> "Unknown"
    }

    private fun hardwareLevelLabel(value: Int?): String = when (value) {
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "Legacy"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "Limited"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "Full"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "Level 3"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "External"
        else -> "Unknown"
    }
}

