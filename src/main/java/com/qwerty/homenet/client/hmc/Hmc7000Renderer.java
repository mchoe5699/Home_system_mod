package com.qwerty.homenet.client.hmc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.qwerty.homenet.HomeNet;
import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.blockentity.Hmc7000BlockEntity;
import com.qwerty.homenet.client.logo.LogoRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.data.ModelData;

public class Hmc7000Renderer implements BlockEntityRenderer<Hmc7000BlockEntity> {
    public static final int[][] LIGHTS = new int[][]{
        {402, 367},
        {521, 367},
        {640, 367},
        {759, 367},
        {863, 402},
        {905, 402},
        {947, 402},
        {863, 332},
        {905, 332},
        {800, 530},
        {864, 530},
        {928, 530},
        {800, 565},
        {864, 565},
        {967, 1188}
    };

    public Hmc7000Renderer(Context context) {
    }

    public static ResourceLocation texture(String name) {
        return HomeNet.id("textures/block/hmc7000/" + name + ".png");
    }

    private void mesh(String part, Hmc7000BlockEntity b, PoseStack p, MultiBufferSource out, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        mc.getBlockRenderer()
            .getModelRenderer()
            .renderModel(
                p.last(),
                out.getBuffer(RenderType.cutout()),
                b.getBlockState(),
                mc.getModelManager().getModel(HomeNet.id("block/hmc_7000_" + part)),
                1.0F,
                1.0F,
                1.0F,
                light,
                overlay,
                ModelData.EMPTY,
                RenderType.cutout()
            );
    }

    public void render(Hmc7000BlockEntity b, float tick, PoseStack p, MultiBufferSource out, int light, int overlay) {
        p.pushPose();
        p.translate(0.5, 0.0, 0.5);
        float size = 0.84210527F;
        p.scale(size, size, size);

        p.mulPose(Axis.YN.rotationDegrees(switch ((Direction)b.getBlockState().getValue(WallMountedBlock.FACING)) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        }));
        p.translate(-0.5, 0.0, -0.5);
        this.mesh("skirt", b, p, out, light, overlay);
        p.translate(0.0, 0.05, 0.025);
        p.mulPose(Axis.XP.rotationDegrees(-6.0F));
        p.translate(0.0, -0.05, -0.025);
        this.mesh("body", b, p, out, light, overlay);
        if (!b.lifted()) {
            this.mesh("handset", b, p, out, light, overlay);
        }

        LogoRenderer.desk(b, p, out, 6.0F, 0.9F, 9.85F, 1.45F, 0.947F, texture("logo"), light);

        for (int i = 0; i < LIGHTS.length; i++) {
            if (b.led(i)) {
                float x = 14.4F - LIGHTS[i][0] / 1024.0F * 12.8F;
                float z = 15.6F - LIGHTS[i][1] / 1280.0F * 15.2F;
                lamp(p, out, x, z, i < 4 ? 15096381 : 6406043);
            }
        }

        p.translate(0.0, 0.05875, 0.760625);
        p.mulPose(Axis.XP.rotationDegrees(-b.screenAngle()));
        p.translate(0.0, -0.05875, -0.760625);
        this.mesh("screen", b, p, out, light, overlay);
        if (b.power()) {
            String[] lines = b.lcd();

            for (int ix = 0; ix < 2; ix++) {
                float fit = Math.min(0.0024F, 0.285F / Math.max(1, Minecraft.getInstance().font.width(lines[ix])));
                p.pushPose();
                p.translate(0.5175, 0.065375, (14.17 - ix * 0.47) / 16.0);
                p.mulPose(Axis.XP.rotationDegrees(90.0F));
                p.mulPose(Axis.YP.rotationDegrees(180.0F));
                p.scale(fit, -fit, fit);
                Minecraft.getInstance().font.drawInBatch(lines[ix], 0.0F, 0.0F, 2636322, false, p.last().pose(), out, DisplayMode.NORMAL, 0, light);
                p.popPose();
            }
        }

        p.popPose();
    }

    private static void lamp(PoseStack p, MultiBufferSource out, float x, float z, int color) {
        VertexConsumer v = out.getBuffer(RenderType.entityCutoutNoCull(texture("shell")));

        for (float[] c : new float[][]{{x - 0.06F, z - 0.06F}, {x - 0.06F, z + 0.06F}, {x + 0.06F, z + 0.06F}, {x + 0.06F, z - 0.06F}}) {
            v.vertex(p.last().pose(), c[0] / 16.0F, 0.065625F, c[1] / 16.0F)
                .color(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, 255)
                .uv(0.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880)
                .normal(p.last().normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();
        }
    }
}
