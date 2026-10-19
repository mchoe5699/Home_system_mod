package com.qwerty.homenet.sht;

import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.data.ZoneData;
import com.qwerty.homenet.intercom.Intercom;
import com.qwerty.homenet.item.HomeLinkerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Complete the indoor receiver -> HT-100S Home Linker path before block UIs consume the click. */
@Mod.EventBusSubscriber(modid=Product.ID)
public final class Ht100sLinker {
 private Ht100sLinker(){}
 static boolean connect(ServerPlayer player,ItemStack linker,BlockPos target){
  var level=player.level();if(!Intercom.inReach(player,target)||!level.isLoaded(target)||!(level.getBlockEntity(target) instanceof Ht100s.Unit phone))return false;
  var tag=linker.getTag();BlockPos source=tag!=null&&tag.contains("Wallpad")&&tag.getString("Dim").equals(level.dimension().location().toString())?BlockPos.of(tag.getLong("Wallpad")):null;
  if(source==null||!level.isLoaded(source)||!(level.getBlockEntity(source) instanceof ReceiverBlockEntity receiver)){notice(player,"홈링커로 세대 내부기기를 먼저 선택하세요");return false;}
  if(receiver.getUnit().isBlank()){notice(player,"선택한 세대 내부기기의 동·호 번호를 먼저 설정하세요");return false;}
  if(!ZoneData.connected(level,source,target)){notice(player,"HT-100S와 세대 내부기기는 같은 단지에 있어야 합니다");return false;}
  String unit=receiver.getUnit();phone.setUnit(DeviceRegistry.normalize(unit).equals(DeviceRegistry.normalize(phone.unit))?"":unit);
  notice(player,phone.unit.isEmpty()?"HT-100S 세대 연결 해제":"HT-100S 세대 "+phone.unit+" 연결");return true;
 }
 private static void notice(ServerPlayer p,String message){p.displayClientMessage(Component.literal(message),true);}
 @SubscribeEvent public static void use(PlayerInteractEvent.RightClickBlock e){
  if(!(e.getItemStack().getItem() instanceof HomeLinkerItem))return;
  var l=e.getLevel();var at=e.getPos();var be=l.getBlockEntity(at);
  if(be instanceof ReceiverBlockEntity r){
   if(PortableLinker.selected(e.getItemStack(),PortableLinker.DOCK,l)!=null)return;
   e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);
   if(e.getEntity() instanceof ServerPlayer p){var tag=e.getItemStack().getOrCreateTag();tag.putLong("Wallpad",at.asLong());tag.putString("Dim",l.dimension().location().toString());notice(p,"세대 내부기기 선택: "+(r.getUnit().isBlank()?"동·호 번호 미설정":r.getUnit())+" · HT-100S를 클릭하세요");}return;
  }
  if(!(be instanceof Ht100s.Unit))return;
  e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);
  if(e.getEntity() instanceof ServerPlayer p)connect(p,e.getItemStack(),at);
 }
}
