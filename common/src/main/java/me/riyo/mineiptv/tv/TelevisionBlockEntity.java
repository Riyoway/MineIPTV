package me.riyo.mineiptv.tv;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.UUID;

public final class TelevisionBlockEntity extends BlockEntity {
    private String channelName = "";
    private String streamUrl = "";
    private boolean playing;
    private long revision;
    private UUID owner;

    public TelevisionBlockEntity(BlockPos pos, BlockState state) {
        super(ModTelevisions.televisionBlockEntity(), pos, state);
    }

    public String channelName() { return channelName; }
    public String streamUrl() { return streamUrl; }
    public boolean playing() { return playing; }
    public long revision() { return revision; }
    public UUID owner() { return owner; }

    public boolean canEdit(UUID playerId) {
        return owner == null || owner.equals(playerId);
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public void claimIfUnowned(UUID playerId) {
        if (owner == null && playerId != null) {
            owner = playerId;
            setChanged();
        }
    }

    public void setChannel(String channelName, String streamUrl, boolean playing) {
        this.channelName = channelName == null ? "" : channelName;
        this.streamUrl = streamUrl == null ? "" : streamUrl;
        this.playing = playing;
        this.revision++;
        setChanged();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        output.putString("ChannelName", channelName);
        output.putString("StreamUrl", streamUrl);
        output.putBoolean("Playing", playing);
        output.putLong("Revision", revision);
        if (owner != null) output.putString("Owner", owner.toString());
        super.saveAdditional(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        channelName = input.getStringOr("ChannelName", "");
        streamUrl = input.getStringOr("StreamUrl", "");
        playing = input.getBooleanOr("Playing", false);
        revision = input.getLongOr("Revision", 0L);
        String ownerText = input.getStringOr("Owner", "");
        try {
            owner = ownerText.isBlank() ? null : UUID.fromString(ownerText);
        } catch (IllegalArgumentException ignored) {
            owner = null;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
