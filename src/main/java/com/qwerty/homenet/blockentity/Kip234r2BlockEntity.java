package com.qwerty.homenet.blockentity;

import com.qwerty.homenet.analog.AnalogBus;
import com.qwerty.homenet.analog.AnalogLobbyEndpoint;
import com.qwerty.homenet.data.ZoneData;
import com.qwerty.homenet.guard.GuardSystem;
import com.qwerty.homenet.intercom.CallState;
import com.qwerty.homenet.intercom.DoorStatus;
import com.qwerty.homenet.intercom.IntercomCaller;
import com.qwerty.homenet.intercom.IntercomLine;
import com.qwerty.homenet.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class Kip234r2BlockEntity extends Ip203apbBlockEntity {
    private final Map<String, Kip234r2BlockEntity.Session> held = new HashMap<>();
    private String requested = "";
    private String latest = "";
    private long chordStart = -1L;
    private boolean clientLobby;
    private int endToneTicks;

    public Kip234r2BlockEntity(BlockPos p, BlockState s) {
        super(ModBlockEntities.KIP_234R2.get(), p, s);
        this.volume = 65;
    }

    @Override
    public AnalogBus analogBus() {
        return AnalogBus.COMMON_8_WIRE;
    }

    private String source(BlockPos p) {
        if (p != null && this.level != null) {
            BlockEntity be = this.level.getBlockEntity(p);
            return be instanceof AnalogLobbyEndpoint ? "lobby" : (p.equals(this.analogStation) ? "guard" : "front");
        } else {
            return "idle";
        }
    }

    private String active() {
        return this.callState == CallState.IDLE ? "idle" : this.source(this.caller);
    }

    private Kip234r2BlockEntity.Session snapshot() {
        return new Kip234r2BlockEntity.Session(this.callState, this.caller, this.incomingKey, this.callTicks, new ArrayList<>(this.log));
    }

    private void restore(Kip234r2BlockEntity.Session s) {
        this.callState = s.state;
        this.caller = s.from;
        this.incomingKey = s.key;
        this.callTicks = s.ticks;
        this.outgoing = false;
        this.log.clear();
        this.log.addAll(s.lines);
        this.setRingingState(this.callState == CallState.RINGING);
    }

    private boolean select(String type) {
        if (this.active().equals(type)) {
            if (this.callState == CallState.RINGING && this.lifted()) {
                this.answer();
            }

            this.changed();
            return true;
        } else {
            Kip234r2BlockEntity.Session target = this.held.remove(type);
            if (target == null) {
                return false;
            } else {
                if (this.callState != CallState.IDLE) {
                    this.held.put(this.active(), this.snapshot());
                }

                this.restore(target);
                if (this.callState == CallState.RINGING && this.lifted()) {
                    this.answer();
                }

                this.changed();
                return true;
            }
        }
    }

    public boolean lobbyAllowed(BlockPos from) {
        return this.level != null
            && ZoneData.connected(this.level, this.worldPosition, from)
            && this.level.getBlockEntity(from) instanceof AnalogLobbyEndpoint p
            && this.level.getBlockEntity(from) instanceof CallerBlockEntity
            && p.supports(this.analogBus())
            && this.guardSystem().compatible(this.manufacturer(), GuardSystem.BUS, p.manufacturer())
            && this.analogStation != null
            && this.analogStation.equals(p.switchboardPosition())
            && this.switchboard() != null;
    }

    @Override
    protected BlockPos ring(BlockPos from, String key, int depth) {
        return this.away && this.lobbyAllowed(from) && this.switchboard() instanceof Am10kpBlockEntity board
            ? board.routeFrontDoor(this, from)
            : super.ring(from, key, depth);
    }

    @Override
    public boolean startRinging(BlockPos from, String key) {
        if (!ZoneData.connected(this.level, this.worldPosition, from)) {
            return false;
        } else {
            String type = this.source(from);
            boolean valid = type.equals("lobby")
                ? this.lobbyAllowed(from)
                : (type.equals("guard") ? this.switchboard() != null && from.equals(this.analogStation) : this.hasLinkedCamera(from));
            if (valid
                && (!this.away || type.equals("front"))
                && !this.held.containsKey(type)
                && (this.callState == CallState.IDLE || !type.equals(this.active()))) {
                this.latest = type;
                if (this.callState == CallState.IDLE) {
                    return super.startRinging(from, key);
                } else {
                    this.held.put(type, new Kip234r2BlockEntity.Session(CallState.RINGING, from.immutable(), key, 0, new ArrayList<>()));
                    this.changed();
                    return true;
                }
            } else {
                return false;
            }
        }
    }

    @Override
    public void handset(ServerPlayer p) {
        if (this.lifted()) {
            this.endToneTicks = 0;
            this.setLifted(false);
            this.hangUp();
            this.setAway(false);
            this.requested = "";
        } else {
            this.setLifted(true);
            if (!this.requested.isEmpty()) {
                this.select(this.requested);
            } else if (!this.latest.isEmpty()) {
                this.select(this.latest);
            }

            if (this.callState == CallState.RINGING) {
                this.answer();
            } else if (this.callState == CallState.IDLE && this.requested.equals("front")) {
                this.frontCall();
            }

            if (this.active().equals("front") && this.callState == CallState.CONNECTED) {
                this.setAway(false);
            }

            this.requested = "";
        }

        this.changed();
    }

    private void frontCall() {
        BlockPos pos = this.linkedCamera();
        if (pos != null
            && this.level.getBlockEntity(pos) instanceof DoorStationBlockEntity door
            && door.callFromReceiver(this) == CallerBlockEntity.CallResult.OK) {
            this.answer();
        }
    }

    private void frontButton() {
        String type = !this.held.containsKey("front")
                || !this.active().equals("lobby") && (this.held.get("front").state != CallState.RINGING || this.latest.equals("lobby"))
            ? (this.held.containsKey("lobby") ? "lobby" : (this.held.containsKey("front") ? "front" : (this.active().equals("lobby") ? "lobby" : "front")))
            : "front";
        if (!this.lifted()) {
            this.requested = type;
            this.changed();
        } else {
            if (!this.select(type)) {
                Kip234r2BlockEntity.Session old = this.callState == CallState.IDLE ? null : this.snapshot();
                String before = this.active();
                if (old != null) {
                    this.held.put(before, old);
                    this.resetCall();
                }

                this.frontCall();
                if (this.callState == CallState.IDLE && old != null) {
                    this.held.remove(before);
                    this.restore(old);
                }

                this.changed();
            }
        }
    }

    private void guardButton(ServerPlayer p) {
        if (!this.lifted()) {
            this.requested = "guard";
            this.changed();
        } else if (!this.select("guard")) {
            Kip234r2BlockEntity.Session old = this.callState == CallState.IDLE ? null : this.snapshot();
            String type = this.active();
            if (old != null) {
                this.held.put(type, old);
                this.resetCall();
            }

            super.callGuard(p, "guard");
            if (this.callState == CallState.IDLE && old != null) {
                this.held.remove(type);
                this.restore(old);
            }

            this.changed();
        }
    }

    @Override
    public void answer() {
        if (this.lifted()) {
            super.answer();
            if (this.callState == CallState.CONNECTED && this.active().equals("front")) {
                this.setAway(false);
            }

            this.changed();
        }
    }

    private void setAway(boolean value) {
        this.away = value;
        if (this.switchboard() != null) {
            this.switchboard().setAway(this.worldPosition, this.unit, this.away);
        }

        this.changed();
    }

    public boolean canOpenLobby() {
        return this.lifted() && this.callState == CallState.CONNECTED && !this.outgoing && this.caller != null && this.lobbyAllowed(this.caller);
    }

    @Override
    public void openDoor() {
        if (this.canOpenLobby()) {
            IntercomCaller c = this.callerBe();
            this.resetCall();
            if (c != null) {
                c.onDoorOpened(this.worldPosition);
            }

            this.changed();
        }
    }

    @Override
    public void handleAction(ServerPlayer p, ReceiverBlockEntity.Action a, BlockPos target, String text) {
        switch (a) {
            case PICKUP:
                this.handset(p);
                break;
            case HANG_UP:
                this.setLifted(false);
                this.hangUp();
                this.setAway(false);
                break;
            case CALL_GUARD:
                this.guardButton(p);
                break;
            case OPEN_DOOR:
                this.openDoor();
                break;
            case ANSWER:
                if (this.lifted()) {
                    this.answer();
                }
            case EMERGENCY:
            case OUTING:
                break;
            case SET_SETTING:
                if (text.equals("front")) {
                    this.frontButton();
                } else if (text.equals("front+guard_start")) {
                    this.chordStart = this.level.getGameTime();
                } else if (text.equals("front+guard_end")) {
                    if (this.chordStart >= 0L && this.level.getGameTime() - this.chordStart >= 10L) {
                        this.setAway(!this.away);
                    }

                    this.chordStart = -1L;
                } else if (text.startsWith("volume=")) {
                    try {
                        int v = Integer.parseInt(text.substring(7));
                        this.volume = v < 48 ? 30 : (v < 83 ? 65 : 100);
                    } catch (NumberFormatException var6) {
                    }
                }
                break;
            default:
                super.handleAction(p, a, target, text);
        }

        this.changed();
    }

    @Override
    public boolean hasCallerSession(BlockPos p) {
        return super.hasCallerSession(p) || this.held.values().stream().anyMatch(s -> p.equals(s.from));
    }

    @Override
    public CallState callerSessionState(BlockPos p) {
        for (Kip234r2BlockEntity.Session s : this.held.values()) {
            if (p.equals(s.from)) {
                return s.state;
            }
        }

        return super.callerSessionState(p);
    }

    @Override
    public void endCallerSession(BlockPos p, boolean missed) {
        if (this.held.values().removeIf(s -> p.equals(s.from))) {
            this.changed();
        } else {
            if (this.callState == CallState.CONNECTED && !this.active().equals("front")) {
                this.endToneTicks = 600;
            }

            super.endCallerSession(p, missed);
            this.changed();
        }
    }

    @Override
    public void addLine(IntercomLine line) {
        String side = line.side();
        String type = side.startsWith("guard") ? "guard" : (side.equals("lobby") ? "lobby" : "front");
        Kip234r2BlockEntity.Session s = this.held.get(type);
        if (s != null) {
            s.lines.add(line);
        } else {
            super.addLine(line);
        }
    }

    @Override
    public void hangUp() {
        ArrayList<Kip234r2BlockEntity.Session> waiting = new ArrayList<>(this.held.values());
        this.held.clear();
        super.hangUp();

        for (Kip234r2BlockEntity.Session s : waiting) {
            this.restore(s);
            super.hangUp();
        }

        this.latest = "";
        this.changed();
    }

    @Override
    public boolean frontLit() {
        return this.level != null && this.level.isClientSide ? super.frontLit() : this.active().equals("front") || this.held.containsKey("front");
    }

    @Override
    public boolean guardLit() {
        return super.guardLit() || this.held.containsKey("guard");
    }

    @Override
    protected int serverLampMode(String lamp) {
        if (lamp.equals("power")) {
            return 1;
        } else if (lamp.equals("away")) {
            return this.away ? 1 : 0;
        } else if (!lamp.equals("lobby") && !lamp.equals("front") && !lamp.equals("guard")) {
            return 0;
        } else {
            Kip234r2BlockEntity.Session waiting = this.held.get(lamp);
            int mode = this.active().equals(lamp) ? (this.callState == CallState.CONNECTED ? 1 : 2) : 0;
            if (waiting != null) {
                mode = waiting.state == CallState.CONNECTED ? Math.max(mode, 1) : 2;
            }

            return mode;
        }
    }

    public boolean lobbyLit() {
        return this.level != null && this.level.isClientSide ? this.clientLobby : this.active().equals("lobby") || this.held.containsKey("lobby");
    }

    @Override
    protected Map<String, String> settingsForClient() {
        Map<String, String> s = super.settingsForClient();
        s.put("kip234r2", "1");
        s.put("source", this.active());
        s.put("lobby_lit", this.lobbyLit() ? "1" : "0");
        s.put("can_open_door", this.canOpenLobby() ? "1" : "0");
        s.put("front_waiting", !this.held.containsKey("front") && !this.held.containsKey("lobby") ? "0" : "1");
        return s;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag t = super.getUpdateTag();
        t.putBoolean("KipLobbyLit", this.lobbyLit());
        return t;
    }

    @Override
    public void load(CompoundTag t) {
        super.load(t);
        this.held.clear();
        this.requested = this.latest = "";
        this.chordStart = -1L;
        this.endToneTicks = 0;
        this.emergency = false;
        this.volume = t.contains("ApVolume") ? (this.volume < 48 ? 30 : (this.volume < 83 ? 65 : 100)) : 65;
        this.clientLobby = t.getBoolean("KipLobbyLit");
    }

    public static void tick(Level l, BlockPos p, BlockState state, Kip234r2BlockEntity b) {
        b.enforceScope();
        b.receiverDoors.tick(l);
        b.syncLampState();
        if (l.getGameTime() % 20L == 0L) {
            b.refreshBoard();
        }

        for (Entry<String, Kip234r2BlockEntity.Session> e : new ArrayList<>(b.held.entrySet())) {
            Kip234r2BlockEntity.Session s = e.getValue();
            int ticks = s.ticks + 1;
            boolean expire = !ZoneData.connected(l, p, s.from)
                || s.state == CallState.RINGING && ticks >= 600
                || s.state == CallState.CONNECTED && ticks >= 3600;
            if (expire) {
                b.held.remove(e.getKey());
                if (l.getBlockEntity(s.from) instanceof IntercomCaller c) {
                    c.onReceiverHangUp(p, s.state == CallState.RINGING ? DoorStatus.NO_ANSWER : DoorStatus.TIMEOUT);
                }

                b.changed();
            } else {
                b.held.put(e.getKey(), new Kip234r2BlockEntity.Session(s.state, s.from, s.key, ticks, s.lines));
            }
        }

        if (b.callState != CallState.IDLE) {
            b.callTicks++;
            if (b.callState == CallState.RINGING && b.callTicks >= 600
                || b.callState == CallState.DIALING && b.callTicks >= 600
                || b.callState == CallState.CONNECTED && b.callTicks >= 3600) {
                IntercomCaller c = b.callerBe();
                b.resetCall();
                if (c != null) {
                    c.onReceiverHangUp(p, DoorStatus.TIMEOUT);
                }

                b.changed();
            }
        }

        String ringing = b.held
            .entrySet()
            .stream()
            .filter(ex -> ex.getValue().state == CallState.RINGING)
            .map(Entry::getKey)
            .findFirst()
            .orElse(b.callState == CallState.RINGING ? b.active() : "");
        boolean frontPending = b.active().equals("front") && b.callState == CallState.RINGING
            || b.held.containsKey("front") && b.held.get("front").state == CallState.RINGING;
        boolean otherPending = b.active().equals("guard") && b.callState == CallState.RINGING
            || b.active().equals("lobby") && b.callState == CallState.RINGING
            || b.held.entrySet().stream().anyMatch(ex -> !ex.getKey().equals("front") && ex.getValue().state == CallState.RINGING);
        if (frontPending) {
            ringing = otherPending && l.getGameTime() % 80L >= 40L ? "guard" : "front";
        }

        if (!ringing.isEmpty()) {
            int t = (int)(l.getGameTime() % 40L);
            float v = b.volume / 100.0F * (b.lifted() ? 0.25F : 1.0F);
            if (ringing.equals("front")) {
                if (t == 0 || t == 8) {
                    l.playSound(null, p, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, v, t == 0 ? 1.19F : 0.94F);
                }
            } else if (t == 0 || t == 5 || t == 10 || t == 15) {
                l.playSound(null, p, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, v, new float[]{1.06F, 1.335F, 1.189F, 1.587F}[t / 5]);
            }
        } else if (b.endToneTicks > 0) {
            b.endToneTicks--;
            if (b.lifted() && l.getGameTime() % 20L == 0L) {
                l.playSound(null, p, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 0.15F, 1.2F);
            }
        } else if (b.lifted() && b.callState == CallState.IDLE && l.getGameTime() % 40L == 0L) {
            l.playSound(null, p, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 0.15F, 0.8F);
        }
    }

    private record Session(CallState state, BlockPos from, String key, int ticks, List<IntercomLine> lines) {
    }
}
