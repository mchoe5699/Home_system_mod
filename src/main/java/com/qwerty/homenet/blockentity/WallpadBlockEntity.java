package com.qwerty.homenet.blockentity;

import com.qwerty.homenet.block.DeviceType;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.intercom.CallState;
import com.qwerty.homenet.intercom.Intercom;
import com.qwerty.homenet.intercom.MissedCall;
import com.qwerty.homenet.network.DeviceEntry;
import com.qwerty.homenet.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class WallpadBlockEntity extends ReceiverBlockEntity {
    public static final int MAX_DEVICES = 32;
    public static final int LINK_RANGE = 64;
    public static final int MAX_VISITORS = 64;
    public static final String MASTER_PASSWORD = "15770051";
    public static final String DEFAULT_PASSWORD = "9999";
    public static final int ENERGY_KINDS = 5;
    private static final Map<String, String> DEFAULTS = new LinkedHashMap<>();
    private static final Map<String, String> PATTERNS = new HashMap<>();
    private final List<BlockPos> devices = new ArrayList<>();
    private final Map<String, String> settings = new HashMap<>();
    private final List<MissedCall> visitorLog = new ArrayList<>();
    private final List<MissedCall> parcels = new ArrayList<>();
    private boolean emergency;
    private boolean outing;
    private int alarmTest;
    private final long[] energyCur = new long[5];
    private final long[] energyPrev = new long[5];
    private final long[] energyPrev2 = new long[5];
    private long energyMonth = -1L;
    private int overNotified;

    private static void def(String key, String value, String pattern) {
        DEFAULTS.put(key, value);
        PATTERNS.put(key, pattern);
    }

    public WallpadBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WALLPAD.get(), pos, state);
    }

    @Override
    public DeviceRegistry.Kind kind() {
        return DeviceRegistry.Kind.WALLPAD;
    }

    public String setting(String key) {
        String v = this.settings.get(key);
        return v != null ? v : DEFAULTS.getOrDefault(key, "");
    }

    private int settingInt(String key) {
        try {
            return Integer.parseInt(this.setting(key));
        } catch (NumberFormatException var3) {
            return 0;
        }
    }

    public WallpadBlockEntity.LinkResult toggleDevice(BlockPos devicePos) {
        if (this.devices.remove(devicePos)) {
            this.setChanged();
            return WallpadBlockEntity.LinkResult.UNLINKED;
        } else if (this.devices.size() >= 32) {
            return WallpadBlockEntity.LinkResult.FULL;
        } else if (!devicePos.closerThan(this.worldPosition, 64.0)) {
            return WallpadBlockEntity.LinkResult.TOO_FAR;
        } else {
            this.devices.add(devicePos.immutable());
            this.setChanged();
            return WallpadBlockEntity.LinkResult.LINKED;
        }
    }

    @Nullable
    private DeviceBlockEntity device(BlockPos pos) {
        if (this.level != null && this.level.isLoaded(pos)) {
            return this.level.getBlockEntity(pos) instanceof DeviceBlockEntity d ? d : null;
        } else {
            return null;
        }
    }

    private void pruneDevices() {
        if (this.level != null) {
            boolean changed = false;
            Iterator<BlockPos> it = this.devices.iterator();

            while (it.hasNext()) {
                BlockPos p = it.next();
                if (this.level.isLoaded(p) && !(this.level.getBlockEntity(p) instanceof DeviceBlockEntity)) {
                    it.remove();
                    changed = true;
                }
            }

            if (changed) {
                this.setChanged();
            }
        }
    }

    @Override
    protected List<DeviceEntry> deviceEntries() {
        this.pruneDevices();
        List<DeviceEntry> out = new ArrayList<>();

        for (BlockPos p : this.devices) {
            DeviceBlockEntity d = this.device(p);
            if (d != null) {
                out.add(
                    new DeviceEntry(
                        p, d.getDeviceName(), d.getDeviceType().ordinal(), d.isOn(), true, d.getSetTemp(), d.roomTemp(), d.getFanLevel(), d.isAway()
                    )
                );
            } else {
                out.add(new DeviceEntry(p, "", DeviceType.OTHER.ordinal(), false, false));
            }
        }

        return out;
    }

    @Override
    protected List<MissedCall> visitors() {
        return new ArrayList<>(this.visitorLog);
    }

    @Override
    protected Map<String, String> settingsForClient() {
        Map<String, String> m = new HashMap<>(DEFAULTS);
        m.putAll(this.settings);
        m.remove("admin_pw");
        m.put("emergency", this.emergency ? "1" : "0");
        m.put("outing", this.outing ? "1" : "0");
        m.put("alarm", !this.emergency && this.alarmTest <= 0 ? "0" : "1");
        m.put("pw_default", this.doorPassword.isEmpty() ? "1" : "0");
        return m;
    }

    @Override
    protected long[] energyForClient() {
        long[] out = new long[15];
        System.arraycopy(this.energyCur, 0, out, 0, 5);
        System.arraycopy(this.energyPrev, 0, out, 5, 5);
        System.arraycopy(this.energyPrev2, 0, out, 10, 5);
        return out;
    }

    @Override
    protected void handleExtra(ServerPlayer player, ReceiverBlockEntity.Action action, BlockPos target, String text) {
        String t = text == null ? "" : text.trim();
        switch (action) {
            case TOGGLE:
                DeviceBlockEntity dxxxx = this.linked(target);
                if (dxxxx != null) {
                    dxxxx.setOn(!dxxxx.isOn());
                }
                break;
            case SET_POWER:
                DeviceBlockEntity dxxx = this.linked(target);
                if (dxxx != null) {
                    dxxx.setOn("1".equals(t));
                }
                break;
            case SET_TEMP:
                DeviceBlockEntity dxx = this.linked(target);
                if (dxx != null) {
                    dxx.adjustSetTemp(t.startsWith("-") ? -1 : 1);
                }
                break;
            case SET_LEVEL:
                DeviceBlockEntity dx = this.linked(target);
                if (dx != null) {
                    try {
                        dx.setFanLevel(Integer.parseInt(t));
                        dx.setOn(true);
                    } catch (NumberFormatException var13) {
                    }
                }
                break;
            case SET_AWAY:
                DeviceBlockEntity d = this.linked(target);
                if (d != null) {
                    d.setAway("1".equals(t));
                }
                break;
            case ALL_LIGHTS_OFF:
            case ALL_LIGHTS_ON:
                boolean on = action == ReceiverBlockEntity.Action.ALL_LIGHTS_ON;

                for (BlockPos p : this.devices) {
                    DeviceBlockEntity dxxxxx = this.device(p);
                    if (dxxxxx != null && dxxxxx.getDeviceType() == DeviceType.LIGHT) {
                        dxxxxx.setOn(on);
                    }
                }

                if (!on) {
                    player.displayClientMessage(Component.translatable("msg.qwertys_homenet.all_lights_off"), true);
                }
                break;
            case ALL_OFF:
                for (BlockPos px : this.devices) {
                    DeviceBlockEntity dxxxxx = this.device(px);
                    if (dxxxxx != null) {
                        dxxxxx.setOn(false);
                    }
                }

                player.displayClientMessage(Component.translatable("msg.qwertys_homenet.all_off"), true);
                break;
            case SET_SETTING:
                int eq = t.indexOf(61);
                if (eq <= 0) {
                    return;
                }

                String key = t.substring(0, eq);
                String value = t.substring(eq + 1).toUpperCase(Locale.ROOT);
                String pattern = PATTERNS.get(key);
                if (pattern == null || key.equals("admin_pw") || !value.matches(pattern)) {
                    return;
                }

                this.settings.put(key, value);
                if (key.startsWith("target_")) {
                    this.overNotified = 0;
                }

                this.setChanged();
                break;
            case RESET_SOUND:
                for (String k : new String[]{"notify_notice", "notify_parking", "notify_parcel", "touch_sound", "vol_call", "vol_ring", "vol_system"}) {
                    this.settings.remove(k);
                }

                this.setChanged();
                break;
            case EMERGENCY:
                this.setEmergency(player, !this.emergency);
                break;
            case ALARM_STOP:
                if (this.emergency) {
                    this.setEmergency(player, false);
                }

                this.alarmTest = 0;
                break;
            case ALARM_TEST:
                this.alarmTest = 60;
                break;
            case OUTING:
                this.outing = !this.outing;
                this.setChanged();
                player.displayClientMessage(Component.translatable("msg.qwertys_homenet" + (this.outing ? ".outing_on" : ".outing_off")), true);
                if (this.level != null) {
                    this.level.playSound(null, this.worldPosition, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.5F, this.outing ? 1.4F : 1.0F);
                }
                break;
            case SAVE_VISITOR:
                if (this.callState != CallState.IDLE && !this.outgoing && !Intercom.isReceiverKey(this.incomingKey)) {
                    this.addVisitor(this.incomingKey);
                    this.noticeTo(player, "visitor_saved");
                }
                break;
            case DELETE_VISITOR:
                try {
                    int i = Integer.parseInt(t);
                    if (i >= 0 && i < this.visitorLog.size()) {
                        this.visitorLog.remove(i);
                    }

                    this.setChanged();
                } catch (NumberFormatException var12) {
                }
                break;
            case CLEAR_VISITORS:
                this.visitorLog.clear();
                this.setChanged();
                break;
            case UNLOCK:
                int c = t.indexOf(58);
                String scope = c < 0 ? "" : t.substring(0, c);
                String pw = c < 0 ? t : t.substring(c + 1);

                boolean ok = switch (scope) {
                    case "pw" -> this.checkSettingsPassword(pw);
                    case "admin" -> this.checkSettingsPassword(pw);
                    default -> false;
                };
                this.noticeTo(player, ok ? "unlock_" + scope : "unlock_fail");
        }
    }

    @Nullable
    private DeviceBlockEntity linked(BlockPos target) {
        return this.devices.contains(target) ? this.device(target) : null;
    }

    private boolean checkSettingsPassword(String pw) {
        return pw.equals(this.doorPassword.isEmpty() ? "9999" : this.doorPassword) || pw.equals("15770051");
    }

    @Override
    protected void setDoorPassword(ServerPlayer player, String text) {
        String s = text == null ? "" : text.trim();
        int bar = s.indexOf(124);
        if (bar >= 0 && this.checkSettingsPassword(s.substring(0, bar))) {
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

    private void setEmergency(ServerPlayer player, boolean on) {
        if (this.level instanceof ServerLevel sl) {
            this.emergency = on;
            this.setChanged();
            if (on) {
                Set<BlockPos> guards = new LinkedHashSet<>(DeviceRegistry.get(sl).guards(sl, this.worldPosition, false));
                guards.addAll(DeviceRegistry.get(sl).guards(sl, this.worldPosition, true));

                for (BlockPos p : guards) {
                    ReceiverBlockEntity r = this.receiverAt(p);
                    if (r != null) {
                        r.addAlert(this.unit);
                    }
                }

                player.displayClientMessage(Component.translatable("msg.qwertys_homenet.emergency_on").withStyle(ChatFormatting.RED), true);
            } else {
                player.displayClientMessage(Component.translatable("msg.qwertys_homenet.emergency_off"), true);
            }
        }
    }

    @Override
    protected void onIncoming(String key) {
        if (!Intercom.isReceiverKey(key)) {
            this.addVisitor(key);
        }
    }

    private void addVisitor(String key) {
        this.visitorLog.add(0, new MissedCall(key, System.currentTimeMillis()));

        while (this.visitorLog.size() > 64) {
            this.visitorLog.remove(this.visitorLog.size() - 1);
        }

        this.setChanged();
    }

    public void addParcel(String guardLabel) {
        this.parcels.add(0, new MissedCall(guardLabel + "|0", System.currentTimeMillis()));

        while (this.parcels.size() > 30) {
            this.parcels.remove(this.parcels.size() - 1);
        }

        this.setChanged();
        this.pendingNotice = "parcel_arrived";
        this.syncScreens();
        if (this.level != null) {
            this.level.playSound(null, this.worldPosition, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.8F, 1.5F);
        }
    }

    @Override
    protected List<MissedCall> records() {
        List<MissedCall> out = new ArrayList<>();

        for (MissedCall m : this.parcels) {
            out.add(new MissedCall("parcel|" + m.caller(), m.dayTime()));
        }

        return out;
    }

    @Override
    protected String preferredGuard() {
        return this.setting("guard_no");
    }

    @Override
    protected float ringVolume() {
        return this.settingInt("vol_ring") / 10.0F * 1.2F;
    }

    @Override
    protected void tickExtra() {
        if (this.level instanceof ServerLevel sl) {
            long var10 = sl.getGameTime();
            if (this.emergency || this.alarmTest > 0) {
                if (this.alarmTest > 0) {
                    this.alarmTest--;
                }

                if (var10 % 10L == 0L) {
                    float pitch = var10 / 10L % 2L == 0L ? 1.6F : 1.1F;
                    sl.playSound(null, this.worldPosition, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.BLOCKS, 1.0F, pitch);
                }

                if (this.alarmTest == 0 && !this.emergency && var10 % 10L == 0L) {
                    this.syncScreens();
                }
            }

            if (Math.floorMod(var10 + this.worldPosition.hashCode(), 20) == 0) {
                long month = sl.getDayTime() / 24000L / 30L;
                if (this.energyMonth < 0L) {
                    this.energyMonth = month;
                }

                if (month != this.energyMonth) {
                    long diff = Math.min(3L, Math.max(1L, month - this.energyMonth));

                    for (long i = 0L; i < diff; i++) {
                        System.arraycopy(this.energyPrev, 0, this.energyPrev2, 0, 5);
                        System.arraycopy(this.energyCur, 0, this.energyPrev, 0, 5);
                        Arrays.fill(this.energyCur, 0L);
                    }

                    this.energyMonth = month;
                    this.overNotified = 0;
                }

                for (BlockPos p : this.devices) {
                    DeviceBlockEntity d = this.device(p);
                    if (d != null && d.isOn()) {
                        switch (d.getDeviceType()) {
                            case LIGHT:
                                this.energyCur[0] = this.energyCur[0] + 6L;
                                break;
                            case OUTLET:
                            case OTHER:
                            case DOOR_LOCK:
                            case CURTAIN:
                                this.energyCur[0] = this.energyCur[0] + 10L;
                                break;
                            case VENT:
                                this.energyCur[0] = this.energyCur[0] + 3L * d.getFanLevel();
                                break;
                            case AIRCON:
                                this.energyCur[0] = this.energyCur[0] + 30L;
                                break;
                            case HEATING:
                                if (!d.isAway()) {
                                    this.energyCur[4] = this.energyCur[4] + 40L;
                                    this.energyCur[2] = this.energyCur[2] + 2L;
                                } else {
                                    this.energyCur[4] = this.energyCur[4] + 8L;
                                }
                                break;
                            case GAS:
                                this.energyCur[2]++;
                        }
                    }
                }

                if (!Intercom.playersNear(sl, this.worldPosition, 12.0).isEmpty()) {
                    this.energyCur[1] = this.energyCur[1] + 3L;
                    this.energyCur[3]++;
                }

                for (int i = 0; i < 5; i++) {
                    int target = this.settingInt("target_" + i);
                    if (target > 0 && this.energyCur[i] / 1000L >= target && (this.overNotified & 1 << i) == 0) {
                        this.overNotified |= 1 << i;
                        this.pendingNotice = "energy_over:" + i;
                        this.syncScreens();
                    }
                }

                this.setChanged();
            }
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.devices.clear();

        for (long l : tag.getLongArray("Devices")) {
            this.devices.add(BlockPos.of(l));
        }

        this.settings.clear();
        CompoundTag s = tag.getCompound("Settings");

        for (String k : s.getAllKeys()) {
            if (DEFAULTS.containsKey(k)) {
                this.settings.put(k, s.getString(k));
            }
        }

        this.visitorLog.clear();
        ListTag v = tag.getList("Visitors", 10);

        for (int i = 0; i < v.size(); i++) {
            this.visitorLog.add(MissedCall.load(v.getCompound(i)));
        }

        this.parcels.clear();
        ListTag pl = tag.getList("Parcels", 10);

        for (int i = 0; i < pl.size(); i++) {
            this.parcels.add(MissedCall.load(pl.getCompound(i)));
        }

        this.emergency = tag.getBoolean("Emergency");
        this.outing = tag.getBoolean("Outing");
        copy(tag.getLongArray("EnergyCur"), this.energyCur);
        copy(tag.getLongArray("EnergyPrev"), this.energyPrev);
        copy(tag.getLongArray("EnergyPrev2"), this.energyPrev2);
        this.energyMonth = tag.contains("EnergyMonth") ? tag.getLong("EnergyMonth") : -1L;
        this.overNotified = tag.getInt("OverNotified");
    }

    private static void copy(long[] from, long[] to) {
        Arrays.fill(to, 0L);
        System.arraycopy(from, 0, to, 0, Math.min(from.length, to.length));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Devices", new LongArrayTag(this.devices.stream().mapToLong(BlockPos::asLong).toArray()));
        CompoundTag s = new CompoundTag();
        this.settings.forEach(s::putString);
        tag.put("Settings", s);
        ListTag v = new ListTag();

        for (MissedCall m : this.visitorLog) {
            v.add(m.save());
        }

        tag.put("Visitors", v);
        ListTag pl = new ListTag();

        for (MissedCall m : this.parcels) {
            pl.add(m.save());
        }

        tag.put("Parcels", pl);
        tag.putBoolean("Emergency", this.emergency);
        tag.putBoolean("Outing", this.outing);
        tag.put("EnergyCur", new LongArrayTag((long[])this.energyCur.clone()));
        tag.put("EnergyPrev", new LongArrayTag((long[])this.energyPrev.clone()));
        tag.put("EnergyPrev2", new LongArrayTag((long[])this.energyPrev2.clone()));
        tag.putLong("EnergyMonth", this.energyMonth);
        tag.putInt("OverNotified", this.overNotified);
    }

    static {
        def("notify_notice", "1", "[01]");
        def("notify_parking", "0", "[01]");
        def("notify_parcel", "0", "[01]");
        def("touch_sound", "1", "[01]");
        def("vol_call", "10", "[1-9]|10");
        def("vol_ring", "5", "[1-9]|10");
        def("vol_system", "5", "[1-9]|10");

        for (int i = 1; i <= 5; i++) {
            def("sms" + i, "", "\\d{0,15}");
        }

        def("sms_on", "", "[01]{0,10}");

        for (int i = 1; i <= 3; i++) {
            def("ars_phone" + i, "", "\\d{0,15}");
        }

        def("ars_pw", "FFFF", "[0-9A-F]{1,8}");
        def("ars_auto", "1", "[01]");
        def("ars_use", "1", "[01]");
        def("ars_rings", "10", "[1-9]|1\\d|20");
        def("sec1", "0", "[01]");
        def("sec2", "0", "[01]");

        for (int i = 0; i < 5; i++) {
            def("target_" + i, "0", "\\d{1,6}");
        }

        def("admin_pw", "0000", "\\d{4,8}");
        def("guard_no", "", "\\d{0,6}");
    }

    public static enum LinkResult {
        LINKED,
        UNLINKED,
        FULL,
        TOO_FAR;
    }
}
