package com.qwerty.homenet.sht;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.qwerty.homenet.block.WallMountedBlock;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;

/** Photo-shaped HT-100S: a fitted tray, one continuous handset shell and recessed front details. */
final class Ht100sVisual {
 static final float W=230,H=300,S=(float)(Ht100s.HEIGHT/H);
 static final float WINDOW_X=67,WINDOW_Y=46,WINDOW_W=102,WINDOW_H=102;
 static final float UPPER_TOP_Z=80;
 static final float FRONT_OFFSET_X=-3;
 static final ResourceLocation LOGO=Product.id("ht100s/logo.png"),BRAND=Product.id("ht100s/brand.png"),WHITE=Product.id("textures/solid.png");
 private record Ring(float x,float y,float w,float h,float r,float z,int color){}
 static void solid(PoseStack p,MultiBufferSource o,int l,String name,float x,float y,float w,float h,float z,float d,float r,int c){Surface.solid(new Surface.Solid(name,x,y,w,h,z,d,r,String.format("#%06x",c),"ht100s",0),p,o,l);}
 static void patch(PoseStack p,MultiBufferSource o,int l,float x,float y,float w,float h,float z,int c){Surface.patch(p,o,x,y,w,h,z,c,l);}
 private static float[] edge(Ring q,int i){int corner=(i%48)/12;float angle=(float)Math.toRadians(-90+corner*90+(i%12)*7.5);float r=q.r;float cx=q.x+(corner==0||corner==1?q.w-r:r),cy=q.y+(corner==0||corner==3?r:q.h-r);return new float[]{cx+r*(float)Math.cos(angle),cy+r*(float)Math.sin(angle),(float)Math.cos(angle),(float)Math.sin(angle)};}
 private static void vertex(PoseStack p,MultiBufferSource o,int l,float x,float y,float z,float nx,float ny,float nz,int c){Surface.vertex(o.getBuffer(RenderType.entityCutoutNoCull(WHITE)),p.last(),x,y,z,nx,ny,nz,c,l,.5f,.5f);}
 private static void contour(PoseStack p,MultiBufferSource o,int l,boolean cap,Ring... rings){
  for(int n=0;n<rings.length-1;n++){Ring a=rings[n],b=rings[n+1];for(int i=0;i<48;i++){float[] u=edge(a,i),v=edge(a,i+1),w=edge(b,i+1),x=edge(b,i);float nz=(b.z-a.z)==0?0:.18f;int col=b.color;vertex(p,o,l,u[0],u[1],a.z,u[2],u[3],nz,col);vertex(p,o,l,v[0],v[1],a.z,v[2],v[3],nz,col);vertex(p,o,l,w[0],w[1],b.z,w[2],w[3],nz,col);vertex(p,o,l,x[0],x[1],b.z,x[2],x[3],nz,col);}}
  if(cap){Ring q=rings[rings.length-1];float cx=q.x+q.w/2,cy=q.y+q.h/2;for(int i=0;i<48;i++){float[] a=edge(q,i),b=edge(q,i+1);vertex(p,o,l,a[0],a[1],q.z,0,0,1,q.color);vertex(p,o,l,b[0],b[1],q.z,0,0,1,q.color);vertex(p,o,l,cx,cy,q.z,0,0,1,q.color);vertex(p,o,l,cx,cy,q.z,0,0,1,q.color);}}
 }
 private static void rearCap(PoseStack p,MultiBufferSource o,int l,Ring q){
  float cx=q.x+q.w/2,cy=q.y+q.h/2;
  for(int i=0;i<48;i++){
   float[] a=edge(q,i),b=edge(q,i+1);
   vertex(p,o,l,a[0],a[1],q.z,0,0,-1,q.color);
   vertex(p,o,l,cx,cy,q.z,0,0,-1,q.color);
   vertex(p,o,l,b[0],b[1],q.z,0,0,-1,q.color);
   vertex(p,o,l,b[0],b[1],q.z,0,0,-1,q.color);
  }
 }
 private static void bodyShell(PoseStack p,MultiBufferSource o,int l){
  // A straight side meets a circular front fillet tangentially at both ends.
  final int steps=16;Ring[] rings=new Ring[steps+2];
  rings[0]=new Ring(0,0,230,300,13,35,0xaeb5a7);
  for(int j=0;j<=steps;j++){
   double angle=j*Math.PI/(2*steps),inset=9*(1-Math.cos(angle));
   float at=(float)inset,z=(float)(59+9*Math.sin(angle));
   int red=(int)(0xae+(0xc5-0xae)*Math.sin(angle));
   int green=(int)(0xb5+(0xc9-0xb5)*Math.sin(angle));
   int blue=(int)(0xa7+(0xba-0xa7)*Math.sin(angle));
   rings[j+1]=new Ring(at,at,230-2*at,300-2*at,13,z,(red<<16)|(green<<8)|blue);
  }
  for(int n=0;n<rings.length-1;n++){
   Ring a=rings[n],b=rings[n+1];
   float angleA=(float)(Math.max(0,n-1)*Math.PI/(2*steps));
   float angleB=(float)(n*Math.PI/(2*steps));
   for(int i=0;i<48;i++){
    float[] u=edge(a,i),v=edge(a,i+1),w=edge(b,i+1),x=edge(b,i);
    bodyVertex(p,o,l,u,a,angleA);bodyVertex(p,o,l,v,a,angleA);
    bodyVertex(p,o,l,w,b,angleB);bodyVertex(p,o,l,x,b,angleB);
   }
  }
 }
 private static void bodyVertex(PoseStack p,MultiBufferSource o,int l,float[] at,Ring ring,float angle){
  float radial=(float)Math.cos(angle);
  vertex(p,o,l,at[0],at[1],ring.z,at[2]*radial,at[3]*radial,(float)Math.sin(angle),ring.color);
 }
 private static void bodyFace(PoseStack p,MultiBufferSource o,int l){
  // Leave the call-key socket open in the casing, so the key can sit below the surrounding face.
  Ring outer=new Ring(9,9,212,282,13,68,0xc5c9ba),inner=new Ring(16,14,198,272,0,68,0xc5c9ba);
  for(int i=0;i<48;i++){float[] a=edge(outer,i),b=edge(outer,i+1),c=edge(inner,i+1),d=edge(inner,i);
   for(float[] q:new float[][]{a,b,c,d})vertex(p,o,l,q[0],q[1],68,0,0,1,0xc5c9ba);
  }
  patch(p,o,l,16,14,198,188,68,0xc5c9ba);
  patch(p,o,l,16,202,121,73,68,0xc5c9ba);
  patch(p,o,l,207,202,7,73,68,0xc5c9ba);
  patch(p,o,l,16,275,198,11,68,0xc5c9ba);
 }
 static float upperZ(float x,float y){
  // A broad, nearly flat moulding surrounds the blank HT-100S panel like the HT-100 shell.
  float tx=Math.min((x-24)/(WINDOW_X-10-24),(212-x)/(212-WINDOW_X-WINDOW_W-10));
  float ty=Math.min((y-9)/(WINDOW_Y-11-9),(191-y)/(191-WINDOW_Y-WINDOW_H-11));
  tx=Math.max(0,Math.min(1,tx));ty=Math.max(0,Math.min(1,ty));
  // A smooth minimum keeps the four diagonal slopes without making corner hollows.
  float blend=.35f,h=Math.max(0,blend-Math.abs(tx-ty))/blend;
  float t=Math.max(0,Math.min(1,(Math.min(tx,ty)-h*h*blend*.25f)/(1-blend*.25f)));
  return 68+(UPPER_TOP_Z-68)*t*t*(3-2*t);
 }
 private static void upperVertex(PoseStack p,MultiBufferSource o,int l,float x,float y){
  float z=upperZ(x,y),nx=upperZ(x-1,y)-upperZ(x+1,y),ny=upperZ(x,y-1)-upperZ(x,y+1),length=(float)Math.sqrt(nx*nx+ny*ny+4);
  int shade=(int)(0xc5+(z-68)*.2f);
  int color=(shade<<16)|((shade+4)<<8)|(shade-11);
  vertex(p,o,l,x,y,z,nx/length,ny/length,2/length,color);
 }
 private static void upperFace(PoseStack p,MultiBufferSource o,int l){
  for(int row=0;row<60;row++)for(int col=0;col<48;col++){
   float x0=24+col*188f/48,x1=24+(col+1)*188f/48,y0=9+row*182f/60,y1=9+(row+1)*182f/60;
   upperVertex(p,o,l,x0,y1);upperVertex(p,o,l,x1,y1);upperVertex(p,o,l,x1,y0);upperVertex(p,o,l,x0,y0);
  }
 }
 private static float[] upperOutline(int i,float inset){
  int corner=(i%48)/12;
  float angle=(float)Math.toRadians(-90+corner*90+(i%12)*7.5);
  float cx=corner==0?200:corner==1?192:corner==2?44:36;
  float cy=corner==0||corner==3?21:171;
  float radius=(corner==0||corner==3?12:20)-inset;
  return new float[]{cx+radius*(float)Math.cos(angle),cy+radius*(float)Math.sin(angle)};
 }
 private static void upperSeam(PoseStack p,MultiBufferSource o,int l){
  // The two rounded shoulders rise to the top edge; the logo area has no crosswise seam.
  for(int i=0;i<47;i++){
   float[][] points={upperOutline(i,0),upperOutline(i+1,0),upperOutline(i+1,1.15f),upperOutline(i,1.15f)};
   for(float[] xy:points)vertex(p,o,l,xy[0],xy[1],upperZ(xy[0],xy[1])+.18f,0,0,1,0x8e9589);
  }
 }
 private static void sector(PoseStack p,MultiBufferSource o,int l,float cx,float cy,float outer,float inner,float zOuter,float zInner,int color){
  for(int i=0;i<64;i++){float a=(float)(i*Math.PI/32),b=(float)((i+1)*Math.PI/32);float[] r={a,b,b,a},d={outer,outer,inner,inner};for(int j=0;j<4;j++){float x=cx+(float)Math.cos(r[j])*d[j],y=cy+(float)Math.sin(r[j])*d[j];float gradient=(zOuter-zInner)/(outer-inner+.001f);float nx=(float)Math.cos(r[j])*gradient,ny=(float)Math.sin(r[j])*gradient;float length=(float)Math.sqrt(1+nx*nx+ny*ny);vertex(p,o,l,x,y,j<2?zOuter:zInner,nx/length,ny/length,1/length,color);}}
 }
 private static void buttonFace(PoseStack p,MultiBufferSource o,int l){
  // The centre is deliberately open: a solid button cap used to hide the concave bowl.
  float cx=175,cy=239,r=25;for(int i=0;i<64;i++){float a=(float)(i*Math.PI/32),b=(float)((i+1)*Math.PI/32);float[] angles={a,b,b,a};for(int j=0;j<4;j++){float t=angles[j],dx=(float)Math.cos(t),dy=(float)Math.sin(t),edge=Math.min(32.5f/Math.max(.0001f,Math.abs(dx)),33.5f/Math.max(.0001f,Math.abs(dy)));float radius=j<2?edge:r;vertex(p,o,l,cx+dx*radius,cy+dy*radius,67.9f,0,0,1,j<2?0xc8cbbd:0xb6bbac);}}
  sector(p,o,l,cx,cy,25,19,67.9f,67.2f,0xaeb5a7);sector(p,o,l,cx,cy,19,11,67.2f,66.15f,0xb9bfb1);sector(p,o,l,cx,cy,11,0,66.15f,65.8f,0xc3c9ba);
 }
 private static void arch(PoseStack p,MultiBufferSource o,int l,float x,float top,float w,float bottom,float z,int color){
  float r=w/2,cy=top+r,cx=x+r;
  patch(p,o,l,x,cy,w,bottom-cy,z,color);
  for(int i=0;i<32;i++){float a=(float)(Math.PI+i*Math.PI/32),b=(float)(Math.PI+(i+1)*Math.PI/32);
   vertex(p,o,l,cx+r*(float)Math.cos(a),cy+r*(float)Math.sin(a),z,0,0,1,color);
   vertex(p,o,l,cx+r*(float)Math.cos(b),cy+r*(float)Math.sin(b),z,0,0,1,color);
   vertex(p,o,l,cx,cy,z,0,0,1,color);vertex(p,o,l,cx,cy,z,0,0,1,color);
  }
 }
 private static void print(PoseStack p,MultiBufferSource o,int l,ResourceLocation texture,float x,float y,float w,float h,float z){var b=o.getBuffer(RenderType.entityCutoutNoCull(texture));float[][] v={{x,y+h,0,1},{x+w,y+h,1,1},{x+w,y,1,0},{x,y,0,0}};for(var a:v)Surface.vertex(b,p.last(),a[0],a[1],z,0,0,1,0xffffff,l,a[2],a[3]);}
 private static void curvedLogo(PoseStack p,MultiBufferSource o,int l){
  var b=o.getBuffer(RenderType.entityCutoutNoCull(LOGO));
  for(int row=0;row<6;row++)for(int col=0;col<24;col++)for(int corner=0;corner<4;corner++){
   float u=(col+(corner==1||corner==2?1:0))/24f,v=(row+(corner<2?1:0))/6f,x=68+109*u,y=15+25*v;
   Surface.vertex(b,p.last(),x,y,upperZ(x,y)+.22f,0,0,1,0xffffff,l,u,v);
  }
 }
 static void mesh(PoseStack p,MultiBufferSource o){int l=0;
  // Frame depth is a single broad wall with one rolled front lip; the phone nests inside it.
  Ring rear=new Ring(-6,-5,242,310,13,0,0x9da294);
  rearCap(p,o,l,rear);
  contour(p,o,l,true,rear,new Ring(-6,-5,242,310,13,35,0xa7ac9f),new Ring(-5,-4,240,308,12,41,0xc4c8b8));
  bodyShell(p,o,l);
  bodyFace(p,o,l);
  p.pushPose();p.translate(FRONT_OFFSET_X,0,0);
  // The broad upper region rises within the body itself; its top, bottom and both sides are rounded.
  upperFace(p,o,l);
  upperSeam(p,o,l);
  // The casing shoulder remains visible left of the raised surface; no separate long rail crosses it.
  // HT-100S has a dark blank window, and no camera lens or video lamp.
  solid(p,o,l,"window_gasket",WINDOW_X,WINDOW_Y,WINDOW_W,WINDOW_H,UPPER_TOP_Z+.15f,.8f,3,0x555d59);
  solid(p,o,l,"window_glass",WINDOW_X+2,WINDOW_Y+2,WINDOW_W-4,WINDOW_H-4,UPPER_TOP_Z+.95f,1,2,0x303638);
  patch(p,o,l,WINDOW_X+6,WINDOW_Y+5,WINDOW_W-12,1.1f,UPPER_TOP_Z+1.98f,0x414746);
  // The speaker is six narrow recessed slots within the lower skin, each with a gentle lower highlight.
  for(int i=0;i<6;i++){float y=205+i*13;patch(p,o,l,26,y-2,104,2,69.2f,0xc7ccbc);solid(p,o,l,"speaker_floor_"+i,29,y,99,3.6f,68.2f,.35f,1.7f,0x424b44);patch(p,o,l,30,y+3.6f,97,.65f,69.4f,0xabb3a4);}
  // A single edged call key with an actual opening in its face and a sloping concave finger bowl.
  contour(p,o,l,false,new Ring(140,202,70,73,6,65.5f,0x899184),new Ring(140,202,70,73,6,67.2f,0xb9c0b0),new Ring(142,204,66,69,5,67.9f,0xd0d4c5));
  buttonFace(p,o,l);solid(p,o,l,"button_indicator",169,209,13,4.5f,67.95f,.3f,2,0x222b2c);
  // The plug follows the arched opening exactly and sits flush with the lower rounded shell.
  arch(p,o,l,107,274.5f,25,291,68.25f,0x646b61);
  arch(p,o,l,108.5f,276,22,290.8f,68.42f,0xaeb4a6);
  curvedLogo(p,o,l);print(p,o,l,BRAND,146,278,64,12,68.4f);
  p.popPose();
  for(int i=0;i<2;i++)solid(p,o,l,"mic_"+i,212,38+i*8,9,3,69,1,.7f,0x232b2a);
 }
 static void draw(PoseStack p,MultiBufferSource o,int light){GeometryCache.draw("ht100s_broad_moulding",p,o,light,Ht100sVisual::mesh);}
 static void pose(Ht100s.Unit b,PoseStack p){var off=com.qwerty.homenet.move.DeviceOffsets.get(b.getLevel(),b.getBlockPos());p.translate(off.x,off.y,off.z);double bottom=.5-Ht100s.HEIGHT/2;switch(b.getBlockState().getValue(WallMountedBlock.FACING)){case NORTH->{p.translate(.5,bottom,1);p.mulPose(Axis.YP.rotationDegrees(180));}case SOUTH->p.translate(.5,bottom,0);case EAST->{p.translate(0,bottom,.5);p.mulPose(Axis.YP.rotationDegrees(90));}case WEST->{p.translate(1,bottom,.5);p.mulPose(Axis.YP.rotationDegrees(-90));}default->{}}p.scale(S,S,S);p.translate(-W/2,H-700,0);}
 static final class Renderer implements BlockEntityRenderer<Ht100s.Unit>{public void render(Ht100s.Unit b,float tick,PoseStack p,MultiBufferSource o,int light,int overlay){if(DeviceVisibility.hidden(b,GeometryCache.bounds("ht100s_broad_moulding"),q->pose(b,q)))return;p.pushPose();pose(b,p);draw(p,o,light);p.popPose();}}
}
