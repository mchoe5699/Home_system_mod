package com.qwerty.homenet.samsung;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import java.util.*;
/** Continuous rounded surfaces with analytic normals, not layered cuboid contour lines. */
public final class ShtMesh {
 public record Part(String name,float x,float y,float w,float h,float z,float d,float r,String color,String group,float bevel){}
 public record Key(String name,String label,float x,float y,float w,float h){}
 private static List<Part> parts;private static List<Key> keys;
 public static final float SCALE=.5f/930f;
 public static List<Part> parts(){load();return parts;}public static List<Key> keys(){load();return keys;}
 private static void load(){if(parts!=null)return;try(var reader=Minecraft.getInstance().getResourceManager().openAsReader(Samsung.id("geometry.json"))){var root=JsonParser.parseReader(reader).getAsJsonObject();var gson=new Gson();parts=new ArrayList<>();keys=new ArrayList<>();for(var e:root.getAsJsonArray("parts"))parts.add(gson.fromJson(e,Part.class));for(var e:root.getAsJsonArray("keys"))keys.add(gson.fromJson(e,Key.class));}catch(Exception e){throw new IllegalStateException("SHT-101C geometry",e);}}
 private static void vertex(VertexConsumer out,PoseStack.Pose pose,float x,float y,float z,float nx,float ny,float nz,int color,int light,float u,float v){out.vertex(pose.pose(),x,740-y,z).color((color>>16)&255,(color>>8)&255,color&255,255).uv(u,v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(),nx,-ny,nz).endVertex();}
 public static void quad(PoseStack stack,MultiBufferSource buffers,float x,float y,float w,float h,float z,net.minecraft.resources.ResourceLocation tex,int light){var out=buffers.getBuffer(RenderType.entityTranslucent(tex));int c=0xffffff;vertex(out,stack.last(),x,y+h,z,0,0,1,c,light,0,1);vertex(out,stack.last(),x+w,y+h,z,0,0,1,c,light,1,1);vertex(out,stack.last(),x+w,y,z,0,0,1,c,light,1,0);vertex(out,stack.last(),x,y,z,0,0,1,c,light,0,0);}
 public static void tint(PoseStack stack,MultiBufferSource out,float x,float y,float w,float h,float z,int color,int light){var b=out.getBuffer(RenderType.entityCutoutNoCull(Samsung.id("textures/white.png")));for(float[] v:new float[][]{{x,y+h},{x+w,y+h},{x+w,y},{x,y}})vertex(b,stack.last(),v[0],v[1],z,0,0,1,color,light,.5f,.5f);}
 private static float[] perimeter(Part p,int index,float inset){int corner=index/8;float a=(float)((index%8)/7.0*Math.PI/2+corner*Math.PI/2);float r=Math.max(.1f,p.r-inset),cx=p.x+(corner==0||corner==3?p.w-p.r:p.r),cy=p.y+(corner<2?p.h-p.r:p.r);return new float[]{cx+(float)Math.cos(a)*r,cy+(float)Math.sin(a)*r,(float)Math.cos(a),(float)Math.sin(a)};}
 private static void box(Part p,PoseStack stack,MultiBufferSource buffers,int light){var out=buffers.getBuffer(RenderType.entityCutoutNoCull(Samsung.id("textures/white.png")));int c=Integer.parseInt(p.color.substring(1),16);float b=Math.min(p.bevel,Math.min(p.d/2,Math.min(p.w,p.h)/3));b=Math.max(.05f,b);var pose=stack.last();
  // Flat front fan uses pairs of perimeter points; duplicated centre makes a valid four-vertex triangle.
  for(int i=0;i<32;i++){var a=perimeter(p,i,b);var n=perimeter(p,(i+1)%32,b);vertex(out,pose,p.x+p.w/2,p.y+p.h/2,p.z+p.d,0,0,1,c,light,.5f,.5f);vertex(out,pose,a[0],a[1],p.z+p.d,0,0,1,c,light,.5f,.5f);vertex(out,pose,n[0],n[1],p.z+p.d,0,0,1,c,light,.5f,.5f);vertex(out,pose,p.x+p.w/2,p.y+p.h/2,p.z+p.d,0,0,1,c,light,.5f,.5f);}
  for(int ring=0;ring<5;ring++){float a=ring*(float)Math.PI/8,an=(ring+1)*(float)Math.PI/8;for(int i=0;i<32;i++){int[] indices={i,i,(i+1)%32,(i+1)%32};float[] angles={a,an,an,a};for(int j=0;j<4;j++){float ang=Math.min(angles[j],(float)Math.PI/2);float expansion=b*(float)Math.sin(ang);var at=perimeter(p,indices[j],b-expansion);float z=p.z+p.d-b+b*(float)Math.cos(ang);if(ring==4)z=j==0||j==3?p.z+p.d-b:p.z;vertex(out,pose,at[0],at[1],z,at[2]*(float)Math.sin(ang),at[3]*(float)Math.sin(ang),(float)Math.cos(ang),c,light,.5f,.5f);}}}
 }
 public static void draw(PoseStack stack,MultiBufferSource buffers,int light,boolean docked,boolean folded,boolean cordless){
  if(cordless){Part body=new Part("cordless",0,0,240,680,0,75,45,"#f4f1df","",10);box(body,stack,buffers,light);
  box(new Part("black_panel",43,190,154,61,75,1,12,"#253b34","",.5f),stack,buffers,light);
  box(new Part("talk",75,210,90,17,76,4,8,"#e1e2d5","",1),stack,buffers,light);
  for(int row=0;row<4;row++)for(int col=0;col<3;col++)box(new Part("dial",50+col*55,275+row*48,30,30,75,5,15,"#294637","",1.5f),stack,buffers,light);
  for(int row=0;row<2;row++)for(int col=0;col<3;col++)box(new Part("function",42+col*55,483+row*32,46,14,75,4,6,"#d8dacd","",1),stack,buffers,light);
  var ear=buffers.getBuffer(RenderType.entityCutoutNoCull(Samsung.id("textures/white.png")));
  for(int i=0;i<40;i++){float a=i*(float)Math.PI/20,b=(i+1)*(float)Math.PI/20;for(float[] v:new float[][]{{120+60*(float)Math.cos(a),90+60*(float)Math.sin(a),82},{120+60*(float)Math.cos(b),90+60*(float)Math.sin(b),82},{120+43*(float)Math.cos(b),90+43*(float)Math.sin(b),76},{120+43*(float)Math.cos(a),90+43*(float)Math.sin(a),76}})vertex(ear,stack.last(),v[0],v[1],v[2],0,0,1,0xcfd2c2,light,.5f,.5f);}
  for(int i=0;i<8;i++){float a=i*(float)Math.PI/4;box(new Part("ear_hole",118+22*(float)Math.cos(a),88+22*(float)Math.sin(a),4,4,75,1,2,"#77816c","",.2f),stack,buffers,light);}
  quad(stack,buffers,0,0,240,680,81,Samsung.id("textures/handset_labels.png"),light);
stack.pushPose();stack.translate(210,740-20,35);box(new Part("antenna",0,-210,6,230,0,6,3,"#59644f","",1),stack,buffers,light);stack.popPose();return;}
  for(var p:parts()){if(p.group.equals("docked")&&!docked||p.group.equals("folded")&&!folded||p.group.equals("upright")&&folded)continue;if(p.group.equals("radio_antenna")){if(!docked)continue;stack.pushPose();stack.translate(124,740-115,0);stack.translate(-124,-(740-115),0);box(p,stack,buffers,light);stack.popPose();}else box(p,stack,buffers,light);}
  // Four sloping black faces form an actual opening instead of a printed recess.
  var out=buffers.getBuffer(RenderType.entityCutoutNoCull(Samsung.id("textures/white.png")));
  float[][] outer={{565,112,57},{815,112,57},{815,330,57},{565,330,57}},inner={{578,124,43},{802,124,43},{802,317,43},{578,317,43}};
  for(int i=0;i<4;i++){int n=(i+1)%4;for(var a:new float[][]{outer[i],outer[n],inner[n],inner[i]})vertex(out,stack.last(),a[0],a[1],a[2],0,0,1,0x242c28,light,.5f,.5f);}
  quad(stack,buffers,0,0,1000,740,61,Samsung.id("textures/front.png"),light);
 }
}
