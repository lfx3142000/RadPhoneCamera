package com.radphonecamera.app.baseline

import com.radphonecamera.app.detector.DarkQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BaselineQualityScorerTest {
    @Test
    fun manyValidDarkFramesScoreGood() {
        val progress = (1..60).fold(BaselineProgress()) { current, _ ->
            current.record(DarkQuality.Good)
        }

        val result = BaselineQualityScorer.score(progress)

        assertEquals(BaselineQuality.Good, result.quality)
        assertTrue(result.enablesNormalAlarmMode)
    }

    @Test
    fun someValidFramesScoreFair() {
        val valid = (1..25).fold(BaselineProgress()) { current, _ ->
            current.record(DarkQuality.Fair)
        }
        val mixed = (1..15).fold(valid) { current, _ ->
            current.record(DarkQuality.Invalid)
        }

        val result = BaselineQualityScorer.score(mixed)

        assertEquals(BaselineQuality.Fair, result.quality)
        assertTrue(result.enablesNormalAlarmMode)
    }

    @Test
    fun noValidFramesScoreInvalid() {
        val progress = (1..20).fold(BaselineProgress()) { current, _ ->
            current.record(DarkQuality.Invalid)
        }

        val result = BaselineQualityScorer.score(progress)

        assertEquals(BaselineQuality.Invalid, result.quality)
        assertFalse(result.enablesNormalAlarmMode)
    }
}
