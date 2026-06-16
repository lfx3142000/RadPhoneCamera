package com.radphonecamera.app.detector

enum class AlarmState(val label: String) {
    Baseline("Baseline"),
    LowAnomaly("Low Anomaly"),
    Elevated("Elevated"),
    HighElevated("High Elevated"),
    LimitedSensitivity("Limited Sensitivity"),
    Invalid("Invalid"),
}

data class AlarmInput(
    val z2s: Double = 0.0,
    val z10s: Double = 0.0,
    val z30s: Double = 0.0,
    val z60s: Double = 0.0,
    val darkQuality: DarkQuality = DarkQuality.Good,
    val validFrameFraction: Double = 1.0,
)

object AlarmEngine {
    fun evaluate(input: AlarmInput): AlarmState = when {
        input.darkQuality == DarkQuality.Invalid -> AlarmState.Invalid
        input.darkQuality == DarkQuality.Poor || input.validFrameFraction < 0.5 ->
            AlarmState.LimitedSensitivity

        input.z2s >= 8.0 -> AlarmState.HighElevated
        input.z10s >= 5.0 || input.z30s >= 5.0 -> AlarmState.Elevated
        input.z60s >= 3.0 -> AlarmState.LowAnomaly
        else -> AlarmState.Baseline
    }
}

