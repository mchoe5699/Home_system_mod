package com.qwerty.homenet.sht;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;

/** Exposed SHT-305V shell: a deep wall case, fixed receiver cradle and shallow raised keys. */
final class Sht305Visual {
 static final float W=700,H=600,S=(float)(Station305.Mounted.HEIGHT/H);
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
  // The deep rear box remains visible in side view; the front face rolls over its edges.
  part(p,out,l,"305_rear_case",192,2,508,596,0,62,22,0xd0c7ad,0);
  part(p,out,l,"305_case_shoulder",194,2,504,594,43,43,25,0xded5bc,1);
  part(p,out,l,"305_body",194,3,501,591,60,24,26,0xece5d0,4);
  part(p,out,l,"305_left_seam",192,20,3,555,65,24,1,0xc6baa2,0);
  // The handset sits on a recessed, exposed base. Hooks interrupt the cavity at either end.
  part(p,out,l,"305_bridge",166,45,45,510,18,55,12,0xd4c9ae,1);
  part(p,out,l,"305_cradle_outer",7,15,179,570,36,41,28,0xd3c8ad,2);
  part(p,out,l,"305_cradle_well",24,65,138,475,76,4,18,0xb6aa90,0);
  part(p,out,l,"305_upper_hook",158,84,37,71,76,10,11,0xece5d0,1);
  part(p,out,l,"305_lower_hook",158,449,37,71,76,10,11,0xece5d0,1);
  part(p,out,l,"305_side_speaker",175,208,24,221,79,4,5,0xddd4bb,0);
  for(int i=0;i<16;i++)part(p,out,l,"305_side_louver_"+i,178,216+i*13,19,2.2f,83,.4f,1,0x776f5e,0);
  part(p,out,l,"305_power_recess",178,489,17,31,83,.5f,2,0x7e7969,0);
  part(p,out,l,"305_power_rocker",180,493,13,23,83.5f,1.8f,2,0xf0e8d3,1);
  // Continuous black top fascia, wide CRT throat, and four indicator windows.
  part(p,out,l,"305_fascia_lip",215,18,467,249,88,2,10,0xf4ecd8,0);
  part(p,out,l,"305_fascia",220,23,457,239,90,3,8,0x17191e,0);
  part(p,out,l,"305_crt_frame",234,40,307,208,93,1.5f,4,0x303239,0);
  part(p,out,l,"305_crt_throat",242,49,291,191,94.5f,.6f,3,0x17191d,0);
  part(p,out,l,"305_crt_glass",251,59,273,171,95.1f,.6f,2,0x646176,.6f);
  part(p,out,l,"305_status_panel",551,28,116,229,93,.8f,3,0x15181d,0);
  for(int i=0;i<4;i++){
   float y=90+i*38;
   part(p,out,l,"305_indicator_trim_"+i,562,y,94,32,93.8f,.35f,2,0x777c7b,0);
   part(p,out,l,"305_indicator_glass_"+i,565,y+3,88,26,94.15f,.35f,1,0x272d31,0);
  }
  // The right control band is a softly rolled section of the same ivory casing.
  part(p,out,l,"305_control_band",574,267,116,321,89,3,11,0xe5dcc5,3);
  part(p,out,l,"305_band_seam",574,269,1.3f,315,92,.25f,.5f,0xc6bda9,0);
  for(int i=0;i<3;i++){
   float y=311+i*54;
   part(p,out,l,"305_small_socket_"+i,504,y,39,39,91,.35f,19,0x9c9789,0);
   part(p,out,l,"305_small_bevel_"+i,507,y+3,33,33,91.35f,1.1f,16,0xd4d0c5,0);
   part(p,out,l,"305_small_key_"+i,510,y+6,27,27,92.45f,1.5f,13,0x747d90,1.2f);
  }
  for(int i=0;i<5;i++){
   float y=314+i*37;
   part(p,out,l,"305_lamp_well_"+i,603,y+2,10,10,93,.4f,5,0x565447,0);
   part(p,out,l,"305_switch_socket_"+i,624,y,50,13,93,.3f,6,0xa9a08b,0);
   part(p,out,l,"305_switch_bevel_"+i,627,y+2,44,9,93.3f,1.1f,4,0xf1ebd9,.5f);
   part(p,out,l,"305_switch_cap_"+i,635,y+3,28,7,94.4f,.55f,3,0xe5dcc8,.3f);
  }
  for(int i=0;i<2;i++){
   float x=i==0?500:618;
   part(p,out,l,"305_large_socket_"+i,x,496,55,55,91,.5f,27,0xa9a18e,0);
   part(p,out,l,"305_large_bevel_"+i,x+3,499,49,49,91.5f,1.2f,24,0xe2dbc9,0);
   part(p,out,l,"305_large_key_"+i,x+7,503,41,41,92.7f,2,20,0xf0ead8,2);
  }
  for(int i=0;i<3;i++){
   float y=342+i*34;
   part(p,out,l,"305_side_adjustment_recess_"+i,693,y,5,20,51,1,1,0x8c8575,0);
   part(p,out,l,"305_side_adjustment_tab_"+i,694,y+7,7,5,52,2,1,0xdcd3ba,0);
  }
  Surface.rectangle(p,out,0,0,W,H,98,INK,l);
 }
 static float receiverHalfWidth(float t){return 66+18*(float)Math.pow(Math.abs(2*t-1),1.7);}
 static float receiverX(float t,float u){
  float end=(float)Math.min(1,Math.min(t,1-t)*600/45);
  return 93+u*receiverHalfWidth(t)*(.42f+.58f*(float)Math.sqrt(Math.max(0,end)));
 }
 static float receiverZ(float t,float u){return 100+18*(1-u*u)+3*(float)Math.cos(2*Math.PI*t);}
 static void handset(PoseStack p,MultiBufferSource out,int light){
  part(p,out,light,"handset_back",10,8,167,584,49,48,48,0xd9cfb5,1);
  var b=out.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));
  for(int row=0;row<48;row++)for(int col=0;col<16;col++){
   for(int j=0;j<4;j++){
    float t=(row+(j==1||j==2?1:0))/48f,u=-1+2*(col+(j>=2?1:0))/16f;
    float x=receiverX(t,u),y=12+t*576,z=receiverZ(t,u);
    float nx=u*.47f,ny=(receiverZ(Math.min(1,t+.001f),u)-receiverZ(Math.max(0,t-.001f),u))*.1f,nz=1;
    float n=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);
    Surface.vertex(b,p.last(),x,y,z,nx/n,ny/n,nz/n,0xf0ead7,light,.5f,.5f);
   }
  }
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
   var camera=b.camera();if(camera!=null){p.pushPose();p.scale(16,16,16);video.drawWall(p,out,camera,259,700-225,258,159,99,b.brightness,b.contrast,com.qwerty.homenet.video.VideoFormat.MONOCHROME);p.popPose();}
   int bits=b.lights();if((bits&Indicators.FIRE)!=0)dot(p,out,575,106,5,0xf07850);
   if((bits&Indicators.GAS)!=0)dot(p,out,575,144,5,0xe1bf59);
   if((bits&Indicators.SECURITY)!=0)dot(p,out,575,182,5,0x98dc81);
   if((bits&Indicators.EMERGENCY)!=0)dot(p,out,575,220,5,0xf07850);
   if((bits&Indicators.DOOR)!=0)dot(p,out,608,430,4,0x9bd294);
   if((bits&Indicators.GUARD)!=0)dot(p,out,608,320,4,0x9bd294);
   if((bits&Indicators.ARMED)!=0)dot(p,out,608,393,4,0x9bd294);
   if((bits&Indicators.AWAY)!=0)dot(p,out,608,357,4,0x9bd294);
   p.popPose();
  }
 }
}
