package com.qwerty.homenet.blockentity;

import com.qwerty.homenet.analog.AnalogBus;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.data.ZoneData;
import com.qwerty.homenet.guard.GuardExchange;
import com.qwerty.homenet.guard.GuardSystem;
import com.qwerty.homenet.intercom.CallSounds;
import com.qwerty.homenet.intercom.DoorStatus;
import com.qwerty.homenet.intercom.Intercom;
import com.qwerty.homenet.intercom.IntercomCaller;
import com.qwerty.homenet.intercom.IntercomLine;
import com.qwerty.homenet.intercom.UnitAddress;
import com.qwerty.homenet.network.DeviceEntry;
import com.qwerty.homenet.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class Am10kpBlockEntity extends ReceiverBlockEntity implements GuardExchange {
    protected final LinkedHashMap<BlockPos, String> homes = new LinkedHashMap<>();
    protected final LinkedHashMap<BlockPos, String> alarms = new LinkedHashMap<>();
    protected final LinkedHashMap<BlockPos, String> guardAlerts = new LinkedHashMap<>();
    protected final Set<BlockPos> away = new HashSet<>();
    protected final Am10kpBlockEntity.Channel[] channels = new Am10kpBlockEntity.Channel[4];
    protected boolean power = true;
    protected boolean lifted;
    protected boolean muted;
    protected boolean absent;
    protected boolean mutual;
    protected boolean transfer;
    protected int selected = -1;
    protected int volume = 2;
    protected int melody = 1;
    protected int ticks;
    protected int guardTicks;
    protected String digits = "";
    protected String building = "";
    protected String sequence = "";
    protected String registration = "";
    protected String status = "대기";
    protected BlockPos registering;
    protected BlockPos guardPeer;
    protected BlockPos absentTarget;
    protected BlockPos transferSource;
    protected boolean guardConnected;
    protected boolean guardIncoming;
    protected final String[] clientDisplay = new String[]{"", "", "", "", ""};
    private final boolean[] clientLed = new boolean[5];
    private final boolean[] clientModes = new boolean[4];

    public Am10kpBlockEntity(BlockPos p, BlockState s) {
        this(ModBlockEntities.AM_10KP.get(), p, s);
    }

    protected Am10kpBlockEntity(BlockEntityType<?> type, BlockPos p, BlockState s) {
        super(type, p, s);
        this.unit = "101";
    }

    protected int channelLimit() {
        return 4;
    }

    protected int guardLimit() {
        return 100;
    }

    protected int alarmCapacity() {
        return 10;
    }

    @Override
    public DeviceRegistry.Kind kind() {
        return DeviceRegistry.Kind.GUARD_CONSOLE;
    }

    @Override
    public String modelId() {
        return "AM-10KP";
    }

    @Override
    public GuardSystem guardSystem() {
        return GuardSystem.BUS;
    }

    @Override
    public String manufacturer() {
        return "KOCOM";
    }

    @Override
    public boolean supports(AnalogBus b) {
        return b == AnalogBus.COMMON_4_WIRE || b == AnalogBus.COMMON_8_WIRE;
    }

    @Override
    public boolean canSetUnitLocally() {
        return false;
    }

    @Override
    public boolean bindSwitchboard(BlockPos p) {
        return false;
    }

    @Override
    public String callerKey() {
        return "guard:" + this.modelId();
    }

    protected ReceiverBlockEntity home(BlockPos p) {
        return p != null
                && this.level != null
                && ZoneData.connected(this.level, this.worldPosition, p)
                && this.level.isLoaded(p)
                && this.level.getBlockEntity(p) instanceof ReceiverBlockEntity r
                && !(r instanceof GuardExchange)
            ? r
            : null;
    }

    protected Am10kpBlockEntity board(BlockPos p) {
        return p != null
                && this.level != null
                && ZoneData.connected(this.level, this.worldPosition, p)
                && this.level.isLoaded(p)
                && this.level.getBlockEntity(p) instanceof Am10kpBlockEntity b
            ? b
            : null;
    }

    protected boolean valid(BlockPos p) {
        ReceiverBlockEntity r = this.home(p);
        return this.homes.containsKey(p) && r != null && this.worldPosition.equals(r.getAnalogStation()) && r.supportsSwitchboard(this);
    }

    public boolean houseEmergency(BlockPos p) {
        return this.alarms.containsKey(p);
    }

    public boolean houseAway(BlockPos p) {
        return this.away.contains(p);
    }

    public int linkedCount() {
        return this.homes.size();
    }

    public int occupiedChannels() {
        return (int)Arrays.stream(this.channels).filter(Objects::nonNull).count();
    }

    public boolean lifted() {
        return this.lifted;
    }

    public boolean power() {
        return this.power;
    }

    public int alarmCount() {
        return this.alarms.size() + this.guardAlerts.size();
    }

    public boolean alarmMuted() {
        return this.muted;
    }

    @Override
    protected void register() {
        super.register();
        if (this.level instanceof ServerLevel sl) {
            sl.getServer()
                .execute(
                    () -> {
                        for (DeviceRegistry.Entry e : DeviceRegistry.get(sl).all()) {
                            if (e.kind() == DeviceRegistry.Kind.LOBBY_PHONE
                                && sl.isLoaded(e.pos())
                                && sl.getBlockEntity(e.pos()) instanceof Klp203rfBlockEntity panel) {
                                panel.publish();
                            }
                        }
                    }
                );
        }
    }

    public boolean canLink(BlockPos p) {
        return true;
    }

    @Override
    public void onLinked(BlockPos p, String n) {
        if (this.canLink(p)) {
            this.homes.put(p.immutable(), n);
            this.changed();
        }
    }

    @Override
    public void onUnlinked(BlockPos p) {
        this.homes.remove(p);
        this.alarms.remove(p);
        this.away.remove(p);

        for (int i = 0; i < 4; i++) {
            if (this.channels[i] != null && this.channels[i].home.equals(p)) {
                this.end(i, true);
            }
        }

        this.changed();
    }

    @Override
    public String assignedUnit(BlockPos p) {
        return this.homes.getOrDefault(p, "");
    }

    protected int free() {
        for (int i = 0; i < this.channelLimit(); i++) {
            if (this.channels[i] == null) {
                return i;
            }
        }

        return -1;
    }

    protected int find(BlockPos p) {
        for (int i = 0; i < 4; i++) {
            if (this.channels[i] != null && this.channels[i].home.equals(p)) {
                return i;
            }
        }

        return -1;
    }

    protected void changed() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            this.syncScreens();
        }
    }

    @Override
    public void callFrom(ReceiverBlockEntity r, ServerPlayer player) {
        int i = this.free();
        if (this.power && this.valid(r.getBlockPos()) && i >= 0) {
            this.channels[i] = new Am10kpBlockEntity.Channel(r.getBlockPos(), true, this.worldPosition);
            this.status = "세대 호출 " + r.getUnit();
            Am10kpBlockEntity next = this.board(this.absentTarget);
            if (this.absent && next != null && next.power && !next.absent) {
                next.forward(this.channels[i]);
            }

            this.changed();
        } else {
            r.endCallerSession(this.worldPosition, false);
            this.status = "호출 불가: 전원 또는 회선";
            this.changed();
        }
    }

    protected void forward(Am10kpBlockEntity.Channel c) {
        int i = this.free();
        if (i >= 0) {
            this.channels[i] = new Am10kpBlockEntity.Channel(c.home, true, c.origin);
            this.channels[i].frontDoor = c.frontDoor;
            c.redirected = true;
            c.forwardedTo = this.worldPosition;
            this.status = "부재 전환 호출";
            this.changed();
        }
    }

    public boolean audioActive() {
        return this.lifted;
    }

    @Override
    public boolean startRinging(BlockPos from, String key) {
        if (this.power && this.level.getBlockEntity(from) instanceof Klp203rfBlockEntity panel && this.worldPosition.equals(panel.switchboardPosition())) {
            int i = this.free();
            if (i < 0) {
                return false;
            } else {
                Am10kpBlockEntity.Channel c = new Am10kpBlockEntity.Channel(this.worldPosition, true, this.worldPosition);
                c.frontDoor = from.immutable();
                this.channels[i] = c;
                this.status = "로비 호출";
                this.changed();
                return true;
            }
        } else {
            return false;
        }
    }

    public boolean answerLine(int i) {
        if (this.power && this.audioActive() && i >= 0 && i < 4 && this.channels[i] != null) {
            Am10kpBlockEntity.Channel c = this.channels[i];
            Am10kpBlockEntity origin = this.board(c.origin);
            boolean ok;
            if (origin != null && origin != this) {
                ok = origin.acceptRemote(c.home);
            } else if (c.frontDoor != null) {
                ok = c.connected || this.level.getBlockEntity(c.frontDoor) instanceof IntercomCaller camera && camera.onAnswered(this.worldPosition);
            } else {
                ReceiverBlockEntity r = this.home(c.home);
                ok = c.connected || c.incoming && r != null && r.acceptSwitchboardCall(this.worldPosition);
            }

            if (ok) {
                this.selected = i;
                c.connected = true;
                c.age = 0;
                this.status = "통화 " + this.number(c);
                this.changed();
            }

            return ok;
        } else {
            return false;
        }
    }

    protected boolean acceptRemote(BlockPos p) {
        int i = this.find(p);
        if (i < 0) {
            return false;
        } else {
            boolean before = this.lifted;
            this.lifted = true;
            boolean ok = this.answerLine(i);
            this.lifted = before;
            return ok;
        }
    }

    @Override
    public boolean onAnswered(BlockPos p) {
        int i = this.find(p);
        if (i >= 0 && !this.channels[i].incoming) {
            this.channels[i].connected = true;
            this.channels[i].age = 0;
            this.selected = i;
            this.status = "통화 " + this.assignedUnit(p);
            this.changed();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onReceiverHangUp(BlockPos p, DoorStatus reason) {
        int i = this.find(p);
        if (i < 0) {
            for (int j = 0; j < 4; j++) {
                if (this.channels[j] != null && p.equals(this.channels[j].frontDoor)) {
                    i = j;
                    break;
                }
            }
        }

        if (i >= 0) {
            this.end(i, false);
        }

        this.changed();
    }

    @Override
    public void onDoorOpened(BlockPos p) {
        this.onReceiverHangUp(p, DoorStatus.ENDED);
    }

    protected void end(int i, boolean notify) {
        Am10kpBlockEntity.Channel c = this.channels[i];
        if (c != null) {
            this.channels[i] = null;
            if (this.selected == i) {
                this.selected = -1;
            }

            Am10kpBlockEntity origin = this.board(c.origin);
            if (origin != null && origin != this) {
                origin.endRemote(c.home, notify);
            } else if (notify && c.frontDoor != null) {
                if (this.level.getBlockEntity(c.frontDoor) instanceof IntercomCaller camera) {
                    camera.onReceiverHangUp(this.worldPosition, c.connected ? DoorStatus.ENDED : DoorStatus.NO_ANSWER);
                }
            } else if (notify) {
                ReceiverBlockEntity r = this.home(c.home);
                if (r != null) {
                    r.endCallerSession(this.worldPosition, !c.connected);
                }
            }

            if (c.home.equals(this.registering)) {
                this.registering = null;
                this.registration = "";
            }
        }
    }

    protected void endRemote(BlockPos home, boolean notify) {
        int i = this.find(home);
        if (i >= 0) {
            this.end(i, notify);
            this.changed();
        }
    }

    @Override
    public void hangUp() {
        boolean bridged = false;
        if (this.transfer
            && this.transferSource != null
            && this.selected >= 0
            && this.channels[this.selected] != null
            && this.channels[this.selected].connected
            && !this.channels[this.selected].home.equals(this.transferSource)) {
            ReceiverBlockEntity a = this.home(this.transferSource);
            ReceiverBlockEntity b = this.home(this.channels[this.selected].home);
            if (a != null && b != null && a.bridgeThroughGuard(this.worldPosition, b)) {
                int destination = this.selected;
                int source = this.find(this.transferSource);
                if (source >= 0) {
                    this.end(source, false);
                }

                this.end(destination, false);
                this.status = "세대 간 교환 완료";
                bridged = true;
            }
        }

        if (!bridged && this.selected >= 0) {
            this.end(this.selected, true);
        }

        Am10kpBlockEntity peer = this.board(this.guardPeer);
        this.guardPeer = null;
        this.guardConnected = false;
        if (peer != null) {
            peer.guardPeer = null;
            peer.guardConnected = false;
            peer.changed();
        }

        this.lifted = false;
        this.transfer = false;
        this.transferSource = null;
        this.digits = "";
        this.building = "";
        this.registration = "";
        this.registering = null;
        this.changed();
    }

    @Override
    public void setEmergency(BlockPos p, String n, boolean active) {
        if (this.valid(p)) {
            if (!active || !(this.home(p) instanceof Kvm906BlockEntity k && !k.design().model().emergency())) {
                if (active) {
                    if (this.alarms.containsKey(p) || this.alarms.size() < this.alarmCapacity()) {
                        this.alarms.put(p.immutable(), n);
                        this.muted = false;
                        this.status = "비상 " + n;
                    }
                } else {
                    this.alarms.remove(p);
                }

                this.changed();
            }
        }
    }

    @Override
    public void setAway(BlockPos p, String n, boolean active) {
        if (this.valid(p)) {
            if (active) {
                this.away.add(p.immutable());
            } else {
                this.away.remove(p);
            }

            this.changed();
        }
    }

    @Override
    public void setGuardEmergency(BlockPos p, String id, boolean active) {
        if (ZoneData.connected(this.level, this.worldPosition, p)
            && !p.equals(this.worldPosition)
            && this.level.getBlockEntity(p) instanceof GuardExchange source
            && this.guardSystem().compatible(this.manufacturer(), source.guardSystem(), source.manufacturer())) {
            if (active) {
                if (!this.guardAlerts.containsKey(p)) {
                    this.muted = false;
                }

                this.guardAlerts.put(p.immutable(), id);
            } else {
                this.guardAlerts.remove(p);
            }

            this.changed();
        }
    }

    public void muteAlarm() {
        this.muted = true;
        this.changed();
    }

    public void key(String key) {
        if (this.power && key != null && key.length() == 1 && "0123456789*#".contains(key)) {
            if (key.equals("5") && this.audioActive()) {
                for (int i = 0; i < this.channelLimit(); i++) {
                    if (this.channels[i] != null
                        && this.channels[i].frontDoor != null
                        && !this.channels[i].connected
                        && this.level.getBlockEntity(this.channels[i].frontDoor) instanceof Klp203rfBlockEntity) {
                        this.answerLine(i);
                        return;
                    }
                }
            }

            this.sequence = this.sequence + key;
            if (this.sequence.length() > 5) {
                this.sequence = this.sequence.substring(this.sequence.length() - 5);
            }

            if (!this.sequence.equals("**00*")) {
                if (key.equals("*")) {
                    if (!this.registration.isEmpty()) {
                        this.commit();
                    } else {
                        this.digits = "";
                        this.building = "";
                        this.cancelTransfer();
                    }
                } else if (key.equals("#")) {
                    if (this.registration.isEmpty()) {
                        this.dialDigits();
                    }
                } else if (this.digits.length() < 4) {
                    this.digits = this.digits + key;
                }

                this.changed();
            } else {
                if (this.mutual && this.guardPeer == null) {
                    this.registration = "guard";
                    this.registering = null;
                } else {
                    if (this.selected < 0
                        || this.channels[this.selected] == null
                        || !this.channels[this.selected].incoming
                        || !this.channels[this.selected].connected) {
                        this.status = "세대 발신 통화 중에만 등록 가능";
                        this.changed();
                        return;
                    }

                    this.registration = "home";
                    this.registering = this.channels[this.selected].home;
                }

                this.digits = "";
                this.building = "";
                this.sequence = "";
                this.status = "동 입력 → 동 → 호 입력 → *";
                this.changed();
            }
        }
    }

    protected void commit() {
        if (this.registration.equals("guard")) {
            if (!this.digits.matches("[0-9]{1,3}") || Integer.parseInt(this.digits) == 0) {
                this.status = "경비실 번호 1–999";
                return;
            }

            String n = String.valueOf(Integer.parseInt(this.digits));
            if (this.level instanceof ServerLevel sl) {
                for (DeviceRegistry.Entry e : DeviceRegistry.get(sl).all()) {
                    Am10kpBlockEntity b = this.board(e.pos());
                    if (b != null && b != this && b.unit.equals(n)) {
                        this.status = "사용 중인 경비실 번호";
                        return;
                    }
                }
            }

            this.unit = n;
            this.register();
            this.status = "경비실 번호 등록 완료";
        } else {
            int i = this.find(this.registering);
            if (i < 0
                || !this.channels[i].connected
                || this.home(this.registering) == null
                || !this.home(this.registering).roomOnlyUnit() && !UnitAddress.component(this.building)
                || !UnitAddress.component(this.digits)) {
                this.status = "동·호수와 통화 상태 확인";
                return;
            }

            String n = this.home(this.registering).roomOnlyUnit()
                ? Integer.parseInt(this.digits) + ""
                : Integer.parseInt(this.building) + "-" + Integer.parseInt(this.digits);
            if (this.homes.entrySet().stream().anyMatch(ex -> !((BlockPos)ex.getKey()).equals(this.registering) && ((String)ex.getValue()).equals(n))) {
                this.status = "이미 등록된 동·호수";
                return;
            }

            ReceiverBlockEntity r = this.home(this.registering);
            if (r == null || !r.assignUnitFromSwitchboard(this.worldPosition, n)) {
                this.status = "등록 실패";
                return;
            }

            this.homes.put(this.registering, n);
            if (this.alarms.containsKey(this.registering)) {
                this.alarms.put(this.registering, n);
            }

            this.status = "세대 등록 완료 " + n;
        }

        this.registration = "";
        this.registering = null;
        this.digits = "";
        this.building = "";
        this.sequence = "";
    }

    protected void dialDigits() {
        if (!this.audioActive()) {
            this.status = "수화기를 먼저 들어주세요";
        } else {
            if (this.alarms.isEmpty() && !this.guardAlerts.isEmpty()) {
                this.mutual = true;
                this.digits = new ArrayList<>(this.guardAlerts.values()).get(this.ticks / 40 % this.guardAlerts.size());
            }

            if (this.mutual) {
                this.dialGuard();
            } else {
                String n = this.building.isEmpty() ? this.unit + "-" + this.digits : Integer.parseInt(this.building) + "-" + this.digits;
                if (this.building.isEmpty()
                    && UnitAddress.component(this.digits)
                    && this.homes
                        .entrySet()
                        .stream()
                        .anyMatch(
                            e -> e.getValue().equals(Integer.parseInt(this.digits) + "")
                                && this.home(e.getKey()) != null
                                && this.home(e.getKey()).roomOnlyUnit()
                        )) {
                    n = Integer.parseInt(this.digits) + "";
                }

                if (!this.alarms.isEmpty()) {
                    n = this.selectedAlarmUnit();
                }

                if (!UnitAddress.valid(n)) {
                    this.status = "동·호수는 각각 1–9999";
                } else {
                    String target = n;
                    BlockPos p = this.homes
                        .entrySet()
                        .stream()
                        .filter(e -> normalize(e.getValue()).equals(normalize(target)))
                        .map(Entry::getKey)
                        .findFirst()
                        .orElse(null);
                    int i = this.free();
                    if (p != null && this.valid(p) && i >= 0) {
                        ReceiverBlockEntity r = this.home(p);
                        if (r.startRinging(this.worldPosition, this.callerKey())) {
                            this.channels[i] = new Am10kpBlockEntity.Channel(p, false, this.worldPosition);
                            this.selected = i;
                            this.status = this.away.contains(p) ? "외출 세대 호출 " + n : "호출 " + n;
                            this.digits = "";
                            this.building = "";
                        } else {
                            this.status = r instanceof Kip234r2BlockEntity k && k.away() ? "not home · 부재 세대" : "세대 통화 중";
                        }
                    } else {
                        this.status = "미등록 세대 또는 회선 부족";
                    }
                }
            }
        }
    }

    protected static String normalize(String s) {
        try {
            String[] a = s.split("-");
            return Integer.parseInt(a[0]) + "-" + Integer.parseInt(a[1]);
        } catch (Exception var2) {
            return s;
        }
    }

    protected void dialGuard() {
        if (this.guardPeer == null
            && this.occupiedChannels() <= 0
            && (this.alarms.isEmpty() || this.mayCallGuardDuringAlarm())
            && this.level instanceof ServerLevel sl) {
            for (DeviceRegistry.Entry e : DeviceRegistry.get(sl).all()) {
                Am10kpBlockEntity b = this.board(e.pos());
                if (b != null && b != this && b.power && b.unit.equals(this.digits) && b.guardPeer == null && b.occupiedChannels() == 0 && b.alarms.isEmpty()) {
                    this.guardPeer = b.worldPosition;
                    b.guardPeer = this.worldPosition;
                    this.guardTicks = b.guardTicks = 0;
                    this.guardIncoming = false;
                    b.guardIncoming = true;
                    this.guardConnected = b.guardConnected = false;
                    b.status = "경비실 호출 " + this.unit;
                    b.changed();
                    this.digits = "";
                    this.status = "경비실 호출 중";
                    return;
                }
            }

            this.status = "경비실을 찾을 수 없음";
        } else {
            this.status = "상호 통화 불가";
        }
    }

    protected void cancelTransfer() {
        if (this.transfer) {
            if (this.selected >= 0 && this.channels[this.selected] != null && !this.channels[this.selected].home.equals(this.transferSource)) {
                this.end(this.selected, true);
            }

            this.selected = this.find(this.transferSource);
            this.transfer = false;
            this.transferSource = null;
        }
    }

    public void mode(String value) {
        switch (value) {
            case "building":
                if (UnitAddress.component(this.digits)) {
                    this.building = this.digits;
                    this.digits = "";
                } else {
                    this.status = "동 번호 1–9999";
                }
                break;
            case "emergency":
                this.muteAlarm();
                break;
            case "away":
                this.absent = !this.absent;
                this.status = this.absent ? "경비실 부재 전환" : "경비실 복귀";
                break;
            case "exchange":
                if (this.transfer) {
                    this.cancelTransfer();
                } else if (this.selected >= 0 && this.channels[this.selected] != null && this.channels[this.selected].connected) {
                    this.transfer = true;
                    this.transferSource = this.channels[this.selected].home;
                    this.digits = "";
                    this.building = "";
                    this.status = "교환할 세대 번호 입력";
                }
                break;
            case "mutual":
                if (this.guardPeer != null) {
                    Am10kpBlockEntity b = this.board(this.guardPeer);
                    if (this.audioActive() && b != null && b.audioActive()) {
                        this.guardConnected = b.guardConnected = true;
                        this.guardTicks = b.guardTicks = 0;
                        b.changed();
                        this.status = "경비실 상호 통화";
                    }
                } else if (this.occupiedChannels() == 0 && this.alarms.isEmpty()) {
                    this.mutual = !this.mutual;
                    this.digits = "";
                    this.building = "";
                    this.status = "상호 → **00* 경비실 번호 등록";
                }
                break;
            case "handset":
                if (this.lifted) {
                    this.hangUp();
                } else {
                    this.lifted = true;
                }
                break;
            case "power":
                this.power = !this.power;
                if (!this.power) {
                    for (int i = 0; i < 4; i++) {
                        this.end(i, true);
                    }

                    this.hangUp();
                }
                break;
            case "volume":
                this.volume = (this.volume + 1) % 3;
                break;
            case "melody":
                this.melody = 3 - this.melody;
        }

        this.changed();
    }

    @Override
    public void handleAction(ServerPlayer player, ReceiverBlockEntity.Action action, BlockPos target, String text) {
        if (action == ReceiverBlockEntity.Action.SET_SETTING && text != null) {
            int split = text.indexOf(61);
            if (split < 0) {
                return;
            }

            String k = text.substring(0, split);
            String v = text.substring(split + 1);
            if (k.equals("key")) {
                this.key(v);
            } else if (k.equals("mode")) {
                this.mode(v);
            } else if (k.equals("line")) {
                try {
                    this.answerLine(Integer.parseInt(v));
                } catch (NumberFormatException var9) {
                }
            } else if (k.equals("forward")) {
                Am10kpBlockEntity b = this.board(target);
                if (b != null && b != this) {
                    this.absentTarget = target.immutable();
                    this.status = "부재 전환 대상 " + b.unit;
                }
            }
        } else if (action == ReceiverBlockEntity.Action.PICKUP) {
            this.mode("handset");
        } else if (action == ReceiverBlockEntity.Action.HANG_UP) {
            this.hangUp();
        } else if (action == ReceiverBlockEntity.Action.SEND_MESSAGE) {
            this.sendMessage(player, text);
        }

        this.changed();
    }

    public BlockPos routeFrontDoor(ReceiverBlockEntity household, BlockPos camera) {
        int i = this.free();
        if (this.power
            && i >= 0
            && this.valid(household.getBlockPos())
            && (
                household.hasLinkedCamera(camera)
                    || household instanceof Kip234r2BlockEntity k && k.lobbyAllowed(camera)
                    || household instanceof Kvm906BlockEntity kv && kv.lobbyAllowed(camera)
            )) {
            Am10kpBlockEntity.Channel c = new Am10kpBlockEntity.Channel(household.getBlockPos(), true, this.worldPosition);
            c.frontDoor = camera.immutable();
            this.channels[i] = c;
            this.status = "현관 우회 호출 " + household.getUnit();
            Am10kpBlockEntity next = this.board(this.absentTarget);
            if (this.absent && next != null && next.power && !next.absent) {
                next.forward(c);
            }

            this.changed();
            return this.worldPosition;
        } else {
            return null;
        }
    }

    @Override
    public boolean hasCallerSession(BlockPos from) {
        for (Am10kpBlockEntity.Channel c : this.channels) {
            if (c != null && from.equals(c.frontDoor)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void endCallerSession(BlockPos from, boolean missed) {
        for (int i = 0; i < 4; i++) {
            if (this.channels[i] != null && from.equals(this.channels[i].frontDoor)) {
                this.end(i, false);
                this.changed();
            }
        }
    }

    @Override
    public List<IntercomLine> conversation() {
        return List.copyOf(this.log);
    }

    @Override
    public void addLineFrom(BlockPos source, IntercomLine line) {
        this.addLine(line);
        if (this.transfer
            && this.transferSource != null
            && this.selected >= 0
            && this.channels[this.selected] != null
            && this.channels[this.selected].connected) {
            int first = this.find(this.transferSource);
            Am10kpBlockEntity.Channel second = this.channels[this.selected];
            if (first >= 0 && this.channels[first].connected) {
                ReceiverBlockEntity recipient = source.equals(this.transferSource)
                    ? this.home(second.home)
                    : (source.equals(second.home) ? this.home(this.transferSource) : null);
                if (recipient != null) {
                    recipient.addLine(line);
                    recipient.syncScreens();
                    if (this.level instanceof ServerLevel sl) {
                        for (ServerPlayer player : Intercom.playersNear(sl, recipient.getBlockPos(), 8.0)) {
                            player.sendSystemMessage(Component.literal(line.name() + ": " + line.text()));
                        }
                    }
                }
            }
        }

        int index = this.find(source);
        if (index < 0) {
            for (int i = 0; i < 4; i++) {
                if (this.channels[i] != null && source.equals(this.channels[i].frontDoor)) {
                    index = i;
                    break;
                }
            }
        }

        Am10kpBlockEntity remote = this.board(index >= 0 && this.channels[index].forwardedTo != null ? this.channels[index].forwardedTo : this.absentTarget);
        if (index >= 0 && this.channels[index].origin.equals(this.worldPosition) && remote != null) {
            int at = remote.find(this.channels[index].home);
            if (at >= 0 && remote.channels[at].origin.equals(this.worldPosition) && remote.channels[at].connected) {
                remote.addLine(line);
                remote.syncScreens();
                if (this.level instanceof ServerLevel sl) {
                    for (ServerPlayer player : Intercom.playersNear(sl, remote.worldPosition, 8.0)) {
                        player.sendSystemMessage(Component.literal(line.name() + ": " + line.text()));
                    }
                }
            }
        }
    }

    @Override
    protected void sendMessage(ServerPlayer p, String text) {
        if (this.selected >= 0
            && this.channels[this.selected] != null
            && this.channels[this.selected].frontDoor != null
            && this.channels[this.selected].connected
            && this.audioActive()
            && this.level instanceof ServerLevel sl) {
            if (this.level.getBlockEntity(this.channels[this.selected].frontDoor) instanceof IntercomCaller c) {
                Intercom.say(sl, this, c, p, "guard:" + this.unit, text);
            }
        } else if (this.audioActive() && text != null && !text.isBlank()) {
            ReceiverBlockEntity r = (ReceiverBlockEntity)(this.guardConnected
                ? this.board(this.guardPeer)
                : (
                    this.selected >= 0 && this.channels[this.selected] != null && this.channels[this.selected].connected
                        ? this.home(this.channels[this.selected].home)
                        : null
                ));
            if (r != null && this.level instanceof ServerLevel sl) {
                Intercom.say(sl, this, r, p, "guard:" + this.unit, text);
            }
        }
    }

    protected String number(Am10kpBlockEntity.Channel c) {
        if (c.frontDoor != null && this.level.getBlockEntity(c.frontDoor) instanceof Klp203rfBlockEntity panel) {
            return "로비 " + panel.lobbyAddress();
        } else {
            Am10kpBlockEntity origin = this.board(c.origin);
            return origin == null ? "" : origin.homes.getOrDefault(c.home, "");
        }
    }

    protected String selectedAlarmUnit() {
        return this.alarmUnit();
    }

    protected boolean mayCallGuardDuringAlarm() {
        return false;
    }

    protected String alarmUnit() {
        return this.alarms.isEmpty() ? "" : new ArrayList<>(this.alarms.values()).get(this.ticks / 40 % this.alarms.size());
    }

    public String display(int row) {
        if (!this.power) {
            return "";
        } else if (row == 0 && (!this.registration.isEmpty() || !this.alarms.isEmpty()) && this.ticks % 20 >= 10) {
            return "";
        } else if (row != 0) {
            Am10kpBlockEntity.Channel c = this.channels[row - 1];
            return c == null ? "" : this.number(c);
        } else {
            return !this.registration.isEmpty()
                ? (this.building.isEmpty() && this.digits.isEmpty() ? (this.registration.equals("guard") ? this.unit : "0") : this.building + "-" + this.digits)
                : (
                    !this.alarms.isEmpty()
                        ? this.alarmUnit()
                        : (
                            !this.guardAlerts.isEmpty()
                                ? new ArrayList<>(this.guardAlerts.values()).get(this.ticks / 40 % this.guardAlerts.size())
                                : (
                                    this.guardPeer != null && this.board(this.guardPeer) != null
                                        ? this.board(this.guardPeer).unit
                                        : (!this.digits.isEmpty() ? this.building + "-" + this.digits : this.unit)
                                )
                        )
                );
        }
    }

    public boolean modeLit(int i) {
        if (!this.power) {
            return false;
        } else {
            return switch (i) {
                case 0 -> this.alarmCount() > 0 && this.ticks % 20 < 10;
                case 1 -> this.absent
                    || this.selected >= 0
                        && this.channels[this.selected] != null
                        && (this.channels[this.selected].frontDoor != null || this.away.contains(this.channels[this.selected].home));
                case 2 -> this.transfer;
                case 3 -> this.guardPeer != null ? this.guardConnected || this.ticks % 20 < 10 : this.mutual;
                default -> false;
            };
        }
    }

    public boolean lit(int row) {
        if (!this.power) {
            return false;
        } else if (row == 0) {
            return !this.registration.isEmpty() && this.ticks % 20 < 10
                || this.alarmCount() > 0 && this.ticks % 20 < 10
                || this.guardPeer != null && (this.guardConnected || this.ticks % 10 < 5);
        } else {
            Am10kpBlockEntity.Channel c = this.channels[row - 1];
            return c != null && (c.connected || this.ticks % 10 < 5);
        }
    }

    @Override
    protected List<DeviceEntry> deviceEntries() {
        return new ArrayList<>();
    }

    @Override
    protected Map<String, String> settingsForClient() {
        Map<String, String> s = new HashMap<>();
        s.put("am10kp", "1");
        s.put("power", this.power ? "1" : "0");
        s.put("lifted", this.lifted ? "1" : "0");
        s.put("status", this.status);
        s.put("registered", Integer.toString(this.homes.size()));
        s.put("registration", this.registration);
        s.put("alarms", Integer.toString(this.alarmCount()));
        s.put("muted", this.muted ? "1" : "0");
        s.put("absent", this.absent ? "1" : "0");
        s.put("volume", Integer.toString(this.volume));
        s.put("melody", Integer.toString(this.melody));
        s.put("selected", Integer.toString(this.selected));
        s.put("transfer", this.transfer ? "1" : "0");
        s.put("mutual", this.guardPeer == null && !this.mutual ? "0" : "1");

        for (int i = 0; i < 5; i++) {
            s.put("display" + i, this.display(i));
            s.put("led" + i, this.lit(i) ? "1" : "0");
            if (i < 4) {
                s.put("mode" + i, this.modeLit(i) ? "1" : "0");
            }
        }

        for (int ix = 0; ix < 4; ix++) {
            Am10kpBlockEntity.Channel c = this.channels[ix];
            s.put("line" + ix, c == null ? "IDLE" : (c.connected ? "CONNECTED" : "RINGING"));
            s.put("away" + ix, c == null || !this.away.contains(c.home) && c.frontDoor == null ? "0" : "1");
        }

        s.put("forward", this.absentTarget == null ? "미설정" : (this.board(this.absentTarget) == null ? "신호 없음" : this.board(this.absentTarget).unit));
        if (this.level instanceof ServerLevel sl) {
            int ix = 0;

            for (DeviceRegistry.Entry e : DeviceRegistry.get(sl).all()) {
                Am10kpBlockEntity b = this.board(e.pos());
                if (b != null && b != this && ix < 100) {
                    s.put("peer" + ix++, e.pos().getX() + "," + e.pos().getY() + "," + e.pos().getZ() + ":" + b.unit);
                }
            }

            s.put("peer_count", Integer.toString(ix));
        }

        return s;
    }

    public static void tick(Level l, BlockPos p, BlockState s, Am10kpBlockEntity b) {
        b.ticks++;
        b.alarms.keySet().removeIf(at -> !ZoneData.connected(l, p, at));
        b.away.removeIf(at -> !ZoneData.connected(l, p, at));
        b.guardAlerts.keySet().removeIf(at -> !ZoneData.connected(l, p, at));
        if (b.power) {
            for (int i = 0; i < 4; i++) {
                Am10kpBlockEntity.Channel c = b.channels[i];
                if (c != null) {
                    c.age++;
                    ReceiverBlockEntity r = b.home(c.home);
                    if (c.frontDoor == null && r == null
                        || (
                            c.frontDoor != null
                                ? !ZoneData.connected(l, p, c.frontDoor)
                                    || !(l.getBlockEntity(c.frontDoor) instanceof CallerBlockEntity camera)
                                    || !camera.isInCall()
                                : !r.hasCallerSession(c.origin)
                        )
                        || c.age > (c.connected ? 3600 : 600)) {
                        b.end(i, true);
                        b.changed();
                    }
                }
            }

            if (b.guardPeer != null) {
                b.guardTicks++;
                if (b.board(b.guardPeer) == null || b.guardTicks > (b.guardConnected ? 3600 : 600)) {
                    Am10kpBlockEntity peer = b.board(b.guardPeer);
                    b.guardPeer = null;
                    b.guardConnected = false;
                    if (peer != null) {
                        peer.guardPeer = null;
                        peer.guardConnected = false;
                        peer.changed();
                    }

                    b.changed();
                }
            }

            boolean dialing = Arrays.stream(b.channels).anyMatch(c -> c != null && !c.incoming && !c.connected && !c.redirected)
                || b.guardPeer != null && !b.guardIncoming && !b.guardConnected;
            if (dialing) {
                CallSounds.ringback(l, p, b.ticks - 1, (b.volume + 1) * 0.22F);
            }

            if (b.ticks % 5 == 0
                && (
                    b.alarmCount() > 0
                        || !b.registration.isEmpty()
                        || b.guardPeer != null
                        || Arrays.stream(b.channels).anyMatch(c -> c != null && !c.connected)
                )) {
                b.changed();
            }

            if (b.ticks % 20 == 0) {
                if (l instanceof ServerLevel sl) {
                    for (DeviceRegistry.Entry e : DeviceRegistry.get(sl).all()) {
                        if (!e.pos().equals(p)
                            && sl.isLoaded(e.pos())
                            && sl.getBlockEntity(e.pos()) instanceof Kam305BlockEntity kam
                            && (kam.selfEmergency() || b.guardAlerts.containsKey(e.pos()))) {
                            b.setGuardEmergency(e.pos(), kam.getUnit(), kam.selfEmergency());
                        }
                    }
                }

                for (Entry<BlockPos, String> entry : new ArrayList<>(b.homes.entrySet())) {
                    ReceiverBlockEntity r = b.home(entry.getKey());
                    if (r != null && b.valid(entry.getKey())) {
                        b.homes.put(entry.getKey(), r.getUnit());
                        if (r instanceof Kvm906BlockEntity k) {
                            if (k.emergency() != b.alarms.containsKey(entry.getKey())) {
                                b.setEmergency(entry.getKey(), r.getUnit(), k.emergency());
                            }

                            if (k.away()) {
                                b.away.add(entry.getKey());
                            } else {
                                b.away.remove(entry.getKey());
                            }
                        }
                    }
                }

                boolean alarm = b.alarmCount() > 0 && !b.muted;
                boolean ring = Arrays.stream(b.channels).anyMatch(c -> c != null && !c.connected && !c.redirected)
                    || b.guardPeer != null && b.guardIncoming && !b.guardConnected;
                if (alarm || ring) {
                    l.playSound(
                        null,
                        p,
                        alarm ? SoundEvents.NOTE_BLOCK_BELL.value() : SoundEvents.NOTE_BLOCK_CHIME.value(),
                        SoundSource.BLOCKS,
                        alarm ? 1.0F : (b.volume + 1) * 0.22F,
                        alarm ? 0.55F : (b.melody == 1 ? 1.2F : 1.6F)
                    );
                }

                b.changed();
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level instanceof ServerLevel sl) {
            HashSet<String> used = new HashSet<>();
            int count = 0;

            for (DeviceRegistry.Entry e : DeviceRegistry.get(sl).all()) {
                Am10kpBlockEntity b = this.board(e.pos());
                if (b != null && b != this) {
                    used.add(b.unit);
                    count++;
                }
            }

            if (count >= this.guardLimit()) {
                this.power = false;
                this.status = "최대 " + this.guardLimit() + "개 경비실기";
            } else if (this.unit.isEmpty() || used.contains(this.unit)) {
                for (int n = 101; n <= 200; n++) {
                    if (!used.contains(Integer.toString(n))) {
                        this.unit = Integer.toString(n);
                        break;
                    }
                }

                this.register();
                this.setChanged();
            }
        }
    }

    @Override
    public void onBroken() {
        for (int i = 0; i < 4; i++) {
            this.end(i, true);
        }

        this.hangUp();

        for (BlockPos p : new ArrayList<>(this.homes.keySet())) {
            ReceiverBlockEntity r = this.home(p);
            if (r != null && this.worldPosition.equals(r.getAnalogStation())) {
                r.unlinkSwitchboard();
            }
        }

        super.onBroken();
    }

    @Override
    protected void saveAdditional(CompoundTag t) {
        super.saveAdditional(t);
        t.putBoolean("Power", this.power);
        t.putBoolean("Absent", this.absent);
        t.putInt("Volume", this.volume);
        t.putInt("Melody", this.melody);
        if (this.absentTarget != null) {
            t.putLong("Forward", this.absentTarget.asLong());
        }

        ListTag list = new ListTag();
        this.homes.forEach((p, n) -> {
            CompoundTag c = new CompoundTag();
            c.putLong("P", p.asLong());
            c.putString("N", n);
            list.add(c);
        });
        t.put("Homes", list);
    }

    @Override
    public void load(CompoundTag t) {
        super.load(t);
        this.power = !t.contains("Power") || t.getBoolean("Power");
        this.absent = t.getBoolean("Absent");
        this.volume = Math.max(0, Math.min(2, t.getInt("Volume")));
        this.melody = t.getInt("Melody") == 2 ? 2 : 1;
        this.absentTarget = t.contains("Forward") ? BlockPos.of(t.getLong("Forward")) : null;
        this.homes.clear();
        ListTag list = t.getList("Homes", 10);

        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            this.homes.put(BlockPos.of(c.getLong("P")), c.getString("N"));
        }

        Arrays.fill(this.channels, null);
        this.alarms.clear();
        this.guardAlerts.clear();
        this.away.clear();
        this.lifted = false;
        this.guardPeer = null;
        this.selected = -1;
        this.registration = "";
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag t = new CompoundTag();
        this.productLogo.write(t);
        t.putBoolean("Power", this.power);
        t.putBoolean("Lifted", this.lifted);

        for (int i = 0; i < 5; i++) {
            t.putString("D" + i, this.display(i));
            t.putBoolean("L" + i, this.lit(i));
            if (i < 4) {
                t.putBoolean("M" + i, this.modeLit(i));
            }
        }

        return t;
    }

    @Override
    public void handleUpdateTag(CompoundTag t) {
        this.productLogo.read(t);
        this.power = t.getBoolean("Power");
        this.lifted = t.getBoolean("Lifted");

        for (int i = 0; i < 5; i++) {
            this.clientDisplay[i] = t.getString("D" + i);
            this.clientLed[i] = t.getBoolean("L" + i);
            if (i < 4) {
                this.clientModes[i] = t.getBoolean("M" + i);
            }
        }
    }

    public String renderedDisplay(int i) {
        return this.level != null && this.level.isClientSide ? this.clientDisplay[i] : this.display(i);
    }

    public boolean renderedLed(int i) {
        return this.level != null && this.level.isClientSide ? this.clientLed[i] : this.lit(i);
    }

    public boolean renderedMode(int i) {
        return this.level != null && this.level.isClientSide ? this.clientModes[i] : this.modeLit(i);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) {
            this.handleUpdateTag(packet.getTag());
        }
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    protected static final class Channel {
        final BlockPos home;
        final BlockPos origin;
        final boolean incoming;
        BlockPos frontDoor;
        BlockPos forwardedTo;
        boolean connected;
        boolean redirected;
        int age;

        Channel(BlockPos p, boolean in, BlockPos origin) {
            this.home = p.immutable();
            this.incoming = in;
            this.origin = origin.immutable();
        }
    }
}
