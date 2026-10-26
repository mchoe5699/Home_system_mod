package com.qwerty.homenet.sht;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

final class DeviceVisibility {
    private static final Map<BlockEntity, DeviceVisibility.Result> LAST = new WeakHashMap<>();

    static void clear() {
        LAST.clear();
    }

    static boolean hidden(BlockEntity device, AABB local, Consumer<PoseStack> placement) {
        if (local == null) {
            return false;
        } else {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && mc.getCameraEntity() != null) {
                PoseStack pose = new PoseStack();
                placement.accept(pose);
                Matrix4f matrix = pose.last().pose();
                Vector3f v = new Vector3f();
                double minX = Double.POSITIVE_INFINITY;
                double minY = minX;
                double minZ = minX;
                double maxX = Double.NEGATIVE_INFINITY;
                double maxY = maxX;
                double maxZ = maxX;

                for (int i = 0; i < 8; i++) {
                    matrix.transformPosition(
                        (float)((i & 1) == 0 ? local.minX : local.maxX),
                        (float)((i & 2) == 0 ? local.minY : local.maxY),
                        (float)((i & 4) == 0 ? local.minZ : local.maxZ),
                        v
                    );
                    minX = Math.min(minX, (double)v.x);
                    minY = Math.min(minY, (double)v.y);
                    minZ = Math.min(minZ, (double)v.z);
                    maxX = Math.max(maxX, (double)v.x);
                    maxY = Math.max(maxY, (double)v.y);
                    maxZ = Math.max(maxZ, (double)v.z);
                }

                AABB box = new AABB(minX, minY, minZ, maxX, maxY, maxZ).move(device.getBlockPos());
                Vec3 eye = mc.gameRenderer.getMainCamera().getPosition();
                long now = System.nanoTime();
                DeviceVisibility.Result previous = LAST.get(device);
                if (previous != null && previous.box.equals(box) && previous.eye.distanceToSqr(eye) < 0.0025 && now < previous.expires) {
                    return previous.hidden;
                } else {
                    boolean hidden = OpaqueCover.covered(mc.level, eye, box, mc.getCameraEntity());
                    LAST.put(device, new DeviceVisibility.Result(box, eye, now + 100000000L, hidden));
                    return hidden;
                }
            } else {
                return false;
            }
        }
    }

    private record Result(AABB box, Vec3 eye, long expires, boolean hidden) {
    }
}
