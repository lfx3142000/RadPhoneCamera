package com.radphonecamera.app.detector

import org.junit.Assert.assertTrue
import org.junit.Test

class ScreeningGuidanceTest {
    @Test
    fun limitedSensitivityGuidanceCallsForBetterDarkConditions() {
        val guidance = ScreeningGuidance.forAlarm(AlarmState.LimitedSensitivity)

        assertTrue(guidance.title.contains("Limited"))
        assertTrue(guidance.action.contains("dark pocket"))
    }

    @Test
    fun elevatedGuidanceRequiresExternalConfirmationWithoutDoseClaim() {
        val guidance = ScreeningGuidance.forAlarm(AlarmState.Elevated)

        assertTrue(guidance.action.contains("calibrated radiation instrument"))
        assertTrue(guidance.action.contains("not use this app as a dosimeter"))
    }
}
