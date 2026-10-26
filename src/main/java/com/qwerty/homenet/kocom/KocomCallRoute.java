package com.qwerty.homenet.kocom;

import com.qwerty.homenet.blockentity.GuardMasterBlockEntity;
import com.qwerty.homenet.blockentity.Klp203rfBlockEntity;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.blockentity.WallpadBlockEntity;
import com.qwerty.homenet.network.WallpadActionPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public final class KocomCallRoute {
    private KocomCallRoute() {
    }

    public static boolean owns(BlockEntity device) {
        return device instanceof Klp203rfBlockEntity
            || device instanceof ReceiverBlockEntity receiver
                && ("KOCOM".equalsIgnoreCase(receiver.manufacturer()) || device instanceof GuardMasterBlockEntity || device instanceof WallpadBlockEntity);
    }

    public static Vec3 centre(BlockGetter level, BlockPos pos) {
        return Vec3.atCenterOf(pos);
    }

    public static boolean dispatch(WallpadActionPacket packet, ServerPlayer player) {
        BlockEntity device = player.level().getBlockEntity(packet.pos());
        if (device instanceof ReceiverBlockEntity receiver && owns(device)) {
            receiver.handleAction(player, ReceiverBlockEntity.Action.byId(packet.action()), packet.target(), packet.text());
            return true;
        } else {
            return false;
        }
    }
}
