package com.radphonecamera.app.detector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveScanAccumulatorTest {
    @Test
    fun validDarkFrameCountsCandidateEventsPerMinute() {
        val luma = ByteArray(25)
        luma[12] = 120
        val accumulator = LiveScanAccumulator(cameraId = "0")

        accumulator.recordFrame(
            LiveScanFrameInput(
                width = 5,
                height = 5,
                luma = luma,
                darkQuality = DarkQuality.Good,
            ),
        )
        val progress = accumulator.snapshot(
            durationMillis = 30_000L,
            elapsedMillis = 30_000L,
            remainingMillis = 0L,
        )

        assertEquals(1, progress.validDarkFrames)
        assertEquals(1, progress.candidateEvents)
        assertEquals(2.0, progress.eventsPerMinute, 0.001)
        assertEquals(AlarmState.Baseline, progress.alarmState)
    }

    @Test
    fun invalidFramesLimitSensitivity() {
        val accumulator = LiveScanAccumulator(cameraId = "0")

        accumulator.recordFrame(
            LiveScanFrameInput(
                width = 5,
                height = 5,
                luma = ByteArray(25),
                darkQuality = DarkQuality.Invalid,
            ),
        )
        val progress = accumulator.snapshot(
            durationMillis = 30_000L,
            elapsedMillis = 1_000L,
            remainingMillis = 29_000L,
        )

        assertEquals(0, progress.validDarkFrames)
        assertEquals(AlarmState.Invalid, progress.alarmState)
    }

    @Test
    fun hotPixelMapRejectsPersistentBrightPixels() {
        val luma = ByteArray(9)
        luma[4] = 125
        val hotPixelMap = HotPixelMap.fromDarkFrames(
            frames = listOf(luma, luma, luma),
            width = 3,
            height = 3,
            threshold = 85,
        )
        val accumulator = LiveScanAccumulator(
            cameraId = "0",
            hotPixelMap = hotPixelMap,
        )

        accumulator.recordFrame(
            LiveScanFrameInput(
                width = 3,
                height = 3,
                luma = luma,
                darkQuality = DarkQuality.Good,
            ),
        )
        val progress = accumulator.snapshot(
            durationMillis = 30_000L,
            elapsedMillis = 10_000L,
            remainingMillis = 20_000L,
        )

        assertEquals(0, progress.candidateEvents)
        assertEquals(1, progress.rejectedHotPixels)
        assertTrue(progress.hotPixelMaskApplied)
    }
}
