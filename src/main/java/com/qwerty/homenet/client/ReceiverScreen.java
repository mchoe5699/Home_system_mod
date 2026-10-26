package com.qwerty.homenet.client;

import com.qwerty.homenet.network.WallpadDataPacket;
import net.minecraft.core.BlockPos;

public interface ReceiverScreen {
    BlockPos getPos();

    void update(WallpadDataPacket var1);
}
