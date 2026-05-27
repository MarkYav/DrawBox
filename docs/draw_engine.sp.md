# Draw Engine — Specification  {#SP_ENG}

> **Code:** SP_ENG
> **Status:** active
> **Created:** 2026-05-27
> **Updated:** 2026-05-27
>
> **Concept:** [C_ENG](draw_engine.concept.md)
> **Depends on specs:** [SP_UTL](util.sp.md)
> **Used by specs:** SP_CTL_v2 *(not yet written)*
> **Plan:** [draw_engine.plan.md](draw_engine.plan.md)
>
> Defines all data structures, interfaces, contracts, state transitions,
> and invariants for the Draw Engine module (C_ENG). The engine owns drawing
> action history, three-bitmap rendering, undo/redo, and the open extension
> interfaces (DrawAction, DrawTool).

---

## 01 — Data Structures  {#SP_ENG_01}

> Implements: [C_ENG_02](draw_engine.concept.md#C_ENG_02)

### 01_01. PaintOptions  {#SP_ENG_01_01}

Immutable snapshot of the user's paint settings, captured at gesture start.

```
class PaintOptions(
    val color:       Color,
    val strokeWidth: Float,   // logical pixels; > 0
    opacity:         Float,   // constructor param (not val); clamped at property initializer
) {
    val opacity: Float = opacity.coerceIn(0f, 1f)   // [0.0 .. 1.0] — always clamped
}
```

Invariants:
- `strokeWidth > 0`
- `opacity` is clamped to `[0.0, 1.0]` — values outside this range are rejected

### 01_02. ToolContext  {#SP_ENG_01_02}

Immutable snapshot of the drawing environment at gesture start.
Valid for the lifetime of one gesture only.

```
data class ToolContext(
    val paintOptions:     PaintOptions,
    val committedBitmap:  ImageBitmap,   // read-only view of committed canvas
    val logicalSize:      IntSize,
)
```

Invariants:
- `committedBitmap` is a snapshot taken at gesture start; it does not update mid-gesture
- `logicalSize.width > 0` and `logicalSize.height > 0`

### 01_03. DrawAction (open interface)  {#SP_ENG_01_03}

A completed or partially-completed drawing operation that knows how to render itself.

```
interface DrawAction {
    fun render(canvas: Canvas)
    fun renderLastSegment(canvas: Canvas)   // default implementation: calls render(canvas)
    fun getBounds(): Rect?                  // null means "covers entire canvas" (e.g., fill)
}
```

Invariants:
- `render` must be idempotent — calling it N times on a fresh canvas produces the same result as calling it once
- `renderLastSegment` paints only the newest segment appended since the last `renderLastSegment` call on the same active canvas pass; for non-incremental actions (shapes, fill) the default implementation is correct
- `getBounds` returns coordinates in logical pixel space; `null` is a valid "unknown / full-canvas" sentinel

### 01_04. DrawTool (open interface — stateless)  {#SP_ENG_01_04}

A stateless gesture strategy. Holds no mutable state between gestures.

```
interface DrawTool {
    val clearsActiveCanvasOnDrag: Boolean   // default: false
    fun onGestureStart(point: Offset, context: ToolContext): DrawAction?
    fun onGestureDrag(from: Offset, to: Offset, current: DrawAction?, context: ToolContext): DrawAction?
    fun onGestureEnd(current: DrawAction?, context: ToolContext): DrawAction?
    fun onTap(point: Offset, context: ToolContext): DrawAction?
}
```

Invariants:
- All implementations are stateless across calls — every piece of mutable state lives in the returned `DrawAction`, not in the tool
- Returning `null` from any callback means "no action for this event"
- When `clearsActiveCanvasOnDrag = true`, the engine clears the active canvas before calling `renderLastSegment` on each drag event (used by shape tools whose visual replaces rather than accumulates)

### 01_05. CanvasManager (internal)  {#SP_ENG_01_05}

Private rendering subsystem. Never exposed outside the engine package.

Internal state:

| Field | Type | Description |
|-------|------|-------------|
| `logicalSize` | `IntSize` | Bitmap dimensions; set at construction |
| `undoDepth` | `Int` | Max steps between checkpoint and current; ≥ 1 |
| `checkpointBitmap` | `ImageBitmap` | Canvas state at `checkpointIndex` |
| `committedBitmap` | `ImageBitmap` | Canvas state at `currentIndex` |
| `activeBitmap` | `ImageBitmap` | In-progress gesture; cleared on commit |
| `drawnActions` | `MutableList<DrawAction>` | All committed actions in order |
| `suppressedIndices` | `MutableSet<Int>` | Indices in `drawnActions` suppressed by a StrokeEraseAction |
| `eraseRemovals` | `MutableMap<Int, List<Int>>` | Maps each StrokeEraseAction index → list of indices it suppressed; enables undo |
| `currentIndex` | `Int` | Index of last live action; −1 = nothing committed |
| `checkpointIndex` | `Int` | Index at which `checkpointBitmap` was taken; −1 = blank canvas |

Invariants:
- `checkpointIndex <= currentIndex` at all times
- `currentIndex - checkpointIndex <= undoDepth` after every commit and redo
- `committedBitmap` = replay of all non-suppressed `drawnActions[0..currentIndex]`
- `checkpointBitmap` = replay of all non-suppressed `drawnActions[0..checkpointIndex]`
- `activeBitmap` is blank when no gesture is in progress

### 01_06. DrawEngine (public facade exposed to C_CTL_v2)  {#SP_ENG_01_06}

```
class DrawEngine(
    val logicalSize: IntSize,
    val undoDepth:   Int = 20,    // > 0
)
```

Internal state:

| Field | Type | Description |
|-------|------|-------------|
| `canvasManager` | `CanvasManager` | Private |
| `activeTool` | `DrawTool?` | Tool currently handling a gesture; null when idle |
| `activeContext` | `ToolContext?` | Context snapshot for current gesture |
| `partialAction` | `DrawAction?` | Partial action built during gesture |

---

### 01_07. Built-in DrawAction Implementations  {#SP_ENG_01_07}

#### PathAction

Smooth bezier stroke through a list of points. Used by BrushTool and PixelEraserTool.

```
class PathAction(
    val points:      List<Offset>,   // logical pixels; simplified on commit
    val color:       Color,
    val strokeWidth: Float,
    val opacity:     Float,
    val blendMode:   BlendMode,      // SrcOver for brush; Clear for pixel eraser
)
```

- `render(canvas)`: calls `createPath(points)` ([SP_UTL](util.sp.md)) then `canvas.drawPath` with paint derived from fields
- `renderLastSegment(canvas)`: draws only the segment from `points[points.size-2]` to `points[points.size-1]`; if `points.size < 2`, calls `render`
- `getBounds()`: axis-aligned bounding rect of all points expanded by `strokeWidth / 2`

#### FillAction

Result of a flood fill operation, rasterized at commit time.

```
class FillAction(
    val filledBitmap: ImageBitmap,   // same dimensions as logicalSize; pre-rasterized
)
```

- `render(canvas)`: draws `filledBitmap` at `(0, 0)` with `BlendMode.SrcOver`
- `renderLastSegment(canvas)`: same as `render` (fill is never incremental)
- `getBounds()`: returns `null` (fill may cover any region — bounding box not tracked)

#### ShapeAction

Geometric shape parameterized by type and two bounding points.

```
class ShapeAction(
    val shapeType:   ShapeType,   // Line | Rectangle | Oval
    val start:       Offset,
    val end:         Offset,
    val color:       Color,
    val strokeWidth: Float,
    val opacity:     Float,
)

enum class ShapeType { Line, Rectangle, Oval }
```

- `render(canvas)`: draws the appropriate shape using start/end; rectangle and oval are axis-aligned
- `renderLastSegment(canvas)`: same as `render` (shape always redraws fully — engine uses `clearsActiveCanvasOnDrag=true`)
- `getBounds()`: axis-aligned bounding rect of `start`..`end` expanded by `strokeWidth / 2`

#### StrokeEraseAction

Records the spatial region swept by the stroke eraser. CanvasManager computes which previously
committed actions intersect that region at commit time and suppresses them.

```
class StrokeEraseAction(
    val eraseRect: Rect,   // bounding rect of all drag points, expanded by ERASE_RADIUS
)
```

- `render(canvas)`: no-op (renders nothing; suppression is computed and applied by CanvasManager)
- `getBounds()`: returns `null`
- Committed via a dedicated `CanvasManager.commitStrokeErase(action)` contract (see 02_06)

> **Implementation note (intentional deviation from draft spec):** The original draft specified
> `StrokeEraseAction(removedIndices)` — requiring the tool to pre-compute which actions to remove.
> The implementation instead passes only `eraseRect` and lets CanvasManager compute intersections
> at commit time. This keeps the tool free from action-history access and simplifies undo/redo
> bookkeeping (CanvasManager records the computed `removedIndices` in `eraseRemovals` keyed by
> the action's index in `drawnActions`).

---

### 01_08. Built-in DrawTool Implementations  {#SP_ENG_01_08}

| Tool | `clearsActiveCanvasOnDrag` | Action produced | Gesture support |
|------|--------------------------|-----------------|-----------------|
| `BrushTool` | false | `PathAction` (blendMode=SrcOver) | start / drag / end / tap |
| `PixelEraserTool` | false | `PathAction` (blendMode=Clear) | start / drag / end / tap |
| `StrokeEraserTool` | false | `StrokeEraseAction` | start / drag / end |
| `FillTool` | — | `FillAction` | tap only |
| `EyedropperTool(onColorPicked)` | — | null (side-effect only) | tap only |
| `ShapeTool(type: ShapeType)` | true | `ShapeAction` | start / drag / end |

**BrushTool / PixelEraserTool:**
- `onGestureStart`: creates `PathAction` with `points=[point]`
- `onGestureDrag`: appends `to` to `points`; returns updated action (unsimplified)
- `onGestureEnd`: applies Douglas-Peucker simplification to `points`; returns simplified action
- `onTap`: creates `PathAction` with `points=[point]` (single-point dot)

**StrokeEraserTool:**
- Uses a private `StrokeEraseInProgress` accumulator class (implements `DrawAction`; renders as no-op)
  to carry the growing list of drag points across `onGestureStart` / `onGestureDrag` calls without
  storing mutable state in the tool object itself
- `onGestureStart`: returns `StrokeEraseInProgress(mutableListOf(point))`
- `onGestureDrag`: appends `to` to the accumulator's point list; returns updated accumulator
- `onGestureEnd`: computes `eraseRect` (bounding box of all accumulated points ± `ERASE_RADIUS`);
  returns `StrokeEraseAction(eraseRect)`; CanvasManager identifies and suppresses intersecting actions
- `onTap`: returns `StrokeEraseAction` with a small rect centred on the tap point (radius = `ERASE_RADIUS`)

> **Implementation note (intentional deviation from draft spec):** The original draft specified
> that `onGestureStart` / `onGestureDrag` return `null`. The implementation returns a
> `StrokeEraseInProgress` accumulator so that gesture state can be passed through the
> `current: DrawAction?` parameter without storing mutable fields in the tool singleton.
> `StrokeEraseInProgress` is a private class — it is not part of the public API.

**FillTool:**
- `onTap`: reads pixel color at `point` from `context.committedBitmap`; runs BFS flood fill to find contiguous same-colored region; rasterizes filled region into a new `ImageBitmap`; returns `FillAction`
- All gesture callbacks (`onGestureStart`, `onGestureDrag`, `onGestureEnd`) return null

**EyedropperTool:**
- Constructed with `onColorPicked: (Color) -> Unit`
- `onTap`: reads pixel color at `point` from `context.committedBitmap`; invokes `onColorPicked(color)`; returns null
- All gesture callbacks return null

**ShapeTool:**
- `onGestureStart`: creates `ShapeAction(start=point, end=point, ...)`
- `onGestureDrag`: returns updated `ShapeAction` with `end=to`
- `onGestureEnd`: returns final `ShapeAction`
- `onTap`: returns null (degenerate zero-size shape)

---

## 02 — Contracts  {#SP_ENG_02}

> Implements: [C_ENG_02_02](draw_engine.concept.md#C_ENG_02_02), [C_ENG_04_02](draw_engine.concept.md#C_ENG_04_02)

All DrawEngine operations are called from the main thread. Concurrent calls are not supported.

### 02_01. beginGesture  {#SP_ENG_02_01}

```
fun beginGesture(tool: DrawTool, point: Offset, context: ToolContext)
```

| | |
|-|-|
| Pre-condition | `activeTool == null` (no gesture in progress) |
| Side effects | `activeTool = tool`; `activeContext = context`; `partialAction = tool.onGestureStart(point, context)`; clears `activeBitmap`; if `partialAction != null`, calls `partialAction.render(activeBitmap)` |
| Error case: gesture already active | `require` throws — caller must not call `beginGesture` twice without `endGesture` |

### 02_02. extendGesture  {#SP_ENG_02_02}

```
fun extendGesture(from: Offset, to: Offset)
```

| | |
|-|-|
| Pre-condition | `activeTool != null` |
| Processing | `newPartial = activeTool.onGestureDrag(from, to, partialAction, activeContext)`; if `activeTool.clearsActiveCanvasOnDrag`, clears `activeBitmap`; if `newPartial != null`, calls `newPartial.renderLastSegment(activeBitmap)`; `partialAction = newPartial` |
| Side effects | `activeBitmap` updated incrementally (or fully if `clearsActiveCanvasOnDrag`) |
| Error case: no active gesture | silent no-op |

### 02_03. endGesture  {#SP_ENG_02_03}

```
fun endGesture()
```

| | |
|-|-|
| Pre-condition | `activeTool != null` |
| Processing | `finalAction = activeTool.onGestureEnd(partialAction, activeContext)`; clears `activeBitmap`; if `finalAction != null`, dispatches to `canvasManager.commit(finalAction)` or `canvasManager.commitStrokeErase(finalAction)` depending on type; resets `activeTool`, `activeContext`, `partialAction` to null |
| Error case: no active gesture | silent no-op |

### 02_04. onTap  {#SP_ENG_02_04}

```
fun onTap(tool: DrawTool, point: Offset, context: ToolContext)
```

| | |
|-|-|
| Pre-condition | `activeTool == null` |
| Processing | `action = tool.onTap(point, context)`; if `action != null`, dispatches to commit; active canvas is not affected |
| Error case: gesture already active | `require` throws |

### 02_05. CanvasManager.commit  {#SP_ENG_02_05}

```
internal fun CanvasManager.commit(action: DrawAction)
```

Processing logic:
```
REMOVE drawnActions[currentIndex+1 .. end]    // discard redo stack
APPEND action TO drawnActions
currentIndex++
APPLY action.render TO committedBitmap
CALL advanceCheckpointIfNeeded()
```

### 02_06. CanvasManager.commitStrokeErase  {#SP_ENG_02_06}

```
internal fun CanvasManager.commitStrokeErase(action: StrokeEraseAction)
```

Processing logic:
```
REMOVE drawnActions[currentIndex+1 .. end]    // discard redo stack
COMPUTE intersecting = { i ∈ [0..currentIndex] : drawnActions[i].getBounds() intersects action.eraseRect }
ADD intersecting TO suppressedIndices
STORE eraseRemovals[currentIndex+1] = intersecting.toList()   // keyed by the new action's index
APPEND action TO drawnActions
currentIndex++
CALL replayFromCheckpoint()                   // rebuild committedBitmap without suppressed actions
CALL advanceCheckpointIfNeeded()
```

`eraseRemovals` is a `MutableMap<Int, List<Int>>` in CanvasManager that records which indices
were suppressed by each `StrokeEraseAction` (keyed by that action's index in `drawnActions`).
This enables clean undo: when a `StrokeEraseAction` is undone, its recorded `removedIndices`
are removed from `suppressedIndices`.

### 02_07. CanvasManager.undo  {#SP_ENG_02_07}

```
internal fun CanvasManager.undo()
```

| | |
|-|-|
| Pre-condition | `currentIndex >= 0` |
| Processing | Decrements `currentIndex`; rebuilds `committedBitmap` using `replayFromCheckpoint()` |
| Side effect: StrokeEraseAction undone | removes its `removedIndices` from `suppressedIndices` |
| Error case: nothing to undo | silent no-op |

### 02_08. CanvasManager.redo  {#SP_ENG_02_08}

```
internal fun CanvasManager.redo()
```

| | |
|-|-|
| Pre-condition | `currentIndex < drawnActions.size - 1` |
| Processing | `currentIndex++`; applies `drawnActions[currentIndex]` to `committedBitmap`; if the redone action is a `StrokeEraseAction`, re-adds its `removedIndices` to `suppressedIndices` and calls `replayFromCheckpoint()`; calls `advanceCheckpointIfNeeded()` |
| Error case: nothing to redo | silent no-op |

### 02_09. CanvasManager.replayFromCheckpoint  {#SP_ENG_02_09}

Internal helper — rebuilds `committedBitmap` from the checkpoint.

```
RESET committedBitmap:
    IF currentIndex >= checkpointIndex:
        COPY checkpointBitmap → committedBitmap
        FOR i IN [checkpointIndex+1 .. currentIndex]:
            IF i NOT IN suppressedIndices:
                drawnActions[i].render(committedBitmap)
    ELSE:
        CLEAR committedBitmap (blank canvas)
        FOR i IN [0 .. currentIndex]:
            IF i NOT IN suppressedIndices:
                drawnActions[i].render(committedBitmap)
```

### 02_10. CanvasManager.advanceCheckpointIfNeeded  {#SP_ENG_02_10}

Internal helper — keeps checkpoint exactly `undoDepth` steps behind current.

```
WHILE currentIndex - checkpointIndex > undoDepth:
    checkpointIndex++
    IF checkpointIndex NOT IN suppressedIndices:
        drawnActions[checkpointIndex].render(checkpointBitmap)
```

### 02_11. DrawEngine.reset  {#SP_ENG_02_11}

```
fun reset()
```

Clears all state: `drawnActions`, `suppressedIndices`, `currentIndex = -1`, `checkpointIndex = -1`.
Blanks all three bitmaps. Resets `activeTool`, `partialAction`, `activeContext` to null.
Error case: called during active gesture — `require` throws; caller must end gesture first.

### 02_12. DrawEngine.canUndo / canRedo  {#SP_ENG_02_12}

```
val canUndo: Boolean  →  currentIndex >= 0
val canRedo: Boolean  →  currentIndex < drawnActions.size - 1
```

### 02_13. DrawEngine.getDisplayOutput  {#SP_ENG_02_13}

```
fun getDisplayOutput(): ImageBitmap
```

Returns a composite of `committedBitmap` and `activeBitmap`. The composition renders
`committedBitmap` first, then `activeBitmap` on top. The returned bitmap is a snapshot —
it is not updated reactively. Callers (C_CTL_v2) request a new snapshot after each
state-changing operation.

### 02_14. DrawEngine.exportBitmap  {#SP_ENG_02_14}

```
fun exportBitmap(size: IntSize): ImageBitmap
```

Creates a new `ImageBitmap(size.width, size.height)`. Scales the drawing canvas to `size`
using `canvas.scale(size.width / logicalSize.width, size.height / logicalSize.height)`.
Replays all non-suppressed `drawnActions[0..currentIndex]` in order. Returns the result.
Active canvas is not included (export represents the committed state only).

---

## 03 — State Transitions  {#SP_ENG_03}

> Implements: [C_ENG_02_02](draw_engine.concept.md#C_ENG_02_02), [C_ENG_03_01](draw_engine.concept.md#C_ENG_03_01)

### 03_01. DrawEngine gesture state  {#SP_ENG_03_01}

```
[idle] ──beginGesture()──► [gesturing] ──extendGesture()*──► [gesturing] ──endGesture()──► [idle]
[idle] ──onTap()──────────────────────────────────────────────────────────────────────────► [idle]
[idle] ──undo() / redo() / reset()────────────────────────────────────────────────────────► [idle]
```

`undo`, `redo`, `reset`, and `onTap` are only valid in `[idle]`. `extendGesture` and `endGesture`
are only valid in `[gesturing]`. Violations throw via `require`.

### 03_02. CanvasManager currentIndex / checkpointIndex  {#SP_ENG_03_02}

| Event | currentIndex | checkpointIndex | committedBitmap |
|-------|-------------|-----------------|-----------------|
| initial | −1 | −1 | blank |
| commit(action) | +1 | unchanged or +1 if undoDepth exceeded | action applied |
| undo() | −1 | unchanged | rebuilt from checkpoint |
| redo() | +1 | unchanged or +1 if undoDepth exceeded | action applied |
| reset() | −1 | −1 | blank |

Checkpoint advancement rule: after any event that increases `currentIndex`,
if `currentIndex − checkpointIndex > undoDepth` → advance checkpoint by one step
(apply `drawnActions[checkpointIndex+1]` to `checkpointBitmap`, increment `checkpointIndex`).

---

## 04 — Validation Rules  {#SP_ENG_04}

- `PaintOptions.strokeWidth` must be `> 0`; reject with `IllegalArgumentException` at construction
- `PaintOptions.opacity` is clamped to `[0.0, 1.0]` at construction; no exception
- `DrawEngine.undoDepth` must be `>= 1`; reject with `IllegalArgumentException` at construction
- `DrawEngine.logicalSize` width and height must both be `> 0`; reject at construction
- Points passed to `beginGesture`, `extendGesture`, `onTap` may lie outside `[0, logicalSize]`; tools
  and actions clip as needed — the engine does not validate point bounds
- `StrokeEraseAction.eraseRect` is computed by the tool; CanvasManager intersects it against `drawnActions[0..currentIndex]` at commit time — no pre-validation of indices required

---

## 05 — Verification Criteria  {#SP_ENG_05}

### 05_01. Functional Expectations  {#SP_ENG_05_01}

| Contract | Scenario | Expected outcome |
|----------|----------|------------------|
| beginGesture + extendGesture × N + endGesture | Brush stroke | PathAction committed; `canUndo=true`; `committedBitmap` shows stroke |
| undo after commit | One stroke drawn | `canUndo=false`; `committedBitmap` reverts to pre-stroke state |
| redo after undo | Stroke undone | `canUndo=true`; `canRedo=false`; stroke re-appears |
| undo past checkpoint | More undos than `undoDepth` | Full replay from index 0; result identical to incremental replay |
| commit after undo | New stroke after undo | Redo stack cleared; `canRedo=false` |
| reset | Any state | All bitmaps blank; `canUndo=false`; `canRedo=false` |
| StrokeEraser over two strokes | Two PathActions in history intersect erase region | Both suppressed; `committedBitmap` replays without them; `canUndo=true` |
| Undo of StrokeEraseAction | StrokeEraseAction is current | Suppression removed; strokes re-appear in `committedBitmap` |
| FillTool tap | Empty canvas, tap center | `FillAction` committed; entire canvas filled with paint color |
| EyedropperTool tap | Committed stroke present at tap point | `onColorPicked` invoked with stroke color; no action committed |
| exportBitmap at 2× size | Three strokes committed | Returns `ImageBitmap` scaled 2×; strokes visually identical at higher resolution |

### 05_02. Invariant Checks  {#SP_ENG_05_02}

| Invariant | Verification method |
|-----------|---------------------|
| `checkpointIndex ≤ currentIndex` always | After every commit/undo/redo, assert `checkpointIndex ≤ currentIndex` |
| `currentIndex − checkpointIndex ≤ undoDepth` after commit/redo | Assert after each commit and redo |
| `committedBitmap` = full replay | After any state change, replay all non-suppressed actions from 0; compare pixels to `committedBitmap` |
| `activeBitmap` blank when idle | Assert all pixels are transparent after `endGesture` |
| Tool produces no mutable state | Call same tool method twice with same args; both calls return equivalent (equal) actions |

### 05_03. Integration Scenarios  {#SP_ENG_05_03}

| Scenario | Preconditions | Steps | Expected result |
|----------|--------------|-------|-----------------|
| Brush stroke rendered in DrawBox | DrawEngine connected to C_CTL_v2; C_CTL_v2 connected to DrawBoxCanvas | User drags finger; DrawBox re-renders after each extendGesture | Stroke appears incrementally during drag |
| Undo button triggers C_CTL_v2.undo | One stroke committed | Controller calls `engine.undo()`; requests `getDisplayOutput()`; triggers recomposition | Canvas shows blank state |
| PaintOptions change mid-stroke | BrushTool active gesture | User changes color between strokes (not mid-gesture) | Next stroke uses new color; current stroke unaffected (snapshot taken at start) |

### 05_04. Edge Cases  {#SP_ENG_05_04}

| Case | Expected behavior |
|------|-------------------|
| Tool returns null from `onGestureStart` | `extendGesture` passes `null` as `current`; `endGesture` commits nothing |
| Zero-duration tap on BrushTool | `onTap` produces single-point PathAction (dot at tap position) |
| StrokeEraser with no intersecting actions | `StrokeEraseAction(eraseRect=...)` committed; CanvasManager finds 0 intersecting actions; no visible change; undo removes the action |
| `undoDepth=1` | Checkpoint always one step behind current; undo always does O(1) checkpoint-restore |
| `undo()` when `currentIndex=−1` | Silent no-op |
| `redo()` when no redo stack | Silent no-op |
| `extendGesture` during no active gesture | Silent no-op |
| `endGesture` during no active gesture | Silent no-op |
| Very long stroke (1000+ points) | PathAction stores simplified points after commit; `renderLastSegment` stays O(1) during gesture |
| `exportBitmap` with non-integer scale factor | Canvas scale is applied via `canvas.scale(sx, sy)`; rendering artifacts are acceptable — not a spec constraint |

---

## 06 — Reversibility  {#SP_ENG_06}

> Implements: [C_ENG_01_02](draw_engine.concept.md#C_ENG_01_02)

### 06_01. Rollback Strategy  {#SP_ENG_06_01}

| Aspect | Rollback approach |
|--------|-------------------|
| Code | C_ENG lives in its own package. Removing the package and reverting C_CTL_v2 → C_CTL restores v1 behavior. No shared state is mutated. |
| Data | All state is in-memory. No database, no file, no persistence layer. Rollback = revert source + restart app. |
| Dependent specs | SP_CTL_v2 and SP_BOX_v2 depend on SP_ENG. Removing C_ENG requires those to revert to their v1 equivalents. |
| Public extension points | DrawAction and DrawTool are public interfaces. Library consumers who implemented them would need to update if interfaces change. Removing C_ENG entirely removes the interfaces — breaking change for consumers. |
| Mitigation | Interface stability: DrawAction and DrawTool must be treated as stable public API once v2 ships. Any change is a semver-breaking major version bump. |

---

## Changelog

| Date | Change |
|------|--------|
| 2026-05-27 | Post-implementation update — aligned with implementation: PaintOptions changed from data class to regular class (W-01); StrokeEraseAction changed from removedIndices to eraseRect (W-02); StrokeEraserTool accumulator pattern documented (W-03); eraseRemovals added to CanvasManager state table; commitStrokeErase contract updated |
| 2026-05-27 | Initial draft — created from C_ENG concept |
