package com.qwerty.homenet.client.kam;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.qwerty.homenet.HomeNet;
import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.blockentity.Kam305BlockEntity;
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

public class Kam305Renderer implements BlockEntityRenderer<Kam305BlockEntity> {
    public Kam305Renderer(Context c) {
    }

    public static ResourceLocation texture(String n) {
        return HomeNet.id("textures/block/kam305/" + n + ".png");
    }

    private void mesh(String part, Kam305BlockEntity b, PoseStack p, MultiBufferSource out, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        mc.getBlockRenderer()
            .getModelRenderer()
            .renderModel(
                p.last(),
                out.getBuffer(RenderType.cutout()),
                b.getBlockState(),
                mc.getModelManager().getModel(HomeNet.id("block/kam_305_" + part)),
                1.0F,
                1.0F,
                1.0F,
                light,
                overlay,
                ModelData.EMPTY,
                RenderType.cutout()
            );
    }

    public void render(Kam305BlockEntity b, float tick, PoseStack p, MultiBufferSource out, int light, int overlay) {
        p.pushPose();
        p.translate(0.5, 0.0, 0.5);
        float size = 0.8648649F;
        p.scale(size, size, size);

        p.mulPose(Axis.YN.rotationDegrees(switch ((Direction)b.getBlockState().getValue(WallMountedBlock.FACING)) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        }));
        p.translate(-0.5, 0.0, -0.5);
        this.mesh("skirt", b, p, out, light, overlay);
        p.translate(0.0, 0.1, 0.19375);
        p.mulPose(Axis.XP.rotationDegrees(-8.0F));
        p.translate(0.0, -0.1, -0.19375);
        this.mesh("body", b, p, out, light, overlay);
        if (!b.lifted()) {
            this.mesh("handset", b, p, out, light, overlay);
        }

        p.translate(0.284375, 0.085625, 0.3675);
        p.mulPose(Axis.XP.rotationDegrees(90 - b.screenAngle()));
        p.translate(-0.284375, -0.105, -0.525);
        this.mesh("screen", b, p, out, light, overlay);
        LogoRenderer.wall(b, p, out, 2.65F, 2.57F, 6.45F, 3.13F, 8.19F, texture("logo"), light);
        if (b.power() || b.lcdPage().equals("factory")) {
            String[] lines = b.lcd();
            boolean standby = lines[2].equals("KOCOM");
            boolean call = b.lcdPage().equals("calls") || b.lcdPage().equals("lobby") || b.lcdPage().equals("lobby_ring");
            if (standby) {
                if (b.productLogo.configured()) {
                    LogoRenderer.wall(b, p, out, 2.0F, 5.1F, 7.15F, 6.18F, 8.173F, texture("lcd_logo"), 15728880);
                } else {
                    quad(p, out, "lcd_logo", 2.0F, 5.1F, 7.15F, 6.18F, 16777215);
                }
            }

            if (call) {
                quad(p, out, "lcd_phone", 5.3F, 4.85F, 7.1F, 5.95F, 16777215);
            }

            for (int i = 0; i < 8; i++) {
                if (!standby || i != 2) {
                    double y = 6.7 - i * 0.35;
                    String line = lines[i];
                    float fit = Math.min(0.0019F, (!call || i != 2 && i != 3 ? 0.322F : 0.17F) / Math.max(1, Minecraft.getInstance().font.width(line)));
                    float x = !call || i != 2 && i != 3 ? 7.15F : 4.85F;
                    if (standby && (i == 0 || i == 6) || call && i == 0 || b.lcdPage().equals("form") && i == 0) {
                        x = 4.55F + Minecraft.getInstance().font.width(line) * fit * 8.0F;
                    }

                    if (i == b.lcdSelectedRow() && b.lcdPage().equals("form")) {
                        int start = Math.max(0, Math.min(line.length(), b.lcdHighlightStart()));
                        int length = Math.max(0, Math.min(line.length() - start, b.lcdHighlightLength()));
                        String label = line.substring(0, start);
                        String value = line.substring(start, start + length);
                        String suffix = line.substring(start + length);
                        float startX = x - Minecraft.getInstance().font.width(label) * fit * 16.0F;
                        float endX = startX - Minecraft.getInstance().font.width(value) * fit * 16.0F;
                        quad(p, out, "lcd_solid", Math.min(endX, startX - 0.3F) - 0.035F, (float)y - 0.22F, startX + 0.035F, (float)y + 0.04F, 2241562);
                        drawText(p, out, label, x, y, fit, 2241562);
                        drawText(p, out, value, startX, y, fit, 10333537);
                        drawText(p, out, suffix, endX, y, fit, 2241562);
                    } else {
                        int color = 2241562;
                        if (i == b.lcdSelectedRow()) {
                            quad(p, out, "lcd_solid", 1.85F, (float)y - 0.22F, 7.18F, (float)y + 0.04F, color);
                            color = 10333537;
                        }

                        drawText(p, out, line, x, y, fit, color);
                    }
                }
            }
        }

        p.popPose();
    }

    private static void drawText(PoseStack p, MultiBufferSource out, String line, float x, double y, float fit, int color) {
        p.pushPose();
        p.translate((double)(x / 16.0F), y / 16.0, 0.5109375);
        p.mulPose(Axis.YP.rotationDegrees(180.0F));
        p.scale(fit, -fit, fit);
        Minecraft.getInstance().font.drawInBatch(line, 0.0F, 0.0F, color, false, p.last().pose(), out, DisplayMode.NORMAL, 0, 15728880);
        p.popPose();
    }

    private static void quad(PoseStack p, MultiBufferSource out, String texture, float x1, float y1, float x2, float y2, int rgb) {
        VertexConsumer v = out.getBuffer(RenderType.entityCutoutNoCull(texture(texture)));
        float z = texture.equals("lcd_solid") ? 8.18F : 8.173F;
        vertex(v, p, x2, y2, z, rgb, 0.0F, 0.0F);
        vertex(v, p, x2, y1, z, rgb, 0.0F, 1.0F);
        vertex(v, p, x1, y1, z, rgb, 1.0F, 1.0F);
        vertex(v, p, x1, y2, z, rgb, 1.0F, 0.0F);
    }

    private static void vertex(VertexConsumer v, PoseStack p, float x, float y, float z, int rgb, float u, float t) {
        v.vertex(p.last().pose(), x / 16.0F, y / 16.0F, z / 16.0F)
            .color(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, 255)
            .uv(u, t)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(15728880)
            .normal(p.last().normal(), 0.0F, 0.0F, -1.0F)
            .endVertex();
    }
}
