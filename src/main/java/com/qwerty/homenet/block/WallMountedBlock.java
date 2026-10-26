package com.qwerty.homenet.block;

import com.qwerty.homenet.item.HomeLinkerItem;
import com.qwerty.homenet.move.DeviceOffsets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class WallMountedBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final VoxelShape north;
    private final VoxelShape south;
    private final VoxelShape east;
    private final VoxelShape west;

    protected WallMountedBlock(Properties props, double halfWidth, double y1, double y2, double depth) {
        super(props.dynamicShape());
        double x1 = 8.0 - halfWidth;
        double x2 = 8.0 + halfWidth;
        this.north = Block.box(x1, y1, 16.0 - depth, x2, y2, 16.0);
        this.south = Block.box(x1, y1, 0.0, x2, y2, depth);
        this.east = Block.box(0.0, y1, x1, depth, y2, x2);
        this.west = Block.box(16.0 - depth, y1, x1, 16.0, y2, x2);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return DeviceOffsets.shape(switch ((Direction)state.getValue(FACING)) {
            case SOUTH -> this.south;
            case EAST -> this.east;
            case WEST -> this.west;
            default -> this.north;
        }, level, pos);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction clicked = ctx.getClickedFace();
        Direction facing = clicked.getAxis().isHorizontal() ? clicked : ctx.getHorizontalDirection().getOpposite();
        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    protected static boolean holdingLinker(Player player, InteractionHand hand) {
        return player.getItemInHand(hand).getItem() instanceof HomeLinkerItem;
    }
}
