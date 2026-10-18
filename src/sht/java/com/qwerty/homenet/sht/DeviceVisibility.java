package com.qwerty.homenet.sht;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.*;
import java.util.*;
import java.util.function.Consumer;

/** Skip vertex submission and video work when an opaque wall covers the device. */
final class DeviceVisibility {
 private record Result(AABB box,Vec3 eye,long expires,boolean hidden){}
 private static final Map<BlockEntity,Result> LAST=new WeakHashMap<>();
 static void clear(){LAST.clear();}
 static boolean hidden(BlockEntity device,AABB local,Consumer<PoseStack> placement){
  if(local==null)return false; // Build each shared mesh once before its physical bounds are known.
  var mc=Minecraft.getInstance();if(mc.level==null||mc.getCameraEntity()==null)return false;
  var pose=new PoseStack();placement.accept(pose);var matrix=pose.last().pose();var v=new org.joml.Vector3f();
  double minX=Double.POSITIVE_INFINITY,minY=minX,minZ=minX,maxX=Double.NEGATIVE_INFINITY,maxY=maxX,maxZ=maxX;
  for(int i=0;i<8;i++){matrix.transformPosition((float)((i&1)==0?local.minX:local.maxX),(float)((i&2)==0?local.minY:local.maxY),(float)((i&4)==0?local.minZ:local.maxZ),v);minX=Math.min(minX,v.x);minY=Math.min(minY,v.y);minZ=Math.min(minZ,v.z);maxX=Math.max(maxX,v.x);maxY=Math.max(maxY,v.y);maxZ=Math.max(maxZ,v.z);}
  var box=new AABB(minX,minY,minZ,maxX,maxY,maxZ).move(device.getBlockPos());
  var eye=mc.gameRenderer.getMainCamera().getPosition();long now=System.nanoTime();var previous=LAST.get(device);
  if(previous!=null&&previous.box.equals(box)&&previous.eye.distanceToSqr(eye)<.0025&&now<previous.expires)return previous.hidden;
  boolean hidden=OpaqueCover.covered(mc.level,eye,box,mc.getCameraEntity());LAST.put(device,new Result(box,eye,now+100_000_000L,hidden));return hidden;
 }
}
