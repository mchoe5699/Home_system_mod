package com.qwerty.homenet.network;

import com.qwerty.homenet.data.ZoneData;
import com.qwerty.homenet.item.DashboardItem;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public record ZoneGroupPacket(String name, int[] ids, boolean detach) {
    public static void encode(ZoneGroupPacket p, FriendlyByteBuf b) {
        b.writeUtf(p.name, 64);
        b.writeVarIntArray(p.ids);
        b.writeBoolean(p.detach);
    }

    public static ZoneGroupPacket decode(FriendlyByteBuf b) {
        return new ZoneGroupPacket(b.readUtf(64), b.readVarIntArray(4096), b.readBoolean());
    }

    public static boolean apply(ServerPlayer p, ZoneGroupPacket packet) {
        if (!(p.getMainHandItem().getItem() instanceof DashboardItem) && !(p.getOffhandItem().getItem() instanceof DashboardItem)) {
            return false;
        } else {
            ZoneData data = ZoneData.get(p.serverLevel());
            List<Integer> ids = Arrays.stream(packet.ids).boxed().toList();
            return packet.detach ? data.ungroup(ids) : data.group(ids, packet.name);
        }
    }

    public static void handle(ZoneGroupPacket p, Supplier<Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            if (!apply(player, p)) {
                player.displayClientMessage(Component.literal("구획 선택과 단지 이름을 확인하세요."), true);
            }

            DashboardItem.sendData(player, false);
        }
    }
}
