# DrawBox — Documentation Index

**Library:** DrawBox v2.0.0 — KMP Compose drawing library  
**Onboarded:** 2026-05-27

## Modules (bottom-up dependency order)

| Module | Concept | Spec | Plan | Layer | Status |
|--------|---------|------|------|-------|--------|
| `util` | [C_UTL](util.concept.md) | [SP_UTL](util.sp.md) | [PL_UTL](util.plan.md) | 0 | active (v1) |
| `model` | [C_MDL](model.concept.md) | [SP_MDL](model.sp.md) | [PL_MDL](model.plan.md) | 0 | active (v1) |
| `engine` | [C_ENG](draw_engine.concept.md) | [SP_ENG](draw_engine.sp.md) | [PL_ENG](draw_engine.plan.md) | 1 | **active (v2, implemented)** |
| `controller` | [C_CTL](controller.concept.md) *(deprecated)* | [SP_CTL](controller.sp.md) | [PL_CTL](controller.plan.md) | 1 | deprecated → C_CTL_v2 |
| `controller v2` | [C_CTL_v2](draw_controller_v2.concept.md) | [SP_CTL_v2](draw_controller_v2.sp.md) | [PL_CTL_v2](draw_controller_v2.plan.md) | 2 | **active (v2, implemented)** |
| `box` | [C_BOX](box.concept.md) *(deprecated)* | [SP_BOX](box.sp.md) | [PL_BOX](box.plan.md) | 3 | deprecated → C_BOX_v2 |
| `box v2` | [C_BOX_v2](draw_box_v2.concept.md) | [SP_BOX_v2](draw_box_v2.sp.md) | [PL_BOX_v2](draw_box_v2.plan.md) | 3 | **active (v2, implemented)** |

## Quick Reference (v2 — in progress)

**Public API surface (v2):**
- `DrawController(logicalSize)` — state manager. Set `color`, `strokeWidth`, `opacity`, `activeTool`.
- `DrawBox(controller, modifier, background)` — only public Composable. Background is a lambda.
- `DrawController.exportBitmap(size)` — export committed drawing as `ImageBitmap`.

**Key invariant (v2):** All engine data in logical pixel space (set at construction).

**Known issues:** See `.dev_flow/onboard/issues.md` (5 open issues).

**Rules:** `.dev_flow/rules/` — 5 rule files covering naming, structure, architecture, error-handling, style.

**Known issues:** See `.dev_flow/onboard/issues.md` (5 open issues).

**Rules:** `.dev_flow/rules/` — 5 rule files covering naming, structure, architecture, error-handling, style.
