package com.qwerty.homenet.analog;

import com.qwerty.homenet.data.ZoneData;
import com.qwerty.homenet.intercom.DoorControl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class ReceiverDoors {
    private final List<BlockPos> doors = new ArrayList<>();
    private final Map<BlockPos, Long> closeAt = new HashMap<>();

    public List<BlockPos> positions() {
        return List.copyOf(this.doors);
    }

    public String toggle(Level level, BlockPos owner, BlockPos raw) {
        if (!DoorControl.isDoor(level.getBlockState(raw))) {
            return "not_door";
        } else {
            BlockPos pos = DoorControl.normalize(level.getBlockState(raw), raw);
            if (this.doors.remove(pos)) {
                this.closeOne(level, pos);
                return "unlinked";
            } else if (!ZoneData.connected(level, owner, pos)) {
                return "outside_zone";
            } else if (this.doors.size() >= 16) {
                return "full";
            } else if (!pos.closerThan(owner, 32.0)) {
                return "too_far";
            } else {
                this.doors.add(pos.immutable());
                return "linked";
            }
        }
    }

    private void closeOne(Level level, BlockPos pos) {
        if (this.closeAt.remove(pos) != null && level instanceof ServerLevel sl && sl.isLoaded(pos)) {
            DoorControl.setOpen(sl, pos, false);
        }
    }

    public void clear(Level level) {
        for (BlockPos pos : List.copyOf(this.closeAt.keySet())) {
            this.closeOne(level, pos);
        }

        this.doors.clear();
    }

    public void open(Level level, BlockPos owner) {
        if (level instanceof ServerLevel sl) {
            for (BlockPos pos : this.doors) {
                if (sl.isLoaded(pos) && ZoneData.connected(level, owner, pos)) {
                    if (!DoorControl.isOpen(sl, pos)) {
                        DoorControl.setOpen(sl, pos, true);
                        this.closeAt.put(pos, sl.getGameTime() + 60L);
                    } else if (this.closeAt.containsKey(pos)) {
                        this.closeAt.put(pos, sl.getGameTime() + 60L);
                    }
                }
            }
        }
    }

    public void tick(Level level) {
        if (level instanceof ServerLevel sl) {
            for (Entry<BlockPos, Long> entry : new HashMap<>(this.closeAt).entrySet()) {
                if (sl.getGameTime() >= entry.getValue() && sl.isLoaded(entry.getKey())) {
                    this.closeOne(sl, entry.getKey());
                }
            }
        }
    }

    public void broken(Level level) {
        for (BlockPos pos : List.copyOf(this.closeAt.keySet())) {
            this.closeOne(level, pos);
        }
    }

    public void read(CompoundTag tag) {
        this.doors.clear();

        for (long pos : tag.getLongArray("ReceiverDoors")) {
            if (this.doors.size() < 16) {
                this.doors.add(BlockPos.of(pos));
            }
        }
    }

    public void write(CompoundTag tag) {
        tag.putLongArray("ReceiverDoors", this.doors.stream().mapToLong(BlockPos::asLong).toArray());
    }
}
