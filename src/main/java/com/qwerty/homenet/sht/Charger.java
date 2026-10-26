package com.qwerty.homenet.sht;

import com.qwerty.homenet.intercom.CallSounds;
import com.qwerty.homenet.move.DeviceMoves;
import com.qwerty.homenet.move.DeviceOffsets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class Charger extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    Charger() {
        super(Properties.of().strength(1.2F).noOcclusion());
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext c) {
        return this.defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState s) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        boolean full = l.getBlockEntity(p) instanceof Charger.Dock d && !d.stack.isEmpty();
        return DeviceOffsets.shape(Block.box(4.8, 0.0, 4.8, 11.2, full ? 14.5 : 2.4, 11.2), l, p);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
        return new Charger.Dock(p, s);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        return !l.isClientSide && t == Product.CHARGER_TYPE.get() ? (level, pos, state, be) -> ((Charger.Dock)be).tick() : null;
    }

    @Override
    public InteractionResult use(BlockState s, Level l, BlockPos at, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player instanceof ServerPlayer p && l.getBlockEntity(at) instanceof Charger.Dock d) {
            ItemStack held = p.getItemInHand(hand);
            if (d.stack.isEmpty() && Portable.isPortable(held)) {
                Station b = Portable.base(l, held);
                if (b != null) {
                    b.portableReturn();
                }

                d.stack = held.split(1);
                d.sync();
            } else if (held.isEmpty() && !d.stack.isEmpty()) {
                ItemStack item = d.stack.copy();
                if (p.getInventory().add(item)) {
                    Station b = Portable.base(l, d.stack);
                    d.stack = ItemStack.EMPTY;
                    d.sync();
                    if (b != null) {
                        b.portablePickup(p);
                    }
                } else {
                    p.displayClientMessage(Component.literal("인벤토리 공간이 필요합니다"), true);
                }
            }
        }

        return InteractionResult.sidedSuccess(l.isClientSide);
    }

    @Override
    public void onRemove(BlockState s, Level l, BlockPos p, BlockState n, boolean moving) {
        if (!s.is(n.getBlock()) && l.getBlockEntity(p) instanceof Charger.Dock d && !l.isClientSide) {
            Containers.dropItemStack(l, p.getX() + 0.5, p.getY() + 0.2, p.getZ() + 0.5, d.stack);
            d.stack = ItemStack.EMPTY;
        }

        super.onRemove(s, l, p, n, moving);
    }

    public static final class Dock extends BlockEntity {
        ItemStack stack = ItemStack.EMPTY;

        @Override
        public AABB getRenderBoundingBox() {
            return new AABB(this.worldPosition).move(DeviceOffsets.get(this.level, this.worldPosition));
        }

        Dock(BlockPos p, BlockState s) {
            super(Product.CHARGER_TYPE.get(), p, s);
        }

        @Override
        public void onLoad() {
            super.onLoad();
            if (this.level != null && !this.level.isClientSide) {
                DeviceMoves.applyPending(this);
            }
        }

        void tick() {
            if (this.level.getGameTime() % 20L == 0L && !this.stack.isEmpty()) {
                if (this.refresh()) {
                    this.sync();
                }
            }
        }

        boolean refresh() {
            if (this.stack.isEmpty()) {
                return false;
            } else {
                CompoundTag before = this.stack.getOrCreateTag().copy();
                CompoundTag t = this.stack.getOrCreateTag();
                int battery = t.getInt("battery");
                t.putInt("battery", Math.min(36000, battery + 120));
                Station base = Portable.base(this.level, this.stack);
                boolean ringing = base != null
                    && base.same(this.worldPosition)
                    && base.getBlockPos().distSqr(this.worldPosition) <= 1024.0
                    && t.getBoolean("power")
                    && base.portableRinging();
                t.putBoolean("intercom", base != null && base.internal > 0 && ringing);
                t.putBoolean("ringing", ringing);
                t.putBoolean("talking", false);
                if (ringing) {
                    CallSounds.note(this.level, this.worldPosition, SoundEvents.NOTE_BLOCK_BELL.value(), base.ring * 0.25F, 1.1F);
                }

                return !before.equals(t);
            }
        }

        void sync() {
            this.setChanged();
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }

        @Override
        protected void saveAdditional(CompoundTag t) {
            super.saveAdditional(t);
            t.put("Portable", this.stack.save(new CompoundTag()));
        }

        @Override
        public void load(CompoundTag t) {
            super.load(t);
            this.stack = ItemStack.of(t.getCompound("Portable"));
        }

        @Override
        public CompoundTag getUpdateTag() {
            return this.saveWithoutMetadata();
        }

        public ClientboundBlockEntityDataPacket getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }
    }
}
