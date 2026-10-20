package com.qwerty.homenet.sht;

import com.mojang.blaze3d.vertex.PoseStack;
import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.guard.GuardSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.world.item.*;

/** Numeric, image-free checks of SHT-305V's exposed shell and BUS controls. */
final class Sht305Probe {
 static boolean world(Minecraft mc,BlockPos at){
  ClientRun.check(mc.level.getBlockEntity(at) instanceof Station305,"SHT-305V block entity missing");
  var base=(Station305)mc.level.getBlockEntity(at);
  ClientRun.check(Math.abs(Station305.Mounted.HEIGHT-.5)<1e-9,"SHT-305V is not half a block high");
  ClientRun.check(Math.abs(Station305.Mounted.WIDTH/Station305.Mounted.HEIGHT-660d/600)<1e-8,"SHT-305V width ratio wrong");
  ClientRun.check(base.guardSystem()==GuardSystem.BUS&&!base.supportsWirelessPortable(),"SHT-305V BUS or wired handset wrong");
  var docked=new MeshProbe.Capture();Sht305Visual.draw(new PoseStack(),t->docked,15728880,true);
  var empty=new MeshProbe.Capture();Sht305Visual.draw(new PoseStack(),t->empty,15728880,false);
  ClientRun.check(docked.vertices.size()>1000&&docked.vertices.size()>empty.vertices.size(),"SHT-305V receiver is not removable");
  ClientRun.check(docked.vertices.stream().anyMatch(v->v.x()<50&&v.z()>100),"Curved receiver face is missing");
  ClientRun.check(docked.vertices.stream().anyMatch(v->v.x()>630&&v.z()>85),"Exposed right side or control strip is missing");
  ClientRun.check(docked.vertices.stream().anyMatch(v->v.color()==0xf0ead7),"Ivory case color is missing");
  ClientRun.check(docked.vertices.stream().anyMatch(v->v.color()==0x747d90),"Small round control keys are missing");
  ClientRun.check(docked.vertices.stream().anyMatch(v->v.color()==0xf0ead8),"Shallow large keys are missing");
  for(Direction facing:new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST}){
   var state=Product.STATION305.get().defaultBlockState().setValue(WallMountedBlock.FACING,facing);
   var unit=new Station305(at,state);unit.setLevel(mc.level);var p=new PoseStack();Sht305Visual.pose(unit,p);var mesh=new MeshProbe.Capture();Sht305Visual.draw(p,t->mesh,15728880,true);
   double min=mesh.vertices.stream().mapToDouble(MeshProbe.V::y).min().orElseThrow(),max=mesh.vertices.stream().mapToDouble(MeshProbe.V::y).max().orElseThrow();
   ClientRun.check(Math.abs(max-min-.5)<.0001,"SHT-305V world pose is not half a block high");
  }
  ClientRun.check(mc.getBlockEntityRenderDispatcher().getRenderer(base) instanceof Sht305Visual.Renderer,"SHT-305V renderer missing");
  var item=new MeshProbe.Capture();Client.itemExtension().getCustomRenderer().renderByItem(new ItemStack(Product.BASE305_ITEM.get()),ItemDisplayContext.GUI,new PoseStack(),t->item,15728880,0);
  ClientRun.check(item.vertices.size()>1000,"SHT-305V item model is invisible");
  System.out.println("SHT305V_CLIENT_PASS: 0.5-block surface case, ivory, round controls, BUS, detachable fixed handset, four wall poses and inventory model; no images");
  return true;
 }
}
