package com.qwerty.homenet.blockentity;

import com.qwerty.homenet.analog.AnalogBus;
import com.qwerty.homenet.analog.AnalogLobbyEndpoint;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.data.ZoneData;
import com.qwerty.homenet.guard.GuardSystem;
import com.qwerty.homenet.intercom.CallSounds;
import com.qwerty.homenet.intercom.Intercom;
import com.qwerty.homenet.intercom.MissedCall;
import com.qwerty.homenet.intercom.UnitAddress;
import com.qwerty.homenet.move.DeviceMoves;
import com.qwerty.homenet.registry.ModBlockEntities;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class Hmc7000BlockEntity extends HyundaiBusConsoleBlockEntity {
    private int angle = 35;
    private int held = -1;
    private int preferred = -1;
    private int testType = -1;
    private int reviewIndex;
    private int reviewTicks;
    private int alarmTicks;
    private String page = "standby";
    private String route = "house";
    private String reviewType = "";
    private String version = "HMC-7000 V1.0";
    private final LinkedHashMap<BlockPos, Hmc7000BlockEntity.ManagerCall> managers = new LinkedHashMap<>();
    private BlockPos activeManager;
    private final LinkedHashMap<String, Hmc7000BlockEntity.Alarm> activeAlarms = new LinkedHashMap<>();
    private final Map<String, Boolean> alarmEdges = new HashMap<>();
    private final List<Hmc7000BlockEntity.Alarm> archive = new ArrayList<>();
    private final Map<BlockPos, String> lobbyNumbers = new HashMap<>();
    private final Set<BlockPos> waitingMessages = new HashSet<>();
    private long rtcEpoch = 946684800L;
    private long rtcTick;
    private long wakeDay = Long.MIN_VALUE;
    private boolean wakeEnabled;
    private boolean wakeRinging;
    private boolean editWake;
    private boolean clockArmed;
    private int wakeHour = 6;
    private int wakeMinute;
    private int clockField;
    private int weekdayOffset;
    private final int[] clockEdit = new int[]{1, 1, 6, 0, 0, 6};
    private String editDigits = "";
    private String assignBuilding = "";
    private String assignRoom = "";
    private boolean assignRoomDone;
    private static final String[] TYPES = new String[]{"fire", "gas", "security", "emergency"};
    private static final String[] ALARM_WORDS = new String[]{"불이 났습니다", "가스가 새고 있습니다", "문이 열렸습니다", "비상사태 발생"};
    private final String[] clientLcd = new String[]{"", ""};
    private final boolean[] clientLights = new boolean[15];

    public Hmc7000BlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HMC_7000.get(), pos, state);
        this.unit = "2";
        this.productLogo.set("logo_text", "HYUNDAI TELECOM");
    }

    @Override
    public String modelId() {
        return "HMC-7000";
    }

    @Override
    public String manufacturer() {
        return "HYUNDAI";
    }

    @Override
    protected int guardLimit() {
        return Integer.MAX_VALUE;
    }

    @Override
    protected HyundaiBusConsoleBlockEntity board(BlockPos pos) {
        HyundaiBusConsoleBlockEntity b = super.board(pos);
        return b instanceof Hmc7000BlockEntity ? b : null;
    }

    @Override
    public boolean supports(AnalogBus bus) {
        return bus == AnalogBus.COMMON_4_WIRE || bus == AnalogBus.COMMON_8_WIRE;
    }

    public int screenAngle() {
        return this.angle;
    }

    public void adjustAngle(int delta) {
        this.angle = Math.max(0, Math.min(65, this.angle + Integer.signum(delta)));
        this.changed();
    }

    @Override
    public int occupiedChannels() {
        return super.occupiedChannels() + this.managers.size();
    }

    public boolean held() {
        return this.held >= 0 && this.held < 4 && this.channels[this.held] != null;
    }

    public String page() {
        return this.page;
    }

    public int historyCount() {
        return this.archive.size();
    }

    public boolean wakeEnabled() {
        return this.wakeEnabled;
    }

    public boolean wakeRinging() {
        return this.wakeRinging;
    }

    public boolean messageWaiting(BlockPos home) {
        return this.waitingMessages.contains(home);
    }

    public boolean clearHouseMessage(BlockPos home) {
        if (!this.valid(home)) {
            return false;
        } else {
            this.waitingMessages.remove(home);
            ReceiverBlockEntity r = this.home(home);
            if (r != null) {
                r.getPersistentData().remove("HmcGuardMessage");
                r.setChanged();
                r.syncScreens();
            }

            this.changed();
            return true;
        }
    }

    private boolean scoped(BlockPos pos) {
        return this.level != null && ZoneData.connected(this.level, this.worldPosition, pos);
    }

    private Hmc7000BlockEntity manager(BlockPos pos) {
        return this.board(pos) instanceof Hmc7000BlockEntity b ? b : null;
    }

    private long epoch() {
        return this.rtcEpoch + (this.level == null ? 0L : Math.max(0L, this.level.getGameTime() - this.rtcTick) / 20L);
    }

    private LocalDateTime now() {
        return LocalDateTime.ofEpochSecond(this.epoch(), 0, ZoneOffset.UTC);
    }

    private void idle() {
        this.page = "standby";
        this.digits = "";
        this.building = "";
        this.route = "house";
        this.clockArmed = false;
        this.editDigits = "";
        this.registration = "";
        this.assignBuilding = this.assignRoom = "";
        this.assignRoomDone = false;
    }

    private void interruptPanels() {
        this.testType = -1;
        this.reviewTicks = 0;
        if (Set.of("test", "history", "wake", "version", "clock").contains(this.page)) {
            this.idle();
        }
    }

    private int priority(HyundaiBusConsoleBlockEntity.Channel c) {
        return c.frontDoor != null ? 0 : 1;
    }

    private int nextChannel() {
        if (this.preferred >= 0 && this.preferred < 4 && this.channels[this.preferred] != null && !this.channels[this.preferred].connected) {
            return this.preferred;
        } else {
            int best = -1;

            for (int i = 0; i < 4; i++) {
                if (this.channels[i] != null
                    && !this.channels[i].connected
                    && !this.channels[i].redirected
                    && (best < 0 || this.priority(this.channels[i]) < this.priority(this.channels[best]))) {
                    best = i;
                }
            }

            return best;
        }
    }

    private BlockPos nextManager() {
        return this.managers
            .entrySet()
            .stream()
            .filter(e -> !e.getValue().connected)
            .sorted(Comparator.comparingInt(e -> e.getKey().equals(this.activeManager) ? -1 : this.managerPriority(e.getKey())))
            .map(Entry::getKey)
            .findFirst()
            .orElse(null);
    }

    private int managerPriority(BlockPos pos) {
        Hmc7000BlockEntity b = this.manager(pos);
        if (b == null) {
            return 9;
        } else {
            return b.unit.equals("0") ? 3 : (Set.of("1", "2", "3", "4").contains(b.unit) ? 4 : 2);
        }
    }

    private void pickup() {
        if (this.lifted) {
            this.hangUp();
        } else {
            this.interruptPanels();
            this.wakeRinging = false;
            this.lifted = true;
            if (this.held()) {
                this.selected = this.held;
                this.held = -1;
                this.transfer = false;
                this.transferSource = null;
                this.page = "call";
                this.changed();
            } else {
                int i = this.nextChannel();
                BlockPos other = this.nextManager();
                if (i < 0 || other != null && this.priority(this.channels[i]) >= this.managerPriority(other)) {
                    if (other != null) {
                        this.answerManager(other);
                    } else {
                        this.page = "dial";
                        this.route = "house";
                        this.digits = "";
                        this.building = "";
                    }
                } else {
                    this.answerLine(i);
                }

                this.changed();
            }
        }
    }

    public Hmc7000BlockEntity.HomeCallResult requestHomeCall(ReceiverBlockEntity receiver, ServerPlayer player) {
        if (!this.power) {
            return Hmc7000BlockEntity.HomeCallResult.POWER_OFF;
        } else if (!this.valid(receiver.getBlockPos())) {
            return Hmc7000BlockEntity.HomeCallResult.NOT_LINKED;
        } else if (this.find(receiver.getBlockPos()) >= 0) {
            return Hmc7000BlockEntity.HomeCallResult.BUSY;
        } else if (this.free() < 0) {
            return Hmc7000BlockEntity.HomeCallResult.NO_LINE;
        } else {
            this.interruptPanels();
            int slot = this.free();
            this.channels[slot] = new HyundaiBusConsoleBlockEntity.Channel(receiver.getBlockPos(), true, this.worldPosition);
            if (receiver instanceof Ha4100hBlockEntity ha && !ha.beginHmcCall(this)) {
                this.channels[slot] = null;
                return Hmc7000BlockEntity.HomeCallResult.BUSY;
            } else {
                this.status = "세대 호출 " + receiver.getUnit();
                HyundaiBusConsoleBlockEntity next = this.board(this.absentTarget);
                if (this.absent && next != null && next.power && !next.absent) {
                    next.forward(this.channels[slot]);
                }

                this.waitingMessages.remove(receiver.getBlockPos());
                receiver.getPersistentData().remove("HmcGuardMessage");
                receiver.setChanged();
                this.preferred = -1;
                if (this.lifted) {
                    this.status = "세대 수신 호출 · 세대 버튼을 눌러 응답하세요";
                }

                this.changed();
                return Hmc7000BlockEntity.HomeCallResult.ACCEPTED;
            }
        }
    }

    @Override
    public void callFrom(ReceiverBlockEntity receiver, ServerPlayer player) {
        this.requestHomeCall(receiver, player);
    }

    private boolean lobbyValid(BlockPos pos) {
        return this.scoped(pos)
            && this.level.isLoaded(pos)
            && this.level.getBlockEntity(pos) instanceof CallerBlockEntity
            && this.level.getBlockEntity(pos) instanceof AnalogLobbyEndpoint lp
            && this.worldPosition.equals(lp.switchboardPosition())
            && lp.supports(AnalogBus.COMMON_8_WIRE)
            && this.guardSystem().compatible(this.manufacturer(), GuardSystem.BUS, lp.manufacturer());
    }

    @Override
    public boolean startRinging(BlockPos from, String key) {
        if (this.power && this.lobbyValid(from)) {
            int i = this.free();
            if (i < 0) {
                return false;
            } else {
                this.interruptPanels();
                HyundaiBusConsoleBlockEntity.Channel c = new HyundaiBusConsoleBlockEntity.Channel(this.worldPosition, true, this.worldPosition);
                c.frontDoor = from.immutable();
                this.channels[i] = c;
                this.preferred = -1;
                this.changed();
                return true;
            }
        } else {
            return false;
        }
    }

    private boolean channelValid(HyundaiBusConsoleBlockEntity.Channel c) {
        if (c.frontDoor != null) {
            return this.lobbyValid(c.frontDoor);
        } else {
            Hmc7000BlockEntity origin = this.manager(c.origin);
            return this.scoped(c.home) && origin != null && origin.valid(c.home);
        }
    }

    @Override
    protected void forward(HyundaiBusConsoleBlockEntity.Channel c) {
        if (this.scoped(c.home) && this.manager(c.origin) != null) {
            this.interruptPanels();
            super.forward(c);
        }
    }

    @Override
    public boolean answerLine(int index) {
        if (this.held() && index != this.held) {
            return false;
        } else if (index >= 0 && index < 4 && this.channels[index] != null) {
            HyundaiBusConsoleBlockEntity.Channel c = this.channels[index];
            if (!this.channelValid(c)) {
                return false;
            } else {
                int previous = this.selected;
                if (c.frontDoor == null && c.origin.equals(this.worldPosition) && this.home(c.home) instanceof Ha4100hBlockEntity ha) {
                    if (!this.power || !this.lifted) {
                        return false;
                    }

                    if (!c.connected && (!c.incoming || !ha.acceptSwitchboardCall(this.worldPosition))) {
                        return false;
                    }

                    c.connected = true;
                    c.age = 0;
                    this.selected = index;
                    this.status = "통화 " + this.number(c);
                } else if (!super.answerLine(index)) {
                    return false;
                }

                if (previous >= 0 && previous != index && this.channels[previous] != null && this.channels[previous].connected) {
                    this.end(previous, true);
                }

                for (BlockPos at : new ArrayList<>(this.managers.keySet())) {
                    if (this.managers.get(at).connected) {
                        this.endManager(at);
                    }
                }

                this.activeManager = null;
                this.preferred = -1;
                this.page = "call";
                this.changed();
                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean onAnswered(BlockPos source) {
        int slot = this.find(source);
        if (this.home(source) instanceof Ha4100hBlockEntity ha) {
            if (this.power && this.lifted && slot >= 0 && !this.channels[slot].incoming && this.valid(source) && ha.answeringHmc(this)) {
                this.channels[slot].connected = true;
                this.channels[slot].age = 0;
                this.selected = slot;
                this.status = "통화 " + this.assignedUnit(source);
                this.page = "call";
                this.changed();
                return true;
            } else {
                return false;
            }
        } else {
            boolean ok = super.onAnswered(source);
            if (ok) {
                this.page = "call";
            }

            return ok;
        }
    }

    @Override
    protected void dialDigits() {
        if (this.mutual || !this.alarms.isEmpty() || !this.guardAlerts.isEmpty()) {
            super.dialDigits();
        } else if (this.power && this.lifted) {
            String target = this.building.isEmpty() ? this.unit + "-" + this.digits : this.building + "-" + this.digits;
            BlockPos pos = this.homes
                .entrySet()
                .stream()
                .filter(e -> normalize(e.getValue()).equals(normalize(target)))
                .map(Entry::getKey)
                .findFirst()
                .orElse(null);
            if (this.home(pos) instanceof Ha4100hBlockEntity ha) {
                int var5 = this.free();
                if (this.valid(pos) && var5 >= 0 && this.find(pos) < 0) {
                    this.channels[var5] = new HyundaiBusConsoleBlockEntity.Channel(pos, false, this.worldPosition);
                    if (!ha.startRinging(this.worldPosition, this.callerKey())) {
                        this.channels[var5] = null;
                        this.status = "세대 통화 중 또는 전원 꺼짐";
                    } else {
                        this.selected = var5;
                        this.page = "call";
                        this.status = "세대 호출 " + ha.getUnit();
                        this.digits = "";
                        this.building = "";
                        this.changed();
                    }
                } else {
                    this.status = "미등록 세대 또는 회선 사용 중";
                }
            } else {
                super.dialDigits();
            }
        } else {
            this.status = "수화기를 먼저 들어주세요";
        }
    }

    boolean sendHomeMessage(Ha4100hBlockEntity ha, ServerPlayer player, String text) {
        int slot = this.find(ha.getBlockPos());
        if (slot >= 0 && this.channels[slot].connected && this.valid(ha.getBlockPos()) && ha.talkingToHmc(this)) {
            if (this.level instanceof ServerLevel server) {
                Intercom.say(server, ha, this, player, ha.callerKey(), text);
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    protected String number(HyundaiBusConsoleBlockEntity.Channel c) {
        if (c.frontDoor != null && this.level.getBlockEntity(c.frontDoor) instanceof AnalogLobbyEndpoint lp) {
            return lp.videoNumber();
        } else {
            Hmc7000BlockEntity origin = this.manager(c.origin);
            return origin == null ? "" : origin.homes.getOrDefault(c.home, "");
        }
    }

    private void choose(String kind) {
        if (this.held()) {
            this.status = "세대 보류 중에는 다른 종류의 통화를 선택할 수 없습니다";
        } else if (kind.equals("house")
            && this.selected >= 0
            && this.channels[this.selected] != null
            && this.channels[this.selected].frontDoor == null
            && !this.channels[this.selected].incoming
            && !this.channels[this.selected].connected) {
            this.leaveMessage(this.channels[this.selected].home);
            this.changed();
        } else {
            if (!kind.equals("house") && !kind.equals("lobby")) {
                this.route = kind;
                this.activeManager = this.managers
                    .keySet()
                    .stream()
                    .filter(
                        p -> kind.equals("manager") || this.manager(p) != null && this.manager(p).unit.equals(kind.equals("central") ? "0" : kind.substring(7))
                    )
                    .findFirst()
                    .orElse(null);
                if (this.activeManager != null && this.lifted) {
                    this.answerManager(this.activeManager);
                }
            } else {
                int found = -1;

                for (int i = 0; i < 4; i++) {
                    if (this.channels[i] != null && this.channels[i].frontDoor != null == kind.equals("lobby")) {
                        found = i;
                        break;
                    }
                }

                this.route = kind;
                if (found >= 0) {
                    this.preferred = found;
                    this.activeManager = null;
                    if (this.lifted) {
                        this.answerLine(found);
                    }
                } else if (kind.equals("house")
                    && this.selected >= 0
                    && this.channels[this.selected] != null
                    && !this.channels[this.selected].connected
                    && !this.channels[this.selected].incoming) {
                    this.leaveMessage(this.channels[this.selected].home);
                }
            }

            this.changed();
        }
    }

    private void leaveMessage(BlockPos at) {
        ReceiverBlockEntity r = this.home(at);
        if (r != null) {
            this.waitingMessages.add(at);
            r.getPersistentData().putBoolean("HmcGuardMessage", true);
            r.missed.add(new MissedCall("guard-message:" + this.unit, this.level.getDayTime()));
            r.setChanged();
            r.syncScreens();
            this.status = "세대에 경비실 메시지 표시를 남겼습니다";
        }
    }

    private boolean requestManager(Hmc7000BlockEntity caller) {
        if (this.power && this.scoped(caller.worldPosition) && caller != this && !this.managers.containsKey(caller.worldPosition) && this.managers.size() < 8) {
            this.interruptPanels();
            this.managers.put(caller.worldPosition, new Hmc7000BlockEntity.ManagerCall(true));
            this.changed();
            return true;
        } else {
            return false;
        }
    }

    private void dialManager(String id) {
        if (this.lifted && !this.held()) {
            if (this.level instanceof ServerLevel sl) {
                for (DeviceRegistry.Entry e : DeviceRegistry.get(sl).all()) {
                    Hmc7000BlockEntity other = this.manager(e.pos());
                    if (other != null && other != this && other.unit.equals(id)) {
                        if (!other.requestManager(this)) {
                            this.status = "관리실 통화 중";
                            return;
                        }

                        this.managers.put(other.worldPosition, new Hmc7000BlockEntity.ManagerCall(false));
                        this.activeManager = other.worldPosition;
                        this.page = "dial";
                        this.digits = "";
                        this.changed();
                        return;
                    }
                }

                this.status = "같은 단지에서 해당 관리실을 찾을 수 없습니다";
            }
        } else {
            this.status = "수화기와 보류 상태를 확인하세요";
        }
    }

    private void answerManager(BlockPos at) {
        if (this.lifted && !this.held()) {
            Hmc7000BlockEntity.ManagerCall c = this.managers.get(at);
            Hmc7000BlockEntity other = this.manager(at);
            if (c != null && other != null && other.managers.containsKey(this.worldPosition)) {
                for (BlockPos old : new ArrayList<>(this.managers.keySet())) {
                    if (!old.equals(at) && this.managers.get(old).connected) {
                        this.endManager(old);
                    }
                }

                if (this.selected >= 0 && this.channels[this.selected] != null && this.channels[this.selected].connected) {
                    this.end(this.selected, true);
                }

                c.connected = true;
                c.age = 0;
                Hmc7000BlockEntity.ManagerCall opposite = other.managers.get(this.worldPosition);
                opposite.connected = true;
                opposite.age = 0;
                this.activeManager = at;
                other.activeManager = this.worldPosition;
                this.page = other.page = "call";
                this.selected = -1;
                other.changed();
                this.changed();
            }
        }
    }

    private void endManager(BlockPos at) {
        this.managers.remove(at);
        Hmc7000BlockEntity other = this.manager(at);
        if (other != null) {
            other.managers.remove(this.worldPosition);
            if (this.worldPosition.equals(other.activeManager)) {
                other.activeManager = null;
            }

            other.changed();
        }

        if (at.equals(this.activeManager)) {
            this.activeManager = null;
        }
    }

    private void hold() {
        if (this.lifted) {
            if (this.held()) {
                this.selected = this.held;
                this.held = -1;
                this.transfer = false;
                this.transferSource = null;
                this.page = "call";
            } else if (this.selected >= 0
                && this.channels[this.selected] != null
                && this.channels[this.selected].connected
                && this.channels[this.selected].frontDoor == null) {
                this.held = this.selected;
                this.transfer = true;
                this.transferSource = this.channels[this.selected].home;
                this.digits = "";
                this.building = "";
                this.route = "house";
                this.page = "dial";
            }

            this.changed();
        }
    }

    @Override
    public void hangUp() {
        if (!this.held() || this.selected != this.held && this.selected >= 0 && this.channels[this.selected] != null && this.channels[this.selected].connected) {
            if (this.activeManager != null) {
                this.endManager(this.activeManager);
            }

            super.hangUp();
            this.held = -1;
            this.preferred = -1;
            this.idle();
            this.changed();
        } else {
            if (this.selected >= 0 && this.selected != this.held && this.channels[this.selected] != null) {
                this.end(this.selected, true);
            }

            this.lifted = false;
            this.selected = this.held;
            this.page = "hold";
            this.digits = "";
            this.building = "";
            this.changed();
        }
    }

    @Override
    protected void end(int i, boolean notify) {
        boolean completed = this.channels[i] != null && this.channels[i].connected;
        super.end(i, notify);
        if (i == this.held) {
            this.held = -1;
            this.transfer = false;
            this.transferSource = null;
        }

        if (!this.held() && this.occupiedChannels() == 0 && this.managers.isEmpty()) {
            if (completed) {
                this.lifted = false;
                this.status = "통화 종료 · 대기";
            }

            this.idle();
        }
    }

    @Override
    protected void sendMessage(ServerPlayer player, String text) {
        if (this.lifted && (!this.held() || this.selected != this.held)) {
            if (this.selected >= 0
                && this.channels[this.selected] != null
                && this.channels[this.selected].origin.equals(this.worldPosition)
                && this.channels[this.selected].connected
                && this.home(this.channels[this.selected].home) instanceof Ha4100hBlockEntity ha) {
                if (this.power && this.lifted && this.valid(ha.getBlockPos()) && ha.talkingToHmc(this) && this.level instanceof ServerLevel server) {
                    Intercom.say(server, this, ha, player, this.callerKey(), text);
                }
            } else if (this.activeManager != null) {
                Hmc7000BlockEntity other = this.manager(this.activeManager);
                Hmc7000BlockEntity.ManagerCall call = this.managers.get(this.activeManager);
                if (other != null && call != null && call.connected && this.level instanceof ServerLevel sl) {
                    Intercom.say(sl, this, other, player, "관리실 " + this.unit, text);
                }
            } else {
                super.sendMessage(player, text);
            }
        }
    }

    @Override
    public void key(String key) {
        if (this.power && key != null && key.length() == 1 && "0123456789*#".contains(key)) {
            if (this.page.equals("clock")) {
                this.clockKey(key);
            } else if (key.equals("*")) {
                if (this.clockArmed) {
                    this.beginClock(false);
                } else if (this.page.equals("wake")) {
                    this.beginClock(true);
                } else if (!this.registration.isEmpty()) {
                    this.digits = "";
                    this.assignBuilding = this.assignRoom = "";
                    this.assignRoomDone = false;
                } else if (this.lifted
                    && !this.held()
                    && this.selected >= 0
                    && this.channels[this.selected] != null
                    && this.channels[this.selected].incoming
                    && this.channels[this.selected].connected
                    && this.channels[this.selected].frontDoor == null) {
                    this.registration = "hmc-unit";
                    this.registering = this.channels[this.selected].home;
                    this.digits = "";
                    this.assignBuilding = this.assignRoom = "";
                    this.assignRoomDone = false;
                    this.page = "assign";
                    this.changed();
                } else {
                    this.digits = "";
                    this.building = "";
                    this.changed();
                }
            } else if (key.equals("#")) {
                if (this.page.equals("wake")) {
                    this.wakeEnabled = false;
                    this.idle();
                    this.changed();
                } else if (!this.registration.isEmpty()) {
                    this.assign();
                } else {
                    this.digits = "";
                    this.building = "";
                    if (!this.lifted) {
                        this.idle();
                    }

                    this.changed();
                }
            } else {
                if (!this.lifted && this.page.equals("standby") && this.digits.isEmpty()) {
                    if (key.equals("1")) {
                        this.clockArmed = true;
                        this.status = "* 입력: 현재시간 변경";
                        this.changed();
                        return;
                    }

                    if (key.equals("2")) {
                        this.page = "version";
                        this.reviewTicks = 100;
                        this.changed();
                        return;
                    }

                    if (key.equals("3")) {
                        this.page = "wake";
                        this.reviewTicks = 100;
                        this.changed();
                        return;
                    }
                }

                if (this.digits.length() < 4) {
                    this.digits = this.digits + key;
                }

                this.changed();
            }
        }
    }

    private void assign() {
        int i = this.find(this.registering);
        ReceiverBlockEntity r = this.home(this.registering);
        if (i >= 0
            && r != null
            && this.channels[i].connected
            && !this.held()
            && this.assignRoomDone
            && UnitAddress.component(this.assignBuilding)
            && UnitAddress.component(this.assignRoom)
            && this.digits.matches("[0-9]{1,2}")) {
            String address = Integer.parseInt(this.assignBuilding) + "-" + Integer.parseInt(this.assignRoom);
            if (!this.homes.entrySet().stream().anyMatch(e -> !e.getKey().equals(this.registering) && e.getValue().equals(address))
                && r.assignUnitFromSwitchboard(this.worldPosition, address)) {
                this.homes.put(this.registering, address);
                this.lobbyNumbers.put(this.registering, String.format("%02d", Integer.parseInt(this.digits)));
                this.registration = "";
                this.registering = null;
                this.digits = "";
                this.page = "call";
                this.changed();
            } else {
                this.status = "중복되거나 유효하지 않은 세대번호";
                this.changed();
            }
        } else {
            this.status = "동·호·현관번호를 확인하세요";
            this.registration = "";
            this.page = "call";
            this.changed();
        }
    }

    public void button(String key, ServerPlayer player) {
        if (key.equals("logo")) {
            if (player != null) {
                this.openLogoEditor(player);
            }
        } else if (key.equals("handset")) {
            this.pickup();
        } else if (key.equals("power")) {
            this.power = !this.power;
            if (!this.power) {
                this.held = -1;

                for (int i = 0; i < 4; i++) {
                    this.end(i, true);
                }

                for (BlockPos at : new ArrayList<>(this.managers.keySet())) {
                    this.endManager(at);
                }

                super.hangUp();
                this.testType = -1;
                this.wakeRinging = false;
            }

            this.changed();
        } else if (this.power) {
            if (key.equals("building") && this.page.equals("clock")) {
                this.nextClockField();
            } else {
                switch (key) {
                    case "house":
                    case "lobby":
                    case "manager":
                    case "central":
                    case "manager1":
                    case "manager2":
                    case "manager3":
                    case "manager4":
                        this.choose(key);
                        break;
                    case "hold":
                        this.hold();
                        break;
                    case "building":
                        if (!this.registration.isEmpty()) {
                            if (UnitAddress.component(this.digits)) {
                                if (this.assignBuilding.isEmpty()) {
                                    this.assignBuilding = this.digits;
                                } else {
                                    this.assignRoom = this.digits;
                                    this.assignRoomDone = true;
                                }

                                this.digits = "";
                            }
                        } else if (UnitAddress.component(this.digits)) {
                            this.building = this.digits;
                            this.digits = "";
                        }
                        break;
                    case "call":
                        if (!this.page.equals("forward") && this.lifted) {
                            if (this.route.equals("house")) {
                                this.page = "dial";
                                if (this.building.isEmpty()) {
                                    this.status = "동 번호를 입력하고 동 버튼을 누르세요";
                                } else {
                                    this.dialDigits();
                                }
                            } else if (this.route.equals("lobby")) {
                                int i = this.nextChannel();
                                if (i >= 0) {
                                    this.answerLine(i);
                                }
                            } else {
                                this.dialManager(
                                    this.route.equals("central")
                                        ? "0"
                                        : (this.route.startsWith("manager") && this.route.length() > 7 ? this.route.substring(7) : this.digits)
                                );
                            }
                        }
                        break;
                    case "mute":
                        this.muted = !this.muted;
                        break;
                    case "test":
                        if (!this.lifted && this.occupiedChannels() == 0 && this.managers.isEmpty()) {
                            this.testType = (this.testType + 1) % 4;
                            this.page = "test";
                            this.muted = false;
                            this.alarmTicks = 0;
                        }
                        break;
                    case "return":
                        if (!this.lifted) {
                            this.testType = -1;
                            this.activeAlarms.clear();
                            this.alarms.clear();
                            this.guardAlerts.clear();
                            this.wakeRinging = false;
                            this.muted = false;
                            this.idle();
                        }
                        break;
                    case "fire":
                    case "gas":
                    case "security":
                    case "emergency":
                        if (!this.lifted) {
                            if (this.page.equals("history") && this.reviewType.equals(key)) {
                                this.reviewIndex++;
                            } else {
                                this.reviewIndex = 0;
                            }

                            this.reviewType = key;
                            this.reviewTicks = 100;
                            this.page = "history";
                        }
                        break;
                    case "forward":
                        if (!this.lifted) {
                            if (this.absent) {
                                this.absent = false;
                                this.status = "착신 전환 해제";
                            } else {
                                this.page = "forward";
                                this.digits = "";
                                this.status = "전환할 관리실 번호 입력 후 호출";
                            }
                        }
                        break;
                    case "print":
                        this.status = "경보 기록 " + this.archive.size() + "건";
                        if (player != null) {
                            for (Hmc7000BlockEntity.Alarm a : this.archive) {
                                player.sendSystemMessage(Component.literal(a.type + " " + a.unit + " " + this.stamp(a.epoch)));
                            }
                        }
                }

                if (key.equals("call") && this.page.equals("forward")) {
                    Hmc7000BlockEntity target = this.findManager(this.digits);
                    if (target != null && target != this) {
                        this.absentTarget = target.worldPosition;
                        this.absent = true;
                        this.idle();
                        this.status = "착신 전환 설정";
                    } else {
                        this.status = "전환할 관리실을 찾을 수 없습니다";
                    }
                }

                this.changed();
            }
        }
    }

    private Hmc7000BlockEntity findManager(String id) {
        if (this.level instanceof ServerLevel sl) {
            for (DeviceRegistry.Entry e : DeviceRegistry.get(sl).all()) {
                Hmc7000BlockEntity b = this.manager(e.pos());
                if (b != null && b.unit.equals(id)) {
                    return b;
                }
            }
        }

        return null;
    }

    private void beginClock(boolean wake) {
        this.editWake = wake;
        this.clockField = 0;
        this.editDigits = "";
        LocalDateTime t = this.now();
        this.clockEdit[0] = t.getMonthValue();
        this.clockEdit[1] = t.getDayOfMonth();
        this.clockEdit[2] = wake ? (this.wakeHour % 12 == 0 ? 12 : this.wakeHour % 12) : (t.getHour() % 12 == 0 ? 12 : t.getHour() % 12);
        this.clockEdit[3] = wake ? this.wakeMinute : t.getMinute();
        this.clockEdit[4] = (wake ? this.wakeHour : t.getHour()) >= 12 ? 1 : 0;
        this.clockEdit[5] = Math.floorMod(t.getDayOfWeek().getValue() % 7 + this.weekdayOffset, 7);
        this.page = "clock";
        this.reviewTicks = 0;
        this.clockArmed = false;
        this.changed();
    }

    private int editIndex() {
        return this.editWake ? this.clockField + 2 : this.clockField;
    }

    private void clockKey(String key) {
        int i = this.editIndex();
        if (key.equals("#")) {
            int hour = this.clockEdit[2] % 12 + this.clockEdit[4] * 12;

            try {
                if (this.clockEdit[2] < 1 || this.clockEdit[2] > 12 || this.clockEdit[3] > 59) {
                    throw new IllegalArgumentException();
                }

                if (this.editWake) {
                    this.wakeHour = hour;
                    this.wakeMinute = this.clockEdit[3];
                    this.wakeEnabled = true;
                    this.wakeDay = Long.MIN_VALUE;
                } else {
                    LocalDateTime t = LocalDateTime.of(this.now().getYear(), this.clockEdit[0], this.clockEdit[1], hour, this.clockEdit[3]);
                    this.rtcEpoch = t.toEpochSecond(ZoneOffset.UTC);
                    this.rtcTick = this.level.getGameTime();
                    this.weekdayOffset = Math.floorMod(this.clockEdit[5] - t.getDayOfWeek().getValue() % 7, 7);
                }

                this.idle();
            } catch (RuntimeException var5) {
                this.status = "유효한 날짜와 시간을 입력하세요";
            }

            this.changed();
        } else if (key.equals("0") && i >= 4) {
            this.clockEdit[i] = (this.clockEdit[i] + 1) % (i == 4 ? 2 : 7);
            this.changed();
        } else if (key.equals("*")) {
            this.editDigits = "";
        } else {
            if (i < 4 && key.matches("[0-9]")) {
                this.editDigits = this.editDigits + key;
                if (this.editDigits.length() > 2) {
                    this.editDigits = this.editDigits.substring(this.editDigits.length() - 2);
                }

                this.clockEdit[i] = Integer.parseInt(this.editDigits);
                this.changed();
            }
        }
    }

    private void nextClockField() {
        this.clockField = (this.clockField + 1) % (this.editWake ? 3 : 6);
        this.editDigits = "";
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
            } else if (k.equals("button")) {
                this.button(v, player);
            } else if (k.equals("next")) {
                this.nextClockField();
            }
        } else if (action == ReceiverBlockEntity.Action.SET_UNIT) {
            if (text != null && text.matches("[0-9]{1,4}")) {
                String id = String.valueOf(Integer.parseInt(text));
                Hmc7000BlockEntity existing = this.findManager(id);
                if (existing != null && existing != this) {
                    this.status = "사용 중인 관리실 번호";
                } else {
                    this.unit = id;
                    this.register();
                    this.status = "관리실 번호 저장";
                }
            } else {
                this.status = "관리실 번호는 0~9999입니다";
            }

            this.changed();
        } else if (action == ReceiverBlockEntity.Action.PICKUP) {
            this.pickup();
        } else if (action == ReceiverBlockEntity.Action.HANG_UP) {
            this.hangUp();
        } else if (action == ReceiverBlockEntity.Action.SEND_MESSAGE) {
            this.sendMessage(player, text);
        }
    }

    @Override
    public void setEmergency(BlockPos pos, String n, boolean active) {
        this.setSafetyEvent(pos, n, "emergency", active);
    }

    @Override
    public void setSafetyEvent(BlockPos pos, String n, String raw, boolean active) {
        if (this.valid(pos)) {
            String type = switch (raw) {
                case "FIR", "FIRE", "fire" -> "fire";
                case "GAS", "gas" -> "gas";
                case "BB1", "BB2", "BB3", "SECURITY", "security" -> "security";
                default -> "emergency";
            };
            String key = pos.asLong() + ":" + type;
            if (!active) {
                this.alarmEdges.remove(key);
                this.activeAlarms.remove(key);
                if (this.activeAlarms.values().stream().noneMatch(a -> a.source.equals(pos))) {
                    this.alarms.remove(pos);
                }

                this.changed();
            } else if (!Boolean.TRUE.equals(this.alarmEdges.put(key, true))) {
                this.interruptPanels();
                Hmc7000BlockEntity.Alarm alarm = new Hmc7000BlockEntity.Alarm(type, n, pos.immutable(), this.epoch());
                this.activeAlarms.put(key, alarm);
                this.archive.add(alarm);
                if (this.archive.size() > 1024) {
                    this.archive.remove(0);
                }

                this.alarms.put(pos.immutable(), n);
                this.muted = false;
                this.alarmTicks = 0;
                this.changed();
            }
        }
    }

    private String stamp(long time) {
        return LocalDateTime.ofEpochSecond(time, 0, ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("MM/dd HH:mm"));
    }

    private Hmc7000BlockEntity.Alarm latest() {
        return this.activeAlarms.isEmpty() ? null : new ArrayList<>(this.activeAlarms.values()).get(this.activeAlarms.size() - 1);
    }

    public String[] lcd() {
        if (this.level != null && this.level.isClientSide) {
            return (String[])this.clientLcd.clone();
        } else if (!this.power) {
            return new String[]{"", ""};
        } else if (this.page.equals("clock")) {
            String line = this.editWake
                ? String.format("%02d:%02d%s", this.clockEdit[2], this.clockEdit[3], this.clockEdit[4] == 0 ? "a" : "p")
                : String.format(
                    "%02d/%02d %02d:%02d%s %s",
                    this.clockEdit[0],
                    this.clockEdit[1],
                    this.clockEdit[2],
                    this.clockEdit[3],
                    this.clockEdit[4] == 0 ? "a" : "p",
                    new String[]{"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"}[this.clockEdit[5]]
                );
            if (this.ticks % 20 >= 10) {
                int start = this.editWake ? new int[]{0, 3, 5}[this.clockField] : new int[]{0, 3, 6, 9, 11, 13}[this.clockField];
                int length = this.editWake ? (this.clockField == 2 ? 1 : 2) : (this.clockField == 4 ? 1 : (this.clockField == 5 ? 3 : 2));
                line = line.substring(0, start) + " ".repeat(length) + line.substring(start + length);
            }

            return new String[]{this.editWake ? "MORNING CALL" : "[GRDP #" + this.unit + "]", line};
        } else if (this.page.equals("version")) {
            return new String[]{"S/W VERSION", this.version};
        } else if (this.page.equals("wake")) {
            return new String[]{
                "MORNING CALL",
                this.wakeEnabled
                    ? String.format("%02d:%02d %s", this.wakeHour % 12 == 0 ? 12 : this.wakeHour % 12, this.wakeMinute, this.wakeHour >= 12 ? "PM" : "AM")
                    : "NONE"
            };
        } else if (this.page.equals("history")) {
            List<Hmc7000BlockEntity.Alarm> items = this.archive.stream().filter(ax -> ax.type.equals(this.reviewType)).toList();
            if (items.isEmpty()) {
                return new String[]{this.reviewType.toUpperCase(), "NO RECORD"};
            } else {
                Hmc7000BlockEntity.Alarm a = items.get(items.size() - 1 - Math.floorMod(this.reviewIndex, items.size()));
                return new String[]{a.type + ":" + a.unit, this.stamp(a.epoch)};
            }
        } else if (!this.registration.isEmpty()) {
            return new String[]{"edit room_no", this.assignBuilding + "-" + this.assignRoom + "-" + this.digits};
        } else if (!this.held() || this.selected != this.held && this.selected >= 0) {
            if (this.activeManager != null
                && this.manager(this.activeManager) != null
                && this.managers.containsKey(this.activeManager)
                && this.managers.get(this.activeManager).connected) {
                return new String[]{"> mgr comm", "#" + this.manager(this.activeManager).unit};
            } else {
                int index = this.selected >= 0 && this.selected < 4 && this.channels[this.selected] != null ? this.selected : this.nextChannel();
                if (index >= 0) {
                    HyundaiBusConsoleBlockEntity.Channel c = this.channels[index];
                    return new String[]{
                        c.frontDoor != null
                            ? (c.connected ? "> lobby comm" : "> lobby calling")
                            : (
                                c.connected
                                    ? "> room comm"
                                    : (c.incoming ? "> room calling" : (this.away.contains(c.home) ? "> room is exit" : "> dial room_no"))
                            ),
                        this.number(c)
                    };
                } else {
                    BlockPos mg = this.activeManager != null ? this.activeManager : this.nextManager();
                    if (mg != null && this.manager(mg) != null) {
                        Hmc7000BlockEntity.ManagerCall m = this.managers.get(mg);
                        return new String[]{
                            m.connected ? "> mgr comm" : (m.incoming ? "> call from #" + this.manager(mg).unit : "> call to #" + this.manager(mg).unit), ""
                        };
                    } else if (this.testType >= 0) {
                        return new String[]{"TEST " + TYPES[this.testType], ""};
                    } else {
                        Hmc7000BlockEntity.Alarm a = this.latest();
                        if (a != null) {
                            return new String[]{a.type + ", " + a.type, a.unit};
                        } else {
                            return !this.lifted && !this.page.equals("forward")
                                ? new String[]{
                                    "[GRDP #" + this.unit + "]",
                                    this.now().format(DateTimeFormatter.ofPattern("MM/dd hh:mm", Locale.ENGLISH))
                                        + (this.now().getHour() >= 12 ? "p " : "a ")
                                        + new String[]{"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"}[Math.floorMod(
                                            this.now().getDayOfWeek().getValue() % 7 + this.weekdayOffset, 7
                                        )]
                                }
                                : new String[]{
                                    this.page.equals("forward") ? "CALL FORWARD" : "> dial " + this.route,
                                    this.building.isEmpty() ? this.digits : this.building + "-" + this.digits
                                };
                        }
                    }
                }
            }
        } else {
            return this.digits.isEmpty() && this.building.isEmpty()
                ? new String[]{"> room hold", this.number(this.channels[this.held])}
                : new String[]{"> dial room_no", this.building + "-" + this.digits};
        }
    }

    public boolean led(int index) {
        if (this.level != null && this.level.isClientSide) {
            return index >= 0 && index < this.clientLights.length && this.clientLights[index];
        } else if (!this.power) {
            return false;
        } else if (index < 4) {
            String type = TYPES[index];
            return (this.testType == index || this.activeAlarms.values().stream().anyMatch(a -> a.type.equals(type))) && this.ticks % 20 < 10;
        } else if (index == 4) {
            return this.held() && this.ticks % 20 < 10;
        } else if (index == 5) {
            return this.absent;
        } else if (index != 6) {
            if (index == 7) {
                return this.channelsFor(false);
            } else if (index == 8) {
                return this.channelsFor(true);
            } else if (index >= 9 && index < 14) {
                String id = index == 13 ? "0" : String.valueOf(index - 8);
                return this.managers
                    .entrySet()
                    .stream()
                    .anyMatch(
                        e -> this.manager(e.getKey()) != null && this.manager(e.getKey()).unit.equals(id) && (e.getValue().connected || this.ticks % 20 < 10)
                    );
            } else {
                return index == 14 ? this.power : false;
            }
        } else {
            return this.testType >= 0 && this.ticks % 20 < 10;
        }
    }

    private boolean channelsFor(boolean lobby) {
        for (HyundaiBusConsoleBlockEntity.Channel c : this.channels) {
            if (c != null && c.frontDoor != null == lobby && (c.connected || this.ticks % 20 < 10)) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected Map<String, String> settingsForClient() {
        HashMap<String, String> s = new HashMap<>();
        s.put("hmc7000", "1");
        s.put("power", this.power ? "1" : "0");
        s.put("lifted", this.lifted ? "1" : "0");
        s.put("status", this.status);
        s.put("page", this.page);
        s.put("angle", this.angle + "");
        s.put("clock_field", this.editIndex() + "");
        s.put("clock_edit", this.page.equals("clock") ? "1" : "0");
        s.put("registered", this.homes.size() + "");
        s.put("held", this.held() ? "1" : "0");
        s.put("version", this.version);
        String[] lines = this.lcd();
        s.put("lcd0", lines[0]);
        s.put("lcd1", lines[1]);

        for (int i = 0; i < 15; i++) {
            s.put("led" + i, this.led(i) ? "1" : "0");
        }

        return s;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putInt("HmcAngle", this.angle);
        tag.putString("HmcLcd0", this.lcd()[0]);
        tag.putString("HmcLcd1", this.lcd()[1]);

        for (int i = 0; i < 15; i++) {
            tag.putBoolean("HmcLed" + i, this.led(i));
        }

        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        this.angle = tag.getInt("HmcAngle");
        this.clientLcd[0] = tag.getString("HmcLcd0");
        this.clientLcd[1] = tag.getString("HmcLcd1");

        for (int i = 0; i < 15; i++) {
            this.clientLights[i] = tag.getBoolean("HmcLed" + i);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, Hmc7000BlockEntity b) {
        b.ticks++;
        if (b.power) {
            b.activeAlarms.values().removeIf(a -> !b.scoped(a.source));
            b.alarms.keySet().removeIf(px -> !b.scoped(px));
            b.away.removeIf(px -> !b.scoped(px));

            for (int i = 0; i < 4; i++) {
                HyundaiBusConsoleBlockEntity.Channel c = b.channels[i];
                if (c != null) {
                    c.age++;
                    if (c.forwardedTo != null && b.manager(c.forwardedTo) == null
                        || !b.channelValid(c)
                        || (
                            c.frontDoor != null
                                ? !(level.getBlockEntity(c.frontDoor) instanceof CallerBlockEntity camera) || !camera.isInCall()
                                : b.home(c.home) == null || !b.home(c.home).hasCallerSession(c.origin)
                        )) {
                        b.end(i, true);
                    } else if (c.age > (c.connected ? 3600 : 600) && i != b.held) {
                        b.end(i, true);
                    }
                }
            }

            for (BlockPos at : new ArrayList<>(b.managers.keySet())) {
                Hmc7000BlockEntity.ManagerCall c = b.managers.get(at);
                Hmc7000BlockEntity other = b.manager(at);
                if (other == null || !other.managers.containsKey(pos) || ++c.age > (c.connected ? 3600 : 600)) {
                    b.endManager(at);
                }
            }

            if (b.reviewTicks > 0 && --b.reviewTicks == 0) {
                b.idle();
            }

            LocalDateTime now = b.now();
            long day = Math.floorDiv(b.epoch(), 86400);
            if (b.wakeEnabled
                && b.page.equals("standby")
                && b.activeAlarms.isEmpty()
                && b.testType < 0
                && !b.lifted
                && b.occupiedChannels() == 0
                && b.managers.isEmpty()
                && now.getHour() == b.wakeHour
                && now.getMinute() == b.wakeMinute
                && b.wakeDay != day) {
                b.wakeDay = day;
                b.wakeRinging = true;
                b.changed();
            }

            boolean pending = b.nextChannel() >= 0 || b.nextManager() != null;
            if (b.lifted
                && pending
                && b.ticks % 20 == 0
                && Arrays.stream(b.channels).anyMatch(cx -> cx != null && cx.incoming && !cx.connected && cx.age <= 60)) {
                CallSounds.note(level, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), 0.45F, 1.19F);
            }

            if (b.wakeRinging) {
                CallSounds.incoming(level, pos, true, b.ticks, 0.6F);
            } else if (!b.lifted && !b.held() && pending && b.activeAlarms.isEmpty()) {
                CallSounds.incoming(level, pos, true, b.ticks, 0.6F);
            }

            boolean outgoing = Arrays.stream(b.channels).anyMatch(cx -> cx != null && !cx.incoming && !cx.connected)
                || b.managers.values().stream().anyMatch(cx -> !cx.incoming && !cx.connected);
            if (outgoing) {
                CallSounds.ringback(level, pos, b.ticks, 0.45F);
            }

            if ((b.testType >= 0 || !b.activeAlarms.isEmpty()) && !b.muted && (!b.held() || !b.lifted)) {
                int t = b.alarmTicks++ % 240;
                int type = b.testType >= 0 ? b.testType : Arrays.asList(TYPES).indexOf(b.latest().type);
                if (t < 60 && t % 5 == 0) {
                    CallSounds.note(level, pos, SoundEvents.NOTE_BLOCK_BELL.value(), 0.7F, (float)(0.6 + 0.5 * Math.sin(t * 0.25)));
                }

                if (t == 60 || t == 120 || t == 180) {
                    for (ServerPlayer p : Intercom.playersNear((ServerLevel)level, pos, 8.0)) {
                        p.displayClientMessage(Component.literal(ALARM_WORDS[Math.max(0, type)]), true);
                    }
                }
            }

            if (b.ticks % 5 == 0) {
                b.changed();
            }
        }
    }

    @Override
    public void onUnlinked(BlockPos pos) {
        this.activeAlarms.values().removeIf(a -> a.source.equals(pos));
        this.alarmEdges.keySet().removeIf(key -> key.startsWith(pos.asLong() + ":"));
        this.lobbyNumbers.remove(pos);
        this.waitingMessages.remove(pos);
        ReceiverBlockEntity r = this.home(pos);
        if (r != null) {
            r.getPersistentData().remove("HmcGuardMessage");
            r.setChanged();
        }

        super.onUnlinked(pos);
    }

    @Override
    public void onBroken() {
        this.held = -1;

        for (BlockPos at : new ArrayList<>(this.managers.keySet())) {
            this.endManager(at);
        }

        super.onBroken();
    }

    @Override
    protected void saveAdditional(CompoundTag t) {
        super.saveAdditional(t);
        t.putInt("HmcAngle", this.angle);
        t.putInt("HmcWeekday", this.weekdayOffset);
        t.putLong("HmcEpoch", this.epoch());
        t.putLong("HmcClockTick", this.level == null ? this.rtcTick : this.level.getGameTime());
        t.putBoolean("WakeEnabled", this.wakeEnabled);
        t.putInt("WakeHour", this.wakeHour);
        t.putInt("WakeMinute", this.wakeMinute);
        t.putLong("WakeDay", this.wakeDay);
        ListTag records = new ListTag();

        for (Hmc7000BlockEntity.Alarm a : this.archive) {
            CompoundTag c = new CompoundTag();
            c.putString("Type", a.type);
            c.putString("Unit", a.unit);
            c.putLong("P", a.source.asLong());
            c.putLong("Time", a.epoch);
            records.add(c);
        }

        t.put("HmcAlarms", records);
        ListTag rooms = new ListTag();
        this.lobbyNumbers.forEach((p, n) -> {
            CompoundTag cx = new CompoundTag();
            cx.putLong("P", p.asLong());
            cx.putString("Lobby", n);
            rooms.add(cx);
        });
        t.put("HmcRooms", rooms);
        t.putLongArray("HmcMessages", this.waitingMessages.stream().mapToLong(BlockPos::asLong).toArray());
    }

    @Override
    public void load(CompoundTag t) {
        super.load(t);
        this.angle = Math.max(0, Math.min(65, t.contains("HmcAngle") ? t.getInt("HmcAngle") : 35));
        this.weekdayOffset = Math.floorMod(t.getInt("HmcWeekday"), 7);
        this.rtcEpoch = t.contains("HmcEpoch") ? t.getLong("HmcEpoch") : 946684800L;
        this.rtcTick = t.getLong("HmcClockTick");
        this.wakeEnabled = t.getBoolean("WakeEnabled");
        this.wakeHour = Math.max(0, Math.min(23, t.contains("WakeHour") ? t.getInt("WakeHour") : 6));
        this.wakeMinute = Math.max(0, Math.min(59, t.getInt("WakeMinute")));
        this.wakeDay = t.contains("WakeDay") ? t.getLong("WakeDay") : Long.MIN_VALUE;
        this.archive.clear();

        for (Tag row : t.getList("HmcAlarms", 10)) {
            CompoundTag c = (CompoundTag)row;
            if (this.archive.size() < 1024) {
                this.archive.add(new Hmc7000BlockEntity.Alarm(c.getString("Type"), c.getString("Unit"), BlockPos.of(c.getLong("P")), c.getLong("Time")));
            }
        }

        this.lobbyNumbers.clear();

        for (Tag rowx : t.getList("HmcRooms", 10)) {
            CompoundTag c = (CompoundTag)rowx;
            this.lobbyNumbers.put(BlockPos.of(c.getLong("P")), c.getString("Lobby"));
        }

        this.waitingMessages.clear();

        for (long p : t.getLongArray("HmcMessages")) {
            this.waitingMessages.add(BlockPos.of(p));
        }

        this.managers.clear();
        this.activeManager = null;
        this.activeAlarms.clear();
        this.alarmEdges.clear();
        this.held = -1;
        this.testType = -1;
        this.wakeRinging = false;
        this.idle();
    }

    static {
        DeviceMoves.registerPositionArray("HmcMessages");
    }

    private record Alarm(String type, String unit, BlockPos source, long epoch) {
    }

    public static enum HomeCallResult {
        ACCEPTED,
        POWER_OFF,
        NOT_LINKED,
        NO_LINE,
        BUSY;
    }

    private static final class ManagerCall {
        boolean incoming;
        boolean connected;
        int age;

        ManagerCall(boolean incoming) {
            this.incoming = incoming;
        }
    }
}
