package com.qwerty.homenet.client.klp;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.qwerty.homenet.HomeNet;
import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.blockentity.Klp203rfBlockEntity;
import com.qwerty.homenet.client.logo.LogoRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public class Klp203rfRenderer implements BlockEntityRenderer<Klp203rfBlockEntity> {
    public Klp203rfRenderer(Context c) {
    }

    public static ResourceLocation texture(String name) {
        return HomeNet.id("textures/block/klp203rf/" + name + ".png");
    }

    public static ResourceLocation texture(Klp203rfBlockEntity b, String name) {
        return HomeNet.id("textures/block/" + b.model().folder + "/" + name + ".png");
    }

    public void render(Klp203rfBlockEntity b, float t, PoseStack p, MultiBufferSource out, int light, int overlay) {
        p.pushPose();
        p.translate(0.5, 0.0, 0.5);

        p.mulPose(Axis.YN.rotationDegrees(switch ((Direction)b.getBlockState().getValue(WallMountedBlock.FACING)) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        }));
        p.translate(-0.5, 0.0, -0.5);
        p.translate(0.5, 0.5, 1.0);
        p.scale(0.6666667F, 0.6666667F, 0.6666667F);
        p.translate(-0.5, -0.5, -1.0);
        LogoRenderer.wallLogo(b.productLogo, p, out, 9.819F, 13.714286F, 11.673667F, 14.142857F, 15.085F, texture(b, "logo"), light);
        VertexConsumer v = out.getBuffer(RenderType.entityTranslucentEmissive(texture(b, "solid")));
        String text = KlpSegments.four(b.display());

        for (int i = 0; i < 4; i++) {
            for (int k = 0; k < 7; k++) {
                int[] r = KlpSegments.BARS[k];
                boolean on = (KlpSegments.mask(text.charAt(i)) & 1 << k) != 0;
                quad(v, p, 341 + i * 36 + r[0], 140 + r[1], 341 + i * 36 + r[2], 140 + r[3], on ? 16726053 : 3611159);
            }
        }

        if (b.visitorLight()) {
            for (int x : new int[]{112, 273}) {
                for (int j = 0; j < 3; j++) {
                    quad(v, p, x, 143 + j * 14, x + 18, 156 + j * 14, b.model().colorVideo() ? 16775133 : 9319277);
                }
            }
        }

        boolean blink = b.getLevel() != null && b.getLevel().getGameTime() % 20L < 10L;

        for (int i = 0; i < 3; i++) {
            boolean on = i == 0 ? b.homeLamp() : (i == 1 ? b.guardLamp() : b.talkLamp());
            quad(v, p, 364 + i * 54, 198.0F, 369 + i * 54, 201.0F, !on || !b.talkLamp() && i != 2 && !blink ? 2627094 : 16726053);
        }

        p.popPose();
    }

    private static void quad(VertexConsumer v, PoseStack p, float a, float b, float c, float d, int rgb) {
        float x1 = (float)(13.35 - c / 600.0F * 10.7);
        float x2 = (float)(13.35 - a / 600.0F * 10.7);
        float y1 = 15.0F - d / 784.0F * 14.0F;
        float y2 = 15.0F - b / 784.0F * 14.0F;
        vertex(v, p, x2, y2, rgb, 0.0F, 0.0F);
        vertex(v, p, x2, y1, rgb, 0.0F, 1.0F);
        vertex(v, p, x1, y1, rgb, 1.0F, 1.0F);
        vertex(v, p, x1, y2, rgb, 1.0F, 0.0F);
    }

    private static void vertex(VertexConsumer v, PoseStack p, float x, float y, int rgb, float u, float t) {
        v.vertex(p.last().pose(), x / 16.0F, y / 16.0F, 0.9409375F)
            .color(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, 255)
            .uv(u, t)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(15728880)
            .normal(p.last().normal(), 0.0F, 0.0F, -1.0F)
            .endVertex();
    }
}
