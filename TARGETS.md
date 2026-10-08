# MineIPTV compatibility targets

This file is the source of truth for the compatibility audit. A target is **not supported** merely because the source compiles on a nearby Minecraft version. Every advertised loader/version pair must pass its own build and runtime checks against that exact Minecraft release.

## Minecraft releases in scope

All stable Java Edition releases from Minecraft 1.21 through the current 26.3 release are in scope:

- 1.21
- 1.21.1
- 1.21.2
- 1.21.3
- 1.21.4
- 1.21.5
- 1.21.6
- 1.21.7
- 1.21.8
- 1.21.9
- 1.21.10
- 1.21.11
- 26.1
- 26.1.1
- 26.1.2
- 26.2
- 26.3

Snapshots, pre-releases and release candidates are not release targets.

## Loader matrix

The loaders in this repository are Fabric, NeoForge and Minecraft Forge. The matrix below intentionally distinguishes "target" from "verified".

| Minecraft | Java | Fabric | NeoForge | Forge | Audit state |
|---|---:|---|---|---|---|
| 1.21 | 21 | target | target | target | pending exact build/runtime audit |
| 1.21.1 | 21 | target | verified separately | target | NeoForge audit exists; Fabric/Forge pending |
| 1.21.2 | 21 | target | target | **do not advertise yet** | no official Forge release artifact has been established in the Forge download index; keep disabled unless official evidence is found |
| 1.21.3 | 21 | target | target | target | pending exact build/runtime audit |
| 1.21.4 | 21 | target | target | target | pending exact build/runtime audit |
| 1.21.5 | 21 | target | target | target | pending exact build/runtime audit |
| 1.21.6 | 21 | target | target | target | pending exact build/runtime audit |
| 1.21.7 | 21 | target | target | target | pending exact build/runtime audit |
| 1.21.8 | 21 | target | target | target | pending exact build/runtime audit |
| 1.21.9 | 21 | target | target | target | pending exact build/runtime audit |
| 1.21.10 | 21 | target | target | target | pending exact build/runtime audit |
| 1.21.11 | 21 | target | target | target | pending exact build/runtime audit |
| 26.1 | 25 | target | target | target | pending exact build/runtime audit |
| 26.1.1 | 25 | target | target | target | pending exact build/runtime audit |
| 26.1.2 | 25 | target | target | target | pending exact build/runtime audit |
| 26.2 | 25 | target | target | target | pending exact build/runtime audit |
| 26.3 | 25 | implemented | implemented | implemented | builds on main; full packaged runtime audit still required for each loader |

## Toolchain boundaries that must not be guessed around

- Minecraft 1.21 through 1.21.11 use Java 21.
- Minecraft 26.1 and newer use Java 25.
- Fabric Loom distinguishes obfuscated Minecraft 1.21.11 and older from unobfuscated Minecraft 26.1 and newer. The documented plugin path changes at this boundary; one Loom configuration must not be assumed valid for both eras.
- NeoForge must use the official per-version MDK / ModDevGradle or NeoGradle configuration for each target. Loader ranges, NeoForge coordinates and metadata are taken from the exact MDK, not inferred from another version.
- Forge must use the exact Forge MDK/artifact published for that Minecraft release. A mapping/config artifact existing for a version is not proof that a Forge loader release exists.
- Resource/data paths, item models, networking APIs, client input APIs and rendering APIs must be checked at every vanilla migration boundary documented by the loader/vanilla migration guides.

## Required verification for every loader/version pair

A cell only becomes `verified` after all of the following pass for the exact pair:

1. Build with the correct JDK and the loader's documented Gradle toolchain.
2. Inspect the final JAR metadata and reject metadata from another loader.
3. Validate all JSON resources and required MineIPTV assets/data files.
4. Validate class-file Java version.
5. Start a clean loader installation with the **packaged MineIPTV JAR placed in `mods/`** and prove the loader lists MineIPTV.
6. Reach dedicated-server ready state without MineIPTV exceptions.
7. Launch a client and verify the client entrypoint/key binding, TV renderer, GUI and FFmpeg playback path.
8. Verify multiplayer packet registration and a real client/server TV state update.
9. Verify placement, multiblock destruction/drop behavior, recipes, blockstates, item models and saved block-entity state.
10. Record any version-specific compatibility shim instead of widening a version range by assumption.

`build/neoforge-1.21.1` is only the NeoForge 1.21.1 audit. It must not be used as evidence for another loader or Minecraft version.
