package me.riyo.mineiptv;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.riyo.mineiptv.tv.TelevisionBlock;
import me.riyo.mineiptv.tv.TelevisionBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class TelevisionRenderer implements BlockEntityRenderer<TelevisionBlockEntity> {
    public TelevisionRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(TelevisionBlockEntity blockEntity, float tickDelta, PoseStack poses,
                       MultiBufferSource buffers, int light, int overlay) {
        if (!TvPlaybackManager.isActive(blockEntity.getBlockPos())) return;
        TvPlaybackManager.player().uploadLatestFrame();

        var state = blockEntity.getBlockState();
        int width = state.getValue(TelevisionBlock.WIDTH);
        int height = state.getValue(TelevisionBlock.HEIGHT);
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(TvPlaybackManager.player().textureId()));
        quad(poses.last(), consumer, facing, width, height);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer c, Direction facing, int width, int height) {
        final float inset = 0.04f;
        final float front = 0.012f;
        final float bottom = inset;
        final float top = height - inset;

        switch (facing) {
            case NORTH -> {
                float z = -front;
                vertex(pose, c, inset, bottom, z, 0, 1, 0, 0, -1);
                vertex(pose, c, width - inset, bottom, z, 1, 1, 0, 0, -1);
                vertex(pose, c, width - inset, top, z, 1, 0, 0, 0, -1);
                vertex(pose, c, inset, top, z, 0, 0, 0, 0, -1);
            }
            case SOUTH -> {
                float z = 1.0f + front;
                float left = 1.0f - inset;
                float right = 1.0f - width + inset;
                vertex(pose, c, left, bottom, z, 0, 1, 0, 0, 1);
                vertex(pose, c, right, bottom, z, 1, 1, 0, 0, 1);
                vertex(pose, c, right, top, z, 1, 0, 0, 0, 1);
                vertex(pose, c, left, top, z, 0, 0, 0, 0, 1);
            }
            case EAST -> {
                float x = 1.0f + front;
                vertex(pose, c, x, bottom, inset, 0, 1, 1, 0, 0);
                vertex(pose, c, x, bottom, width - inset, 1, 1, 1, 0, 0);
                vertex(pose, c, x, top, width - inset, 1, 0, 1, 0, 0);
                vertex(pose, c, x, top, inset, 0, 0, 1, 0, 0);
            }
            case WEST -> {
                float x = -front;
                float left = 1.0f - inset;
                float right = 1.0f - width + inset;
                vertex(pose, c, x, bottom, left, 0, 1, -1, 0, 0);
                vertex(pose, c, x, bottom, right, 1, 1, -1, 0, 0);
                vertex(pose, c, x, top, right, 1, 0, -1, 0, 0);
                vertex(pose, c, x, top, left, 0, 0, -1, 0, 0);
            }
            default -> { }
        }
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer c,
                               float x, float y, float z, float u, float v,
                               float nx, float ny, float nz) {
        c.addVertex(pose.pose(), x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(nx, ny, nz);
    }
}
