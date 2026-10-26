package com.qwerty.homenet.sht;

import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.intercom.CallState;
import com.qwerty.homenet.item.AnalogLinkerItem;
import com.qwerty.homenet.item.HomeLinkerItem;
import com.qwerty.homenet.move.DeviceOffsets;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class Station305 extends WallMountedBlock {
    final boolean fire;

    public Station305() {
        this(true);
    }

    public Station305(boolean fire) {
        super(Properties.of().strength(1.5F).noOcclusion(), 2.9735999999999994, 5.168, 10.832, 2.0);
        this.fire = fire;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState s) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
        return new Station305.Unit(p, s, this.fire);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        return l.isClientSide ? null : createTickerHelper(t, this.fire ? Product.TYPE305.get() : Product.TYPE305_PLAIN.get(), ReceiverBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState s, Level l, BlockPos at, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (!(held.getItem() instanceof HomeLinkerItem) && !(held.getItem() instanceof AnalogLinkerItem)) {
            if (player instanceof ServerPlayer p && l.getBlockEntity(at) instanceof Station305.Unit unit) {
                Vec3 v = hit.getLocation().subtract(Vec3.atLowerCornerOf(at)).subtract(DeviceOffsets.get(l, at));

                double across = switch ((Direction)s.getValue(FACING)) {
                    case NORTH -> 0.5 - v.x;
                    case SOUTH -> v.x - 0.5;
                    case EAST -> 0.5 - v.z;
                    case WEST -> v.z - 0.5;
                    default -> 10.0;
                };
                double x = 210.0 + across / 8.849999999999999E-4;
                double y = (0.677 - v.y) / 8.849999999999999E-4;
                if (x >= 4.0 && x <= 112.0 && y >= 0.0 && y <= 400.0) {
                    unit.replace(p);
                } else if (x >= 287.0 && x <= 336.0 && y >= 205.0 && y <= 375.0) {
                    unit.press(y < 245.0 ? "guard" : (y < 282.0 ? "away" : (y < 326.0 ? "front" : "open")), p, false);
                } else if (x >= 357.0 && x <= 414.0 && y >= 185.0 && y <= 375.0) {
                    unit.press(
                        y < 214.0 ? "emergency" : (y < 240.0 ? "test" : (y < 266.0 ? "gas_fire" : (y < 292.0 ? "security" : (y < 329.0 ? "restore" : "stop")))),
                        p,
                        false
                    );
                } else {
                    unit.open(p, false);
                }
            }

            return InteractionResult.sidedSuccess(l.isClientSide);
        } else {
            return InteractionResult.PASS;
        }
    }

    @Override
    public void onRemove(BlockState s, Level l, BlockPos p, BlockState next, boolean moving) {
        if (!s.is(next.getBlock()) && l.getBlockEntity(p) instanceof Station305.Unit u) {
            u.onBroken();
        }

        super.onRemove(s, l, p, next, moving);
    }

    public static final class Unit extends Station102 {
        int selfTestTicks;
        int silenceTicks;
        final boolean fire;

        public Unit(BlockPos p, BlockState s) {
            this(p, s, true);
        }

        public Unit(BlockPos p, BlockState s, boolean fire) {
            super(fire ? Product.TYPE305.get() : Product.TYPE305_PLAIN.get(), p, s);
            this.fire = fire;
            this.bus = true;
            this.folded = false;
        }

        @Override
        protected Map<String, String> settingsForClient() {
            Map<String, String> values = super.settingsForClient();
            values.put("model", this.fire ? "SHT-305V" : "SHT-305");
            values.put("self_test", this.selfTestTicks > 0 ? "1" : "0");
            values.put("silenced", this.silenceTicks > 0 ? "1" : "0");
            return values;
        }

        @Override
        public boolean supportsWirelessPortable() {
            return false;
        }

        @Override
        public void lift(ServerPlayer p) {
            if (this.docked) {
                this.docked = false;
                this.audio = true;
                if (this.callState == CallState.RINGING) {
                    this.answer();
                } else if (this.monitor > 0) {
                    this.startDoorTalk();
                } else {
                    this.tell("수화기 발신음 · 경비실 버튼으로 경비실 호출");
                }
            }
        }

        @Override
        public void replace(ServerPlayer p) {
            if (this.docked) {
                this.lift(p);
            } else {
                this.docked = true;
                this.finish();
                this.tell("수화기 거치");
            }
        }

        @Override
        public void press(String key, ServerPlayer p, boolean remote) {
            if (!remote) {
                switch (key) {
                    case "emergency":
                        this.trigger("emergency");
                        return;
                    case "test":
                        this.selfTestTicks = 200;
                        this.test = true;
                        this.tell("경보 자체시험 · 10초");
                        return;
                    case "gas_fire":
                        if (this.selfTestTicks > 0) {
                            this.trigger(this.fire ? "fire" : "gas");
                            this.test = true;
                            this.tell(this.fire ? "화재·가스 경보 자체시험" : "가스 경보 자체시험");
                        }

                        return;
                    case "stop":
                        if (!this.alarm.isEmpty()) {
                            this.mute = true;
                            this.silenceTicks = 1200;
                            this.resetTime = 0;
                            this.tell("경보음 정지 · 1분 안에 복귀하세요");
                        }

                        return;
                    case "restore":
                        if (!this.alarm.isEmpty() && !this.test && this.alarmSourceActive()) {
                            this.tell("경보 원인을 먼저 제거하세요");
                            return;
                        }

                        this.selfTestTicks = this.silenceTicks = 0;
                        this.restore();
                        return;
                    case "security":
                        super.press("security", p, false);
                        return;
                    case "open":
                        if (this.callState != CallState.CONNECTED || !this.incomingKey.equals("lobby") && !this.incomingKey.equals("front_door")) {
                            this.tell("현관 통화 중에만 문을 열 수 있습니다");
                        } else {
                            this.openDoor();
                            this.tell("현관 문열림");
                        }

                        return;
                    default:
                        if (key.equals("guard") && this.docked) {
                            this.tell("경비실 호출은 수화기를 든 뒤 누르세요");
                        } else if (key.equals("front") && this.monitor > 0 && this.docked) {
                            this.monitor = 0;
                            this.tell("현관 화면 종료");
                        } else {
                            super.press(key, p, false);
                            if (key.equals("front") && this.monitor > 0) {
                                this.monitor = 300;
                                this.update();
                            }
                        }
                }
            }
        }

        @Override
        public void trigger(String cause) {
            if (this.fire || !"fire".equals(cause)) {
                super.trigger(cause);
            }
        }

        private boolean alarmSourceActive() {
            if (this.level == null) {
                return false;
            } else {
                for (Direction d : Direction.values()) {
                    if (this.level.getSignal(this.worldPosition.relative(d), d) > 0) {
                        return true;
                    }
                }

                return false;
            }
        }

        @Override
        protected void tickExtra() {
            super.tickExtra();
            if (this.selfTestTicks > 0 && --this.selfTestTicks == 0) {
                if (this.test) {
                    this.restore();
                }

                this.test = false;
                this.update();
            }

            if (this.silenceTicks > 0 && --this.silenceTicks == 0) {
                if (!this.alarm.isEmpty()) {
                    this.mute = false;
                    this.tell("경보음 재개 · 복귀 필요");
                }

                this.update();
            }
        }
    }
}
