package com.radphonecamera.app.detector

import com.radphonecamera.app.camera.DetectorSupportLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class MultiCameraScanAggregatorTest {
    @Test
    fun combinesSequentialCameraProgressWithWeights() {
        val plan = MultiCameraPlan(
            cameraWeights = listOf(
                CameraWeight(cameraId = "0", score = 80, weight = 0.75),
                CameraWeight(cameraId = "1", score = 40, weight = 0.25),
            ),
            combinedScore = 85,
            supportLevel = DetectorSupportLevel.Supported,
        )
        val progress = MultiCameraScanAggregator.combine(
            plan = plan,
            selectedCameraIds = listOf("0", "1"),
            progressByCamera = listOf(
                scanProgress(cameraId = "0", eventsPerMinuteInput = 12.0, zScore = 2.0),
                scanProgress(cameraId = "1", eventsPerMinuteInput = 4.0, zScore = 6.0),
            ),
            activeCameraId = null,
            perCameraDurationMillis = 15_000L,
        )

        assertEquals(2, progress.completedCameraCount)
        assertEquals(10.0, progress.weightedEventsPerMinute, 0.001)
        assertEquals(3.0, progress.baselineZScore, 0.001)
        assertEquals(AlarmState.Baseline, progress.alarmState)
    }

    @Test
    fun aggregateCanRaiseAlarmFromWeightedZScore() {
        val plan = MultiCameraPlan(
            cameraWeights = listOf(
                CameraWeight(cameraId = "0", score = 80, weight = 0.50),
                CameraWeight(cameraId = "1", score = 80, weight = 0.50),
            ),
            combinedScore = 85,
            supportLevel = DetectorSupportLevel.Supported,
        )
        val progress = MultiCameraScanAggregator.combine(
            plan = plan,
            selectedCameraIds = listOf("0", "1"),
            progressByCamera = listOf(
                scanProgress(cameraId = "0", eventsPerMinuteInput = 20.0, zScore = 6.0),
                scanProgress(cameraId = "1", eventsPerMinuteInput = 16.0, zScore = 5.0),
            ),
            activeCameraId = null,
            perCameraDurationMillis = 15_000L,
        )

        assertEquals(AlarmState.Elevated, progress.alarmState)
        assertTrue(progress.isComplete)
    }

    private fun scanProgress(
        cameraId: String,
        eventsPerMinuteInput: Double,
        zScore: Double,
    ): LiveScanProgress {
        val elapsedMillis = 15_000L
        val candidateEvents = (eventsPerMinuteInput / 4.0).roundToInt()
        return LiveScanProgress(
            cameraId = cameraId,
            durationMillis = elapsedMillis,
            elapsedMillis = elapsedMillis,
            remainingMillis = 0L,
            framesAnalyzed = 20,
            validDarkFrames = 20,
            candidateEvents = candidateEvents,
            rejectedHotPixels = 0,
            rejectedArtifacts = 0,
            latestDarkQuality = DarkQuality.Good,
            alarmState = AlarmState.Baseline,
            hotPixelMaskApplied = false,
            baselineFrameCount = 30,
            baselineZScore = zScore,
        )
    }
}
