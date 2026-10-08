# MineIPTV

Minecraft television / IPTV mod. This development branch is structured as a **multi-loader + multiplayer** mod.

## Current cross-loader target

- Minecraft **26.3**
- Fabric Loader + Fabric API
- NeoForge
- Minecraft Forge
- Java 25
- Common Network 26.3-1.1.1 (required on both client and server)

Each loader produces its own jar. They share the same `common/` game logic and packet format.

## Multiplayer design

The dedicated server **does not decode or relay video**. It only owns/saves/synchronizes the television state:

- master TV block position
- channel name
- HTTP/HTTPS stream URL
- playing/stopped state
- state revision
- TV owner (the player who placed it)

When a player changes a TV, a validated serverbound payload updates the TV block entity. Vanilla block-entity update packets then synchronize that state to tracking clients. Each client decodes the stream locally with FFmpeg. This prevents IPTV traffic from passing through the Minecraft server.

### Server requirements

- MineIPTV jar for the server's loader
- Common Network jar for the same loader/version
- **FFmpeg is not required on the server**

### Client requirements

- MineIPTV jar matching the client's loader/version
- Common Network jar
- FFmpeg available as `ffmpeg` on PATH, or configured in MineIPTV

## TV gameplay

TVs are real multi-block structures. Sizes:

`1x1 -> 2x1 -> 2x2 -> 3x2 -> 4x3`

All crafting uses vanilla Minecraft items. Upgrade recipes consume the previous MineIPTV TV plus vanilla materials. Right-click any panel to control the master TV. Breaking a panel removes the complete structure and returns one correctly-sized TV item.

## Multiplayer safety

Serverbound TV changes are validated server-side:

- only the player who placed the TV can change its channel/state
- player must be within 8 blocks of the TV
- target must be a MineIPTV TV block entity
- synchronized streams must be HTTP or HTTPS
- URLs containing embedded credentials are rejected
- URL and channel-name lengths are capped

Local standalone playback may still use local paths; only server-synchronized TV streams are restricted.

## Build

On Windows, the included bootstrap downloads a private Java 25 + Gradle 9.7.1 toolchain when needed:

```powershell
.\build.bat all
.\build.bat fabric
.\build.bat neoforge
.\build.bat forge
```

If Java 25 and Gradle 9.7.1 are already installed, you can also run:

```powershell
gradle :fabric:build
gradle :neoforge:build
gradle :forge:build
```

Artifacts are generated in each loader module's `build/libs/` directory.

## Version coverage

The previous Fabric/Stonecutter branch contains the 1.21 -> 26.3 source adaptations. This cross-loader branch starts at 26.3 so the multiplayer/networking architecture can be debugged on one Minecraft version before backporting it. See `TARGETS.md` for the planned cross-loader baselines.

This is still a **development build**, not a release/tag candidate.
