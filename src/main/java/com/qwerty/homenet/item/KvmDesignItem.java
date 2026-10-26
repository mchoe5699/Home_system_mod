package com.qwerty.homenet.item;

import com.qwerty.homenet.blockentity.CallerBlockEntity;
import com.qwerty.homenet.blockentity.Kvm906BlockEntity;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;

public class KvmDesignItem extends Item {
    private final boolean converter;

    public KvmDesignItem(Properties properties, boolean converter) {
        super(properties);
        this.converter = converter;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getLevel().getBlockEntity(ctx.getClickedPos()) instanceof CallerBlockEntity caller) {
            if (ctx.getPlayer() instanceof ServerPlayer p) {
                caller.openLogoEditor(p);
            }

            return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
        } else if (!(ctx.getLevel().getBlockEntity(ctx.getClickedPos()) instanceof ReceiverBlockEntity receiver)) {
            return InteractionResult.PASS;
        } else if (!(receiver instanceof Kvm906BlockEntity) || ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown()) {
            if (ctx.getPlayer() instanceof ServerPlayer player) {
                receiver.openLogoEditor(player);
            }

            return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
        } else {
            Kvm906BlockEntity be = (Kvm906BlockEntity)receiver;
            if (ctx.getPlayer() instanceof ServerPlayer player) {
                be.openEditor(player);
            }

            return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
        }
    }
}
