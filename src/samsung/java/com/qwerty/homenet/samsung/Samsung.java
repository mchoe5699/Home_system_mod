package com.qwerty.homenet.samsung;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.*;
@Mod(Samsung.ID)
public final class Samsung {
 public static final String ID="qwertys_homenet_samsung";
 public static ResourceLocation id(String n){return new ResourceLocation(ID,n);}
 public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,ID);
 public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,ID);
 public static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,ID);
 public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,ID);
 public static final RegistryObject<ShtBlock> SHT=BLOCKS.register("sht_101c",ShtBlock::new);
 public static final RegistryObject<Item> UNIT=ITEMS.register("sht_101c",()->new ShtBlockItem(SHT.get()));
 public static final RegistryObject<Item> CORDLESS=ITEMS.register("sht_101c_cordless",()->new ShtItem());
 public static final RegistryObject<BlockEntityType<Sht101c>> BE=ENTITIES.register("sht_101c",()->BlockEntityType.Builder.of(Sht101c::new,SHT.get()).build(null));
 public static final RegistryObject<CreativeModeTab> TAB=TABS.register("samsung",()->CreativeModeTab.builder().title(net.minecraft.network.chat.Component.literal("SAMSUNG HomeNet")).icon(()->new ItemStack(UNIT.get())).displayItems((p,o)->o.accept(UNIT.get())).build());
 public Samsung(){var bus=FMLJavaModLoadingContext.get().getModEventBus();BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);TABS.register(bus);ShtNetwork.init();}
}
