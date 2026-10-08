# Cross-loader target matrix

The architecture is loader-neutral in `common/`. Version-specific build/mapping changes still need compile verification per Minecraft line.

| Minecraft | Fabric | NeoForge | Forge | Status |
|---|---:|---:|---:|---|
| 1.21.1 | target | target | target | backport target |
| 1.21.4 | target | target | target | backport target |
| 1.21.7 | target | target | target | backport target |
| 1.21.10 | target | target | target | backport target |
| 1.21.11 | target | target | target | backport target |
| 26.1 | target | target | target | backport target |
| 26.1.2 | target | target | target | backport target |
| 26.2 | target | target | target | backport target |
| 26.3 | implemented | implemented | implemented | current development baseline |

The old Fabric Stonecutter source also contains 1.21, 1.21.2, 1.21.3, 1.21.5, 1.21.6, 1.21.8, 1.21.9 and 26.1.1 nodes. Those remain useful for Fabric-only compatibility but should not be advertised as Forge/NeoForge builds until loader-specific compile tests pass.
