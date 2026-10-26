package com.qwerty.homenet.intercom;

import java.util.Locale;
import net.minecraft.network.chat.Component;

public enum DoorStatus {
    IDLE,
    CALLING,
    CONNECTED,
    OPENED,
    NO_UNIT,
    NO_SIGNAL,
    BUSY,
    NO_ANSWER,
    REJECTED,
    ENDED,
    TIMEOUT;

    public Component display(String arg) {
        return Component.translatable("door_status.qwertys_homenet." + this.name().toLowerCase(Locale.ROOT), arg);
    }

    public static DoorStatus byId(int id) {
        DoorStatus[] v = values();
        return id >= 0 && id < v.length ? v[id] : IDLE;
    }
}
