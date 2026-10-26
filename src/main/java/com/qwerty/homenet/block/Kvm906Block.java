package com.qwerty.homenet.block;

import com.qwerty.homenet.blockentity.Kvm906BlockEntity;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.item.AnalogLinkerItem;
import com.qwerty.homenet.item.KvmDesignItem;
import com.qwerty.homenet.item.ModelChangerItem;
import com.qwerty.homenet.kvm.KvmModel;
import com.qwerty.homenet.move.DeviceOffsets;
import com.qwerty.homenet.registry.ModBlockEntities;
import com.qwerty.homenet.registry.ModItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.storage.loot.LootParams.Builder;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class Kvm906Block extends ReceiverBlock {
    private final KvmModel defaultModel;

    public Kvm906Block(Properties properties) {
        this(properties, KvmModel.KVM_906);
    }

    public Kvm906Block(Properties properties, KvmModel model) {
        super(properties, DeviceRegistry.Kind.VIDEO_PHONE, 3.2, 4.8, 11.2, 1.410909090909091, false);
        this.defaultModel = model;
    }

    public KvmModel defaultModel() {
        return this.defaultModel;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        KvmModel model = level.getBlockEntity(pos) instanceof Kvm906BlockEntity be ? be.design().model() : this.defaultModel;
        double h = model.heightPixels() / 2.0;
        double w = model.widthPixels() / 2.0;
        double x1 = 8.0 - w;
        double x2 = 8.0 + w;
        double y1 = 8.0 - h;
        double y2 = 8.0 + h;
        double d = model.depthPixels();

        return DeviceOffsets.shape(switch ((Direction)state.getValue(FACING)) {
            case SOUTH -> Block.box(x1, y1, 0.0, x2, y2, d);
            case EAST -> Block.box(0.0, y1, x1, d, y2, x2);
            case WEST -> Block.box(16.0 - d, y1, x1, 16.0, y2, x2);
            default -> Block.box(x1, y1, 16.0 - d, x2, y2, 16.0);
        }, level, pos);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Kvm906BlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.KVM_906.get(), Kvm906BlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!holdingLinker(player, hand)
            && !(player.getItemInHand(hand).getItem() instanceof KvmDesignItem)
            && !(player.getItemInHand(hand).getItem() instanceof ModelChangerItem)
            && !(player.getItemInHand(hand).getItem() instanceof AnalogLinkerItem)) {
            if (!level.isClientSide && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof Kvm906BlockEntity be) {
                be.openFor(sp);
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        } else {
            return InteractionResult.PASS;
        }
    }

    public static ItemStack configuredItem(Kvm906BlockEntity be) {
        ItemStack stack = new ItemStack(ModItems.kvmItem(be.design().model()));
        stack.getOrCreateTag().put("BlockEntityTag", be.saveWithoutMetadata());
        return stack;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, Builder params) {
        return params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof Kvm906BlockEntity be
            ? List.of(configuredItem(be))
            : List.of(new ItemStack(ModItems.kvmItem(this.defaultModel)));
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        return level.getBlockEntity(pos) instanceof Kvm906BlockEntity be ? configuredItem(be) : new ItemStack(ModItems.kvmItem(this.defaultModel));
    }
}
