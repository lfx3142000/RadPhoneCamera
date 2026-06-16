package com.radphonecamera.app.baseline

import com.radphonecamera.app.detector.DarkQuality
import com.radphonecamera.app.detector.BaselineModel

enum class BaselineQuality(val label: String) {
    Good("Good"),
    Fair("Fair"),
    Poor("Poor"),
    Invalid("Invalid"),
}

data class BaselineProgress(
    val totalFrames: Int = 0,
    val goodFrames: Int = 0,
    val fairFrames: Int = 0,
    val poorFrames: Int = 0,
    val invalidFrames: Int = 0,
) {
    val validDarkFrames: Int = goodFrames + fairFrames
    val validFrameFraction: Double =
        if (totalFrames == 0) 0.0 else validDarkFrames.toDouble() / totalFrames

    fun record(quality: DarkQuality?): BaselineProgress = when (quality) {
        DarkQuality.Good -> copy(
            totalFrames = totalFrames + 1,
            goodFrames = goodFrames + 1,
        )

        DarkQuality.Fair -> copy(
            totalFrames = totalFrames + 1,
            fairFrames = fairFrames + 1,
        )

        DarkQuality.Poor -> copy(
            totalFrames = totalFrames + 1,
            poorFrames = poorFrames + 1,
        )

        DarkQuality.Invalid, null -> copy(
            totalFrames = totalFrames + 1,
            invalidFrames = invalidFrames + 1,
        )
    }
}

data class BaselineResult(
    val quality: BaselineQuality,
    val progress: BaselineProgress,
    val message: String,
    val cameraId: String? = null,
    val hotPixelCount: Int = 0,
    val collectedAtMillis: Long = 0L,
    val baselineEventFrameCount: Int = 0,
    val baselineCandidateEvents: Int = 0,
    val baselineMeanEventsPerFrame: Double = 0.0,
    val baselineVarianceEventsPerFrame: Double = 0.0,
) {
    val enablesNormalAlarmMode: Boolean =
        quality == BaselineQuality.Good || quality == BaselineQuality.Fair

    val baselineModel: BaselineModel?
        get() = BaselineModel(
            frameCount = baselineEventFrameCount,
            meanEventsPerFrame = baselineMeanEventsPerFrame,
            varianceEventsPerFrame = baselineVarianceEventsPerFrame,
        ).takeIf { it.hasEnoughData }
}

object BaselineQualityScorer {
    fun score(
        progress: BaselineProgress,
        error: String? = null,
    ): BaselineResult {
        if (error != null && progress.totalFrames == 0) {
            return BaselineResult(
                quality = BaselineQuality.Invalid,
                progress = progress,
                message = error,
            )
        }

        val validFraction = progress.validFrameFraction
        val quality = when {
            progress.totalFrames == 0 -> BaselineQuality.Invalid
            progress.validDarkFrames >= 45 && validFraction >= 0.75 -> BaselineQuality.Good
            progress.validDarkFrames >= 20 && validFraction >= 0.50 -> BaselineQuality.Fair
            progress.validDarkFrames > 0 -> BaselineQuality.Poor
            else -> BaselineQuality.Invalid
        }

        val message = when (quality) {
            BaselineQuality.Good -> "Enough dark, stable frames for normal detector startup."
            BaselineQuality.Fair -> "Usable baseline, but another face-down refresh is recommended."
            BaselineQuality.Poor -> "Some dark frames were found, but sensitivity remains limited."
            BaselineQuality.Invalid -> error ?: "No usable dark-frame baseline was collected."
        }

        return BaselineResult(
            quality = quality,
            progress = progress,
            message = message,
        )
    }
}
