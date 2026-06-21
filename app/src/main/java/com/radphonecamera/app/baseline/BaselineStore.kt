package com.radphonecamera.app.baseline

import android.content.Context
import com.radphonecamera.app.detector.HotPixelMap

class BaselineStore(
    context: Context,
) {
    private val prefs = context.getSharedPreferences("baseline_store", Context.MODE_PRIVATE)

    /**
     * Returns the most recently saved camera baseline. Legacy single-camera
     * records are migrated on first read so an app update keeps existing data.
     */
    fun load(): BaselineResult? {
        migrateLegacyBaselineIfNeeded()
        val primaryCameraId = prefs.getString(KEY_PRIMARY_CAMERA_ID, null)
        return primaryCameraId?.let(::load)
            ?: loadAll().values.maxByOrNull { it.collectedAtMillis }
    }

    fun load(cameraId: String): BaselineResult? {
        migrateLegacyBaselineIfNeeded()
        if (cameraId !in savedCameraIds()) return null
        return loadCameraBaseline(cameraId)
    }

    fun loadAll(): Map<String, BaselineResult> {
        migrateLegacyBaselineIfNeeded()
        return savedCameraIds().mapNotNull { cameraId ->
            loadCameraBaseline(cameraId)?.let { cameraId to it }
        }.toMap()
    }

    fun loadHotPixelMap(cameraId: String): HotPixelMap? {
        migrateLegacyBaselineIfNeeded()
        if (cameraId !in savedCameraIds()) return null
        val prefix = cameraPrefix(cameraId)
        val width = prefs.getInt("${prefix}${KEY_HOT_PIXEL_WIDTH}", 0)
        val height = prefs.getInt("${prefix}${KEY_HOT_PIXEL_HEIGHT}", 0)
        if (width <= 0 || height <= 0) return null
        return HotPixelMap.fromPackedString(
            width = width,
            height = height,
            packedHotPixels = prefs.getString("${prefix}${KEY_HOT_PIXEL_PACKED}", null).orEmpty(),
        )
    }

    fun save(
        result: BaselineResult,
        hotPixelMap: HotPixelMap? = null,
    ) {
        val cameraId = result.cameraId ?: return
        val prefix = cameraPrefix(cameraId)
        val cameraIds = savedCameraIds() + cameraId
        prefs.edit()
            .putStringSet(KEY_CAMERA_IDS, cameraIds)
            .putString(KEY_PRIMARY_CAMERA_ID, cameraId)
            .putString("${prefix}${KEY_QUALITY}", result.quality.name)
            .putString("${prefix}${KEY_MESSAGE}", result.message)
            .putInt("${prefix}${KEY_TOTAL_FRAMES}", result.progress.totalFrames)
            .putInt("${prefix}${KEY_GOOD_FRAMES}", result.progress.goodFrames)
            .putInt("${prefix}${KEY_FAIR_FRAMES}", result.progress.fairFrames)
            .putInt("${prefix}${KEY_POOR_FRAMES}", result.progress.poorFrames)
            .putInt("${prefix}${KEY_INVALID_FRAMES}", result.progress.invalidFrames)
            .putInt("${prefix}${KEY_HOT_PIXEL_COUNT}", result.hotPixelCount)
            .putLong("${prefix}${KEY_COLLECTED_AT}", result.collectedAtMillis)
            .putInt("${prefix}${KEY_BASELINE_EVENT_FRAME_COUNT}", result.baselineEventFrameCount)
            .putInt("${prefix}${KEY_BASELINE_CANDIDATE_EVENTS}", result.baselineCandidateEvents)
            .putDouble("${prefix}${KEY_BASELINE_EVENT_MEAN}", result.baselineMeanEventsPerFrame)
            .putDouble("${prefix}${KEY_BASELINE_EVENT_VARIANCE}", result.baselineVarianceEventsPerFrame)
            .writeHotPixelMap(prefix, hotPixelMap)
            .apply()
    }

    private fun loadCameraBaseline(cameraId: String): BaselineResult? {
        val prefix = cameraPrefix(cameraId)
        val qualityName = prefs.getString("${prefix}${KEY_QUALITY}", null) ?: return null
        val quality = runCatching { BaselineQuality.valueOf(qualityName) }.getOrNull() ?: return null
        val progress = BaselineProgress(
            totalFrames = prefs.getInt("${prefix}${KEY_TOTAL_FRAMES}", 0),
            goodFrames = prefs.getInt("${prefix}${KEY_GOOD_FRAMES}", 0),
            fairFrames = prefs.getInt("${prefix}${KEY_FAIR_FRAMES}", 0),
            poorFrames = prefs.getInt("${prefix}${KEY_POOR_FRAMES}", 0),
            invalidFrames = prefs.getInt("${prefix}${KEY_INVALID_FRAMES}", 0),
        )
        return BaselineResult(
            quality = quality,
            progress = progress,
            message = prefs.getString("${prefix}${KEY_MESSAGE}", null) ?: quality.defaultMessage(),
            cameraId = cameraId,
            hotPixelCount = prefs.getInt("${prefix}${KEY_HOT_PIXEL_COUNT}", 0),
            collectedAtMillis = prefs.getLong("${prefix}${KEY_COLLECTED_AT}", 0L),
            baselineEventFrameCount = prefs.getInt("${prefix}${KEY_BASELINE_EVENT_FRAME_COUNT}", 0),
            baselineCandidateEvents = prefs.getInt("${prefix}${KEY_BASELINE_CANDIDATE_EVENTS}", 0),
            baselineMeanEventsPerFrame = prefs.getDouble("${prefix}${KEY_BASELINE_EVENT_MEAN}"),
            baselineVarianceEventsPerFrame = prefs.getDouble("${prefix}${KEY_BASELINE_EVENT_VARIANCE}"),
        )
    }

    private fun migrateLegacyBaselineIfNeeded() {
        if (savedCameraIds().isNotEmpty()) return
        val legacyResult = loadLegacyBaseline() ?: return
        val cameraId = legacyResult.cameraId ?: return
        save(legacyResult, loadLegacyHotPixelMap(cameraId))
    }

    private fun loadLegacyBaseline(): BaselineResult? {
        val qualityName = prefs.getString(KEY_QUALITY, null) ?: return null
        val quality = runCatching { BaselineQuality.valueOf(qualityName) }.getOrNull() ?: return null
        return BaselineResult(
            quality = quality,
            progress = BaselineProgress(
                totalFrames = prefs.getInt(KEY_TOTAL_FRAMES, 0),
                goodFrames = prefs.getInt(KEY_GOOD_FRAMES, 0),
                fairFrames = prefs.getInt(KEY_FAIR_FRAMES, 0),
                poorFrames = prefs.getInt(KEY_POOR_FRAMES, 0),
                invalidFrames = prefs.getInt(KEY_INVALID_FRAMES, 0),
            ),
            message = prefs.getString(KEY_MESSAGE, null) ?: quality.defaultMessage(),
            cameraId = prefs.getString(KEY_CAMERA_ID, null),
            hotPixelCount = prefs.getInt(KEY_HOT_PIXEL_COUNT, 0),
            collectedAtMillis = prefs.getLong(KEY_COLLECTED_AT, 0L),
            baselineEventFrameCount = prefs.getInt(KEY_BASELINE_EVENT_FRAME_COUNT, 0),
            baselineCandidateEvents = prefs.getInt(KEY_BASELINE_CANDIDATE_EVENTS, 0),
            baselineMeanEventsPerFrame = prefs.getDouble(KEY_BASELINE_EVENT_MEAN),
            baselineVarianceEventsPerFrame = prefs.getDouble(KEY_BASELINE_EVENT_VARIANCE),
        )
    }

    private fun loadLegacyHotPixelMap(cameraId: String): HotPixelMap? {
        if (prefs.getString(KEY_HOT_PIXEL_CAMERA_ID, null) != cameraId) return null
        val width = prefs.getInt(KEY_HOT_PIXEL_WIDTH, 0)
        val height = prefs.getInt(KEY_HOT_PIXEL_HEIGHT, 0)
        if (width <= 0 || height <= 0) return null
        return HotPixelMap.fromPackedString(
            width = width,
            height = height,
            packedHotPixels = prefs.getString(KEY_HOT_PIXEL_PACKED, null).orEmpty(),
        )
    }

    private fun savedCameraIds(): Set<String> =
        prefs.getStringSet(KEY_CAMERA_IDS, emptySet()).orEmpty().toSet()

    private fun cameraPrefix(cameraId: String): String = "camera.$cameraId."

    private fun android.content.SharedPreferences.Editor.writeHotPixelMap(
        prefix: String,
        hotPixelMap: HotPixelMap?,
    ): android.content.SharedPreferences.Editor {
        if (hotPixelMap == null) {
            remove("${prefix}${KEY_HOT_PIXEL_WIDTH}")
            remove("${prefix}${KEY_HOT_PIXEL_HEIGHT}")
            remove("${prefix}${KEY_HOT_PIXEL_PACKED}")
            return this
        }

        putInt("${prefix}${KEY_HOT_PIXEL_WIDTH}", hotPixelMap.width)
        putInt("${prefix}${KEY_HOT_PIXEL_HEIGHT}", hotPixelMap.height)
        putString("${prefix}${KEY_HOT_PIXEL_PACKED}", hotPixelMap.toPackedString(MAX_STORED_HOT_PIXELS))
        return this
    }

    private fun android.content.SharedPreferences.getDouble(key: String): Double =
        java.lang.Double.longBitsToDouble(
            getLong(key, java.lang.Double.doubleToRawLongBits(0.0)),
        )

    private fun android.content.SharedPreferences.Editor.putDouble(
        key: String,
        value: Double,
    ): android.content.SharedPreferences.Editor =
        putLong(key, java.lang.Double.doubleToRawLongBits(value))

    private fun BaselineQuality.defaultMessage(): String = when (this) {
        BaselineQuality.Good -> "Enough dark, stable frames for normal detector startup."
        BaselineQuality.Fair -> "Usable baseline, but another face-down refresh is recommended."
        BaselineQuality.Poor -> "Some dark frames were found, but sensitivity remains limited."
        BaselineQuality.Invalid -> "No usable dark-frame baseline was collected."
    }

    private companion object {
        private const val KEY_CAMERA_IDS = "camera_ids"
        private const val KEY_PRIMARY_CAMERA_ID = "primary_camera_id"
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
        private const val KEY_HOT_PIXEL_CAMERA_ID = "hot_pixel_camera_id"
        private const val KEY_HOT_PIXEL_WIDTH = "hot_pixel_width"
        private const val KEY_HOT_PIXEL_HEIGHT = "hot_pixel_height"
        private const val KEY_HOT_PIXEL_PACKED = "hot_pixel_packed"
        private const val KEY_BASELINE_EVENT_FRAME_COUNT = "baseline_event_frame_count"
        private const val KEY_BASELINE_CANDIDATE_EVENTS = "baseline_candidate_events"
        private const val KEY_BASELINE_EVENT_MEAN = "baseline_event_mean"
        private const val KEY_BASELINE_EVENT_VARIANCE = "baseline_event_variance"
        private const val MAX_STORED_HOT_PIXELS = 5_000
    }
}
