package com.qwerty.homenet.client.am;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.qwerty.homenet.HomeNet;
import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.blockentity.Am10kpBlockEntity;
import com.qwerty.homenet.client.logo.LogoRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.data.ModelData;

public class Am10kpRenderer implements BlockEntityRenderer<Am10kpBlockEntity> {
    private static final int[] MASK = new int[]{63, 6, 91, 79, 102, 109, 125, 7, 127, 111};
    private static final int[][] SEG = new int[][]{
        {2, 0, 10, 2}, {10, 2, 12, 10}, {10, 12, 12, 20}, {2, 20, 10, 22}, {0, 12, 2, 20}, {0, 2, 2, 10}, {2, 10, 10, 12}
    };

    public Am10kpRenderer(Context c) {
    }

    public static ResourceLocation texture(String n) {
        return HomeNet.id("textures/block/am10kp/" + n + ".png");
    }

    private void mesh(String name, Am10kpBlockEntity b, PoseStack p, MultiBufferSource out, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        mc.getBlockRenderer()
            .getModelRenderer()
            .renderModel(
                p.last(),
                out.getBuffer(RenderType.cutout()),
                b.getBlockState(),
                mc.getModelManager().getModel(HomeNet.id("block/am_10kp_" + name)),
                1.0F,
                1.0F,
                1.0F,
                light,
                overlay,
                ModelData.EMPTY,
                RenderType.cutout()
            );
    }

    public void render(Am10kpBlockEntity b, float t, PoseStack p, MultiBufferSource out, int light, int overlay) {
        p.pushPose();
        p.translate(0.5, 0.0, 0.5);
        float size = 0.88888896F;
        p.scale(size, size, size);

        p.mulPose(Axis.YN.rotationDegrees(switch ((Direction)b.getBlockState().getValue(WallMountedBlock.FACING)) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        }));
        p.translate(-0.5, 0.0, -0.5);
        this.mesh("skirt", b, p, out, light, overlay);
        p.translate(0.5, 0.078125, 0.203125);
        p.mulPose(Axis.XP.rotationDegrees(-12.0F));
        p.translate(-0.5, -0.078125, -0.203125);
        this.mesh("body", b, p, out, light, overlay);
        if (!b.lifted()) {
            this.mesh("handset", b, p, out, light, overlay);
        }

        LogoRenderer.desk(b, p, out, 9.68F, 8.68F, 11.83F, 9.2F, 1.515F, texture("logo"), light);
        if (b.power()) {
            for (int row = 0; row < 5; row++) {
                String s = b.renderedDisplay(row);
                String[] pair = s.split("-", 2);
                String dong = pair[0];
                String ho = pair.length > 1 ? pair[1] : "";
                this.digits(out, p, dong, 434, 153 + row * 83, 3, overlay);
                this.digits(out, p, ho, 507, 153 + row * 83, 4, overlay);
                if (row > 0 && b.renderedLed(row)) {
                    rect(out, p, 615, row == 0 ? 132 : 153 + row * 83 - 25, 622, row == 0 ? 139 : 153 + row * 83 - 18, 15289178, overlay);
                }
            }
        }

        for (int i = 0; i < 4; i++) {
            if (b.renderedMode(i)) {
                rect(out, p, 699 + i * 53, 188, 707 + i * 53, 196, i == 0 ? 15157599 : 4504969, overlay);
            }
        }

        if (!b.power()) {
            rect(out, p, 358, 512, 389, 544, 5450292, overlay);
        }

        p.popPose();
    }

    private void digits(MultiBufferSource out, PoseStack p, String s, int x, int y, int count, int overlay) {
        s = s.replaceAll("[^0-9]", "");
        if (s.length() > count) {
            s = s.substring(s.length() - count);
        }

        s = " ".repeat(count - s.length()) + s;

        for (int i = 0; i < count; i++) {
            if (s.charAt(i) != ' ') {
                for (int j = 0; j < 7; j++) {
                    int[] a = SEG[j];
                    rect(
                        out,
                        p,
                        x + 3 + i * 15 + a[0],
                        y + 4 + a[1],
                        x + 3 + i * 15 + a[2],
                        y + 4 + a[3],
                        (MASK[s.charAt(i) - '0'] & 1 << j) != 0 ? 12075940 : 3482169,
                        overlay
                    );
                }
            }
        }
    }

    private static void rect(MultiBufferSource out, PoseStack p, int x, int y, int r, int bot, int rgb, int overlay) {
        VertexConsumer v = out.getBuffer(RenderType.entityCutoutNoCull(texture("shell")));
        float x1 = (float)(15.2 - r * 0.0144) / 16.0F;
        float x2 = (float)(15.2 - x * 0.0144) / 16.0F;
        float z1 = (float)(12.5 - bot * 9.25 / 645.0) / 16.0F;
        float z2 = (float)(12.5 - y * 9.25 / 645.0) / 16.0F;
        vertex(v, p, x1, z1, rgb, overlay, 0.0F, 0.0F);
        vertex(v, p, x2, z1, rgb, overlay, 1.0F, 0.0F);
        vertex(v, p, x2, z2, rgb, overlay, 1.0F, 1.0F);
        vertex(v, p, x1, z2, rgb, overlay, 0.0F, 1.0F);
    }

    private static void vertex(VertexConsumer v, PoseStack p, float x, float z, int c, int overlay, float u, float t) {
        v.vertex(p.last().pose(), x, 0.094375F, z)
            .color(c >> 16 & 0xFF, c >> 8 & 0xFF, c & 0xFF, 255)
            .uv(u, t)
            .overlayCoords(overlay)
            .uv2(15728880)
            .normal(p.last().normal(), 0.0F, 1.0F, 0.0F)
            .endVertex();
    }
}
