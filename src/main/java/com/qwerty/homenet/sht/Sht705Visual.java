package com.qwerty.homenet.sht;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.client.kvm.KvmCameraView;
import com.qwerty.homenet.move.DeviceOffsets;
import com.qwerty.homenet.video.VideoFormat;
import java.util.ArrayList;
import java.util.TreeSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

final class Sht705Visual {
    static final float W = 480.0F;
    static final float H = 354.0F;
    static final float S = 0.001F;
    static final int[] FRONT_X = new int[]{211, 244, 275, 308, 350};
    static final int LCD_X = 205;
    static final int LCD_Y = 67;
    static final int LCD_W = 154;
    static final int LCD_H = 116;
    private static final ResourceLocation FACE = Product.id("textures/block/sht_705_face.png");
    private static final ResourceLocation PANEL = Product.id("textures/block/sht_705_panel.png");
    private static final ResourceLocation COVER = Product.id("textures/block/sht_705_cover.png");
    private static final ResourceLocation LED_RING = Product.id("textures/block/sht_705_led_ring.png");
    private static final ResourceLocation BUTTON = Product.id("textures/block/sht_705_button.png");
    private static final float[] SIDE_PROFILE_Y = new float[]{0.0F, 20.0F, 84.0F, 164.0F, 246.0F, 308.0F, 354.0F};
    private static final float[] SIDE_PROFILE_Z = new float[]{17.0F, 17.0F, 15.0F, 21.0F, 15.0F, 17.0F, 17.0F};

    static float mx(float x) {
        if (x < 94.0F) {
            return x * 80.0F / 94.0F;
        } else if (x < 203.0F) {
            return 80.0F + (x - 94.0F) * 83.0F / 109.0F;
        } else if (x <= 367.0F) {
            return x - 40.0F;
        } else {
            return x < 466.0F ? 327.0F + (x - 367.0F) * 73.0F / 99.0F : 400.0F + (x - 466.0F) * 80.0F / 94.0F;
        }
    }

    private static Surface.Solid box(String name, float x, float y, float w, float h, float z, float depth, float radius, String color) {
        return new Surface.Solid(name, x, y, w, h, z, depth, radius, color, "body", 0.0F);
    }

    static void draw(PoseStack p, MultiBufferSource out, int ignored, boolean coverOpen) {
        GeometryCache.draw("sht705_cast_v19_" + coverOpen, p, out, 15728880, (pose, buffers) -> drawBody(pose, buffers, coverOpen));
    }

    private static void drawBody(PoseStack p, MultiBufferSource out, boolean coverOpen) {
        int light = 15728880;
        castShell(p, out, light);
        int[] i = FRONT_X;
        int x = i.length;

        for (int r = 0; r < x; r++) {
            float xx = i[r];
            float rx = xx == 350.0F ? 10.4F : 8.5F;
            Surface.solid(box("metal_button_bezel", mx(xx) - rx, 221.0F - rx, 2.0F * rx, 2.0F * rx, 27.6F, 1.0F, rx, "#707c83"), p, out, light);
            Surface.solid(
                box("metal_button", mx(xx) - rx * 0.72F, 221.0F - rx * 0.72F, rx * 1.44F, rx * 1.44F, 28.6F, 0.9F, rx * 0.72F, "#dce0de"), p, out, light
            );
            Surface.rectangle(p, out, mx(xx) - rx, 221.0F - rx, 2.0F * rx, 2.0F * rx, 29.8F, BUTTON, light);
        }

        flushPanel(p, out, light, coverOpen);
        if (coverOpen) {
            for (int ix = 0; ix < 7; ix++) {
                float xx = mx(224 + ix * 24);
                float r = 5.8F;
                Surface.solid(box("inner_button", xx - r, 323.0F - r, r * 2.0F, r * 2.0F, silverDepth(323.0F) + 0.42F, 0.35F, r, "#c7cccd"), p, out, light);
            }
        }

        Surface.solid(box("cover_hinge", mx(142.0F), 352.0F, mx(415.0F) - mx(142.0F), 2.0F, silverDepth(354.0F) - 0.25F, 0.65F, 0.8F, "#b9c1c4"), p, out, light);
        if (coverOpen) {
            p.pushPose();
            p.translate(0.0F, 346.0F, silverDepth(354.0F));
            p.mulPose(Axis.XP.rotationDegrees(78.0F));
            p.translate(0.0F, -346.0F, -silverDepth(354.0F));
            drawCover(p, out, light);
            p.popPose();
        } else {
            drawCover(p, out, light);
        }
    }

    private static float smooth(float t) {
        t = Math.max(0.0F, Math.min(1.0F, t));
        return t * t * (3.0F - 2.0F * t);
    }

    static float silverDepth(float y) {
        for (int i = 0; i < SIDE_PROFILE_Y.length - 1; i++) {
            if (y <= SIDE_PROFILE_Y[i + 1]) {
                float t = smooth((y - SIDE_PROFILE_Y[i]) / (SIDE_PROFILE_Y[i + 1] - SIDE_PROFILE_Y[i]));
                return SIDE_PROFILE_Z[i] + (SIDE_PROFILE_Z[i + 1] - SIDE_PROFILE_Z[i]) * t;
            }
        }

        return 17.0F;
    }

    static float silverDepth(float x, float y) {
        return silverDepth(y);
    }

    static float shellDepth(float x, float y) {
        float face = smooth((x - 94.0F) / 2.0F) * smooth((473.0F - x) / 2.0F) * smooth((284.0F - y) / 2.0F);
        float z = silverDepth(x, y) * (1.0F - face) + 26.5F * face;
        float glass = smooth((x - 124.0F) / 7.0F) * smooth((446.0F - x) / 7.0F) * smooth((y - 21.0F) / 7.0F) * smooth((256.0F - y) / 7.0F);
        z -= 0.5F * glass;
        float lcd = smooth((x - 203.0F) / 5.0F) * smooth((362.0F - x) / 5.0F) * smooth((y - 63.0F) / 5.0F) * smooth((187.0F - y) / 5.0F);
        return z - 0.7F * lcd;
    }

    private static void skinVertex(PoseStack p, VertexConsumer b, float x, float y, int light) {
        float z = shellDepth(x, y);
        float nx = -(shellDepth(Math.min(560.0F, x + 0.4F), y) - shellDepth(Math.max(0.0F, x - 0.4F), y));
        float ny = -(shellDepth(x, Math.min(354.0F, y + 0.4F)) - shellDepth(x, Math.max(0.0F, y - 0.4F)));
        float n = (float)Math.sqrt(nx * nx + ny * ny + 1.0F);
        Surface.vertex(b, p.last(), mx(x), y, z, nx / n, ny / n, 1.0F / n, 16777215, light, mx(x) / 480.0F, y / 354.0F);
    }

    private static void castShell(PoseStack p, MultiBufferSource out, int light) {
        VertexConsumer b = out.getBuffer(RenderType.entityCutoutNoCull(FACE));
        TreeSet<Float> xs = new TreeSet<>();
        TreeSet<Float> ys = new TreeSet<>();

        for (int x = 0; x <= 560; x += 7) {
            xs.add((float)x);
        }

        for (int y = 0; y <= 354; y += 7) {
            ys.add((float)y);
        }

        for (float x : new float[]{
            94.0F, 99.0F, 104.0F, 124.0F, 127.0F, 131.0F, 203.0F, 205.0F, 359.0F, 362.0F, 443.0F, 446.0F, 463.0F, 468.0F, 473.0F, 560.0F
        }) {
            xs.add(x);
        }

        for (float y : new float[]{0.0F, 21.0F, 24.0F, 28.0F, 63.0F, 67.0F, 183.0F, 187.0F, 246.0F, 253.0F, 256.0F, 275.0F, 284.0F, 354.0F}) {
            ys.add(y);
        }

        ArrayList<Float> xx = new ArrayList<>(xs);
        ArrayList<Float> yy = new ArrayList<>(ys);

        for (int row = 0; row < yy.size() - 1; row++) {
            for (int col = 0; col < xx.size() - 1; col++) {
                float x0 = xx.get(col);
                float x1 = xx.get(col + 1);
                float y0 = yy.get(row);
                float y1 = yy.get(row + 1);
                skinVertex(p, b, x0, y1, light);
                skinVertex(p, b, x1, y1, light);
                skinVertex(p, b, x1, y0, light);
                skinVertex(p, b, x0, y0, light);
            }
        }

        VertexConsumer solid = out.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));

        for (int i = 0; i < yy.size() - 1; i++) {
            for (float x : new float[]{0.0F, 560.0F}) {
                wallQuad(p, solid, x, yy.get(i), x, yy.get(i + 1), light);
            }
        }

        for (int i = 0; i < xx.size() - 1; i++) {
            for (float y : new float[]{0.0F, 354.0F}) {
                wallQuad(p, solid, xx.get(i), y, xx.get(i + 1), y, light);
            }
        }

        for (float[] v : new float[][]{{0.0F, 354.0F}, {560.0F, 354.0F}, {560.0F, 0.0F}, {0.0F, 0.0F}}) {
            Surface.vertex(solid, p.last(), mx(v[0]), v[1], 0.0F, 0.0F, 0.0F, -1.0F, 11976385, light, 0.5F, 0.5F);
        }
    }

    private static void wallQuad(PoseStack p, VertexConsumer b, float x0, float y0, float x1, float y1, int light) {
        for (float[] v : new float[][]{{x0, y0, 0.0F}, {x1, y1, 0.0F}, {x1, y1, shellDepth(x1, y1)}, {x0, y0, shellDepth(x0, y0)}}) {
            Surface.vertex(b, p.last(), mx(v[0]), v[1], v[2], 0.0F, 0.0F, 1.0F, 11976385, light, 0.5F, 0.5F);
        }
    }

    private static void flushPanel(PoseStack p, MultiBufferSource out, int light, boolean coverOpen) {
        for (int y = coverOpen ? 284 : 300; y < (coverOpen ? 354 : 314); y += 2) {
            float end = Math.min(coverOpen ? 354 : 314, y + 2);
            quad(p, out, PANEL, 142.0F, y, 415.0F, end, silverDepth(y) + 0.2F, silverDepth(end) + 0.2F, light);
        }
    }

    private static void drawCover(PoseStack p, MultiBufferSource out, int light) {
        for (int y = 284; y < 354; y += 2) {
            float end = Math.min(354, y + 2);
            float z0 = silverDepth(y) + 0.4F;
            float z1 = silverDepth(end) + 0.4F;
            if (y < 300) {
                quad(p, out, COVER, 142.0F, y, 415.0F, end, z0, z1, light);
            } else if (y < 314) {
                quad(p, out, COVER, 142.0F, y, 183.0F, end, z0, z1, light);
                quad(p, out, COVER, 387.0F, y, 415.0F, end, z0, z1, light);
            } else {
                quad(p, out, COVER, 142.0F, y, 415.0F, end, z0, z1, light);
            }
        }
    }

    private static void quad(PoseStack p, MultiBufferSource out, ResourceLocation image, float x0, float y0, float x1, float y1, float z0, float z1, int light) {
        VertexConsumer b = out.getBuffer(RenderType.entityCutoutNoCull(image));

        for (float[] v : new float[][]{{x0, y1, z1}, {x1, y1, z1}, {x1, y0, z0}, {x0, y0, z0}}) {
            Surface.vertex(b, p.last(), mx(v[0]), v[1], v[2], 0.0F, 0.0F, 1.0F, 16777215, light, mx(v[0]) / 480.0F, v[1] / 354.0F);
        }
    }

    private static void part(PoseStack p, MultiBufferSource out, ResourceLocation image, float x, float y, float width, float height, float z, int light) {
        VertexConsumer b = out.getBuffer(RenderType.entityCutoutNoCull(image));

        for (float[] v : new float[][]{
            {x, y + height, x / 480.0F, (y + height) / 354.0F},
            {x + width, y + height, (x + width) / 480.0F, (y + height) / 354.0F},
            {x + width, y, (x + width) / 480.0F, y / 354.0F},
            {x, y, x / 480.0F, y / 354.0F}
        }) {
            Surface.vertex(b, p.last(), v[0], v[1], z, 0.0F, 0.0F, 1.0F, 16777215, light, v[2], v[3]);
        }
    }

    static void litRing(PoseStack p, MultiBufferSource out, float x, float y, float radius, float z) {
        x = mx(x);
        float half = 13.0F * radius / 9.5F;
        VertexConsumer b = out.getBuffer(RenderType.entityTranslucent(LED_RING));

        for (float[] v : new float[][]{
            {x - half, y + half, 0.0F, 1.0F}, {x + half, y + half, 1.0F, 1.0F}, {x + half, y - half, 1.0F, 0.0F}, {x - half, y - half, 0.0F, 0.0F}
        }) {
            Surface.vertex(b, p.last(), v[0], v[1], z, 0.0F, 0.0F, 1.0F, 16777215, 15728880, v[2], v[3]);
        }
    }

    static void pose(Station705.Unit unit, PoseStack p) {
        Vec3 shift = DeviceOffsets.get(unit.getLevel(), unit.getBlockPos());
        p.translate(shift.x, shift.y, shift.z);
        double bottom = 0.323;
        switch ((Direction)unit.getBlockState().getValue(WallMountedBlock.FACING)) {
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
        p.translate(-240.0F, -346.0F, 0.0F);
    }

    static void video(PoseStack p, MultiBufferSource out, int[] pixels, int width, int height) {
        if (pixels != null) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    Surface.patch(
                        p,
                        out,
                        mx(205.0F) + (float)(x * 154) / width,
                        67.0F + (float)(y * 116) / height,
                        154.0F / width + 0.06F,
                        116.0F / height + 0.06F,
                        25.6F,
                        pixels[y * width + x] & 16777215,
                        15728880
                    );
                }
            }
        }
    }

    static final class Renderer implements BlockEntityRenderer<Station705.Unit> {
        final KvmCameraView legacy = new KvmCameraView();

        @Override
        public int getViewDistance() {
            return 24;
        }

        public void render(Station705.Unit unit, float tick, PoseStack pose, MultiBufferSource out, int light, int overlay) {
            pose.pushPose();
            Sht705Visual.pose(unit, pose);
            Sht705Visual.draw(pose, out, light, unit.coverOpen);
            int[] frontBits = new int[]{1, 2, 2, 4, 8};
            int state = unit.frontLamps();

            for (int i = 0; i < Sht705Visual.FRONT_X.length; i++) {
                if ((state & frontBits[i]) != 0) {
                    Sht705Visual.litRing(pose, out, Sht705Visual.FRONT_X[i], 221.0F, i == 4 ? 12.0F : 10.0F, 30.05F);
                }
            }

            int[] bits = new int[]{256, 512, 1024, 1};
            int[] colors = new int[]{15025215, 15055677, 6667370, 15025215};
            int lamps = unit.lights();

            for (int ix = 0; ix < 4; ix++) {
                if ((lamps & bits[ix]) != 0 && (ix == 3 || unit.getLevel().getGameTime() % 20L < 10L)) {
                    Sht305Visual.lamp(pose, out, Sht705Visual.mx(405.0F), 78 + ix * 22, 27.3F, colors[ix], 3.1F);
                }
            }

            int inside = unit.innerLamps();

            for (int ixx = 0; ixx < 7; ixx++) {
                if ((inside & 1 << ixx) != 0) {
                    Sht305Visual.lamp(
                        pose, out, Sht705Visual.mx(224 + ixx * 24), 306.0F, Sht705Visual.silverDepth(306.0F) + 0.5F, ixx == 6 ? 15093833 : 7712482, 2.2F
                    );
                }
            }

            BlockPos source = unit.camera();
            int[] saved = unit.recordedFrame();
            if (saved != null) {
                Sht705Visual.video(pose, out, saved, 20, 15);
            } else if (source != null && SamsungVideoView.isColorSource(unit.getLevel(), source)) {
                Sht705Visual.video(pose, out, SamsungVideoView.frame(source), 48, 36);
            } else if (source != null) {
                pose.pushPose();
                pose.scale(16.0F, 16.0F, 16.0F);
                this.legacy
                    .drawWall(
                        pose,
                        out,
                        source,
                        Sht705Visual.mx(359.0F),
                        517.0F,
                        Sht705Visual.mx(205.0F),
                        633.0F,
                        25.6F,
                        unit.brightness,
                        unit.contrast,
                        VideoDisplays.sourceFormat(unit.getLevel(), source, VideoFormat.COLOR)
                    );
                pose.popPose();
            }

            pose.popPose();
        }
    }
}
