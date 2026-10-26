package com.qwerty.homenet.block;

import com.qwerty.homenet.blockentity.GuardMasterBlockEntity;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.move.DeviceOffsets;
import com.qwerty.homenet.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class GuardMasterBlock extends WallMountedBlock {
    public static final BooleanProperty OFFHOOK = BooleanProperty.create("offhook");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final double[][] BOXES = new double[][]{
        {2.0, 0.0, 3.4, 14.0, 0.7, 14.6}, {1.0, 0.7, 2.8, 15.0, 6.5, 7.4}, {1.0, 6.5, 5.0, 15.0, 13.0, 10.5}
    };
    private static final VoxelShape NORTH = shape(Direction.NORTH);
    private static final VoxelShape SOUTH = shape(Direction.SOUTH);
    private static final VoxelShape EAST = shape(Direction.EAST);
    private static final VoxelShape WEST = shape(Direction.WEST);

    private static VoxelShape shape(Direction facing) {
        VoxelShape out = Shapes.empty();

        for (double[] b : BOXES) {
            double x1 = b[0];
            double y1 = b[1];
            double z1 = b[2];
            double x2 = b[3];
            double y2 = b[4];
            double z2 = b[5];

            VoxelShape v = switch (facing) {
                case SOUTH -> Block.box(16.0 - x2, y1, 16.0 - z2, 16.0 - x1, y2, 16.0 - z1);
                case EAST -> Block.box(16.0 - z2, y1, x1, 16.0 - z1, y2, x2);
                case WEST -> Block.box(z1, y1, 16.0 - x2, z2, y2, 16.0 - x1);
                default -> Block.box(x1, y1, z1, x2, y2, z2);
            };
            out = Shapes.or(out, v);
        }

        return out;
    }

    public GuardMasterBlock(Properties props) {
        super(props, 7.0, 0.0, 10.0, 12.0);
        this.registerDefaultState(
            this.stateDefinition
                .any()
                .setValue(FACING, Direction.NORTH)
                .setValue(WallpadBlock.RINGING, false)
                .setValue(OFFHOOK, false)
                .setValue(POWERED, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(FACING, WallpadBlock.RINGING, OFFHOOK, POWERED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return DeviceOffsets.shape(switch ((Direction)state.getValue(FACING)) {
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            default -> NORTH;
        }, level, pos);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GuardMasterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.GUARD_MASTER.get(), ReceiverBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (holdingLinker(player, hand)) {
            return InteractionResult.PASS;
        } else if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            if (level.getBlockEntity(pos) instanceof GuardMasterBlockEntity be && player instanceof ServerPlayer sp) {
                be.openFor(sp);
            }

            return InteractionResult.CONSUME;
        }
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, false), 3);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!isMoving && !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof GuardMasterBlockEntity be) {
            be.onBroken();
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }
}
