package com.qwerty.homenet.sht;

import com.qwerty.homenet.analog.AnalogBus;
import com.qwerty.homenet.analog.AnalogLobbyEndpoint;
import com.qwerty.homenet.blockentity.CallerBlockEntity;
import com.qwerty.homenet.blockentity.DoorStationBlockEntity;
import com.qwerty.homenet.blockentity.LobbyPhoneBlockEntity;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.data.ZoneData;
import com.qwerty.homenet.guard.GuardExchange;
import com.qwerty.homenet.guard.GuardSystem;
import com.qwerty.homenet.intercom.CallSounds;
import com.qwerty.homenet.intercom.CallState;
import com.qwerty.homenet.intercom.Intercom;
import com.qwerty.homenet.intercom.IntercomLine;
import com.qwerty.homenet.move.DeviceOffsets;
import com.qwerty.homenet.network.WallpadDataPacket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Station extends ReceiverBlockEntity {
    public boolean docked = true;
    public boolean bus = true;
    public boolean folded = true;
    boolean portableRegistered;
    boolean portableActive;
    boolean portableCommand;
    int transfer;
    int transferTicks;
    final Set<UUID> portableViews = new HashSet<>();
    UUID serial = UUID.randomUUID();
    UUID ticket = UUID.randomUUID();
    UUID portableTicket = this.ticket;
    String status = "대기";
    String digits = "";
    String editing = "";
    String pin = "00";
    String radioPin = "00";
    String fixedPin = "00";
    String number = "";
    String last = "";
    String alarm = "";
    String guardNumber = "";
    boolean memory;
    boolean verify;
    boolean audio;
    boolean hold;
    boolean away;
    boolean armed;
    boolean test;
    boolean mute;
    boolean tone = true;
    boolean recall;
    int channel = 1;
    int volume = 2;
    int ring = 2;
    int brightness = 2;
    int contrast = 2;
    int internal;
    int monitor;
    int alarmTime;
    int awayTime;
    int resetTime;
    int clock;
    int phone;
    int phoneTime;
    int reportStage = -1;
    int reportTimer;
    int reportAudio;
    BlockPos phoneAt;
    BlockPos clientVideo;
    int clientLights;
    int lastVisualLights = Integer.MIN_VALUE;
    Map<String, String> stored = new HashMap<>();
    Set<String> edges = new HashSet<>();
    Map<UUID, Boolean> observers = new HashMap<>();
    BlockPos forwardedGuard;

    @Override
    public AABB getRenderBoundingBox() {
        AABB bounds = new AABB(this.worldPosition).inflate(0.25, 1.0, 0.25);
        if (this.level != null) {
            Vec3 offset = DeviceOffsets.get(this.level, this.worldPosition);
            return bounds.move(offset);
        } else {
            return bounds;
        }
    }

    public Station(BlockPos p, BlockState s) {
        this(Product.TYPE.get(), p, s);
    }

    protected Station(BlockEntityType<?> type, BlockPos p, BlockState s) {
        super(type, p, s);
        this.productLogo.set("logo_text", "SAMSUNG");
    }

    public boolean wired() {
        return false;
    }

    @Override
    public DeviceRegistry.Kind kind() {
        return DeviceRegistry.Kind.VIDEO_PHONE;
    }

    @Override
    public GuardSystem guardSystem() {
        return this.bus ? GuardSystem.BUS : GuardSystem.STAR;
    }

    @Override
    public String manufacturer() {
        return "SAMSUNG";
    }

    @Override
    public AnalogBus analogBus() {
        return this.bus ? AnalogBus.COMMON_8_WIRE : AnalogBus.DEDICATED;
    }

    @Override
    public boolean canSetUnitLocally() {
        return true;
    }

    @Override
    public boolean supportsSwitchboard(GuardExchange g) {
        return super.supportsSwitchboard(g);
    }

    boolean same(BlockPos p) {
        return p != null && this.level != null && ZoneData.connected(this.level, this.worldPosition, p);
    }

    boolean paired() {
        return this.portableCommand || this.pin.equals(this.radioPin) && this.pin.equals(this.fixedPin);
    }

    boolean ownsRadio(ItemStack s) {
        return !this.docked
            && s.is(Product.RADIO.get())
            && s.hasTag()
            && s.getTag().hasUUID("serial")
            && this.serial.equals(s.getTag().getUUID("serial"))
            && s.getTag().hasUUID("ticket")
            && this.ticket.equals(s.getTag().getUUID("ticket"));
    }

    public boolean supportsWirelessPortable() {
        return true;
    }

    public boolean owns(ItemStack s) {
        return this.ownsRadio(s) || this.ownsPortable(s);
    }

    public boolean ownsPortable(ItemStack stack) {
        return this.portableRegistered
            && Portable.compatible(this, stack)
            && stack.hasTag()
            && stack.getTag().getLong("base") == this.worldPosition.asLong()
            && (this.level == null || stack.getTag().getString("dimension").equals(this.level.dimension().location().toString()))
            && stack.getTag().hasUUID("serial")
            && this.serial.equals(stack.getTag().getUUID("serial"))
            && stack.getTag().hasUUID("ticket")
            && this.portableTicket.equals(stack.getTag().getUUID("ticket"));
    }

    static Station find(ServerPlayer p, ItemStack s) {
        if (s.hasTag() && s.getTag().hasUUID("serial")) {
            ServerLevel sl = p.serverLevel();

            for (DeviceRegistry.Entry e : DeviceRegistry.get(sl).all()) {
                if (sl.isLoaded(e.pos()) && sl.getBlockEntity(e.pos()) instanceof Station b && b.owns(s)) {
                    return b;
                }
            }

            return null;
        } else {
            return null;
        }
    }

    public boolean allowed(ServerPlayer p, boolean remote) {
        if (p.level() != this.level) {
            return false;
        } else if (!remote) {
            return Intercom.inReach(p, this.worldPosition);
        } else if (p.level() == this.level
            && this.same(p.blockPosition())
            && !(p.distanceToSqr(Vec3.atCenterOf(this.worldPosition).add(DeviceOffsets.get(this.level, this.worldPosition))) > 1024.0)) {
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                ItemStack s = p.getInventory().getItem(i);
                if (this.owns(s) && s.getTag().getBoolean("power") && s.getTag().getInt("battery") > 0) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    void update() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.lastVisualLights = this.lights();
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            this.syncScreens();
        }
    }

    void tell(String s) {
        this.status = s;
        this.update();
    }

    public void lift(ServerPlayer p) {
        if (this.docked) {
            ItemStack s = new ItemStack(Product.RADIO.get());
            CompoundTag t = s.getOrCreateTag();
            t.putUUID("serial", this.serial);
            t.putUUID("ticket", this.ticket);
            t.putBoolean("power", true);
            t.putInt("battery", 36000);
            if (!p.getInventory().add(s)) {
                this.tell("수화기를 넣을 인벤토리 공간이 필요합니다");
            } else {
                this.docked = false;
                this.tell("무선수화기 분리");
            }
        }
    }

    public void replace(ServerPlayer p) {
        if (this.docked) {
            this.lift(p);
        } else {
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                ItemStack s = p.getInventory().getItem(i);
                if (this.ownsRadio(s)) {
                    if (!this.portableActive && this.transfer != 1 || this.internal == 3) {
                        this.finish();
                    }

                    s.shrink(1);
                    this.docked = true;
                    this.ticket = UUID.randomUUID();
                    if (Boolean.TRUE.equals(this.observers.get(p.getUUID())) && !this.portableViews.contains(p.getUUID())) {
                        this.observers.remove(p.getUUID());
                    }

                    this.tell("수화기 거치 · 충전 중");
                    return;
                }
            }

            this.tell("이 본체의 등록된 수화기가 인벤토리에 없습니다");
        }
    }

    @Override
    public void openFor(ServerPlayer p) {
        this.open(p, false);
    }

    public void open(ServerPlayer p, boolean remote) {
        if (this.allowed(p, remote)) {
            this.portableViews.remove(p.getUUID());
            this.observers.put(p.getUUID(), remote);
            Wire.show(p, this.buildPacket(true), remote, false);
        }
    }

    public void openPortable(ServerPlayer p) {
        if (this.allowed(p, true) && Portable.carried(p, this)) {
            this.portableViews.add(p.getUUID());
            this.observers.put(p.getUUID(), true);
            Wire.show(p, this.buildPacket(true), true, true);
        }
    }

    public WallpadDataPacket snapshot() {
        return this.buildPacket(false);
    }

    @Override
    public void syncScreens() {
        if (this.level instanceof ServerLevel sl) {
            for (Entry<UUID, Boolean> e : new ArrayList<>(this.observers.entrySet())) {
                ServerPlayer p = sl.getServer().getPlayerList().getPlayer(e.getKey());
                if (p != null && this.allowed(p, e.getValue()) && (!this.portableViews.contains(e.getKey()) || Portable.carried(p, this))) {
                    Wire.show(p, this.buildPacket(false), e.getValue(), this.portableViews.contains(p.getUUID()));
                } else {
                    this.observers.remove(e.getKey());
                    this.portableViews.remove(e.getKey());
                }
            }
        }
    }

    @Override
    protected Map<String, String> settingsForClient() {
        Map<String, String> s = super.settingsForClient();
        s.put("portable_ringing", this.portableRinging() ? "1" : "0");
        s.put("portable_registered", this.portableRegistered ? "1" : "0");
        s.put("portable_active", this.portableActive ? "1" : "0");
        s.put("sht", "new");
        s.put("wired", this.wired() ? "1" : "0");
        s.put("mode", this.bus ? "BUS" : "STAR");
        s.put("status", this.status);
        s.put("docked", this.docked ? "1" : "0");
        s.put("folded", this.folded ? "1" : "0");
        s.put("alarm", this.alarm);
        s.put("armed", this.armed ? "1" : "0");
        s.put("away", this.away ? "1" : "0");
        s.put("guard_no", this.guardNumber);
        s.put("brightness", this.brightness + "");
        s.put("contrast", this.contrast + "");
        s.put("channel", this.channel + "");
        s.put("phone", this.phone + "");
        s.put("intercom", this.internal + "");
        s.put("paired", this.paired() ? "1" : "0");
        s.put("lights", this.lights() + "");
        BlockPos v = this.camera();
        if (v != null) {
            s.put("video", "1");
            s.put("vx", v.getX() + "");
            s.put("vy", v.getY() + "");
            s.put("vz", v.getZ() + "");
        }

        return s;
    }

    boolean source(BlockPos at) {
        if (this.same(at) && this.level.isLoaded(at)) {
            BlockEntity b = this.level.getBlockEntity(at);
            if (b instanceof Ht100s.Unit h) {
                return !h.unit.isEmpty() && DeviceRegistry.normalize(h.unit).equals(DeviceRegistry.normalize(this.getUnit()));
            } else if (b instanceof DoorStationBlockEntity) {
                return this.hasLinkedCamera(at);
            } else if (b instanceof GuardExchange g) {
                return this.supportsSwitchboard(g);
            } else if (b instanceof ReceiverBlockEntity r) {
                return !this.bus && r.kind() == DeviceRegistry.Kind.GUARD_CONSOLE && r.guardSystem() == GuardSystem.STAR;
            } else {
                return b instanceof AnalogLobbyEndpoint a ? GuardRouting.lobbyMatches(this.guardSystem(), a) : !this.bus && b instanceof LobbyPhoneBlockEntity;
            }
        } else {
            return false;
        }
    }

    @Override
    protected String preferredGuard() {
        return GuardRouting.preferred(this);
    }

    void setGuardNumber(String raw) {
        this.guardNumber = DeviceRegistry.digits(raw);
        if (this.guardNumber.length() > 8) {
            this.guardNumber = this.guardNumber.substring(0, 8);
        }

        this.tell(this.guardNumber.isEmpty() ? "경비실 우선번호 해제" : "경비실 " + this.guardNumber + "번 우선 호출");
    }

    @Override
    protected BlockPos ring(BlockPos at, String key, int depth) {
        if (depth <= 3 && this.source(at)) {
            if (this.away && key.equals("lobby")) {
                if (this.switchboard() instanceof ReceiverBlockEntity r) {
                    return r.ring(at, key);
                }

                if (!this.bus && this.level instanceof ServerLevel sl) {
                    for (BlockPos p : DeviceRegistry.get(sl).guards(sl, this.worldPosition, false)) {
                        if (this.same(p) && this.level.getBlockEntity(p) instanceof ReceiverBlockEntity r && r.guardSystem() == GuardSystem.STAR) {
                            return r.ring(at, key);
                        }
                    }
                }
            }

            return super.ring(at, key, depth);
        } else {
            return null;
        }
    }

    @Override
    public boolean startRinging(BlockPos at, String key) {
        if (!this.source(at)) {
            return false;
        } else {
            boolean ok = super.startRinging(at, key);
            if (ok) {
                this.tell("착신 · 스피커 또는 무선수화기로 응답");
            }

            return ok;
        }
    }

    @Override
    public void callGuard(ServerPlayer p, String unused) {
        this.forwardedGuard = null;
        if (!this.paired()) {
            this.tell("수화기와 본체의 두 자리 암호를 일치시키세요");
        } else {
            boolean activeAudio = this.audio;
            this.endPhone();
            super.hangUp();
            this.audio = activeAudio;
            this.internal = 0;
            String selected = this.preferredGuard();
            if (this.bus) {
                GuardExchange g = null;
                if (!selected.isBlank() && this.level instanceof ServerLevel sl) {
                    for (BlockPos at : DeviceRegistry.get(sl).guardsByNumber(sl, this.worldPosition, selected)) {
                        if (this.same(at) && sl.isLoaded(at) && sl.getBlockEntity(at) instanceof GuardExchange candidate && this.supportsSwitchboard(candidate)
                            )
                         {
                            g = candidate;
                            break;
                        }
                    }
                } else {
                    g = this.switchboard();
                }

                if (g == null) {
                    this.tell("지정 경비실기에 연결할 수 없습니다");
                    return;
                }

                this.caller = g.intercomPosition();
                this.incomingKey = g.callerKey();
                this.callState = CallState.DIALING;
                this.outgoing = false;
                this.callTicks = 0;
                g.callFrom(this, p);
            } else if (this.level instanceof ServerLevel sl) {
                List<BlockPos> peers = DeviceRegistry.get(sl)
                    .guards(sl, this.worldPosition, false)
                    .stream()
                    .filter(
                        atx -> this.same(atx)
                            && sl.isLoaded(atx)
                            && sl.getBlockEntity(atx) instanceof ReceiverBlockEntity r
                            && r.kind() == DeviceRegistry.Kind.GUARD_CONSOLE
                            && r.guardSystem() == GuardSystem.STAR
                            && (selected.isBlank() || DeviceRegistry.digits(r.getUnit()).equals(selected))
                    )
                    .toList();
                if (peers.isEmpty()) {
                    this.tell("지정 경비실기에 연결할 수 없습니다");
                    return;
                }

                this.startOutgoing(p, peers, "경비실");
            }

            this.tell("경비실 호출 중");
        }
    }

    @Override
    public boolean acceptSwitchboardCall(BlockPos p) {
        if (this.audio && this.callState == CallState.DIALING && p.equals(this.caller)) {
            boolean valid = this.switchboard() != null;
            if (!valid && this.same(p) && this.level.isLoaded(p) && this.level.getBlockEntity(p) instanceof GuardExchange g) {
                valid = this.supportsSwitchboard(g)
                    && (
                        p.equals(this.forwardedGuard)
                            || DeviceRegistry.digits(g instanceof ReceiverBlockEntity r ? r.getUnit() : "")
                                .equals(DeviceRegistry.digits(this.preferredGuard()))
                    );
            }

            if (!valid) {
                return false;
            } else {
                this.callState = CallState.CONNECTED;
                this.callTicks = 0;
                this.tell("경비실 통화");
                return true;
            }
        } else {
            return false;
        }
    }

    public boolean forwardGuardTo(GuardExchange guard, ServerPlayer p) {
        BlockPos at = guard.intercomPosition();
        if (this.same(at) && this.supportsSwitchboard(guard) && this.level instanceof ServerLevel) {
            this.forwardedGuard = at;
            this.caller = at;
            this.incomingKey = guard.callerKey();
            this.callState = CallState.DIALING;
            this.outgoing = false;
            this.callTicks = 0;
            guard.callFrom(this, p);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void answer() {
        if (this.paired() && this.audio) {
            super.answer();
            this.update();
        }
    }

    BlockPos cameraLink() {
        return this.linkedCamera();
    }

    public BlockPos camera() {
        if (this.level != null && this.level.isClientSide) {
            return this.clientVideo;
        } else {
            if (this.callState != CallState.IDLE && this.source(this.caller)) {
                BlockEntity origin = this.level.getBlockEntity(this.caller);
                if (origin instanceof Ht100s.Unit camera) {
                    return DoorCameraRouting.isVideo(camera) ? this.caller : null;
                }

                if (origin instanceof DoorStationBlockEntity door && door.isDoorPhone()) {
                    return null;
                }

                if (origin instanceof CallerBlockEntity) {
                    return this.caller;
                }
            }

            return this.monitor > 0 ? this.cameraLink() : null;
        }
    }

    public int lights() {
        if (this.level != null && this.level.isClientSide) {
            return this.clientLights;
        } else {
            boolean doorCall = this.callState != CallState.IDLE && (this.incomingKey.equals("front_door") || this.incomingKey.equals("lobby"));
            boolean guardCall = this.callState != CallState.IDLE && !doorCall;
            boolean line = this.phone > 0 || this.audio && this.internal == 0 && this.callState == CallState.IDLE && this.monitor == 0;
            int var10000 = 1
                | (this.docked ? 2 : 0)
                | (!this.armed && this.awayTime <= 0 ? 0 : 4)
                | (this.away ? 8 : 0)
                | (this.monitor <= 0 && !doorCall ? 0 : 32)
                | (guardCall ? 64 : 0)
                | (line ? 128 : 0);
            String var4 = this.alarm;

            return var10000 | switch (var4) {
                case "fire" -> 256;
                case "gas" -> 512;
                case "security" -> 1024;
                case "emergency" -> 16;
                default -> 0;
            };
        }
    }

    @Override
    protected float ringVolume() {
        return this.ring * 0.25F;
    }

    void finish() {
        this.forwardedGuard = null;
        this.portableActive = false;
        this.transfer = this.transferTicks = 0;
        super.hangUp();
        this.endPhone();
        this.internal = 0;
        this.audio = false;
        this.monitor = 0;
        this.digits = "";
        this.recall = false;
        this.tell("대기");
    }

    @Override
    protected void resetCall() {
        super.resetCall();
        this.audio = false;
        this.update();
    }

    @Override
    public void endCallerSession(BlockPos p, boolean missed) {
        super.endCallerSession(p, missed);
        this.update();
    }

    void talk() {
        if (!this.paired()) {
            this.tell("무선수화기 암호가 일치하지 않습니다");
        } else if (this.callState == CallState.RINGING) {
            this.audio = true;
            this.answer();
        } else if ((this.callState != CallState.CONNECTED || !this.audio) && (this.phone != 3 || !this.audio) && this.internal != 3) {
            if (this.internal != 1 && this.internal != 2) {
                if (this.phone == 2) {
                    Station b = this.phonePeer();
                    if (b != null) {
                        this.phone = b.phone = 3;
                        this.phoneTime = b.phoneTime = 0;
                        this.audio = true;
                        b.update();
                    }
                } else if (this.phone > 0) {
                    this.audio = true;
                    this.hold = false;
                } else if (this.callState == CallState.DIALING || this.callState == CallState.CONNECTED) {
                    this.audio = true;
                } else if (this.audio) {
                    this.digits = "";
                    this.tell("국선 발신음 · 번호 입력");
                } else {
                    this.audio = true;
                    this.digits = "";
                    this.tell("국선 발신음 · 번호 입력");
                }

                this.update();
            } else {
                this.internal = 3;
                this.audio = true;
                this.tell("본체 ↔ 무선수화기 내선 통화");
            }
        } else {
            this.finish();
        }
    }

    Station phonePeer() {
        return this.same(this.phoneAt) && this.level.isLoaded(this.phoneAt) && this.level.getBlockEntity(this.phoneAt) instanceof Station s ? s : null;
    }

    void clearPhone() {
        this.phone = 0;
        this.phoneAt = null;
        this.phoneTime = 0;
        this.hold = false;
    }

    void endPhone() {
        Station b = this.phonePeer();
        if (b != null && this.worldPosition.equals(b.phoneAt)) {
            b.clearPhone();
            b.audio = false;
            b.update();
        }

        this.clearPhone();
    }

    public boolean dialNumber(String n) {
        if (n.matches("[0-9]{1,18}") && this.level instanceof ServerLevel sl) {
            boolean var8 = this.audio;
            this.endPhone();
            super.hangUp();
            this.audio = var8;
            this.internal = 0;

            for (DeviceRegistry.Entry e : DeviceRegistry.get(sl).all()) {
                if (!e.pos().equals(this.worldPosition)
                    && this.same(e.pos())
                    && sl.isLoaded(e.pos())
                    && sl.getBlockEntity(e.pos()) instanceof Station b
                    && b.number.equals(n)) {
                    if (b.phone > 0) {
                        this.tell("통화 중");
                        return false;
                    }

                    this.phone = 1;
                    this.phoneAt = e.pos();
                    this.phoneTime = 0;
                    b.phone = 2;
                    b.phoneAt = this.worldPosition;
                    b.phoneTime = 0;
                    this.last = n;
                    b.update();
                    this.tell("국선 호출 중");
                    return true;
                }
            }

            this.tell("게임 내에 등록된 전화번호가 없습니다");
            return false;
        } else {
            return false;
        }
    }

    public void say(ServerPlayer p, String text, boolean remote) {
        String t = Intercom.sanitize(text, 80);
        if (!t.isBlank()) {
            if (this.allowed(p, remote) && (!remote || this.paired())) {
                if (this.internal == 3) {
                    this.addLine(new IntercomLine(remote ? "무선수화기" : "본체", p.getGameProfile().getName(), t));
                    this.update();
                } else if (this.phone == 3 && !this.hold) {
                    Station b = this.phonePeer();
                    if (b != null && this.audio) {
                        Intercom.say((ServerLevel)this.level, this, b, p, "전화", t);
                        b.update();
                        this.update();
                    }
                } else if (this.callState == CallState.CONNECTED && this.audio) {
                    super.sendMessage(p, t);
                    this.update();
                } else {
                    this.tell("통화가 연결된 후 메시지를 보낼 수 있습니다");
                }
            }
        }
    }

    void saveMemory(boolean remote) {
        if (!this.editing.isEmpty()) {
            if (Set.of("pin", "radioPin", "fixedPin").contains(this.editing) && this.digits.length() != 2) {
                this.tell("암호는 두 자리입니다");
            } else {
                String var2 = this.editing;
                switch (var2) {
                    case "pin":
                        this.pin = this.digits;
                        break;
                    case "radioPin":
                        this.radioPin = this.digits;
                        break;
                    case "fixedPin":
                        this.fixedPin = this.digits;
                        break;
                    case "number":
                        this.number = this.digits;
                        break;
                    case "tone":
                        this.tone = !this.digits.equals("0");
                        break;
                    default:
                        if (Set.of("*", "#").contains(this.editing) && Set.of("112", "113", "119").contains(this.digits)) {
                            this.tell("자동통보 번호로 등록할 수 없습니다");
                            return;
                        }

                        this.stored.put((remote ? "R:" : "B:") + this.editing, this.digits);
                }

                this.memory = false;
                this.editing = "";
                this.digits = "";
                this.tell("기억되었습니다");
            }
        }
    }

    public void digit(String k, boolean remote) {
        if (k.matches("[0-9*#]")) {
            if (!remote
                || this.portableActive
                || !k.equals("*")
                || this.callState != CallState.CONNECTED
                || !this.audio
                || this.hold
                || this.docked
                || !this.incomingKey.equals("front_door") && !this.incomingKey.equals("lobby")) {
                if (this.verify) {
                    this.verify = false;
                    if (!k.equals("1") && !k.equals("2")) {
                        this.tell("기억 번호: " + this.stored.getOrDefault("B:" + k, "없음"));
                    } else {
                        this.test = true;
                        this.alarm = k.equals("1") ? "fire" : "gas";
                        this.alarmTime = 0;
                        this.tell("경보 시험");
                    }
                } else if (this.memory) {
                    if (this.editing.isEmpty()) {
                        this.editing = k;
                        this.digits = "";
                    } else if (remote && this.editing.equals("*") && k.equals("#") && this.digits.isEmpty()) {
                        this.editing = "radioPin";
                    } else if (k.matches("[0-9]") && this.digits.length() < (remote ? 16 : 18)) {
                        this.digits = this.digits + k;
                    }

                    if (Set.of("radioPin", "fixedPin").contains(this.editing) && this.digits.length() == 2) {
                        this.saveMemory(remote);
                    } else {
                        this.update();
                    }
                } else if (this.recall) {
                    this.recall = false;
                    this.dialNumber(this.stored.getOrDefault((remote ? "R:" : "B:") + k, ""));
                } else if (this.phone == 3) {
                    if (k.equals("*")) {
                        this.tone = true;
                    }
                } else {
                    if (this.audio || this.phone == 0 && this.status.startsWith("국선 선택")) {
                        if (k.equals("#")) {
                            this.dialNumber(this.digits);
                            this.digits = "";
                        } else if (k.matches("[0-9]") && this.digits.length() < 18) {
                            this.digits = this.digits + k;
                            this.phoneTime = -30;
                            this.update();
                        }
                    }
                }
            } else {
                this.openDoor();
            }
        }
    }

    public void press(String key, ServerPlayer p, boolean remote) {
        if (key.equals("portable") && !remote) {
            if (p.getInventory().getFreeSlot() < 0) {
                this.tell("인벤토리 공간이 필요합니다");
            } else {
                ItemStack item = new ItemStack(Product.PORTABLE102.get());
                item.getOrCreateTag().putBoolean("power", true);
                item.getTag().putInt("battery", 36000);
                p.getInventory().add(item);
                this.tell("휴대장치를 충전기에 놓고 홈링커로 삼성 내부기기와 연결하세요");
            }
        } else {
            if (this.portableRegistered && !this.memory && !this.verify) {
                if (!remote && key.equals("intercom")) {
                    this.requestInternal(false);
                    return;
                }

                if (this.transfer == 2 && (key.equals("talk") || !remote && key.equals("speaker"))) {
                    this.acceptInternal(false);
                    return;
                }

                if (this.portableActive
                    && (this.phone > 0 || this.callState != CallState.IDLE)
                    && Set.of("talk", "speaker", "door", "guard", "redial").contains(key)) {
                    this.tell("휴대장치 통화 중 · 내선으로 전환하세요");
                    return;
                }
            }

            this.pressCore(key, p, remote);
        }
    }

    void remotePress(String key, ServerPlayer p) {
        boolean before = this.portableCommand;
        this.portableCommand = true;

        try {
            this.remoteCore(key, p);
        } finally {
            this.portableCommand = before;
        }
    }

    void radioPress(String key, ServerPlayer p) {
        if (key.equals("talk")) {
            this.talk();
        } else if (key.equals("onhook")) {
            if (!this.audio && this.callState == CallState.IDLE && this.phone <= 0) {
                this.audio = true;
                this.tell("수화기 온후크 · 국선 번호 입력");
            } else {
                this.finish();
            }
        } else if (key.equals("door") && this.callState == CallState.CONNECTED && (this.incomingKey.equals("front_door") || this.incomingKey.equals("lobby"))) {
            if (!this.wired()) {
                this.openDoor();
            } else {
                this.tell("문열림은 통화 중 *");
            }
        } else {
            this.pressCore(key, p, true);
        }
    }

    void portableDigit(String key) {
        if (!key.equals("*")
            || !this.portableActive
            || !this.audio
            || this.hold
            || this.callState != CallState.CONNECTED
            || !this.incomingKey.equals("front_door") && !this.incomingKey.equals("lobby")) {
            this.digit(key, true);
        } else {
            this.openDoor();
        }
    }

    void portableSay(ServerPlayer p, String text) {
        boolean before = this.portableCommand;
        this.portableCommand = true;

        try {
            this.say(p, text, true);
        } finally {
            this.portableCommand = before;
        }
    }

    void requestInternal(boolean remote) {
        if (!this.portableRegistered) {
            this.tell("등록된 삼성 휴대장치가 없습니다");
        } else if (this.transfer != 0) {
            this.portableActive = this.transfer == 2;
            this.internal = this.transfer = 0;
            this.hold = false;
            this.audio = true;
            this.tell("통화전환 취소");
        } else if (this.internal == 3) {
            this.finish();
        } else if (this.phone != 3 && this.getCallState() != CallState.CONNECTED) {
            this.finish();
            this.portableActive = remote;
            this.internal = remote ? 2 : 1;
            this.transferTicks = 0;
            this.tell(remote ? "본체 내선 호출 · 본체 수화기/통화 버튼으로 응답" : "휴대장치 내선 호출 · 통화로 응답");
        } else {
            this.transfer = remote ? 2 : 1;
            this.internal = this.transfer;
            this.hold = this.phone == 3;
            this.audio = false;
            this.transferTicks = 0;
            this.tell(remote ? "본체로 통화전환 · 본체 수화기/통화 버튼으로 응답하세요" : "휴대장치로 통화전환 · 통화 버튼으로 응답");
        }
    }

    void acceptInternal(boolean remote) {
        if (this.transfer != 0) {
            this.internal = this.transfer = 0;
            this.hold = false;
            this.portableActive = remote;
            this.audio = true;
            this.tell(remote ? "휴대장치로 통화전환 완료" : "본체로 통화전환 완료");
        } else {
            this.internal = 3;
            this.audio = true;
            this.tell("본체 ↔ 삼성 휴대장치 내선 통화");
        }
    }

    void portablePickup(ServerPlayer p) {
        if (this.allowed(p, true) && Portable.carried(p, this)) {
            if (this.internal == 1) {
                this.acceptInternal(true);
            } else if (this.phone == 2 || this.getCallState() == CallState.RINGING) {
                this.portableActive = true;
                this.audio = false;
                this.remotePress("talk", p);
            }

            this.update();
        }
    }

    void portableReturn() {
        if (this.transfer == 2) {
            this.tell("휴대장치 거치 · 본체의 전환 응답 대기");
        } else {
            if (this.portableActive || this.internal == 3) {
                this.finish();
            }
        }
    }

    boolean portableRinging() {
        return this.phone == 2 || this.getCallState() == CallState.RINGING || this.internal == 1;
    }

    void remoteCore(String key, ServerPlayer p) {
        if (this.portableActive
            || this.transfer == 1
            || this.phone != 3 && this.getCallState() != CallState.CONNECTED && this.getCallState() != CallState.DIALING) {
            if (key.equals("intercom")) {
                this.requestInternal(true);
            } else if (key.equals("talk")) {
                if (this.internal == 1) {
                    this.acceptInternal(true);
                } else if ((this.phone == 3 || this.getCallState() == CallState.CONNECTED) && !this.portableActive) {
                    this.tell("본체 통화 중 · 본체 내선 버튼으로 전환하세요");
                } else {
                    this.portableActive = true;
                    this.talk();
                }
            } else if (key.equals("onhook")) {
                if (this.portableActive && this.audio) {
                    this.finish();
                } else if (this.phone <= 0 && this.getCallState() == CallState.IDLE) {
                    this.finish();
                    this.portableActive = true;
                    this.audio = true;
                    this.tell("휴대장치 온후크 · 국선 번호 입력");
                } else {
                    this.remotePress("talk", p);
                }
            } else if (key.equals("memory")) {
                this.pressCore(key, p, true);
            } else if (this.memory) {
                this.pressCore(key, p, true);
            } else if (key.equals("flash")) {
                if (this.phone == 3) {
                    this.hold = !this.hold;
                    this.audio = !this.hold;
                    this.tell(this.hold ? "휴대장치 국선 보류" : "휴대장치 국선 재통화");
                } else {
                    this.phoneTime = -60;
                    this.tell("포즈 · 번호 입력");
                }
            } else if (key.equals("redial")) {
                this.finish();
                this.portableActive = true;
                this.audio = true;
                if (!this.last.isEmpty()) {
                    this.dialNumber(this.last);
                } else {
                    this.tell("재다이얼 번호가 없습니다");
                }
            } else if (key.equals("channel")) {
                this.channel = this.channel % 16 + 1;
                this.tell("휴대장치 채널 " + this.channel);
            } else if (!key.equals("door")) {
                if (key.equals("guard")) {
                    this.portableActive = true;
                    this.pressCore(key, p, true);
                }
            } else if (this.getCallState() == CallState.CONNECTED && (this.incomingKey.equals("front_door") || this.incomingKey.equals("lobby"))) {
                if (!this.wired()) {
                    this.openDoor();
                } else {
                    this.tell("현관 영상 · 문열림은 통화 중 *");
                }
            } else {
                this.portableActive = true;
                this.pressCore(key, p, true);
            }
        } else {
            this.tell("본체 통화 중 · 본체 내선 버튼으로 전환하세요");
        }
    }

    void pressCore(String key, ServerPlayer p, boolean remote) {
        if (key.equals("handset")) {
            if (!remote) {
                this.replace(p);
            }
        } else if (key.equals("mode")) {
            if (!remote) {
                if (this.docked && this.callState == CallState.IDLE && this.phone <= 0) {
                    this.unlinkSwitchboard();
                    this.bus = !this.bus;
                    this.tell(this.bus ? "삼성 BUS" : "아날로그 STAR");
                } else {
                    this.tell("수화기 거치·통화 종료 후 변경하세요");
                }
            }
        } else if (key.equals("antenna")) {
            if (!remote) {
                this.folded = !this.folded;
                this.update();
            }
        } else if (key.equals("memory")) {
            if (this.memory) {
                this.saveMemory(remote);
            } else if (!this.audio && !this.status.startsWith("국선 선택")) {
                this.memory = true;
                this.editing = "";
                this.digits = "";
                this.tell("저장할 기능 또는 단축번호");
            } else {
                this.recall = true;
                this.tell("단축번호 입력");
            }
        } else if (this.memory) {
            if (key.equals("intercom") && this.paired()) {
                this.memory = false;
                this.tell("삐 · 암호 일치");
            } else {
                this.editing = switch (key) {
                    case "guard" -> "pin";
                    case "hold" -> "fixedPin";
                    case "speaker" -> "number";
                    case "redial" -> "tone";
                    default -> key;
                };
                this.digits = "";
                this.tell("번호 입력 후 메모리");
            }
        } else if (this.verify && !key.equals("restore")) {
            this.verify = false;
            this.tell(key.equals("guard") ? "등록 암호: " + this.pin : (key.equals("speaker") ? "자택 전화번호: " + this.number : "기억 확인"));
        } else {
            switch (key) {
                case "talk":
                    this.talk();
                    break;
                case "guard":
                    if (remote) {
                        this.audio = true;
                    }

                    this.callGuard(p, "");
                    break;
                case "door":
                    if ((this.callState != CallState.CONNECTED || !this.incomingKey.equals("lobby"))
                        && (this.callState != CallState.CONNECTED || !this.incomingKey.equals("front_door"))) {
                        super.hangUp();
                        this.endPhone();
                        this.internal = 0;
                        this.monitor = 600;
                        BlockPos at = this.cameraLink();
                        if (!(at != null && this.level.getBlockEntity(at) instanceof CallerBlockEntity c)) {
                            this.tell("연결된 도어카메라가 없습니다");
                        } else if (remote) {
                            c.callFromReceiver(this);
                            this.audio = true;
                            this.answer();
                        } else {
                            this.tell("세대 현관 영상 확인");
                        }
                    } else {
                        this.openDoor();
                    }
                    break;
                case "speaker":
                    if (this.internal == 2) {
                        this.internal = 3;
                        this.audio = true;
                        this.tell("본체 ↔ 수화기 내선 통화");
                    } else if (this.internal == 3) {
                        this.finish();
                    } else if (this.callState != CallState.IDLE || this.phone > 0) {
                        this.talk();
                    } else if (this.monitor > 0) {
                        BlockPos at = this.cameraLink();
                        if (at != null && this.level.getBlockEntity(at) instanceof CallerBlockEntity c) {
                            c.callFromReceiver(this);
                            this.audio = true;
                            this.answer();
                            this.tell("현관 스피커 통화");
                        } else {
                            this.tell("연결된 도어카메라가 없습니다");
                        }
                    } else if (this.audio) {
                        this.finish();
                    } else {
                        this.audio = true;
                        this.digits = "";
                        this.tell("국선 발신음 · 번호 입력");
                    }
                    break;
                case "intercom":
                    this.finish();
                    this.internal = remote ? 2 : 1;
                    this.tell(remote ? "본체 호출 · 스피커로 응답" : "무선수화기 호출 · 통화로 응답");
                    break;
                case "hold":
                    if (this.phone == 3) {
                        this.hold = true;
                        this.audio = false;
                        this.tell("국선 보류");
                    }
                    break;
                case "redial":
                    boolean speaking = this.audio;
                    this.finish();
                    this.audio = remote && speaking;
                    if (this.last.isEmpty() && remote) {
                        this.audio = true;
                        this.tell("국선 발신음 · 번호 입력");
                    } else {
                        this.dialNumber(this.last);
                    }
                    break;
                case "fire_call":
                case "police_call":
                case "medical_call":
                    this.finish();
                    this.dialNumber(this.stored.getOrDefault("B:" + key, ""));
                    break;
                case "channel":
                    this.channel = this.channel % 16 + 1;
                    this.tell("채널 " + this.channel);
                    break;
                case "security":
                    if (!this.armed && this.openDoors()) {
                        this.tell("문이 열려 있습니다");
                        return;
                    }

                    this.armed = !this.armed;
                    this.update();
                    break;
                case "away":
                    if (!this.away && this.openDoors()) {
                        this.tell("문이 열려 있습니다");
                        return;
                    }

                    this.away = !this.away;
                    if (!this.away) {
                        this.armed = false;
                        this.awayTime = 0;
                    } else {
                        this.awayTime = 1200;
                    }

                    GuardExchange g = this.switchboard();
                    if (g != null) {
                        g.setAway(this.worldPosition, this.getUnit(), this.away);
                    }

                    this.update();
                    break;
                case "test":
                    this.verify = true;
                    this.tell("시험 1: 화재 · 2: 가스 · 스피커/경비실/단축번호 확인");
                    break;
                case "stop":
                    this.mute = true;
                    this.resetTime = 6000;
                    this.reportStage = -1;
                    if (this.phone > 0) {
                        this.endPhone();
                    }

                    this.tell("경보정지 · 표시등 유지");
                    break;
                case "restore":
                    this.restore();
                    break;
                case "emergency":
                    this.trigger("emergency");
                    break;
                case "up":
                case "down":
                    int delta = key.equals("up") ? 1 : -1;
                    if (this.callState == CallState.IDLE && this.phone != 3 && this.internal != 3) {
                        this.ring = Math.max(0, Math.min(3, this.ring + delta));
                    } else {
                        this.volume = Math.max(0, Math.min(3, this.volume + delta));
                    }

                    this.update();
                    break;
                case "brightness":
                    this.brightness = (this.brightness + 1) % 5;
                    this.update();
                    break;
                case "contrast":
                    this.contrast = (this.contrast + 1) % 5;
                    this.update();
            }
        }
    }

    boolean openDoors() {
        for (BlockPos p : this.getReceiverDoors()) {
            if (this.level.isLoaded(p) && this.same(p)) {
                BlockState s = this.level.getBlockState(p);
                if (s.hasProperty(BlockStateProperties.OPEN) && s.getValue(BlockStateProperties.OPEN)) {
                    return true;
                }
            }
        }

        return false;
    }

    public void trigger(String type) {
        if (!this.mute) {
            this.alarm = type;
            this.alarmTime = 0;
            this.test = false;
            this.reportStage = -1;
            this.reportTimer = 0;
            this.tell(type + " 경보");
        }
    }

    void guardAlarm(boolean on) {
        GuardExchange g = this.switchboard();
        if (g != null) {
            g.setSafetyEvent(this.worldPosition, this.getUnit(), this.alarm, on);
        } else if (!this.bus && this.level instanceof ServerLevel sl) {
            for (BlockPos p : DeviceRegistry.get(sl).guards(sl, this.worldPosition, false)) {
                if (this.same(p) && sl.isLoaded(p) && sl.getBlockEntity(p) instanceof ReceiverBlockEntity r && r.guardSystem() == GuardSystem.STAR) {
                    if (r instanceof GuardExchange exchange) {
                        exchange.setSafetyEvent(this.worldPosition, this.getUnit(), this.alarm, on);
                    } else if (on) {
                        r.addAlert(this.getUnit() + " " + this.alarm + " 경보");
                    }
                }
            }
        }
    }

    void restore() {
        if (!this.test) {
            this.guardAlarm(false);
        }

        this.alarm = "";
        this.test = false;
        this.mute = false;
        this.verify = false;
        this.resetTime = 0;
        this.reportStage = -1;
        this.tell("복귀 · 대기");
    }

    @Override
    protected void tickExtra() {
        if (this.portableRegistered && (this.internal == 1 || this.internal == 2)) {
            if (++this.transferTicks >= 600) {
                if (this.transfer != 0) {
                    this.portableActive = this.transfer == 2;
                    this.hold = false;
                    this.audio = true;
                    this.internal = this.transfer = 0;
                    this.tell("통화전환 응답 없음 · 원래 통화로 복귀");
                } else {
                    this.finish();
                }

                this.transferTicks = 0;
            }
        } else {
            this.transferTicks = 0;
        }

        if ((this.phone == 2 || this.internal == 1 || this.internal == 2) && this.clock % 40 == 0) {
            CallSounds.note(this.level, this.worldPosition, SoundEvents.NOTE_BLOCK_BELL.value(), this.ring * 0.25F, 1.0F);
        }

        if (++this.clock % 20 == 0) {
            if (!this.wired() && !this.docked && this.level instanceof ServerLevel sl) {
                for (ServerPlayer p : sl.players()) {
                    for (ItemStack s : p.getInventory().items) {
                        if (this.ownsRadio(s) && s.getTag().getBoolean("power")) {
                            int battery = s.getTag().getInt("battery");
                            s.getTag().putInt("battery", Math.max(0, battery - 20));
                            if (battery <= 20 && this.audio) {
                                this.finish();
                            }
                        }
                    }
                }
            }

            this.syncScreens();
        }

        if (this.monitor > 0 && --this.monitor == 0) {
            this.update();
        }

        if (this.awayTime > 0 && --this.awayTime == 0) {
            this.armed = true;
            this.update();
        }

        if (this.resetTime > 0 && --this.resetTime == 0) {
            this.restore();
        }

        if (this.callState == CallState.RINGING && this.incomingKey.equals("front_door") && this.callTicks >= 300) {
            super.hangUp();
            this.update();
        }

        if (this.phone == 0
            && this.digits.length() > 0
            && !this.memory
            && !this.recall
            && (this.audio || this.status.startsWith("국선 선택"))
            && ++this.phoneTime == 0) {
            String n = this.digits;
            this.digits = "";
            this.dialNumber(n);
        }

        if (this.phone > 0 && ++this.phoneTime > 2400) {
            this.endPhone();
            this.audio = false;
            this.update();
        }

        for (Direction d : Direction.values()) {
            boolean on = this.level.getSignal(this.worldPosition.relative(d), d) > 0;
            String event = d == Direction.DOWN ? "fire" : (d == Direction.UP ? "emergency" : (d == Direction.EAST ? "gas" : "security"));
            String edge = d.name();
            if (!on || this.edges.contains(edge) || event.equals("security") && !this.armed) {
                if (!on) {
                    this.edges.remove(edge);
                }
            } else {
                this.trigger(event);
                this.edges.add(edge);
            }
        }

        if (!this.alarm.isEmpty() && !this.mute) {
            this.alarmTime++;
            if (this.alarmTime % 40 == 1) {
                CallSounds.note(this.level, this.worldPosition, SoundEvents.NOTE_BLOCK_BELL.value(), this.ring * 0.25F, this.alarm.equals("gas") ? 0.7F : 1.2F);
            }

            if (!this.test && this.alarmTime == 600) {
                this.guardAlarm(true);
                this.reportStage = 0;
                this.reportTimer = 0;
                this.reportAudio = 0;
            }

            if (!this.test && this.reportStage >= 0) {
                if (++this.reportTimer == 1) {
                    String n = this.stored.getOrDefault("B:" + (this.reportStage % 2 == 0 ? "*" : "#"), "");
                    if (!n.isEmpty()) {
                        this.dialNumber(n);
                    }
                }

                Station peer = this.phonePeer();
                if (this.phone == 3 && peer != null) {
                    this.reportAudio++;
                    if (this.reportAudio == 1 || this.reportAudio == 241) {
                        peer.addLine(new IntercomLine("자동통보", "SHT-101C", this.alarm + " 경보"));
                        peer.update();
                    }

                    if (this.reportAudio == 201) {
                        peer.addLine(new IntercomLine("자동통보", "SHT-101C", "발생지 " + this.number));
                        peer.update();
                    }
                }

                if (this.reportTimer >= 600 || this.reportAudio >= 441) {
                    this.endPhone();
                    this.reportTimer = 0;
                    this.reportAudio = 0;
                    if (++this.reportStage >= 6) {
                        this.reportStage = -1;
                    }
                }
            }
        }

        if (this.lastVisualLights != this.lights()) {
            this.update();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag t) {
        super.saveAdditional(t);
        t.putBoolean("PortableRegistered", this.portableRegistered);
        t.putUUID("PortableTicket", this.portableTicket);
        t.putUUID("Serial", this.serial);
        t.putUUID("Ticket", this.ticket);
        t.putBoolean("Docked", this.docked);
        t.putBoolean("Bus", this.bus);
        t.putBoolean("Folded", this.folded);
        t.putBoolean("Armed", this.armed);
        t.putBoolean("Away", this.away);
        t.putString("GuardNumber", this.guardNumber);
        t.putString("Pin", this.pin);
        t.putString("RadioPin", this.radioPin);
        t.putString("FixedPin", this.fixedPin);
        t.putString("Number", this.number);
        t.putString("Last", this.last);
        t.putInt("Channel", this.channel);
        t.putInt("Ring", this.ring);
        t.putInt("Volume", this.volume);
        t.putInt("Brightness", this.brightness);
        t.putInt("Contrast", this.contrast);
        CompoundTag m = new CompoundTag();
        this.stored.forEach(m::putString);
        t.put("Memory", m);
    }

    @Override
    public void load(CompoundTag t) {
        super.load(t);
        if (t.hasUUID("Serial")) {
            this.serial = t.getUUID("Serial");
        }

        if (t.hasUUID("Ticket")) {
            this.ticket = t.getUUID("Ticket");
        }

        this.portableRegistered = t.getBoolean("PortableRegistered");
        this.portableTicket = t.hasUUID("PortableTicket") ? t.getUUID("PortableTicket") : this.ticket;
        this.docked = !t.contains("Docked") || t.getBoolean("Docked");
        this.bus = !t.contains("Bus") || t.getBoolean("Bus");
        this.folded = !t.contains("Folded") || t.getBoolean("Folded");
        this.armed = t.getBoolean("Armed");
        this.away = t.getBoolean("Away");
        this.guardNumber = t.getString("GuardNumber");
        this.pin = t.contains("Pin") ? t.getString("Pin") : "00";
        this.radioPin = t.contains("RadioPin") ? t.getString("RadioPin") : "00";
        this.fixedPin = t.contains("FixedPin") ? t.getString("FixedPin") : "00";
        this.number = t.getString("Number");
        this.last = t.getString("Last");
        this.channel = t.contains("Channel") ? Math.max(1, Math.min(16, t.getInt("Channel"))) : 1;
        this.ring = t.contains("Ring") ? Math.min(3, Math.max(0, t.getInt("Ring"))) : 2;
        this.volume = t.contains("Volume") ? Math.min(3, Math.max(0, t.getInt("Volume"))) : 2;
        this.brightness = t.contains("Brightness") ? Math.min(4, Math.max(0, t.getInt("Brightness"))) : 2;
        this.contrast = t.contains("Contrast") ? Math.min(4, Math.max(0, t.getInt("Contrast"))) : 2;
        this.stored.clear();
        CompoundTag m = t.getCompound("Memory");
        m.getAllKeys().forEach(k -> this.stored.put(k, m.getString(k)));
        this.clientVideo = t.contains("Video") ? BlockPos.of(t.getLong("Video")) : null;
        this.clientLights = t.getInt("Lights");
        if (this.level != null && this.level.isClientSide) {
            this.alarm = t.getString("RenderAlarm");
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag t = super.getUpdateTag();
        BlockPos c = this.camera();
        if (c != null) {
            t.putLong("Video", c.asLong());
        }

        t.putInt("Lights", this.lights());
        t.putString("RenderAlarm", this.alarm);
        return t;
    }
}
