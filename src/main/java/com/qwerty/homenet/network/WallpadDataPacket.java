package com.qwerty.homenet.network;

import com.qwerty.homenet.client.ClientPacketHandler;
import com.qwerty.homenet.intercom.IntercomLine;
import com.qwerty.homenet.intercom.MissedCall;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public record WallpadDataPacket(
    BlockPos pos,
    boolean open,
    int kind,
    String unit,
    boolean hasDoorPassword,
    int callState,
    String callerKey,
    boolean outgoing,
    String peer,
    String notice,
    List<DeviceEntry> devices,
    List<MissedCall> missed,
    List<IntercomLine> log,
    List<MissedCall> visitors,
    Map<String, String> settings,
    long[] energy,
    List<MissedCall> records
) {
    public WallpadDataPacket withNotice(String n) {
        return new WallpadDataPacket(
            this.pos,
            this.open,
            this.kind,
            this.unit,
            this.hasDoorPassword,
            this.callState,
            this.callerKey,
            this.outgoing,
            this.peer,
            n,
            this.devices,
            this.missed,
            this.log,
            this.visitors,
            this.settings,
            this.energy,
            this.records
        );
    }

    public String setting(String key, String def) {
        String v = this.settings.get(key);
        return v == null ? def : v;
    }

    public boolean flag(String key) {
        return "1".equals(this.settings.get(key));
    }

    public static void encode(WallpadDataPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeBoolean(p.open);
        buf.writeVarInt(p.kind);
        buf.writeUtf(p.unit, 64);
        buf.writeBoolean(p.hasDoorPassword);
        buf.writeVarInt(p.callState);
        buf.writeUtf(p.callerKey, 64);
        buf.writeBoolean(p.outgoing);
        buf.writeUtf(p.peer, 64);
        buf.writeUtf(p.notice, 128);
        buf.writeCollection(p.devices, (b, e) -> e.write(b));
        buf.writeCollection(p.missed, (b, m) -> m.write(b));
        buf.writeCollection(p.log, (b, l) -> l.write(b));
        buf.writeCollection(p.visitors, (b, m) -> m.write(b));
        buf.writeMap(p.settings, (b, k) -> b.writeUtf(k, 32), (b, v) -> b.writeUtf(v, 256));
        buf.writeLongArray(p.energy);
        buf.writeCollection(p.records, (b, m) -> m.write(b));
    }

    public static WallpadDataPacket decode(FriendlyByteBuf buf) {
        return new WallpadDataPacket(
            buf.readBlockPos(),
            buf.readBoolean(),
            buf.readVarInt(),
            buf.readUtf(64),
            buf.readBoolean(),
            buf.readVarInt(),
            buf.readUtf(64),
            buf.readBoolean(),
            buf.readUtf(64),
            buf.readUtf(128),
            buf.readList(DeviceEntry::read),
            buf.readList(MissedCall::read),
            buf.readList(IntercomLine::read),
            buf.readList(MissedCall::read),
            buf.readMap(b -> b.readUtf(32), b -> b.readUtf(256)),
            buf.readLongArray(),
            buf.readList(MissedCall::read)
        );
    }

    public static void handle(WallpadDataPacket p, Supplier<Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleWallpad(p));
    }
}
