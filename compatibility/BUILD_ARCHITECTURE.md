# Multi-version build architecture

MineIPTV targets Fabric, NeoForge and Forge across every stable Minecraft release from 1.21 through 26.3. The build must preserve each loader/version pair's real upstream toolchain instead of forcing all targets through one Gradle/plugin stack.

## Why one Gradle build is not enough

The exact upstream audit proves that Minecraft version alone is not a safe proxy for the build toolchain.

Fabric's current official example branches use Gradle 9.7.1 and Loom 1.18-SNAPSHOT, but the Loom plugin changes at the 26.1 unobfuscation boundary:

- 1.21 through 1.21.11: `net.fabricmc.fabric-loom-remap`
- 26.1 and newer: `net.fabricmc.fabric-loom`

NeoForge's current official ModDevGradle MDKs use Gradle 9.2.1 and ModDevGradle 2.0.148, with an exact NeoForge coordinate per Minecraft release.

Forge cannot safely be put into a single wrapper lane. The published MDKs in this compatibility window currently require multiple toolchain generations:

| Build lane | Minecraft releases | ForgeGradle | Gradle |
|---|---|---|---|
| Forge 8.7 | 1.21 | `[6.0.24,6.2)` | 8.7 |
| Forge 8.12.1 | 1.21.6, 1.21.7, 1.21.9 | `[6.0.36,6.2)` | 8.12.1 |
| Forge 9.3.1 | 1.21.1, 1.21.3, 1.21.4, 1.21.5, 1.21.8, 1.21.10, 26.1, 26.1.1 | FG7 (`[7.0.3,8)` on 1.21.x; `[7.0.17,8)` on 26.x) | 9.3.1 |
| Forge 9.5.0 | 1.21.11, 26.1.2, 26.2 | FG7 | 9.5.0 |
| Forge 9.7.1 | 26.3 | `[7.0.17,8)` | 9.7.1 |

Forge 1.21.2 is intentionally absent because the official Forge Maven metadata contains no 1.21.2 loader artifact.

## Source strategy

There is one canonical MineIPTV source tree. Version differences are represented explicitly with source preprocessing / compatibility shims. A neighboring Minecraft version is never accepted merely because it happens to compile.

Stonecutter is used for source-version selection, not as permission to replace a loader's documented toolchain. Exact builds are executed in loader-specific lanes:

1. Fabric lane — official Fabric/Loom values for the exact target.
2. NeoForge lane — exact official NeoForge ModDevGradle MDK values.
3. Forge lanes — exact published MDK values grouped only when their wrapper/plugin requirements actually match.

The source preprocessor may share code between cells, but the final build and runtime verification are always performed for the exact cell.

## Verification stages

Every loader/version pair moves through these states independently:

1. `upstream-verified` — dependency/plugin/JDK/wrapper values match official upstream sources.
2. `compiles` — MineIPTV source compiles against that exact target.
3. `jar-validated` — final metadata, class version and resources are correct for that target.
4. `server-verified` — a clean loader installation recognizes the packaged MineIPTV JAR and reaches dedicated-server ready state.
5. `client-verified` — a real client reaches title/world, registers the keybind and renderer, opens the UI, and can invoke FFmpeg playback.
6. `multiplayer-verified` — a client/server session places TVs and synchronizes TV state through the target loader's networking path.
7. `verified` — all above checks pass.

No row in `TARGETS.md` should be described as supported before the last stage.

## Sources of truth

- `compatibility/toolchains.json`: pinned exact upstream versions.
- `.github/scripts/audit_upstream_toolchains.py`: continuously re-fetches official Fabric examples, NeoForge MDKs, Forge Maven metadata and exact Forge MDK ZIPs.
- `compatibility/targets.json`: desired compatibility matrix and current verification state.
- `TARGETS.md`: human-readable status.

When an upstream MDK/example changes, CI must fail first. The pinned matrix is then updated only after the new value is confirmed from the exact source.
