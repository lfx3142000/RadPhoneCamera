package com.radphonecamera.app.detector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BaselineModelTest {
    @Test
    fun rollingBaselineComputesMean() {
        val baseline = (1..30).fold(BaselineModel()) { model, _ -> model.update(2) }

        assertTrue(baseline.hasEnoughData)
        assertEquals(2.0, baseline.meanEventsPerFrame, 0.0001)
        assertEquals(0.0, baseline.varianceEventsPerFrame, 0.0001)
    }

    @Test
    fun zScoreRisesWhenObservedExceedsExpected() {
        val baseline = (1..30).fold(BaselineModel()) { model, _ -> model.update(1) }

        val z = baseline.zScore(observedEvents = 30, observedFrames = 10)

        assertTrue(z > 5.0)
    }
}

