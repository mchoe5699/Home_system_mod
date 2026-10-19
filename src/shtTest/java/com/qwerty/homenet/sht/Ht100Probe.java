package com.qwerty.homenet.sht;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.renderer.RenderType;

/** Checks the shipped three-dimensional camera and removable frame without images. */
final class Ht100Probe {
 static void check(boolean value,String message){ClientRun.check(value,message);}
 static void mesh(){
  var plain=new MeshProbe.Capture();var ignored=new MeshProbe.Capture();ignored.save=false;
  var solid=RenderType.entityCutoutNoCull(Product.id("textures/solid.png"));
  Ht100Visual.draw(new PoseStack(),t->t==solid?plain:ignored,15728880,false);
  var framed=new MeshProbe.Capture();Ht100Visual.draw(new PoseStack(),t->t==solid?framed:ignored,15728880,true);
  check(Math.abs(Ht100.WIDTH/Ht100.HEIGHT-226d/300)<1e-8&&Ht100.HEIGHT==.3,"HT-100 body ratio or 0.3 block height changed");
  double pmin=plain.vertices.stream().mapToDouble(MeshProbe.V::x).min().orElseThrow(),fmin=framed.vertices.stream().mapToDouble(MeshProbe.V::x).min().orElseThrow();
  check(fmin<pmin-5,"HT-100 surround is fused to the body");
  for(int color:new int[]{0x273135,0x0b1c2a,0x3a2036,0x273b45,0xc8cfce,0xcad1d0})check(plain.vertices.stream().anyMatch(v->v.color()==color),"HT-100 clean-photo detail missing: "+color);
  // The dark floors sit behind a physically interrupted lower face, not a painted grille.
  for(int i=0;i<6;i++){double center=202+i*11.25+1.9;check(plain.vertices.stream().anyMatch(v->v.color()==0x303b3e&&Math.abs((700-v.y())-center)<2.1&&v.z()>30&&v.z()<31),"HT-100 recessed speaker aperture missing: "+i);}
  check(plain.vertices.stream().anyMatch(v->v.color()==0x39474c&&v.z()>38),"HT-100 microphone missing");
  // The frontal photo has a square glass module with independent lamp and lens.
  var glass=plain.vertices.stream().filter(v->v.color()==0x273135).toList();
  double glassW=glass.stream().mapToDouble(MeshProbe.V::x).max().orElseThrow()-glass.stream().mapToDouble(MeshProbe.V::x).min().orElseThrow();
  double glassH=glass.stream().mapToDouble(MeshProbe.V::y).max().orElseThrow()-glass.stream().mapToDouble(MeshProbe.V::y).min().orElseThrow();
  check(glassH/glassW>.95&&glassH/glassW<1.08,"HT-100 camera glass must be near-square");
  check(MeshProbe.nearest(plain.vertices,12,150).z()>MeshProbe.nearest(plain.vertices,35,150).z()+4,"HT-100 side roll is flat");
  check(MeshProbe.nearest(plain.vertices,173,235).z()>MeshProbe.nearest(plain.vertices,132,235).z()+3,"HT-100 call button is flat");
  check(Math.abs(MeshProbe.nearest(framed.vertices,113,175).z()-MeshProbe.nearest(plain.vertices,113,175).z()-12)<.1,"HT-100 body did not move with surround");
  check(plain.vertices.size()<40000&&framed.vertices.size()<40000,"HT-100 mesh too large");
  int builds=GeometryCache.builds;for(int i=0;i<8;i++)Ht100Visual.draw(new PoseStack(),t->new MeshProbe.Capture(),15728880,true);check(GeometryCache.builds==builds,"HT-100 mesh rebuilds every frame");
  System.out.println("HT100_MESH_PASS: cool-silver casing, curved side rolls, near-square glass, independent lamp and optic, two mic slots, six recessed grille slots, raised call key, separate surround; no images");
 }
 static boolean world(Minecraft mc,BlockPos at){if(!(mc.level.getBlockEntity(at) instanceof Ht100.Unit u))return false;
  check(mc.getBlockEntityRenderDispatcher().getRenderer(u) instanceof Ht100Visual.Renderer,"HT-100 world renderer absent");
  var item=new MeshProbe.Capture();Client.itemExtension().getCustomRenderer().renderByItem(new ItemStack(Product.HT100_ITEM.get()),net.minecraft.world.item.ItemDisplayContext.GUI,new PoseStack(),t->item,15728880,0);
  check(item.vertices.size()>500,"HT-100 inventory model absent");
  System.out.println("HT100_WORLD_PASS: registered camera entity and item renderer");return true;
 }
}
