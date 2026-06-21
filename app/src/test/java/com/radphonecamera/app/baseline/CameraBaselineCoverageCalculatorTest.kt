package com.radphonecamera.app.baseline

import com.radphonecamera.app.camera.CameraCapability
import com.radphonecamera.app.camera.CameraDetectorScore
import com.radphonecamera.app.camera.DetectorSupportLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraBaselineCoverageCalculatorTest {
    @Test
    fun requiresAUsableBaselineForEachSelectedCamera() {
        val coverage = CameraBaselineCoverageCalculator.evaluate(
            cameras = listOf(
                capability("0", score = 90),
                capability("1", score = 70),
                capability("2", score = 50),
            ),
            baselinesByCamera = mapOf(
                "0" to usableBaseline("0"),
                "2" to unusableBaseline("2"),
            ),
            maximumCameraCount = 2,
        )

        assertEquals(listOf("0", "1"), coverage.eligibleCameraIds)
        assertEquals(listOf("0"), coverage.usableBaselineCameraIds)
        assertEquals(listOf("1"), coverage.missingCameraIds)
        assertFalse(coverage.isComplete)
    }

    @Test
    fun marksCoverageCompleteWhenEverySelectedCameraIsUsable() {
        val coverage = CameraBaselineCoverageCalculator.evaluate(
            cameras = listOf(capability("0", score = 90), capability("1", score = 70)),
            baselinesByCamera = mapOf("0" to usableBaseline("0"), "1" to usableBaseline("1")),
            maximumCameraCount = 3,
        )

        assertTrue(coverage.isComplete)
        assertEquals(2, coverage.coveredCameraCount)
    }

    private fun usableBaseline(cameraId: String): BaselineResult = BaselineResult(
        quality = BaselineQuality.Good,
        progress = BaselineProgress(totalFrames = 60, goodFrames = 60),
        message = "Ready",
        cameraId = cameraId,
    )

    private fun unusableBaseline(cameraId: String): BaselineResult = BaselineResult(
        quality = BaselineQuality.Poor,
        progress = BaselineProgress(totalFrames = 20, poorFrames = 20),
        message = "Retry",
        cameraId = cameraId,
    )

    private fun capability(cameraId: String, score: Int): CameraCapability = CameraCapability(
        cameraId = cameraId,
        lensFacing = "Back",
        hardwareLevel = "Full",
        supportsManualExposure = true,
        supportsManualSensitivity = true,
        supportsFocusLock = true,
        supportsYuv = true,
        supportsRaw = false,
        physicalCameraIds = emptyList(),
        suggestedYuvSize = null,
        availableYuvSizes = emptyList(),
        detectorScore = CameraDetectorScore(
            supportLevel = DetectorSupportLevel.Supported,
            score = score,
            reasons = emptyList(),
        ),
    )
}
