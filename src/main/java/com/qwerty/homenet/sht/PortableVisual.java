package com.qwerty.homenet.sht;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import com.qwerty.homenet.move.DeviceOffsets;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

final class PortableVisual {
    static List<Surface.Solid> parts;
    static List<Surface.Solid> parts104;
    static List<Surface.Key> keys;
    static List<Surface.Key> keys104;

    static void load() {
        if (parts == null) {
            try {
                try (
                    BufferedReader r = Minecraft.getInstance().getResourceManager().openAsReader(Product.id("portable102/model.json"));
                    BufferedReader r104 = Minecraft.getInstance().getResourceManager().openAsReader(Product.id("portable104m/model.json"));
                ) {
                    JsonObject m = JsonParser.parseReader(r).getAsJsonObject();
                    JsonObject other = JsonParser.parseReader(r104).getAsJsonObject();
                    Gson g = new Gson();
                    parts = new ArrayList<>();
                    parts104 = new ArrayList<>();
                    keys = new ArrayList<>();
                    keys104 = new ArrayList<>();

                    for (JsonElement part : m.getAsJsonArray("parts")) {
                        parts.add((Surface.Solid)g.fromJson(part, Surface.Solid.class));
                    }

                    for (JsonElement part : other.getAsJsonArray("parts")) {
                        parts104.add((Surface.Solid)g.fromJson(part, Surface.Solid.class));
                    }

                    for (JsonElement k : m.getAsJsonArray("keys")) {
                        keys.add((Surface.Key)g.fromJson(k, Surface.Key.class));
                    }

                    for (JsonElement k : other.getAsJsonArray("keys")) {
                        keys104.add((Surface.Key)g.fromJson(k, Surface.Key.class));
                    }
                }
            } catch (Exception var11) {
                throw new IllegalStateException(var11);
            }
        }
    }

    static void handset(PoseStack p, MultiBufferSource out, int light, boolean active) {
        handset(p, out, light, active, false);
    }

    static void handset(PoseStack p, MultiBufferSource out, int light, boolean active, boolean intercom) {
        handset(p, out, light, active, intercom, false);
    }

    static void handset(PoseStack p, MultiBufferSource out, int light, boolean active, boolean intercom, boolean model104) {
        GeometryCache.draw(
            "portable/" + model104 + "/" + active + "/" + intercom, p, out, light, (pose, buffers) -> handsetRaw(pose, buffers, 0, active, intercom, model104)
        );
    }

    static void handsetRaw(PoseStack p, MultiBufferSource out, int light, boolean active, boolean intercom, boolean model104) {
        load();

        for (Surface.Solid s : model104 ? parts104 : parts) {
            Surface.solid(s, p, out, light);
        }

        Surface.rectangle(p, out, 0.0F, 0.0F, 240.0F, 950.0F, 73.8F, Product.id((model104 ? "portable104m" : "portable102") + "/textures/radio_ink.png"), light);
        if (active) {
            Surface.patch(p, out, intercom ? 148.0F : 76.0F, 474.0F, 8.0F, 3.0F, 75.0F, 10547066, 15728880);
        }
    }

    static void vertex(VertexConsumer b, Pose p, float x, float y, float z, int rgb, int light, float nx, float ny, float nz) {
        b.vertex(p.pose(), x, y, z)
            .color(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, 255)
            .uv(0.5F, 0.5F)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(light)
            .normal(p.normal(), nx, ny, nz)
            .endVertex();
    }

    static String standKey(boolean inserted, boolean charging, boolean active, boolean intercom) {
        return "stand/" + inserted + "/" + charging + "/" + active + "/" + intercom;
    }

    static void stand(PoseStack p, MultiBufferSource out, int light, boolean inserted, boolean charging, boolean active) {
        stand(p, out, light, inserted, charging, active, false);
    }

    static void stand(PoseStack p, MultiBufferSource out, int light, boolean inserted, boolean charging, boolean active, boolean intercom) {
        stand(p, out, light, inserted, charging, active, intercom, false);
    }

    static void stand(PoseStack p, MultiBufferSource out, int light, boolean inserted, boolean charging, boolean active, boolean intercom, boolean model104) {
        GeometryCache.draw(
            standKey(inserted, charging, active, intercom) + "/" + model104,
            p,
            out,
            light,
            (pose, buffers) -> standRaw(pose, buffers, 0, inserted, charging, active, intercom, model104)
        );
    }

    static void standRaw(PoseStack p, MultiBufferSource out, int light, boolean inserted, boolean charging, boolean active, boolean intercom, boolean model104) {
        VertexConsumer b = out.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));
        Pose pose = p.last();

        for (int i = 0; i < 64; i++) {
            float[] a = edge(i);
            float[] c = edge(i + 1);
            float ha = top(a[1]);
            float hc = top(c[1]);
            float nx = (a[0] + c[0]) / 2.0F - 200.0F;
            float nz = (a[1] + c[1]) / 2.0F - 200.0F;
            float len = (float)Math.sqrt(nx * nx + nz * nz);
            nx /= len;
            nz /= len;
            vertex(b, pose, a[0], 0.0F, a[1], 14606287, light, nx, 0.0F, nz);
            vertex(b, pose, c[0], 0.0F, c[1], 14606287, light, nx, 0.0F, nz);
            vertex(b, pose, c[0], hc, c[1], 15263703, light, nx, 0.0F, nz);
            vertex(b, pose, a[0], ha, a[1], 15263703, light, nx, 0.0F, nz);
        }

        deck(p, out, light);
        pocket(p, out, light, 165.0F, 270.0F, 75.0F, 48.0F);
        pocket(p, out, light, 305.0F, 105.0F, 43.0F, 47.0F);
        Surface.Solid led = new Surface.Solid("charging_led", 278.0F, 650.0F, 35.0F, 8.0F, 398.0F, 2.0F, 4.0F, charging ? "#83dd6e" : "#6e7866", "", 0.0F);
        Surface.solid(led, p, out, charging ? 15728880 : light);
        if (inserted) {
            p.pushPose();
            p.translate(63.75, 108.0, 280.0);
            p.mulPose(Axis.XP.rotationDegrees(-10.0F));
            p.scale(0.9F, 0.9F, 0.9F);
            p.translate(0.0F, 200.0F, 0.0F);
            handset(p, out, light, active, intercom, model104);
            p.popPose();
        }
    }

    static float[] edge(int n) {
        int corner = n % 64 / 16;
        double angle = (corner * 90 + n % 16 * 6) * Math.PI / 180.0;
        float x = corner != 0 && corner != 3 ? 55.0F : 345.0F;
        float z = corner < 2 ? 345.0F : 55.0F;
        return new float[]{x + 55.0F * (float)Math.cos(angle), z + 55.0F * (float)Math.sin(angle)};
    }

    static float top(float depth) {
        return 140.0F - 0.15F * depth;
    }

    static float outline(float z) {
        float d = z < 55.0F ? 55.0F - z : (z > 345.0F ? z - 345.0F : 0.0F);
        return 55.0F - (float)Math.sqrt(Math.max(0.0F, 3025.0F - d * d));
    }

    static void deck(PoseStack p, MultiBufferSource out, int light) {
        TreeSet<Float> rows = new TreeSet<>();

        for (int i = 0; i <= 100; i++) {
            rows.add(i * 4.0F);
        }

        for (float v : new float[]{58.0F, 152.0F, 222.0F, 318.0F}) {
            rows.add(v);
        }

        Float last = null;

        for (float z : rows) {
            if (last != null) {
                float mid = (last + z) / 2.0F;
                float[] hole = mid > 222.0F && mid < 318.0F
                    ? new float[]{165.0F, 270.0F, 75.0F, 48.0F}
                    : (mid > 58.0F && mid < 152.0F ? new float[]{305.0F, 105.0F, 43.0F, 47.0F} : null);
                if (hole == null) {
                    strip(p, out, light, last, z, outline(last), 400.0F - outline(last), outline(z), 400.0F - outline(z));
                } else {
                    float a = holeEdge(hole, last);
                    float c = holeEdge(hole, z);
                    strip(p, out, light, last, z, outline(last), hole[0] - a, outline(z), hole[0] - c);
                    strip(p, out, light, last, z, hole[0] + a, 400.0F - outline(last), hole[0] + c, 400.0F - outline(z));
                }
            }

            last = z;
        }
    }

    static float holeEdge(float[] h, float z) {
        return h[2] * (float)Math.sqrt(Math.max(0.0, 1.0 - Math.pow((z - h[1]) / h[3], 2.0)));
    }

    static void strip(PoseStack p, MultiBufferSource out, int light, float a, float c, float l0, float r0, float l1, float r1) {
        VertexConsumer b = out.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));
        Pose q = p.last();
        vertex(b, q, l0, top(a), a, 15855583, light, 0.0F, 1.0F, 0.15F);
        vertex(b, q, r0, top(a), a, 15855583, light, 0.0F, 1.0F, 0.15F);
        vertex(b, q, r1, top(c), c, 15855583, light, 0.0F, 1.0F, 0.15F);
        vertex(b, q, l1, top(c), c, 15855583, light, 0.0F, 1.0F, 0.15F);
    }

    static void pocket(PoseStack p, MultiBufferSource out, int light, float x, float z, float rx, float rz) {
        VertexConsumer b = out.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));
        Pose pose = p.last();

        for (int i = 0; i < 48; i++) {
            double a = i * Math.PI / 24.0;
            double c = (i + 1) * Math.PI / 24.0;

            for (int ring = 0; ring < 4; ring++) {
                for (int j = 0; j < 4; j++) {
                    float radius = 1.0F - (ring + (j != 1 && j != 2 ? 0 : 1)) / 4.0F;
                    double angle = j < 2 ? a : c;
                    float xx = x + rx * radius * (float)Math.cos(angle);
                    float zz = z + rz * radius * (float)Math.sin(angle);
                    vertex(b, pose, xx, top(zz) - 25.0F * (1.0F - radius * radius), zz, 11910056, light, 0.0F, 1.0F, 0.0F);
                }
            }
        }
    }

    static void dockPose(Charger.Dock d, PoseStack p) {
        Vec3 offset = DeviceOffsets.get(d.getLevel(), d.getBlockPos());
        p.translate(offset.x, offset.y, offset.z);
        p.translate(0.5, 0.0, 0.5);

        p.mulPose(Axis.YP.rotationDegrees(switch ((Direction)d.getBlockState().getValue(Charger.FACING)) {
            case NORTH -> 180.0F;
            case EAST -> 90.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        }));
        p.translate(-0.2, 0.0, -0.2);
        p.scale(0.001F, 0.001F, 0.001F);
    }

    static final class Renderer implements BlockEntityRenderer<Charger.Dock> {
        public void render(Charger.Dock d, float time, PoseStack p, MultiBufferSource out, int light, int overlay) {
            boolean inserted = !d.stack.isEmpty();
            boolean charging = inserted && d.stack.getOrCreateTag().getInt("battery") < 36000;
            boolean active = inserted && d.stack.getOrCreateTag().getBoolean("ringing");
            boolean intercom = inserted && d.stack.getOrCreateTag().getBoolean("intercom");
            boolean model104 = inserted && d.stack.is(Product.PORTABLE104M.get());
            if (!DeviceVisibility.hidden(
                d,
                GeometryCache.bounds(PortableVisual.standKey(inserted, charging, active, intercom) + "/" + model104),
                pose -> PortableVisual.dockPose(d, pose)
            )) {
                p.pushPose();
                PortableVisual.dockPose(d, p);
                PortableVisual.stand(p, out, light, inserted, charging, active, intercom, model104);
                p.popPose();
            }
        }
    }
}
