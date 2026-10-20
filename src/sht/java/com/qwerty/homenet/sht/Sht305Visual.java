package com.qwerty.homenet.sht;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;

/** Exposed SHT-305V shell: a deep wall case, fixed receiver cradle and shallow raised keys. */
final class Sht305Visual {
 static final float W=640,H=600,S=(float)(Station305.Mounted.HEIGHT/H);
 static final ResourceLocation INK=Product.id("sht305v/ink.png");
 static void part(PoseStack p,MultiBufferSource out,int light,String name,float x,float y,float w,float h,float z,float d,float r,int rgb,float curve){
  Surface.solid(new Surface.Solid(name,x,y,w,h,z,d,r,String.format("#%06x",rgb),"sht305v",curve),p,out,light);
 }
 static void dot(PoseStack p,MultiBufferSource out,float x,float y,float size,int rgb){
  var b=out.getBuffer(RenderType.entityTranslucentEmissive(Product.id("textures/solid.png")));
  for(int i=0;i<24;i++){double a=i*Math.PI/12,c=(i+1)*Math.PI/12;for(float[] v:new float[][]{{x,y},{x+(float)Math.cos(a)*size,y+(float)Math.sin(a)*size},{x+(float)Math.cos(c)*size,y+(float)Math.sin(c)*size},{x,y}})
   Surface.vertex(b,p.last(),v[0],v[1],100,0,0,1,rgb,15728880,.5f,.5f);}
 }
 static void mesh(PoseStack p,MultiBufferSource out){int l=0;
  // Complete surface-mounted case: the back stays behind the rounded front by 88 units.
  part(p,out,l,"305_back",190,0,450,600,0,67,18,0xd1c9b0,0);
  part(p,out,l,"305_body",196,0,444,600,31,57,18,0xe9e2cb,1.2f);
  part(p,out,l,"305_side_joint",194,13,3,574,68,22,1,0xc5bba0,0);
  part(p,out,l,"305_handset_bridge",154,36,50,526,19,63,12,0xd9d1b9,1);
  part(p,out,l,"305_cradle",12,18,164,564,41,36,26,0xc9c0a6,2);
  part(p,out,l,"305_cradle_floor",36,68,122,466,75,8,17,0xb3a993,0);
  part(p,out,l,"305_hook_top",145,67,49,74,80,7,10,0xe7dfc5,0);
  part(p,out,l,"305_hook_bottom",145,461,49,74,80,7,10,0xe7dfc5,0);
  // The louvers belong to the exposed left wall, beside the receiver cradle.
  part(p,out,l,"305_speaker_grille",161,217,37,211,79,5,8,0xd9d1b9,0);
  for(int i=0;i<13;i++)part(p,out,l,"305_speaker_slot_"+i,164,226+i*15,31,2.5f,84,1,1.2f,0x666457,0);
  // One inset black glass panel, with a 4:3 monochrome CRT and alarm windows to its right.
  part(p,out,l,"305_screen_ledge",225,19,400,239,87,4,11,0xeee7d1,0);
  part(p,out,l,"305_screen_bezel",231,24,389,233,90,4,8,0x17191c,0);
  part(p,out,l,"305_crt_recess",246,45,268,198,94,1,4,0x292b31,0);
  part(p,out,l,"305_crt_glass",254,57,249,173,95,1,2,0x656276,0);
  part(p,out,l,"305_status_glass",529,30,85,219,94,1,2,0x171a20,0);
  for(int i=0;i<4;i++){
   float y=89+i*39;part(p,out,l,"305_alarm_key_border_"+i,541,y,61,32,95,.6f,2,0x858b88,0);
   part(p,out,l,"305_alarm_key_"+i,544,y+3,55,26,95.6f,.5f,1,0x20262a,0);
  }
  // The lower right strip rises gently from the otherwise flat ivory face.
  part(p,out,l,"305_control_strip",518,259,119,333,87,3,9,0xe1dac2,1.5f);
  for(int i=0;i<3;i++){
   float y=354+i*58;part(p,out,l,"305_round_rim_"+i,475,y,37,37,89,1.1f,18,0xb0aa99,0);
   part(p,out,l,"305_round_key_"+i,479,y+4,29,29,90.1f,1.5f,14,0x777e91,1);
  }
  for(int i=0;i<5;i++){
   float y=304+i*34;part(p,out,l,"305_control_slot_"+i,566,y+3,9,9,91,.1f,4,0x5b594e,0);
   part(p,out,l,"305_pill_shadow_"+i,585,y,43,12,91,1,5,0xaaa38d,0);
   part(p,out,l,"305_pill_key_"+i,587,y+1,39,9,92,1.3f,4,0xf1ebd9,.4f);
  }
  for(int i=0;i<2;i++){
   float x=i==0?477:575;
   part(p,out,l,"305_large_rim_"+i,x,505,49,49,89,1.2f,24,0xa79f88,0);
   part(p,out,l,"305_large_key_"+i,x+3,508,43,43,90.2f,2.2f,21,0xf4eedb,1.4f);
  }
  part(p,out,l,"305_power_switch",174,481,11,32,86,1,2,0x504f45,0);
  Surface.rectangle(p,out,0,0,W,H,98,INK,l);
 }
 static void handset(PoseStack p,MultiBufferSource out,int light){
  part(p,out,light,"handset_back",7,8,151,584,52,47,46,0xe3dcc5,2);
  part(p,out,light,"305_handset_face",12,10,141,578,89,16,44,0xf0ead5,6);
  part(p,out,light,"305_handset_end_top",37,25,91,19,91,12,9,0xeae3cd,0);
  part(p,out,light,"305_handset_end_bottom",37,553,91,19,91,12,9,0xeae3cd,0);
 }
 static void draw(PoseStack p,MultiBufferSource out,int light,boolean docked){GeometryCache.draw("sht305v_surface_"+docked,p,out,light,(pose,buffers)->{mesh(pose,buffers);if(docked)handset(pose,buffers,0);});}
 static void pose(Station305 b,PoseStack p){var off=com.qwerty.homenet.move.DeviceOffsets.get(b.getLevel(),b.getBlockPos());p.translate(off.x,off.y,off.z);switch(b.getBlockState().getValue(com.qwerty.homenet.block.WallMountedBlock.FACING)){
  case NORTH->{p.translate(.5,.25,1);p.mulPose(Axis.YP.rotationDegrees(180));}case SOUTH->p.translate(.5,.25,0);
  case EAST->{p.translate(0,.25,.5);p.mulPose(Axis.YP.rotationDegrees(90));}case WEST->{p.translate(1,.25,.5);p.mulPose(Axis.YP.rotationDegrees(-90));}default->{}}
  p.scale(S,S,S);p.translate(-W/2,H-700,0);
 }
 static final class Renderer implements BlockEntityRenderer<Station305>{
  final com.qwerty.homenet.client.kvm.KvmCameraView video=new com.qwerty.homenet.client.kvm.KvmCameraView();
  public void render(Station305 b,float tick,PoseStack p,MultiBufferSource out,int light,int overlay){
   if(DeviceVisibility.hidden(b,GeometryCache.bounds("sht305v_surface_"+b.docked),q->pose(b,q)))return;
   p.pushPose();pose(b,p);draw(p,out,light,b.docked);
   var camera=b.camera();if(camera!=null){p.pushPose();p.scale(16,16,16);video.drawWall(p,out,camera,263,700-223,231,156,99,b.brightness,b.contrast,com.qwerty.homenet.video.VideoFormat.MONOCHROME);p.popPose();}
   int bits=b.lights();if((bits&Indicators.FIRE)!=0)dot(p,out,535,104,5,0xf07850);
   if((bits&Indicators.GAS)!=0)dot(p,out,535,143,5,0xe1bf59);
   if((bits&Indicators.SECURITY)!=0)dot(p,out,535,182,5,0x98dc81);
   if((bits&Indicators.EMERGENCY)!=0)dot(p,out,535,221,5,0xf07850);
   if((bits&Indicators.DOOR)!=0)dot(p,out,565,327,4,0x9bd294);
   if((bits&Indicators.GUARD)!=0)dot(p,out,565,363,4,0x9bd294);
   if((bits&Indicators.ARMED)!=0)dot(p,out,565,430,4,0x9bd294);
   if((bits&Indicators.AWAY)!=0)dot(p,out,565,464,4,0x9bd294);
   p.popPose();
  }
 }
}
