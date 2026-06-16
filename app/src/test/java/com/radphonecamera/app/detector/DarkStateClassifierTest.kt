package com.radphonecamera.app.detector

import org.junit.Assert.assertEquals
import org.junit.Test

class DarkStateClassifierTest {
    @Test
    fun darkStableFrameIsGood() {
        val state = DarkStateClassifier.classify(
            FrameStats(
                width = 4,
                height = 4,
                sampledPixels = 16,
                mean = 8.0,
                variance = 20.0,
                min = 0,
                max = 20,
            ),
        )

        assertEquals(DarkQuality.Good, state.quality)
    }

    @Test
    fun brightFrameIsInvalid() {
        val state = DarkStateClassifier.classify(
            FrameStats(
                width = 4,
                height = 4,
                sampledPixels = 16,
                mean = 150.0,
                variance = 30.0,
                min = 120,
                max = 180,
            ),
        )

        assertEquals(DarkQuality.Invalid, state.quality)
    }
}

