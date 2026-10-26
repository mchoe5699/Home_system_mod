package com.qwerty.homenet.block;

import com.qwerty.homenet.blockentity.Hmc7000BlockEntity;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.move.DeviceOffsets;
import com.qwerty.homenet.registry.ModBlockEntities;
import com.qwerty.homenet.size.GuardConsoleSize;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class Hmc7000Block extends ReceiverBlock {
    public Hmc7000Block(Properties properties) {
        super(properties, DeviceRegistry.Kind.GUARD_CONSOLE, 6.4, 0.0, 6.0, 15.2, true);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = Shapes.or(Block.box(1.6, 0.0, 0.4, 14.4, 2.6, 15.6), Block.box(10.8, 1.0, 1.5, 14.0, 3.5, 14.4));
        if (level.getBlockEntity(pos) instanceof Hmc7000BlockEntity b) {
            double a = Math.toRadians(b.screenAngle() + 6);
            double rise = 3.43 * Math.sin(a);
            shape = Shapes.or(shape, Block.box(2.13, 1.8, 11.8, 10.15, 2.1 + rise, 15.6));
        }

        int i = 0;

        while (true) {
            if (i >= switch ((Direction)state.getValue(FACING)) {
                case EAST -> 1;
                case SOUTH -> 2;
                case WEST -> 3;
                default -> 0;
            }) {
                return DeviceOffsets.shape(GuardConsoleSize.scale(shape, 0.84210527F), level, pos);
            }

            VoxelShape next = Shapes.empty();

            for (AABB box : shape.toAabbs()) {
                next = Shapes.or(next, Shapes.create(1.0 - box.maxZ, box.minY, box.minX, 1.0 - box.minZ, box.maxY, box.maxX));
            }

            shape = next;
            i++;
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Hmc7000BlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.HMC_7000.get(), Hmc7000BlockEntity::tick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()
            && player.getMainHandItem().isEmpty()
            && player.getOffhandItem().isEmpty()
            && hit.getDirection() == state.getValue(FACING).getOpposite()) {
            if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof Hmc7000BlockEntity b) {
                b.button("power", server);
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        } else {
            return super.use(state, level, pos, player, hand, hit);
        }
    }
}
