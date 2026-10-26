package com.qwerty.homenet.sht;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class Surface {
    public static final float SCALE = 6.067961E-4F;
    static Surface.Mount mounting;
    static Surface.Shell casing;
    static List<Surface.Solid> solids;
    static List<Surface.Key> buttons;
    static final Map<Boolean, Surface.Model> models = new HashMap<>();
    static boolean wiredModel;

    static ResourceLocation texture(String path) {
        return Product.id((wiredModel ? "sht102/" : "") + path);
    }

    static void load() {
        if (solids == null) {
            select(false);
        }
    }

    static void select(boolean wired) {
        if (solids == null || wiredModel != wired) {
            Surface.Model model = models.get(wired);
            if (model == null) {
                try (BufferedReader r = Minecraft.getInstance().getResourceManager().openAsReader(Product.id((wired ? "sht102/" : "") + "model.json"))) {
                    JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                    Gson g = new Gson();
                    ArrayList<Surface.Solid> ss = new ArrayList<>();
                    ArrayList<Surface.Key> bb = new ArrayList<>();

                    for (JsonElement e : o.getAsJsonArray("parts")) {
                        ss.add((Surface.Solid)g.fromJson(e, Surface.Solid.class));
                    }

                    for (JsonElement e : o.getAsJsonArray("keys")) {
                        bb.add((Surface.Key)g.fromJson(e, Surface.Key.class));
                    }

                    model = new Surface.Model(
                        (Surface.Mount)g.fromJson(o.get("mounting"), Surface.Mount.class),
                        (Surface.Shell)g.fromJson(o.get("shell"), Surface.Shell.class),
                        ss,
                        bb
                    );
                    models.put(wired, model);
                } catch (Exception var11) {
                    throw new IllegalStateException(var11);
                }
            }

            wiredModel = wired;
            mounting = model.mount;
            casing = model.shell;
            solids = model.solids;
            buttons = model.buttons;
        }
    }

    static void vertex(VertexConsumer b, Pose pose, float x, float y, float z, float nx, float ny, float nz, int col, int light, float u, float v) {
        b.vertex(pose.pose(), x, 700.0F - y, z)
            .color(col >> 16 & 0xFF, col >> 8 & 0xFF, col & 0xFF, 255)
            .uv(u, v)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(light)
            .normal(pose.normal(), nx, -ny, nz)
            .endVertex();
    }

    static float[] edge(Surface.Solid p, int n, float inset) {
        if (p.name.equals("portable_case")) {
            double t = (Math.PI * 2) * (n % 64) / 64.0;
            float nx = (float)Math.cos(t);
            float ny = -((float)Math.sin(t));
            float x = p.x + p.w / 2.0F + (p.w / 2.0F - inset) * nx;
            float y = n % 64 <= 32 ? p.y + p.w / 2.0F + (p.w / 2.0F - inset) * ny : p.y + p.h - 16.0F + (16.0F - inset) * ny;
            float taper = 1.0F - 0.055F * (float)Math.sin(Math.PI * Math.max(0.0F, Math.min(1.0F, (y - p.y) / p.h)));
            x = p.x + p.w / 2.0F + (x - p.x - p.w / 2.0F) * taper;
            return new float[]{x, y, nx, ny};
        } else if (p.name.length() != 2 || p.name.charAt(0) != 'r' && p.name.charAt(0) != 'n') {
            float rad = Math.max(0.0F, Math.min(p.r, Math.min(p.w, p.h) / 2.0F) - inset);
            int corner = n % 64 / 16;
            double a = (corner * 90 + n % 16 * 6) * Math.PI / 180.0;
            float cx = p.x + (corner != 0 && corner != 3 ? p.r : p.w - p.r);
            float cy = p.y + (corner < 2 ? p.h - p.r : p.r);
            float x = cx + rad * (float)Math.cos(a);
            float y = cy + rad * (float)Math.sin(a);
            if (p.name.equals("handset_back") || p.name.equals("radio_face")) {
                x = p.x + p.w / 2.0F + (x - p.x - p.w / 2.0F) * (0.95F + 0.05F * Math.abs((y - p.y) / p.h * 2.0F - 1.0F));
            }

            return new float[]{x, y, (float)Math.cos(a), (float)Math.sin(a)};
        } else {
            double t = (Math.PI * 2) * (n % 64) / 64.0;
            float nx = (float)Math.cos(t);
            float ny = (float)Math.sin(t);
            return new float[]{p.x + p.w / 2.0F + (p.w / 2.0F - inset) * nx, p.y + p.h / 2.0F + (p.h / 2.0F - inset) * ny, nx, ny};
        }
    }

    static void solid(Surface.Solid p, PoseStack ps, MultiBufferSource buffer, int light) {
        VertexConsumer b = buffer.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));
        int col = Integer.parseInt(p.color.substring(1), 16);
        float bevel = p.name.startsWith("face_") ? 0.0F : Math.max(0.03F, Math.min(p.d * 0.45F, Math.min(p.w, p.h) * 0.12F));
        if (p.name.equals("mount_plate")) {
            bevel = Math.min(0.12F, p.d * 0.45F);
        }

        Pose pose = ps.last();
        int step = !(p.w < 15.0F) && !(p.h < 8.0F) ? 2 : 4;

        for (int i = 0; i < 64; i += step) {
            for (int ring = 0; ring < 4; ring++) {
                float a = (float)((Math.PI / 2) * Math.max(0, ring - 1) / 3.0);
                float c = (float)((Math.PI / 2) * Math.max(0, ring) / 3.0);

                for (int j = 0; j < 4; j++) {
                    boolean upper = j == 1 || j == 2;
                    float angle = upper ? c : a;
                    float[] at = edge(p, (i + (j >= 2 ? step : 0)) % 64, bevel * (1.0F - (float)Math.cos(angle)));
                    float z = ring == 0 && !upper ? p.z : p.z + p.d - bevel + bevel * (float)Math.sin(angle);
                    vertex(
                        b,
                        pose,
                        at[0],
                        at[1],
                        z,
                        at[2] * (float)Math.cos(angle),
                        at[3] * (float)Math.cos(angle),
                        (float)Math.sin(angle),
                        col,
                        light,
                        0.5F,
                        0.5F
                    );
                }
            }

            float[] a = edge(p, i, bevel);
            float[] c = edge(p, i + step, bevel);
            float cx = p.x + p.w / 2.0F;
            float cy = p.y + p.h / 2.0F;
            int frontRings = p.curve > 0.0F ? 5 : 1;

            for (int ring = 0; ring < frontRings; ring++) {
                for (int j = 0; j < 4; j++) {
                    float radial = 1.0F - (float)(ring + (j != 1 && j != 2 ? 0 : 1)) / frontRings;
                    float[] e = j < 2 ? a : c;
                    float vx = cx + (e[0] - cx) * radial;
                    float vy = cy + (e[1] - cy) * radial;
                    float z = p.z + p.d + p.curve * (1.0F - radial * radial);
                    float nx = p.curve * 2.0F * (vx - cx) / (p.w * p.w / 4.0F);
                    float ny = p.curve * 2.0F * (vy - cy) / (p.h * p.h / 4.0F);
                    float nz = 1.0F;
                    float len = (float)Math.sqrt(nx * nx + ny * ny + 1.0F);
                    vertex(b, pose, vx, vy, z, nx / len, ny / len, nz / len, col, light, 0.5F, 0.5F);
                }
            }
        }
    }

    public static void rectangle(PoseStack p, MultiBufferSource out, float x, float y, float w, float h, float z, ResourceLocation tex, int light) {
        VertexConsumer b = out.getBuffer(RenderType.entityTranslucent(tex));

        for (float[] v : new float[][]{{x, y + h, 0.0F, 1.0F}, {x + w, y + h, 1.0F, 1.0F}, {x + w, y, 1.0F, 0.0F}, {x, y, 0.0F, 0.0F}}) {
            vertex(b, p.last(), v[0], v[1], z, 0.0F, 0.0F, 1.0F, 16777215, light, v[2], v[3]);
        }
    }

    static void indicator(PoseStack p, MultiBufferSource out, Indicators.Lamp lamp) {
        VertexConsumer b = out.getBuffer(RenderType.entityTranslucentEmissive(Product.id("textures/solid.png")));
        float z = lamp.round() ? 71.4F : 72.2F;
        if (lamp.round()) {
            float cx = lamp.x() + lamp.w() / 2.0F;
            float cy = lamp.y() + lamp.h() / 2.0F;

            for (int i = 0; i < 32; i++) {
                double a = -i * Math.PI / 16.0;
                double c = -(i + 1) * Math.PI / 16.0;

                for (float[] v : new float[][]{
                    {cx, cy},
                    {cx + (float)Math.cos(a) * lamp.w() / 2.0F, cy + (float)Math.sin(a) * lamp.h() / 2.0F},
                    {cx + (float)Math.cos(c) * lamp.w() / 2.0F, cy + (float)Math.sin(c) * lamp.h() / 2.0F},
                    {cx, cy}
                }) {
                    vertex(b, p.last(), v[0], v[1], z, 0.0F, 0.0F, 1.0F, lamp.rgb(), 15728880, 0.5F, 0.5F);
                }
            }
        } else {
            for (float[] v : new float[][]{
                {lamp.x(), lamp.y() + lamp.h()}, {lamp.x() + lamp.w(), lamp.y() + lamp.h()}, {lamp.x() + lamp.w(), lamp.y()}, {lamp.x(), lamp.y()}
            }) {
                vertex(b, p.last(), v[0], v[1], z, 0.0F, 0.0F, 1.0F, lamp.rgb(), 15728880, 0.5F, 0.5F);
            }
        }
    }

    static void patch(PoseStack p, MultiBufferSource out, float x, float y, float w, float h, float z, int color, int light) {
        VertexConsumer b = out.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));

        for (float[] v : new float[][]{{x, y + h}, {x + w, y + h}, {x + w, y}, {x, y}}) {
            vertex(b, p.last(), v[0], v[1], z, 0.0F, 0.0F, 1.0F, color, light, 0.5F, 0.5F);
        }
    }

    static void rim(PoseStack p, MultiBufferSource out, int light, Surface.Solid outer, Surface.Solid inner, int color) {
        VertexConsumer b = out.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));

        for (int i = 0; i < 64; i++) {
            for (int j = 0; j < 4; j++) {
                boolean inside = j >= 2;
                int index = (i + (j != 1 && j != 2 ? 0 : 1)) % 64;
                float[] edge = edge(inside ? inner : outer, index, 0.0F);
                vertex(b, p.last(), edge[0], edge[1], inside ? inner.z : outer.z, -edge[2] * 0.4F, -edge[3] * 0.4F, 0.9165F, color, light, 0.5F, 0.5F);
            }
        }
    }

    static void throat(PoseStack p, MultiBufferSource out, int light) {
        Surface.Solid outer = new Surface.Solid("frame", 548.0F, 129.0F, 248.0F, 216.0F, 70.0F, 1.0F, 8.0F, "#182324", "base", 0.0F);
        Surface.Solid opening = new Surface.Solid("opening", 566.0F, 154.0F, 212.0F, 174.0F, 66.0F, 1.0F, 2.0F, "#182324", "base", 0.0F);
        Surface.Solid glass = new Surface.Solid("glass", 578.0F, 174.0F, 192.0F, 144.0F, 65.0F, 1.0F, 3.0F, "#182324", "base", 0.0F);
        rim(p, out, light, outer, opening, 1581860);
        rim(p, out, light, opening, glass, 1120795);

        for (int yy : new int[]{233, 265}) {
            rim(
                p,
                out,
                light,
                new Surface.Solid("track", 838.0F, yy - 5, 48.0F, 14.0F, 68.0F, 1.0F, 0.0F, "", "", 0.0F),
                new Surface.Solid("track", 840.0F, yy - 3, 44.0F, 10.0F, 60.0F, 1.0F, 1.0F, "", "", 0.0F),
                14211267
            );
        }
    }

    static float bowl(float y) {
        return 60.0F - 13.0F * (float)Math.exp(-Math.pow((y - 204.0F) / 65.0F, 4.0)) - 13.0F * (float)Math.exp(-Math.pow((y - 555.0F) / 55.0F, 4.0));
    }

    static float left(float y) {
        float dy = y < 172.0F ? y - 172.0F : (y > 580.0F ? y - 580.0F : 0.0F);
        return 206.0F - (float)Math.sqrt(Math.max(0.0F, 900.0F - dy * dy));
    }

    static void cradle(PoseStack p, MultiBufferSource out, int light) {
        if (wiredModel) {
            rim(
                p,
                out,
                light,
                new Surface.Solid("wiredCradle", 160.0F, 136.0F, 114.0F, 480.0F, 68.0F, 1.0F, 5.0F, "", "", 0.0F),
                new Surface.Solid("wiredCradle", 166.0F, 142.0F, 102.0F, 468.0F, 53.0F, 1.0F, 8.0F, "", "", 0.0F),
                15197652
            );
            solid(new Surface.Solid("wiredCradleFloor", 166.0F, 142.0F, 102.0F, 468.0F, 52.7F, 0.3F, 8.0F, "#dedfce", "base", 0.0F), p, out, light);
        } else {
            VertexConsumer b = out.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));
            Surface.Solid outer = new Surface.Solid("cradle", 170.0F, 136.0F, 116.0F, 480.0F, 68.0F, 1.0F, 0.0F, "", "", 0.0F);
            Surface.Solid inner = new Surface.Solid("cradle", 176.0F, 142.0F, 104.0F, 468.0F, 60.0F, 1.0F, 30.0F, "", "", 0.0F);

            for (int i = 0; i < 64; i++) {
                for (int j = 0; j < 4; j++) {
                    boolean inside = j >= 2;
                    float[] at = edge(inside ? inner : outer, (i + (j != 1 && j != 2 ? 0 : 1)) % 64, 0.0F);
                    vertex(b, p.last(), at[0], at[1], inside ? bowl(at[1]) + 2.0F : 68.0F, -at[2] * 0.7F, -at[3] * 0.7F, 0.714F, 15197652, light, 0.5F, 0.5F);
                }
            }

            for (int row = 0; row < 78; row++) {
                for (int col = 0; col < 10; col++) {
                    for (int j = 0; j < 4; j++) {
                        float y = 142.0F + (row + (j != 1 && j != 2 ? 0 : 1)) * 6.0F;
                        float u = (col + (j >= 2 ? 1 : 0)) / 10.0F;
                        float l = left(y);
                        float x = l + (456.0F - 2.0F * l) * u;
                        float z = bowl(y) + 2.0F * (2.0F * u - 1.0F) * (2.0F * u - 1.0F);
                        float nx = -8.0F * (2.0F * u - 1.0F) / 104.0F;
                        float ny = -(bowl(y + 0.25F) - bowl(y - 0.25F)) * 2.0F;
                        float len = (float)Math.sqrt(1.0F + nx * nx + ny * ny);
                        vertex(b, p.last(), x, y, z, nx / len, ny / len, 1.0F / len, 14934480, light, 0.5F, 0.5F);
                    }
                }
            }
        }
    }

    static float joinWeight(Surface.Join j, float x, float y) {
        float dx = j.x1 - j.x0;
        float dy = j.y1 - j.y0;
        float t = Math.max(0.0F, Math.min(1.0F, ((x - j.x0) * dx + (y - j.y0) * dy) / (dx * dx + dy * dy)));
        float u = x - j.x0 - t * dx;
        float v = y - j.y0 - t * dy;
        return Math.max(0.0F, 1.0F - (float)Math.sqrt(u * u + v * v) / (j.width / 2.0F));
    }

    static float seamDrop(float x, float y) {
        float depth = 0.0F;

        for (Surface.Join j : casing.seams) {
            depth = Math.max(depth, j.depth * joinWeight(j, x, y));
        }

        return depth;
    }

    static int seamColor(float x, float y) {
        float strength = 0.0F;
        int color = 15855583;

        for (Surface.Join j : casing.seams) {
            float w = joinWeight(j, x, y);
            if (w >= 0.5F && j.depth * w > strength) {
                strength = j.depth * w;
                color = j.color;
            }
        }

        return color;
    }

    static float shellZ(float x, float y) {
        float[] a = casing.bounds;
        float fromEdge = Math.min(y - a[1], a[3] - y);
        float t = Math.max(0.0F, Math.min(1.0F, fromEdge / 8.0F));
        float z = casing.front - 4.0F * (1.0F - t) * (1.0F - t);
        float width = x < (a[0] + a[2]) / 2.0F ? casing.sideRoll[0] : casing.sideRoll[1];
        float edge = Math.min(x - a[0], a[2] - x);
        float u = Math.max(0.0F, Math.min(1.0F, 1.0F - edge / width));
        z -= casing.sideRoll[2] * (1.0F - (float)Math.sqrt(Math.max(0.0F, 1.0F - u * u))) * t;
        return z - seamDrop(x, y);
    }

    static float shellLeft(float y) {
        float[] a = casing.bounds;
        float r = casing.radius;
        float dy = y < a[1] + r ? y - a[1] - r : (y > a[3] - r ? y - a[3] + r : 0.0F);
        return a[0] + r - (float)Math.sqrt(Math.max(0.0F, r * r - dy * dy));
    }

    static void skinVertex(VertexConsumer b, PoseStack p, float x, float y, int light) {
        float nx = -(shellZ(x + 0.2F, y) - shellZ(x - 0.2F, y)) * 2.5F;
        float ny = -(shellZ(x, y + 0.2F) - shellZ(x, y - 0.2F)) * 2.5F;
        float n = (float)Math.sqrt(1.0F + nx * nx + ny * ny);
        vertex(b, p.last(), x, y, shellZ(x, y), nx / n, ny / n, 1.0F / n, seamColor(x, y), light, 0.5F, 0.5F);
    }

    static void shell(PoseStack p, MultiBufferSource out, int light) {
        VertexConsumer b = out.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));
        TreeSet<Float> xx = new TreeSet<>();
        TreeSet<Float> yy = new TreeSet<>();
        float[] a = casing.bounds;

        for (float x = a[0]; x < a[2]; x += 8.0F) {
            xx.add(x);
        }

        for (float y = a[1]; y < a[3]; y += 8.0F) {
            yy.add(y);
        }

        for (float x = 110.0F; x <= 154.0F; x += 2.0F) {
            xx.add(x);
        }

        for (float x = 891.0F; x <= 934.0F; x += 2.0F) {
            xx.add(x);
        }

        xx.add(a[2]);
        xx.add(517.35F);
        xx.add(518.0F);
        xx.add(518.65F);
        yy.add(a[3]);
        yy.add(110.0F);
        yy.add(646.0F);

        for (float y : new float[]{91.0F, 93.0F, 95.0F, 97.0F, 101.0F, 660.0F, 664.0F, 668.0F, 670.0F}) {
            yy.add(y);
        }

        for (float[] h : casing.holes) {
            xx.add(h[0]);
            xx.add(h[0] + h[2]);
            yy.add(h[1]);
            yy.add(h[1] + h[3]);
        }

        for (Surface.Join seam : casing.seams) {
            for (float factor : new float[]{-0.5F, -0.25F, 0.0F, 0.25F, 0.5F}) {
                float d = seam.width * factor;
                if (seam.x0 == seam.x1) {
                    xx.add(seam.x0 + d);
                } else {
                    yy.add(seam.y0 + d);
                }
            }
        }

        ArrayList<Float> xs = new ArrayList<>(xx);
        ArrayList<Float> ys = new ArrayList<>(yy);

        for (int row = 0; row < ys.size() - 1; row++) {
            float y0 = ys.get(row);
            float y1 = ys.get(row + 1);
            float l0 = shellLeft(y0);
            float l1 = shellLeft(y1);
            float r0 = a[0] + a[2] - l0;
            float r1 = a[0] + a[2] - l1;

            for (int col = 0; col < xs.size() - 1; col++) {
                float x0 = xs.get(col);
                float x1 = xs.get(col + 1);
                float cx = (x0 + x1) / 2.0F;
                float cy = (y0 + y1) / 2.0F;
                boolean hole = false;

                for (float[] h : casing.holes) {
                    if (cx > h[0] && cx < h[0] + h[2] && cy > h[1] && cy < h[1] + h[3]) {
                        hole = true;
                    }
                }

                if (!hole && !(x1 <= Math.min(l0, l1)) && !(x0 >= Math.max(r0, r1))) {
                    for (float[] v : new float[][]{
                        {Math.max(l0, Math.min(r0, x0)), y0},
                        {Math.max(l1, Math.min(r1, x0)), y1},
                        {Math.max(l1, Math.min(r1, x1)), y1},
                        {Math.max(l0, Math.min(r0, x1)), y0}
                    }) {
                        skinVertex(b, p, v[0], v[1], light);
                    }
                }
            }

            for (boolean right : new boolean[]{false, true}) {
                float x0 = right ? r0 : l0;
                float x1 = right ? r1 : l1;
                float nx = right ? 1.0F : -1.0F;
                float ny = (right ? -1 : 1) * (x1 - x0) / Math.max(0.001F, y1 - y0);
                float len = (float)Math.sqrt(1.0F + ny * ny);

                for (float[] v : new float[][]{{x0, y0, shellZ(x0, y0)}, {x1, y1, shellZ(x1, y1)}, {x1, y1, casing.back}, {x0, y0, casing.back}}) {
                    vertex(b, p.last(), v[0], v[1], v[2], nx / len, ny / len, 0.0F, 15855583, light, 0.5F, 0.5F);
                }
            }
        }

        for (boolean bottom : new boolean[]{false, true}) {
            float y = bottom ? a[3] : a[1];
            float l = shellLeft(y);
            float r = a[0] + a[2] - l;

            for (int i = 0; i < xs.size() - 1; i++) {
                float x0 = Math.max(l, Math.min(r, xs.get(i)));
                float x1 = Math.max(l, Math.min(r, xs.get(i + 1)));
                if (!(x1 <= x0)) {
                    for (float[] v : new float[][]{{x0, y, shellZ(x0, y)}, {x1, y, shellZ(x1, y)}, {x1, y, casing.back}, {x0, y, casing.back}}) {
                        vertex(b, p.last(), v[0], v[1], v[2], 0.0F, bottom ? 1.0F : -1.0F, 0.0F, 15855583, light, 0.5F, 0.5F);
                    }
                }
            }
        }
    }

    static void vents(PoseStack p, MultiBufferSource out, int light) {
        VertexConsumer b = out.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));

        for (Surface.Solid v : solids) {
            if (v.group.equals("underside")) {
                for (int i = 0; i < 20; i++) {
                    double a = i * Math.PI / 10.0;
                    double c = (i + 1) * Math.PI / 10.0;

                    for (float[] q : new float[][]{
                        {v.x + 2.5F, v.z},
                        {v.x + 2.5F + 2.5F * (float)Math.cos(a), v.z + 2.5F * (float)Math.sin(a)},
                        {v.x + 2.5F + 2.5F * (float)Math.cos(c), v.z + 2.5F * (float)Math.sin(c)},
                        {v.x + 2.5F, v.z}
                    }) {
                        vertex(b, p.last(), q[0], 672.04F, q[1], 0.0F, 1.0F, 0.0F, 5002313, light, 0.5F, 0.5F);
                    }
                }
            }
        }
    }

    static float bodyDepth(float z) {
        load();
        return mounting.frame + (z - mounting.back) * mounting.depth / (mounting.front - mounting.back);
    }

    static float handsetShift() {
        return bodyDepth(mounting.front) - mounting.front;
    }

    static void bodyPose(PoseStack p) {
        load();
        p.pushPose();
        p.translate(0.0F, 0.0F, mounting.frame);
        p.scale(1.0F, 1.0F, mounting.depth / (mounting.front - mounting.back));
        p.translate(0.0F, 0.0F, -mounting.back);
    }

    static String meshKey(boolean radio, boolean docked, boolean folded) {
        return "station/" + wiredModel + "/" + radio + "/" + docked + "/" + folded;
    }

    static void draw(PoseStack p, MultiBufferSource out, int light, boolean radio, boolean docked, boolean folded) {
        load();
        GeometryCache.draw(meshKey(radio, docked, folded), p, out, light, (pose, buffers) -> drawRaw(pose, buffers, 0, radio, docked, folded));
    }

    static void draw101N(PoseStack p, MultiBufferSource out, int light, boolean docked) {
        select(true);
        GeometryCache.draw("station/101n/" + docked, p, out, light, (pose, buffers) -> drawRaw(pose, buffers, 0, false, docked, false, true));
    }

    static void drawRaw(PoseStack p, MultiBufferSource out, int light, boolean radio, boolean docked, boolean folded) {
        drawRaw(p, out, light, radio, docked, folded, false);
    }

    static void drawRaw(PoseStack p, MultiBufferSource out, int light, boolean radio, boolean docked, boolean folded, boolean noAerial) {
        load();
        if (radio) {
            for (Surface.Solid s : solids) {
                if (s.group.equals("radio")) {
                    solid(s, p, out, light);
                }
            }

            rectangle(p, out, 0.0F, 0.0F, 1042.0F, 950.0F, 73.0F, Product.id("textures/radio_ink.png"), light);
        } else {
            for (Surface.Solid sx : solids) {
                if (sx.name.equals("mount_plate")) {
                    solid(sx, p, out, light);
                }
            }

            bodyPose(p);

            for (Surface.Solid sxx : solids) {
                if (!sxx.group.equals("radio")
                    && !sxx.group.equals("docked")
                    && (!sxx.group.equals("folded") || folded)
                    && (!sxx.group.equals("extended") || !folded)
                    && (!noAerial || !sxx.name.startsWith("black_aerial"))
                    && !sxx.name.equals("mount_plate")
                    && !sxx.name.equals("body_shell")
                    && !sxx.name.equals("crt_outer")
                    && !sxx.name.equals("cradle_void")
                    && !sxx.group.equals("underside")) {
                    solid(sxx, p, out, light);
                }
            }

            shell(p, out, light);
            throat(p, out, light);
            cradle(p, out, light);
            vents(p, out, light);
            rectangle(p, out, 0.0F, 0.0F, 1042.0F, 720.0F, 71.8F, texture("textures/base_ink.png"), light);
            p.popPose();
            if (docked) {
                p.pushPose();
                p.translate(0.0F, 0.0F, handsetShift());

                for (Surface.Solid sxxx : solids) {
                    if (sxxx.group.equals("docked")) {
                        solid(sxxx, p, out, light);
                    }
                }

                p.popPose();
            }
        }
    }

    public record Join(String name, float x0, float y0, float x1, float y1, float width, float depth, int color) {
    }

    public record Key(String name, String label, float x, float y, float w, float h, String group) {
    }

    record Model(Surface.Mount mount, Surface.Shell shell, List<Surface.Solid> solids, List<Surface.Key> buttons) {
    }

    public record Mount(float frame, float back, float front, float depth) {
    }

    public record Shell(float[] bounds, float back, float front, float radius, float[] sideRoll, Surface.Join[] seams, float[][] holes) {
    }

    public record Solid(String name, float x, float y, float w, float h, float z, float d, float r, String color, String group, float curve) {
    }
}
