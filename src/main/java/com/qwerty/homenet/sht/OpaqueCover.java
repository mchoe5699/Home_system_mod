package com.qwerty.homenet.sht;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

final class OpaqueCover {
    static boolean covered(Level level, Vec3 eye, AABB box, Entity viewer) {
        if (box.inflate(0.15).contains(eye)) {
            return false;
        } else if (level.getBlockState(BlockPos.containing(eye)).isSolidRender(level, BlockPos.containing(eye))) {
            return false;
        } else {
            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 3; y++) {
                    for (int z = 0; z < 3; z++) {
                        Vec3 target = new Vec3(
                            box.minX + (box.maxX - box.minX) * x / 2.0, box.minY + (box.maxY - box.minY) * y / 2.0, box.minZ + (box.maxZ - box.minZ) * z / 2.0
                        );
                        var context = new ClipContext(eye, target, Block.VISUAL, Fluid.NONE, viewer) {
                            @Override
                            public VoxelShape getBlockShape(BlockState state, BlockGetter world, BlockPos pos) {
                                return state.isSolidRender(world, pos) ? Shapes.block() : Shapes.empty();
                            }
                        };
                        if (level.clip(context).getType() == Type.MISS) {
                            return false;
                        }
                    }
                }
            }

            return true;
        }
    }
}
