package com.radphonecamera.app.detector

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmEngineTest {
    @Test
    fun invalidDarkQualityProducesInvalidAlarm() {
        val alarm = AlarmEngine.evaluate(
            AlarmInput(darkQuality = DarkQuality.Invalid),
        )

        assertEquals(AlarmState.Invalid, alarm)
    }

    @Test
    fun highTwoSecondScoreProducesHighElevatedAlarm() {
        val alarm = AlarmEngine.evaluate(
            AlarmInput(z2s = 8.5),
        )

        assertEquals(AlarmState.HighElevated, alarm)
    }

    @Test
    fun sustainedSixtySecondScoreProducesLowAnomaly() {
        val alarm = AlarmEngine.evaluate(
            AlarmInput(z60s = 3.2),
        )

        assertEquals(AlarmState.LowAnomaly, alarm)
    }
}
