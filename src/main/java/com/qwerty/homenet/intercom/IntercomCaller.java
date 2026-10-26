package com.qwerty.homenet.intercom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface IntercomCaller {
    default BlockPos intercomPosition() {
        return ((BlockEntity)this).getBlockPos();
    }

    String callerKey();

    boolean onAnswered(BlockPos var1);

    void onDoorOpened(BlockPos var1);

    void onReceiverHangUp(BlockPos var1, DoorStatus var2);

    void addLine(IntercomLine var1);

    void syncScreens();
}
