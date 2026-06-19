package com.radphonecamera.app.detector

import com.radphonecamera.app.camera.CameraCapability
import com.radphonecamera.app.camera.DetectorSupportLevel
import kotlin.math.roundToInt

data class CameraWeight(
    val cameraId: String,
    val score: Int,
    val weight: Double,
)

data class MultiCameraPlan(
    val cameraWeights: List<CameraWeight>,
    val combinedScore: Int,
    val supportLevel: DetectorSupportLevel,
) {
    val activeCameraCount: Int = cameraWeights.size
}

object MultiCameraWeighting {
    fun plan(cameras: List<CameraCapability>): MultiCameraPlan {
        val eligible = cameras
            .filter { camera ->
                camera.supportsYuv &&
                    camera.detectorScore.supportLevel != DetectorSupportLevel.NotSupported &&
                    camera.detectorScore.score > 0
            }
            .sortedByDescending { it.detectorScore.score }

        if (eligible.isEmpty()) {
            return MultiCameraPlan(
                cameraWeights = emptyList(),
                combinedScore = 0,
                supportLevel = DetectorSupportLevel.NotSupported,
            )
        }

        val totalRawWeight = eligible.sumOf { it.detectorScore.score.coerceAtLeast(1) }
        val weights = eligible.map { camera ->
            CameraWeight(
                cameraId = camera.cameraId,
                score = camera.detectorScore.score,
                weight = camera.detectorScore.score.coerceAtLeast(1).toDouble() / totalRawWeight,
            )
        }
        val weightedScore = weights.sumOf { it.score * it.weight }
        val multiCameraBonus = when {
            weights.size >= 3 -> 10
            weights.size == 2 -> 5
            else -> 0
        }
        val combinedScore = (weightedScore + multiCameraBonus)
            .roundToInt()
            .coerceIn(0, 100)
        val supportLevel = when {
            combinedScore >= 80 -> DetectorSupportLevel.Supported
            combinedScore >= 55 -> DetectorSupportLevel.Experimental
            else -> DetectorSupportLevel.Limited
        }

        return MultiCameraPlan(
            cameraWeights = weights,
            combinedScore = combinedScore,
            supportLevel = supportLevel,
        )
    }
}
