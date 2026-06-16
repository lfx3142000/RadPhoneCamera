package com.radphonecamera.app.detector

import com.radphonecamera.app.camera.CameraCapability
import com.radphonecamera.app.camera.CameraDetectorScore
import com.radphonecamera.app.camera.DetectorSupportLevel

object DeviceQualification {
    fun score(capability: CameraCapability): CameraDetectorScore {
        var score = 0
        val reasons = mutableListOf<String>()

        if (capability.supportsYuv) {
            score += 25
            reasons += "YUV frame access available"
        } else {
            reasons += "YUV frame access missing"
        }

        if (capability.supportsManualExposure) {
            score += 20
            reasons += "Manual exposure available"
        } else {
            reasons += "Manual exposure unavailable"
        }

        if (capability.supportsManualSensitivity) {
            score += 20
            reasons += "Manual ISO/sensitivity available"
        } else {
            reasons += "Manual ISO/sensitivity unavailable"
        }

        if (capability.hardwareLevel == "Full" || capability.hardwareLevel == "Level 3") {
            score += 15
            reasons += "Camera2 ${capability.hardwareLevel} hardware level"
        } else {
            reasons += "Camera2 ${capability.hardwareLevel} hardware level"
        }

        if (capability.supportsRaw) {
            score += 10
            reasons += "RAW sensor output available"
        }

        if (capability.supportsFocusLock) {
            score += 5
            reasons += "Focus lock/control available"
        }

        if (capability.physicalCameraIds.isNotEmpty()) {
            score += 5
            reasons += "${capability.physicalCameraIds.size} physical camera IDs reported"
        }

        val supportLevel = when {
            !capability.supportsYuv -> DetectorSupportLevel.NotSupported
            score >= 80 -> DetectorSupportLevel.Supported
            score >= 55 -> DetectorSupportLevel.Experimental
            else -> DetectorSupportLevel.Limited
        }

        return CameraDetectorScore(
            supportLevel = supportLevel,
            score = score.coerceIn(0, 100),
            reasons = reasons,
        )
    }
}

