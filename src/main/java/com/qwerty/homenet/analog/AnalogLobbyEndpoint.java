package com.qwerty.homenet.analog;

import net.minecraft.core.BlockPos;

public interface AnalogLobbyEndpoint {
    boolean supports(AnalogBus var1);

    default String manufacturer() {
        return "";
    }

    default String videoNumber() {
        return "";
    }

    BlockPos switchboardPosition();

    default BlockPos cameraPosition() {
        return null;
    }
}
