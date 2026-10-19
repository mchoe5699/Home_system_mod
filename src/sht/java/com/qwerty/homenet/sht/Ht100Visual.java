package com.qwerty.homenet.sht;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.qwerty.homenet.block.WallMountedBlock;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;

/** HT-100 clean-reference casing with a continuous rolled shell and detachable rear surround. */
final class Ht100Visual {
 static final float W=226,H=300,SCALE=(float)(Ht100.HEIGHT/H);
 static final ResourceLocation PRINT=Product.id("ht100/label.png"),BRAND=Product.id("ht100/brand.png"),GLOW=Product.id("ht100/glow.png");
 private static final ResourceLocation SOLID=Product.id("textures/solid.png");
 private static final int SHELL=0xd2d4d1,UPPER=0xd8d9d5,LOWER=0xd4d6d2;
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
 private static void lens(PoseStack p,MultiBufferSource o,int light,float lift){
  // Concentric annuli leave the central optic visibly set back within its bezel.
  float z=lift+33.75f;
  ring(p,o,light,0x1c2025,outline(88,100,50,50,25,z+.8f),outline(92,104,42,42,21,z+1.1f));
  ring(p,o,light,0x302534,outline(92,104,42,42,21,z+1.12f),outline(97,109,32,32,16,z+.7f));
  ring(p,o,light,0x111720,outline(97,109,32,32,16,z+.72f),outline(103,115,20,20,10,z+.32f));
  fill(p,o,light,outline(103,115,20,20,10,z+.34f),0x080e17);
  circle(p,o,light,109,121,2.5f,z+.36f,z+.38f,0x283b4b);
 }
 private static void slit(PoseStack p,MultiBufferSource o,int light,float y,float z){
  // The front skin stops at each opening; the dark floor is recessed behind it.
  float x=29,w=98,h=2.9f;
  fill(p,o,light,outline(x,y,w,h,1.45f,z-.6f),0x586668);
  ring(p,o,light,0x9da9a9,outline(x-.5f,y-.5f,w+1,h+1,1.8f,z+.025f),outline(x,y,w,h,1.45f,z-.25f));
  rect(p,o,light,x+2,y+h+.35f,w-4,.36f,z+.03f,0xe0e5e2);
 }
 private static void lowerFace(PoseStack p,MultiBufferSource o,int light,float z){
  float x=19,right=214,last=190;
  for(int i=0;i<6;i++){
   float y=202+i*11.25f,end=y+2.9f;
   rect(p,o,light,x,last,right-x,y-last,z,LOWER);
   rect(p,o,light,x,y,29-x,end-y,z,LOWER);
   rect(p,o,light,127,y,right-127,end-y,z,LOWER);
   // Cover the rectangular aperture corners so that the visible cut-out has round ends.
   for(int side=0;side<2;side++)for(int step=0;step<3;step++){
    float yy=y+step*hThird(),xx=side==0?29:127;
    float inset=(step==0||step==2)?1.1f:.25f;
    if(side==0)rect(p,o,light,xx,yy,inset,hThird(),z+.035f,LOWER);
    else rect(p,o,light,xx-inset,yy,inset,hThird(),z+.035f,LOWER);
   }
   slit(p,o,light,y,z);last=end;
  }
  rect(p,o,light,x,last,right-x,294-last,z,LOWER);
 }
 private static float hThird(){return 2.9f/3;}
 private static void bottomClip(PoseStack p,MultiBufferSource o,int light,float z){
  // Semicircular crown with straight legs reaches the lower casing edge.
  float cx=113,r=12,cy=282;
  Point[] shape=new Point[19];
  for(int i=0;i<=16;i++){double a=Math.PI+i*Math.PI/16;shape[i]=new Point(cx+r*(float)Math.cos(a),cy+r*(float)Math.sin(a));}
  shape[17]=new Point(cx+r,300);shape[18]=new Point(cx-r,300);
  for(int i=0;i<shape.length;i++){int j=(i+1)%shape.length;
   quad(p,o,light,at(shape[i],z),at(shape[j],z),at(shape[j],z+1.8f),at(shape[i],z+1.8f),0x9aa8aa,0,0,1);
  }
  float[] center={cx,289,z+1.8f};
  for(int i=0;i<shape.length;i++){int j=(i+1)%shape.length;
   quad(p,o,light,center,at(shape[i],z+1.8f),at(shape[j],z+1.8f),center,0xb6c0c0,0,0,1);
  }
  rect(p,o,light,110,279,6,1,z+1.82f,0x48595c);
 }
 private static void body(PoseStack p,MultiBufferSource o,int light,boolean framed){
  float lift=framed?12:0;
  if(framed){
   // A detachable rear surround with its own perimeter and bevel.
   Outline rear=outline(-6,-5,238,310,12,0);
   Outline wall=outline(-6,-5,238,310,12,9);
   Outline lip=outline(-3,-3,232,306,10,13);
   loft(p,o,light,0xc8cecb,rear,wall,lip);fill(p,o,light,lip,0xd3d7d3);
   ring(p,o,light,0xa6aeaa,outline(-3,-3,232,306,10,13.02f),outline(1,1,224,298,11,13.06f));
  }
  // A single rounded casting grows from its narrow back into the broad sidewall,
  // then curves into the front. The left shoulder is wider than the right one.
  Outline back=outline(5,3,216,294,13,lift);
  Outline belly=outline(0,0,W,H,18,lift+11);
  Outline side=outline(0,0,W,H,18,lift+19);
  Outline shoulder=outline(3,2,220,296,16,lift+25);
  Outline crest=outline(10,4,206,292,14,lift+33.35f);
  Outline front=outline(20,7,191,286,12,lift+32.2f);
  loft(p,o,light,0xc6cbc8,back,belly,side,shoulder);
  loft(p,o,light,SHELL,shoulder,crest,front);fill(p,o,light,front,SHELL);
  // Upper cover is one broad, slightly swollen moulding with a recessed joint.
  Outline seam=outline(20,8,186,183,11,lift+32.03f);
  Outline panel=outline(23,10,180,179,9,lift+32.65f);
  ring(p,o,light,0xb2bab7,seam,outline(22,9,182,181,10,lift+32.08f));
  plate(p,o,light,UPPER,outline(22,9,182,181,10,lift+32.1f),panel);
  rect(p,o,light,22,189,182,1.0f,lift+32.2f,0x9ba5a4);
  lowerFace(p,o,light,lift+32.2f);
  // The clean reference has a near-square charcoal glass, not a tall window.
  Outline bezel=outline(57,46,112,113,4.2f,lift+32.7f);
  Outline rim=outline(59,48,108,109,3.4f,lift+33.1f);
  plate(p,o,light,0x697477,bezel,rim);
  plate(p,o,light,0x273135,outline(61,50,104,105,2.5f,lift+33.12f),outline(61,50,104,105,2.5f,lift+33.3f));
  // The upper emitter is dark navy when idle. Peeling blue material is damage, not paint.
  plate(p,o,light,0x0b1c2a,outline(75,60,76,34,1.7f,lift+33.33f),outline(75,60,76,34,1.7f,lift+33.55f));
  rect(p,o,light,78,62,70,.9f,lift+33.57f,0x344a5b);
  lens(p,o,light,lift);
  // Two small microphone slots pass through the upper right rolled rail.
  for(int i=0;i<2;i++){
   float y=35+i*10;
   plate(p,o,light,0x39474c,outline(210,y,8,3.2f,1.5f,lift+32.05f),outline(210,y,8,3.2f,1.5f,lift+32.2f));
  }
  // Inset perimeter, then a softly raised call key with a dark status notch.
  ring(p,o,light,0x9aa5a4,outline(137,196,73,79,7,lift+32.22f),outline(139,198,69,75,6.4f,lift+32.24f));
  plate(p,o,light,0xdce0dc,outline(141,200,65,71,6.5f,lift+32.25f),outline(142,201,63,69,7,lift+33.4f));
  plate(p,o,light,0x273b45,outline(165,206,15,4.3f,2,lift+33.42f),outline(165,206,15,4.3f,2,lift+33.58f));
  bottomClip(p,o,light,lift+32.25f);
  Surface.rectangle(p,o,58,17,110,32,lift+32.74f,PRINT,light);
  Surface.rectangle(p,o,141,276,70,18,lift+32.5f,BRAND,light);
 }
 static void draw(PoseStack p,MultiBufferSource o,int light,boolean framed){GeometryCache.draw(framed?"ht100_photo_frame":"ht100_photo_bare",p,o,light,(q,b)->body(q,b,0,framed));}
 static void pose(Ht100.Unit b,PoseStack p){var offset=com.qwerty.homenet.move.DeviceOffsets.get(b.getLevel(),b.getBlockPos());p.translate(offset.x,offset.y,offset.z);double bottom=.5-Ht100.HEIGHT/2;switch(b.getBlockState().getValue(WallMountedBlock.FACING)){case NORTH->{p.translate(.5,bottom,1);p.mulPose(Axis.YP.rotationDegrees(180));}case SOUTH->p.translate(.5,bottom,0);case EAST->{p.translate(0,bottom,.5);p.mulPose(Axis.YP.rotationDegrees(90));}case WEST->{p.translate(1,bottom,.5);p.mulPose(Axis.YP.rotationDegrees(-90));}default->{}}p.scale(SCALE,SCALE,SCALE);p.translate(-W/2,H-700,0);}
 static final class Renderer implements BlockEntityRenderer<Ht100.Unit>{public void render(Ht100.Unit b,float time,PoseStack p,MultiBufferSource out,int light,int overlay){boolean framed=b.getBlockState().getValue(Ht100.FRAMED);String key=framed?"ht100_photo_frame":"ht100_photo_bare";if(DeviceVisibility.hidden(b,GeometryCache.bounds(key),q->pose(b,q)))return;p.pushPose();pose(b,p);draw(p,out,light,framed);if(b.getBlockState().getValue(Ht100.LIT))Surface.rectangle(p,out,75,60,76,34,(framed?12:0)+33.58f,GLOW,15728880);p.popPose();}}
}
