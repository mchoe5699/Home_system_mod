package com.qwerty.homenet.sht;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.qwerty.homenet.block.WallMountedBlock;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;

/** HT-100S photo reconstruction in 230 x 300 body units. The rear mounting tray and phone share one mesh. */
final class Ht100sVisual {
 static final float W=230,H=300,S=(float)(Ht100s.HEIGHT/H);
 static final ResourceLocation LOGO=Product.id("ht100s/logo.png"),BRAND=Product.id("ht100s/brand.png");
 static void solid(PoseStack p,MultiBufferSource o,int l,String name,float x,float y,float w,float h,float z,float d,float r,int c){Surface.solid(new Surface.Solid(name,x,y,w,h,z,d,r,String.format("#%06x",c),"ht100s",0),p,o,l);}
 static void patch(PoseStack p,MultiBufferSource o,int l,float x,float y,float w,float h,float z,int c){Surface.patch(p,o,x,y,w,h,z,c,l);}
 static void arc(PoseStack p,MultiBufferSource o,int l,float cx,float cy,float outer,float inner,float z0,float z1,int color){
  var b=o.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));
  for(int i=0;i<48;i++){float a=(float)(i*Math.PI/24),n=(float)((i+1)*Math.PI/24);for(int k=0;k<4;k++){float t=(k==0||k==3?a:n),rad=(k<2?outer:inner),z=(k<2?z0:z1);Surface.vertex(b,p.last(),cx+(float)Math.cos(t)*rad,cy+(float)Math.sin(t)*rad,z,0,0,1,color,l,.5f,.5f);}}
 }
 static void mesh(PoseStack p,MultiBufferSource o){int l=0;
  // A separate physical surround is fitted behind the body, retained as part of one placeable product.
  solid(p,o,l,"tray_back",-6,-5,242,310,0,21,13,0x858779);
  solid(p,o,l,"tray_side",-6,-5,242,310,20,21,13,0xabb0a1);
  solid(p,o,l,"tray_lip",-5,-4,240,308,40,4,13,0xc6c8b9);
  solid(p,o,l,"tray_reveal",-1,0,232,300,43,2,15,0x555b52);
  // The handset shell rolls inward from its sidewall to the face. Three steps describe the deep shoulder.
  solid(p,o,l,"body_side",0,0,230,300,44,11,16,0x8b9083);
  solid(p,o,l,"body_shoulder",0,0,230,300,55,8,16,0xaeb1a5);
  solid(p,o,l,"body_front",2,2,226,296,63,5,15,0xc5c8bb);
  solid(p,o,l,"body_crown",6,4,218,290,68,2,14,0xd2d3c6);
  // Raised upper moulding has a distinct rounded left rail, a convex centre, and a dark perimeter seam.
  solid(p,o,l,"upper_seam",22,4,189,193,69,1,15,0x555b53);
  solid(p,o,l,"upper_roll",23,5,187,190,70,6,15,0xa5aa9b);
  solid(p,o,l,"upper_face",25,7,183,185,76,3,15,0xc9cbbd);
  solid(p,o,l,"upper_inner",31,10,174,178,79,2,14,0xd3d5c8);
  solid(p,o,l,"left_rail",7,5,22,288,69,7,11,0xc4c8b9);
  solid(p,o,l,"left_rail_highlight",9,7,16,284,76,1.5f,8,0xd9dacb);
  solid(p,o,l,"right_rail",207,6,17,286,69,7,9,0xb7bcae);
  solid(p,o,l,"right_rail_highlight",209,8,10,281,76,1.5f,6,0xd2d5c7);
  // The model has a dark featureless window: unlike HT-100, there is no circular camera lens.
  solid(p,o,l,"window_reveal",64,47,106,105,81,1.5f,3,0x616863);
  solid(p,o,l,"window_edge",65,48,104,103,82.5f,1,2,0x272e2e);
  solid(p,o,l,"window_glass",67,50,100,99,83.5f,.4f,1,0x303638);
  patch(p,o,l,72,53,87,2,84,0x3d4344);
  // Two short microphone openings on the narrow right margin.
  for(int i=0;i<2;i++)solid(p,o,l,"mic_"+i,212,37+i*8,9,3.2f,77,1,.8f,0x182126);
  // Six real long speaker openings, with bright bevels and dark recessed floors.
  for(int i=0;i<6;i++){float y=205+i*13;solid(p,o,l,"speaker_bevel_"+i,27,y,101,6,70,1.2f,2.8f,0xe0dfd0);solid(p,o,l,"speaker_shadow_"+i,28,y+1,99,3.8f,71.25f,.4f,1.9f,0x303a37);patch(p,o,l,31,y+4.2f,92,.55f,71.8f,0xaeb4a6);}
  // Button face, lip and the photograph's pronounced concave bowl.
  solid(p,o,l,"button_shadow",139,201,72,74,69,1,6,0x555d55);
  solid(p,o,l,"button_lip",140,202,70,72,70,3,6,0xdbddce);
  solid(p,o,l,"button_face",142,204,66,68,73,1.3f,5,0xbfc4b6);
  arc(p,o,l,175,239,25,20,74.4f,72.7f,0xa9afa1);
  arc(p,o,l,175,239,20,14,72.7f,71.2f,0xb3b9aa);
  arc(p,o,l,175,239,14,7,71.2f,70.8f,0xbcc2b3);
  arc(p,o,l,175,239,7,0,70.8f,70.8f,0xc6cabb);
  solid(p,o,l,"button_indicator",169,209,13,5,74.4f,.5f,2,0x222b2e);
  // Bottom centre cable/fastener cutout is dark and inset, not a raised tab.
  solid(p,o,l,"bottom_notch",107,275,25,25,69,.8f,12,0x373c39);
  solid(p,o,l,"notch_plug",110,278,19,20,70,1,9,0x777e72);
  for(int i=0;i<2;i++)solid(p,o,l,"notch_hole_"+i,113+i*9,278,4,5,71,1,2,0x171e1d);
  // Logo and Samsung inscription are printed on their own plane, leaving moulding beneath visible.
  for(float[] q:new float[][]{{68,19,109,25,82},{146,278,64,14,72}}){float x=q[0],y=q[1],w=q[2],h=q[3],z=q[4];var b=o.getBuffer(RenderType.entityCutoutNoCull(y<100?LOGO:BRAND));float[][] v={{x,y+h,0,1},{x+w,y+h,1,1},{x+w,y,1,0},{x,y,0,0}};for(var a:v)Surface.vertex(b,p.last(),a[0],a[1],z,0,0,1,0xffffff,l,a[2],a[3]);}
 }
 static void draw(PoseStack p,MultiBufferSource o,int light){GeometryCache.draw("ht100s_photo",p,o,light,Ht100sVisual::mesh);}
 static void pose(Ht100s.Unit b,PoseStack p){var off=com.qwerty.homenet.move.DeviceOffsets.get(b.getLevel(),b.getBlockPos());p.translate(off.x,off.y,off.z);double bottom=.5-Ht100s.HEIGHT/2;switch(b.getBlockState().getValue(WallMountedBlock.FACING)){case NORTH->{p.translate(.5,bottom,1);p.mulPose(Axis.YP.rotationDegrees(180));}case SOUTH->p.translate(.5,bottom,0);case EAST->{p.translate(0,bottom,.5);p.mulPose(Axis.YP.rotationDegrees(90));}case WEST->{p.translate(1,bottom,.5);p.mulPose(Axis.YP.rotationDegrees(-90));}default->{}}p.scale(S,S,S);p.translate(-W/2,H-700,0);}
 static final class Renderer implements BlockEntityRenderer<Ht100s.Unit>{public void render(Ht100s.Unit b,float tick,PoseStack p,MultiBufferSource o,int light,int overlay){if(DeviceVisibility.hidden(b,GeometryCache.bounds("ht100s_photo"),q->pose(b,q)))return;p.pushPose();pose(b,p);draw(p,o,light);p.popPose();}}
}
