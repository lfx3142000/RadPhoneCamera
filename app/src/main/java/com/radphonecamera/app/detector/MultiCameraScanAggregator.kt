package com.radphonecamera.app.detector

import com.radphonecamera.app.data.ScanEvent
import kotlin.math.roundToInt

data class MultiCameraScanProgress(
    val cameraIds: List<String>,
    val activeCameraId: String?,
    val completedCameraCount: Int,
    val totalCameraCount: Int,
    val durationMillis: Long,
    val elapsedMillis: Long,
    val remainingMillis: Long,
    val framesAnalyzed: Int,
    val validDarkFrames: Int,
    val candidateEvents: Int,
    val weightedEventsPerMinute: Double,
    val validFrameFraction: Double,
    val baselineZScore: Double,
    val baselineFrameCount: Int,
    val alarmState: AlarmState,
    val error: String? = null,
) {
    val isComplete: Boolean =
        totalCameraCount > 0 && completedCameraCount >= totalCameraCount && remainingMillis <= 0L
}

object MultiCameraScanAggregator {
    fun combine(
        plan: MultiCameraPlan,
        selectedCameraIds: List<String>,
        progressByCamera: List<LiveScanProgress>,
        activeCameraId: String?,
        perCameraDurationMillis: Long,
        error: String? = null,
    ): MultiCameraScanProgress {
        val selected = selectedCameraIds.distinct()
        val selectedWeights = plan.cameraWeights
            .filter { it.cameraId in selected }
            .ifEmpty {
                selected.map { cameraId ->
                    CameraWeight(
                        cameraId = cameraId,
                        score = 1,
                        weight = 1.0 / selected.size.coerceAtLeast(1),
                    )
                }
            }
        val progressById = progressByCamera.associateBy { it.cameraId }
        val observedWeight = selectedWeights
            .filter { progressById.containsKey(it.cameraId) }
            .sumOf { it.weight }
            .coerceAtLeast(MIN_WEIGHT)

        val framesAnalyzed = progressByCamera.sumOf { it.framesAnalyzed }
        val validDarkFrames = progressByCamera.sumOf { it.validDarkFrames }
        val candidateEvents = progressByCamera.sumOf { it.candidateEvents }
        val elapsedMillis = progressByCamera.sumOf {
            it.elapsedMillis.coerceAtMost(perCameraDurationMillis)
        }
        val durationMillis = perCameraDurationMillis * selected.size
        val completedCameraCount = progressByCamera.count {
            it.remainingMillis <= 0L || it.error != null
        }.coerceAtMost(selected.size)
        val weightedEventsPerMinute = selectedWeights.sumOf { weight ->
            (progressById[weight.cameraId]?.eventsPerMinute ?: 0.0) * weight.weight
        } / observedWeight
        val weightedZScore = selectedWeights.sumOf { weight ->
            (progressById[weight.cameraId]?.baselineZScore ?: 0.0) * weight.weight
        } / observedWeight
        val baselineFrameCount = progressByCamera.sumOf { it.baselineFrameCount }
        val validFrameFraction = if (framesAnalyzed == 0) {
            0.0
        } else {
            validDarkFrames.toDouble() / framesAnalyzed
        }
        val aggregateDarkQuality = when {
            framesAnalyzed == 0 -> DarkQuality.Poor
            validFrameFraction < 0.25 -> DarkQuality.Invalid
            validFrameFraction < 0.50 -> DarkQuality.Poor
            else -> DarkQuality.Good
        }
        val alarmState = AlarmEngine.evaluate(
            AlarmInput(
                z30s = weightedZScore,
                darkQuality = aggregateDarkQuality,
                validFrameFraction = validFrameFraction,
            ),
        )

        return MultiCameraScanProgress(
            cameraIds = selected,
            activeCameraId = activeCameraId,
            completedCameraCount = completedCameraCount,
            totalCameraCount = selected.size,
            durationMillis = durationMillis,
            elapsedMillis = elapsedMillis,
            remainingMillis = (durationMillis - elapsedMillis).coerceAtLeast(0L),
            framesAnalyzed = framesAnalyzed,
            validDarkFrames = validDarkFrames,
            candidateEvents = candidateEvents,
            weightedEventsPerMinute = weightedEventsPerMinute,
            validFrameFraction = validFrameFraction,
            baselineZScore = weightedZScore,
            baselineFrameCount = baselineFrameCount,
            alarmState = alarmState,
            error = error,
        )
    }

    private const val MIN_WEIGHT = 0.0001
}

fun MultiCameraScanProgress.toScanEvent(timestampMillis: Long): ScanEvent = ScanEvent(
    timestampMillis = timestampMillis,
    cameraId = "multi:${cameraIds.joinToString("+")}",
    alarmState = alarmState,
    durationMillis = durationMillis,
    framesAnalyzed = framesAnalyzed,
    validDarkFrames = validDarkFrames,
    candidateEvents = candidateEvents,
    eventsPerMinute = weightedEventsPerMinute,
    validFrameFraction = validFrameFraction,
    baselineZScore = baselineZScore,
    baselineFrameCount = baselineFrameCount,
)
