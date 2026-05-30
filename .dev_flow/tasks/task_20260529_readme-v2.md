# Task: README v2 Update

**Task ID:** task_20260529_readme-v2
**Created:** 2026-05-29
**Last updated:** 2026-05-29
**Status:** done
**Contributors:** claude-sonnet-4-6

## Current Work Item

**Document:** README.md
**Phase:** Implement → Review
**Traceable ID:** —

## Description

*(claude-sonnet-4-6)* Rewrite README.md to reflect the v2 API. Key changes:
- Updated feature list: multi-tool, logical resolution, export, no background ownership
- Updated usage snippets: new `DrawController(logicalSize = ...)` constructor, `controller` parameter name
- Added tool switching, export, and background sections
- Updated version to `2.0.0`
- Removed outdated "Next releases / Planned" items (erase tool is done, Compose dep migration is done)
- Removed v1 API references

## Subtasks

### Subtask: Rewrite README
**Author:** claude-sonnet-4-6
**Status:** done
**Goal:** Update README.md to accurately document the v2 public API

**Progress:**
- [x] Analyze current README vs v2 spec
- [x] Rewrite README
- [x] Pre-commit review passed (0 blockers; W1 activeTool fixed)
- [ ] Gate: user approves commit

**Activity:**
- 2026-05-29: README rewritten; review passed; W1 (activeTool snippet) addressed; awaiting commit approval

## Coordination Notes

*(none)*

## Blocking Issues

*(none)*

## Relevant Context

| Item | Note | Added by |
|------|------|----------|
| docs/draw_controller_v2.sp.md | V2 public API reference | claude-sonnet-4-6 |
| docs/draw_box_v2.sp.md | V2 DrawBox signature | claude-sonnet-4-6 |

## Shared Activity Log

- 2026-05-29 — Task created (claude-sonnet-4-6)
