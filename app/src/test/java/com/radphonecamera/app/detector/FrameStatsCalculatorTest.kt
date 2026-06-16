package com.radphonecamera.app.detector

import org.junit.Assert.assertEquals
import org.junit.Test

class FrameStatsCalculatorTest {
    @Test
    fun uniformFrameHasExpectedMeanAndZeroVariance() {
        val stats = FrameStatsCalculator.fromBytes(
            luma = ByteArray(16) { 12 },
            width = 4,
            height = 4,
        )

        assertEquals(16, stats.sampledPixels)
        assertEquals(12.0, stats.mean, 0.0001)
        assertEquals(0.0, stats.variance, 0.0001)
        assertEquals(12, stats.min)
        assertEquals(12, stats.max)
    }

    @Test
    fun variableFrameUsesSampleVariance() {
        val stats = FrameStatsCalculator.fromBytes(
            luma = byteArrayOf(0, 10, 20, 30),
            width = 2,
            height = 2,
        )

        assertEquals(4, stats.sampledPixels)
        assertEquals(15.0, stats.mean, 0.0001)
        assertEquals(166.6666, stats.variance, 0.001)
        assertEquals(0, stats.min)
        assertEquals(30, stats.max)
    }

    @Test
    fun rowStrideAndPixelStrideAreRespected() {
        val stats = FrameStatsCalculator.fromBytes(
            luma = byteArrayOf(
                1, 99, 2, 99,
                3, 99, 4, 99,
            ),
            width = 2,
            height = 2,
            rowStride = 4,
            pixelStride = 2,
        )

        assertEquals(4, stats.sampledPixels)
        assertEquals(2.5, stats.mean, 0.0001)
        assertEquals(1, stats.min)
        assertEquals(4, stats.max)
    }
}

