package com.qwerty.homenet.sht;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.move.DeviceOffsets;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

final class Ht512Visual {
    static final int[] SEGMENTS = new int[]{63, 6, 91, 79, 102, 109, 125, 7, 127, 111};
    static final float[][] STROKES = new float[][]{
        {3.0F, 0.0F, 12.0F, 2.0F},
        {14.0F, 2.0F, 2.0F, 9.0F},
        {14.0F, 13.0F, 2.0F, 9.0F},
        {3.0F, 22.0F, 12.0F, 2.0F},
        {1.0F, 13.0F, 2.0F, 9.0F},
        {1.0F, 2.0F, 2.0F, 9.0F},
        {3.0F, 11.0F, 12.0F, 2.0F}
    };
    static final float W = 464.0F;
    static final float H = 610.0F;
    static final float SCALE = 9.5628423E-4F;
    static final ResourceLocation FRONT = Product.id("ht512/front.png");
    static final ResourceLocation INK = Product.id("ht512/key_ink.png");
    static Ht512Visual.Panel panel;
    static Ht512Visual.KeyStyle keyStyle;
    static List<Ht512Visual.Key> keys;

    static String digits(Ht512.Unit b) {
        return b == null ? "----" : b.segmentText();
    }

    static void segments(GuiGraphics g, String value) {
        segments(g, value, 281, 262);
    }

    static void segments(GuiGraphics g, String value, int left, int top) {
        for (int digit = 0; digit < 4; digit++) {
            int index = value.length() - 4 + digit;
            char c = index >= 0 ? value.charAt(index) : 32;
            int mask = c >= '0' && c <= '9' ? SEGMENTS[c - '0'] : (c == '-' ? 64 : 0);

            for (int bit = 0; bit < 7; bit++) {
                float[] s = STROKES[bit];
                int color = (mask & 1 << bit) != 0 ? -1358494 : -10672585;
                g.fill(left + digit * 19 + (int)s[0], top + (int)s[1], left + digit * 19 + (int)(s[0] + s[2]), top + (int)(s[1] + s[3]), color);
            }
        }
    }

    static void segments(PoseStack p, MultiBufferSource out, String value) {
        segments(p, out, value, 281.0F, 262.0F, 16.8F);
    }

    static void segments(PoseStack p, MultiBufferSource out, String value, float left, float top, float z) {
        ResourceLocation white = Product.id("textures/solid.png");

        for (int digit = 0; digit < 4; digit++) {
            int index = value.length() - 4 + digit;
            char c = index >= 0 ? value.charAt(index) : 32;
            int mask = c >= '0' && c <= '9' ? SEGMENTS[c - '0'] : (c == '-' ? 64 : 0);

            for (int bit = 0; bit < 7; bit++) {
                float[] s = STROKES[bit];
                int color = (mask & 1 << bit) != 0 ? -1358494 : -10672585;
                float x = left + digit * 19 + s[0];
                float y = top + s[1];
                face(p, out, 15728880, new float[][]{{x, y + s[3], z}, {x + s[2], y + s[3], z}, {x + s[2], y, z}, {x, y, z}}, white, color);
            }
        }
    }

    static void load() {
        if (keys == null) {
            try {
                try (BufferedReader r = Minecraft.getInstance().getResourceManager().openAsReader(Product.id("ht512/model.json"))) {
                    JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
                    ArrayList<Ht512Visual.Key> list = new ArrayList<>();

                    for (JsonElement e : root.getAsJsonArray("keys")) {
                        list.add((Ht512Visual.Key)new Gson().fromJson(e, Ht512Visual.Key.class));
                    }

                    panel = (Ht512Visual.Panel)new Gson().fromJson(root.get("panel"), Ht512Visual.Panel.class);
                    keyStyle = (Ht512Visual.KeyStyle)new Gson().fromJson(root.get("keyStyle"), Ht512Visual.KeyStyle.class);
                    keys = List.copyOf(list);
                }
            } catch (Exception var7) {
                throw new IllegalStateException("HT-512 model", var7);
            }
        }
    }

    static void vertex(VertexConsumer b, Pose p, float x, float y, float z, float nx, float ny, float nz, int c, int light, float u, float v) {
        Surface.vertex(b, p, x, y, z, nx, ny, nz, c, light, u, v);
    }

    static void face(PoseStack p, MultiBufferSource out, int light, float[][] points, ResourceLocation texture, int color) {
        VertexConsumer b = out.getBuffer(RenderType.entityCutoutNoCull(texture));
        float[] a = points[0];
        float[] c = points[1];
        float[] d = points[2];
        float ux = c[0] - a[0];
        float uy = c[1] - a[1];
        float uz = c[2] - a[2];
        float vx = d[0] - a[0];
        float vy = d[1] - a[1];
        float vz = d[2] - a[2];
        float nx = uy * vz - uz * vy;
        float ny = uz * vx - ux * vz;
        float nz = ux * vy - uy * vx;
        float len = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len == 0.0F) {
            ny = 0.0F;
            nx = 0.0F;
            nz = 1.0F;
            len = 1.0F;
        }

        if (nz < 0.0F) {
            nx = -nx;
            ny = -ny;
            nz = -nz;
        }

        for (float[] q : points) {
            vertex(
                b,
                p.last(),
                q[0],
                q[1],
                q[2],
                nx / len,
                ny / len,
                nz / len,
                color,
                light,
                texture.equals(Product.id("textures/solid.png")) ? 0.5F : q[0] / 464.0F,
                texture.equals(Product.id("textures/solid.png")) ? 0.5F : q[1] / 610.0F
            );
        }
    }

    static void rect(PoseStack p, MultiBufferSource out, int light, float x, float y, float w, float h, float z, ResourceLocation tex) {
        face(p, out, light, new float[][]{{x, y + h, z}, {x + w, y + h, z}, {x + w, y, z}, {x, y, z}}, tex, 16777215);
    }

    static void solid(
        PoseStack p,
        MultiBufferSource o,
        int light,
        String name,
        float x,
        float y,
        float w,
        float h,
        float z,
        float depth,
        float radius,
        int color,
        float curve
    ) {
        Surface.solid(new Surface.Solid(name, x, y, w, h, z, depth, radius, String.format("#%06x", color), "ht512", curve), p, o, light);
    }

    static float[] keyEdge(float x, float y, float w, float h, float r, int i, float inset) {
        int corner = i % 16 / 4;
        double a = (corner * 90 + i % 4 * 30) * Math.PI / 180.0;
        float cx = x + (corner != 0 && corner != 3 ? r : w - r);
        float cy = y + (corner < 2 ? h - r : r);
        float nx = (float)Math.cos(a);
        float ny = (float)Math.sin(a);
        return new float[]{cx + Math.max(0.0F, r - inset) * nx, cy + Math.max(0.0F, r - inset) * ny, nx, ny};
    }

    static void keySolid(
        PoseStack p, MultiBufferSource o, int light, float x, float y, float w, float h, float z, float depth, float radius, int color, float curve
    ) {
        VertexConsumer b = o.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));
        float bevel = Math.min(0.5F, depth * 0.35F);
        float front = z + depth;

        for (int i = 0; i < 16; i++) {
            for (int ring = 0; ring < 2; ring++) {
                for (int j = 0; j < 4; j++) {
                    boolean upper = j == 1 || j == 2;
                    int profile = ring + (upper ? 1 : 0);
                    float[] at = keyEdge(x, y, w, h, radius, i + (j >= 2 ? 1 : 0), profile == 2 ? bevel : 0.0F);
                    float zz = profile == 0 ? z : (profile == 1 ? front - bevel : front);
                    float nx = at[2];
                    float ny = at[3];
                    float nz = profile == 2 ? 1.0F : 0.0F;
                    float len = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
                    vertex(b, p.last(), at[0], at[1], zz, nx / len, ny / len, nz / len, color, light, 0.5F, 0.5F);
                }
            }

            float[] a = keyEdge(x, y, w, h, radius, i, bevel);
            float[] c = keyEdge(x, y, w, h, radius, i + 1, bevel);
            float cx = x + w / 2.0F;
            float cy = y + h / 2.0F;
            int rings = curve > 0.0F ? 3 : 1;

            for (int ring = 0; ring < rings; ring++) {
                for (int j = 0; j < 4; j++) {
                    float radial = 1.0F - (float)(ring + (j != 1 && j != 2 ? 0 : 1)) / rings;
                    float[] at = j < 2 ? a : c;
                    float vx = cx + (at[0] - cx) * radial;
                    float vy = cy + (at[1] - cy) * radial;
                    float nx = curve * 2.0F * (vx - cx) / (w * w / 4.0F);
                    float ny = curve * 2.0F * (vy - cy) / (h * h / 4.0F);
                    float len = (float)Math.sqrt(1.0F + nx * nx + ny * ny);
                    vertex(b, p.last(), vx, vy, front + curve * (1.0F - radial * radial), nx / len, ny / len, 1.0F / len, color, light, 0.5F, 0.5F);
                }
            }
        }
    }

    static void trim(PoseStack p, MultiBufferSource o, int light, float inset0, float z0, float inset1, float z1, ResourceLocation tex, int color) {
        float[][] a = new float[][]{{inset0, inset0, z0}, {464.0F - inset0, inset0, z0}, {464.0F - inset0, 610.0F - inset0, z0}, {inset0, 610.0F - inset0, z0}};
        float[][] b = new float[][]{{inset1, inset1, z1}, {464.0F - inset1, inset1, z1}, {464.0F - inset1, 610.0F - inset1, z1}, {inset1, 610.0F - inset1, z1}};

        for (int i = 0; i < 4; i++) {
            face(p, o, light, new float[][]{a[i], a[(i + 1) % 4], b[(i + 1) % 4], b[i]}, tex, color);
        }
    }

    static boolean cut(float x, float y) {
        return x >= panel.x && x <= panel.x + panel.w && y >= panel.y && y <= panel.y + panel.h
            || x >= 107.0F && x <= 198.8F && y >= 330.0F && y <= 442.2F
            || x >= 112.0F && x <= 194.0F && y >= 462.0F && y <= 538.0F;
    }

    static void mesh(PoseStack p, MultiBufferSource o) {
        int l = 0;
        load();
        ResourceLocation white = Product.id("textures/solid.png");
        solid(p, o, l, "ht_back", 0.0F, 0.0F, 464.0F, 610.0F, 0.0F, 4.0F, 1.0F, 9407860, 0.0F);
        trim(p, o, l, 0.0F, 4.0F, 1.5F, 7.0F, white, 12171165);
        trim(p, o, l, 1.5F, 7.0F, 17.0F, 18.0F, FRONT, 16777215);
        trim(p, o, l, 17.0F, 18.0F, 18.0F, 15.0F, white, 5594186);
        float[] xs = new float[]{18.0F, panel.x, 107.0F, 112.0F, 194.0F, 198.8F, panel.x + panel.w, 446.0F};
        float[] ys = new float[]{18.0F, 210.0F, 306.0F, 330.0F, 442.2F, 462.0F, 538.0F, 592.0F};

        for (int x = 0; x < xs.length - 1; x++) {
            for (int y = 0; y < ys.length - 1; y++) {
                if (!cut((xs[x] + xs[x + 1]) / 2.0F, (ys[y] + ys[y + 1]) / 2.0F)) {
                    rect(p, o, l, xs[x], ys[y], xs[x + 1] - xs[x], ys[y + 1] - ys[y], 15.0F, FRONT);
                }
            }
        }

        solid(p, o, l, "display_bezel", panel.x, panel.y, panel.w, panel.h, 12.0F, 4.0F, 1.0F, 1120533, 0.0F);
        rect(p, o, l, panel.x + 2.0F, panel.y + 2.0F, panel.w - 4.0F, panel.h - 4.0F, 16.05F, FRONT);
        float panelShift = panel.x - 78.0F;
        Surface.rim(
            p,
            o,
            l,
            new Surface.Solid("lens", 157.0F + panelShift, 255.0F, 23.0F, 23.0F, 16.5F, 0.0F, 11.5F, "", "", 0.0F),
            new Surface.Solid("lens", 162.0F + panelShift, 260.0F, 13.0F, 13.0F, 16.1F, 0.0F, 6.5F, "", "", 0.0F),
            4476736
        );
        rect(p, o, l, 162.0F + panelShift, 260.0F, 13.0F, 13.0F, 16.15F, FRONT);

        for (int row = 0; row < 11; row++) {
            for (int col = 0; col < 9; col++) {
                float cx = 112.1F + col * 10.2F;
                float cy = 335.1F + row * 10.2F;

                for (int i = 0; i < 16; i++) {
                    double a = i * Math.PI / 8.0;
                    double b = (i + 1) * Math.PI / 8.0;
                    float ax = (float)Math.cos(a);
                    float ay = (float)Math.sin(a);
                    float bx = (float)Math.cos(b);
                    float by = (float)Math.sin(b);
                    float ar = 5.1F / Math.max(Math.abs(ax), Math.abs(ay));
                    float br = 5.1F / Math.max(Math.abs(bx), Math.abs(by));
                    face(
                        p,
                        o,
                        l,
                        new float[][]{
                            {cx + ax * ar, cy + ay * ar, 15.0F},
                            {cx + bx * br, cy + by * br, 15.0F},
                            {cx + bx * 2.6F, cy + by * 2.6F, 15.0F},
                            {cx + ax * 2.6F, cy + ay * 2.6F, 15.0F}
                        },
                        FRONT,
                        16777215
                    );
                    face(
                        p,
                        o,
                        l,
                        new float[][]{
                            {cx + ax * 2.6F, cy + ay * 2.6F, 15.0F},
                            {cx + bx * 2.6F, cy + by * 2.6F, 15.0F},
                            {cx + bx * 2.4F, cy + by * 2.4F, 12.0F},
                            {cx + ax * 2.4F, cy + ay * 2.4F, 12.0F}
                        },
                        white,
                        5462339
                    );
                    face(
                        p,
                        o,
                        l,
                        new float[][]{{cx, cy, 11.9F}, {cx + ax * 2.4F, cy + ay * 2.4F, 11.9F}, {cx + bx * 2.4F, cy + by * 2.4F, 11.9F}, {cx, cy, 11.9F}},
                        white,
                        1054225
                    );
                }
            }
        }

        for (float[] q : new float[][]{
            {112.0F, 462.0F, 82.0F, 33.0F}, {112.0F, 500.0F, 82.0F, 38.0F}, {112.0F, 495.0F, 12.0F, 5.0F}, {182.0F, 495.0F, 12.0F, 5.0F}
        }) {
            rect(p, o, l, q[0], q[1], q[2], q[3], 15.3F, FRONT);
        }

        solid(p, o, l, "card_slot", 124.0F, 495.0F, 58.0F, 5.0F, 8.0F, 1.0F, 0.0F, 1515542, 0.0F);
        face(p, o, l, new float[][]{{124.0F, 495.0F, 15.3F}, {182.0F, 495.0F, 15.3F}, {182.0F, 496.0F, 9.0F}, {124.0F, 496.0F, 9.0F}}, white, 8291178);
        Ht512Visual.KeyStyle style = keyStyle;
        float ri = style.rimInset;
        float fi = style.faceInset;
        float li = style.labelInset;

        for (Ht512Visual.Key k : keys) {
            keySolid(p, o, l, k.x, k.y, k.w, k.h, 15.0F, 1.1F, style.socketRadius, Integer.parseInt(style.socketColor.substring(1), 16), 0.0F);
            keySolid(
                p,
                o,
                l,
                k.x + ri,
                k.y + ri,
                k.w - 2.0F * ri,
                k.h - 2.0F * ri,
                16.1F,
                1.0F,
                style.rimRadius,
                Integer.parseInt(style.rimColor.substring(1), 16),
                0.0F
            );
            keySolid(
                p,
                o,
                l,
                k.x + fi,
                k.y + fi,
                k.w - 2.0F * fi,
                k.h - 2.0F * fi,
                17.1F,
                2.3F,
                style.faceRadius,
                Integer.parseInt(style.faceColor.substring(1), 16),
                0.25F
            );
            rect(p, o, l, k.x + li, k.y + li, k.w - 2.0F * li, k.h - 2.0F * li, 19.7F, INK);
        }
    }

    static void draw(PoseStack p, MultiBufferSource o, int light) {
        GeometryCache.draw("ht512", p, o, light, Ht512Visual::mesh);
    }

    static void pose(Ht512.Unit b, PoseStack p) {
        Vec3 offset = DeviceOffsets.get(b.getLevel(), b.getBlockPos());
        p.translate(offset.x, offset.y, offset.z);
        double bottom = 0.20833331249999998;
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

        p.scale(9.5628423E-4F, 9.5628423E-4F, 9.5628423E-4F);
        p.translate(-232.0F, -90.0F, 0.0F);
    }

    static void lamp(PoseStack p, MultiBufferSource out, float x, float y, float z) {
        face(
            p, out, 15728880, new float[][]{{x, y + 5.0F, z}, {x + 5.0F, y + 5.0F, z}, {x + 5.0F, y, z}, {x, y, z}}, Product.id("textures/solid.png"), 16734022
        );
    }

    static void open(BlockPos at) {
        Minecraft.getInstance().setScreen(new Ht512Visual.ModelScreen(at));
    }

    record Key(String label, float x, float y, float w, float h) {
    }

    record KeyStyle(
        float socketRadius,
        float rimInset,
        float rimRadius,
        float faceInset,
        float faceRadius,
        float labelInset,
        String socketColor,
        String rimColor,
        String faceColor
    ) {
    }

    static final class ModelScreen extends Screen {
        final BlockPos at;

        ModelScreen() {
            this(BlockPos.ZERO);
        }

        ModelScreen(BlockPos at) {
            super(Component.literal("SAMSUNG HT-512"));
            this.at = at;
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        @Override
        public void render(GuiGraphics g, int mx, int my, float dt) {
            this.renderBackground(g);
            g.drawString(this.font, this.title, 12, 8, -1118482);
            ClientLevel level = Minecraft.getInstance().level;
            Ht512.Unit u = level != null && level.getBlockEntity(this.at) instanceof Ht512.Unit phone ? phone : null;
            String status = u != null ? u.display() : "대기";
            g.drawString(this.font, "BUS · " + status, 12, 27, -1843272);
            float s = Math.min((this.width - 32) / 464.0F, (this.height - 68) / 610.0F);
            g.pose().pushPose();
            g.pose().translate((this.width - 464.0F * s) / 2.0F, 52.0F, 0.0F);
            g.pose().scale(s, s, 1.0F);
            g.blit(Ht512Visual.FRONT, 0, 0, 464, 610, 0.0F, 0.0F, 464, 610, 464, 610);
            Ht512Visual.segments(g, Ht512Visual.digits(u));
            if (u != null && u.callLamp) {
                g.fill(290, 244, 295, 249, -43194);
            }

            if (u != null && u.doorTicks > 0) {
                g.fill(339, 244, 344, 249, -43194);
            }

            g.pose().popPose();
            super.render(g, mx, my, dt);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (button != 0) {
                return super.mouseClicked(mx, my, button);
            } else {
                Ht512Visual.load();
                float s = Math.min((this.width - 32) / 464.0F, (this.height - 68) / 610.0F);
                float x = (float)((mx - (this.width - 464.0F * s) / 2.0F) / s);
                float y = (float)((my - 52.0) / s);

                for (Ht512Visual.Key key : Ht512Visual.keys) {
                    if (x >= key.x() - 2.0F && x <= key.x() + key.w() + 2.0F && y >= key.y() - 2.0F && y <= key.y() + key.h() + 2.0F) {
                        Wire.lobbyKey(this.at, key.label());
                        return true;
                    }
                }

                return super.mouseClicked(mx, my, button);
            }
        }

        @Override
        public boolean keyPressed(int key, int scan, int mods) {
            String value = switch (key) {
                case 35, 259 -> "#";
                case 42, 332 -> "*";
                case 48, 320 -> "0";
                case 49, 321 -> "1";
                case 50, 322 -> "2";
                case 51, 323 -> "3";
                case 52, 324 -> "4";
                case 53, 325 -> "5";
                case 54, 326 -> "6";
                case 55, 327 -> "7";
                case 56, 328 -> "8";
                case 57, 329 -> "9";
                case 257, 335 -> "호출";
                default -> "";
            };
            if (!value.isEmpty()) {
                Wire.lobbyKey(this.at, value);
                return true;
            } else {
                return super.keyPressed(key, scan, mods);
            }
        }
    }

    record Panel(float x, float y, float w, float h) {
    }

    static class Renderer implements BlockEntityRenderer<Ht512.Unit> {
        public void render(Ht512.Unit b, float time, PoseStack p, MultiBufferSource out, int light, int overlay) {
            if (!DeviceVisibility.hidden(b, GeometryCache.bounds("ht512"), q -> Ht512Visual.pose(b, q))) {
                p.pushPose();
                Ht512Visual.pose(b, p);
                Ht512Visual.draw(p, out, light);
                if (b.callLamp) {
                    Ht512Visual.lamp(p, out, 290.0F, 244.0F, 16.7F);
                }

                if (b.doorTicks > 0) {
                    Ht512Visual.lamp(p, out, 339.0F, 244.0F, 16.7F);
                }

                Ht512Visual.segments(p, out, Ht512Visual.digits(b));
                p.popPose();
            }
        }
    }
}
