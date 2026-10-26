package com.qwerty.homenet.item;

import com.qwerty.homenet.blockentity.Ip203apBlockEntity;
import com.qwerty.homenet.blockentity.Klp203rfBlockEntity;
import com.qwerty.homenet.blockentity.Kvm906BlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;

public class ModelChangerItem extends Item {
    public ModelChangerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        if (ctx.getLevel().getBlockEntity(ctx.getClickedPos()) instanceof Klp203rfBlockEntity panel) {
            if (ctx.getPlayer() instanceof ServerPlayer player) {
                panel.openModels(player);
            }

            return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
        } else if (ctx.getLevel().getBlockEntity(ctx.getClickedPos()) instanceof Ip203apBlockEntity phone) {
            if (ctx.getPlayer() instanceof ServerPlayer player) {
                phone.openFamilyModels(player);
            }

            return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
        } else if (ctx.getLevel().getBlockEntity(ctx.getClickedPos()) instanceof Kvm906BlockEntity be) {
            if (ctx.getPlayer() instanceof ServerPlayer player) {
                be.openModels(player);
            }

            return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
        } else {
            if (ctx.getPlayer() instanceof ServerPlayer player) {
                player.displayClientMessage(Component.literal("현재 이 기기에 등록된 형제모델이 없습니다."), true);
            }

            return InteractionResult.PASS;
        }
    }
}
