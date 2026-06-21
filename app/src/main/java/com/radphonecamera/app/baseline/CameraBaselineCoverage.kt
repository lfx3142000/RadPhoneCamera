package com.radphonecamera.app.baseline

import com.radphonecamera.app.camera.CameraCapability
import com.radphonecamera.app.detector.MultiCameraWeighting

data class CameraBaselineCoverage(
    val eligibleCameraIds: List<String>,
    val usableBaselineCameraIds: List<String>,
) {
    val missingCameraIds: List<String> = eligibleCameraIds - usableBaselineCameraIds.toSet()
    val coveredCameraCount: Int = usableBaselineCameraIds.size
    val isComplete: Boolean = eligibleCameraIds.isNotEmpty() && missingCameraIds.isEmpty()

    fun hasUsableBaseline(cameraId: String): Boolean = cameraId in usableBaselineCameraIds
}

data class MultiCameraBaselineProgress(
    val cameraIds: List<String> = emptyList(),
    val activeCameraId: String? = null,
    val completedCameraIds: List<String> = emptyList(),
    val failedCameraIds: List<String> = emptyList(),
) {
    val totalCameraCount: Int = cameraIds.size
    val completedCameraCount: Int = completedCameraIds.size
    val isRunning: Boolean = activeCameraId != null
}

object CameraBaselineCoverageCalculator {
    fun evaluate(
        cameras: List<CameraCapability>,
        baselinesByCamera: Map<String, BaselineResult>,
        maximumCameraCount: Int,
    ): CameraBaselineCoverage {
        val eligibleCameraIds = MultiCameraWeighting.plan(cameras)
            .cameraWeights
            .take(maximumCameraCount)
            .map { it.cameraId }
        val usableBaselineCameraIds = eligibleCameraIds.filter { cameraId ->
            baselinesByCamera[cameraId]?.enablesNormalAlarmMode == true
        }
        return CameraBaselineCoverage(
            eligibleCameraIds = eligibleCameraIds,
            usableBaselineCameraIds = usableBaselineCameraIds,
        )
    }
}
