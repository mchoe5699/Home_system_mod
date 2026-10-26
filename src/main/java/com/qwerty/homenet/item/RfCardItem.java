package com.qwerty.homenet.item;

import com.qwerty.homenet.blockentity.LobbyPhoneBlockEntity;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class RfCardItem extends Item {
    private static final String TAG_ID = "CardId";
    private static final Random RANDOM = new Random();

    public RfCardItem(Properties props) {
        super(props);
    }

    @Nullable
    public static String cardId(ItemStack stack) {
        if (!(stack.getItem() instanceof RfCardItem) && !(stack.getItem() instanceof InsertCardItem)) {
            return null;
        } else {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains("CardId")) {
                return tag.getString("CardId");
            } else {
                String id = String.format(Locale.ROOT, "%08X", RANDOM.nextInt());
                stack.getOrCreateTag().putString("CardId", id);
                return id;
            }
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && (stack.getTag() == null || !stack.getTag().contains("CardId"))) {
            cardId(stack);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("CardId")) {
            tooltip.add(Component.translatable("item.qwertys_homenet.rf_card.number", tag.getString("CardId")).withStyle(ChatFormatting.AQUA));
        }

        tooltip.add(Component.translatable("item.qwertys_homenet.rf_card.tip").withStyle(ChatFormatting.GRAY));
    }

    public static boolean tap(LobbyPhoneBlockEntity be, ItemStack stack) {
        String id = cardId(stack);
        if (id == null) {
            return false;
        } else {
            be.tapCard(id);
            return true;
        }
    }
}
