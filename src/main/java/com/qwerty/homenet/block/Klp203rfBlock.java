package com.qwerty.homenet.block;

import com.qwerty.homenet.blockentity.Kam305BlockEntity;
import com.qwerty.homenet.blockentity.Klp203rfBlockEntity;
import com.qwerty.homenet.item.AnalogLinkerItem;
import com.qwerty.homenet.item.DoorLinkerItem;
import com.qwerty.homenet.item.InsertCardItem;
import com.qwerty.homenet.item.KvmDesignItem;
import com.qwerty.homenet.item.RfCardItem;
import com.qwerty.homenet.klp.KlpModel;
import com.qwerty.homenet.registry.ModBlockEntities;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class Klp203rfBlock extends WallMountedBlock {
    public final KlpModel model;

    public Klp203rfBlock(Properties p) {
        this(p, KlpModel.RF);
    }

    public Klp203rfBlock(Properties p, KlpModel model) {
        super(p, 3.5666666666666664, 3.333333333333333, 12.666666666666668, 0.8066666666666666);
        this.model = model;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(BlockStateProperties.POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
        b.add(FACING, BlockStateProperties.POWERED);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
        return new Klp203rfBlockEntity(p, s);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        return l.isClientSide ? null : createTickerHelper(t, ModBlockEntities.KLP_203RF.get(), Klp203rfBlockEntity::tick);
    }

    @Override
    public InteractionResult use(BlockState s, Level l, BlockPos at, Player p, InteractionHand hand, BlockHitResult hit) {
        if (!(p.getItemInHand(hand).getItem() instanceof AnalogLinkerItem)
            && !(p.getItemInHand(hand).getItem() instanceof DoorLinkerItem)
            && !(p.getItemInHand(hand).getItem() instanceof KvmDesignItem)) {
            if (!l.isClientSide && p instanceof ServerPlayer sp && l.getBlockEntity(at) instanceof Klp203rfBlockEntity b) {
                if (!(p.getItemInHand(hand).getItem() instanceof RfCardItem) && !(p.getItemInHand(hand).getItem() instanceof InsertCardItem)) {
                    b.openFor(sp);
                } else {
                    b.useCard(sp, p.getItemInHand(hand));
                }
            }

            return InteractionResult.sidedSuccess(l.isClientSide);
        } else {
            return InteractionResult.PASS;
        }
    }

    private static ItemStack configured(Klp203rfBlockEntity panel) {
        ItemStack item = new ItemStack(panel.getBlockState().getBlock().asItem());
        item.getOrCreateTag().put("BlockEntityTag", panel.saveWithoutMetadata());
        return item;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        return params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof Klp203rfBlockEntity panel
            ? List.of(configured(panel))
            : List.of(new ItemStack(this.asItem()));
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult hit, BlockGetter l, BlockPos at, Player p) {
        return l.getBlockEntity(at) instanceof Klp203rfBlockEntity panel ? configured(panel) : new ItemStack(this.asItem());
    }

    @Override
    public boolean isSignalSource(BlockState s) {
        return true;
    }

    @Override
    public int getSignal(BlockState s, BlockGetter l, BlockPos p, Direction d) {
        return s.getValue(BlockStateProperties.POWERED) ? 15 : 0;
    }

    @Override
    public void tick(BlockState s, ServerLevel l, BlockPos p, RandomSource r) {
        if (s.getValue(BlockStateProperties.POWERED)) {
            l.setBlock(p, s.setValue(BlockStateProperties.POWERED, false), 3);
        }
    }

    @Override
    public void onRemove(BlockState s, Level l, BlockPos p, BlockState n, boolean moving) {
        if (!moving && !s.is(n.getBlock()) && l.getBlockEntity(p) instanceof Klp203rfBlockEntity b) {
            if (b.exchange() instanceof Kam305BlockEntity k) {
                k.removeLobbyPanel(p);
            }

            b.onBroken();
        }

        super.onRemove(s, l, p, n, moving);
    }
}
