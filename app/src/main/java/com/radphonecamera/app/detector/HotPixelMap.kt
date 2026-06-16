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

    fun toPackedString(maxPixels: Int = Int.MAX_VALUE): String =
        hotPixels
            .take(maxPixels.coerceAtLeast(0))
            .joinToString(separator = ";") { coordinate ->
                "${coordinate.x},${coordinate.y}"
            }

    companion object {
        fun empty(width: Int, height: Int): HotPixelMap = HotPixelMap(
            width = width,
            height = height,
            hotPixels = emptySet(),
        )

        fun fromPackedString(
            width: Int,
            height: Int,
            packedHotPixels: String,
        ): HotPixelMap {
            if (width <= 0 || height <= 0 || packedHotPixels.isBlank()) {
                return empty(width, height)
            }

            val hot = packedHotPixels
                .split(";")
                .mapNotNull { token ->
                    val parts = token.split(",")
                    if (parts.size != 2) return@mapNotNull null
                    val x = parts[0].toIntOrNull() ?: return@mapNotNull null
                    val y = parts[1].toIntOrNull() ?: return@mapNotNull null
                    if (x !in 0 until width || y !in 0 until height) {
                        null
                    } else {
                        PixelCoordinate(x, y)
                    }
                }
                .toSet()

            return HotPixelMap(width, height, hot)
        }

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
