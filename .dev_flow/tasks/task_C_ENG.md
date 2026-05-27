# Task: C_ENG Draw Engine Concept + Specification

**Task ID:** task_C_ENG
**Created:** 2026-05-27
**Last updated:** 2026-05-27
**Status:** in-progress
**Contributors:** claude-sonnet-4-6

## Current Work Item

**Document:** engine/ (18 source files)
**Phase:** Implement → Review
**Traceable ID:** PL_ENG

## Description

*(claude-sonnet-4-6)* Creating the Draw Engine concept and specification for DrawBox v2. This is
the foundational architecture that introduces DrawTool, DrawAction, ToolContext, PaintOptions, and
CanvasManager as separate concerns extracted from the v1 DrawController. Concept approved; spec
written and awaiting user review before proceeding to the plan gate.

## Subtasks

### Subtask: Write C_ENG concept
**Author:** claude-sonnet-4-6
**Status:** done
**Goal:** Write docs/draw_engine.concept.md following concept phase rules

**Progress:**
- [x] Pre-concept checklist passed
- [x] Reuse check — C_CTL and C_MDL overlap justified
- [x] Philosophy section (core principle + design constraints)
- [x] Domain model (5 entities + data flows with ASCII diagrams)
- [x] Mechanisms (incremental rendering, checkpoint, simplification, stroke eraser)
- [x] Edge cases documented
- [x] Integration points (dependencies + API surface)
- [x] Gate: user approved concept (ran `/dev-flow spec`)

**Activity:**
- 2026-05-27: Concept written. Awaiting review.
- 2026-05-27: Gate passed. User approved concept.

### Subtask: Write SP_ENG specification
**Author:** claude-sonnet-4-6
**Status:** done
**Goal:** Write docs/draw_engine.sp.md following spec phase rules

**Progress:**
- [x] Data structures — DrawAction, DrawTool, ToolContext, PaintOptions, CanvasManager, DrawEngine
- [x] Built-in DrawAction implementations — PathAction, FillAction, ShapeAction, StrokeEraseAction
- [x] Built-in DrawTool implementations — Brush, PixelEraser, StrokeEraser, Fill, Eyedropper, Shape
- [x] Contracts — all DrawEngine operations (beginGesture, extendGesture, endGesture, onTap, undo, redo, reset, canUndo/canRedo, getDisplayOutput, exportBitmap)
- [x] Internal CanvasManager contracts — commit, commitStrokeErase, undo, redo, replayFromCheckpoint, advanceCheckpointIfNeeded
- [x] State transitions — gesture state machine, checkpoint advancement table
- [x] Validation rules
- [x] Verification criteria — functional, invariants, integration scenarios, edge cases
- [x] Reversibility / rollback strategy
- [x] Gate: user approved spec (ran `/dev-flow plan`)

**Activity:**
- 2026-05-27: Spec written. Awaiting review.
- 2026-05-27: Gate passed. User approved spec.

### Subtask: Write PL_ENG plan
**Author:** claude-sonnet-4-6
**Status:** done
**Goal:** Write docs/draw_engine.plan.md following plan phase rules

**Progress:**
- [x] Technology decisions documented (package, types, coordinate space, pixel access)
- [x] Phase 1 — Core interfaces and data types
- [x] Phase 2 — Built-in DrawAction implementations
- [x] Phase 3 — Path simplification utility
- [x] Phase 4 — Built-in DrawTool implementations (all 6 tools)
- [x] Phase 5 — CanvasManager (internal)
- [x] Phase 6 — DrawEngine facade
- [x] Backlog items listed
- [x] Gate: user approved plan (ran `/dev-flow implement`)

**Activity:**
- 2026-05-27: Plan written. Awaiting review.
- 2026-05-27: Gate passed. User approved plan.

### Subtask: Implement PL_ENG (all 6 phases)
**Author:** claude-sonnet-4-6
**Status:** done
**Goal:** Write all engine source files per draw_engine.plan.md

**Progress:**
- [x] Phase 1 — PaintOptions, ToolContext, DrawAction, DrawTool, ShapeType (5 files)
- [x] Phase 2 — PathAction, FillAction, ShapeAction, StrokeEraseAction (4 files)
- [x] Phase 3 — PathSimplifier (1 file)
- [x] Phase 4 — BrushTool, PixelEraserTool, StrokeEraserTool, FillTool, EyedropperTool, ShapeTool (6 files)
- [x] Phase 5 — CanvasManager (1 file)
- [x] Phase 6 — DrawEngine (1 file)
- [x] Pre-commit review (subagent)
- [x] Post-review fixes: W-01 (PaintOptions→regular class), W-04 (PathSimplifier t-clamping), W-02/W-03 (spec updated for StrokeEraseAction/StrokeEraserTool)

**Activity:**
- 2026-05-27: Post-review fixes applied; W-01 PaintOptions regular class, W-04 PathSimplifier t.coerceIn, W-02/W-03 spec aligned with implementation.
- 2026-05-27: Pre-commit review returned WARNINGS (no hard blockers); fixes in progress.
- 2026-05-27: All 18 engine files written.

## Coordination Notes

*(claude-sonnet-4-6, 2026-05-27)* Plan written. Key decisions:
- Package: `io.github.markyav.drawbox.engine` (isolated; sub-packages `engine.action`, `engine.tool`)
- Coordinate space: logical pixels — explicitly NOT the v1 [0..1] normalized system
- `CanvasManager.getDisplayOutput()` returns `Pair<ImageBitmap, ImageBitmap>` (committed, active) to avoid per-frame allocation; DrawEngine composes them in `getDisplayOutput()`
- `BrushTool` may use a mutable growing points list for O(N) allocation efficiency — noted as optimization, not spec deviation
- `StrokeEraserTool` passes gesture accumulation via a private marker action type (`StrokeEraseInProgress`)

After Mark reviews PL_ENG, run `/dev-flow implement` to begin Phase 1 code.

## Blocking Issues

*(none)*

## Relevant Context

| Item | Note | Added by |
|------|------|----------|
| docs/v2_architecture.spike.md | All architectural decisions this is based on | claude-sonnet-4-6 |
| docs/draw_engine.concept.md | Approved concept document | claude-sonnet-4-6 |
| docs/draw_engine.sp.md | Approved specification | claude-sonnet-4-6 |
| docs/draw_engine.plan.md | Implementation plan awaiting review | claude-sonnet-4-6 |

## Shared Activity Log

- 2026-05-27 — Post-review fixes complete; entering Verify phase
- 2026-05-27 — PL_ENG pre-commit review: WARNINGS — W-01/W-04 code fixes, W-02/W-03 spec updates applied
- 2026-05-27 — PL_ENG all 18 engine files implemented by claude-sonnet-4-6; entering review
- 2026-05-27 — PL_ENG plan gate passed; user ran `/dev-flow implement`
- 2026-05-27 — PL_ENG plan written by claude-sonnet-4-6; awaiting review
- 2026-05-27 — SP_ENG specification gate passed; user ran `/dev-flow plan`
- 2026-05-27 — SP_ENG specification written by claude-sonnet-4-6; awaiting review
- 2026-05-27 — C_ENG concept gate passed; user ran `/dev-flow spec`
- 2026-05-27 — Concept draft written by claude-sonnet-4-6; awaiting review
- 2026-05-27 — Task created (claude-sonnet-4-6)
