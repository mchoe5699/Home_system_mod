package com.qwerty.homenet.network;

import com.qwerty.homenet.client.move.MoveClientEvents;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public record DeviceOffsetPacket(BlockPos pos, int x, int y, int z) {
    public static void encode(DeviceOffsetPacket p, FriendlyByteBuf b) {
        b.writeBlockPos(p.pos);
        b.writeInt(p.x);
        b.writeInt(p.y);
        b.writeInt(p.z);
    }

    public static DeviceOffsetPacket decode(FriendlyByteBuf b) {
        return new DeviceOffsetPacket(b.readBlockPos(), b.readInt(), b.readInt(), b.readInt());
    }

    public static void handle(DeviceOffsetPacket p, Supplier<Context> c) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MoveClientEvents.offset(p));
    }
}
