package com.qwerty.homenet.blockentity;

import com.qwerty.homenet.block.DoorPhoneBlock;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.data.ZoneData;
import com.qwerty.homenet.intercom.DoorStatus;
import com.qwerty.homenet.intercom.Intercom;
import com.qwerty.homenet.intercom.IntercomLine;
import com.qwerty.homenet.network.DoorStationDataPacket;
import com.qwerty.homenet.network.ModNetwork;
import com.qwerty.homenet.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class DoorStationBlockEntity extends CallerBlockEntity {
    private final List<BlockPos> receivers = new ArrayList<>();
    private DoorStatus status = DoorStatus.IDLE;
    private String statusArg = "";

    public DoorStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DOOR_STATION.get(), pos, state);
    }

    public boolean isDoorPhone() {
        return this.getBlockState().getBlock() instanceof DoorPhoneBlock;
    }

    @Override
    public DeviceRegistry.Kind kind() {
        return this.isDoorPhone() ? DeviceRegistry.Kind.DOOR_PHONE : DeviceRegistry.Kind.DOOR_CAMERA;
    }

    @Override
    public String callerKey() {
        return "front_door";
    }

    @Override
    public String registeredUnit() {
        return "";
    }

    public String getUnit() {
        return "";
    }

    public boolean hasReceiver(BlockPos pos) {
        return this.receivers.contains(pos) && ZoneData.connected(this.level, this.worldPosition, pos);
    }

    public List<BlockPos> linkedReceivers() {
        return List.copyOf(this.receivers);
    }

    public boolean toggleReceiver(BlockPos pos) {
        if (this.receivers.remove(pos)) {
            if (this.inCall) {
                this.hangUpFromCaller();
            }

            this.setChanged();
            this.syncScreens();
            return false;
        } else if (!ZoneData.connected(this.level, this.worldPosition, pos)) {
            return false;
        } else if (this.level != null && this.level.getBlockEntity(pos) instanceof ReceiverBlockEntity r && !r.supportsDoorCommunication()) {
            return false;
        } else if (this.receivers.size() >= 32) {
            throw new IllegalStateException("too many linked receivers");
        } else {
            this.receivers.add(pos.immutable());
            this.setChanged();
            this.syncScreens();
            return true;
        }
    }

    @Override
    public CallerBlockEntity.CallResult startCall(String ignoredUnit) {
        if (this.inCall) {
            return CallerBlockEntity.CallResult.BUSY;
        } else if (this.receivers.isEmpty()) {
            return CallerBlockEntity.CallResult.NO_UNIT;
        } else {
            List<ReceiverBlockEntity> loaded = new ArrayList<>();

            for (BlockPos pos : this.receivers) {
                ReceiverBlockEntity receiver = this.receiverAt(pos);
                if (receiver != null) {
                    loaded.add(receiver);
                }
            }

            return loaded.isEmpty() ? CallerBlockEntity.CallResult.NO_SIGNAL : this.startReceivers(loaded);
        }
    }

    @Override
    public List<ReceiverBlockEntity> receiversOf(String ignoredUnit) {
        List<ReceiverBlockEntity> loaded = new ArrayList<>();

        for (BlockPos pos : this.receivers) {
            ReceiverBlockEntity receiver = this.receiverAt(pos);
            if (receiver != null) {
                loaded.add(receiver);
            }
        }

        return loaded;
    }

    @Override
    public void openDoors() {
    }

    public void openFor(ServerPlayer player) {
        ModNetwork.sendTo(player, this.buildPacket(true));
    }

    @Override
    public void syncScreens() {
        if (this.level instanceof ServerLevel sl) {
            DoorStationDataPacket var5 = this.buildPacket(false);

            for (ServerPlayer p : Intercom.playersNear(sl, this.worldPosition, 8.0)) {
                ModNetwork.sendTo(p, var5);
            }
        }
    }

    private DoorStationDataPacket buildPacket(boolean open) {
        return new DoorStationDataPacket(
            this.worldPosition,
            open,
            false,
            "링크 " + this.receivers.size() + "개",
            this.status.ordinal(),
            this.statusArg,
            this.inCall,
            this.isConnected(),
            new ArrayList<>(this.log)
        );
    }

    public void handleAction(ServerPlayer player, DoorStationBlockEntity.Action action, String text) {
        switch (action) {
            case CALL:
                this.call();
                break;
            case HANG_UP:
                if (this.inCall) {
                    this.hangUpFromCaller();
                    this.setStatus(DoorStatus.ENDED);
                }
                break;
            case SEND_MESSAGE:
                if (this.isConnected()) {
                    this.say(player, this.callerKey(), text);
                }
        }

        this.syncScreens();
    }

    public void call() {
        if (this.level instanceof ServerLevel) {
            if (this.inCall) {
                this.setStatus(DoorStatus.BUSY);
            } else if (this.receivers.isEmpty()) {
                this.setStatus(DoorStatus.NO_UNIT);
                this.notifyNearby();
            } else {
                switch (this.startCall("")) {
                    case OK:
                        this.setStatus(DoorStatus.CALLING);
                        this.level.playSound(null, this.worldPosition, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.BLOCKS, 0.6F, 1.4F);
                        break;
                    case NO_UNIT:
                        this.setStatus(DoorStatus.NO_UNIT);
                        break;
                    case NO_SIGNAL:
                        this.setStatus(DoorStatus.NO_SIGNAL);
                        break;
                    case BUSY:
                        this.setStatus(DoorStatus.BUSY);
                }

                this.notifyNearby();
                this.syncScreens();
            }
        }
    }

    private void setStatus(DoorStatus s) {
        this.status = s;
        this.statusArg = "연결 기기 " + this.receivers.size() + "개";
    }

    @Override
    protected void onConnected() {
        this.setStatus(DoorStatus.CONNECTED);
        this.notifyNearby();
    }

    @Override
    protected void onEnded(DoorStatus reason) {
        this.setStatus(reason);
        this.notifyNearby();
    }

    @Override
    protected void onDoorOpenedByReceiver() {
        this.setStatus(DoorStatus.OPENED);
        this.notifyNearby();
    }

    private void notifyNearby() {
        if (this.level instanceof ServerLevel sl) {
            MutableComponent var5 = this.status.display(this.statusArg).copy().withStyle(ChatFormatting.GREEN);

            for (ServerPlayer p : Intercom.playersNear(sl, this.worldPosition, 8.0)) {
                p.displayClientMessage(var5, true);
            }
        }
    }

    @Override
    public void addLine(IntercomLine line) {
        super.addLine(line);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DoorStationBlockEntity be) {
        be.tickCaller();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.receivers.clear();

        for (long pos : tag.getLongArray("Receivers")) {
            if (this.receivers.size() < 32) {
                this.receivers.add(BlockPos.of(pos));
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLongArray("Receivers", this.receivers.stream().mapToLong(BlockPos::asLong).toArray());
    }

    public static enum Action {
        CALL,
        HANG_UP,
        SEND_MESSAGE;

        public static DoorStationBlockEntity.Action byId(int id) {
            DoorStationBlockEntity.Action[] v = values();
            return id >= 0 && id < v.length ? v[id] : HANG_UP;
        }
    }
}
