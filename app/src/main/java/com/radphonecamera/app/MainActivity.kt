package com.radphonecamera.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.radphonecamera.app.baseline.BaselineProgress
import com.radphonecamera.app.baseline.BaselineQualityScorer
import com.radphonecamera.app.baseline.BaselineResult
import com.radphonecamera.app.camera.CameraRepository
import com.radphonecamera.app.camera.DeviceCameraReport
import com.radphonecamera.app.camera.FrameProbe
import com.radphonecamera.app.camera.FrameProbeListener
import com.radphonecamera.app.camera.FrameProbeResult
import com.radphonecamera.app.camera.FrameProbeSession
import com.radphonecamera.app.ui.RadPhoneCameraApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var cameraPermissionGranted by remember { mutableStateOf(hasCameraPermission()) }
            var report by remember { mutableStateOf<DeviceCameraReport?>(null) }
            var loadingReport by remember { mutableStateOf(false) }
            var reportError by remember { mutableStateOf<String?>(null) }
            var runningProbeCameraId by remember { mutableStateOf<String?>(null) }
            var probeResult by remember { mutableStateOf<FrameProbeResult?>(null) }
            var runningBaselineCameraId by remember { mutableStateOf<String?>(null) }
            var baselineProgress by remember { mutableStateOf(BaselineProgress()) }
            var baselineResult by remember { mutableStateOf<BaselineResult?>(null) }
            var activeProbeSession by remember { mutableStateOf<FrameProbeSession?>(null) }
            val scope = rememberCoroutineScope()
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { granted ->
                cameraPermissionGranted = granted
            }

            fun refreshReport() {
                if (!cameraPermissionGranted) return
                scope.launch {
                    loadingReport = true
                    reportError = null
                    report = withContext(Dispatchers.IO) {
                        runCatching { CameraRepository(this@MainActivity).discoverCameras() }
                            .onFailure { reportError = it.message ?: "Camera discovery failed." }
                            .getOrNull()
                    }
                    loadingReport = false
                }
            }

            fun runBaseline(cameraId: String) {
                var progress = BaselineProgress()
                activeProbeSession?.stop()
                runningBaselineCameraId = cameraId
                runningProbeCameraId = null
                baselineProgress = progress
                baselineResult = null

                activeProbeSession = FrameProbe(this@MainActivity).runSingleCameraProbe(
                    cameraId = cameraId,
                    durationMillis = 60_000L,
                    listener = object : FrameProbeListener {
                        override fun onProgress(result: FrameProbeResult) {
                            progress = progress.record(result.latestDarkState?.quality)
                            runOnUiThread {
                                baselineProgress = progress
                                probeResult = result
                            }
                        }

                        override fun onCompleted(result: FrameProbeResult) {
                            val finalResult = BaselineQualityScorer.score(
                                progress = progress,
                                error = result.error,
                            )
                            runOnUiThread {
                                baselineProgress = progress
                                baselineResult = finalResult
                                probeResult = result
                                runningBaselineCameraId = null
                                activeProbeSession = null
                            }
                        }
                    },
                )
            }

            fun stopActiveCapture() {
                activeProbeSession?.stop()
                activeProbeSession = null
                runningProbeCameraId = null
                runningBaselineCameraId = null
            }

            LaunchedEffect(cameraPermissionGranted) {
                refreshReport()
            }

            RadPhoneCameraApp(
                cameraPermissionGranted = cameraPermissionGranted,
                report = report,
                loadingReport = loadingReport,
                reportError = reportError,
                runningProbeCameraId = runningProbeCameraId,
                runningBaselineCameraId = runningBaselineCameraId,
                probeResult = probeResult,
                baselineProgress = baselineProgress,
                baselineResult = baselineResult,
                onRequestCameraPermission = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onRefresh = ::refreshReport,
                onRunBaseline = ::runBaseline,
                onStopCapture = ::stopActiveCapture,
                onRunProbe = { cameraId ->
                    activeProbeSession?.stop()
                    runningProbeCameraId = cameraId
                    runningBaselineCameraId = null
                    probeResult = null
                    activeProbeSession = FrameProbe(this@MainActivity).runSingleCameraProbe(
                        cameraId = cameraId,
                        listener = object : FrameProbeListener {
                            override fun onProgress(result: FrameProbeResult) {
                                runOnUiThread { probeResult = result }
                            }

                            override fun onCompleted(result: FrameProbeResult) {
                                runOnUiThread {
                                    probeResult = result
                                    runningProbeCameraId = null
                                    activeProbeSession = null
                                }
                            }
                        },
                    )
                },
            )
        }
    }

    private fun hasCameraPermission(): Boolean =
        checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
}
