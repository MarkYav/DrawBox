# Draw Controller v2 — Specification  {#SP_CTL_v2}

> **Code:** SP_CTL_v2
> **Status:** active
> **Created:** 2026-05-27
> **Updated:** 2026-05-27
>
> **Concept:** [C_CTL_v2](draw_controller_v2.concept.md)
> **Depends on specs:** [SP_ENG](draw_engine.sp.md)
> **Used by specs:** [SP_BOX_v2](draw_box_v2.sp.md)
> **Plan:** [draw_controller_v2.plan.md](draw_controller_v2.plan.md)
>
> Defines all data structures, contracts, state transitions, validation rules,
> and verification criteria for DrawController v2. The controller is the bridge
> between library consumers (paint settings, tool selection, undo/redo),
> the Compose rendering layer (gesture events, canvas size), and the Draw Engine
> (drawing algorithms, action history).

---

## 01 — Data Structures  {#SP_CTL_v2_01}

> Implements: [C_CTL_v2_02](draw_controller_v2.concept.md#C_CTL_v2_02)

### 01_01. DrawController  {#SP_CTL_v2_01_01}

The public entry point for library consumers and the composable layer.

**Construction parameters:**

| Parameter | Type | Required | Default | Constraints |
|-----------|------|----------|---------|-------------|
| `logicalSize` | `IntSize` | yes | — | `width > 0` and `height > 0` |
| `undoDepth` | `Int` | no | `20` | `>= 1` |
| `defaultTool` | `DrawTool` | no | `BrushTool` | non-null |

**Mutable observable state** (consumer-settable; each change is observable by the rendering layer):

| Field | Type | Default | Constraints |
|-------|------|---------|-------------|
| `color` | `Color` | `Color.Black` | — |
| `strokeWidth` | `Float` | `4.0` | `> 0`; values `<= 0` are clamped to `0.1` silently |
| `opacity` | `Float` | `1.0` | `[0.0, 1.0]`; values outside this range are clamped silently |
| `activeTool` | `DrawTool` | `defaultTool` | non-null; assignment during active gesture is deferred to next gesture |

**Read-only observable state** (derived; observable by the rendering layer and consumers):

| Field | Type | Derived from | Updated after |
|-------|------|-------------|---------------|
| `canUndo` | `Boolean` | `engine.canUndo` | `onGestureEnd`, `onTap`, `undo`, `redo`, `reset` |
| `canRedo` | `Boolean` | `engine.canRedo` | `onGestureEnd`, `onTap`, `undo`, `redo`, `reset` |
| `invalidationTick` | `Int` | monotonic counter | any operation that changes display output |

`invalidationTick` is a monotonically increasing integer. It is consumed exclusively by
C_BOX_v2's `DrawBoxCanvas` to know when to re-render. Consumers must not rely on its value.

**Internal state:**

| Field | Type | Description |
|-------|------|-------------|
| `engine` | `DrawEngine` | Owns all drawing state; created at construction |
| `connectionState` | `ConnectionState` | Current canvas connection; starts `Disconnected` |
| `gestureState` | `GestureState` | Current gesture state; starts `Idle` |

Invariants:
- `logicalSize` is immutable after construction
- `undoDepth` is immutable after construction
- `engine` is created once at construction with `logicalSize` and `undoDepth`; never replaced
- The tool active at `onGestureStart` is used for the entire gesture; mid-gesture `activeTool` changes take effect only after `onGestureEnd`

### 01_02. ConnectionState  {#SP_CTL_v2_01_02}

Tracks whether the controller has a canvas size for coordinate mapping.

```
Disconnected
Connected(screenSize: IntSize)
```

| State | Meaning |
|-------|---------|
| `Disconnected` | No canvas size reported; gesture events are ignored |
| `Connected(screenSize)` | Canvas size known; coordinate mapping is active |

Transitions: see [04_01](#SP_CTL_v2_04_01).

### 01_03. GestureState  {#SP_CTL_v2_01_03}

Tracks in-progress gesture and the tool/context locked for it.

```
Idle
Gesturing(
    tool:    DrawTool,      // locked at onGestureStart; does not update mid-gesture
    context: ToolContext,   // snapshot from engine at gesture start; does not update mid-gesture
)
```

| State | Meaning |
|-------|---------|
| `Idle` | No gesture in progress; undo/redo/reset/onTap accepted |
| `Gesturing(tool, context)` | Gesture in progress; undo/redo/reset/onTap are no-ops |

Transitions: see [04_02](#SP_CTL_v2_04_02).

---

## 02 — Contracts  {#SP_CTL_v2_02}

> Implements: [C_CTL_v2_02_02](draw_controller_v2.concept.md#C_CTL_v2_02_02),
> [C_CTL_v2_03](draw_controller_v2.concept.md#C_CTL_v2_03)

All operations are called from the main thread. Concurrent calls are not supported.

### 02_01. onCanvasSizeChanged  {#SP_CTL_v2_02_01}

Called by `DrawBoxCanvas` whenever its measured layout size changes.

```
fun onCanvasSizeChanged(size: IntSize)
```

| | |
|-|-|
| Pre-condition | none |
| Processing | IF `size.width <= 0` OR `size.height <= 0`: no-op. ELSE: IF `gestureState == Gesturing`: abort gesture (see 02_05 abort path). SET `connectionState = Connected(size)`. |
| Side effects | `connectionState` updated. If gesture was aborted: `gestureState = Idle`, `canUndo`/`canRedo` refreshed (engine may have committed the partial action), `invalidationTick` incremented. |

### 02_02. onGestureStart  {#SP_CTL_v2_02_02}

Called by `DrawBoxCanvas` on pointer-down.

```
fun onGestureStart(screenPoint: Offset)
```

| | |
|-|-|
| Pre-condition: disconnected | `connectionState == Disconnected` → no-op |
| Pre-condition: nested gesture | `gestureState == Gesturing` → no-op |
| Processing | `logicalPoint = mapToLogical(screenPoint)` (see 02_09). `paintOptions = PaintOptions(color, strokeWidth, opacity)`. `context = engine.createToolContext(paintOptions)`. `engine.beginGesture(activeTool, logicalPoint, context)` (see [SP_ENG_02_01](draw_engine.sp.md#SP_ENG_02_01)). `gestureState = Gesturing(activeTool, context)`. `invalidationTick++`. |
| Side effects | Gesture started in engine; `gestureState` updated; `invalidationTick` incremented. |

### 02_03. onGestureMove  {#SP_CTL_v2_02_03}

Called by `DrawBoxCanvas` on each pointer-move event.

```
fun onGestureMove(from: Offset, to: Offset)
```

| | |
|-|-|
| Pre-condition | `gestureState == Gesturing`; else no-op |
| Processing | `logicalFrom = mapToLogical(from)`. `logicalTo = mapToLogical(to)`. `engine.extendGesture(logicalFrom, logicalTo)` (see [SP_ENG_02_02](draw_engine.sp.md#SP_ENG_02_02)). `invalidationTick++`. |
| Side effects | Engine active canvas updated; `invalidationTick` incremented. |

### 02_04. onGestureEnd  {#SP_CTL_v2_02_04}

Called by `DrawBoxCanvas` on pointer-up or pointer-cancel.

```
fun onGestureEnd()
```

| | |
|-|-|
| Pre-condition | `gestureState == Gesturing`; else no-op |
| Processing | `engine.endGesture()` (see [SP_ENG_02_03](draw_engine.sp.md#SP_ENG_02_03)). `gestureState = Idle`. Refresh `canUndo`, `canRedo` from engine. `invalidationTick++`. |
| Side effects | Action committed in engine; `gestureState = Idle`; `canUndo`/`canRedo` refreshed; `invalidationTick` incremented. |

### 02_05. onTap  {#SP_CTL_v2_02_05}

Called by `DrawBoxCanvas` on a single-point tap gesture.

```
fun onTap(screenPoint: Offset)
```

| | |
|-|-|
| Pre-condition: disconnected | `connectionState == Disconnected` → no-op |
| Pre-condition: gesture in progress | `gestureState == Gesturing` → no-op |
| Processing | `logicalPoint = mapToLogical(screenPoint)`. `paintOptions = PaintOptions(color, strokeWidth, opacity)`. `context = engine.createToolContext(paintOptions)`. `engine.onTap(activeTool, logicalPoint, context)` (see [SP_ENG_02_04](draw_engine.sp.md#SP_ENG_02_04)). Refresh `canUndo`, `canRedo`. `invalidationTick++`. |
| Side effects | Action committed in engine (if tool produces one); `canUndo`/`canRedo` refreshed; `invalidationTick` incremented. |

### 02_06. undo  {#SP_CTL_v2_02_06}

```
fun undo()
```

| | |
|-|-|
| Pre-condition | `gestureState == Idle`; else no-op |
| Processing | `engine.undo()` (see [SP_ENG_02_07](draw_engine.sp.md#SP_ENG_02_07)). Refresh `canUndo`, `canRedo`. `invalidationTick++`. |
| Side effects | Engine history decremented; display updated; `canUndo`/`canRedo` refreshed; `invalidationTick` incremented. |

### 02_07. redo  {#SP_CTL_v2_02_07}

```
fun redo()
```

| | |
|-|-|
| Pre-condition | `gestureState == Idle`; else no-op |
| Processing | `engine.redo()` (see [SP_ENG_02_08](draw_engine.sp.md#SP_ENG_02_08)). Refresh `canUndo`, `canRedo`. `invalidationTick++`. |
| Side effects | Engine history advanced; display updated; `canUndo`/`canRedo` refreshed; `invalidationTick` incremented. |

### 02_08. reset  {#SP_CTL_v2_02_08}

```
fun reset()
```

| | |
|-|-|
| Pre-condition | `gestureState == Idle`; else no-op |
| Processing | `engine.reset()` (see [SP_ENG_02_11](draw_engine.sp.md#SP_ENG_02_11)). Refresh `canUndo`, `canRedo`. `invalidationTick++`. |
| Side effects | All engine state cleared; `canUndo = false`; `canRedo = false`; `invalidationTick` incremented. |

### 02_09. mapToLogical (internal)  {#SP_CTL_v2_02_09}

Internal coordinate mapping. Called at the start of every gesture and tap operation.

```
fun mapToLogical(screenPoint: Offset): Offset
```

Pre-condition: `connectionState == Connected(screenSize)` (callers guarantee this).

```
logicalX = screenPoint.x * logicalSize.width  / screenSize.width
logicalY = screenPoint.y * logicalSize.height / screenSize.height
RETURN Offset(logicalX, logicalY)
```

Output may be outside `[0, logicalSize]`. The engine accepts out-of-bounds logical points;
tools and actions clip as needed.

### 02_10. getDisplayOutput  {#SP_CTL_v2_02_10}

Returns the current composed image for rendering.

```
fun getDisplayOutput(): ImageBitmap
```

| | |
|-|-|
| Pre-condition | none |
| Processing | Delegates to `engine.getDisplayOutput()` (see [SP_ENG_02_13](draw_engine.sp.md#SP_ENG_02_13)). |
| Output | `ImageBitmap` at logical resolution; composed of committed canvas + active canvas. |
| Side effects | none |
| Note | The returned bitmap is a snapshot. It becomes stale the next time the engine state changes. Callers (DrawBoxCanvas) must call this again after each `invalidationTick` increment. |

### 02_11. exportBitmap  {#SP_CTL_v2_02_11}

Exports committed drawing at arbitrary resolution.

```
fun exportBitmap(size: IntSize): ImageBitmap
```

| | |
|-|-|
| Pre-condition | `size.width > 0` and `size.height > 0`; else throws `IllegalArgumentException` |
| Processing | Delegates to `engine.exportBitmap(size)` (see [SP_ENG_02_14](draw_engine.sp.md#SP_ENG_02_14)). |
| Output | `ImageBitmap` at `size` dimensions. Only committed actions included (active gesture excluded). |
| Side effects | none |
| Note | May be called during an active gesture. Result reflects committed state only. |

---

## 03 — Validation Rules  {#SP_CTL_v2_03}

> Implements: [C_CTL_v2_01_02](draw_controller_v2.concept.md#C_CTL_v2_01_02)

| Field / Operation | Rule | Violation response |
|-------------------|------|--------------------|
| `logicalSize.width` | must be `> 0` | `IllegalArgumentException` at construction |
| `logicalSize.height` | must be `> 0` | `IllegalArgumentException` at construction |
| `undoDepth` | must be `>= 1` | `IllegalArgumentException` at construction |
| `strokeWidth` (set) | must be `> 0`; values `<= 0` are silently clamped to `0.1` | clamp (no exception) |
| `opacity` (set) | `[0.0, 1.0]`; values outside are silently clamped | clamp (no exception) |
| `activeTool` (set) | null assignment is ignored; previous value retained | ignore (no exception) |
| `exportBitmap` size | `width > 0` and `height > 0` | `IllegalArgumentException` |
| `onCanvasSizeChanged` | size with `width <= 0` or `height <= 0` | no-op (no exception) |

`PaintOptions` construction from controller state always succeeds because `strokeWidth >= 0.1`
(clamped above) satisfies `SP_ENG_04`'s `strokeWidth > 0` requirement.

---

## 04 — State Transitions  {#SP_CTL_v2_04}

> Implements: [C_CTL_v2_03_01](draw_controller_v2.concept.md#C_CTL_v2_03_01)

### 04_01. ConnectionState Transitions  {#SP_CTL_v2_04_01}

```
Disconnected ──onCanvasSizeChanged(valid size)──────► Connected(size)
Connected    ──onCanvasSizeChanged(valid newSize)───► Connected(newSize)   // screen resize
Connected    ──onCanvasSizeChanged(invalid size)────► Connected (unchanged)
```

Any transition to `Connected` while `gestureState == Gesturing` aborts the active gesture:
calls `engine.endGesture()`, sets `gestureState = Idle`, increments `invalidationTick`.

### 04_02. GestureState Transitions  {#SP_CTL_v2_04_02}

```
[Idle] ──onGestureStart (connected)──► [Gesturing]
[Idle] ──onTap (connected)──────────► [Idle]
[Idle] ──undo / redo / reset────────► [Idle]

[Gesturing] ──onGestureMove──► [Gesturing]    (repeated, 0..N times)
[Gesturing] ──onGestureEnd───► [Idle]
[Gesturing] ──onCanvasSizeChanged (valid, resize)──► [Idle]  // gesture aborted
```

Operations that are **no-ops** when in `[Gesturing]`: `undo`, `redo`, `reset`, `onTap`.
Operations that are **no-ops** when in `[Disconnected]`: `onGestureStart`, `onTap`.

### 04_03. invalidationTick Advancement  {#SP_CTL_v2_04_03}

`invalidationTick` increments by 1 after each of these operations:

| Operation | Increments when |
|-----------|----------------|
| `onGestureStart` | Always (when not no-op) |
| `onGestureMove` | Always (when not no-op) |
| `onGestureEnd` | Always (when not no-op) |
| `onTap` | Always (when not no-op) |
| `undo` | Always (when not no-op) |
| `redo` | Always (when not no-op) |
| `reset` | Always (when not no-op) |
| Gesture abort (on resize) | Once per abort |

`invalidationTick` never decrements. Overflow wraps silently (32-bit signed integer).

### 04_04. canUndo / canRedo Refresh  {#SP_CTL_v2_04_04}

After each of `onGestureEnd`, `onTap`, `undo`, `redo`, `reset`, and **gesture abort** (when the operation is not a no-op):
```
canUndo = engine.canUndo
canRedo = engine.canRedo
```
These observable state updates happen before `invalidationTick` increments (so observers
always see consistent `canUndo`/`canRedo` when they re-render).

*Note: Gesture abort (triggered by `onCanvasSizeChanged` mid-gesture) calls `engine.endGesture()`,
which may commit the partial action. Refreshing here prevents `canUndo`/`canRedo` from becoming stale.*

---

## 05 — Verification Criteria  {#SP_CTL_v2_05}

### 05_01. Functional Expectations  {#SP_CTL_v2_05_01}

| Contract | Scenario | Expected outcome |
|----------|----------|------------------|
| `onGestureStart` + `onGestureEnd` | Brush stroke with default tool | Action committed; `canUndo = true`; `invalidationTick` incremented twice |
| `onGestureStart` + 5× `onGestureMove` + `onGestureEnd` | Multi-point stroke | `invalidationTick` incremented 7 times total; committed action in engine |
| `undo` after commit | One stroke committed | `canUndo = false`; `canRedo = true`; `invalidationTick` incremented |
| `redo` after undo | One stroke undone | `canUndo = true`; `canRedo = false` |
| `reset` | Three strokes committed | `canUndo = false`; `canRedo = false`; display shows blank canvas |
| `onGestureStart` while `Disconnected` | No canvas size reported | No-op; `invalidationTick` unchanged; engine untouched |
| `undo` during `Gesturing` | Active gesture in progress | No-op; gesture continues |
| `onTap` with EyedropperTool | Stroke present at tap point | `onColorPicked` callback fires; no action committed; `canUndo` unchanged |
| `strokeWidth = -1` set by consumer | — | `strokeWidth` clamped to `0.1`; no exception |
| `opacity = 1.5` set by consumer | — | `opacity` clamped to `1.0`; no exception |
| `onCanvasSizeChanged` mid-gesture | Valid new size | Gesture aborted; `gestureState = Idle`; `invalidationTick` incremented; new `screenSize` applied |
| `exportBitmap(IntSize(200, 200))` | Two strokes committed | Returns `ImageBitmap(200, 200)` with scaled strokes; active canvas excluded |
| `exportBitmap(IntSize(0, 100))` | — | `IllegalArgumentException` thrown |

### 05_02. Invariant Checks  {#SP_CTL_v2_05_02}

| Invariant | Verification method |
|-----------|---------------------|
| `logicalSize` never changes after construction | Read `logicalSize` before and after any sequence of operations — must be equal |
| Tool active at gesture start is used throughout | Set `activeTool = BrushTool`, start gesture, change `activeTool = ShapeTool`, end gesture — committed action must be a `PathAction` (from Brush), not `ShapeAction` |
| `strokeWidth >= 0.1` always | Set `strokeWidth = -5`; assert result is `0.1` |
| `opacity` in `[0.0, 1.0]` always | Set `opacity = 2.0`; assert result is `1.0` |
| `invalidationTick` never decrements | Record tick before any operation; assert tick after equals tick-before or greater |
| `canUndo` consistent with engine | After any history-changing op: assert `controller.canUndo == engine.canUndo` |
| Gesture state machine | Attempt `undo` during gesture; assert `canUndo` unchanged, engine unchanged |
| `PaintOptions` construction never throws | Set `strokeWidth = 0.1`; call `onTap` — must not throw |

### 05_03. Integration Scenarios  {#SP_CTL_v2_05_03}

| Scenario | Preconditions | Steps | Expected result |
|----------|--------------|-------|-----------------|
| Full gesture cycle in DrawBox | DrawController connected to DrawBoxCanvas | User drags finger across canvas | `invalidationTick` increments on each move; stroke appears incrementally |
| Tool switch between gestures | BrushTool active | Finish gesture; set `activeTool = ShapeTool`; start new gesture | New gesture produces ShapeAction; previous stroke (PathAction) unaffected |
| Undo button binds to `canUndo` | Three strokes committed | Consumer binds undo button's `enabled` to `canUndo` | Button enables/disables reactively after each undo/redo |
| Screen rotation | Gesture in progress | OS rotates screen → `onCanvasSizeChanged` fires with new size | Gesture aborted cleanly; next gesture uses new screen size; committed strokes unchanged |
| EyedropperTool updates paint color | `activeTool = EyedropperTool { c -> controller.color = c }` | User taps on a red stroke | `controller.color` becomes red; `activeTool` still EyedropperTool |
| Export at 2× logical size | Two committed strokes, `logicalSize = IntSize(400, 300)` | `exportBitmap(IntSize(800, 600))` | Returns `ImageBitmap(800, 600)` with strokes scaled 2× |

### 05_04. Edge Cases  {#SP_CTL_v2_05_04}

| Case | Expected behavior |
|------|-------------------|
| `onGestureEnd` with no prior `onGestureStart` | No-op (gestureState == Idle) |
| `onGestureMove` with no prior `onGestureStart` | No-op |
| `onGestureStart` twice without `onGestureEnd` | Second call is no-op; first gesture continues |
| `undo` when `canUndo = false` | Delegates to `engine.undo()` which is a no-op; `invalidationTick` still increments |
| `redo` when `canRedo = false` | Same — engine no-op; `invalidationTick` still increments |
| `reset` when nothing committed | Engine no-op; `invalidationTick` increments; `canUndo = false`, `canRedo = false` |
| `getDisplayOutput()` during gesture | Returns committed + active canvas; active canvas reflects current partial stroke |
| `onCanvasSizeChanged` with same size | `connectionState` updated (no-op net effect); no gesture abort |
| Touch point outside canvas bounds | Logical point outside `[0, logicalSize]`; engine accepts it; tools/actions clip |
| FillTool tap on empty canvas | `FillAction` with full-canvas fill committed; `canUndo = true` |

---

## 06 — Reversibility  {#SP_CTL_v2_06}

### 06_01. Rollback Strategy  {#SP_CTL_v2_06_01}

> Implements: [C_CTL_v2_01_02](draw_controller_v2.concept.md#C_CTL_v2_01_02)

| Aspect | Rollback approach |
|--------|-------------------|
| Code | C_CTL_v2 lives in its own package (separate from C_CTL's package). Removing the package and reverting C_BOX_v2 → C_BOX restores v1 behavior. C_ENG must also be removed or left unused. |
| Data | All state is in-memory. No persistence. Rollback = revert source + restart app. |
| Public API | `DrawController` class name is reused (same as v1). Consumers must update their import path from v1 package to v2 package when migrating. Reverting = update import path back. |
| Dependent specs | SP_BOX_v2 depends on SP_CTL_v2. Reverting requires SP_BOX_v2 → SP_BOX and corresponding code revert. |
| C_CTL coexistence | C_CTL (v1) is deprecated but not deleted. Both can compile simultaneously in a transition period. They live in different packages. |

---

## Changelog

| Date | Change |
|------|--------|
| 2026-05-27 | Initial version — created from C_CTL_v2 concept |
