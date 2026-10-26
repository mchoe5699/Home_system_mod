package com.qwerty.homenet.block;

import com.qwerty.homenet.blockentity.DoorbellBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.phys.BlockHitResult;

public final class DoorbellBlock extends WallMountedBlock {
    public DoorbellBlock(Properties p) {
        super(p, 1.5, 6.0, 10.0, 1.0);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
        return new DoorbellBlockEntity(p, s);
    }

    @Override
    public InteractionResult use(BlockState s, Level l, BlockPos p, Player player, InteractionHand hand, BlockHitResult hit) {
        if (holdingLinker(player, hand)) {
            return InteractionResult.PASS;
        } else {
            if (!l.isClientSide && l.getBlockEntity(p) instanceof DoorbellBlockEntity bell) {
                bell.press();
            }

            return InteractionResult.sidedSuccess(l.isClientSide);
        }
    }
}
