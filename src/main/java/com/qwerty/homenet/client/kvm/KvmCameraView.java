package com.qwerty.homenet.client.kvm;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.qwerty.homenet.HomeNet;
import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.blockentity.DoorStationBlockEntity;
import com.qwerty.homenet.blockentity.Klp203rfBlockEntity;
import com.qwerty.homenet.video.VideoFormat;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class KvmCameraView {
    private static final int W = 48;
    private static final int H = 36;
    private final int[] pixels = new int[1728];
    private long nextFrame;
    private BlockPos lastCamera;
    private boolean lastColor;
    private boolean lastIlluminated;

    private boolean update(BlockPos camera, VideoFormat displayFormat) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null && mc.level.hasChunkAt(camera)) {
            if (mc.level.getBlockEntity(camera) instanceof DoorStationBlockEntity door && door.isDoorPhone()) {
                return false;
            } else {
                BlockState state = mc.level.getBlockState(camera);
                if (!state.hasProperty(WallMountedBlock.FACING)) {
                    return false;
                } else {
                    VideoFormat sourceFormat = mc.level.getBlockEntity(camera) instanceof Klp203rfBlockEntity panel && panel.model().colorVideo()
                        ? VideoFormat.COLOR
                        : VideoFormat.MONOCHROME;
                    boolean color = VideoFormat.colorAllowed(sourceFormat, displayFormat);
                    boolean illuminated = mc.level.getBlockEntity(camera) instanceof Klp203rfBlockEntity panelx && panelx.visitorLight();
                    long now = System.currentTimeMillis();
                    if (now >= this.nextFrame || !camera.equals(this.lastCamera) || color != this.lastColor || illuminated != this.lastIlluminated) {
                        this.lastCamera = camera;
                        this.lastColor = color;
                        this.lastIlluminated = illuminated;
                        this.nextFrame = now + 200L;
                        Direction facing = state.getValue(WallMountedBlock.FACING);
                        Vec3 forward = Vec3.atLowerCornerOf(facing.getNormal());
                        Vec3 right = forward.cross(new Vec3(0.0, 1.0, 0.0));
                        Vec3 origin = Vec3.atCenterOf(camera).add(forward.scale(0.55));
                        List<Entity> entities = mc.level.getEntities(mc.player, new AABB(origin, origin).inflate(16.0), e -> e.isAlive() && !e.isInvisible());
                        if (mc.player.position().distanceToSqr(origin) < 256.0) {
                            entities.add(mc.player);
                        }

                        for (int py = 0; py < 36; py++) {
                            for (int px = 0; px < 48; px++) {
                                Vec3 ray = forward.add(right.scale(((px + 0.5) / 48.0 * 2.0 - 1.0) * 1.11))
                                    .add(0.0, (1.0 - (py + 0.5) / 36.0 * 2.0) * 0.727, 0.0)
                                    .normalize();
                                Vec3 end = origin.add(ray.scale(16.0));
                                BlockHitResult hit = mc.level.clip(new ClipContext(origin, end, Block.OUTLINE, Fluid.NONE, mc.player));
                                double distance = hit.getType() == Type.MISS ? 16.0 : hit.getLocation().distanceTo(origin);
                                int rgb = ray.y > 0.0 ? 9550304 : 5529417;
                                double lighting = 1.0;
                                if (hit.getType() != Type.MISS) {
                                    BlockState block = mc.level.getBlockState(hit.getBlockPos());
                                    rgb = block.getMapColor(mc.level, hit.getBlockPos()).col;
                                    lighting = (1.0 - distance / 30.0) * (hit.getDirection().getAxis() == Axis.Y ? 1.0 : 0.8);
                                }

                                for (Entity entity : entities) {
                                    Optional<Vec3> intersect = entity.getBoundingBox().clip(origin, end);
                                    if (intersect.isPresent() && intersect.get().distanceTo(origin) < distance) {
                                        distance = intersect.get().distanceTo(origin);
                                        rgb = 13084551;
                                        lighting = 1.0 - distance / 38.0;
                                    }
                                }

                                if (illuminated) {
                                    lighting = Math.max(0.65, lighting);
                                }

                                int red = clamp((int)((rgb >> 16 & 0xFF) * lighting));
                                int green = clamp((int)((rgb >> 8 & 0xFF) * lighting));
                                int blue = clamp((int)((rgb & 0xFF) * lighting));
                                if (!color) {
                                    int gray = VideoFormat.displayRgb(red << 16 | green << 8 | blue, sourceFormat, displayFormat) & 0xFF;
                                    blue = gray;
                                    green = gray;
                                    red = gray;
                                }

                                this.pixels[py * 48 + px] = red << 16 | green << 8 | blue;
                            }
                        }
                    }

                    return true;
                }
            }
        } else {
            return false;
        }
    }

    public boolean draw(GuiGraphics graphics, BlockPos camera, int x, int y, int width, int height, int brightness, int contrast, VideoFormat displayFormat) {
        if (!this.update(camera, displayFormat)) {
            return false;
        } else {
            for (int py = 0; py < 36; py++) {
                for (int px = 0; px < 48; px++) {
                    int rgb = this.pixels[py * 48 + px];
                    int red = adjust(rgb >> 16 & 0xFF, brightness, contrast);
                    int green = adjust(rgb >> 8 & 0xFF, brightness, contrast);
                    int blue = adjust(rgb & 0xFF, brightness, contrast);
                    graphics.fill(
                        x + px * width / 48,
                        y + py * height / 36,
                        x + (px + 1) * width / 48,
                        y + (py + 1) * height / 36,
                        0xFF000000 | red << 16 | green << 8 | blue
                    );
                }
            }

            return true;
        }
    }

    public boolean drawWall(
        PoseStack pose,
        MultiBufferSource buffers,
        BlockPos camera,
        float x1,
        float y1,
        float x2,
        float y2,
        float z,
        int brightness,
        int contrast,
        VideoFormat displayFormat
    ) {
        if (!this.update(camera, displayFormat)) {
            return false;
        } else {
            VertexConsumer v = buffers.getBuffer(RenderType.entityTranslucentEmissive(HomeNet.id("textures/block/ha4100h/video_white.png")));

            for (int py = 0; py < 36; py++) {
                for (int px = 0; px < 48; px++) {
                    int rgb = this.pixels[py * 48 + px];
                    int r = adjust(rgb >> 16 & 0xFF, brightness, contrast);
                    int g = adjust(rgb >> 8 & 0xFF, brightness, contrast);
                    int b = adjust(rgb & 0xFF, brightness, contrast);
                    float left = x2 - (x2 - x1) * px / 48.0F;
                    float right = x2 - (x2 - x1) * (px + 1) / 48.0F;
                    float top = y2 - (y2 - y1) * py / 36.0F;
                    float bottom = y2 - (y2 - y1) * (py + 1) / 36.0F;

                    for (float[] point : new float[][]{
                        {left, top, 0.0F, 0.0F}, {left, bottom, 0.0F, 1.0F}, {right, bottom, 1.0F, 1.0F}, {right, top, 1.0F, 0.0F}
                    }) {
                        v.vertex(pose.last().pose(), point[0] / 16.0F, point[1] / 16.0F, z / 16.0F)
                            .color(r, g, b, 255)
                            .uv(point[2], point[3])
                            .overlayCoords(OverlayTexture.NO_OVERLAY)
                            .uv2(15728880)
                            .normal(pose.last().normal(), 0.0F, 0.0F, -1.0F)
                            .endVertex();
                    }
                }
            }

            return true;
        }
    }

    private static int clamp(int x) {
        return Math.max(0, Math.min(255, x));
    }

    private static int adjust(int x, int brightness, int contrast) {
        return clamp((int)((x - 128) * (0.6 + contrast * 0.2) + 128.0 + (brightness - 2) * 22));
    }
}
