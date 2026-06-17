package com.radphonecamera.app

import android.Manifest
import android.content.Intent
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
import com.radphonecamera.app.baseline.BaselineStore
import com.radphonecamera.app.camera.CameraRepository
import com.radphonecamera.app.camera.DeviceCameraReport
import com.radphonecamera.app.camera.FrameProbe
import com.radphonecamera.app.camera.FrameProbeListener
import com.radphonecamera.app.camera.FrameProbeResult
import com.radphonecamera.app.camera.FrameProbeSession
import com.radphonecamera.app.camera.LumaFrameSnapshot
import com.radphonecamera.app.data.ScanEvent
import com.radphonecamera.app.data.ScanEventLogCodec
import com.radphonecamera.app.data.ScanEventLogStore
import com.radphonecamera.app.data.toScanEvent
import com.radphonecamera.app.detector.BaselineEventStats
import com.radphonecamera.app.detector.DarkQuality
import com.radphonecamera.app.detector.HotPixelMap
import com.radphonecamera.app.detector.LiveScanAccumulator
import com.radphonecamera.app.detector.LiveScanFrameInput
import com.radphonecamera.app.detector.LiveScanProgress
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
            var runningScanCameraId by remember { mutableStateOf<String?>(null) }
            var baselineProgress by remember { mutableStateOf(BaselineProgress()) }
            val baselineStore = remember { BaselineStore(this@MainActivity) }
            val scanEventLogStore = remember { ScanEventLogStore(this@MainActivity) }
            val savedBaselineResult = remember { baselineStore.load() }
            val savedHotPixelMap = remember {
                savedBaselineResult?.cameraId?.let { cameraId ->
                    baselineStore.loadHotPixelMap(cameraId)
                }
            }
            var baselineResult by remember { mutableStateOf(savedBaselineResult) }
            var liveScanProgress by remember { mutableStateOf<LiveScanProgress?>(null) }
            var scanEvents by remember { mutableStateOf<List<ScanEvent>>(scanEventLogStore.load()) }
            var activeHotPixelMap by remember { mutableStateOf(savedHotPixelMap) }
            var activeHotPixelCameraId by remember {
                mutableStateOf(savedHotPixelMap?.let { savedBaselineResult?.cameraId })
            }
            var activeProbeSession by remember { mutableStateOf<FrameProbeSession?>(null) }
            var activeCaptureId by remember { mutableStateOf(0) }
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
                val baselineSnapshots = mutableListOf<LumaFrameSnapshot>()
                var lastRecordedBaselineFrameCount = 0
                val previousSession = activeProbeSession
                val captureId = activeCaptureId + 1
                activeCaptureId = captureId
                previousSession?.stop()
                runningBaselineCameraId = cameraId
                runningProbeCameraId = null
                runningScanCameraId = null
                baselineProgress = progress
                baselineResult = null
                probeResult = null
                liveScanProgress = null

                activeProbeSession = FrameProbe(this@MainActivity).runSingleCameraProbe(
                    cameraId = cameraId,
                    durationMillis = 60_000L,
                    listener = object : FrameProbeListener {
                        override fun onProgress(result: FrameProbeResult) {
                            if (result.framesAnalyzed > lastRecordedBaselineFrameCount) {
                                progress = progress.record(result.latestDarkState?.quality)
                                lastRecordedBaselineFrameCount = result.framesAnalyzed
                                if (
                                    result.latestDarkState?.quality == DarkQuality.Good ||
                                    result.latestDarkState?.quality == DarkQuality.Fair
                                ) {
                                    result.latestSnapshot?.let { snapshot ->
                                        if (
                                            baselineSnapshots.size < MAX_BASELINE_SNAPSHOTS &&
                                            result.framesAnalyzed % BASELINE_SNAPSHOT_INTERVAL == 0
                                        ) {
                                            baselineSnapshots += snapshot
                                        }
                                    }
                                }
                            }
                            runOnUiThread {
                                if (captureId == activeCaptureId) {
                                    baselineProgress = progress
                                    probeResult = result
                                }
                            }
                        }

                        override fun onCompleted(result: FrameProbeResult) {
                            if (result.error == USER_STOPPED_ERROR) {
                                runOnUiThread {
                                    if (captureId == activeCaptureId) {
                                        baselineResult = baselineStore.load()
                                        probeResult = result
                                        runningBaselineCameraId = null
                                        activeProbeSession = null
                                    }
                                }
                                return
                            }

                            val hotPixelMap = baselineSnapshots.hotPixelMap()
                            val baselineEventStats = BaselineEventStats.fromSnapshots(
                                snapshots = baselineSnapshots,
                                hotPixelMap = hotPixelMap,
                            )
                            val finalResult = BaselineQualityScorer.score(
                                progress = progress,
                                error = result.error,
                            ).copy(
                                cameraId = cameraId,
                                hotPixelCount = hotPixelMap?.size ?: 0,
                                collectedAtMillis = System.currentTimeMillis(),
                                baselineEventFrameCount = baselineEventStats.frameCount,
                                baselineCandidateEvents = baselineEventStats.totalCandidateEvents,
                                baselineMeanEventsPerFrame = baselineEventStats.meanEventsPerFrame,
                                baselineVarianceEventsPerFrame = baselineEventStats.varianceEventsPerFrame,
                            )
                            runOnUiThread {
                                if (captureId == activeCaptureId) {
                                    baselineStore.save(finalResult, hotPixelMap)
                                    activeHotPixelMap = hotPixelMap
                                    activeHotPixelCameraId = cameraId
                                    baselineProgress = progress
                                    baselineResult = finalResult
                                    probeResult = result
                                    runningBaselineCameraId = null
                                    activeProbeSession = null
                                }
                            }
                        }
                    },
                )
            }

            fun runQuickScan(cameraId: String) {
                val durationMillis = QUICK_SCAN_DURATION_MILLIS
                val hotPixelMap = activeHotPixelMap.takeIf { activeHotPixelCameraId == cameraId }
                val accumulator = LiveScanAccumulator(
                    cameraId = cameraId,
                    hotPixelMap = hotPixelMap,
                    baselineModel = baselineResult?.baselineModel,
                )
                var lastRecordedFrameCount = 0
                val previousSession = activeProbeSession
                val captureId = activeCaptureId + 1
                activeCaptureId = captureId
                previousSession?.stop()
                runningScanCameraId = cameraId
                runningProbeCameraId = null
                runningBaselineCameraId = null
                probeResult = null
                liveScanProgress = accumulator.snapshot(
                    durationMillis = durationMillis,
                    elapsedMillis = 0L,
                    remainingMillis = durationMillis,
                )

                fun recordLatestFrame(result: FrameProbeResult) {
                    val snapshot = result.latestSnapshot
                    if (snapshot != null && result.framesAnalyzed > lastRecordedFrameCount) {
                        accumulator.recordFrame(
                            LiveScanFrameInput(
                                width = snapshot.width,
                                height = snapshot.height,
                                luma = snapshot.luma,
                                darkQuality = result.latestDarkState?.quality,
                            ),
                        )
                        lastRecordedFrameCount = result.framesAnalyzed
                    }
                }

                activeProbeSession = FrameProbe(this@MainActivity).runSingleCameraProbe(
                    cameraId = cameraId,
                    durationMillis = durationMillis,
                    listener = object : FrameProbeListener {
                        override fun onProgress(result: FrameProbeResult) {
                            recordLatestFrame(result)
                            val progress = accumulator.snapshot(
                                durationMillis = result.durationMillis,
                                elapsedMillis = result.elapsedMillis,
                                remainingMillis = result.remainingMillis,
                            )
                            runOnUiThread {
                                if (captureId == activeCaptureId) {
                                    liveScanProgress = progress
                                    probeResult = result
                                }
                            }
                        }

                        override fun onCompleted(result: FrameProbeResult) {
                            recordLatestFrame(result)
                            val progress = accumulator.snapshot(
                                durationMillis = result.durationMillis,
                                elapsedMillis = result.elapsedMillis,
                                remainingMillis = result.remainingMillis,
                                error = result.error,
                            )
                            runOnUiThread {
                                if (captureId == activeCaptureId) {
                                    if (result.error == null && progress.framesAnalyzed > 0) {
                                        scanEventLogStore.append(
                                            progress.toScanEvent(System.currentTimeMillis()),
                                        )
                                        scanEvents = scanEventLogStore.load()
                                    }
                                    liveScanProgress = progress
                                    probeResult = result
                                    runningScanCameraId = null
                                    activeProbeSession = null
                                }
                            }
                        }
                    },
                )
            }

            fun stopActiveCapture() {
                val wasBaselineRunning = runningBaselineCameraId != null
                val wasAnyCaptureRunning = runningProbeCameraId != null ||
                    runningBaselineCameraId != null ||
                    runningScanCameraId != null
                val wasScanRunning = runningScanCameraId != null
                activeCaptureId += 1
                activeProbeSession?.stop()
                activeProbeSession = null
                if (wasBaselineRunning) {
                    baselineResult = baselineStore.load()
                }
                if (wasScanRunning) {
                    liveScanProgress = liveScanProgress?.copy(
                        remainingMillis = 0L,
                        error = USER_STOPPED_ERROR,
                    )
                }
                if (wasAnyCaptureRunning) {
                    probeResult = probeResult?.copy(
                        remainingMillis = 0L,
                        error = USER_STOPPED_ERROR,
                    )
                }
                runningProbeCameraId = null
                runningBaselineCameraId = null
                runningScanCameraId = null
            }

            fun exportScanLog() {
                if (scanEvents.isEmpty()) return
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_SUBJECT, "RadPhoneCamera scan log")
                    putExtra(Intent.EXTRA_TEXT, ScanEventLogCodec.toCsv(scanEvents))
                }
                startActivity(Intent.createChooser(sendIntent, "Export scan log"))
            }

            fun clearScanLog() {
                scanEventLogStore.clear()
                scanEvents = emptyList()
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
                runningScanCameraId = runningScanCameraId,
                probeResult = probeResult,
                baselineProgress = baselineProgress,
                baselineResult = baselineResult,
                liveScanProgress = liveScanProgress,
                scanEvents = scanEvents,
                onExportScanLog = ::exportScanLog,
                onClearScanLog = ::clearScanLog,
                onRequestCameraPermission = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onRefresh = ::refreshReport,
                onRunBaseline = ::runBaseline,
                onRunQuickScan = ::runQuickScan,
                onStopCapture = ::stopActiveCapture,
                onRunProbe = { cameraId ->
                    val previousSession = activeProbeSession
                    val captureId = activeCaptureId + 1
                    activeCaptureId = captureId
                    previousSession?.stop()
                    runningProbeCameraId = cameraId
                    runningBaselineCameraId = null
                    runningScanCameraId = null
                    probeResult = null
                    activeProbeSession = FrameProbe(this@MainActivity).runSingleCameraProbe(
                        cameraId = cameraId,
                        listener = object : FrameProbeListener {
                            override fun onProgress(result: FrameProbeResult) {
                                runOnUiThread {
                                    if (captureId == activeCaptureId) {
                                        probeResult = result
                                    }
                                }
                            }

                            override fun onCompleted(result: FrameProbeResult) {
                                runOnUiThread {
                                    if (captureId == activeCaptureId) {
                                        probeResult = result
                                        runningProbeCameraId = null
                                        activeProbeSession = null
                                    }
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

    private fun List<LumaFrameSnapshot>.hotPixelMap(): HotPixelMap? {
        val first = firstOrNull() ?: return null
        val matchingFrames = filter { it.width == first.width && it.height == first.height }
        if (matchingFrames.isEmpty()) return null
        return HotPixelMap.fromDarkFrames(
            frames = matchingFrames.map { it.luma },
            width = first.width,
            height = first.height,
        )
    }

    private companion object {
        const val QUICK_SCAN_DURATION_MILLIS = 30_000L
        const val MAX_BASELINE_SNAPSHOTS = 120
        const val BASELINE_SNAPSHOT_INTERVAL = 5
        const val USER_STOPPED_ERROR = "Stopped by user."
    }
}
