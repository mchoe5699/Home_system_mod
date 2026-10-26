package com.qwerty.homenet.intercom;

import com.qwerty.homenet.move.DeviceOffsets;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class CallSounds {
    public static void note(Level l, BlockPos p, SoundEvent sound, float volume, float pitch) {
        if (l != null && !l.isClientSide && !(volume <= 0.0F)) {
            Vec3 v = DeviceOffsets.get(l, p);
            l.playSound(null, p.getX() + 0.5 + v.x, p.getY() + 0.5 + v.y, p.getZ() + 0.5 + v.z, sound, SoundSource.BLOCKS, volume, pitch);
        }
    }

    public static void ringback(Level l, BlockPos p, long ticks, float volume) {
        int t = Math.floorMod(ticks, 40);
        if (t == 0 || t == 8) {
            note(l, p, SoundEvents.NOTE_BLOCK_BIT.value(), volume, 0.8F);
        }
    }

    public static void incoming(Level l, BlockPos p, boolean melody, long ticks, float volume) {
        int t = Math.floorMod(ticks, 40);
        if (melody) {
            if (t == 0 || t == 5 || t == 10 || t == 15) {
                note(l, p, SoundEvents.NOTE_BLOCK_CHIME.value(), volume, new float[]{1.06F, 1.335F, 1.189F, 1.587F}[t / 5]);
            }
        } else if (t == 0 || t == 8) {
            note(l, p, SoundEvents.NOTE_BLOCK_CHIME.value(), volume, t == 0 ? 1.19F : 0.94F);
        }
    }
}
