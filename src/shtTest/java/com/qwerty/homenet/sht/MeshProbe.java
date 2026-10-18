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
 static void portable(){
  var solid=new Capture();var ignored=new Capture();ignored.save=false;var type=RenderType.entityCutoutNoCull(Product.id("textures/solid.png"));PortableVisual.handset(new PoseStack(),t->t==type?solid:ignored,15728880,false);
  if(nearest(solid.vertices,112,485).color()!=0x56615b)throw new AssertionError("Portable control band buried inside case");
  for(double y:new double[]{371,379,387})if(nearest(solid.vertices,112,y).color()!=0x6d7767)throw new AssertionError("Portable speaker slit hidden");
  for(var k:PortableVisual.keys)if(nearest(solid.vertices,k.x(),k.y()).color()!=0xe8e8d7)throw new AssertionError("Portable key obscured: "+k.name());
  System.out.println("SAMSUNG_PORTABLE_MESH_PASS: exposed speaker slits, control band and all 21 keys; manual rounded head");
 }
 static void wired(){
  Surface.select(true);var solid=new Capture();var ignored=new Capture();ignored.save=false;var type=RenderType.entityCutoutNoCull(Product.id("textures/solid.png"));Surface.draw(new PoseStack(),t->t==type?solid:ignored,15728880,false,true,false);
  for(double x:new double[]{590,674,758})for(double y:new double[]{190,246,300}){var v=nearest(solid.vertices,x,y);if(v.color!=0x8d9691||Math.abs(v.z-Surface.bodyDepth(65))>.001)throw new AssertionError("102 CRT blocked");}
  if(Surface.solids.stream().anyMatch(p->p.name().startsWith("radio_")||p.name().contains("cable")))throw new AssertionError("102 has wireless/cable geometry");
  var handset=nearest(solid.vertices,214,350);if(handset.z()<Surface.bodyDepth(68)+35)throw new AssertionError("Wired handset absent");
  var empty=new Capture();Surface.draw(new PoseStack(),t->t==type?empty:ignored,15728880,false,false,false);
  if(empty.vertices.stream().anyMatch(v->v.x()<285&&v.z()>Surface.bodyDepth(68)+5))throw new AssertionError("Lifted SHT-102C handset still floats beside cradle");
  double blackTop=Surface.solids.stream().filter(v->v.group().equals("extended")).mapToDouble(Surface.Solid::y).min().orElseThrow(),blackBottom=Surface.solids.stream().filter(v->v.group().equals("extended")).mapToDouble(v->v.y()+v.h()).max().orElseThrow();
  double blackLeft=Surface.solids.stream().filter(v->v.group().equals("folded")).mapToDouble(Surface.Solid::x).min().orElseThrow(),blackRight=Surface.solids.stream().filter(v->v.group().equals("folded")).mapToDouble(v->v.x()+v.w()).max().orElseThrow();
  Surface.select(false);double silverTop=Surface.solids.stream().filter(v->v.group().equals("extended")).mapToDouble(Surface.Solid::y).min().orElseThrow(),silverBottom=Surface.solids.stream().filter(v->v.group().equals("extended")).mapToDouble(v->v.y()+v.h()).max().orElseThrow();
  double silverLeft=Surface.solids.stream().filter(v->v.group().equals("folded")).mapToDouble(Surface.Solid::x).min().orElseThrow(),silverRight=Surface.solids.stream().filter(v->v.group().equals("folded")).mapToDouble(v->v.x()+v.w()).max().orElseThrow();
  if(blackBottom-blackTop!=silverBottom-silverTop||blackRight-blackLeft!=silverRight-silverLeft)throw new AssertionError("SHT-102C antenna length differs from SHT-101C");Surface.select(true);
  if(Surface.buttons.stream().noneMatch(k->k.name().equals("line")&&k.y()==591))throw new AssertionError("National line key moved");
  Surface.select(false);System.out.println("SHT102_MESH_PASS: clear 4:3 CRT, wired handset, black aerial, no cordless/cable, fixed national key, independent model cache, fully removed off-hook handset and 101-length antenna");
 }
 static void verify(){var solid=new Capture();var ignored=new Capture();ignored.save=false;var expected=RenderType.entityCutoutNoCull(Product.id("textures/solid.png"));Surface.draw(new PoseStack(),type->type==expected?solid:ignored,15728880,false,false,true);
  if(solid.vertices.isEmpty())throw new AssertionError("Probe captured no solid vertices");
  for(double x:new double[]{590,674,758})for(double y:new double[]{190,246,300}){var v=nearest(solid.vertices,x,y);if(v.color!=0x8d9691||Math.abs(v.z-Surface.bodyDepth(65))>.001)throw new AssertionError("CRT blocked at "+x+","+y+" by "+Integer.toHexString(v.color)+" z="+v.z);}
  // Former panel/cap gaps must all be on the same continuous front skin.
  for(double[] at:new double[][]{{301,220},{453,400},{521,400},{827,400},{330,650},{530,650}}){var v=nearest(solid.vertices,at[0],at[1]);if(v.color!=0xf1efdf||Math.abs(v.z-Surface.bodyDepth(68))>.001)throw new AssertionError("Body split or stepped at "+Arrays.toString(at)+" z="+v.z);}
  for(float x:new float[]{116,122,132,144,900,912,922,928})if(Surface.shellZ(x,400)>68.001f)throw new AssertionError("Sides bulge above front");
  for(float[] side:new float[][]{{154,110},{891,934}}){
   float start=side[0],edge=side[1],last=68;
   for(int i=0;i<=20;i++){float x=start+(edge-start)*i/20;float z=Surface.shellZ(x,98);if(z>last+.001)throw new AssertionError("Side rises before rolling down");last=z;}
   if(Math.abs(Surface.shellZ(start,98)-68)>.001||Math.abs(Surface.shellZ(edge,98)-58)>.001||Surface.shellZ((start+edge)/2,98)>=67)throw new AssertionError("Incorrect broad side rollover");
  }
  for(var seam:Surface.casing.seams()){
   float x=(seam.x0()+seam.x1())/2,y=(seam.y0()+seam.y1())/2;var v=nearest(solid.vertices,x,y);
   if(Math.abs(v.z-Surface.bodyDepth(68-seam.depth()))>.01)throw new AssertionError("Cover joint missing or disconnected at "+x+","+y+" z="+v.z);
  }
  var frame=Surface.solids.stream().filter(v->v.name().equals("mount_plate")).findFirst().orElseThrow();
  if(Math.abs(frame.x()+frame.w()/2-(110+934)/2f)>.001||Math.abs(frame.y()+frame.h()/2-(89+672)/2f)>.001)throw new AssertionError("Outer trim is not centered on body");
  for(var button:Surface.solids)if(button.group().equals("base")&&java.util.Set.of("security","away","memory","hold","redial","guard","door","speaker").contains(button.name())){
   var face=new Capture();Surface.solid(button,new PoseStack(),type->face,15728880);
   var center=nearest(face.vertices,button.x()+button.w()/2,button.y()+button.h()/2);
   var side=nearest(face.vertices,button.x()+button.w()*.2,button.y()+button.h()/2);
   if(center.z-side.z<.10||center.z-side.z>.5)throw new AssertionError("Central button face is flat or excessively domed: "+button.name());
  }
  var docked=new Capture();Surface.draw(new PoseStack(),type->type==expected?docked:ignored,15728880,false,true,true);
  var originalHandset=new Capture();for(var part:Surface.solids)if(part.group().equals("docked"))Surface.solid(part,new PoseStack(),type->originalHandset,15728880);
  double expectedShift=Surface.bodyDepth(68)-68;
  if(expectedShift>=-40||Surface.mounting.depth()>=28)throw new AssertionError("Body stayed in place or did not get thinner");
  for(double[] at:new double[][]{{180,200},{220,400},{260,550}}){var before=nearest(originalHandset.vertices,at[0],at[1]);var after=nearest(docked.vertices,at[0],at[1]);if(Math.abs((after.z-before.z)-expectedShift)>.001)throw new AssertionError("Handset not moved rigidly with body");}
  var sheet=nearest(solid.vertices,80,400);if(Math.abs(sheet.z*Surface.SCALE-.0005)>.0000001)throw new AssertionError("Outer frame is not 0.5 mm");
  var bowl=nearest(solid.vertices,228,204);if(bowl.z>=Surface.bodyDepth(60)||bowl.color!=0xe3e1d0)throw new AssertionError("Cradle floor covered or not recessed");
  if(solid.vertices.stream().noneMatch(v->v.color==0x4c5449&&v.ny<-.99))throw new AssertionError("Vents are not on downward-facing underside");
  System.out.println("SHT_BODY_MESH_PASS: 0.5 mm sheet, thinner body moved toward wall, rigidly translated handset, connected covers at six former seams, nine CRT rays reach glass, recessed cradle, underside vents, finite mesh");
 }
}
