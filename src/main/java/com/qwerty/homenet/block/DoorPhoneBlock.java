package com.qwerty.homenet.block;

import com.qwerty.homenet.blockentity.DoorStationBlockEntity;
import com.qwerty.homenet.item.KvmDesignItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;

public class DoorPhoneBlock extends DoorStationBlock {
    public DoorPhoneBlock(Properties props) {
        super(props, 2.5, 4.0, 12.0, 1.0);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof KvmDesignItem || holdingLinker(player, hand)) {
            return InteractionResult.PASS;
        } else if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            if (level.getBlockEntity(pos) instanceof DoorStationBlockEntity be) {
                if (!be.isInCall()) {
                    be.call();
                }

                if (player instanceof ServerPlayer sp) {
                    be.openFor(sp);
                }
            }

            return InteractionResult.CONSUME;
        }
    }
}
