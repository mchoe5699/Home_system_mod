package com.qwerty.homenet.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;

public final class InsertCardItem extends Item {
    public InsertCardItem(Properties p) {
        super(p);
    }

    @Override
    public void inventoryTick(ItemStack s, Level l, Entity e, int slot, boolean selected) {
        if (!l.isClientSide) {
            RfCardItem.cardId(s);
        }
    }

    @Override
    public void appendHoverText(ItemStack s, Level l, List<Component> t, TooltipFlag f) {
        t.add(Component.literal("KLP-202R2 초기형·후기형 삽입구에 사용"));
        if (s.hasTag() && s.getTag().contains("CardId")) {
            t.add(Component.literal("카드 번호: " + s.getTag().getString("CardId")));
        }
    }
}
