package com.radphonecamera.app.detector

data class PixelCoordinate(
    val x: Int,
    val y: Int,
)

data class HotPixelMap(
    val width: Int,
    val height: Int,
    private val hotPixels: Set<PixelCoordinate>,
) {
    val size: Int = hotPixels.size

    fun isHot(x: Int, y: Int): Boolean = hotPixels.contains(PixelCoordinate(x, y))

    companion object {
        fun empty(width: Int, height: Int): HotPixelMap = HotPixelMap(
            width = width,
            height = height,
            hotPixels = emptySet(),
        )

        fun fromDarkFrames(
            frames: List<ByteArray>,
            width: Int,
            height: Int,
            threshold: Int = 70,
            persistenceFraction: Double = 0.6,
        ): HotPixelMap {
            if (frames.isEmpty() || width <= 0 || height <= 0) {
                return empty(width, height)
            }

            val pixelCount = width * height
            val hitCounts = IntArray(pixelCount)
            frames.forEach { frame ->
                val limit = minOf(frame.size, pixelCount)
                for (index in 0 until limit) {
                    val value = frame[index].toInt() and 0xFF
                    if (value >= threshold) {
                        hitCounts[index] += 1
                    }
                }
            }

            val requiredHits = (frames.size * persistenceFraction)
                .toInt()
                .coerceAtLeast(1)
            val hot = buildSet {
                hitCounts.forEachIndexed { index, hits ->
                    if (hits >= requiredHits) {
                        add(PixelCoordinate(index % width, index / width))
                    }
                }
            }

            return HotPixelMap(width, height, hot)
        }
    }
}

