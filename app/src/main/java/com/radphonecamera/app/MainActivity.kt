package com.radphonecamera.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.radphonecamera.app.baseline.CameraBaselineCoverageCalculator
import com.radphonecamera.app.baseline.BaselineProgress
import com.radphonecamera.app.baseline.BaselineQualityScorer
import com.radphonecamera.app.baseline.BaselineResult
import com.radphonecamera.app.baseline.BaselineStore
import com.radphonecamera.app.baseline.MultiCameraBaselineProgress
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
import com.radphonecamera.app.detector.MultiCameraScanAggregator
import com.radphonecamera.app.detector.MultiCameraScanProgress
import com.radphonecamera.app.detector.MultiCameraWeighting
import com.radphonecamera.app.patrol.PatrolBatteryMode
import com.radphonecamera.app.patrol.PatrolScheduler
import com.radphonecamera.app.sensors.BatteryThermalState
import com.radphonecamera.app.sensors.BatteryThermalStateProvider
import com.radphonecamera.app.sensors.MotionState
import com.radphonecamera.app.sensors.MotionStateProvider
import com.radphonecamera.app.ui.RadPhoneCameraApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
            val batteryThermalProvider = remember { BatteryThermalStateProvider(this@MainActivity) }
            val savedBaselineResult = remember { baselineStore.load() }
            val savedBaselineResults = remember { baselineStore.loadAll() }
            var baselineResult by remember { mutableStateOf(savedBaselineResult) }
            var baselinesByCamera by remember { mutableStateOf(savedBaselineResults) }
            var liveScanProgress by remember { mutableStateOf<LiveScanProgress?>(null) }
            var multiCameraScanProgress by remember { mutableStateOf<MultiCameraScanProgress?>(null) }
            var multiCameraBaselineProgress by remember { mutableStateOf<MultiCameraBaselineProgress?>(null) }
            var scanEvents by remember { mutableStateOf<List<ScanEvent>>(scanEventLogStore.load()) }
            var activeProbeSession by remember { mutableStateOf<FrameProbeSession?>(null) }
            var activeCaptureId by remember { mutableStateOf(0) }
            var runningMultiCameraScan by remember { mutableStateOf(false) }
            var runningMultiCameraBaseline by remember { mutableStateOf(false) }
            var motionState by remember { mutableStateOf(MotionState.Unavailable) }
            var batteryThermalState by remember { mutableStateOf(BatteryThermalState.Unknown) }
            var patrolEnabled by remember { mutableStateOf(false) }
            var patrolBatteryMode by remember { mutableStateOf(PatrolBatteryMode.Balanced) }
            var appInForeground by remember { mutableStateOf(true) }
            var runningPatrolBurst by remember { mutableStateOf(false) }
            var patrolBurstProgress by remember { mutableStateOf<LiveScanProgress?>(null) }
            var patrolLastBurstAtMillis by remember { mutableStateOf(0L) }
            var patrolNextBurstAtMillis by remember { mutableStateOf(0L) }
            val scope = rememberCoroutineScope()
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { granted ->
                cameraPermissionGranted = granted
            }
            val cameraBaselineCoverage = report?.let { currentReport ->
                CameraBaselineCoverageCalculator.evaluate(
                    cameras = currentReport.cameras,
                    baselinesByCamera = baselinesByCamera,
                    maximumCameraCount = MAX_MULTI_CAMERA_SCAN_CHANNELS,
                )
            }
            val patrolCameraId = cameraBaselineCoverage
                ?.usableBaselineCameraIds
                ?.firstOrNull()
                ?: baselineResult?.cameraId
            val patrolBaseline = patrolCameraId?.let { baselinesByCamera[it] }
                ?: baselineResult
            val baselineStale = patrolBaseline?.let {
                it.collectedAtMillis > 0L &&
                    System.currentTimeMillis() - it.collectedAtMillis > BASELINE_STALE_MILLIS
            } ?: false
            val patrolStatus = PatrolScheduler.evaluate(
                enabled = patrolEnabled,
                mode = patrolBatteryMode,
                hasUsableBaseline = patrolBaseline?.enablesNormalAlarmMode == true,
                baselineStale = baselineStale,
                motionState = motionState,
                batteryThermalState = batteryThermalState,
                appInForeground = appInForeground,
            )

            fun refreshReport() {
                if (!cameraPermissionGranted) return
                batteryThermalState = batteryThermalProvider.read()
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

            fun motionGatedQuality(quality: DarkQuality?): DarkQuality? =
                if (motionState.allowsDetectorFrame) quality else DarkQuality.Invalid

            fun startBaselineCapture(
                cameraId: String,
                captureId: Int,
                onFinished: (BaselineResult?) -> Unit,
            ) {
                var progress = BaselineProgress()
                val baselineSnapshots = mutableListOf<LumaFrameSnapshot>()
                var lastRecordedBaselineFrameCount = 0
                runningBaselineCameraId = cameraId
                baselineProgress = progress

                fun recordLatestBaselineFrame(result: FrameProbeResult) {
                    if (result.framesAnalyzed <= lastRecordedBaselineFrameCount) return
                    val effectiveQuality = motionGatedQuality(result.latestDarkState?.quality)
                    progress = progress.record(effectiveQuality)
                    lastRecordedBaselineFrameCount = result.framesAnalyzed
                    if (effectiveQuality != DarkQuality.Good && effectiveQuality != DarkQuality.Fair) return
                    result.latestSnapshot?.let { snapshot ->
                        if (
                            baselineSnapshots.size < MAX_BASELINE_SNAPSHOTS &&
                            result.framesAnalyzed % BASELINE_SNAPSHOT_INTERVAL == 0
                        ) {
                            baselineSnapshots += snapshot
                        }
                    }
                }

                activeProbeSession = FrameProbe(this@MainActivity).runSingleCameraProbe(
                    cameraId = cameraId,
                    durationMillis = BASELINE_DURATION_MILLIS,
                    listener = object : FrameProbeListener {
                        override fun onProgress(result: FrameProbeResult) {
                            recordLatestBaselineFrame(result)
                            runOnUiThread {
                                if (captureId == activeCaptureId) {
                                    baselineProgress = progress
                                    probeResult = result
                                }
                            }
                        }

                        override fun onCompleted(result: FrameProbeResult) {
                            recordLatestBaselineFrame(result)
                            if (result.error == USER_STOPPED_ERROR) {
                                runOnUiThread {
                                    if (captureId == activeCaptureId) {
                                        baselineResult = baselineStore.load()
                                        baselinesByCamera = baselineStore.loadAll()
                                        probeResult = result
                                        runningBaselineCameraId = null
                                        activeProbeSession = null
                                        onFinished(null)
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
                                    baselineProgress = progress
                                    baselinesByCamera = baselineStore.loadAll()
                                    baselineResult = baselineStore.load()
                                    probeResult = result
                                    runningBaselineCameraId = null
                                    activeProbeSession = null
                                    onFinished(finalResult)
                                }
                            }
                        }
                    },
                )
            }

            fun runBaseline(cameraId: String) {
                val previousSession = activeProbeSession
                val captureId = activeCaptureId + 1
                activeCaptureId = captureId
                previousSession?.stop()
                runningProbeCameraId = null
                runningScanCameraId = null
                runningMultiCameraScan = false
                runningMultiCameraBaseline = false
                multiCameraBaselineProgress = null
                runningPatrolBurst = false
                probeResult = null
                liveScanProgress = null
                multiCameraScanProgress = null
                startBaselineCapture(cameraId, captureId) { }
            }

            fun runSequentialMultiCameraBaseline() {
                val currentReport = report ?: return
                val coverage = CameraBaselineCoverageCalculator.evaluate(
                    cameras = currentReport.cameras,
                    baselinesByCamera = baselinesByCamera,
                    maximumCameraCount = MAX_MULTI_CAMERA_BASELINE_CHANNELS,
                )
                val selectedCameraIds = coverage.eligibleCameraIds
                if (selectedCameraIds.isEmpty()) return

                val previousSession = activeProbeSession
                val captureId = activeCaptureId + 1
                activeCaptureId = captureId
                previousSession?.stop()
                runningProbeCameraId = null
                runningScanCameraId = null
                runningMultiCameraScan = false
                runningMultiCameraBaseline = true
                runningPatrolBurst = false
                probeResult = null
                liveScanProgress = null
                multiCameraScanProgress = null
                multiCameraBaselineProgress = MultiCameraBaselineProgress(
                    cameraIds = selectedCameraIds,
                    activeCameraId = selectedCameraIds.first(),
                )

                fun startCamera(index: Int, completed: List<String>, failed: List<String>) {
                    if (captureId != activeCaptureId) return
                    val cameraId = selectedCameraIds.getOrNull(index)
                    if (cameraId == null) {
                        runningMultiCameraBaseline = false
                        multiCameraBaselineProgress = MultiCameraBaselineProgress(
                            cameraIds = selectedCameraIds,
                            completedCameraIds = completed,
                            failedCameraIds = failed,
                        )
                        return
                    }

                    multiCameraBaselineProgress = MultiCameraBaselineProgress(
                        cameraIds = selectedCameraIds,
                        activeCameraId = cameraId,
                        completedCameraIds = completed,
                        failedCameraIds = failed,
                    )
                    startBaselineCapture(cameraId, captureId) { result ->
                        val didProduceUsableBaseline = result?.enablesNormalAlarmMode == true
                        startCamera(
                            index = index + 1,
                            completed = completed + cameraId,
                            failed = if (didProduceUsableBaseline) failed else failed + cameraId,
                        )
                    }
                }

                startCamera(index = 0, completed = emptyList(), failed = emptyList())
            }

            fun runQuickScan(cameraId: String) {
                val durationMillis = QUICK_SCAN_DURATION_MILLIS
                val cameraBaseline = baselinesByCamera[cameraId]
                val hotPixelMap = baselineStore.loadHotPixelMap(cameraId)
                val accumulator = LiveScanAccumulator(
                    cameraId = cameraId,
                    hotPixelMap = hotPixelMap,
                    baselineModel = cameraBaseline?.baselineModel,
                )
                var lastRecordedFrameCount = 0
                val previousSession = activeProbeSession
                val captureId = activeCaptureId + 1
                activeCaptureId = captureId
                previousSession?.stop()
                runningScanCameraId = cameraId
                runningProbeCameraId = null
                runningBaselineCameraId = null
                runningMultiCameraScan = false
                runningMultiCameraBaseline = false
                multiCameraBaselineProgress = null
                runningPatrolBurst = false
                probeResult = null
                multiCameraScanProgress = null
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
                                darkQuality = motionGatedQuality(result.latestDarkState?.quality),
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

            fun runSequentialMultiCameraScan() {
                val currentReport = report ?: return
                val plan = MultiCameraWeighting.plan(currentReport.cameras)
                val coverage = CameraBaselineCoverageCalculator.evaluate(
                    cameras = currentReport.cameras,
                    baselinesByCamera = baselinesByCamera,
                    maximumCameraCount = MAX_MULTI_CAMERA_SCAN_CHANNELS,
                )
                val selectedCameraIds = coverage.eligibleCameraIds
                if (selectedCameraIds.size < 2 || !coverage.isComplete) return

                val perCameraDurationMillis = (
                    QUICK_SCAN_DURATION_MILLIS / selectedCameraIds.size
                    ).coerceAtLeast(MIN_MULTI_CAMERA_SEGMENT_MILLIS)
                val completedProgress = mutableListOf<LiveScanProgress>()
                val previousSession = activeProbeSession
                val captureId = activeCaptureId + 1
                activeCaptureId = captureId
                previousSession?.stop()
                runningMultiCameraScan = true
                runningMultiCameraBaseline = false
                multiCameraBaselineProgress = null
                runningPatrolBurst = false
                runningProbeCameraId = null
                runningBaselineCameraId = null
                probeResult = null
                liveScanProgress = null
                multiCameraScanProgress = MultiCameraScanAggregator.combine(
                    plan = plan,
                    selectedCameraIds = selectedCameraIds,
                    progressByCamera = emptyList(),
                    activeCameraId = selectedCameraIds.firstOrNull(),
                    perCameraDurationMillis = perCameraDurationMillis,
                )

                fun aggregateProgress(
                    activeProgress: LiveScanProgress? = null,
                    activeCameraId: String? = null,
                    error: String? = null,
                ): MultiCameraScanProgress =
                    MultiCameraScanAggregator.combine(
                        plan = plan,
                        selectedCameraIds = selectedCameraIds,
                        progressByCamera = completedProgress.toList() + listOfNotNull(activeProgress),
                        activeCameraId = activeCameraId,
                        perCameraDurationMillis = perCameraDurationMillis,
                        error = error,
                    )

                fun finishMultiCameraScan(finalProgress: MultiCameraScanProgress) {
                    if (finalProgress.framesAnalyzed > 0 && finalProgress.error == null) {
                        scanEventLogStore.append(
                            ScanEvent(
                                timestampMillis = System.currentTimeMillis(),
                                cameraId = "multi:${finalProgress.cameraIds.joinToString("+")}",
                                alarmState = finalProgress.alarmState,
                                durationMillis = finalProgress.durationMillis,
                                framesAnalyzed = finalProgress.framesAnalyzed,
                                validDarkFrames = finalProgress.validDarkFrames,
                                candidateEvents = finalProgress.candidateEvents,
                                eventsPerMinute = finalProgress.weightedEventsPerMinute,
                                validFrameFraction = finalProgress.validFrameFraction,
                                baselineZScore = finalProgress.baselineZScore,
                                baselineFrameCount = finalProgress.baselineFrameCount,
                            ),
                        )
                        scanEvents = scanEventLogStore.load()
                    }
                    multiCameraScanProgress = finalProgress
                    liveScanProgress = null
                    probeResult = null
                    runningScanCameraId = null
                    runningMultiCameraScan = false
                    activeProbeSession = null
                }

                fun startCamera(index: Int) {
                    if (captureId != activeCaptureId) return
                    val cameraId = selectedCameraIds.getOrNull(index)
                    if (cameraId == null) {
                        val error = completedProgress
                            .mapNotNull { it.error }
                            .takeIf { it.isNotEmpty() }
                            ?.joinToString("; ")
                        finishMultiCameraScan(
                            aggregateProgress(error = error),
                        )
                        return
                    }

                    val cameraBaseline = baselinesByCamera[cameraId]
                    val hotPixelMap = baselineStore.loadHotPixelMap(cameraId)
                    val accumulator = LiveScanAccumulator(
                        cameraId = cameraId,
                        hotPixelMap = hotPixelMap,
                        baselineModel = cameraBaseline?.baselineModel,
                    )
                    var lastRecordedFrameCount = 0
                    runningScanCameraId = cameraId
                    liveScanProgress = accumulator.snapshot(
                        durationMillis = perCameraDurationMillis,
                        elapsedMillis = 0L,
                        remainingMillis = perCameraDurationMillis,
                    )
                    multiCameraScanProgress = aggregateProgress(
                        activeProgress = liveScanProgress,
                        activeCameraId = cameraId,
                    )

                    fun recordLatestFrame(result: FrameProbeResult) {
                        val snapshot = result.latestSnapshot
                        if (snapshot != null && result.framesAnalyzed > lastRecordedFrameCount) {
                            accumulator.recordFrame(
                                LiveScanFrameInput(
                                    width = snapshot.width,
                                    height = snapshot.height,
                                    luma = snapshot.luma,
                                    darkQuality = motionGatedQuality(result.latestDarkState?.quality),
                                ),
                            )
                            lastRecordedFrameCount = result.framesAnalyzed
                        }
                    }

                    activeProbeSession = FrameProbe(this@MainActivity).runSingleCameraProbe(
                        cameraId = cameraId,
                        durationMillis = perCameraDurationMillis,
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
                                        multiCameraScanProgress = aggregateProgress(
                                            activeProgress = progress,
                                            activeCameraId = cameraId,
                                        )
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
                                        completedProgress += progress
                                        liveScanProgress = progress
                                        probeResult = result
                                        multiCameraScanProgress = aggregateProgress(
                                            activeCameraId = cameraId,
                                        )
                                        startCamera(index + 1)
                                    }
                                }
                            }
                        },
                    )
                }

                startCamera(0)
            }

            fun runPatrolBurst(
                cameraId: String,
                durationMillis: Long,
                minimumIntervalMillis: Long,
            ) {
                val cameraBaseline = baselinesByCamera[cameraId] ?: return
                if (!cameraBaseline.enablesNormalAlarmMode || activeProbeSession != null) return

                val accumulator = LiveScanAccumulator(
                    cameraId = cameraId,
                    hotPixelMap = baselineStore.loadHotPixelMap(cameraId),
                    baselineModel = cameraBaseline.baselineModel,
                )
                var lastRecordedFrameCount = 0
                val captureId = activeCaptureId + 1
                activeCaptureId = captureId
                runningPatrolBurst = true
                runningScanCameraId = cameraId
                runningProbeCameraId = null
                runningBaselineCameraId = null
                runningMultiCameraScan = false
                runningMultiCameraBaseline = false
                multiCameraBaselineProgress = null
                probeResult = null
                multiCameraScanProgress = null
                patrolBurstProgress = accumulator.snapshot(
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
                                darkQuality = motionGatedQuality(result.latestDarkState?.quality),
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
                                    patrolBurstProgress = progress
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
                                    val completedAtMillis = System.currentTimeMillis()
                                    patrolLastBurstAtMillis = completedAtMillis
                                    patrolNextBurstAtMillis = completedAtMillis + minimumIntervalMillis
                                    patrolBurstProgress = progress
                                    liveScanProgress = progress
                                    probeResult = result
                                    runningPatrolBurst = false
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
                val wasPatrolBurstRunning = runningPatrolBurst
                val wasAnyCaptureRunning = runningProbeCameraId != null ||
                    runningBaselineCameraId != null ||
                    runningScanCameraId != null ||
                    runningMultiCameraScan ||
                    runningMultiCameraBaseline ||
                    runningPatrolBurst
                val wasScanRunning = runningScanCameraId != null
                activeCaptureId += 1
                activeProbeSession?.stop()
                activeProbeSession = null
                if (wasBaselineRunning) {
                    baselineResult = baselineStore.load()
                    baselinesByCamera = baselineStore.loadAll()
                }
                if (wasScanRunning) {
                    liveScanProgress = liveScanProgress?.copy(
                        remainingMillis = 0L,
                        error = USER_STOPPED_ERROR,
                    )
                }
                if (runningMultiCameraScan) {
                    multiCameraScanProgress = multiCameraScanProgress?.copy(
                        remainingMillis = 0L,
                        error = USER_STOPPED_ERROR,
                    )
                }
                if (runningMultiCameraBaseline) {
                    multiCameraBaselineProgress = multiCameraBaselineProgress?.copy(
                        activeCameraId = null,
                    )
                }
                if (wasPatrolBurstRunning) {
                    patrolNextBurstAtMillis = System.currentTimeMillis() +
                        patrolStatus.minimumIntervalSeconds * MILLIS_PER_SECOND
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
                runningMultiCameraScan = false
                runningMultiCameraBaseline = false
                runningPatrolBurst = false
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

            val currentPatrolEnabled by rememberUpdatedState(patrolEnabled)
            val currentPatrolBatteryMode by rememberUpdatedState(patrolBatteryMode)
            val currentPatrolCameraId by rememberUpdatedState(patrolCameraId)
            val currentPatrolBaseline by rememberUpdatedState(patrolBaseline)
            val currentPatrolMotionState by rememberUpdatedState(motionState)
            val currentAppInForeground by rememberUpdatedState(appInForeground)
            val currentPatrolNextBurstAtMillis by rememberUpdatedState(patrolNextBurstAtMillis)
            val currentCaptureRunning by rememberUpdatedState(
                activeProbeSession != null ||
                    runningProbeCameraId != null ||
                    runningBaselineCameraId != null ||
                    runningScanCameraId != null ||
                    runningMultiCameraScan ||
                    runningMultiCameraBaseline ||
                    runningPatrolBurst,
            )
            val currentRunPatrolBurst by rememberUpdatedState(::runPatrolBurst)
            val currentStopPatrolBurst by rememberUpdatedState {
                if (runningPatrolBurst) {
                    stopActiveCapture()
                }
            }

            DisposableEffect(Unit) {
                val observer = object : DefaultLifecycleObserver {
                    override fun onResume(owner: LifecycleOwner) {
                        appInForeground = true
                    }

                    override fun onPause(owner: LifecycleOwner) {
                        appInForeground = false
                        currentStopPatrolBurst()
                    }
                }
                lifecycle.addObserver(observer)
                onDispose {
                    lifecycle.removeObserver(observer)
                }
            }

            DisposableEffect(Unit) {
                val motionStateProvider = MotionStateProvider(this@MainActivity)
                motionStateProvider.start { state ->
                    runOnUiThread {
                        motionState = state
                    }
                }
                batteryThermalState = batteryThermalProvider.read()

                onDispose {
                    motionStateProvider.stop()
                }
            }

            LaunchedEffect(patrolEnabled, patrolBatteryMode, patrolCameraId) {
                while (isActive && currentPatrolEnabled) {
                    val currentBatteryThermalState = batteryThermalProvider.read()
                    batteryThermalState = currentBatteryThermalState
                    val currentBaseline = currentPatrolBaseline
                    val currentStatus = PatrolScheduler.evaluate(
                        enabled = currentPatrolEnabled,
                        mode = currentPatrolBatteryMode,
                        hasUsableBaseline = currentBaseline?.enablesNormalAlarmMode == true,
                        baselineStale = currentBaseline?.let {
                            it.collectedAtMillis > 0L &&
                                System.currentTimeMillis() - it.collectedAtMillis > BASELINE_STALE_MILLIS
                        } ?: false,
                        motionState = currentPatrolMotionState,
                        batteryThermalState = currentBatteryThermalState,
                        appInForeground = currentAppInForeground,
                    )
                    val now = System.currentTimeMillis()
                    val currentCameraId = currentPatrolCameraId
                    if (
                        currentStatus.allowsCameraBurst &&
                        currentCameraId != null &&
                        !currentCaptureRunning &&
                        now >= currentPatrolNextBurstAtMillis
                    ) {
                        currentRunPatrolBurst(
                            currentCameraId,
                            currentStatus.burstDurationSeconds * MILLIS_PER_SECOND,
                            currentStatus.minimumIntervalSeconds * MILLIS_PER_SECOND,
                        )
                    }
                    delay(PATROL_TICK_MILLIS)
                }
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
                baselinesByCamera = baselinesByCamera,
                cameraBaselineCoverage = cameraBaselineCoverage,
                multiCameraBaselineProgress = multiCameraBaselineProgress,
                runningMultiCameraBaseline = runningMultiCameraBaseline,
                liveScanProgress = liveScanProgress,
                multiCameraScanProgress = multiCameraScanProgress,
                runningMultiCameraScan = runningMultiCameraScan,
                motionState = motionState,
                batteryThermalState = batteryThermalState,
                patrolStatus = patrolStatus,
                patrolBatteryMode = patrolBatteryMode,
                patrolBurstProgress = patrolBurstProgress,
                patrolLastBurstAtMillis = patrolLastBurstAtMillis,
                patrolNextBurstAtMillis = patrolNextBurstAtMillis,
                scanEvents = scanEvents,
                onExportScanLog = ::exportScanLog,
                onClearScanLog = ::clearScanLog,
                onRequestCameraPermission = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onRefresh = ::refreshReport,
                onRunBaseline = ::runBaseline,
                onRunMultiCameraBaseline = ::runSequentialMultiCameraBaseline,
                onRunQuickScan = ::runQuickScan,
                onRunMultiCameraScan = ::runSequentialMultiCameraScan,
                onStopCapture = ::stopActiveCapture,
                onTogglePatrol = {
                    batteryThermalState = batteryThermalProvider.read()
                    if (patrolEnabled) {
                        patrolEnabled = false
                        if (runningPatrolBurst) {
                            stopActiveCapture()
                        }
                    } else {
                        patrolLastBurstAtMillis = 0L
                        patrolNextBurstAtMillis = 0L
                        patrolEnabled = true
                    }
                },
                onSetPatrolBatteryMode = {
                    batteryThermalState = batteryThermalProvider.read()
                    patrolBatteryMode = it
                },
                onRunProbe = { cameraId ->
                    val previousSession = activeProbeSession
                    val captureId = activeCaptureId + 1
                    activeCaptureId = captureId
                    previousSession?.stop()
                    runningProbeCameraId = cameraId
                    runningBaselineCameraId = null
                    runningScanCameraId = null
                    runningMultiCameraScan = false
                    probeResult = null
                    multiCameraScanProgress = null
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
        const val BASELINE_DURATION_MILLIS = 60_000L
        const val MAX_MULTI_CAMERA_SCAN_CHANNELS = 3
        const val MAX_MULTI_CAMERA_BASELINE_CHANNELS = 3
        const val MIN_MULTI_CAMERA_SEGMENT_MILLIS = 10_000L
        const val MAX_BASELINE_SNAPSHOTS = 120
        const val BASELINE_SNAPSHOT_INTERVAL = 5
        const val BASELINE_STALE_MILLIS = 72L * 60L * 60L * 1_000L
        const val MILLIS_PER_SECOND = 1_000L
        const val PATROL_TICK_MILLIS = 1_000L
        const val USER_STOPPED_ERROR = "Stopped by user."
    }
}
