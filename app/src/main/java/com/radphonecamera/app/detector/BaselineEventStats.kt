package com.radphonecamera.app.detector

import com.radphonecamera.app.camera.LumaFrameSnapshot

data class BaselineEventStats(
    val frameCount: Int = 0,
    val totalCandidateEvents: Int = 0,
    val meanEventsPerFrame: Double = 0.0,
    val varianceEventsPerFrame: Double = 0.0,
) {
    fun toBaselineModel(): BaselineModel = BaselineModel(
        frameCount = frameCount,
        meanEventsPerFrame = meanEventsPerFrame,
        varianceEventsPerFrame = varianceEventsPerFrame,
    )

    companion object {
        fun fromSnapshots(
            snapshots: List<LumaFrameSnapshot>,
            hotPixelMap: HotPixelMap?,
            detector: SparseEventDetector = SparseEventDetector(),
        ): BaselineEventStats {
            val first = snapshots.firstOrNull() ?: return BaselineEventStats()
            val matchingSnapshots = snapshots.filter {
                it.width == first.width && it.height == first.height
            }
            val activeHotPixelMap = hotPixelMap
                ?.takeIf { it.width == first.width && it.height == first.height }
                ?: HotPixelMap.empty(first.width, first.height)

            val eventCounts = matchingSnapshots.map { snapshot ->
                detector.detect(
                    luma = snapshot.luma,
                    width = snapshot.width,
                    height = snapshot.height,
                    hotPixelMap = activeHotPixelMap,
                ).candidateEventCount
            }
            val model = eventCounts.fold(BaselineModel()) { baseline, events ->
                baseline.update(events)
            }

            return BaselineEventStats(
                frameCount = model.frameCount,
                totalCandidateEvents = eventCounts.sum(),
                meanEventsPerFrame = model.meanEventsPerFrame,
                varianceEventsPerFrame = model.varianceEventsPerFrame,
            )
        }
    }
}
