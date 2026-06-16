package com.radphonecamera.app.baseline

import android.content.Context

class BaselineStore(
    context: Context,
) {
    private val prefs = context.getSharedPreferences("baseline_store", Context.MODE_PRIVATE)

    fun load(): BaselineResult? {
        val qualityName = prefs.getString(KEY_QUALITY, null) ?: return null
        val quality = runCatching { BaselineQuality.valueOf(qualityName) }.getOrNull() ?: return null
        val totalFrames = prefs.getInt(KEY_TOTAL_FRAMES, 0)
        val progress = BaselineProgress(
            totalFrames = totalFrames,
            goodFrames = prefs.getInt(KEY_GOOD_FRAMES, 0),
            fairFrames = prefs.getInt(KEY_FAIR_FRAMES, 0),
            poorFrames = prefs.getInt(KEY_POOR_FRAMES, 0),
            invalidFrames = prefs.getInt(KEY_INVALID_FRAMES, 0),
        )

        return BaselineResult(
            quality = quality,
            progress = progress,
            message = prefs.getString(KEY_MESSAGE, null) ?: quality.defaultMessage(),
            cameraId = prefs.getString(KEY_CAMERA_ID, null),
            hotPixelCount = prefs.getInt(KEY_HOT_PIXEL_COUNT, 0),
            collectedAtMillis = prefs.getLong(KEY_COLLECTED_AT, 0L),
        )
    }

    fun save(result: BaselineResult) {
        prefs.edit()
            .putString(KEY_QUALITY, result.quality.name)
            .putString(KEY_MESSAGE, result.message)
            .putString(KEY_CAMERA_ID, result.cameraId)
            .putInt(KEY_TOTAL_FRAMES, result.progress.totalFrames)
            .putInt(KEY_GOOD_FRAMES, result.progress.goodFrames)
            .putInt(KEY_FAIR_FRAMES, result.progress.fairFrames)
            .putInt(KEY_POOR_FRAMES, result.progress.poorFrames)
            .putInt(KEY_INVALID_FRAMES, result.progress.invalidFrames)
            .putInt(KEY_HOT_PIXEL_COUNT, result.hotPixelCount)
            .putLong(KEY_COLLECTED_AT, result.collectedAtMillis)
            .apply()
    }

    private fun BaselineQuality.defaultMessage(): String = when (this) {
        BaselineQuality.Good -> "Enough dark, stable frames for normal detector startup."
        BaselineQuality.Fair -> "Usable baseline, but another face-down refresh is recommended."
        BaselineQuality.Poor -> "Some dark frames were found, but sensitivity remains limited."
        BaselineQuality.Invalid -> "No usable dark-frame baseline was collected."
    }

    companion object {
        private const val KEY_QUALITY = "quality"
        private const val KEY_MESSAGE = "message"
        private const val KEY_CAMERA_ID = "camera_id"
        private const val KEY_TOTAL_FRAMES = "total_frames"
        private const val KEY_GOOD_FRAMES = "good_frames"
        private const val KEY_FAIR_FRAMES = "fair_frames"
        private const val KEY_POOR_FRAMES = "poor_frames"
        private const val KEY_INVALID_FRAMES = "invalid_frames"
        private const val KEY_HOT_PIXEL_COUNT = "hot_pixel_count"
        private const val KEY_COLLECTED_AT = "collected_at"
    }
}
