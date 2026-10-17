package com.qwerty.homenet.sht;
import com.qwerty.homenet.blockentity.*;
import com.qwerty.homenet.registry.ModBlocks;
import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.data.ZoneData;
import net.minecraft.core.*;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Block;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import java.util.*;
/** Actual client packet and renderer checks, deliberately without image capture. */
final class VideoProbe {
 static final List<BlockPos> positions=new ArrayList<>();
 static void setup(MinecraftServer server){
  var l=server.overworld();ZoneData.get(l).add("physical video test",1,-100,-100,100,100);
  List<Block> blocks=List.of(ModBlocks.KVM_906.get(),ModBlocks.KVM_904.get(),ModBlocks.KVM_930.get(),ModBlocks.KVM_960.get(),ModBlocks.KVM_933.get(),ModBlocks.KVM_940.get(),ModBlocks.KVM_931.get(),ModBlocks.KVM_931_LAMP.get(),ModBlocks.KVM_932T.get(),ModBlocks.KVM_932.get(),ModBlocks.KVM_936.get(),ModBlocks.KVM_931T.get(),ModBlocks.KVM_940R3.get(),ModBlocks.VIDEO_PHONE.get(),ModBlocks.WALLPAD.get(),ModBlocks.HA_4100H.get(),Product.STATION.get());
  for(int i=0;i<blocks.size();i++){
   var at=new BlockPos(i*2,80,6);var door=at.south(2);positions.add(at);
   l.setBlockAndUpdate(at,blocks.get(i).defaultBlockState().setValue(WallMountedBlock.FACING,Direction.values()[2+i%4]));
   l.setBlockAndUpdate(door,ModBlocks.DOOR_STATION.get().defaultBlockState());
   var receiver=(ReceiverBlockEntity)l.getBlockEntity(at);receiver.onLoad();var camera=(DoorStationBlockEntity)l.getBlockEntity(door);
   ClientRun.check(camera.toggleReceiver(at),"Video probe link "+i);ClientRun.check(camera.startCall("")==CallerBlockEntity.CallResult.OK,"Video probe call "+i);
  }
 }
 @SuppressWarnings({"rawtypes","unchecked"}) static boolean verify(Minecraft mc){
  var videoType=RenderType.entityTranslucentEmissive(com.qwerty.homenet.HomeNet.id("textures/block/ha4100h/video_white.png"));
  for(var at:positions){
   var b=mc.level.getBlockEntity(at);if(b==null)return false;
   var renderer=mc.getBlockEntityRenderDispatcher().getRenderer(b);ClientRun.check(renderer!=null,"Missing renderer "+at);
   var capture=new MeshProbe.Capture();var ignore=new MeshProbe.Capture();ignore.save=false;
   ((net.minecraft.client.renderer.blockentity.BlockEntityRenderer)renderer).render(b,0,new PoseStack(),type->type==videoType?capture:ignore,15728880,0);
   if(capture.vertices.size()<48*36*4)return false;
   if(b instanceof Station){
    double minX=capture.vertices.stream().mapToDouble(MeshProbe.V::x).min().orElseThrow(),maxX=capture.vertices.stream().mapToDouble(MeshProbe.V::x).max().orElseThrow();
    double minY=capture.vertices.stream().mapToDouble(MeshProbe.V::y).min().orElseThrow(),maxY=capture.vertices.stream().mapToDouble(MeshProbe.V::y).max().orElseThrow();
    ClientRun.check(Math.abs(maxY-minY-144*Surface.SCALE)<.00001,"SHT world video height wrong");
   }
  }
  System.out.println("PHYSICAL_VIDEO_PASS: 13 KVM models, generic video phone, wallpad, HA-4100H, SHT-101C; incoming calls before opening UI; four wall orientations; actual video vertices");return true;
 }
 @SuppressWarnings({"rawtypes","unchecked"}) static boolean cleared(Minecraft mc){
  var videoType=RenderType.entityTranslucentEmissive(com.qwerty.homenet.HomeNet.id("textures/block/ha4100h/video_white.png"));
  for(var at:positions){var b=mc.level.getBlockEntity(at);if(b==null)return false;var capture=new MeshProbe.Capture();var ignore=new MeshProbe.Capture();ignore.save=false;
   ((net.minecraft.client.renderer.blockentity.BlockEntityRenderer)mc.getBlockEntityRenderDispatcher().getRenderer(b)).render(b,0,new PoseStack(),t->t==videoType?capture:ignore,15728880,0);
   if(!capture.vertices.isEmpty())return false;
  }
  System.out.println("PHYSICAL_VIDEO_CLEAR_PASS: all 17 physical screens clear after call termination");return true;
 }

}
