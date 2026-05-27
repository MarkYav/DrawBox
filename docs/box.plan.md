---
ID: PL_BOX
Title: DrawBox Composable Layer — Implementation Plan
Status: completed
Created: 2026-05-27
Updated: 2026-05-27
Spec: [SP_BOX](box.sp.md)
---

# PL_BOX — DrawBox Composable Layer Plan

## Technology Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Entry point | Single `@Composable fun DrawBox(controller, modifier)` | Minimal public surface |
| Gesture wiring | Separate `pointerInput` blocks for tap vs drag | Required by Compose gesture API |
| Drag delta | Use `change.position` (absolute), ignore `dragAmount` | Matches controller's absolute-coord model |
| Rendering | Compose `Canvas` + `drawPath` | KMP-compatible, no platform APIs |
| Background | Separate `DrawBoxBackground` Composable | Clean separation of background rendering |

## Phases

### Phase 1 — Internal DrawBoxBackground Composable [DONE]
- Render NoBackground, ColourBackground, ImageBackground variants.

### Phase 2 — Internal DrawBoxCanvas Composable [DONE]
- Gesture detection (tap + drag).
- Canvas rendering: opened image + path list.
- Size reporting via `onSizeChanged`.

### Phase 3 — Public DrawBox Composable [DONE]
- Collect state from controller.
- Stack background + canvas in `Box`.
- Wire DynamicUpdate subscription.

## Backlog

- Future: Support non-square (rectangular) canvas once controller allows it.
- Future: Expose background scale/fit options for `ImageBackground`.
- Note: Box-package `DrawBoxBackground` shares a name with `controller.DrawBoxBackground`
  (the sealed interface) — consider renaming the internal Composable to `DrawBoxBackgroundLayer`
  to avoid confusion.

## Changelog

- 2026-05-27 — Initialized from existing codebase via onboard procedure.
