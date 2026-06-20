package com.radphonecamera.app.sensors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionStateClassifierTest {
    @Test
    fun stableFaceDownSamplesAreAllowed() {
        val classifier = MotionStateClassifier()
        val states = (1..8).map {
            classifier.record(MotionSample(x = 0.0f, y = 0.1f, z = -9.7f))
        }

        val state = states.last()

        assertEquals(MotionQuality.Still, state.quality)
        assertEquals(DevicePosture.FaceDown, state.posture)
        assertTrue(state.allowsDetectorFrame)
    }

    @Test
    fun largeAccelerationDeltaBlocksDetectorFrames() {
        val classifier = MotionStateClassifier()
        classifier.record(MotionSample(x = 0.0f, y = 0.0f, z = -9.8f))
        classifier.record(MotionSample(x = 8.0f, y = 1.0f, z = -4.0f))
        classifier.record(MotionSample(x = -8.0f, y = 1.0f, z = -2.0f))
        val state = classifier.record(MotionSample(x = 8.0f, y = -1.0f, z = -3.0f))

        assertEquals(MotionQuality.Moving, state.quality)
        assertFalse(state.allowsDetectorFrame)
    }

    @Test
    fun unavailableMotionDoesNotBlockCaptureOnUnsupportedDevices() {
        assertEquals(MotionQuality.Unavailable, MotionState.Unavailable.quality)
        assertTrue(MotionState.Unavailable.allowsDetectorFrame)
    }
}
