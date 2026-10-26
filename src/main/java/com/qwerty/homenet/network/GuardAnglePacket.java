package com.qwerty.homenet.network;

import com.qwerty.homenet.blockentity.GuardMasterBlockEntity;
import com.qwerty.homenet.blockentity.Hmc7000BlockEntity;
import com.qwerty.homenet.blockentity.Kam305BlockEntity;
import com.qwerty.homenet.intercom.Intercom;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public record GuardAnglePacket(BlockPos pos, int delta) {
    public static void encode(GuardAnglePacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeVarInt(p.delta);
    }

    public static GuardAnglePacket decode(FriendlyByteBuf buf) {
        return new GuardAnglePacket(buf.readBlockPos(), buf.readVarInt());
    }

    public static void handle(GuardAnglePacket p, Supplier<Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null && player.level().isLoaded(p.pos) && Intercom.inReach(player, p.pos)) {
            if (player.level().getBlockEntity(p.pos) instanceof Hmc7000BlockEntity hmc) {
                hmc.adjustAngle(p.delta);
            } else if (player.level().getBlockEntity(p.pos) instanceof Kam305BlockEntity kam) {
                kam.adjustAngle(p.delta);
            } else if (player.getMainHandItem().isEmpty()) {
                if (player.level().getBlockEntity(p.pos) instanceof GuardMasterBlockEntity be) {
                    be.adjustAngle(player, Math.max(-5, Math.min(5, p.delta)));
                }
            }
        }
    }
}
