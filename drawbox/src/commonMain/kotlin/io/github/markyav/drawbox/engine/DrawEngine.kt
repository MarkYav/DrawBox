package io.github.markyav.drawbox.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.IntSize
import io.github.markyav.drawbox.engine.action.StrokeEraseAction

// [SP_ENG_01_06] Public facade exposed to C_CTL_v2.
// All operations must be called from the main thread — this class is not thread-safe.
class DrawEngine(
    val logicalSize: IntSize,
    val undoDepth: Int = 20,
) {
    init {
        require(logicalSize.width > 0 && logicalSize.height > 0) {
            "logicalSize dimensions must be > 0"
        }
        require(undoDepth >= 1) { "undoDepth must be >= 1, was $undoDepth" }
    }

    private val canvasManager = CanvasManager(logicalSize, undoDepth)

    // Gesture state — null when idle, non-null during an active gesture.
    private var activeTool: DrawTool?    = null
    private var activeContext: ToolContext? = null
    private var partialAction: DrawAction? = null

    val canUndo: Boolean get() = canvasManager.canUndo
    val canRedo: Boolean get() = canvasManager.canRedo

    // [SP_ENG_02_01]
    fun beginGesture(tool: DrawTool, point: Offset, context: ToolContext) {
        require(activeTool == null) { "beginGesture called while a gesture is already active" }
        activeTool = tool
        activeContext = context
        partialAction = tool.onGestureStart(point, context)
        canvasManager.clearActiveBitmap()
        partialAction?.let { Canvas(canvasManager.activeBitmap).also { c -> it.render(c) } }
    }

    // [SP_ENG_02_02]
    fun extendGesture(from: Offset, to: Offset) {
        val tool = activeTool ?: return
        val newPartial = tool.onGestureDrag(from, to, partialAction, activeContext!!)
        if (tool.clearsActiveCanvasOnDrag) canvasManager.clearActiveBitmap()
        newPartial?.renderLastSegment(Canvas(canvasManager.activeBitmap))
        partialAction = newPartial
    }

    // [SP_ENG_02_03]
    fun endGesture() {
        val tool = activeTool ?: return
        val final = tool.onGestureEnd(partialAction, activeContext!!)
        canvasManager.clearActiveBitmap()
        when (final) {
            is StrokeEraseAction -> canvasManager.commitStrokeErase(final)
            null -> {}
            else -> canvasManager.commit(final)
        }
        activeTool = null
        activeContext = null
        partialAction = null
    }

    // [SP_ENG_02_04]
    fun onTap(tool: DrawTool, point: Offset, context: ToolContext) {
        require(activeTool == null) { "onTap called while a gesture is already active" }
        val action = tool.onTap(point, context)
        when (action) {
            is StrokeEraseAction -> canvasManager.commitStrokeErase(action)
            null -> {}
            else -> canvasManager.commit(action)
        }
    }

    fun undo() {
        require(activeTool == null) { "undo called during active gesture" }
        canvasManager.undo()
    }

    fun redo() {
        require(activeTool == null) { "redo called during active gesture" }
        canvasManager.redo()
    }

    // [SP_ENG_02_11]
    fun reset() {
        require(activeTool == null) { "reset called during active gesture" }
        canvasManager.reset()
    }

    // [SP_ENG_02_13] Composes committed + active bitmaps into a display snapshot.
    fun getDisplayOutput(): ImageBitmap {
        val result = ImageBitmap(logicalSize.width, logicalSize.height, ImageBitmapConfig.Argb8888)
        Canvas(result).apply {
            drawImage(canvasManager.committedBitmap, Offset.Zero, Paint())
            drawImage(canvasManager.activeBitmap, Offset.Zero, Paint())
        }
        return result
    }

    // [SP_ENG_02_14] Exports the committed drawing at a target resolution via canvas scaling.
    fun exportBitmap(size: IntSize): ImageBitmap {
        require(size.width > 0 && size.height > 0) { "export size dimensions must be > 0" }
        val result = ImageBitmap(size.width, size.height, ImageBitmapConfig.Argb8888)
        val canvas = Canvas(result)
        canvas.scale(
            sx = size.width.toFloat() / logicalSize.width.toFloat(),
            sy = size.height.toFloat() / logicalSize.height.toFloat(),
        )
        for (i in 0..canvasManager.currentIndex) {
            if (i !in canvasManager.suppressedIndices) {
                canvasManager.drawnActions[i].render(canvas)
            }
        }
        return result
    }

    // Returns a ToolContext snapshot for the current engine state.
    // Call this immediately before beginGesture or onTap to get a fresh snapshot.
    fun createToolContext(paintOptions: PaintOptions): ToolContext {
        val bitmapSnapshot = ImageBitmap(logicalSize.width, logicalSize.height, ImageBitmapConfig.Argb8888)
        Canvas(bitmapSnapshot).drawImage(canvasManager.committedBitmap, Offset.Zero, Paint())
        return ToolContext(paintOptions, bitmapSnapshot, logicalSize)
    }
}
