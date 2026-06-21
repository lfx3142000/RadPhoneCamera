package com.radphonecamera.app.detector

data class ScreeningGuidance(
    val title: String,
    val detail: String,
    val action: String,
) {
    companion object {
        fun forAlarm(alarmState: AlarmState): ScreeningGuidance = when (alarmState) {
            AlarmState.Baseline -> ScreeningGuidance(
                title = "No elevated screening signal",
                detail = "This scan did not rise above the current camera baseline.",
                action = "Continue normal use or repeat a dark scan if conditions change.",
            )

            AlarmState.LowAnomaly -> ScreeningGuidance(
                title = "Recheck recommended",
                detail = "A small increase above this phone's baseline was observed.",
                action = "Keep the phone dark and still, then run another scan before drawing conclusions.",
            )

            AlarmState.Elevated,
            AlarmState.HighElevated -> ScreeningGuidance(
                title = "Elevated screening signal",
                detail = "This camera-only screen observed a larger increase above baseline.",
                action = "Confirm with a calibrated radiation instrument or qualified specialist. Do not use this app as a dosimeter.",
            )

            AlarmState.LimitedSensitivity -> ScreeningGuidance(
                title = "Limited sensitivity",
                detail = "Too few stable dark frames were available for a confident screen.",
                action = "Place the phone face down or in a dark pocket, keep it still, and scan again.",
            )

            AlarmState.Invalid -> ScreeningGuidance(
                title = "No screening result",
                detail = "The scan conditions were not usable for a baseline comparison.",
                action = "Check camera access and darkness, then run a new scan while the phone is still.",
            )
        }
    }
}
