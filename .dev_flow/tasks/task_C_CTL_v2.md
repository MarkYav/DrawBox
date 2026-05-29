# Task: C_CTL_v2 Draw Controller v2

**Task ID:** task_C_CTL_v2
**Created:** 2026-05-27
**Last updated:** 2026-05-29
**Status:** in-progress
**Contributors:** claude-sonnet-4-6

## Current Work Item

**Document:** drawbox/.../controller/DrawController.kt
**Phase:** Implement → Review
**Traceable ID:** PL_CTL_v2

## Description

*(claude-sonnet-4-6)* Creating the Draw Controller v2 concept. C_CTL_v2 is the bridge between
the consumer (paint settings, tool selection, undo/redo), the rendering layer (C_BOX_v2), and
the Draw Engine (C_ENG). It supersedes C_CTL (deprecated). Key changes: logical-pixel coordinates
(no [0..1] normalization), multi-tool support, rectangular canvas, background removed.

## Subtasks

### Subtask: Write C_CTL_v2 concept
**Author:** claude-sonnet-4-6
**Status:** done
**Goal:** Write docs/draw_controller_v2.concept.md following concept phase rules

**Progress:**
- [x] Pre-concept checklist passed
- [x] Reuse check — C_CTL incompatible (different coordinate system, single-tool, square-only); new version justified
- [x] Philosophy section (core principle + design constraints)
- [x] Domain model (5 entities + data flows with ASCII diagrams)
- [x] Mechanisms (coordinate mapping, tool context snapshot, invalidation pull model, canUndo/canRedo tracking)
- [x] Edge cases documented
- [x] Integration points (dependencies + API surface)
- [x] Gate: user approved concept (ran `/dev-flow spec`)

**Activity:**
- 2026-05-27: Gate passed. User approved concept.
- 2026-05-27: Concept written. Awaiting review.

### Subtask: Write SP_CTL_v2 specification
**Author:** claude-sonnet-4-6
**Status:** done
**Goal:** Write docs/draw_controller_v2.sp.md following spec phase rules

**Progress:**
- [x] Data structures — DrawController, ConnectionState, GestureState
- [x] Contracts — onCanvasSizeChanged, onGestureStart/Move/End, onTap, undo, redo, reset, mapToLogical, getDisplayOutput, exportBitmap (11 contracts)
- [x] Validation rules — all fields and operations
- [x] State transitions — ConnectionState, GestureState, invalidationTick, canUndo/canRedo refresh
- [x] Verification criteria — functional, invariants, integration scenarios, edge cases
- [x] Reversibility / rollback strategy
- [x] Self-validation checklist passed
- [x] Gate: user approved spec (ran `/dev-flow plan`)

**Activity:**
- 2026-05-27: Gate passed. User approved spec.
- 2026-05-27: Specification written. Awaiting review.

### Subtask: Write PL_CTL_v2 plan
**Author:** claude-sonnet-4-6
**Status:** done
**Goal:** Write docs/draw_controller_v2.plan.md following plan phase rules

**Progress:**
- [x] Technology decisions documented (StateFlow, clamping strategy, connection/gesture state representation, v1 file deferral)
- [x] Phase 1 — Update architecture rule (engine layer added, normalization/square-canvas rules retired)
- [x] Phase 2 — DrawController v2 (full pseudocode sketch for all 11 contracts + helpers)
- [x] Backlog items listed (v1 file cleanup, EyedropperTool convenience, thread-safety)
- [x] Gate: user approves plan (runs `/dev-flow implement`)

**Activity:**
- 2026-05-27: Plan written. Awaiting review.
- 2026-05-29: Gate approved (user ran `/dev-flow implement`). Implementation confirmed complete from previous session.

### Subtask: Implement PL_CTL_v2
**Author:** claude-sonnet-4-6
**Status:** done
**Goal:** Verify and finalize DrawController.kt per PL_CTL_v2

**Progress:**
- [x] Phase 1 — architecture rule confirmed updated
- [x] Phase 2 — DrawController.kt confirmed implemented
- [x] Fix: `undoDepth` default corrected from 5 → 20 (spec/plan say 20)
- [x] Note: `gestureContext` field added to match spec SP_CTL_v2_01_03; set in onGestureStart, cleared in onGestureEnd and abortGesture
- [x] Fix: `abortGesture()` retains `refreshUndoRedo()` — spec updated (SP_CTL_v2_02_01 + SP_CTL_v2_04_04) + plan note corrected
- [x] Pre-commit review passed (2 blockers resolved, 4 warnings non-blocking)
- [x] Gate: ready for commit approval

**Activity:**
- 2026-05-29: Implementation verified; all review blockers resolved; awaiting commit approval
- 2026-05-29: Pre-commit review run; undoDepth default fixed; gestureContext added

## Coordination Notes

*(claude-sonnet-4-6, 2026-05-27)* Key design decisions captured in concept:
- Controller is a thin coordinator; no drawing logic — all delegated to C_ENG
- Invalidation pull model: controller emits a signal; composable pulls fresh ImageBitmap
- Tool context snapshot taken at gesture start — paint/tool changes mid-gesture are ignored
- Background removed from controller; it's a DrawBox composable parameter
- logicalSize immutable at construction; screenSize reported by DrawBoxCanvas at runtime

## Blocking Issues

*(none)*

## Relevant Context

| Item | Note | Added by |
|------|------|----------|
| docs/v2_architecture.spike.md | All architectural decisions this concept is based on | claude-sonnet-4-6 |
| docs/draw_engine.concept.md | C_ENG — primary dependency | claude-sonnet-4-6 |
| docs/controller.concept.md | C_CTL (v1) — deprecated by this concept | claude-sonnet-4-6 |

## Shared Activity Log

- 2026-05-29 — Implementation verified; undoDepth fix applied; ready for pre-commit review
- 2026-05-27 — PL_CTL_v2 plan written by claude-sonnet-4-6; awaiting review
- 2026-05-27 — SP_CTL_v2 specification gate passed; user ran `/dev-flow plan`
- 2026-05-27 — SP_CTL_v2 specification written by claude-sonnet-4-6; awaiting review
- 2026-05-27 — C_CTL_v2 concept gate passed; user ran `/dev-flow spec`
- 2026-05-27 — C_CTL_v2 concept written by claude-sonnet-4-6; awaiting review
- 2026-05-27 — Task created (claude-sonnet-4-6)
