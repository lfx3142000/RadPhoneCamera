package com.radphonecamera.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
    probeResult: FrameProbeResult?,
    baselineProgress: BaselineProgress,
    baselineResult: BaselineResult?,
    onRequestCameraPermission: () -> Unit,
    onRefresh: () -> Unit,
    onRunBaseline: (String) -> Unit,
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
                    StatusPanel(
                        report = report,
                        loadingReport = loadingReport,
                        reportError = reportError,
                        baselineProgress = baselineProgress,
                        baselineResult = baselineResult,
                        runningBaselineCameraId = runningBaselineCameraId,
                        onRefresh = onRefresh,
                    )
                }

                report?.let { cameraReport ->
                    items(cameraReport.cameras, key = { it.cameraId }) { camera ->
                        CameraCard(
                            camera = camera,
                            isProbeRunning = runningProbeCameraId == camera.cameraId,
                            isBaselineRunning = runningBaselineCameraId == camera.cameraId,
                            anyBaselineRunning = runningBaselineCameraId != null,
                            probeResult = probeResult?.takeIf { it.cameraId == camera.cameraId },
                            baselineResult = baselineResult,
                            onRunBaseline = { onRunBaseline(camera.cameraId) },
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
private fun StatusPanel(
    report: DeviceCameraReport?,
    loadingReport: Boolean,
    reportError: String?,
    baselineProgress: BaselineProgress,
    baselineResult: BaselineResult?,
    runningBaselineCameraId: String?,
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
                ),
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
    anyBaselineRunning: Boolean,
    probeResult: FrameProbeResult?,
    baselineResult: BaselineResult?,
    onRunBaseline: () -> Unit,
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

            Button(
                onClick = onRunProbe,
                enabled = camera.supportsYuv && !isProbeRunning && !anyBaselineRunning,
            ) {
                Text(if (isProbeRunning) "Probing YUV frames" else "Run YUV probe")
            }

            Button(
                onClick = onRunBaseline,
                enabled = camera.supportsYuv && !isProbeRunning && !anyBaselineRunning,
            ) {
                Text(if (isBaselineRunning) "Collecting baseline" else "Refresh baseline")
            }

            baselineResult?.let {
                BaselineSummary(it)
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
): String = when {
    runningBaselineCameraId != null ->
        "Baseline running on camera $runningBaselineCameraId: ${baselineProgress.validDarkFrames}/${baselineProgress.totalFrames} valid dark frames."

    baselineResult != null ->
        "Baseline: ${baselineResult.quality.label}. ${baselineResult.message}"

    else ->
        "Baseline status: not started. Normal alarm mode remains disabled until a valid dark baseline exists."
}
