# MineIPTV

A client-side Fabric mod that plays IPTV/HLS streams inside Minecraft.

## Target

- Minecraft Java Edition 26.3
- Fabric Loader 0.19.5+
- Fabric API 0.162.0+26.3
- Java 25
- FFmpeg available on PATH, or a custom `ffmpeg.exe` path entered in the player

## Features

- Press `I` to open MineIPTV
- Load remote or local M3U playlists
- Direct HLS/M3U8/HTTP stream playback
- Previous / next channel switching
- Audio playback and volume control
- Remembers playlist URL, FFmpeg path, and volume
- No IPTV provider or bundled channel list

## FFmpeg on Windows

Install FFmpeg, for example:

```powershell
winget install Gyan.FFmpeg
```

Restart Minecraft after installing it so the updated PATH is visible.

If FFmpeg is not on PATH, paste the full path to `ffmpeg.exe` in MineIPTV's FFmpeg field.

## Build

On Windows, double-click `build.bat` or run:

```bat
build.bat
```

The script downloads Gradle 9.6.0 locally on first use, then builds the mod. The jar is written under `build/libs/`.

For development:

```bat
run-client.bat
```

## Usage

1. Start Minecraft with Fabric + Fabric API + MineIPTV.
2. Join/open a world.
3. Press `I`.
4. Paste an M3U playlist URL/path and click **Load M3U**, or paste a direct stream URL and click **Play URL**.
5. Use Prev/Next and Play to switch channels.

### Local playlist

You can enter a normal Windows path, for example:

```text
D:\\IPTV\\channels.m3u
```

or a `file:///...` URI.

## Notes

MineIPTV uses one FFmpeg process. Video is emitted as fixed 512x288 RGBA frames over stdout, while 48 kHz stereo PCM audio is emitted over stderr. FFmpeg logging is disabled while streaming so the audio pipe remains clean.

The current version is intentionally an MVP player UI. A later version can render the same dynamic texture onto an in-world TV block / multi-block screen.

Only use streams and playlists you are authorized to access.
