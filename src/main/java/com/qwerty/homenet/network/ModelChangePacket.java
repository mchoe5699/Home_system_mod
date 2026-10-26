package com.qwerty.homenet.network;

import com.qwerty.homenet.blockentity.Ip203apBlockEntity;
import com.qwerty.homenet.blockentity.Klp203rfBlockEntity;
import com.qwerty.homenet.blockentity.Kvm906BlockEntity;
import com.qwerty.homenet.intercom.Intercom;
import com.qwerty.homenet.kvm.KvmModel;
import com.qwerty.homenet.registry.ModItems;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public record ModelChangePacket(BlockPos pos, String model) {
    public static void encode(ModelChangePacket p, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(p.pos);
        buffer.writeUtf(p.model, 32);
    }

    public static ModelChangePacket decode(FriendlyByteBuf buffer) {
        return new ModelChangePacket(buffer.readBlockPos(), buffer.readUtf(32));
    }

    public static void handle(ModelChangePacket p, Supplier<Context> context) {
        ServerPlayer player = context.get().getSender();
        KvmModel model = KvmModel.byId(p.model);
        if (player != null && player.level().isLoaded(p.pos) && Intercom.inReach(player, p.pos)) {
            if (player.getMainHandItem().is(ModItems.MODEL_CHANGER.get()) || player.getOffhandItem().is(ModItems.MODEL_CHANGER.get())) {
                if (player.level().getBlockEntity(p.pos) instanceof Klp203rfBlockEntity panel) {
                    panel.changeModel(player, p.model);
                } else if (player.level().getBlockEntity(p.pos) instanceof Ip203apBlockEntity phone) {
                    phone.changeFamily(player, p.model);
                } else if (model != null && player.level().getBlockEntity(p.pos) instanceof Kvm906BlockEntity be) {
                    be.changeModel(player, model);
                }
            }
        }
    }
}
