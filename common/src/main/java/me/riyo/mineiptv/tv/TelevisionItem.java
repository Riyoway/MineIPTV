package me.riyo.mineiptv.tv;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

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

        BlockPos origin = context.getClickedPos().relative(context.getClickedFace());
        Direction facing = context.getHorizontalDirection().getOpposite();
        Direction right = facing.getClockWise();

        for (int y = 0; y < size.height(); y++) {
            for (int x = 0; x < size.width(); x++) {
                BlockPos pos = origin.relative(right, x).above(y);
                if (!level.getBlockState(pos).canBeReplaced()) {
                    return InteractionResult.FAIL;
                }
            }
        }

        BlockState master = ModTelevisions.television().defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                .setValue(TelevisionBlock.WIDTH, size.width())
                .setValue(TelevisionBlock.HEIGHT, size.height());
        level.setBlock(origin, master, 3);
        if (player != null && level.getBlockEntity(origin) instanceof TelevisionBlockEntity television) {
            television.setOwner(player.getUUID());
        }

        for (int y = 0; y < size.height(); y++) {
            for (int x = 0; x < size.width(); x++) {
                if (x == 0 && y == 0) continue;
                BlockPos pos = origin.relative(right, x).above(y);
                BlockState panel = ModTelevisions.panel().defaultBlockState()
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                        .setValue(TelevisionPanelBlock.X_OFFSET, x)
                        .setValue(TelevisionPanelBlock.Y_OFFSET, y);
                level.setBlock(pos, panel, 3);
            }
        }

        if (player == null || !player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
