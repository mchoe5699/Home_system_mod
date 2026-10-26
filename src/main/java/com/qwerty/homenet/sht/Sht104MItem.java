package com.qwerty.homenet.sht;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

final class Sht104MItem extends BlockItem {
    private final String model;

    Sht104MItem(Block block) {
        this(block, "SHT-104M");
    }

    Sht104MItem(Block block, String model) {
        super(block, new Properties());
        this.model = model;
    }

    static String variant(ItemStack stack) {
        String type = stack.hasTag() ? stack.getTag().getString("Sht104Variant") : "";
        boolean cs = stack.is(Product.BASE104CS_ITEM.get());
        return type.matches(cs ? "[ABC]" : "[ABCD]") ? type : "C";
    }

    static String variantName(String type) {
        return switch (type) {
            case "A" -> "A(녹화·무선전화·내선)";
            case "B" -> "B(무선전화·내선)";
            case "C" -> "C(기본)";
            case "D" -> "D(경보방송·무선전화·내선)";
            default -> "C(기본)";
        };
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(this.model + " " + variantName(variant(stack)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            String current = variant(stack);
            String types = this.model.equals("SHT-104CS") ? "ABC" : "ABCD";
            String next = types.substring((types.indexOf(current) + 1) % types.length(), (types.indexOf(current) + 1) % types.length() + 1);
            stack.getOrCreateTag().putString("Sht104Variant", next);
            player.displayClientMessage(Component.literal(this.model + " " + variantName(next) + " 파트 선택"), true);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> c) {
        c.accept(Client.itemExtension());
    }
}
