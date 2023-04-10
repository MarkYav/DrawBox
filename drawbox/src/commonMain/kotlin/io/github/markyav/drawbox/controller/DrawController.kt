package io.github.markyav.drawbox.controller

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.IntSize
import io.github.markyav.drawbox.model.PathWrapper
import io.github.markyav.drawbox.util.addNotNull
import io.github.markyav.drawbox.util.createPath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

/**
 * DrawController interacts with [DrawBox] and it allows you to control the canvas and all the components with it.
 */
class DrawController {
    private var state: MutableStateFlow<DrawBoxConnectionState> = MutableStateFlow(DrawBoxConnectionState.Disconnected)

    /** A stateful list of [Path] that is drawn on the [Canvas]. */
    private val drawnPaths: MutableStateFlow<List<PathWrapper>> = MutableStateFlow(emptyList())

    private val activeDrawingPath: MutableStateFlow<List<Offset>?> = MutableStateFlow(null)

    /** A stateful list of [Path] that was drawn on the [Canvas] but user retracted his action. */
    private val canceledPaths: MutableStateFlow<List<PathWrapper>> = MutableStateFlow(emptyList())

    /** An [canvasOpacity] of the [Canvas] in the [DrawBox] */
    var canvasOpacity: MutableStateFlow<Float> = MutableStateFlow(1f)

    /** An [opacity] of the stroke */
    var opacity: MutableStateFlow<Float> = MutableStateFlow(1f)

    /** A [strokeWidth] of the stroke */
    var strokeWidth: MutableStateFlow<Float> = MutableStateFlow(10f)

    /** A [color] of the stroke */
    var color: MutableStateFlow<Color> = MutableStateFlow(Color.Red)

    /** A [background] of the background of DrawBox */
    var background: MutableStateFlow<DrawBoxBackground> = MutableStateFlow(DrawBoxBackground.NoBackground)

    /** Indicate how many redos it is possible to do. */
    val undoCount = drawnPaths.value.size

    /** Indicate how many undos it is possible to do. */
    val redoCount = canceledPaths.value.size

    /** Executes undo the drawn path if possible. */
    fun undo() {
        if (drawnPaths.value.isNotEmpty()) {
            val _drawnPaths = drawnPaths.value.toMutableList()
            val _canceledPaths = canceledPaths.value.toMutableList()

            _canceledPaths.add(_drawnPaths.removeLast())

            drawnPaths.value = _drawnPaths
            canceledPaths.value = _canceledPaths
        }
    }

    /** Executes redo the drawn path if possible. */
    fun redo() {
        if (canceledPaths.value.isNotEmpty()) {
            val _drawnPaths = drawnPaths.value.toMutableList()
            val _canceledPaths = canceledPaths.value.toMutableList()

            _drawnPaths.add(_canceledPaths.removeLast())

            drawnPaths.value = _drawnPaths
            canceledPaths.value = _canceledPaths
        }
    }

    /** Clear drawn paths and the bitmap image. */
    fun reset() {
        drawnPaths.value = emptyList()
        canceledPaths.value = emptyList()
    }

    /** Call this function when user starts drawing a path. */
    internal fun updateLatestPath(newPoint: Offset) {
        (state.value as? DrawBoxConnectionState.Connected)?.let {
            require(activeDrawingPath.value != null)
            val list = activeDrawingPath.value!!.toMutableList()
            list.add(newPoint.div(it.size.toFloat()))
            activeDrawingPath.value = list
        }
    }

    /** When dragging call this function to update the last path. */
    internal fun insertNewPath(newPoint: Offset) {
        (state.value as? DrawBoxConnectionState.Connected)?.let {
            require(activeDrawingPath.value == null)
            /*val pathWrapper = PathWrapper(
                points = mutableStateListOf(newPoint.div(it.size.toFloat())),
                strokeColor = color.value,
                alpha = opacity.value,
                strokeWidth = strokeWidth.value.div(it.size.toFloat()),
            )*/
            activeDrawingPath.value = listOf(newPoint.div(it.size.toFloat()))
            canceledPaths.value = emptyList()
        }
    }

    internal fun finalizePath() {
        (state.value as? DrawBoxConnectionState.Connected)?.let {
            require(activeDrawingPath.value != null)
            val _drawnPaths = drawnPaths.value.toMutableList()

            val pathWrapper = PathWrapper(
                points = activeDrawingPath.value!!,
                strokeColor = color.value,
                alpha = opacity.value,
                strokeWidth = strokeWidth.value.div(it.size.toFloat()),
            )
            _drawnPaths.add(pathWrapper)

            drawnPaths.value = _drawnPaths
            activeDrawingPath.value = null
        }
    }

    /** Call this function to connect to the [DrawBox]. */
    internal fun connectToDrawBox(size: IntSize) {
        if (
            size.width > 0 &&
            size.height > 0 &&
            size.width == size.height
        ) {
            state.value = DrawBoxConnectionState.Connected(size = size.width)
        }
    }

    internal fun onTap(newPoint: Offset) {
        insertNewPath(newPoint)
        finalizePath()
    }

    private fun List<PathWrapper>.scale(size: Float): List<PathWrapper> {
        return this.map { pw ->
            val t = pw.points.map { it.times(size) }
            pw.copy(
                points = mutableListOf<Offset>().also { it.addAll(t) },
                strokeWidth = pw.strokeWidth * size
            )
        }
    }

    fun getDrawPath(subscription: DrawBoxSubscription, coroutineScope: CoroutineScope): StateFlow<List<PathWrapper>> {
        return when (subscription) {
            is DrawBoxSubscription.DynamicUpdate -> getDynamicUpdateDrawnPath(coroutineScope)
            is DrawBoxSubscription.FinishDrawingUpdate -> drawnPaths
        }
    }

    private fun getDynamicUpdateDrawnPath(coroutineScope: CoroutineScope): StateFlow<List<PathWrapper>> {
        return combine(drawnPaths, activeDrawingPath) { a, b ->
            val _a = a.toMutableList()
            (state.value as? DrawBoxConnectionState.Connected)?.let {
                val pathWrapper = PathWrapper(
                    points = activeDrawingPath.value ?: emptyList(),
                    strokeColor = color.value,
                    alpha = opacity.value,
                    strokeWidth = strokeWidth.value.div(it.size.toFloat()),
                )
                _a.addNotNull(pathWrapper)
            }
            _a
        }.stateIn(coroutineScope, started = SharingStarted.Eagerly, initialValue = drawnPaths.value)
    }

    internal fun getPathWrappersForDrawbox(subscription: DrawBoxSubscription, coroutineScope: CoroutineScope): StateFlow<List<PathWrapper>> {
        return combine(getDrawPath(subscription, coroutineScope), state) { paths, st ->
            val size = (st as? DrawBoxConnectionState.Connected)?.let { it.size } ?: 1
            paths.scale(size.toFloat())
        }.stateIn(coroutineScope, started = SharingStarted.Eagerly, initialValue = drawnPaths.value)
    }

    fun getBitmap(size: Int, subscription: DrawBoxSubscription, coroutineScope: CoroutineScope): StateFlow<ImageBitmap> {
        val initialBitmap = ImageBitmap(size, size, ImageBitmapConfig.Argb8888)
        val path = getDrawPath(subscription, coroutineScope)
        return path.map {
            val bitmap = ImageBitmap(size, size, ImageBitmapConfig.Argb8888)
            val canvas = Canvas(bitmap)
            it.scale(size.toFloat()).forEach { pw ->
                canvas.drawPath(
                    createPath(pw.points),
                    paint = Paint().apply {
                        color = pw.strokeColor
                        alpha = pw.alpha
                        style = PaintingStyle.Stroke
                        strokeCap = StrokeCap.Round
                        strokeJoin = StrokeJoin.Round
                        strokeWidth = pw.strokeWidth
                    }
                )
            }
            bitmap
        }.stateIn(coroutineScope, started = SharingStarted.Eagerly, initialValue = initialBitmap)
    }
}