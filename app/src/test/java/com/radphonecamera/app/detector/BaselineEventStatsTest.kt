package com.radphonecamera.app.detector

import com.radphonecamera.app.camera.LumaFrameSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class BaselineEventStatsTest {
    @Test
    fun snapshotsProduceBaselineEventStats() {
        val quiet = LumaFrameSnapshot(
            width = 5,
            height = 5,
            luma = ByteArray(25),
        )
        val active = LumaFrameSnapshot(
            width = 5,
            height = 5,
            luma = ByteArray(25).also { it[12] = 120 },
        )

        val stats = BaselineEventStats.fromSnapshots(
            snapshots = listOf(quiet, active, quiet),
            hotPixelMap = null,
        )

        assertEquals(3, stats.frameCount)
        assertEquals(1, stats.totalCandidateEvents)
        assertEquals(1.0 / 3.0, stats.meanEventsPerFrame, 0.0001)
    }

    @Test
    fun hotPixelMaskIsAppliedBeforeBaselineStats() {
        val luma = ByteArray(9).also { it[4] = 125 }
        val hotPixelMap = HotPixelMap.fromDarkFrames(
            frames = listOf(luma, luma, luma),
            width = 3,
            height = 3,
            threshold = 85,
        )

        val stats = BaselineEventStats.fromSnapshots(
            snapshots = listOf(
                LumaFrameSnapshot(width = 3, height = 3, luma = luma),
                LumaFrameSnapshot(width = 3, height = 3, luma = luma),
            ),
            hotPixelMap = hotPixelMap,
        )

        assertEquals(2, stats.frameCount)
        assertEquals(0, stats.totalCandidateEvents)
    }
}
