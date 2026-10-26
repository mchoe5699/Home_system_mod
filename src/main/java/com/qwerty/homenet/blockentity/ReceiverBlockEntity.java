package com.qwerty.homenet.blockentity;

import com.qwerty.homenet.analog.AnalogBus;
import com.qwerty.homenet.analog.ReceiverDoors;
import com.qwerty.homenet.block.ReceiverBlock;
import com.qwerty.homenet.block.WallpadBlock;
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
import com.qwerty.homenet.intercom.IntercomLine;
import com.qwerty.homenet.intercom.MissedCall;
import com.qwerty.homenet.intercom.UnitAddress;
import com.qwerty.homenet.logo.ProductLogo;
import com.qwerty.homenet.network.DeviceEntry;
import com.qwerty.homenet.network.ModNetwork;
import com.qwerty.homenet.network.WallpadDataPacket;
import com.qwerty.homenet.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ReceiverBlockEntity extends BlockEntity implements IntercomCaller {
    public static final int MAX_UNIT = 16;
    public static final int MAX_MISSED = 6;
    public static final int MAX_LOG = 8;
    public static final int RING_TIMEOUT = 700;
    public static final int DIAL_TIMEOUT = 600;
    public static final int TALK_TIMEOUT = 3600;
    public final ProductLogo productLogo = new ProductLogo();
    protected String unit = "";
    protected final ReceiverDoors receiverDoors = new ReceiverDoors();
    @Nullable
    protected BlockPos analogStation;
    protected String doorPassword = "";
    protected final List<MissedCall> missed = new ArrayList<>();
    protected CallState callState = CallState.IDLE;
    @Nullable
    protected BlockPos caller;
    protected String incomingKey = "lobby";
    protected int callTicks;
    protected final List<IntercomLine> log = new ArrayList<>();
    protected boolean outgoing;
    protected String peerLabel = "";
    protected final List<BlockPos> outRinging = new ArrayList<>();
    @Nullable
    protected BlockPos outPeer;
    protected String pendingNotice = "";

    public void openLogoEditor(ServerPlayer p) {
        ModNetwork.sendTo(p, this.buildPacket(true).withNotice("logo_editor"));
    }

    public boolean editLogo(String text) {
        int sep = text.indexOf(61);
        if (sep >= 0 && this.productLogo.set(text.substring(0, sep), text.substring(sep + 1))) {
            this.setChanged();
            if (this.level != null) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            }

            this.syncScreens();
            return true;
        } else {
            return false;
        }
    }

    public List<BlockPos> getReceiverDoors() {
        return this.receiverDoors.positions();
    }

    public String toggleReceiverDoor(BlockPos pos) {
        String result = this.receiverDoors.toggle(this.level, this.worldPosition, pos);
        this.setChanged();
        this.syncScreens();
        return result;
    }

    public void clearReceiverDoors() {
        this.receiverDoors.clear(this.level);
        this.setChanged();
        this.syncScreens();
    }

    public AnalogBus analogBus() {
        return AnalogBus.DEDICATED;
    }

    public GuardSystem guardSystem() {
        return GuardSystem.STAR;
    }

    public String manufacturer() {
        return "";
    }

    public boolean acceptSwitchboardCall(BlockPos station) {
        return false;
    }

    public boolean canSetUnitLocally() {
        return true;
    }

    public boolean supportsDoorCommunication() {
        return true;
    }

    public boolean supportsSwitchboard(GuardExchange board) {
        return this.guardSystem().compatible(this.manufacturer(), board.guardSystem(), board.manufacturer());
    }

    @Nullable
    public BlockPos getAnalogStation() {
        return this.analogStation;
    }

    @Nullable
    protected GuardExchange switchboard() {
        if (this.level != null
            && this.analogStation != null
            && this.level.isLoaded(this.analogStation)
            && ZoneData.connected(this.level, this.worldPosition, this.analogStation)) {
            return this.level.getBlockEntity(this.analogStation) instanceof GuardExchange board && this.supportsSwitchboard(board) ? board : null;
        } else {
            return null;
        }
    }

    public void unlinkSwitchboard() {
        GuardExchange board = this.switchboard();
        if (board != null) {
            board.onUnlinked(this.worldPosition);
        }

        this.analogStation = null;
        this.setChanged();
    }

    public boolean bindSwitchboard(BlockPos pos) {
        if (!(this.level instanceof ServerLevel)
            || !ZoneData.connected(this.level, this.worldPosition, pos)
            || !this.level.isLoaded(pos)
            || !(this.level.getBlockEntity(pos) instanceof GuardExchange board && this.supportsSwitchboard(board))) {
            return false;
        } else if (board instanceof Am10kpBlockEntity am && !am.canLink(this.worldPosition)) {
            return false;
        } else {
            String linkedUnit = this.canSetUnitLocally()
                ? this.unit
                : (
                    pos.equals(this.analogStation) && !board.assignedUnit(this.worldPosition).isBlank()
                        ? board.assignedUnit(this.worldPosition)
                        : GuardUnitAssignments.get((ServerLevel)this.level).allocate((ServerLevel)this.level, this.worldPosition, this.roomOnlyUnit())
                );
            this.unlinkSwitchboard();
            this.analogStation = pos.immutable();
            this.unit = linkedUnit;
            board.onLinked(this.worldPosition, linkedUnit);
            this.unit = linkedUnit;
            this.register();
            this.setChanged();
            this.syncScreens();
            return true;
        }
    }

    public boolean roomOnlyUnit() {
        return false;
    }

    public boolean validAssignedUnit(String value) {
        return this.roomOnlyUnit() ? UnitAddress.component(value) : UnitAddress.full(value);
    }

    public boolean assignUnitFromSwitchboard(BlockPos station, String raw) {
        if (this.analogStation != null && this.analogStation.equals(station) && this.switchboard() != null) {
            String address = UnitAddress.canonical(raw);
            if (!this.validAssignedUnit(address)) {
                return false;
            } else {
                this.unit = address;
                this.register();
                this.setChanged();
                this.syncScreens();
                return true;
            }
        } else {
            return false;
        }
    }

    public boolean setDoorPasswordFromLobby(BlockPos source, String value) {
        if (this.level != null
            && this.level.getBlockEntity(source) instanceof Klp203rfBlockEntity panel
            && panel.authorizesPasswordChange(this)
            && (value.isEmpty() || value.matches("[0-9]{4}"))) {
            this.doorPassword = value;
            this.setChanged();
            this.syncScreens();
            return true;
        } else {
            return false;
        }
    }

    public boolean assignDoorPasswordFromSwitchboard(BlockPos station, String value) {
        if (station.equals(this.analogStation) && this.switchboard() != null && value.matches("[0-9]{4,8}")) {
            this.doorPassword = value;
            this.setChanged();
            this.syncScreens();
            return true;
        } else {
            return false;
        }
    }

    public boolean hasLinkedCamera(BlockPos pos) {
        return this.level != null
            && ZoneData.connected(this.level, this.worldPosition, pos)
            && this.level.isLoaded(pos)
            && this.level.getBlockEntity(pos) instanceof DoorStationBlockEntity door
            && door.hasReceiver(this.worldPosition);
    }

    @Nullable
    public BlockPos linkedCamera() {
        return this.level instanceof ServerLevel sl
            ? DeviceRegistry.get(sl)
                .all()
                .stream()
                .filter(e -> e.kind() == DeviceRegistry.Kind.DOOR_CAMERA && this.hasLinkedCamera(e.pos()))
                .map(DeviceRegistry.Entry::pos)
                .min(Comparator.comparingDouble(p -> p.distSqr(this.worldPosition)))
                .orElse(null)
            : null;
    }

    public ReceiverBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.RECEIVER.get(), pos, state);
    }

    protected ReceiverBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        if (this.kind() == DeviceRegistry.Kind.GUARD_CONSOLE) {
            this.unit = "경비실";
        }
    }

    public DeviceRegistry.Kind kind() {
        return this.getBlockState().getBlock() instanceof ReceiverBlock rb ? rb.kind() : DeviceRegistry.Kind.WALLPAD;
    }

    public String getUnit() {
        return this.unit;
    }

    public boolean hasDoorPassword() {
        return !this.doorPassword.isEmpty();
    }

    public String getDoorPassword() {
        return this.doorPassword;
    }

    public boolean checkDoorPassword(String input) {
        return !this.doorPassword.isEmpty() && this.doorPassword.equals(input);
    }

    public void enforceScope() {
        if (this.level instanceof ServerLevel
            && (
                this.caller != null && !ZoneData.connected(this.level, this.worldPosition, this.caller)
                    || this.outPeer != null && !ZoneData.connected(this.level, this.worldPosition, this.outPeer)
            )) {
            this.hangUp();
        }
    }

    public boolean isBusy() {
        return this.callState != CallState.IDLE;
    }

    public CallState getCallState() {
        return this.callState;
    }

    @Nullable
    public BlockPos getCaller() {
        return this.outgoing ? null : this.caller;
    }

    public boolean hasCallerSession(BlockPos from) {
        return from.equals(this.getCaller());
    }

    public CallState callerSessionState(BlockPos from) {
        return from.equals(this.getCaller()) ? this.callState : CallState.IDLE;
    }

    public void endCallerSession(BlockPos from, boolean missed) {
        if (this.hasCallerSession(from)) {
            if (this.callState == CallState.RINGING) {
                this.stopRinging(missed);
            } else {
                this.endCall();
            }
        }
    }

    protected void register() {
        if (this.level instanceof ServerLevel sl) {
            DeviceRegistry.get(sl).register(this.worldPosition, this.kind(), this.unit);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        this.register();
    }

    public void setUnit(ServerPlayer player, String raw) {
        if (this.canSetUnitLocally()) {
            String u = this.kind() == DeviceRegistry.Kind.GUARD_CONSOLE ? Intercom.sanitize(raw, 16) : UnitAddress.canonical(raw);
            if (this.kind() != DeviceRegistry.Kind.GUARD_CONSOLE && raw != null && !raw.isBlank() && u.isEmpty()) {
                player.displayClientMessage(Component.literal("동·호수는 각각 1~9999까지 입력하세요."), true);
            } else {
                this.unit = u;
                this.setChanged();
                this.register();
                player.displayClientMessage(Component.translatable("msg.qwertys_homenet.unit_set", this.unit.isEmpty() ? "-" : this.unit), true);
                this.syncScreens();
            }
        }
    }

    public void openFor(ServerPlayer player) {
        ModNetwork.sendTo(player, this.buildPacket(true));
    }

    public void noticeTo(ServerPlayer player, String notice) {
        ModNetwork.sendTo(player, this.buildPacket(false).withNotice(notice));
    }

    @Override
    public void syncScreens() {
        if (this.level instanceof ServerLevel sl) {
            WallpadDataPacket var5 = this.buildPacket(false);
            if (!this.pendingNotice.isEmpty()) {
                var5 = var5.withNotice(this.pendingNotice);
                this.pendingNotice = "";
            }

            for (ServerPlayer p : Intercom.playersNear(sl, this.worldPosition, 8.0)) {
                ModNetwork.sendTo(p, var5);
            }
        }
    }

    protected List<DeviceEntry> deviceEntries() {
        return new ArrayList<>();
    }

    protected List<MissedCall> visitors() {
        return new ArrayList<>();
    }

    protected Map<String, String> settingsForClient() {
        return new HashMap<>();
    }

    protected long[] energyForClient() {
        return new long[0];
    }

    protected List<MissedCall> records() {
        return new ArrayList<>();
    }

    protected WallpadDataPacket buildPacket(boolean open) {
        Map<String, String> clientSettings = new HashMap<>(this.settingsForClient());
        this.productLogo.settings(clientSettings);
        return new WallpadDataPacket(
            this.worldPosition,
            open,
            this.kind().ordinal(),
            this.unit,
            this.hasDoorPassword(),
            this.callState.ordinal(),
            this.incomingKey,
            this.outgoing,
            this.peerLabel,
            "",
            this.deviceEntries(),
            new ArrayList<>(this.missed),
            new ArrayList<>(this.log),
            this.visitors(),
            clientSettings,
            this.energyForClient(),
            this.records()
        );
    }

    public void handleAction(ServerPlayer player, ReceiverBlockEntity.Action action, BlockPos target, String text) {
        switch (action) {
            case SET_UNIT:
                this.setUnit(player, text);
                break;
            case ANSWER:
                this.answer();
                break;
            case OPEN_DOOR:
                this.openDoor();
                break;
            case HANG_UP:
                this.hangUp();
                break;
            case DIAL:
                this.dial(player, text);
                break;
            case CALL_GUARD:
                this.callGuard(player, text != null && !text.isEmpty() ? text : "guard");
                break;
            case CALL_LOBBY:
                this.callLobby(player, text == null ? "" : text);
                break;
            case SEND_MESSAGE:
                this.sendMessage(player, text);
                break;
            case SET_DOOR_PASSWORD:
                this.setDoorPassword(player, text);
                break;
            case CLEAR_MISSED:
                this.missed.clear();
                this.setChanged();
                break;
            default:
                this.handleExtra(player, action, target, text);
        }

        this.syncScreens();
    }

    protected void setDoorPassword(ServerPlayer player, String text) {
        String pw = text == null ? "" : text.trim();
        if (!pw.isEmpty() && !pw.matches("\\d{4}")) {
            player.displayClientMessage(Component.translatable("msg.qwertys_homenet.door_pw_invalid").withStyle(ChatFormatting.RED), true);
        } else {
            this.doorPassword = pw;
            this.setChanged();
            player.displayClientMessage(Component.translatable(pw.isEmpty() ? "msg.qwertys_homenet.door_pw_cleared" : "msg.qwertys_homenet.door_pw_set"), true);
        }
    }

    protected void handleExtra(ServerPlayer player, ReceiverBlockEntity.Action action, BlockPos target, String text) {
    }

    public List<IntercomLine> conversation() {
        return List.copyOf(this.log);
    }

    protected void sendMessage(ServerPlayer player, String text) {
        if (this.callState == CallState.CONNECTED && this.level instanceof ServerLevel sl) {
            if (this.outgoing) {
                ReceiverBlockEntity peer = this.receiverAt(this.outPeer);
                if (peer != null) {
                    Intercom.say(sl, peer, this, player, this.callerKey(), text);
                }
            } else {
                IntercomCaller c = this.callerBe();
                if (c == null) {
                    return;
                }

                String side = Intercom.isReceiverKey(this.incomingKey) ? this.callerKey() : "unit";
                Intercom.say(sl, this, c, player, side, text);
            }
        }
    }

    @Nullable
    protected IntercomCaller callerBe() {
        if (!this.outgoing
            && this.caller != null
            && this.level != null
            && this.level.isLoaded(this.caller)
            && ZoneData.connected(this.level, this.worldPosition, this.caller)) {
            return this.level.getBlockEntity(this.caller) instanceof IntercomCaller c ? c : null;
        } else {
            return null;
        }
    }

    @Nullable
    protected ReceiverBlockEntity receiverAt(@Nullable BlockPos pos) {
        if (pos != null && this.level != null && this.level.isLoaded(pos)) {
            return ZoneData.connected(this.level, this.worldPosition, pos) && this.level.getBlockEntity(pos) instanceof ReceiverBlockEntity r ? r : null;
        } else {
            return null;
        }
    }

    @Nullable
    public BlockPos ring(BlockPos from, String key) {
        return this.ring(from, key, 0);
    }

    @Nullable
    protected BlockPos ring(BlockPos from, String key, int depth) {
        return this.startRinging(from, key) ? this.worldPosition : null;
    }

    public boolean startRinging(BlockPos from, String key) {
        if (!this.isBusy() && this.level instanceof ServerLevel sl && ZoneData.connected(this.level, this.worldPosition, from)) {
            this.callState = CallState.RINGING;
            this.outgoing = false;
            this.caller = from.immutable();
            this.incomingKey = key;
            this.callTicks = 0;
            this.log.clear();
            this.setRingingState(true);
            this.onIncoming(key);
            Component note = Component.translatable("msg.qwertys_homenet.incoming", Intercom.sideName(key), this.unit.isEmpty() ? "-" : this.unit)
                .withStyle(ChatFormatting.YELLOW);

            for (ServerPlayer p : Intercom.playersNear(sl, this.worldPosition, 24.0)) {
                p.displayClientMessage(note, true);
            }

            this.syncScreens();
            return true;
        } else {
            return false;
        }
    }

    protected void onIncoming(String key) {
    }

    public void answer() {
        if (this.callState == CallState.RINGING) {
            IntercomCaller c = this.callerBe();
            if (c != null && c.onAnswered(this.worldPosition)) {
                this.callState = CallState.CONNECTED;
                this.callTicks = 0;
                this.setRingingState(false);
            } else {
                this.resetCall();
            }
        }
    }

    public void openDoor() {
        this.receiverDoors.open(this.level, this.worldPosition);
        if (this.callState != CallState.IDLE && !this.outgoing) {
            IntercomCaller c = this.callerBe();
            this.resetCall();
            if (c != null) {
                c.onDoorOpened(this.worldPosition);
            }

            if (this.level != null) {
                this.level.playSound(null, this.worldPosition, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.6F, 1.6F);
            }
        }
    }

    public void hangUp() {
        if (this.callState != CallState.IDLE) {
            if (this.outgoing) {
                if (this.callState == CallState.DIALING) {
                    this.stopOutRinging(true);
                } else {
                    ReceiverBlockEntity peer = this.receiverAt(this.outPeer);
                    if (peer != null && peer.callerSessionState(this.worldPosition) != CallState.IDLE) {
                        peer.endCallerSession(this.worldPosition, false);
                    }
                }

                this.resetCall();
            } else {
                DoorStatus reason = this.callState == CallState.RINGING ? DoorStatus.REJECTED : DoorStatus.ENDED;
                IntercomCaller c = this.callerBe();
                this.resetCall();
                if (c != null) {
                    c.onReceiverHangUp(this.worldPosition, reason);
                }
            }
        }
    }

    public void stopRinging(boolean addMissed) {
        if (this.callState == CallState.RINGING) {
            if (addMissed) {
                this.addMissed();
            }

            this.resetCall();
            this.syncScreens();
        }
    }

    public void endCall() {
        if (this.callState != CallState.IDLE && !this.outgoing) {
            this.resetCall();
            this.syncScreens();
        }
    }

    protected void addMissed() {
        if (this.level != null) {
            this.missed.add(0, new MissedCall(this.incomingKey, this.level.getDayTime()));

            while (this.missed.size() > 6) {
                this.missed.remove(this.missed.size() - 1);
            }

            this.setChanged();
        }
    }

    public boolean bridgeThroughGuard(BlockPos station, ReceiverBlockEntity peer) {
        if (this.level != null
            && !this.level.isClientSide
            && peer != this
            && ZoneData.connected(this.level, this.worldPosition, peer.worldPosition)
            && ZoneData.connected(this.level, this.worldPosition, station)
            && station.equals(this.analogStation)
            && station.equals(peer.analogStation)
            && this.callState == CallState.CONNECTED
            && peer.callState == CallState.CONNECTED
            && station.equals(this.getCaller())
            && station.equals(peer.getCaller())) {
            this.caller = null;
            this.outgoing = true;
            this.outPeer = peer.worldPosition;
            this.outRinging.clear();
            this.peerLabel = peer.unit;
            this.callTicks = 0;
            peer.caller = this.worldPosition;
            peer.incomingKey = "unit:" + this.unit;
            peer.outgoing = false;
            peer.callTicks = 0;
            peer.outPeer = null;
            this.setChanged();
            peer.setChanged();
            this.syncScreens();
            peer.syncScreens();
            return true;
        } else {
            return false;
        }
    }

    public void addAlert(String fromUnit) {
        if (this.level instanceof ServerLevel sl) {
            this.missed.add(0, new MissedCall("emergency:" + fromUnit, this.level.getDayTime()));

            while (this.missed.size() > 6) {
                this.missed.remove(this.missed.size() - 1);
            }

            this.setChanged();
            Component msg = Component.translatable("msg.qwertys_homenet.emergency_alert", fromUnit.isEmpty() ? "-" : fromUnit)
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD);

            for (ServerPlayer p : Intercom.playersNear(sl, this.worldPosition, 24.0)) {
                p.sendSystemMessage(msg);
            }

            this.level.playSound(null, this.worldPosition, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 1.0F, 0.7F);
            this.syncScreens();
        }
    }

    protected void resetCall() {
        this.callState = CallState.IDLE;
        this.caller = null;
        this.callTicks = 0;
        this.outgoing = false;
        this.outPeer = null;
        this.outRinging.clear();
        this.peerLabel = "";
        this.setRingingState(false);
    }

    @Override
    public void addLine(IntercomLine line) {
        this.log.add(line);

        while (this.log.size() > 8) {
            this.log.remove(0);
        }
    }

    protected void setRingingState(boolean ringing) {
        if (this.level != null) {
            BlockState s = this.getBlockState();
            if (s.hasProperty(WallpadBlock.RINGING) && s.getValue(WallpadBlock.RINGING) != ringing) {
                this.level.setBlock(this.worldPosition, s.setValue(WallpadBlock.RINGING, ringing), 2);
            }
        }
    }

    @Override
    public String callerKey() {
        return this.kind() == DeviceRegistry.Kind.GUARD_CONSOLE ? "guard:" + this.unit : "unit:" + this.unit;
    }

    public void dial(ServerPlayer player, String raw) {
        String target = Intercom.sanitize(raw, 16);
        if (this.level instanceof ServerLevel sl) {
            if (this.isBusy()) {
                this.noticeTo(player, "busy_self");
            } else {
                String key = DeviceRegistry.normalize(target);
                if (UnitAddress.valid(target)
                    && !key.isEmpty()
                    && (this.unit.isEmpty() || !key.equals(DeviceRegistry.normalize(this.unit)) || this.kind() != DeviceRegistry.Kind.WALLPAD)) {
                    this.startOutgoing(player, DeviceRegistry.get(sl).receivers(sl, this.worldPosition, target), target);
                } else {
                    this.noticeTo(player, "bad_unit");
                }
            }
        }
    }

    public void callGuard(ServerPlayer player, String mode) {
        if (this.analogStation != null) {
            if (this.switchboard() != null) {
                this.switchboard().callFrom(this, player);
            } else {
                player.displayClientMessage(Component.literal("연결된 경비실기를 사용할 수 없습니다."), true);
            }
        } else if (this.level instanceof ServerLevel sl) {
            if (this.isBusy()) {
                this.noticeTo(player, "busy_self");
            } else {
                DeviceRegistry reg = DeviceRegistry.get(sl);
                if (mode.startsWith("no:")) {
                    String no = mode.substring(3).replaceAll("[^0-9]", "");
                    List<BlockPos> list = no.isEmpty() ? reg.guards(sl, this.worldPosition, false) : reg.guardsByNumber(sl, this.worldPosition, no);
                    if (list.isEmpty()) {
                        this.noticeTo(player, "no_guard");
                    } else {
                        this.startOutgoing(player, list, no.isEmpty() ? "#guard" : "#guard:" + no);
                    }
                } else {
                    boolean office = "office".equals(mode);
                    String pref = office ? "" : this.preferredGuard();
                    if (!pref.isEmpty()) {
                        List<BlockPos> list = reg.guardsByNumber(sl, this.worldPosition, pref);
                        if (!list.isEmpty() && this.tryOutgoing(null, list, "#guard:" + pref)) {
                            return;
                        }
                    }

                    this.startOutgoing(player, reg.guards(sl, this.worldPosition, office), office ? "#office" : "#guard");
                }
            }
        }
    }

    protected String preferredGuard() {
        return "";
    }

    protected void startOutgoing(ServerPlayer player, List<BlockPos> registered, String label) {
        this.tryOutgoing(player, registered, label);
    }

    protected boolean tryOutgoing(@Nullable ServerPlayer player, List<BlockPos> registered, String label) {
        List<BlockPos> var9 = new ArrayList<>(registered);
        var9.removeIf(px -> px.equals(this.worldPosition));
        if (var9.isEmpty()) {
            if (player != null) {
                this.noticeTo(player, "no_unit");
            }

            return false;
        } else {
            List<ReceiverBlockEntity> loaded = new ArrayList<>();

            for (BlockPos p : var9) {
                ReceiverBlockEntity r = this.receiverAt(p);
                if (r != null) {
                    loaded.add(r);
                }
            }

            if (loaded.isEmpty()) {
                if (player != null) {
                    this.noticeTo(player, "no_signal");
                }

                return false;
            } else {
                this.outRinging.clear();
                String key = this.callerKey();

                for (ReceiverBlockEntity r : loaded) {
                    BlockPos at = r.ring(this.worldPosition, key);
                    if (at != null && !at.equals(this.worldPosition) && !this.outRinging.contains(at)) {
                        this.outRinging.add(at);
                    }
                }

                if (this.outRinging.isEmpty()) {
                    if (player != null) {
                        this.noticeTo(player, "busy");
                    }

                    return false;
                } else {
                    this.callState = CallState.DIALING;
                    this.outgoing = true;
                    this.outPeer = null;
                    this.peerLabel = label;
                    this.caller = null;
                    this.callTicks = 0;
                    this.log.clear();
                    this.setChanged();
                    this.onCallStarted(label);
                    return true;
                }
            }
        }
    }

    protected void onCallStarted(String label) {
    }

    public void callLobby(ServerPlayer player, String number) {
        if (this.level instanceof ServerLevel sl) {
            if (this.isBusy()) {
                this.noticeTo(player, "busy_self");
            } else {
                boolean found = false;

                for (BlockPos p : DeviceRegistry.get(sl).lobbies(sl, this.worldPosition)) {
                    if (sl.isLoaded(p) && sl.getBlockEntity(p) instanceof LobbyPhoneBlockEntity lp && lp.matchesNumber(number)) {
                        found = true;
                        if (lp.acceptGuardCall(this.worldPosition)) {
                            this.callState = CallState.CONNECTED;
                            this.outgoing = false;
                            this.caller = p.immutable();
                            this.incomingKey = "lobby";
                            this.callTicks = 0;
                            this.log.clear();
                            this.setRingingState(false);
                            this.onCallStarted("#lobby:" + number);
                            this.syncScreens();
                            return;
                        }
                    }
                }

                this.noticeTo(player, found ? "busy" : "no_lobby");
            }
        }
    }

    protected void stopOutRinging(boolean missedCall) {
        for (BlockPos p : this.outRinging) {
            ReceiverBlockEntity r = this.receiverAt(p);
            if (r != null && this.worldPosition.equals(r.getCaller())) {
                r.stopRinging(missedCall);
            }
        }

        this.outRinging.clear();
    }

    @Override
    public boolean onAnswered(BlockPos receiver) {
        if (ZoneData.connected(this.level, this.worldPosition, receiver)
            && this.outgoing
            && this.callState == CallState.DIALING
            && this.outRinging.contains(receiver)) {
            this.outRinging.remove(receiver);
            this.stopOutRinging(false);
            this.outPeer = receiver.immutable();
            this.callState = CallState.CONNECTED;
            this.callTicks = 0;
            this.syncScreens();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onDoorOpened(BlockPos receiver) {
        this.onReceiverHangUp(receiver, DoorStatus.ENDED);
    }

    @Override
    public void onReceiverHangUp(BlockPos receiver, DoorStatus reason) {
        if (this.outgoing) {
            if (receiver.equals(this.outPeer)) {
                this.resetCall();
                this.pendingNotice = reason == DoorStatus.TIMEOUT ? "timeout" : "ended";
            } else if (this.outRinging.remove(receiver)) {
                if (reason == DoorStatus.REJECTED) {
                    this.stopOutRinging(false);
                    this.resetCall();
                    this.pendingNotice = "rejected";
                } else if (this.outRinging.isEmpty() && this.outPeer == null) {
                    this.resetCall();
                    this.pendingNotice = "no_answer";
                }
            }

            this.syncScreens();
        }
    }

    protected float ringVolume() {
        return this.kind() == DeviceRegistry.Kind.INTERPHONE ? 0.8F : 1.0F;
    }

    protected void tickExtra() {
    }

    public void onBroken() {
        this.receiverDoors.broken(this.level);
        this.unlinkSwitchboard();
        if (this.level instanceof ServerLevel sl) {
            if (this.outgoing) {
                this.hangUp();
            } else if (this.callState != CallState.IDLE) {
                IntercomCaller c = this.callerBe();
                CallState st = this.callState;
                this.resetCall();
                if (c != null) {
                    c.onReceiverHangUp(this.worldPosition, st == CallState.RINGING ? DoorStatus.NO_ANSWER : DoorStatus.NO_SIGNAL);
                }
            }

            DeviceRegistry.get(sl).remove(this.worldPosition);
        }
    }

    public static <T extends ReceiverBlockEntity> void serverTick(Level level, BlockPos pos, BlockState state, T be) {
        be.enforceScope();
        be.receiverDoors.tick(level);
        be.tickExtra();
        if (be.callState == CallState.IDLE) {
            if (state.hasProperty(WallpadBlock.RINGING) && state.getValue(WallpadBlock.RINGING)) {
                be.setRingingState(false);
            }
        } else {
            be.callTicks++;
            if (be.outgoing) {
                be.tickOutgoing(level, pos);
            } else if (be.callTicks % 20 == 0 && be.callerBe() == null) {
                be.resetCall();
                be.syncScreens();
            } else {
                if (be.callState == CallState.RINGING) {
                    int t = be.callTicks % 40;
                    float vol = be.ringVolume();
                    CallSounds.incoming(
                        level,
                        pos,
                        be.incomingKey.startsWith("guard")
                            || be.incomingKey.startsWith("unit:")
                            || be instanceof Kvm906BlockEntity k && k.design().model().busPanel() && be.incomingKey.equals("lobby"),
                        be.callTicks - 1,
                        vol
                    );
                    if (be.callTicks >= 700) {
                        IntercomCaller c = be.callerBe();
                        be.addMissed();
                        be.resetCall();
                        if (c != null) {
                            c.onReceiverHangUp(pos, DoorStatus.NO_ANSWER);
                        }

                        be.syncScreens();
                    }
                } else if (be.callState == CallState.CONNECTED && be.callTicks >= 3600) {
                    IntercomCaller c = be.callerBe();
                    be.resetCall();
                    if (c != null) {
                        c.onReceiverHangUp(pos, DoorStatus.TIMEOUT);
                    }

                    be.syncScreens();
                }
            }
        }
    }

    protected void tickOutgoing(Level level, BlockPos pos) {
        if (this.callState == CallState.DIALING) {
            if (this.callTicks % 40 == 1) {
                level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 0.35F, 0.8F);
            }

            if (this.callTicks % 20 == 0) {
                this.outRinging.removeIf(p -> {
                    ReceiverBlockEntity r = this.receiverAt(p);
                    return r == null || r.callerSessionState(this.worldPosition) == CallState.IDLE;
                });
                if (this.outRinging.isEmpty()) {
                    this.resetCall();
                    this.pendingNotice = "no_answer";
                    this.syncScreens();
                    return;
                }
            }

            if (this.callTicks >= 600) {
                this.stopOutRinging(true);
                this.resetCall();
                this.pendingNotice = "no_answer";
                this.syncScreens();
            }
        } else if (this.callState == CallState.CONNECTED) {
            if (this.callTicks % 20 == 0) {
                ReceiverBlockEntity peer = this.receiverAt(this.outPeer);
                if (peer == null || peer.callerSessionState(this.worldPosition) != CallState.CONNECTED) {
                    this.resetCall();
                    this.pendingNotice = "ended";
                    this.syncScreens();
                    return;
                }
            }

            if (this.callTicks >= 3620) {
                ReceiverBlockEntity peer = this.receiverAt(this.outPeer);
                if (peer != null && peer.callerSessionState(this.worldPosition) != CallState.IDLE) {
                    peer.endCallerSession(this.worldPosition, false);
                }

                this.resetCall();
                this.pendingNotice = "timeout";
                this.syncScreens();
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.productLogo.read(tag);
        this.receiverDoors.read(tag);
        this.analogStation = tag.contains("AnalogStation") ? BlockPos.of(tag.getLong("AnalogStation")) : null;
        if (tag.contains("Unit")) {
            this.unit = tag.getString("Unit");
        }

        this.doorPassword = tag.getString("DoorPassword");
        this.missed.clear();
        ListTag list = tag.getList("Missed", 10);

        for (int i = 0; i < list.size(); i++) {
            this.missed.add(MissedCall.load(list.getCompound(i)));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        this.productLogo.write(tag);
        this.receiverDoors.write(tag);
        if (this.analogStation != null) {
            tag.putLong("AnalogStation", this.analogStation.asLong());
        }

        tag.putString("Unit", this.unit);
        tag.putString("DoorPassword", this.doorPassword);
        ListTag list = new ListTag();

        for (MissedCall m : this.missed) {
            list.add(m.save());
        }

        tag.put("Missed", list);
    }

    public static enum Action {
        REFRESH,
        TOGGLE,
        ALL_LIGHTS_OFF,
        ALL_OFF,
        SET_UNIT,
        ANSWER,
        OPEN_DOOR,
        HANG_UP,
        SEND_MESSAGE,
        CLEAR_MISSED,
        SET_DOOR_PASSWORD,
        DIAL,
        CALL_GUARD,
        SET_SETTING,
        EMERGENCY,
        OUTING,
        SAVE_VISITOR,
        DELETE_VISITOR,
        CLEAR_VISITORS,
        UNLOCK,
        SET_TEMP,
        SET_LEVEL,
        SET_POWER,
        ALARM_STOP,
        ALARM_TEST,
        ALL_LIGHTS_ON,
        SET_AWAY,
        RESET_SOUND,
        CALL_LOBBY,
        ABSENT_SET,
        ABSENT_CLEAR,
        BUSY_FWD_SET,
        BUSY_FWD_CLEAR,
        CONFIRM_ALERT,
        CLEAR_RECORDS,
        PARCEL_ADD,
        PARCEL_DONE,
        GUARD_DOOR,
        PICKUP;

        public static ReceiverBlockEntity.Action byId(int id) {
            ReceiverBlockEntity.Action[] v = values();
            return id >= 0 && id < v.length ? v[id] : REFRESH;
        }
    }
}
