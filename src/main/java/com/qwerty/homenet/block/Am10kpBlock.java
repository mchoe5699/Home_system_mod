package com.qwerty.homenet.block;

import com.qwerty.homenet.blockentity.Am10kpBlockEntity;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.move.DeviceOffsets;
import com.qwerty.homenet.registry.ModBlockEntities;
import com.qwerty.homenet.size.GuardConsoleSize;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class Am10kpBlock extends ReceiverBlock {
    public Am10kpBlock(Properties p) {
        super(p, DeviceRegistry.Kind.GUARD_CONSOLE, 7.2, 0.0, 5.0, 10.4, true);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return DeviceOffsets.shape(
            GuardConsoleSize.scale(
                s.getValue(FACING).getAxis() == Axis.X ? Block.box(2.8, 0.0, 0.8, 13.2, 6.2, 15.2) : Block.box(0.8, 0.0, 2.8, 15.2, 6.2, 13.2), 0.88888896F
            ),
            l,
            p
        );
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
        return new Am10kpBlockEntity(p, s);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        return l.isClientSide ? null : createTickerHelper(t, ModBlockEntities.AM_10KP.get(), Am10kpBlockEntity::tick);
    }
}
