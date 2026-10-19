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
 private static void dome(PoseStack p,MultiBufferSource o,int l,Ring q){
  float cx=q.x+q.w/2,cy=q.y+q.h/2;
  for(int band=0;band<8;band++)for(int i=0;i<48;i++){
   float[] angles={edge(q,i)[0],edge(q,i+1)[0],edge(q,i+1)[0],edge(q,i)[0]};
   float[] ys={edge(q,i)[1],edge(q,i+1)[1],edge(q,i+1)[1],edge(q,i)[1]};
   for(int j=0;j<4;j++){float radial=1-(band+(j>=2?1:0))/8f,x=cx+(angles[j]-cx)*radial,y=cy+(ys[j]-cy)*radial,z=q.z+4*(1-radial*radial);float nx=(x-cx)*.0006f,ny=(y-cy)*.0012f,len=(float)Math.sqrt(1+nx*nx+ny*ny);vertex(p,o,l,x,y,z,nx/len,ny/len,1/len,q.color);}
  }
 }
 private static void sector(PoseStack p,MultiBufferSource o,int l,float cx,float cy,float outer,float inner,float zOuter,float zInner,int color){
  for(int i=0;i<64;i++){float a=(float)(i*Math.PI/32),b=(float)((i+1)*Math.PI/32);float[] r={a,b,b,a},d={outer,outer,inner,inner};for(int j=0;j<4;j++){float x=cx+(float)Math.cos(r[j])*d[j],y=cy+(float)Math.sin(r[j])*d[j];float gradient=(zOuter-zInner)/(outer-inner+.001f);float nx=(float)Math.cos(r[j])*gradient,ny=(float)Math.sin(r[j])*gradient;float length=(float)Math.sqrt(1+nx*nx+ny*ny);vertex(p,o,l,x,y,j<2?zOuter:zInner,nx/length,ny/length,1/length,color);}}
 }
 private static void buttonFace(PoseStack p,MultiBufferSource o,int l){
  // The centre is deliberately open: a solid button cap used to hide the concave bowl.
  float cx=175,cy=239,r=25;for(int i=0;i<64;i++){float a=(float)(i*Math.PI/32),b=(float)((i+1)*Math.PI/32);float[] angles={a,b,b,a};for(int j=0;j<4;j++){float t=angles[j],dx=(float)Math.cos(t),dy=(float)Math.sin(t),edge=Math.min(32.5f/Math.max(.0001f,Math.abs(dx)),33.5f/Math.max(.0001f,Math.abs(dy)));float radius=j<2?edge:r;vertex(p,o,l,cx+dx*radius,cy+dy*radius,75,0,0,1,j<2?0xc8cbbd:0xb6bbac);}}
  sector(p,o,l,cx,cy,25,19,75,72.5f,0xaeb5a7);sector(p,o,l,cx,cy,19,11,72.5f,70.6f,0xb9bfb1);sector(p,o,l,cx,cy,11,0,70.6f,70.1f,0xc3c9ba);
 }
 private static void print(PoseStack p,MultiBufferSource o,int l,ResourceLocation texture,float x,float y,float w,float h,float z){var b=o.getBuffer(RenderType.entityCutoutNoCull(texture));float[][] v={{x,y+h,0,1},{x+w,y+h,1,1},{x+w,y,1,0},{x,y,0,0}};for(var a:v)Surface.vertex(b,p.last(),a[0],a[1],z,0,0,1,0xffffff,l,a[2],a[3]);}
 static void mesh(PoseStack p,MultiBufferSource o){int l=0;
  // Frame depth is a single broad wall with one rolled front lip; the phone nests inside it.
  contour(p,o,l,true,new Ring(-6,-5,242,310,13,0,0x9da294),new Ring(-6,-5,242,310,13,35,0xa7ac9f),new Ring(-5,-4,240,308,12,41,0xc4c8b8));
  // One continuously rounded casing replaces the former stack of four visible slabs.
  contour(p,o,l,true,new Ring(0,0,230,300,17,35,0x8e9588),new Ring(0,0,230,300,17,51,0x9fa699),new Ring(1,1,228,298,16,61,0xb9beaf),new Ring(4,3,222,294,15,68,0xc5c9ba));
  // The photographed upper moulding swells forward and sweeps down into its lower curved edge.
  Ring upper=new Ring(31,9,171,178,15,82,0xc9cebe);
  contour(p,o,l,false,new Ring(23,6,187,189,18,68.5f,0x979f91),new Ring(23,6,187,189,18,70,0xa6ae9f),new Ring(25,7,183,186,17,76,0xbec4b4),upper);
  dome(p,o,l,upper);
  // One rounded side spine, rather than several parallel bright rails.
  contour(p,o,l,true,new Ring(7,6,22,286,11,68,0xb0b7a8),new Ring(8,7,20,284,10,73,0xc3c9b9),new Ring(10,9,16,280,8,77,0xd1d5c5));
  // HT-100S has a dark blank window, and no camera lens or video lamp.
  solid(p,o,l,"window_gasket",64,47,106,105,86.5f,.8f,3,0x555d59);
  solid(p,o,l,"window_glass",66,49,102,101,87.3f,1,2,0x303638);
  patch(p,o,l,70,52,94,1.1f,88.33f,0x414746);
  for(int i=0;i<2;i++)solid(p,o,l,"mic_"+i,212,38+i*8,9,3,69,1,.7f,0x232b2a);
  // The speaker is six narrow recessed slots within the lower skin, each with a gentle lower highlight.
  for(int i=0;i<6;i++){float y=205+i*13;patch(p,o,l,26,y-2,104,2,69.2f,0xc7ccbc);solid(p,o,l,"speaker_floor_"+i,29,y,99,3.6f,68.2f,.35f,1.7f,0x424b44);patch(p,o,l,30,y+3.6f,97,.65f,69.4f,0xabb3a4);}
  // A single edged call key with an actual opening in its face and a sloping concave finger bowl.
  contour(p,o,l,false,new Ring(140,202,70,73,6,68.5f,0x8d9589),new Ring(140,202,70,73,6,71,0xaeb5a6),new Ring(142,204,66,69,5,75,0xd0d4c5));
  buttonFace(p,o,l);solid(p,o,l,"button_indicator",169,209,13,4.5f,75,.4f,2,0x222b2c);
  // The cable opening sits inside the lower edge; its dark floor remains behind two small holes.
  solid(p,o,l,"notch_floor",107,275,25,24,68.1f,.4f,11,0x414844);
  solid(p,o,l,"notch_inner",110,279,19,20,68.6f,.3f,9,0x899084);
  for(int i=0;i<2;i++)solid(p,o,l,"notch_hole_"+i,113+i*9,279,4,5,69,.2f,2,0x252b29);
  print(p,o,l,LOGO,68,19,109,25,86.8f);print(p,o,l,BRAND,146,278,64,14,68.4f);
 }
 static void draw(PoseStack p,MultiBufferSource o,int light){GeometryCache.draw("ht100s_refined",p,o,light,Ht100sVisual::mesh);}
 static void pose(Ht100s.Unit b,PoseStack p){var off=com.qwerty.homenet.move.DeviceOffsets.get(b.getLevel(),b.getBlockPos());p.translate(off.x,off.y,off.z);double bottom=.5-Ht100s.HEIGHT/2;switch(b.getBlockState().getValue(WallMountedBlock.FACING)){case NORTH->{p.translate(.5,bottom,1);p.mulPose(Axis.YP.rotationDegrees(180));}case SOUTH->p.translate(.5,bottom,0);case EAST->{p.translate(0,bottom,.5);p.mulPose(Axis.YP.rotationDegrees(90));}case WEST->{p.translate(1,bottom,.5);p.mulPose(Axis.YP.rotationDegrees(-90));}default->{}}p.scale(S,S,S);p.translate(-W/2,H-700,0);}
 static final class Renderer implements BlockEntityRenderer<Ht100s.Unit>{public void render(Ht100s.Unit b,float tick,PoseStack p,MultiBufferSource o,int light,int overlay){if(DeviceVisibility.hidden(b,GeometryCache.bounds("ht100s_refined"),q->pose(b,q)))return;p.pushPose();pose(b,p);draw(p,o,light);p.popPose();}}
}
