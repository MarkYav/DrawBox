# Task: v2 Architecture Spike

**Task ID:** task_20260527_v2-spike
**Created:** 2026-05-27
**Last updated:** 2026-05-27
**Status:** done
**Contributors:** claude-sonnet-4-6

## Current Work Item

**Document:** docs/v2_architecture.spike.md
**Phase:** Spike (pre-concept investigation)
**Traceable ID:** n/a (spike — no gate)

## Description

*(claude-sonnet-4-6)* Investigated and documented the architecture decisions for DrawBox v2.0.0
through a series of design discussions with Mark Yavorskyi. The spike answers three core questions:
coordinate system approach, tool/action extensibility model, and rendering/undo bitmap strategy.
All questions are concluded. The spike is written and ready for Mark's review before proceeding
to the concept phase.

## Subtasks

### Subtask: Write v2 architecture spike
**Author:** claude-sonnet-4-6
**Status:** done
**Goal:** Capture all v2 architecture decisions into a spike document

**Progress:**
- [x] Coordinate system: logical space + canvas.scale() — decided
- [x] Tool/action system: open interfaces DrawTool + DrawAction — decided
- [x] Rendering: three-bitmap model — decided
- [x] Undo: two snapshots + configurable depth — decided
- [x] Background: @Composable lambda on DrawBox — decided
- [x] Canvas shape: rectangular IntSize — decided
- [x] PaintOptions ownership: DrawController + ToolContext snapshot — decided
- [x] Write spike document to docs/v2_architecture.spike.md
- [x] Update active_context.md and tasks/_index.md

**Activity:**
- 2026-05-27: Completed spike document. All decisions captured. Ready for user review.

## Coordination Notes

*(claude-sonnet-4-6, 2026-05-27)* Spike is concluded. Recommended next step: Mark reviews
`docs/v2_architecture.spike.md`, confirms or adjusts any decisions, then we start
`/dev-flow concept` for C_ENG (Draw Engine).

## Relevant Context

| Item | Note | Added by |
|------|------|----------|
| docs/v2_architecture.spike.md | Spike document with all v2 decisions | claude-sonnet-4-6 |
| CyrusCastle/DrawBox-Enhanced | Fork analyzed for comparison — inspired tool/action system | claude-sonnet-4-6 |

## Shared Activity Log

- 2026-05-27 — Task created and spike completed by claude-sonnet-4-6
