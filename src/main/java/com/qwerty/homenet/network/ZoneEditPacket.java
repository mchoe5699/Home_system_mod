package com.qwerty.homenet.network;

import com.qwerty.homenet.data.ZoneData;
import com.qwerty.homenet.intercom.Intercom;
import com.qwerty.homenet.item.DashboardItem;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public record ZoneEditPacket(int action, int id, String name, int color, int x1, int z1, int x2, int z2) {
    public static final int ADD = 0;
    public static final int UPDATE = 1;
    public static final int DELETE = 2;
    public static final int MAX_SIDE = 4096;

    public static void encode(ZoneEditPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.action);
        buf.writeVarInt(p.id);
        buf.writeUtf(p.name, 64);
        buf.writeInt(p.color);
        buf.writeInt(p.x1);
        buf.writeInt(p.z1);
        buf.writeInt(p.x2);
        buf.writeInt(p.z2);
    }

    public static ZoneEditPacket decode(FriendlyByteBuf buf) {
        return new ZoneEditPacket(
            buf.readVarInt(), buf.readVarInt(), buf.readUtf(64), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt()
        );
    }

    public static void handle(ZoneEditPacket p, Supplier<Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null && (player.getMainHandItem().getItem() instanceof DashboardItem || player.getOffhandItem().getItem() instanceof DashboardItem)) {
            ZoneData data = ZoneData.get(player.serverLevel());
            String name = Intercom.sanitize(p.name, 32);
            if (name.isEmpty()) {
                name = "단지";
            }

            int color = p.color & 16777215;
            boolean sizeOk = Math.abs((long)p.x2 - p.x1) <= 4096L && Math.abs((long)p.z2 - p.z1) <= 4096L;
            switch (p.action) {
                case 0:
                    if (sizeOk) {
                        data.add(name, color, p.x1, p.z1, p.x2, p.z2);
                    }
                    break;
                case 1:
                    if (sizeOk && data.byId(p.id) != null) {
                        data.update(new ZoneData.Zone(p.id, name, color, p.x1, p.z1, p.x2, p.z2, data.byId(p.id).complex()));
                    }
                    break;
                case 2:
                    data.remove(p.id);
            }

            DashboardItem.sendData(player, false);
        }
    }
}
