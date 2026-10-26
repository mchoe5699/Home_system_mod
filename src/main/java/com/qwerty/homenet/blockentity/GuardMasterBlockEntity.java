package com.qwerty.homenet.blockentity;

import com.qwerty.homenet.block.GuardMasterBlock;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.intercom.CallState;
import com.qwerty.homenet.intercom.Intercom;
import com.qwerty.homenet.intercom.MissedCall;
import com.qwerty.homenet.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class GuardMasterBlockEntity extends ReceiverBlockEntity {
    public static final String ADMIN_PASSWORD = "56266";
    public static final int MAX_HISTORY = 40;
    public static final int MAX_ALERTS = 100;
    public static final int MAX_PARCELS = 40;
    public static final int MIN_ANGLE = 0;
    public static final int MAX_ANGLE = 60;
    public static final int DEFAULT_ANGLE = 22;
    private int angle = 22;
    private boolean absent;
    private String absentFwd = "";
    private String busyFwd = "";
    private final Map<String, String> settings = new HashMap<>();
    private final List<MissedCall> history = new ArrayList<>();
    private final List<MissedCall> alerts = new ArrayList<>();
    private final List<MissedCall> parcels = new ArrayList<>();

    public GuardMasterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GUARD_MASTER.get(), pos, state);
        this.unit = "1";
    }

    @Override
    public DeviceRegistry.Kind kind() {
        return DeviceRegistry.Kind.GUARD_CONSOLE;
    }

    @Override
    public String callerKey() {
        return "guard:" + this.guardLabel();
    }

    public String guardLabel() {
        String d = DeviceRegistry.digits(this.unit);
        if (d.isEmpty()) {
            return this.unit;
        } else {
            return d.equals("99") ? "통합관리실" : d + "번 경비실";
        }
    }

    @Nullable
    @Override
    protected BlockPos ring(BlockPos from, String key, int depth) {
        if (this.level instanceof ServerLevel sl) {
            String to = this.absent ? this.absentFwd : (this.isBusy() ? this.busyFwd : "");
            if (!to.isEmpty() && depth < 4) {
                for (BlockPos p : DeviceRegistry.get(sl).guardsByNumber(sl, this.worldPosition, to)) {
                    if (!p.equals(this.worldPosition) && !p.equals(from)) {
                        ReceiverBlockEntity r = this.receiverAt(p);
                        BlockPos at = r == null ? null : r.ring(from, key, depth + 1);
                        if (at != null) {
                            this.addHistory("fwd|" + key);
                            return at;
                        }
                    }
                }
            }

            if (!this.absent) {
                return super.ring(from, key, depth);
            } else {
                this.addHistory("miss|" + key);
                this.missed.add(0, new MissedCall(key, sl.getDayTime()));

                while (this.missed.size() > 6) {
                    this.missed.remove(this.missed.size() - 1);
                }

                this.setChanged();
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    protected void onIncoming(String key) {
        this.addHistory("in|" + key);
    }

    @Override
    protected void onCallStarted(String label) {
        this.addHistory("out|" + label);
    }

    @Override
    protected void addMissed() {
        super.addMissed();
        this.addHistory("miss|" + this.incomingKey);
    }

    @Override
    public void addAlert(String fromUnit) {
        this.alerts.add(0, new MissedCall(fromUnit + "|0", System.currentTimeMillis()));

        while (this.alerts.size() > 100) {
            this.alerts.remove(this.alerts.size() - 1);
        }

        super.addAlert(fromUnit);
    }

    private void addHistory(String entry) {
        this.history.add(0, new MissedCall(entry.length() > 60 ? entry.substring(0, 60) : entry, System.currentTimeMillis()));

        while (this.history.size() > 40) {
            this.history.remove(this.history.size() - 1);
        }

        this.setChanged();
    }

    @Override
    protected Map<String, String> settingsForClient() {
        Map<String, String> m = new HashMap<>();
        m.put("vol_system", "5");
        m.put("vol_call", "8");
        m.putAll(this.settings);
        m.put("absent", this.absent ? "1" : "0");
        m.put("absent_fwd", this.absentFwd);
        m.put("busy_fwd", this.busyFwd);
        m.put("guard_label", this.guardLabel());
        m.put("pw_default", this.doorPassword.isEmpty() ? "1" : "0");
        return m;
    }

    @Override
    protected List<MissedCall> records() {
        List<MissedCall> out = new ArrayList<>();

        for (MissedCall m : this.history) {
            out.add(new MissedCall("hist|" + m.caller(), m.dayTime()));
        }

        for (MissedCall m : this.alerts) {
            out.add(new MissedCall("alert|" + m.caller(), m.dayTime()));
        }

        for (MissedCall m : this.parcels) {
            out.add(new MissedCall("parcel|" + m.caller(), m.dayTime()));
        }

        return out;
    }

    @Override
    protected void handleExtra(ServerPlayer player, ReceiverBlockEntity.Action action, BlockPos target, String text) {
        String t = text == null ? "" : text.trim();
        String num = t.replaceAll("[^0-9]", "");
        switch (action) {
            case PICKUP:
                if (this.callState == CallState.RINGING && !this.outgoing) {
                    this.answer();
                } else if (this.callState != CallState.IDLE) {
                    this.hangUp();
                }
                break;
            case GUARD_DOOR:
                if (this.callState == CallState.CONNECTED && !this.outgoing && !Intercom.isReceiverKey(this.incomingKey)) {
                    this.openDoor();
                } else if (this.level != null) {
                    BlockState s = this.getBlockState();
                    if (s.hasProperty(GuardMasterBlock.POWERED)) {
                        this.level.setBlock(this.worldPosition, s.setValue(GuardMasterBlock.POWERED, true), 3);
                        this.level.scheduleTick(this.worldPosition, s.getBlock(), 60);
                    }

                    this.level.playSound(null, this.worldPosition, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.5F, 1.6F);
                }
                break;
            case ABSENT_SET:
                if (num.equals(DeviceRegistry.digits(this.unit))) {
                    this.noticeTo(player, "fwd_self");
                    return;
                }

                this.absent = true;
                this.absentFwd = num;
                this.setChanged();
                this.noticeTo(player, "absent_on");
                break;
            case ABSENT_CLEAR:
                this.absent = false;
                this.setChanged();
                this.noticeTo(player, "absent_off");
                break;
            case BUSY_FWD_SET:
                if (!num.isEmpty() && !num.equals(DeviceRegistry.digits(this.unit))) {
                    this.busyFwd = num;
                    this.setChanged();
                    this.noticeTo(player, "busy_fwd_on");
                    break;
                }

                this.noticeTo(player, num.isEmpty() ? "fwd_need" : "fwd_self");
                return;
            case BUSY_FWD_CLEAR:
                this.busyFwd = "";
                this.setChanged();
                this.noticeTo(player, "busy_fwd_off");
                break;
            case CONFIRM_ALERT:
                int ix = parse(t);
                if (ix >= 0 && ix < this.alerts.size()) {
                    MissedCall m = this.alerts.get(ix);
                    String u = m.caller().substring(0, Math.max(0, m.caller().lastIndexOf(124)));
                    this.alerts.set(ix, new MissedCall(u + "|1", m.dayTime()));
                    this.setChanged();
                }
                break;
            case CLEAR_RECORDS:
                switch (t) {
                    case "hist":
                        this.history.clear();
                        break;
                    case "alert":
                        this.alerts.clear();
                        break;
                    case "parcel":
                        this.parcels.clear();
                }

                this.setChanged();
                break;
            case PARCEL_ADD:
                this.addParcel(player, t);
                break;
            case PARCEL_DONE:
                int i = parse(t);
                if (i >= 0 && i < this.parcels.size()) {
                    MissedCall m = this.parcels.get(i);
                    String u = m.caller().substring(0, Math.max(0, m.caller().lastIndexOf(124)));
                    boolean done = m.caller().endsWith("|1");
                    this.parcels.set(i, new MissedCall(u + (done ? "|0" : "|1"), m.dayTime()));
                    this.setChanged();
                }
                break;
            case SET_SETTING:
                int eq = t.indexOf(61);
                if (eq <= 0) {
                    return;
                }

                String k = t.substring(0, eq);
                String v = t.substring(eq + 1);
                if ((k.equals("vol_system") || k.equals("vol_call")) && v.matches("[1-9]|10")) {
                    this.settings.put(k, v);
                    this.setChanged();
                }
                break;
            case RESET_SOUND:
                this.settings.remove("vol_system");
                this.settings.remove("vol_call");
                this.setChanged();
                break;
            case UNLOCK:
                boolean ok = t.equals("admin:56266") || t.equals("pw:" + (this.doorPassword.isEmpty() ? "9999" : this.doorPassword));
                this.noticeTo(player, ok ? (t.startsWith("admin:") ? "unlock_admin" : "unlock_pw") : "unlock_fail");
        }
    }

    private static int parse(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException var2) {
            return -1;
        }
    }

    @Override
    public void setUnit(ServerPlayer player, String raw) {
        String d = raw == null ? "" : raw.replaceAll("[^0-9]", "");
        if (!d.isEmpty() && d.length() <= 2) {
            super.setUnit(player, String.valueOf(Integer.parseInt(d)));
        } else {
            this.noticeTo(player, "addr_invalid");
        }
    }

    @Override
    protected void setDoorPassword(ServerPlayer player, String text) {
        String s = text == null ? "" : text.trim();
        int bar = s.indexOf(124);
        String cur = this.doorPassword.isEmpty() ? "9999" : this.doorPassword;
        if (bar >= 0 && s.substring(0, bar).equals(cur)) {
            String pw = s.substring(bar + 1);
            if (!pw.matches("\\d{4}")) {
                this.noticeTo(player, "pw_invalid");
            } else {
                this.doorPassword = pw.equals("9999") ? "" : pw;
                this.setChanged();
                this.noticeTo(player, "pw_saved");
            }
        } else {
            this.noticeTo(player, "unlock_fail");
        }
    }

    private void addParcel(ServerPlayer player, String raw) {
        String u = Intercom.sanitize(raw, 16);
        if (this.level instanceof ServerLevel sl && !DeviceRegistry.normalize(u).isEmpty()) {
            this.parcels.add(0, new MissedCall(u + "|0", System.currentTimeMillis()));

            while (this.parcels.size() > 40) {
                this.parcels.remove(this.parcels.size() - 1);
            }

            this.setChanged();
            int sent = 0;

            for (BlockPos p : DeviceRegistry.get(sl).receivers(sl, this.worldPosition, u)) {
                if (this.receiverAt(p) instanceof WallpadBlockEntity w) {
                    w.addParcel(this.guardLabel());
                    sent++;
                }
            }

            this.noticeTo(player, sent > 0 ? "parcel_sent" : "parcel_saved");
        } else {
            this.noticeTo(player, "bad_unit");
        }
    }

    @Override
    protected float ringVolume() {
        try {
            return Integer.parseInt(this.settings.getOrDefault("vol_system", "5")) / 10.0F * 1.2F;
        } catch (NumberFormatException var2) {
            return 0.6F;
        }
    }

    @Override
    protected void tickExtra() {
        if (this.level != null && this.level.getGameTime() % 5L == 0L) {
            BlockState s = this.getBlockState();
            boolean off = this.callState == CallState.CONNECTED || this.callState == CallState.DIALING;
            if (s.hasProperty(GuardMasterBlock.OFFHOOK) && s.getValue(GuardMasterBlock.OFFHOOK) != off) {
                this.level.setBlock(this.worldPosition, s.setValue(GuardMasterBlock.OFFHOOK, off), 2);
            }
        }
    }

    public int getAngle() {
        return this.angle;
    }

    public void adjustAngle(ServerPlayer player, int delta) {
        int a = Math.max(0, Math.min(60, this.angle + delta));
        if (a != this.angle && this.level != null) {
            this.angle = a;
            this.setChanged();
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 2);
            this.level.playSound(null, this.worldPosition, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.4F, 1.4F);
        }

        player.displayClientMessage(Component.translatable("msg.qwertys_homenet.guard_angle", this.angle), true);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putInt("Angle", this.angle);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        if (tag.contains("Angle")) {
            this.angle = tag.getInt("Angle");
        }
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null && tag.contains("Angle")) {
            this.angle = tag.getInt("Angle");
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.angle = tag.contains("Angle") ? tag.getInt("Angle") : 22;
        this.absent = tag.getBoolean("Absent");
        this.absentFwd = tag.getString("AbsentFwd");
        this.busyFwd = tag.getString("BusyFwd");
        this.settings.clear();
        CompoundTag s = tag.getCompound("Settings");

        for (String k : s.getAllKeys()) {
            this.settings.put(k, s.getString(k));
        }

        readList(tag, "History", this.history);
        readList(tag, "Alerts", this.alerts);
        readList(tag, "Parcels", this.parcels);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Angle", this.angle);
        tag.putBoolean("Absent", this.absent);
        tag.putString("AbsentFwd", this.absentFwd);
        tag.putString("BusyFwd", this.busyFwd);
        CompoundTag s = new CompoundTag();
        this.settings.forEach(s::putString);
        tag.put("Settings", s);
        writeList(tag, "History", this.history);
        writeList(tag, "Alerts", this.alerts);
        writeList(tag, "Parcels", this.parcels);
    }

    private static void readList(CompoundTag tag, String key, List<MissedCall> out) {
        out.clear();
        ListTag l = tag.getList(key, 10);

        for (int i = 0; i < l.size(); i++) {
            out.add(MissedCall.load(l.getCompound(i)));
        }
    }

    private static void writeList(CompoundTag tag, String key, List<MissedCall> in) {
        ListTag l = new ListTag();

        for (MissedCall m : in) {
            l.add(m.save());
        }

        tag.put(key, l);
    }
}
