# Concept: DrawBox Composable v2  {#C_BOX_v2}

> **Code:** C_BOX_v2
> **Status:** active
> **Created:** 2026-05-27
> **Updated:** 2026-05-27
> **Author:** claude-sonnet-4-6
> **Owner:** Library maintainer (MarkYav)
> **Complexity:** low
>
> **Depends on:** [C_CTL_v2](draw_controller_v2.concept.md)
> **Used by:** —
> **Spike:** —
> **Specification:** [SP_BOX_v2](draw_box_v2.sp.md)
> **Plan:** [draw_box_v2.plan.md](draw_box_v2.plan.md)
>
> The v2 Compose rendering and gesture layer for the drawing canvas. Replaces [C_BOX](box.concept.md).
> Bridges `DrawController` v2 to Compose UI using an invalidation pull model and the v2 gesture API.
> Background and opacity are no longer owned here — the consumer controls them.

---

## 1. Philosophy  {#C_BOX_v2_01}

### 1.1. Core Principle  {#C_BOX_v2_01_01}

DrawBox v2 is a pure rendering and input bridge. It has one job: observe the controller's
invalidation signal, pull a pre-rendered image when the signal fires, and route pointer
events back to the controller. No drawing logic, no state, no visual customization beyond
the canvas surface itself lives here.

### 1.2. Design Constraints  {#C_BOX_v2_01_02}

- **No background ownership.** Background was removed from `DrawController` in v2. The consumer
  places DrawBox inside their own layout and applies background via their own composables or
  modifier. DrawBox renders only the drawing layer.
- **No opacity control.** Canvas opacity was a v1 controller property. V2 consumers apply opacity
  via `Modifier.alpha()` if they need it.
- **Read-only access to controller internals.** The box layer may observe `invalidationTick`
  (internal) and call the public gesture and rendering contracts. It must not reach into
  engine, model, or util packages directly.
- **Rollback:** The box and controller layers are independently reversible. Reverting
  `DrawBox.kt` and `DrawBoxCanvas.kt` to v1 restores the v1 box behavior, provided the v1
  controller is also restored.
- **V1 support files** (`DrawBoxBackground.kt` in box package, `DrawBoxBackground` /
  `DrawBoxSubscription` / `OpenedImage` / `DrawBoxConnectionState` in controller package)
  remain in place until a coordinated cleanup pass removes them.

---

## 2. Domain Model  {#C_BOX_v2_02}

### 2.1. Key Entities  {#C_BOX_v2_02_01}

| Entity | Visibility | Responsibility |
|--------|-----------|----------------|
| `DrawBox` | public composable | Public entry point. Accepts `DrawController` and `Modifier`. Delegates rendering and input to `DrawBoxCanvas`. |
| `DrawBoxCanvas` | internal composable | Owns size reporting, gesture wiring, and bitmap rendering. The only place in the box layer that interacts with the controller. |

The visual stack contains only one layer: `DrawBoxCanvas`. The consumer is responsible for
any background beneath it.

```
Consumer layout
├── [consumer background, optional]
└── DrawBox
    └── DrawBoxCanvas  (internal)
```

### 2.2. Data Flows  {#C_BOX_v2_02_02}

**Rendering flow (pull model):**

```
DrawController.invalidationTick (StateFlow<Int>)
    → DrawBoxCanvas observes tick
    → on tick change: call DrawController.getDisplayOutput()
    → receive ImageBitmap (pre-rendered by engine)
    → draw bitmap onto Compose Canvas
```

The composable never holds drawing state. The bitmap is the controller's responsibility;
the composable only requests and displays it.

**Input flows:**

```
onSizeChanged(IntSize)       → DrawController.onCanvasSizeChanged(size)
pointer-down(Offset)         → DrawController.onGestureStart(screenPoint)
pointer-move(prev, cur)      → DrawController.onGestureMove(from, to)
pointer-up / cancel          → DrawController.onGestureEnd()
tap(Offset)                  → DrawController.onTap(screenPoint)
```

All screen-space coordinates are forwarded as-is. Coordinate mapping (screen → logical)
is the controller's responsibility.

---

## 3. Mechanisms  {#C_BOX_v2_03}

### 3.1. Invalidation Pull Model  {#C_BOX_v2_03_01}

`DrawController` exposes an `invalidationTick` counter that increments every time the
visual state changes (stroke added, undo, reset, etc.). `DrawBoxCanvas` subscribes to this
counter. When the tick changes, a new bitmap is requested from the controller and cached
for the current composition — the composable does not compute or cache any drawing logic.

This model decouples rendering frequency from drawing logic: the composable recomposes
only when the controller signals a change, and it always shows the latest committed state.

### 3.2. Gesture Segment Model  {#C_BOX_v2_03_02}

V2 gesture routing passes pointer-move events as two-point segments `(from, to)` rather
than a single absolute position. This matches the engine's segment model for stroke
building. The composable reads `previousPosition` and `position` from the pointer event
and forwards both to the controller.

Drag-start provides the gesture anchor point. The controller uses it for the first segment
start; subsequent segments chain from the previous endpoint.

### 3.3. Size Reporting  {#C_BOX_v2_03_03}

`DrawBoxCanvas` reports its layout size to the controller whenever the measured bounds
change. The controller uses this to connect (transition from Disconnected to Connected)
and to compute the screen-to-logical coordinate mapping. If the size changes mid-gesture,
the controller aborts the active gesture before updating the size.

### 3.4. Edge Cases  {#C_BOX_v2_03_04}

| Scenario | Behavior |
|----------|----------|
| Size reported before any gesture | Controller connects; subsequent gestures are mapped correctly |
| Pointer-move received before pointer-down | Controller ignores: no active gesture |
| Tap and drag conflict | Two separate gesture detectors; tap fires for short presses, drag for sustained movement |
| Bitmap requested while controller is disconnected | Controller returns a blank bitmap for the logical size |
| Canvas resized during active gesture | Controller aborts the gesture (commits partial action) then accepts new size |

---

## 4. Integration Points  {#C_BOX_v2_04}

### 4.1. Dependencies  {#C_BOX_v2_04_01}

| Dependency | What is used |
|------------|-------------|
| [C_CTL_v2](draw_controller_v2.concept.md) | `invalidationTick`, `getDisplayOutput()`, all gesture contracts, `onCanvasSizeChanged()` |

The box layer has no direct dependency on the engine, model, or util packages.

### 4.2. API Surface  {#C_BOX_v2_04_02}

**Public (consumer-facing):**

| Symbol | Kind | Description |
|--------|------|-------------|
| `DrawBox(controller, modifier)` | composable | Single public entry point |

**Internal (library-only):**

| Symbol | Kind | Description |
|--------|------|-------------|
| `DrawBoxCanvas(controller, modifier)` | composable | Rendering + gesture composable; not callable by consumers |

**What is NOT exposed:**
- Any path or action data from the engine
- Background, opacity, or other decorative state
- `invalidationTick` (internal to controller package)

---

## Changelog

| Date | Change |
|------|--------|
| 2026-05-27 | Initial version — replaces C_BOX with v2 invalidation pull model and v2 gesture API |
