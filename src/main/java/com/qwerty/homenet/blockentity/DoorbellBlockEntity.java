package com.qwerty.homenet.blockentity;

import com.qwerty.homenet.intercom.CallSounds;
import com.qwerty.homenet.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class DoorbellBlockEntity extends BlockEntity {
    private final List<BlockPos> receivers = new ArrayList<>();
    private long lastPress = -100L;

    public DoorbellBlockEntity(BlockPos p, BlockState s) {
        super(ModBlockEntities.DOORBELL.get(), p, s);
    }

    public boolean hasReceiver(BlockPos p) {
        return this.receivers.contains(p);
    }

    public boolean toggleReceiver(BlockPos p) {
        if (this.receivers.remove(p)) {
            this.setChanged();
            return false;
        } else if (this.level != null
            && this.worldPosition.closerThan(p, 64.0)
            && this.level.getBlockEntity(p) instanceof CompactInterphoneBlockEntity r
            && r.bellOnly()
            && this.receivers.size() < 32) {
            this.receivers.add(p.immutable());
            this.setChanged();
            return true;
        } else {
            return false;
        }
    }

    public int press() {
        if (this.level != null && !this.level.isClientSide && this.level.getGameTime() - this.lastPress >= 10L) {
            this.lastPress = this.level.getGameTime();
            CallSounds.note(this.level, this.worldPosition, SoundEvents.NOTE_BLOCK_CHIME.value(), 0.65F, 1.19F);
            int count = 0;

            for (BlockPos p : this.receivers) {
                if (this.level.isLoaded(p) && this.level.getBlockEntity(p) instanceof CompactInterphoneBlockEntity r && r.ringBell(this.worldPosition)) {
                    count++;
                }
            }

            return count;
        } else {
            return 0;
        }
    }

    @Override
    public void load(CompoundTag t) {
        super.load(t);
        this.receivers.clear();

        for (long p : t.getLongArray("Receivers")) {
            if (this.receivers.size() < 32) {
                this.receivers.add(BlockPos.of(p));
            }
        }

        this.lastPress = -100L;
    }

    @Override
    protected void saveAdditional(CompoundTag t) {
        super.saveAdditional(t);
        t.putLongArray("Receivers", this.receivers.stream().mapToLong(BlockPos::asLong).toArray());
    }
}
