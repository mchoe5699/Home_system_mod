package com.qwerty.homenet.ha;

import com.qwerty.homenet.blockentity.Ha4100hBlockEntity;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.data.ZoneData;
import com.qwerty.homenet.intercom.CallSounds;
import com.qwerty.homenet.intercom.Intercom;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;

public final class HaTelephone {
    private final Ha4100hBlockEntity owner;
    private BlockPos peer;
    private HaTelephone.State state = HaTelephone.State.IDLE;
    private boolean selected;
    private boolean held;
    private int ticks;
    private int wait;
    private String digits = "";
    private String last = "";

    public HaTelephone(Ha4100hBlockEntity b) {
        this.owner = b;
    }

    public HaTelephone.State state() {
        return this.state;
    }

    public boolean held() {
        return this.held;
    }

    public boolean selected() {
        return this.selected;
    }

    public String digits() {
        return this.digits;
    }

    public Ha4100hBlockEntity other() {
        Level l = this.owner.getLevel();
        return l != null
                && this.peer != null
                && ZoneData.connected(l, this.owner.getBlockPos(), this.peer)
                && l.isLoaded(this.peer)
                && l.getBlockEntity(this.peer) instanceof Ha4100hBlockEntity b
            ? b
            : null;
    }

    private void reset() {
        this.state = HaTelephone.State.IDLE;
        this.peer = null;
        this.selected = false;
        this.held = false;
        this.ticks = 0;
        this.wait = 0;
        this.digits = "";
    }

    public void hangup() {
        Ha4100hBlockEntity b = this.other();
        if (b != null && this.owner.getBlockPos().equals(b.phone().peer)) {
            b.phone().reset();
            b.refresh();
        }

        this.reset();
        this.owner.refresh();
    }

    public void select() {
        this.selected = true;
        this.held = false;
        if (this.state == HaTelephone.State.RINGING && this.owner.lifted()) {
            Ha4100hBlockEntity b = this.other();
            if (b == null || b.phone().state != HaTelephone.State.DIALING) {
                this.reset();
                return;
            }

            this.state = HaTelephone.State.CONNECTED;
            b.phone().state = HaTelephone.State.CONNECTED;
            this.ticks = 0;
            b.phone().ticks = 0;
            b.refresh();
        }

        this.owner.refresh();
    }

    public void park() {
        this.selected = false;
        if (this.state == HaTelephone.State.CONNECTED) {
            this.held = true;
        }

        this.owner.refresh();
    }

    public void press() {
        if (this.state == HaTelephone.State.CONNECTED && this.selected && !this.held) {
            this.held = true;
            this.selected = false;
        } else if (this.selected && this.state == HaTelephone.State.IDLE && !this.owner.lifted()) {
            this.selected = false;
            this.wait = 0;
            this.digits = "";
        } else {
            this.select();
        }

        this.owner.refresh();
    }

    public void replaceReceiver() {
        if (!this.held) {
            this.hangup();
        } else {
            this.selected = false;
            this.owner.refresh();
        }
    }

    public void key(String key) {
        if (this.selected && key.matches("[0-9*#]")) {
            CallSounds.note(
                this.owner.getLevel(), this.owner.getBlockPos(), SoundEvents.NOTE_BLOCK_BIT.value(), 0.25F, 0.7F + "123456789*0#".indexOf(key) * 0.07F
            );
            if (this.state == HaTelephone.State.IDLE) {
                if (key.equals("#")) {
                    this.dial(this.digits);
                } else if (key.equals("*")) {
                    if (!this.digits.isEmpty()) {
                        this.digits = this.digits.substring(0, this.digits.length() - 1);
                    }

                    this.wait = 0;
                } else if (this.digits.length() < 15) {
                    this.digits = this.digits + key;
                    this.wait = 25;
                }

                this.owner.refresh();
            }
        }
    }

    public boolean dial(String number) {
        this.wait = 0;
        if (this.state == HaTelephone.State.IDLE && number.matches("[0-9]{1,15}") && this.owner.getLevel() instanceof ServerLevel level) {
            for (DeviceRegistry.Entry e : DeviceRegistry.get(level).all()) {
                if (!e.pos().equals(this.owner.getBlockPos())
                    && ZoneData.connected(level, this.owner.getBlockPos(), e.pos())
                    && level.isLoaded(e.pos())
                    && level.getBlockEntity(e.pos()) instanceof Ha4100hBlockEntity b
                    && number.equals(b.number(3))) {
                    if (b.phone().state != HaTelephone.State.IDLE) {
                        this.owner.notice("전화 통화 중");
                        return false;
                    }

                    this.last = number;
                    this.digits = number;
                    this.peer = b.getBlockPos();
                    this.state = HaTelephone.State.DIALING;
                    this.selected = true;
                    this.held = false;
                    this.ticks = 0;
                    b.cancelNumberEntry();
                    b.phone().peer = this.owner.getBlockPos();
                    b.phone().state = HaTelephone.State.RINGING;
                    b.phone().ticks = 0;
                    b.phone().selected = false;
                    b.refresh();
                    this.owner.refresh();
                    return true;
                }
            }

            this.owner.notice("연결된 전화번호가 없습니다");
            return false;
        } else {
            return false;
        }
    }

    public void redial() {
        this.select();
        this.dial(this.last);
    }

    public void message(ServerPlayer player, String text) {
        Ha4100hBlockEntity b = this.other();
        if (this.selected
            && !this.held
            && this.state == HaTelephone.State.CONNECTED
            && this.owner.lifted()
            && b != null
            && b.lifted()
            && b.phone().selected
            && !b.phone().held
            && this.owner.getLevel() instanceof ServerLevel level) {
            Intercom.say(level, b, this.owner, player, "전화 " + this.owner.number(3), text);
        }
    }

    public void tick() {
        if (this.wait > 0 && --this.wait == 0) {
            this.dial(this.digits);
        }

        if (this.state != HaTelephone.State.IDLE) {
            Ha4100hBlockEntity b = this.other();
            if (b != null && this.owner.getBlockPos().equals(b.phone().peer)) {
                this.ticks++;
                if (this.state == HaTelephone.State.RINGING && this.ticks % 40 == 1) {
                    CallSounds.note(this.owner.getLevel(), this.owner.getBlockPos(), SoundEvents.NOTE_BLOCK_BELL.value(), this.owner.bellVolume(), 1.25F);
                }

                if (this.state == HaTelephone.State.DIALING && this.ticks % 40 == 1) {
                    CallSounds.note(this.owner.getLevel(), this.owner.getBlockPos(), SoundEvents.NOTE_BLOCK_BIT.value(), 0.25F, 0.85F);
                }

                if (this.held && this.ticks % 20 == 0) {
                    CallSounds.note(this.owner.getLevel(), b.getBlockPos(), SoundEvents.NOTE_BLOCK_CHIME.value(), 0.25F, 1.1F);
                }

                if (this.state != HaTelephone.State.CONNECTED && this.ticks >= 600) {
                    this.hangup();
                }
            } else {
                this.hangup();
            }
        }
    }

    public void save(CompoundTag tag) {
        tag.putString("HaLastDial", this.last);
    }

    public void load(CompoundTag tag) {
        this.last = tag.getString("HaLastDial");
        this.reset();
    }

    public static enum State {
        IDLE,
        RINGING,
        DIALING,
        CONNECTED;
    }
}
