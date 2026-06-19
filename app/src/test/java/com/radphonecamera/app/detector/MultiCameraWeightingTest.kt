package com.radphonecamera.app.detector

import com.radphonecamera.app.camera.CameraCapability
import com.radphonecamera.app.camera.CameraDetectorScore
import com.radphonecamera.app.camera.DetectorSupportLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiCameraWeightingTest {
    @Test
    fun unsupportedCamerasAreExcluded() {
        val plan = MultiCameraWeighting.plan(
            listOf(
                capability("0", score = 80, supportLevel = DetectorSupportLevel.Supported),
                capability(
                    "1",
                    score = 90,
                    supportLevel = DetectorSupportLevel.NotSupported,
                    supportsYuv = false,
                ),
            ),
        )

        assertEquals(1, plan.activeCameraCount)
        assertEquals("0", plan.cameraWeights.single().cameraId)
    }

    @Test
    fun weightsAreNormalizedByDetectorScore() {
        val plan = MultiCameraWeighting.plan(
            listOf(
                capability("0", score = 90, supportLevel = DetectorSupportLevel.Supported),
                capability("1", score = 60, supportLevel = DetectorSupportLevel.Experimental),
            ),
        )

        assertEquals(2, plan.activeCameraCount)
        assertEquals(1.0, plan.cameraWeights.sumOf { it.weight }, 0.0001)
        assertTrue(plan.cameraWeights.first { it.cameraId == "0" }.weight > 0.5)
        assertTrue(plan.combinedScore >= 80)
    }

    @Test
    fun noEligibleCamerasProducesNotSupportedPlan() {
        val plan = MultiCameraWeighting.plan(
            listOf(
                capability(
                    "0",
                    score = 0,
                    supportLevel = DetectorSupportLevel.NotSupported,
                    supportsYuv = false,
                ),
            ),
        )

        assertEquals(0, plan.activeCameraCount)
        assertEquals(DetectorSupportLevel.NotSupported, plan.supportLevel)
    }

    private fun capability(
        cameraId: String,
        score: Int,
        supportLevel: DetectorSupportLevel,
        supportsYuv: Boolean = true,
    ): CameraCapability = CameraCapability(
        cameraId = cameraId,
        lensFacing = "Back",
        hardwareLevel = "Full",
        supportsManualExposure = true,
        supportsManualSensitivity = true,
        supportsFocusLock = true,
        supportsYuv = supportsYuv,
        supportsRaw = false,
        physicalCameraIds = emptyList(),
        suggestedYuvSize = null,
        availableYuvSizes = emptyList(),
        detectorScore = CameraDetectorScore(
            supportLevel = supportLevel,
            score = score,
            reasons = emptyList(),
        ),
    )
}
