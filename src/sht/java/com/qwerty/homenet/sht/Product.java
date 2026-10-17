package com.qwerty.homenet.sht;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.*;
import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
@Mod(Product.ID)
public final class Product {
 public static final String ID="homenet_sht101c";
 public static ResourceLocation id(String path){return new ResourceLocation(ID,path);}
 static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,ID);
 static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,ID);
 static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,ID);
 static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB,ID);
 public static final RegistryObject<StationBlock> STATION=BLOCKS.register("sht_101c",StationBlock::new);
 public static final RegistryObject<Item> BASE_ITEM=ITEMS.register("sht_101c",()->new BlockItem(STATION.get(),new Item.Properties()){
  public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> c){c.accept(Client.itemExtension());}
 });
 public static final RegistryObject<Item> RADIO=ITEMS.register("wireless_handset",()->new Item(new Item.Properties().stacksTo(1)){
  public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand hand){var stack=p.getItemInHand(hand);if(p instanceof ServerPlayer s){var base=Station.find(s,stack);if(base!=null){if(p.isShiftKeyDown()){stack.getOrCreateTag().putBoolean("power",!stack.getOrCreateTag().getBoolean("power"));}else base.open(s,true);}}return InteractionResultHolder.sidedSuccess(stack,l.isClientSide);}
  public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> c){c.accept(Client.itemExtension());}
 });
 public static final RegistryObject<BlockEntityType<Station>> TYPE=ENTITIES.register("station",()->BlockEntityType.Builder.of(Station::new,STATION.get()).build(null));
 public Product(){IEventBus bus=FMLJavaModLoadingContext.get().getModEventBus();BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);TABS.register("samsung",()->CreativeModeTab.builder().title(Component.literal("SAMSUNG SHT-101C")).icon(()->new ItemStack(BASE_ITEM.get())).displayItems((a,o)->o.accept(BASE_ITEM.get())).build());TABS.register(bus);Wire.init();}
 public static class StationBlock extends WallMountedBlock {
  public StationBlock(){super(Properties.of().strength(1.5f).noOcclusion(),4.36,4.8,11.2,1.5);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(com.qwerty.homenet.block.WallpadBlock.RINGING,false));}
  protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,com.qwerty.homenet.block.WallpadBlock.RINGING);}
  public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,net.minecraft.world.phys.shapes.CollisionContext c){return com.qwerty.homenet.move.DeviceOffsets.shape(super.getShape(s,l,p,c),l,p);}
  public RenderShape getRenderShape(BlockState s){return RenderShape.ENTITYBLOCK_ANIMATED;}
  public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Station(p,s);}
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,TYPE.get(),ReceiverBlockEntity::serverTick);}
  public InteractionResult use(BlockState s,Level l,BlockPos at,Player player,InteractionHand hand,BlockHitResult hit){
   var held=player.getItemInHand(hand);if(held.getItem() instanceof com.qwerty.homenet.item.HomeLinkerItem||held.getItem() instanceof com.qwerty.homenet.item.AnalogLinkerItem)return InteractionResult.PASS;
   if(player instanceof ServerPlayer p&&l.getBlockEntity(at) instanceof Station b){if(held.is(RADIO.get()))b.replace(p);else if(!b.docked&&cradle(s,l,at,hit))b.replace(p);else b.open(p,false);}
   return InteractionResult.sidedSuccess(l.isClientSide);
  }
  public static boolean cradle(BlockState s,Level l,BlockPos at,BlockHitResult hit){var v=hit.getLocation().subtract(Vec3.atLowerCornerOf(at)).subtract(com.qwerty.homenet.move.DeviceOffsets.get(l,at));double across=switch(s.getValue(FACING)){case NORTH->.5-v.x;case SOUTH->v.x-.5;case EAST->.5-v.z;case WEST->v.z-.5;default->10;};double x=523.5+across/(.5/824),y=700-(v.y-.30)/(.5/824);return x>=145&&x<=294&&y>=126&&y<=640;}
  public void onRemove(BlockState s,Level l,BlockPos p,BlockState n,boolean moving){if(!s.is(n.getBlock())&&l.getBlockEntity(p) instanceof Station b)b.onBroken();super.onRemove(s,l,p,n,moving);}
 }
}
