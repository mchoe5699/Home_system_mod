package com.qwerty.homenet.sht;

import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.blockentity.CallerBlockEntity;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.guard.GuardSystem;
import com.qwerty.homenet.item.DoorLinkerItem;
import com.qwerty.homenet.item.HomeLinkerItem;
import com.qwerty.homenet.move.DeviceMoves;
import com.qwerty.homenet.move.DeviceOffsets;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class Ht100s extends WallMountedBlock {
    public static final double HEIGHT = 0.15;
    public static final double WIDTH = 0.115;
    public static final double FRAME_WIDTH = 0.121;
    public static final double FRAME_HEIGHT = 0.155;
    public static final double DEPTH = 0.0455;

    public Ht100s() {
        super(Properties.of().strength(1.5F).noOcclusion(), 0.968, 6.76, 9.24, 0.728);
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
        return new Ht100s.Unit(p, s);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        return l.isClientSide ? null : createTickerHelper(t, Product.HT100S_TYPE.get(), Ht100s.Unit::tick);
    }

    @Override
    public InteractionResult use(BlockState s, Level l, BlockPos at, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (!(held.getItem() instanceof HomeLinkerItem) && !(held.getItem() instanceof DoorLinkerItem)) {
            if (!l.isClientSide && l.getBlockEntity(at) instanceof Ht100s.Unit u) {
                if (button(s, l, at, hit)) {
                    u.call();
                } else {
                    player.displayClientMessage(Component.literal(u.model() + " · 홈링커로 세대 내부기기 연결 · 오른쪽 아래 버튼으로 호출"), true);
                }
            }

            return InteractionResult.sidedSuccess(l.isClientSide);
        } else {
            return InteractionResult.PASS;
        }
    }

    static boolean button(BlockState s, Level l, BlockPos at, BlockHitResult hit) {
        Vec3 v = hit.getLocation().subtract(Vec3.atLowerCornerOf(at)).subtract(DeviceOffsets.get(l, at));

        double across = switch ((Direction)s.getValue(FACING)) {
            case NORTH -> 0.5 - v.x;
            case SOUTH -> v.x - 0.5;
            case EAST -> 0.5 - v.z;
            case WEST -> v.z - 0.5;
            default -> 10.0;
        };
        double x = 115.0 + across / 5.0E-4;
        double y = (0.575 - v.y) / 5.0E-4;
        return x >= 141.0 && x <= 209.0 && y >= 203.0 && y <= 275.0;
    }

    @Override
    public void onRemove(BlockState s, Level l, BlockPos p, BlockState n, boolean moving) {
        if (!s.is(n.getBlock()) && l.getBlockEntity(p) instanceof Ht100s.Unit u) {
            u.onBroken();
        }

        super.onRemove(s, l, p, n, moving);
    }

    public static final class Unit extends CallerBlockEntity {
        String unit = "";

        public Unit(BlockPos p, BlockState s) {
            super(Product.HT100S_TYPE.get(), p, s);
        }

        String model() {
            return this.getBlockState().is(Product.HT100.get()) ? "HT-100" : "HT-100S";
        }

        @Override
        public DeviceRegistry.Kind kind() {
            return DeviceRegistry.Kind.DOOR_PHONE;
        }

        @Override
        public String callerKey() {
            return "front_door";
        }

        @Override
        public String registeredUnit() {
            return this.unit;
        }

        public void setUnit(String value) {
            this.unit = value == null ? "" : value;
            this.setChanged();
            this.registerDevice();
        }

        public void call() {
            if (this.level instanceof ServerLevel) {
                if (this.inCall) {
                    this.hangUpFromCaller();
                } else if (this.unit.isBlank()) {
                    this.nearby(this.model() + "을 홈링커로 세대 내부기기에 연결하세요");
                } else {
                    CallerBlockEntity.CallResult result = this.startCall(this.unit);
                    if (result == CallerBlockEntity.CallResult.OK) {
                        this.level.playSound(null, this.worldPosition, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.BLOCKS, 0.6F, 1.4F);
                    } else {
                        this.nearby(this.model() + " 호출 실패: " + switch (result) {
                            case NO_UNIT, NO_SIGNAL -> "연결된 세대 내부기기를 찾을 수 없습니다";
                            case BUSY -> "통화 중입니다";
                            default -> "호출할 수 없습니다";
                        });
                    }
                }
            }
        }

        private void nearby(String message) {
            this.level
                .players()
                .stream()
                .filter(p -> p.distanceToSqr(this.worldPosition.getCenter()) < 64.0)
                .forEach(p -> p.displayClientMessage(Component.literal(message), true));
        }

        @Override
        public List<ReceiverBlockEntity> receiversOf(String unit) {
            List<ReceiverBlockEntity> matches = super.receiversOf(unit);
            if (matches.isEmpty()) {
                return matches;
            } else {
                GuardSystem selected;
                try {
                    selected = GuardSystem.valueOf(this.getPersistentData().getString("ShtDoorProtocol"));
                } catch (IllegalArgumentException var5) {
                    selected = matches.get(0).guardSystem();
                }

                GuardSystem protocol = selected;
                return protocol == GuardSystem.NONE ? List.of() : matches.stream().filter(r -> r.guardSystem() == protocol).toList();
            }
        }

        @Override
        public void syncScreens() {
        }

        @Override
        public void onLoad() {
            super.onLoad();
            if (this.level != null && !this.level.isClientSide) {
                DeviceMoves.applyPending(this);
            }
        }

        @Override
        public AABB getRenderBoundingBox() {
            return new AABB(this.worldPosition).move(DeviceOffsets.get(this.level, this.worldPosition));
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            this.unit = tag.getString("UnitNo");
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.putString("UnitNo", this.unit);
        }

        static void tick(Level l, BlockPos p, BlockState s, Ht100s.Unit u) {
            u.tickCaller();
        }
    }
}
