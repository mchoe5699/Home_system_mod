package com.qwerty.homenet.blockentity;

import com.qwerty.homenet.analog.AnalogBus;
import com.qwerty.homenet.guard.GuardExchange;
import com.qwerty.homenet.guard.GuardSystem;
import com.qwerty.homenet.intercom.CallState;
import com.qwerty.homenet.registry.ModBlockEntities;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class Ip203apbBlockEntity extends Ip203apBlockEntity {
    protected boolean emergency;
    protected boolean away;

    public Ip203apbBlockEntity(BlockPos p, BlockState s) {
        this(ModBlockEntities.IP_203APB.get(), p, s);
    }

    protected Ip203apbBlockEntity(BlockEntityType<?> type, BlockPos p, BlockState s) {
        super(type, p, s);
    }

    @Override
    public AnalogBus analogBus() {
        return AnalogBus.COMMON_8_WIRE;
    }

    @Override
    public GuardSystem guardSystem() {
        return GuardSystem.BUS;
    }

    @Override
    public boolean canSetUnitLocally() {
        return false;
    }

    public void refreshBoard() {
        if (this.switchboard() != null) {
            this.switchboard().setEmergency(this.worldPosition, this.unit, this.emergency);
            this.switchboard().setAway(this.worldPosition, this.unit, this.away);
        }
    }

    @Override
    protected int serverLampMode(String lamp) {
        if (lamp.equals("power")) {
            return this.emergency ? 2 : 1;
        } else if (lamp.equals("away")) {
            return this.away ? 1 : 0;
        } else {
            return super.serverLampMode(lamp);
        }
    }

    public boolean emergency() {
        return this.emergency;
    }

    public boolean away() {
        return this.away;
    }

    @Override
    public void callGuard(ServerPlayer p, String mode) {
        if (this.lifted() && !this.isBusy() && !this.unit.isBlank() && this.switchboard() != null) {
            this.caller = this.analogStation;
            this.incomingKey = "guard:" + this.switchboard().modelId();
            this.outgoing = false;
            this.callTicks = 0;
            this.callState = CallState.DIALING;
            this.switchboard().callFrom(this, p);
            this.changed();
        }
    }

    @Override
    public boolean bindSwitchboard(BlockPos p) {
        if (!super.bindSwitchboard(p)) {
            return false;
        } else {
            GuardExchange b = this.switchboard();
            if (b != null) {
                b.setEmergency(this.worldPosition, this.unit, this.emergency);
                b.setAway(this.worldPosition, this.unit, this.away);
            }

            this.changed();
            return true;
        }
    }

    @Override
    public void unlinkSwitchboard() {
        GuardExchange b = this.switchboard();
        if (b != null) {
            b.setEmergency(this.worldPosition, this.unit, false);
            b.setAway(this.worldPosition, this.unit, false);
        }

        super.unlinkSwitchboard();
    }

    @Override
    public void handleAction(ServerPlayer p, ReceiverBlockEntity.Action a, BlockPos target, String text) {
        if (this instanceof Kip234r2BlockEntity
            || !this.lifted()
            || this.callState != CallState.CONNECTED
            || this.outgoing
            || this.caller == null
            || !(this.level.getBlockEntity(this.caller) instanceof Klp203rfBlockEntity)
            || a != ReceiverBlockEntity.Action.OPEN_DOOR && (a != ReceiverBlockEntity.Action.SET_SETTING || !text.equals("front"))) {
            if (a == ReceiverBlockEntity.Action.EMERGENCY) {
                this.emergency = !this.emergency;
                if (this.switchboard() != null) {
                    this.switchboard().setEmergency(this.worldPosition, this.unit, this.emergency);
                }

                this.changed();
            } else if (a == ReceiverBlockEntity.Action.OUTING || a == ReceiverBlockEntity.Action.SET_SETTING && text.equals("front+guard")) {
                this.away = !this.away;
                if (this.switchboard() != null) {
                    this.switchboard().setAway(this.worldPosition, this.unit, this.away);
                }

                this.changed();
            } else {
                super.handleAction(p, a, target, text);
            }
        } else {
            super.openDoor();
            this.changed();
        }
    }

    @Override
    protected Map<String, String> settingsForClient() {
        Map<String, String> s = super.settingsForClient();
        s.put("ip203apb", "1");
        s.put("emergency", this.emergency ? "1" : "0");
        s.put("away", this.away ? "1" : "0");
        return s;
    }

    @Override
    protected void saveAdditional(CompoundTag t) {
        super.saveAdditional(t);
        t.putBoolean("Emergency", this.emergency);
        t.putBoolean("Away", this.away);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag t = super.getUpdateTag();
        t.putBoolean("Emergency", this.emergency);
        t.putBoolean("Away", this.away);
        return t;
    }

    @Override
    public void load(CompoundTag t) {
        super.load(t);
        this.emergency = t.getBoolean("Emergency");
        this.away = t.getBoolean("Away");
    }

    @Override
    public void onBroken() {
        this.unlinkSwitchboard();
        super.onBroken();
    }
}
