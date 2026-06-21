package com.radphonecamera.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.radphonecamera.app.baseline.BaselineProgress
import com.radphonecamera.app.baseline.BaselineQuality
import com.radphonecamera.app.baseline.BaselineRefreshRecommendation
import com.radphonecamera.app.baseline.BaselineResult
import com.radphonecamera.app.baseline.CameraBaselineCoverage
import com.radphonecamera.app.baseline.MultiCameraBaselineProgress
import com.radphonecamera.app.camera.CameraCapability
import com.radphonecamera.app.camera.DeviceCameraReport
import com.radphonecamera.app.camera.FrameProbeResult
import com.radphonecamera.app.data.ScanEvent
import com.radphonecamera.app.detector.AlarmState
import com.radphonecamera.app.detector.LiveScanProgress
import com.radphonecamera.app.detector.MultiCameraScanProgress
import com.radphonecamera.app.detector.MultiCameraWeighting
import com.radphonecamera.app.patrol.PatrolBatteryMode
import com.radphonecamera.app.patrol.PatrolStatus
import com.radphonecamera.app.sensors.BatteryThermalState
import com.radphonecamera.app.sensors.MotionState
import java.util.Locale

private val AppColors = lightColorScheme(
    primary = Color(0xFF126B57),
    secondary = Color(0xFF4C6272),
    background = Color(0xFFF7F8FA),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1A1D21),
    onSurface = Color(0xFF1A1D21),
)

@Composable
fun RadPhoneCameraApp(
    cameraPermissionGranted: Boolean,
    report: DeviceCameraReport?,
    loadingReport: Boolean,
    reportError: String?,
    runningProbeCameraId: String?,
    runningBaselineCameraId: String?,
    runningScanCameraId: String?,
    probeResult: FrameProbeResult?,
    baselineProgress: BaselineProgress,
    baselineResult: BaselineResult?,
    baselineRefreshRecommendation: BaselineRefreshRecommendation,
    baselinesByCamera: Map<String, BaselineResult>,
    cameraBaselineCoverage: CameraBaselineCoverage?,
    multiCameraBaselineProgress: MultiCameraBaselineProgress?,
    runningMultiCameraBaseline: Boolean,
    liveScanProgress: LiveScanProgress?,
    multiCameraScanProgress: MultiCameraScanProgress?,
    runningMultiCameraScan: Boolean,
    motionState: MotionState,
    batteryThermalState: BatteryThermalState,
    patrolStatus: PatrolStatus,
    patrolBatteryMode: PatrolBatteryMode,
    patrolBurstProgress: LiveScanProgress?,
    patrolLastBurstAtMillis: Long,
    patrolNextBurstAtMillis: Long,
    scanEvents: List<ScanEvent>,
    localEventLogEnabled: Boolean,
    deleteLocalDataArmed: Boolean,
    onExportScanLog: () -> Unit,
    onClearScanLog: () -> Unit,
    onSetLocalEventLogEnabled: (Boolean) -> Unit,
    onRequestDeleteLocalData: () -> Unit,
    onCancelDeleteLocalData: () -> Unit,
    onRequestCameraPermission: () -> Unit,
    onRefresh: () -> Unit,
    onRunBaseline: (String) -> Unit,
    onRunMultiCameraBaseline: () -> Unit,
    onRunQuickScan: (String) -> Unit,
    onRunMultiCameraScan: () -> Unit,
    onStopCapture: () -> Unit,
    onTogglePatrol: () -> Unit,
    onSetPatrolBatteryMode: (PatrolBatteryMode) -> Unit,
    onRunProbe: (String) -> Unit,
) {
    val anyCaptureRunning = runningProbeCameraId != null ||
        runningBaselineCameraId != null ||
        runningScanCameraId != null ||
        runningMultiCameraScan ||
        runningMultiCameraBaseline

    MaterialTheme(colorScheme = AppColors) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Header()
                }

                if (!cameraPermissionGranted) {
                    item {
                        PermissionPanel(onRequestCameraPermission)
                    }
                    return@LazyColumn
                }

                item {
                    FirstUsePanel(
                        report = report,
                        baselineResult = baselineResult,
                        baselineRefreshRecommendation = baselineRefreshRecommendation,
                        runningBaselineCameraId = runningBaselineCameraId,
                        runningScanCameraId = runningScanCameraId,
                        runningMultiCameraScan = runningMultiCameraScan,
                    )
                }

                item {
                    StatusPanel(
                        report = report,
                        loadingReport = loadingReport,
                        reportError = reportError,
                        probeResult = probeResult,
                        baselineProgress = baselineProgress,
                        baselineResult = baselineResult,
                        baselineRefreshRecommendation = baselineRefreshRecommendation,
                        runningBaselineCameraId = runningBaselineCameraId,
                        liveScanProgress = liveScanProgress,
                        multiCameraScanProgress = multiCameraScanProgress,
                        motionState = motionState,
                        onRefresh = onRefresh,
                    )
                }

                item {
                    PatrolPanel(
                        patrolStatus = patrolStatus,
                        patrolBatteryMode = patrolBatteryMode,
                        batteryThermalState = batteryThermalState,
                        motionState = motionState,
                        patrolBurstProgress = patrolBurstProgress,
                        patrolLastBurstAtMillis = patrolLastBurstAtMillis,
                        patrolNextBurstAtMillis = patrolNextBurstAtMillis,
                        onTogglePatrol = onTogglePatrol,
                        onSetPatrolBatteryMode = onSetPatrolBatteryMode,
                    )
                }

                item {
                    PrivacyPanel(
                        localEventLogEnabled = localEventLogEnabled,
                        deleteLocalDataArmed = deleteLocalDataArmed,
                        anyCaptureRunning = anyCaptureRunning,
                        onSetLocalEventLogEnabled = onSetLocalEventLogEnabled,
                        onRequestDeleteLocalData = onRequestDeleteLocalData,
                        onCancelDeleteLocalData = onCancelDeleteLocalData,
                    )
                }

                report?.let { cameraReport ->
                    item {
                        MultiCameraBaselinePanel(
                            coverage = cameraBaselineCoverage,
                            progress = multiCameraBaselineProgress,
                            running = runningMultiCameraBaseline,
                            anyCaptureRunning = anyCaptureRunning,
                            onRunMultiCameraBaseline = onRunMultiCameraBaseline,
                            onStopCapture = onStopCapture,
                        )
                    }
                    item {
                        MultiCameraScanPanel(
                            report = cameraReport,
                            coverage = cameraBaselineCoverage,
                            progress = multiCameraScanProgress,
                            runningMultiCameraScan = runningMultiCameraScan,
                            anyCaptureRunning = anyCaptureRunning,
                            onRunMultiCameraScan = onRunMultiCameraScan,
                            onStopCapture = onStopCapture,
                        )
                    }
                }

                item {
                    ScanEventLogPanel(
                        scanEvents = scanEvents,
                        localEventLogEnabled = localEventLogEnabled,
                        onExportScanLog = onExportScanLog,
                        onClearScanLog = onClearScanLog,
                    )
                }

                report?.let { cameraReport ->
                    items(cameraReport.cameras, key = { it.cameraId }) { camera ->
                        CameraCard(
                            camera = camera,
                            isProbeRunning = runningProbeCameraId == camera.cameraId,
                            isBaselineRunning = runningBaselineCameraId == camera.cameraId,
                            isScanRunning = runningScanCameraId == camera.cameraId,
                            anyCaptureRunning = anyCaptureRunning,
                            probeResult = probeResult?.takeIf { it.cameraId == camera.cameraId },
                            baselineResult = baselinesByCamera[camera.cameraId],
                            liveScanProgress = liveScanProgress?.takeIf { it.cameraId == camera.cameraId },
                            onRunBaseline = { onRunBaseline(camera.cameraId) },
                            onRunQuickScan = { onRunQuickScan(camera.cameraId) },
                            onStopCapture = onStopCapture,
                            onRunProbe = { onRunProbe(camera.cameraId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanEventLogPanel(
    scanEvents: List<ScanEvent>,
    localEventLogEnabled: Boolean,
    onExportScanLog: () -> Unit,
    onClearScanLog: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Scan log",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (!localEventLogEnabled) {
                Text(
                    text = "New scan summaries are not being saved.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onExportScanLog,
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp),
                    enabled = scanEvents.isNotEmpty(),
                ) {
                    Text("Export CSV")
                }
                Button(
                    onClick = onClearScanLog,
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp),
                    enabled = scanEvents.isNotEmpty(),
                ) {
                    Text("Delete log")
                }
            }
            if (scanEvents.isEmpty()) {
                Text(
                    text = "No completed quick scans yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            } else {
                scanEvents.take(MAX_VISIBLE_SCAN_EVENTS).forEach { event ->
                    ScanEventRow(event)
                }
            }
        }
    }
}

@Composable
private fun PrivacyPanel(
    localEventLogEnabled: Boolean,
    deleteLocalDataArmed: Boolean,
    anyCaptureRunning: Boolean,
    onSetLocalEventLogEnabled: (Boolean) -> Unit,
    onRequestDeleteLocalData: () -> Unit,
    onCancelDeleteLocalData: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Data and privacy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Frames are processed and discarded on this phone. No photos, GPS, or cloud upload are used.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Save scan summaries locally",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Stores summary statistics only; never frames or location.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                Switch(
                    checked = localEventLogEnabled,
                    onCheckedChange = onSetLocalEventLogEnabled,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onRequestDeleteLocalData,
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp),
                    enabled = !anyCaptureRunning,
                ) {
                    Text(if (deleteLocalDataArmed) "Confirm delete" else "Delete local data")
                }
                if (deleteLocalDataArmed) {
                    Button(
                        onClick = onCancelDeleteLocalData,
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp),
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanEventRow(event: ScanEvent) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "Camera ${event.cameraId}: ${event.alarmState.label}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = "${event.timestampMillis.eventAgeText()} - ${event.eventsPerMinute.fixed(1)} candidate events/min, ${event.validDarkFrames}/${event.framesAnalyzed} valid frames.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        if (event.baselineFrameCount > 0) {
            Text(
                text = "Baseline Z ${event.baselineZScore.fixed(1)} from ${event.baselineFrameCount} baseline frames.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "RadPhoneCamera",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Camera-only radiation-like anomaly screening debug build. Local processing only.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun PermissionPanel(onRequestCameraPermission: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Camera permission required",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "The camera sensor is the detector input. Frames are analyzed on this device and are not saved by default.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onRequestCameraPermission) {
                Text("Grant camera access")
            }
        }
    }
}

@Composable
private fun FirstUsePanel(
    report: DeviceCameraReport?,
    baselineResult: BaselineResult?,
    baselineRefreshRecommendation: BaselineRefreshRecommendation,
    runningBaselineCameraId: String?,
    runningScanCameraId: String?,
    runningMultiCameraScan: Boolean,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Start here",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = firstUseMessage(
                    report = report,
                    baselineResult = baselineResult,
                    baselineRefreshRecommendation = baselineRefreshRecommendation,
                    runningBaselineCameraId = runningBaselineCameraId,
                    runningScanCameraId = runningScanCameraId,
                    runningMultiCameraScan = runningMultiCameraScan,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "For the baseline: put the phone face down on a flat surface or inside a dark pocket. Leave it still for 60 seconds. It stops automatically when the timer reaches 0; use Stop only if you need to cancel.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            if (runningScanCameraId != null || runningMultiCameraScan) {
                Text(
                    text = "Quick scan is running. Keep the phone dark and still until the timer finishes or tap Stop.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun StatusPanel(
    report: DeviceCameraReport?,
    loadingReport: Boolean,
    reportError: String?,
    probeResult: FrameProbeResult?,
    baselineProgress: BaselineProgress,
    baselineResult: BaselineResult?,
    baselineRefreshRecommendation: BaselineRefreshRecommendation,
    runningBaselineCameraId: String?,
    liveScanProgress: LiveScanProgress?,
    multiCameraScanProgress: MultiCameraScanProgress?,
    motionState: MotionState,
    onRefresh: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Device check",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Button(onClick = onRefresh, enabled = !loadingReport) {
                    Text(if (loadingReport) "Checking" else "Refresh")
                }
            }
            Text(
                text = when {
                    reportError != null -> "Error: $reportError"
                    loadingReport -> "Reading Camera2 capabilities..."
                    report != null -> report.summary
                    else -> "No report loaded yet."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Motion: ${motionState.quality.label}, ${motionState.posture.label}; ${motionState.reason}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = baselineStatusText(
                    baselineProgress = baselineProgress,
                    baselineResult = baselineResult,
                    runningBaselineCameraId = runningBaselineCameraId,
                    activeResult = probeResult,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            if (baselineRefreshRecommendation.shouldRefresh && baselineResult != null) {
                Text(
                    text = baselineRefreshRecommendation.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            liveScanProgress?.let {
                Text(
                    text = "Quick scan: ${it.alarmState.label}, ${it.eventsPerMinute.fixed(1)} candidate events/min, ${it.validDarkFrames}/${it.framesAnalyzed} valid dark frames.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            multiCameraScanProgress?.let {
                Text(
                    text = "Multi-camera scan: ${it.alarmState.label}, ${it.completedCameraCount}/${it.totalCameraCount} channels, ${it.weightedEventsPerMinute.fixed(1)} weighted events/min.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            report?.let {
                val plan = MultiCameraWeighting.plan(it.cameras)
                Text(
                    text = "Multi-camera plan: ${plan.supportLevel.label}, ${plan.activeCameraCount} usable channels, combined score ${plan.combinedScore}/100.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                if (plan.cameraWeights.size > 1) {
                    Text(
                        text = "Weights: ${plan.cameraWeights.take(3).joinToString { weight -> "camera ${weight.cameraId} ${(weight.weight * 100.0).fixed(0)}%" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun PatrolPanel(
    patrolStatus: PatrolStatus,
    patrolBatteryMode: PatrolBatteryMode,
    batteryThermalState: BatteryThermalState,
    motionState: MotionState,
    patrolBurstProgress: LiveScanProgress?,
    patrolLastBurstAtMillis: Long,
    patrolNextBurstAtMillis: Long,
    onTogglePatrol: () -> Unit,
    onSetPatrolBatteryMode: (PatrolBatteryMode) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Patrol",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = patrolStatus.readiness.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                Button(onClick = onTogglePatrol) {
                    Text(if (patrolStatus.enabled) "Turn off" else "Turn on")
                }
            }

            Text(
                text = patrolStatus.reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = "Foreground-only Patrol uses ${patrolStatus.burstDurationSeconds}s bursts at least ${patrolStatus.minimumIntervalSeconds}s apart. It closes the camera when the app is not visible.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            patrolBurstProgress?.let { progress ->
                Text(
                    text = if (progress.remainingMillis > 0L) {
                        "Burst on camera ${progress.cameraId}: ${progress.remainingMillis.asSeconds()} remaining."
                    } else {
                        "Last burst on camera ${progress.cameraId}: ${progress.candidateEvents} candidates in ${progress.framesAnalyzed} frames."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            if (patrolStatus.enabled && patrolNextBurstAtMillis > 0L) {
                Text(
                    text = "Next eligible burst: ${patrolNextBurstAtMillis.timeFromNowText()}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            } else if (patrolLastBurstAtMillis > 0L) {
                Text(
                    text = "Last Patrol burst: ${patrolLastBurstAtMillis.timeAgoText()}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Text(
                text = "Battery ${batteryThermalState.batteryPercent?.let { "$it%" } ?: "unknown"}, charging ${batteryThermalState.isCharging.label()}, thermal ${batteryThermalState.thermalStatus}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = "Motion gate: ${motionState.quality.label}, ${motionState.posture.label}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PatrolBatteryMode.values().forEach { mode ->
                    Button(
                        onClick = { onSetPatrolBatteryMode(mode) },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp),
                        enabled = patrolBatteryMode != mode,
                    ) {
                        Text(mode.shortLabel())
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiCameraBaselinePanel(
    coverage: CameraBaselineCoverage?,
    progress: MultiCameraBaselineProgress?,
    running: Boolean,
    anyCaptureRunning: Boolean,
    onRunMultiCameraBaseline: () -> Unit,
    onStopCapture: () -> Unit,
) {
    val eligibleCameraIds = coverage?.eligibleCameraIds.orEmpty()
    val missingCameraIds = coverage?.missingCameraIds.orEmpty()
    val canStart = eligibleCameraIds.isNotEmpty() && !anyCaptureRunning

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Camera baselines",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = when {
                    eligibleCameraIds.isEmpty() -> "No Camera2 YUV channel is eligible for a detector baseline."
                    coverage?.isComplete == true -> "Baselines ready for ${coverage.coveredCameraCount}/${eligibleCameraIds.size} selected cameras."
                    else -> "Baseline coverage ${coverage?.coveredCameraCount ?: 0}/${eligibleCameraIds.size}. Multi-camera scans stay limited until each selected camera has a usable dark baseline."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            if (missingCameraIds.isNotEmpty()) {
                Text(
                    text = "Needs baseline: ${missingCameraIds.joinToString()}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onRunMultiCameraBaseline,
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                    enabled = canStart,
                ) {
                    Text(if (running) "Collecting" else "Refresh all baselines")
                }
                if (running) {
                    Button(
                        onClick = onStopCapture,
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                    ) {
                        Text("Stop")
                    }
                }
            }
            progress?.let {
                val activeCameraText = it.activeCameraId?.let { cameraId -> "Collecting camera $cameraId" }
                    ?: "Baseline collection finished"
                Text(
                    text = "$activeCameraText, ${it.completedCameraCount}/${it.totalCameraCount} cameras complete.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                if (it.failedCameraIds.isNotEmpty()) {
                    Text(
                        text = "Needs retry: ${it.failedCameraIds.joinToString()}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun MultiCameraScanPanel(
    report: DeviceCameraReport,
    coverage: CameraBaselineCoverage?,
    progress: MultiCameraScanProgress?,
    runningMultiCameraScan: Boolean,
    anyCaptureRunning: Boolean,
    onRunMultiCameraScan: () -> Unit,
    onStopCapture: () -> Unit,
) {
    val plan = MultiCameraWeighting.plan(report.cameras)
    val selectedCameraIds = plan.cameraWeights.take(MAX_MULTI_CAMERA_SCAN_CHANNELS).map { it.cameraId }
    val enabled = selectedCameraIds.size >= 2 &&
        coverage?.isComplete == true &&
        !anyCaptureRunning

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Multi-camera quick scan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (selectedCameraIds.size >= 2) {
                    if (coverage?.isComplete == true) {
                        "Sequential scan: cameras ${selectedCameraIds.joinToString()}, each using its own baseline and hot-pixel mask."
                    } else {
                        "Collect a usable baseline for every selected camera before multi-camera scanning."
                    }
                } else {
                    "Multi-camera scan needs at least two usable Camera2 YUV channels."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onRunMultiCameraScan,
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                    enabled = enabled,
                ) {
                    Text(if (runningMultiCameraScan) "Scanning" else "Start multi-camera")
                }
                if (runningMultiCameraScan) {
                    Button(
                        onClick = onStopCapture,
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                    ) {
                        Text("Stop")
                    }
                }
            }
            progress?.let {
                MultiCameraScanSummary(it)
            }
        }
    }
}

@Composable
private fun MultiCameraScanSummary(progress: MultiCameraScanProgress) {
    val color = when (progress.alarmState) {
        AlarmState.Baseline -> MaterialTheme.colorScheme.primary
        AlarmState.LowAnomaly,
        AlarmState.Elevated,
        AlarmState.HighElevated -> MaterialTheme.colorScheme.error

        AlarmState.LimitedSensitivity -> MaterialTheme.colorScheme.secondary
        AlarmState.Invalid -> MaterialTheme.colorScheme.error
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Combined: ${progress.alarmState.label}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = color,
        )
        if (progress.activeCameraId != null) {
            Text(
                text = "Scanning camera ${progress.activeCameraId}; ${progress.completedCameraCount}/${progress.totalCameraCount} channels complete.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        if (progress.remainingMillis > 0L) {
            Text(
                text = "Total time remaining: ${progress.remainingMillis.asSeconds()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Text(
            text = "${progress.weightedEventsPerMinute.fixed(1)} weighted candidate events/min, ${progress.candidateEvents} total candidates.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = "${progress.validDarkFrames}/${progress.framesAnalyzed} valid dark frames (${(progress.validFrameFraction * 100.0).fixed(0)}%).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        if (progress.baselineFrameCount > 0) {
            Text(
                text = "Combined baseline Z: ${progress.baselineZScore.fixed(1)} from ${progress.baselineFrameCount} baseline frames.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        progress.error?.let {
            Text(
                text = "Scan note: $it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun CameraCard(
    camera: CameraCapability,
    isProbeRunning: Boolean,
    isBaselineRunning: Boolean,
    isScanRunning: Boolean,
    anyCaptureRunning: Boolean,
    probeResult: FrameProbeResult?,
    baselineResult: BaselineResult?,
    liveScanProgress: LiveScanProgress?,
    onRunBaseline: () -> Unit,
    onRunQuickScan: () -> Unit,
    onStopCapture: () -> Unit,
    onRunProbe: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Camera ${camera.cameraId}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${camera.lensFacing} - ${camera.hardwareLevel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                Text(
                    text = "${camera.detectorScore.score}/100",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Text(
                text = camera.detectorScore.supportLevel.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )

            CapabilityRows(camera)

            HorizontalDivider()

            camera.detectorScore.reasons.take(5).forEach { reason ->
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onRunProbe,
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                    enabled = camera.supportsYuv && !anyCaptureRunning,
                ) {
                    Text(if (isProbeRunning) "Probing" else "Test camera")
                }

                Button(
                    onClick = onRunBaseline,
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                    enabled = camera.supportsYuv && !anyCaptureRunning,
                ) {
                    Text(if (isBaselineRunning) "Collecting" else "Start baseline")
                }
            }

            Button(
                onClick = onRunQuickScan,
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                enabled = camera.supportsYuv &&
                    baselineResult?.enablesNormalAlarmMode == true &&
                    !anyCaptureRunning,
            ) {
                Text(if (isScanRunning) "Scanning" else "Quick scan")
            }

            if (isProbeRunning || isBaselineRunning || isScanRunning) {
                Button(
                    onClick = onStopCapture,
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                ) {
                    Text("Stop")
                }
            }

            baselineResult?.let {
                BaselineSummary(it)
            }

            liveScanProgress?.let {
                LiveScanSummary(it)
            }

            probeResult?.let {
                ProbeSummary(it)
            }
        }
    }
}

@Composable
private fun CapabilityRows(camera: CameraCapability) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        CapabilityRow("YUV", camera.supportsYuv.label())
        CapabilityRow("RAW", camera.supportsRaw.label())
        CapabilityRow("Manual exposure", camera.supportsManualExposure.label())
        CapabilityRow("Manual ISO", camera.supportsManualSensitivity.label())
        CapabilityRow("Focus control", camera.supportsFocusLock.label())
        CapabilityRow("Suggested YUV", camera.suggestedYuvSize?.toString() ?: "None")
        if (camera.physicalCameraIds.isNotEmpty()) {
            CapabilityRow("Physical cameras", camera.physicalCameraIds.joinToString())
        }
    }
}

@Composable
private fun CapabilityRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun BaselineSummary(result: BaselineResult) {
    val color = when (result.quality) {
        BaselineQuality.Good -> MaterialTheme.colorScheme.primary
        BaselineQuality.Fair -> MaterialTheme.colorScheme.primary
        BaselineQuality.Poor -> MaterialTheme.colorScheme.secondary
        BaselineQuality.Invalid -> MaterialTheme.colorScheme.error
    }
    val nowMillis = System.currentTimeMillis()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Baseline: ${result.quality.label}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = color,
        )
        Text(
            text = "${result.progress.validDarkFrames}/${result.progress.totalFrames} valid dark frames. ${result.message}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = result.ageText(nowMillis),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        if (result.isStale(nowMillis)) {
            Text(
                text = "Baseline refresh recommended after 72 hours without new dark data.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        if (result.cameraId != null || result.hotPixelCount > 0) {
            Text(
                text = "Camera ${result.cameraId ?: "unknown"} baseline, ${result.hotPixelCount} hot pixels mapped.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        if (result.baselineEventFrameCount > 0) {
            Text(
                text = "Baseline candidates: ${result.baselineCandidateEvents} across ${result.baselineEventFrameCount} sampled frames.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun LiveScanSummary(progress: LiveScanProgress) {
    val color = when (progress.alarmState) {
        AlarmState.Baseline -> MaterialTheme.colorScheme.primary
        AlarmState.LowAnomaly,
        AlarmState.Elevated,
        AlarmState.HighElevated -> MaterialTheme.colorScheme.error

        AlarmState.LimitedSensitivity -> MaterialTheme.colorScheme.secondary
        AlarmState.Invalid -> MaterialTheme.colorScheme.error
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Quick scan: ${progress.alarmState.label}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = color,
        )
        if (progress.remainingMillis > 0L) {
            Text(
                text = "Time remaining: ${progress.remainingMillis.asSeconds()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Text(
            text = "${progress.eventsPerMinute.fixed(1)} candidate events/min, ${progress.candidateEvents} total candidates.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = "${progress.validDarkFrames}/${progress.framesAnalyzed} valid dark frames (${(progress.validFrameFraction * 100.0).fixed(0)}%).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        if (progress.baselineFrameCount > 0) {
            Text(
                text = "Baseline Z: ${progress.baselineZScore.fixed(1)} from ${progress.baselineFrameCount} baseline frames.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Text(
            text = if (progress.hotPixelMaskApplied) {
                "Hot-pixel mask active for this scan."
            } else {
                "Hot-pixel mask not loaded; refresh baseline first for better rejection."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        progress.error?.let {
            Text(
                text = "Scan note: $it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun ProbeSummary(result: FrameProbeResult) {
    Spacer(modifier = Modifier.height(4.dp))
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Probe frames: ${result.framesAnalyzed}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        if (result.durationMillis > 0L && result.remainingMillis > 0L) {
            Text(
                text = "Time remaining: ${result.remainingMillis.asSeconds()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        result.error?.let {
            Text(
                text = "Probe error: $it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        result.latestStats?.let { stats ->
            Text(
                text = "Luma mean ${stats.mean.fixed(1)}, variance ${stats.variance.fixed(1)}, range ${stats.min}-${stats.max}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        result.latestDarkState?.let { state ->
            Text(
                text = "${state.quality.label}: ${state.reason}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Text(
            text = "Manual controls: exposure ${result.manualControlAttempt.exposureLocked.label()}, ISO ${result.manualControlAttempt.sensitivityLocked.label()}, focus ${result.manualControlAttempt.focusLocked.label()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

private fun Boolean.label(): String = if (this) "Yes" else "No"

private fun Double.fixed(digits: Int): String =
    String.format(Locale.US, "%.${digits}f", this)

private fun baselineStatusText(
    baselineProgress: BaselineProgress,
    baselineResult: BaselineResult?,
    runningBaselineCameraId: String?,
    activeResult: FrameProbeResult?,
): String = when {
    runningBaselineCameraId != null ->
        "Baseline running on camera $runningBaselineCameraId: ${activeResult?.remainingMillis?.asSeconds() ?: "60s"} remaining, ${baselineProgress.validDarkFrames}/${baselineProgress.totalFrames} valid dark frames."

    baselineResult != null ->
        "Baseline: ${baselineResult.quality.label}. ${baselineResult.message}"

    else ->
        "Baseline status: not started. Normal alarm mode remains disabled until a valid dark baseline exists."
}

private fun Long.asSeconds(): String {
    val seconds = ((this + 999L) / 1_000L).coerceAtLeast(0L)
    return "${seconds}s"
}

private fun Long.timeFromNowText(): String {
    val seconds = ((this - System.currentTimeMillis()).coerceAtLeast(0L) + 999L) / 1_000L
    return if (seconds < 60L) "in ${seconds}s" else "in ${(seconds + 59L) / 60L}m"
}

private fun Long.timeAgoText(): String {
    val seconds = ((System.currentTimeMillis() - this).coerceAtLeast(0L) + 999L) / 1_000L
    return if (seconds < 60L) "${seconds}s ago" else "${(seconds + 59L) / 60L}m ago"
}

private fun firstUseMessage(
    report: DeviceCameraReport?,
    baselineResult: BaselineResult?,
    baselineRefreshRecommendation: BaselineRefreshRecommendation,
    runningBaselineCameraId: String?,
    runningScanCameraId: String?,
    runningMultiCameraScan: Boolean,
): String = when {
    report == null -> "Grant camera access, then wait for the device check to list usable cameras."
    runningBaselineCameraId != null -> "Baseline is running. Keep the phone dark and still until the timer reaches 0. It will stop automatically."
    runningMultiCameraScan -> "Multi-camera Quick scan is running. Keep the phone face down, dark, and still until all camera channels finish."
    runningScanCameraId != null -> "Quick scan is running. Keep the phone face down, dark, and still until the timer finishes."
    baselineRefreshRecommendation.shouldRefresh && baselineResult?.enablesNormalAlarmMode == true -> baselineRefreshRecommendation.summary
    baselineResult?.isStale() == true -> "Baseline is usable, but a refresh is recommended. Tap Start baseline with the phone face down and still."
    baselineResult?.enablesNormalAlarmMode == true -> "Baseline is ready. Use Quick scan for a 30-second dark scan, or repeat baseline if conditions change."
    baselineResult != null -> "Baseline is not good enough yet. Try Start baseline again with the phone face down and completely still."
    else -> "Choose the best back camera, then tap Start baseline before using detector features."
}

private fun PatrolBatteryMode.shortLabel(): String = when (this) {
    PatrolBatteryMode.BatterySaver -> "Saver"
    PatrolBatteryMode.Balanced -> "Balanced"
    PatrolBatteryMode.MaxSensitivity -> "Max"
}

private fun BaselineResult.isStale(nowMillis: Long = System.currentTimeMillis()): Boolean =
    collectedAtMillis > 0L && nowMillis - collectedAtMillis > BASELINE_STALE_MILLIS

private fun BaselineResult.ageText(nowMillis: Long): String {
    if (collectedAtMillis <= 0L) return "Last baseline: not recorded."
    val elapsedMillis = (nowMillis - collectedAtMillis).coerceAtLeast(0L)
    val elapsedHours = elapsedMillis / MILLIS_PER_HOUR
    return when {
        elapsedHours < 1L -> "Last baseline: less than 1 hour ago."
        elapsedHours < 24L -> "Last baseline: ${elapsedHours}h ago."
        else -> "Last baseline: ${elapsedHours / 24L}d ago."
    }
}

private fun Long.eventAgeText(nowMillis: Long = System.currentTimeMillis()): String {
    val elapsedMillis = (nowMillis - this).coerceAtLeast(0L)
    val elapsedMinutes = elapsedMillis / MILLIS_PER_MINUTE
    val elapsedHours = elapsedMillis / MILLIS_PER_HOUR
    return when {
        elapsedMinutes < 1L -> "just now"
        elapsedMinutes < 60L -> "${elapsedMinutes}m ago"
        elapsedHours < 24L -> "${elapsedHours}h ago"
        else -> "${elapsedHours / 24L}d ago"
    }
}

private const val MAX_VISIBLE_SCAN_EVENTS = 5
private const val MAX_MULTI_CAMERA_SCAN_CHANNELS = 3
private const val MILLIS_PER_MINUTE = 60L * 1_000L
private const val MILLIS_PER_HOUR = 60L * 60L * 1_000L
private const val BASELINE_STALE_MILLIS = 72L * MILLIS_PER_HOUR
