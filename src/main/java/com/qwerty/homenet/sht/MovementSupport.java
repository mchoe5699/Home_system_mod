package com.qwerty.homenet.sht;

import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.move.DeviceMoves;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

public final class MovementSupport {
    public static boolean additionalProduct(BlockState state) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return id != null && id.getNamespace().equals("homenet_sht101c");
    }

    public static boolean additionalBusy(BlockEntity entity) {
        if (entity instanceof Ht512.Unit h) {
            return h.isInCall();
        } else if (entity instanceof Station s) {
            return s.phone > 0 || s.internal > 0 || !s.docked || s.audio || s.monitor > 0 || s.awayTime > 0 || s.resetTime > 0 || s.test || !s.alarm.isEmpty();
        } else if (entity instanceof Charger.Dock d && !d.stack.isEmpty()) {
            Station s = Portable.base(d.getLevel(), d.stack);
            return s != null && (s.portableActive || s.internal > 0);
        } else {
            return false;
        }
    }

    static void init() {
        DeviceMoves.registerPositionField("base");
    }

    static void relocatePortable(Level level, ItemStack stack) {
        if (level != null && !level.isClientSide && stack.hasTag() && Portable.isPortable(stack)) {
            CompoundTag tag = stack.getTag();
            if (tag.hasUUID("serial") && tag.hasUUID("ticket") && tag.getString("dimension").equals(level.dimension().location().toString())) {
                BlockPos old = BlockPos.of(tag.getLong("base"));
                if (!level.isLoaded(old) || !(level.getBlockEntity(old) instanceof Station s && s.ownsPortable(stack))) {
                    if (level instanceof ServerLevel server) {
                        for (DeviceRegistry.Entry entry : DeviceRegistry.get(server).all()) {
                            if (level.isLoaded(entry.pos())
                                && level.getBlockEntity(entry.pos()) instanceof Station sx
                                && sx.portableRegistered
                                && sx.serial.equals(tag.getUUID("serial"))
                                && (
                                    sx instanceof Station104M.Unit m
                                        ? m.hasPortableTicket(tag.getUUID("ticket"))
                                        : sx.portableTicket.equals(tag.getUUID("ticket"))
                                )) {
                                tag.putLong("base", entry.pos().asLong());
                                return;
                            }
                        }
                    }
                }
            }
        }
    }
}
