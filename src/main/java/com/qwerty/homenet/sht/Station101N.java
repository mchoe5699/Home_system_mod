package com.qwerty.homenet.sht;

import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class Station101N extends Station102 {
    public Station101N(BlockPos p, BlockState s) {
        super(Product.TYPE101N.get(), p, s);
    }

    @Override
    protected Map<String, String> settingsForClient() {
        Map<String, String> m = super.settingsForClient();
        m.put("model", "SHT-101N");
        return m;
    }

    @Override
    public void press(String key, ServerPlayer p, boolean remote) {
        if (!key.equals("antenna")) {
            super.press(key.equals("intercom") ? "redial" : key, p, remote);
        }
    }

    public static final class Block extends Product.StationBlock {
        public Block() {
            super(true);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
            return new Station101N(p, s);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
            return l.isClientSide ? null : createTickerHelper(t, Product.TYPE101N.get(), ReceiverBlockEntity::serverTick);
        }
    }
}
