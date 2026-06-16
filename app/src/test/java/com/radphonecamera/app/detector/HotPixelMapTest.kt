package com.radphonecamera.app.detector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HotPixelMapTest {
    @Test
    fun persistentBrightPixelsAreMarkedHot() {
        val frames = listOf(
            byteArrayOf(0, 90, 0, 0),
            byteArrayOf(0, 95, 0, 0),
            byteArrayOf(0, 10, 0, 0),
            byteArrayOf(0, 92, 0, 0),
        )

        val map = HotPixelMap.fromDarkFrames(
            frames = frames,
            width = 2,
            height = 2,
            threshold = 70,
            persistenceFraction = 0.6,
        )

        assertEquals(1, map.size)
        assertTrue(map.isHot(1, 0))
        assertFalse(map.isHot(0, 0))
    }
}

