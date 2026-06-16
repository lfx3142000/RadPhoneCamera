package com.radphonecamera.app.detector

enum class DarkQuality(val label: String) {
    Good("Good dark frame"),
    Fair("Fair dark frame"),
    Poor("Poor dark frame"),
    Invalid("Invalid frame"),
}

data class DarkState(
    val quality: DarkQuality,
    val reason: String,
)

object DarkStateClassifier {
    fun classify(stats: FrameStats): DarkState = when {
        stats.sampledPixels <= 0 -> DarkState(
            quality = DarkQuality.Invalid,
            reason = "No luma samples were available.",
        )

        stats.mean <= 18.0 && stats.variance <= 180.0 -> DarkState(
            quality = DarkQuality.Good,
            reason = "Dark, stable luma distribution.",
        )

        stats.mean <= 40.0 && stats.variance <= 600.0 -> DarkState(
            quality = DarkQuality.Fair,
            reason = "Usable dark data, but more stable darkness is preferred.",
        )

        stats.mean <= 70.0 -> DarkState(
            quality = DarkQuality.Poor,
            reason = "Frame is dim but not dark enough for normal sensitivity.",
        )

        else -> DarkState(
            quality = DarkQuality.Invalid,
            reason = "Frame is too bright for detector baseline work.",
        )
    }
}

