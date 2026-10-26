package com.qwerty.homenet.blockentity;

import com.qwerty.homenet.block.ControlBlock;
import com.qwerty.homenet.block.DeviceType;
import com.qwerty.homenet.intercom.Intercom;
import com.qwerty.homenet.network.DeviceConfigOpenPacket;
import com.qwerty.homenet.network.ModNetwork;
import com.qwerty.homenet.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class DeviceBlockEntity extends BlockEntity {
    public static final int MAX_NAME = 20;
    private String name = "";
    @Nullable
    private DeviceType type;
    private int setTemp;
    private int fanLevel = 1;
    private boolean away;

    public DeviceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DEVICE.get(), pos, state);
    }

    public String getDeviceName() {
        return this.name;
    }

    public DeviceType getDeviceType() {
        if (this.type != null) {
            return this.type;
        } else {
            return this.getBlockState().getBlock() instanceof ControlBlock cb ? cb.defaultType() : DeviceType.OTHER;
        }
    }

    public boolean isOn() {
        BlockState s = this.getBlockState();
        return s.hasProperty(ControlBlock.POWERED) && s.getValue(ControlBlock.POWERED);
    }

    public void setOn(boolean on) {
        if (this.level != null && !this.level.isClientSide) {
            BlockState s = this.getBlockState();
            if (s.hasProperty(ControlBlock.POWERED) && s.getValue(ControlBlock.POWERED) != on) {
                this.level.setBlock(this.worldPosition, s.setValue(ControlBlock.POWERED, on), 3);
            }
        }
    }

    public int getSetTemp() {
        if (this.setTemp != 0) {
            return this.setTemp;
        } else {
            return this.getDeviceType() == DeviceType.AIRCON ? 26 : 24;
        }
    }

    public void adjustSetTemp(int delta) {
        boolean ac = this.getDeviceType() == DeviceType.AIRCON;
        this.setTemp = Math.max(ac ? 18 : 10, Math.min(ac ? 30 : 40, this.getSetTemp() + delta));
        this.setChanged();
    }

    public int getFanLevel() {
        return this.fanLevel;
    }

    public void setFanLevel(int level) {
        this.fanLevel = Math.max(1, Math.min(3, level));
        this.setChanged();
    }

    public boolean isAway() {
        return this.away;
    }

    public void setAway(boolean away) {
        this.away = away;
        this.setChanged();
    }

    public static int outdoorTemp(Level level, BlockPos pos) {
        float base = level.getBiome(pos).value().getBaseTemperature();
        int t = Math.round(base * 25.0F);
        if (level.isRaining()) {
            t -= 3;
        }

        return Math.max(-15, Math.min(40, t));
    }

    public int roomTemp() {
        if (this.level == null) {
            return 20;
        } else {
            int t = Math.max(8, Math.min(34, outdoorTemp(this.level, this.worldPosition)));
            if (this.isOn() && this.getDeviceType() == DeviceType.HEATING && !this.away) {
                t = Math.max(t, this.getSetTemp());
            }

            if (this.isOn() && this.getDeviceType() == DeviceType.AIRCON) {
                t = Math.min(t, this.getSetTemp());
            }

            return t;
        }
    }

    public void configure(String newName, DeviceType newType) {
        this.name = Intercom.sanitize(newName, 20);
        this.type = newType;
        this.setChanged();
    }

    public void openConfig(ServerPlayer player) {
        ModNetwork.sendTo(player, new DeviceConfigOpenPacket(this.worldPosition, this.name, this.getDeviceType().ordinal(), this.isOn()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.name = tag.getString("Name");
        this.type = tag.contains("Type") ? DeviceType.byId(tag.getInt("Type")) : null;
        this.setTemp = tag.getInt("SetTemp");
        this.fanLevel = tag.contains("Fan") ? tag.getInt("Fan") : 1;
        this.away = tag.getBoolean("Away");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Name", this.name);
        if (this.type != null) {
            tag.putInt("Type", this.type.ordinal());
        }

        tag.putInt("SetTemp", this.setTemp);
        tag.putInt("Fan", this.fanLevel);
        tag.putBoolean("Away", this.away);
    }
}
