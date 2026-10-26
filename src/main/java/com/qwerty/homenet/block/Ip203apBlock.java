package com.qwerty.homenet.block;

import com.qwerty.homenet.blockentity.Ip203apBlockEntity;
import com.qwerty.homenet.blockentity.Ip203apbBlockEntity;
import com.qwerty.homenet.blockentity.Kip234r2BlockEntity;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public class Ip203apBlock extends ReceiverBlock {
    protected boolean bus;
    private final boolean kip;

    public Ip203apBlock(Properties properties) {
        this(properties, false);
    }

    public Ip203apBlock(Properties properties, boolean bus) {
        this(properties, bus, false);
    }

    public Ip203apBlock(Properties properties, boolean bus, boolean kip) {
        super(properties, DeviceRegistry.Kind.INTERPHONE, 1.63125, 4.4, 11.6, 2.30625, false);
        this.bus = bus;
        this.kip = kip;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
        return (BlockEntity)(this.kip ? new Kip234r2BlockEntity(p, s) : (this.bus ? new Ip203apbBlockEntity(p, s) : new Ip203apBlockEntity(p, s)));
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        return l.isClientSide
            ? null
            : createTickerHelper(
                t,
                this.kip ? ModBlockEntities.KIP_234R2.get() : (this.bus ? ModBlockEntities.IP_203APB.get() : ModBlockEntities.IP_203AP.get()),
                (level, pos, state, b) -> {
                    if (b instanceof Kip234r2BlockEntity k) {
                        Kip234r2BlockEntity.tick(level, pos, state, k);
                    } else {
                        Ip203apBlockEntity.tick(level, pos, state, b);
                    }
                }
            );
    }
}
