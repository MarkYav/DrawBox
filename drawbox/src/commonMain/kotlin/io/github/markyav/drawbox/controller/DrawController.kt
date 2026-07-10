package io.github.markyav.drawbox.controller

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntSize
import io.github.markyav.drawbox.model.ActionHistory
import io.github.markyav.drawbox.model.DrawSettings
import io.github.markyav.drawbox.model.DrawTool
import io.github.markyav.drawbox.model.ImageMode
import io.github.markyav.drawbox.model.NormPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class DrawController(
    val canvasSize: IntSize,
    checkpointInterval: Int = 5,
    startingColor: Long = 0xFFFF0000,
    startingWidth: Float = 10f,
    startingTool: DrawTool = DrawTool.Brush,
) {
    init {
        require(canvasSize.width > 0 && canvasSize.height > 0) { "Canvas size must be positive" }
    }

    private val documentManager = DocumentManager()
    private val imageManager = ImageManager().apply { allocateBuffers(canvasSize) }

    private val _settings = MutableStateFlow(
        DrawSettings(
            tool = startingTool,
            color = startingColor,
            strokeWidth = startingWidth
        )
    )
    val settings: StateFlow<DrawSettings> = _settings.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)

    private val _committedImage = MutableStateFlow(imageManager.current!!)
    private val _liveImage = MutableStateFlow(imageManager.current!!)

    private val gestureController = GestureController(
        documentManager = documentManager,
        imageManager = imageManager,
        getSettings = { _settings.value },
        onLiveUpdate = {
            val merged = ImageBitmap(canvasSize.width, canvasSize.height)
            val canvas = Canvas(merged)
            imageManager.current?.let { canvas.drawImage(it, Offset.Zero, Paint()) }
            imageManager.active?.let { canvas.drawImage(it, Offset.Zero, Paint()) }
            _liveImage.value = merged
        },
        onCommitted = {
            val newBitmap = ImageBitmap(canvasSize.width, canvasSize.height)
            val canvas = Canvas(newBitmap)
            imageManager.current?.let { canvas.drawImage(it, Offset.Zero, Paint()) }
            _committedImage.value = newBitmap
            _liveImage.value = newBitmap
        }
    )

    val canUndo: StateFlow<Boolean> = documentManager.history.map { it.actions.isNotEmpty() }
        .stateIn(scope, SharingStarted.Eagerly, false)

    val canRedo: StateFlow<Boolean> = documentManager.redoStack.map { it.isNotEmpty() }
        .stateIn(scope, SharingStarted.Eagerly, false)

    fun image(mode: ImageMode): StateFlow<ImageBitmap> = when (mode) {
        ImageMode.Committed -> _committedImage.asStateFlow()
        ImageMode.Live -> _liveImage.asStateFlow()
    }

    fun setTool(tool: DrawTool) {
        _settings.update { it.copy(tool = tool) }
    }

    fun setColor(color: Long) {
        _settings.update { it.copy(color = color) }
    }

    fun setStrokeWidth(width: Float) {
        _settings.update { it.copy(strokeWidth = width) }
    }

    fun onTap(p: NormPoint) = gestureController.onTap(p)
    fun onDragStart(p: NormPoint) = gestureController.onDragStart(p)
    fun onDrag(p: NormPoint) = gestureController.onDrag(p)
    fun onDragEnd() = gestureController.onDragEnd()

    private fun reDrawHistory() {
        imageManager.current?.let { RenderEngine.render(documentManager.history.value, it) }
        
        val newBitmap = ImageBitmap(canvasSize.width, canvasSize.height)
        val canvas = Canvas(newBitmap)
        imageManager.current?.let { canvas.drawImage(it, Offset.Zero, Paint()) }
        _committedImage.value = newBitmap
        _liveImage.value = newBitmap
    }

    fun undo() {
        if (documentManager.undo()) {
            reDrawHistory()
        }
    }

    fun redo() {
        if (documentManager.redo()) {
            reDrawHistory()
        }
    }

    fun clear() {
        documentManager.clear()
        imageManager.current?.let { RenderEngine.clearBitmap(it) }
        val newBitmap = ImageBitmap(canvasSize.width, canvasSize.height)
        _committedImage.value = newBitmap
        _liveImage.value = newBitmap
    }

    fun snapshotHistory(): ActionHistory = documentManager.history.value

    fun loadHistory(history: ActionHistory) {
        documentManager.clear()
        history.actions.forEach { documentManager.commitAction(it) }
        reDrawHistory()
    }

    suspend fun export(size: IntSize): ImageBitmap {
        val aspectOriginal = canvasSize.width.toFloat() / canvasSize.height.toFloat()
        val aspectNew = size.width.toFloat() / size.height.toFloat()
        require(kotlin.math.abs(aspectOriginal - aspectNew) < 0.01f) { 
            "Export size must have the same aspect ratio as canvasSize" 
        }

        val bitmap = ImageBitmap(size.width, size.height)
        RenderEngine.render(documentManager.history.value, bitmap)
        return bitmap
    }
}