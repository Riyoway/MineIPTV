package me.riyo.mineiptv.network;

import me.riyo.mineiptv.tv.TelevisionBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.function.Consumer;

public final class MineIptvNetwork {
    private static Consumer<TvControlPayload> clientSender = payload -> {};

    private MineIptvNetwork() {}

    public static void initialize() {
        // Loader-specific networking is registered by the platform module.
    }

    public static void installClientSender(Consumer<TvControlPayload> sender) {
        clientSender = Objects.requireNonNull(sender);
    }

    public static void handleServer(TvControlPayload payload, ServerPlayer player) {
        if (payload == null || player == null) return;

        BlockPos pos = payload.pos();
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) return;
        if (!player.level().hasChunkAt(pos)) return;
        if (!(player.level().getBlockEntity(pos) instanceof TelevisionBlockEntity television)) return;
        television.claimIfUnowned(player.getUUID());
        if (!television.canEdit(player.getUUID())) return;

        String name = payload.channelName() == null ? "" : payload.channelName().trim();
        if (name.length() > 256) name = name.substring(0, 256);
        String url = payload.streamUrl() == null ? "" : payload.streamUrl().trim();

        if (payload.playing() && !StreamUrlPolicy.allowed(url)) return;
        television.setChannel(name, url, payload.playing());
    }

    public static void setTelevision(BlockPos pos, String channelName, String streamUrl, boolean playing) {
        if (pos == null) return;
        clientSender.accept(new TvControlPayload(pos, channelName == null ? "" : channelName,
                streamUrl == null ? "" : streamUrl, playing));
    }
}
