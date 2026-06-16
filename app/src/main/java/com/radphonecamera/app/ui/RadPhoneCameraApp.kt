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
import com.radphonecamera.app.baseline.BaselineResult
import com.radphonecamera.app.camera.CameraCapability
import com.radphonecamera.app.camera.DeviceCameraReport
import com.radphonecamera.app.camera.FrameProbeResult
import com.radphonecamera.app.detector.AlarmState
import com.radphonecamera.app.detector.LiveScanProgress
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
    liveScanProgress: LiveScanProgress?,
    onRequestCameraPermission: () -> Unit,
    onRefresh: () -> Unit,
    onRunBaseline: (String) -> Unit,
    onRunQuickScan: (String) -> Unit,
    onStopCapture: () -> Unit,
    onRunProbe: (String) -> Unit,
) {
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
                        runningBaselineCameraId = runningBaselineCameraId,
                        runningScanCameraId = runningScanCameraId,
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
                        runningBaselineCameraId = runningBaselineCameraId,
                        liveScanProgress = liveScanProgress,
                        onRefresh = onRefresh,
                    )
                }

                report?.let { cameraReport ->
                    val anyCaptureRunning = runningProbeCameraId != null ||
                        runningBaselineCameraId != null ||
                        runningScanCameraId != null
                    items(cameraReport.cameras, key = { it.cameraId }) { camera ->
                        CameraCard(
                            camera = camera,
                            isProbeRunning = runningProbeCameraId == camera.cameraId,
                            isBaselineRunning = runningBaselineCameraId == camera.cameraId,
                            isScanRunning = runningScanCameraId == camera.cameraId,
                            anyCaptureRunning = anyCaptureRunning,
                            probeResult = probeResult?.takeIf { it.cameraId == camera.cameraId },
                            baselineResult = baselineResult,
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
    runningBaselineCameraId: String?,
    runningScanCameraId: String?,
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
                    runningBaselineCameraId = runningBaselineCameraId,
                    runningScanCameraId = runningScanCameraId,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "For the baseline: put the phone face down on a flat surface or inside a dark pocket. Leave it still for 60 seconds. Use Stop if you need to cancel.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            if (runningScanCameraId != null) {
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
    runningBaselineCameraId: String?,
    liveScanProgress: LiveScanProgress?,
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
                text = baselineStatusText(
                    baselineProgress = baselineProgress,
                    baselineResult = baselineResult,
                    runningBaselineCameraId = runningBaselineCameraId,
                    activeResult = probeResult,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            liveScanProgress?.let {
                Text(
                    text = "Quick scan: ${it.alarmState.label}, ${it.eventsPerMinute.fixed(1)} candidate events/min, ${it.validDarkFrames}/${it.framesAnalyzed} valid dark frames.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
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
        if (result.cameraId != null || result.hotPixelCount > 0) {
            Text(
                text = "Camera ${result.cameraId ?: "unknown"} baseline, ${result.hotPixelCount} hot pixels mapped.",
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

private fun firstUseMessage(
    report: DeviceCameraReport?,
    baselineResult: BaselineResult?,
    runningBaselineCameraId: String?,
    runningScanCameraId: String?,
): String = when {
    report == null -> "Grant camera access, then wait for the device check to list usable cameras."
    runningBaselineCameraId != null -> "Baseline is running. Keep the phone dark and still until the count finishes or tap Stop."
    runningScanCameraId != null -> "Quick scan is running. Keep the phone face down, dark, and still until the timer finishes."
    baselineResult?.enablesNormalAlarmMode == true -> "Baseline is ready. Use Quick scan for a 30-second dark scan, or repeat baseline if conditions change."
    baselineResult != null -> "Baseline is not good enough yet. Try Start baseline again with the phone face down and completely still."
    else -> "Choose the best back camera, then tap Start baseline before using detector features."
}
