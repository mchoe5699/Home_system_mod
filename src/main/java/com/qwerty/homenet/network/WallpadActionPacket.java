package com.qwerty.homenet.network;

import com.qwerty.homenet.blockentity.CallerBlockEntity;
import com.qwerty.homenet.blockentity.Klp203rfBlockEntity;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.intercom.Intercom;
import com.qwerty.homenet.kocom.KocomCallRoute;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public record WallpadActionPacket(BlockPos pos, int action, BlockPos target, String text) {
    public WallpadActionPacket(BlockPos pos, ReceiverBlockEntity.Action action) {
        this(pos, action.ordinal(), BlockPos.ZERO, "");
    }

    public static void encode(WallpadActionPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeVarInt(p.action);
        buf.writeBlockPos(p.target);
        buf.writeUtf(p.text, 256);
    }

    public static WallpadActionPacket decode(FriendlyByteBuf buf) {
        return new WallpadActionPacket(buf.readBlockPos(), buf.readVarInt(), buf.readBlockPos(), buf.readUtf(256));
    }

    public static void handle(WallpadActionPacket p, Supplier<Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null) {
            dispatch(p, player);
        }
    }

    public static void dispatch(WallpadActionPacket p, ServerPlayer player) {
        if (player.level().isLoaded(p.pos)) {
            if (!Intercom.inReach(player, p.pos)) {
                player.displayClientMessage(Component.literal("기기에서 너무 멀어 조작할 수 없습니다. 기기 가까이에서 다시 시도하세요."), true);
            } else if (player.level().getBlockEntity(p.pos) instanceof Klp203rfBlockEntity panel && !p.text.startsWith("logo_")) {
                panel.action(player, ReceiverBlockEntity.Action.byId(p.action), p.text);
            } else if (p.text.startsWith("logo_")
                && ReceiverBlockEntity.Action.byId(p.action) == ReceiverBlockEntity.Action.SET_SETTING
                && player.level().getBlockEntity(p.pos) instanceof CallerBlockEntity caller) {
                caller.editLogo(p.text);
                ModNetwork.sendTo(player, caller.logoPacket(false));
            } else {
                if (player.level().getBlockEntity(p.pos) instanceof ReceiverBlockEntity be) {
                    if (p.text.startsWith("logo_") && ReceiverBlockEntity.Action.byId(p.action) == ReceiverBlockEntity.Action.SET_SETTING) {
                        be.editLogo(p.text);
                        return;
                    }

                    if (KocomCallRoute.dispatch(p, player)) {
                        return;
                    }

                    be.handleAction(player, ReceiverBlockEntity.Action.byId(p.action), p.target, p.text);
                }
            }
        }
    }
}
