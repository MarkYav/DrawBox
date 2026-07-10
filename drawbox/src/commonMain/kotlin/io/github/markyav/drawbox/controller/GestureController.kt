package io.github.markyav.drawbox.controller

import io.github.markyav.drawbox.model.Action
import io.github.markyav.drawbox.model.BrushAction
import io.github.markyav.drawbox.model.DrawSettings
import io.github.markyav.drawbox.model.DrawTool
import io.github.markyav.drawbox.model.NormPoint

internal class GestureController(
    private val documentManager: DocumentManager,
    private val imageManager: ImageManager,
    private val getSettings: () -> DrawSettings,
    private val onLiveUpdate: () -> Unit,
    private val onCommitted: () -> Unit
) {
    private var ongoingAction: Action? = null
    private var isGestureLocked = false

    fun onTap(point: NormPoint) {
        if (isGestureLocked) return
        
        val settings = getSettings()
        if (settings.tool == DrawTool.Brush) {
            val action = BrushAction(
                color = settings.color,
                strokeWidth = settings.strokeWidth,
                points = listOf(point)
            )
            documentManager.commitAction(action)
            imageManager.current?.let {
                RenderEngine.renderAction(action, it)
            }
            onCommitted()
        }
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
            else -> return // M2 tools deferred
        }

        imageManager.clearActive()
        imageManager.active?.let { 
            RenderEngine.renderAction(ongoingAction!!, it) 
        }
        onLiveUpdate()
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
            onLiveUpdate()
        }
    }

    fun onDragEnd() {
        if (!isGestureLocked) return
        
        val action = ongoingAction
        if (action != null) {
            // Commit to history
            documentManager.commitAction(action)
            
            // Draw to current image
            imageManager.current?.let {
                RenderEngine.renderAction(action, it)
            }
            
            // Clear active
            imageManager.clearActive()
            ongoingAction = null
            
            onCommitted()
        }
        
        isGestureLocked = false
    }
}
