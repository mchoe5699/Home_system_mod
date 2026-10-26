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
import net.minecraft.world.phys.Vec3;

final class Hi101Visual {
    static final float W = 216.0F;
    static final float H = 400.0F;
    static final float S = 8.85E-4F;

    static void draw(PoseStack pose, MultiBufferSource out, int light, boolean docked) {
        GeometryCache.draw(docked ? "hi101_docked" : "hi101_open", pose, out, light, (p, b) -> {
            Hi101Mesh.draw("body.meshz", p, b);
            if (docked) {
                Hi101Mesh.draw("handset.meshz", p, b);
            }
        });
    }

    static void pose(StationHi101.Unit b, PoseStack p) {
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
        p.translate(-108.0F, -300.0F, 0.0F);
    }

    static final class Renderer implements BlockEntityRenderer<StationHi101.Unit> {
        public void render(StationHi101.Unit b, float tick, PoseStack p, MultiBufferSource out, int light, int overlay) {
            if (!DeviceVisibility.hidden(b, GeometryCache.bounds(b.docked ? "hi101_docked" : "hi101_open"), q -> Hi101Visual.pose(b, q))) {
                p.pushPose();
                Hi101Visual.pose(b, p);
                Hi101Visual.draw(p, out, light, b.docked);
                if ((b.lights() & 8) != 0) {
                    VertexConsumer buffer = out.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));
                    float cx = 185.0F;
                    float cy = 321.0F;
                    float r = 2.2F;
                    float z = 62.6F;

                    for (int i = 0; i < 20; i++) {
                        double a = i * Math.PI / 10.0;
                        double c = (i + 1) * Math.PI / 10.0;

                        for (float[] v : new float[][]{
                            {cx, cy},
                            {cx + (float)Math.cos(a) * r, cy + (float)Math.sin(a) * r},
                            {cx + (float)Math.cos(c) * r, cy + (float)Math.sin(c) * r},
                            {cx, cy}
                        }) {
                            Surface.vertex(buffer, p.last(), v[0], v[1], z, 0.0F, 0.0F, 1.0F, 15279392, 15728880, 0.5F, 0.5F);
                        }
                    }
                }

                p.popPose();
            }
        }
    }
}
