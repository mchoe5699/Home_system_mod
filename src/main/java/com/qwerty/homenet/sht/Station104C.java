package com.qwerty.homenet.sht;

import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class Station104C extends Station104M {
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Station104C.Unit(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, Product.TYPE104C.get(), ReceiverBlockEntity::serverTick);
    }

    public static class Unit extends Station104M.Unit {
        public Unit(BlockPos pos, BlockState state) {
            super(Product.TYPE104C.get(), pos, state);
        }

        @Override
        String modelName() {
            return "SHT-104C";
        }
    }
}
