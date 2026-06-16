package com.radphonecamera.app.detector

import com.radphonecamera.app.camera.CameraCapability
import com.radphonecamera.app.camera.CameraDetectorScore
import com.radphonecamera.app.camera.DetectorSupportLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceQualificationTest {
    @Test
    fun strongCameraScoresSupported() {
        val score = DeviceQualification.score(
            capability(
                supportsManualExposure = true,
                supportsManualSensitivity = true,
                hardwareLevel = "Level 3",
                supportsYuv = true,
                supportsRaw = true,
                supportsFocusLock = true,
                physicalCameraIds = listOf("0a", "0b"),
            ),
        )

        assertEquals(DetectorSupportLevel.Supported, score.supportLevel)
        assertEquals(100, score.score)
    }

    @Test
    fun missingYuvIsNotSupported() {
        val score = DeviceQualification.score(
            capability(
                supportsManualExposure = true,
                supportsManualSensitivity = true,
                hardwareLevel = "Full",
                supportsYuv = false,
                supportsRaw = true,
                supportsFocusLock = true,
            ),
        )

        assertEquals(DetectorSupportLevel.NotSupported, score.supportLevel)
    }

    private fun capability(
        supportsManualExposure: Boolean,
        supportsManualSensitivity: Boolean,
        hardwareLevel: String,
        supportsYuv: Boolean,
        supportsRaw: Boolean,
        supportsFocusLock: Boolean,
        physicalCameraIds: List<String> = emptyList(),
    ): CameraCapability = CameraCapability(
        cameraId = "0",
        lensFacing = "Back",
        hardwareLevel = hardwareLevel,
        supportsManualExposure = supportsManualExposure,
        supportsManualSensitivity = supportsManualSensitivity,
        supportsFocusLock = supportsFocusLock,
        supportsYuv = supportsYuv,
        supportsRaw = supportsRaw,
        physicalCameraIds = physicalCameraIds,
        suggestedYuvSize = null,
        availableYuvSizes = emptyList(),
        detectorScore = CameraDetectorScore(
            supportLevel = DetectorSupportLevel.NotSupported,
            score = 0,
            reasons = emptyList(),
        ),
    )
}
