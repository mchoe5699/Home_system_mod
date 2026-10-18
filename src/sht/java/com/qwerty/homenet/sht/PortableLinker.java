package com.qwerty.homenet.sht;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.data.ZoneData;
import com.qwerty.homenet.intercom.Intercom;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** Extends the existing home linker with a Samsung wireless-accessory endpoint. */
@Mod.EventBusSubscriber(modid=Product.ID)
public final class PortableLinker {
 static final String DOCK="SamsungPortableDock";
 static BlockPos selected(ItemStack linker,String key,net.minecraft.world.level.Level l){var t=linker.getTag();return t!=null&&t.contains(key)&&t.getString("Dim").equals(l.dimension().location().toString())?BlockPos.of(t.getLong(key)):null;}
 static void notice(ServerPlayer p,String text){p.displayClientMessage(net.minecraft.network.chat.Component.literal(text),true);}
 static boolean connect(ServerPlayer p,ItemStack linker,BlockPos receiver,BlockPos dock){
  var l=p.level();if(receiver==null||dock==null||!l.isLoaded(receiver)||!l.isLoaded(dock)||!Intercom.inReach(p,dock)&&!Intercom.inReach(p,receiver))return false;
  if(!(l.getBlockEntity(receiver) instanceof ReceiverBlockEntity r)||!Portable.supports(r)){notice(p,"무선 휴대장치를 지원하는 삼성 세대내 기기를 선택하세요");return false;}
  if(!ZoneData.connected(l,receiver,dock)||receiver.distSqr(dock)>1024){notice(p,"같은 단지의 32블록 이내 기기끼리 연결하세요");return false;}
  if(!(l.getBlockEntity(dock) instanceof Charger.Dock d)||d.stack.isEmpty()){notice(p,"충전기에 휴대장치를 먼저 놓으세요");return false;}
  var s=(Station)r;
  if(s.ownsPortable(d.stack)){if(s.portableActive||s.transfer!=0||s.internal==3)s.finish();s.portableRegistered=false;s.portableTicket=java.util.UUID.randomUUID();s.update();for(String k:new String[]{"serial","ticket","base","dimension"})d.stack.getOrCreateTag().remove(k);d.sync();notice(p,"삼성 휴대장치 연결 해제");return true;}
  if(s.portableRegistered&&!p.isShiftKeyDown()){notice(p,"이미 휴대장치가 연결되어 있습니다 · 교체 연결은 웅크리고 클릭");return false;}
  // Both physical endpoints have already passed dimension, scope, range and reach checks.
  boolean ok=Portable.bindFromLinker(p,s,d.stack);if(ok){d.sync();notice(p,"삼성 내부기기와 휴대장치 연결 완료");}return ok;
 }
 @SubscribeEvent public static void use(PlayerInteractEvent.RightClickBlock e){
  if(!(e.getItemStack().getItem() instanceof com.qwerty.homenet.item.HomeLinkerItem))return;
  var l=e.getLevel();var at=e.getPos();var be=l.getBlockEntity(at);var linker=e.getItemStack();
  if(be instanceof Charger.Dock){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);if(e.getEntity() instanceof ServerPlayer p){var receiver=selected(linker,"Wallpad",l);if(receiver!=null)connect(p,linker,receiver,at);else{var t=linker.getOrCreateTag();t.putLong(DOCK,at.asLong());t.putString("Dim",l.dimension().location().toString());notice(p,"휴대장치 충전기 선택 · 삼성 무선 지원 내부기기를 클릭하세요");}}return;}
  if(be instanceof ReceiverBlockEntity&&selected(linker,DOCK,l)!=null){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);if(e.getEntity() instanceof ServerPlayer p){if(connect(p,linker,at,selected(linker,DOCK,l))){linker.getOrCreateTag().remove(DOCK);linker.getTag().putLong("Wallpad",at.asLong());}}}
 }
}
