package com.qwerty.homenet.sht;

import com.qwerty.homenet.item.InsertCardItem;
import com.qwerty.homenet.video.VideoFormat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public final class Ht513 extends Ht512 {
    public static final double WIDTH = 0.42579078467153286;
    public static final double DEPTH = 0.03170290081521739;

    public Ht513() {
        super(0.42579078467153286, 0.03170290081521739);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Ht513.Unit(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, Product.HT513_TYPE.get(), (l, p, s, u) -> u.serverTick());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos at, Player player, InteractionHand hand, BlockHitResult hit) {
        if (holdingLinker(player, hand)) {
            return InteractionResult.PASS;
        } else if (!(player.getItemInHand(hand).getItem() instanceof InsertCardItem) && !player.getItemInHand(hand).is(Product.S1_CARD.get())) {
            if (level.isClientSide) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Ht513Visual.open(at));
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        } else {
            if (player instanceof ServerPlayer sp && level.getBlockEntity(at) instanceof Ht513.Unit unit) {
                unit.insert(sp, player.getItemInHand(hand));
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    public static final class Unit extends Ht512.Unit {
        public Unit(BlockPos pos, BlockState state) {
            super(Product.HT513_TYPE.get(), pos, state);
        }

        @Override
        public VideoFormat videoFormat() {
            return VideoFormat.COLOR;
        }
    }
}
