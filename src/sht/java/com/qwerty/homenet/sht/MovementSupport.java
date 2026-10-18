package com.qwerty.homenet.sht;

import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.move.DeviceMoves;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Add the new product namespace to the existing shared movement engine, without replacing it. */
public final class MovementSupport {
 public static boolean additionalProduct(BlockState state){var id=ForgeRegistries.BLOCKS.getKey(state.getBlock());return id!=null&&id.getNamespace().equals(Product.ID);}
 public static boolean additionalBusy(BlockEntity entity){
  if(entity instanceof Station s)return s.phone>0||s.internal>0||!s.docked||s.audio||s.monitor>0||s.awayTime>0||s.resetTime>0||s.test||!s.alarm.isEmpty();
  if(entity instanceof Charger.Dock d&&!d.stack.isEmpty()){var s=Portable.base(d.getLevel(),d.stack);return s!=null&&(s.portableActive||s.internal>0);}
  return false;
 }
 static void init(){DeviceMoves.registerPositionField("base");}
 /** Held accessories are not block entities; resolve a moved base by its unchanged identity. */
 static void relocatePortable(Level level,ItemStack stack){
  if(level==null||level.isClientSide||!stack.hasTag()||!stack.is(Product.PORTABLE102.get()))return;var tag=stack.getTag();
  if(!tag.hasUUID("serial")||!tag.hasUUID("ticket")||!tag.getString("dimension").equals(level.dimension().location().toString()))return;
  var old=net.minecraft.core.BlockPos.of(tag.getLong("base"));if(level.isLoaded(old)&&level.getBlockEntity(old) instanceof Station s&&s.ownsPortable(stack))return;
  if(!(level instanceof net.minecraft.server.level.ServerLevel server))return;
  for(var entry:DeviceRegistry.get(server).all())if(level.isLoaded(entry.pos())&&level.getBlockEntity(entry.pos()) instanceof Station s&&s.portableRegistered&&s.serial.equals(tag.getUUID("serial"))&&s.portableTicket.equals(tag.getUUID("ticket"))){tag.putLong("base",entry.pos().asLong());return;}
 }
}
