package com.qwerty.homenet.block;

import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.item.KvmDesignItem;
import com.qwerty.homenet.move.DeviceOffsets;
import com.qwerty.homenet.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class ReceiverBlock extends WallMountedBlock {
    private final DeviceRegistry.Kind kind;
    private final boolean desk;
    private static final VoxelShape DESK_SHAPE = Block.box(1.0, 0.0, 2.0, 15.0, 11.0, 14.0);

    public ReceiverBlock(Properties props, DeviceRegistry.Kind kind, double halfWidth, double y1, double y2, double depth, boolean desk) {
        super(props, halfWidth, y1, y2, depth);
        this.kind = kind;
        this.desk = desk;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WallpadBlock.RINGING, false));
    }

    public DeviceRegistry.Kind kind() {
        return this.kind;
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(FACING, WallpadBlock.RINGING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return this.desk ? DeviceOffsets.shape(DESK_SHAPE, level, pos) : super.getShape(state, level, pos, ctx);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.desk ? this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()) : super.getStateForPlacement(ctx);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReceiverBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.RECEIVER.get(), ReceiverBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof KvmDesignItem || holdingLinker(player, hand)) {
            return InteractionResult.PASS;
        } else if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            if (level.getBlockEntity(pos) instanceof ReceiverBlockEntity be && player instanceof ServerPlayer sp) {
                be.openFor(sp);
            }

            return InteractionResult.CONSUME;
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!isMoving && !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ReceiverBlockEntity be) {
            be.onBroken();
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }
}
