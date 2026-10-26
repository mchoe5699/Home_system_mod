package com.qwerty.homenet.guard;

import com.qwerty.homenet.analog.AnalogBus;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.intercom.IntercomCaller;
import com.qwerty.homenet.intercom.IntercomLine;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public interface GuardExchange extends IntercomCaller {
    default void addLineFrom(BlockPos source, IntercomLine line) {
        this.addLine(line);
    }

    default void setGuardEmergency(BlockPos source, String guardId, boolean active) {
    }

    String modelId();

    GuardSystem guardSystem();

    String manufacturer();

    boolean supports(AnalogBus var1);

    void onLinked(BlockPos var1, String var2);

    void onUnlinked(BlockPos var1);

    String assignedUnit(BlockPos var1);

    void callFrom(ReceiverBlockEntity var1, ServerPlayer var2);

    void setEmergency(BlockPos var1, String var2, boolean var3);

    default void setSafetyEvent(BlockPos receiver, String unit, String type, boolean active) {
        if (type.equals("BS")) {
            this.setEmergency(receiver, unit, active);
        }
    }

    void setAway(BlockPos var1, String var2, boolean var3);
}
