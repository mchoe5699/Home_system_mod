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
 public record Join(String name,float x0,float y0,float x1,float y1,float width,float depth,int color){}
 public record Shell(float[] bounds,float back,float front,float radius,float[] sideRoll,Join[] seams,float[][] holes){}
 public record Mount(float frame,float back,float front,float depth){}
 static Mount mounting;static Shell casing;static List<Solid> solids;static List<Key> buttons;
 record Model(Mount mount,Shell shell,List<Solid> solids,List<Key> buttons){}
 static final Map<Boolean,Model> models=new HashMap<>();static boolean wiredModel;
 static ResourceLocation texture(String path){return Product.id((wiredModel?"sht102/":"")+path);}
 static void load(){if(solids==null)select(false);}
 static void select(boolean wired){
  if(solids!=null&&wiredModel==wired)return;
  var model=models.get(wired);
  if(model==null)try(var r=Minecraft.getInstance().getResourceManager().openAsReader(Product.id((wired?"sht102/":"")+"model.json"))){
   var o=JsonParser.parseReader(r).getAsJsonObject();Gson g=new Gson();var ss=new ArrayList<Solid>();var bb=new ArrayList<Key>();for(var e:o.getAsJsonArray("parts"))ss.add(g.fromJson(e,Solid.class));for(var e:o.getAsJsonArray("keys"))bb.add(g.fromJson(e,Key.class));
   model=new Model(g.fromJson(o.get("mounting"),Mount.class),g.fromJson(o.get("shell"),Shell.class),ss,bb);models.put(wired,model);
  }catch(Exception e){throw new IllegalStateException(e);}
  wiredModel=wired;mounting=model.mount;casing=model.shell;solids=model.solids;buttons=model.buttons;
 }
 static void vertex(VertexConsumer b,PoseStack.Pose pose,float x,float y,float z,float nx,float ny,float nz,int col,int light,float u,float v){b.vertex(pose.pose(),x,700-y,z).color(col>>16&255,col>>8&255,col&255,255).uv(u,v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(),nx,-ny,nz).endVertex();}
 static float[] edge(Solid p,int n,float inset){if(p.name.equals("portable_case")){double t=2*Math.PI*(n%64)/64;float nx=(float)Math.cos(t),ny=-(float)Math.sin(t),x=p.x+p.w/2+(p.w/2-inset)*nx,y=n%64<=32?p.y+p.w/2+(p.w/2-inset)*ny:p.y+p.h-16+(16-inset)*ny;float taper=1-.055f*(float)Math.sin(Math.PI*Math.max(0,Math.min(1,(y-p.y)/p.h)));x=p.x+p.w/2+(x-p.x-p.w/2)*taper;return new float[]{x,y,nx,ny};}if(p.name.length()==2&&(p.name.charAt(0)=='r'||p.name.charAt(0)=='n')){double t=2*Math.PI*(n%64)/64;float nx=(float)Math.cos(t),ny=(float)Math.sin(t);return new float[]{p.x+p.w/2+(p.w/2-inset)*nx,p.y+p.h/2+(p.h/2-inset)*ny,nx,ny};}float rad=Math.max(0,Math.min(p.r,Math.min(p.w,p.h)/2)-inset);int corner=(n%64)/16;double a=(corner*90+(n%16)*6)*Math.PI/180;float cx=p.x+(corner==0||corner==3?p.w-p.r:p.r),cy=p.y+(corner<2?p.h-p.r:p.r);float x=cx+rad*(float)Math.cos(a),y=cy+rad*(float)Math.sin(a);if(p.name.equals("handset_back")||p.name.equals("radio_face"))x=p.x+p.w/2+(x-p.x-p.w/2)*(.95f+.05f*Math.abs((y-p.y)/p.h*2-1));return new float[]{x,y,(float)Math.cos(a),(float)Math.sin(a)};}
 static void solid(Solid p,PoseStack ps,MultiBufferSource buffer,int light){var b=buffer.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));int col=Integer.parseInt(p.color.substring(1),16);float bevel=p.name.startsWith("face_")?0:Math.max(.03f,Math.min(p.d*.45f,Math.min(p.w,p.h)*.12f));if(p.name.equals("mount_plate"))bevel=Math.min(.12f,p.d*.45f);var pose=ps.last();
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
 static void indicator(PoseStack p,MultiBufferSource out,Indicators.Lamp lamp){
  var b=out.getBuffer(RenderType.entityTranslucentEmissive(Product.id("textures/solid.png")));
  float z=lamp.round()?71.4f:72.2f;
  if(lamp.round()){
   float cx=lamp.x()+lamp.w()/2f,cy=lamp.y()+lamp.h()/2f;
   for(int i=0;i<32;i++){
    double a=-i*Math.PI/16,c=-(i+1)*Math.PI/16;
    for(float[] v:new float[][]{{cx,cy},{cx+(float)Math.cos(a)*lamp.w()/2,cy+(float)Math.sin(a)*lamp.h()/2},{cx+(float)Math.cos(c)*lamp.w()/2,cy+(float)Math.sin(c)*lamp.h()/2},{cx,cy}})vertex(b,p.last(),v[0],v[1],z,0,0,1,lamp.rgb(),15728880,.5f,.5f);
   }
  }else for(float[] v:new float[][]{{lamp.x(),lamp.y()+lamp.h()},{lamp.x()+lamp.w(),lamp.y()+lamp.h()},{lamp.x()+lamp.w(),lamp.y()},{lamp.x(),lamp.y()}})vertex(b,p.last(),v[0],v[1],z,0,0,1,lamp.rgb(),15728880,.5f,.5f);
 }
 static void patch(PoseStack p,MultiBufferSource out,float x,float y,float w,float h,float z,int color,int light){var b=out.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));for(float[] v:new float[][]{{x,y+h},{x+w,y+h},{x+w,y},{x,y}})vertex(b,p.last(),v[0],v[1],z,0,0,1,color,light,.5f,.5f);}
 static void rim(PoseStack p,MultiBufferSource out,int light,Solid outer,Solid inner,int color){
  var b=out.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));
  for(int i=0;i<64;i++)for(int j=0;j<4;j++){
   boolean inside=j>=2;int index=(i+(j==1||j==2?1:0))%64;var edge=edge(inside?inner:outer,index,0);
   vertex(b,p.last(),edge[0],edge[1],inside?inner.z:outer.z,-edge[2]*.4f,-edge[3]*.4f,.9165f,color,light,.5f,.5f);
  }
 }
 static void throat(PoseStack p,MultiBufferSource out,int light){
  var outer=new Solid("frame",548,129,248,216,70,1,8,"#182324","base",0);
  var opening=new Solid("opening",566,154,212,174,66,1,2,"#182324","base",0);
  var glass=new Solid("glass",578,174,192,144,65,1,3,"#182324","base",0);
  rim(p,out,light,outer,opening,0x182324);rim(p,out,light,opening,glass,0x111a1b);
  for(int yy:new int[]{233,265})rim(p,out,light,new Solid("track",838,yy-5,48,14,68,1,0,"","",0),new Solid("track",840,yy-3,44,10,60,1,1,"","",0),0xd8d8c3);
 }
 static float bowl(float y){return 60-13*(float)Math.exp(-Math.pow((y-204)/65,4))-13*(float)Math.exp(-Math.pow((y-555)/55,4));}
 static float left(float y){float dy=y<172?y-172:y>580?y-580:0;return 176+30-(float)Math.sqrt(Math.max(0,900-dy*dy));}
 static void cradle(PoseStack p,MultiBufferSource out,int light){
  if(wiredModel){
   rim(p,out,light,new Solid("wiredCradle",160,136,114,480,68,1,5,"","",0),new Solid("wiredCradle",166,142,102,468,53,1,8,"","",0),0xe7e5d4);
   solid(new Solid("wiredCradleFloor",166,142,102,468,52.7f,.3f,8,"#dedfce","base",0),p,out,light);return;
  }
  var b=out.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));
  Solid outer=new Solid("cradle",170,136,116,480,68,1,0,"","",0),inner=new Solid("cradle",176,142,104,468,60,1,30,"","",0);
  for(int i=0;i<64;i++)for(int j=0;j<4;j++){
   boolean inside=j>=2;var at=edge(inside?inner:outer,(i+(j==1||j==2?1:0))%64,0);
   vertex(b,p.last(),at[0],at[1],inside?bowl(at[1])+2:68,-at[2]*.7f,-at[3]*.7f,.714f,0xe7e5d4,light,.5f,.5f);
  }
  for(int row=0;row<78;row++)for(int col=0;col<10;col++)for(int j=0;j<4;j++){
   float y=142+(row+(j==1||j==2?1:0))*6f,u=(col+(j>=2?1:0))/10f;
   float l=left(y),x=l+(456-2*l)*u,z=bowl(y)+2*(2*u-1)*(2*u-1);
   float nx=-8*(2*u-1)/104,ny=-(bowl(y+.25f)-bowl(y-.25f))*2;
   float len=(float)Math.sqrt(1+nx*nx+ny*ny);vertex(b,p.last(),x,y,z,nx/len,ny/len,1/len,0xe3e1d0,light,.5f,.5f);
  }
 }
 // A single continuous shell: no independent top/bottom bars or overlapping panel boxes.
 static float joinWeight(Join j,float x,float y){
  float dx=j.x1-j.x0,dy=j.y1-j.y0,t=Math.max(0,Math.min(1,((x-j.x0)*dx+(y-j.y0)*dy)/(dx*dx+dy*dy)));
  float u=x-j.x0-t*dx,v=y-j.y0-t*dy;return Math.max(0,1-(float)Math.sqrt(u*u+v*v)/(j.width/2));
 }
 static float seamDrop(float x,float y){float depth=0;for(var j:casing.seams)depth=Math.max(depth,j.depth*joinWeight(j,x,y));return depth;}
 static int seamColor(float x,float y){float strength=0;int color=0xf1efdf;for(var j:casing.seams){float w=joinWeight(j,x,y);if(w>=.5f&&j.depth*w>strength){strength=j.depth*w;color=j.color;}}return color;}
 static float shellZ(float x,float y){
  float[] a=casing.bounds;float fromEdge=Math.min(y-a[1],a[3]-y);float t=Math.max(0,Math.min(1,fromEdge/8));
  float z=casing.front-4*(1-t)*(1-t);
  float width=x<(a[0]+a[2])/2?casing.sideRoll[0]:casing.sideRoll[1];
  float edge=Math.min(x-a[0],a[2]-x);float u=Math.max(0,Math.min(1,1-edge/width));
  z-=casing.sideRoll[2]*(1-(float)Math.sqrt(Math.max(0,1-u*u)))*t;
  z-=seamDrop(x,y);
  return z;
 }
 static float shellLeft(float y){float[] a=casing.bounds;float r=casing.radius,dy=y<a[1]+r?y-a[1]-r:y>a[3]-r?y-a[3]+r:0;return a[0]+r-(float)Math.sqrt(Math.max(0,r*r-dy*dy));}
 static void skinVertex(VertexConsumer b,PoseStack p,float x,float y,int light){float nx=-(shellZ(x+.2f,y)-shellZ(x-.2f,y))*2.5f,ny=-(shellZ(x,y+.2f)-shellZ(x,y-.2f))*2.5f;float n=(float)Math.sqrt(1+nx*nx+ny*ny);vertex(b,p.last(),x,y,shellZ(x,y),nx/n,ny/n,1/n,seamColor(x,y),light,.5f,.5f);}
 static void shell(PoseStack p,MultiBufferSource out,int light){
  var b=out.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));
  var xx=new TreeSet<Float>();var yy=new TreeSet<Float>();float[] a=casing.bounds;
  for(float x=a[0];x<a[2];x+=8)xx.add(x);for(float y=a[1];y<a[3];y+=8)yy.add(y);
  for(float x=110;x<=154;x+=2)xx.add(x);for(float x=891;x<=934;x+=2)xx.add(x);
  xx.add(a[2]);xx.add(517.35f);xx.add(518f);xx.add(518.65f);yy.add(a[3]);yy.add(110f);yy.add(646f);for(float y:new float[]{91,93,95,97,101,660,664,668,670})yy.add(y);
  for(float[] h:casing.holes){xx.add(h[0]);xx.add(h[0]+h[2]);yy.add(h[1]);yy.add(h[1]+h[3]);}
  for(var seam:casing.seams)for(float factor:new float[]{-.5f,-.25f,0,.25f,.5f}){
   float d=seam.width*factor;
   if(seam.x0==seam.x1)xx.add(seam.x0+d);else yy.add(seam.y0+d);
  }
  var xs=new ArrayList<>(xx);var ys=new ArrayList<>(yy);
  for(int row=0;row<ys.size()-1;row++){
   float y0=ys.get(row),y1=ys.get(row+1),l0=shellLeft(y0),l1=shellLeft(y1),r0=a[0]+a[2]-l0,r1=a[0]+a[2]-l1;
   for(int col=0;col<xs.size()-1;col++){
    float x0=xs.get(col),x1=xs.get(col+1),cx=(x0+x1)/2,cy=(y0+y1)/2;boolean hole=false;
    for(float[] h:casing.holes)if(cx>h[0]&&cx<h[0]+h[2]&&cy>h[1]&&cy<h[1]+h[3])hole=true;
    if(hole||x1<=Math.min(l0,l1)||x0>=Math.max(r0,r1))continue;
    for(float[] v:new float[][]{{Math.max(l0,Math.min(r0,x0)),y0},{Math.max(l1,Math.min(r1,x0)),y1},{Math.max(l1,Math.min(r1,x1)),y1},{Math.max(l0,Math.min(r0,x1)),y0}})skinVertex(b,p,v[0],v[1],light);
   }
   // Side walls share their front boundary vertices with this very same row.
   for(boolean right:new boolean[]{false,true}){
    float x0=right?r0:l0,x1=right?r1:l1;float nx=right?1:-1,ny=(right?-1:1)*(x1-x0)/Math.max(.001f,y1-y0),len=(float)Math.sqrt(1+ny*ny);
    for(float[] v:new float[][]{{x0,y0,shellZ(x0,y0)},{x1,y1,shellZ(x1,y1)},{x1,y1,casing.back},{x0,y0,casing.back}})vertex(b,p.last(),v[0],v[1],v[2],nx/len,ny/len,0,0xf1efdf,light,.5f,.5f);
   }
  }
  for(boolean bottom:new boolean[]{false,true}){float y=bottom?a[3]:a[1],l=shellLeft(y),r=a[0]+a[2]-l;
   for(int i=0;i<xs.size()-1;i++){float x0=Math.max(l,Math.min(r,xs.get(i))),x1=Math.max(l,Math.min(r,xs.get(i+1)));if(x1<=x0)continue;
    for(float[] v:new float[][]{{x0,y,shellZ(x0,y)},{x1,y,shellZ(x1,y)},{x1,y,casing.back},{x0,y,casing.back}})vertex(b,p.last(),v[0],v[1],v[2],0,bottom?1:-1,0,0xf1efdf,light,.5f,.5f);
   }
  }
 }
 static void vents(PoseStack p,MultiBufferSource out,int light){
  var b=out.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));
  for(var v:solids)if(v.group.equals("underside"))for(int i=0;i<20;i++){
   double a=i*Math.PI/10,c=(i+1)*Math.PI/10;
   for(float[] q:new float[][]{{v.x+2.5f,v.z},{v.x+2.5f+2.5f*(float)Math.cos(a),v.z+2.5f*(float)Math.sin(a)},{v.x+2.5f+2.5f*(float)Math.cos(c),v.z+2.5f*(float)Math.sin(c)},{v.x+2.5f,v.z}})vertex(b,p.last(),q[0],672.04f,q[1],0,1,0,0x4c5449,light,.5f,.5f);
  }
 }
 static float bodyDepth(float z){load();return mounting.frame+(z-mounting.back)*mounting.depth/(mounting.front-mounting.back);}
 static float handsetShift(){return bodyDepth(mounting.front)-mounting.front;}
 static void bodyPose(PoseStack p){load();p.pushPose();p.translate(0,0,mounting.frame);p.scale(1,1,mounting.depth/(mounting.front-mounting.back));p.translate(0,0,-mounting.back);}
 static void draw(PoseStack p,MultiBufferSource out,int light,boolean radio,boolean docked,boolean folded){
  load();GeometryCache.draw("station/"+wiredModel+"/"+radio+"/"+docked+"/"+folded,p,out,light,(pose,buffers)->drawRaw(pose,buffers,0,radio,docked,folded));
 }
 static void drawRaw(PoseStack p,MultiBufferSource out,int light,boolean radio,boolean docked,boolean folded){
  load();
  if(radio){for(var s:solids)if(s.group.equals("radio"))solid(s,p,out,light);rectangle(p,out,0,0,1042,950,73,Product.id("textures/radio_ink.png"),light);return;}
  // The sheet stays on the wall. Every base feature moves together and becomes 14% thinner.
  for(var s:solids)if(s.name.equals("mount_plate"))solid(s,p,out,light);
  bodyPose(p);
  for(var s:solids){
   if(s.group.equals("radio")||s.group.equals("docked")||s.group.equals("folded")&&!folded||s.group.equals("extended")&&folded)continue;
   if(s.name.equals("mount_plate")||s.name.equals("body_shell")||s.name.equals("crt_outer")||s.name.equals("cradle_void")||s.group.equals("underside"))continue;
   solid(s,p,out,light);
  }
  shell(p,out,light);throat(p,out,light);cradle(p,out,light);vents(p,out,light);
  rectangle(p,out,0,0,1042,720,71.8f,texture("textures/base_ink.png"),light);
  p.popPose();
  // Keep handset proportions and antenna cross-sections intact; translate the docked assembly rigidly.
  if(docked||wiredModel){p.pushPose();p.translate(wiredModel&&!docked?-65:0,wiredModel&&!docked?5:0,handsetShift()+(wiredModel&&!docked?15:0));for(var s:solids)if(s.group.equals("docked"))solid(s,p,out,light);p.popPose();}
 }
}
