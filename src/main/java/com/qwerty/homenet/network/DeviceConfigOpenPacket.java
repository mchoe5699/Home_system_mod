package com.qwerty.homenet.network;

import com.qwerty.homenet.client.ClientPacketHandler;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public record DeviceConfigOpenPacket(BlockPos pos, String name, int type, boolean on) {
    public static void encode(DeviceConfigOpenPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeUtf(p.name, 128);
        buf.writeVarInt(p.type);
        buf.writeBoolean(p.on);
    }

    public static DeviceConfigOpenPacket decode(FriendlyByteBuf buf) {
        return new DeviceConfigOpenPacket(buf.readBlockPos(), buf.readUtf(128), buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(DeviceConfigOpenPacket p, Supplier<Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleDeviceConfig(p));
    }
}
