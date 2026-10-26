package com.qwerty.homenet.client.ipap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.qwerty.homenet.HomeNet;
import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.blockentity.Ip203apBlockEntity;
import com.qwerty.homenet.client.logo.LogoRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.data.ModelData;

public final class Ip203apRenderer implements BlockEntityRenderer<Ip203apBlockEntity> {
    private static final int ROWS = 128;
    private static final int COLS = 24;
    private final boolean bus;
    private final String family;

    public Ip203apRenderer(Context context) {
        this(context, false);
    }

    public Ip203apRenderer(Context context, boolean bus) {
        this(context, bus ? "ip203apb" : "ip203ap");
    }

    public Ip203apRenderer(Context context, String family) {
        this.family = family;
        this.bus = !family.equals("ip203ap");
    }

    public static ResourceLocation texture(String part, String family) {
        return HomeNet.id("textures/block/" + family + "/" + part + ".png");
    }

    public static ResourceLocation texture(String part, boolean bus) {
        return HomeNet.id("textures/block/" + (bus ? "ip203apb" : "ip203ap") + "/" + part + ".png");
    }

    private String modelName() {
        String var1 = this.family;

        return switch (var1) {
            case "ip200pc" -> "ip_200pc";
            case "ip200pchb" -> "ip_200pchb";
            case "dp202h" -> "dp_202h";
            case "kip234r2" -> "kip_234r2";
            case "ip203apb" -> "ip_203apb";
            default -> "ip_203ap";
        };
    }

    private boolean compact() {
        return this.family.equals("ip200pc") || this.family.equals("ip200pchb") || this.family.equals("dp202h");
    }

    private ResourceLocation tex(String part) {
        return texture(part, this.family);
    }

    public static ResourceLocation texture(String part) {
        return HomeNet.id("textures/block/ip203ap/" + part + ".png");
    }

    private static double smooth(double v) {
        v = Math.max(0.0, Math.min(1.0, v));
        return v * v * (3.0 - 2.0 * v);
    }

    private static double inset(double y, double lo, double hi, double bottom, double top) {
        double r = y < lo + bottom ? bottom : (y > hi - top ? top : 0.0);
        double d = y < lo + r ? lo + r - y : y - (hi - r);
        return r == 0.0 ? 0.0 : r - Math.sqrt(Math.max(0.0, r * r - d * d));
    }

    private static double[] limits(double y, boolean hand) {
        if (hand) {
            double t = (y - 6.3) / 8.88;
            double o = inset(y, 6.3, 15.18, 0.88, 0.43);
            return new double[]{7.69 + 0.11 * Math.sin(Math.PI * t) + o, 10.15 - 0.035 * Math.sin(Math.PI * t) - o};
        } else if (y < 7.35) {
            double h = Math.max(1.0E-4, 2.3 * Math.sqrt(Math.max(0.0, 1.0 - Math.pow((7.35 - y) / 1.75, 2.0))));
            return new double[]{8.0 - h, 8.0 + h};
        } else {
            return new double[]{5.7 + inset(y, 5.6, 15.3, 0.0, 0.33), 10.3 - inset(y, 5.6, 15.3, 0.0, 0.33)};
        }
    }

    private static double front(double y, double u, boolean hand, boolean open) {
        if (hand) {
            return 13.1 - 0.08 * Math.sin(Math.PI * (y - 6.3) / 8.88) + 0.18 * Math.pow(2.0 * u - 1.0, 4.0);
        } else {
            double edge = 0.32 * Math.pow(Math.max(0.0, Math.abs(2.0 * u - 1.0) - 0.72) / 0.28, 2.0);
            double chin = -0.16 * (1.0 - smooth((y - 5.6) / 1.2));
            double t = (y - 5.6) / 9.7;
            double seat = 0.07 * smooth((u - 0.4) / 0.3);
            double lateral = Math.pow(Math.sin(Math.PI * smooth((u - 0.4) / 0.58)), 2.0);
            double bowl = lateral
                * (0.18 * Math.pow(Math.sin(Math.PI * smooth((t - 0.72) / 0.24)), 2.0) + 0.14 * Math.pow(Math.sin(Math.PI * smooth((t - 0.065) / 0.19)), 2.0));
            return 14.22 + edge + chin + seat + bowl;
        }
    }

    private static double back(double y, boolean hand) {
        if (!hand) {
            return 15.38;
        } else {
            double t = (y - 6.3) / 8.88;
            double grip = smooth((t - 0.15) / 0.13) * (1.0 - smooth((t - 0.76) / 0.09));
            return 14.32 - 0.57 * grip - 0.04 * Math.sin(Math.PI * t);
        }
    }

    private static void v(VertexConsumer out, PoseStack p, double x, double y, double z, double nx, double ny, double nz, int light) {
        double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
        out.vertex(p.last().pose(), (float)x / 16.0F, (float)y / 16.0F, (float)z / 16.0F)
            .color(255, 255, 255, 255)
            .uv((float)((10.3 - x) / 4.6), (float)((15.3 - y) / 9.7))
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(light)
            .normal(p.last().normal(), (float)(nx / length), (float)(ny / length), (float)(nz / length))
            .endVertex();
    }

    private static void surface(VertexConsumer out, PoseStack p, double y, double u, boolean hand, boolean open, boolean rear, int light) {
        double[] lr = limits(y, hand);
        double x = lr[0] + (lr[1] - lr[0]) * u;
        double z = rear ? back(y, hand) : front(y, u, hand, open);
        double nx = rear ? 0.0 : (front(y, Math.min(1.0, u + 0.001), hand, open) - front(y, Math.max(0.0, u - 0.001), hand, open)) / 0.002 / (lr[1] - lr[0]);
        double lo = hand ? 6.3 : 5.6;
        double hi = hand ? 15.18 : 15.3;
        double ya = Math.max(lo, y - 0.002);
        double yb = Math.min(hi, y + 0.002);
        double dy = rear ? (back(yb, hand) - back(ya, hand)) / (yb - ya) : (front(yb, u, hand, open) - front(ya, u, hand, open)) / (yb - ya);
        v(out, p, x, y, z, rear ? 0.0 : nx, rear ? -dy : dy, rear ? 1.0 : -1.0, light);
    }

    private void shell(PoseStack p, MultiBufferSource buffers, int light, boolean hand, boolean open) {
        double lo = hand ? 6.3 : 5.6;
        double hi = hand ? 15.18 : 15.3;
        VertexConsumer face = buffers.getBuffer(WhitePlasticRenderType.get(this.tex(hand ? "handset" : "face_open")));

        for (int i = 0; i < 128; i++) {
            double a = lo + (hi - lo) * i / 128.0;
            double b = lo + (hi - lo) * (i + 1) / 128.0;

            for (int j = 0; j < 24; j++) {
                double u = j / 24.0;
                double w = (j + 1) / 24.0;
                surface(face, p, b, w, hand, open, false, light);
                surface(face, p, a, w, hand, open, false, light);
                surface(face, p, a, u, hand, open, false, light);
                surface(face, p, b, u, hand, open, false, light);
            }
        }

        VertexConsumer rear = buffers.getBuffer(WhitePlasticRenderType.get(this.tex(hand ? "handset_rear" : "edge")));

        for (int i = 0; i < 128; i++) {
            double a = lo + (hi - lo) * i / 128.0;
            double b = lo + (hi - lo) * (i + 1) / 128.0;

            for (int j = 0; j < 24; j++) {
                double u = j / 24.0;
                double w = (j + 1) / 24.0;
                surface(rear, p, a, w, hand, open, true, light);
                surface(rear, p, b, w, hand, open, true, light);
                surface(rear, p, b, u, hand, open, true, light);
                surface(rear, p, a, u, hand, open, true, light);
            }
        }

        VertexConsumer edge = buffers.getBuffer(WhitePlasticRenderType.get(this.tex("edge")));

        for (int i = 0; i < 128; i++) {
            double a = lo + (hi - lo) * i / 128.0;
            double b = lo + (hi - lo) * (i + 1) / 128.0;
            double[] la = limits(a, hand);
            double[] lb = limits(b, hand);

            for (int side = 0; side < 2; side++) {
                double sign = side == 0 ? -1.0 : 1.0;

                for (int band = 0; band < 8; band++) {
                    double k = band / 8.0;
                    double w = (band + 1) / 8.0;
                    double xa = la[side] + sign * 0.06 * Math.sin(Math.PI * k);
                    double xb = lb[side] + sign * 0.06 * Math.sin(Math.PI * k);
                    double xc = lb[side] + sign * 0.06 * Math.sin(Math.PI * w);
                    double xd = la[side] + sign * 0.06 * Math.sin(Math.PI * w);
                    double fa = front(a, side, hand, open);
                    double fb = front(b, side, hand, open);
                    double ba = back(a, hand);
                    double bb = back(b, hand);
                    v(edge, p, xa, a, fa + (ba - fa) * k, sign, 0.18, -0.35, light);
                    v(edge, p, xb, b, fb + (bb - fb) * k, sign, 0.18, -0.35, light);
                    v(edge, p, xc, b, fb + (bb - fb) * w, sign, 0.18, -0.35, light);
                    v(edge, p, xd, a, fa + (ba - fa) * w, sign, 0.18, -0.35, light);
                }
            }
        }

        for (double y : new double[]{lo, hi}) {
            double[] e = limits(y, hand);

            for (int j = 0; j < 24; j++) {
                double u = j / 24.0;
                double w = (j + 1) / 24.0;
                double xa = e[0] + u * (e[1] - e[0]);
                double xb = e[0] + w * (e[1] - e[0]);
                double ny = y == lo ? -1.0 : 1.0;
                v(edge, p, xa, y, front(y, u, hand, open), 0.0, ny, -0.3, light);
                v(edge, p, xb, y, front(y, w, hand, open), 0.0, ny, -0.3, light);
                v(edge, p, xb, y, back(y, hand), 0.0, ny, -0.3, light);
                v(edge, p, xa, y, back(y, hand), 0.0, ny, -0.3, light);
            }
        }

        VertexConsumer seam = buffers.getBuffer(RenderType.entityCutoutNoCull(this.tex("seam")));

        for (int i = 0; i < 128; i++) {
            double a = lo + (hi - lo) * i / 128.0;
            double b = lo + (hi - lo) * (i + 1) / 128.0;
            double[] la = limits(a, hand);
            double[] lb = limits(b, hand);

            for (int side = 0; side < 2; side++) {
                double sign = side == 0 ? -1.0 : 1.0;
                double za = hand ? front(a, side, true, false) * 0.4 + back(a, true) * 0.6 : 15.33;
                double zb = hand ? front(b, side, true, false) * 0.4 + back(b, true) * 0.6 : 15.33;
                v(seam, p, la[side] + sign * 0.055, a, za, sign, 0.0, -0.3, light);
                v(seam, p, lb[side] + sign * 0.055, b, zb, sign, 0.0, -0.3, light);
                v(seam, p, lb[side] + sign * 0.055, b, zb + 0.014, sign, 0.0, -0.3, light);
                v(seam, p, la[side] + sign * 0.055, a, za + 0.014, sign, 0.0, -0.3, light);
            }
        }
    }

    private void backplate(PoseStack p, MultiBufferSource out, int light) {
        VertexConsumer edge = out.getBuffer(RenderType.entityCutoutNoCull(this.tex("backplate")));

        for (int i = 0; i < 128; i++) {
            double a = 5.85 + 9.25 * i / 128.0;
            double b = 5.85 + 9.25 * (i + 1) / 128.0;
            double[] la = limits(a, false);
            double[] lb = limits(b, false);
            la[0] += 0.12;
            la[1] -= 0.12;
            lb[0] += 0.12;
            lb[1] -= 0.12;
            double[][] c = new double[][]{
                {la[0], a, 15.4}, {la[1], a, 15.4}, {lb[1], b, 15.4}, {lb[0], b, 15.4}, {la[0], a, 16.0}, {la[1], a, 16.0}, {lb[1], b, 16.0}, {lb[0], b, 16.0}
            };
            int[][] faces = new int[][]{{0, 1, 2, 3}, {1, 5, 6, 2}, {5, 4, 7, 6}, {4, 0, 3, 7}, {3, 2, 6, 7}, {4, 5, 1, 0}};

            for (int[] face : faces) {
                for (int k : face) {
                    double[] v = c[k];
                    v(edge, p, v[0], v[1], v[2], 0.0, 0.3, -1.0, light);
                }
            }
        }
    }

    private void sideDetails(PoseStack p, MultiBufferSource out, int light) {
        VertexConsumer dark = out.getBuffer(RenderType.entityCutoutNoCull(this.tex("dark")));

        for (int i = 0; i < 9; i++) {
            double y = 15.3 - (84.98 + i * 16) / 100.0;
            double f = front(y, 0.0, false, false);
            double b = back(y, false);

            for (int j = 0; j < 6; j++) {
                double a = 0.28 * j / 6.0;
                double c = 0.28 * (j + 1) / 6.0;
                double xa = 5.7 - 0.06 * Math.sin(Math.PI * a) - 0.002;
                double xc = 5.7 - 0.06 * Math.sin(Math.PI * c) - 0.002;
                double za = f + (b - f) * a;
                double zc = f + (b - f) * c;
                v(dark, p, xa, y + 0.017, za, -1.0, 0.0, -0.3, light);
                v(dark, p, xa, y - 0.017, za, -1.0, 0.0, -0.3, light);
                v(dark, p, xc, y - 0.017, zc, -1.0, 0.0, -0.3, light);
                v(dark, p, xc, y + 0.017, zc, -1.0, 0.0, -0.3, light);
            }
        }

        v(dark, p, 10.078, 14.52, 15.47, 1.0, 0.0, 0.0, light);
        v(dark, p, 10.078, 14.15, 15.47, 1.0, 0.0, 0.0, light);
        v(dark, p, 10.078, 14.15, 15.8, 1.0, 0.0, 0.0, light);
        v(dark, p, 10.078, 14.52, 15.8, 1.0, 0.0, 0.0, light);
    }

    private void indicator(PoseStack p, MultiBufferSource out, int light, int px, int py, double r, boolean on) {
        VertexConsumer v = out.getBuffer(on ? RenderType.entityTranslucentEmissive(this.tex("lamp_on")) : RenderType.entityCutoutNoCull(this.tex("lamp_off")));
        light = on ? 15728880 : light;
        double x = 10.3 - px / 100.0;
        double y = 15.3 - py / 100.0;

        for (int i = 0; i < 16; i++) {
            double a = (Math.PI * 2) * i / 16.0;
            double b = (Math.PI * 2) * (i + 1) / 16.0;
            v(v, p, x, y, 14.16, 0.0, 0.0, -1.0, light);
            v(v, p, x + r * Math.cos(a), y + r * Math.sin(a), 14.16, 0.0, 0.0, -1.0, light);
            v(v, p, x + r * Math.cos(b), y + r * Math.sin(b), 14.16, 0.0, 0.0, -1.0, light);
            v(v, p, x, y, 14.16, 0.0, 0.0, -1.0, light);
        }
    }

    public void render(Ip203apBlockEntity b, float ticks, PoseStack p, MultiBufferSource out, int light, int overlay) {
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
        p.scale(0.68869567F, 0.742268F, 0.742268F);
        p.translate(-0.5, -0.653125, -1.0);
        this.backplate(p, out, light);
        this.shell(p, out, light, false, true);
        this.sideDetails(p, out, light);
        Minecraft mc = Minecraft.getInstance();
        mc.getBlockRenderer()
            .getModelRenderer()
            .renderModel(
                p.last(),
                out.getBuffer(WhitePlasticRenderType.get(TextureAtlas.LOCATION_BLOCKS)),
                b.getBlockState(),
                mc.getModelManager().getModel(HomeNet.id("block/" + this.modelName() + "_hardware_open")),
                1.0F,
                1.0F,
                1.0F,
                light,
                overlay,
                ModelData.EMPTY,
                RenderType.cutout()
            );
        if (this.bus || this.family.equals("dp202h")) {
            long time = b.getLevel() == null ? 0L : b.getLevel().getGameTime();
            if (!this.compact()) {
                this.indicator(
                    p, out, light, 310, 525, 0.055, Ip203apBlockEntity.lampVisible(b.lampMode(this.family.equals("kip234r2") ? "lobby" : "front"), time)
                );
                int middle = b.lampMode("away") != 0 ? b.lampMode("away") : (this.family.equals("kip234r2") ? b.lampMode("guard") : 0);
                this.indicator(p, out, light, 310, 585, 0.055, Ip203apBlockEntity.lampVisible(middle, time));
                this.indicator(
                    p, out, light, 310, 645, 0.055, Ip203apBlockEntity.lampVisible(b.lampMode(this.family.equals("kip234r2") ? "front" : "guard"), time)
                );
            }

            if (!this.family.equals("ip200pc")) {
                this.indicator(p, out, light, 375, 290, 0.13, Ip203apBlockEntity.lampVisible(b.lampMode("power"), time));
            }
        }

        if (!b.lifted()) {
            this.shell(p, out, light, true, false);
        }

        LogoRenderer.wall(
            b,
            p,
            out,
            6.1F,
            this.compact() ? 7.88F : (this.bus ? (this.family.equals("kip234r2") ? 7.88F : 8.12F) : 7.72F),
            7.2F,
            this.compact() ? 8.12F : (this.bus ? (this.family.equals("kip234r2") ? 8.12F : 8.36F) : 7.96F),
            14.18F,
            this.tex("logo"),
            light
        );
        p.popPose();
    }
}
