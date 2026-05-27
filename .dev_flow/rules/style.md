# Code Style

## Kotlin Idioms

**should** — Prefer `data class` for value types with multiple fields. Use `object` for
singleton sealed variants. Never use regular `class` for sealed variants without mutable state.

**prefer** — Use `.apply { }` for builder-style object initialization (e.g., `Path().apply { ... }`,
`Paint().apply { ... }`).

Example: `util/Util.kt:6`, `controller/DrawController.kt:221`

**prefer** — Use extension functions for operations that logically belong to a type but cannot
be added to the class directly (`List<PathWrapper>.scale()`, `MutableList<E>.addNotNull()`).

## StateFlow Usage

**should** — Expose internal mutable state as `MutableStateFlow` directly on `DrawController`
for settable properties that consumers need to drive (color, strokeWidth, opacity, background).
Do not wrap in a getter/setter pattern.

**should** — Use `mapState` / `combineStates` (from `util/StateFlowUtil.kt`) to derive
`StateFlow` from other `StateFlow`s. Do not use `.stateIn(scope)` directly in controller code
unless a scope is explicitly managed.

## Compose Conventions

**should** — Use `remember { }` for stable references to StateFlows inside Composables to
prevent re-subscription on recomposition.

Example: `box/DrawBox.kt:18-19`

**should** — Separate gesture handling lambdas from rendering lambdas in Canvas Composables.
Gesture lambdas are set up with `pointerInput`; render code lives inside the `Canvas { }` block.

**prefer** — Modifier chains are written one-modifier-per-line when they exceed 2 modifiers.

## Comments

**prefer** — Use KDoc `/** */` only on public API (`DrawController` methods). Do not comment
internal implementation details unless non-obvious (workaround, invariant, known limitation).

Example: `util/StateFlowUtil.kt:10-12` — KDoc explaining the workaround source.
