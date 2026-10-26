package com.qwerty.homenet.data;

import com.qwerty.homenet.intercom.UnitAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class DeviceRegistry extends SavedData {
    private static final String NAME = "qwertys_homenet_devices";
    private final Map<Long, DeviceRegistry.Entry> devices = new LinkedHashMap<>();

    public static DeviceRegistry get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(DeviceRegistry::load, DeviceRegistry::new, "qwertys_homenet_devices");
    }

    public static DeviceRegistry load(CompoundTag tag) {
        DeviceRegistry r = new DeviceRegistry();
        ListTag list = tag.getList("Devices", 10);

        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            BlockPos p = BlockPos.of(c.getLong("Pos"));
            r.devices.put(p.asLong(), new DeviceRegistry.Entry(p, DeviceRegistry.Kind.byId(c.getInt("Kind")), c.getString("Unit")));
        }

        return r;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();

        for (DeviceRegistry.Entry e : this.devices.values()) {
            CompoundTag c = new CompoundTag();
            c.putLong("Pos", e.pos().asLong());
            c.putInt("Kind", e.kind().ordinal());
            c.putString("Unit", e.unit());
            list.add(c);
        }

        tag.put("Devices", list);
        return tag;
    }

    public void register(BlockPos pos, DeviceRegistry.Kind kind, String unit) {
        DeviceRegistry.Entry old = this.devices.get(pos.asLong());
        DeviceRegistry.Entry e = new DeviceRegistry.Entry(pos.immutable(), kind, unit == null ? "" : unit);
        if (!e.equals(old)) {
            this.devices.put(pos.asLong(), e);
            this.setDirty();
        }
    }

    public void relocate(BlockPos from, BlockPos to) {
        DeviceRegistry.Entry e = this.devices.remove(from.asLong());
        if (e != null) {
            this.devices.put(to.asLong(), new DeviceRegistry.Entry(to.immutable(), e.kind(), e.unit()));
            this.setDirty();
        }
    }

    public void remove(BlockPos pos) {
        if (this.devices.remove(pos.asLong()) != null) {
            this.setDirty();
        }
    }

    public List<DeviceRegistry.Entry> all() {
        return new ArrayList<>(this.devices.values());
    }

    public List<BlockPos> receivers(ServerLevel level, BlockPos from, String unit) {
        ZoneData zones = ZoneData.get(level);
        int zone = zones.zoneAt(from);
        String key = normalize(unit);
        List<BlockPos> out = new ArrayList<>();
        if (key.isEmpty()) {
            return out;
        } else {
            for (DeviceRegistry.Entry e : this.devices.values()) {
                if (e.kind().receiver && normalize(e.unit()).equals(key) && zones.sameComplex(from, e.pos())) {
                    out.add(e.pos());
                }
            }

            return out;
        }
    }

    public List<BlockPos> guards(ServerLevel level, BlockPos from, boolean office) {
        ZoneData zones = ZoneData.get(level);
        int zone = zones.zoneAt(from);
        List<BlockPos> all = new ArrayList<>();
        List<BlockPos> match = new ArrayList<>();

        for (DeviceRegistry.Entry e : this.devices.values()) {
            if (e.kind() == DeviceRegistry.Kind.GUARD_CONSOLE && zones.sameComplex(from, e.pos())) {
                all.add(e.pos());
                boolean isOffice = e.unit().contains("관리") || "99".equals(digits(e.unit()));
                if (isOffice == office) {
                    match.add(e.pos());
                }
            }
        }

        return match.isEmpty() ? all : match;
    }

    public List<BlockPos> guardsByNumber(ServerLevel level, BlockPos from, String number) {
        ZoneData zones = ZoneData.get(level);
        int zone = zones.zoneAt(from);
        String want = digits(number);
        List<BlockPos> out = new ArrayList<>();
        if (want.isEmpty()) {
            return out;
        } else {
            for (DeviceRegistry.Entry e : this.devices.values()) {
                if (e.kind() == DeviceRegistry.Kind.GUARD_CONSOLE && zones.sameComplex(from, e.pos()) && digits(e.unit()).equals(want)) {
                    out.add(e.pos());
                }
            }

            return out;
        }
    }

    public List<BlockPos> lobbies(ServerLevel level, BlockPos from) {
        ZoneData zones = ZoneData.get(level);
        int zone = zones.zoneAt(from);
        List<BlockPos> out = new ArrayList<>();

        for (DeviceRegistry.Entry e : this.devices.values()) {
            if (e.kind() == DeviceRegistry.Kind.LOBBY_PHONE && zones.sameComplex(from, e.pos())) {
                out.add(e.pos());
            }
        }

        return out;
    }

    public static String digits(String s) {
        return s.replaceAll("[^0-9]", "").replaceFirst("^0+(?=\\d)", "");
    }

    public static String normalize(String unit) {
        String address = UnitAddress.canonical(unit);
        return !address.isEmpty() ? address : unit.replaceAll("[^0-9A-Za-z가-힣]", "").replace("동", "").replace("호", "").toUpperCase(Locale.ROOT);
    }

    public record Entry(BlockPos pos, DeviceRegistry.Kind kind, String unit) {
        public void write(FriendlyByteBuf buf) {
            buf.writeBlockPos(this.pos);
            buf.writeVarInt(this.kind.ordinal());
            buf.writeUtf(this.unit, 64);
        }

        public static DeviceRegistry.Entry read(FriendlyByteBuf buf) {
            return new DeviceRegistry.Entry(buf.readBlockPos(), DeviceRegistry.Kind.byId(buf.readVarInt()), buf.readUtf(64));
        }
    }

    public static enum Kind {
        WALLPAD(true),
        VIDEO_PHONE(true),
        INTERPHONE(true),
        GUARD_CONSOLE(true),
        LOBBY_PHONE(false),
        DOOR_CAMERA(false),
        DOOR_PHONE(false);

        public final boolean receiver;

        private Kind(boolean receiver) {
            this.receiver = receiver;
        }

        public String key() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        public static DeviceRegistry.Kind byId(int id) {
            DeviceRegistry.Kind[] v = values();
            return id >= 0 && id < v.length ? v[id] : WALLPAD;
        }
    }
}
