package com.qwerty.homenet.client.guard;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.qwerty.homenet.HomeNet;
import com.qwerty.homenet.block.GuardMasterBlock;
import com.qwerty.homenet.block.WallpadBlock;
import com.qwerty.homenet.blockentity.GuardMasterBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

public class GuardMasterRenderer implements BlockEntityRenderer<GuardMasterBlockEntity> {
    public static final ResourceLocation BODY = HomeNet.id("block/guard_master_body");
    public static final ResourceLocation BODY_RINGING = HomeNet.id("block/guard_master_body_ringing");
    public static final ResourceLocation BODY_OFFHOOK = HomeNet.id("block/guard_master_body_offhook");
    public static final ResourceLocation LEG = HomeNet.id("block/guard_master_leg");
    private static final float OY = 0.7F;
    private static final float OZ = 4.0F;
    private static final float THICK = 1.6F;
    private static final float SUPPORT = 7.0F;

    public GuardMasterRenderer(Context ctx) {
    }

    public void render(GuardMasterBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = be.getBlockState();
        if (state.getBlock() instanceof GuardMasterBlock) {
            Minecraft mc = Minecraft.getInstance();
            boolean offHook = state.getValue(GuardMasterBlock.OFFHOOK);
            boolean ringing = state.getValue(WallpadBlock.RINGING);
            BakedModel body = mc.getModelManager().getModel(offHook ? BODY_OFFHOOK : (ringing ? BODY_RINGING : BODY));
            BakedModel leg = mc.getModelManager().getModel(LEG);

            float yRot = switch ((Direction)state.getValue(GuardMasterBlock.FACING)) {
                case EAST -> 90.0F;
                case SOUTH -> 180.0F;
                case WEST -> 270.0F;
                default -> 0.0F;
            };
            float angle = be.getAngle();
            VertexConsumer consumer = buffers.getBuffer(RenderType.cutout());
            pose.pushPose();
            pose.translate(0.5, 0.0, 0.5);
            pose.mulPose(Axis.YN.rotationDegrees(yRot));
            pose.translate(-0.5, 0.0, -0.5);
            double a = Math.toRadians(angle);
            float topY = (float)(0.7F + 7.0 * Math.cos(a) - 1.6F * Math.sin(a));
            float backZ = (float)(4.0 + 7.0 * Math.sin(a) + 1.6F * Math.cos(a));
            float height = Math.max(0.3F, topY - 0.7F);
            pose.pushPose();
            pose.translate(0.0F, 0.04375F, (backZ - 0.15F) / 16.0F);
            pose.scale(1.0F, height / 16.0F, 1.0F);
            mc.getBlockRenderer()
                .getModelRenderer()
                .renderModel(pose.last(), consumer, state, leg, 1.0F, 1.0F, 1.0F, light, overlay, ModelData.EMPTY, RenderType.cutout());
            pose.popPose();
            pose.translate(0.0F, 0.04375F, 0.25F);
            pose.mulPose(Axis.XP.rotationDegrees(angle));
            pose.translate(0.0F, -0.04375F, -0.25F);
            mc.getBlockRenderer()
                .getModelRenderer()
                .renderModel(pose.last(), consumer, state, body, 1.0F, 1.0F, 1.0F, light, overlay, ModelData.EMPTY, RenderType.cutout());
            pose.popPose();
        }
    }
}
