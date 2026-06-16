package com.radphonecamera.app.detector

import java.util.ArrayDeque
import kotlin.math.abs

data class SparseEvent(
    val pixels: List<PixelCoordinate>,
    val peakValue: Int,
) {
    val size: Int = pixels.size
}

data class SparseEventDetectionResult(
    val events: List<SparseEvent>,
    val rejectedHotPixels: Int,
    val rejectedArtifacts: Int,
) {
    val candidateEventCount: Int = events.size
}

class SparseEventDetector(
    private val brightPixelThreshold: Int = 85,
    private val maxClusterPixels: Int = 12,
) {
    fun detect(
        luma: ByteArray,
        width: Int,
        height: Int,
        hotPixelMap: HotPixelMap = HotPixelMap.empty(width, height),
    ): SparseEventDetectionResult {
        if (width <= 0 || height <= 0 || luma.isEmpty()) {
            return SparseEventDetectionResult(emptyList(), 0, 0)
        }

        val pixelCount = minOf(width * height, luma.size)
        val visited = BooleanArray(pixelCount)
        val events = mutableListOf<SparseEvent>()
        var rejectedHotPixels = 0
        var rejectedArtifacts = 0

        for (index in 0 until pixelCount) {
            if (visited[index]) continue

            val x = index % width
            val y = index / width
            val value = luma[index].toInt() and 0xFF
            if (value < brightPixelThreshold) continue

            if (hotPixelMap.isHot(x, y)) {
                visited[index] = true
                rejectedHotPixels += 1
                continue
            }

            val cluster = floodFill(luma, width, height, index, visited, hotPixelMap)
            if (cluster.isEmpty()) continue

            val peak = cluster.maxOf { coordinate ->
                luma[coordinate.y * width + coordinate.x].toInt() and 0xFF
            }
            if (cluster.size <= maxClusterPixels && isCompact(cluster)) {
                events += SparseEvent(cluster, peak)
            } else {
                rejectedArtifacts += 1
            }
        }

        return SparseEventDetectionResult(
            events = events,
            rejectedHotPixels = rejectedHotPixels,
            rejectedArtifacts = rejectedArtifacts,
        )
    }

    private fun floodFill(
        luma: ByteArray,
        width: Int,
        height: Int,
        startIndex: Int,
        visited: BooleanArray,
        hotPixelMap: HotPixelMap,
    ): List<PixelCoordinate> {
        val queue = ArrayDeque<Int>()
        val cluster = mutableListOf<PixelCoordinate>()
        queue.add(startIndex)
        visited[startIndex] = true

        while (!queue.isEmpty()) {
            val index = queue.removeFirst()
            val x = index % width
            val y = index / width
            val value = luma[index].toInt() and 0xFF
            if (value < brightPixelThreshold || hotPixelMap.isHot(x, y)) {
                continue
            }

            cluster += PixelCoordinate(x, y)

            for (ny in (y - 1)..(y + 1)) {
                for (nx in (x - 1)..(x + 1)) {
                    if (nx == x && ny == y) continue
                    if (nx !in 0 until width || ny !in 0 until height) continue
                    val nextIndex = ny * width + nx
                    if (nextIndex >= visited.size || visited[nextIndex]) continue
                    val nextValue = luma[nextIndex].toInt() and 0xFF
                    if (nextValue >= brightPixelThreshold) {
                        visited[nextIndex] = true
                        queue.add(nextIndex)
                    }
                }
            }
        }

        return cluster
    }

    private fun isCompact(cluster: List<PixelCoordinate>): Boolean {
        val xs = cluster.map { it.x }
        val ys = cluster.map { it.y }
        val width = (xs.maxOrNull() ?: 0) - (xs.minOrNull() ?: 0) + 1
        val height = (ys.maxOrNull() ?: 0) - (ys.minOrNull() ?: 0) + 1
        return abs(width - height) <= maxClusterPixels
    }
}

