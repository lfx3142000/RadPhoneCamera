package com.radphonecamera.app.data

import com.radphonecamera.app.detector.AlarmState
import org.junit.Assert.assertEquals
import org.junit.Test

class ScanEventLogCodecTest {
    @Test
    fun scanEventsRoundTripThroughCodec() {
        val events = listOf(
            ScanEvent(
                timestampMillis = 123L,
                cameraId = "camera|rear",
                alarmState = AlarmState.Baseline,
                durationMillis = 30_000L,
                framesAnalyzed = 90,
                validDarkFrames = 80,
                candidateEvents = 3,
                eventsPerMinute = 6.0,
                validFrameFraction = 0.88,
                baselineZScore = 1.25,
                baselineFrameCount = 60,
            ),
        )

        val restored = ScanEventLogCodec.decode(ScanEventLogCodec.encode(events))

        assertEquals(events, restored)
    }

    @Test
    fun invalidRowsAreIgnored() {
        val restored = ScanEventLogCodec.decode("not|valid\n")

        assertEquals(emptyList<ScanEvent>(), restored)
    }
}
