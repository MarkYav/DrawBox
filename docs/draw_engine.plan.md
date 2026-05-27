# Implementation Plan: Draw Engine  {#PL_ENG}

> **Code:** PL_ENG
> **Status:** in-progress
> **Created:** 2026-05-27
> **Updated:** 2026-05-27
>
> **Concept:** [C_ENG](draw_engine.concept.md)
> **Specification:** [SP_ENG](draw_engine.sp.md)
> **Depends on plans:** [PL_UTL](util.plan.md)
> **Used by plans:** PL_CTL_v2 *(not yet written)*
>
> Implement the Draw Engine module: the open DrawAction/DrawTool interfaces,
> all built-in tool and action implementations, the private CanvasManager,
> and the public DrawEngine facade that C_CTL_v2 will call.

---

## Goal

When this plan is complete, `DrawEngine` is a standalone, fully-tested drawing
subsystem that C_CTL_v2 can instantiate with a logical size and use to drive
gesture recording, history management, undo/redo, and bitmap export — with no
knowledge of Compose UI, gestures, or the controller.

---

## Technology Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Language / target | Kotlin, `commonMain` | All library code must be KMP-compatible (rule: architecture — KMP Targets) |
| Package root | `io.github.markyav.drawbox.engine` | Isolated new package; engine is removed by deleting this package (C_ENG rollback) |
| Sub-packages | `engine.action`, `engine.tool` | Mirrors responsibility split; avoids cross-cutting packages (rule: architecture) |
| One file per declaration | Yes | Strict rule: structure — File Organization |
| Bitmap type | `androidx.compose.ui.graphics.ImageBitmap` | Already a project dependency; KMP-compatible |
| Canvas type | `androidx.compose.ui.graphics.Canvas` | Wraps `ImageBitmap` via `Canvas(bitmap)` |
| Points / geometry | `androidx.compose.ui.geometry.Offset`, `androidx.compose.ui.geometry.Rect`, `androidx.compose.ui.unit.IntSize` | Already used in project |
| Color | `androidx.compose.ui.graphics.Color` | Existing project type |
| Blend mode | `androidx.compose.ui.graphics.BlendMode` | Used for `BlendMode.Clear` in pixel eraser |
| Path drawing | `androidx.compose.ui.graphics.Path` + `createPath` from SP_UTL | Reuses existing bezier helper |
| Coordinate space | **Logical pixels** (not [0..1] normalized) | v2 design decision from spike; `logicalSize` is the coordinate reference. Note: v1 PathWrapper normalization rule does NOT apply to engine package — engine types use logical pixels by design |
| Flood fill pixel access | `ImageBitmap.toPixelMap()` for reads; `Canvas.drawRect(1×1)` for writes | Only KMP-safe pixel access API; flag for performance profiling on large canvases |
| Path simplification | Ramer-Douglas-Peucker (inline in `PathSimplifier`) | Industry-standard; tolerance tunable per tool |
| Error guards | `require()` for call-order violations; silent no-op for operational errors | rule: error-handling |
| Object init style | `.apply { }` builder pattern for `Paint`, `Path` | rule: style — Kotlin Idioms |
| Public API visibility | `DrawAction`, `DrawTool`, `ToolContext`, `PaintOptions`, `ShapeType` are `public`; `CanvasManager` is `internal` | rule: architecture — Public vs Internal API |

---

## Progress

- [x] Phase 1 — Core interfaces and data types
- [x] Phase 2 — Built-in DrawAction implementations
- [x] Phase 3 — Path simplification utility
- [x] Phase 4 — Built-in DrawTool implementations
- [x] Phase 5 — CanvasManager
- [x] Phase 6 — DrawEngine facade

---

## Phases

### Phase 1 — Core interfaces and data types [DONE]

**Depends on:** none
**Implements:** [SP_ENG_01_01](draw_engine.sp.md#SP_ENG_01_01), [SP_ENG_01_02](draw_engine.sp.md#SP_ENG_01_02), [SP_ENG_01_03](draw_engine.sp.md#SP_ENG_01_03), [SP_ENG_01_04](draw_engine.sp.md#SP_ENG_01_04)

Files to create:

| File | Declaration | Visibility |
|------|-------------|------------|
| `engine/PaintOptions.kt` | `data class PaintOptions(color, strokeWidth, opacity)` | public |
| `engine/ToolContext.kt` | `data class ToolContext(paintOptions, committedBitmap, logicalSize)` | public |
| `engine/DrawAction.kt` | `interface DrawAction` | public |
| `engine/DrawTool.kt` | `interface DrawTool` | public |
| `engine/action/ShapeType.kt` | `enum class ShapeType { Line, Rectangle, Oval }` | public |

Notes:
- `PaintOptions`: add `init { require(strokeWidth > 0)` and `opacity = opacity.coerceIn(0f, 1f)` }` — per SP_ENG_04
- `DrawAction.renderLastSegment` has a default implementation: `renderLastSegment(canvas) = render(canvas)`
- `DrawTool.clearsActiveCanvasOnDrag` has a default: `false`
- `ToolContext.committedBitmap` is typed as `ImageBitmap`; the caller (DrawEngine) passes a copy so the tool cannot mutate the engine's internal bitmap

### Phase 2 — Built-in DrawAction implementations [DONE]

**Depends on:** Phase 1
**Implements:** [SP_ENG_01_07](draw_engine.sp.md#SP_ENG_01_07)

Files to create:

| File | Declaration |
|------|-------------|
| `engine/action/PathAction.kt` | `class PathAction(points, color, strokeWidth, opacity, blendMode)` |
| `engine/action/FillAction.kt` | `class FillAction(filledBitmap)` |
| `engine/action/ShapeAction.kt` | `class ShapeAction(shapeType, start, end, color, strokeWidth, opacity)` |
| `engine/action/StrokeEraseAction.kt` | `class StrokeEraseAction(removedIndices: List<Int>)` |

Implementation notes:

**PathAction:**
- `render(canvas)`: call `createPath(points)` (from `util/Util.kt`) then `canvas.drawPath(path, paint)` where paint uses color, strokeWidth, opacity, blendMode, `StrokeCap.Round`, `StrokeJoin.Round`, `PaintingStyle.Stroke`
- `renderLastSegment(canvas)`: if `points.size < 2`, call `render`; else draw segment from `points[size-2]` to `points[size-1]` using `canvas.drawLine` (or `canvas.drawPath` with 2-point path)
- `getBounds()`: `Rect(minX - sw/2, minY - sw/2, maxX + sw/2, maxY + sw/2)` over all points
- Seal `points` as `ImmutableList` or `List` (copy at construction to prevent external mutation)

**FillAction:**
- `render(canvas)`: `canvas.drawImage(filledBitmap, Offset.Zero, Paint())`
- `renderLastSegment`: delegates to `render`
- `getBounds()`: return `null` (fill regions are not tracked at pixel level)

**ShapeAction:**
- `render(canvas)`:
  - `Line` → `canvas.drawLine(start, end, paint)`
  - `Rectangle` → `canvas.drawRect(Rect(topLeft, bottomRight), paint)`
  - `Oval` → `canvas.drawOval(Rect(topLeft, bottomRight), paint)`
- `renderLastSegment`: delegates to `render` (shape always re-renders fully; engine clears via `clearsActiveCanvasOnDrag`)
- `getBounds()`: axis-aligned bounding rect of start/end + strokeWidth/2 expansion

**StrokeEraseAction:**
- `render(canvas)`: no-op
- `getBounds()`: return `null`
- `removedIndices` stored as `List<Int>` (immutable copy at construction)

### Phase 3 — Path simplification utility [DONE]

**Depends on:** none (pure math — no engine types needed)
**Implements:** [SP_ENG_01_08](draw_engine.sp.md#SP_ENG_01_08) (BrushTool behaviour — `onGestureEnd` simplifies)

File to create:

| File | Declaration |
|------|-------------|
| `engine/util/PathSimplifier.kt` | `internal object PathSimplifier` |

Implementation notes:
- `PathSimplifier.simplify(points: List<Offset>, tolerance: Float): List<Offset>`
- Implements Ramer-Douglas-Peucker (recursive perpendicular-distance split)
- Returns input list unchanged if `points.size <= 2`
- Default tolerance: `1.0f` logical pixel — exposed as parameter so tools can tune it
- Keep `internal` — not part of the public engine API

Pseudocode sketch:
```
FUNCTION simplify(points, tolerance):
    IF points.size <= 2: RETURN points
    maxDist = 0; maxIdx = 0
    FOR i IN 1..points.size-2:
        dist = perpendicularDistance(points[i], points[0], points[last])
        IF dist > maxDist: maxDist = dist; maxIdx = i
    IF maxDist > tolerance:
        left  = simplify(points[0..maxIdx], tolerance)
        right = simplify(points[maxIdx..last], tolerance)
        RETURN left + right.drop(1)
    ELSE:
        RETURN [points[0], points[last]]
```

### Phase 4 — Built-in DrawTool implementations [DONE]

**Depends on:** Phase 1, Phase 2, Phase 3
**Implements:** [SP_ENG_01_08](draw_engine.sp.md#SP_ENG_01_08)

Files to create:

| File | Declaration |
|------|-------------|
| `engine/tool/BrushTool.kt` | `object BrushTool : DrawTool` |
| `engine/tool/PixelEraserTool.kt` | `object PixelEraserTool : DrawTool` |
| `engine/tool/StrokeEraserTool.kt` | `object StrokeEraserTool : DrawTool` |
| `engine/tool/FillTool.kt` | `object FillTool : DrawTool` |
| `engine/tool/EyedropperTool.kt` | `class EyedropperTool(val onColorPicked: (Color) -> Unit) : DrawTool` |
| `engine/tool/ShapeTool.kt` | `class ShapeTool(val shapeType: ShapeType) : DrawTool` |

Implementation notes:

**BrushTool / PixelEraserTool:**
- `onGestureStart(point, ctx)` → `PathAction(points=[point], color, strokeWidth, opacity, blendMode)`
- `onGestureDrag(from, to, current, ctx)` → new `PathAction` with `points = current.points + to` (or mutable accumulation strategy — see note)
- `onGestureEnd(current, ctx)` → `PathAction` with `PathSimplifier.simplify(current.points, tolerance=1f)`
- `onTap(point, ctx)` → `PathAction(points=[point, point])` (two identical points → single dot rendered)
- Note: returning a new `PathAction` on every drag creates N allocations for an N-point stroke. An alternative is to use a mutable builder pattern during gesture and produce an immutable action only at `onGestureEnd`. For the spec contract, the returned `DrawAction` from each drag callback is what the engine uses for `renderLastSegment`. If we use a builder, `renderLastSegment` must be called on the same mutable instance. Decision: use a mutable `PointsAccumulator` list inside `BrushTool.onGestureDrag` return value — each returned `PathAction` holds a reference to the same growing list (copy-on-end). Document this in the implementation; it is an optimization, not a spec deviation.

**StrokeEraserTool:**
- Tracks erase bounding rect across gesture (via internal var in returned "accumulator" action — see `StrokeEraseAccumulator` note below, or use a local `MutableList<Offset>` captured across calls)
- Problem: tools are stateless — no instance state. Solution: a private marker action class `StrokeEraseInProgress(erasePoints: MutableList<Offset>)` that is returned during gesture and finalized in `onGestureEnd`
- `onGestureEnd(current, ctx)`: compute bounding rect of all erase points; find all committed actions whose `getBounds()` intersect the erase rect; return `StrokeEraseAction(removedIndices=[...])`
- Note: `current` in `onGestureEnd` is typed as `DrawAction?` but will be the private `StrokeEraseInProgress` instance — cast with `as? StrokeEraseInProgress` internally
- `onTap`: same as `onGestureEnd` for a single point (0-radius erase region)

**FillTool:**
- `onTap(point, ctx)`: `pixelMap = ctx.committedBitmap.toPixelMap()`; BFS flood fill from `point`; rasterize filled region into new `ImageBitmap(ctx.logicalSize)`; return `FillAction(filledBitmap)`
- All other callbacks: return `null`
- BFS reads seed color at `point`; fills all contiguous pixels with same color (within tolerance `epsilon=8` per channel); writes fill color from `ctx.paintOptions.color`
- Performance flag: BFS over a large canvas is O(W×H); acceptable for interactive use but should be tested on 4K export sizes

**EyedropperTool:**
- `onTap(point, ctx)`: read color from `ctx.committedBitmap.toPixelMap()` at `point`; call `onColorPicked(color)`; return `null`
- All other callbacks: return `null`

**ShapeTool:**
- `clearsActiveCanvasOnDrag = true` — each drag replaces the shape on active canvas
- `onGestureStart(point, ctx)` → `ShapeAction(shapeType, start=point, end=point, ...)`
- `onGestureDrag(from, to, current, ctx)` → `ShapeAction(shapeType, start=current.start, end=to, ...)`
- `onGestureEnd(current, ctx)` → `current` (no simplification needed)
- `onTap` → `null`

### Phase 5 — CanvasManager [DONE]

**Depends on:** Phase 1, Phase 2
**Implements:** [SP_ENG_01_05](draw_engine.sp.md#SP_ENG_01_05), [SP_ENG_02_05](draw_engine.sp.md#SP_ENG_02_05) through [SP_ENG_02_10](draw_engine.sp.md#SP_ENG_02_10), [SP_ENG_03_02](draw_engine.sp.md#SP_ENG_03_02)

File to create:

| File | Declaration |
|------|-------------|
| `engine/CanvasManager.kt` | `internal class CanvasManager(logicalSize: IntSize, undoDepth: Int)` |

Internal state:
```
private val checkpointBitmap: ImageBitmap = ImageBitmap(w, h)
private val committedBitmap: ImageBitmap  = ImageBitmap(w, h)
private val activeBitmap: ImageBitmap     = ImageBitmap(w, h)
private val drawnActions: MutableList<DrawAction> = mutableListOf()
private val suppressedIndices: MutableSet<Int>    = mutableSetOf()
private var currentIndex: Int    = -1
private var checkpointIndex: Int = -1
```

Key method sketches:

`commit(action)`:
```
drawnActions.subList(currentIndex + 1, drawnActions.size).clear()
drawnActions.add(action)
currentIndex++
Canvas(committedBitmap).apply { action.render(this) }
advanceCheckpointIfNeeded()
```

`commitStrokeErase(action: StrokeEraseAction)`:
```
drawnActions.subList(currentIndex + 1, drawnActions.size).clear()
suppressedIndices.addAll(action.removedIndices)
drawnActions.add(action)
currentIndex++
replayFromCheckpoint()
advanceCheckpointIfNeeded()
```

`undo()`:
```
if (currentIndex < 0) return
val undone = drawnActions[currentIndex]
if (undone is StrokeEraseAction) suppressedIndices.removeAll(undone.removedIndices)
currentIndex--
replayFromCheckpoint()
```

`redo()`:
```
if (currentIndex >= drawnActions.size - 1) return
currentIndex++
val redone = drawnActions[currentIndex]
if (redone is StrokeEraseAction) {
    suppressedIndices.addAll(redone.removedIndices)
    replayFromCheckpoint()
} else {
    Canvas(committedBitmap).apply { redone.render(this) }
}
advanceCheckpointIfNeeded()
```

`replayFromCheckpoint()`:
```
if (currentIndex >= checkpointIndex) {
    copyBitmap(src=checkpointBitmap, dst=committedBitmap)
    for i in (checkpointIndex+1)..currentIndex:
        if i not in suppressedIndices:
            Canvas(committedBitmap).apply { drawnActions[i].render(this) }
} else {
    clearBitmap(committedBitmap)
    for i in 0..currentIndex:
        if i not in suppressedIndices:
            Canvas(committedBitmap).apply { drawnActions[i].render(this) }
}
```

`advanceCheckpointIfNeeded()`:
```
while (currentIndex - checkpointIndex > undoDepth):
    checkpointIndex++
    if checkpointIndex not in suppressedIndices:
        Canvas(checkpointBitmap).apply { drawnActions[checkpointIndex].render(this) }
```

Notes:
- Bitmap copy (`copyBitmap`) and clear (`clearBitmap`) are private helpers:
  `copyBitmap`: draw src onto dst via `Canvas(dst).drawImage(src, Offset.Zero, Paint())`
  `clearBitmap`: `Canvas(dst).drawRect(full rect, Paint().apply { blendMode = BlendMode.Clear })`
- `getDisplayOutput()`: return a composed bitmap — draw `committedBitmap` then `activeBitmap` onto a fresh bitmap. Or return the two bitmaps and let DrawEngine/C_CTL_v2 composite them. Decision: return a `Pair<ImageBitmap, ImageBitmap>` (committed, active) and let DrawEngine compose — avoids an extra bitmap allocation on every frame
- `clearActiveBitmap()` and `getActiveBitmap()`: package-private helpers for DrawEngine to paint/clear the active canvas during gesture

### Phase 6 — DrawEngine facade [DONE]

**Depends on:** Phase 4, Phase 5
**Implements:** [SP_ENG_01_06](draw_engine.sp.md#SP_ENG_01_06), [SP_ENG_02_01](draw_engine.sp.md#SP_ENG_02_01) through [SP_ENG_02_14](draw_engine.sp.md#SP_ENG_02_14), [SP_ENG_03_01](draw_engine.sp.md#SP_ENG_03_01), [SP_ENG_04](draw_engine.sp.md#SP_ENG_04)

File to create:

| File | Declaration |
|------|-------------|
| `engine/DrawEngine.kt` | `class DrawEngine(val logicalSize: IntSize, val undoDepth: Int = 20)` |

Internal state:
```
private val canvasManager = CanvasManager(logicalSize, undoDepth)
private var activeTool: DrawTool? = null
private var activeContext: ToolContext? = null
private var partialAction: DrawAction? = null
```

Method sketches:

`beginGesture(tool, point, context)`:
```
require(activeTool == null) { "Gesture already active" }
activeTool = tool; activeContext = context
partialAction = tool.onGestureStart(point, context)
canvasManager.clearActiveBitmap()
partialAction?.render(Canvas(canvasManager.activeBitmap))
```

`extendGesture(from, to)`:
```
val tool = activeTool ?: return
val newPartial = tool.onGestureDrag(from, to, partialAction, activeContext!!)
if (tool.clearsActiveCanvasOnDrag) canvasManager.clearActiveBitmap()
newPartial?.renderLastSegment(Canvas(canvasManager.activeBitmap))
partialAction = newPartial
```

`endGesture()`:
```
val tool = activeTool ?: return
val final = tool.onGestureEnd(partialAction, activeContext!!)
canvasManager.clearActiveBitmap()
when (final) {
    is StrokeEraseAction -> canvasManager.commitStrokeErase(final)
    null -> { /* no-op */ }
    else -> canvasManager.commit(final)
}
activeTool = null; activeContext = null; partialAction = null
```

`getDisplayOutput()`:
```
val result = ImageBitmap(logicalSize.width, logicalSize.height)
val canvas = Canvas(result)
canvas.drawImage(canvasManager.committedBitmap, Offset.Zero, Paint())
canvas.drawImage(canvasManager.activeBitmap, Offset.Zero, Paint())
return result
```

`exportBitmap(size)`:
```
val result = ImageBitmap(size.width, size.height)
val canvas = Canvas(result)
canvas.scale(size.width / logicalSize.width.toFloat(), size.height / logicalSize.height.toFloat())
for i in 0..canvasManager.currentIndex:
    if i not in canvasManager.suppressedIndices:
        canvasManager.drawnActions[i].render(canvas)
return result
```

Notes:
- `canUndo` / `canRedo`: simple delegation to CanvasManager state
- `reset()`: `require(activeTool == null)`; delegates to `canvasManager.reset()` which blanks all bitmaps and resets all indices
- `DrawEngine` is NOT thread-safe; all calls must be from the main/UI thread — document with `@MainThread` annotation if available in KMP; otherwise document in KDoc

---

## Backlog

- **Bitmap pooling:** `getDisplayOutput()` allocates a new `ImageBitmap` on every call. A two-bitmap swap pool could reduce GC pressure during animation. Defer until profiling confirms it's a bottleneck.
- **Async flood fill:** FillTool BFS is synchronous on the main thread. For large canvases (>1024×1024), this may cause a dropped frame. Future work: run BFS in a coroutine, show a preview fill color during computation.
- **Export at source resolution:** `exportBitmap` scales via `canvas.scale()`. Non-integer scale factors may alias. Future option: run BFS / Douglas-Peucker at target resolution.
- **Layer system:** C_ENG_03_02 edge case — "very long history" suggests a future `flattenHistory()` that collapses history into a single committed snapshot. Out of scope.
- **Spray / Eyedropper drag tracking:** `EyedropperTool` currently only supports `onTap`. Live color preview during drag (drag eyedropper across canvas) is out of scope for v1 of C_ENG.

---

## Changelog

| Date | Change |
|------|--------|
| 2026-05-27 | Initial version — created from SP_ENG |
