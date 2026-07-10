package io.github.markyav.drawbox.controller

import io.github.markyav.drawbox.model.Action
import io.github.markyav.drawbox.model.BrushAction
import io.github.markyav.drawbox.model.DrawSettings
import io.github.markyav.drawbox.model.DrawTool
import io.github.markyav.drawbox.model.EraserAction
import io.github.markyav.drawbox.model.FillAction
import io.github.markyav.drawbox.model.RemoveAction
import io.github.markyav.drawbox.model.NormPoint
import io.github.markyav.drawbox.util.StrokeProcessor

internal class GestureController(
    private val documentManager: DocumentManager,
    private val imageManager: ImageManager,
    private val getSettings: () -> DrawSettings,
    private val onLiveUpdate: (Action?) -> Unit,
    private val onCommitted: () -> Unit,
    private val onNeedsFullRedraw: () -> Unit
) {
    private var ongoingAction: Action? = null
    private var isGestureLocked = false

    fun onTap(point: NormPoint) {
        if (isGestureLocked) return
        
        val settings = getSettings()
        when (settings.tool) {
            DrawTool.Brush -> {
                val action = BrushAction(
                    color = settings.color,
                    strokeWidth = settings.strokeWidth,
                    points = listOf(point)
                )
                commitAndDraw(action)
            }
            DrawTool.Eraser -> {
                val action = EraserAction(
                    strokeWidth = settings.strokeWidth,
                    points = listOf(point)
                )
                commitAndDraw(action)
            }
            DrawTool.ActionEraser -> {
                val hits = findHitActions(point, emptyList())
                if (hits.isNotEmpty()) {
                    val action = RemoveAction(removedActionIds = hits)
                    documentManager.commitAction(action)
                    onNeedsFullRedraw()
                    onCommitted()
                }
            }
            DrawTool.ColorFill -> {
                val spans = imageManager.current?.let { bitmap ->
                    RenderEngine.calculateFillSpans(
                        target = bitmap,
                        startXNorm = point.x,
                        startYNorm = point.y,
                        tolerance = settings.fillTolerance,
                        fillColorInt = settings.color.toInt()
                    )
                } ?: emptyList()
                val action = FillAction(
                    color = settings.color,
                    tolerance = settings.fillTolerance,
                    point = point,
                    spans = spans
                )
                commitAndDraw(action)
            }
        }
    }

    private fun commitAndDraw(action: Action) {
        documentManager.commitAction(action)
        imageManager.current?.let {
            RenderEngine.renderAction(action, it)
        }
        onCommitted()
    }

    fun onDragStart(point: NormPoint) {
        if (isGestureLocked) return
        isGestureLocked = true
        
        val settings = getSettings()
        ongoingAction = when (settings.tool) {
            DrawTool.Brush -> BrushAction(
                color = settings.color,
                strokeWidth = settings.strokeWidth,
                points = listOf(point)
            )
            DrawTool.Eraser -> EraserAction(
                strokeWidth = settings.strokeWidth,
                points = listOf(point)
            )
            DrawTool.ActionEraser -> RemoveAction(
                removedActionIds = findHitActions(point, emptyList())
            )
            else -> {
                isGestureLocked = false
                return
            }
        }

        imageManager.clearActive()
        imageManager.active?.let { 
            RenderEngine.renderAction(ongoingAction!!, it) 
        }
        onLiveUpdate(ongoingAction)
    }

    fun onDrag(point: NormPoint) {
        if (!isGestureLocked) return
        
        val action = ongoingAction
        if (action is BrushAction) {
            ongoingAction = action.copy(points = action.points + point)
            imageManager.clearActive()
            imageManager.active?.let { 
                RenderEngine.renderAction(ongoingAction!!, it) 
            }
            onLiveUpdate(ongoingAction)
        } else if (action is EraserAction) {
            ongoingAction = action.copy(points = action.points + point)
            onLiveUpdate(ongoingAction)
        } else if (action is RemoveAction) {
            val newHits = findHitActions(point, action.removedActionIds)
            if (newHits.isNotEmpty()) {
                ongoingAction = action.copy(removedActionIds = action.removedActionIds + newHits)
                // We do not render RemoveAction incrementally. 
                // A more advanced implementation might redraw the live image here.
            }
        }
    }

    fun onDragEnd() {
        if (!isGestureLocked) return
        
        var action = ongoingAction
        if (action != null) {
            val width = imageManager.current?.width?.toFloat() ?: 1000f
            val height = imageManager.current?.height?.toFloat() ?: 1000f

            // Apply StrokeProcessor for Brush and Eraser actions
            if (action is BrushAction) {
                action = action.copy(points = StrokeProcessor.process(action.points, width, height))
            } else if (action is EraserAction) {
                action = action.copy(points = StrokeProcessor.process(action.points, width, height))
            }

            // Commit to history
            documentManager.commitAction(action)
            
            if (action is RemoveAction) {
                onNeedsFullRedraw()
            } else {
                // Draw to current image incrementally
                imageManager.current?.let {
                    RenderEngine.renderAction(action, it)
                }
            }
            
            // Clear active
            imageManager.clearActive()
            ongoingAction = null
            
            onCommitted()
        }
        
        isGestureLocked = false
    }

    private fun findHitActions(point: NormPoint, alreadyRemoved: List<String>): List<String> {
        val history = documentManager.history.value.actions
        val globallyRemoved = history.filterIsInstance<RemoveAction>().flatMap { it.removedActionIds }.toSet()
        val hitIds = mutableListOf<String>()
        val width = imageManager.current?.width?.toFloat() ?: 1000f
        val height = imageManager.current?.height?.toFloat() ?: 1000f
        
        for (action in history) {
            if (action.id in globallyRemoved || action.id in alreadyRemoved) continue
            when (action) {
                is BrushAction -> {
                    val threshold = (action.strokeWidth / 2f) + (width * 0.02f)
                    if (action.points.any { p -> distPixels(p, point, width, height) < threshold }) {
                        hitIds.add(action.id)
                    }
                }
                is FillAction -> {
                    val ex = (point.x * width).toInt()
                    val ey = (point.y * height).toInt()
                    val radius = (width * 0.02f).toInt()
                    if (action.spans.any { span -> 
                        val insideY = ey in (span.y - radius)..(span.y + radius)
                        val insideX = ex in (span.lx - radius)..(span.rx + radius)
                        insideX && insideY
                    }) {
                        hitIds.add(action.id)
                    }
                }
            }
        }
        return hitIds
    }
    
    private fun distPixels(p1: NormPoint, p2: NormPoint, w: Float, h: Float): Float {
        val dx = (p1.x - p2.x) * w
        val dy = (p1.y - p2.y) * h
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}
