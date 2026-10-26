package com.qwerty.homenet.sht;

import com.mojang.blaze3d.vertex.PoseStack;
import com.qwerty.homenet.video.VideoFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

final class Sht104Recording {
    static final int W = 20;
    static final int H = 15;
    static final int MAX_FRAMES = 6;

    static int[] capture(ServerLevel level, BlockPos camera) {
        if (camera != null && level.isLoaded(camera)) {
            BlockState state = level.getBlockState(camera);
            Direction dir = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;
            Vec3 forward = new Vec3(dir.getStepX(), 0.0, dir.getStepZ());
            Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
            Vec3 origin = Vec3.atCenterOf(camera).add(forward.scale(0.55));
            int[] pixels = new int[300];

            for (int y = 0; y < 15; y++) {
                for (int x = 0; x < 20; x++) {
                    double h = (x + 0.5 - 10.0) / 10.0;
                    double v = (7.5 - y - 0.5) / 7.5;
                    Vec3 aim = forward.add(right.scale(h * 0.7)).add(0.0, v * 0.52, 0.0).normalize();
                    BlockHitResult hit = level.clip(new ClipContext(origin, origin.add(aim.scale(12.0)), Block.VISUAL, Fluid.NONE, null));
                    if (hit.getType() == Type.BLOCK) {
                        BlockPos at = hit.getBlockPos();
                        pixels[y * 20 + x] = 0xFF000000 | level.getBlockState(at).getMapColor(level, at).col;
                    } else {
                        pixels[y * 20 + x] = v > 0.0 ? -8348731 : -8418951;
                    }
                }
            }

            if (level.getBlockEntity(camera) instanceof Ht512.Unit lobby && lobby.videoFormat() == VideoFormat.MONOCHROME) {
                for (int i = 0; i < pixels.length; i++) {
                    pixels[i] = tint(pixels[i], false);
                }
            }

            return pixels;
        } else {
            return null;
        }
    }

    static void gui(GuiGraphics g, int[] frame, int left, int top, int width, int height, boolean color) {
        if (frame != null && frame.length == 300) {
            for (int y = 0; y < 15; y++) {
                for (int x = 0; x < 20; x++) {
                    g.fill(
                        left + x * width / 20,
                        top + y * height / 15,
                        left + (x + 1) * width / 20 + 1,
                        top + (y + 1) * height / 15 + 1,
                        tint(frame[y * 20 + x], color)
                    );
                }
            }
        }
    }

    static void wall(PoseStack p, MultiBufferSource out, int[] frame, boolean color) {
        if (frame != null && frame.length == 300) {
            for (int y = 0; y < 15; y++) {
                for (int x = 0; x < 20; x++) {
                    Surface.patch(
                        p,
                        out,
                        183.0F + x * 103.0F / 20.0F,
                        79.0F + y * 74.0F / 15.0F,
                        5.21F,
                        4.9933333F,
                        46.3F,
                        tint(frame[y * 20 + x], color) & 16777215,
                        15728880
                    );
                }
            }
        }
    }

    private static int tint(int argb, boolean color) {
        if (color) {
            return argb;
        } else {
            int r = argb >> 16 & 0xFF;
            int g = argb >> 8 & 0xFF;
            int b = argb & 0xFF;
            int grey = (r * 30 + g * 59 + b * 11) / 100;
            return 0xFF000000 | grey << 16 | grey << 8 | grey;
        }
    }
}
