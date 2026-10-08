package me.riyo.mineiptv.tv;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class TvStructure {
    private TvStructure() {}

    public static BlockPos masterPos(BlockPos pos, BlockState state) {
        if (state.is(ModTelevisions.television())) return pos;
        if (!state.is(ModTelevisions.panel())) return null;
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        Direction right = facing.getClockWise();
        return pos.relative(right.getOpposite(), state.getValue(TelevisionPanelBlock.X_OFFSET))
                .below(state.getValue(TelevisionPanelBlock.Y_OFFSET));
    }

    public static void destroyWhole(Level level, Player player, BlockPos brokenPos, BlockState brokenState) {
        BlockPos masterPos = masterPos(brokenPos, brokenState);
        if (masterPos == null) return;
        BlockState masterState = brokenState.is(ModTelevisions.television()) ? brokenState : level.getBlockState(masterPos);
        if (!masterState.is(ModTelevisions.television())) return;

        int width = masterState.getValue(TelevisionBlock.WIDTH);
        int height = masterState.getValue(TelevisionBlock.HEIGHT);
        Direction right = masterState.getValue(BlockStateProperties.HORIZONTAL_FACING).getClockWise();
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            BlockPos part = masterPos.relative(right, x).above(y);
            if (part.equals(brokenPos)) continue;
            BlockState partState = level.getBlockState(part);
            if (partState.is(ModTelevisions.television()) || partState.is(ModTelevisions.panel())) {
                level.setBlock(part, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        if (!player.getAbilities().instabuild) {
            ItemStack drop = new ItemStack(ModTelevisions.itemFor(TvSize.from(width, height)));
            level.addFreshEntity(new ItemEntity(level, masterPos.getX()+0.5, masterPos.getY()+0.5, masterPos.getZ()+0.5, drop));
        }
    }
}
