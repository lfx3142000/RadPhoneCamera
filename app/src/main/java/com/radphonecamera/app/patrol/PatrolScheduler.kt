package com.radphonecamera.app.patrol

import com.radphonecamera.app.sensors.BatteryThermalState
import com.radphonecamera.app.sensors.DevicePosture
import com.radphonecamera.app.sensors.MotionQuality
import com.radphonecamera.app.sensors.MotionState

enum class PatrolBatteryMode(val label: String) {
    BatterySaver("Battery Saver"),
    Balanced("Balanced"),
    MaxSensitivity("Max Sensitivity"),
}

enum class PatrolReadiness(val label: String) {
    Off("Off"),
    NeedsBaseline("Baseline required"),
    PausedLowBattery("Paused: low battery"),
    PausedThermal("Paused: phone warm"),
    WaitingForDarkStable("Waiting for dark/stable"),
    ReadyForBurst("Ready for short burst"),
}

data class PatrolStatus(
    val enabled: Boolean,
    val mode: PatrolBatteryMode,
    val readiness: PatrolReadiness,
    val burstDurationSeconds: Int,
    val minimumIntervalSeconds: Int,
    val allowsCameraBurst: Boolean,
    val reason: String,
)

object PatrolScheduler {
    fun evaluate(
        enabled: Boolean,
        mode: PatrolBatteryMode,
        hasUsableBaseline: Boolean,
        baselineStale: Boolean,
        motionState: MotionState,
        batteryThermalState: BatteryThermalState,
    ): PatrolStatus {
        val burstDuration = mode.burstDurationSeconds()
        val interval = mode.minimumIntervalSeconds()

        if (!enabled) {
            return PatrolStatus(
                enabled = false,
                mode = mode,
                readiness = PatrolReadiness.Off,
                burstDurationSeconds = burstDuration,
                minimumIntervalSeconds = interval,
                allowsCameraBurst = false,
                reason = "Patrol is off; the camera is closed.",
            )
        }

        if (!hasUsableBaseline) {
            return PatrolStatus(
                enabled = true,
                mode = mode,
                readiness = PatrolReadiness.NeedsBaseline,
                burstDurationSeconds = burstDuration,
                minimumIntervalSeconds = interval,
                allowsCameraBurst = false,
                reason = "Run a guided baseline before Patrol can collect short bursts.",
            )
        }

        val batteryPercent = batteryThermalState.batteryPercent
        if (
            batteryPercent != null &&
            batteryPercent < LOW_BATTERY_PERCENT &&
            !batteryThermalState.isCharging
        ) {
            return PatrolStatus(
                enabled = true,
                mode = mode,
                readiness = PatrolReadiness.PausedLowBattery,
                burstDurationSeconds = burstDuration,
                minimumIntervalSeconds = interval,
                allowsCameraBurst = false,
                reason = "Battery is below $LOW_BATTERY_PERCENT%; Patrol will not start camera bursts.",
            )
        }

        if (batteryThermalState.isWarmOrHot) {
            return PatrolStatus(
                enabled = true,
                mode = mode,
                readiness = PatrolReadiness.PausedThermal,
                burstDurationSeconds = burstDuration,
                minimumIntervalSeconds = interval,
                allowsCameraBurst = false,
                reason = "Phone thermal status is ${batteryThermalState.thermalStatus}; Patrol is paused.",
            )
        }

        val darkOpportunityPosture = motionState.posture == DevicePosture.FaceDown ||
            motionState.posture == DevicePosture.Unknown
        val stableEnough = motionState.quality == MotionQuality.Still ||
            motionState.quality == MotionQuality.MostlyStill ||
            motionState.quality == MotionQuality.Unavailable

        if (!stableEnough || !darkOpportunityPosture) {
            return PatrolStatus(
                enabled = true,
                mode = mode,
                readiness = PatrolReadiness.WaitingForDarkStable,
                burstDurationSeconds = burstDuration,
                minimumIntervalSeconds = interval,
                allowsCameraBurst = false,
                reason = "Waiting for face-down or pocket-dark stillness before any camera burst.",
            )
        }

        return PatrolStatus(
            enabled = true,
            mode = mode,
            readiness = PatrolReadiness.ReadyForBurst,
            burstDurationSeconds = burstDuration,
            minimumIntervalSeconds = interval,
            allowsCameraBurst = true,
            reason = if (baselineStale) {
                "Baseline is stale; Patrol may collect short maintenance bursts when dark and stable."
            } else {
                "Dark/stable opportunity detected; Patrol would use a bounded short burst."
            },
        )
    }

    private fun PatrolBatteryMode.burstDurationSeconds(): Int = when (this) {
        PatrolBatteryMode.BatterySaver -> 2
        PatrolBatteryMode.Balanced -> 5
        PatrolBatteryMode.MaxSensitivity -> 10
    }

    private fun PatrolBatteryMode.minimumIntervalSeconds(): Int = when (this) {
        PatrolBatteryMode.BatterySaver -> 300
        PatrolBatteryMode.Balanced -> 180
        PatrolBatteryMode.MaxSensitivity -> 60
    }

    private const val LOW_BATTERY_PERCENT = 20
}
