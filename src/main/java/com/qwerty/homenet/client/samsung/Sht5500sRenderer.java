package com.qwerty.homenet.client.samsung;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.qwerty.homenet.HomeNet;
import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.blockentity.Sht5500sBlockEntity;
import com.qwerty.homenet.client.logo.LogoRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraftforge.client.model.data.ModelData;

public class Sht5500sRenderer implements BlockEntityRenderer<Sht5500sBlockEntity> {
    public Sht5500sRenderer(Context context) {
    }

    private void mesh(String part, Sht5500sBlockEntity b, PoseStack pose, MultiBufferSource out, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        mc.getBlockRenderer()
            .getModelRenderer()
            .renderModel(
                pose.last(),
                out.getBuffer(RenderType.cutout()),
                b.getBlockState(),
                mc.getModelManager().getModel(HomeNet.id("block/sht_5500s_" + part)),
                1.0F,
                1.0F,
                1.0F,
                light,
                overlay,
                ModelData.EMPTY,
                RenderType.cutout()
            );
    }

    public void render(Sht5500sBlockEntity b, float tick, PoseStack pose, MultiBufferSource out, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5, 0.0, 0.5);

        pose.mulPose(Axis.YN.rotationDegrees(switch ((Direction)b.getBlockState().getValue(WallMountedBlock.FACING)) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        }));
        pose.translate(-0.5, 0.0, -0.5);
        this.mesh("base", b, pose, out, light, overlay);
        pose.translate(0.0, 0.041875, 0.25625);
        pose.mulPose(Axis.XP.rotationDegrees(-8.0F));
        pose.translate(0.0, -0.041875, -0.25625);
        this.mesh("deck", b, pose, out, light, overlay);
        this.mesh("handset", b, pose, out, light, overlay);
        pose.translate(0.0, 0.055625, 0.671875);
        pose.mulPose(Axis.XP.rotationDegrees(-67.5F));
        pose.translate(0.0, -0.055625, -0.671875);
        this.mesh("panel", b, pose, out, light, overlay);
        pose.pushPose();
        pose.translate(0.0, 0.060875, 0.0);
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        LogoRenderer.wallLogo(b.productLogo, pose, out, 10.12F, 13.32F, 11.24F, 13.58F, 0.0F, HomeNet.id("textures/block/sht5500s/logo.png"), light, 13884364);
        pose.popPose();
        pose.popPose();
    }
}
