package com.radphonecamera.app.sensors

import kotlin.math.abs
import kotlin.math.sqrt

enum class MotionQuality(val label: String) {
    Still("Still"),
    MostlyStill("Mostly still"),
    Moving("Moving"),
    Unavailable("Sensor unavailable"),
}

enum class DevicePosture(val label: String) {
    FaceDown("Face down"),
    FaceUp("Face up"),
    Upright("Upright"),
    Side("Side"),
    Unknown("Unknown"),
}

data class MotionState(
    val quality: MotionQuality,
    val posture: DevicePosture,
    val sampleCount: Int,
    val deltaG: Double,
    val reason: String,
) {
    val allowsDetectorFrame: Boolean =
        quality == MotionQuality.Still ||
            quality == MotionQuality.MostlyStill ||
            quality == MotionQuality.Unavailable

    companion object {
        val Unavailable = MotionState(
            quality = MotionQuality.Unavailable,
            posture = DevicePosture.Unknown,
            sampleCount = 0,
            deltaG = 0.0,
            reason = "Accelerometer is not available; motion rejection is disabled.",
        )
    }
}

data class MotionSample(
    val x: Float,
    val y: Float,
    val z: Float,
)

class MotionStateClassifier {
    private var previousSample: MotionSample? = null
    private var smoothedDeltaG = 0.0
    private var sampleCount = 0

    fun record(sample: MotionSample): MotionState {
        sampleCount += 1
        val previous = previousSample
        previousSample = sample

        val instantDeltaG = if (previous == null) {
            0.0
        } else {
            sample.deltaFrom(previous) / STANDARD_GRAVITY
        }
        smoothedDeltaG = if (sampleCount <= 2) {
            instantDeltaG
        } else {
            (MOTION_SMOOTHING * smoothedDeltaG) + ((1.0 - MOTION_SMOOTHING) * instantDeltaG)
        }

        val posture = sample.posture()
        val quality = when {
            sampleCount < MIN_SAMPLES_FOR_STABLE -> MotionQuality.MostlyStill
            smoothedDeltaG <= STILL_DELTA_G -> MotionQuality.Still
            smoothedDeltaG <= MOSTLY_STILL_DELTA_G -> MotionQuality.MostlyStill
            else -> MotionQuality.Moving
        }
        val reason = when (quality) {
            MotionQuality.Still -> "Phone appears still; ${posture.label.lowercase()} posture detected."
            MotionQuality.MostlyStill -> "Small motion detected; keep the phone still for best sensitivity."
            MotionQuality.Moving -> "Motion detected; detector frames are treated as unstable."
            MotionQuality.Unavailable -> MotionState.Unavailable.reason
        }

        return MotionState(
            quality = quality,
            posture = posture,
            sampleCount = sampleCount,
            deltaG = smoothedDeltaG,
            reason = reason,
        )
    }

    private fun MotionSample.deltaFrom(other: MotionSample): Double {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return sqrt((dx * dx + dy * dy + dz * dz).toDouble())
    }

    private fun MotionSample.posture(): DevicePosture = when {
        z <= -FACE_DOWN_THRESHOLD -> DevicePosture.FaceDown
        z >= FACE_UP_THRESHOLD -> DevicePosture.FaceUp
        abs(y) >= UPRIGHT_THRESHOLD -> DevicePosture.Upright
        abs(x) >= SIDE_THRESHOLD -> DevicePosture.Side
        else -> DevicePosture.Unknown
    }

    private companion object {
        const val STANDARD_GRAVITY = 9.80665
        const val MIN_SAMPLES_FOR_STABLE = 4
        const val MOTION_SMOOTHING = 0.70
        const val STILL_DELTA_G = 0.035
        const val MOSTLY_STILL_DELTA_G = 0.12
        const val FACE_DOWN_THRESHOLD = 7.0f
        const val FACE_UP_THRESHOLD = 7.0f
        const val UPRIGHT_THRESHOLD = 7.0f
        const val SIDE_THRESHOLD = 7.0f
    }
}
