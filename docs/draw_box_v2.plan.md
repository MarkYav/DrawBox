# Implementation Plan: DrawBox v2  {#PL_BOX_v2}

> **Code:** PL_BOX_v2
> **Status:** in-progress
> **Created:** 2026-05-27
> **Updated:** 2026-05-27
>
> **Concept:** [C_BOX_v2](draw_box_v2.concept.md)
> **Specification:** [SP_BOX_v2](draw_box_v2.sp.md)
> **Depends on plans:** [PL_CTL_v2](draw_controller_v2.plan.md)
> **Used by plans:** —
>
> Replace the v1 box package with a v2 implementation that uses the invalidation pull
> model, the v2 gesture API, and removes background/opacity ownership from the composable layer.

---

## Goal

When this plan is complete:
- `box/DrawBox.kt` is the v2 public facade (controller + modifier only; no background).
- `box/DrawBoxCanvas.kt` is the v2 internal composable (invalidation pull rendering, v2 gesture wiring).
- V1 box-package and controller-package stubs are left in place pending a future cleanup pass.

---

## Technology Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Language | Kotlin (KMP, commonMain) | Project standard |
| Rendering trigger | `remember(tick) { controller.getDisplayOutput() }` | Stable bitmap per tick; no re-request on unrelated recompositions (style.md: no redundant computation) |
| Gesture wiring | Two separate `pointerInput(Unit)` blocks — one for tap, one for drag | Matches v1 pattern; gesture detectors do not share state; platform resolves tap-vs-drag by threshold |
| Segment model | `change.previousPosition` + `change.position` forwarded as `(from, to)` | Matches engine's two-point segment model; no delta accumulation in box layer |
| `DrawBoxCanvas` visibility | `internal` | Architecture rule: internal composables must not be callable by consumers (architecture.md) |
| Background ownership | Removed from `DrawBox` | Design decision from C_BOX_v2: consumer controls background via modifier or wrapping composable |
| V1 stubs | Left in place | `DrawBoxBackground.kt` (box), `DrawBoxBackground` / `DrawBoxSubscription` / `OpenedImage` / `DrawBoxConnectionState` (controller) still compile and are referenced in VCS; cleanup deferred to a future pass |
| Clipping | `Modifier.clipToBounds()` | Prevents ink from rendering outside the canvas bounds (SP_BOX_v2_03_02) |

---

## Progress

- [x] Phase 1 — DrawBox public facade
- [x] Phase 2 — DrawBoxCanvas internal composable

---

## Phases

### Phase 1 — DrawBox public facade (`box/DrawBox.kt`) [DONE]

**Depends on:** PL_CTL_v2 (DrawController v2 must exist)
**Implements:** [SP_BOX_v2_01_01](draw_box_v2.sp.md#SP_BOX_v2_01_01), [SP_BOX_v2_02_01](draw_box_v2.sp.md#SP_BOX_v2_02_01)

**File:** `drawbox/src/commonMain/kotlin/io/github/markyav/drawbox/box/DrawBox.kt`

**What to replace:** Remove all v1 controller API calls (`getPathWrappersForDrawbox`,
`getOpenImageForDrawbox`, `background`, `canvasOpacity`, `connectToDrawBox`,
`insertNewPath`, `updateLatestPath`, `finalizePath`). Replace with a single
delegate to `DrawBoxCanvas`.

**Structure sketch:**
```
@Composable
fun DrawBox(
    controller: DrawController,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    DrawBoxCanvas(controller = controller, modifier = modifier)
}
```

**Notes:**
- `DrawBoxBackground` is no longer called from `DrawBox`. Consumer wraps in their own layout if background is needed.
- No state collection in `DrawBox` itself — all rendering state is in `DrawBoxCanvas`.

---

### Phase 2 — DrawBoxCanvas internal composable (`box/DrawBoxCanvas.kt`) [DONE]

**Depends on:** Phase 1, PL_CTL_v2
**Implements:** [SP_BOX_v2_01_02](draw_box_v2.sp.md#SP_BOX_v2_01_02), [SP_BOX_v2_02_02](draw_box_v2.sp.md#SP_BOX_v2_02_02)–[SP_BOX_v2_02_07](draw_box_v2.sp.md#SP_BOX_v2_02_07)

**File:** `drawbox/src/commonMain/kotlin/io/github/markyav/drawbox/box/DrawBoxCanvas.kt`

**What to replace:** Remove all v1 parameter list (`pathListWrapper`, `openedImage`, `alpha`,
`onSizeChanged`, `onTap`, `onDragStart`, `onDrag`, `onDragEnd`). Replace with `controller`
parameter and internal invalidation-pull rendering.

**Structure sketch:**
```
@Composable
internal fun DrawBoxCanvas(controller: DrawController, modifier: Modifier = Modifier) {
    val tick by controller.invalidationTick.collectAsState()
    val bitmap = remember(tick) { controller.getDisplayOutput() }

    Canvas(modifier = modifier
        .onSizeChanged { controller.onCanvasSizeChanged(it) }
        .pointerInput(Unit) { detectTapGestures(onTap = { controller.onTap(it) }) }
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { controller.onGestureStart(it) },
                onDrag = { change, _ -> controller.onGestureMove(change.previousPosition, change.position) },
                onDragEnd = { controller.onGestureEnd() },
                onDragCancel = { controller.onGestureEnd() },
            )
        }
        .clipToBounds()
    ) {
        drawImage(bitmap)
    }
}
```

**Notes:**
- `DrawBoxCanvas` is `internal` — compiler enforces this (SP_BOX_v2_03_01).
- `change.previousPosition` and `change.position` provide the `(from, to)` segment for `onGestureMove` (SP_BOX_v2_02_05).
- `onDragCancel` maps to `controller.onGestureEnd()` — same as `onDragEnd` (SP_BOX_v2_02_06).
- `clipToBounds()` prevents ink from overflowing canvas bounds (SP_BOX_v2_03_02).
- Old import chain (`PathWrapper`, `OpenedImage`, `createPath`, `StrokeCap`, `StrokeJoin`, `Stroke`) is fully removed.

---

## Backlog

| Item | Reason deferred |
|------|----------------|
| Delete v1 box stubs (`DrawBoxBackground.kt` in box package) | Still compiles; no breakage; cleanup reserved for a coordinated pass with controller stub removal |
| Delete v1 controller stubs (`DrawBoxBackground`, `DrawBoxSubscription`, `OpenedImage`, `DrawBoxConnectionState`) | Still referenced in VCS; deferred to same pass |
| Sample app updates (`sample/android`, `sample/desktop`) | Sample apps fully use v1 API; excluded from published artifact; rewrite is a separate task |

---

## Changelog

| Date | Change |
|------|--------|
| 2026-05-27 | Initial version — plan created post-implementation (code written during fix phase) |
