package me.riyo.mineiptv.tv;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.ArrayList;
import java.util.List;

public final class TelevisionItem extends Item {
    private final TvSize size;

    public TelevisionItem(TvSize size, Properties properties) {
        super(properties);
        this.size = size;
    }

    public TvSize size() { return size; }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        ItemStack stack = context.getItemInHand();
        BlockPos origin = context.getClickedPos().relative(context.getClickedFace());
        Direction facing = context.getHorizontalDirection().getOpposite();
        Direction right = facing.getClockWise();
        CollisionContext collision = player == null ? CollisionContext.empty() : CollisionContext.of(player);

        BlockState master = ModTelevisions.television().defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                .setValue(TelevisionBlock.WIDTH, size.width())
                .setValue(TelevisionBlock.HEIGHT, size.height());

        // Validate the entire footprint before changing the world. This mirrors the relevant
        // vanilla BlockItem placement guards for bounds, permissions, replacement and collision.
        for (int y = 0; y < size.height(); y++) {
            for (int x = 0; x < size.width(); x++) {
                BlockPos pos = origin.relative(right, x).above(y);
                if (!level.isInWorldBounds(pos)) return InteractionResult.FAIL;
                if (player != null && (!level.mayInteract(player, pos)
                        || !player.mayUseItemAt(pos, context.getClickedFace(), stack))) {
                    return InteractionResult.FAIL;
                }
                if (!level.getBlockState(pos).canBeReplaced()) return InteractionResult.FAIL;

                BlockState candidate = (x == 0 && y == 0) ? master : ModTelevisions.panel().defaultBlockState()
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                        .setValue(TelevisionPanelBlock.X_OFFSET, x)
                        .setValue(TelevisionPanelBlock.Y_OFFSET, y);
                if (!level.isUnobstructed(candidate, pos, collision)) return InteractionResult.FAIL;
            }
        }

        List<BlockPos> placed = new ArrayList<>(size.width() * size.height());
        if (!level.setBlock(origin, master, Block.UPDATE_ALL)) return InteractionResult.FAIL;
        placed.add(origin);

        for (int y = 0; y < size.height(); y++) {
            for (int x = 0; x < size.width(); x++) {
                if (x == 0 && y == 0) continue;
                BlockPos pos = origin.relative(right, x).above(y);
                BlockState panel = ModTelevisions.panel().defaultBlockState()
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                        .setValue(TelevisionPanelBlock.X_OFFSET, x)
                        .setValue(TelevisionPanelBlock.Y_OFFSET, y);
                if (!level.setBlock(pos, panel, Block.UPDATE_ALL)) {
                    for (BlockPos placedPos : placed) {
                        level.setBlock(placedPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    }
                    return InteractionResult.FAIL;
                }
                placed.add(pos);
            }
        }

        if (player != null && level.getBlockEntity(origin) instanceof TelevisionBlockEntity television) {
            television.setOwner(player.getUUID());
        }
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
