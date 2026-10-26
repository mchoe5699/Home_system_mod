package com.qwerty.homenet.kvm;

import com.qwerty.homenet.blockentity.Kvm906BlockEntity;
import com.qwerty.homenet.data.ZoneData;
import com.qwerty.homenet.intercom.Intercom;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class KvmTelephone {
    private final Kvm906BlockEntity owner;
    private String number = "";
    private String buffer = "";
    private String lastDial = "";
    private boolean held;
    private BlockPos peer;
    private KvmTelephone.State state = KvmTelephone.State.IDLE;
    private boolean active;
    private boolean pulse;
    private int ticks;
    private int dialWait = -1;

    public boolean held() {
        return this.held;
    }

    public void hold() {
        if (this.owner.design().model().is932() && this.owner.offHook()) {
            if (this.held) {
                this.held = false;
                this.select();
            } else if (this.state == KvmTelephone.State.CONNECTED) {
                this.held = true;
                this.active = false;
                this.update();
            }
        }
    }

    public boolean redial(ServerPlayer player) {
        return !this.lastDial.isEmpty() && this.dial(player, this.lastDial);
    }

    public boolean pulse() {
        return this.pulse;
    }

    public boolean setMode(String mode) {
        if (this.owner.design().model().keypad() && !this.busy() && (mode.equals("tone") || mode.equals("pulse"))) {
            this.pulse = mode.equals("pulse");
            this.update();
            return true;
        } else {
            return false;
        }
    }

    public KvmTelephone(Kvm906BlockEntity owner) {
        this.owner = owner;
    }

    public String number() {
        return this.number;
    }

    public String buffer() {
        return this.buffer;
    }

    public KvmTelephone.State state() {
        return this.state;
    }

    public boolean active() {
        return this.active;
    }

    public boolean busy() {
        return this.state != KvmTelephone.State.IDLE;
    }

    private ServerLevel level() {
        return this.owner.getLevel() instanceof ServerLevel level ? level : null;
    }

    private Kvm906BlockEntity other() {
        ServerLevel level = this.level();
        return this.peer != null
                && level != null
                && ZoneData.connected(level, this.owner.getBlockPos(), this.peer)
                && level.isLoaded(this.peer)
                && level.getBlockEntity(this.peer) instanceof Kvm906BlockEntity be
                && be.design().model().keypad()
            ? be
            : null;
    }

    private void update() {
        this.owner.telephoneUpdated();
    }

    public void register() {
        if (this.level() != null && this.owner.design().model().keypad() && !this.number.isEmpty()) {
            KvmPhoneDirectory.get(this.level()).claim(this.number, this.owner.getBlockPos());
        }
    }

    public boolean setNumber(String raw) {
        String value = raw == null ? "" : raw.strip();
        if (this.level() != null && this.owner.design().model().keypad() && !this.busy() && value.matches("[0-9]{0,16}")) {
            if (!KvmPhoneDirectory.get(this.level()).claim(value, this.owner.getBlockPos())) {
                return false;
            } else {
                this.number = value;
                this.update();
                return true;
            }
        } else {
            return false;
        }
    }

    public void modelChanged() {
        this.hangUp();
        if (this.level() != null) {
            KvmPhoneDirectory.get(this.level()).release(this.owner.getBlockPos());
        }

        this.register();
    }

    public void broken() {
        this.hangUp();
        if (this.level() != null) {
            KvmPhoneDirectory.get(this.level()).release(this.owner.getBlockPos());
        }
    }

    public void reset() {
        this.peer = null;
        this.state = KvmTelephone.State.IDLE;
        this.active = false;
        this.held = false;
        this.ticks = 0;
        this.dialWait = -1;
        this.buffer = "";
    }

    public void hangUp() {
        Kvm906BlockEntity other = this.other();
        if (other != null && this.owner.getBlockPos().equals(other.telephone().peer)) {
            other.telephone().reset();
            other.telephone().update();
        }

        this.reset();
        this.update();
    }

    private void tone(float pitch) {
        if (this.level() != null) {
            this.level()
                .playSound(
                    null,
                    this.owner.getBlockPos(),
                    this.pulse ? SoundEvents.NOTE_BLOCK_HAT.value() : SoundEvents.NOTE_BLOCK_BIT.value(),
                    SoundSource.BLOCKS,
                    0.3F,
                    pitch
                );
        }
    }

    public void key(ServerPlayer player, String key) {
        if (this.owner.design().model().keypad() && this.owner.power() && this.owner.offHook() && key.matches("[0-9*#]")) {
            this.tone(0.65F + "123456789*0#".indexOf(key) * 0.08F);
            if (this.state != KvmTelephone.State.CONNECTED) {
                if (this.state == KvmTelephone.State.IDLE) {
                    if (key.equals("*")) {
                        if (!this.buffer.isEmpty()) {
                            this.buffer = this.buffer.substring(0, this.buffer.length() - 1);
                        }

                        this.dialWait = -1;
                    } else if (key.equals("#")) {
                        this.dial(player, this.buffer);
                    } else if (this.buffer.length() < 16) {
                        this.buffer = this.buffer + key;
                        this.dialWait = 20;
                        this.active = true;
                    }

                    this.update();
                }
            }
        }
    }

    public boolean dial(ServerPlayer player, String raw) {
        if (this.level() != null && this.owner.design().model().keypad() && this.owner.power() && this.owner.offHook() && !this.busy()) {
            String target = raw == null ? "" : raw.strip();
            this.dialWait = -1;
            if (!target.matches("[0-9]{1,16}")) {
                return false;
            } else {
                BlockPos pos = KvmPhoneDirectory.get(this.level()).find(target);
                if (pos != null
                    && ZoneData.connected(this.level(), this.owner.getBlockPos(), pos)
                    && !pos.equals(this.owner.getBlockPos())
                    && this.level().isLoaded(pos)
                    && this.level().getBlockEntity(pos) instanceof Kvm906BlockEntity receiver
                    && receiver.design().model().keypad()
                    && receiver.power()
                    && receiver.telephone().number.equals(target)) {
                    KvmTelephone telephone = receiver.telephone();
                    if (telephone.busy()) {
                        if (player != null) {
                            player.displayClientMessage(Component.literal("국선: 통화 중입니다."), true);
                        }

                        return false;
                    } else {
                        this.lastDial = target;
                        this.held = false;
                        this.peer = pos.immutable();
                        this.buffer = target;
                        this.state = KvmTelephone.State.DIALING;
                        this.active = true;
                        this.ticks = 0;
                        telephone.peer = this.owner.getBlockPos().immutable();
                        telephone.state = KvmTelephone.State.RINGING;
                        telephone.ticks = 0;
                        telephone.active = false;
                        this.update();
                        telephone.update();
                        return true;
                    }
                } else {
                    if (player != null) {
                        player.displayClientMessage(Component.literal("국선: 등록된 수신 기기가 없거나 로드되지 않았습니다."), true);
                    }

                    return false;
                }
            }
        } else {
            return false;
        }
    }

    public void select() {
        if (this.owner.design().model().keypad() && this.owner.power() && this.owner.offHook()) {
            this.active = true;
            this.held = false;
            if (this.state == KvmTelephone.State.RINGING) {
                Kvm906BlockEntity other = this.other();
                if (other == null || other.telephone().state != KvmTelephone.State.DIALING) {
                    this.reset();
                    this.update();
                    return;
                }

                this.state = KvmTelephone.State.CONNECTED;
                this.ticks = 0;
                other.telephone().state = KvmTelephone.State.CONNECTED;
                other.telephone().ticks = 0;
                other.telephone().update();
            }

            this.update();
        }
    }

    public void selectIntercom() {
        this.active = false;
        this.update();
    }

    public void hook() {
        if (this.active) {
            this.selectIntercom();
        } else {
            this.select();
        }
    }

    public void sendMessage(ServerPlayer player, String text) {
        Kvm906BlockEntity other = this.other();
        if (this.active && this.state == KvmTelephone.State.CONNECTED && other != null && (!other.design().model().is932() || other.telephone().active())) {
            Intercom.say(this.level(), other, this.owner, player, "국선 " + this.number, text);
        }
    }

    public void tick() {
        if (this.level() != null) {
            if (this.dialWait > 0 && --this.dialWait == 0) {
                this.dial(null, this.buffer);
            }

            if (this.busy()) {
                Kvm906BlockEntity other = this.other();
                if (other != null && other.power() && this.owner.getBlockPos().equals(other.telephone().peer)) {
                    this.ticks++;
                    if (this.state == KvmTelephone.State.RINGING && (this.ticks % 40 == 1 || this.ticks % 40 == 7)) {
                        this.level()
                            .playSound(
                                null,
                                this.owner.getBlockPos(),
                                SoundEvents.NOTE_BLOCK_BELL.value(),
                                SoundSource.BLOCKS,
                                0.25F * Math.min(3, this.owner.design().number(this.owner.design().model().is932() ? "phone_bell" : "volume")),
                                1.35F
                            );
                    }

                    if (this.state == KvmTelephone.State.DIALING && this.ticks % 40 == 1) {
                        this.tone(0.85F);
                    }

                    if ((this.state == KvmTelephone.State.RINGING || this.state == KvmTelephone.State.DIALING) && this.ticks >= 600) {
                        this.hangUp();
                    } else if (this.state == KvmTelephone.State.CONNECTED && !this.owner.design().model().is932() && this.ticks >= 3600) {
                        this.hangUp();
                    }
                } else {
                    this.hangUp();
                }
            }
        }
    }

    public void settings(Map<String, String> settings) {
        settings.put("phone_held", this.held ? "1" : "0");
        settings.put("phone_last", this.lastDial);
        settings.put("phone_mode", this.pulse ? "pulse" : "tone");
        settings.put("phone_number", this.number);
        settings.put("phone_buffer", this.buffer);
        settings.put("phone_state", this.state.name());
        settings.put("phone_active", this.active ? "1" : "0");
    }

    public void save(CompoundTag tag, boolean runtime) {
        tag.putString("PhoneLast", this.lastDial);
        tag.putString("PhoneNumber", this.number);
        tag.putBoolean("PhonePulse", this.pulse);
        if (runtime) {
            tag.putBoolean("PhoneHeld", this.held);
            tag.putString("PhoneBuffer", this.buffer);
            tag.putInt("PhoneState", this.state.ordinal());
            tag.putBoolean("PhoneActive", this.active);
        }
    }

    public void load(CompoundTag tag, boolean runtime) {
        this.lastDial = tag.getString("PhoneLast");
        this.number = tag.getString("PhoneNumber");
        this.pulse = tag.getBoolean("PhonePulse");
        if (runtime) {
            this.held = tag.getBoolean("PhoneHeld");
            this.buffer = tag.getString("PhoneBuffer");
            int id = tag.getInt("PhoneState");
            this.state = id >= 0 && id < KvmTelephone.State.values().length ? KvmTelephone.State.values()[id] : KvmTelephone.State.IDLE;
            this.active = tag.getBoolean("PhoneActive");
        } else {
            this.reset();
        }
    }

    public static enum State {
        IDLE,
        RINGING,
        DIALING,
        CONNECTED;
    }
}
