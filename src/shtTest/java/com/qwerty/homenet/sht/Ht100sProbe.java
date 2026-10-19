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
  check(c.vertices.stream().mapToDouble(MeshProbe.V::z).max().orElseThrow()>81,"Raised window and upper cover missing");
  check(Math.abs(Ht100sVisual.WINDOW_W/Ht100sVisual.W-108d/230)<.01&&Math.abs(Ht100sVisual.WINDOW_H/Ht100sVisual.H-108d/300)<.01,"Dark panel proportions differ from the reference photo");
  check(c.vertices.stream().anyMatch(v->Math.abs(v.x()-115)<.01&&Math.abs(v.y()-550)<.01&&Math.abs(v.z())<.001&&v.color()==0x9da294),"The rear mounting frame is open");
  check(c.vertices.stream().filter(v->v.color()==0x8e9589).count()>=188,"Upper moulding seam is incomplete");
  check(c.vertices.stream().noneMatch(v->v.color()==0x8e9589&&v.x()>70&&v.x()<160&&v.y()>689),"Unwanted seam crosses above the logo");
  for(double x:new double[]{21,209})
   check(c.vertices.stream().anyMatch(v->v.color()==0x8e9589&&Math.abs(v.x()-x)<.6&&Math.abs(v.y()-700)<.01&&Math.abs(v.z()-59.18)<.02),"Upper seam stops before the casing top edge");
  for(double[] xy:new double[][]{{209,691},{209,529},{41,509},{21,691}})
   check(c.vertices.stream().anyMatch(v->v.color()==0x8e9589&&Math.abs(v.x()-xy[0])<.02&&Math.abs(v.y()-xy[1])<.02),"Upper moulding seam misses a rounded corner");
  for(double x:new double[]{21,209})for(double y:new double[]{529,691})
   check(c.vertices.stream().anyMatch(v->v.color()==0x8e9589&&Math.abs(v.x()-x)<.02&&Math.abs(v.y()-y)<.02),"Upper seam bends instead of rising straight");
  check(Math.abs(Ht100sVisual.WINDOW_X+Ht100sVisual.WINDOW_W/2+Ht100sVisual.FRONT_OFFSET_X-Ht100sVisual.W/2)<.001,"Dark panel is off centre");
  check(Math.abs((26+210)/2f+Ht100sVisual.FRONT_OFFSET_X-Ht100sVisual.W/2)<.001,"Speaker and button group is off centre");
  var bowl=MeshProbe.nearest(c.vertices,175,239);var buttonRim=MeshProbe.nearest(c.vertices,175,214);
  check(buttonRim.z()-bowl.z()>1.5&&buttonRim.z()<68,"Call key must be recessed below the casing face");
  check(MeshProbe.nearest(c.vertices,119,285).color()==0xaeb4a6,"Lower speaker opening is not covered by its matching arch plug");
  check(c.vertices.stream().anyMatch(v->Math.abs(v.x()-9)<.01&&Math.abs(v.y()-678)<.01&&Math.abs(v.z()-68)<.01),"Body top-left corner has no rounded arc");
  check(c.vertices.stream().noneMatch(v->Math.abs(v.x()-9)<.01&&Math.abs(v.y()-691)<.01&&Math.abs(v.z()-68)<.01),"Body top-left corner is square");
  for(double z:new double[]{35,59})for(double x:new double[]{0,230})
   check(c.vertices.stream().anyMatch(v->Math.abs(v.x()-x)<.01&&Math.abs(v.z()-z)<.01),"Casing side wall must stay flat before its rounded front edge");
  double arcInset=9*(1-Math.cos(Math.PI/4)),arcZ=59+9*Math.sin(Math.PI/4);
  check(c.vertices.stream().anyMatch(v->Math.abs(v.x()-arcInset)<.01&&Math.abs(v.z()-arcZ)<.01),"Casing front edge does not curve into the face");
  check(Math.abs(Ht100sVisual.upperZ(116,9)-68)<.001,"Upper moulding does not begin at the top edge");
  check(Ht100sVisual.upperZ(116,10)-Ht100sVisual.upperZ(116,9)<.1,"Upper moulding begins with a sharp bend");
  check(Ht100sVisual.upperZ(118,33)>79&&Ht100sVisual.upperZ(118,33)<80.1,"Upper moulding no longer rises smoothly from the top edge");
  for(float[] corners:new float[][]{{24,9,57,35},{212,9,179,35},{24,191,57,159},{212,191,179,159}}){
   float last=Ht100sVisual.upperZ(corners[0],corners[1]);
   check(Math.abs(last-68)<.001,"Outer moulding corner is lifted off the casing");
   for(int i=1;i<=4;i++){float t=i/4f,z=Ht100sVisual.upperZ(corners[0]+(corners[2]-corners[0])*t,corners[1]+(corners[3]-corners[1])*t);
    check(z>last&&z<=80.001,"Corner slope does not flow toward the window");last=z;}
  }
  check(Math.abs(Ht100sVisual.upperZ(64,43)-80)<.001&&Math.abs(Ht100sVisual.upperZ(172,151)-80)<.001,"Window has no broad raised surround");
  check(Ht100sVisual.upperZ(45.5f,27.5f)<Ht100sVisual.upperZ(45.5f,100),"Raised corner forms a bulge instead of a window-facing slope");
  check(Math.abs(Ht100sVisual.upperZ(116,191)-68)<.001,"Raised panel lower edge did not move up with the panel");
  check(MeshProbe.nearest(c.vertices,16,150).z()<70,"Raised strip still obstructs the upper moulding at left");
  for(double[] pair:new double[][]{{15,150,2,150},{215,150,228,150},{115,15,115,2},{115,285,115,298}})
   check(MeshProbe.nearest(c.vertices,pair[0],pair[1]).z()-MeshProbe.nearest(c.vertices,pair[2],pair[3]).z()>3,"Casing edge does not roll toward its mounting frame");
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
