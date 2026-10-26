package com.qwerty.homenet.blockentity;

import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.data.ZoneData;
import com.qwerty.homenet.intercom.CallSounds;
import com.qwerty.homenet.intercom.DoorControl;
import com.qwerty.homenet.intercom.DoorStatus;
import com.qwerty.homenet.intercom.Intercom;
import com.qwerty.homenet.intercom.IntercomCaller;
import com.qwerty.homenet.intercom.IntercomLine;
import com.qwerty.homenet.logo.ProductLogo;
import com.qwerty.homenet.network.ModNetwork;
import com.qwerty.homenet.network.WallpadDataPacket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

public abstract class CallerBlockEntity extends BlockEntity implements IntercomCaller {
    public final ProductLogo productLogo = new ProductLogo();
    public static final int MAX_DOORS = 16;
    public static final int DOOR_RANGE = 32;
    public static final int CALLER_RING_TICKS = 600;
    protected final List<BlockPos> linkedDoors = new ArrayList<>();
    private final Map<BlockPos, Long> closeAt = new HashMap<>();
    protected final List<BlockPos> ringing = new ArrayList<>();
    @Nullable
    protected BlockPos connectedTo;
    protected boolean inCall;
    protected long callStarted;
    protected final List<IntercomLine> log = new ArrayList<>();

    public WallpadDataPacket logoPacket(boolean open) {
        HashMap<String, String> settings = new HashMap<>();
        this.productLogo.settings(settings);
        return new WallpadDataPacket(
            this.worldPosition,
            open,
            this.kind().ordinal(),
            this.registeredUnit(),
            false,
            0,
            "",
            false,
            "",
            open ? "logo_editor" : "",
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            settings,
            new long[0],
            List.of()
        );
    }

    public void openLogoEditor(ServerPlayer player) {
        ModNetwork.sendTo(player, this.logoPacket(true));
    }

    public boolean editLogo(String text) {
        int i = text.indexOf(61);
        if (i >= 0 && this.productLogo.set(text.substring(0, i), text.substring(i + 1))) {
            this.setChanged();
            if (this.level != null) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            }

            return true;
        } else {
            return false;
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

    protected CallerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public abstract DeviceRegistry.Kind kind();

    public String registeredUnit() {
        return "";
    }

    protected int openTicks() {
        return 60;
    }

    protected long now() {
        return this.level == null ? 0L : this.level.getGameTime();
    }

    protected void registerDevice() {
        if (this.level instanceof ServerLevel sl) {
            DeviceRegistry.get(sl).register(this.worldPosition, this.kind(), this.registeredUnit());
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        this.registerDevice();
    }

    protected void onConnected() {
    }

    protected void onEnded(DoorStatus reason) {
    }

    protected void onDoorOpenedByReceiver() {
    }

    public boolean isInCall() {
        return this.inCall;
    }

    public boolean isConnected() {
        return this.connectedTo != null;
    }

    public List<IntercomLine> getLog() {
        return this.log;
    }

    @Nullable
    protected ReceiverBlockEntity receiverAt(@Nullable BlockPos pos) {
        if (pos != null && this.level != null && this.level.isLoaded(pos)) {
            return ZoneData.connected(this.level, this.worldPosition, pos) && this.level.getBlockEntity(pos) instanceof ReceiverBlockEntity r ? r : null;
        } else {
            return null;
        }
    }

    public List<ReceiverBlockEntity> receiversOf(String unit) {
        List<ReceiverBlockEntity> out = new ArrayList<>();
        if (this.level instanceof ServerLevel sl) {
            for (BlockPos p : DeviceRegistry.get(sl).receivers(sl, this.worldPosition, unit)) {
                ReceiverBlockEntity r = this.receiverAt(p);
                if (r != null && DeviceRegistry.normalize(r.getUnit()).equals(DeviceRegistry.normalize(unit))) {
                    out.add(r);
                }
            }

            return out;
        } else {
            return out;
        }
    }

    public CallerBlockEntity.CallResult startCall(String unit) {
        if (this.level instanceof ServerLevel sl && !this.inCall) {
            List<BlockPos> registered = DeviceRegistry.get(sl).receivers(sl, this.worldPosition, unit);
            if (registered.isEmpty()) {
                return CallerBlockEntity.CallResult.NO_UNIT;
            } else {
                List<ReceiverBlockEntity> loaded = this.receiversOf(unit);
                return loaded.isEmpty() ? CallerBlockEntity.CallResult.NO_SIGNAL : this.startReceivers(loaded);
            }
        } else {
            return CallerBlockEntity.CallResult.BUSY;
        }
    }

    public CallerBlockEntity.CallResult callFromReceiver(ReceiverBlockEntity receiver) {
        return this instanceof DoorStationBlockEntity && receiver.hasLinkedCamera(this.worldPosition)
            ? this.startReceivers(List.of(receiver))
            : CallerBlockEntity.CallResult.NO_SIGNAL;
    }

    protected CallerBlockEntity.CallResult startReceivers(List<ReceiverBlockEntity> loaded) {
        if (this.inCall) {
            return CallerBlockEntity.CallResult.BUSY;
        } else {
            this.ringing.clear();

            for (ReceiverBlockEntity r : loaded) {
                if (ZoneData.connected(this.level, this.worldPosition, r.getBlockPos())) {
                    BlockPos at = r.ring(this.worldPosition, this.callerKey());
                    if (at != null && !this.ringing.contains(at)) {
                        this.ringing.add(at);
                    }
                }
            }

            if (this.ringing.isEmpty()) {
                return CallerBlockEntity.CallResult.BUSY;
            } else {
                this.inCall = true;
                this.connectedTo = null;
                this.callStarted = this.now();
                CallSounds.ringback(this.level, this.worldPosition, 0L, 0.6F);
                this.log.clear();
                return CallerBlockEntity.CallResult.OK;
            }
        }
    }

    public boolean acceptIncoming(BlockPos receiver) {
        if (!this.inCall && ZoneData.connected(this.level, this.worldPosition, receiver)) {
            this.inCall = true;
            this.ringing.clear();
            this.connectedTo = receiver.immutable();
            CallSounds.note(this.level, this.worldPosition, SoundEvents.NOTE_BLOCK_BELL.value(), 0.6F, 1.6F);
            CallSounds.note(this.level, receiver, SoundEvents.NOTE_BLOCK_BELL.value(), 0.6F, 1.6F);
            this.callStarted = this.now();
            this.log.clear();
            this.onConnected();
            this.syncScreens();
            return true;
        } else {
            return false;
        }
    }

    public void hangUpFromCaller() {
        this.stopAllRinging(true);
        ReceiverBlockEntity r = this.receiverAt(this.connectedTo);
        if (r != null && r.hasCallerSession(this.worldPosition)) {
            r.endCallerSession(this.worldPosition, false);
        }

        this.resetSession();
    }

    protected void stopAllRinging(boolean missed) {
        for (BlockPos p : this.ringing) {
            ReceiverBlockEntity r = this.receiverAt(p);
            if (r != null && r.hasCallerSession(this.worldPosition)) {
                r.endCallerSession(this.worldPosition, missed);
            }
        }

        this.ringing.clear();
    }

    protected void resetSession() {
        this.inCall = false;
        this.connectedTo = null;
        this.ringing.clear();
    }

    @Override
    public boolean onAnswered(BlockPos receiver) {
        if (ZoneData.connected(this.level, this.worldPosition, receiver) && this.inCall && this.connectedTo == null && this.ringing.contains(receiver)) {
            this.ringing.remove(receiver);
            this.stopAllRinging(false);
            this.connectedTo = receiver.immutable();
            CallSounds.note(this.level, this.worldPosition, SoundEvents.NOTE_BLOCK_BELL.value(), 0.6F, 1.6F);
            CallSounds.note(this.level, receiver, SoundEvents.NOTE_BLOCK_BELL.value(), 0.6F, 1.6F);
            this.callStarted = this.now();
            this.onConnected();
            this.syncScreens();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onDoorOpened(BlockPos receiver) {
        if (this.inCall) {
            this.ringing.remove(receiver);
            this.stopAllRinging(false);
            this.resetSession();
            this.openDoors();
            this.onDoorOpenedByReceiver();
            this.syncScreens();
        }
    }

    @Override
    public void onReceiverHangUp(BlockPos receiver, DoorStatus reason) {
        if (this.inCall) {
            if (receiver.equals(this.connectedTo)) {
                this.resetSession();
                this.onEnded(reason == DoorStatus.TIMEOUT ? DoorStatus.TIMEOUT : DoorStatus.ENDED);
            } else if (this.ringing.remove(receiver)) {
                if (reason == DoorStatus.REJECTED) {
                    this.stopAllRinging(false);
                    this.resetSession();
                    this.onEnded(DoorStatus.REJECTED);
                } else if (this.ringing.isEmpty() && this.connectedTo == null) {
                    this.resetSession();
                    this.onEnded(reason);
                }
            }

            this.syncScreens();
        }
    }

    @Override
    public void addLine(IntercomLine line) {
        this.log.add(line);

        while (this.log.size() > 8) {
            this.log.remove(0);
        }
    }

    public void say(ServerPlayer player, String side, String text) {
        ReceiverBlockEntity r = this.receiverAt(this.connectedTo);
        if (r != null && this.level instanceof ServerLevel sl) {
            Intercom.say(sl, r, this, player, side, text);
        }
    }

    public List<BlockPos> getLinkedDoors() {
        return this.linkedDoors;
    }

    public CallerBlockEntity.DoorLinkResult toggleDoor(BlockPos rawPos) {
        if (this.level == null) {
            return CallerBlockEntity.DoorLinkResult.NOT_DOOR;
        } else {
            BlockState s = this.level.getBlockState(rawPos);
            if (!DoorControl.isDoor(s)) {
                return CallerBlockEntity.DoorLinkResult.NOT_DOOR;
            } else {
                BlockPos pos = DoorControl.normalize(s, rawPos);
                if (this.linkedDoors.remove(pos)) {
                    this.setChanged();
                    return CallerBlockEntity.DoorLinkResult.UNLINKED;
                } else if (!ZoneData.connected(this.level, this.worldPosition, pos)) {
                    return CallerBlockEntity.DoorLinkResult.OUTSIDE_ZONE;
                } else if (this.linkedDoors.size() >= 16) {
                    return CallerBlockEntity.DoorLinkResult.FULL;
                } else if (!pos.closerThan(this.worldPosition, 32.0)) {
                    return CallerBlockEntity.DoorLinkResult.TOO_FAR;
                } else {
                    this.linkedDoors.add(pos);
                    this.setChanged();
                    return CallerBlockEntity.DoorLinkResult.LINKED;
                }
            }
        }
    }

    public void clearDoors() {
        this.linkedDoors.clear();
        this.setChanged();
    }

    public void openDoors() {
        if (this.level instanceof ServerLevel sl) {
            int var8 = this.openTicks();
            BlockState state = this.getBlockState();
            if (state.hasProperty(BlockStateProperties.POWERED)) {
                this.level.setBlock(this.worldPosition, state.setValue(BlockStateProperties.POWERED, true), 3);
                this.level.scheduleTick(this.worldPosition, state.getBlock(), Math.max(20, var8));
            }

            long close = this.now() + var8;

            for (BlockPos p : this.linkedDoors) {
                if (ZoneData.connected(this.level, this.worldPosition, p) && sl.isLoaded(p)) {
                    if (!DoorControl.isOpen(sl, p)) {
                        DoorControl.setOpen(sl, p, true);
                        this.closeAt.put(p, close);
                    } else if (this.closeAt.containsKey(p)) {
                        this.closeAt.put(p, close);
                    }
                }
            }
        }
    }

    protected void tickCaller() {
        if (this.level instanceof ServerLevel sl) {
            long var6 = this.now();
            if (this.inCall && this.connectedTo == null && var6 > this.callStarted) {
                CallSounds.ringback(this.level, this.worldPosition, var6 - this.callStarted, 0.6F);
            }

            if (!this.closeAt.isEmpty()) {
                Iterator<Entry<BlockPos, Long>> it = this.closeAt.entrySet().iterator();

                while (it.hasNext()) {
                    Entry<BlockPos, Long> e = it.next();
                    if (var6 >= e.getValue()) {
                        DoorControl.setOpen(sl, e.getKey(), false);
                        it.remove();
                    }
                }
            }

            if (this.inCall && var6 % 20L == 5L) {
                this.ringing.removeIf(p -> {
                    ReceiverBlockEntity rx = this.receiverAt(p);
                    return rx == null || !rx.hasCallerSession(this.worldPosition);
                });
                if (this.connectedTo != null) {
                    ReceiverBlockEntity r = this.receiverAt(this.connectedTo);
                    if (r == null || !r.hasCallerSession(this.worldPosition)) {
                        this.resetSession();
                        this.onEnded(DoorStatus.ENDED);
                        this.syncScreens();
                    }
                } else if (this.ringing.isEmpty()) {
                    this.resetSession();
                    this.onEnded(DoorStatus.NO_ANSWER);
                    this.syncScreens();
                } else if (var6 - this.callStarted >= 600L) {
                    this.stopAllRinging(true);
                    this.resetSession();
                    this.onEnded(DoorStatus.NO_ANSWER);
                    this.syncScreens();
                }
            }
        }
    }

    public void onBroken() {
        if (this.inCall) {
            this.hangUpFromCaller();
        }

        if (this.level instanceof ServerLevel sl) {
            for (BlockPos p : this.closeAt.keySet()) {
                DoorControl.setOpen(sl, p, false);
            }

            DeviceRegistry.get(sl).remove(this.worldPosition);
        }

        this.closeAt.clear();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.productLogo.read(tag);
        this.linkedDoors.clear();

        for (long l : tag.getLongArray("Doors")) {
            this.linkedDoors.add(BlockPos.of(l));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        this.productLogo.write(tag);
        tag.put("Doors", new LongArrayTag(this.linkedDoors.stream().mapToLong(BlockPos::asLong).toArray()));
    }

    public static enum CallResult {
        OK,
        NO_UNIT,
        NO_SIGNAL,
        BUSY;
    }

    public static enum DoorLinkResult {
        LINKED,
        UNLINKED,
        FULL,
        TOO_FAR,
        NOT_DOOR,
        OUTSIDE_ZONE;
    }
}
