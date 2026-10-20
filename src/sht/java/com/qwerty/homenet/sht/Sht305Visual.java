package com.qwerty.homenet.sht;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.qwerty.homenet.block.WallMountedBlock;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;

/** New photo-based exposed casing and closed, curved handset. Coordinates follow a 625 x 600 front elevation. */
final class Sht305Visual {
 static final float W=625,H=600,S=(float)(Station305.Mounted.HEIGHT/H);
 static final ResourceLocation WHITE=Product.id("textures/solid.png"),INK=Product.id("sht305v/ink.png");
 static void part(PoseStack p,MultiBufferSource o,int light,String name,float x,float y,float w,float h,float z,float depth,float radius,int color,float crown){Surface.solid(new Surface.Solid(name,x,y,w,h,z,depth,radius,String.format("#%06x",color),"305",crown),p,o,light);}
 static void v(PoseStack p,MultiBufferSource o,int light,float x,float y,float z,float nx,float ny,float nz,int col){Surface.vertex(o.getBuffer(RenderType.entityCutoutNoCull(WHITE)),p.last(),x,y,z,nx,ny,nz,col,light,.5f,.5f);}
 static float ease(float t){t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
 static float faceZ(float x,float y){float shoulder=ease((x-175)/48);float z=60+19*shoulder;float edge=(1-ease((y-2)/17))+(1-ease((598-y)/17));return z-6*edge;}
 static void casing(PoseStack p,MultiBufferSource o,int light){
  // A single closed rear housing extends under the handset and all the way through the control edge.
  part(p,o,light,"305_one_piece_back",147,4,478,592,17,43,19,0xd7ccb4,0);
  for(int row=0;row<60;row++)for(int col=0;col<96;col++){
   float x0=149+col*474f/96,x1=149+(col+1)*474f/96,y0=6+row*588f/60,y1=6+(row+1)*588f/60;
   faceVertex(p,o,light,x0,y1);faceVertex(p,o,light,x1,y1);faceVertex(p,o,light,x1,y0);faceVertex(p,o,light,x0,y0);
  }
  // Thin outside rim only; the broad right side remains a flat sidewall.
  part(p,o,light,"305_right_side",618,14,7,572,18,55,3,0xc3b79e,0);
  // The cradle and its grille remain visible between the handset and the one-piece main front.
  for(int i=0;i<15;i++)part(p,o,light,"305_cradle_vent_"+i,173,220+i*10,33,2.2f,75,.3f,1,0x777363,0);
  part(p,o,light,"305_hook_top",154,71,53,63,59,12,12,0xf0e8d6,1);
  part(p,o,light,"305_hook_bottom",158,475,48,66,58,13,11,0xeee6d4,1);
  // The control band is formed by a very shallow moulded rise, not an attached second case.
  part(p,o,light,"305_control_band",528,267,87,320,78,1.6f,13,0xe9dec6,0);
  // One shallow glossy fascia combines the display and the indicator strip.
  part(p,o,light,"305_fascia_gasket",222,26,389,226,78.5f,1,9,0xd7cbb4,0);
  part(p,o,light,"305_fascia_glass",226,30,381,218,79.5f,1.1f,6,0x14161c,0);
  part(p,o,light,"305_monitor_reveal",243,48,269,181,80.6f,.5f,3,0x3a3c47,0);
  part(p,o,light,"305_monitor_throat",250,55,255,169,81.1f,.35f,2,0x22232b,0);
  part(p,o,light,"305_monitor_screen",257,63,241,153,81.45f,.3f,2,0x6a6879,.4f);
  part(p,o,light,"305_status_strip",515,34,88,210,80.65f,.3f,2,0x191b20,0);
  for(int i=0;i<4;i++){int y=89+i*37;part(p,o,light,"305_alarm_border_"+i,527,y,66,31,80.95f,.25f,2,0x747b78,0);part(p,o,light,"305_alarm_dark_"+i,530,y+3,60,25,81.2f,.2f,1,0x272c2d,0);}
  // Left row: three gray low-profile round keys, then a larger ivory key.
  for(int i=0;i<3;i++){int y=351+i*54;part(p,o,light,"305_round_recess_"+i,468,y,39,39,79,.35f,19,0xa4a092,0);part(p,o,light,"305_round_key_"+i,472,y+4,31,31,79.35f,1.2f,15,0x788294,.5f);}
  for(int i=0;i<2;i++){int x=i==0?461:547;part(p,o,light,"305_large_recess_"+i,x,515,53,53,79+i*1.5f,.35f,26,0xaaa18f,0);part(p,o,light,"305_large_key_"+i,x+4,519,45,45,79.35f+i*1.5f,1.2f,22,0xece5d3,.5f);}
  for(int i=0;i<5;i++){int y=348+i*36;part(p,o,light,"305_indicator_"+i,546,y+2,7,7,81,.2f,3,0x4b4d43,0);part(p,o,light,"305_switch_socket_"+i,568,y,34,11,81,.3f,5,0xaaa18d,0);part(p,o,light,"305_switch_"+i,571,y+2,28,7,81.3f,.7f,3,0xeee7d7,.2f);}
  Surface.rectangle(p,o,223,26,384,555,82,INK,light);
 }
 static void faceVertex(PoseStack p,MultiBufferSource o,int light,float x,float y){float z=faceZ(x,y),nx=faceZ(x-1,y)-faceZ(x+1,y),ny=faceZ(x,y-1)-faceZ(x,y+1);float len=(float)Math.sqrt(nx*nx+ny*ny+4);int col=x<197?0xe2d7bf:x>528?0xe8ddc8:0xefe6d4;v(p,o,light,x,y,z,nx/len,ny/len,2/len,col);}
 static float handsetHalf(float y){float end=Math.min(y-13,587-y);return 75+13*ease(end/45);}
 static float handsetZ(float y,float u,boolean front){float cap=ease(Math.min(y-13,587-y)/36);return front?82+11*cap+17*(1-u*u):35+5*(1-u*u);}
 static void handset(PoseStack p,MultiBufferSource o,int light){
  int rows=72,cols=24;
  for(int side=0;side<2;side++)for(int j=0;j<rows;j++)for(int i=0;i<cols;i++){
   float y0=13+j*574f/rows,y1=13+(j+1)*574f/rows,u0=-1+i*2f/cols,u1=-1+(i+1)*2f/cols;
   handsetVertex(p,o,light,y0,u0,side==0);handsetVertex(p,o,light,y1,u0,side==0);handsetVertex(p,o,light,y1,u1,side==0);handsetVertex(p,o,light,y0,u1,side==0);
  }
  for(int j=0;j<rows;j++)for(int side=0;side<2;side++){
   float y0=13+j*574f/rows,y1=13+(j+1)*574f/rows,u=side==0?-1:1,x0=100+u*handsetHalf(y0),x1=100+u*handsetHalf(y1);
   v(p,o,light,x0,y0,handsetZ(y0,u,false),u,0,0,0xd9ceb8);v(p,o,light,x1,y1,handsetZ(y1,u,false),u,0,0,0xd9ceb8);v(p,o,light,x1,y1,handsetZ(y1,u,true),u,0,0,0xe8deca);v(p,o,light,x0,y0,handsetZ(y0,u,true),u,0,0,0xe8deca);
  }
  for(int end=0;end<2;end++)for(int i=0;i<cols;i++){float y=end==0?13:587,u0=-1+i*2f/cols,u1=-1+(i+1)*2f/cols;float x0=100+u0*handsetHalf(y),x1=100+u1*handsetHalf(y);v(p,o,light,x0,y,handsetZ(y,u0,false),0,end==0?-1:1,0,0xd9ceb8);v(p,o,light,x1,y,handsetZ(y,u1,false),0,end==0?-1:1,0,0xd9ceb8);v(p,o,light,x1,y,handsetZ(y,u1,true),0,end==0?-1:1,0,0xeee5d2);v(p,o,light,x0,y,handsetZ(y,u0,true),0,end==0?-1:1,0,0xeee5d2);}
 }
 static void handsetVertex(PoseStack p,MultiBufferSource o,int light,float y,float u,boolean front){float x=100+u*handsetHalf(y),z=handsetZ(y,u,front);int col=front?(u<-.6?0xf6eddb:u>.6?0xddd3bd:0xf0e8d5):0xd6ccb8;v(p,o,light,x,y,z,u*.4f,0,front?1:-1,col);}
 static void draw(PoseStack p,MultiBufferSource o,int light,boolean docked){GeometryCache.draw("sht305v_fresh_"+docked,p,o,light,(pose,buffers)->{casing(pose,buffers,light);if(docked)handset(pose,buffers,light);});}
 static void pose(Station305 b,PoseStack p){var off=com.qwerty.homenet.move.DeviceOffsets.get(b.getLevel(),b.getBlockPos());p.translate(off.x,off.y,off.z);switch(b.getBlockState().getValue(WallMountedBlock.FACING)){case NORTH->{p.translate(.5,.25,1);p.mulPose(Axis.YP.rotationDegrees(180));}case SOUTH->p.translate(.5,.25,0);case EAST->{p.translate(0,.25,.5);p.mulPose(Axis.YP.rotationDegrees(90));}case WEST->{p.translate(1,.25,.5);p.mulPose(Axis.YP.rotationDegrees(-90));}default->{}}p.scale(S,S,S);p.translate(-W/2,H-700,0);}
 static final class Renderer implements BlockEntityRenderer<Station305>{final Sht305CanvasVisual.Renderer canvas=new Sht305CanvasVisual.Renderer();com.qwerty.homenet.client.kvm.KvmCameraView video=new com.qwerty.homenet.client.kvm.KvmCameraView();public void render(Station305 b,float tick,PoseStack p,MultiBufferSource o,int light,int overlay){if(b.getBlockState().is(Product.STATION305_CANVAS.get())){canvas.render(b,tick,p,o,light,overlay);return;}if(DeviceVisibility.hidden(b,GeometryCache.bounds("sht305v_fresh_"+b.docked),q->pose(b,q)))return;p.pushPose();pose(b,p);draw(p,o,light,b.docked);int lit=b.lights();int[] bits={Indicators.FIRE,Indicators.GAS,Indicators.SECURITY,Indicators.EMERGENCY};int[] colors={0xf36b58,0xe5c953,0x77c588,0xe68d75};for(int i=0;i<4;i++)if((lit&bits[i])!=0)Surface.patch(p,o,535,100+i*37,7,7,82,colors[i],15728880);var camera=b.camera();if(camera!=null){p.pushPose();p.scale(16,16,16);video.drawWall(p,o,camera,257,700-216,241,153,83,b.brightness,b.contrast,com.qwerty.homenet.video.VideoFormat.MONOCHROME);p.popPose();}p.popPose();}public boolean shouldRenderOffScreen(Station305 b){return false;}}
}
