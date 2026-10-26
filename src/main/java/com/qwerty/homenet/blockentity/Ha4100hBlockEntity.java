package com.qwerty.homenet.blockentity;

import com.qwerty.homenet.analog.AnalogBus;
import com.qwerty.homenet.analog.AnalogLobbyEndpoint;
import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.data.ZoneData;
import com.qwerty.homenet.guard.GuardExchange;
import com.qwerty.homenet.guard.GuardSystem;
import com.qwerty.homenet.ha.HaSafety;
import com.qwerty.homenet.ha.HaTelephone;
import com.qwerty.homenet.intercom.CallSounds;
import com.qwerty.homenet.intercom.CallState;
import com.qwerty.homenet.intercom.DoorStatus;
import com.qwerty.homenet.intercom.Intercom;
import com.qwerty.homenet.intercom.IntercomCaller;
import com.qwerty.homenet.move.DeviceMoves;
import com.qwerty.homenet.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class Ha4100hBlockEntity extends ReceiverBlockEntity {
    public static final List<String> LAMPS = List.of(
        "power", "emergency", "fire", "gas", "intrusion", "stop", "test", "security", "guard", "doorphone", "door", "away", "phone", "bell", "monitor"
    );
    private final HaTelephone telephone = new HaTelephone(this);
    private final HaSafety safety = new HaSafety(this);
    private final String[] numbers = new String[]{"", "", "", ""};
    private int entry = -1;
    private int entryTicks;
    private boolean editing;
    private boolean lifted;
    private boolean power = true;
    private boolean away;
    private boolean guardSelected;
    private String selected = "phone";
    private String digits = "";
    private String status = "";
    private int monitorTicks;
    private int doorTicks;
    private int brightness = 2;
    private int contrast = 2;
    private int speaker = 2;
    private int bell = 2;
    private int visualHash;
    private boolean cameraWatched;
    private BlockPos watchedCamera;
    private final LinkedHashMap<BlockPos, Ha4100hBlockEntity.Waiting> waiting = new LinkedHashMap<>();
    private final Map<String, Integer> clientLamps = new HashMap<>();
    private boolean clientScreen;
    private BlockPos clientCamera;

    public Ha4100hBlockEntity(BlockPos p, BlockState s) {
        super(ModBlockEntities.HA_4100H.get(), p, s);
        this.productLogo.set("logo_text", "HYUNDAI");
    }

    public HaTelephone phone() {
        return this.telephone;
    }

    public HaSafety safety() {
        return this.safety;
    }

    public boolean lifted() {
        return this.lifted;
    }

    public boolean power() {
        return this.power;
    }

    public String number(int i) {
        return this.numbers[i];
    }

    public String status() {
        return this.status;
    }

    public float speakerVolume() {
        return this.speaker * 0.25F;
    }

    public float bellVolume() {
        return this.bell * 0.25F;
    }

    public int brightness() {
        return this.brightness;
    }

    public int contrast() {
        return this.contrast;
    }

    public int monitorTicks() {
        return this.monitorTicks;
    }

    @Override
    public GuardSystem guardSystem() {
        return GuardSystem.BUS;
    }

    @Override
    public String manufacturer() {
        return "HYUNDAI";
    }

    @Override
    public AnalogBus analogBus() {
        return AnalogBus.COMMON_8_WIRE;
    }

    @Override
    public boolean canSetUnitLocally() {
        return false;
    }

    @Override
    public boolean supportsDoorCommunication() {
        return true;
    }

    @Override
    public boolean supportsSwitchboard(GuardExchange b) {
        return super.supportsSwitchboard(b) && b.supports(this.analogBus());
    }

    public boolean inScope(BlockPos p) {
        return this.level != null && p != null && ZoneData.connected(this.level, this.worldPosition, p);
    }

    public void refresh() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            this.syncScreens();
        }
    }

    public void notice(String text) {
        this.status = text;
        this.refresh();
    }

    public void announce(String text) {
        this.status = text;
        if (this.level instanceof ServerLevel server) {
            for (ServerPlayer p : Intercom.playersNear(server, this.worldPosition, 8.0)) {
                p.displayClientMessage(Component.literal(text), true);
            }
        }

        this.refresh();
    }

    public void cancelNumberEntry() {
        this.entry = -1;
        this.entryTicks = 0;
        this.editing = false;
        this.digits = "";
    }

    public boolean reportGuard(String type, boolean on) {
        GuardExchange g = this.switchboard();
        if (g == null) {
            return false;
        } else {
            g.setSafetyEvent(this.worldPosition, this.unit, type, on);
            return true;
        }
    }

    @Override
    public void unlinkSwitchboard() {
        for (String t : HaSafety.PRIORITY) {
            if (this.safety.active(t)) {
                this.reportGuard(t, false);
            }
        }

        this.safety.unlinked();
        this.guardSelected = false;
        super.unlinkSwitchboard();
    }

    public void soundDoorAlarm(float pitch) {
        BlockPos camera = this.linkedCamera();
        if (camera != null && this.inScope(camera)) {
            CallSounds.note(this.level, camera, SoundEvents.NOTE_BLOCK_BELL.value(), this.speakerVolume(), pitch);
        }
    }

    public void setSensor(String type, boolean value) {
        this.safety.sensor(type, value);
    }

    private String kind(String key) {
        return key.equals("front_door") ? "doorphone" : (key.equals("lobby") ? "lobby" : "guard");
    }

    private int priority(String key) {
        String var2 = this.kind(key);

        return switch (var2) {
            case "lobby" -> 0;
            case "guard" -> 1;
            default -> 2;
        };
    }

    private boolean validSource(BlockPos p, String key) {
        if (this.inScope(p) && this.level.isLoaded(p)) {
            BlockEntity b = this.level.getBlockEntity(p);
            if (b instanceof GuardExchange g) {
                return this.supportsSwitchboard(g);
            } else {
                return b instanceof DoorStationBlockEntity
                    ? this.hasLinkedCamera(p)
                    : b instanceof CallerBlockEntity
                        && b instanceof AnalogLobbyEndpoint lobby
                        && this.manufacturer().equalsIgnoreCase(lobby.manufacturer())
                        && lobby.supports(AnalogBus.COMMON_8_WIRE);
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean startRinging(BlockPos from, String key) {
        if (this.power && this.validSource(from, key) && !this.hasCallerSession(from) && this.waiting.size() < 4) {
            this.cancelNumberEntry();
            if (this.callState == CallState.IDLE) {
                boolean ok = super.startRinging(from, key);
                if (ok) {
                    this.selected = this.kind(key);
                    this.monitorTicks = 0;
                    this.refresh();
                }

                return ok;
            } else {
                this.waiting.put(from.immutable(), new Ha4100hBlockEntity.Waiting(key, 0));
                if (this.callState == CallState.RINGING && this.priority(key) < this.priority(this.incomingKey)) {
                    this.choose(from);
                }

                this.refresh();
                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean hasCallerSession(BlockPos from) {
        return this.waiting.containsKey(from) || super.hasCallerSession(from);
    }

    @Override
    public CallState callerSessionState(BlockPos from) {
        return this.waiting.containsKey(from) ? CallState.RINGING : super.callerSessionState(from);
    }

    private void choose(BlockPos target) {
        Ha4100hBlockEntity.Waiting next = this.waiting.remove(target);
        if (next != null) {
            if (this.caller != null && this.callState == CallState.RINGING) {
                this.waiting.put(this.caller, new Ha4100hBlockEntity.Waiting(this.incomingKey, this.callTicks));
            } else if (this.caller != null && this.callState != CallState.IDLE) {
                IntercomCaller c = this.callerBe();
                if (c != null) {
                    c.onReceiverHangUp(this.worldPosition, DoorStatus.ENDED);
                }
            }

            this.caller = target;
            this.incomingKey = next.key;
            this.callTicks = next.ticks;
            this.callState = CallState.RINGING;
            this.outgoing = false;
            this.selected = this.kind(this.incomingKey);
            this.setRingingState(true);
        }
    }

    private void promote() {
        if (this.callState == CallState.IDLE && !this.waiting.isEmpty()) {
            BlockPos p = this.waiting.entrySet().stream().min(Comparator.comparingInt(e -> this.priority(e.getValue().key))).orElseThrow().getKey();
            this.choose(p);
        }
    }

    private void cancelWaiting() {
        ArrayList<BlockPos> copy = new ArrayList<>(this.waiting.keySet());
        this.waiting.clear();

        for (BlockPos p : copy) {
            if (this.inScope(p) && this.level.isLoaded(p) && this.level.getBlockEntity(p) instanceof IntercomCaller c) {
                c.onReceiverHangUp(this.worldPosition, DoorStatus.ENDED);
            }
        }
    }

    @Override
    public void endCallerSession(BlockPos from, boolean missed) {
        boolean guardEnded = from.equals(this.caller) && this.callerBe() instanceof GuardExchange;
        if (this.waiting.remove(from) == null) {
            super.endCallerSession(from, missed);
        }

        this.promote();
        if (guardEnded && this.callState == CallState.IDLE && this.telephone.state() == HaTelephone.State.IDLE) {
            this.lifted = false;
            this.selected = "phone";
            this.monitorTicks = 0;
            this.status = "통화 종료 · 대기";
        }

        this.refresh();
    }

    @Override
    public boolean acceptSwitchboardCall(BlockPos station) {
        if (this.power && this.lifted && this.callState == CallState.DIALING && station.equals(this.caller) && this.switchboard() != null) {
            this.callState = CallState.CONNECTED;
            this.callTicks = 0;
            this.status = "경비실 통화 연결됨";
            this.refresh();
            return true;
        } else {
            return false;
        }
    }

    public boolean requestRegistration(ServerPlayer p) {
        if (this.power && !this.isBusy() && this.switchboard() != null) {
            if (!this.lifted) {
                this.lifted = true;
            }

            this.selected = "guard";
            this.callGuard(p, "guard");
            return this.isBusy();
        } else {
            return false;
        }
    }

    @Override
    public void callGuard(ServerPlayer p, String mode) {
        if (this.power && this.callState == CallState.IDLE) {
            GuardExchange g = this.switchboard();
            if (g == null) {
                this.guardSelected = false;
                this.notice("현대 BUS 경비실이 연결되지 않았습니다");
            } else if (g instanceof HyundaiBusConsoleBlockEntity board && !board.power()) {
                this.guardSelected = false;
                this.notice("경비실 전원이 꺼져 있습니다");
            } else {
                this.selected = "guard";
                if (!this.lifted) {
                    this.guardSelected = true;
                    this.notice("관리실 선택됨 · 수화기를 들면 HMC-7000을 호출합니다");
                } else {
                    this.guardSelected = false;
                    if (g instanceof Hmc7000BlockEntity hmc) {
                        Hmc7000BlockEntity.HomeCallResult result = hmc.requestHomeCall(this, p);
                        if (result != Hmc7000BlockEntity.HomeCallResult.ACCEPTED) {
                            this.notice(switch (result) {
                                case POWER_OFF -> "경비실 전원이 꺼져 있습니다";
                                case NOT_LINKED -> "경비실에 이 세대가 등록되지 않았습니다";
                                case NO_LINE -> "경비실 수신 회선이 모두 사용 중입니다";
                                case BUSY -> "경비실 또는 세대가 통화 중입니다";
                                default -> "경비실 호출이 거절되었습니다";
                            });
                        }
                    } else {
                        this.telephone.park();
                        this.incomingKey = g.callerKey();
                        this.caller = g.intercomPosition();
                        this.outgoing = false;
                        this.callState = CallState.DIALING;
                        this.callTicks = 0;
                        g.callFrom(this, p);
                        this.refresh();
                    }
                }
            }
        }
    }

    boolean beginHmcCall(Hmc7000BlockEntity hmc) {
        if (this.power && this.lifted && this.callState == CallState.IDLE && this.switchboard() == hmc) {
            this.telephone.park();
            this.guardSelected = false;
            this.selected = "guard";
            this.caller = hmc.getBlockPos();
            this.incomingKey = hmc.callerKey();
            this.outgoing = false;
            this.callTicks = 0;
            this.callState = CallState.DIALING;
            this.monitorTicks = 0;
            this.status = "경비실 호출 중 · 경비실 응답을 기다립니다";
            this.refresh();
            return true;
        } else {
            return false;
        }
    }

    boolean answeringHmc(Hmc7000BlockEntity hmc) {
        return this.power && this.lifted && this.callState == CallState.RINGING && hmc.getBlockPos().equals(this.caller) && this.switchboard() == hmc;
    }

    boolean talkingToHmc(Hmc7000BlockEntity hmc) {
        return this.power && this.lifted && this.callState == CallState.CONNECTED && hmc.getBlockPos().equals(this.caller) && this.switchboard() == hmc;
    }

    public void pickUp(ServerPlayer p) {
        if (!this.lifted) {
            this.handset(p);
        } else {
            if (this.guardSelected && this.callState == CallState.IDLE) {
                this.callGuard(p, "guard");
            }
        }
    }

    @Override
    public void answer() {
        if (this.power && this.lifted && this.callState == CallState.RINGING) {
            this.telephone.park();
            this.guardSelected = false;
            if (this.callerBe() instanceof Hmc7000BlockEntity hmc) {
                if (hmc.onAnswered(this.worldPosition)) {
                    this.callState = CallState.CONNECTED;
                    this.callTicks = 0;
                    this.setRingingState(false);
                } else {
                    this.resetCall();
                }
            } else {
                super.answer();
            }

            this.selected = this.kind(this.incomingKey);
            this.status = this.callState == CallState.CONNECTED ? "통화 연결됨" : "응답할 수 없는 호출입니다";
            this.refresh();
        }
    }

    @Override
    public void hangUp() {
        super.hangUp();
        this.promote();
        this.refresh();
    }

    private void selectLine(String name, ServerPlayer p) {
        if (name.equals("guard") && this.callState == CallState.IDLE && this.waiting.isEmpty()) {
            this.selected = "guard";
            this.callGuard(p, "guard");
        } else {
            Entry<BlockPos, Ha4100hBlockEntity.Waiting> target = this.waiting
                .entrySet()
                .stream()
                .filter(e -> name.equals("guard") ? !e.getValue().key.equals("front_door") : e.getValue().key.equals("front_door"))
                .min(Comparator.comparingInt(e -> this.priority(e.getValue().key)))
                .orElse(null);
            if (target != null) {
                this.choose(target.getKey());
            }

            this.selected = name;
            this.telephone.park();
            if (this.lifted && this.callState == CallState.RINGING) {
                this.answer();
            }
        }
    }

    public void handset(ServerPlayer p) {
        this.cancelNumberEntry();
        this.lifted = !this.lifted;
        if (this.lifted) {
            this.safety.stopReports();
            if (this.telephone.state() == HaTelephone.State.RINGING) {
                this.guardSelected = false;
                this.selected = "phone";
                this.telephone.select();
            } else if (this.callState == CallState.RINGING) {
                this.guardSelected = false;
                this.answer();
            } else if (this.guardSelected && this.power) {
                this.callGuard(p, "guard");
            } else {
                this.telephone.select();
            }
        } else {
            this.guardSelected = false;
            this.cancelWaiting();
            super.hangUp();
            this.telephone.replaceReceiver();
            this.selected = "phone";
            this.monitorTicks = 0;
        }

        this.refresh();
    }

    @Override
    public void openDoor() {
        if (this.power) {
            this.doorTicks = 20;
            if (this.callState != CallState.IDLE && this.incomingKey.equals("lobby")) {
                IntercomCaller c = this.callerBe();
                if (c != null) {
                    c.onDoorOpened(this.worldPosition);
                }

                this.resetCall();
                this.promote();
            } else {
                this.receiverDoors.open(this.level, this.worldPosition);
            }

            this.refresh();
        }
    }

    public void button(String key, ServerPlayer p) {
        if (key.equals("logo")) {
            this.openLogoEditor(p);
        } else if (key.equals("handset")) {
            this.handset(p);
        } else if (key.equals("power")) {
            this.power = !this.power;
            if (!this.power) {
                this.guardSelected = false;
                this.cancelWaiting();
                super.hangUp();
                this.monitorTicks = 0;
                this.safety.stopReports();
            }

            this.refresh();
        } else if (key.equals("phone")) {
            this.guardSelected = false;
            this.selected = "phone";
            this.telephone.press();
            this.refresh();
        } else if (key.equals("redial")) {
            this.guardSelected = false;
            this.selected = "phone";
            this.telephone.redial();
        } else if (!this.power) {
            this.notice("본체 전원이 꺼져 있습니다. 하단 전원 버튼을 켜세요.");
        } else {
            switch (key) {
                case "guard":
                    this.selectLine("guard", p);
                    break;
                case "doorphone":
                    if (this.safety.active("camera")) {
                        this.safety.clear("camera");
                    }

                    if ((this.callState == CallState.IDLE || !this.incomingKey.equals("front_door"))
                        && !this.waiting.values().stream().anyMatch(w -> w.key.equals("front_door"))) {
                        if (this.linkedCamera() == null) {
                            this.monitorTicks = 0;
                            this.notice("도어카메라가 연결되지 않았습니다. 같은 단지에서 홈 링커로 본체 → 도어카메라를 클릭하세요.");
                        } else {
                            this.monitorTicks = this.monitorTicks > 0 ? 0 : 600;
                        }
                    } else {
                        this.selectLine("doorphone", p);
                    }
                    break;
                case "door":
                    this.openDoor();
                    break;
                case "away":
                    if (this.getPersistentData().getBoolean("HmcGuardMessage")) {
                        this.getPersistentData().remove("HmcGuardMessage");
                        if (this.switchboard() instanceof Hmc7000BlockEntity g) {
                            g.clearHouseMessage(this.worldPosition);
                        }
                    } else {
                        this.away = !this.away;
                        this.safety.setArmed(this.away);
                        GuardExchange g = this.switchboard();
                        if (g != null) {
                            g.setAway(this.worldPosition, this.unit, this.away);
                        }
                    }
                    break;
                case "security":
                    this.safety.arm();
                    break;
                case "emergency":
                    this.safety.emergency();
                    break;
                case "test":
                    this.safety.trial();
                    break;
                case "stop":
                    this.safety.stop();
                    break;
                case "brightness":
                    this.brightness = (this.brightness + 1) % 5;
                    break;
                case "contrast":
                    this.contrast = (this.contrast + 1) % 5;
                    break;
                case "speaker":
                    this.speaker = (this.speaker + 1) % 4;
                    break;
                case "bell":
                    this.bell = (this.bell + 1) % 4;
            }

            this.refresh();
        }
    }

    public void key(String key) {
        if (key.matches("[0-9*#]")) {
            if (this.lifted || this.telephone.selected()) {
                this.telephone.key(key);
            } else if (this.power) {
                if (this.entry < 0) {
                    if ("1236".contains(key)) {
                        this.entry = "1236".indexOf(key);
                        this.editing = false;
                        this.entryTicks = 200;
                        this.digits = "";
                    }
                } else {
                    this.entryTicks = 200;
                    if (key.equals("*")) {
                        this.editing = true;
                        this.digits = "";
                    } else if (key.equals("0") && !this.editing) {
                        this.announce("기억 번호: " + this.numbers[this.entry]);
                    } else if (key.equals("#")) {
                        if (this.editing) {
                            if (!this.digits.isEmpty() && !this.digits.matches("[0-9]{4,15}")) {
                                this.status = "번호는 4~15자리입니다";
                            } else if (this.entry == 3 && this.duplicate(this.digits)) {
                                this.status = "이미 사용 중인 우리집 전화번호";
                            } else {
                                this.numbers[this.entry] = this.digits;
                                this.status = "번호 저장: " + this.digits;
                            }
                        } else {
                            this.announce("기억 번호: " + this.numbers[this.entry]);
                        }

                        this.cancelNumberEntry();
                    } else if (this.editing && this.digits.length() < 15) {
                        this.digits = this.digits + key;
                    }
                }

                this.refresh();
            }
        }
    }

    private boolean duplicate(String number) {
        return !number.isEmpty() && this.level instanceof ServerLevel server
            ? DeviceRegistry.get(server)
                .all()
                .stream()
                .anyMatch(
                    e -> !e.pos().equals(this.worldPosition)
                        && this.inScope(e.pos())
                        && server.isLoaded(e.pos())
                        && server.getBlockEntity(e.pos()) instanceof Ha4100hBlockEntity b
                        && b.number(3).equals(number)
                )
            : false;
    }

    public BlockPos videoCamera() {
        if (this.level != null && this.level.isClientSide) {
            return this.clientCamera;
        } else if (!this.power) {
            return null;
        } else if (this.callState != CallState.IDLE
            && this.caller != null
            && this.validSource(this.caller, this.incomingKey)
            && this.level.getBlockEntity(this.caller) instanceof CallerBlockEntity) {
            return this.incomingKey.equals("front_door") ? (this.hasLinkedCamera(this.caller) ? this.caller : null) : this.caller;
        } else {
            return this.monitorTicks > 0 ? this.linkedCamera() : null;
        }
    }

    public boolean screenOn() {
        return this.level != null && this.level.isClientSide
            ? this.clientScreen
            : this.power && (this.videoCamera() != null || this.safety.active("camera") && this.safety.age("camera") < 3600);
    }

    public int lamp(String name) {
        if (this.level != null && this.level.isClientSide) {
            return this.clientLamps.getOrDefault(name, 0);
        } else if (name.equals("phone")) {
            return !this.telephone.held() && this.telephone.state() != HaTelephone.State.RINGING
                ? (!this.telephone.selected() && this.telephone.state() == HaTelephone.State.IDLE ? 0 : 1)
                : 2;
        } else if (name.equals("bell")) {
            return this.telephone.state() == HaTelephone.State.RINGING ? 2 : 0;
        } else if (!this.power) {
            return 0;
        } else {
            return switch (name) {
                case "power" -> 1;
                case "emergency", "fire", "gas" -> this.safety.active(name) ? 2 : 0;
                case "intrusion" -> this.safety.active("security") ? 2 : 0;
                case "stop" -> this.safety.muted() ? 1 : 0;
                case "test" -> this.safety.test() >= 0 ? 1 : 0;
                case "security" -> this.safety.armed() ? 1 : 0;
                case "away" -> this.getPersistentData().getBoolean("HmcGuardMessage") ? 2 : (this.away ? 1 : 0);
                case "door" -> this.doorTicks > 0 ? 2 : 0;
                case "monitor" -> this.screenOn() ? 1 : 0;
                case "guard", "doorphone" -> {
                    boolean front = name.equals("doorphone");
                    boolean pending = this.waiting.values().stream().anyMatch(w -> w.key.equals("front_door") == front);
                    yield front && this.safety.active("camera")
                        ? 2
                        : (
                            pending
                                ? 2
                                : (
                                    this.callState != CallState.IDLE && this.incomingKey.equals("front_door") == front
                                        ? (this.callState == CallState.CONNECTED ? 1 : 2)
                                        : (front && this.monitorTicks > 0 ? 1 : 0)
                                )
                        );
                }
                default -> 0;
            };
        }
    }

    @Override
    protected float ringVolume() {
        return !this.power
                || this.callState != CallState.RINGING
                || this.telephone.selected() && this.telephone.state() == HaTelephone.State.CONNECTED && this.callTicks >= 60
            ? 0.0F
            : this.bellVolume();
    }

    @Override
    protected void tickExtra() {
        this.telephone.tick();
        if (this.power && this.callState == CallState.DIALING) {
            CallSounds.ringback(this.level, this.worldPosition, this.callTicks, 0.35F);
        }

        if (this.entryTicks > 0 && --this.entryTicks == 0) {
            this.cancelNumberEntry();
            this.refresh();
        }

        if (this.doorTicks > 0 && --this.doorTicks == 0) {
            this.refresh();
        }

        if (this.monitorTicks > 0 && --this.monitorTicks == 0) {
            this.refresh();
        }

        if (this.power) {
            Direction facing = this.getBlockState().getValue(WallMountedBlock.FACING);
            Direction[] dirs = new Direction[]{Direction.DOWN, facing.getClockWise(), facing.getCounterClockWise(), Direction.UP};
            String[] types = new String[]{"fire", "gas", "security", "emergency"};
            if (this.level.getGameTime() % 5L == 0L) {
                for (int i = 0; i < 4; i++) {
                    BlockPos pos = this.worldPosition.relative(dirs[i]);
                    this.safety.contact(types[i], this.inScope(pos) && this.level.getSignal(pos, dirs[i]) > 0);
                }
            }

            if (this.level.getGameTime() % 20L == 0L) {
                BlockPos cam = this.linkedCamera();
                if (cam != null) {
                    this.watchedCamera = cam;
                    this.cameraWatched = true;
                } else if (this.cameraWatched && this.watchedCamera != null && this.inScope(this.watchedCamera) && this.level.isLoaded(this.watchedCamera)) {
                    this.cameraWatched = false;
                    this.safety.trigger("camera");
                }
            }

            this.safety.tick();
        }

        for (Entry<BlockPos, Ha4100hBlockEntity.Waiting> e : new ArrayList<>(this.waiting.entrySet())) {
            Ha4100hBlockEntity.Waiting w = e.getValue();
            if (this.validSource(e.getKey(), w.key) && w.ticks < 599) {
                this.waiting.put(e.getKey(), new Ha4100hBlockEntity.Waiting(w.key, w.ticks + 1));
                if (w.ticks < 60 && w.ticks % 20 == 0) {
                    CallSounds.note(this.level, this.worldPosition, SoundEvents.NOTE_BLOCK_CHIME.value(), this.bellVolume(), 1.5F);
                }
            } else {
                this.waiting.remove(e.getKey());
                if (this.inScope(e.getKey()) && this.level.isLoaded(e.getKey()) && this.level.getBlockEntity(e.getKey()) instanceof IntercomCaller c) {
                    c.onReceiverHangUp(this.worldPosition, DoorStatus.NO_ANSWER);
                }
            }
        }

        if (this.callState == CallState.RINGING && this.callTicks >= 599) {
            this.stopRinging(true);
        } else if (this.callState == CallState.DIALING && this.callTicks >= 599) {
            super.hangUp();
        } else if (this.callState == CallState.CONNECTED && this.incomingKey.equals("lobby") && this.callTicks >= 1199) {
            super.hangUp();
        }

        this.promote();
        int hash = Objects.hash(this.power, this.lifted, this.screenOn(), this.videoCamera(), this.telephone.state(), this.telephone.held(), this.status);

        for (String l : LAMPS) {
            hash = hash * 31 + this.lamp(l);
        }

        if (hash != this.visualHash) {
            this.visualHash = hash;
            this.refresh();
        }
    }

    @Override
    public void handleAction(ServerPlayer p, ReceiverBlockEntity.Action a, BlockPos target, String text) {
        if (a == ReceiverBlockEntity.Action.SET_SETTING && text != null) {
            if (!text.equals("logo") && !text.equals("register") && !text.equals("cancel_registration")) {
                if (text.startsWith("button=")) {
                    this.button(text.substring(7), p);
                } else if (text.startsWith("key=")) {
                    this.key(text.substring(4));
                }
            } else if (text.equals("logo")) {
                this.openLogoEditor(p);
            } else if (text.equals("register")) {
                this.requestRegistration(p);
            } else {
                this.hangUp();
            }
        } else if (a == ReceiverBlockEntity.Action.CALL_GUARD) {
            this.button("guard", p);
        } else if (a == ReceiverBlockEntity.Action.PICKUP) {
            this.pickUp(p);
        } else if (a == ReceiverBlockEntity.Action.HANG_UP) {
            if (this.lifted) {
                this.handset(p);
            } else {
                this.hangUp();
            }
        } else if (a == ReceiverBlockEntity.Action.SEND_MESSAGE) {
            if (this.callerBe() instanceof Hmc7000BlockEntity hmc && hmc.sendHomeMessage(this, p, text)) {
                return;
            }

            if (this.telephone.selected() && this.telephone.state() == HaTelephone.State.CONNECTED) {
                this.telephone.message(p, text);
            } else if (this.lifted && this.callState == CallState.CONNECTED) {
                super.sendMessage(p, text);
            } else {
                this.notice("통화 연결 후 메시지를 보낼 수 있습니다");
            }
        } else if (a == ReceiverBlockEntity.Action.REFRESH) {
            this.syncScreens();
        }
    }

    @Override
    public void onBroken() {
        this.cancelWaiting();
        this.telephone.hangup();

        for (String t : HaSafety.PRIORITY) {
            if (this.safety.active(t)) {
                this.reportGuard(t, false);
            }
        }

        super.onBroken();
    }

    @Override
    protected Map<String, String> settingsForClient() {
        Map<String, String> s = super.settingsForClient();
        s.put("ha4100h_config", "1");
        s.put("ha4100h", "1");
        s.put("ha_controls_version", "1.0.8");
        s.put("guard_ready", this.guardSelected ? "1" : "0");
        s.put(
            "camera_linked",
            this.level instanceof ServerLevel sl && DeviceRegistry.get(sl).all().stream().anyMatch(e -> this.hasLinkedCamera(e.pos())) ? "1" : "0"
        );
        s.put("zoned", this.level instanceof ServerLevel slx && ZoneData.get(slx).zoneAt(this.worldPosition) != 0 ? "1" : "0");
        s.put("linked", this.switchboard() != null ? "1" : "0");
        s.put("registration", this.callState != CallState.IDLE && this.selected.equals("guard") ? "1" : "0");
        s.put("power", this.power ? "1" : "0");
        s.put("lifted", this.lifted ? "1" : "0");
        s.put("status", this.status);
        s.put("digits", this.entry >= 0 ? this.digits : this.telephone.digits());
        s.put("phone_state", this.telephone.state().name());
        s.put("brightness", this.brightness + "");
        s.put("contrast", this.contrast + "");
        s.put("speaker", this.speaker + "");
        s.put("bell", this.bell + "");
        s.put("screen", this.screenOn() ? "1" : "0");

        for (String l : LAMPS) {
            s.put("ha_lamp_" + l, this.lamp(l) + "");
        }

        BlockPos cam = this.videoCamera();
        if (cam != null) {
            s.put("camera", "1");
            s.put("camera_x", cam.getX() + "");
            s.put("camera_y", cam.getY() + "");
            s.put("camera_z", cam.getZ() + "");
        }

        return s;
    }

    @Override
    protected void saveAdditional(CompoundTag t) {
        super.saveAdditional(t);
        t.putBoolean("HaGuardSelected", this.guardSelected);
        t.putBoolean("HaPower", this.power);
        t.putBoolean("HaLifted", this.lifted);
        t.putBoolean("HaAway", this.away);
        t.putInt("HaBrightness", this.brightness);
        t.putInt("HaContrast", this.contrast);
        t.putInt("HaSpeaker", this.speaker);
        t.putInt("HaBell", this.bell);

        for (int i = 0; i < 4; i++) {
            t.putString("HaNumber" + i, this.numbers[i]);
        }

        this.telephone.save(t);
        this.safety.save(t);
        t.putBoolean("HaCameraWatched", this.cameraWatched);
        if (this.watchedCamera != null) {
            t.putLong("HaWatchedCamera", this.watchedCamera.asLong());
        }

        for (String l : LAMPS) {
            t.putInt("HaLamp_" + l, this.lamp(l));
        }

        t.putBoolean("HaScreen", this.screenOn());
        BlockPos cam = this.videoCamera();
        if (cam != null) {
            t.putLong("HaVideo", cam.asLong());
        }
    }

    @Override
    public void load(CompoundTag t) {
        super.load(t);
        this.guardSelected = t.getBoolean("HaGuardSelected");
        this.selected = this.guardSelected ? "guard" : "phone";
        this.power = !t.contains("HaPower") || t.getBoolean("HaPower");
        this.lifted = t.getBoolean("HaLifted");
        this.away = t.getBoolean("HaAway");
        this.brightness = this.clamp(t, "HaBrightness", 2, 4);
        this.contrast = this.clamp(t, "HaContrast", 2, 4);
        this.speaker = this.clamp(t, "HaSpeaker", 2, 3);
        this.bell = this.clamp(t, "HaBell", 2, 3);

        for (int i = 0; i < 4; i++) {
            this.numbers[i] = t.getString("HaNumber" + i);
        }

        this.telephone.load(t);
        this.safety.load(t);
        this.cameraWatched = t.getBoolean("HaCameraWatched");
        this.watchedCamera = t.contains("HaWatchedCamera") ? BlockPos.of(t.getLong("HaWatchedCamera")) : null;
        this.clientLamps.clear();

        for (String l : LAMPS) {
            this.clientLamps.put(l, t.getInt("HaLamp_" + l));
        }

        this.clientScreen = t.getBoolean("HaScreen");
        this.clientCamera = t.contains("HaVideo") ? BlockPos.of(t.getLong("HaVideo")) : null;
    }

    private int clamp(CompoundTag t, String k, int def, int max) {
        return Math.max(0, Math.min(max, t.contains(k) ? t.getInt(k) : def));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide) {
            this.lifted = false;
            this.waiting.clear();
        }
    }

    static {
        DeviceMoves.registerPositionField("HaWatchedCamera");
    }

    private record Waiting(String key, int ticks) {
    }
}
