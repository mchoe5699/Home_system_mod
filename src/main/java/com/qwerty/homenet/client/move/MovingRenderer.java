package com.qwerty.homenet.client.move;

import com.mojang.blaze3d.vertex.PoseStack;
import com.qwerty.homenet.move.DeviceOffsets;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;

public final class MovingRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    private final BlockEntityRenderer<T> delegate;

    public MovingRenderer(BlockEntityRenderer<T> renderer) {
        this.delegate = renderer;
    }

    public static <T extends BlockEntity> void register(RegisterRenderers e, BlockEntityType<T> type, BlockEntityRendererProvider<T> provider) {
        e.registerBlockEntityRenderer(type, c -> new MovingRenderer<>(provider.create(c)));
    }

    @Override
    public void render(T b, float tick, PoseStack p, MultiBufferSource out, int light, int overlay) {
        Vec3 v = DeviceOffsets.get(b.getLevel(), b.getBlockPos());
        p.pushPose();
        p.translate(v.x, v.y, v.z);
        this.delegate.render(b, tick, p, out, light, overlay);
        p.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(T b) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return this.delegate.getViewDistance();
    }
}
