# DrawBox v2 — Specification  {#SP_BOX_v2}

> **Code:** SP_BOX_v2
> **Status:** draft
> **Created:** 2026-05-27
> **Updated:** 2026-05-27
>
> **Concept:** [C_BOX_v2](draw_box_v2.concept.md)
> **Depends on specs:** [SP_CTL_v2](draw_controller_v2.sp.md)
> **Used by specs:** —
> **Plan:** [draw_box_v2.plan.md](draw_box_v2.plan.md)
>
> Defines all data structures, contracts, state transitions, validation rules,
> and verification criteria for the DrawBox v2 composable layer. The box layer
> is a thin rendering and gesture bridge: it observes the controller's invalidation
> signal, pulls a pre-rendered bitmap on each change, and forwards pointer events
> to the controller using the v2 gesture API.

---

## 01 — Data Structures  {#SP_BOX_v2_01}

> Implements: [C_BOX_v2_02](draw_box_v2.concept.md#C_BOX_v2_02)

### 01_01. DrawBox  {#SP_BOX_v2_01_01}

The single public composable entry point exposed to library consumers.

**Parameters:**

| Parameter | Type | Required | Default | Constraints |
|-----------|------|----------|---------|-------------|
| `controller` | `DrawController` | yes | — | non-null |
| `modifier` | `Modifier` | no | `Modifier.fillMaxSize()` | — |

Invariants:
- `DrawBox` is the only public composable in the box package.
- `DrawBox` delegates all rendering and gesture handling to `DrawBoxCanvas`.
- `DrawBox` does not render background, apply opacity, or collect any controller state directly.

### 01_02. DrawBoxCanvas  {#SP_BOX_v2_01_02}

The internal composable that owns size reporting, gesture wiring, and bitmap rendering.
Not callable by library consumers.

**Parameters:**

| Parameter | Type | Required | Default | Constraints |
|-----------|------|----------|---------|-------------|
| `controller` | `DrawController` | yes | — | non-null |
| `modifier` | `Modifier` | no | `Modifier` | — |

**Internal reactive state:**

| State | Type | Source | Recomputed when |
|-------|------|--------|-----------------|
| `tick` | `Int` | `controller.invalidationTick` (observed) | Controller emits a new tick value |
| `bitmap` | `ImageBitmap` | `controller.getDisplayOutput()` | `tick` changes |

Invariants:
- `DrawBoxCanvas` is `internal` — must not be accessible to consumers.
- The `bitmap` is always the result of `controller.getDisplayOutput()` from the most recent `tick`.
- Drawing is always clipped to the composable's own bounds.
- All pointer coordinates forwarded to the controller are raw screen-space offsets — no normalization or transformation applied in the box layer.

---

## 02 — Contracts  {#SP_BOX_v2_02}

### 02_01. DrawBox composable  {#SP_BOX_v2_02_01}

**Purpose:** Public entry point. Hosts `DrawBoxCanvas` with the given controller and modifier.

**Behavior:**
1. Compose `DrawBoxCanvas(controller, modifier)`.
2. No additional layers, backgrounds, or overlays are added by `DrawBox` itself.

**Output:** A composable node whose size and position are determined by `modifier`.

---

### 02_02. Size reporting — onCanvasSizeChanged  {#SP_BOX_v2_02_02}

**Purpose:** Notify the controller whenever the composable's measured layout size changes.

**Trigger:** The composable's `onSizeChanged` layout callback fires.

**Behavior:**
1. Receive new `IntSize`.
2. Call `controller.onCanvasSizeChanged(size)`.
3. The controller applies its own guards (ignores zero/negative dimensions; aborts active gesture if one is in progress).

**Invariant:** No size filtering is performed in the box layer — all reported sizes, including zero, are forwarded to the controller.

---

### 02_03. Tap gesture — onTap  {#SP_BOX_v2_02_03}

**Purpose:** Forward a single-point tap to the controller.

**Trigger:** Pointer pressed and released without significant movement (platform-defined threshold).

**Behavior:**
1. Receive tap `Offset` (screen space).
2. Call `controller.onTap(screenPoint)`.

**Invariant:** The tap offset is forwarded unchanged. No clamping or normalization.

---

### 02_04. Drag start — onGestureStart  {#SP_BOX_v2_02_04}

**Purpose:** Signal the start of a drag gesture to the controller.

**Trigger:** Pointer pressed and initial movement threshold exceeded (platform-defined).

**Behavior:**
1. Receive drag start `Offset` (screen space).
2. Call `controller.onGestureStart(screenPoint)`.

---

### 02_05. Drag move — onGestureMove  {#SP_BOX_v2_02_05}

**Purpose:** Forward each drag movement segment to the controller.

**Trigger:** Pointer moves during an active drag.

**Behavior:**
1. Receive pointer event containing `previousPosition: Offset` and `position: Offset` (both screen space).
2. Call `controller.onGestureMove(from = previousPosition, to = position)`.

**Invariant:** Both endpoints are forwarded unchanged. The segment `(from, to)` represents one
movement step; successive calls chain segments end-to-end.

---

### 02_06. Drag end / cancel — onGestureEnd  {#SP_BOX_v2_02_06}

**Purpose:** Signal the end (or cancellation) of a drag gesture to the controller.

**Trigger:** Pointer released (`onDragEnd`) or gesture system cancels the drag (`onDragCancel`).

**Behavior:**
1. Call `controller.onGestureEnd()`.

**Invariant:** Both `onDragEnd` and `onDragCancel` map to the same controller call.
The controller treats both as gesture completion.

---

### 02_07. Rendering — bitmap pull  {#SP_BOX_v2_02_07}

**Purpose:** Keep the visual output synchronized with the controller's drawing state.

**Trigger:** `controller.invalidationTick` emits a new value.

**Behavior:**
1. Observe `controller.invalidationTick`.
2. When tick changes, call `controller.getDisplayOutput()` to obtain the current `ImageBitmap`.
3. Cache the bitmap for the current composition pass.
4. Draw the bitmap to fill the composable's entire drawing area.

**Invariant:** The bitmap is never held across tick changes — a new bitmap is requested from the
controller on every tick increment. The composable does not cache bitmaps independently of the
controller's invalidation signal.

---

## 03 — Validation Rules  {#SP_BOX_v2_03}

### 03_01. Architectural constraints  {#SP_BOX_v2_03_01}

- **must** — `DrawBoxCanvas` must be declared `internal`. It must not appear in the public API surface.
- **must** — The box layer must not import `engine`, `model`, or `util` packages directly. All access to drawing state goes through `DrawController`.
- **must** — No coordinate transformation (normalization, scaling) may be applied in the box layer. Screen-space offsets are passed to the controller as-is.
- **must** — `DrawBox` must not call any controller method directly. All controller interaction is delegated to `DrawBoxCanvas`.

### 03_02. Rendering constraints  {#SP_BOX_v2_03_02}

- **must** — Drawing must be clipped to the composable's bounds. Content that overflows the canvas area must not be visible.
- **must** — The bitmap drawn is always the result of `controller.getDisplayOutput()` called after the most recent `invalidationTick` change. Stale bitmaps must not persist across tick increments.

---

## 04 — State Transitions  {#SP_BOX_v2_04}

### 04_01. Rendering lifecycle  {#SP_BOX_v2_04_01}

The composable's rendering state has two stages:

```
[Initial — no tick observed]
    → observe invalidationTick
    → tick = 0, call getDisplayOutput()
    → draw blank/initial bitmap

[Tick changes — controller signals update]
    → recompose triggered
    → call getDisplayOutput() → new bitmap
    → redraw
```

**Key rule:** The bitmap is recomputed as a `remember(tick)` — it is stable within a tick
and replaced exactly once per tick increment. Recompositions not driven by a tick change
do not re-request the bitmap.

### 04_02. Gesture routing lifecycle  {#SP_BOX_v2_04_02}

The box layer is stateless with respect to gestures. It forwards events and does not track
gesture state itself. The controller owns all gesture state transitions (see [SP_CTL_v2_04](draw_controller_v2.sp.md#SP_CTL_v2_04)).

```
pointer-down                 → onGestureStart (forwarded to controller)
pointer-move × N             → onGestureMove (forwarded, one call per event)
pointer-up OR cancel         → onGestureEnd  (forwarded to controller)

tap (short press, no move)   → onTap (forwarded to controller)
```

**Tap vs drag resolution:** Tap and drag detectors run concurrently on separate input channels.
A tap fires when the pointer is released without crossing the platform's drag threshold.
A drag fires when the threshold is exceeded. They do not fire simultaneously for the same gesture.

---

## 05 — Verification Criteria  {#SP_BOX_v2_05}

### 05_01. Functional expectations  {#SP_BOX_v2_05_01}

| Contract | Scenario | Input | Expected outcome |
|----------|----------|-------|-----------------|
| DrawBox | Composition | controller + modifier | DrawBoxCanvas rendered within modifier bounds |
| onCanvasSizeChanged | Canvas laid out | layout measurement fires | `controller.onCanvasSizeChanged(size)` called with measured size |
| onCanvasSizeChanged | Zero size | width=0 or height=0 | Forwarded to controller; controller applies its own guard |
| onTap | Short press | tap at (x,y) | `controller.onTap(Offset(x,y))` called |
| onGestureStart | Drag begins | press + movement | `controller.onGestureStart(startOffset)` called |
| onGestureMove | Pointer moves | move from p1 to p2 | `controller.onGestureMove(p1, p2)` called |
| onGestureEnd | Pointer released | drag end | `controller.onGestureEnd()` called |
| onGestureEnd | Gesture cancelled | system cancel | `controller.onGestureEnd()` called |
| bitmap pull | `invalidationTick` increments | tick + 1 | `getDisplayOutput()` called; new bitmap drawn |
| bitmap pull | No tick change | recompose from parent | Same bitmap reused; `getDisplayOutput()` not called |

### 05_02. Invariant checks  {#SP_BOX_v2_05_02}

| Invariant | Verification |
|-----------|-------------|
| `DrawBoxCanvas` is internal | Attempt to call `DrawBoxCanvas` from outside the box package — must fail to compile |
| Box layer imports no engine/model/util | Inspect import list in DrawBox.kt and DrawBoxCanvas.kt — must show only `controller` and Compose imports |
| Screen coordinates forwarded unchanged | Instrument controller mock; compare coordinates received with coordinates entered |
| Bitmap stable within tick | Verify `getDisplayOutput()` is called exactly once per tick increment, not on every recomposition |
| Drawing clipped to bounds | Render a stroke that extends beyond canvas edges — pixels outside must not be visible |

### 05_03. Integration scenarios  {#SP_BOX_v2_05_03}

| Scenario | Preconditions | Steps | Expected result |
|----------|--------------|-------|-----------------|
| First render | Controller constructed with logicalSize | Embed DrawBox; wait for layout | `onCanvasSizeChanged` called; `getDisplayOutput()` returns blank bitmap; blank canvas visible |
| Draw a stroke | Controller connected (size set) | Press, drag, release | `onGestureStart`, `onGestureMove` × N, `onGestureEnd` called; tick increments after each; strokes visible |
| Tap to draw a point | Controller connected | Short tap | `onTap` called; tick increments; point stroke visible |
| Undo | At least one stroke committed | Call `controller.undo()` | `invalidationTick` increments; new bitmap without last stroke drawn |
| Canvas resize during gesture | Active drag in progress | Window resizes mid-drag | Controller aborts gesture; tick increments twice (abort + resize connect); new bitmap at new size |

### 05_04. Edge cases  {#SP_BOX_v2_05_04}

| Case | Expected behavior |
|------|-------------------|
| Controller Disconnected at composition time | `getDisplayOutput()` returns blank bitmap; no crash |
| Tap immediately after drag end | Both events forwarded independently; controller handles ordering |
| Drag starts before controller connects | `onGestureStart` called with disconnected controller; controller returns no-op |
| Very fast successive taps | Each tap forwarded independently; controller guards re-entrancy |
| Bitmap drawn at zero-size canvas | No draw operation; no crash |

---

## 06 — Reversibility  {#SP_BOX_v2_06}

### 06_01. Rollback strategy  {#SP_BOX_v2_06_01}

| Aspect | Rollback approach |
|--------|-------------------|
| Code | Restore `DrawBox.kt` and `DrawBoxCanvas.kt` to v1 from VCS; no data migration needed |
| State | Box layer holds no persistent state; rollback is stateless |
| Controller dependency | V2 box requires v2 controller; rolling back box without rolling back controller requires restoring both |
| Consumer API | `DrawBox(controller, modifier)` signature is the same in v1 and v2 — no consumer breakage |
| V1 stub files | `DrawBoxBackground.kt` (box), `DrawBoxBackground` / `DrawBoxSubscription` / `OpenedImage` / `DrawBoxConnectionState` (controller) remain in VCS; available for v1 restoration |

---

## Changelog

| Date | Change |
|------|--------|
| 2026-05-27 | Initial version — created from C_BOX_v2 concept |
