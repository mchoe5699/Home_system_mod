package com.qwerty.homenet.network;

import com.qwerty.homenet.client.move.MoveClientEvents;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public record DeviceMoveOpenPacket(BlockPos pos, String message, boolean open) {
    public DeviceMoveOpenPacket(BlockPos pos, String message) {
        this(pos, message, true);
    }

    public static void encode(DeviceMoveOpenPacket p, FriendlyByteBuf b) {
        b.writeBlockPos(p.pos);
        b.writeUtf(p.message, 128);
        b.writeBoolean(p.open);
    }

    public static DeviceMoveOpenPacket decode(FriendlyByteBuf b) {
        return new DeviceMoveOpenPacket(b.readBlockPos(), b.readUtf(128), b.readBoolean());
    }

    public static void handle(DeviceMoveOpenPacket p, Supplier<Context> c) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MoveClientEvents.open(p));
    }
}
