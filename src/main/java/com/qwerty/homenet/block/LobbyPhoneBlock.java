package com.qwerty.homenet.block;

import com.qwerty.homenet.blockentity.LobbyPhoneBlockEntity;
import com.qwerty.homenet.client.ClientHooks;
import com.qwerty.homenet.item.RfCardItem;
import com.qwerty.homenet.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

public class LobbyPhoneBlock extends WallMountedBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final double HEIGHT = 9.6;
    public static final double HALF_WIDTH = 4.266666666666667;
    public static final double Y1 = 3.2;
    public static final double Y2 = 12.8;
    public static final double DEPTH = 0.57;

    public LobbyPhoneBlock(Properties props) {
        super(props, 4.266666666666667, 3.2, 12.8, 0.57);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LobbyPhoneBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.LOBBY_PHONE.get(), LobbyPhoneBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (holdingLinker(player, hand)) {
            return InteractionResult.PASS;
        } else if (player.getItemInHand(hand).getItem() instanceof RfCardItem) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof LobbyPhoneBlockEntity be) {
                RfCardItem.tap(be, player.getItemInHand(hand));
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        } else if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openLobbyPhone(pos));
            return InteractionResult.SUCCESS;
        } else {
            if (level.getBlockEntity(pos) instanceof LobbyPhoneBlockEntity be) {
                be.wake();
            }

            return InteractionResult.CONSUME;
        }
    }

    public static void pulse(ServerLevel level, BlockPos pos, int ticks) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof LobbyPhoneBlock block) {
            level.setBlock(pos, state.setValue(POWERED, true), 3);
            level.scheduleTick(pos, block, Math.max(20, ticks));
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, false), 3);
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
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!isMoving && !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof LobbyPhoneBlockEntity be) {
            be.onBroken();
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }
}
