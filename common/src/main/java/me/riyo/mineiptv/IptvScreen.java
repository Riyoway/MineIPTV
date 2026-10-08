package me.riyo.mineiptv;

import me.riyo.mineiptv.network.MineIptvNetwork;
import me.riyo.mineiptv.tv.TelevisionBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class IptvScreen extends Screen {
    private final MineIptvConfig config = MineIptvConfig.load();
    private final FfmpegPlayer player = TvPlaybackManager.player();
    private final List<Channel> channels = new ArrayList<>();

    private EditBox sourceBox;
    private EditBox ffmpegBox;
    private int selected = -1;
    private volatile String message = "Paste an M3U playlist URL/path, or play a stream URL directly.";
    private volatile boolean loading;

    public IptvScreen(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        int controlsX = Math.max(12, this.width - 174);
        int controlsW = 162;

        sourceBox = new EditBox(this.font, controlsX, 40, controlsW, 20, Component.literal("Playlist / stream URL"));
        sourceBox.setMaxLength(4096);
        sourceBox.setValue(config.playlist);
        sourceBox.setHint(Component.literal("https://.../playlist.m3u"));
        addRenderableWidget(sourceBox);

        addRenderableWidget(Button.builder(Component.literal("Load M3U"), btn -> loadPlaylist())
                .bounds(controlsX, 66, 78, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Play URL"), btn -> playDirect())
                .bounds(controlsX + 84, 66, 78, 20).build());

        addRenderableWidget(Button.builder(Component.literal("< Prev"), btn -> selectRelative(-1))
                .bounds(controlsX, 92, 78, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Next >"), btn -> selectRelative(1))
                .bounds(controlsX + 84, 92, 78, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Play"), btn -> playSelected())
                .bounds(controlsX, 118, 78, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Stop"), btn -> stopPlayback())
                .bounds(controlsX + 84, 118, 78, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Vol -"), btn -> changeVolume(-10))
                .bounds(controlsX, 144, 78, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Vol +"), btn -> changeVolume(10))
                .bounds(controlsX + 84, 144, 78, 20).build());

        ffmpegBox = new EditBox(this.font, controlsX, 190, controlsW, 20, Component.literal("FFmpeg path"));
        ffmpegBox.setMaxLength(1024);
        ffmpegBox.setValue(config.ffmpeg);
        ffmpegBox.setHint(Component.literal("ffmpeg / C:\\...\\ffmpeg.exe"));
        addRenderableWidget(ffmpegBox);

        player.setVolume(config.volume);

        if (TvPlaybackManager.activeTv() != null && this.minecraft != null && this.minecraft.level != null
                && this.minecraft.level.getBlockEntity(TvPlaybackManager.activeTv()) instanceof TelevisionBlockEntity tv) {
            if (!tv.streamUrl().isBlank()) sourceBox.setValue(tv.streamUrl());
            message = tv.playing() ? "Server TV: " + tv.channelName() : "Server TV is stopped.";
        }
    }

    private void loadPlaylist() {
        if (loading) return;
        String source = sourceBox.getValue().trim();
        if (source.isEmpty()) {
            message = "Enter a playlist URL/path first.";
            return;
        }

        config.playlist = source;
        config.ffmpeg = ffmpegBox.getValue().trim();
        config.save();
        loading = true;
        message = "Loading playlist...";

        Thread.ofVirtual().name("MineIPTV-playlist").start(() -> {
            try {
                List<Channel> loaded = PlaylistLoader.load(source);
                if (this.minecraft != null) this.minecraft.execute(() -> {
                    channels.clear();
                    channels.addAll(loaded);
                    selected = channels.isEmpty() ? -1 : 0;
                    loading = false;
                    message = "Loaded " + channels.size() + " channel(s).";
                });
            } catch (Exception e) {
                if (this.minecraft != null) this.minecraft.execute(() -> {
                    loading = false;
                    message = "Load failed: " + safeMessage(e);
                });
            }
        });
    }

    private void playDirect() {
        String url = sourceBox.getValue().trim();
        if (url.isEmpty()) {
            message = "Enter a stream URL first.";
            return;
        }
        channels.clear();
        channels.add(new Channel("Direct stream", url, ""));
        selected = 0;
        playSelected();
    }

    private void selectRelative(int delta) {
        if (channels.isEmpty()) {
            message = "Load a playlist first.";
            return;
        }
        selected = Math.floorMod(selected + delta, channels.size());
        message = "Selected: " + channels.get(selected).name();
    }

    private void playSelected() {
        if (selected < 0 || selected >= channels.size()) {
            message = "No channel selected.";
            return;
        }

        config.playlist = sourceBox.getValue().trim();
        config.ffmpeg = ffmpegBox.getValue().trim().isEmpty() ? "ffmpeg" : ffmpegBox.getValue().trim();
        config.save();
        player.setVolume(config.volume);

        Channel channel = channels.get(selected);
        if (TvPlaybackManager.activeTv() != null) {
            MineIptvNetwork.setTelevision(TvPlaybackManager.activeTv(), channel.name(), channel.url(), true);
            message = "Syncing TV: " + channel.name();
            return;
        }
        try {
            player.play(config.ffmpeg, channel.url());
            message = "Opening: " + channel.name();
        } catch (Exception e) {
            message = "Could not start FFmpeg: " + safeMessage(e);
        }
    }

    private void stopPlayback() {
        if (TvPlaybackManager.activeTv() != null) {
            String url = selected >= 0 && selected < channels.size() ? channels.get(selected).url() : sourceBox.getValue().trim();
            String name = selected >= 0 && selected < channels.size() ? channels.get(selected).name() : "";
            MineIptvNetwork.setTelevision(TvPlaybackManager.activeTv(), name, url, false);
            message = "Stopping server TV...";
        } else {
            player.stop();
            message = "Stopped.";
        }
    }

    private void changeVolume(int delta) {
        config.volume = Math.max(0, Math.min(100, config.volume + delta));
        config.save();
        player.setVolume(config.volume);
        message = "Volume: " + config.volume + "%";
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        player.uploadLatestFrame();

        int videoX = 12;
        int videoY = 40;
        int videoMaxW = Math.max(128, this.width - 204);
        int videoMaxH = Math.max(72, this.height - 74);
        float scale = Math.min((float) videoMaxW / FfmpegPlayer.FRAME_WIDTH, (float) videoMaxH / FfmpegPlayer.FRAME_HEIGHT);
        int videoW = Math.max(1, Math.round(FfmpegPlayer.FRAME_WIDTH * scale));
        int videoH = Math.max(1, Math.round(FfmpegPlayer.FRAME_HEIGHT * scale));

        graphics.fill(videoX - 1, videoY - 1, videoX + videoW + 1, videoY + videoH + 1, 0xFF303030);
        graphics.blit(player.textureId(), videoX, videoY, 0, 0,
                videoW, videoH, FfmpegPlayer.FRAME_WIDTH, FfmpegPlayer.FRAME_HEIGHT);
        graphics.drawString(this.font, this.title, 12, 14, 0xFFFFFFFF, true);
        graphics.drawString(this.font, truncate(message, 80), 12, this.height - 22, 0xFFBFBFBF, false);

        int controlsX = Math.max(12, this.width - 174);
        graphics.drawString(this.font, "Source", controlsX, 28, 0xFFFFFFFF, false);
        graphics.drawString(this.font, "FFmpeg", controlsX, 178, 0xFFFFFFFF, false);
        graphics.drawString(this.font, "Volume: " + config.volume + "%", controlsX, 218, 0xFFFFFFFF, false);
        graphics.drawString(this.font, "Status: " + truncate(player.status(), 24), controlsX, 232, 0xFFBFBFBF, false);
        String channelText = selected >= 0 && selected < channels.size()
                ? (selected + 1) + "/" + channels.size() + "  " + channels.get(selected).name() : "No channel";
        graphics.drawString(this.font, truncate(channelText, 28), controlsX, 250, 0xFFFFFFFF, false);
        if (selected >= 0 && selected < channels.size() && !channels.get(selected).group().isBlank()) {
            graphics.drawString(this.font, truncate(channels.get(selected).group(), 28), controlsX, 264, 0xFF8F8F8F, false);
        }
    }

    @Override
    public void removed() {
        config.playlist = sourceBox == null ? config.playlist : sourceBox.getValue().trim();
        config.ffmpeg = ffmpegBox == null ? config.ffmpeg : ffmpegBox.getValue().trim();
        config.save();
        super.removed();
    }

    private static String safeMessage(Exception e) {
        String text = e.getMessage();
        return text == null || text.isBlank() ? e.getClass().getSimpleName() : text;
    }

    private static String truncate(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, Math.max(0, max - 1)) + "…";
    }
}
