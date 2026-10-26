package com.qwerty.homenet.blockentity;

import com.qwerty.homenet.block.CompactInterphoneBlock;
import com.qwerty.homenet.block.Ip203apBlock;
import com.qwerty.homenet.block.ReceiverBlock;
import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.data.ZoneData;
import com.qwerty.homenet.guard.GuardExchange;
import com.qwerty.homenet.guard.GuardSystem;
import com.qwerty.homenet.intercom.CallState;
import com.qwerty.homenet.intercom.DoorStatus;
import com.qwerty.homenet.intercom.Intercom;
import com.qwerty.homenet.intercom.IntercomCaller;
import com.qwerty.homenet.intercom.IntercomLine;
import com.qwerty.homenet.network.ModNetwork;
import com.qwerty.homenet.registry.ModBlockEntities;
import com.qwerty.homenet.registry.ModBlocks;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class Ip203apBlockEntity extends ReceiverBlockEntity {
    private boolean lifted;
    private boolean clientFrontLit;
    private boolean clientGuardLit;
    public static final String[] LAMPS = new String[]{"power", "front", "guard", "away", "lobby"};
    private final Map<String, Integer> clientLampModes = new HashMap<>();
    private int lastLampHash = -1;
    protected int volume = 70;
    private Ip203apBlockEntity.Session heldGuard;
    private Ip203apBlockEntity.Session heldFront;

    public int lampMode(String lamp) {
        return this.level != null && this.level.isClientSide ? this.clientLampModes.getOrDefault(lamp, 0) : this.serverLampMode(lamp);
    }

    protected int serverLampMode(String lamp) {
        if (lamp.equals("power")) {
            return 1;
        } else if (!lamp.equals("front") && !lamp.equals("guard")) {
            return 0;
        } else {
            boolean guard = lamp.equals("guard");
            Ip203apBlockEntity.Session held = guard ? this.heldGuard : this.heldFront;
            int active = this.callState != CallState.IDLE && this.guardActive() == guard ? (this.callState == CallState.CONNECTED ? 1 : 2) : 0;
            int waiting = held == null ? 0 : (held.state == CallState.CONNECTED ? 1 : 2);
            return active != 2 && waiting != 2 ? Math.max(active, waiting) : 2;
        }
    }

    public static boolean lampVisible(int mode, long ticks) {
        return mode == 1 || mode == 2 && Math.floorMod(ticks, 20) < 10;
    }

    protected void syncLampState() {
        if (this.level != null && !this.level.isClientSide) {
            int hash = 1;

            for (String lamp : LAMPS) {
                hash = hash * 3 + this.lampMode(lamp);
            }

            if (hash != this.lastLampHash) {
                this.lastLampHash = hash;
                this.changed();
            }
        }
    }

    @Override
    public void syncScreens() {
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }

        super.syncScreens();
    }

    private boolean guardActive() {
        return this.outgoing || Intercom.isReceiverKey(this.incomingKey);
    }

    private Ip203apBlockEntity.Session snapshot() {
        return new Ip203apBlockEntity.Session(
            this.callState,
            this.caller,
            this.incomingKey,
            this.outgoing,
            this.outPeer,
            this.peerLabel,
            new ArrayList<>(this.outRinging),
            new ArrayList<>(this.log),
            this.callTicks
        );
    }

    private void restore(Ip203apBlockEntity.Session s) {
        this.callState = s.state;
        this.caller = s.caller;
        this.incomingKey = s.key;
        this.outgoing = s.outgoing;
        this.outPeer = s.peer;
        this.peerLabel = s.label;
        this.outRinging.clear();
        this.outRinging.addAll(s.ringing);
        this.log.clear();
        this.log.addAll(s.lines);
        this.callTicks = s.ticks;
        this.setRingingState(this.callState == CallState.RINGING);
    }

    private boolean select(boolean guard) {
        if (this.callState != CallState.IDLE && this.guardActive() == guard) {
            if (this.callState == CallState.RINGING) {
                this.answer();
            }

            this.changed();
            return true;
        } else {
            Ip203apBlockEntity.Session target = guard ? this.heldGuard : this.heldFront;
            if (target == null) {
                return false;
            } else {
                if (guard) {
                    this.heldGuard = null;
                } else {
                    this.heldFront = null;
                }

                if (this.callState != CallState.IDLE) {
                    if (this.guardActive()) {
                        this.heldGuard = this.snapshot();
                    } else {
                        this.heldFront = this.snapshot();
                    }
                }

                this.restore(target);
                if (this.callState == CallState.RINGING) {
                    this.answer();
                }

                this.changed();
                return true;
            }
        }
    }

    @Override
    public boolean startRinging(BlockPos from, String key) {
        if (!ZoneData.connected(this.level, this.worldPosition, from)) {
            return false;
        } else if (this.callState == CallState.CONNECTED
            && this.guardActive()
            && !Intercom.isReceiverKey(key)
            && this.hasLinkedCamera(from)
            && this.heldFront == null) {
            this.heldFront = new Ip203apBlockEntity.Session(CallState.RINGING, from.immutable(), key, false, null, "", new ArrayList<>(), new ArrayList<>(), 0);
            this.changed();
            return true;
        } else {
            return super.startRinging(from, key);
        }
    }

    @Override
    public boolean hasCallerSession(BlockPos from) {
        return super.hasCallerSession(from)
            || this.heldGuard != null && from.equals(this.heldGuard.caller)
            || this.heldFront != null && from.equals(this.heldFront.caller);
    }

    @Override
    public CallState callerSessionState(BlockPos from) {
        if (this.heldGuard != null && from.equals(this.heldGuard.caller)) {
            return this.heldGuard.state;
        } else {
            return this.heldFront != null && from.equals(this.heldFront.caller) ? this.heldFront.state : super.callerSessionState(from);
        }
    }

    @Override
    public void endCallerSession(BlockPos from, boolean missed) {
        if (this.heldGuard != null && from.equals(this.heldGuard.caller)) {
            this.heldGuard = null;
        } else if (this.heldFront != null && from.equals(this.heldFront.caller)) {
            this.heldFront = null;
        } else {
            super.endCallerSession(from, missed);
        }

        this.changed();
    }

    @Override
    public void onReceiverHangUp(BlockPos from, DoorStatus reason) {
        if (this.heldGuard == null || !this.heldGuard.outgoing || !from.equals(this.heldGuard.peer) && !this.heldGuard.ringing.contains(from)) {
            super.onReceiverHangUp(from, reason);
            this.changed();
        } else {
            this.heldGuard = null;
            this.changed();
        }
    }

    @Override
    public void addLine(IntercomLine line) {
        if (this.heldGuard != null && line.side().startsWith("guard")) {
            this.heldGuard.lines.add(line);
        } else if (this.heldFront != null && line.side().equals("front_door")) {
            this.heldFront.lines.add(line);
        } else {
            super.addLine(line);
        }
    }

    @Override
    public void onBroken() {
        this.hangUp();
        super.onBroken();
    }

    @Override
    public void hangUp() {
        Ip203apBlockEntity.Session guard = this.heldGuard;
        Ip203apBlockEntity.Session front = this.heldFront;
        this.heldGuard = null;
        this.heldFront = null;
        super.hangUp();
        if (guard != null) {
            this.restore(guard);
            super.hangUp();
        }

        if (front != null) {
            this.restore(front);
            super.hangUp();
        }

        this.changed();
    }

    public Ip203apBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.IP_203AP.get(), pos, state);
    }

    protected Ip203apBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void openFamilyModels(ServerPlayer player) {
        ModNetwork.sendTo(player, this.buildPacket(true).withNotice("ip_family_models"));
    }

    public boolean changeFamily(ServerPlayer player, String id) {
        if (this.level instanceof ServerLevel sl
            && !this.isBusy()
            && !this.lifted()
            && !this.frontLit()
            && !this.guardLit()
            && !(this instanceof Kip234r2BlockEntity k && k.lobbyLit())) {
            ReceiverBlock block = (ReceiverBlock)(switch (id) {
                case "ip203ap" -> (Ip203apBlock)ModBlocks.IP_203AP.get();
                case "ip203apb" -> (Ip203apBlock)ModBlocks.IP_203APB.get();
                case "kip234r2" -> (Ip203apBlock)ModBlocks.KIP_234R2.get();
                case "ip200pc" -> (CompactInterphoneBlock)ModBlocks.IP_200PC.get();
                case "ip200pchb" -> (CompactInterphoneBlock)ModBlocks.IP_200PCHB.get();
                case "dp202h" -> (CompactInterphoneBlock)ModBlocks.DP_202H.get();
                default -> null;
            });
            if (block == null) {
                return false;
            } else if (this.getBlockState().is(block)) {
                ModNetwork.sendTo(player, this.buildPacket(true).withNotice("ip_family_changed"));
                return true;
            } else {
                CompoundTag tag = this.saveWithoutMetadata();
                BlockPos station = this.analogStation;
                GuardSystem system = this.guardSystem();
                String number = this.unit;
                tag.remove("AnalogStation");
                tag.remove("Emergency");
                tag.remove("Away");
                tag.remove("ApLifted");
                Direction facing = this.getBlockState().getValue(WallMountedBlock.FACING);
                sl.setBlock(this.worldPosition, block.defaultBlockState().setValue(WallMountedBlock.FACING, facing), 3);
                if (sl.getBlockEntity(this.worldPosition) instanceof Ip203apBlockEntity next) {
                    if (!next.canSetUnitLocally()) {
                        tag.putString("Unit", "");
                    }

                    if (!next.supportsDoorCommunication()) {
                        tag.remove("ReceiverDoors");
                    }

                    next.load(tag);
                    next.register();
                    if (station != null && system == next.guardSystem() && next.bindSwitchboard(station) && next.switchboard() != null && !number.isBlank()) {
                        next.assignUnitFromSwitchboard(station, number);
                        next.switchboard().onLinked(this.worldPosition, number);
                    }

                    if (!next.supportsDoorCommunication()) {
                        for (DeviceRegistry.Entry e : DeviceRegistry.get(sl).all()) {
                            if (sl.isLoaded(e.pos())
                                && sl.getBlockEntity(e.pos()) instanceof DoorStationBlockEntity door
                                && door.hasReceiver(this.worldPosition)) {
                                door.toggleReceiver(this.worldPosition);
                            }
                        }
                    }

                    next.changed();
                    ModNetwork.sendTo(player, next.buildPacket(true).withNotice("ip_family_changed"));
                    return true;
                } else {
                    return false;
                }
            }
        } else {
            player.displayClientMessage(Component.literal("수화기를 내려놓고 통화를 끝낸 뒤 변경하세요."), true);
            return false;
        }
    }

    @Override
    public GuardSystem guardSystem() {
        return GuardSystem.STAR;
    }

    @Override
    public String manufacturer() {
        return "KOCOM";
    }

    public boolean lifted() {
        return this.lifted;
    }

    protected void setLifted(boolean value) {
        this.lifted = value;
    }

    public boolean frontLit() {
        return this.level != null && this.level.isClientSide
            ? this.clientFrontLit
            : this.callState != CallState.IDLE && !this.guardActive() || this.heldFront != null;
    }

    public boolean guardLit() {
        return this.level != null && this.level.isClientSide
            ? this.clientGuardLit
            : this.callState != CallState.IDLE && this.guardActive() || this.heldGuard != null;
    }

    public void handset(ServerPlayer player) {
        this.lifted = !this.lifted;
        if (this.lifted) {
            if (this.callState == CallState.RINGING) {
                this.answer();
            } else if (this.callState == CallState.IDLE) {
                this.callGuard(player, "guard");
            }
        } else {
            this.hangUp();
        }

        this.changed();
    }

    protected void changed() {
        this.setChanged();
        this.syncScreens();
    }

    @Override
    public void setUnit(ServerPlayer player, String raw) {
        super.setUnit(player, raw);
        if (this.switchboard() != null) {
            this.switchboard().onLinked(this.worldPosition, this.unit);
        }
    }

    @Override
    public boolean acceptSwitchboardCall(BlockPos station) {
        if (this.lifted && this.callState == CallState.DIALING && station.equals(this.caller) && this.switchboard() != null) {
            this.callState = CallState.CONNECTED;
            this.callTicks = 0;
            this.changed();
            return true;
        } else {
            return false;
        }
    }

    public static boolean isStarConsole(BlockEntity be) {
        return be instanceof ReceiverBlockEntity r && r.getBlockState().is(ModBlocks.GUARD_CONSOLE.get()) && r.guardSystem() == GuardSystem.STAR;
    }

    @Override
    public boolean bindSwitchboard(BlockPos pos) {
        if (this.level instanceof ServerLevel
            && ZoneData.connected(this.level, this.worldPosition, pos)
            && this.level.isLoaded(pos)
            && this.guardSystem() == GuardSystem.STAR
            && isStarConsole(this.level.getBlockEntity(pos))
            && !(this.level.getBlockEntity(pos) instanceof GuardExchange)) {
            this.unlinkSwitchboard();
            this.analogStation = pos.immutable();
            this.register();
            this.changed();
            return true;
        } else {
            return super.bindSwitchboard(pos);
        }
    }

    protected void callStarGuard(ServerPlayer player) {
        this.callGuardStar(player);
    }

    @Override
    public void callGuard(ServerPlayer player, String mode) {
        this.callGuardStar(player);
    }

    private void callGuardStar(ServerPlayer player) {
        if (this.lifted && !this.isBusy() && !this.unit.isBlank()) {
            if (this.analogStation != null) {
                GuardExchange board = this.switchboard();
                if (board != null) {
                    this.caller = this.analogStation;
                    this.incomingKey = "guard:" + board.modelId();
                    this.outgoing = false;
                    this.callTicks = 0;
                    this.callState = CallState.DIALING;
                    board.callFrom(this, player);
                    this.changed();
                } else {
                    if (this.level instanceof ServerLevel sl && sl.isLoaded(this.analogStation) && isStarConsole(sl.getBlockEntity(this.analogStation))) {
                        this.tryOutgoing(player, List.of(this.analogStation), "#guard");
                        this.changed();
                    }
                }
            } else {
                if (this.level instanceof ServerLevel sl) {
                    List<BlockPos> guards = DeviceRegistry.get(sl)
                        .guards(sl, this.worldPosition, false)
                        .stream()
                        .filter(p -> isStarConsole(sl.getBlockEntity(p)))
                        .toList();
                    this.tryOutgoing(player, guards, "#guard");
                    this.changed();
                }
            }
        }
    }

    @Override
    public void handleAction(ServerPlayer player, ReceiverBlockEntity.Action action, BlockPos target, String text) {
        switch (action) {
            case PICKUP:
                this.handset(player);
                break;
            case HANG_UP:
                this.lifted = false;
                this.hangUp();
                this.changed();
                break;
            case SET_UNIT:
                this.setUnit(player, text);
                break;
            case ANSWER:
                if (this.lifted) {
                    this.answer();
                    this.changed();
                }
                break;
            case CALL_GUARD:
                if (this.lifted && !this.select(true)) {
                    this.callGuard(player, "guard");
                }
                break;
            case SET_SETTING:
                if ("front".equals(text) && this.lifted) {
                    this.select(false);
                } else if (text.startsWith("volume=")) {
                    try {
                        this.volume = Math.max(0, Math.min(100, Integer.parseInt(text.substring(7))));
                        this.changed();
                    } catch (NumberFormatException var6) {
                    }
                }
                break;
            case SEND_MESSAGE:
                if (this.lifted) {
                    this.sendMessage(player, text);
                }
        }

        this.syncScreens();
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putBoolean("ApLifted", this.lifted);
        tag.putBoolean("ApFrontLit", this.frontLit());
        tag.putBoolean("ApGuardLit", this.guardLit());
        CompoundTag lamps = new CompoundTag();

        for (String lamp : LAMPS) {
            lamps.putInt(lamp, this.lampMode(lamp));
        }

        tag.put("ApLampModes", lamps);
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.clientLampModes.clear();
        CompoundTag lamps = tag.getCompound("ApLampModes");

        for (String lamp : LAMPS) {
            this.clientLampModes.put(lamp, lamps.getInt(lamp));
        }

        this.lastLampHash = -1;
        this.lifted = tag.getBoolean("ApLifted");
        this.clientFrontLit = tag.getBoolean("ApFrontLit");
        this.clientGuardLit = tag.getBoolean("ApGuardLit");
        this.volume = tag.contains("ApVolume") ? Math.max(0, Math.min(100, tag.getInt("ApVolume"))) : 70;
        this.heldGuard = null;
        this.heldFront = null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("ApVolume", this.volume);
    }

    private void ringSound(boolean guard, int ticks) {
        if (this.level != null && this.volume != 0) {
            int t = ticks % 40;
            float v = this.volume / 100.0F;
            if (guard) {
                if (t == 0 || t == 5 || t == 10 || t == 15) {
                    this.level
                        .playSound(
                            null,
                            this.worldPosition,
                            SoundEvents.NOTE_BLOCK_CHIME.value(),
                            SoundSource.BLOCKS,
                            v,
                            new float[]{1.06F, 1.335F, 1.189F, 1.587F}[t / 5]
                        );
                }
            } else if (t == 0 || t == 8) {
                this.level.playSound(null, this.worldPosition, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, v, t == 0 ? 1.19F : 0.94F);
            }
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, Ip203apBlockEntity b) {
        b.enforceScope();
        if (b.heldFront != null && !ZoneData.connected(level, pos, b.heldFront.caller)
            || b.heldGuard != null && !ZoneData.connected(level, pos, b.heldGuard.caller != null ? b.heldGuard.caller : b.heldGuard.peer)) {
            b.hangUp();
        }

        b.receiverDoors.tick(level);
        b.syncLampState();
        if (b instanceof Ip203apbBlockEntity apb && level.getGameTime() % 20L == 0L) {
            apb.refreshBoard();
        }

        if (b.heldFront != null && b.heldFront.state == CallState.RINGING) {
            Ip203apBlockEntity.Session s = b.heldFront;
            b.ringSound(false, s.ticks);
            b.heldFront = new Ip203apBlockEntity.Session(s.state, s.caller, s.key, s.outgoing, s.peer, s.label, s.ringing, s.lines, s.ticks + 1);
            if (s.ticks >= 700) {
                b.heldFront = null;
                if (level.getBlockEntity(s.caller) instanceof IntercomCaller c) {
                    c.onReceiverHangUp(pos, DoorStatus.NO_ANSWER);
                }

                b.changed();
            }
        }

        if (b.callState != CallState.IDLE) {
            if (b.callState == CallState.RINGING) {
                b.ringSound(b.guardActive(), b.callTicks);
            }

            b.callTicks++;
            if (b.outgoing) {
                b.tickOutgoing(level, pos);
            } else if (b.callTicks % 20 == 0 && b.callerBe() == null) {
                b.resetCall();
                b.changed();
            } else {
                if (b.callState == CallState.RINGING && b.callTicks >= 700
                    || b.callState == CallState.DIALING && b.callTicks >= 600
                    || b.callState == CallState.CONNECTED && b.callTicks >= 3600) {
                    b.hangUp();
                    b.changed();
                }
            }
        }
    }

    @Override
    protected Map<String, String> settingsForClient() {
        HashMap<String, String> s = new HashMap<>();
        s.put("ip203ap", "1");

        for (String lamp : LAMPS) {
            s.put("lamp_" + lamp, Integer.toString(this.lampMode(lamp)));
        }

        s.put("front_lit", this.frontLit() ? "1" : "0");
        s.put("guard_lit", this.guardLit() ? "1" : "0");
        s.put("board", this.switchboard() == null ? (this.analogStation == null ? "미연결" : "아날로그 경비실") : this.switchboard().modelId());
        s.put("lifted", this.lifted ? "1" : "0");
        s.put("guard_system", this.guardSystem().name());
        s.put("unit_edit", this.canSetUnitLocally() ? "1" : "0");
        s.put("source", this.callState == CallState.IDLE ? "idle" : (this.guardActive() ? "guard" : "front"));
        s.put("front_waiting", this.heldFront != null && this.heldFront.state == CallState.RINGING ? "1" : "0");
        s.put("volume", Integer.toString(this.volume));
        return s;
    }

    private record Session(
        CallState state,
        BlockPos caller,
        String key,
        boolean outgoing,
        BlockPos peer,
        String label,
        List<BlockPos> ringing,
        List<IntercomLine> lines,
        int ticks
    ) {
    }
}
