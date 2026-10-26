package com.qwerty.homenet.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record DeviceEntry(BlockPos pos, String name, int type, boolean on, boolean online, int setTemp, int roomTemp, int level, boolean away) {
    public DeviceEntry(BlockPos pos, String name, int type, boolean on, boolean online) {
        this(pos, name, type, on, online, 0, 0, 1, false);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeUtf(this.name, 128);
        buf.writeVarInt(this.type);
        buf.writeBoolean(this.on);
        buf.writeBoolean(this.online);
        buf.writeVarInt(this.setTemp);
        buf.writeInt(this.roomTemp);
        buf.writeVarInt(this.level);
        buf.writeBoolean(this.away);
    }

    public static DeviceEntry read(FriendlyByteBuf buf) {
        return new DeviceEntry(
            buf.readBlockPos(),
            buf.readUtf(128),
            buf.readVarInt(),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readVarInt(),
            buf.readInt(),
            buf.readVarInt(),
            buf.readBoolean()
        );
    }
}
