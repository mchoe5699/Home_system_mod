package com.qwerty.homenet.sht;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.qwerty.homenet.block.WallMountedBlock;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;

/** HT-100 reconstructed from the clean front photo and five older side views. */
final class Ht100Visual {
 static final float W=226,H=300,SCALE=(float)(Ht100.HEIGHT/H);
 static final ResourceLocation PRINT=Product.id("ht100/print.png"),GLOW=Product.id("ht100/glow.png");
 private static final ResourceLocation SOLID=Product.id("textures/solid.png");
 private static final int SHELL=0xbfc7c4,UPPER=0xc5ccca,LOWER=0xbcc4c1;
 private record Point(float x,float y){}
 private record Outline(Point[] points,float z){}
 private static Point[] round(float x,float y,float w,float h,float r){
  Point[] out=new Point[32];int k=0;
  for(int corner=0;corner<4;corner++){float cx=switch(corner){case 0,3->x+r;default->x+w-r;},cy=corner<2?y+r:y+h-r;double start=(corner+2)*Math.PI/2;
   for(int i=0;i<8;i++){double a=start+i*Math.PI/16;out[k++]=new Point(cx+r*(float)Math.cos(a),cy+r*(float)Math.sin(a));}}
  return out;
 }
 private static Outline outline(float x,float y,float w,float h,float r,float z){return new Outline(round(x,y,w,h,r),z);}
 private static void vertex(VertexConsumer b,PoseStack p,float x,float y,float z,float nx,float ny,float nz,int color,int light){Surface.vertex(b,p.last(),x,y,z,nx,ny,nz,color,light,.5f,.5f);}
 private static void quad(PoseStack p,MultiBufferSource o,int light,float[] a,float[] b,float[] c,float[] d,int color,float nx,float ny,float nz){
  var v=o.getBuffer(RenderType.entityCutoutNoCull(SOLID));for(float[] q:new float[][]{a,b,c,d})vertex(v,p,q[0],q[1],q[2],nx,ny,nz,color,light);
 }
 private static float[] at(Point p,float z){return new float[]{p.x,p.y,z};}
 private static void loft(PoseStack p,MultiBufferSource o,int light,int color,Outline... rings){
  for(int ring=0;ring<rings.length-1;ring++){var a=rings[ring];var b=rings[ring+1];for(int i=0;i<32;i++){int j=(i+1)%32;var pa=a.points[i];var pb=a.points[j];float dx=pb.x-pa.x,dy=pb.y-pa.y,len=(float)Math.sqrt(dx*dx+dy*dy);if(len==0)len=1;
   quad(p,o,light,at(pa,a.z),at(pb,a.z),at(b.points[j],b.z),at(b.points[i],b.z),color,dy/len,-dx/len,.22f);
  }}
 }
 private static void fill(PoseStack p,MultiBufferSource o,int light,Outline shape,int color){
  var points=shape.points;float cx=0,cy=0;for(var q:points){cx+=q.x;cy+=q.y;}cx/=32;cy/=32;
  for(int i=0;i<32;i++){int j=(i+1)%32;quad(p,o,light,new float[]{cx,cy,shape.z},at(points[i],shape.z),at(points[j],shape.z),new float[]{cx,cy,shape.z},color,0,0,1);}
 }
 private static void plate(PoseStack p,MultiBufferSource o,int light,int color,Outline back,Outline front){loft(p,o,light,color,back,front);fill(p,o,light,front,color);}
 private static void rect(PoseStack p,MultiBufferSource o,int light,float x,float y,float w,float h,float z,int color){quad(p,o,light,new float[]{x,y+h,z},new float[]{x+w,y+h,z},new float[]{x+w,y,z},new float[]{x,y,z},color,0,0,1);}
 private static void ring(PoseStack p,MultiBufferSource o,int light,int color,Outline outer,Outline inner){for(int i=0;i<32;i++){int j=(i+1)%32;quad(p,o,light,at(outer.points[i],outer.z),at(outer.points[j],outer.z),at(inner.points[j],inner.z),at(inner.points[i],inner.z),color,0,0,1);}}
 private static void circle(PoseStack p,MultiBufferSource o,int light,float cx,float cy,float radius,float z0,float z1,int color){plate(p,o,light,color,outline(cx-radius,cy-radius,2*radius,2*radius,radius,z0),outline(cx-radius+.4f,cy-radius+.4f,2*radius-.8f,2*radius-.8f,radius-.4f,z1));}
 private static void speaker(PoseStack p,MultiBufferSource o,int light,float y,int n,float front){
  float left=25,right=121-n*.35f,height=4.2f;
  // This is an actual front opening: the surrounding lower face is drawn in strips around it.
  rect(p,o,light,left,y,right-left,height,front-.8f,0x34433f);
  quad(p,o,light,new float[]{left,y,front},new float[]{right,y,front},new float[]{right,y+.9f,front-.8f},new float[]{left,y+.9f,front-.8f},0x65736e,0,-.6f,.8f);
  quad(p,o,light,new float[]{left,y+height-.9f,front-.8f},new float[]{right,y+height-.9f,front-.8f},new float[]{right,y+height,front},new float[]{left,y+height,front},0x85918c,0,.5f,.86f);
  rect(p,o,light,left+2,y+height+.45f,right-left-5,.55f,front+.02f,0xe0e5df);
 }
 private static void lowerFace(PoseStack p,MultiBufferSource o,int light,float front){
  float left=15,right=211,top=202,bottom=291,last=top;
  for(int i=0;i<6;i++){float y=212+i*12.55f,slitEnd=y+4.2f,slotRight=121-i*.35f;
   rect(p,o,light,left,last,right-left,y-last,front,LOWER);
   rect(p,o,light,left,y,25-left,slitEnd-y,front,LOWER);
   rect(p,o,light,slotRight,y,right-slotRight,slitEnd-y,front,LOWER);
   speaker(p,o,light,y,i,front);last=slitEnd;
  }
  rect(p,o,light,left,last,right-left,bottom-last,front,LOWER);
 }
 private static void body(PoseStack p,MultiBufferSource o,int light,boolean framed){
  float lift=framed?16:0;
  if(framed){
   Outline rear=outline(-6,-5,238,310,13,0),shoulder=outline(-6,-5,238,310,13,12),bevel=outline(-3,-3,232,306,11,17);
   loft(p,o,light,0xb8c0bd,rear,shoulder,bevel);fill(p,o,light,bevel,0xc4cac5);
   ring(p,o,light,0x929d9b,outline(-3,-3,232,306,11,17.05f),outline(1,1,224,298,11,17.1f));
  }
  Outline back=outline(0,0,W,H,13,lift),side=outline(0,0,W,H,13,lift+21),front=outline(5,3,216,294,11,lift+30);
  loft(p,o,light,0xb3bcb9,back,side,front);fill(p,o,light,front,SHELL);
  // Upper front is a single gently domed cover with a narrow, continuous parting line.
  Outline seam=outline(17,6,192,193,9,lift+30.15f),upper=outline(19,8,188,188,8,lift+31.6f);
  ring(p,o,light,0x879390,seam,outline(18,7,190,190,8.5f,lift+30.18f));
  plate(p,o,light,UPPER,outline(18,7,190,190,8.5f,lift+30.2f),upper);
  rect(p,o,light,17,199,192,1,lift+30.5f,0x778683);
  // The lower cover has real recessed horizontal apertures instead of painted stripes.
  lowerFace(p,o,light,lift+31);
  // Tall smoked module. Blue upper window and the lens have independent depths.
  Outline glassEdge=outline(57,47,112,143,4,lift+31.8f),glass=outline(59,49,108,139,3,lift+32.4f);
  plate(p,o,light,0x606968,glassEdge,glass);
  plate(p,o,light,0x28363b,outline(61,51,104,135,2,lift+32.45f),outline(61,51,104,135,2,lift+32.65f));
  plate(p,o,light,0x0d2034,outline(72,66,83,44,1.5f,lift+32.7f),outline(72,66,83,44,1.5f,lift+32.9f));
  circle(p,o,light,113,144,24,lift+32.8f,lift+34.0f,0x17121c);
  circle(p,o,light,113,144,20,lift+34.0f,lift+34.8f,0x371c32);
  circle(p,o,light,113,144,16,lift+34.8f,lift+35.1f,0x150d1a);
  circle(p,o,light,113,144,10,lift+35.1f,lift+36.3f,0x070b14);
  circle(p,o,light,109,139,3,lift+36.3f,lift+36.35f,0x344454);
  // The two short slots sit beside the model name, not in the lower speaker area.
  for(int i=0;i<2;i++)plate(p,o,light,0x344642,outline(211,27+i*13,8,3.6f,1.6f,lift+30),outline(211,27+i*13,8,3.6f,1.6f,lift+31.1f));
  // A fine rectangular reveal surrounds the large, slightly swollen call key.
  ring(p,o,light,0x737f7c,outline(137,204,72,76,7,lift+31.1f),outline(139,206,68,72,6,lift+31.15f));
  plate(p,o,light,0xcbd1ce,outline(141,208,64,68,6,lift+31.1f),outline(142,209,62,66,6,lift+34.6f));
  plate(p,o,light,0x304a50,outline(166,215,13,4.3f,2,lift+34.65f),outline(166,215,13,4.3f,2,lift+34.8f));
  // Separate crescent keeper closes the lower speaker hole in the clean photo.
  plate(p,o,light,0xaebbb7,outline(103,282,22,18,8,lift+31),outline(104,283,20,17,8,lift+33.0f));
  rect(p,o,light,111,284,7,1,lift+33.02f,0x3d4c49);
  Surface.rectangle(p,o,0,0,W,H,lift+36.7f,PRINT,light);
 }
 static void draw(PoseStack p,MultiBufferSource o,int light,boolean framed){GeometryCache.draw(framed?"ht100_new_frame":"ht100_new_bare",p,o,light,(q,b)->body(q,b,0,framed));}
 static void pose(Ht100.Unit b,PoseStack p){var offset=com.qwerty.homenet.move.DeviceOffsets.get(b.getLevel(),b.getBlockPos());p.translate(offset.x,offset.y,offset.z);double bottom=.5-Ht100.HEIGHT/2;switch(b.getBlockState().getValue(WallMountedBlock.FACING)){case NORTH->{p.translate(.5,bottom,1);p.mulPose(Axis.YP.rotationDegrees(180));}case SOUTH->p.translate(.5,bottom,0);case EAST->{p.translate(0,bottom,.5);p.mulPose(Axis.YP.rotationDegrees(90));}case WEST->{p.translate(1,bottom,.5);p.mulPose(Axis.YP.rotationDegrees(-90));}default->{}}p.scale(SCALE,SCALE,SCALE);p.translate(-W/2,H-700,0);}
 static final class Renderer implements BlockEntityRenderer<Ht100.Unit>{public void render(Ht100.Unit b,float time,PoseStack p,MultiBufferSource out,int light,int overlay){boolean framed=b.getBlockState().getValue(Ht100.FRAMED);String key=framed?"ht100_new_frame":"ht100_new_bare";if(DeviceVisibility.hidden(b,GeometryCache.bounds(key),q->pose(b,q)))return;p.pushPose();pose(b,p);draw(p,out,light,framed);if(b.getBlockState().getValue(Ht100.LIT))Surface.rectangle(p,out,72,66,83,44,(framed?16:0)+36.75f,GLOW,15728880);p.popPose();}}
}
