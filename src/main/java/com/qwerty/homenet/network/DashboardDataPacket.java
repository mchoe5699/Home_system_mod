package com.qwerty.homenet.network;

import com.qwerty.homenet.client.ClientPacketHandler;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.data.ZoneData;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public record DashboardDataPacket(boolean open, List<ZoneData.Zone> zones, List<DeviceRegistry.Entry> devices) {
    public static void encode(DashboardDataPacket p, FriendlyByteBuf buf) {
        buf.writeBoolean(p.open);
        buf.writeCollection(p.zones, (b, z) -> z.write(b));
        buf.writeCollection(p.devices, (b, d) -> d.write(b));
    }

    public static DashboardDataPacket decode(FriendlyByteBuf buf) {
        return new DashboardDataPacket(buf.readBoolean(), buf.readList(ZoneData.Zone::read), buf.readList(DeviceRegistry.Entry::read));
    }

    public static void handle(DashboardDataPacket p, Supplier<Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleDashboard(p));
    }
}
