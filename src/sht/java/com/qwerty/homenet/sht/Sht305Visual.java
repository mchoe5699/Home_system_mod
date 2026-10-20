package com.qwerty.homenet.sht;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;

/** One continuously moulded exposed case with a removable handset. */
final class Sht305Visual {
 static final float W=660,H=600,S=(float)(Station305.Mounted.HEIGHT/H);
 static final ResourceLocation INK=Product.id("sht305v/ink.png");
 static void part(PoseStack p,MultiBufferSource out,int light,String name,float x,float y,float w,float h,float z,float d,float r,int rgb,float curve){
  Surface.solid(new Surface.Solid(name,x,y,w,h,z,d,r,String.format("#%06x",rgb),"sht305v",curve),p,out,light);
 }
 static float smooth(float t){t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
 static float shellEdge(float y,boolean right){
  float r=23,delta=0;
  if(y<r)delta=r-y;else if(y>H-r)delta=y-(H-r);
  float inset=r-(float)Math.sqrt(Math.max(0,r*r-delta*delta));
  return right?W-inset:inset;
 }
 static float shellZ(float x,float y){
  float edge=Math.max(0,Math.min(x-shellEdge(y,false),shellEdge(y,true)-x));
  float end=Math.max(0,Math.min(y,H-y));
  float body=70+20*smooth((x-175)/45);
  float cavity=6*smooth((x-12)/20)*(1-smooth((x-155)/28))*smooth((y-40)/35)*(1-smooth((y-530)/30));
  float hook=10*(float)Math.exp(-Math.pow((x-176)/19,2))*(float)(Math.exp(-Math.pow((y-110)/33,2))+Math.exp(-Math.pow((y-487)/33,2)));
  float controls=3*smooth((x-530)/25);
  return body-cavity+hook+controls-5*(1-smooth(edge/20))-4*(1-smooth(end/18));
 }
 static void shell(PoseStack p,MultiBufferSource out,int light){
  // One rear moulding supports the entire front, including the recessed handset cradle.
  part(p,out,light,"305_single_rear_case",0,0,660,600,0,58,23,0xd8cfb9,0);
  var b=out.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));
  for(int row=0;row<100;row++)for(int col=0;col<110;col++)for(int j=0;j<4;j++){
   float y=6*(row+(j==1||j==2?1:0));
   float u=(col+(j>=2?1:0))/110f;
   float left=shellEdge(y,false),right=shellEdge(y,true),x=left+(right-left)*u;
   float z=shellZ(x,y),nx=-(shellZ(x+.3f,y)-shellZ(x-.3f,y))/ .6f;
   float ny=-(shellZ(x,y+.3f)-shellZ(x,y-.3f))/ .6f;
   float n=(float)Math.sqrt(1+nx*nx+ny*ny);
   int color=x>555?0xe7dec9:x<185?0xe8dfc9:0xf0ead7;
   Surface.vertex(b,p.last(),x,y,z,nx/n,ny/n,1/n,color,light,.5f,.5f);
  }
  // Close the thin perimeter between the rear wall box and the continuous front skin.
  for(int side=0;side<2;side++)for(int row=0;row<100;row++)for(int j=0;j<4;j++){
   float y=6*(row+(j==1||j==2?1:0));
   float x=shellEdge(y,side==1),z=j<2?58:shellZ(x,y);
   Surface.vertex(b,p.last(),x,y,z,side==0?-1:1,0,0,0xe4dbc5,light,.5f,.5f);
  }
  for(int end=0;end<2;end++)for(int col=0;col<110;col++)for(int j=0;j<4;j++){
   float y=end==0?0:H,u=(col+(j==1||j==2?1:0))/110f;
   float x=shellEdge(y,false)+(shellEdge(y,true)-shellEdge(y,false))*u;
   float z=j<2?58:shellZ(x,y);
   Surface.vertex(b,p.last(),x,y,z,0,end==0?-1:1,0,0xe4dbc5,light,.5f,.5f);
  }
 }
 static float receiverHalf(float t){
  float ends=(float)Math.pow(Math.abs(2*t-1),1.6);
  float corner=smooth(Math.min(t,1-t)*600/45);
  return (72+18*ends)*(.52f+.48f*corner);
 }
 static float receiverX(float t,float u){return 96+u*receiverHalf(t);}
 static float receiverZ(float t,float u){return 97+20*(1-u*u)+3*(float)Math.pow(Math.abs(2*t-1),2);}
 static void receiver(PoseStack p,MultiBufferSource out,int light){
  part(p,out,light,"handset_back",7,7,178,586,37,57,43,0xdcd2bb,1);
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
  // Vents, switch and display are details on that case, not separate shell segments.
  for(int i=0;i<17;i++)part(p,out,l,"305_speaker_slot_"+i,185,211+i*12.5f,27,2.2f,83,.3f,1,0x69695e,0);
  part(p,out,l,"305_power_switch",188,483,15,29,82,2,2,0xeae2cb,1);

  // Inset upper glass: a single dark field with a CRT and four warning windows.
  part(p,out,l,"305_fascia_edge",216,17,427,248,89,2,11,0xf3ebd8,0);
  part(p,out,l,"305_fascia",221,22,417,238,91,6,7,0x15171b,0);
  part(p,out,l,"305_crt_reveal",233,38,282,208,97,.5f,3,0x414147,0);
  part(p,out,l,"305_crt_throat",239,46,270,194,97.5f,.5f,2,0x202127,0);
  part(p,out,l,"305_crt_glass",246,55,256,177,98,.7f,2,0x676478,.4f);
  part(p,out,l,"305_status_glass",522,29,111,224,97,.7f,2,0x17191e,0);
  for(int i=0;i<4;i++){
   float y=87+i*38;
   part(p,out,l,"305_warning_outline_"+i,530,y,92,33,97.7f,.3f,2,0x747a78,0);
   part(p,out,l,"305_warning_window_"+i,533,y+3,86,27,98,.4f,1,0x282d31,0);
  }

  // Controls follow the curved face; only the keys, lenses and fine rims stand proud.
  for(int i=0;i<3;i++){
   float y=313+i*54;
   part(p,out,l,"305_round_shadow_"+i,465,y,42,42,90,.5f,21,0x9a9588,0);
   part(p,out,l,"305_round_bevel_"+i,469,y+4,34,34,90.5f,1.5f,17,0xcac8c0,0);
   part(p,out,l,"305_round_key_"+i,472,y+7,28,28,92,1.7f,14,0x747d90,1.3f);
  }
  for(int i=0;i<5;i++){
   float y=314+i*37;
   part(p,out,l,"305_lamp_lens_"+i,567,y+2,10,10,94,.4f,5,0x41443f,0);
   part(p,out,l,"305_switch_recess_"+i,588,y,48,13,94,.5f,6,0xaea590,0);
   part(p,out,l,"305_switch_rim_"+i,591,y+2,42,9,94.5f,1.1f,4,0xf0ead8,.4f);
   part(p,out,l,"305_switch_key_"+i,597,y+3,30,7,95.6f,.6f,3,0xe4dcc8,.2f);
  }
  for(int i=0;i<2;i++){
   float x=i==0?461:580;
   part(p,out,l,"305_large_ring_"+i,x,494,56,56,i==0?90:94,.6f,28,0xa9a18e,0);
   part(p,out,l,"305_large_bevel_"+i,x+3,497,50,50,i==0?90.6f:94.6f,1.6f,25,0xe0d9c7,0);
   part(p,out,l,"305_large_button_"+i,x+7,501,42,42,i==0?92.2f:96.2f,2.1f,21,0xf0ead8,1.5f);
  }
  for(int i=0;i<3;i++){
   float y=341+i*34;
   part(p,out,l,"305_side_slider_well_"+i,655,y,4,21,51,1,1,0x837c6b,0);
   part(p,out,l,"305_side_slider_"+i,656,y+7,5,7,52,2,1,0xdad1bb,0);
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
    video.drawWall(p,out,camera,253,700-227,242,164,101,b.brightness,b.contrast,com.qwerty.homenet.video.VideoFormat.MONOCHROME);
    p.popPose();
   }
   int bits=b.lights();
   if((bits&Indicators.FIRE)!=0)dot(p,out,541,103,5,0xf07850);
   if((bits&Indicators.GAS)!=0)dot(p,out,541,141,5,0xe1bf59);
   if((bits&Indicators.SECURITY)!=0)dot(p,out,541,179,5,0x98dc81);
   if((bits&Indicators.EMERGENCY)!=0)dot(p,out,541,217,5,0xf07850);
   if((bits&Indicators.GUARD)!=0)dot(p,out,572,321,4,0x9bd294);
   if((bits&Indicators.AWAY)!=0)dot(p,out,572,358,4,0x9bd294);
   if((bits&Indicators.ARMED)!=0)dot(p,out,572,395,4,0x9bd294);
   if((bits&Indicators.DOOR)!=0)dot(p,out,572,432,4,0x9bd294);
   p.popPose();
  }
 }
}
