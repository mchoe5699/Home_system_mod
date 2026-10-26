package com.qwerty.homenet.intercom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;

public final class DoorControl {
    private DoorControl() {
    }

    public static boolean isDoor(BlockState state) {
        return state.hasProperty(BlockStateProperties.OPEN);
    }

    public static BlockPos normalize(BlockState state, BlockPos pos) {
        return state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER
            ? pos.below()
            : pos.immutable();
    }

    public static boolean isOpen(ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return isDoor(s) && s.getValue(BlockStateProperties.OPEN);
    }

    public static void setOpen(ServerLevel level, BlockPos pos, boolean open) {
        if (level.isLoaded(pos)) {
            BlockState state = level.getBlockState(pos);
            if (isDoor(state) && state.getValue(BlockStateProperties.OPEN) != open) {
                if (state.getBlock() instanceof DoorBlock door) {
                    door.setOpen(null, level, state, pos, open);
                } else {
                    level.setBlock(pos, state.setValue(BlockStateProperties.OPEN, open), 3);
                    SoundEvent sound = state.getBlock() instanceof FenceGateBlock
                        ? (open ? SoundEvents.FENCE_GATE_OPEN : SoundEvents.FENCE_GATE_CLOSE)
                        : (
                            state.getBlock() instanceof TrapDoorBlock
                                ? (open ? SoundEvents.IRON_TRAPDOOR_OPEN : SoundEvents.IRON_TRAPDOOR_CLOSE)
                                : (open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE)
                        );
                    level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
                    level.gameEvent(null, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
                }
            }
        }
    }
}
