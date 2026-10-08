package me.riyo.mineiptv.tv;

import me.riyo.mineiptv.ClientBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public final class TelevisionPanelBlock extends Block {
    public static final IntegerProperty X_OFFSET = IntegerProperty.create("x_offset", 0, 3);
    public static final IntegerProperty Y_OFFSET = IntegerProperty.create("y_offset", 0, 2);

    public TelevisionPanelBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(X_OFFSET, 0).setValue(Y_OFFSET, 0));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(BlockStateProperties.HORIZONTAL_FACING, X_OFFSET, Y_OFFSET);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos master = TvStructure.masterPos(pos, state);
        if (level.isClientSide() && master != null) ClientBridge.openTv(master);
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) TvStructure.destroyWhole(level, player, pos, state);
        return super.playerWillDestroy(level, pos, state, player);
    }
}
