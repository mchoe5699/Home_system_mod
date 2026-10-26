package com.qwerty.homenet.sht;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.client.kvm.KvmCameraView;
import com.qwerty.homenet.move.DeviceOffsets;
import com.qwerty.homenet.video.VideoFormat;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

final class Sht104MVisual {
    static final float W = 592.0F;
    static final float H = 405.0F;
    static final float S = 0.001F;
    private static final ThreadLocal<Boolean> CS_SKIN = ThreadLocal.withInitial(() -> false);

    private static int skin(int color) {
        if (!CS_SKIN.get()) {
            return color;
        } else {
            int r = color >> 16 & 0xFF;
            int g = color >> 8 & 0xFF;
            int b = color & 0xFF;
            int gray = Math.min(255, Math.round((r * 0.299F + g * 0.587F + b * 0.114F) * 1.03F));
            return gray << 16 | gray << 8 | gray;
        }
    }

    private static Surface.Solid solid(String name, float x, float y, float w, float h, float z, float depth, float radius, String color) {
        if (CS_SKIN.get()) {
            int rgb = Integer.parseInt(color.substring(1), 16);
            color = String.format("#%06x", skin(rgb));
        }

        return new Surface.Solid(name, x, y, w, h, z, depth, radius, color, "body", 0.0F);
    }

    static void draw(PoseStack p, MultiBufferSource out, int light, boolean docked) {
        draw(p, out, light, docked, "C");
    }

    static void draw(PoseStack p, MultiBufferSource out, int light, boolean docked, String variant) {
        draw(p, out, light, docked, variant, false);
    }

    static void draw(PoseStack p, MultiBufferSource out, int light, boolean docked, String variant, boolean color) {
        draw(p, out, light, docked, variant, color, false);
    }

    static void draw(PoseStack p, MultiBufferSource out, int light, boolean docked, String variant, boolean color, boolean s1) {
        String type = variant.matches("[ABCD]") ? variant : "C";
        GeometryCache.draw(
            "sht104" + (s1 ? "cs" : (color ? "c" : "m")) + "_" + type + "_" + (docked ? "docked" : "open"),
            p,
            out,
            light,
            (pose, buffers) -> drawUncached(pose, buffers, docked, type, color, s1)
        );
    }

    private static void drawUncached(PoseStack p, MultiBufferSource out, boolean docked, String variant, boolean color, boolean s1) {
        CS_SKIN.set(false);
        int light = 15728880;
        if (!variant.equals("C")) {
            Surface.solid(solid("antenna_socket", 516.0F, -2.0F, 13.0F, 18.0F, 8.0F, 12.0F, 5.0F, "#373938"), p, out, light);
            Surface.solid(solid("antenna_shaft", 518.0F, -73.0F, 9.0F, 78.0F, 9.0F, 9.0F, 4.0F, "#303132"), p, out, light);
            Surface.solid(solid("antenna_tip", 518.0F, -77.0F, 9.0F, 13.0F, 9.0F, 9.0F, 4.0F, "#414242"), p, out, light);
        }

        Surface.solid(solid("plate", 1.0F, 1.0F, 590.0F, 403.0F, 0.0F, 16.0F, 20.0F, "#dcd4ba"), p, out, light);
        Surface.rectangle(p, out, 0.0F, 0.0F, 592.0F, 405.0F, 16.2F, Product.id("textures/block/sht_104" + (s1 ? "cs" : "m") + "_plate.png"), light);
        sideCover(p, out, light, false);
        sideCover(p, out, light, true);
        p.pushPose();
        p.translate(0.0F, 0.0F, -8.0F);
        Surface.solid(solid("housing", 40.0F, 25.0F, 513.0F, 354.0F, 16.0F, 34.0F, 2.0F, "#e6dfc8"), p, out, light);
        Surface.rim(
            p,
            out,
            light,
            solid("face_outer", 40.0F, 25.0F, 513.0F, 354.0F, 57.0F, 0.0F, 2.0F, "#e6dfc8"),
            solid("face_inner", 41.0F, 26.0F, 511.0F, 352.0F, 50.0F, 0.0F, 1.0F, "#e6dfc8"),
            15130568
        );
        ResourceLocation face = Product.id("textures/block/sht_104" + (s1 ? "cs" : (color ? "c" : "m")) + "_face.png");
        Surface.patch(p, out, 350.4F, 25.0F, 1.2F, 354.0F, 54.6F, 12235422, light);
        Surface.patch(p, out, 451.4F, 25.0F, 1.2F, 354.0F, 54.6F, 12235422, light);
        Surface.patch(p, out, 351.0F, 223.5F, 202.0F, 2.1F, 54.6F, 12235422, light);
        texturePart(p, out, face, 40.0F, 25.0F, 310.4F, 354.0F, 57.0F, light);
        texturePart(p, out, face, 351.6F, 25.0F, 99.8F, 198.5F, 56.8F, light);
        texturePart(p, out, face, 351.6F, 225.6F, 99.8F, 153.4F, 56.8F, light);
        texturePart(p, out, face, 452.6F, 25.0F, 100.4F, 198.5F, 56.6F, light);
        texturePart(p, out, face, 452.6F, 225.6F, 100.4F, 153.4F, 56.6F, light);
        Surface.patch(p, out, 150.0F, 44.0F, 172.0F, 141.0F, 50.5F, 2435115, light);
        frontRim(
            p,
            out,
            light,
            solid("crt_outer", 150.0F, 44.0F, 172.0F, 141.0F, 56.0F, 0.0F, 2.0F, "#a8abaa"),
            solid("crt_inner", 166.0F, 69.0F, 141.0F, 95.0F, 55.7F, 0.0F, 1.0F, "#1d2024"),
            11053994
        );
        frontRim(
            p,
            out,
            light,
            solid("crt_mask", 166.0F, 69.0F, 141.0F, 95.0F, 55.7F, 0.0F, 1.0F, "#222428"),
            solid("crt_glass", 180.0F, 74.0F, 113.0F, 84.0F, 53.6F, 0.0F, 1.0F, "#6e7481"),
            1908772
        );
        Surface.patch(p, out, 180.0F, 74.0F, 113.0F, 84.0F, 53.6F, 6449527, light);
        Surface.patch(p, out, 183.0F, 77.0F, 107.0F, 79.0F, 53.8F, 7897230, light);
        leftCapRim(p, out, light);
        Surface.patch(p, out, 451.5F, 57.0F, 1.0F, 34.0F, 64.05F, 13288109, light);
        Surface.patch(p, out, 451.5F, 91.0F, 1.0F, 49.0F, 62.05F, 1513241, light);
        Surface.patch(p, out, 451.5F, 140.0F, 1.0F, 37.0F, 64.05F, 13288109, light);
        controlCrescent(p, out, light);
        Surface.solid(solid("belt", 351.0F, 225.7F, 202.0F, 17.4F, 56.7F, 1.8F, 0.5F, "#ded5bf"), p, out, light);
        functionKey(p, out, 171.7F, 205.5F, 173.7F, 206.0F, light);
        functionKey(p, out, 207.5F, 257.0F, 207.0F, 260.0F, light);
        functionKey(p, out, 263.0F, 296.3F, 261.0F, 298.3F, light);

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 3; col++) {
                float x = 176.7F + 39 * col;
                float y = 267.4F + 22.6F * row;
                keycap(p, out, x, y, col, light);
                texturePart(p, out, Product.id("textures/block/sht_104" + (s1 ? "cs" : "m") + "_keys.png"), x, y, 36.9F, 17.9F, 60.45F, light);
            }
        }

        for (float centerY : new float[]{265.6F, 282.6F}) {
            float x = 310.8F;
            float y = centerY - 4.75F;
            Surface.solid(solid("volume", x, y, 20.4F, 9.5F, 57.0F, 2.8F, 4.75F, "#eee7d4"), p, out, light);
            decal(p, out, x, y, 20.4F, 9.5F, 59.95F, light);
        }

        roundButton(p, out, 321.0F, 310.0F, 8.7F, light);
        roundButton(p, out, 322.0F, 343.0F, 11.2F, light);

        for (float y : new float[]{201.0F, 265.0F, 287.4F, 310.8F}) {
            Surface.solid(solid("groove", 429.0F, y - 8.7F, 22.0F, 17.4F, 55.3F, 1.0F, 8.7F, "#c5bca8"), p, out, light);
            roundButton(p, out, 429.6F, y, 8.7F, light);
        }

        roundButton(p, out, 430.7F, 343.0F, 11.2F, light);
        Surface.rectangle(p, out, 0.0F, 0.0F, 592.0F, 405.0F, 59.25F, Product.id("textures/block/sht_104" + (s1 ? "cs" : "m") + "_details.png"), light);
        if (!variant.equals("C")) {
            texturePart(p, out, Product.id("textures/block/sht_104m_type_" + variant.toLowerCase() + ".png"), 452.6F, 174.0F, 100.4F, 142.0F, 57.2F, light);
            if (variant.equals("A") || variant.equals("D")) {
                for (float x : new float[]{471.0F, 492.0F, 513.0F, 534.0F}) {
                    roundButton(p, out, x, 201.0F, 7.4F, light);
                }
            }

            if (variant.equals("B")) {
                roundButton(p, out, 532.0F, 201.0F, 8.7F, light);
                roundButton(p, out, 532.0F, 265.0F, 8.7F, light);
            } else {
                roundButton(p, out, 532.0F, 265.0F, 8.7F, light);
                roundButton(p, out, 532.0F, 287.4F, 8.7F, light);
            }

            drawPartLabels(p, out, light, variant);
        }

        if (docked) {
            sculptedHandset(p, out, light);
        }

        p.popPose();
        CS_SKIN.remove();
    }

    private static void drawPartLabels(PoseStack p, MultiBufferSource out, int light, String variant) {
        if (!variant.equals("A") && !variant.equals("D")) {
            if (variant.equals("B")) {
                partLabel(p, out, light, "무선전화", 488.0F, 194.0F);
                partLabel(p, out, light, "내선", 488.0F, 258.0F);
            }
        } else {
            String[] names = variant.equals("A") ? new String[]{"재생", "녹화", "지움", "시계"} : new String[]{"방송", "청취", "중단", "YES"};
            float[] xs = new float[]{471.0F, 492.0F, 513.0F, 534.0F};

            for (int index = 0; index < 4; index++) {
                partLabel(p, out, light, names[index], xs[index], 176.0F);
            }

            partLabel(p, out, light, "무선전화", 488.0F, 258.0F);
            partLabel(p, out, light, "내선", 488.0F, 281.0F);
        }
    }

    private static void partLabel(PoseStack p, MultiBufferSource out, int light, String value, float cx, float y) {
        Font font = Minecraft.getInstance().font;
        p.pushPose();
        p.translate((double)cx, (double)(700.0F - y), 57.9);
        p.scale(1.0F, -1.0F, 1.0F);
        font.drawInBatch(value, -font.width(value) / 2.0F, 0.0F, -14341592, false, p.last().pose(), out, DisplayMode.NORMAL, 0, light);
        p.popPose();
    }

    private static void sideCover(PoseStack p, MultiBufferSource out, int light, boolean right) {
        VertexConsumer b = out.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));
        float outside = right ? 588.0F : 5.0F;
        float inside = right ? 553.0F : 40.0F;

        for (int i = 0; i < 24; i++) {
            float u = i / 24.0F;
            float v = (i + 1) / 24.0F;
            float x = outside + (inside - outside) * u;
            float nx = outside + (inside - outside) * v;
            float z = 16.3F + 32.7F * (float)Math.sin(u * Math.PI / 2.0);
            float nz = 16.3F + 32.7F * (float)Math.sin(v * Math.PI / 2.0);
            float top = 18.0F + 7.0F * u;
            float ntop = 18.0F + 7.0F * v;
            float bottom = 386.0F - 7.0F * u;
            float nbottom = 386.0F - 7.0F * v;
            int color = skin(i < 4 ? 14077625 : (i < 18 ? 15196107 : 15788246));

            for (float[] q : new float[][]{{x, top, z}, {nx, ntop, nz}, {nx, nbottom, nz}, {x, bottom, z}}) {
                Surface.vertex(b, p.last(), q[0], q[1], q[2], 0.0F, 0.0F, 1.0F, color, light, 0.5F, 0.5F);
            }
        }
    }

    private static void roundButton(PoseStack p, MultiBufferSource out, float cx, float cy, float r, int light) {
        Surface.solid(solid("round_key", cx - r, cy - r, r * 2.0F, r * 2.0F, 57.0F, 4.0F, r, "#c9cbc8"), p, out, light);
        decal(p, out, cx - r, cy - r, r * 2.0F, r * 2.0F, 61.15F, light);
    }

    private static ArrayList<float[]> keyOutline(float x, float y, int column) {
        float[] radii = column == 0
            ? new float[]{3.1F, 1.0F, 1.0F, 3.1F}
            : (column == 2 ? new float[]{1.0F, 3.1F, 3.1F, 1.0F} : new float[]{1.0F, 1.0F, 1.0F, 1.0F});
        float[][] corners = new float[][]{
            {x + 36.9F - radii[1], y + radii[1], radii[1], -90.0F},
            {x + 36.9F - radii[2], y + 17.9F - radii[2], radii[2], 0.0F},
            {x + radii[3], y + 17.9F - radii[3], radii[3], 90.0F},
            {x + radii[0], y + radii[0], radii[0], 180.0F}
        };
        ArrayList<float[]> points = new ArrayList<>();

        for (float[] corner : corners) {
            for (int step = 0; step <= 4; step++) {
                double angle = Math.toRadians(corner[3] + 90.0 * step / 4.0);
                points.add(new float[]{corner[0] + corner[2] * (float)Math.cos(angle), corner[1] + corner[2] * (float)Math.sin(angle)});
            }
        }

        return points;
    }

    private static void keycap(PoseStack p, MultiBufferSource out, float x, float y, int col, int light) {
        VertexConsumer b = out.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));
        ArrayList<float[]> outline = keyOutline(x, y, col);

        for (int i = 0; i < outline.size(); i++) {
            float[] a = outline.get(i);
            float[] c = outline.get((i + 1) % outline.size());

            for (float[] v : new float[][]{{a[0], a[1], 57.0F}, {c[0], c[1], 57.0F}, {c[0], c[1], 60.3F}, {a[0], a[1], 60.3F}}) {
                Surface.vertex(b, p.last(), v[0], v[1], v[2], 0.0F, 0.0F, 1.0F, skin(13682870), light, 0.5F, 0.5F);
            }

            for (float[] v : new float[][]{{x + 18.45F, y + 8.95F}, {a[0], a[1]}, {c[0], c[1]}, {x + 18.45F, y + 8.95F}}) {
                Surface.vertex(b, p.last(), v[0], v[1], 60.3F, 0.0F, 0.0F, 1.0F, skin(15656916), light, 0.5F, 0.5F);
            }
        }
    }

    private static void frontRim(PoseStack p, MultiBufferSource out, int light, Surface.Solid outer, Surface.Solid inner, int color) {
        VertexConsumer b = out.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));

        for (int i = 0; i < 64; i++) {
            float[] a = Surface.edge(outer, i, 0.0F);
            float[] c = Surface.edge(outer, (i + 1) % 64, 0.0F);
            float[] d = Surface.edge(inner, i, 0.0F);
            float[] e = Surface.edge(inner, (i + 1) % 64, 0.0F);

            for (float[] v : new float[][]{{a[0], a[1], outer.z()}, {c[0], c[1], outer.z()}, {e[0], e[1], inner.z()}, {d[0], d[1], inner.z()}}) {
                Surface.vertex(b, p.last(), v[0], v[1], v[2], 0.0F, 0.0F, 1.0F, skin(color), light, 0.5F, 0.5F);
            }
        }
    }

    private static float crescentTop(float u) {
        return 225.7F + 13.3F * (float)Math.pow(Math.max(0.0F, 1.0F - u / 0.12F), 2.0);
    }

    private static float crescentBottom(float u) {
        return 243.1F + 18.0F * (float)Math.sin(Math.PI * u);
    }

    private static void controlCrescent(PoseStack p, MultiBufferSource out, int light) {
        VertexConsumer b = out.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));

        for (int i = 0; i < 64; i++) {
            float u = i / 64.0F;
            float v = (i + 1) / 64.0F;
            float x = 136.0F + 215.0F * u;
            float nx = 136.0F + 215.0F * v;
            float top = crescentTop(u);
            float nextTop = crescentTop(v);
            float bottom = crescentBottom(u);
            float nextBottom = crescentBottom(v);
            crescentStrip(b, p, x, nx, top, nextTop, 57.0F, top + 2.0F, nextTop + 2.0F, 54.3F, 10984840, light);
            crescentStrip(b, p, x, nx, top + 2.0F, nextTop + 2.0F, 54.3F, bottom - 2.0F, nextBottom - 2.0F, 54.3F, 14274747, light);
            crescentStrip(b, p, x, nx, bottom - 2.0F, nextBottom - 2.0F, 54.3F, bottom, nextBottom, 57.0F, 15787986, light);
        }
    }

    private static void crescentStrip(
        VertexConsumer b, PoseStack p, float x, float nx, float ya, float nya, float za, float yb, float nyb, float zb, int color, int light
    ) {
        for (float[] v : new float[][]{{x, ya, za}, {nx, nya, za}, {nx, nyb, zb}, {x, yb, zb}}) {
            Surface.vertex(b, p.last(), v[0], v[1], v[2], 0.0F, 0.0F, 1.0F, skin(color), light, 0.5F, 0.5F);
        }
    }

    private static void decal(PoseStack p, MultiBufferSource out, float x, float y, float w, float h, float z, int light) {
        texturePart(p, out, Product.id("textures/block/sht_104" + (CS_SKIN.get() ? "cs" : "m") + "_gui.png"), x, y, w, h, z, light);
    }

    private static void texturePart(PoseStack p, MultiBufferSource out, ResourceLocation texture, float x, float y, float w, float h, float z, int light) {
        VertexConsumer b = out.getBuffer(RenderType.entityCutoutNoCull(texture));

        for (float[] v : new float[][]{
            {x, y + h, x / 592.0F, (y + h) / 405.0F},
            {x + w, y + h, (x + w) / 592.0F, (y + h) / 405.0F},
            {x + w, y, (x + w) / 592.0F, y / 405.0F},
            {x, y, x / 592.0F, y / 405.0F}
        }) {
            Surface.vertex(b, p.last(), v[0], v[1], z, 0.0F, 0.0F, 1.0F, 16777215, light, v[2], v[3]);
        }
    }

    private static float functionArc(float x) {
        return 260.4F - 0.00225F * (x - 234.0F) * (x - 234.0F);
    }

    private static void functionKey(PoseStack p, MultiBufferSource out, float topLeft, float topRight, float bottomLeft, float bottomRight, int light) {
        VertexConsumer b = out.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));
        float[][] outline = new float[15][2];
        outline[0] = new float[]{topLeft, 239.0F};
        outline[1] = new float[]{topRight, 239.0F};

        for (int i = 0; i <= 12; i++) {
            float x = bottomRight + (bottomLeft - bottomRight) * i / 12.0F;
            outline[i + 2] = new float[]{x, functionArc(x)};
        }

        for (int i = 0; i < outline.length; i++) {
            int j = (i + 1) % outline.length;
            float[] a = outline[i];
            float[] c = outline[j];

            for (float[] v : new float[][]{{a[0], a[1], 54.3F}, {c[0], c[1], 54.3F}, {c[0], c[1], 58.3F}, {a[0], a[1], 58.3F}}) {
                Surface.vertex(b, p.last(), v[0], v[1], v[2], 0.0F, 0.0F, 1.0F, 13616298, light, 0.5F, 0.5F);
            }

            for (float[] v : new float[][]{{(topLeft + topRight) / 2.0F, 246.0F}, {a[0], a[1]}, {c[0], c[1]}, {(topLeft + topRight) / 2.0F, 246.0F}}) {
                Surface.vertex(b, p.last(), v[0], v[1], 58.3F, 0.0F, 0.0F, 1.0F, 15854297, light, 0.5F, 0.5F);
            }
        }
    }

    private static ArrayList<float[]> leftOutline(float x0, float y0, float x1, float y1, float r) {
        ArrayList<float[]> outline = new ArrayList<>();
        outline.add(new float[]{x0 + r, y0});
        outline.add(new float[]{x1, y0});
        outline.add(new float[]{x1, y1});
        outline.add(new float[]{x0 + r, y1});

        for (int i = 1; i <= 16; i++) {
            double a = (Math.PI / 2) + i * Math.PI / 16.0;
            outline.add(new float[]{x0 + r + r * (float)Math.cos(a), y0 + r + r * (float)Math.sin(a)});
        }

        return outline;
    }

    private static void leftCapRim(PoseStack p, MultiBufferSource out, int light) {
        VertexConsumer b = out.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));
        ArrayList<float[]> outer = leftOutline(369.0F, 56.0F, 553.0F, 178.0F, 61.0F);
        ArrayList<float[]> inner = leftOutline(380.0F, 91.0F, 553.0F, 140.0F, 24.5F);

        for (int i = 0; i < outer.size(); i++) {
            int j = (i + 1) % outer.size();
            float[] a = outer.get(i);
            float[] c = outer.get(j);
            float[] d = inner.get(i);
            float[] e = inner.get(j);
            float[][] rings = new float[][]{
                {a[0], a[1], 57.0F},
                {a[0] + (d[0] - a[0]) * 0.28F, a[1] + (d[1] - a[1]) * 0.28F, 60.0F},
                {a[0] + (d[0] - a[0]) * 0.68F, a[1] + (d[1] - a[1]) * 0.68F, 64.5F},
                {d[0], d[1], 65.0F}
            };
            float[][] next = new float[][]{
                {c[0], c[1], 57.0F},
                {c[0] + (e[0] - c[0]) * 0.28F, c[1] + (e[1] - c[1]) * 0.28F, 60.0F},
                {c[0] + (e[0] - c[0]) * 0.68F, c[1] + (e[1] - c[1]) * 0.68F, 64.5F},
                {e[0], e[1], 65.0F}
            };
            int[] colors = new int[]{13288108, 15064264, 15788244};

            for (int k = 0; k < 3; k++) {
                for (float[] v : new float[][]{rings[k], next[k], next[k + 1], rings[k + 1]}) {
                    Surface.vertex(b, p.last(), v[0], v[1], v[2], 0.0F, 0.0F, 1.0F, colors[k], light, 0.5F, 0.5F);
                }
            }

            for (float[] v : new float[][]{{d[0], d[1], 65.0F}, {e[0], e[1], 65.0F}, {e[0], e[1], 62.0F}, {d[0], d[1], 62.0F}}) {
                Surface.vertex(b, p.last(), v[0], v[1], v[2], 0.0F, 0.0F, 1.0F, 10787978, light, 0.5F, 0.5F);
            }

            for (float[] v : new float[][]{{466.5F, 115.5F}, {d[0], d[1]}, {e[0], e[1]}, {466.5F, 115.5F}}) {
                Surface.vertex(b, p.last(), v[0], v[1], 62.0F, 0.0F, 0.0F, 1.0F, 2368552, light, 0.5F, 0.5F);
            }
        }
    }

    private static void sculptedHandset(PoseStack p, MultiBufferSource out, int light) {
        VertexConsumer b = out.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));
        int steps = 80;

        for (int i = 0; i < steps; i++) {
            float y0 = 44.0F + 313.0F * i / steps;
            float y1 = 44.0F + 313.0F * (i + 1) / steps;

            for (int face = 0; face < 3; face++) {
                float z0 = face == 0 ? 57.0F : (face == 1 ? 78.0F : 94.0F);
                float z1 = face == 0 ? 78.0F : (face == 1 ? 94.0F : 98.0F);
                int color = face == 0 ? 14538437 : (face == 1 ? 15525333 : 15920607);
                float inset0 = face == 0 ? 0.0F : (face == 1 ? 3.0F : 7.0F);
                float inset1 = face == 0 ? 3.0F : (face == 1 ? 7.0F : 9.0F);

                for (int side = 0; side < 2; side++) {
                    float a = handsetEdge(y0, side, inset0);
                    float c = handsetEdge(y1, side, inset0);
                    float d = handsetEdge(y0, side, inset1);
                    float e = handsetEdge(y1, side, inset1);

                    for (float[] v : new float[][]{{a, y0, z0}, {c, y1, z0}, {e, y1, z1}, {d, y0, z1}}) {
                        Surface.vertex(b, p.last(), v[0], v[1], v[2], side == 0 ? -1.0F : 1.0F, 0.0F, 0.4F, color, light, 0.5F, 0.5F);
                    }
                }
            }

            float l0 = handsetEdge(y0, 0, 9.0F);
            float r0 = handsetEdge(y0, 1, 9.0F);
            float l1 = handsetEdge(y1, 0, 9.0F);
            float r1 = handsetEdge(y1, 1, 9.0F);

            for (float[] v : new float[][]{{l0, y0, 98.0F}, {l1, y1, 98.0F}, {r1, y1, 98.0F}, {r0, y0, 98.0F}}) {
                Surface.vertex(b, p.last(), v[0], v[1], v[2], 0.0F, 0.0F, 1.0F, 15920866, light, 0.5F, 0.5F);
            }
        }
    }

    private static float handsetEdge(float y, int side, float inset) {
        float t = (y - 44.0F) / 313.0F;
        float cap = 1.0F;
        if (y < 67.0F) {
            cap = 0.5F + 0.5F * (float)Math.sqrt(Math.max(0.0, 1.0 - Math.pow((y - 67.0F) / 23.0F, 2.0)));
        } else if (y > 334.0F) {
            cap = 0.5F + 0.5F * (float)Math.sqrt(Math.max(0.0, 1.0 - Math.pow((y - 334.0F) / 23.0F, 2.0)));
        }

        float bulge = (float)(0.5 + 0.5 * Math.cos((Math.PI * 2) * t));
        float width = 61.0F + 13.0F * bulge;
        float edgeInset = inset * cap;
        return side == 0 ? 81.0F - 37.0F * cap + edgeInset : 81.0F + (width - 37.0F) * cap - edgeInset;
    }

    static void pose(Station104M.Unit u, PoseStack p) {
        Vec3 off = DeviceOffsets.get(u.getLevel(), u.getBlockPos());
        p.translate(off.x, off.y, off.z);
        double bottom = 0.29769999999999996;
        switch ((Direction)u.getBlockState().getValue(WallMountedBlock.FACING)) {
            case NORTH:
                p.translate(0.5, bottom, 1.0);
                p.mulPose(Axis.YP.rotationDegrees(180.0F));
                break;
            case SOUTH:
                p.translate(0.5, bottom, 0.0);
                break;
            case EAST:
                p.translate(0.0, bottom, 0.5);
                p.mulPose(Axis.YP.rotationDegrees(90.0F));
                break;
            case WEST:
                p.translate(1.0, bottom, 0.5);
                p.mulPose(Axis.YP.rotationDegrees(-90.0F));
        }

        p.scale(0.001F, 0.001F, 0.001F);
        p.translate(-296.0F, -295.0F, 0.0F);
    }

    static final class Renderer implements BlockEntityRenderer<Station104M.Unit> {
        final KvmCameraView video = new KvmCameraView();

        @Override
        public int getViewDistance() {
            return 24;
        }

        public void render(Station104M.Unit u, float time, PoseStack p, MultiBufferSource out, int light, int overlay) {
            p.pushPose();
            Sht104MVisual.pose(u, p);
            boolean s1 = u instanceof Station104CS.Unit;
            boolean color = s1 || u instanceof Station104C.Unit;
            Sht104MVisual.draw(p, out, light, u.docked, u.variant, color, s1);
            int[] recorded = u.playbackFrame();
            if (recorded != null) {
                Sht104Recording.wall(p, out, recorded, color);
            } else {
                BlockPos camera = VideoDisplays.camera(u.getBlockPos());
                if (camera == null) {
                    camera = u.camera();
                }

                if (camera != null) {
                    if (color && SamsungVideoView.isColorLobby(u.getLevel(), camera)) {
                        SamsungVideoView.wall(p, out, camera);
                    } else {
                        p.pushPose();
                        p.scale(16.0F, 16.0F, 16.0F);
                        this.video
                            .drawWall(
                                p,
                                out,
                                camera,
                                286.0F,
                                547.0F,
                                183.0F,
                                621.0F,
                                46.2F,
                                u.brightness,
                                u.contrast,
                                VideoDisplays.sourceFormat(u.getLevel(), camera, color ? VideoFormat.COLOR : VideoFormat.MONOCHROME)
                            );
                        p.popPose();
                    }
                }
            }

            p.popPose();
        }
    }

    static final class RendererC implements BlockEntityRenderer<Station104C.Unit> {
        private final Sht104MVisual.Renderer base = new Sht104MVisual.Renderer();

        @Override
        public int getViewDistance() {
            return this.base.getViewDistance();
        }

        public void render(Station104C.Unit u, float time, PoseStack p, MultiBufferSource out, int light, int overlay) {
            this.base.render((Station104M.Unit)u, time, p, out, light, overlay);
        }
    }

    static final class RendererCS implements BlockEntityRenderer<Station104CS.Unit> {
        private final Sht104MVisual.Renderer base = new Sht104MVisual.Renderer();

        @Override
        public int getViewDistance() {
            return this.base.getViewDistance();
        }

        public void render(Station104CS.Unit u, float time, PoseStack p, MultiBufferSource out, int light, int overlay) {
            this.base.render((Station104M.Unit)u, time, p, out, light, overlay);
        }
    }
}
