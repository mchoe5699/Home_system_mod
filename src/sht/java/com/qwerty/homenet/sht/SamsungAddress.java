package com.qwerty.homenet.sht;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.intercom.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** Direct commissioning for Samsung models whose baseline has no operational screen. */
@Mod.EventBusSubscriber(modid=Product.ID)
public final class SamsungAddress {
 static boolean legacy(ReceiverBlockEntity r){return !(r instanceof Station)&&r.manufacturer().equalsIgnoreCase("SAMSUNG");}
 @SubscribeEvent public static void open(PlayerInteractEvent.RightClickBlock e){
  if(e.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND||!e.getItemStack().isEmpty())return;
  if(e.getLevel().getBlockEntity(e.getPos()) instanceof ReceiverBlockEntity r&&legacy(r)){
   e.setCanceled(true);e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
   if(e.getEntity() instanceof ServerPlayer p&&Intercom.inReach(p,e.getPos()))Wire.address(p,r.getBlockPos(),r.getUnit());
  }
 }
 static boolean set(ServerPlayer p,BlockPos at,String raw){
  if(!p.level().isLoaded(at)||!Intercom.inReach(p,at)||!(p.level().getBlockEntity(at) instanceof ReceiverBlockEntity r)||!legacy(r)||r.getCallState()!=CallState.IDLE)return false;
  String unit=UnitAddress.canonical(raw);if(unit.isEmpty()&&!raw.isBlank()){r.noticeTo(p,"동-호 형식으로 입력하세요");return false;}
  var tag=r.saveWithoutMetadata();tag.putString("Unit",unit);r.load(tag);r.setChanged();r.onLoad();r.syncScreens();Wire.address(p,at,r.getUnit());return true;
 }
}
