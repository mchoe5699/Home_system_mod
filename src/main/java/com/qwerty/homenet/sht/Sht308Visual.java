package com.qwerty.homenet.sht;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.client.kvm.KvmCameraView;
import com.qwerty.homenet.intercom.CallState;
import com.qwerty.homenet.move.DeviceOffsets;
import com.qwerty.homenet.video.VideoFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

final class Sht308Visual {
    static final float W = 420.0F;
    static final float H = 400.0F;
    static final float S = 8.85E-4F;

    static void draw(PoseStack pose, MultiBufferSource out, int light, boolean docked) {
        draw(pose, out, light, docked, false);
    }

    static void draw(PoseStack pose, MultiBufferSource out, int light, boolean docked, boolean star) {
        String prefix = star ? "sht307" : "sht308";
        GeometryCache.draw(prefix + (docked ? "_docked" : "_open"), pose, out, light, (p, b) -> {
            Sht308Mesh.draw(prefix + "/body.meshz", p, b);
            if (docked) {
                Sht308Mesh.draw(prefix + "/handset.meshz", p, b);
            }
        });
    }

    static void lamp(PoseStack p, MultiBufferSource out, float cx, float cy, float z, int rgb) {
        lamp(p, out, cx, cy, z, rgb, 10.5F);
    }

    static void lamp(PoseStack p, MultiBufferSource out, float cx, float cy, float z, int rgb, float r) {
        VertexConsumer buffer = out.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));

        for (int i = 0; i < 32; i++) {
            double a = i * Math.PI / 16.0;
            double c = (i + 1) * Math.PI / 16.0;

            for (float[] v : new float[][]{
                {cx, cy}, {cx + (float)Math.cos(a) * r, cy + (float)Math.sin(a) * r}, {cx + (float)Math.cos(c) * r, cy + (float)Math.sin(c) * r}, {cx, cy}
            }) {
                Surface.vertex(buffer, p.last(), v[0], v[1], z, 0.0F, 0.0F, 1.0F, rgb, 15728880, 0.5F, 0.5F);
            }
        }
    }

    static void pose(Station308.Unit b, PoseStack p) {
        Vec3 offset = DeviceOffsets.get(b.getLevel(), b.getBlockPos());
        p.translate(offset.x, offset.y, offset.z);
        double bottom = 0.323;
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

        p.scale(8.85E-4F, 8.85E-4F, 8.85E-4F);
        p.translate(-210.0F, -300.0F, 0.0F);
    }

    static final class Renderer implements BlockEntityRenderer<Station308.Unit> {
        final KvmCameraView video = new KvmCameraView();

        @Override
        public int getViewDistance() {
            return 24;
        }

        public void render(Station308.Unit b, float tick, PoseStack p, MultiBufferSource out, int light, int overlay) {
            String key = (b.star ? "sht307" : "sht308") + (b.docked ? "_docked" : "_open");
            if (!DeviceVisibility.hidden(b, GeometryCache.bounds(key), q -> Sht308Visual.pose(b, q))) {
                p.pushPose();
                Sht308Visual.pose(b, p);
                Sht308Visual.draw(p, out, light, b.docked, b.star);
                int lamps = b.lights();
                if (b.star && (lamps & 32) != 0) {
                    Sht308Visual.lamp(p, out, 385.0F, 225.0F, 57.2F, 14501934);
                }

                if (!b.star && (lamps & 16) != 0 && b.getLevel().getGameTime() / 10L % 2L == 0L) {
                    Sht308Visual.lamp(p, out, 385.0F, 225.0F, 57.2F, 14959154);
                }

                if ((lamps & 64) != 0 && (b.getCallState() != CallState.RINGING || b.getLevel().getGameTime() / 10L % 2L == 0L)) {
                    Sht308Visual.lamp(p, out, 385.0F, 263.0F, 56.5F, 14959154);
                }

                if (!b.star && (lamps & 8) != 0) {
                    Sht308Visual.lamp(p, out, 385.0F, 301.0F, 54.8F, 14959154);
                }

                BlockPos camera = b.camera();
                if (camera != null) {
                    p.pushPose();
                    p.scale(16.0F, 16.0F, 16.0F);
                    this.video.drawWall(p, out, camera, 318.0F, 543.0F, 166.0F, 655.0F, 64.2F, b.brightness, b.contrast, VideoFormat.MONOCHROME);
                    p.popPose();
                }

                p.popPose();
            }
        }
    }
}
