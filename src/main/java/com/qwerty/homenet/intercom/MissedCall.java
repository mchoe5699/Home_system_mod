package com.qwerty.homenet.intercom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public record MissedCall(String caller, long dayTime) {
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Caller", this.caller);
        tag.putLong("Time", this.dayTime);
        return tag;
    }

    public static MissedCall load(CompoundTag tag) {
        return new MissedCall(tag.getString("Caller"), tag.getLong("Time"));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.caller, 64);
        buf.writeLong(this.dayTime);
    }

    public static MissedCall read(FriendlyByteBuf buf) {
        return new MissedCall(buf.readUtf(64), buf.readLong());
    }
}
