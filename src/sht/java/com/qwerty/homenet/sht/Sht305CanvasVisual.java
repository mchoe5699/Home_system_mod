package com.qwerty.homenet.sht;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.qwerty.homenet.block.WallMountedBlock;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;

/** Canvas reference variant: narrower handset, deeper cradle and one gently rising control face. */
final class Sht305CanvasVisual {
 // The regenerated front canvas is about 668:600; the earlier canvas was 625:600.
 static final float W=668,H=600,X=W/625f,S=(float)(Station305.Mounted.HEIGHT/H);
 static final ResourceLocation WHITE=Product.id("textures/solid.png"),INK=Product.id("sht305v_canvas/ink.png");
 static void part(PoseStack p,MultiBufferSource o,int light,String name,float x,float y,float w,float h,float z,float depth,float radius,int color,float crown){Surface.solid(new Surface.Solid(name,x*X,y,w*X,h,z,depth,radius,String.format("#%06x",color),"canvas305",crown),p,o,light);}
 static void v(PoseStack p,MultiBufferSource o,int light,float x,float y,float z,float nx,float ny,float nz,int col){Surface.vertex(o.getBuffer(RenderType.entityCutoutNoCull(WHITE)),p.last(),x*X,y,z,nx/X,ny,nz,col,light,.5f,.5f);}
 static float ease(float t){t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
 static float faceZ(float x,float y){float shoulder=ease((x-160)/45);float controls=ease((x-492)/34);float edge=(1-ease((y-2)/19))+(1-ease((598-y)/19));return 59+19*shoulder+2.4f*controls-6*edge;}
 static float edgeX(float x,float y){float inset=12*(1-ease(Math.min(y-6,594-y)/18));return x<380?x+inset*(380-x)/243:x-inset*(x-380)/243;}
 static void edgeWalls(PoseStack p,MultiBufferSource o,int light){
  // Close the moulded front against its rear shell on all four sides.
  for(int i=0;i<96;i++){
   float a=137+i*486f/96,b=137+(i+1)*486f/96;
   for(float y:new float[]{6,594}){
    float x0=edgeX(a,y),x1=edgeX(b,y),z0=faceZ(x0,y),z1=faceZ(x1,y);
    v(p,o,light,x0,y,59,0,y<300?-1:1,0,0xd7ccb4);v(p,o,light,x1,y,59,0,y<300?-1:1,0,0xd7ccb4);
    v(p,o,light,x1,y,z1,0,y<300?-1:1,0,0xece1cc);v(p,o,light,x0,y,z0,0,y<300?-1:1,0,0xece1cc);
   }
  }
  for(int i=0;i<60;i++){
   float y0=6+i*588f/60,y1=6+(i+1)*588f/60;
   for(float x:new float[]{137,623}){
    float x0=edgeX(x,y0),x1=edgeX(x,y1),z0=faceZ(x0,y0),z1=faceZ(x1,y1);
    v(p,o,light,x0,y0,59,x<380?-1:1,0,0,0xcbbfa8);v(p,o,light,x1,y1,59,x<380?-1:1,0,0,0xcbbfa8);
    v(p,o,light,x1,y1,z1,x<380?-1:1,0,0,0xe3d8c1);v(p,o,light,x0,y0,z0,x<380?-1:1,0,0,0xe3d8c1);
   }
  }
 }
 static void casing(PoseStack p,MultiBufferSource o,int light){
  // A single closed rear housing extends under the handset and all the way through the control edge.
  part(p,o,light,"canvas305_one_piece_back",136,4,489,592,0,59,19,0xd7ccb4,0);
  for(int row=0;row<60;row++)for(int col=0;col<96;col++){
   float y0=6+row*588f/60,y1=6+(row+1)*588f/60;float x0=137+col*486f/96,x1=137+(col+1)*486f/96;
   faceVertex(p,o,light,x0,y1);faceVertex(p,o,light,x1,y1);faceVertex(p,o,light,x1,y0);faceVertex(p,o,light,x0,y0);
  }
  // Thin outside rim only; the broad right side remains a flat sidewall.
  edgeWalls(p,o,light);
  // The cradle and its grille remain visible between the handset and the one-piece main front.
  for(int i=0;i<15;i++)part(p,o,light,"canvas305_cradle_vent_"+i,145,220+i*10,43,2.2f,75,.3f,1,0x777363,0);
  part(p,o,light,"canvas305_hook_top",145,71,49,63,59,12,12,0xf0e8d6,1);
  part(p,o,light,"canvas305_hook_bottom",146,475,48,66,58,13,11,0xeee6d4,1);
  // The control band is formed by a very shallow moulded rise, not an attached second case.
  // One shallow glossy fascia combines the display and the indicator strip.
  part(p,o,light,"canvas305_fascia_gasket",208,28,392,217,78.5f,1,9,0xd7cbb4,0);
  part(p,o,light,"canvas305_fascia_glass",212,32,384,209,79.5f,1.1f,6,0x14161c,0);
  part(p,o,light,"canvas305_monitor_reveal",233,51,239,177,80.6f,.5f,3,0x3a3c47,0);
  part(p,o,light,"canvas305_monitor_throat",239,57,227,166,81.1f,.35f,2,0x22232b,0);
  part(p,o,light,"canvas305_monitor_screen",245,67,215,149,81.45f,.3f,2,0x6a6879,.4f);
  part(p,o,light,"canvas305_status_strip",495,35,98,204,80.65f,.3f,2,0x191b20,0);
  for(int i=0;i<3;i++){int y=106+i*46;part(p,o,light,"canvas305_alarm_border_"+i,506,y,77,38,80.95f,.25f,2,0x747b78,0);part(p,o,light,"canvas305_alarm_dark_"+i,510,y+3,69,32,81.2f,.2f,1,0x272c2d,0);}
  // Left row: three gray low-profile round keys, then a larger ivory key.
  for(int i=0;i<3;i++){int y=318+i*54;part(p,o,light,"canvas305_round_recess_"+i,446,y,39,39,80,.35f,19,0xa4a092,0);part(p,o,light,"canvas305_round_key_"+i,450,y+4,31,31,80.35f,1.2f,15,0x788294,.5f);}
  for(int i=0;i<2;i++){int x=i==0?438:541;part(p,o,light,"canvas305_large_recess_"+i,x,480,53,53,80+i*1.5f,.35f,26,0xaaa18f,0);part(p,o,light,"canvas305_large_key_"+i,x+4,484,45,45,80.35f+i*1.5f,1.2f,22,0xece5d3,.5f);}
  for(int i=0;i<5;i++){int y=300+i*36;part(p,o,light,"canvas305_indicator_"+i,529,y+2,7,7,82,.2f,3,0x4b4d43,0);part(p,o,light,"canvas305_switch_socket_"+i,550,y,34,11,82,.3f,5,0xaaa18d,0);part(p,o,light,"canvas305_switch_"+i,553,y+2,28,7,82.3f,.7f,3,0xeee7d7,.2f);}
  Surface.rectangle(p,o,208*X,28,389*X,558,84,INK,light);
 }
 static void faceVertex(PoseStack p,MultiBufferSource o,int light,float x,float y){x=edgeX(x,y);float z=faceZ(x,y),nx=faceZ(x-1,y)-faceZ(x+1,y),ny=faceZ(x,y-1)-faceZ(x,y+1);float len=(float)Math.sqrt(nx*nx+ny*ny+4);int col=x<187?0xe7dcc4:x>495?0xece1cd:0xf2e9d7;v(p,o,light,x,y,z,nx/len,ny/len,2/len,col);}
 static float handsetHalf(float y){float end=Math.min(y-13,587-y);return 62+9*ease(end/44);}
 static float handsetZ(float y,float u,boolean front){float cap=ease(Math.min(y-13,587-y)/36);return front?82+12*cap+18*(1-u*u):34+5*(1-u*u);}
 static void handset(PoseStack p,MultiBufferSource o,int light){
  int rows=72,cols=24;
  for(int side=0;side<2;side++)for(int j=0;j<rows;j++)for(int i=0;i<cols;i++){
   float y0=13+j*574f/rows,y1=13+(j+1)*574f/rows,u0=-1+i*2f/cols,u1=-1+(i+1)*2f/cols;
   handsetVertex(p,o,light,y0,u0,side==0);handsetVertex(p,o,light,y1,u0,side==0);handsetVertex(p,o,light,y1,u1,side==0);handsetVertex(p,o,light,y0,u1,side==0);
  }
  for(int j=0;j<rows;j++)for(int side=0;side<2;side++){
   float y0=13+j*574f/rows,y1=13+(j+1)*574f/rows,u=side==0?-1:1,x0=73+u*handsetHalf(y0),x1=73+u*handsetHalf(y1);
   v(p,o,light,x0,y0,handsetZ(y0,u,false),u,0,0,0xd9ceb8);v(p,o,light,x1,y1,handsetZ(y1,u,false),u,0,0,0xd9ceb8);v(p,o,light,x1,y1,handsetZ(y1,u,true),u,0,0,0xe8deca);v(p,o,light,x0,y0,handsetZ(y0,u,true),u,0,0,0xe8deca);
  }
  for(int end=0;end<2;end++)for(int i=0;i<cols;i++){float y=end==0?13:587,u0=-1+i*2f/cols,u1=-1+(i+1)*2f/cols;float x0=73+u0*handsetHalf(y),x1=73+u1*handsetHalf(y);v(p,o,light,x0,y,handsetZ(y,u0,false),0,end==0?-1:1,0,0xd9ceb8);v(p,o,light,x1,y,handsetZ(y,u1,false),0,end==0?-1:1,0,0xd9ceb8);v(p,o,light,x1,y,handsetZ(y,u1,true),0,end==0?-1:1,0,0xeee5d2);v(p,o,light,x0,y,handsetZ(y,u0,true),0,end==0?-1:1,0,0xeee5d2);}
 }
 static void handsetVertex(PoseStack p,MultiBufferSource o,int light,float y,float u,boolean front){float x=73+u*handsetHalf(y),z=handsetZ(y,u,front);int col=front?(u<-.6?0xf6eddb:u>.6?0xddd3bd:0xf0e8d5):0xd6ccb8;v(p,o,light,x,y,z,u*.4f,0,front?1:-1,col);}
 static void draw(PoseStack p,MultiBufferSource o,int light,boolean docked){GeometryCache.draw("sht305v_canvas_"+docked,p,o,light,(pose,buffers)->{casing(pose,buffers,light);if(docked)handset(pose,buffers,light);});}
 static void pose(Station305 b,PoseStack p){var off=com.qwerty.homenet.move.DeviceOffsets.get(b.getLevel(),b.getBlockPos());p.translate(off.x,off.y,off.z);switch(b.getBlockState().getValue(WallMountedBlock.FACING)){case NORTH->{p.translate(.5,.25,1);p.mulPose(Axis.YP.rotationDegrees(180));}case SOUTH->p.translate(.5,.25,0);case EAST->{p.translate(0,.25,.5);p.mulPose(Axis.YP.rotationDegrees(90));}case WEST->{p.translate(1,.25,.5);p.mulPose(Axis.YP.rotationDegrees(-90));}default->{}}p.scale(S,S,S);p.translate(-W/2,H-700,0);}
 static final class Renderer implements BlockEntityRenderer<Station305>{com.qwerty.homenet.client.kvm.KvmCameraView video=new com.qwerty.homenet.client.kvm.KvmCameraView();public void render(Station305 b,float tick,PoseStack p,MultiBufferSource o,int light,int overlay){if(DeviceVisibility.hidden(b,GeometryCache.bounds("sht305v_canvas_"+b.docked),q->pose(b,q)))return;p.pushPose();pose(b,p);draw(p,o,light,b.docked);int lit=b.lights();int[] bits={Indicators.GAS,Indicators.SECURITY,Indicators.EMERGENCY};int[] colors={0xe5c953,0x77c588,0xe68d75};for(int i=0;i<3;i++)if((lit&bits[i])!=0)Surface.patch(p,o,515*X,117+i*46,7*X,7,84,colors[i],15728880);if((lit&Indicators.FIRE)!=0)Surface.patch(p,o,539*X,53,7*X,7,84,0xf36b58,15728880);var camera=b.camera();if(camera!=null){p.pushPose();p.scale(16,16,16);video.drawWall(p,o,camera,(int)(245*X),700-216,(int)(215*X),149,83,b.brightness,b.contrast,com.qwerty.homenet.video.VideoFormat.MONOCHROME);p.popPose();}p.popPose();}public boolean shouldRenderOffScreen(Station305 b){return false;}}
}
