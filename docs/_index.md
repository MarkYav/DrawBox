# DrawBox — Documentation Index

**Library:** DrawBox v2.0.0 — KMP Compose drawing library  
**Onboarded:** 2026-05-27

## Modules (bottom-up dependency order)

| Module | Concept | Spec | Plan | Layer |
|--------|---------|------|------|-------|
| `util` | [C_UTL](util.concept.md) | [SP_UTL](util.sp.md) | [PL_UTL](util.plan.md) | 0 |
| `model` | [C_MDL](model.concept.md) | [SP_MDL](model.sp.md) | [PL_MDL](model.plan.md) | 0 |
| `controller` | [C_CTL](controller.concept.md) | [SP_CTL](controller.sp.md) | [PL_CTL](controller.plan.md) | 1 |
| `box` | [C_BOX](box.concept.md) | [SP_BOX](box.sp.md) | [PL_BOX](box.plan.md) | 2 |

## Quick Reference

**Public API surface:**
- `DrawController` — state manager. Create one; mutate `color`, `strokeWidth`, `opacity`, etc.
- `DrawBox(controller, modifier)` — the only public Composable. Embed in your UI.
- `DrawController.getBitmap(size, subscription)` — export drawing as `StateFlow<ImageBitmap>`.

**Key invariant:** All stroke data in `PathWrapper` is in normalized [0..1] space.

**Known issues:** See `.dev_flow/onboard/issues.md` (5 open issues).

**Rules:** `.dev_flow/rules/` — 5 rule files covering naming, structure, architecture, error-handling, style.
