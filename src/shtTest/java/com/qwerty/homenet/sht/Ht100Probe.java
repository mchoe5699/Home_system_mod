package com.qwerty.homenet.sht;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/** Checks the shipped three-dimensional camera and removable frame without images. */
final class Ht100Probe {
 static void check(boolean value,String message){ClientRun.check(value,message);}
 static void mesh(){
  var plain=new MeshProbe.Capture();Ht100Visual.draw(new PoseStack(),t->plain,15728880,false);
  var framed=new MeshProbe.Capture();Ht100Visual.draw(new PoseStack(),t->framed,15728880,true);
  check(Math.abs(Ht100.WIDTH/Ht100.HEIGHT-228d/300)<1e-8&&Ht100.HEIGHT==.3,"HT-100 body ratio or 0.3 block height changed");
  double pmin=plain.vertices.stream().mapToDouble(MeshProbe.V::x).min().orElseThrow(),fmin=framed.vertices.stream().mapToDouble(MeshProbe.V::x).min().orElseThrow();
  check(fmin<pmin-5,"HT-100 surround is fused to the body");
  for(int color:new int[]{0x35433f,0x253034,0x293a40,0x241320,0xa9b2af})check(plain.vertices.stream().anyMatch(v->v.color()==color),"HT-100 detail missing: "+color);
  // Presence in the mesh is insufficient: the original slot surfaces sat behind the front shell.
  for(int i=0;i<6;i++){double center=213+i*12.1+1.85;check(plain.vertices.stream().anyMatch(v->v.color()==0x35433f&&Math.abs((700-v.y())-center)<2.2&&v.z()>38.9),"HT-100 speaker slot hidden behind shell: "+i);}
  check(plain.vertices.stream().anyMatch(v->v.color()==0x253034&&v.z()>38.9),"HT-100 microphone hidden behind shell");
  check(plain.vertices.size()<40000&&framed.vertices.size()<40000,"HT-100 mesh too large");
  int builds=GeometryCache.builds;for(int i=0;i<8;i++)Ht100Visual.draw(new PoseStack(),t->new MeshProbe.Capture(),15728880,true);check(GeometryCache.builds==builds,"HT-100 mesh rebuilds every frame");
  System.out.println("HT100_MESH_PASS: 0.3-block body, separate frame, lens, lamp, mic, six grille slots, button and bottom plug; no images");
 }
 static boolean world(Minecraft mc,BlockPos at){if(!(mc.level.getBlockEntity(at) instanceof Ht100.Unit u))return false;
  check(mc.getBlockEntityRenderDispatcher().getRenderer(u) instanceof Ht100Visual.Renderer,"HT-100 world renderer absent");
  var item=new MeshProbe.Capture();Client.itemExtension().getCustomRenderer().renderByItem(new ItemStack(Product.HT100_ITEM.get()),net.minecraft.world.item.ItemDisplayContext.GUI,new PoseStack(),t->item,15728880,0);
  check(item.vertices.size()>500,"HT-100 inventory model absent");
  System.out.println("HT100_WORLD_PASS: registered camera entity and item renderer");return true;
 }
}
