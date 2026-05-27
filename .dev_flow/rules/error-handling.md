# Error Handling

## Precondition Checks

**should** — Use `require(condition)` for internal preconditions on `DrawController` gesture methods.
These guard against incorrect call order (e.g., calling `updateLatestPath` before `insertNewPath`).

Example:
```kotlin
// DrawController.kt:91
require(activeDrawingPath.value != null)
// DrawController.kt:101
require(activeDrawingPath.value == null)
```

These checks are internal invariants — they are not expected to throw in normal usage.

## Silent Rejection

**prefer** — Operations that require a specific state (e.g., path operations requiring `Connected`)
silently no-op when the state is wrong, using `?.let {}` on the cast result.

Example:
```kotlin
// DrawController.kt:90
(state.value as? DrawBoxConnectionState.Connected)?.let { ... }
```

This is appropriate because the `box` layer guarantees connection before gesture callbacks fire.

## No External Error Reporting

**prefer** — The library does not throw exceptions to consumers and does not log errors.
All state transitions are defensive. New code should follow this pattern.

## Null Safety

**prefer** — Avoid `!!` operator. Use safe casts (`as?`), `?.let`, or `?: return`.
The only acceptable `!!` use is immediately after a `require(x != null)` check on the same value.

Example: `DrawController.kt:119` — `activeDrawingPath.value!!` after `require(... != null)` on line 116.
