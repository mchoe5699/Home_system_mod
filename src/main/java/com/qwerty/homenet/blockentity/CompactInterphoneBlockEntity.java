package com.qwerty.homenet.blockentity;

import com.qwerty.homenet.guard.GuardSystem;
import com.qwerty.homenet.intercom.CallState;
import com.qwerty.homenet.intercom.DoorControl;
import com.qwerty.homenet.registry.ModBlocks;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class CompactInterphoneBlockEntity extends Ip203apbBlockEntity {
    private int bellTicks;
    private int bellLampTicks;

    public CompactInterphoneBlockEntity(BlockEntityType<?> type, BlockPos p, BlockState s) {
        super(type, p, s);
    }

    @Override
    public void load(CompoundTag t) {
        super.load(t);
        if (!this.pchb()) {
            this.emergency = false;
        }

        this.away = false;
        this.bellTicks = this.bellLampTicks = 0;
    }

    @Override
    protected int serverLampMode(String lamp) {
        if (!lamp.equals("power")) {
            return 0;
        } else if (this.pc()) {
            return 0;
        } else if (this.dp()) {
            if (this.getReceiverDoors().stream().anyMatch(p -> this.level instanceof ServerLevel sl && sl.isLoaded(p) && DoorControl.isOpen(sl, p))) {
                return 1;
            } else {
                return this.callState == CallState.IDLE ? 0 : (this.callState == CallState.CONNECTED ? 1 : 2);
            }
        } else {
            return !this.emergency && this.bellLampTicks <= 0 && this.callState != CallState.RINGING && this.callState != CallState.DIALING ? 1 : 2;
        }
    }

    public boolean pc() {
        return this.getBlockState().is(ModBlocks.IP_200PC.get());
    }

    public boolean pchb() {
        return this.getBlockState().is(ModBlocks.IP_200PCHB.get());
    }

    public boolean dp() {
        return this.getBlockState().is(ModBlocks.DP_202H.get());
    }

    public boolean bellOnly() {
        return this.pc() || this.pchb();
    }

    @Override
    public GuardSystem guardSystem() {
        return this.pc() ? GuardSystem.STAR : (this.pchb() ? GuardSystem.BUS : GuardSystem.NONE);
    }

    @Override
    public boolean canSetUnitLocally() {
        return this.pc();
    }

    @Override
    public boolean supportsDoorCommunication() {
        return this.dp();
    }

    @Override
    public boolean hasLinkedCamera(BlockPos p) {
        return this.dp() && super.hasLinkedCamera(p);
    }

    @Override
    public boolean bindSwitchboard(BlockPos p) {
        return !this.dp() && super.bindSwitchboard(p);
    }

    @Override
    public void callGuard(ServerPlayer p, String mode) {
        if (this.pc()) {
            this.callStarGuard(p);
        } else if (this.pchb()) {
            super.callGuard(p, mode);
        }
    }

    @Override
    public boolean startRinging(BlockPos from, String key) {
        if (this.bellOnly()) {
            if (this.analogStation != null && this.analogStation.equals(from)) {
                boolean valid = this.pchb()
                    ? this.switchboard() != null
                    : this.level != null && (isStarConsole(this.level.getBlockEntity(from)) || this.switchboard() != null);
                return valid && super.startRinging(from, key);
            } else {
                return false;
            }
        } else {
            return this.hasLinkedCamera(from) && super.startRinging(from, key);
        }
    }

    public boolean ringBell(BlockPos bell) {
        if (this.bellOnly() && this.level != null && this.level.getBlockEntity(bell) instanceof DoorbellBlockEntity b && b.hasReceiver(this.worldPosition)) {
            this.bellTicks = 10;
            this.bellLampTicks = 40;
            this.chime(1.19F);
            this.changed();
            return true;
        } else {
            return false;
        }
    }

    private void chime(float pitch) {
        if (this.level != null && this.volume > 0) {
            this.level.playSound(null, this.worldPosition, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, this.volume / 100.0F, pitch);
        }
    }

    public void tickBell() {
        if (this.bellLampTicks > 0) {
            this.bellLampTicks--;
        }

        if (this.bellTicks > 0 && --this.bellTicks == 2) {
            this.chime(0.94F);
        }
    }

    @Override
    public void handleAction(ServerPlayer p, ReceiverBlockEntity.Action a, BlockPos target, String text) {
        if (a == ReceiverBlockEntity.Action.EMERGENCY) {
            if (this.pchb()) {
                super.handleAction(p, a, target, text);
            }
        } else if (a == ReceiverBlockEntity.Action.OPEN_DOOR) {
            if (this.dp()) {
                this.openDoor();
                this.changed();
            }
        } else if (a != ReceiverBlockEntity.Action.CALL_GUARD
            && a != ReceiverBlockEntity.Action.OUTING
            && (a != ReceiverBlockEntity.Action.SET_SETTING || !text.equals("front") && !text.startsWith("front+guard"))) {
            super.handleAction(p, a, target, text);
        }
    }

    @Override
    protected Map<String, String> settingsForClient() {
        Map<String, String> s = super.settingsForClient();
        s.put("family", this.pc() ? "ip200pc" : (this.pchb() ? "ip200pchb" : "dp202h"));
        s.put("compact", "1");
        s.put("bell_only", this.bellOnly() ? "1" : "0");
        s.put("bell", this.bellTicks > 0 ? "1" : "0");
        return s;
    }
}
