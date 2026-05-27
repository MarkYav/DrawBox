# Naming Conventions

## Classes and Interfaces

**must** — Sealed interfaces and their variants follow the pattern `DrawBox<Noun>`:
`DrawBoxConnectionState`, `DrawBoxBackground`, `DrawBoxSubscription`.
Object variants use PascalCase nouns: `Disconnected`, `Connected`, `NoBackground`, `DynamicUpdate`.
Data class variants use descriptive PascalCase: `ColourBackground`, `ImageBackground`.

Example: `controller/DrawBoxBackground.kt`, `controller/DrawBoxConnectionState.kt`

**must** — The primary public Composable is named after the library: `DrawBox`.
Internal Composables are prefixed with the public name: `DrawBoxCanvas`, `DrawBoxBackground`.

**should** — The controller class is named `DrawController` (not `DrawBoxController`) —
consistent with the library being called DrawBox but the controller API being shorter.

## Properties and Functions

**should** — MutableStateFlow properties use noun names (no `mutable` prefix):
`canvasOpacity`, `strokeWidth`, `color`, `background`, `opacity`.
Derived read-only StateFlows use noun names: `undoCount`, `redoCount`.

Example: `controller/DrawController.kt:31-49`

**should** — Internal-only functions that interact with the Compose gesture system
use verb phrases: `insertNewPath`, `updateLatestPath`, `finalizePath`, `connectToDrawBox`, `onTap`.

**prefer** — Temporary mutable list copies inside functions use underscore prefix:
`_drawnPaths`, `_canceledPaths`. Do not promote this pattern to long-lived variables.

## Packages

**must** — Package structure mirrors module responsibility:
`util` (helpers) → `model` (data) → `controller` (logic) → `box` (UI).
No cross-cutting packages (no `common`, `shared`, `base`).
