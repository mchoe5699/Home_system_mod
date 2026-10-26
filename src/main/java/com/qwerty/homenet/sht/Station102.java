package com.qwerty.homenet.sht;

import com.qwerty.homenet.blockentity.CallerBlockEntity;
import com.qwerty.homenet.intercom.CallState;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class Station102 extends Station {
    int heldTicks;

    public Station102(BlockPos p, BlockState s) {
        this(Product.TYPE102.get(), p, s);
    }

    protected Station102(BlockEntityType<?> type, BlockPos p, BlockState s) {
        super(type, p, s);
        this.folded = false;
    }

    @Override
    public boolean wired() {
        return true;
    }

    @Override
    boolean paired() {
        return true;
    }

    @Override
    public boolean owns(ItemStack stack) {
        return this.ownsPortable(stack);
    }

    @Override
    public void lift(ServerPlayer p) {
        if (this.docked) {
            this.docked = false;
            if (this.internal == 2) {
                this.acceptInternal(false);
                this.update();
            } else if (this.internal == 3) {
                this.audio = true;
                this.update();
            } else if (this.portableActive) {
                this.tell("휴대장치 통화 중 · 내선으로 전환하세요");
            } else {
                this.portableActive = false;
                if (this.callState == CallState.RINGING || this.phone == 2) {
                    this.audio = false;
                    this.talk();
                } else if (this.callState == CallState.CONNECTED || this.callState == CallState.DIALING || this.phone > 0) {
                    this.audio = true;
                    this.hold = false;
                } else if (this.monitor > 0) {
                    this.startDoorTalk();
                } else {
                    this.audio = true;
                    this.tell("국선 발신음 · 번호 입력");
                }

                this.update();
            }
        }
    }

    @Override
    public void replace(ServerPlayer p) {
        if (this.docked) {
            this.lift(p);
        } else {
            this.docked = true;
            if (this.internal == 3) {
                this.finish();
                this.tell("내선 통화 종료 · 유선 수화기 거치");
            } else if (this.portableActive || this.transfer == 1) {
                this.tell("유선 수화기 거치 · 휴대장치 연결 유지");
            } else if (this.phone == 3 && this.hold) {
                this.audio = false;
                this.tell("국선 보류 · 수화기 거치");
            } else {
                this.finish();
                this.tell("수화기 거치 · 대기");
            }
        }
    }

    void startDoorTalk() {
        BlockPos at = this.linkedCamera();
        if (at != null && this.level.getBlockEntity(at) instanceof CallerBlockEntity c) {
            c.callFromReceiver(this);
            this.audio = true;
            this.answer();
            this.tell("현관 통화");
        } else {
            this.tell("연결된 도어카메라가 없습니다");
        }
    }

    @Override
    public boolean startRinging(BlockPos at, String key) {
        boolean accepted = super.startRinging(at, key);
        if (accepted) {
            this.tell(this.portableRegistered ? "착신 · 유선 수화기 또는 휴대장치로 응답" : "착신 · 유선 수화기를 드세요");
        }

        return accepted;
    }

    @Override
    public void digit(String key, boolean remote) {
        if (!key.equals("*")
            || this.callState != CallState.CONNECTED
            || !this.incomingKey.equals("lobby") && !this.incomingKey.equals("front_door")
            || !this.audio
            || this.hold
            || (remote ? !this.portableActive : this.docked || this.portableActive)) {
            if (remote) {
                super.digit(key, true);
            } else if (this.memory && this.editing.equals("slot") && key.matches("[0-9*#]")) {
                this.editing = key;
                this.digits = "";
                this.update();
            } else {
                super.digit(key, false);
            }
        } else {
            this.openDoor();
        }
    }

    @Override
    public void press(String key, ServerPlayer p, boolean remote) {
        if (remote) {
            this.remotePress(key, p);
        } else {
            if (key.equals("door")) {
                key = "front";
            }

            if (!Set.of("speaker", "talk", "channel").contains(key)) {
                if (key.equals("handset")) {
                    this.replace(p);
                } else if (this.portableActive
                    && (this.phone > 0 || this.getCallState() != CallState.IDLE)
                    && Set.of("front", "line", "guard", "redial", "fire_call", "police_call", "medical_call").contains(key)) {
                    this.tell("휴대장치 통화 중 · 내선으로 본체에 전환하세요");
                } else if (key.equals("front")) {
                    if (this.memory) {
                        this.editing = "front";
                        this.digits = "";
                        this.update();
                    } else if (this.callState != CallState.CONNECTED || !this.incomingKey.equals("front_door") && !this.incomingKey.equals("lobby")) {
                        this.finish();
                        if (this.cameraLink() == null) {
                            this.tell("연결된 도어카메라가 없습니다");
                        } else {
                            this.monitor = 600;
                            if (!this.docked) {
                                this.startDoorTalk();
                            } else {
                                this.tell("현관 영상 감시 · 수화기를 들면 통화");
                            }
                        }
                    } else {
                        this.tell("현관 영상 · 문열림은 통화 중 *");
                    }
                } else if (key.equals("line")) {
                    if (this.memory) {
                        this.editing = this.editing.equals("slot") ? "number" : "slot";
                        this.digits = "";
                        this.tell("단축키 선택 · 국선을 다시 누르면 자택 전화번호");
                    } else if (this.verify) {
                        this.verify = false;
                        this.tell("자택 전화번호: " + this.number);
                    } else if (this.phone == 3 && this.hold) {
                        this.hold = false;
                        this.heldTicks = 0;
                        this.audio = !this.docked;
                        this.tell("국선 재통화 · 수화기를 드세요");
                    } else if (this.phone <= 0 && (!this.audio || this.callState != CallState.IDLE || this.monitor != 0) && !this.status.startsWith("국선 선택")) {
                        this.finish();
                        this.audio = !this.docked;
                        this.tell("국선 선택 · 번호 입력 · 응답 후 수화기를 드세요");
                    } else {
                        this.finish();
                    }
                } else if (key.equals("guard") && !this.memory && !this.verify) {
                    this.audio = !this.docked;
                    this.callGuard(p, "");
                } else if (key.equals("redial") && !this.memory && !this.verify) {
                    this.finish();
                    this.audio = !this.docked;
                    if (!this.last.isEmpty()) {
                        this.dialNumber(this.last);
                    } else {
                        this.tell("재다이얼 번호가 없습니다");
                    }
                } else if (Set.of("fire_call", "police_call", "medical_call").contains(key) && !this.memory && !this.verify) {
                    this.finish();
                    this.audio = !this.docked;
                    this.dialNumber(this.stored.getOrDefault("B:" + key, ""));
                } else if (key.equals("intercom") && !this.memory && !this.verify) {
                    this.requestInternal(false);
                } else {
                    super.press(key, p, false);
                }
            }
        }
    }

    @Override
    public int lights() {
        int bits = super.lights() & -3;
        return this.status.startsWith("국선 선택") ? bits | 128 : bits;
    }

    @Override
    protected void tickExtra() {
        if (this.hold && this.phone == 3) {
            this.phoneTime = 0;
            if (++this.heldTicks >= 3600) {
                this.finish();
                this.heldTicks = 0;
            }
        } else {
            this.heldTicks = 0;
        }

        super.tickExtra();
    }

    @Override
    public void say(ServerPlayer p, String text, boolean remote) {
        if ((this.internal == 3 || remote == this.portableActive) && (remote || !this.docked)) {
            super.say(p, text, remote);
        } else {
            this.tell("현재 통화 중인 수화기를 사용하세요");
        }
    }
}
