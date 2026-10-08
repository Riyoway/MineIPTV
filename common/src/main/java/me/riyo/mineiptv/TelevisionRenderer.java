package me.riyo.mineiptv;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.riyo.mineiptv.tv.TelevisionBlock;
import me.riyo.mineiptv.tv.TelevisionBlockEntity;
//? if >=1.21.9 {
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
//? if >=26.1 {
import net.minecraft.client.renderer.state.level.CameraRenderState;
//?} else {
/*import net.minecraft.client.renderer.state.CameraRenderState;
*///?}
//? if >=1.21.11 {
import net.minecraft.client.renderer.rendertype.RenderTypes;
//?} else {
/*import net.minecraft.client.renderer.RenderType;
*///?}
//?} else {
/*import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
*///?}
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

//? if >=1.21.9 {
public final class TelevisionRenderer implements BlockEntityRenderer<TelevisionBlockEntity, TelevisionRenderer.State> {
    private static final int FULL_BRIGHT = 0x00F000F0;

    public static final class State extends BlockEntityRenderState {
        int width = 1;
        int height = 1;
        Direction facing = Direction.NORTH;
        boolean active;
    }

    public TelevisionRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TelevisionBlockEntity blockEntity, State state, float partialTick, Vec3 cameraPos,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPos, crumblingOverlay);
        var blockState = blockEntity.getBlockState();
        state.width = blockState.getValue(TelevisionBlock.WIDTH);
        state.height = blockState.getValue(TelevisionBlock.HEIGHT);
        state.facing = blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
        state.active = TvPlaybackManager.isActive(blockEntity.getBlockPos());
    }

    @Override
    public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (!state.active) return;
        TvPlaybackManager.player().uploadLatestFrame();
        //? if >=1.21.11 {
        collector.submitCustomGeometry(poses, RenderTypes.entityTranslucent(TvPlaybackManager.player().textureId()),
                (pose, consumer) -> quad(pose, consumer, state.facing, state.width, state.height));
        //?} else {
        /*collector.submitCustomGeometry(poses, RenderType.entityTranslucent(TvPlaybackManager.player().textureId()),
                (pose, consumer) -> quad(pose, consumer, state.facing, state.width, state.height));
        *///?}
    }
//?} else {
/*public final class TelevisionRenderer implements BlockEntityRenderer<TelevisionBlockEntity> {
    private static final int FULL_BRIGHT = 0x00F000F0;

    public TelevisionRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    //? if >=1.21.5 {
    public void render(TelevisionBlockEntity blockEntity, float tickDelta, PoseStack poses,
                       MultiBufferSource buffers, int light, int overlay, Vec3 cameraPos) {
    //?} else {
    /*public void render(TelevisionBlockEntity blockEntity, float tickDelta, PoseStack poses,
                       MultiBufferSource buffers, int light, int overlay) {
    *///?}
        if (!TvPlaybackManager.isActive(blockEntity.getBlockPos())) return;
        TvPlaybackManager.player().uploadLatestFrame();
        var state = blockEntity.getBlockState();
        int width = state.getValue(TelevisionBlock.WIDTH);
        int height = state.getValue(TelevisionBlock.HEIGHT);
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(TvPlaybackManager.player().textureId()));
        quad(poses.last(), consumer, facing, width, height);
    }
*///?}

    /**
     * Draw one continuous screen over the whole multi-block television.
     * The master is the lower-left panel when looking at the front of the TV.
     */
    private static void quad(PoseStack.Pose pose, VertexConsumer c, Direction facing, int width, int height) {
        final float inset = 0.04f;
        final float front = 0.012f;
        final float bottom = inset;
        final float top = height - inset;

        switch (facing) {
            case NORTH -> {
                float z = -front;
                vertex(pose, c, inset, bottom, z, 0, 1, 0, 0, -1, FULL_BRIGHT);
                vertex(pose, c, width - inset, bottom, z, 1, 1, 0, 0, -1, FULL_BRIGHT);
                vertex(pose, c, width - inset, top, z, 1, 0, 0, 0, -1, FULL_BRIGHT);
                vertex(pose, c, inset, top, z, 0, 0, 0, 0, -1, FULL_BRIGHT);
            }
            case SOUTH -> {
                float z = 1.0f + front;
                float left = 1.0f - inset;
                float right = 1.0f - width + inset;
                vertex(pose, c, left, bottom, z, 0, 1, 0, 0, 1, FULL_BRIGHT);
                vertex(pose, c, right, bottom, z, 1, 1, 0, 0, 1, FULL_BRIGHT);
                vertex(pose, c, right, top, z, 1, 0, 0, 0, 1, FULL_BRIGHT);
                vertex(pose, c, left, top, z, 0, 0, 0, 0, 1, FULL_BRIGHT);
            }
            case EAST -> {
                float x = 1.0f + front;
                vertex(pose, c, x, bottom, inset, 0, 1, 1, 0, 0, FULL_BRIGHT);
                vertex(pose, c, x, bottom, width - inset, 1, 1, 1, 0, 0, FULL_BRIGHT);
                vertex(pose, c, x, top, width - inset, 1, 0, 1, 0, 0, FULL_BRIGHT);
                vertex(pose, c, x, top, inset, 0, 0, 1, 0, 0, FULL_BRIGHT);
            }
            case WEST -> {
                float x = -front;
                float left = 1.0f - inset;
                float right = 1.0f - width + inset;
                vertex(pose, c, x, bottom, left, 0, 1, -1, 0, 0, FULL_BRIGHT);
                vertex(pose, c, x, bottom, right, 1, 1, -1, 0, 0, FULL_BRIGHT);
                vertex(pose, c, x, top, right, 1, 0, -1, 0, 0, FULL_BRIGHT);
                vertex(pose, c, x, top, left, 0, 0, -1, 0, 0, FULL_BRIGHT);
            }
            default -> { }
        }
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer c,
                               float x, float y, float z, float u, float v,
                               float nx, float ny, float nz, int light) {
        c.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
