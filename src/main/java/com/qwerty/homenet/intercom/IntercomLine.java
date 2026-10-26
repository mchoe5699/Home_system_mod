package com.qwerty.homenet.intercom;

import net.minecraft.network.FriendlyByteBuf;

public record IntercomLine(String side, String name, String text) {
    public static final int MAX_TEXT = 80;

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.side, 64);
        buf.writeUtf(this.name, 64);
        buf.writeUtf(this.text, 320);
    }

    public static IntercomLine read(FriendlyByteBuf buf) {
        return new IntercomLine(buf.readUtf(64), buf.readUtf(64), buf.readUtf(320));
    }
}
