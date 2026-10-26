package com.qwerty.homenet.block;

import com.qwerty.homenet.blockentity.Ha4100hBlockEntity;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public final class Ha4100hBlock extends ReceiverBlock {
    public Ha4100hBlock(Properties p) {
        super(p, DeviceRegistry.Kind.VIDEO_PHONE, 4.0, 4.864, 11.136, 1.01, false);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
        return new Ha4100hBlockEntity(p, s);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        return l.isClientSide ? null : createTickerHelper(t, ModBlockEntities.HA_4100H.get(), ReceiverBlockEntity::serverTick);
    }
}
