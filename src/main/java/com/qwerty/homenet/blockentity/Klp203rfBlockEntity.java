package com.qwerty.homenet.blockentity;

import com.qwerty.homenet.analog.AnalogBus;
import com.qwerty.homenet.analog.AnalogLobbyEndpoint;
import com.qwerty.homenet.block.Klp203rfBlock;
import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.data.ZoneData;
import com.qwerty.homenet.guard.GuardSystem;
import com.qwerty.homenet.intercom.CallState;
import com.qwerty.homenet.intercom.DoorStatus;
import com.qwerty.homenet.intercom.Intercom;
import com.qwerty.homenet.intercom.UnitAddress;
import com.qwerty.homenet.item.InsertCardItem;
import com.qwerty.homenet.item.RfCardItem;
import com.qwerty.homenet.klp.KlpModel;
import com.qwerty.homenet.move.DeviceMoves;
import com.qwerty.homenet.network.ModNetwork;
import com.qwerty.homenet.network.WallpadDataPacket;
import com.qwerty.homenet.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class Klp203rfBlockEntity extends CallerBlockEntity implements AnalogLobbyEndpoint {
    public static final int ADMIN_HOLD = 60;
    public static final int SESSION_TICKS = 2400;
    private String dong = "101";
    private String line = "1";
    private String guardNo = "10";
    private String adminPin = "9999";
    private int first = 1;
    private int last = 99;
    private BlockPos station;
    private final Set<String> cards = new HashSet<>();
    private String input = "";
    private String address = "";
    private String mode = "normal";
    private String status = "대기";
    private String callTarget = "";
    private boolean guardCall;
    private String confirmation = "";
    private long confirmationUntil;
    private UUID operator;
    private UUID administrator;
    private long adminUntil;
    private long inputAt;
    private long changeAt;
    private String changeKeys = "";
    private BlockPos passwordHome;
    private final Map<UUID, Klp203rfBlockEntity.Press> presses = new HashMap<>();
    private String clientDisplay = "-";
    private boolean clientHome;
    private boolean clientGuard;
    private boolean clientTalk;

    public Klp203rfBlockEntity(BlockPos p, BlockState s) {
        super(ModBlockEntities.KLP_203RF.get(), p, s);
    }

    public KlpModel model() {
        return this.getBlockState().getBlock() instanceof Klp203rfBlock b ? b.model : KlpModel.RF;
    }

    public void openModels(ServerPlayer p) {
        ModNetwork.sendTo(p, this.packet(p, true).withNotice("klp_models"));
    }

    public boolean changeModel(ServerPlayer p, String id) {
        KlpModel target = KlpModel.byId(id);
        if (target != null && this.level instanceof ServerLevel sl && !DeviceMoves.busy(this) && this.administrator == null && this.mode.equals("normal")) {
            if (this.model() != target) {
                CompoundTag tag = this.saveWithoutMetadata();
                BlockState state = target.block().defaultBlockState().setValue(WallMountedBlock.FACING, this.getBlockState().getValue(WallMountedBlock.FACING));
                sl.removeBlockEntity(this.worldPosition);
                sl.setBlock(this.worldPosition, state, 67);
                if (!(sl.getBlockEntity(this.worldPosition) instanceof Klp203rfBlockEntity next)) {
                    return false;
                }

                next.load(tag);
                next.registerDevice();
                next.publish();
                next.changed();
                next.openFor(p);
            } else {
                this.openFor(p);
            }

            return true;
        } else {
            return false;
        }
    }

    public boolean useCard(ServerPlayer p, ItemStack stack) {
        boolean insertion = stack.getItem() instanceof InsertCardItem;
        boolean matching = this.model().reader.equals(insertion ? "insert" : "RF") && (insertion || stack.getItem() instanceof RfCardItem);
        if (!matching) {
            this.status = this.model().hasCard() ? "이 리더에 맞는 출입카드를 사용하세요." : "카드 인식 기능이 없는 모델입니다.";
            this.changed();
            return false;
        } else {
            return this.recognizeCard(p, RfCardItem.cardId(stack));
        }
    }

    public boolean insertCard(ServerPlayer p, String id) {
        return this.model().reader.equals("insert") && this.recognizeCard(p, id);
    }

    @Override
    public DeviceRegistry.Kind kind() {
        return DeviceRegistry.Kind.LOBBY_PHONE;
    }

    @Override
    public String callerKey() {
        return "lobby";
    }

    @Override
    public String manufacturer() {
        return "KOCOM";
    }

    @Override
    public boolean supports(AnalogBus bus) {
        return bus == AnalogBus.COMMON_8_WIRE;
    }

    public String dong() {
        return this.dong;
    }

    public String line() {
        return this.line;
    }

    public String guardNumber() {
        return this.guardNo;
    }

    public int firstHouse() {
        return this.first;
    }

    public int lastHouse() {
        return this.last;
    }

    public String lobbyAddress() {
        return this.dong + "-" + this.line;
    }

    public String configSummary() {
        return "동 " + this.dong + " / 라인 " + this.line + " / 호 " + String.format(Locale.ROOT, "%02d~%02d", this.first, this.last) + " / 경비 " + this.guardNo;
    }

    public Am10kpBlockEntity exchange() {
        if (this.level instanceof ServerLevel sl) {
            if (this.station == null) {
                return DeviceRegistry.get(sl)
                    .all()
                    .stream()
                    .filter(e -> e.kind() == DeviceRegistry.Kind.GUARD_CONSOLE && this.level.isLoaded(e.pos()))
                    .map(e -> this.level.getBlockEntity(e.pos()))
                    .filter(e -> e instanceof Am10kpBlockEntity b && this.compatible(b))
                    .map(e -> (Am10kpBlockEntity)e)
                    .min(Comparator.comparingDouble(b -> b.getBlockPos().distSqr(this.worldPosition)))
                    .orElse(null);
            } else {
                return this.level.isLoaded(this.station) && this.level.getBlockEntity(this.station) instanceof Am10kpBlockEntity b && this.compatible(b)
                    ? b
                    : null;
            }
        } else {
            return null;
        }
    }

    private boolean compatible(Am10kpBlockEntity b) {
        return ZoneData.connected(this.level, this.worldPosition, b.getBlockPos())
            && b.power()
            && this.guardNo.equals(b.getUnit())
            && b.guardSystem() == GuardSystem.BUS
            && b.manufacturer().equals("KOCOM");
    }

    @Override
    public BlockPos switchboardPosition() {
        Am10kpBlockEntity b = this.exchange();
        return b == null ? null : b.getBlockPos();
    }

    @Override
    public BlockPos cameraPosition() {
        return this.isConnected() ? this.worldPosition : null;
    }

    @Override
    public String videoNumber() {
        return this.line;
    }

    public boolean canBind(BlockPos at) {
        return this.level != null
            && ZoneData.connected(this.level, this.worldPosition, at)
            && this.level.isLoaded(at)
            && this.level.getBlockEntity(at) instanceof Am10kpBlockEntity b
            && b.guardSystem() == GuardSystem.BUS
            && b.manufacturer().equals("KOCOM");
    }

    public boolean bindSwitchboard(BlockPos at) {
        if (this.canBind(at) && !this.inCall) {
            if (this.exchange() instanceof Kam305BlockEntity k) {
                k.removeLobbyPanel(this.worldPosition);
            }

            this.station = at.immutable();
            this.publish();
            this.changed();
            return true;
        } else {
            return false;
        }
    }

    public void publish() {
        if (this.level instanceof ServerLevel sl) {
            for (DeviceRegistry.Entry e : DeviceRegistry.get(sl).all()) {
                if (this.level.isLoaded(e.pos()) && this.level.getBlockEntity(e.pos()) instanceof Kam305BlockEntity b) {
                    if (b == this.exchange()) {
                        b.registerLobbyPanel(this);
                    } else {
                        b.removeLobbyPanel(this.worldPosition);
                    }
                }
            }
        }
    }

    public void applyBoardAddress(Kam305BlockEntity board, String value) {
        if (this.exchange() == board && UnitAddress.full(value)) {
            String[] a = UnitAddress.canonical(value).split("-");
            this.dong = a[0];
            this.line = a[1];
            this.changed();
        }
    }

    private boolean allowedHouse(String ho) {
        if (!UnitAddress.component(ho)) {
            return false;
        } else {
            int suffix = Integer.parseInt(ho) % 100;
            return suffix >= this.first && suffix <= this.last;
        }
    }

    @Override
    public List<ReceiverBlockEntity> receiversOf(String unit) {
        Am10kpBlockEntity b = this.exchange();
        if (b == null) {
            return List.of();
        } else {
            ArrayList<ReceiverBlockEntity> candidates = new ArrayList<>(super.receiversOf(unit));
            String ho = unit.substring(unit.lastIndexOf("-") + 1);
            if (this.level instanceof ServerLevel sl) {
                for (DeviceRegistry.Entry e : DeviceRegistry.get(sl).all()) {
                    if (sl.isLoaded(e.pos())
                        && sl.getBlockEntity(e.pos()) instanceof ReceiverBlockEntity r
                        && r.roomOnlyUnit()
                        && r.getUnit().equals(ho)
                        && !candidates.contains(r)) {
                        candidates.add(r);
                    }
                }
            }

            return candidates.stream()
                .filter(
                    rx -> rx.guardSystem() == GuardSystem.BUS
                        && rx.manufacturer().equals("KOCOM")
                        && b.getBlockPos().equals(rx.getAnalogStation())
                        && rx.supportsDoorCommunication()
                )
                .toList();
        }
    }

    private CallerBlockEntity.CallResult callError(CallerBlockEntity.CallResult result) {
        this.confirmation = "Err";
        this.confirmationUntil = this.now() + 60L;
        this.changed();
        return result;
    }

    public boolean visitorLight() {
        return this.homeLamp() || this.guardLamp();
    }

    @Override
    public void openDoors() {
        super.openDoors();
        this.confirmation = "OPEN";
        this.confirmationUntil = this.now() + 60L;
        this.changed();
    }

    public CallerBlockEntity.CallResult callHouse() {
        if (this.inCall) {
            return this.callError(CallerBlockEntity.CallResult.BUSY);
        } else if (this.mode.equals("normal") && this.allowedHouse(this.input)) {
            this.confirmationUntil = 0L;
            String target = this.dong + "-" + Integer.parseInt(this.input);
            this.guardCall = false;
            this.callTarget = this.input;
            CallerBlockEntity.CallResult result = this.startCall(target);
            this.status = result == CallerBlockEntity.CallResult.OK ? "세대 호출 중" : (result == CallerBlockEntity.CallResult.BUSY ? "통화 중" : "등록된 세대 / 경비번호 확인");
            this.input = "";
            if (result != CallerBlockEntity.CallResult.OK) {
                return this.callError(result);
            } else {
                this.changed();
                return result;
            }
        } else {
            this.status = "호출 가능한 호수를 입력하세요";
            this.changed();
            return this.callError(CallerBlockEntity.CallResult.NO_UNIT);
        }
    }

    public CallerBlockEntity.CallResult callGuard() {
        if (this.inCall) {
            return this.callError(CallerBlockEntity.CallResult.BUSY);
        } else {
            this.confirmationUntil = 0L;
            Am10kpBlockEntity b = this.exchange();
            this.guardCall = true;
            this.callTarget = this.guardNo;
            CallerBlockEntity.CallResult result = b == null ? CallerBlockEntity.CallResult.NO_SIGNAL : this.startReceivers(List.of(b));
            this.status = result == CallerBlockEntity.CallResult.OK ? "경비실 호출 중" : "경비번호 / 연결 확인";
            this.input = "";
            if (result != CallerBlockEntity.CallResult.OK) {
                return this.callError(result);
            } else {
                this.changed();
                return result;
            }
        }
    }

    @Override
    public CallerBlockEntity.CallResult startCall(String target) {
        if (!this.mode.equals("normal")) {
            this.clearInput();
        }

        List<ReceiverBlockEntity> homes = this.receiversOf(target);
        if (!homes.isEmpty() && homes.stream().allMatch(ReceiverBlockEntity::isBusy)) {
            return CallerBlockEntity.CallResult.BUSY;
        } else {
            return homes.isEmpty() ? super.startCall(target) : this.startReceivers(homes);
        }
    }

    @Override
    public boolean acceptIncoming(BlockPos from) {
        Am10kpBlockEntity b = this.exchange();
        if (b != null && b.getBlockPos().equals(from)) {
            this.clearInput();
            this.guardCall = true;
            this.callTarget = this.guardNo;
            return super.acceptIncoming(from);
        } else {
            return false;
        }
    }

    @Override
    protected void onConnected() {
        this.clearInput();
        this.status = "통화 중";
        this.changed();
    }

    @Override
    protected void onEnded(DoorStatus reason) {
        this.clearInput();
        this.status = reason == DoorStatus.NO_ANSWER ? "응답 없음" : "통화 종료";
        if (reason != DoorStatus.ENDED && reason != DoorStatus.OPENED) {
            this.confirmation = "Err";
            this.confirmationUntil = this.now() + 60L;
        }

        this.input = "";
        this.callTarget = "";
        this.changed();
    }

    @Override
    protected void onDoorOpenedByReceiver() {
        this.clearInput();
        this.status = "문 열림";
        this.input = "";
        this.callTarget = "";
        this.changed();
    }

    @Override
    public void onDoorOpened(BlockPos receiver) {
        if (this.inCall && receiver.equals(this.connectedTo)) {
            super.onDoorOpened(receiver);
        }
    }

    @Override
    public void hangUpFromCaller() {
        super.hangUpFromCaller();
        this.clearInput();
        this.status = "통화 종료";
        this.callTarget = "";
        this.changed();
    }

    public String display() {
        if (this.level != null && this.level.isClientSide) {
            return this.clientDisplay;
        } else if (this.now() < this.confirmationUntil) {
            return this.confirmation;
        } else if (this.mode.equals("change_wait")) {
            return "====";
        } else if (!this.mode.equals("normal")) {
            return this.input.isEmpty() ? "----" : "-".repeat(Math.min(4, this.input.length()));
        } else {
            String value = this.inCall ? this.callTarget : this.input;
            return value.isEmpty() ? "-" : value.substring(Math.max(0, value.length() - 4));
        }
    }

    public boolean homeLamp() {
        return this.level != null && this.level.isClientSide ? this.clientHome : this.inCall && !this.guardCall;
    }

    public boolean guardLamp() {
        return this.level != null && this.level.isClientSide ? this.clientGuard : this.inCall && this.guardCall;
    }

    public boolean talkLamp() {
        return this.level != null && this.level.isClientSide ? this.clientTalk : this.isConnected();
    }

    public boolean authenticated(ServerPlayer p) {
        return this.administrator != null && this.administrator.equals(p.getUUID()) && this.now() < this.adminUntil && Intercom.inReach(p, this.worldPosition);
    }

    private void clearInput() {
        this.input = "";
        this.address = "";
        this.mode = "normal";
        this.operator = null;
        this.status = "대기";
        this.changeKeys = "";
        this.passwordHome = null;
    }

    public void key(ServerPlayer p, String key) {
        if (key != null
            && key.length() == 1
            && "0123456789*#".contains(key)
            && (this.administrator == null || this.authenticated(p))
            && (this.operator == null || this.operator.equals(p.getUUID()))) {
            if (this.mode.equals("normal")) {
                this.confirmationUntil = 0L;
            }

            this.inputAt = this.now();
            if (key.equals("*")) {
                if (this.inCall) {
                    this.hangUpFromCaller();
                }

                this.clearInput();
                this.changed();
            } else if (this.inCall) {
                if (!this.isConnected() || this.guardCall) {
                    this.status = "세대 통화 중에만 비밀번호를 변경할 수 있습니다.";
                    this.changed();
                } else if (this.mode.equals("change_wait")) {
                    this.changed();
                } else if (!this.mode.equals("change_new")) {
                    this.changeKeys = this.changeKeys + key;
                    if (!"##00#".startsWith(this.changeKeys)) {
                        this.changeKeys = key.equals("#") ? "#" : "";
                    }

                    if (this.changeKeys.equals("##00#")) {
                        this.confirmationUntil = 0L;
                        this.operator = p.getUUID();
                        this.passwordHome = this.connectedTo;
                        this.mode = "change_wait";
                        this.input = "";
                        this.changeAt = this.now() + 200L;
                        this.status = "10초 경보 안내 후 새 비밀번호 입력";
                    }

                    this.changed();
                } else {
                    if (key.matches("[0-9]") && this.input.length() < 4) {
                        this.input = this.input + key;
                    } else if (key.equals("#")) {
                        boolean ok = this.input.matches("[0-9]{4}")
                            && (this.passwordHome == null ? null : this.level.getBlockEntity(this.passwordHome)) instanceof ReceiverBlockEntity r
                            && r.setDoorPasswordFromLobby(this.worldPosition, this.input.equals("0000") ? "" : this.input);
                        if (ok) {
                            this.clearInput();
                            this.confirmation = "YES";
                            this.confirmationUntil = this.now() + 60L;
                            this.status = "세대 비밀번호 변경 완료";
                        } else {
                            this.status = "새 비밀번호 4자리 입력 후 #";
                        }
                    }

                    this.changed();
                }
            } else if (key.matches("[0-9]")) {
                if (this.input.length() < 4) {
                    this.input = this.input + key;
                }

                this.changed();
            } else if (!this.mode.equals("auth")) {
                if (this.mode.equals("password")) {
                    boolean opened = false;
                    if (this.exchange() instanceof Kam305BlockEntity k && this.address.equals(this.guardNo)) {
                        opened = k.checkPanelPassword(this, this.input);
                    }

                    if (!opened && this.allowedHouse(this.address)) {
                        opened = this.receiversOf(this.dong + "-" + Integer.parseInt(this.address)).stream().anyMatch(r -> r.checkDoorPassword(this.input));
                    }

                    this.clearInput();
                    this.status = opened ? "문 열림" : "비밀번호 / 경비번호 확인";
                    if (opened) {
                        this.openDoors();
                    }

                    this.changed();
                } else {
                    if (this.input.isEmpty()) {
                        this.status = "세대와 통화 연결 후 ##00#를 입력하세요.";
                    } else {
                        this.address = this.input;
                        this.input = "";
                        this.mode = "password";
                        this.operator = p.getUUID();
                        this.status = "비밀번호 입력 후 #";
                    }

                    this.changed();
                }
            } else {
                if (this.input.matches("[0-9]{4}") && this.input.equals(this.adminPin)) {
                    this.administrator = p.getUUID();
                    this.adminUntil = this.now() + 2400L;
                    this.mode = "normal";
                    this.input = "";
                    this.status = "관리자 설정";
                } else {
                    this.input = "";
                    this.status = "관리 비밀번호 오류";
                }

                this.changed();
            }
        }
    }

    public void button(ServerPlayer p, String name, boolean down) {
        if (name.equals("home") || name.equals("guard")) {
            Klp203rfBlockEntity.Press press = this.presses.computeIfAbsent(p.getUUID(), ignored -> new Klp203rfBlockEntity.Press());
            boolean was = name.equals("home") ? press.home : press.guard;
            if (!down || !was) {
                if (name.equals("home")) {
                    press.home = down;
                } else {
                    press.guard = down;
                }

                if (down && press.home && press.guard) {
                    press.together = this.now();
                    press.triggered = false;
                    press.chord = true;
                }

                if (!down) {
                    boolean chord = press.chord;
                    press.together = -1L;
                    if (was && !chord && this.mode.equals("normal") && !this.authenticated(p)) {
                        if (name.equals("home")) {
                            this.callHouse();
                        } else {
                            this.callGuard();
                        }
                    }

                    if (!press.home && !press.guard) {
                        this.presses.remove(p.getUUID());
                    }
                }
            }
        }
    }

    public void action(ServerPlayer p, ReceiverBlockEntity.Action action, String text) {
        if (action == ReceiverBlockEntity.Action.SEND_MESSAGE) {
            if (this.isConnected()) {
                this.say(p, "lobby", text);
            }

            this.syncScreens();
        } else if (action == ReceiverBlockEntity.Action.HANG_UP) {
            this.hangUpFromCaller();
        } else if (action == ReceiverBlockEntity.Action.SET_SETTING) {
            int split = text.indexOf(61);
            if (split >= 0) {
                String key = text.substring(0, split);
                String value = text.substring(split + 1);
                switch (key) {
                    case "key":
                        this.key(p, value);
                        break;
                    case "down":
                        this.button(p, value, true);
                        break;
                    case "up":
                        this.button(p, value, false);
                        break;
                    case "cancel_hold":
                        this.presses.remove(p.getUUID());
                        break;
                    case "exit":
                        if (this.authenticated(p)) {
                            this.administrator = null;
                            this.adminUntil = 0L;
                            this.clearInput();
                            this.changed();
                        }
                        break;
                    case "save":
                        this.saveConfig(p, value);
                        break;
                    case "card":
                        this.manageCard(p, value);
                }
            }
        }
    }

    public boolean saveConfig(ServerPlayer p, String value) {
        if (this.authenticated(p) && !this.inCall) {
            try {
                String[] a = value.split("\\|", -1);
                if (a.length == 6
                    && UnitAddress.component(a[0])
                    && UnitAddress.component(a[1])
                    && UnitAddress.component(a[2])
                    && a[3].matches("[0-9]{2}~[0-9]{2}")
                    && (a[4].isEmpty() || a[4].matches("[0-9]{4}"))
                    && a[5].equals("save")) {
                    String[] range = a[3].split("~");
                    int lo = Integer.parseInt(range[0]);
                    int hi = Integer.parseInt(range[1]);
                    if (lo > hi) {
                        throw new IllegalArgumentException();
                    } else {
                        this.dong = Integer.parseInt(a[0]) + "";
                        this.line = Integer.parseInt(a[1]) + "";
                        this.guardNo = Integer.parseInt(a[2]) + "";
                        this.first = lo;
                        this.last = hi;
                        if (!a[4].isEmpty()) {
                            this.adminPin = a[4];
                        }

                        this.adminUntil = this.now() + 2400L;
                        this.status = "설정 저장 완료";
                        this.publish();
                        this.changed();
                        return true;
                    }
                } else {
                    throw new IllegalArgumentException();
                }
            } catch (Exception var7) {
                this.status = "동·라인·경비 1~9999 / 범위 00~99 / 암호 4자리";
                this.syncScreens();
                return false;
            }
        } else {
            return false;
        }
    }

    private void manageCard(ServerPlayer p, String mode) {
        if (this.authenticated(p) && this.model().hasCard()) {
            ItemStack stack = p.getMainHandItem();
            boolean insert = this.model().reader.equals("insert");
            if (insert ? !(stack.getItem() instanceof InsertCardItem) : !(stack.getItem() instanceof RfCardItem)) {
                stack = p.getOffhandItem();
            }

            if (insert ? stack.getItem() instanceof InsertCardItem : stack.getItem() instanceof RfCardItem) {
                String id = RfCardItem.cardId(stack);
                if (id == null) {
                    this.status = "손에 RF 카드를 들어주세요";
                    this.syncScreens();
                } else {
                    if (mode.equals("add")) {
                        this.cards.add(id);
                    } else {
                        if (!mode.equals("remove")) {
                            return;
                        }

                        this.cards.remove(id);
                    }

                    this.status = mode.equals("add") ? "카드 등록 완료" : "카드 삭제 완료";
                    this.changed();
                }
            } else {
                this.status = insert ? "손에 삽입형 출입카드를 들어주세요" : "손에 RF 카드를 들어주세요";
                this.syncScreens();
            }
        }
    }

    public boolean tapCard(ServerPlayer p, String id) {
        return this.model().reader.equals("RF") && this.recognizeCard(p, id);
    }

    private boolean recognizeCard(ServerPlayer p, String id) {
        if (!this.model().hasCard() || id == null) {
            return false;
        } else if (this.authenticated(p)) {
            this.cards.add(id);
            this.status = "카드 등록 완료";
            this.changed();
            return true;
        } else {
            boolean ok = this.exchange() != null && this.cards.contains(id);
            this.status = ok ? "문 열림" : "미등록 카드 / 경비번호 확인";
            if (ok) {
                this.openDoors();
            }

            this.changed();
            return ok;
        }
    }

    public boolean authorizesPasswordChange(ReceiverBlockEntity home) {
        return this.mode.equals("change_new")
            && this.isConnected()
            && !this.guardCall
            && home.getBlockPos().equals(this.passwordHome)
            && home.getBlockPos().equals(this.connectedTo)
            && this.receiversOf(home.getUnit()).contains(home);
    }

    public void pollHeld(ServerPlayer p) {
        Klp203rfBlockEntity.Press held = this.presses.get(p.getUUID());
        if (held != null && Intercom.inReach(p, this.worldPosition)) {
            if (held.home && held.guard && held.together >= 0L && !held.triggered && this.now() - held.together >= 60L) {
                held.triggered = true;
                if (!this.inCall && this.administrator == null && (this.operator == null || this.operator.equals(p.getUUID()))) {
                    this.operator = p.getUUID();
                    this.mode = "auth";
                    this.input = "";
                    this.inputAt = this.now();
                    this.status = "관리 비밀번호 4자리 입력 후 #";
                    this.changed();
                }
            }
        }
    }

    private Map<String, String> settings(ServerPlayer p) {
        HashMap<String, String> s = new HashMap<>();
        this.productLogo.settings(s);
        s.put("klp203rf", "1");
        s.put("klp_model", this.model().id);
        s.put("klp_label", this.model().label);
        s.put("klp_folder", this.model().folder);
        s.put("klp_reader", this.model().reader);
        s.put("display", this.display());
        s.put("visitor_light", this.visitorLight() ? "1" : "0");
        s.put("color_video", this.model().colorVideo() ? "1" : "0");
        s.put("status", this.status);
        s.put("auth", this.authenticated(p) ? "1" : "0");
        s.put("dong", this.dong);
        s.put("line", this.line);
        s.put("guard", this.guardNo);
        s.put("range", String.format(Locale.ROOT, "%02d~%02d", this.first, this.last));
        s.put("home_led", this.homeLamp() ? "1" : "0");
        s.put("guard_led", this.guardLamp() ? "1" : "0");
        s.put("talk_led", this.talkLamp() ? "1" : "0");
        s.put("connected", this.isConnected() ? "1" : "0");
        s.put("linked", this.exchange() != null ? "1" : "0");
        if (this.authenticated(p)) {
            s.put("cards", this.cards.size() + "");
        }

        return s;
    }

    public WallpadDataPacket packet(ServerPlayer p, boolean open) {
        return new WallpadDataPacket(
            this.worldPosition,
            open,
            this.kind().ordinal(),
            this.lobbyAddress(),
            false,
            (this.isConnected() ? CallState.CONNECTED : (this.inCall ? CallState.DIALING : CallState.IDLE)).ordinal(),
            "lobby",
            false,
            this.callTarget,
            "",
            List.of(),
            List.of(),
            List.copyOf(this.log),
            List.of(),
            this.settings(p),
            new long[0],
            List.of()
        );
    }

    public void openFor(ServerPlayer p) {
        ModNetwork.sendTo(p, this.packet(p, true));
    }

    @Override
    public void syncScreens() {
        if (this.level instanceof ServerLevel sl) {
            for (ServerPlayer p : Intercom.playersNear(sl, this.worldPosition, 8.0)) {
                ModNetwork.sendTo(p, this.packet(p, false));
            }
        }
    }

    private void changed() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            this.syncScreens();
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level instanceof ServerLevel sl) {
            sl.getServer().execute(this::publish);
        }
    }

    public static void tick(Level l, BlockPos at, BlockState state, Klp203rfBlockEntity b) {
        b.tickCaller();
        if (l instanceof ServerLevel sl) {
            for (Entry<UUID, Klp203rfBlockEntity.Press> entry : new HashMap<>(b.presses).entrySet()) {
                ServerPlayer p = sl.getServer().getPlayerList().getPlayer(entry.getKey());
                Klp203rfBlockEntity.Press held = entry.getValue();
                if (p != null && Intercom.inReach(p, at)) {
                    b.pollHeld(p);
                } else {
                    b.presses.remove(entry.getKey());
                }
            }

            if ((b.mode.equals("change_wait") || b.mode.equals("change_new")) && (!b.isConnected() || !Objects.equals(b.passwordHome, b.connectedTo))) {
                b.clearInput();
                b.status = "통화 종료로 비밀번호 변경 취소";
                b.changed();
            }

            if (b.mode.equals("change_wait") && b.now() >= b.changeAt) {
                b.mode = "change_new";
                b.input = "";
                b.inputAt = b.now();
                b.status = "새 비밀번호 4자리 입력 후 # (0000 삭제)";
                b.changed();
            }

            if (b.mode.equals("change_wait") && b.now() % 20L == 0L && b.level instanceof ServerLevel) {
                b.level.playSound(null, b.worldPosition, SoundEvents.NOTE_BLOCK_PLING.get(), SoundSource.BLOCKS, 0.5F, 1.3F);
            }

            if (b.now() == b.confirmationUntil) {
                b.changed();
            }

            if (b.administrator != null) {
                ServerPlayer p = sl.getServer().getPlayerList().getPlayer(b.administrator);
                if (p == null || !b.authenticated(p)) {
                    b.administrator = null;
                    b.clearInput();
                    b.changed();
                }
            }

            if (!b.mode.equals("normal") && b.now() - b.inputAt > 600L) {
                b.clearInput();
                b.changed();
            }

            if (b.inCall && b.exchange() == null) {
                b.hangUpFromCaller();
                b.callError(CallerBlockEntity.CallResult.NO_SIGNAL);
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag t = new CompoundTag();
        t.putBoolean("KlpPublic", true);
        this.productLogo.write(t);
        t.putString("Display", this.display());
        t.putBoolean("HomeLed", this.homeLamp());
        t.putBoolean("GuardLed", this.guardLamp());
        t.putBoolean("TalkLed", this.talkLamp());
        return t;
    }

    @Override
    protected void saveAdditional(CompoundTag t) {
        super.saveAdditional(t);
        t.putString("Dong", this.dong);
        t.putString("Line", this.line);
        t.putString("Guard", this.guardNo);
        t.putString("AdminPin", this.adminPin);
        t.putInt("First", this.first);
        t.putInt("Last", this.last);
        if (this.station != null) {
            t.putLong("Station", this.station.asLong());
        }

        ListTag list = new ListTag();
        this.cards.forEach(c -> list.add(StringTag.valueOf(c)));
        t.put("Cards", list);
    }

    @Override
    public void load(CompoundTag t) {
        if (t.getBoolean("KlpPublic")) {
            this.productLogo.read(t);
            this.clientDisplay = t.getString("Display");
            this.clientHome = t.getBoolean("HomeLed");
            this.clientGuard = t.getBoolean("GuardLed");
            this.clientTalk = t.getBoolean("TalkLed");
        } else {
            super.load(t);
            this.dong = UnitAddress.component(t.getString("Dong")) ? t.getString("Dong") : "101";
            this.line = UnitAddress.component(t.getString("Line")) ? t.getString("Line") : "1";
            this.guardNo = UnitAddress.component(t.getString("Guard")) ? t.getString("Guard") : "10";
            this.adminPin = t.getString("AdminPin").matches("[0-9]{4}") ? t.getString("AdminPin") : "9999";
            this.first = Math.max(0, Math.min(99, t.contains("First") ? t.getInt("First") : 1));
            this.last = Math.max(this.first, Math.min(99, t.contains("Last") ? t.getInt("Last") : 99));
            this.station = t.contains("Station") ? BlockPos.of(t.getLong("Station")) : null;
            this.cards.clear();

            for (Tag c : t.getList("Cards", 8)) {
                this.cards.add(c.getAsString());
            }

            this.administrator = null;
            this.presses.clear();
            this.clearInput();
            this.resetSession();
        }
    }

    private static class Press {
        boolean home;
        boolean guard;
        boolean triggered;
        boolean chord;
        long together = -1L;
    }
}
