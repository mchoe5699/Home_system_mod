package com.qwerty.homenet.sht;

import com.qwerty.homenet.analog.AnalogBus;
import com.qwerty.homenet.blockentity.CallerBlockEntity;
import com.qwerty.homenet.blockentity.LobbyPhoneBlockEntity;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.blockentity.Sht5500sBlockEntity;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.data.ZoneData;
import com.qwerty.homenet.guard.GuardExchange;
import com.qwerty.homenet.guard.GuardSystem;
import com.qwerty.homenet.intercom.CallState;
import com.qwerty.homenet.intercom.DoorControl;
import com.qwerty.homenet.intercom.Intercom;
import com.qwerty.homenet.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
    modid = "homenet_sht101c"
)
public final class Sht5500s extends Sht5500sBlockEntity implements GuardExchange {
    private final Map<BlockPos, String> homes = new HashMap<>();
    private final Map<BlockPos, String> alerts = new HashMap<>();
    private final Map<BlockPos, Boolean> awayHomes = new HashMap<>();
    private final Map<Integer, String> shortcuts = new HashMap<>();
    private final List<String> alarmHistory = new ArrayList<>();
    private final List<Long> alarmTimes = new ArrayList<>();
    private final List<String> accessHistory = new ArrayList<>();
    private final List<Long> accessTimes = new ArrayList<>();
    private final Set<BlockPos> nightOccupied = new HashSet<>();
    private int page;
    private String awayFilter = "";
    private long doorLampUntil;
    private String forwarding = "";
    private String status = "대기";
    private String operation = "house";
    private String entry = "";
    private String clockLabel = "";
    private int shortcutSlot;
    private boolean handsetUp;
    private boolean muted;
    private boolean night;
    private int volume = 4;
    private BlockPos transferSource;
    private BlockPos transferTarget;

    @SubscribeEvent(
        priority = EventPriority.HIGHEST
    )
    public static void openExisting(RightClickBlock event) {
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            BlockEntity existing = event.getLevel().getBlockEntity(event.getPos());
            if (existing instanceof Sht5500sBlockEntity) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                if (event.getLevel().isClientSide) {
                    Wire.guardOpen(event.getPos());
                } else {
                    if (event.getEntity() instanceof ServerPlayer player) {
                        openAt(player, event.getPos());
                    }
                }
            }
        }
    }

    static void openAt(ServerPlayer player, BlockPos pos) {
        if (player.level().isLoaded(pos) && Intercom.inReach(player, pos)) {
            BlockEntity existing = player.level().getBlockEntity(pos);
            if (existing instanceof Sht5500sBlockEntity) {
                Sht5500s guard;
                if (existing instanceof Sht5500s upgraded) {
                    guard = upgraded;
                } else {
                    guard = new Sht5500s(pos, existing.getBlockState());
                    guard.load(existing.saveWithoutMetadata());
                    player.level().setBlockEntity(guard);
                    guard.onLoad();
                }

                guard.openFor(player);
            }
        }
    }

    public Sht5500s(BlockPos p, BlockState s) {
        super(p, s);
        this.unit = "101";
    }

    public static <T extends BlockEntity> BlockEntityTicker<T> ticker(Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide && type == ModBlockEntities.SHT_5500S.get() ? (world, pos, block, entity) -> {
            if (entity instanceof Sht5500s guard) {
                ReceiverBlockEntity.serverTick(world, pos, block, guard);
            }
        } : null;
    }

    @Override
    public DeviceRegistry.Kind kind() {
        return DeviceRegistry.Kind.GUARD_CONSOLE;
    }

    @Override
    public String modelId() {
        return "SHT-5500S";
    }

    @Override
    public GuardSystem guardSystem() {
        return GuardSystem.BUS;
    }

    @Override
    public boolean supports(AnalogBus bus) {
        return bus == AnalogBus.COMMON_8_WIRE || bus == AnalogBus.COMMON_4_WIRE;
    }

    @Override
    public boolean canSetUnitLocally() {
        return true;
    }

    @Override
    protected void register() {
        if (this.level instanceof ServerLevel sl) {
            DeviceRegistry.get(sl).register(this.worldPosition, this.kind(), this.unit);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        CompoundTag d = this.getPersistentData();
        this.forwarding = d.getString("5500Forward");
        this.muted = d.getBoolean("5500Muted");
        this.night = d.getBoolean("5500Night");
        this.volume = d.contains("5500Volume") ? d.getInt("5500Volume") : 4;
        this.status = d.getString("5500Status");
        this.clockLabel = d.getString("5500Clock");
        this.shortcuts.clear();

        for (int i = 1; i <= 17; i++) {
            if (d.contains("5500Shortcut" + i)) {
                this.shortcuts.put(i, d.getString("5500Shortcut" + i));
            }
        }

        this.alarmHistory.clear();
        this.alarmTimes.clear();
        this.accessHistory.clear();
        this.accessTimes.clear();
        loadLog(d, "5500Alarm", this.alarmHistory, this.alarmTimes);
        loadLog(d, "5500Access", this.accessHistory, this.accessTimes);
        if (this.status.isEmpty()) {
            this.status = "대기";
        }
    }

    private static void loadLog(CompoundTag data, String key, List<String> labels, List<Long> times) {
        ListTag rows = data.getList(key, 10);

        for (int i = 0; i < rows.size(); i++) {
            CompoundTag row = rows.getCompound(i);
            labels.add(row.getString("text"));
            times.add(row.getLong("tick"));
        }
    }

    private static void saveLog(CompoundTag data, String key, List<String> labels, List<Long> times) {
        ListTag rows = new ListTag();

        for (int i = 0; i < labels.size(); i++) {
            CompoundTag row = new CompoundTag();
            row.putString("text", labels.get(i));
            row.putLong("tick", times.get(i));
            rows.add(row);
        }

        data.put(key, rows);
    }

    private void changed() {
        CompoundTag d = this.getPersistentData();
        d.putString("5500Forward", this.forwarding);
        d.putBoolean("5500Muted", this.muted);
        d.putBoolean("5500Night", this.night);
        d.putInt("5500Volume", this.volume);
        d.putString("5500Status", this.status);
        d.putString("5500Clock", this.clockLabel);

        for (int i = 1; i <= 17; i++) {
            if (this.shortcuts.containsKey(i)) {
                d.putString("5500Shortcut" + i, this.shortcuts.get(i));
            }
        }

        saveLog(d, "5500Alarm", this.alarmHistory, this.alarmTimes);
        saveLog(d, "5500Access", this.accessHistory, this.accessTimes);
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    public boolean startRinging(BlockPos source, String key) {
        if (!this.isBusy() && this.level != null && ZoneData.connected(this.level, this.worldPosition, source)) {
            this.callState = CallState.RINGING;
            this.outgoing = false;
            this.caller = source.immutable();
            this.incomingKey = key;
            this.callTicks = 0;
            this.log.clear();
            String address = this.level.isLoaded(source) && this.level.getBlockEntity(source) instanceof ReceiverBlockEntity home ? home.getUnit() : "";
            this.status = "guard".equals(key) ? "세대 호출: " + (address.isBlank() ? source.toShortString() : address) : "착신: " + key;
            this.setRingingState(true);
            this.level.playSound(null, this.worldPosition, SoundEvents.NOTE_BLOCK_BELL.get(), SoundSource.BLOCKS, Math.max(0.2F, this.volume / 8.0F), 1.2F);
            this.changed();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void answer() {
        if (this.callState == CallState.RINGING
            && this.caller != null
            && this.level.isLoaded(this.caller)
            && this.level.getBlockEntity(this.caller) instanceof ReceiverBlockEntity r
            && r.acceptSwitchboardCall(this.worldPosition)) {
            this.callState = CallState.CONNECTED;
            this.callTicks = 0;
            this.setRingingState(false);
        } else {
            super.answer();
        }

        this.status = this.callState == CallState.CONNECTED ? "통화 중" : "대기";
        this.changed();
    }

    @Override
    public void hangUp() {
        BlockPos peer = this.caller;
        boolean connected = this.callState != CallState.IDLE;
        super.hangUp();
        if (connected
            && peer != null
            && this.level != null
            && this.level.isLoaded(peer)
            && this.level.getBlockEntity(peer) instanceof ReceiverBlockEntity r
            && r.getCallState() != CallState.IDLE) {
            r.endCallerSession(this.worldPosition, false);
        }

        this.transferSource = null;
        this.transferTarget = null;
        this.status = "대기";
        this.changed();
    }

    @Override
    public boolean onAnswered(BlockPos at) {
        return at.equals(this.transferTarget) ? true : super.onAnswered(at);
    }

    @Override
    protected void tickExtra() {
        super.tickExtra();
        this.tickNight();
        if (this.transferTarget != null
            && this.transferSource != null
            && this.level != null
            && this.level.isLoaded(this.transferTarget)
            && this.level.isLoaded(this.transferSource)) {
            BlockEntity source = this.level.getBlockEntity(this.transferSource);
            BlockEntity target = this.level.getBlockEntity(this.transferTarget);
            if (source instanceof ReceiverBlockEntity first && target instanceof ReceiverBlockEntity second && second.getCallState() == CallState.CONNECTED) {
                first.bindSwitchboard(this.worldPosition);
                second.bindSwitchboard(this.worldPosition);
                if (first.bridgeThroughGuard(this.worldPosition, second)) {
                    this.resetCall();
                    this.status = "세대 간 전환 완료";
                } else {
                    this.status = "전환 실패";
                }

                this.transferSource = null;
                this.transferTarget = null;
                this.changed();
            }
        }
    }

    private void tickNight() {
        if (this.night && this.level instanceof ServerLevel server) {
            if (this.callState == CallState.RINGING && this.incomingKey.equals("night") && this.callTicks >= 300) {
                this.hangUp();
                this.status = "야간감시중";
                this.changed();
            }

            if (server.getGameTime() % 20L == 0L) {
                HashSet<BlockPos> occupied = new HashSet<>();

                for (BlockPos at : DeviceRegistry.get(server).lobbies(server, this.worldPosition)) {
                    if (server.isLoaded(at)
                        && ZoneData.connected(server, this.worldPosition, at)
                        && server.getBlockEntity(at) instanceof CallerBlockEntity lobby
                        && !lobby.isInCall()) {
                        boolean nearby = !server.getEntitiesOfClass(ServerPlayer.class, new AABB(at).inflate(3.0)).isEmpty();
                        if (nearby) {
                            occupied.add(at);
                            if (!this.nightOccupied.contains(at) && this.callState == CallState.IDLE && this.startRinging(at, "night")) {
                                this.status = "야간감시: 공동현관 접근";
                                this.changed();
                            }
                        }
                    }
                }

                this.nightOccupied.retainAll(occupied);
                this.nightOccupied.addAll(occupied);
            }
        }
    }

    @Override
    public void endCallerSession(BlockPos at, boolean missed) {
        super.endCallerSession(at, missed);
        this.status = "대기";
        this.changed();
    }

    @Override
    public void callFrom(ReceiverBlockEntity home, ServerPlayer player) {
        if (this.level != null && ZoneData.connected(this.level, this.worldPosition, home.getBlockPos()) && home.supportsSwitchboard(this)) {
            if (!this.forwarding.isEmpty() && this.level instanceof ServerLevel sl) {
                for (BlockPos at : DeviceRegistry.get(sl).guards(sl, this.worldPosition, false)) {
                    if (!at.equals(this.worldPosition)
                        && sl.isLoaded(at)
                        && ZoneData.connected(sl, this.worldPosition, at)
                        && sl.getBlockEntity(at) instanceof ReceiverBlockEntity destination
                        && DeviceRegistry.digits(destination.getUnit()).equals(this.forwarding)
                        && sl.getBlockEntity(at) instanceof Sht5500s other
                        && other.forwarding.isEmpty()
                        && !other.isBusy()) {
                        if (home instanceof Station station) {
                            station.forwardGuardTo(other, player);
                        } else {
                            home.callGuard(player, "no:" + this.forwarding);
                        }

                        this.status = "부재 우회: " + this.forwarding;
                        this.changed();
                        return;
                    }
                }

                home.endCallerSession(this.worldPosition, false);
                this.status = "우회 경비실 응답 없음";
                this.changed();
            } else {
                if (!this.startRinging(home.getBlockPos(), "guard")) {
                    home.endCallerSession(this.worldPosition, false);
                    this.status = "통화 중";
                    this.changed();
                }
            }
        }
    }

    @Override
    public void onLinked(BlockPos at, String address) {
        if (this.level != null && ZoneData.connected(this.level, this.worldPosition, at)) {
            this.homes.put(at, address);
            this.changed();
        }
    }

    @Override
    public void onUnlinked(BlockPos at) {
        this.homes.remove(at);
        this.awayHomes.remove(at);
        this.alerts.remove(at);
        this.changed();
    }

    @Override
    public String assignedUnit(BlockPos at) {
        return this.homes.getOrDefault(at, "");
    }

    @Override
    public void setEmergency(BlockPos at, String address, boolean active) {
        this.setSafetyEvent(at, address, "비상", active);
    }

    @Override
    public void setSafetyEvent(BlockPos at, String address, String type, boolean active) {
        if (this.level != null && ZoneData.connected(this.level, this.worldPosition, at)) {
            String name = switch (type) {
                case "fire" -> "화재";
                case "gas" -> "가스";
                case "security" -> "방범";
                case "emergency" -> "비상";
                default -> type;
            };
            String label = address + " " + name;
            if (active) {
                this.alerts.put(at, label);
                this.alarmHistory.add(0, label);
                this.alarmTimes.add(0, this.level.getGameTime());
                if (this.alarmHistory.size() > 250) {
                    this.alarmHistory.remove(250);
                    this.alarmTimes.remove(250);
                }

                this.muted = false;
            } else {
                this.alerts.remove(at);
            }

            this.status = active ? name + ": " + address : "경보 복귀";
            this.changed();
        }
    }

    @Override
    public void setAway(BlockPos at, String address, boolean active) {
        if (this.level != null && ZoneData.connected(this.level, this.worldPosition, at)) {
            if (active) {
                this.awayHomes.put(at, true);
            } else {
                this.awayHomes.remove(at);
            }

            this.status = active ? "부재: " + address : "부재 해제: " + address;
            this.changed();
        }
    }

    @Override
    public void openFor(ServerPlayer p) {
        if (Intercom.inReach(p, this.worldPosition)) {
            Wire.guardView(p, this);
        }
    }

    public String display() {
        return this.status;
    }

    public String forward() {
        return this.forwarding;
    }

    public boolean muted() {
        return this.muted;
    }

    public boolean night() {
        return this.night;
    }

    public int volume() {
        return this.volume;
    }

    public int alertCount() {
        return this.alerts.size();
    }

    public int awayCount() {
        return this.awayHomes.size();
    }

    public int lamps() {
        int mask = 1;
        if (this.night) {
            mask |= 128;
        }

        if (!this.forwarding.isEmpty()) {
            mask |= 64;
        }

        if (this.level != null && this.level.getGameTime() < this.doorLampUntil) {
            mask |= 32;
        }

        for (String event : this.alerts.values()) {
            if (event.contains("화재")) {
                mask |= 2;
            } else if (event.contains("가스")) {
                mask |= 4;
            } else if (event.contains("방범")) {
                mask |= 8;
            } else {
                mask |= 16;
            }
        }

        return mask;
    }

    public String lcd() {
        return "SHT-5500S  경비실 "
            + this.unit
            + "\n"
            + (this.clockLabel.isEmpty() ? "시간 미설정" : this.clockLabel)
            + "\n"
            + this.status
            + "\n"
            + (this.operation.equals("house") ? "" : this.prompt());
    }

    private List<String> sortedAway() {
        return this.awayHomes.keySet().stream().map(at -> this.homes.getOrDefault(at, at.toShortString())).sorted().toList();
    }

    private void pageAway() {
        List<String> list = this.sortedAway().stream().filter(s -> this.awayFilter.isEmpty() || s.startsWith(this.awayFilter)).toList();
        this.status = list.isEmpty() ? "부재 세대 없음" : "부재세대 " + (this.page % list.size() + 1) + "/" + list.size() + " : " + list.get(this.page % list.size());
    }

    private void pageAlarm() {
        this.status = this.alarmHistory.isEmpty()
            ? "상황 내역 없음"
            : "상황 " + (this.page + 1) + "/" + this.alarmHistory.size() + " : " + this.alarmHistory.get(this.page % this.alarmHistory.size());
    }

    public void action(ServerPlayer p, String key, String value) {
        if (Intercom.inReach(p, this.worldPosition) && p.level() == this.level) {
            value = value == null ? "" : Intercom.sanitize(value, 32);
            label538:
            switch (key) {
                case "chat":
                    if (this.callState == CallState.CONNECTED && this.callerBe() != null && this.level instanceof ServerLevel server && !value.isBlank()) {
                        Intercom.say(server, this, this.callerBe(), p, "경비실", value);
                        this.status = "통화 중";
                        break;
                    }

                    this.status = "통화 중에 메시지를 입력하세요";
                    break;
                case "number":
                    String nx = DeviceRegistry.digits(value);
                    if (nx.length() <= 4 && !nx.isEmpty()) {
                        this.unit = nx;
                        this.register();
                        this.status = "경비실 번호: " + nx;
                    } else {
                        this.status = "경비실 번호는 1~4자리";
                    }
                    break;
                case "lift":
                    this.handsetUp = true;
                    if (this.callState == CallState.RINGING) {
                        this.answer();
                    } else if (this.callState == CallState.IDLE) {
                        this.operation = "house";
                        this.entry = "";
                        this.status = "세대 : ___";
                    }
                    break;
                case "hang":
                    this.handsetUp = false;
                    this.entry = "";
                    this.operation = "house";
                    this.hangUp();
                    break;
                case "digit":
                    String digit = DeviceRegistry.digits(value);
                    label527:
                    if (digit.length() == 1 && this.entry.length() < 16) {
                        if (this.operation.equals("program")) {
                            this.operation = digit.equals("1") ? "program_volume" : (digit.equals("2") ? "program_clock" : "program");
                            this.entry = "";
                        } else if (this.operation.equals("print")) {
                            this.operation = digit.equals("1")
                                ? "print_away"
                                : (digit.equals("2") ? "print_alarm_choice" : (digit.equals("3") ? "print_access_choice" : "print"));
                            this.entry = "";
                        } else if (!this.operation.equals("print_alarm_choice") && !this.operation.equals("print_access_choice")) {
                            this.entry = this.entry + digit;
                        } else {
                            this.operation = this.operation.replace("_choice", "_") + digit;
                            this.entry = "";
                            if (!digit.equals("1")) {
                                this.status = this.printSelection(p);
                                this.operation = "house";
                                break label527;
                            }
                        }

                        this.status = this.prompt();
                    }
                    break;
                case "dong":
                    if ((
                            this.operation.equals("house")
                                || this.operation.equals("lobby")
                                || this.operation.equals("transfer")
                                || this.operation.equals("door_open")
                                || this.operation.equals("door_close")
                        )
                        && !this.entry.isEmpty()
                        && !this.entry.contains("-")) {
                        this.entry = this.entry + "-";
                        this.status = this.prompt();
                    }
                    break;
                case "mode_house":
                    this.operation = "house";
                    this.entry = "";
                    this.status = this.prompt();
                    break;
                case "mode_guard":
                    this.operation = "guard";
                    this.entry = "";
                    this.status = this.prompt();
                    break;
                case "mode_lobby":
                    this.operation = "lobby";
                    this.entry = "";
                    this.status = this.prompt();
                    break;
                case "mode_office":
                    this.operation = "office";
                    this.entry = "";
                    this.status = "관리실 호출 · 호출/다음";
                    break;
                case "mode_transfer":
                    this.operation = "transfer";
                    this.entry = "";
                    this.status = this.callState == CallState.CONNECTED ? this.prompt() : "세대 통화 중에만 전환 가능";
                    break;
                case "mode_away":
                    if (!this.forwarding.isEmpty()) {
                        this.forwarding = "";
                        this.status = "외출 해제";
                    } else {
                        this.operation = "away";
                        this.entry = "";
                        this.status = this.prompt();
                    }
                    break;
                case "program":
                    if (this.operation.equals("program_clock")
                        && this.entry.matches("[0-9]{12}")
                        && Integer.parseInt(this.entry.substring(2, 4)) >= 1
                        && Integer.parseInt(this.entry.substring(2, 4)) <= 12
                        && Integer.parseInt(this.entry.substring(4, 6)) >= 1
                        && Integer.parseInt(this.entry.substring(4, 6)) <= 31
                        && this.entry.charAt(6) <= '6'
                        && this.entry.charAt(7) <= '1'
                        && Integer.parseInt(this.entry.substring(8, 10)) <= 12
                        && Integer.parseInt(this.entry.substring(10, 12)) <= 59) {
                        this.clockLabel = this.entry.substring(0, 2)
                            + "/"
                            + this.entry.substring(2, 4)
                            + "/"
                            + this.entry.substring(4, 6)
                            + " "
                            + this.entry.substring(6, 7)
                            + " "
                            + (this.entry.charAt(7) == '0' ? "A" : "P")
                            + this.entry.substring(8, 10)
                            + ":"
                            + this.entry.substring(10, 12);
                        this.status = "시간 설정: " + this.clockLabel;
                        this.operation = "house";
                        this.entry = "";
                    } else if (this.operation.equals("program_clock")) {
                        this.status = "시간 형식 오류 · YYMMDD요일AM/PM시분";
                    } else if (!this.operation.startsWith("program") && !this.operation.equals("shortcut_set")) {
                        this.operation = "program";
                        this.entry = "";
                        this.status = this.prompt();
                    } else {
                        this.status = "프로그램 종료";
                        this.operation = "house";
                        this.entry = "";
                    }
                    break;
                case "print":
                    this.operation = "print";
                    this.entry = "";
                    this.status = this.prompt();
                    break;
                case "shortcut":
                    int slot = 0;

                    try {
                        slot = Integer.parseInt(value);
                    } catch (NumberFormatException var13) {
                    }

                    if (slot >= 1 && slot <= 17) {
                        if (this.operation.equals("program")) {
                            this.shortcutSlot = slot;
                            this.operation = "shortcut_set";
                            this.entry = "";
                            this.status = this.prompt();
                        } else if (this.handsetUp && this.shortcuts.containsKey(slot)) {
                            String target = this.shortcuts.get(slot);
                            this.dial(p, target);
                            this.status = this.callState == CallState.DIALING ? "세대 : " + target + " · 호출중!" : "단축 " + slot + " 연결 실패";
                        } else {
                            this.status = "단축 " + slot + " : " + this.shortcuts.getOrDefault(slot, "등록 없음");
                        }
                    }
                    break;
                case "call":
                    if (this.operation.equals("away_list")) {
                        if (!this.entry.isEmpty()) {
                            this.awayFilter = this.entry;
                            this.page = 0;
                            this.entry = "";
                        } else {
                            this.page++;
                        }

                        this.pageAway();
                    } else if (this.operation.equals("alarm_list")) {
                        this.page++;
                        this.pageAlarm();
                    } else if (this.operation.equals("shortcut_set")) {
                        if (!this.entry.isEmpty()) {
                            this.shortcuts.put(this.shortcutSlot, this.entry);
                            this.status = "MEMORY " + this.shortcutSlot + " : " + this.entry;
                        } else {
                            this.status = "단축번호를 입력하세요";
                        }

                        this.operation = "program";
                        this.entry = "";
                    } else if (this.operation.startsWith("print")) {
                        this.status = this.printSelection(p);
                        this.operation = "house";
                        this.entry = "";
                    } else if (this.operation.equals("door_open") || this.operation.equals("door_close")) {
                        boolean opening = this.operation.equals("door_open");
                        this.status = this.operateNumberedLobby(this.entry, opening)
                            ? (opening ? "현관열림 : " : "현관닫힘 : ") + this.entry
                            : "현관 또는 연결된 문을 찾을 수 없습니다";
                        this.entry = "";
                        this.operation = "house";
                    } else if (!this.handsetUp && !this.operation.equals("away")) {
                        this.status = "송수화기를 먼저 드세요";
                    } else {
                        String target = this.entry;
                        this.entry = "";
                        String var28 = this.operation;
                        switch (var28) {
                            case "house":
                                this.dial(p, target);
                                this.status = this.callState == CallState.DIALING ? "세대 : " + target + " · 호출중!" : "세대 연결 실패: " + target;
                                break label538;
                            case "guard":
                                this.callSamsungGuard(p, DeviceRegistry.digits(target));
                                this.status = this.callState == CallState.DIALING ? "경비실 : " + target + " · 호출중!" : "경비실 연결 실패: " + target;
                                break label538;
                            case "office":
                                this.callSamsungGuard(p, "");
                                this.status = this.callState == CallState.DIALING ? "관리실 호출중!" : "관리실 연결 실패";
                                break label538;
                            case "lobby":
                                this.callLobby(p, target);
                                this.status = this.callState == CallState.DIALING ? "현관 : " + target + " · 호출중!" : "현관 연결 실패: " + target;
                                break label538;
                            case "transfer":
                                this.action(p, "transfer", target);
                                return;
                            case "away":
                                this.action(p, "away", target);
                                return;
                            default:
                                this.status = "호출 대상을 선택하세요";
                        }
                    }
                    break;
                case "house":
                    this.dial(p, value);
                    this.status = this.callState == CallState.DIALING ? "세대 호출중: " + value : "세대 연결 실패: " + value;
                    break;
                case "guard":
                    String n = DeviceRegistry.digits(value);
                    if (n.isEmpty()) {
                        this.status = "경비실 번호를 입력하세요";
                    } else {
                        this.callSamsungGuard(p, n);
                        this.status = this.callState == CallState.DIALING ? "경비실 호출중: " + n : "경비실 연결 실패: " + n;
                    }
                    break;
                case "office":
                    this.callSamsungGuard(p, "");
                    this.status = this.callState == CallState.DIALING ? "관리실 호출중" : "관리실 연결 실패";
                    break;
                case "lobby":
                    this.callLobby(p, value);
                    this.status = this.callState == CallState.DIALING ? "공동현관 호출중: " + value : "공동현관 연결 실패: " + value;
                    break;
                case "transfer":
                    if (this.callState == CallState.CONNECTED && this.caller != null && this.level instanceof ServerLevel sl) {
                        List<BlockPos> targets = DeviceRegistry.get(sl).receivers(sl, this.worldPosition, value);
                        boolean sent = false;

                        for (BlockPos at : targets) {
                            if (!at.equals(this.caller)
                                && sl.isLoaded(at)
                                && ZoneData.connected(sl, this.worldPosition, at)
                                && sl.getBlockEntity(at) instanceof ReceiverBlockEntity target
                                && target.guardSystem() == GuardSystem.BUS
                                && target.bindSwitchboard(this.worldPosition)
                                && target.startRinging(this.worldPosition, "guard_transfer")) {
                                this.transferSource = this.caller;
                                this.transferTarget = at;
                                this.status = "전환 호출: " + value;
                                sent = true;
                                break;
                            }
                        }

                        if (!sent) {
                            this.status = "전환 대상 연결 실패";
                        }
                        break;
                    }

                    this.status = "세대 통화 중에 전환할 수 있습니다";
                    break;
                case "door":
                    if (this.callState == CallState.CONNECTED
                        && this.callerBe() instanceof CallerBlockEntity door
                        && door.kind() == DeviceRegistry.Kind.LOBBY_PHONE) {
                        if (door.getLinkedDoors().isEmpty()) {
                            this.status = "공동현관에 연결된 문이 없습니다";
                        } else {
                            this.openDoor();
                            this.recordAccess(door, this.level.getGameTime());
                            this.doorLampUntil = this.level.getGameTime() + 200L;
                            this.status = "문 열림";
                        }
                    } else if (this.callState == CallState.IDLE) {
                        this.operation = "door_open";
                        this.entry = "";
                        this.status = this.prompt();
                    } else {
                        this.status = "공동현관 통화 중에 사용하세요";
                    }
                    break;
                case "door_close":
                    if (this.callState == CallState.IDLE) {
                        this.operation = "door_close";
                        this.entry = "";
                        this.status = this.prompt();
                    } else {
                        this.status = "통화 종료 후 현관을 지정하세요";
                    }
                    break;
                case "away":
                    if (this.forwarding.isEmpty()) {
                        String forwardNumber = DeviceRegistry.digits(value);
                        if (!forwardNumber.isEmpty() && !forwardNumber.equals(this.unit)) {
                            this.forwarding = forwardNumber;
                            this.status = "외출 중 · " + forwardNumber + "번으로 우회";
                        } else {
                            this.status = "우회 경비실 번호 입력";
                        }
                    } else {
                        this.forwarding = "";
                        this.status = "외출 해제";
                    }
                    break;
                case "stop":
                    if (this.operation.equals("alarm_list") && !this.alarmHistory.isEmpty()) {
                        int index = this.page % this.alarmHistory.size();
                        String selected = this.alarmHistory.get(index);
                        if (this.alerts.containsValue(selected)) {
                            this.status = "경보 원인 해제 후 복귀 가능";
                        } else {
                            this.alarmHistory.remove(index);
                            this.alarmTimes.remove(index);
                            this.page = 0;
                            this.pageAlarm();
                        }
                    } else {
                        this.muted = true;
                        this.status = "경보음 정지";
                    }
                    break;
                case "restore":
                    this.muted = false;
                    this.status = this.alerts.isEmpty() ? "경보 복귀" : "경보 원인 해제 대기";
                    break;
                case "night":
                    this.night = !this.night;
                    this.nightOccupied.clear();
                    this.status = this.night ? "야간감시중" : "야간감시 해제";
                    break;
                case "volume_up":
                    this.volume = Math.min(8, this.volume + 1);
                    this.status = this.operation.equals("program_volume") ? "프로그램 · 착신음량 : " + this.volume : "통화음량: " + this.volume;
                    break;
                case "volume_down":
                    this.volume = Math.max(0, this.volume - 1);
                    this.status = this.operation.equals("program_volume") ? "프로그램 · 착신음량 : " + this.volume : "통화음량: " + this.volume;
                    break;
                case "away_list":
                    this.operation = "away_list";
                    this.entry = "";
                    this.awayFilter = "";
                    this.page = 0;
                    this.status = "부재세대 확인 · 동번호 입력";
                    break;
                case "alarm_list":
                    this.operation = "alarm_list";
                    this.entry = "";
                    this.page = 0;
                    this.pageAlarm();
                    break;
                case "backspace":
                    if (!this.entry.isEmpty()) {
                        this.entry = this.entry.substring(0, this.entry.length() - 1);
                    }

                    this.status = this.prompt();
                    break;
                case "cancel":
                    this.entry = "";
                    this.operation = "house";
                    if (this.callState != CallState.IDLE) {
                        this.hangUp();
                    }

                    this.status = "취소";
            }

            this.changed();
            Wire.guardView(p, this);
        }
    }

    private void callSamsungGuard(ServerPlayer player, String number) {
        if (this.level instanceof ServerLevel sl) {
            DeviceRegistry var7 = DeviceRegistry.get(sl);
            List<BlockPos> candidates = number.isEmpty() ? var7.guards(sl, this.worldPosition, false) : var7.guardsByNumber(sl, this.worldPosition, number);
            List<BlockPos> selected = candidates.stream()
                .filter(at -> sl.isLoaded(at) && ZoneData.connected(sl, this.worldPosition, at) && sl.getBlockEntity(at) instanceof Sht5500s)
                .toList();
            this.startOutgoing(player, selected, number.isEmpty() ? "#office" : "#guard:" + number);
        }
    }

    private String printSelection(ServerPlayer player) {
        String selection = this.operation;
        String dong = DeviceRegistry.digits(this.entry);
        List<String> entries;
        if (selection.equals("print_away")) {
            entries = this.sortedAway().stream().filter(s -> dong.isEmpty() || s.startsWith(dong)).toList();
        } else {
            if (!selection.startsWith("print_alarm_") && !selection.startsWith("print_access_")) {
                return "출력 항목을 선택하세요";
            }

            boolean alarm = selection.startsWith("print_alarm_");
            List<String> labels = alarm ? this.alarmHistory : this.accessHistory;
            List<Long> times = alarm ? this.alarmTimes : this.accessTimes;
            int filter = selection.endsWith("1") ? 1 : (selection.endsWith("2") ? 2 : (selection.endsWith("3") ? 3 : 0));
            ArrayList<String> result = new ArrayList<>();

            for (int i = 0; i < labels.size(); i++) {
                if (filter == 1
                    ? dong.isEmpty() || labels.get(i).startsWith(dong)
                    : (filter == 2 || filter == 3) && this.level != null && this.level.getGameTime() - times.get(i) <= (filter == 2 ? 12000 : 24000)) {
                    result.add(labels.get(i));
                }
            }

            entries = result;
        }

        for (String item : entries) {
            player.sendSystemMessage(Component.literal("[SHT-5500S 출력] " + item));
        }

        return "출력 " + entries.size() + "건";
    }

    private boolean openNumberedLobby(String number) {
        return this.operateNumberedLobby(number, true);
    }

    private boolean operateNumberedLobby(String number, boolean opening) {
        if (this.level instanceof ServerLevel server && !number.isBlank()) {
            String digits = DeviceRegistry.digits(number);
            boolean dongOnly = !number.contains("-") && digits.length() <= 4;
            CallerBlockEntity selected = null;
            double distance = Double.POSITIVE_INFINITY;
            boolean operated = false;

            for (BlockPos at : DeviceRegistry.get(server).lobbies(server, this.worldPosition)) {
                if (server.isLoaded(at) && ZoneData.connected(server, this.worldPosition, at)) {
                    BlockEntity entity = server.getBlockEntity(at);
                    if (entity instanceof CallerBlockEntity door && !door.getLinkedDoors().isEmpty()) {
                        boolean match = entity instanceof LobbyPhoneBlockEntity lobby && lobby.matchesNumber(number)
                            || entity instanceof Ht512.Unit unit
                                && (DeviceRegistry.digits(unit.registeredUnit()).equals(digits) || dongOnly && DeviceRegistry.digits(unit.dong).equals(digits));
                        if (match) {
                            if (dongOnly) {
                                this.operateDoor(door, opening, server);
                                operated = true;
                            } else if (at.distSqr(this.worldPosition) < distance) {
                                selected = door;
                                distance = at.distSqr(this.worldPosition);
                            }
                        }
                    }
                }
            }

            if (selected != null) {
                this.operateDoor(selected, opening, server);
                operated = true;
            }

            if (operated && opening) {
                this.doorLampUntil = server.getGameTime() + 200L;
            }

            return operated;
        } else {
            return false;
        }
    }

    private void recordAccess(CallerBlockEntity lobby, long tick) {
        this.accessHistory.add(0, "현관 " + lobby.registeredUnit() + " 열림");
        this.accessTimes.add(0, tick);
        if (this.accessHistory.size() > 250) {
            this.accessHistory.remove(250);
            this.accessTimes.remove(250);
        }
    }

    private void operateDoor(CallerBlockEntity lobby, boolean opening, ServerLevel server) {
        if (opening) {
            lobby.openDoors();
            this.recordAccess(lobby, server.getGameTime());
        } else {
            for (BlockPos at : lobby.getLinkedDoors()) {
                if (server.isLoaded(at)) {
                    DoorControl.setOpen(server, at, false);
                }
            }
        }
    }

    private String prompt() {
        String var1 = this.operation;

        return switch (var1) {
            case "guard" -> "경비실 : " + (this.entry.isEmpty() ? "____" : this.entry) + " · 호출/다음";
            case "lobby" -> "현 관 : " + (this.entry.isEmpty() ? "___" : this.entry) + " · 호출/다음";
            case "door_open" -> "현관열림 : " + (this.entry.isEmpty() ? "____" : this.entry) + " · 호출/다음";
            case "door_close" -> "현관닫힘 : " + (this.entry.isEmpty() ? "____" : this.entry) + " · 호출/다음";
            case "transfer" -> "전 환 : " + (this.entry.isEmpty() ? "____" : this.entry) + " · 호출/다음";
            case "away" -> "외출지정 · 경비실 : " + (this.entry.isEmpty() ? "____" : this.entry);
            case "away_list" -> "부재세대 확인 · 동번호 " + this.entry;
            case "program" -> "프로그램 · 1 착신음량 / 2 시간설정 / 단축키";
            case "program_volume" -> "프로그램 · 착신음량 : " + this.volume;
            case "program_clock" -> "시간 : " + this.entry + " · 프로그램으로 저장";
            case "shortcut_set" -> "MEMORY " + this.shortcutSlot + " : " + this.entry + " · 호출/다음으로 저장";
            case "print" -> "출력내용 · 1 부재 / 2 상황 / 3 출입";
            case "print_alarm_choice", "print_access_choice" -> "1 동별 / 2 12시간 / 3 24시간";
            case "print_away", "print_alarm_1", "print_access_1" -> "동번호 : " + (this.entry.isEmpty() ? "___" : this.entry) + " · 호출/다음";
            default -> "세 대 : " + (this.entry.isEmpty() ? "___" : this.entry) + " · 호출/다음";
        };
    }
}
