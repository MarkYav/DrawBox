# Implementation Plan: Draw Controller v2  {#PL_CTL_v2}

> **Code:** PL_CTL_v2
> **Status:** in-progress
> **Created:** 2026-05-27
> **Updated:** 2026-05-27
>
> **Concept:** [C_CTL_v2](draw_controller_v2.concept.md)
> **Specification:** [SP_CTL_v2](draw_controller_v2.sp.md)
> **Depends on plans:** [PL_ENG](draw_engine.plan.md)
> **Used by plans:** [PL_BOX_v2](draw_box_v2.plan.md)
>
> Replace the v1 DrawController with a v2 implementation that delegates all drawing
> operations to DrawEngine, supports multiple tools, uses logical-pixel coordinates,
> and drops background ownership.

---

## Goal

When this plan is complete:
- `controller/DrawController.kt` is the v2 implementation (engine-backed, multi-tool,
  rectangular canvas, logical coordinates).
- V1 DrawController is gone; v1 support files are unused stubs pending C_BOX_v2 cleanup.
- `.dev_flow/rules/architecture.md` names the `engine` layer.

---

## Technology Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Language | Kotlin (KMP, commonMain) | Project standard |
| Package | `io.github.markyav.drawbox.controller` | Same as v1; consumers keep the same import path |
| Observable state | `MutableStateFlow<T>` / `StateFlow<T>` | Matches v1 pattern; no Compose runtime in controller (ISSUE-002 fix) |
| ConnectionState impl | `private var screenSize: IntSize?` (null = Disconnected) | Avoids a sealed class for a single nullable field |
| GestureState impl | `private var gestureActiveTool: DrawTool?` + `private var gestureContext: ToolContext?` | Avoids a sealed class; null = Idle, non-null = Gesturing |
| `strokeWidth`/`opacity` clamping | Applied in `buildPaintOptions()`, not at StateFlow write time | Satisfies style rule "do not wrap in getter/setter" (style.md); consumers observe raw values |
| `canUndo`/`canRedo` backing | Private `MutableStateFlow<Boolean>` + public read-only `StateFlow<Boolean>` | Spec requires read-only for consumer; internal mutability needed |
| `invalidationTick` visibility | `internal val invalidationTick: StateFlow<Int>` | Consumed exclusively by DrawBoxCanvas (box package); not public API |
| V1 support file cleanup | Deferred to PL_BOX_v2 | `DrawBoxConnectionState.kt`, `DrawBoxSubscription.kt`, `OpenedImage.kt`, `DrawBoxBackground.kt` (controller package) still compile and are imported by the current box package; deleting them now breaks box compilation |
| Architecture rule | Add `engine` between `model` and `controller` | `engine` is a new dependency layer; the rule `util → model → controller → box` must include it |

---

## Progress

- [x] Phase 1 — Update architecture rule
- [x] Phase 2 — DrawController v2

---

## Phases

### Phase 1 — Update architecture rule [DONE]

**Depends on:** none
**Implements:** [SP_CTL_v2_06_01](draw_controller_v2.sp.md#SP_CTL_v2_06_01) (rollback — layer boundary documentation)

Update `.dev_flow/rules/architecture.md`:
- Change `util → model → controller → box` to `util → model → engine → controller → box`
- Update the "Layer Boundaries" **must** rule accordingly
- Remove the "Normalization Invariant" rule (logical-pixel coordinates replace [0..1]; the invariant no longer applies to v2)
- Remove the "Square Canvas Constraint" rule (rectangular canvas is now fully supported in v2)

### Phase 2 — DrawController v2 (`controller/DrawController.kt`) [DONE]

**Depends on:** Phase 1, PL_ENG (DrawEngine must exist)
**Implements:** [SP_CTL_v2_01](draw_controller_v2.sp.md#SP_CTL_v2_01), [SP_CTL_v2_02](draw_controller_v2.sp.md#SP_CTL_v2_02), [SP_CTL_v2_03](draw_controller_v2.sp.md#SP_CTL_v2_03), [SP_CTL_v2_04](draw_controller_v2.sp.md#SP_CTL_v2_04)

**What to create:** Replace `drawbox/src/commonMain/kotlin/io/github/markyav/drawbox/controller/DrawController.kt` entirely.

**File structure sketch:**

```
package io.github.markyav.drawbox.controller

imports: engine.*, kotlinx.coroutines.flow.*, compose.ui.geometry.*, compose.ui.graphics.*, compose.ui.unit.*

// [SP_CTL_v2_01_01] DrawController — public facade
class DrawController(
    val logicalSize: IntSize,
    private val undoDepth: Int = 20,
    defaultTool: DrawTool = BrushTool,
) {
    // --- Construction validation [SP_CTL_v2_03] ---
    init {
        require(logicalSize.width > 0 && logicalSize.height > 0)
        require(undoDepth >= 1)
    }

    // --- Engine (private) [SP_CTL_v2_01_01] ---
    private val engine = DrawEngine(logicalSize, undoDepth)

    // --- Connection state [SP_CTL_v2_01_02] ---
    private var screenSize: IntSize? = null          // null = Disconnected

    // --- Gesture state [SP_CTL_v2_01_03] ---
    private var gestureActiveTool: DrawTool? = null  // null = Idle
    private var gestureContext: ToolContext? = null  // non-null when Gesturing

    // --- Mutable observable paint state [SP_CTL_v2_01_01] ---
    val color: MutableStateFlow<Color> = MutableStateFlow(Color.Black)
    val strokeWidth: MutableStateFlow<Float> = MutableStateFlow(4f)
    val opacity: MutableStateFlow<Float> = MutableStateFlow(1f)
    val activeTool: MutableStateFlow<DrawTool> = MutableStateFlow(defaultTool)

    // --- Read-only observable state [SP_CTL_v2_01_01] ---
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo

    private val _invalidationTick = MutableStateFlow(0)
    internal val invalidationTick: StateFlow<Int> = _invalidationTick
}
```

**Helper — buildPaintOptions (private):**
```
// Clamps strokeWidth and opacity before passing to engine [SP_CTL_v2_03]
fun buildPaintOptions(): PaintOptions =
    PaintOptions(
        color = color.value,
        strokeWidth = strokeWidth.value.coerceAtLeast(0.1f),
        opacity = opacity.value.coerceIn(0f, 1f),
    )
```

**Helper — refreshUndoRedo (private):**
```
fun refreshUndoRedo() {
    _canUndo.value = engine.canUndo
    _canRedo.value = engine.canRedo
}
```

**Helper — invalidate (private):**
```
fun invalidate() { _invalidationTick.value++ }
```

**Helper — mapToLogical (private) [SP_CTL_v2_02_09]:**
```
fun mapToLogical(screenPoint: Offset): Offset {
    val s = screenSize ?: return screenPoint  // callers always check connected state first
    return Offset(
        screenPoint.x * logicalSize.width  / s.width,
        screenPoint.y * logicalSize.height / s.height,
    )
}
```

**onCanvasSizeChanged [SP_CTL_v2_02_01]:**
```
fun onCanvasSizeChanged(size: IntSize) {
    if (size.width <= 0 || size.height <= 0) return
    if (gestureActiveTool != null) abortGesture()
    screenSize = size
}

private fun abortGesture() {
    engine.endGesture()
    gestureActiveTool = null
    gestureContext = null
    invalidate()
}
```

**onGestureStart [SP_CTL_v2_02_02]:**
```
fun onGestureStart(screenPoint: Offset) {
    if (screenSize == null || gestureActiveTool != null) return
    val tool = activeTool.value
    val logicalPoint = mapToLogical(screenPoint)
    val context = engine.createToolContext(buildPaintOptions())
    engine.beginGesture(tool, logicalPoint, context)
    gestureActiveTool = tool
    gestureContext = context
    invalidate()
}
```

**onGestureMove [SP_CTL_v2_02_03]:**
```
fun onGestureMove(from: Offset, to: Offset) {
    if (gestureActiveTool == null) return
    engine.extendGesture(mapToLogical(from), mapToLogical(to))
    invalidate()
}
```

**onGestureEnd [SP_CTL_v2_02_04]:**
```
fun onGestureEnd() {
    if (gestureActiveTool == null) return
    engine.endGesture()
    gestureActiveTool = null
    gestureContext = null
    refreshUndoRedo()
    invalidate()
}
```

**onTap [SP_CTL_v2_02_05]:**
```
fun onTap(screenPoint: Offset) {
    if (screenSize == null || gestureActiveTool != null) return
    val tool = activeTool.value
    val context = engine.createToolContext(buildPaintOptions())
    engine.onTap(tool, mapToLogical(screenPoint), context)
    refreshUndoRedo()
    invalidate()
}
```

**undo / redo / reset [SP_CTL_v2_02_06–08]:**
```
fun undo()  { if (gestureActiveTool != null) return; engine.undo();  refreshUndoRedo(); invalidate() }
fun redo()  { if (gestureActiveTool != null) return; engine.redo();  refreshUndoRedo(); invalidate() }
fun reset() { if (gestureActiveTool != null) return; engine.reset(); refreshUndoRedo(); invalidate() }
```

**getDisplayOutput / exportBitmap [SP_CTL_v2_02_10–11]:**
```
fun getDisplayOutput(): ImageBitmap = engine.getDisplayOutput()

fun exportBitmap(size: IntSize): ImageBitmap {
    require(size.width > 0 && size.height > 0)
    return engine.exportBitmap(size)
}
```

**Notes:**
- `refreshUndoRedo()` is called BEFORE `invalidate()` in all contracts — per [SP_CTL_v2_04_04]: observers see consistent `canUndo`/`canRedo` when they re-render after the tick change.
- `abortGesture()` DOES call `refreshUndoRedo()` before `invalidate()` — `engine.endGesture()` may commit the partial action (e.g., a BrushTool stroke), so `canUndo` can legitimately change. SP_CTL_v2_02_01 and SP_CTL_v2_04_04 updated to reflect this.
- `activeTool.value` is read fresh on every `onGestureStart` / `onTap` — if consumer changes the tool between gestures, the new tool is used. Mid-gesture changes are ignored because `gestureActiveTool` is the locked reference.
- The v1 support files (`DrawBoxConnectionState.kt`, `DrawBoxSubscription.kt`, `OpenedImage.kt`, `DrawBoxBackground.kt` in controller package) are left in place. They are not imported by the new DrawController.kt but are still referenced by the v1 box package. They will be deleted in PL_BOX_v2.

---

## Backlog

| Item | Reason deferred |
|------|----------------|
| Delete v1 controller support files (`DrawBoxConnectionState.kt`, `DrawBoxSubscription.kt`, `OpenedImage.kt`, `DrawBoxBackground.kt`) | These are still imported by the v1 box package; deletion deferred to PL_BOX_v2 |
| `EyedropperTool` convenience property | Not in spec; low priority; consumers can construct `EyedropperTool { c -> controller.color.value = c }` themselves |
| Thread-safety / coroutine scope | All operations called from main thread; no scope needed; revisit if background export is added |

---

## Changelog

| Date | Change |
|------|--------|
| 2026-05-27 | Initial version — created from SP_CTL_v2 |
