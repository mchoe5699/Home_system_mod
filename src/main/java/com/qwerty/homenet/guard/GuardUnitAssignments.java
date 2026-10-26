package com.qwerty.homenet.guard;

import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.intercom.UnitAddress;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class GuardUnitAssignments extends SavedData {
    private final Map<Long, String> units = new LinkedHashMap<>();

    public static GuardUnitAssignments get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(GuardUnitAssignments::load, GuardUnitAssignments::new, "qwertys_homenet_guard_units");
    }

    public static GuardUnitAssignments load(CompoundTag tag) {
        GuardUnitAssignments result = new GuardUnitAssignments();

        for (Tag row : tag.getList("Assignments", 10)) {
            CompoundTag entry = (CompoundTag)row;
            result.units.put(entry.getLong("Receiver"), entry.getString("Unit"));
        }

        return result;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        this.units.forEach((pos, unit) -> {
            CompoundTag row = new CompoundTag();
            row.putLong("Receiver", pos);
            row.putString("Unit", unit);
            list.add(row);
        });
        tag.put("Assignments", list);
        return tag;
    }

    public void relocate(BlockPos from, BlockPos to) {
        String n = this.units.remove(from.asLong());
        if (n != null) {
            this.units.put(to.asLong(), n);
            this.setDirty();
        }
    }

    public String allocate(ServerLevel level, BlockPos receiver, boolean roomOnly) {
        if (!roomOnly) {
            return this.allocate(level, receiver);
        } else {
            long key = receiver.asLong();
            HashSet<String> used = new HashSet<>();
            this.units.forEach((p, nx) -> {
                if (p != key) {
                    used.add(nx);
                }
            });

            for (DeviceRegistry.Entry e : DeviceRegistry.get(level).all()) {
                if (!e.pos().equals(receiver)) {
                    used.add(e.unit());
                }
            }

            String previous = this.units.get(key);
            if (UnitAddress.component(previous) && !used.contains(previous)) {
                return previous;
            } else {
                for (int i = 0; i < 9999; i++) {
                    String n = 1 + (i + Math.floorMod(key, 9999)) % 9999 + "";
                    if (!used.contains(n)) {
                        this.units.put(key, n);
                        this.setDirty();
                        return n;
                    }
                }

                throw new IllegalStateException("No available house numbers");
            }
        }
    }

    public String allocate(ServerLevel level, BlockPos receiver) {
        long key = receiver.asLong();
        Set<String> used = new HashSet<>();
        this.units.forEach((pos, unitx) -> {
            if (pos != key) {
                used.add(DeviceRegistry.normalize(unitx));
            }
        });

        for (DeviceRegistry.Entry entry : DeviceRegistry.get(level).all()) {
            if (!entry.pos().equals(receiver)) {
                used.add(DeviceRegistry.normalize(entry.unit()));
            }
        }

        String previous = this.units.get(key);
        if (UnitAddress.full(previous) && !used.contains(DeviceRegistry.normalize(previous))) {
            return previous;
        } else {
            String unit = "";

            for (int attempt = 0; attempt < 1024; attempt++) {
                String candidate = 1 + level.random.nextInt(9999) + "-" + (1 + level.random.nextInt(9999));
                if (!used.contains(DeviceRegistry.normalize(candidate))) {
                    unit = candidate;
                    break;
                }
            }

            if (unit.isEmpty()) {
                label46:
                for (int building = 1; building <= 9999; building++) {
                    for (int house = 1; house <= 9999; house++) {
                        String candidate = building + "-" + house;
                        if (!used.contains(DeviceRegistry.normalize(candidate))) {
                            unit = candidate;
                            break label46;
                        }
                    }
                }
            }

            if (unit.isEmpty()) {
                throw new IllegalStateException("No available guard-assigned unit numbers");
            } else {
                this.units.put(key, unit);
                this.setDirty();
                return unit;
            }
        }
    }
}
