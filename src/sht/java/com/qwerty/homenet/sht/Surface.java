package com.qwerty.homenet.sht;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import java.util.*;
/** Rounded bevels, a domed handset and an open CRT throat, with actual surface normals. */
public final class Surface {
 public static final float SCALE=.5f/824;
 public record Solid(String name,float x,float y,float w,float h,float z,float d,float r,String color,String group,float curve){}
 public record Key(String name,String label,float x,float y,float w,float h,String group){}
 static List<Solid> solids;static List<Key> buttons;
 static void load(){if(solids!=null)return;try(var r=Minecraft.getInstance().getResourceManager().openAsReader(Product.id("model.json"))){var o=JsonParser.parseReader(r).getAsJsonObject();Gson g=new Gson();solids=new ArrayList<>();buttons=new ArrayList<>();for(var e:o.getAsJsonArray("parts"))solids.add(g.fromJson(e,Solid.class));for(var e:o.getAsJsonArray("keys"))buttons.add(g.fromJson(e,Key.class));}catch(Exception e){throw new IllegalStateException(e);}}
 static void vertex(VertexConsumer b,PoseStack.Pose pose,float x,float y,float z,float nx,float ny,float nz,int col,int light,float u,float v){b.vertex(pose.pose(),x,700-y,z).color(col>>16&255,col>>8&255,col&255,255).uv(u,v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(),nx,-ny,nz).endVertex();}
 static float[] edge(Solid p,int n,float inset){if(p.name.length()==2&&(p.name.charAt(0)=='r'||p.name.charAt(0)=='n')){double t=2*Math.PI*(n%64)/64;float nx=(float)Math.cos(t),ny=(float)Math.sin(t);return new float[]{p.x+p.w/2+(p.w/2-inset)*nx,p.y+p.h/2+(p.h/2-inset)*ny,nx,ny};}float rad=Math.max(0,Math.min(p.r,Math.min(p.w,p.h)/2)-inset);int corner=(n%64)/16;double a=(corner*90+(n%16)*6)*Math.PI/180;float cx=p.x+(corner==0||corner==3?p.w-p.r:p.r),cy=p.y+(corner<2?p.h-p.r:p.r);float x=cx+rad*(float)Math.cos(a),y=cy+rad*(float)Math.sin(a);if(p.name.equals("handset_back")||p.name.equals("radio_face"))x=p.x+p.w/2+(x-p.x-p.w/2)*(.95f+.05f*Math.abs((y-p.y)/p.h*2-1));return new float[]{x,y,(float)Math.cos(a),(float)Math.sin(a)};}
 static void solid(Solid p,PoseStack ps,MultiBufferSource buffer,int light){var b=buffer.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));int col=Integer.parseInt(p.color.substring(1),16);float bevel=p.name.startsWith("face_")?0:Math.max(.03f,Math.min(p.d*.45f,Math.min(p.w,p.h)*.12f));if(p.name.equals("mount_plate"))bevel=3;var pose=ps.last();
  int step=p.w<15||p.h<8?4:2;
  for(int i=0;i<64;i+=step){
   for(int ring=0;ring<4;ring++){float a=(float)(Math.PI/2*Math.max(0,ring-1)/3),c=(float)(Math.PI/2*Math.max(0,ring)/3);for(int j=0;j<4;j++){boolean upper=j==1||j==2;float angle=upper?c:a;var at=edge(p,(i+(j>=2?step:0))%64,bevel*(1-(float)Math.cos(angle)));float z=ring==0&&!upper?p.z:p.z+p.d-bevel+bevel*(float)Math.sin(angle);vertex(b,pose,at[0],at[1],z,at[2]*(float)Math.cos(angle),at[3]*(float)Math.cos(angle),(float)Math.sin(angle),col,light,.5f,.5f);}}
   var a=edge(p,i,bevel);var c=edge(p,i+step,bevel);
   float cx=p.x+p.w/2,cy=p.y+p.h/2;
   int frontRings=p.curve>0?5:1;
   for(int ring=0;ring<frontRings;ring++)for(int j=0;j<4;j++){
    float radial=1-(ring+(j==1||j==2?1:0))/(float)frontRings;var e=j<2?a:c;
    float vx=cx+(e[0]-cx)*radial,vy=cy+(e[1]-cy)*radial,z=p.z+p.d+p.curve*(1-radial*radial);
    float nx=p.curve*2*(vx-cx)/(p.w*p.w/4),ny=p.curve*2*(vy-cy)/(p.h*p.h/4),nz=1;
    float len=(float)Math.sqrt(nx*nx+ny*ny+1);vertex(b,pose,vx,vy,z,nx/len,ny/len,nz/len,col,light,.5f,.5f);
   }
  }
 }
 public static void rectangle(PoseStack p,MultiBufferSource out,float x,float y,float w,float h,float z,ResourceLocation tex,int light){var b=out.getBuffer(RenderType.entityTranslucent(tex));for(float[] v:new float[][]{{x,y+h,0,1},{x+w,y+h,1,1},{x+w,y,1,0},{x,y,0,0}})vertex(b,p.last(),v[0],v[1],z,0,0,1,0xffffff,light,v[2],v[3]);}
 static void patch(PoseStack p,MultiBufferSource out,float x,float y,float w,float h,float z,int color,int light){var b=out.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));for(float[] v:new float[][]{{x,y+h},{x+w,y+h},{x+w,y},{x,y}})vertex(b,p.last(),v[0],v[1],z,0,0,1,color,light,.5f,.5f);}
 static void rim(PoseStack p,MultiBufferSource out,int light,Solid outer,Solid inner,int color){
  var b=out.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));
  for(int i=0;i<64;i++)for(int j=0;j<4;j++){
   boolean inside=j>=2;int index=(i+(j==1||j==2?1:0))%64;var edge=edge(inside?inner:outer,index,0);
   vertex(b,p.last(),edge[0],edge[1],inside?inner.z:outer.z,-edge[2]*.4f,-edge[3]*.4f,.9165f,color,light,.5f,.5f);
  }
 }
 static void throat(PoseStack p,MultiBufferSource out,int light){
  var outer=new Solid("frame",548,129,248,216,71,1,8,"#182324","base",0);
  var opening=new Solid("opening",566,154,212,174,57,1,2,"#182324","base",0);
  var glass=new Solid("glass",578,174,192,144,54,1,3,"#182324","base",0);
  rim(p,out,light,outer,opening,0x182324);rim(p,out,light,opening,glass,0x111a1b);
  for(int yy:new int[]{233,265})rim(p,out,light,new Solid("track",838,yy-5,48,14,68,1,0,"","",0),new Solid("track",840,yy-3,44,10,60,1,1,"","",0),0xd8d8c3);
 }
 static float bowl(float y){return 60-13*(float)Math.exp(-Math.pow((y-204)/65,4))-13*(float)Math.exp(-Math.pow((y-555)/55,4));}
 static float left(float y){float dy=y<172?y-172:y>580?y-580:0;return 176+30-(float)Math.sqrt(Math.max(0,900-dy*dy));}
 static void cradle(PoseStack p,MultiBufferSource out,int light){
  var b=out.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));
  Solid outer=new Solid("cradle",170,136,116,480,67,1,0,"","",0),inner=new Solid("cradle",176,142,104,468,60,1,30,"","",0);
  for(int i=0;i<64;i++)for(int j=0;j<4;j++){
   boolean inside=j>=2;var at=edge(inside?inner:outer,(i+(j==1||j==2?1:0))%64,0);
   vertex(b,p.last(),at[0],at[1],inside?bowl(at[1])+2:67,-at[2]*.7f,-at[3]*.7f,.714f,0xe7e5d4,light,.5f,.5f);
  }
  for(int row=0;row<78;row++)for(int col=0;col<10;col++)for(int j=0;j<4;j++){
   float y=142+(row+(j==1||j==2?1:0))*6f,u=(col+(j>=2?1:0))/10f;
   float l=left(y),x=l+(456-2*l)*u,z=bowl(y)+2*(2*u-1)*(2*u-1);
   float nx=-8*(2*u-1)/104,ny=-(bowl(y+.25f)-bowl(y-.25f))*2;
   float len=(float)Math.sqrt(1+nx*nx+ny*ny);vertex(b,p.last(),x,y,z,nx/len,ny/len,1/len,0xe3e1d0,light,.5f,.5f);
  }
 }
 static void vents(PoseStack p,MultiBufferSource out,int light){
  var b=out.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));
  for(var v:solids)if(v.group.equals("underside"))for(int i=0;i<20;i++){
   double a=i*Math.PI/10,c=(i+1)*Math.PI/10;
   for(float[] q:new float[][]{{v.x+2.5f,v.z},{v.x+2.5f+2.5f*(float)Math.cos(a),v.z+2.5f*(float)Math.sin(a)},{v.x+2.5f+2.5f*(float)Math.cos(c),v.z+2.5f*(float)Math.sin(c)},{v.x+2.5f,v.z}})vertex(b,p.last(),q[0],671.04f,q[1],0,1,0,0x4c5449,light,.5f,.5f);
  }
 }
 static void draw(PoseStack p,MultiBufferSource out,int light,boolean radio,boolean docked,boolean folded){
  load();for(var s:solids){
   if(radio?!s.group.equals("radio"):s.group.equals("radio")||s.group.equals("docked")&&!docked||s.group.equals("folded")&&!folded||s.group.equals("extended")&&folded)continue;
   if(!radio&&(s.name.equals("crt_outer")||s.name.equals("cradle_void")||s.group.equals("underside")))continue;
   solid(s,p,out,light);
  }
  if(!radio){throat(p,out,light);cradle(p,out,light);vents(p,out,light);}
  rectangle(p,out,0,0,1042,radio?950:702,radio?73:71.8f,Product.id(radio?"textures/radio_ink.png":"textures/base_ink.png"),light);
 }
}
