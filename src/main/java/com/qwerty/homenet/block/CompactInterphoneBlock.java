package com.qwerty.homenet.block;

import com.qwerty.homenet.blockentity.CompactInterphoneBlockEntity;
import com.qwerty.homenet.blockentity.Ip203apBlockEntity;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public final class CompactInterphoneBlock extends ReceiverBlock {
    private final String family;

    public CompactInterphoneBlock(Properties p, String family) {
        super(p, DeviceRegistry.Kind.INTERPHONE, 1.63125, 4.4, 11.6, 2.30625, false);
        this.family = family;
    }

    private BlockEntityType<Ip203apBlockEntity> type() {
        String var1 = this.family;

        return switch (var1) {
            case "ip200pc" -> (BlockEntityType)ModBlockEntities.IP_200PC.get();
            case "ip200pchb" -> (BlockEntityType)ModBlockEntities.IP_200PCHB.get();
            default -> (BlockEntityType)ModBlockEntities.DP_202H.get();
        };
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
        return new CompactInterphoneBlockEntity(this.type(), p, s);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        return l.isClientSide ? null : createTickerHelper(t, this.type(), (level, pos, state, b) -> {
            ((CompactInterphoneBlockEntity)b).tickBell();
            Ip203apBlockEntity.tick(level, pos, state, b);
        });
    }
}
