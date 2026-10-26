package com.qwerty.homenet.blockentity;

import com.qwerty.homenet.analog.AnalogBus;
import com.qwerty.homenet.analog.AnalogLobbyEndpoint;
import com.qwerty.homenet.block.Kvm906Block;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.data.ZoneData;
import com.qwerty.homenet.guard.GuardExchange;
import com.qwerty.homenet.guard.GuardSystem;
import com.qwerty.homenet.guard.GuardUnitAssignments;
import com.qwerty.homenet.intercom.CallSounds;
import com.qwerty.homenet.intercom.CallState;
import com.qwerty.homenet.intercom.DoorStatus;
import com.qwerty.homenet.intercom.Intercom;
import com.qwerty.homenet.intercom.IntercomCaller;
import com.qwerty.homenet.kvm.KvmDesign;
import com.qwerty.homenet.kvm.KvmModel;
import com.qwerty.homenet.kvm.KvmTelephone;
import com.qwerty.homenet.network.ModNetwork;
import com.qwerty.homenet.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class Kvm906BlockEntity extends ReceiverBlockEntity {
    private final KvmDesign design = new KvmDesign();
    private final KvmTelephone telephone = new KvmTelephone(this);
    private boolean power = true;
    private boolean offHook;
    private int screenTicks;
    private int doorLampTicks;
    private boolean emergency;
    private boolean away;
    private String selected932 = "";
    private boolean guardOutgoing932;
    private final Map<BlockPos, Kvm906BlockEntity.HeldCall> heldCalls = new LinkedHashMap<>();

    public KvmTelephone telephone() {
        return this.telephone;
    }

    public void telephoneUpdated() {
        this.updateWorld();
        this.syncScreens();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        this.telephone.register();
    }

    @Override
    public void onBroken() {
        this.telephone.broken();
        super.onBroken();
    }

    @Override
    public boolean hasCallerSession(BlockPos from) {
        return super.hasCallerSession(from) || this.heldCalls.containsKey(from);
    }

    @Override
    public void endCallerSession(BlockPos from, boolean missed) {
        if (from.equals(this.getCaller())) {
            super.endCallerSession(from, missed);
        } else {
            this.heldCalls.remove(from);
        }

        this.updateWorld();
        this.syncScreens();
    }

    private void parkIntercom() {
        if (this.caller != null && this.callState != CallState.IDLE) {
            this.heldCalls.put(this.caller, new Kvm906BlockEntity.HeldCall(this.callState, this.incomingKey, this.callTicks));
        }

        this.caller = null;
        this.callState = CallState.IDLE;
        this.incomingKey = "";
        this.callTicks = 0;
        this.screenTicks = 0;
        this.setRingingState(false);
    }

    private boolean selectIntercom(String function) {
        boolean guard = function.equals("guard");
        if (this.callState != CallState.IDLE && guard == Intercom.isReceiverKey(this.incomingKey)) {
            if (this.callState == CallState.RINGING) {
                this.answer();
            } else if (!guard) {
                this.screenTicks = 3600;
            }

            this.updateWorld();
            this.syncScreens();
            return true;
        } else {
            Entry<BlockPos, Kvm906BlockEntity.HeldCall> target = this.heldCalls
                .entrySet()
                .stream()
                .filter(e -> guard == Intercom.isReceiverKey(e.getValue().key()))
                .findFirst()
                .orElse(null);
            if (target == null) {
                return false;
            } else {
                BlockPos pos = target.getKey();
                Kvm906BlockEntity.HeldCall saved = target.getValue();
                this.heldCalls.remove(pos);
                this.parkIntercom();
                this.caller = pos;
                this.callState = saved.state();
                this.incomingKey = saved.key();
                this.callTicks = saved.ticks();
                this.outgoing = false;
                this.screenTicks = guard ? 0 : 3600;
                if (this.callState == CallState.RINGING) {
                    this.answer();
                }

                this.updateWorld();
                this.syncScreens();
                return true;
            }
        }
    }

    @Override
    public void hangUp() {
        for (BlockPos pos : List.copyOf(this.heldCalls.keySet())) {
            if (this.level != null && this.level.isLoaded(pos) && this.level.getBlockEntity(pos) instanceof IntercomCaller source) {
                source.onReceiverHangUp(this.worldPosition, DoorStatus.ENDED);
            }
        }

        this.heldCalls.clear();
        super.hangUp();
        this.updateWorld();
    }

    public boolean emergency() {
        return this.emergency;
    }

    public boolean away() {
        return this.away;
    }

    public boolean doorLamp() {
        return this.doorLampTicks > 0;
    }

    public Kvm906BlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.KVM_906.get(), pos, state);
        if (state.getBlock() instanceof Kvm906Block block) {
            this.design.select(block.defaultModel());
        }
    }

    public KvmDesign design() {
        return this.design;
    }

    public boolean offHook() {
        return this.offHook;
    }

    public boolean power() {
        return this.power;
    }

    public boolean screenOn() {
        return this.power && this.screenTicks > 0 && (!this.telephone.active() || this.design.model().is932());
    }

    private void updateWorld() {
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 2);
        }
    }

    public void openModels(ServerPlayer player) {
        ModNetwork.sendTo(player, this.buildPacket(true).withNotice("kvm_models"));
    }

    public boolean changeModel(ServerPlayer player, KvmModel model) {
        if (!this.design.model().sameFamily(model)) {
            return false;
        } else if (!this.isBusy() && this.heldCalls.isEmpty() && !this.telephone.busy()) {
            GuardExchange oldBoard = this.switchboard();
            if (this.emergency && oldBoard != null) {
                oldBoard.setEmergency(this.worldPosition, this.unit, false);
            }

            this.emergency = false;
            this.away = false;
            this.doorLampTicks = 0;
            this.screenTicks = 0;
            this.offHook = false;
            this.design.select(model);
            this.telephone.modelChanged();
            if (this.analogStation != null && this.switchboard() == null) {
                if (oldBoard != null) {
                    oldBoard.onUnlinked(this.worldPosition);
                }

                this.analogStation = null;
            }

            if (model.apartment()) {
                this.unit = this.switchboard() == null ? "" : this.switchboard().assignedUnit(this.worldPosition);
                if (this.switchboard() != null && !this.validAssignedUnit(this.unit) && this.level instanceof ServerLevel sl) {
                    this.unit = GuardUnitAssignments.get(sl).allocate(sl, this.worldPosition, this.roomOnlyUnit());
                    this.switchboard().onLinked(this.worldPosition, this.unit);
                }
            }

            this.register();
            this.updateWorld();
            this.syncScreens();
            ModNetwork.sendTo(player, this.buildPacket(true).withNotice("kvm_model_changed"));
            return true;
        } else {
            player.displayClientMessage(Component.literal("통화를 끝낸 뒤 모델을 변경하세요."), true);
            return false;
        }
    }

    @Override
    public AnalogBus analogBus() {
        return this.design.model().bus;
    }

    @Override
    public GuardSystem guardSystem() {
        return this.design.model().guardSystem();
    }

    @Override
    public String manufacturer() {
        return this.design.model().manufacturer();
    }

    @Override
    public boolean roomOnlyUnit() {
        return this.design.model().roomOnly();
    }

    @Override
    public boolean canSetUnitLocally() {
        return this.design.model() == KvmModel.KVM_906 || this.design.model() == KvmModel.KVM_940;
    }

    @Override
    public boolean bindSwitchboard(BlockPos pos) {
        if (!super.bindSwitchboard(pos)) {
            return false;
        } else {
            GuardExchange board = this.switchboard();
            if (board != null) {
                board.setEmergency(this.worldPosition, this.unit, this.emergency);
                board.setAway(this.worldPosition, this.unit, this.away);
            }

            this.updateWorld();
            this.syncScreens();
            return true;
        }
    }

    @Override
    public void unlinkSwitchboard() {
        GuardExchange board = this.switchboard();
        if (board != null) {
            board.setEmergency(this.worldPosition, this.unit, false);
            board.setAway(this.worldPosition, this.unit, false);
        }

        super.unlinkSwitchboard();
        if (this.design.model().apartment()) {
            this.unit = "";
            this.register();
        }

        this.updateWorld();
        this.syncScreens();
    }

    @Override
    public void setUnit(ServerPlayer player, String raw) {
        if (!this.canSetUnitLocally()) {
            player.displayClientMessage(Component.literal("이 모델의 동·호수는 연결된 경비실기에서만 설정합니다."), true);
        } else {
            super.setUnit(player, raw);
            if (this.switchboard() != null) {
                this.switchboard().onLinked(this.worldPosition, this.unit);
            }
        }
    }

    public String indicator(String function) {
        if (!this.power) {
            return "off";
        } else if (this.design.model().roomOnly() && function.equals("power_round")) {
            return this.indicator("power");
        } else if (function.equals("power")) {
            return this.design.model() == KvmModel.KVM_940R3 && this.emergency ? "blink" : "on";
        } else if (this.design.model().is932() && function.equals("telephone")) {
            return this.telephone.active()
                ? "on"
                : (
                    this.telephone.state() == KvmTelephone.State.RINGING
                        ? (this.callState == CallState.IDLE && this.heldCalls.isEmpty() ? "on" : "blink")
                        : "off"
                );
        } else if (!this.design.model().roomOnly() || !function.equals("lobby") && !function.equals("monitor")) {
            if (function.equals("telephone")) {
                return this.telephone.active() ? "on" : (this.telephone.state() == KvmTelephone.State.RINGING ? "blink" : "off");
            } else if (this.design.model().multiSource()
                && (function.equals("guard") || function.equals("monitor"))
                && this.heldCalls
                    .values()
                    .stream()
                    .anyMatch(c -> c.state() == CallState.RINGING && function.equals("guard") == Intercom.isReceiverKey(c.key()))) {
                return "blink";
            } else if (function.equals("emergency")) {
                return this.emergency ? (this.design.model().busPanel() ? "on" : "blink") : "off";
            } else if (this.design.model().busPanel() && function.equals("guard") && this.selected932.equals("guard")) {
                return "blink";
            } else if (this.design.model().nativeCase()
                && (function.equals("guard") || function.equals("monitor"))
                && this.heldCalls
                    .values()
                    .stream()
                    .anyMatch(c -> c.state() == CallState.CONNECTED && function.equals("guard") == Intercom.isReceiverKey(c.key()))) {
                return "on";
            } else if (function.equals("away")) {
                return this.away ? "on" : "off";
            } else if (function.equals("door")) {
                return this.doorLampTicks > 0 ? "on" : "off";
            } else {
                boolean guard = Intercom.isReceiverKey(this.incomingKey);
                if (function.equals("guard")) {
                    return this.telephone.active() && this.callState == CallState.CONNECTED
                        ? "off"
                        : (
                            !guard || this.callState != CallState.RINGING && this.callState != CallState.DIALING
                                ? (guard && this.callState == CallState.CONNECTED ? "on" : "off")
                                : "blink"
                        );
                } else if (this.telephone.active() && this.callState != CallState.RINGING) {
                    return "off";
                } else {
                    return !guard && this.callState == CallState.RINGING
                        ? "blink"
                        : (!this.screenOn() && (guard || this.callState != CallState.CONNECTED) ? "off" : "on");
                }
            }
        } else {
            boolean lobby = function.equals("lobby");

            for (Entry<BlockPos, Kvm906BlockEntity.HeldCall> e : this.heldCalls.entrySet()) {
                if (!Intercom.isReceiverKey(e.getValue().key()) && lobby == e.getValue().key().equals("lobby")) {
                    return e.getValue().state() == CallState.RINGING ? "blink" : "on";
                }
            }

            if (this.callState != CallState.IDLE && !Intercom.isReceiverKey(this.incomingKey) && lobby == this.incomingKey.equals("lobby")) {
                return this.callState == CallState.RINGING ? "blink" : (this.callState == CallState.CONNECTED ? "on" : "off");
            } else {
                return !lobby && this.screenOn() ? "on" : "off";
            }
        }
    }

    private void toggleEmergency(ServerPlayer player) {
        if (this.power && this.design.model().emergency()) {
            this.emergency = this.design.model().is931() || !this.emergency;
            GuardExchange board = this.switchboard();
            if (board != null) {
                board.setEmergency(this.worldPosition, this.unit, this.emergency);
            } else if (this.emergency) {
                this.guardPending(player);
            }

            this.updateWorld();
        }
    }

    private void toggleAway() {
        if (this.power && this.design.model().away()) {
            this.away = !this.away;
            if (this.switchboard() != null) {
                this.switchboard().setAway(this.worldPosition, this.unit, this.away);
            }

            this.updateWorld();
        }
    }

    public void openEditor(ServerPlayer player) {
        ModNetwork.sendTo(player, this.buildPacket(true).withNotice("kvm_edit"));
    }

    @Override
    protected BlockPos ring(BlockPos from, String key, int depth) {
        if (this.design.model().busPanel()) {
            return this.away && this.lobbyAllowed(from) && this.switchboard() instanceof Am10kpBlockEntity am
                ? am.routeFrontDoor(this, from)
                : super.ring(from, key, depth);
        } else {
            if (this.power
                && this.away
                && !this.design.model().is931()
                && this.design.model().away()
                && this.hasLinkedCamera(from)
                && this.switchboard() instanceof Am10kpBlockEntity am) {
                BlockPos routed = am.routeFrontDoor(this, from);
                if (routed != null) {
                    return routed;
                }
            }

            return super.ring(from, key, depth);
        }
    }

    @Override
    public boolean startRinging(BlockPos from, String key) {
        if (this.power && this.level != null && ZoneData.connected(this.level, this.worldPosition, from)) {
            BlockEntity source = this.level.getBlockEntity(from);
            boolean door = source instanceof DoorStationBlockEntity camera && camera.hasReceiver(this.worldPosition);
            boolean guard = this.analogStation != null && this.analogStation.equals(from) && this.switchboard() != null;
            boolean lobby = this.design.model().lobby()
                && source instanceof AnalogLobbyEndpoint panel
                && panel.supports(this.analogBus())
                && this.guardSystem().compatible(this.manufacturer(), GuardSystem.BUS, panel.manufacturer())
                && source instanceof CallerBlockEntity
                && this.analogStation != null
                && this.analogStation.equals(panel.switchboardPosition())
                && this.switchboard() != null;
            if (!door && !guard && !lobby) {
                return false;
            } else if (!this.design.model().busPanel() || !this.away || !guard && !lobby) {
                if (!this.design.model().multiSource() || !this.isBusy()) {
                    return super.startRinging(from, key);
                } else if (!from.equals(this.caller) && !this.heldCalls.containsKey(from) && this.heldCalls.size() < 4) {
                    this.heldCalls.put(from.immutable(), new Kvm906BlockEntity.HeldCall(CallState.RINGING, key, 0));
                    if (this.design.model().nativeCase() && !Intercom.isReceiverKey(key)) {
                        this.screenTicks = 600;
                    }

                    this.updateWorld();
                    this.syncScreens();
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    protected void onIncoming(String key) {
        if (this.design.model().busPanel()) {
            this.guardOutgoing932 = false;
        }

        this.screenTicks = Intercom.isReceiverKey(key) ? 0 : 600;
        this.updateWorld();
    }

    @Override
    protected float ringVolume() {
        return this.design.number("volume") * 0.25F;
    }

    @Override
    public void answer() {
        if (this.power && this.offHook) {
            super.answer();
            if (this.design.model().busPanel() && this.callState == CallState.CONNECTED) {
                this.selected932 = "";
                if (this.away) {
                    this.away = false;
                    if (this.switchboard() != null) {
                        this.switchboard().setAway(this.worldPosition, this.unit, false);
                    }
                }

                if (this.emergency && Intercom.isReceiverKey(this.incomingKey)) {
                    this.emergency = false;
                    if (this.switchboard() != null) {
                        this.switchboard().setEmergency(this.worldPosition, this.unit, false);
                    }
                }
            }

            if (this.callState == CallState.CONNECTED) {
                this.screenTicks = Intercom.isReceiverKey(this.incomingKey) ? 0 : 3600;
            }

            this.updateWorld();
        }
    }

    public boolean canOpenDoor() {
        if (this.power && !this.telephone.active() && this.callState == CallState.CONNECTED && !this.outgoing && this.design.model().keys.contains("door")) {
            IntercomCaller source = this.callerBe();
            if (this.design.model() != KvmModel.KVM_904 && this.design.model() != KvmModel.KVM_940) {
                return !this.design.model().lobby()
                    ? false
                    : source instanceof AnalogLobbyEndpoint lobby
                        && lobby.supports(this.analogBus())
                        && this.analogStation != null
                        && this.analogStation.equals(lobby.switchboardPosition())
                        && this.switchboard() != null;
            } else {
                return source instanceof DoorStationBlockEntity camera
                    && camera.kind() == DeviceRegistry.Kind.DOOR_CAMERA
                    && camera.hasReceiver(this.worldPosition);
            }
        } else {
            return false;
        }
    }

    @Override
    public void openDoor() {
        if (this.canOpenDoor()) {
            super.openDoor();
            this.doorLampTicks = 60;
            this.updateWorld();
        }
    }

    public boolean lobbyAllowed(BlockPos from) {
        return this.design.model().busPanel()
            && this.level != null
            && this.level.getBlockEntity(from) instanceof AnalogLobbyEndpoint panel
            && panel.supports(this.analogBus())
            && this.analogStation != null
            && this.analogStation.equals(panel.switchboardPosition())
            && this.switchboard() != null;
    }

    private void clear932Flags() {
        this.selected932 = "";
        this.emergency = false;
        if (!this.design.model().manual932()) {
            this.away = false;
        }

        if (this.switchboard() != null) {
            this.switchboard().setEmergency(this.worldPosition, this.unit, false);
            this.switchboard().setAway(this.worldPosition, this.unit, this.away);
        }
    }

    private boolean select932Front() {
        boolean currentFront = this.callState != CallState.IDLE && !Intercom.isReceiverKey(this.incomingKey);
        Entry<BlockPos, Kvm906BlockEntity.HeldCall> target = this.heldCalls
            .entrySet()
            .stream()
            .filter(e -> !Intercom.isReceiverKey(e.getValue().key()))
            .sorted(Comparator.comparingInt(e -> this.level.getBlockEntity(e.getKey()) instanceof AnalogLobbyEndpoint ? 0 : 1))
            .findFirst()
            .orElse(null);
        if (target != null) {
            BlockPos at = target.getKey();
            Kvm906BlockEntity.HeldCall saved = target.getValue();
            this.heldCalls.remove(at);
            this.parkIntercom();
            this.caller = at;
            this.incomingKey = saved.key();
            this.callState = saved.state();
            this.callTicks = saved.ticks();
            this.outgoing = false;
            this.screenTicks = 3600;
            if (this.offHook && this.callState == CallState.RINGING) {
                this.answer();
            }

            this.updateWorld();
            this.syncScreens();
            return true;
        } else {
            return currentFront && this.selectIntercom("monitor");
        }
    }

    private boolean selectDefaultBus() {
        if (this.selectIntercom("guard")) {
            return true;
        } else {
            return this.caller != null && this.level.getBlockEntity(this.caller) instanceof AnalogLobbyEndpoint
                ? this.selectIntercom("monitor")
                : this.select932Front();
        }
    }

    private boolean select936(String function) {
        boolean lobby = function.equals("lobby");
        if (this.caller != null
            && !Intercom.isReceiverKey(this.incomingKey)
            && lobby == (this.level.getBlockEntity(this.caller) instanceof AnalogLobbyEndpoint)) {
            if (this.offHook && this.callState == CallState.RINGING) {
                this.answer();
            } else if (!lobby) {
                this.screenTicks = 3600;
            }

            return true;
        } else {
            Entry<BlockPos, Kvm906BlockEntity.HeldCall> target = this.heldCalls
                .entrySet()
                .stream()
                .filter(e -> !Intercom.isReceiverKey(e.getValue().key()) && lobby == (this.level.getBlockEntity(e.getKey()) instanceof AnalogLobbyEndpoint))
                .findFirst()
                .orElse(null);
            if (target == null) {
                return false;
            } else {
                BlockPos at = target.getKey();
                Kvm906BlockEntity.HeldCall saved = target.getValue();
                this.heldCalls.remove(at);
                this.parkIntercom();
                this.caller = at;
                this.incomingKey = saved.key();
                this.callState = saved.state();
                this.callTicks = saved.ticks();
                this.screenTicks = 3600;
                if (this.offHook && this.callState == CallState.RINGING) {
                    this.answer();
                }

                return true;
            }
        }
    }

    private void press932(ServerPlayer player, String function) {
        switch (function) {
            case "emergency":
                this.toggleEmergency(player);
                break;
            case "away":
                this.toggleAway();
                break;
            case "door":
                this.openDoor();
                break;
            case "guard":
                this.telephone.selectIntercom();
                if (!this.offHook) {
                    this.selected932 = "guard";
                    if (this.callState != CallState.IDLE) {
                        this.selectIntercom("guard");
                    }
                } else {
                    this.callGuard(player, "guard");
                }
                break;
            case "lobby":
                this.selected932 = "lobby";
                this.select936("lobby");
                break;
            case "monitor":
                this.telephone.selectIntercom();
                this.selected932 = "monitor";
                if (this.design.model().roomOnly() ? !this.select936("monitor") : !this.select932Front()) {
                    if (this.screenOn()) {
                        this.screenTicks = 0;
                    } else if (this.camera() != null) {
                        this.screenTicks = this.offHook ? 3600 : 600;
                    }
                }
                break;
            case "line":
                if (this.offHook) {
                    if (this.callState != CallState.IDLE) {
                        this.parkIntercom();
                    }

                    this.telephone.select();
                    this.selected932 = "";
                }
                break;
            case "hold":
                boolean resume = this.telephone.held();
                if (resume && this.callState != CallState.IDLE) {
                    this.parkIntercom();
                }

                this.telephone.hold();
                break;
            case "redial":
                if (this.offHook) {
                    if (this.callState != CallState.IDLE) {
                        this.parkIntercom();
                    }

                    this.telephone.select();
                    this.telephone.redial(player);
                }
        }

        this.updateWorld();
        this.syncScreens();
    }

    private void pickUp(ServerPlayer player) {
        if (this.power) {
            this.offHook = !this.offHook;
            if (this.design.model().busPanel()) {
                if (this.offHook) {
                    if (this.selected932.equals("guard")) {
                        this.selected932 = "";
                        this.callGuard(player, "guard");
                    } else if (!this.selected932.equals("monitor") && !this.selected932.equals("lobby")) {
                        if (!this.design.model().is932() || this.telephone.state() != KvmTelephone.State.RINGING && this.callState != CallState.IDLE) {
                            if (!this.design.model().is932()) {
                                if (!this.selectDefaultBus()) {
                                    this.answer();
                                }
                            } else {
                                this.answer();
                            }
                        } else {
                            this.telephone.select();
                        }
                    } else {
                        this.telephone.selectIntercom();
                        if (this.design.model().roomOnly()) {
                            if (!this.select936(this.selected932)) {
                                this.answer();
                            }
                        } else if (!this.select932Front()) {
                            this.answer();
                        }
                    }
                } else {
                    this.telephone.hangUp();
                    this.hangUp();
                    this.screenTicks = 0;
                    this.clear932Flags();
                }

                this.updateWorld();
            } else {
                if (this.offHook) {
                    if (!this.design.model().keypad() || this.telephone.state() != KvmTelephone.State.RINGING && this.callState != CallState.IDLE) {
                        if (this.design.model().is931() && this.callState == CallState.IDLE) {
                            if (this.screenOn()
                                && this.linkedCamera() != null
                                && this.level.getBlockEntity(this.linkedCamera()) instanceof DoorStationBlockEntity d) {
                                d.callFromReceiver(this);
                                this.answer();
                            } else {
                                this.callGuard(player, "guard");
                            }
                        } else {
                            this.answer();
                        }
                    } else {
                        this.telephone.select();
                    }
                } else {
                    this.telephone.hangUp();
                    this.hangUp();
                    this.screenTicks = 0;
                    if ((this.design.model().lobby() || this.design.model().is931()) && this.emergency) {
                        this.emergency = false;
                        if (this.switchboard() != null) {
                            this.switchboard().setEmergency(this.worldPosition, this.unit, false);
                        }
                    }
                }

                this.updateWorld();
            }
        }
    }

    private void guardPending(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("gui.qwertys_homenet.kvm.guard_pending"), true);
    }

    @Override
    public void callGuard(ServerPlayer player, String mode) {
        if (this.power && this.offHook && this.design.model().guard()) {
            GuardExchange board = this.switchboard();
            if (board == null) {
                this.guardPending(player);
            } else if (this.unit.isEmpty()) {
                player.displayClientMessage(Component.literal("경비실 연결과 동·호수 설정을 먼저 완료하세요."), true);
            } else {
                if (this.design.model().multiSource()) {
                    if (this.design.model().keypad()) {
                        this.telephone.selectIntercom();
                    }

                    if (this.selectIntercom("guard")) {
                        return;
                    }

                    if (this.isBusy()) {
                        this.parkIntercom();
                    }
                }

                if (this.beginSwitchboardCall(this.analogStation)) {
                    if (this.design.model().busPanel()) {
                        this.guardOutgoing932 = true;
                    }

                    board.callFrom(this, player);
                }
            }
        }
    }

    public boolean beginSwitchboardCall(BlockPos station) {
        if (this.power
            && this.offHook
            && this.callState == CallState.IDLE
            && this.analogStation != null
            && this.analogStation.equals(station)
            && this.switchboard() != null) {
            this.caller = station.immutable();
            this.incomingKey = "guard:" + this.switchboard().modelId();
            this.outgoing = false;
            this.callTicks = 0;
            this.callState = CallState.DIALING;
            this.screenTicks = 0;
            this.updateWorld();
            this.syncScreens();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean acceptSwitchboardCall(BlockPos station) {
        if (this.power
            && this.offHook
            && this.callState == CallState.DIALING
            && this.caller != null
            && this.caller.equals(station)
            && this.switchboard() != null) {
            this.callState = CallState.CONNECTED;
            this.callTicks = 0;
            this.updateWorld();
            this.syncScreens();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void callLobby(ServerPlayer player, String number) {
        this.guardPending(player);
    }

    @Override
    public void dial(ServerPlayer player, String raw) {
        if (this.design.model().keypad()) {
            this.telephone.dial(player, raw);
        } else {
            this.guardPending(player);
        }
    }

    public void pressButton(ServerPlayer player, int slot) {
        if (this.power && slot >= 0 && slot < this.design.number("buttons")) {
            if (this.design.model().busPanel()) {
                this.press932(player, this.design.get("key" + slot));
            } else {
                String var3 = this.design.get("key" + slot);
                switch (var3) {
                    case "screen":
                    case "monitor":
                        if (this.design.model().is931() && this.offHook && this.selectIntercom("monitor")) {
                            this.updateWorld();
                            return;
                        }

                        if (this.design.model().keypad()) {
                            boolean held = this.telephone.active();
                            this.telephone.selectIntercom();
                            if (this.selectIntercom("monitor")) {
                                return;
                            }

                            if (this.callState == CallState.RINGING && !Intercom.isReceiverKey(this.incomingKey)) {
                                this.answer();
                                this.updateWorld();
                                return;
                            }

                            if (held && this.callState == CallState.CONNECTED) {
                                this.screenTicks = 3600;
                                this.updateWorld();
                                return;
                            }
                        }

                        if (this.screenOn()) {
                            this.screenTicks = 0;
                        } else if (this.camera() != null) {
                            this.screenTicks = this.offHook ? 3600 : 600;
                        } else {
                            player.displayClientMessage(Component.translatable("gui.qwertys_homenet.kvm.no_camera"), true);
                        }

                        this.updateWorld();
                        break;
                    case "guard":
                        if (this.design.model().keypad()) {
                            this.telephone.selectIntercom();
                        }

                        if (this.design.model().keypad() && this.callState == CallState.RINGING && Intercom.isReceiverKey(this.incomingKey)) {
                            this.answer();
                        } else {
                            this.callGuard(player, "guard");
                        }
                        break;
                    case "emergency":
                        this.toggleEmergency(player);
                        break;
                    case "away":
                        this.toggleAway();
                        break;
                    case "door":
                        this.openDoor();
                }
            }
        }
    }

    @Override
    public void handleAction(ServerPlayer player, ReceiverBlockEntity.Action action, BlockPos target, String text) {
        switch (action) {
            case REFRESH:
            default:
                break;
            case SET_UNIT:
                this.setUnit(player, text);
                break;
            case PICKUP:
                this.pickUp(player);
                break;
            case ANSWER:
                if (this.design.model().busPanel()) {
                    if (!this.offHook) {
                        this.pickUp(player);
                    }
                } else if (this.power) {
                    this.offHook = true;
                    if (this.telephone.state() == KvmTelephone.State.RINGING) {
                        this.telephone.select();
                    } else if (this.design.model().is931() && this.callState == CallState.IDLE) {
                        if (this.screenOn()
                            && this.linkedCamera() != null
                            && this.level.getBlockEntity(this.linkedCamera()) instanceof DoorStationBlockEntity d) {
                            d.callFromReceiver(this);
                            this.answer();
                        } else {
                            this.callGuard(player, "guard");
                        }
                    } else {
                        this.answer();
                    }
                }
                break;
            case HANG_UP:
                if (this.design.model().busPanel()) {
                    this.clear932Flags();
                }

                if (this.offHook && this.design.model().is931() && this.emergency) {
                    this.emergency = false;
                    if (this.switchboard() != null) {
                        this.switchboard().setEmergency(this.worldPosition, this.unit, false);
                    }
                }

                this.offHook = false;
                this.telephone.hangUp();
                this.hangUp();
                this.screenTicks = 0;
                this.updateWorld();
                break;
            case OPEN_DOOR:
                this.openDoor();
                break;
            case SEND_MESSAGE:
                if (this.offHook && this.power) {
                    if (this.telephone.active()) {
                        this.telephone.sendMessage(player, text);
                    } else {
                        this.sendMessage(player, text);
                    }
                }
                break;
            case CALL_GUARD:
                this.callGuard(player, "guard");
                break;
            case CALL_LOBBY:
                this.guardPending(player);
                break;
            case DIAL:
                this.dial(player, text);
                break;
            case EMERGENCY:
                this.toggleEmergency(player);
                break;
            case OUTING:
                this.toggleAway();
                break;
            case SET_SETTING:
                String[] pair = text.split("=", 2);
                if (pair.length == 2) {
                    if (pair[0].equals("phone_number")) {
                        if (!this.telephone.setNumber(pair[1])) {
                            player.displayClientMessage(Component.literal("국선 번호는 중복 없이 숫자 1~16자리로 설정하세요. 통화 중에는 변경할 수 없습니다."), true);
                        }
                    } else if (pair[0].equals("phone_mode")) {
                        this.telephone.setMode(pair[1]);
                    } else if (pair[0].equals("dial_key")) {
                        this.telephone.key(player, pair[1]);
                    } else if (pair[0].equals("hook") && this.design.model().keypad()) {
                        this.telephone.hook();
                        if (!this.telephone.active() && this.callState == CallState.RINGING) {
                            this.answer();
                        }
                    } else if (pair[0].equals("button") && pair[1].matches("[0-9]")) {
                        this.pressButton(player, Integer.parseInt(pair[1]));
                    } else if (pair[0].equals("power") && pair[1].matches("[01]")) {
                        this.power = pair[1].equals("1");
                        if (!this.power) {
                            this.telephone.hangUp();
                            this.hangUp();
                            this.screenTicks = 0;
                            this.offHook = false;
                            this.emergency = false;
                            if (this.switchboard() != null) {
                                this.switchboard().setEmergency(this.worldPosition, this.unit, false);
                            }
                        }

                        this.updateWorld();
                    } else if (this.design.set(pair[0], pair[1])) {
                        this.updateWorld();
                    } else {
                        player.displayClientMessage(Component.translatable("gui.qwertys_homenet.kvm.invalid_setting"), true);
                    }
                }
        }

        this.syncScreens();
    }

    @Nullable
    private BlockPos camera() {
        if (this.caller != null && !ZoneData.connected(this.level, this.worldPosition, this.caller)) {
            return null;
        } else if (this.level instanceof ServerLevel sl) {
            if (this.caller != null && sl.isLoaded(this.caller) && sl.getBlockEntity(this.caller) instanceof DoorStationBlockEntity door) {
                return door.isDoorPhone() ? null : this.caller;
            } else if (!(this.caller != null && sl.isLoaded(this.caller) && sl.getBlockEntity(this.caller) instanceof AnalogLobbyEndpoint lobby)) {
                BlockPos linked = this.linkedCamera();
                return linked != null && sl.getBlockEntity(linked) instanceof DoorStationBlockEntity d && !d.isDoorPhone() ? linked : null;
            } else if (this.switchboard() instanceof Kam305BlockEntity kam && !kam.allowsLobbyVideo(this.unit, lobby)) {
                return null;
            } else {
                BlockPos signal = lobby.cameraPosition();
                return signal != null && sl.isLoaded(signal) ? signal : null;
            }
        } else {
            return null;
        }
    }

    @Override
    protected Map<String, String> settingsForClient() {
        Map<String, String> settings = this.design.snapshot();
        settings.put("kvm", "1");
        settings.put("power", this.power ? "1" : "0");
        settings.put("offhook", this.offHook ? "1" : "0");
        settings.put("screen", this.screenOn() ? "1" : "0");
        settings.put("emergency", this.emergency ? "1" : "0");
        settings.put("away", this.away ? "1" : "0");
        settings.put("room_only", this.roomOnlyUnit() ? "1" : "0");
        settings.put("unit_edit", this.canSetUnitLocally() ? "1" : "0");
        settings.put("board", !this.design.model().guard() ? "경비 기능 없음" : (this.switchboard() == null ? "미연결" : this.switchboard().modelId()));
        settings.put("guard_system", this.guardSystem().name());
        settings.put("manufacturer", this.manufacturer());
        IntercomCaller source = this.callerBe();
        settings.put(
            "call_source",
            source instanceof GuardExchange
                ? "guard"
                : (
                    source instanceof AnalogLobbyEndpoint
                        ? "lobby"
                        : (
                            source instanceof DoorStationBlockEntity d
                                ? (d.isDoorPhone() ? "doorphone" : "camera")
                                : (source instanceof ReceiverBlockEntity ? "unit" : "")
                        )
                )
        );
        settings.put("can_open_door", this.canOpenDoor() ? "1" : "0");
        settings.put("linked_doors", Integer.toString(this.getReceiverDoors().size()));
        this.telephone.settings(settings);

        for (String key : List.of("emergency", "away", "monitor", "guard", "door", "power", "telephone", "lobby")) {
            settings.put("led_" + key, this.indicator(key));
        }

        BlockPos camera = this.camera();
        if (camera != null) {
            settings.put("camera", Long.toString(camera.asLong()));
        }

        return settings;
    }

    @Override
    protected void resetCall() {
        super.resetCall();
        this.screenTicks = 0;
        this.updateWorld();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, Kvm906BlockEntity be) {
        be.enforceScope();
        be.receiverDoors.tick(level);
        be.telephone.tick();
        if (be.doorLampTicks > 0 && --be.doorLampTicks == 0) {
            be.updateWorld();
            be.syncScreens();
        }

        if (be.emergency && be.power && !be.design.model().is931() && level.getGameTime() % 10L == 0L) {
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 1.0F, level.getGameTime() % 20L == 0L ? 0.65F : 1.3F);
        }

        if (be.offHook && !be.design.model().keypad() && !be.design.model().busPanel() && !be.telephone.active() && be.callState == CallState.RINGING) {
            be.answer();
            be.syncScreens();
        }

        for (Entry<BlockPos, Kvm906BlockEntity.HeldCall> entry : new ArrayList<>(be.heldCalls.entrySet())) {
            Kvm906BlockEntity.HeldCall saved = entry.getValue();
            if (ZoneData.connected(level, pos, entry.getKey())
                && level.isLoaded(entry.getKey())
                && level.getBlockEntity(entry.getKey()) instanceof IntercomCaller) {
                if (saved.state() == CallState.RINGING) {
                    if (be.design.model().busPanel()) {
                        CallSounds.incoming(level, pos, Intercom.isReceiverKey(saved.key()) || saved.key().equals("lobby"), saved.ticks(), be.ringVolume());
                    } else if (saved.ticks() % 40 == 0) {
                        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 0.35F, 1.4F);
                    }

                    if (saved.ticks() >= 600) {
                        be.heldCalls.remove(entry.getKey());
                        ((IntercomCaller)level.getBlockEntity(entry.getKey())).onReceiverHangUp(pos, DoorStatus.NO_ANSWER);
                        be.updateWorld();
                        be.syncScreens();
                    } else {
                        be.heldCalls.put(entry.getKey(), new Kvm906BlockEntity.HeldCall(saved.state(), saved.key(), saved.ticks() + 1));
                    }
                }
            } else {
                be.heldCalls.remove(entry.getKey());
                be.updateWorld();
            }
        }

        if (be.design.model().nativeCase() && be.callState == CallState.RINGING && be.callTicks >= 599) {
            IntercomCaller c = be.callerBe();
            be.stopRinging(true);
            if (c != null) {
                c.onReceiverHangUp(pos, DoorStatus.NO_ANSWER);
            }

            be.updateWorld();
        } else {
            if (be.screenTicks > 0 && --be.screenTicks == 0) {
                be.updateWorld();
                be.syncScreens();
            }

            if (be.callState == CallState.DIALING) {
                be.callTicks++;
                if (be.callTicks % 40 == 1) {
                    level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 0.35F, 0.8F);
                }

                if (be.callTicks >= 600 || be.callerBe() == null) {
                    be.hangUp();
                    be.syncScreens();
                }
            } else if (be.callState == CallState.CONNECTED) {
                be.callTicks++;
                if (be.design.model().busPanel()
                    && !(be.callerBe() instanceof DoorStationBlockEntity)
                    && be.callTicks >= (be.design.model().is932() && (!be.guardOutgoing932 || !Intercom.isReceiverKey(be.incomingKey)) ? 300 : 180) * 20) {
                    be.hangUp();
                    be.syncScreens();
                    return;
                }

                if (be.callTicks % 20 == 0 && be.callerBe() == null) {
                    be.resetCall();
                    be.syncScreens();
                }
            } else {
                ReceiverBlockEntity.serverTick(level, pos, state, be);
                if (be.callState == CallState.RINGING && be.callTicks >= 600) {
                    IntercomCaller caller = be.callerBe();
                    be.addMissed();
                    be.resetCall();
                    if (caller != null) {
                        caller.onReceiverHangUp(pos, DoorStatus.NO_ANSWER);
                    }

                    be.syncScreens();
                }
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        this.writeDesign(tag);
        this.productLogo.write(tag);
        tag.putBoolean("Power", this.power);
        tag.putString("SelectedFunction", this.selected932);
        tag.putBoolean("OffHook", this.offHook);
        tag.putInt("ScreenTicks", this.screenTicks);
        tag.putBoolean("Emergency", this.emergency);
        tag.putBoolean("Away", this.away);
        tag.putInt("DoorLamp", this.doorLampTicks);
        ListTag waiting = new ListTag();
        this.heldCalls.forEach((pos, call) -> {
            CompoundTag row = new CompoundTag();
            row.putLong("Pos", pos.asLong());
            row.putString("Key", call.key());
            row.putInt("State", call.state().ordinal());
            waiting.add(row);
        });
        tag.put("HeldCalls", waiting);
        this.telephone.save(tag, true);
        tag.putInt("CallState", this.callState.ordinal());
        tag.putString("IncomingKey", this.incomingKey);
        return tag;
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        this.readDesign(tag);
        this.productLogo.read(tag);
        this.power = !tag.contains("Power") || tag.getBoolean("Power");
        this.selected932 = tag.getString("SelectedFunction");
        this.offHook = tag.getBoolean("OffHook");
        this.screenTicks = tag.getInt("ScreenTicks");
        this.emergency = tag.getBoolean("Emergency");
        this.away = tag.getBoolean("Away");
        this.doorLampTicks = tag.getInt("DoorLamp");
        this.heldCalls.clear();

        for (Tag value : tag.getList("HeldCalls", 10)) {
            CompoundTag row = (CompoundTag)value;
            this.heldCalls.put(BlockPos.of(row.getLong("Pos")), new Kvm906BlockEntity.HeldCall(CallState.byId(row.getInt("State")), row.getString("Key"), 0));
        }

        this.telephone.load(tag, true);
        this.callState = CallState.byId(tag.getInt("CallState"));
        this.incomingKey = tag.getString("IncomingKey");
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) {
            this.handleUpdateTag(packet.getTag());
        }
    }

    private void writeDesign(CompoundTag tag) {
        CompoundTag data = new CompoundTag();
        this.design.snapshot().forEach(data::putString);
        tag.put("KvmDesign", data);
    }

    private void readDesign(CompoundTag tag) {
        CompoundTag data = tag.getCompound("KvmDesign");
        Map<String, String> saved = new HashMap<>();

        for (String key : data.getAllKeys()) {
            saved.put(key, data.getString(key));
        }

        this.design.restore(saved);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.readDesign(tag);
        this.productLogo.read(tag);
        this.power = !tag.contains("Power") || tag.getBoolean("Power");
        this.emergency = this.power && this.design.model().emergency() && tag.getBoolean("Emergency");
        this.away = tag.getBoolean("Away");
        this.heldCalls.clear();
        this.telephone.load(tag, false);
        this.offHook = false;
        this.screenTicks = 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        this.writeDesign(tag);
        this.telephone.save(tag, false);
        tag.putBoolean("Power", this.power);
        tag.putBoolean("Emergency", this.emergency);
        tag.putBoolean("Away", this.away);
    }

    private record HeldCall(CallState state, String key, int ticks) {
    }
}
