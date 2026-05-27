# Dependency Graph — DrawBox

## Project module dependencies (internal imports only)

```
box  ──────► controller ──► model
  │               │
  └───────────────┴──────► util
```

Detailed edges:

| From | To | Via |
|------|----|-----|
| `controller/DrawController` | `model/PathWrapper` | `PathWrapper` data class |
| `controller/DrawController` | `util/StateFlowUtil` | `mapState`, `combineStates` |
| `controller/DrawController` | `util/Util` | `addNotNull`, `createPath` |
| `box/DrawBox` | `controller/*` | `DrawController`, `DrawBoxSubscription`, `OpenedImage`, `DrawBoxBackground` |
| `box/DrawBox` | `model/PathWrapper` | `PathWrapper` |
| `box/DrawBoxCanvas` | `controller/OpenedImage` | `OpenedImage` |
| `box/DrawBoxCanvas` | `model/PathWrapper` | `PathWrapper` |
| `box/DrawBoxCanvas` | `util/Util` | `createPath` |
| `box/DrawBoxBackground` | `controller/DrawBoxBackground` | `DrawBoxBackground` sealed interface |

Notes:
- `controller/DrawBoxConnectionState`, `DrawBoxBackground`, `DrawBoxSubscription`, `OpenedImage`
  have **no internal project imports** — they are pure value types in the controller package.
- `DrawController` imports them as same-package peers (no explicit cross-package import needed).
