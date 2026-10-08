# MineIPTV

A multiplayer IPTV television mod for Minecraft Java Edition.

MineIPTV provides placeable multi-block televisions with M3U/M3U8/HLS playback. In multiplayer, the server synchronizes TV state (channel, URL and play/stop state), while each client decodes the stream locally with FFmpeg. Dedicated servers do not need FFmpeg.

## Development status

This repository is still in development and has not been released yet.

### NeoForge 1.21.1

The `build/neoforge-1.21.1` branch follows the official NeoForge 1.21.1 ModDevGradle MDK configuration and targets Java 21 / Minecraft 1.21.1 / NeoForge 21.1.256+.

## TVs

MineIPTV currently includes these television sizes:

- 1x1
- 2x1
- 2x2
- 3x2
- 4x3

Larger TVs are multi-block structures and render one continuous video surface across the full screen area.

## Playback

Supported sources include:

- M3U playlists
- M3U8 playlists
- HLS streams
- direct HTTP/HTTPS stream URLs supported by FFmpeg

FFmpeg must be installed on each client that wants to watch a stream. It is not required on a dedicated server.

## Multiplayer

The server stores and synchronizes TV state. Video/audio bytes are **not** proxied through the Minecraft server.

The player who places a TV owns its settings. Other players may watch it, but only the owner may change the synchronized stream settings.

## Building NeoForge 1.21.1

```powershell
gradle :neoforge:build
```

The GitHub Actions workflow also validates the generated JAR metadata, Java class version, required resources and absence of Fabric metadata before uploading the artifact.

## License

MIT
