package com.radphonecamera.app.data

import com.radphonecamera.app.detector.AlarmState
import com.radphonecamera.app.detector.LiveScanProgress
import java.nio.charset.StandardCharsets
import java.util.Base64

data class ScanEvent(
    val timestampMillis: Long,
    val cameraId: String,
    val alarmState: AlarmState,
    val durationMillis: Long,
    val framesAnalyzed: Int,
    val validDarkFrames: Int,
    val candidateEvents: Int,
    val eventsPerMinute: Double,
    val validFrameFraction: Double,
    val baselineZScore: Double,
    val baselineFrameCount: Int,
)

object ScanEventLogCodec {
    fun encode(events: List<ScanEvent>): String =
        events.joinToString(separator = "\n") { event ->
            listOf(
                FORMAT_VERSION,
                event.timestampMillis.toString(),
                event.cameraId.encodeField(),
                event.alarmState.name,
                event.durationMillis.toString(),
                event.framesAnalyzed.toString(),
                event.validDarkFrames.toString(),
                event.candidateEvents.toString(),
                event.eventsPerMinute.toString(),
                event.validFrameFraction.toString(),
                event.baselineZScore.toString(),
                event.baselineFrameCount.toString(),
            ).joinToString(separator = FIELD_SEPARATOR)
        }

    fun decode(raw: String): List<ScanEvent> =
        raw.lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { line -> decodeLine(line) }
            .toList()

    private fun decodeLine(line: String): ScanEvent? {
        val parts = line.split(FIELD_SEPARATOR)
        if (parts.size != EXPECTED_FIELDS || parts[0] != FORMAT_VERSION) return null
        val alarmState = runCatching { AlarmState.valueOf(parts[3]) }.getOrNull() ?: return null

        return ScanEvent(
            timestampMillis = parts[1].toLongOrNull() ?: return null,
            cameraId = parts[2].decodeField(),
            alarmState = alarmState,
            durationMillis = parts[4].toLongOrNull() ?: return null,
            framesAnalyzed = parts[5].toIntOrNull() ?: return null,
            validDarkFrames = parts[6].toIntOrNull() ?: return null,
            candidateEvents = parts[7].toIntOrNull() ?: return null,
            eventsPerMinute = parts[8].toDoubleOrNull() ?: return null,
            validFrameFraction = parts[9].toDoubleOrNull() ?: return null,
            baselineZScore = parts[10].toDoubleOrNull() ?: return null,
            baselineFrameCount = parts[11].toIntOrNull() ?: return null,
        )
    }

    private fun String.encodeField(): String =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(toByteArray(StandardCharsets.UTF_8))

    private fun String.decodeField(): String =
        String(Base64.getUrlDecoder().decode(this), StandardCharsets.UTF_8)

    private const val FORMAT_VERSION = "1"
    private const val FIELD_SEPARATOR = "|"
    private const val EXPECTED_FIELDS = 12
}

fun LiveScanProgress.toScanEvent(timestampMillis: Long): ScanEvent = ScanEvent(
    timestampMillis = timestampMillis,
    cameraId = cameraId,
    alarmState = alarmState,
    durationMillis = durationMillis,
    framesAnalyzed = framesAnalyzed,
    validDarkFrames = validDarkFrames,
    candidateEvents = candidateEvents,
    eventsPerMinute = eventsPerMinute,
    validFrameFraction = validFrameFraction,
    baselineZScore = baselineZScore,
    baselineFrameCount = baselineFrameCount,
)
