package com.radphonecamera.app.camera

data class FrameSize(
    val width: Int,
    val height: Int,
) {
    val pixelCount: Long = width.toLong() * height.toLong()

    override fun toString(): String = "${width}x$height"
}

enum class DetectorSupportLevel(val label: String) {
    Supported("Supported"),
    Experimental("Experimental"),
    Limited("Limited"),
    NotSupported("Not Supported"),
}

data class CameraDetectorScore(
    val supportLevel: DetectorSupportLevel,
    val score: Int,
    val reasons: List<String>,
)

data class CameraCapability(
    val cameraId: String,
    val lensFacing: String,
    val hardwareLevel: String,
    val supportsManualExposure: Boolean,
    val supportsManualSensitivity: Boolean,
    val supportsFocusLock: Boolean,
    val supportsYuv: Boolean,
    val supportsRaw: Boolean,
    val physicalCameraIds: List<String>,
    val suggestedYuvSize: FrameSize?,
    val availableYuvSizes: List<FrameSize>,
    val detectorScore: CameraDetectorScore,
)

data class DeviceCameraReport(
    val cameras: List<CameraCapability>,
) {
    val usableCameraCount: Int = cameras.count {
        it.detectorScore.supportLevel != DetectorSupportLevel.NotSupported
    }

    val summary: String =
        if (cameras.isEmpty()) {
            "No cameras found"
        } else {
            "$usableCameraCount of ${cameras.size} camera channels usable"
        }
}

