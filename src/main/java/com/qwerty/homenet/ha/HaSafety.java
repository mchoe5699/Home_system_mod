package com.qwerty.homenet.ha;

import com.qwerty.homenet.blockentity.Ha4100hBlockEntity;
import com.qwerty.homenet.intercom.CallSounds;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvents;

public final class HaSafety {
    public static final List<String> PRIORITY = List.of("fire", "gas", "emergency", "security", "camera");
    private final Ha4100hBlockEntity owner;
    private final Set<String> inputs = new HashSet<>();
    private final Set<String> contacts = new HashSet<>();
    private final Set<String> notified = new HashSet<>();
    private final Map<String, Integer> alarms = new LinkedHashMap<>();
    private boolean armed;
    private boolean muted;
    private boolean reporting;
    private boolean reportSession;
    private int test = -1;
    private int armAge;
    private int reportWait;
    private int attempts;
    private int successes;
    private int reportTalk;
    private int toneTicks;

    public HaSafety(Ha4100hBlockEntity b) {
        this.owner = b;
    }

    public boolean armed() {
        return this.armed;
    }

    public boolean muted() {
        return this.muted;
    }

    public int test() {
        return this.test;
    }

    public boolean active(String t) {
        return this.alarms.containsKey(t) || this.test == 0 && t.equals("fire") || this.test == 1 && t.equals("gas");
    }

    public boolean reporting() {
        return this.reporting;
    }

    public int reportAttempts() {
        return this.attempts;
    }

    public int reportSuccesses() {
        return this.successes;
    }

    public boolean any() {
        return !this.alarms.isEmpty();
    }

    public int age(String t) {
        return this.alarms.getOrDefault(t, -1);
    }

    public String priority() {
        return PRIORITY.stream().filter(this::active).findFirst().orElse("");
    }

    private String word(String type) {
        return switch (type) {
            case "fire" -> "불이 났습니다";
            case "gas" -> "가스가 샙니다";
            case "emergency" -> "비상사태 발생";
            case "security" -> "문이 열렸습니다";
            default -> "카메라 도난 경보";
        };
    }

    public void sensor(String type, boolean on) {
        if (Set.of("fire", "gas", "security", "emergency").contains(type)) {
            if (on) {
                this.inputs.add(type);
            } else {
                this.inputs.remove(type);
            }

            this.evaluate(type);
            this.owner.refresh();
        }
    }

    public void contact(String type, boolean on) {
        if (on) {
            this.contacts.add(type);
        } else {
            this.contacts.remove(type);
        }

        this.evaluate(type);
    }

    private boolean signal(String t) {
        return this.inputs.contains(t) || this.contacts.contains(t);
    }

    private void evaluate(String t) {
        if (this.signal(t) && (!t.equals("security") || this.armed)) {
            this.trigger(t);
        }
    }

    public void trigger(String t) {
        if (PRIORITY.contains(t) && !this.alarms.containsKey(t)) {
            this.alarms.put(t, 0);
            this.muted = false;
            this.test = -1;
            this.toneTicks = 0;
            this.owner.cancelNumberEntry();
            this.attempts = this.successes = this.reportTalk = 0;
            this.reportWait = 600;
            this.reporting = this.alarms.keySet().stream().anyMatch(a -> !a.equals("camera"));
            this.owner.refresh();
        }
    }

    public void clear(String type) {
        if (this.alarms.remove(type) != null && this.notified.remove(type)) {
            this.owner.reportGuard(type, false);
        }

        if (this.alarms.isEmpty()) {
            this.stopReports();
        }

        this.owner.refresh();
    }

    public void emergency() {
        if (this.active("emergency")) {
            this.clear("emergency");
        } else {
            this.trigger("emergency");
        }
    }

    public void arm() {
        this.armed = !this.armed;
        this.armAge = 0;
        if (!this.armed) {
            this.clear("security");
        }

        this.owner.refresh();
    }

    public void setArmed(boolean value) {
        if (this.armed != value) {
            this.arm();
        }
    }

    public void trial() {
        if (!this.any()) {
            if (this.test < 0) {
                this.test = 0;
                this.muted = false;
                this.owner.cancelNumberEntry();
            } else if (this.muted) {
                this.test = 1 - this.test;
                this.muted = false;
            }

            this.toneTicks = 0;
            this.owner.refresh();
        }
    }

    public void stop() {
        if (this.test >= 0) {
            if (this.muted) {
                this.test = -1;
                this.muted = false;
            } else {
                this.muted = true;
            }
        } else if (this.any()) {
            if (!this.muted) {
                this.muted = true;
            } else {
                for (String t : List.of("fire", "gas")) {
                    if (!this.signal(t)) {
                        this.clear(t);
                    }
                }

                this.muted = false;
            }
        }

        this.owner.refresh();
    }

    public void stopReports() {
        this.reporting = false;
        if (this.reportSession) {
            this.owner.phone().hangup();
        }

        this.reportSession = false;
        this.reportTalk = 0;
    }

    private void soundAt(BlockPos pos, float volume) {
        CallSounds.note(this.owner.getLevel(), pos, SoundEvents.NOTE_BLOCK_BELL.value(), volume, this.toneTicks % 16 < 8 ? 0.65F : 1.25F);
    }

    public void tick() {
        if (this.owner.power()) {
            if (this.armed) {
                this.armAge++;
                this.evaluate("security");
            }

            for (String type : new ArrayList<>(this.alarms.keySet())) {
                this.alarms.computeIfPresent(type, (k, v) -> v + 1);
                if (!this.notified.contains(type) && (!type.equals("security") || this.alarms.get(type) >= 200) && this.owner.reportGuard(type, true)) {
                    this.notified.add(type);
                }
            }

            this.toneTicks++;
            String typex = this.priority();
            boolean limited = Set.of("emergency", "security", "camera").contains(typex);
            boolean audible = !this.muted && !typex.isEmpty() && (!limited || this.age(typex) < 18000) && (!typex.equals("security") || this.armAge >= 1200);
            if (audible) {
                if (this.toneTicks % 8 == 0) {
                    this.soundAt(this.owner.getBlockPos(), this.owner.speakerVolume());
                    if (this.owner.number(0).isEmpty()
                        && this.owner.number(1).isEmpty()
                        && this.owner.number(2).isEmpty()
                        && this.alarms.keySet().stream().anyMatch(a -> a.equals("emergency") || !a.equals("camera") && this.signal(a))) {
                        this.owner.soundDoorAlarm(this.toneTicks % 16 < 8 ? 0.65F : 1.25F);
                    }
                }

                if (this.toneTicks % 240 == 60) {
                    this.owner.announce(this.word(typex));
                }
            }

            if (this.reporting && (!this.priority().equals("security") || this.armAge >= 1200)) {
                if (this.any()
                    && !this.owner.lifted()
                    && !this.alarms.keySet().stream().noneMatch(t -> t.equals("emergency") || !t.equals("camera") && this.signal(t))) {
                    HaTelephone phone = this.owner.phone();
                    if (this.reportTalk > 0) {
                        if (phone.state() != HaTelephone.State.CONNECTED) {
                            this.reportTalk = 0;
                            this.reportSession = false;
                            this.reportWait = 600;
                        } else {
                            int phase = (600 - this.reportTalk) % 200;
                            Ha4100hBlockEntity recipient = phone.other();
                            if (recipient != null) {
                                if (phase < 60 && phase % 8 == 0) {
                                    this.soundAt(recipient.getBlockPos(), 0.65F);
                                }

                                if (phase == 60 || phase == 90 || phase == 120 || phase == 150) {
                                    recipient.announce("여기 전화번호는 " + this.owner.number(3) + "입니다. " + this.word(this.priority()));
                                }
                            }

                            if (--this.reportTalk == 0) {
                                phone.hangup();
                                this.reportSession = false;
                                this.successes++;
                                this.reportWait = 600;
                                if (this.successes >= 3) {
                                    this.reporting = false;
                                }
                            }
                        }
                    } else if (this.reportSession && phone.state() == HaTelephone.State.CONNECTED) {
                        this.reportTalk = 600;
                    } else if (this.reportWait > 0) {
                        this.reportWait--;
                        if (!this.reportSession && phone.state() == HaTelephone.State.IDLE && this.reportWait % 8 == 0) {
                            this.owner.soundDoorAlarm(this.toneTicks % 16 < 8 ? 0.65F : 1.25F);
                        }
                    } else {
                        if (phone.state() == HaTelephone.State.IDLE) {
                            if (this.attempts >= 9) {
                                this.reporting = false;
                                return;
                            }

                            String n = this.owner.number(this.attempts++ % 3);
                            if (!n.isEmpty()) {
                                this.reportSession = phone.dial(n);
                            }

                            this.reportWait = 600;
                        }
                    }
                } else {
                    this.stopReports();
                }
            }
        }
    }

    public void save(CompoundTag t) {
        t.putBoolean("HaArmed", this.armed);
        t.putBoolean("HaMuted", this.muted);
        t.putInt("HaArmAge", this.armAge);
        ListTag list = new ListTag();
        this.alarms.forEach((typex, age) -> {
            CompoundTag row = new CompoundTag();
            row.putString("Type", typex);
            row.putInt("Age", age);
            list.add(row);
        });
        t.put("HaAlarms", list);
        ListTag in = new ListTag();

        for (String type : this.inputs) {
            in.add(StringTag.valueOf(type));
        }

        t.put("HaSensors", in);
    }

    public void load(CompoundTag t) {
        this.armed = t.getBoolean("HaArmed");
        this.muted = t.getBoolean("HaMuted");
        this.armAge = t.getInt("HaArmAge");
        this.alarms.clear();

        for (Tag value : t.getList("HaAlarms", 10)) {
            CompoundTag row = (CompoundTag)value;
            if (PRIORITY.contains(row.getString("Type"))) {
                this.alarms.put(row.getString("Type"), Math.max(0, row.getInt("Age")));
            }
        }

        this.inputs.clear();

        for (Tag type : t.getList("HaSensors", 8)) {
            this.inputs.add(type.getAsString());
        }

        this.test = -1;
        this.notified.clear();
        this.reporting = false;
    }

    public void unlinked() {
        this.notified.clear();
    }
}
