package com.qwerty.homenet.sht;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.move.DeviceOffsets;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

final class Ht100sVisual {
    static final float W = 230.0F;
    static final float H = 300.0F;
    static final float S = 5.0E-4F;
    static final float WINDOW_X = 64.0F;
    static final float WINDOW_Y = 43.0F;
    static final float WINDOW_W = 108.0F;
    static final float WINDOW_H = 108.0F;
    static final float UPPER_TOP_Z = 80.0F;
    static final float FRONT_OFFSET_X = -3.0F;
    static final float FRONT_SHIFT_Y = 2.0F;
    static final ResourceLocation LOGO = Product.id("ht100s/logo.png");
    static final ResourceLocation CAMERA_LOGO = Product.id("ht100/logo.png");
    static final ResourceLocation BRAND = Product.id("ht100s/brand.png");
    static final ResourceLocation WHITE = Product.id("textures/solid.png");

    static void solid(PoseStack p, MultiBufferSource o, int l, String name, float x, float y, float w, float h, float z, float d, float r, int c) {
        Surface.solid(new Surface.Solid(name, x, y, w, h, z, d, r, String.format("#%06x", c), "ht100s", 0.0F), p, o, l);
    }

    static void patch(PoseStack p, MultiBufferSource o, int l, float x, float y, float w, float h, float z, int c) {
        Surface.patch(p, o, x, y, w, h, z, c, l);
    }

    private static float[] edge(Ht100sVisual.Ring q, int i) {
        int corner = i % 48 / 12;
        float angle = (float)Math.toRadians(-90 + corner * 90 + i % 12 * 7.5);
        float r = q.r;
        float cx = q.x + (corner != 0 && corner != 1 ? r : q.w - r);
        float cy = q.y + (corner != 0 && corner != 3 ? q.h - r : r);
        return new float[]{cx + r * (float)Math.cos(angle), cy + r * (float)Math.sin(angle), (float)Math.cos(angle), (float)Math.sin(angle)};
    }

    private static void vertex(PoseStack p, MultiBufferSource o, int l, float x, float y, float z, float nx, float ny, float nz, int c) {
        Surface.vertex(o.getBuffer(RenderType.entityCutoutNoCull(WHITE)), p.last(), x, y, z, nx, ny, nz, c, l, 0.5F, 0.5F);
    }

    private static void contour(PoseStack p, MultiBufferSource o, int l, boolean cap, Ht100sVisual.Ring... rings) {
        for (int n = 0; n < rings.length - 1; n++) {
            Ht100sVisual.Ring a = rings[n];
            Ht100sVisual.Ring b = rings[n + 1];

            for (int i = 0; i < 48; i++) {
                float[] u = edge(a, i);
                float[] v = edge(a, i + 1);
                float[] w = edge(b, i + 1);
                float[] x = edge(b, i);
                float nz = b.z - a.z == 0.0F ? 0.0F : 0.18F;
                int col = b.color;
                vertex(p, o, l, u[0], u[1], a.z, u[2], u[3], nz, col);
                vertex(p, o, l, v[0], v[1], a.z, v[2], v[3], nz, col);
                vertex(p, o, l, w[0], w[1], b.z, w[2], w[3], nz, col);
                vertex(p, o, l, x[0], x[1], b.z, x[2], x[3], nz, col);
            }
        }

        if (cap) {
            Ht100sVisual.Ring q = rings[rings.length - 1];
            float cx = q.x + q.w / 2.0F;
            float cy = q.y + q.h / 2.0F;

            for (int i = 0; i < 48; i++) {
                float[] a = edge(q, i);
                float[] b = edge(q, i + 1);
                vertex(p, o, l, a[0], a[1], q.z, 0.0F, 0.0F, 1.0F, q.color);
                vertex(p, o, l, b[0], b[1], q.z, 0.0F, 0.0F, 1.0F, q.color);
                vertex(p, o, l, cx, cy, q.z, 0.0F, 0.0F, 1.0F, q.color);
                vertex(p, o, l, cx, cy, q.z, 0.0F, 0.0F, 1.0F, q.color);
            }
        }
    }

    private static void rearCap(PoseStack p, MultiBufferSource o, int l, Ht100sVisual.Ring q) {
        float cx = q.x + q.w / 2.0F;
        float cy = q.y + q.h / 2.0F;

        for (int i = 0; i < 48; i++) {
            float[] a = edge(q, i);
            float[] b = edge(q, i + 1);
            vertex(p, o, l, a[0], a[1], q.z, 0.0F, 0.0F, -1.0F, q.color);
            vertex(p, o, l, cx, cy, q.z, 0.0F, 0.0F, -1.0F, q.color);
            vertex(p, o, l, b[0], b[1], q.z, 0.0F, 0.0F, -1.0F, q.color);
            vertex(p, o, l, b[0], b[1], q.z, 0.0F, 0.0F, -1.0F, q.color);
        }
    }

    private static void bodyShell(PoseStack p, MultiBufferSource o, int l) {
        int steps = 16;
        Ht100sVisual.Ring[] rings = new Ht100sVisual.Ring[18];
        rings[0] = new Ht100sVisual.Ring(0.0F, 0.0F, 230.0F, 300.0F, 13.0F, 35.0F, 11119023);

        for (int j = 0; j <= 16; j++) {
            double angle = j * Math.PI / 32.0;
            double inset = 9.0 * (1.0 - Math.cos(angle));
            float at = (float)inset;
            float z = (float)(59.0 + 9.0 * Math.sin(angle));
            int red = (int)(169.0 + 19.0 * Math.sin(angle));
            int green = (int)(169.0 + 19.0 * Math.sin(angle));
            int blue = (int)(175.0 + 19.0 * Math.sin(angle));
            rings[j + 1] = new Ht100sVisual.Ring(at, at, 230.0F - 2.0F * at, 300.0F - 2.0F * at, 13.0F, z, red << 16 | green << 8 | blue);
        }

        for (int n = 0; n < rings.length - 1; n++) {
            Ht100sVisual.Ring a = rings[n];
            Ht100sVisual.Ring b = rings[n + 1];
            float angleA = (float)(Math.max(0, n - 1) * Math.PI / 32.0);
            float angleB = (float)(n * Math.PI / 32.0);

            for (int i = 0; i < 48; i++) {
                float[] u = edge(a, i);
                float[] v = edge(a, i + 1);
                float[] w = edge(b, i + 1);
                float[] x = edge(b, i);
                bodyVertex(p, o, l, u, a, angleA);
                bodyVertex(p, o, l, v, a, angleA);
                bodyVertex(p, o, l, w, b, angleB);
                bodyVertex(p, o, l, x, b, angleB);
            }
        }
    }

    private static void bodyVertex(PoseStack p, MultiBufferSource o, int l, float[] at, Ht100sVisual.Ring ring, float angle) {
        float radial = (float)Math.cos(angle);
        vertex(p, o, l, at[0], at[1], ring.z, at[2] * radial, at[3] * radial, (float)Math.sin(angle), ring.color);
    }

    private static void bodyFace(PoseStack p, MultiBufferSource o, int l) {
        Ht100sVisual.Ring outer = new Ht100sVisual.Ring(9.0F, 9.0F, 212.0F, 282.0F, 13.0F, 68.0F, 12369090);
        Ht100sVisual.Ring inner = new Ht100sVisual.Ring(16.0F, 14.0F, 198.0F, 272.0F, 0.0F, 68.0F, 12369090);

        for (int i = 0; i < 48; i++) {
            float[] a = edge(outer, i);
            float[] b = edge(outer, i + 1);
            float[] c = edge(inner, i + 1);
            float[] d = edge(inner, i);

            for (float[] q : new float[][]{a, b, c, d}) {
                vertex(p, o, l, q[0], q[1], 68.0F, 0.0F, 0.0F, 1.0F, 12369090);
            }
        }

        patch(p, o, l, 16.0F, 14.0F, 198.0F, 186.0F, 68.0F, 12369090);
        patch(p, o, l, 16.0F, 200.0F, 121.0F, 73.0F, 68.0F, 12369090);
        patch(p, o, l, 207.0F, 200.0F, 7.0F, 73.0F, 68.0F, 12369090);
        patch(p, o, l, 16.0F, 273.0F, 198.0F, 13.0F, 68.0F, 12369090);
    }

    static float upperZ(float x, float y) {
        float tx = Math.min((x - 24.0F) / 40.0F, (212.0F - x) / 40.0F);
        float ty = Math.min((y - 9.0F) / 34.0F, (191.0F - y) / 40.0F);
        tx = Math.max(0.0F, Math.min(1.0F, tx));
        ty = Math.max(0.0F, Math.min(1.0F, ty));
        float blend = 0.35F;
        float h = Math.max(0.0F, blend - Math.abs(tx - ty)) / blend;
        float t = Math.max(0.0F, Math.min(1.0F, (Math.min(tx, ty) - h * h * blend * 0.25F) / (1.0F - blend * 0.25F)));
        return 68.0F + 12.0F * t * t * (3.0F - 2.0F * t);
    }

    private static void upperVertex(PoseStack p, MultiBufferSource o, int l, float x, float y) {
        float z = upperZ(x, y);
        float nx = upperZ(x - 1.0F, y) - upperZ(x + 1.0F, y);
        float ny = upperZ(x, y - 1.0F) - upperZ(x, y + 1.0F);
        float length = (float)Math.sqrt(nx * nx + ny * ny + 4.0F);
        int shade = (int)(188.0F + (z - 68.0F) * 0.2F);
        int color = shade << 16 | shade << 8 | shade + 6;
        vertex(p, o, l, x, y, z, nx / length, ny / length, 2.0F / length, color);
    }

    private static void upperFace(PoseStack p, MultiBufferSource o, int l) {
        for (int row = 0; row < 60; row++) {
            for (int col = 0; col < 48; col++) {
                float x0 = 24.0F + col * 188.0F / 48.0F;
                float x1 = 24.0F + (col + 1) * 188.0F / 48.0F;
                float y0 = 9.0F + row * 182.0F / 60.0F;
                float y1 = 9.0F + (row + 1) * 182.0F / 60.0F;
                upperVertex(p, o, l, x0, y1);
                upperVertex(p, o, l, x1, y1);
                upperVertex(p, o, l, x1, y0);
                upperVertex(p, o, l, x0, y0);
            }
        }
    }

    private static float[] lowerOutline(int i, float inset) {
        int corner = (i - 12) / 12;
        float angle = (float)Math.toRadians(corner * 90 + i % 12 * 7.5);
        float cx = corner == 0 ? 192.0F : 44.0F;
        float cy = 171.0F;
        float radius = 20.0F - inset;
        return new float[]{cx + radius * (float)Math.cos(angle), cy + radius * (float)Math.sin(angle)};
    }

    private static void upperSeamVertex(PoseStack p, MultiBufferSource o, int l, float x, float y) {
        float worldY = y - 2.0F;
        float z = y < 9.0F ? 59.0F + (float)Math.sqrt(Math.max(0.0F, 81.0F - (9.0F - worldY) * (9.0F - worldY))) : upperZ(x, y);
        vertex(p, o, l, x, y, z + 0.18F, 0.0F, 0.0F, 1.0F, 9276819);
    }

    private static void upperSeam(PoseStack p, MultiBufferSource o, int l) {
        float[][] right = new float[][]{{212.0F, 9.0F}, {212.0F, 171.0F}, {210.85F, 171.0F}, {210.85F, 9.0F}};

        for (float[] xy : right) {
            upperSeamVertex(p, o, l, xy[0], xy[1]);
        }

        for (int i = 12; i < 36; i++) {
            float[][] points = new float[][]{lowerOutline(i, 0.0F), lowerOutline(i + 1, 0.0F), lowerOutline(i + 1, 1.15F), lowerOutline(i, 1.15F)};

            for (float[] xy : points) {
                upperSeamVertex(p, o, l, xy[0], xy[1]);
            }
        }

        float[][] left = new float[][]{{24.0F, 171.0F}, {24.0F, 9.0F}, {25.15F, 9.0F}, {25.15F, 171.0F}};

        for (float[] xy : left) {
            upperSeamVertex(p, o, l, xy[0], xy[1]);
        }

        for (float[] side : new float[][]{{24.0F, 25.15F}, {212.0F, 210.85F}}) {
            for (int step = 0; step < 12; step++) {
                float y0 = 2.0F + step * 7.0F / 12.0F;
                float y1 = 2.0F + (step + 1) * 7.0F / 12.0F;
                float[][] points = new float[][]{{side[0], y0}, {side[0], y1}, {side[1], y1}, {side[1], y0}};

                for (float[] xy : points) {
                    upperSeamVertex(p, o, l, xy[0], xy[1]);
                }
            }
        }
    }

    private static void sector(PoseStack p, MultiBufferSource o, int l, float cx, float cy, float outer, float inner, float zOuter, float zInner, int color) {
        for (int i = 0; i < 64; i++) {
            float a = (float)(i * Math.PI / 32.0);
            float b = (float)((i + 1) * Math.PI / 32.0);
            float[] r = new float[]{a, b, b, a};
            float[] d = new float[]{outer, outer, inner, inner};

            for (int j = 0; j < 4; j++) {
                float x = cx + (float)Math.cos(r[j]) * d[j];
                float y = cy + (float)Math.sin(r[j]) * d[j];
                float gradient = (zOuter - zInner) / (outer - inner + 0.001F);
                float nx = (float)Math.cos(r[j]) * gradient;
                float ny = (float)Math.sin(r[j]) * gradient;
                float length = (float)Math.sqrt(1.0F + nx * nx + ny * ny);
                vertex(p, o, l, x, y, j < 2 ? zOuter : zInner, nx / length, ny / length, 1.0F / length, color);
            }
        }
    }

    private static void buttonFace(PoseStack p, MultiBufferSource o, int l) {
        float cx = 175.0F;
        float cy = 239.0F;
        float r = 25.0F;

        for (int i = 0; i < 64; i++) {
            float a = (float)(i * Math.PI / 32.0);
            float b = (float)((i + 1) * Math.PI / 32.0);
            float[] angles = new float[]{a, b, b, a};

            for (int j = 0; j < 4; j++) {
                float t = angles[j];
                float dx = (float)Math.cos(t);
                float dy = (float)Math.sin(t);
                float edge = Math.min(32.5F / Math.max(1.0E-4F, Math.abs(dx)), 33.5F / Math.max(1.0E-4F, Math.abs(dy)));
                float radius = j < 2 ? edge : r;
                vertex(p, o, l, cx + dx * radius, cy + dy * radius, 67.9F, 0.0F, 0.0F, 1.0F, j < 2 ? 12632262 : 11447988);
            }
        }

        sector(p, o, l, cx, cy, 25.0F, 19.0F, 67.9F, 67.2F, 11119023);
        sector(p, o, l, cx, cy, 19.0F, 11.0F, 67.2F, 66.15F, 11908539);
        sector(p, o, l, cx, cy, 11.0F, 0.0F, 66.15F, 65.8F, 12632262);
    }

    private static void arch(PoseStack p, MultiBufferSource o, int l, float x, float top, float w, float bottom, float z, int color) {
        float r = w / 2.0F;
        float cy = top + r;
        float cx = x + r;
        patch(p, o, l, x, cy, w, bottom - cy, z, color);

        for (int i = 0; i < 32; i++) {
            float a = (float)(Math.PI + i * Math.PI / 32.0);
            float b = (float)(Math.PI + (i + 1) * Math.PI / 32.0);
            vertex(p, o, l, cx + r * (float)Math.cos(a), cy + r * (float)Math.sin(a), z, 0.0F, 0.0F, 1.0F, color);
            vertex(p, o, l, cx + r * (float)Math.cos(b), cy + r * (float)Math.sin(b), z, 0.0F, 0.0F, 1.0F, color);
            vertex(p, o, l, cx, cy, z, 0.0F, 0.0F, 1.0F, color);
            vertex(p, o, l, cx, cy, z, 0.0F, 0.0F, 1.0F, color);
        }
    }

    private static void print(PoseStack p, MultiBufferSource o, int l, ResourceLocation texture, float x, float y, float w, float h, float z) {
        VertexConsumer b = o.getBuffer(RenderType.entityCutoutNoCull(texture));
        float[][] v = new float[][]{{x, y + h, 0.0F, 1.0F}, {x + w, y + h, 1.0F, 1.0F}, {x + w, y, 1.0F, 0.0F}, {x, y, 0.0F, 0.0F}};

        for (float[] a : v) {
            Surface.vertex(b, p.last(), a[0], a[1], z, 0.0F, 0.0F, 1.0F, 16777215, l, a[2], a[3]);
        }
    }

    private static void curvedLogo(PoseStack p, MultiBufferSource o, int l, boolean camera) {
        VertexConsumer b = o.getBuffer(RenderType.entityCutoutNoCull(camera ? CAMERA_LOGO : LOGO));

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 24; col++) {
                for (int corner = 0; corner < 4; corner++) {
                    float u = (col + (corner != 1 && corner != 2 ? 0 : 1)) / 24.0F;
                    float v = (row + (corner < 2 ? 1 : 0)) / 6.0F;
                    float x = 68.0F + 109.0F * u;
                    float y = 15.0F + 25.0F * v;
                    Surface.vertex(b, p.last(), x, y, upperZ(x, y) + 0.22F, 0.0F, 0.0F, 1.0F, 16777215, l, u, v);
                }
            }
        }
    }

    static void mesh(PoseStack p, MultiBufferSource o) {
        mesh(p, o, false);
    }

    static void mesh(PoseStack p, MultiBufferSource o, boolean camera) {
        int l = 0;
        Ht100sVisual.Ring rear = new Ht100sVisual.Ring(-6.0F, -5.0F, 242.0F, 310.0F, 13.0F, 0.0F, 10066335);
        rearCap(p, o, l, rear);
        contour(
            p,
            o,
            l,
            true,
            rear,
            new Ht100sVisual.Ring(-6.0F, -5.0F, 242.0F, 310.0F, 13.0F, 35.0F, 11119023),
            new Ht100sVisual.Ring(-5.0F, -4.0F, 240.0F, 308.0F, 12.0F, 41.0F, 12369090)
        );
        bodyShell(p, o, l);
        bodyFace(p, o, l);
        p.pushPose();
        p.translate(-3.0F, 2.0F, 0.0F);
        upperFace(p, o, l);
        upperSeam(p, o, l);
        solid(p, o, l, "window_gasket", 64.0F, 43.0F, 108.0F, 108.0F, 80.15F, 0.8F, 3.0F, 5592411);
        solid(p, o, l, "window_glass", 66.0F, 45.0F, 104.0F, 104.0F, 80.95F, 1.0F, 2.0F, camera ? 6580074 : 3158071);
        if (camera) {
            solid(p, o, l, "night_lamp_recess", 77.0F, 59.0F, 82.0F, 39.0F, 82.05F, 0.5F, 1.2F, 2498598);
            solid(p, o, l, "night_lamp_glass", 79.0F, 61.0F, 78.0F, 35.0F, 82.58F, 0.25F, 1.0F, 3022116);

            for (int row = 0; row < 2; row++) {
                for (int col = 0; col < 4; col++) {
                    sector(p, o, l, 91.0F + col * 18, 70.0F + row * 16, 2.2F, 0.0F, 82.9F, 82.9F, 3679787);
                }
            }

            sector(p, o, l, 118.0F, 125.0F, 25.0F, 18.0F, 83.2F, 84.1F, 1776672);
            sector(p, o, l, 118.0F, 125.0F, 18.0F, 13.0F, 84.1F, 83.3F, 3158584);
            sector(p, o, l, 118.0F, 125.0F, 13.0F, 7.0F, 83.3F, 82.9F, 988702);
            sector(p, o, l, 118.0F, 125.0F, 7.0F, 0.0F, 82.9F, 82.8F, 1185051);
            sector(p, o, l, 113.0F, 119.0F, 3.0F, 0.0F, 83.4F, 83.4F, 5593186);
        } else {
            patch(p, o, l, 70.0F, 48.0F, 96.0F, 1.1F, 81.98F, 4276551);
        }

        for (int i = 0; i < 6; i++) {
            float y = 205 + i * 13;
            patch(p, o, l, 26.0F, y - 2.0F, 104.0F, 2.0F, 69.2F, 12829641);
            solid(p, o, l, "speaker_floor_" + i, 29.0F, y, 99.0F, 3.6F, 68.2F, 0.35F, 1.7F, 4473929);
            patch(p, o, l, 30.0F, y + 3.6F, 97.0F, 0.65F, 69.4F, 11053231);
        }

        contour(
            p,
            o,
            l,
            false,
            new Ht100sVisual.Ring(140.0F, 202.0F, 70.0F, 73.0F, 6.0F, 65.5F, 8947855),
            new Ht100sVisual.Ring(140.0F, 202.0F, 70.0F, 73.0F, 6.0F, 67.2F, 11776953),
            new Ht100sVisual.Ring(142.0F, 204.0F, 66.0F, 69.0F, 5.0F, 67.9F, 13158606)
        );
        buttonFace(p, o, l);
        solid(p, o, l, "button_indicator", 169.0F, 209.0F, 13.0F, 4.5F, 67.95F, 0.3F, 2.0F, 2434346);
        arch(p, o, l, 107.0F, 274.5F, 25.0F, 291.0F, 68.25F, 6842477);
        arch(p, o, l, 108.5F, 276.0F, 22.0F, 290.8F, 68.42F, 11184816);
        curvedLogo(p, o, l, camera);
        print(p, o, l, BRAND, 146.0F, 278.0F, 64.0F, 12.0F, 68.4F);
        p.popPose();

        for (int i = 0; i < 2; i++) {
            solid(p, o, l, "mic_" + i, 212.0F, 38 + i * 8, 9.0F, 3.0F, 69.0F, 1.0F, 0.7F, 2368553);
        }
    }

    static void draw(PoseStack p, MultiBufferSource o, int light) {
        GeometryCache.draw("ht100s_cool_gray_shell", p, o, light, Ht100sVisual::mesh);
    }

    static void draw(PoseStack p, MultiBufferSource o, int light, boolean camera) {
        if (camera) {
            GeometryCache.draw("ht100_camera_shell", p, o, light, (q, out) -> mesh(q, out, true));
        } else {
            draw(p, o, light);
        }
    }

    static void pose(Ht100s.Unit b, PoseStack p) {
        Vec3 off = DeviceOffsets.get(b.getLevel(), b.getBlockPos());
        p.translate(off.x, off.y, off.z);
        double bottom = 0.425;
        switch ((Direction)b.getBlockState().getValue(WallMountedBlock.FACING)) {
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

        p.scale(5.0E-4F, 5.0E-4F, 5.0E-4F);
        p.translate(-115.0F, -400.0F, 0.0F);
    }

    static final class Renderer implements BlockEntityRenderer<Ht100s.Unit> {
        public void render(Ht100s.Unit b, float tick, PoseStack p, MultiBufferSource o, int light, int overlay) {
            boolean camera = b.getBlockState().is(Product.HT100.get());
            if (!DeviceVisibility.hidden(b, GeometryCache.bounds(camera ? "ht100_camera_shell" : "ht100s_cool_gray_shell"), q -> Ht100sVisual.pose(b, q))) {
                p.pushPose();
                Ht100sVisual.pose(b, p);
                Ht100sVisual.draw(p, o, light, camera);
                p.popPose();
            }
        }
    }

    private record Ring(float x, float y, float w, float h, float r, float z, int color) {
    }
}
