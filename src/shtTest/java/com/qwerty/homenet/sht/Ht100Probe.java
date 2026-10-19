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
  check(Math.abs(Ht100.WIDTH/Ht100.HEIGHT-226d/300)<1e-8&&Ht100.HEIGHT==.3,"HT-100 body ratio or 0.3 block height changed");
  double pmin=plain.vertices.stream().mapToDouble(MeshProbe.V::x).min().orElseThrow(),fmin=framed.vertices.stream().mapToDouble(MeshProbe.V::x).min().orElseThrow();
  check(fmin<pmin-5,"HT-100 surround is fused to the body");
  for(int color:new int[]{0x28363b,0x0d2034,0x371c32,0x304a50,0xcbd1ce})check(plain.vertices.stream().anyMatch(v->v.color()==color),"HT-100 photo detail missing: "+color);
  // The dark floors sit behind a physically interrupted lower face, not a painted grille.
  for(int i=0;i<6;i++){double center=212+i*12.55+2.1;check(plain.vertices.stream().anyMatch(v->v.color()==0x34433f&&Math.abs((700-v.y())-center)<2.3&&v.z()>30&&v.z()<31),"HT-100 speaker aperture missing: "+i);}
  check(plain.vertices.stream().anyMatch(v->v.color()==0x344642&&v.z()>30),"HT-100 microphone missing");
  // The clean photograph shows a tall module, not the square one from the discarded model.
  var glass=plain.vertices.stream().filter(v->v.color()==0x28363b).toList();
  double glassW=glass.stream().mapToDouble(MeshProbe.V::x).max().orElseThrow()-glass.stream().mapToDouble(MeshProbe.V::x).min().orElseThrow();
  double glassH=glass.stream().mapToDouble(MeshProbe.V::y).max().orElseThrow()-glass.stream().mapToDouble(MeshProbe.V::y).min().orElseThrow();
  check(glassH>glassW*1.25,"HT-100 camera glass is not tall enough");
  check(plain.vertices.size()<40000&&framed.vertices.size()<40000,"HT-100 mesh too large");
  int builds=GeometryCache.builds;for(int i=0;i<8;i++)Ht100Visual.draw(new PoseStack(),t->new MeshProbe.Capture(),15728880,true);check(GeometryCache.builds==builds,"HT-100 mesh rebuilds every frame");
  System.out.println("HT100_MESH_PASS: rebuilt photo-profile 0.3-block body, separate surround, tall glass, lens, lamp, mic, six recessed grille slots, curved call button and keeper; no images");
 }
 static boolean world(Minecraft mc,BlockPos at){if(!(mc.level.getBlockEntity(at) instanceof Ht100.Unit u))return false;
  check(mc.getBlockEntityRenderDispatcher().getRenderer(u) instanceof Ht100Visual.Renderer,"HT-100 world renderer absent");
  var item=new MeshProbe.Capture();Client.itemExtension().getCustomRenderer().renderByItem(new ItemStack(Product.HT100_ITEM.get()),net.minecraft.world.item.ItemDisplayContext.GUI,new PoseStack(),t->item,15728880,0);
  check(item.vertices.size()>500,"HT-100 inventory model absent");
  System.out.println("HT100_WORLD_PASS: registered camera entity and item renderer");return true;
 }
}
