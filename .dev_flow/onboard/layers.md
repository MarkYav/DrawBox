# Dependency Layers — DrawBox

Processing order for analysis and doc generation (lower layers first):

## Layer 0 — Leaf modules (no internal project dependencies)

| Module | Path | Files |
|--------|------|-------|
| `util` | `drawbox/.../util/` | StateFlowUtil.kt, Util.kt |
| `model` | `drawbox/.../model/` | PathWrapper.kt |

Note: controller value types (`DrawBoxConnectionState`, `DrawBoxBackground`,
`DrawBoxSubscription`, `OpenedImage`) are also leaf nodes but are grouped with
the `controller` module conceptually.

## Layer 1 — Depends on Layer 0

| Module | Path | Internal deps |
|--------|------|--------------|
| `controller` | `drawbox/.../controller/` | `model`, `util` |

## Layer 2 — Depends on Layers 0–1

| Module | Path | Internal deps |
|--------|------|--------------|
| `box` | `drawbox/.../box/` | `controller`, `model`, `util` |

## Circular dependencies

None detected.
