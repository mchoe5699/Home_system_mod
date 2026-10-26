package com.qwerty.homenet.sht;

import com.qwerty.homenet.item.RfCardItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;

final class S1Card extends RfCardItem {
    S1Card() {
        super(new Properties().stacksTo(1));
    }

    static String id(ItemStack stack) {
        return RfCardItem.cardId(stack);
    }
}
