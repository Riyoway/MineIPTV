package me.riyo.mineiptv.network;

import me.riyo.mineiptv.MineIptv;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record TvControlPayload(BlockPos pos, String channelName, String streamUrl, boolean playing)
        implements CustomPacketPayload {
    public static final Type<TvControlPayload> TYPE = new Type<>(MineIptv.id("tv_control"));
    public static final StreamCodec<FriendlyByteBuf, TvControlPayload> STREAM_CODEC = StreamCodec.ofMember(
            TvControlPayload::write, TvControlPayload::new);

    public TvControlPayload(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readUtf(256), buf.readUtf(4096), buf.readBoolean());
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(channelName, 256);
        buf.writeUtf(streamUrl, 4096);
        buf.writeBoolean(playing);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
