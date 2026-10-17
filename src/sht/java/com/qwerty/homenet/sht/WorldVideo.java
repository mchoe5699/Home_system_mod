package com.qwerty.homenet.sht;

import com.qwerty.homenet.blockentity.*;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.data.ZoneData;
import com.qwerty.homenet.intercom.CallState;
import com.qwerty.homenet.analog.AnalogLobbyEndpoint;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Nearby physical displays receive call video without opening an operation screen. */
@Mod.EventBusSubscriber(modid=Product.ID)
public final class WorldVideo {
 public record Signal(BlockPos receiver,BlockPos camera){}
 private static final Map<UUID,List<Signal>> LAST=new HashMap<>();
 private static final Map<UUID,String> DIMENSIONS=new HashMap<>();
 public static boolean supports(ReceiverBlockEntity b){
  // HA-4100H and SHT-101C already have their own live physical-screen renderers.
  return !(b instanceof Station)&&!(b instanceof Ha4100hBlockEntity)&&!(b instanceof Sht5500sBlockEntity)
   &&(b instanceof Kvm906BlockEntity||b.kind()==DeviceRegistry.Kind.VIDEO_PHONE||b instanceof WallpadBlockEntity);
 }
 public static BlockPos camera(ReceiverBlockEntity b){
  if(!supports(b)||!(b.getLevel() instanceof ServerLevel l))return null;
  if(b instanceof Kvm906BlockEntity k&&!k.power())return null;
  BlockPos at=b.getCallState()==CallState.IDLE?null:b.getCaller();
  if(at==null&&b instanceof Kvm906BlockEntity k&&k.screenOn())at=b.linkedCamera();
  if(at==null||!l.isLoaded(at)||!ZoneData.connected(l,b.getBlockPos(),at))return null;
  var source=l.getBlockEntity(at);
  if(source instanceof DoorStationBlockEntity door)return door.isDoorPhone()?null:at;
  if(source instanceof AnalogLobbyEndpoint lobby){
   var exchange=b.getAnalogStation();
   if(exchange!=null&&l.getBlockEntity(exchange) instanceof Kam305BlockEntity kam&&!kam.allowsLobbyVideo(b.getUnit(),lobby))return null;
   var camera=lobby.cameraPosition();
   return camera!=null&&l.isLoaded(camera)&&ZoneData.connected(l,b.getBlockPos(),camera)?camera:null;
  }
  return null;
 }
 @SubscribeEvent public static void tick(TickEvent.LevelTickEvent e){
  if(e.phase!=TickEvent.Phase.END||!(e.level instanceof ServerLevel l)||l.getGameTime()%5!=0)return;
  var signals=new ArrayList<Signal>();
  for(var entry:DeviceRegistry.get(l).all())if(entry.kind().receiver&&l.isLoaded(entry.pos())&&l.getBlockEntity(entry.pos()) instanceof ReceiverBlockEntity b){
   var camera=camera(b);if(camera!=null)signals.add(new Signal(entry.pos(),camera));
  }
  for(var player:l.players()){
   var nearby=signals.stream().filter(s->player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(s.receiver()))<=64*64).limit(4096).toList();
   var dim=l.dimension().location().toString();var id=player.getUUID();
   if(l.getGameTime()%20==0||!nearby.equals(LAST.get(id))||!dim.equals(DIMENSIONS.get(id))){Wire.video(player,dim,nearby);LAST.put(id,nearby);DIMENSIONS.put(id,dim);}
  }
  var online=l.getServer().getPlayerList();LAST.keySet().removeIf(id->online.getPlayer(id)==null);DIMENSIONS.keySet().retainAll(LAST.keySet());
 }
}
