# Task: C_BOX_v2 DrawBox Composable v2

**Task ID:** task_C_BOX_v2
**Created:** 2026-05-27
**Last updated:** 2026-05-27
**Status:** in-progress
**Contributors:** claude-sonnet-4-6

## Current Work Item

**Document:** docs/draw_box_v2.plan.md
**Phase:** Plan → Review
**Traceable ID:** PL_BOX_v2

## Description

*(claude-sonnet-4-6)* DrawBox v2 is the Compose rendering and gesture layer built on top of C_CTL_v2.
Key changes from v1: rendering switches from PathWrapper-list traversal to an invalidation pull model
(observe `invalidationTick`, call `getDisplayOutput()`); gesture API updated to v2 controller surface;
background and opacity ownership removed from the box layer.

## Subtasks

### Subtask: Write C_BOX_v2 concept
**Author:** claude-sonnet-4-6
**Status:** done
**Goal:** Write docs/draw_box_v2.concept.md following concept phase rules

**Progress:**
- [x] Pre-concept checklist passed
- [x] Reuse check — C_BOX incompatible (different render model, API); v2 justified
- [x] Philosophy section (core principle + design constraints)
- [x] Domain model (2 entities + visual stack + data flows)
- [x] Mechanisms (invalidation pull, segment model, size reporting, edge cases)
- [x] Integration points (C_CTL_v2 dependency + API surface)
- [x] Gate: user approved concept (ran `/dev-flow spec`)

**Activity:**
- 2026-05-27: Gate passed. User approved concept.
- 2026-05-27: Concept written. Awaiting review.

### Subtask: Write SP_BOX_v2 specification
**Author:** claude-sonnet-4-6
**Status:** done
**Goal:** Write docs/draw_box_v2.sp.md following spec phase rules

**Progress:**
- [x] Data structures — DrawBox (public), DrawBoxCanvas (internal)
- [x] Contracts — 7 contracts: DrawBox, onCanvasSizeChanged, onTap, onGestureStart, onGestureMove, onGestureEnd, bitmap pull
- [x] Validation rules — architectural constraints + rendering constraints
- [x] State transitions — rendering lifecycle, gesture routing lifecycle
- [x] Verification criteria — functional, invariants, integration scenarios, edge cases
- [x] Reversibility — rollback strategy
- [x] Self-validation checklist passed
- [x] Gate: user approved spec (ran `/dev-flow plan`)

**Activity:**
- 2026-05-27: Gate passed. User approved spec.
- 2026-05-27: Specification written. Awaiting review.

### Subtask: Write PL_BOX_v2 plan
**Author:** claude-sonnet-4-6
**Status:** done
**Goal:** Write docs/draw_box_v2.plan.md following plan phase rules

**Progress:**
- [x] Technology decisions documented (rendering model, gesture wiring, segment model, v1 stubs deferral)
- [x] Phase 1 — DrawBox facade (structure sketch, notes) [DONE — code pre-exists from fix phase]
- [x] Phase 2 — DrawBoxCanvas (structure sketch, notes) [DONE — code pre-exists from fix phase]
- [x] Backlog items listed (v1 stubs, sample app updates)
- [x] Gate: user approved plan (ran `/dev-flow implement`)

**Activity:**
- 2026-05-27: Gate passed. User approved plan.
- 2026-05-27: Plan written. Awaiting review.

### Subtask: Implement PL_BOX_v2
**Author:** claude-sonnet-4-6
**Status:** done
**Goal:** Write DrawBox.kt and DrawBoxCanvas.kt per PL_BOX_v2

**Progress:**
- [x] Phase 1 — DrawBox.kt (public facade, SP_BOX_v2_01_01 traceable comment)
- [x] Phase 2 — DrawBoxCanvas.kt (single awaitEachGesture block, clipToBounds first, traceable comments)
- [x] Pre-commit review passed (B1 clipToBounds order, B2 tap/drag race fixed; W3 fillMaxSize default intentional)
- [ ] Gate: user approves implementation

**Activity:**
- 2026-05-27: Implementation complete and review fixes applied.

## Coordination Notes

*(none)*

## Blocking Issues

*(none)*

## Relevant Context

| Item | Note | Added by |
|------|------|----------|
| docs/draw_controller_v2.concept.md | C_CTL_v2 — primary dependency | claude-sonnet-4-6 |
| docs/box.concept.md | C_BOX (v1) — deprecated by this concept | claude-sonnet-4-6 |

## Shared Activity Log

- 2026-05-27 — C_BOX_v2 concept written by claude-sonnet-4-6; awaiting review
- 2026-05-27 — Task created (claude-sonnet-4-6)
