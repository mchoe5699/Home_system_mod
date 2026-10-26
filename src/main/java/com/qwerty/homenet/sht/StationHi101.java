package com.qwerty.homenet.sht;

import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.data.DeviceRegistry;
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

public final class StationHi101 extends WallMountedBlock {
    public static final double HEIGHT = 0.354;
    public static final double WIDTH = 0.19116;
    public static final double DEPTH = 0.1;

    public StationHi101() {
        super(Properties.of().strength(1.5F).noOcclusion(), 1.52928, 5.168, 10.832, 1.6);
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
        return new StationHi101.Unit(p, s);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        return l.isClientSide ? null : createTickerHelper(t, Product.TYPE_HI101.get(), ReceiverBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState s, Level l, BlockPos at, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (!(held.getItem() instanceof HomeLinkerItem) && !(held.getItem() instanceof AnalogLinkerItem)) {
            if (player instanceof ServerPlayer p && l.getBlockEntity(at) instanceof StationHi101.Unit unit) {
                Vec3 v = hit.getLocation().subtract(Vec3.atLowerCornerOf(at)).subtract(DeviceOffsets.get(l, at));

                double across = switch ((Direction)s.getValue(FACING)) {
                    case NORTH -> 0.5 - v.x;
                    case SOUTH -> v.x - 0.5;
                    case EAST -> 0.5 - v.z;
                    case WEST -> v.z - 0.5;
                    default -> 10.0;
                };
                double x = 108.0 + across / 8.849999999999999E-4;
                double y = (0.677 - v.y) / 8.849999999999999E-4;
                if (x >= 4.0 && x <= 106.0 && y >= 0.0 && y <= 400.0) {
                    unit.replace(p);
                } else if (x >= 125.0 && x <= 204.0 && y >= 322.0 && y <= 378.0) {
                    unit.press(x < 165.0 ? "guard_door" : "away", p, false);
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
        if (!s.is(next.getBlock()) && l.getBlockEntity(p) instanceof StationHi101.Unit u) {
            u.onBroken();
        }

        super.onRemove(s, l, p, next, moving);
    }

    public static final class Unit extends Station102 {
        public Unit(BlockPos p, BlockState s) {
            super(Product.TYPE_HI101.get(), p, s);
            this.bus = true;
            this.folded = false;
        }

        @Override
        protected Map<String, String> settingsForClient() {
            Map<String, String> values = super.settingsForClient();
            values.put("model", "HI-101");
            values.remove("video");
            return values;
        }

        @Override
        public DeviceRegistry.Kind kind() {
            return DeviceRegistry.Kind.INTERPHONE;
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
                } else if (this.callState == CallState.IDLE) {
                    this.tell("수화기 발신음 · 경비실/문열림 버튼으로 경비실 호출");
                } else {
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
                this.finish();
                this.tell("수화기 거치 · 통화 종료");
            }
        }

        @Override
        public void press(String key, ServerPlayer p, boolean remote) {
            if (!remote) {
                if (!key.equals("guard_door") && !key.equals("guard")) {
                    if (key.equals("away")) {
                        super.press("away", p, false);
                    } else if (key.equals("handset")) {
                        this.replace(p);
                    }
                } else if (!this.docked && this.callState == CallState.CONNECTED && this.incomingKey.equals("lobby")) {
                    this.openDoor();
                    this.tell("공동현관 문열림");
                } else if (this.docked) {
                    this.tell("수화기를 든 뒤 경비실을 호출하세요");
                } else {
                    this.callGuard(p, "");
                }
            }
        }

        @Override
        public boolean startRinging(BlockPos at, String key) {
            boolean accepted = super.startRinging(at, key);
            if (accepted) {
                this.tell("착신 · 수화기를 들어 통화하세요");
            }

            return accepted;
        }
    }
}
