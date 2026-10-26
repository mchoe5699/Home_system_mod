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
import net.minecraft.nbt.CompoundTag;
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

public class Station308 extends WallMountedBlock {
    public static final double HEIGHT = 0.354;
    public static final double WIDTH = 0.3716999999999999;
    public static final double DEPTH = 0.125;
    final boolean star;

    public Station308() {
        this(false);
    }

    public Station308(boolean star) {
        super(Properties.of().strength(1.5F).noOcclusion(), 2.9735999999999994, 5.168, 10.832, 2.0);
        this.star = star;
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
        return new Station308.Unit(p, s, this.star);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        return l.isClientSide ? null : createTickerHelper(t, this.star ? Product.TYPE307.get() : Product.TYPE308.get(), ReceiverBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState s, Level l, BlockPos at, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (!(held.getItem() instanceof HomeLinkerItem) && !(held.getItem() instanceof AnalogLinkerItem)) {
            if (player instanceof ServerPlayer p && l.getBlockEntity(at) instanceof Station308.Unit unit) {
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
                } else if (!(x >= 330.0) || !(x <= 390.0) || !(y >= 185.0) || !(y <= 390.0)) {
                    unit.open(p, false);
                } else if (y < 235.0) {
                    if (!this.star) {
                        unit.press("emergency", p, false);
                    } else {
                        unit.open(p, false);
                    }
                } else if (y < 280.0) {
                    unit.press("guard", p, false);
                } else if (y < 330.0) {
                    unit.press(this.star ? "front" : "away", p, false);
                } else if (!this.star) {
                    unit.press("front", p, false);
                }
            }

            return InteractionResult.sidedSuccess(l.isClientSide);
        } else {
            return InteractionResult.PASS;
        }
    }

    @Override
    public void onRemove(BlockState s, Level l, BlockPos p, BlockState next, boolean moving) {
        if (!s.is(next.getBlock()) && l.getBlockEntity(p) instanceof Station308.Unit u) {
            u.onBroken();
        }

        super.onRemove(s, l, p, next, moving);
    }

    public static final class Unit extends Station102 {
        final boolean star;
        boolean power = true;

        public Unit(BlockPos p, BlockState s) {
            this(p, s, false);
        }

        public Unit(BlockPos p, BlockState s, boolean star) {
            super(star ? Product.TYPE307.get() : Product.TYPE308.get(), p, s);
            this.star = star;
            this.bus = !star;
            this.folded = false;
        }

        @Override
        protected Map<String, String> settingsForClient() {
            Map<String, String> values = super.settingsForClient();
            values.put("model", this.star ? "SHT-307" : "SHT-308");
            values.put("power", this.power ? "1" : "0");
            return values;
        }

        @Override
        public boolean supportsWirelessPortable() {
            return false;
        }

        @Override
        public void openDoor() {
            if (!this.star) {
                super.openDoor();
            }
        }

        @Override
        public void digit(String key, boolean remote) {
            if (!this.star || !key.equals("*") || this.callState != CallState.CONNECTED) {
                super.digit(key, remote);
            }
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
        public void press(String key, ServerPlayer p, boolean remote) {
            if (!remote) {
                if (!this.star || !key.equals("power") && !key.equals("open")) {
                    if (this.star && key.equals("guard") && this.guardNumber.isBlank()) {
                        this.tell("지정 경비실 번호를 설정하세요");
                    } else if (!this.star || !key.equals("emergency")) {
                        if (key.equals("emergency")) {
                            if (this.alarm.equals("emergency")) {
                                this.restore();
                                this.tell("비상 경보 해제");
                            } else {
                                this.trigger("emergency");
                            }
                        } else if (key.equals("guard") && this.docked) {
                            this.tell("경비실 호출은 수화기를 든 뒤 누르세요");
                        } else {
                            if (key.equals("front") && this.callState == CallState.CONNECTED) {
                                if (!this.star && this.incomingKey.equals("lobby") && !this.docked) {
                                    this.openDoor();
                                    this.tell("공동현관 문열림");
                                    return;
                                }

                                if (this.incomingKey.equals("front_door")) {
                                    this.callTicks = 0;
                                    this.tell("현관 통화 연장");
                                    return;
                                }
                            }

                            if (key.equals("front") && this.monitor > 0 && this.docked) {
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
            }
        }

        @Override
        protected void tickExtra() {
            super.tickExtra();
            if (this.callState == CallState.CONNECTED && (this.incomingKey.equals("front_door") || this.incomingKey.equals("lobby")) && this.callTicks >= 1200) {
                this.finish();
                this.tell("현관 통화 시간 종료");
            }
        }

        @Override
        public void trigger(String cause) {
            if (!this.star || !"emergency".equals(cause)) {
                super.trigger(cause);
            }
        }

        @Override
        protected void saveAdditional(CompoundTag t) {
            super.saveAdditional(t);
            t.putBoolean("ModelPower", this.power);
        }

        @Override
        public void load(CompoundTag t) {
            super.load(t);
            this.power = !t.contains("ModelPower") || t.getBoolean("ModelPower");
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
    }
}
