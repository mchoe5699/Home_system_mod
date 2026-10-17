package com.qwerty.homenet.samsung;
public final class ShtBlockItem extends net.minecraft.world.item.BlockItem {
 public ShtBlockItem(net.minecraft.world.level.block.Block b){super(b,new net.minecraft.world.item.Item.Properties());}
 @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> c){c.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){return ShtClient.itemRenderer();}});}
}
