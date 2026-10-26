package com.qwerty.homenet.sht;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

final class SamsungVideoView {
    private static final int W = 48;
    private static final int H = 36;
    private static final Map<BlockPos, SamsungVideoView.Frame> CACHE = new LinkedHashMap<BlockPos, SamsungVideoView.Frame>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Entry<BlockPos, SamsungVideoView.Frame> e) {
            return this.size() > 16;
        }
    };

    static boolean isColorLobby(Level level, BlockPos at) {
        return level != null && at != null && level.isLoaded(at) && level.getBlockEntity(at) instanceof Ht513.Unit;
    }

    static boolean isColorSource(Level level, BlockPos at) {
        return isColorLobby(level, at)
            || level != null && at != null && level.isLoaded(at) && level.getBlockEntity(at) instanceof Ht100s.Unit door && DoorCameraRouting.isVideo(door);
    }

    static int[] frame(BlockPos camera) {
        ClientLevel level = Minecraft.getInstance().level;
        if (!isColorSource(level, camera)) {
            return null;
        } else {
            long now = System.currentTimeMillis();
            SamsungVideoView.Frame cached = CACHE.get(camera);
            BlockState state = level.getBlockState(camera);
            Direction direction = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;
            Vec3 forward = new Vec3(direction.getStepX(), 0.0, direction.getStepZ());
            Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
            Vec3 origin = Vec3.atCenterOf(camera).add(forward.scale(0.75));
            ArrayList<Entity> entities = new ArrayList<>(
                level.getEntities((Entity)null, new AABB(origin, origin).inflate(16.0), e -> e.isAlive() && !e.isInvisible())
            );
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && player.position().distanceToSqr(origin) < 256.0 && !entities.contains(player)) {
                entities.add(player);
            }

            long entitySignature = 0L;

            for (Entity entity : entities) {
                entitySignature = entitySignature * 31L + entity.getId();
                entitySignature = entitySignature * 31L + Double.doubleToLongBits(entity.getX());
                entitySignature = entitySignature * 31L + Double.doubleToLongBits(entity.getY());
                entitySignature = entitySignature * 31L + Double.doubleToLongBits(entity.getZ());
            }

            if (cached != null && cached.level() == level && cached.entities() == entitySignature && now - cached.time() < 250L) {
                return cached.pixels();
            } else {
                int[] pixels = new int[1728];

                for (int y = 0; y < 36; y++) {
                    for (int x = 0; x < 48; x++) {
                        double horizontal = (x + 0.5 - 24.0) / 24.0;
                        double vertical = (18.0 - y - 0.5) / 18.0;
                        Vec3 aim = forward.add(right.scale(horizontal * 1.11)).add(0.0, vertical * 0.727, 0.0).normalize();
                        Vec3 end = origin.add(aim.scale(16.0));
                        BlockHitResult hit = level.clip(new ClipContext(origin, end, Block.OUTLINE, Fluid.NONE, player));
                        boolean sceneBlock = hit.getType() == Type.BLOCK && !hit.getBlockPos().equals(camera);
                        double distance = sceneBlock ? origin.distanceTo(hit.getLocation()) : 16.0;
                        if (sceneBlock) {
                            BlockPos at = hit.getBlockPos();
                            pixels[y * 48 + x] = 0xFF000000 | level.getBlockState(at).getMapColor(level, at).col;
                        } else {
                            pixels[y * 48 + x] = vertical > 0.0 ? -8348731 : -8418951;
                        }

                        for (Entity entity : entities) {
                            Optional<Vec3> touch = entity.getBoundingBox().clip(origin, end);
                            if (touch.isPresent()) {
                                double near = origin.distanceTo(touch.get());
                                if (near < distance) {
                                    distance = near;
                                    pixels[y * 48 + x] = -3692889;
                                }
                            }
                        }
                    }
                }

                CACHE.put(camera, new SamsungVideoView.Frame(level, now, entitySignature, pixels));
                return pixels;
            }
        }
    }

    static void gui(GuiGraphics g, BlockPos camera) {
        int[] pixels = frame(camera);
        if (pixels != null) {
            for (int y = 0; y < 36; y++) {
                for (int x = 0; x < 48; x++) {
                    g.fill(183 + x * 103 / 48, 79 + y * 74 / 36, 183 + (x + 1) * 103 / 48 + 1, 79 + (y + 1) * 74 / 36 + 1, pixels[y * 48 + x]);
                }
            }
        }
    }

    static void wall(PoseStack p, MultiBufferSource out, BlockPos camera) {
        int[] pixels = frame(camera);
        if (pixels != null) {
            for (int y = 0; y < 36; y++) {
                for (int x = 0; x < 48; x++) {
                    Surface.patch(
                        p, out, 183.0F + x * 103.0F / 48.0F, 79.0F + y * 74.0F / 36.0F, 2.1958332F, 2.1055555F, 46.3F, pixels[y * 48 + x] & 16777215, 15728880
                    );
                }
            }
        }
    }

    private record Frame(Level level, long time, long entities, int[] pixels) {
    }
}
