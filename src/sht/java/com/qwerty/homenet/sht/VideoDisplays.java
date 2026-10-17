package com.qwerty.homenet.sht;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.blockentity.*;
import com.qwerty.homenet.client.kvm.*;
import com.qwerty.homenet.client.logo.LegacyLogoRenderer;
import com.qwerty.homenet.client.move.MovingRenderer;
import com.qwerty.homenet.registry.ModBlockEntities;
import com.qwerty.homenet.video.VideoFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid=Product.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class VideoDisplays {
 private static final Map<BlockPos,BlockPos> SIGNALS=new HashMap<>();
 private static Object level;
 static void receive(Wire.Video packet){
  var l=Minecraft.getInstance().level;SIGNALS.clear();level=l;
  if(l!=null&&l.dimension().location().toString().equals(packet.dimension()))for(var s:packet.signals())SIGNALS.put(s.receiver(),s.camera());
 }
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void register(EntityRenderersEvent.RegisterRenderers e){
  MovingRenderer.register(e,ModBlockEntities.KVM_906.get(),c->new Display<>(new Kvm906Renderer(c)));
  MovingRenderer.register(e,ModBlockEntities.RECEIVER.get(),c->new Display<>(new LegacyLogoRenderer<>(c)));
  MovingRenderer.register(e,ModBlockEntities.WALLPAD.get(),c->new Display<>(new LegacyLogoRenderer<>(c)));
 }
 static class Display<T extends ReceiverBlockEntity> implements BlockEntityRenderer<T>{
  final BlockEntityRenderer<T> original;
  // A renderer is shared by all instances. Keep an independent video cache per camera.
  final Map<BlockPos,KvmCameraView> views=new LinkedHashMap<>(16,.75f,true){protected boolean removeEldestEntry(Map.Entry<BlockPos,KvmCameraView> e){return size()>64;}};
  Display(BlockEntityRenderer<T> original){this.original=original;}
  public boolean shouldRenderOffScreen(T b){return original.shouldRenderOffScreen(b);}
  public int getViewDistance(){return original.getViewDistance();}
  public void render(T b,float tick,PoseStack p,MultiBufferSource out,int light,int overlay){
   original.render(b,tick,p,out,light,overlay);
   if(level!=b.getLevel()||!WorldVideo.supports(b))return;
   var camera=SIGNALS.get(b.getBlockPos());if(camera==null)return;
   p.pushPose();p.translate(.5,0,.5);
   float turn=switch(b.getBlockState().getValue(WallMountedBlock.FACING)){case EAST->90;case SOUTH->180;case WEST->270;default->0;};
   p.mulPose(Axis.YN.rotationDegrees(turn));p.translate(-.5,0,-.5);
   float x1,y1,x2,y2,z;VideoFormat format=VideoFormat.COLOR;
   if(b instanceof Kvm906BlockEntity k){
    var m=k.design().model();format=m.videoFormat();
    if(m.nativeCase()){
     boolean is932=m.is932();
     x1=nativeX(is932?365:355);x2=nativeX(196);y1=nativeY(is932?207:205);y2=nativeY(is932?78:91);z=16+(15.23f-16)*.45f;
    }else{
     float scale=(float)m.size();p.translate(.5,.5,1);p.scale(scale,scale,scale);p.translate(-.5,-.5,-1);
     x1=classic(11.52f-161*.032f);x2=classic(11.52f-85*.032f);y1=classic(11.52f-(m.indicators?81:75)*.032f);y2=classic(11.52f-(m.indicators?25:28)*.032f);z=16-(16-14.40f)*.59532034f;
    }
   }else if(b instanceof WallpadBlockEntity){x1=3.8f;y1=4.8f;x2=12.2f;y2=11.2f;z=15.075f;}
   else{x1=5.75f;y1=4.875f;x2=13.25f;y2=11.1875f;z=14.995f;format=VideoFormat.MONOCHROME;}
   views.computeIfAbsent(camera,key->new KvmCameraView()).drawWall(p,out,camera,x1,y1,x2,y2,z,2,2,format);
   p.popPose();
  }
 }
 static float classic(float v){return 8+(v-8)/1.1f;}
 static float nativeX(float v){return 8+(12.06f-v/500*8.12f-8)*.9f;}
 static float nativeY(float v){return 8+(12-v/500*8-8)*.9f;}
}
