package com.qwerty.homenet.sht;

import com.mojang.blaze3d.vertex.PoseStack;
import com.qwerty.homenet.block.WallMountedBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Numeric mesh and installation probe; no gameplay images are saved. */
final class Ht100sProbe {
 static void check(boolean ok,String why){ClientRun.check(ok,why);}
 static boolean world(Minecraft mc,BlockPos at){if(!(mc.level.getBlockEntity(at) instanceof Ht100s.Unit))return false;
  check(Math.abs(Ht100s.HEIGHT-.15)<1e-10,"HT-100S body is not 0.15 block tall");
  check(Math.abs(Ht100s.WIDTH/Ht100s.HEIGHT-230d/300)<1e-8,"HT-100S body ratio wrong");
  var c=new MeshProbe.Capture();Ht100sVisual.draw(new PoseStack(),t->c,15728880);
  check(c.vertices.stream().mapToDouble(MeshProbe.V::x).min().orElseThrow()<0,"Surround missing on left");
  check(c.vertices.stream().mapToDouble(MeshProbe.V::x).max().orElseThrow()>230,"Surround missing on right");
  check(c.vertices.stream().mapToDouble(MeshProbe.V::z).max().orElseThrow()>83,"Raised window and upper cover missing");
  var bowl=MeshProbe.nearest(c.vertices,175,239);var buttonRim=MeshProbe.nearest(c.vertices,175,214);
  check(buttonRim.z()-bowl.z()>1.5&&buttonRim.z()<68,"Call key must be recessed below the casing face");
  check(MeshProbe.nearest(c.vertices,119,285).color()==0xaeb4a6,"Lower speaker opening is not covered by its matching arch plug");
  check(MeshProbe.nearest(c.vertices,25,6).z()-MeshProbe.nearest(c.vertices,6,6).z()>15,"Body top-left corner is square");
  for(double[] pair:new double[][]{{50,90,31,90},{180,90,201,90},{50,45,50,12},{50,155,50,188}})
   check(MeshProbe.nearest(c.vertices,pair[0],pair[1]).z()-MeshProbe.nearest(c.vertices,pair[2],pair[3]).z()>5,"Upper surface must roll down on all four sides");
  for(int row=0;row<6;row++)check(MeshProbe.nearest(c.vertices,75,206+row*13).color()==0x424b44,"Speaker opening is covered");
  int builds=GeometryCache.builds;Ht100sVisual.draw(new PoseStack(),t->new MeshProbe.Capture(),15728880);check(GeometryCache.builds==builds,"HT-100S mesh rebuilds each frame");
  for(var facing:new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST}){var state=Product.HT100S.get().defaultBlockState().setValue(WallMountedBlock.FACING,facing);var unit=new Ht100s.Unit(at,state);unit.setLevel(mc.level);var p=new PoseStack();Ht100sVisual.pose(unit,p);var mesh=new MeshProbe.Capture();Ht100sVisual.draw(p,t->mesh,15728880);var shape=state.getShape(mc.level,at).bounds();double minY=mesh.vertices.stream().mapToDouble(MeshProbe.V::y).min().orElseThrow(),maxY=mesh.vertices.stream().mapToDouble(MeshProbe.V::y).max().orElseThrow();check(Math.abs(maxY-minY-Ht100s.FRAME_HEIGHT)<.00001,"HT-100S frame/shape height differs");check(Math.abs(shape.maxY-shape.minY-Ht100s.FRAME_HEIGHT)<.00001,"HT-100S click height wrong");}
  check(mc.getBlockEntityRenderDispatcher().getRenderer((Ht100s.Unit)mc.level.getBlockEntity(at)) instanceof Ht100sVisual.Renderer,"HT-100S renderer missing");
  var item=new MeshProbe.Capture();Client.itemExtension().getCustomRenderer().renderByItem(new ItemStack(Product.HT100S_ITEM.get()),ItemDisplayContext.GUI,new PoseStack(),t->item,15728880,0);check(item.vertices.size()>500,"HT-100S inventory mesh invisible");
  System.out.println("HT100S_CLIENT_PASS: body 0.15 block, fitted tray, raised surfaces, four wall poses, cached renderer and inventory model; no images");return true;
 }
}
