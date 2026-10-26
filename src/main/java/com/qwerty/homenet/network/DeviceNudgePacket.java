package com.qwerty.homenet.network;

import com.qwerty.homenet.move.DeviceOffsets;
import com.qwerty.homenet.registry.ModItems;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public record DeviceNudgePacket(BlockPos pos, int direction, int millimetres) {
    public static void encode(DeviceNudgePacket p, FriendlyByteBuf b) {
        b.writeBlockPos(p.pos);
        b.writeInt(p.direction);
        b.writeInt(p.millimetres);
    }

    public static DeviceNudgePacket decode(FriendlyByteBuf b) {
        return new DeviceNudgePacket(b.readBlockPos(), b.readInt(), b.readInt());
    }

    public static void handle(DeviceNudgePacket p, Supplier<Context> c) {
        ServerPlayer player = c.get().getSender();
        if (player != null
            && p.direction >= 0
            && p.direction <= 5
            && (player.getMainHandItem().is(ModItems.DEVICE_MOVER.get()) || player.getOffhandItem().is(ModItems.DEVICE_MOVER.get()))) {
            DeviceOffsets.Result result = DeviceOffsets.nudgeMm(player, p.pos, Direction.values()[p.direction], p.millimetres);
            ModNetwork.sendTo(player, new DeviceMoveOpenPacket(result.pos(), result.message(), false));
        }
    }
}
