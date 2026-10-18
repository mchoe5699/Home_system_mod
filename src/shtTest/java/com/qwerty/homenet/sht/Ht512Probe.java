package com.qwerty.homenet.sht;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Blocks;
import com.qwerty.homenet.block.WallMountedBlock;

/** Numeric geometry, bounds and placement probes only: no gameplay capture. */
final class Ht512Probe {
 static void check(boolean b,String s){ClientRun.check(b,s);}
 static void mesh(){
  var all=new MeshProbe.Capture();Ht512Visual.draw(new PoseStack(),t->all,15728880);
  check(Ht512Visual.keys.size()==14,"HT-512 needs exactly 12 dial and two call keys");
  check(Math.abs(Ht512.WIDTH/Ht512.HEIGHT-464d/610)<1e-8,"HT-512 photo ratio changed");
  // Compare actual KLP-203 model extents loaded from the unchanged baseline.
  try(var r=Minecraft.getInstance().getResourceManager().openAsReader(com.qwerty.homenet.HomeNet.id("models/block/klp_203.json"))){var e=com.google.gson.JsonParser.parseReader(r).getAsJsonObject().getAsJsonArray("elements");double lo=100,hi=-100;for(var q:e){var o=q.getAsJsonObject();lo=Math.min(lo,o.getAsJsonArray("from").get(1).getAsDouble());hi=Math.max(hi,o.getAsJsonArray("to").get(1).getAsDouble());}check(Math.abs(Ht512.HEIGHT-(hi-lo)/16)<1e-8,"HT-512 height does not match KLP-203");}catch(java.io.IOException e){throw new AssertionError(e);}
  check(Math.abs(all.vertices.stream().mapToDouble(MeshProbe.V::x).max().orElseThrow()-464)<.001,"HT width bounds");
  check(Math.abs(all.vertices.stream().mapToDouble(MeshProbe.V::y).max().orElseThrow()-700)<.001,"HT top bounds");
  for(int row=0;row<11;row++)for(int col=0;col<9;col++){var v=MeshProbe.nearest(all.vertices,112.1+col*10.2,335.1+row*10.2);check(v.color()==0x101611&&v.z()<13,"HT speaker hole filled with panel");}
  check(MeshProbe.nearest(all.vertices,151,497.5).z()<10,"Card slot is painted instead of recessed");
  check(MeshProbe.nearest(all.vertices,250,400).z()==15,"Main panel is not flat");
  double front=-1;for(var k:Ht512Visual.keys){double z=MeshProbe.nearest(all.vertices,k.x()+k.w()/2,k.y()+k.h()/2).z();check(z>19.3,"HT button buried");if(front>=0)check(Math.abs(z-front)<.01,"HT keys have different protrusion");front=z;}
  int builds=GeometryCache.builds;var discard=new MeshProbe.Capture();discard.save=false;for(int i=0;i<10;i++)Ht512Visual.draw(new PoseStack(),t->discard,15728880);check(GeometryCache.builds==builds,"HT mesh rebuilt each frame");check(all.vertices.size()<40000,"HT mesh excessive");
  HelpProbe.verify(new Ht512Visual.ModelScreen());Minecraft.getInstance().setScreen(null);
  System.out.println("HT512_MESH_PASS: KLP-203 exact height, photo ratio, 99 open speaker holes, inset card slot, 14 equally raised keys, cached mesh; no images");
 }
 static boolean world(Minecraft mc,BlockPos at){if(!(mc.level.getBlockEntity(at) instanceof Ht512.Unit))return false;
  for(var facing:new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST}){var state=Product.HT512.get().defaultBlockState().setValue(WallMountedBlock.FACING,facing);var unit=new Ht512.Unit(at,state);unit.setLevel(mc.level);var p=new PoseStack();Ht512Visual.pose(unit,p);var capture=new MeshProbe.Capture();Ht512Visual.draw(p,t->capture,15728880);double x0=capture.vertices.stream().mapToDouble(MeshProbe.V::x).min().orElseThrow(),x1=capture.vertices.stream().mapToDouble(MeshProbe.V::x).max().orElseThrow(),y0=capture.vertices.stream().mapToDouble(MeshProbe.V::y).min().orElseThrow(),y1=capture.vertices.stream().mapToDouble(MeshProbe.V::y).max().orElseThrow(),z0=capture.vertices.stream().mapToDouble(MeshProbe.V::z).min().orElseThrow(),z1=capture.vertices.stream().mapToDouble(MeshProbe.V::z).max().orElseThrow();var shape=state.getShape(mc.level,at).bounds();check(Math.abs(y1-y0-Ht512.HEIGHT)<.00001,"HT world height");check(Math.abs(x0-shape.minX)<.0003&&Math.abs(y0-shape.minY)<.00001&&Math.abs(z0-shape.minZ)<.0003&&Math.abs(x1-shape.maxX)<.0003&&Math.abs(y1-shape.maxY)<.00001&&Math.abs(z1-shape.maxZ)<.0003,"HT model/click bounds differ "+facing);}
  var dispatcher=mc.getBlockEntityRenderDispatcher().getRenderer((Ht512.Unit)mc.level.getBlockEntity(at));check(dispatcher instanceof Ht512Visual.Renderer,"HT renderer registration missing");var item=new MeshProbe.Capture();Client.itemExtension().getCustomRenderer().renderByItem(new net.minecraft.world.item.ItemStack(Product.HT512_ITEM.get()),net.minecraft.world.item.ItemDisplayContext.GUI,new PoseStack(),t->item,15728880,0);check(item.vertices.size()>1000,"HT creative item is invisible");
  mc.gameMode.useItemOn(mc.player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at),Direction.NORTH,at,false));check(mc.screen instanceof Ht512Visual.ModelScreen,"HT model view did not open on actual use");check(UsageHelp.state(mc.screen)!=null,"HT use screen missing model usage button");mc.setScreen(null);System.out.println("HT512_WORLD_PASS: registered entity renderer, four wall poses, matching hit bounds and actual use screen");return true;
 }
}
