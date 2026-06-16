package com.radphonecamera.app.detector

import org.junit.Assert.assertEquals
import org.junit.Test

class SparseEventDetectorTest {
    @Test
    fun transientCompactClusterIsDetected() {
        val luma = ByteArray(25)
        luma[12] = 120
        luma[13] = 110

        val result = SparseEventDetector(brightPixelThreshold = 85).detect(
            luma = luma,
            width = 5,
            height = 5,
        )

        assertEquals(1, result.candidateEventCount)
        assertEquals(2, result.events.first().size)
        assertEquals(120, result.events.first().peakValue)
    }

    @Test
    fun hotPixelsAreRejected() {
        val luma = ByteArray(9)
        luma[4] = 125
        val hotPixels = HotPixelMap.fromDarkFrames(
            frames = listOf(luma, luma, luma),
            width = 3,
            height = 3,
            threshold = 85,
        )

        val result = SparseEventDetector(brightPixelThreshold = 85).detect(
            luma = luma,
            width = 3,
            height = 3,
            hotPixelMap = hotPixels,
        )

        assertEquals(0, result.candidateEventCount)
        assertEquals(1, result.rejectedHotPixels)
    }
}

