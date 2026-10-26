package com.qwerty.homenet.blockentity;

import com.qwerty.homenet.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

public class Sht5500sBlockEntity extends ReceiverBlockEntity {
    public Sht5500sBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHT_5500S.get(), pos, state);
        this.unit = "";
        this.productLogo.set("logo_text", "SAMSUNG");
    }

    @Override
    protected void register() {
    }

    @Override
    public String manufacturer() {
        return "SAMSUNG";
    }

    @Override
    public boolean canSetUnitLocally() {
        return false;
    }

    @Override
    public boolean supportsDoorCommunication() {
        return false;
    }

    @Override
    public boolean bindSwitchboard(BlockPos pos) {
        return false;
    }

    @Override
    public boolean startRinging(BlockPos caller, String key) {
        return false;
    }

    @Override
    public String toggleReceiverDoor(BlockPos pos) {
        return "unsupported";
    }

    @Override
    public void openFor(ServerPlayer player) {
        player.displayClientMessage(Component.literal("SHT-5500S · 외형 모델 (동작 기능과 화면 구성은 추후 추가)"), true);
    }

    @Override
    public void handleAction(ServerPlayer player, ReceiverBlockEntity.Action action, BlockPos target, String text) {
        if (action == ReceiverBlockEntity.Action.SET_SETTING && text != null && text.startsWith("logo_")) {
            this.editLogo(text);
        }
    }
}
