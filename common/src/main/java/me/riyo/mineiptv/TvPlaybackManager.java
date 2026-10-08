package me.riyo.mineiptv;

import me.riyo.mineiptv.network.StreamUrlPolicy;
import me.riyo.mineiptv.tv.TelevisionBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public final class TvPlaybackManager {
    private static final FfmpegPlayer PLAYER = new FfmpegPlayer();
    private static BlockPos activeTv;
    private static long appliedRevision = Long.MIN_VALUE;

    private TvPlaybackManager() {}
    public static FfmpegPlayer player() { return PLAYER; }
    public static BlockPos activeTv() { return activeTv; }

    public static void activate(BlockPos pos) {
        activeTv = pos == null ? null : pos.immutable();
        appliedRevision = Long.MIN_VALUE;
    }

    public static boolean isActive(BlockPos pos) { return activeTv != null && activeTv.equals(pos); }

    /** Applies the server-synchronized block entity state to this client's local FFmpeg player. */
    public static void tick(Minecraft client) {
        if (client.level == null) {
            if (PLAYER.isRunning()) PLAYER.stop();
            activeTv = null;
            appliedRevision = Long.MIN_VALUE;
            return;
        }
        if (activeTv == null) return;
        if (!(client.level.getBlockEntity(activeTv) instanceof TelevisionBlockEntity tv)) {
            PLAYER.stop();
            activeTv = null;
            appliedRevision = Long.MIN_VALUE;
            return;
        }
        if (tv.revision() == appliedRevision) return;
        appliedRevision = tv.revision();

        if (!tv.playing() || tv.streamUrl().isBlank() || !StreamUrlPolicy.allowed(tv.streamUrl())) {
            PLAYER.stop();
            return;
        }
        MineIptvConfig cfg = MineIptvConfig.load();
        PLAYER.setVolume(cfg.volume);
        try {
            PLAYER.play(cfg.ffmpeg == null || cfg.ffmpeg.isBlank() ? "ffmpeg" : cfg.ffmpeg, tv.streamUrl());
        } catch (Exception ignored) {
            // FfmpegPlayer exposes the start error in its status string and keeps the game alive.
        }
    }

    public static void close() {
        PLAYER.close();
        activeTv = null;
        appliedRevision = Long.MIN_VALUE;
    }
}
