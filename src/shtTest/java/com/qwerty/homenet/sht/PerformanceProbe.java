package com.qwerty.homenet.sht;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import java.util.*;
/** Numeric geometry and CPU/allocation checks only; no pixels or screenshots. */
final class PerformanceProbe {
 static final class Sink implements com.mojang.blaze3d.vertex.VertexConsumer {
  long vertices;int light;boolean sawLit,sawDark;
  public com.mojang.blaze3d.vertex.VertexConsumer vertex(double x,double y,double z){return this;}
  public com.mojang.blaze3d.vertex.VertexConsumer color(int r,int g,int b,int a){return this;}
  public com.mojang.blaze3d.vertex.VertexConsumer uv(float u,float v){return this;}
  public com.mojang.blaze3d.vertex.VertexConsumer overlayCoords(int a,int b){return this;}
  public com.mojang.blaze3d.vertex.VertexConsumer uv2(int a,int b){light=a|b<<16;sawDark|=light==32;sawLit|=light==15728880;return this;}
  public com.mojang.blaze3d.vertex.VertexConsumer normal(float x,float y,float z){return this;}
  public void endVertex(){vertices++;}public void defaultColor(int r,int g,int b,int a){}public void unsetDefaultColor(){}
 }
 static void verify(){
  GeometryCache.clear();long before=0,after=0;
  for(boolean wired:new boolean[]{false,true})for(boolean docked:new boolean[]{false,true})for(boolean folded:new boolean[]{false,true}){
   Surface.select(wired);var raw=new MeshProbe.Capture();var cached=new MeshProbe.Capture();var skip=new Sink();var type=RenderType.entityCutoutNoCull(Product.id("textures/solid.png"));
   Surface.drawRaw(new PoseStack(),t->t==type?raw:skip,32,false,docked,folded);
   Surface.draw(new PoseStack(),t->t==type?cached:skip,32,false,docked,folded);
   before+=raw.vertices.size();after+=cached.vertices.size();
   for(double x:new double[]{115,130,153,210,300,330,400,455,518,530,560,700,820,890,920})for(double y:new double[]{95,110,145,220,300,400,550,640,660}){
    var a=MeshProbe.nearest(raw.vertices,x,y);var b=MeshProbe.nearest(cached.vertices,x,y);
    if(Math.abs(a.z()-b.z())>.001||a.color()!=b.color()||Math.abs(a.ny()-b.ny())>.001)throw new AssertionError("Cached shape differs: "+wired+"/"+docked+"/"+folded+" at "+x+","+y);
   }
  }
  if(after>=before*.85)throw new AssertionError("Planar compaction did not remove enough redundant vertices: "+before+" -> "+after);
  Surface.select(false);var sink=new Sink();MultiBufferSource out=t->sink;var pose=new PoseStack();
  int builds=GeometryCache.builds;for(int i=0;i<30;i++)Surface.draw(pose,out,32,false,true,true);
  if(GeometryCache.builds!=builds)throw new AssertionError("Station cache rebuilt during steady rendering");
  PortableVisual.handset(pose,out,32,true,false);PortableVisual.stand(pose,out,32,true,true,true,true);builds=GeometryCache.builds;
  for(int i=0;i<30;i++){PortableVisual.handset(pose,out,32,true,false);PortableVisual.stand(pose,out,32,true,true,true,true);}
  if(GeometryCache.builds!=builds||!sink.sawDark||!sink.sawLit)throw new AssertionError("Portable cache or dynamic/emissive lighting regressed");
  var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();long id=Thread.currentThread().getId();
  long a0=bean.getThreadAllocatedBytes(id),t0=System.nanoTime();for(int i=0;i<12;i++)Surface.drawRaw(pose,out,32,false,true,true);long rawNanos=System.nanoTime()-t0,rawBytes=bean.getThreadAllocatedBytes(id)-a0;
  a0=bean.getThreadAllocatedBytes(id);t0=System.nanoTime();for(int i=0;i<12;i++)Surface.draw(pose,out,32,false,true,true);long cachedNanos=System.nanoTime()-t0,cachedBytes=bean.getThreadAllocatedBytes(id)-a0;
  if(cachedBytes>=rawBytes/5)throw new AssertionError("Geometry allocation reduction insufficient: "+rawBytes+" -> "+cachedBytes);
  System.out.println("SAMSUNG_PERFORMANCE_PASS: station variants vertices "+before+" -> "+after+"; 12 frames CPU ms "+rawNanos/1e6+" -> "+cachedNanos/1e6+"; allocated bytes "+rawBytes+" -> "+cachedBytes+"; shape rays, cache reuse, live/emissive lighting passed");
  var buffer=new com.mojang.blaze3d.vertex.BufferBuilder(8*1024*1024);MultiBufferSource real=t->buffer;
  for(int i=0;i<3;i++){buffer.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,com.mojang.blaze3d.vertex.DefaultVertexFormat.NEW_ENTITY);Surface.draw(pose,real,32,false,true,true);buffer.end().release();}
  t0=System.nanoTime();for(int i=0;i<12;i++){buffer.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,com.mojang.blaze3d.vertex.DefaultVertexFormat.NEW_ENTITY);Surface.drawRaw(pose,real,32,false,true,true);buffer.end().release();}long rawBuffer=System.nanoTime()-t0;
  t0=System.nanoTime();for(int i=0;i<12;i++){buffer.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,com.mojang.blaze3d.vertex.DefaultVertexFormat.NEW_ENTITY);Surface.draw(pose,real,32,false,true,true);buffer.end().release();}long cachedBuffer=System.nanoTime()-t0;
  if(cachedBuffer>=rawBuffer)throw new AssertionError("Real vertex buffer path did not improve");
  System.out.println("SAMSUNG_BUFFER_PERFORMANCE_PASS: 12 actual BufferBuilder frames CPU ms "+rawBuffer/1e6+" -> "+cachedBuffer/1e6);
  GeometryCache.clear();Surface.select(false);
 }
}
