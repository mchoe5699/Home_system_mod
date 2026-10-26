package com.qwerty.homenet.network;

import com.qwerty.homenet.blockentity.LobbyPhoneBlockEntity;
import com.qwerty.homenet.intercom.Intercom;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public record LobbyKeyPacket(BlockPos pos, int key, String text) {
    public static void encode(LobbyKeyPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeVarInt(p.key);
        buf.writeUtf(p.text, 256);
    }

    public static LobbyKeyPacket decode(FriendlyByteBuf buf) {
        return new LobbyKeyPacket(buf.readBlockPos(), buf.readVarInt(), buf.readUtf(256));
    }

    public static void handle(LobbyKeyPacket p, Supplier<Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null && player.level().isLoaded(p.pos) && Intercom.inReach(player, p.pos)) {
            if (p.key >= 0 && p.key <= 16) {
                if (player.level().getBlockEntity(p.pos) instanceof LobbyPhoneBlockEntity be) {
                    be.press(player, p.key, p.text);
                }
            }
        }
    }
}
