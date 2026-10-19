package com.qwerty.homenet.sht;

import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.data.ZoneData;
import com.qwerty.homenet.intercom.Intercom;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Route the Home Linker to HT-100, whose camera entity is a caller rather than a legacy door-station entity. */
@Mod.EventBusSubscriber(modid=Product.ID)
public final class Ht100Linker {
 private Ht100Linker(){}
 static boolean connect(ServerPlayer player,ItemStack linker,BlockPos at){
  var level=player.level();
  if(!Intercom.inReach(player,at)||!level.isLoaded(at)||!(level.getBlockEntity(at) instanceof Ht100.Unit camera))return false;
  var tag=linker.getTag();
  BlockPos source=tag!=null&&tag.contains("Wallpad")&&tag.getString("Dim").equals(level.dimension().location().toString())?BlockPos.of(tag.getLong("Wallpad")):null;
  ReceiverBlockEntity receiver=source!=null&&level.isLoaded(source)&&level.getBlockEntity(source) instanceof ReceiverBlockEntity r?r:null;
  if(receiver==null||receiver.getUnit().isBlank()||!ZoneData.connected(level,source,at)){
   player.displayClientMessage(Component.literal("같은 단지의 세대 내부기기를 홈링커로 먼저 선택하세요"),true);return false;
  }
  String unit=receiver.getUnit();
  camera.setUnit(DeviceRegistry.normalize(unit).equals(DeviceRegistry.normalize(camera.unit))?"":unit);
  player.displayClientMessage(Component.literal(camera.unit.isEmpty()?"HT-100 세대 연결 해제":"HT-100 세대 "+camera.unit+" 연결"),true);
  return true;
 }
 @SubscribeEvent public static void use(PlayerInteractEvent.RightClickBlock event){
  if(!(event.getItemStack().getItem() instanceof com.qwerty.homenet.item.HomeLinkerItem)||!(event.getLevel().getBlockEntity(event.getPos()) instanceof Ht100.Unit))return;
  event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);
  if(event.getEntity() instanceof ServerPlayer player)connect(player,event.getItemStack(),event.getPos());
 }
}
