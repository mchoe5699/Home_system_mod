package com.qwerty.homenet.sht;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import java.util.*;
import java.util.function.BiConsumer;

/** Immutable local-space meshes. Reuse across devices, keeping lighting and placement live. */
final class GeometryCache {
 private static final Map<String,List<Batch>> MESHES=new HashMap<>();
 private record Batch(RenderType type,float[] data){}
 private static final Map<String,net.minecraft.world.phys.AABB> BOUNDS=new HashMap<>();
 static int builds;
 static void clear(){MESHES.clear();BOUNDS.clear();builds=0;}
 static net.minecraft.world.phys.AABB bounds(String key){
  var result=BOUNDS.get(key);if(result!=null)return result;var mesh=MESHES.get(key);if(mesh==null)return null;
  double minX=Double.POSITIVE_INFINITY,minY=minX,minZ=minX,maxX=Double.NEGATIVE_INFINITY,maxY=maxX,maxZ=maxX;
  for(var batch:mesh)if(batch.type==RenderType.entityCutoutNoCull(Product.id("textures/solid.png")))for(int i=0;i<batch.data.length;i+=11){var a=batch.data;minX=Math.min(minX,a[i]);minY=Math.min(minY,a[i+1]);minZ=Math.min(minZ,a[i+2]);maxX=Math.max(maxX,a[i]);maxY=Math.max(maxY,a[i+1]);maxZ=Math.max(maxZ,a[i+2]);}
  if(!Double.isFinite(minX))return null;result=new net.minecraft.world.phys.AABB(minX,minY,minZ,maxX,maxY,maxZ);BOUNDS.put(key,result);return result;
 }
 static void draw(String key,PoseStack pose,MultiBufferSource out,int light,BiConsumer<PoseStack,MultiBufferSource> build){
  var mesh=MESHES.get(key);
  if(mesh==null){
   var collectors=new LinkedHashMap<RenderType,Collector>();
   build.accept(new PoseStack(),type->collectors.computeIfAbsent(type,t->new Collector()));
   mesh=new ArrayList<>(collectors.size());
   for(var e:collectors.entrySet())mesh.add(new Batch(e.getKey(),compact(e.getKey(),e.getValue())));
   MESHES.put(key,mesh);builds++;
  }
  var transform=pose.last();var position=new org.joml.Vector3f();var normal=new org.joml.Vector3f();
  for(var batch:mesh){var consumer=out.getBuffer(batch.type);var a=batch.data;
   for(int i=0;i<a.length;i+=11){int color=Float.floatToRawIntBits(a[i+8]),storedLight=Float.floatToRawIntBits(a[i+9]);
    transform.pose().transformPosition(a[i],a[i+1],a[i+2],position);transform.normal().transform(a[i+5],a[i+6],a[i+7],normal);
    // BufferBuilder overrides this bulk vertex call, avoiding six element advances per vertex.
    consumer.vertex(position.x,position.y,position.z,(color>>>24)/255f,(color>>>16&255)/255f,(color>>>8&255)/255f,(color&255)/255f,
     a[i+3],a[i+4],Float.floatToRawIntBits(a[i+10]),storedLight==0?light:storedLight,normal.x,normal.y,normal.z);
   }
  }
 }
 // Merge only truly coplanar solid rectangles with identical color, UV, lighting and normals.
 // Curves, seams, textured planes and nonrectangular faces retain their original vertices.
 private record Plane(int z,int nx,int ny,int nz,int u,int v,int color,int light,int overlay,int winding){}
 private record Span(float lo,float hi){}
 private static final class Rect {
  float x0,x1,y0,y1;final float[] sample;
  Rect(float[] a,int offset,float x0,float x1,float y0,float y1){sample=Arrays.copyOfRange(a,offset,offset+44);this.x0=x0;this.x1=x1;this.y0=y0;this.y1=y1;}
 }
 private static float[] compact(RenderType type,Collector source){
  if(type!=RenderType.entityCutoutNoCull(Product.id("textures/solid.png")))return Arrays.copyOf(source.data,source.size);
  var planes=new LinkedHashMap<Plane,List<Rect>>();var result=new Collector();var a=source.data;
  for(int i=0;i<source.size;i+=44){
   float x0=Float.POSITIVE_INFINITY,x1=Float.NEGATIVE_INFINITY,y0=x0,y1=x1;boolean flat=true;
   for(int j=0;j<4;j++){int at=i+j*11;x0=Math.min(x0,a[at]);x1=Math.max(x1,a[at]);y0=Math.min(y0,a[at+1]);y1=Math.max(y1,a[at+1]);for(int k=2;k<11;k++)if(a[at+k]!=a[i+k]&&Float.floatToRawIntBits(a[at+k])!=Float.floatToRawIntBits(a[i+k]))flat=false;}
   int corners=0;
   for(int j=0;j<4;j++){float x=a[i+j*11],y=a[i+j*11+1];if((x!=x0&&x!=x1)||(y!=y0&&y!=y1))flat=false;corners|=1<<((x==x1?1:0)|(y==y1?2:0));}
   if(!flat||corners!=15||x0==x1||y0==y1){result.append(a,i,44);continue;}
   int pattern=0;for(int j=0;j<4;j++)pattern|=((a[i+j*11]==x1?1:0)|(a[i+j*11+1]==y1?2:0))<<(j*2);
   var plane=new Plane(Float.floatToRawIntBits(a[i+2]),Float.floatToRawIntBits(a[i+5]),Float.floatToRawIntBits(a[i+6]),Float.floatToRawIntBits(a[i+7]),Float.floatToRawIntBits(a[i+3]),Float.floatToRawIntBits(a[i+4]),Float.floatToRawIntBits(a[i+8]),Float.floatToRawIntBits(a[i+9]),Float.floatToRawIntBits(a[i+10]),pattern);
   planes.computeIfAbsent(plane,k->new ArrayList<>()).add(new Rect(a,i,x0,x1,y0,y1));
  }
  for(var rectangles:planes.values())for(var rect:merge(merge(rectangles,true),false)){
   float minX=Float.POSITIVE_INFINITY,minY=minX,maxX=Float.NEGATIVE_INFINITY,maxY=maxX;
   for(int j=0;j<4;j++){minX=Math.min(minX,rect.sample[j*11]);maxX=Math.max(maxX,rect.sample[j*11]);minY=Math.min(minY,rect.sample[j*11+1]);maxY=Math.max(maxY,rect.sample[j*11+1]);}
   for(int j=0;j<4;j++){rect.sample[j*11]=rect.sample[j*11]==minX?rect.x0:rect.x1;rect.sample[j*11+1]=rect.sample[j*11+1]==minY?rect.y0:rect.y1;}result.append(rect.sample,0,44);
  }
  return Arrays.copyOf(result.data,result.size);
 }
 private static List<Rect> merge(List<Rect> rectangles,boolean horizontal){
  var groups=new LinkedHashMap<Span,List<Rect>>();for(var r:rectangles)groups.computeIfAbsent(horizontal?new Span(r.y0,r.y1):new Span(r.x0,r.x1),k->new ArrayList<>()).add(r);
  var result=new ArrayList<Rect>();
  for(var group:groups.values()){group.sort(Comparator.comparingDouble(r->horizontal?r.x0:r.y0));Rect previous=null;for(var r:group){if(previous!=null&&(horizontal?previous.x1==r.x0:previous.y1==r.y0)){if(horizontal)previous.x1=r.x1;else previous.y1=r.y1;}else{result.add(r);previous=r;}}}return result;
 }
 private static final class Collector implements VertexConsumer {
  float[] data=new float[11*1024];int size;float x,y,z,u,v,nx,ny,nz;int color,light,overlay;
  public VertexConsumer vertex(double x,double y,double z){this.x=(float)x;this.y=(float)y;this.z=(float)z;return this;}
  public VertexConsumer color(int r,int g,int b,int a){color=r<<24|g<<16|b<<8|a;return this;}
  public VertexConsumer uv(float u,float v){this.u=u;this.v=v;return this;}
  public VertexConsumer overlayCoords(int u,int v){overlay=u|v<<16;return this;}
  public VertexConsumer uv2(int u,int v){light=u|v<<16;return this;}
  public VertexConsumer normal(float x,float y,float z){nx=x;ny=y;nz=z;return this;}
  void append(float[] input,int offset,int count){while(size+count>data.length)data=Arrays.copyOf(data,data.length*2);System.arraycopy(input,offset,data,size,count);size+=count;}
  public void endVertex(){if(size+11>data.length)data=Arrays.copyOf(data,data.length*2);data[size++]=x;data[size++]=y;data[size++]=z;data[size++]=u;data[size++]=v;data[size++]=nx;data[size++]=ny;data[size++]=nz;data[size++]=Float.intBitsToFloat(color);data[size++]=Float.intBitsToFloat(light);data[size++]=Float.intBitsToFloat(overlay);}
  public void defaultColor(int r,int g,int b,int a){color(r,g,b,a);}public void unsetDefaultColor(){}
 }
}
