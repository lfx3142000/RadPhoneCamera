package com.radphonecamera.app.detector

data class LiveScanFrameInput(
    val width: Int,
    val height: Int,
    val luma: ByteArray,
    val darkQuality: DarkQuality?,
)

data class LiveScanProgress(
    val cameraId: String,
    val durationMillis: Long,
    val elapsedMillis: Long,
    val remainingMillis: Long,
    val framesAnalyzed: Int,
    val validDarkFrames: Int,
    val candidateEvents: Int,
    val rejectedHotPixels: Int,
    val rejectedArtifacts: Int,
    val latestDarkQuality: DarkQuality?,
    val alarmState: AlarmState,
    val hotPixelMaskApplied: Boolean,
    val baselineFrameCount: Int,
    val baselineZScore: Double,
    val error: String? = null,
) {
    val validFrameFraction: Double =
        if (framesAnalyzed == 0) 0.0 else validDarkFrames.toDouble() / framesAnalyzed

    val eventsPerMinute: Double =
        candidateEvents / (elapsedMillis.coerceAtLeast(1L) / MILLIS_PER_MINUTE.toDouble())

    companion object {
        private const val MILLIS_PER_MINUTE = 60_000L
    }
}

class LiveScanAccumulator(
    private val cameraId: String,
    private val hotPixelMap: HotPixelMap? = null,
    private val baselineModel: BaselineModel? = null,
    private val detector: SparseEventDetector = SparseEventDetector(),
) {
    private var framesAnalyzed = 0
    private var validDarkFrames = 0
    private var candidateEvents = 0
    private var rejectedHotPixels = 0
    private var rejectedArtifacts = 0
    private var latestDarkQuality: DarkQuality? = null
    private var hotPixelMaskApplied = false

    fun recordFrame(input: LiveScanFrameInput) {
        framesAnalyzed += 1
        latestDarkQuality = input.darkQuality

        if (input.darkQuality != DarkQuality.Good && input.darkQuality != DarkQuality.Fair) {
            return
        }

        validDarkFrames += 1
        val matchingHotPixelMap = hotPixelMap
            ?.takeIf { it.width == input.width && it.height == input.height }
        val activeHotPixelMap = matchingHotPixelMap ?: HotPixelMap.empty(input.width, input.height)
        hotPixelMaskApplied = hotPixelMaskApplied || matchingHotPixelMap != null

        val detection = detector.detect(
            luma = input.luma,
            width = input.width,
            height = input.height,
            hotPixelMap = activeHotPixelMap,
        )
        candidateEvents += detection.candidateEventCount
        rejectedHotPixels += detection.rejectedHotPixels
        rejectedArtifacts += detection.rejectedArtifacts
    }

    fun snapshot(
        durationMillis: Long,
        elapsedMillis: Long,
        remainingMillis: Long,
        error: String? = null,
    ): LiveScanProgress {
        val baselineZScore = if (validDarkFrames >= MIN_Z_SCORE_FRAMES) {
            baselineModel?.zScore(
                observedEvents = candidateEvents,
                observedFrames = validDarkFrames,
            ) ?: 0.0
        } else {
            0.0
        }
        val aggregateDarkQuality = when {
            framesAnalyzed == 0 -> DarkQuality.Poor
            validFrameFraction < 0.25 -> DarkQuality.Invalid
            validFrameFraction < 0.50 -> DarkQuality.Poor
            else -> DarkQuality.Good
        }
        val alarmState = AlarmEngine.evaluate(
            AlarmInput(
                z30s = baselineZScore,
                darkQuality = aggregateDarkQuality,
                validFrameFraction = validFrameFraction,
            ),
        )

        return LiveScanProgress(
            cameraId = cameraId,
            durationMillis = durationMillis,
            elapsedMillis = elapsedMillis,
            remainingMillis = remainingMillis,
            framesAnalyzed = framesAnalyzed,
            validDarkFrames = validDarkFrames,
            candidateEvents = candidateEvents,
            rejectedHotPixels = rejectedHotPixels,
            rejectedArtifacts = rejectedArtifacts,
            latestDarkQuality = latestDarkQuality,
            alarmState = alarmState,
            hotPixelMaskApplied = hotPixelMaskApplied,
            baselineFrameCount = baselineModel?.frameCount ?: 0,
            baselineZScore = baselineZScore,
            error = error,
        )
    }

    private val validFrameFraction: Double
        get() = if (framesAnalyzed == 0) 0.0 else validDarkFrames.toDouble() / framesAnalyzed

    private companion object {
        const val MIN_Z_SCORE_FRAMES = 10
    }
}
