package com.radphonecamera.app.baseline

import com.radphonecamera.app.camera.CameraCapability
import com.radphonecamera.app.detector.AlarmState

data class BaselineEnvironmentSnapshot(
    val appVersion: String = "",
    val androidApiLevel: Int = 0,
    val deviceModel: String = "",
    val cameraSignature: String = "",
    val thermalStatus: String = "",
)

enum class BaselineRefreshReason(val label: String) {
    Missing("No usable baseline"),
    LimitedQuality("Baseline quality is limited"),
    Stale("Baseline is more than 72 hours old"),
    AppUpdated("App version changed since baseline"),
    AndroidUpdated("Android version changed since baseline"),
    CameraChanged("Camera capability changed since baseline"),
    ThermalChanged("Phone thermal behavior changed"),
    RepeatedLimitedScans("Recent scans had limited sensitivity"),
}

data class BaselineRefreshRecommendation(
    val reasons: List<BaselineRefreshReason>,
) {
    val shouldRefresh: Boolean = reasons.isNotEmpty()
    val summary: String = when {
        reasons.isEmpty() -> "Baseline is current."
        reasons.size == 1 -> "Baseline refresh recommended: ${reasons.single().label}."
        else -> "Baseline refresh recommended: ${reasons.joinToString { it.label }}."
    }
}

object BaselineRefreshEvaluator {
    fun evaluate(
        baseline: BaselineResult?,
        currentEnvironment: BaselineEnvironmentSnapshot,
        recentAlarmStates: List<AlarmState>,
        nowMillis: Long,
    ): BaselineRefreshRecommendation {
        if (baseline == null) {
            return BaselineRefreshRecommendation(listOf(BaselineRefreshReason.Missing))
        }

        val reasons = mutableListOf<BaselineRefreshReason>()
        if (!baseline.enablesNormalAlarmMode) {
            reasons += BaselineRefreshReason.LimitedQuality
        }
        if (baseline.collectedAtMillis > 0L && nowMillis - baseline.collectedAtMillis > STALE_MILLIS) {
            reasons += BaselineRefreshReason.Stale
        }

        val stored = baseline.environment
        if (stored.appVersion.isNotBlank() && stored.appVersion != currentEnvironment.appVersion) {
            reasons += BaselineRefreshReason.AppUpdated
        }
        if (
            stored.androidApiLevel > 0 &&
            currentEnvironment.androidApiLevel > 0 &&
            stored.androidApiLevel != currentEnvironment.androidApiLevel
        ) {
            reasons += BaselineRefreshReason.AndroidUpdated
        }
        val deviceChanged = stored.deviceModel.isNotBlank() &&
            currentEnvironment.deviceModel.isNotBlank() &&
            stored.deviceModel != currentEnvironment.deviceModel
        val cameraChanged = stored.cameraSignature.isNotBlank() &&
            currentEnvironment.cameraSignature.isNotBlank() &&
            stored.cameraSignature != currentEnvironment.cameraSignature
        if (deviceChanged || cameraChanged) {
            reasons += BaselineRefreshReason.CameraChanged
        }
        if (
            stored.thermalStatus.isNotBlank() &&
            currentEnvironment.thermalStatus.isWarmOrHot() &&
            stored.thermalStatus != currentEnvironment.thermalStatus
        ) {
            reasons += BaselineRefreshReason.ThermalChanged
        }
        if (recentAlarmStates.take(MAX_RECENT_SCANS).count { it.isLimitedSensitivity() } >= 2) {
            reasons += BaselineRefreshReason.RepeatedLimitedScans
        }

        return BaselineRefreshRecommendation(reasons)
    }

    private fun AlarmState.isLimitedSensitivity(): Boolean =
        this == AlarmState.LimitedSensitivity || this == AlarmState.Invalid

    private fun String.isWarmOrHot(): Boolean =
        this != "Normal" && this != "Unknown"

    private const val MAX_RECENT_SCANS = 3
    private const val STALE_MILLIS = 72L * 60L * 60L * 1_000L
}

fun CameraCapability.baselineCameraSignature(): String = listOf(
    cameraId,
    lensFacing,
    hardwareLevel,
    supportsYuv.toString(),
    supportsRaw.toString(),
    supportsManualExposure.toString(),
    supportsManualSensitivity.toString(),
    supportsFocusLock.toString(),
    detectorScore.supportLevel.name,
    detectorScore.score.toString(),
    suggestedYuvSize?.toString().orEmpty(),
).joinToString(separator = ":")
