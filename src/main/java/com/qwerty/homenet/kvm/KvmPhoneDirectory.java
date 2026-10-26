package com.qwerty.homenet.kvm;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class KvmPhoneDirectory extends SavedData {
    private final Map<String, BlockPos> numbers = new LinkedHashMap<>();

    public static KvmPhoneDirectory get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(KvmPhoneDirectory::load, KvmPhoneDirectory::new, "qwertys_homenet_telephone");
    }

    public static KvmPhoneDirectory load(CompoundTag tag) {
        KvmPhoneDirectory directory = new KvmPhoneDirectory();

        for (Tag value : tag.getList("Numbers", 10)) {
            CompoundTag row = (CompoundTag)value;
            directory.numbers.put(row.getString("Number"), BlockPos.of(row.getLong("Pos")));
        }

        return directory;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        this.numbers.forEach((number, pos) -> {
            CompoundTag row = new CompoundTag();
            row.putString("Number", number);
            row.putLong("Pos", pos.asLong());
            list.add(row);
        });
        tag.put("Numbers", list);
        return tag;
    }

    public void relocate(BlockPos from, BlockPos to) {
        boolean changed = false;

        for (Entry<String, BlockPos> e : this.numbers.entrySet()) {
            if (e.getValue().equals(from)) {
                e.setValue(to.immutable());
                changed = true;
            }
        }

        if (changed) {
            this.setDirty();
        }
    }

    public BlockPos find(String number) {
        return this.numbers.get(number);
    }

    public boolean claim(String number, BlockPos pos) {
        if (!number.isEmpty() && this.numbers.containsKey(number) && !this.numbers.get(number).equals(pos)) {
            return false;
        } else {
            this.release(pos);
            if (!number.isEmpty()) {
                this.numbers.put(number, pos.immutable());
            }

            this.setDirty();
            return true;
        }
    }

    public void release(BlockPos pos) {
        if (this.numbers.values().removeIf(pos::equals)) {
            this.setDirty();
        }
    }
}
