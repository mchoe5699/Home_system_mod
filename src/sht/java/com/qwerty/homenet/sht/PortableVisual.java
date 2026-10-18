package com.qwerty.homenet.sht;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import java.util.*;
/** Millimetre geometry. Stand bounds remain 400 x 400 in every horizontal orientation. */
final class PortableVisual {
 static List<Surface.Solid> parts;static List<Surface.Key> keys;
 static void load(){if(parts!=null)return;try(var r=Minecraft.getInstance().getResourceManager().openAsReader(Product.id("portable102/model.json"))){var m=JsonParser.parseReader(r).getAsJsonObject();var g=new Gson();parts=new ArrayList<>();keys=new ArrayList<>();for(var p:m.getAsJsonArray("parts"))parts.add(g.fromJson(p,Surface.Solid.class));for(var k:m.getAsJsonArray("keys"))keys.add(g.fromJson(k,Surface.Key.class));}catch(Exception e){throw new IllegalStateException(e);}}
 static void handset(PoseStack p,MultiBufferSource out,int light,boolean active){load();for(var s:parts)Surface.solid(s,p,out,light);Surface.rectangle(p,out,0,0,240,950,73.8f,Product.id("portable102/textures/radio_ink.png"),light);if(active)Surface.patch(p,out,76,425,8,3,75,0xa0ef7a,15728880);}
 static void vertex(VertexConsumer b,PoseStack.Pose p,float x,float y,float z,int rgb,int light,float nx,float ny,float nz){b.vertex(p.pose(),x,y,z).color(rgb>>16&255,rgb>>8&255,rgb&255,255).uv(.5f,.5f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.normal(),nx,ny,nz).endVertex();}
 static void stand(PoseStack p,MultiBufferSource out,int light,boolean inserted,boolean charging,boolean active){
  var b=out.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));var pose=p.last();
  // Rounded footprint, continuously sloping top, low front and raised back.
  for(int i=0;i<64;i++){
   float[] a=edge(i),c=edge(i+1);float ha=top(a[1]),hc=top(c[1]);float nx=(a[0]+c[0])/2-200,nz=(a[1]+c[1])/2-200,len=(float)Math.sqrt(nx*nx+nz*nz);nx/=len;nz/=len;
   vertex(b,pose,a[0],0,a[1],0xdedfcf,light,nx,0,nz);vertex(b,pose,c[0],0,c[1],0xdedfcf,light,nx,0,nz);vertex(b,pose,c[0],hc,c[1],0xe8e7d7,light,nx,0,nz);vertex(b,pose,a[0],ha,a[1],0xe8e7d7,light,nx,0,nz);
  }
  deck(p,out,light);pocket(p,out,light,165,270,75,48);pocket(p,out,light,305,105,43,47);
  // Charging indicator on the front wall; no cable is modeled.
  var led=new Surface.Solid("charging_led",278,650,35,8,398,2,4,charging?"#83dd6e":"#6e7866","",0);Surface.solid(led,p,out,charging?15728880:light);
  if(inserted){p.pushPose();p.translate(103,108,280);p.mulPose(Axis.XP.rotationDegrees(-10));p.scale(.9f,.9f,.9f);p.translate(0,200,0);handset(p,out,light,active);p.popPose();}
 }
 static float[] edge(int n){int corner=(n%64)/16;double angle=(corner*90+(n%16)*6)*Math.PI/180;float x=corner==0||corner==3?345:55,z=corner<2?345:55;return new float[]{x+55*(float)Math.cos(angle),z+55*(float)Math.sin(angle)};}
 static float top(float depth){return 140-.15f*depth;}
 static float outline(float z){float d=z<55?55-z:z>345?z-345:0;return 55-(float)Math.sqrt(Math.max(0,55*55-d*d));}
 static void deck(PoseStack p,MultiBufferSource out,int light){var rows=new TreeSet<Float>();for(int i=0;i<=100;i++)rows.add(i*4f);for(float v:new float[]{58,152,222,318})rows.add(v);Float last=null;for(float z:rows){if(last!=null){float mid=(last+z)/2;float[] hole=mid>222&&mid<318?new float[]{165,270,75,48}:mid>58&&mid<152?new float[]{305,105,43,47}:null;if(hole==null)strip(p,out,light,last,z,outline(last),400-outline(last),outline(z),400-outline(z));else{float a=holeEdge(hole,last),c=holeEdge(hole,z);strip(p,out,light,last,z,outline(last),hole[0]-a,outline(z),hole[0]-c);strip(p,out,light,last,z,hole[0]+a,400-outline(last),hole[0]+c,400-outline(z));}}last=z;}}
 static float holeEdge(float[] h,float z){return h[2]*(float)Math.sqrt(Math.max(0,1-Math.pow((z-h[1])/h[3],2)));}
 static void strip(PoseStack p,MultiBufferSource out,int light,float a,float c,float l0,float r0,float l1,float r1){var b=out.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));var q=p.last();vertex(b,q,l0,top(a),a,0xf1efdf,light,0,1,.15f);vertex(b,q,r0,top(a),a,0xf1efdf,light,0,1,.15f);vertex(b,q,r1,top(c),c,0xf1efdf,light,0,1,.15f);vertex(b,q,l1,top(c),c,0xf1efdf,light,0,1,.15f);}
 static void pocket(PoseStack p,MultiBufferSource out,int light,float x,float z,float rx,float rz){var b=out.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));var pose=p.last();for(int i=0;i<48;i++){double a=i*Math.PI/24,c=(i+1)*Math.PI/24;for(int ring=0;ring<4;ring++)for(int j=0;j<4;j++){float radius=1-(ring+(j==1||j==2?1:0))/4f;double angle=j<2?a:c;float xx=x+rx*radius*(float)Math.cos(angle),zz=z+rz*radius*(float)Math.sin(angle);vertex(b,pose,xx,top(zz)-25*(1-radius*radius),zz,0xb5bba8,light,0,1,0);}}}
 static final class Renderer implements BlockEntityRenderer<Charger.Dock>{
  public void render(Charger.Dock d,float time,PoseStack p,MultiBufferSource out,int light,int overlay){p.pushPose();p.translate(.5,0,.5);p.mulPose(Axis.YP.rotationDegrees(switch(d.getBlockState().getValue(Charger.FACING)){case NORTH->180;case EAST->90;case WEST->270;default->0;}));p.translate(-.2,0,-.2);p.scale(.001f,.001f,.001f);stand(p,out,light,!d.stack.isEmpty(),!d.stack.isEmpty()&&d.stack.getOrCreateTag().getInt("battery")<Portable.CAPACITY,!d.stack.isEmpty()&&d.stack.getOrCreateTag().getBoolean("ringing"));p.popPose();}
 }
}
