package com.qwerty.homenet.sht;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import java.util.*;
/** Probe actual generated triangles without capturing or producing any image. */
final class MeshProbe {
 record V(double x,double y,double z,int color,float ny){}
 static class Capture implements VertexConsumer {
  final List<V> vertices=new ArrayList<>();double x,y,z;int color;float ny;boolean save=true;
  public VertexConsumer vertex(double x,double y,double z){if(!Double.isFinite(x+y+z))throw new AssertionError("Non-finite mesh position");this.x=x;this.y=y;this.z=z;return this;}
  public VertexConsumer color(int r,int g,int b,int a){color=r<<16|g<<8|b;return this;}
  public VertexConsumer uv(float u,float v){return this;}public VertexConsumer overlayCoords(int a,int b){return this;}public VertexConsumer uv2(int a,int b){return this;}
  public VertexConsumer normal(float x,float y,float z){if(!Float.isFinite(x+y+z))throw new AssertionError("Non-finite mesh normal");ny=y/(float)Math.sqrt(x*x+y*y+z*z);return this;}
  public void endVertex(){if(save)vertices.add(new V(x,y,z,color,ny));}public void defaultColor(int r,int g,int b,int a){}public void unsetDefaultColor(){}
 }
 static double hit(V a,V b,V c,double x,double y){double det=(b.y-c.y)*(a.x-c.x)+(c.x-b.x)*(a.y-c.y);if(Math.abs(det)<1e-8)return Double.NEGATIVE_INFINITY;double u=((b.y-c.y)*(x-c.x)+(c.x-b.x)*(y-c.y))/det,v=((c.y-a.y)*(x-c.x)+(a.x-c.x)*(y-c.y))/det;if(u<-.00001||v<-.00001||u+v>1.00001)return Double.NEGATIVE_INFINITY;return u*a.z+v*b.z+(1-u-v)*c.z;}
 static V nearest(List<V> vs,double x,double photoY){double z=Double.NEGATIVE_INFINITY;V found=null;for(int i=0;i<vs.size();i+=4){var a=vs.get(i);for(int j=1;j<=2;j++){double at=hit(a,vs.get(i+j),vs.get(i+j+1),x,700-photoY);if(at>z){z=at;found=new V(x,photoY,z,a.color,a.ny);}}}if(found==null)throw new AssertionError("No surface at "+x+","+photoY);return found;}
 static void verify(){var solid=new Capture();var ignored=new Capture();ignored.save=false;var expected=RenderType.entityCutoutNoCull(Product.id("textures/solid.png"));Surface.draw(new PoseStack(),type->type==expected?solid:ignored,15728880,false,false,true);
  if(solid.vertices.isEmpty())throw new AssertionError("Probe captured no solid vertices");
  for(double x:new double[]{590,674,758})for(double y:new double[]{190,246,300}){var v=nearest(solid.vertices,x,y);if(v.color!=0x8d9691||Math.abs(v.z-Surface.bodyDepth(65))>.001)throw new AssertionError("CRT blocked at "+x+","+y+" by "+Integer.toHexString(v.color)+" z="+v.z);}
  // Former panel/cap gaps must all be on the same continuous front skin.
  for(double[] at:new double[][]{{299,220},{456,400},{519,400},{830,400},{330,648},{530,648}}){var v=nearest(solid.vertices,at[0],at[1]);if(v.color!=0xf1efdf||Math.abs(v.z-Surface.bodyDepth(68))>.001)throw new AssertionError("Body split or stepped at "+Arrays.toString(at)+" z="+v.z);}
  var docked=new Capture();Surface.draw(new PoseStack(),type->type==expected?docked:ignored,15728880,false,true,true);
  var originalHandset=new Capture();for(var part:Surface.solids)if(part.group().equals("docked"))Surface.solid(part,new PoseStack(),type->originalHandset,15728880);
  double expectedShift=Surface.bodyDepth(68)-68;
  if(expectedShift>=-40||Surface.mounting.depth()>=28)throw new AssertionError("Body stayed in place or did not get thinner");
  for(double[] at:new double[][]{{180,200},{220,400},{260,550}}){var before=nearest(originalHandset.vertices,at[0],at[1]);var after=nearest(docked.vertices,at[0],at[1]);if(Math.abs((after.z-before.z)-expectedShift)>.001)throw new AssertionError("Handset not moved rigidly with body");}
  var sheet=nearest(solid.vertices,80,400);if(Math.abs(sheet.z*Surface.SCALE-.0002)>.0000001)throw new AssertionError("Outer frame is not 0.2 mm");
  var bowl=nearest(solid.vertices,228,204);if(bowl.z>=Surface.bodyDepth(60)||bowl.color!=0xe3e1d0)throw new AssertionError("Cradle floor covered or not recessed");
  if(solid.vertices.stream().noneMatch(v->v.color==0x4c5449&&v.ny<-.99))throw new AssertionError("Vents are not on downward-facing underside");
  System.out.println("SHT_BODY_MESH_PASS: 0.2 mm sheet, thinner body moved toward wall, rigidly translated handset, connected covers at six former seams, nine CRT rays reach glass, recessed cradle, underside vents, finite mesh");
 }
}
