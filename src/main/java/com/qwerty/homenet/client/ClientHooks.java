package com.qwerty.homenet.client;

import com.qwerty.homenet.client.lobby.LobbyPhoneScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public final class ClientHooks {
    private ClientHooks() {
    }

    public static void openLobbyPhone(BlockPos pos) {
        Minecraft.getInstance().setScreen(new LobbyPhoneScreen(pos));
    }
}
