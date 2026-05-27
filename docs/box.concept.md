---
ID: C_BOX
Title: DrawBox Composable Layer
Status: active
Created: 2026-05-27
Updated: 2026-05-27
Depends on: [C_CTL, C_MDL, C_UTL]
Used by: []
---

# C_BOX — DrawBox Composable Layer

## 01 — Purpose

Provide the single public `@Composable` entry point that consumers embed in their
Compose UI. `DrawBox` bridges the `DrawController` reactive state to Compose rendering
and gesture handling.

## 02 — Domain Model

The composable layer has three internal components:

- **`DrawBox`** — public. Collects background/opacity state; stacks background and canvas.
- **`DrawBoxCanvas`** — internal. Renders paths and images onto a Compose `Canvas`.
  Wires pointer gestures to controller callbacks.
- **`DrawBoxBackground`** — internal. Renders the decorative background (colour or image)
  behind the drawing canvas.

The visual stack (bottom to top): `DrawBoxBackground` → `DrawBoxCanvas`.

## 03 — Mechanisms

**Size reporting:** `DrawBoxCanvas` uses `onSizeChanged` to call `controller.connectToDrawBox`.
This is the trigger for the controller's state machine transition to `Connected`.

**Gesture routing:**
- Tap → `controller.onTap` (single-point stroke)
- Drag start → `controller.insertNewPath`
- Drag move → `controller.updateLatestPath` (uses absolute position, not delta)
- Drag end/cancel → `controller.finalizePath`

**Rendering:** Each recomposition driven by the `StateFlow` collected from
`getPathWrappersForDrawbox(DynamicUpdate)` — includes the active (in-progress) stroke
so the user sees their stroke as they draw.

**Image center-crop:** `OpenedImage.Image` bakes crop math into default parameters
(`srcOffset`, `srcSize`), so the canvas always displays a square crop of the loaded image.

## 04 — What This IS and IS NOT

**IS:** A thin Compose rendering layer. Zero business logic — all state changes route to `DrawController`.  
**IS NOT:** Responsible for any state management, export, or external I/O.

## 05 — Changelog

- 2026-05-27 — Initialized from existing codebase via onboard procedure.
