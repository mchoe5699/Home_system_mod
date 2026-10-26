package com.qwerty.homenet.block;

import net.minecraft.network.chat.Component;

public enum DeviceType {
    LIGHT("light", false),
    OUTLET("outlet", false),
    HEATING("heating", false),
    VENT("vent", false),
    GAS("gas", true),
    DOOR_LOCK("door_lock", true),
    CURTAIN("curtain", true),
    OTHER("other", false),
    AIRCON("aircon", false);

    private final String key;
    private final boolean openClose;

    private DeviceType(String key, boolean openClose) {
        this.key = key;
        this.openClose = openClose;
    }

    public Component displayName() {
        return Component.translatable("device_type.qwertys_homenet." + this.key);
    }

    public Component stateName(boolean on) {
        String s = this.openClose ? (on ? "open" : "closed") : (on ? "on" : "off");
        return Component.translatable("device_state.qwertys_homenet." + s);
    }

    public DeviceType next() {
        DeviceType[] v = values();
        return v[(this.ordinal() + 1) % v.length];
    }

    public static DeviceType byId(int id) {
        DeviceType[] v = values();
        return id >= 0 && id < v.length ? v[id] : OTHER;
    }
}
