package me.riyo.mineiptv.network;

import commonnetwork.api.Dispatcher;
import commonnetwork.api.Network;
import commonnetwork.networking.data.Side;
import me.riyo.mineiptv.tv.TelevisionBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public final class MineIptvNetwork {
    private MineIptvNetwork() {}

    public static void initialize() {
        Network.registerPacket(TvControlPayload.TYPE, TvControlPayload.STREAM_CODEC, context -> {
            if (context.side() != Side.SERVER) return;
            ServerPlayer player = context.sender();
            TvControlPayload payload = context.message();
            if (player == null) return;

            BlockPos pos = payload.pos();
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) return;
            if (!(player.level().getBlockEntity(pos) instanceof TelevisionBlockEntity television)) return;
            television.claimIfUnowned(player.getUUID());
            if (!television.canEdit(player.getUUID())) return;

            String name = payload.channelName() == null ? "" : payload.channelName().trim();
            if (name.length() > 256) name = name.substring(0, 256);
            String url = payload.streamUrl() == null ? "" : payload.streamUrl().trim();

            if (payload.playing() && !StreamUrlPolicy.allowed(url)) return;
            television.setChannel(name, url, payload.playing());
        });
    }

    public static void setTelevision(BlockPos pos, String channelName, String streamUrl, boolean playing) {
        Dispatcher.sendToServer(new TvControlPayload(pos, channelName == null ? "" : channelName,
                streamUrl == null ? "" : streamUrl, playing));
    }
}
