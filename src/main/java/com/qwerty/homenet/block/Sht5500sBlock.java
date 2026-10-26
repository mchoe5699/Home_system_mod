package com.qwerty.homenet.block;

import com.qwerty.homenet.blockentity.Sht5500sBlockEntity;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.move.DeviceOffsets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class Sht5500sBlock extends ReceiverBlock {
    public Sht5500sBlock(Properties properties) {
        super(properties, DeviceRegistry.Kind.GUARD_CONSOLE, 6.4, 0.0, 5.3, 8.3, true);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = Shapes.or(
            Block.box(1.6, 0.0, 4.1, 14.4, 2.1, 12.4), Block.box(12.0, 0.9, 4.5, 14.1, 3.0, 12.0), Block.box(2.08, 1.5, 10.15, 11.52, 5.1, 12.0)
        );
        Direction facing = state.getValue(FACING);

        int turns = switch (facing) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };

        for (int i = 0; i < turns; i++) {
            VoxelShape next = Shapes.empty();

            for (AABB box : shape.toAabbs()) {
                next = Shapes.or(next, Shapes.create(1.0 - box.maxZ, box.minY, box.minX, 1.0 - box.minZ, box.maxY, box.maxX));
            }

            shape = next;
        }

        return DeviceOffsets.shape(shape, level, pos);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Sht5500sBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }
}
