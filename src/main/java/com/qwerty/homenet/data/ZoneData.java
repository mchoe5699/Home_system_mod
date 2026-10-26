package com.qwerty.homenet.data;

import com.qwerty.homenet.intercom.Intercom;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

public class ZoneData extends SavedData {
    private static final String NAME = "qwertys_homenet_zones";
    private final List<ZoneData.Zone> zones = new ArrayList<>();
    private int nextId = 1;

    public static ZoneData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ZoneData::load, ZoneData::new, "qwertys_homenet_zones");
    }

    public static ZoneData load(CompoundTag tag) {
        ZoneData d = new ZoneData();
        d.nextId = Math.max(1, tag.getInt("NextId"));
        ListTag list = tag.getList("Zones", 10);

        for (int i = 0; i < list.size(); i++) {
            CompoundTag z = list.getCompound(i);
            d.zones
                .add(
                    new ZoneData.Zone(
                        z.getInt("Id"),
                        z.getString("Name"),
                        z.getInt("Color"),
                        z.getInt("X1"),
                        z.getInt("Z1"),
                        z.getInt("X2"),
                        z.getInt("Z2"),
                        z.getString("Complex")
                    )
                );
            d.nextId = Math.max(d.nextId, z.getInt("Id") + 1);
        }

        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("NextId", this.nextId);
        ListTag list = new ListTag();

        for (ZoneData.Zone z : this.zones) {
            CompoundTag c = new CompoundTag();
            c.putString("Complex", z.complex());
            c.putInt("Id", z.id());
            c.putString("Name", z.name());
            c.putInt("Color", z.color());
            c.putInt("X1", z.x1());
            c.putInt("Z1", z.z1());
            c.putInt("X2", z.x2());
            c.putInt("Z2", z.z2());
            list.add(c);
        }

        tag.put("Zones", list);
        return tag;
    }

    public List<ZoneData.Zone> all() {
        return List.copyOf(this.zones);
    }

    @Nullable
    public ZoneData.Zone byId(int id) {
        for (ZoneData.Zone z : this.zones) {
            if (z.id() == id) {
                return z;
            }
        }

        return null;
    }

    public int zoneAt(BlockPos pos) {
        ZoneData.Zone best = null;

        for (ZoneData.Zone z : this.zones) {
            if (z.contains(pos.getX(), pos.getZ()) && (best == null || z.area() < best.area())) {
                best = z;
            }
        }

        return best == null ? 0 : best.id();
    }

    public boolean sameComplex(BlockPos a, BlockPos b) {
        int x = this.zoneAt(a);
        int y = this.zoneAt(b);
        if (x == 0 || y == 0) {
            return false;
        } else if (x == y) {
            return true;
        } else {
            ZoneData.Zone first = this.byId(x);
            ZoneData.Zone second = this.byId(y);
            return !first.complex().isEmpty() && first.complex().equals(second.complex());
        }
    }

    public static boolean connected(Level level, BlockPos a, BlockPos b) {
        return level instanceof ServerLevel sl && a != null && b != null && get(sl).sameComplex(a, b);
    }

    public boolean group(List<Integer> ids, String name) {
        String clean = Intercom.sanitize(name, 32);
        List<Integer> selected = ids.stream().distinct().toList();
        if (!clean.isEmpty() && !selected.isEmpty() && selected.size() <= 4096 && !selected.stream().anyMatch(idx -> this.byId(idx) == null)) {
            for (int id : selected) {
                ZoneData.Zone z = this.byId(id);
                this.update(new ZoneData.Zone(z.id(), z.name(), z.color(), z.x1(), z.z1(), z.x2(), z.z2(), clean));
            }

            return true;
        } else {
            return false;
        }
    }

    public boolean ungroup(List<Integer> ids) {
        if (!ids.isEmpty() && !ids.stream().anyMatch(idx -> this.byId(idx) == null)) {
            for (int id : ids.stream().distinct().toList()) {
                ZoneData.Zone z = this.byId(id);
                this.update(new ZoneData.Zone(z.id(), z.name(), z.color(), z.x1(), z.z1(), z.x2(), z.z2(), ""));
            }

            return true;
        } else {
            return false;
        }
    }

    public ZoneData.Zone add(String name, int color, int x1, int z1, int x2, int z2) {
        ZoneData.Zone z = new ZoneData.Zone(this.nextId++, name, color, x1, z1, x2, z2);
        this.zones.add(z);
        this.setDirty();
        return z;
    }

    public void update(ZoneData.Zone zone) {
        for (int i = 0; i < this.zones.size(); i++) {
            if (this.zones.get(i).id() == zone.id()) {
                this.zones.set(i, zone);
                this.setDirty();
                return;
            }
        }
    }

    public void remove(int id) {
        if (this.zones.removeIf(z -> z.id() == id)) {
            this.setDirty();
        }
    }

    public record Zone(int id, String name, int color, int x1, int z1, int x2, int z2, String complex) {
        public Zone(int id, String name, int color, int x1, int z1, int x2, int z2) {
            this(id, name, color, x1, z1, x2, z2, "");
        }

        public Zone(int id, String name, int color, int x1, int z1, int x2, int z2, String complex) {
            complex = complex == null ? "" : complex;
            int ax = Math.min(x1, x2);
            int bx = Math.max(x1, x2);
            int az = Math.min(z1, z2);
            int bz = Math.max(z1, z2);
            this.id = id;
            this.name = name;
            this.color = color;
            this.x1 = ax;
            this.z1 = az;
            this.x2 = bx;
            this.z2 = bz;
            this.complex = complex;
        }

        public boolean contains(int x, int z) {
            return x >= this.x1 && x <= this.x2 && z >= this.z1 && z <= this.z2;
        }

        public long area() {
            return ((long)this.x2 - this.x1 + 1L) * ((long)this.z2 - this.z1 + 1L);
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.id);
            buf.writeUtf(this.name, 64);
            buf.writeInt(this.color);
            buf.writeInt(this.x1);
            buf.writeInt(this.z1);
            buf.writeInt(this.x2);
            buf.writeInt(this.z2);
            buf.writeUtf(this.complex, 64);
        }

        public static ZoneData.Zone read(FriendlyByteBuf buf) {
            return new ZoneData.Zone(
                buf.readVarInt(), buf.readUtf(64), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readUtf(64)
            );
        }
    }
}
