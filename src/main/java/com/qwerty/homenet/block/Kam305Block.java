package com.qwerty.homenet.block;

import com.qwerty.homenet.blockentity.Kam305BlockEntity;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.move.DeviceOffsets;
import com.qwerty.homenet.registry.ModBlockEntities;
import com.qwerty.homenet.size.GuardConsoleSize;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class Kam305Block extends ReceiverBlock {
    public Kam305Block(Properties p) {
        super(p, DeviceRegistry.Kind.GUARD_CONSOLE, 7.4, 0.0, 8.3, 12.0, true);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return DeviceOffsets.shape(
            GuardConsoleSize.scale(
                s.getValue(FACING).getAxis() == Axis.X ? Block.box(2.0, 0.0, 0.6, 14.8, 8.6, 15.4) : Block.box(0.6, 0.0, 2.0, 15.4, 8.6, 14.8), 0.8648649F
            ),
            l,
            p
        );
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()
            && player.getMainHandItem().isEmpty()
            && player.getOffhandItem().isEmpty()
            && hit.getDirection() == state.getValue(FACING).getOpposite()) {
            if (!level.isClientSide && player instanceof ServerPlayer p && level.getBlockEntity(pos) instanceof Kam305BlockEntity b) {
                b.button("power", p);
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        } else {
            return super.use(state, level, pos, player, hand, hit);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
        return new Kam305BlockEntity(p, s);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        return l.isClientSide ? null : createTickerHelper(t, ModBlockEntities.KAM_305.get(), Kam305BlockEntity::tick);
    }
}
