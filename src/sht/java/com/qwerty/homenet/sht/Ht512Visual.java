package com.qwerty.homenet.sht;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.qwerty.homenet.block.WallMountedBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** A shared immutable mesh: mitered frame, cut round speaker apertures, recessed slot and raised keys. */
final class Ht512Visual {
 static final float W=464,H=610,SCALE=(float)(Ht512.HEIGHT/H);
 static final ResourceLocation FRONT=Product.id("ht512/front.png"),INK=Product.id("ht512/key_ink.png");
 record Key(String label,float x,float y,float w,float h){}
 record KeyStyle(float socketRadius,float rimInset,float rimRadius,float faceInset,float faceRadius,float labelInset,String socketColor,String rimColor,String faceColor){}
 static KeyStyle keyStyle;static List<Key> keys;
 static void load(){if(keys!=null)return;try(var r=Minecraft.getInstance().getResourceManager().openAsReader(Product.id("ht512/model.json"))){var root=com.google.gson.JsonParser.parseReader(r).getAsJsonObject();var list=new ArrayList<Key>();for(var e:root.getAsJsonArray("keys"))list.add(new com.google.gson.Gson().fromJson(e,Key.class));keyStyle=new com.google.gson.Gson().fromJson(root.get("keyStyle"),KeyStyle.class);keys=List.copyOf(list);}catch(Exception e){throw new IllegalStateException("HT-512 model",e);}}
 static void vertex(VertexConsumer b,PoseStack.Pose p,float x,float y,float z,float nx,float ny,float nz,int c,int light,float u,float v){Surface.vertex(b,p,x,y,z,nx,ny,nz,c,light,u,v);}
 static void face(PoseStack p,MultiBufferSource out,int light,float[][] points,ResourceLocation texture,int color){
  var b=out.getBuffer(RenderType.entityCutoutNoCull(texture));var a=points[0];var c=points[1];var d=points[2];float ux=c[0]-a[0],uy=c[1]-a[1],uz=c[2]-a[2],vx=d[0]-a[0],vy=d[1]-a[1],vz=d[2]-a[2];float nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx;float len=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);if(len==0){nx=ny=0;nz=1;len=1;}if(nz<0){nx=-nx;ny=-ny;nz=-nz;}
  for(var q:points)vertex(b,p.last(),q[0],q[1],q[2],nx/len,ny/len,nz/len,color,light,texture.equals(Product.id("textures/solid.png"))?.5f:q[0]/W,texture.equals(Product.id("textures/solid.png"))?.5f:q[1]/H);
 }
 static void rect(PoseStack p,MultiBufferSource out,int light,float x,float y,float w,float h,float z,ResourceLocation tex){face(p,out,light,new float[][]{{x,y+h,z},{x+w,y+h,z},{x+w,y,z},{x,y,z}},tex,0xffffff);}
 static void solid(PoseStack p,MultiBufferSource o,int light,String name,float x,float y,float w,float h,float z,float depth,float radius,int color,float curve){Surface.solid(new Surface.Solid(name,x,y,w,h,z,depth,radius,String.format("#%06x",color),"ht512",curve),p,o,light);}
 // Sixteen outline vertices suffice for these small rounded keys. Avoid handset-density meshes.
 static float[] keyEdge(float x,float y,float w,float h,float r,int i,float inset){int corner=(i%16)/4;double a=(corner*90+(i%4)*30)*Math.PI/180;float cx=x+(corner==0||corner==3?w-r:r),cy=y+(corner<2?h-r:r);float nx=(float)Math.cos(a),ny=(float)Math.sin(a);return new float[]{cx+Math.max(0,r-inset)*nx,cy+Math.max(0,r-inset)*ny,nx,ny};}
 static void keySolid(PoseStack p,MultiBufferSource o,int light,float x,float y,float w,float h,float z,float depth,float radius,int color,float curve){
  var b=o.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));float bevel=Math.min(.5f,depth*.35f),front=z+depth;
  for(int i=0;i<16;i++){
   for(int ring=0;ring<2;ring++)for(int j=0;j<4;j++){boolean upper=j==1||j==2;int profile=ring+(upper?1:0);var at=keyEdge(x,y,w,h,radius,i+(j>=2?1:0),profile==2?bevel:0);float zz=profile==0?z:profile==1?front-bevel:front;float nx=at[2],ny=at[3],nz=profile==2?1:0;float len=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);vertex(b,p.last(),at[0],at[1],zz,nx/len,ny/len,nz/len,color,light,.5f,.5f);}
   var a=keyEdge(x,y,w,h,radius,i,bevel);var c=keyEdge(x,y,w,h,radius,i+1,bevel);float cx=x+w/2,cy=y+h/2;int rings=curve>0?3:1;
   for(int ring=0;ring<rings;ring++)for(int j=0;j<4;j++){float radial=1-(ring+(j==1||j==2?1:0))/(float)rings;var at=j<2?a:c;float vx=cx+(at[0]-cx)*radial,vy=cy+(at[1]-cy)*radial;float nx=curve*2*(vx-cx)/(w*w/4),ny=curve*2*(vy-cy)/(h*h/4),len=(float)Math.sqrt(1+nx*nx+ny*ny);vertex(b,p.last(),vx,vy,front+curve*(1-radial*radial),nx/len,ny/len,1/len,color,light,.5f,.5f);}
  }
 }
 static void trim(PoseStack p,MultiBufferSource o,int light,float inset0,float z0,float inset1,float z1,ResourceLocation tex,int color){
  float[][] a={{inset0,inset0,z0},{W-inset0,inset0,z0},{W-inset0,H-inset0,z0},{inset0,H-inset0,z0}},b={{inset1,inset1,z1},{W-inset1,inset1,z1},{W-inset1,H-inset1,z1},{inset1,H-inset1,z1}};
  for(int i=0;i<4;i++)face(p,o,light,new float[][]{a[i],a[(i+1)%4],b[(i+1)%4],b[i]},tex,color);
 }
 static boolean cut(float x,float y){return x>=78&&x<=402&&y>=210&&y<=306||x>=107&&x<=198.8f&&y>=330&&y<=442.2f||x>=112&&x<=194&&y>=462&&y<=538;}
 static void mesh(PoseStack p,MultiBufferSource o){int l=0;load();var white=Product.id("textures/solid.png");
  solid(p,o,l,"ht_back",0,0,W,H,0,4,1,0x8f8d74,0);
  trim(p,o,l,0,4,1.5f,7,white,0xb9b79d);trim(p,o,l,1.5f,7,17,18,FRONT,0xffffff);
  // Hairline reveal between the four mitered rails and the flat inner plate.
  trim(p,o,l,17,18,18,15,white,0x555c4a);
  float[] xs={18,78,107,112,194,198.8f,402,W-18},ys={18,210,306,330,442.2f,462,538,H-18};
  for(int x=0;x<xs.length-1;x++)for(int y=0;y<ys.length-1;y++)if(!cut((xs[x]+xs[x+1])/2,(ys[y]+ys[y+1])/2))rect(p,o,l,xs[x],ys[y],xs[x+1]-xs[x],ys[y+1]-ys[y],15,FRONT);
  solid(p,o,l,"display_bezel",78,210,324,96,12,4,1,0x111915,0);rect(p,o,l,80,212,320,92,16.05f,FRONT);
  // Lens ring and recessed front optics behind the smoked cover.
  Surface.rim(p,o,l,new Surface.Solid("lens",157,255,23,23,16.5f,0,11.5f,"","",0),new Surface.Solid("lens",162,260,13,13,16.1f,0,6.5f,"","",0),0x444f40);
  rect(p,o,l,162,260,13,13,16.15f,FRONT);
  // The rectangular cell fills the panel around a real sixteen-sided round aperture.
  for(int row=0;row<11;row++)for(int col=0;col<9;col++){
   float cx=112.1f+col*10.2f,cy=335.1f+row*10.2f;
   for(int i=0;i<16;i++){double a=i*Math.PI/8,b=(i+1)*Math.PI/8;float ax=(float)Math.cos(a),ay=(float)Math.sin(a),bx=(float)Math.cos(b),by=(float)Math.sin(b);float ar=5.1f/Math.max(Math.abs(ax),Math.abs(ay)),br=5.1f/Math.max(Math.abs(bx),Math.abs(by));
    face(p,o,l,new float[][]{{cx+ax*ar,cy+ay*ar,15},{cx+bx*br,cy+by*br,15},{cx+bx*2.6f,cy+by*2.6f,15},{cx+ax*2.6f,cy+ay*2.6f,15}},FRONT,0xffffff);
    face(p,o,l,new float[][]{{cx+ax*2.6f,cy+ay*2.6f,15},{cx+bx*2.6f,cy+by*2.6f,15},{cx+bx*2.4f,cy+by*2.4f,12},{cx+ax*2.4f,cy+ay*2.4f,12}},white,0x535943);
    face(p,o,l,new float[][]{{cx,cy,11.9f},{cx+ax*2.4f,cy+ay*2.4f,11.9f},{cx+bx*2.4f,cy+by*2.4f,11.9f},{cx,cy,11.9f}},white,0x101611);
   }
  }
  // Card plate remains separate from the front skin, with an open slit and an inset dark floor.
  for(float[] q:new float[][]{{112,462,82,33},{112,500,82,38},{112,495,12,5},{182,495,12,5}})rect(p,o,l,q[0],q[1],q[2],q[3],15.3f,FRONT);
  solid(p,o,l,"card_slot",124,495,58,5,8,1,0,0x172016,0);
  face(p,o,l,new float[][]{{124,495,15.3f},{182,495,15.3f},{182,496,9},{124,496,9}},white,0x7e836a);
  var style=keyStyle;float ri=style.rimInset,fi=style.faceInset,li=style.labelInset;
  for(var k:keys){
   keySolid(p,o,l,k.x,k.y,k.w,k.h,15,1.1f,style.socketRadius,Integer.parseInt(style.socketColor.substring(1),16),0);
   keySolid(p,o,l,k.x+ri,k.y+ri,k.w-2*ri,k.h-2*ri,16.1f,1,style.rimRadius,Integer.parseInt(style.rimColor.substring(1),16),0);
   keySolid(p,o,l,k.x+fi,k.y+fi,k.w-2*fi,k.h-2*fi,17.1f,2.3f,style.faceRadius,Integer.parseInt(style.faceColor.substring(1),16),.25f);
   rect(p,o,l,k.x+li,k.y+li,k.w-2*li,k.h-2*li,19.7f,INK);
  }
 }
 static void draw(PoseStack p,MultiBufferSource o,int light){GeometryCache.draw("ht512",p,o,light,Ht512Visual::mesh);}
 static void pose(Ht512.Unit b,PoseStack p){var offset=com.qwerty.homenet.move.DeviceOffsets.get(b.getLevel(),b.getBlockPos());p.translate(offset.x,offset.y,offset.z);double bottom=.5-Ht512.HEIGHT/2;switch(b.getBlockState().getValue(WallMountedBlock.FACING)){case NORTH->{p.translate(.5,bottom,1);p.mulPose(Axis.YP.rotationDegrees(180));}case SOUTH->p.translate(.5,bottom,0);case EAST->{p.translate(0,bottom,.5);p.mulPose(Axis.YP.rotationDegrees(90));}case WEST->{p.translate(1,bottom,.5);p.mulPose(Axis.YP.rotationDegrees(-90));}default->{}}p.scale(SCALE,SCALE,SCALE);p.translate(-W/2,H-700,0);}
 static class Renderer implements BlockEntityRenderer<Ht512.Unit>{public void render(Ht512.Unit b,float time,PoseStack p,MultiBufferSource out,int light,int overlay){if(DeviceVisibility.hidden(b,GeometryCache.bounds("ht512"),q->pose(b,q)))return;p.pushPose();pose(b,p);draw(p,out,light);p.popPose();}}
 static void open(){Minecraft.getInstance().setScreen(new ModelScreen());}
 static final class ModelScreen extends Screen {
  ModelScreen(){super(Component.literal("SAMSUNG HT-512"));}
  @Override public boolean isPauseScreen(){return false;}
  @Override public void render(GuiGraphics g,int mx,int my,float dt){renderBackground(g);g.drawString(font,title,12,8,0xffeeeeee);g.drawString(font,"BUS · 외형 모델 (기능은 추후 추가)",12,27,0xffe3dfb8);float s=Math.min((width-32)/W,(height-68)/H);g.pose().pushPose();g.pose().translate((width-W*s)/2,52,0);g.pose().scale(s,s,1);g.blit(FRONT,0,0,(int)W,(int)H,0,0,(int)W,(int)H,(int)W,(int)H);g.pose().popPose();super.render(g,mx,my,dt);}
 }
}
