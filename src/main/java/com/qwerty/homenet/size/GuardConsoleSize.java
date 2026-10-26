package com.qwerty.homenet.size;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class GuardConsoleSize {
    public static final float AM_10KP = 0.88888896F;
    public static final float KAM_305 = 0.8648649F;
    public static final float HMC_7000 = 0.84210527F;

    private GuardConsoleSize() {
    }

    public static VoxelShape scale(VoxelShape shape, float factor) {
        VoxelShape result = Shapes.empty();

        for (AABB box : shape.toAabbs()) {
            result = Shapes.or(
                result,
                Shapes.create(
                    0.5 + (box.minX - 0.5) * factor,
                    box.minY * factor,
                    0.5 + (box.minZ - 0.5) * factor,
                    0.5 + (box.maxX - 0.5) * factor,
                    box.maxY * factor,
                    0.5 + (box.maxZ - 0.5) * factor
                )
            );
        }

        return result;
    }
}
