package com.qwerty.homenet.sht;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;

/** SHT-305V, rebuilt from the exposed front/side housing instead of stacked plates. */
final class Sht305Visual {
 static final float W=700,H=600,S=(float)(Station305.Mounted.HEIGHT/H);
 static final ResourceLocation INK=Product.id("sht305v/ink.png");
 static void part(PoseStack p,MultiBufferSource out,int light,String name,float x,float y,float w,float h,float z,float d,float r,int rgb,float curve){
  Surface.solid(new Surface.Solid(name,x,y,w,h,z,d,r,String.format("#%06x",rgb),"sht305v",curve),p,out,light);
 }
 static float smooth(float t){t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
 static float shellEdge(float y,boolean right){
  float r=23,delta=y<r?r-y:y>H-r?y-(H-r):0;
  float inset=r-(float)Math.sqrt(Math.max(0,r*r-delta*delta));
  return right?W-inset:215+inset;
 }
 static float shellZ(float x,float y){
  float edge=Math.max(0,Math.min(x-shellEdge(y,false),shellEdge(y,true)-x));
  float end=Math.max(0,Math.min(y,H-y));
  float band=3*smooth((x-563)/20);
  float seam=1.2f*(1-smooth(Math.abs(x-563)/3));
  return 89-6*(1-smooth(edge/22))-4*(1-smooth(end/18))+band-seam;
 }
 static void shell(PoseStack p,MultiBufferSource out,int light){
  part(p,out,light,"305_closed_rear_housing",215,0,485,600,0,77,23,0xd2c9b4,0);
  var b=out.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));
  for(int row=0;row<100;row++)for(int col=0;col<80;col++)for(int j=0;j<4;j++){
   float y=6*(row+(j==1||j==2?1:0));
   float u=(col+(j>=2?1:0))/80f;
   float left=shellEdge(y,false),right=shellEdge(y,true),x=left+(right-left)*u;
   float z=shellZ(x,y),nx=-(shellZ(x+.3f,y)-shellZ(x-.3f,y))/ .6f;
   float ny=-(shellZ(x,y+.3f)-shellZ(x,y-.3f))/ .6f;
   float n=(float)Math.sqrt(1+nx*nx+ny*ny);
   int color=x>582?0xe6dec9:0xece5d0;
   Surface.vertex(b,p.last(),x,y,z,nx/n,ny/n,1/n,color,light,.5f,.5f);
  }
 }
 static float receiverHalf(float t){
  float ends=(float)Math.pow(Math.abs(2*t-1),1.6);
  float corner=smooth(Math.min(t,1-t)*600/45);
  return (63+16*ends)*(.52f+.48f*corner);
 }
 static float receiverX(float t,float u){return 96+u*receiverHalf(t);}
 static float receiverZ(float t,float u){return 97+20*(1-u*u)+3*(float)Math.pow(Math.abs(2*t-1),2);}
 static void receiver(PoseStack p,MultiBufferSource out,int light){
  part(p,out,light,"handset_back",17,7,160,586,38,56,40,0xdcd2bb,1);
  var b=out.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));
  for(int row=0;row<96;row++)for(int col=0;col<24;col++)for(int j=0;j<4;j++){
   float t=(row+(j==1||j==2?1:0))/96f,u=-1+2*(col+(j>=2?1:0))/24f;
   float x=receiverX(t,u),y=7+t*586,z=receiverZ(t,u);
   float nx=u*.53f,ny=-.04f*(float)Math.sin(2*Math.PI*t),n=(float)Math.sqrt(1+nx*nx+ny*ny);
   Surface.vertex(b,p.last(),x,y,z,nx/n,ny/n,1/n,0xf0ead8,light,.5f,.5f);
  }
  for(int side:new int[]{-1,1})for(int row=0;row<96;row++)for(int col=0;col<6;col++)for(int j=0;j<4;j++){
   float t=(row+(j==1||j==2?1:0))/96f,v=(col+(j>=2?1:0))/6f;
   float x=receiverX(t,side),y=7+t*586,z=41+(receiverZ(t,side)-41)*v;
   Surface.vertex(b,p.last(),x,y,z,side,0,0,0xe2d9c3,light,.5f,.5f);
  }
 }
 static void dot(PoseStack p,MultiBufferSource out,float x,float y,float size,int rgb){
  var b=out.getBuffer(RenderType.entityTranslucentEmissive(Product.id("textures/solid.png")));
  for(int i=0;i<24;i++){
   double a=i*Math.PI/12,c=(i+1)*Math.PI/12;
   for(float[] v:new float[][]{{x,y},{x+(float)Math.cos(a)*size,y+(float)Math.sin(a)*size},{x+(float)Math.cos(c)*size,y+(float)Math.sin(c)*size},{x,y}})
    Surface.vertex(b,p.last(),v[0],v[1],102,0,0,1,rgb,15728880,.5f,.5f);
  }
 }
 static void mesh(PoseStack p,MultiBufferSource out){int l=0;
  shell(p,out,l);
  // The bridge and cradle are exposed between the wide receiver and the separate wall case.
  part(p,out,l,"305_cradle_housing",4,17,196,570,13,65,28,0xd5cbb4,1);
  part(p,out,l,"305_cradle_recess",31,59,141,479,78,3,20,0xbcb19b,0);
  part(p,out,l,"305_bridge",172,57,48,488,42,38,11,0xdad0b9,1);
  part(p,out,l,"305_hook_top",163,88,39,59,79,11,11,0xece5d0,1);
  part(p,out,l,"305_hook_bottom",163,453,39,59,79,11,11,0xece5d0,1);
  // Horizontal vents and a low rocker sit on the left side, behind the receiver.
  part(p,out,l,"305_speaker_recess",179,202,35,229,75,7,4,0xc5bba3,0);
  for(int i=0;i<17;i++)part(p,out,l,"305_speaker_slot_"+i,182,211+i*12.5f,29,2.4f,82,.35f,1,0x68685d,0);
  part(p,out,l,"305_power_plate",181,478,22,40,76,3,2,0xc7bfac,0);
  part(p,out,l,"305_power_switch",184,483,16,29,79,3,2,0xeae2cb,1);

  // Shallow glass fascia, square CRT throat and a separate narrow warning window.
  part(p,out,l,"305_fascia_edge",235,16,449,251,88,2,12,0xf5edda,0);
  part(p,out,l,"305_fascia",240,21,439,242,90,7,8,0x15171b,0);
  part(p,out,l,"305_crt_reveal",252,37,298,211,97,.5f,3,0x414147,0);
  part(p,out,l,"305_crt_throat",258,44,286,198,97.5f,.5f,2,0x202127,0);
  part(p,out,l,"305_crt_glass",265,54,271,178,98,.7f,2,0x676478,.4f);
  part(p,out,l,"305_status_glass",556,28,117,226,97,.7f,2,0x17191e,0);
  for(int i=0;i<4;i++){
   float y=87+i*38;
   part(p,out,l,"305_warning_outline_"+i,564,y,99,33,97.7f,.3f,2,0x747a78,0);
   part(p,out,l,"305_warning_window_"+i,567,y+3,93,27,98,.4f,1,0x282d31,0);
  }

  // Controls are small, shallow mouldings. The lower-right body itself curves forward.
  for(int i=0;i<3;i++){
   float y=313+i*54;
   part(p,out,l,"305_round_shadow_"+i,499,y,42,42,90,.5f,21,0x9a9588,0);
   part(p,out,l,"305_round_bevel_"+i,503,y+4,34,34,90.5f,1.5f,17,0xcac8c0,0);
   part(p,out,l,"305_round_key_"+i,506,y+7,28,28,92,1.7f,14,0x747d90,1.3f);
  }
  for(int i=0;i<5;i++){
   float y=314+i*37;
   part(p,out,l,"305_lamp_lens_"+i,601,y+2,10,10,92,.4f,5,0x41443f,0);
   part(p,out,l,"305_switch_recess_"+i,622,y,52,13,92,.5f,6,0xaea590,0);
   part(p,out,l,"305_switch_rim_"+i,625,y+2,46,9,92.5f,1.1f,4,0xf0ead8,.4f);
   part(p,out,l,"305_switch_key_"+i,632,y+3,32,7,93.6f,.6f,3,0xe4dcc8,.2f);
  }
  for(int i=0;i<2;i++){
   float x=i==0?495:617;
   part(p,out,l,"305_large_ring_"+i,x,494,56,56,90,.6f,28,0xa9a18e,0);
   part(p,out,l,"305_large_bevel_"+i,x+3,497,50,50,90.6f,1.6f,25,0xe0d9c7,0);
   part(p,out,l,"305_large_button_"+i,x+7,501,42,42,92.2f,2.1f,21,0xf0ead8,1.5f);
  }
  for(int i=0;i<3;i++){
   float y=341+i*34;
   part(p,out,l,"305_side_slider_well_"+i,695,y,4,21,51,1,1,0x837c6b,0);
   part(p,out,l,"305_side_slider_"+i,696,y+7,5,7,52,2,1,0xdad1bb,0);
  }
  Surface.rectangle(p,out,0,0,W,H,100,INK,l);
 }
 static void draw(PoseStack p,MultiBufferSource out,int light,boolean docked){
  GeometryCache.draw("sht305v_surface_"+docked,p,out,light,(pose,buffers)->{
   mesh(pose,buffers);if(docked)receiver(pose,buffers,0);
  });
 }
 static void pose(Station305 b,PoseStack p){
  var off=com.qwerty.homenet.move.DeviceOffsets.get(b.getLevel(),b.getBlockPos());
  p.translate(off.x,off.y,off.z);
  switch(b.getBlockState().getValue(com.qwerty.homenet.block.WallMountedBlock.FACING)){
   case NORTH->{p.translate(.5,.25,1);p.mulPose(Axis.YP.rotationDegrees(180));}
   case SOUTH->p.translate(.5,.25,0);
   case EAST->{p.translate(0,.25,.5);p.mulPose(Axis.YP.rotationDegrees(90));}
   case WEST->{p.translate(1,.25,.5);p.mulPose(Axis.YP.rotationDegrees(-90));}
   default->{}
  }
  p.scale(S,S,S);p.translate(-W/2,H-700,0);
 }
 static final class Renderer implements BlockEntityRenderer<Station305>{
  final com.qwerty.homenet.client.kvm.KvmCameraView video=new com.qwerty.homenet.client.kvm.KvmCameraView();
  public void render(Station305 b,float tick,PoseStack p,MultiBufferSource out,int light,int overlay){
   if(DeviceVisibility.hidden(b,GeometryCache.bounds("sht305v_surface_"+b.docked),q->pose(b,q)))return;
   p.pushPose();pose(b,p);draw(p,out,light,b.docked);
   var camera=b.camera();if(camera!=null){
    p.pushPose();p.scale(16,16,16);
    video.drawWall(p,out,camera,273,700-227,255,165,101,b.brightness,b.contrast,com.qwerty.homenet.video.VideoFormat.MONOCHROME);
    p.popPose();
   }
   int bits=b.lights();
   if((bits&Indicators.FIRE)!=0)dot(p,out,575,103,5,0xf07850);
   if((bits&Indicators.GAS)!=0)dot(p,out,575,141,5,0xe1bf59);
   if((bits&Indicators.SECURITY)!=0)dot(p,out,575,179,5,0x98dc81);
   if((bits&Indicators.EMERGENCY)!=0)dot(p,out,575,217,5,0xf07850);
   if((bits&Indicators.GUARD)!=0)dot(p,out,606,321,4,0x9bd294);
   if((bits&Indicators.AWAY)!=0)dot(p,out,606,358,4,0x9bd294);
   if((bits&Indicators.ARMED)!=0)dot(p,out,606,395,4,0x9bd294);
   if((bits&Indicators.DOOR)!=0)dot(p,out,606,432,4,0x9bd294);
   p.popPose();
  }
 }
}
