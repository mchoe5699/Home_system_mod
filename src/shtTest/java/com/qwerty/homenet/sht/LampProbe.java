package com.qwerty.homenet.sht;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
final class LampProbe {
 static void verify(Station base,int alarm){
  int state=base.lights();ClientRun.check((state&Indicators.FIRE)==alarm,"Wrong synchronized lamp state");
  for(var lamp:Indicators.LAMPS){
   var capture=new MeshProbe.Capture();var type=RenderType.entityTranslucentEmissive(Product.id("textures/solid.png"));
   Surface.indicator(new PoseStack(),t->{ClientRun.check(t==type,"Lamp is affected by room lighting");return capture;},lamp);
   ClientRun.check(!capture.vertices.isEmpty(),"Missing lamp geometry");
   for(int i=0;i<capture.vertices.size();i+=4){var a=capture.vertices.get(i);var b=capture.vertices.get(i+1);var c=capture.vertices.get(i+2);double cross=(b.x()-a.x())*(c.y()-a.y())-(b.y()-a.y())*(c.x()-a.x());ClientRun.check(cross>0,"Lamp culled when viewed from front");}
   ClientRun.check(capture.vertices.stream().allMatch(v->v.color()==lamp.rgb()),"Wrong emitted lamp color");
  }
 }
}
