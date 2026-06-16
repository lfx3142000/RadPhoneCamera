package com.radphonecamera.app.detector

import android.media.Image
import java.nio.ByteBuffer
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

data class FrameStats(
    val width: Int,
    val height: Int,
    val sampledPixels: Int,
    val mean: Double,
    val variance: Double,
    val min: Int,
    val max: Int,
    val timestampNanos: Long = 0L,
)

object FrameStatsCalculator {
    fun fromImage(
        image: Image,
        maxSamples: Int = 20_000,
    ): FrameStats {
        val lumaPlane = image.planes.first()
        return fromBuffer(
            buffer = lumaPlane.buffer,
            width = image.width,
            height = image.height,
            rowStride = lumaPlane.rowStride,
            pixelStride = lumaPlane.pixelStride,
            maxSamples = maxSamples,
            timestampNanos = image.timestamp,
        )
    }

    fun fromBytes(
        luma: ByteArray,
        width: Int,
        height: Int,
        rowStride: Int = width,
        pixelStride: Int = 1,
        maxSamples: Int = Int.MAX_VALUE,
    ): FrameStats = fromBuffer(
        buffer = ByteBuffer.wrap(luma),
        width = width,
        height = height,
        rowStride = rowStride,
        pixelStride = pixelStride,
        maxSamples = maxSamples,
        timestampNanos = 0L,
    )

    fun fromBuffer(
        buffer: ByteBuffer,
        width: Int,
        height: Int,
        rowStride: Int,
        pixelStride: Int,
        maxSamples: Int,
        timestampNanos: Long,
    ): FrameStats {
        if (width <= 0 || height <= 0 || rowStride <= 0 || pixelStride <= 0) {
            return emptyStats(width, height, timestampNanos)
        }

        val duplicate = buffer.duplicate()
        val totalPixels = width.toLong() * height.toLong()
        val sampleStep = max(1, ceil(sqrt(totalPixels.toDouble() / max(1, maxSamples))).toInt())

        var count = 0
        var mean = 0.0
        var m2 = 0.0
        var min = 255
        var max = 0

        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                val index = y * rowStride + x * pixelStride
                if (index < duplicate.limit()) {
                    val value = duplicate.get(index).toInt() and 0xFF
                    count += 1
                    val delta = value - mean
                    mean += delta / count
                    val delta2 = value - mean
                    m2 += delta * delta2
                    if (value < min) min = value
                    if (value > max) max = value
                }
                x += sampleStep
            }
            y += sampleStep
        }

        if (count == 0) {
            return emptyStats(width, height, timestampNanos)
        }

        return FrameStats(
            width = width,
            height = height,
            sampledPixels = count,
            mean = mean,
            variance = if (count > 1) m2 / (count - 1) else 0.0,
            min = min,
            max = max,
            timestampNanos = timestampNanos,
        )
    }

    private fun emptyStats(
        width: Int,
        height: Int,
        timestampNanos: Long,
    ): FrameStats = FrameStats(
        width = width,
        height = height,
        sampledPixels = 0,
        mean = 0.0,
        variance = 0.0,
        min = 0,
        max = 0,
        timestampNanos = timestampNanos,
    )
}

