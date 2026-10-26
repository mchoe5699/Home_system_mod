package com.qwerty.homenet.client.logo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.logo.ProductLogo;
import com.qwerty.homenet.network.WallpadDataPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class LogoRenderer {
    private static ResourceLocation texture(String path, ResourceLocation fallback) {
        ResourceLocation r = ResourceLocation.tryParse(path);
        return r != null && Minecraft.getInstance().getResourceManager().getResource(r).isPresent() ? r : fallback;
    }

    public static void gui(GuiGraphics g, Font font, WallpadDataPacket d, ResourceLocation factory, int x, int y, int w, int h) {
        String mode = d.setting("product_logo_mode", "default");
        if (!mode.equals("none")) {
            if (mode.equals("text")) {
                g.pose().pushPose();
                g.pose().translate((float)x, (float)y, 0.0F);
                float scale = Math.min(h / 9.0F, (float)w / Math.max(1, font.width(d.setting("product_logo_text", ""))));
                g.pose().scale(scale, scale, 1.0F);
                g.drawString(font, d.setting("product_logo_text", ""), 0, 0, -13419981, false);
                g.pose().popPose();
            } else {
                ResourceLocation r = mode.equals("custom") ? texture(d.setting("product_logo_texture", ""), factory) : factory;
                g.blit(r, x, y, w, h, 0.0F, 0.0F, 256, 64, 256, 64);
            }
        }
    }

    public static void wall(
        ReceiverBlockEntity b, PoseStack p, MultiBufferSource out, float x1, float y1, float x2, float y2, float z, ResourceLocation factory, int light
    ) {
        wallLogo(b.productLogo, p, out, x1, y1, x2, y2, z, factory, light);
    }

    public static void wallLogo(
        ProductLogo l, PoseStack p, MultiBufferSource out, float x1, float y1, float x2, float y2, float z, ResourceLocation factory, int light
    ) {
        wallLogo(l, p, out, x1, y1, x2, y2, z, factory, light, 3357235);
    }

    public static void wallLogo(
        ProductLogo l, PoseStack p, MultiBufferSource out, float x1, float y1, float x2, float y2, float z, ResourceLocation factory, int light, int textColor
    ) {
        if (!l.mode().equals("none")) {
            if (l.mode().equals("text")) {
                p.pushPose();
                p.translate(x2 / 16.0F, y2 / 16.0F, z / 16.0F);
                p.mulPose(Axis.YP.rotationDegrees(180.0F));
                float s = Math.min((x2 - x1) / 16.0F / Math.max(1, Minecraft.getInstance().font.width(l.text())), (y2 - y1) / 16.0F / 9.0F);
                p.scale(s, -s, s);
                Minecraft.getInstance().font.drawInBatch(l.text(), 0.0F, 0.0F, textColor, false, p.last().pose(), out, DisplayMode.NORMAL, 0, light);
                p.popPose();
            } else {
                ResourceLocation r = l.mode().equals("custom") ? texture(l.texture(), factory) : factory;
                VertexConsumer v = out.getBuffer(RenderType.entityCutoutNoCull(r));
                vertex(v, p, x2, y2, z, 0.0F, 0.0F, light);
                vertex(v, p, x2, y1, z, 0.0F, 1.0F, light);
                vertex(v, p, x1, y1, z, 1.0F, 1.0F, light);
                vertex(v, p, x1, y2, z, 1.0F, 0.0F, light);
            }
        }
    }

    private static void vertex(VertexConsumer v, PoseStack p, float x, float y, float z, float u, float t, int light) {
        v.vertex(p.last().pose(), x / 16.0F, y / 16.0F, z / 16.0F)
            .color(255, 255, 255, 255)
            .uv(u, t)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(light)
            .normal(p.last().normal(), 0.0F, 0.0F, -1.0F)
            .endVertex();
    }

    public static void desk(
        ReceiverBlockEntity b, PoseStack p, MultiBufferSource out, float x1, float z1, float x2, float z2, float height, ResourceLocation factory, int light
    ) {
        p.pushPose();
        p.translate(0.0F, height / 16.0F, 0.0F);
        p.mulPose(Axis.XP.rotationDegrees(90.0F));
        wall(b, p, out, x1, z1, x2, z2, 0.0F, factory, light);
        p.popPose();
    }
}
