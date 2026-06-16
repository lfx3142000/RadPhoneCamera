package com.radphonecamera.app.detector

import kotlin.math.sqrt

data class BaselineModel(
    val frameCount: Int = 0,
    val meanEventsPerFrame: Double = 0.0,
    val varianceEventsPerFrame: Double = 0.0,
) {
    val hasEnoughData: Boolean = frameCount >= MIN_FRAMES

    fun update(eventCount: Int): BaselineModel {
        val nextCount = frameCount + 1
        val delta = eventCount - meanEventsPerFrame
        val nextMean = meanEventsPerFrame + delta / nextCount
        val nextM2 = varianceEventsPerFrame * (frameCount - 1).coerceAtLeast(0) +
            delta * (eventCount - nextMean)
        val nextVariance = if (nextCount > 1) nextM2 / (nextCount - 1) else 0.0
        return copy(
            frameCount = nextCount,
            meanEventsPerFrame = nextMean,
            varianceEventsPerFrame = nextVariance.coerceAtLeast(0.0),
        )
    }

    fun zScore(observedEvents: Int, observedFrames: Int): Double {
        if (observedFrames <= 0 || !hasEnoughData) return 0.0
        val expected = meanEventsPerFrame * observedFrames
        val systemVariance = varianceEventsPerFrame * observedFrames
        val variance = (expected + systemVariance).coerceAtLeast(1.0)
        return (observedEvents - expected) / sqrt(variance)
    }

    companion object {
        const val MIN_FRAMES = 30
    }
}

