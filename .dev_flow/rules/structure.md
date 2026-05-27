# Code Structure

## File Organization

**must** — One Kotlin file per top-level declaration (sealed interface, class, or object).
Each file is named after its primary declaration.

Example: `DrawBoxConnectionState.kt` contains only `DrawBoxConnectionState`.
`DrawController.kt` contains only `DrawController` (with private extension functions at bottom).

**should** — Private extension functions on a class live in the same file as that class
(e.g., `List<PathWrapper>.scale()` in `DrawController.kt`).

## Class Layout

**prefer** — Within a class, order: private state flows → public state flows / properties →
computed derived flows → public mutating functions → internal functions → private functions.

Example: `DrawController.kt` — private `drawnPaths`, `canceledPaths` → public `color`, `strokeWidth`
→ `undoCount`, `redoCount` → `undo()`, `redo()`, `reset()` → `internal connectToDrawBox(...)` → private `scale(...)`.

## Sealed Interface Variants

**must** — Sealed interface variants are declared inside the same file as the sealed interface.
Object variants use `object`; data variants use `data class`.

Example: `DrawBoxConnectionState.kt:3-6`

## Composable Structure

**should** — Composables are kept thin: collect state at the top, delegate rendering to
sub-composables or Canvas draw calls. No business logic inside Composables.

Example: `DrawBox.kt` — collects state, delegates to `DrawBoxBackground` + `DrawBoxCanvas`.
