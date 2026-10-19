package com.qwerty.homenet.sht;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.qwerty.homenet.block.WallMountedBlock;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;

/** Photo-proportioned HT-100 body and an independent removable surround. */
final class Ht100Visual {
 static final float W=228,H=300,SCALE=(float)(Ht100.HEIGHT/H);
 static final ResourceLocation DETAIL=Product.id("ht100/detail.png"),LIGHT=Product.id("ht100/light.png");
 static void solid(PoseStack p,MultiBufferSource o,int l,String name,float x,float y,float w,float h,float z,float d,float radius,String color,float bow){Surface.solid(new Surface.Solid(name,x,y,w,h,z,d,radius,color,"ht100",bow),p,o,l);}
 static void line(PoseStack p,MultiBufferSource o,int l,float x,float y,float w,float h,float z,int color){Surface.patch(p,o,x,y,w,h,z,color,l);}
 static void mesh(PoseStack p,MultiBufferSource o,boolean frame){int l=0;
  // The old photographed outer mounting shell is a distinct part behind the clean body.
  if(frame){solid(p,o,l,"ht100_frame_back",-6,-6,240,312,0,14,13,"#c9c9bc",0);
   solid(p,o,l,"ht100_frame_lip",-4,-4,236,308,14,5,11,"#b7beb9",0);}
  solid(p,o,l,"ht100_rear",0,0,W,H,frame?12:0,21,15,"#aeb5b2",.8f);
  solid(p,o,l,"ht100_face",8,4,W-16,H-8,32,5,13,"#b9c1bd",1.4f);
  // Thin inset line outlines the upper fascia, including the straight cross seam.
  line(p,o,l,18,197,192,1,37.6f,0x858f8d);
  solid(p,o,l,"ht100_upper",19,9,190,186,36.6f,1.4f,10,"#bac2be",.25f);
  solid(p,o,l,"ht100_glass_rim",56,51,116,116,37,1.1f,4,"#626c6c",0);
  solid(p,o,l,"ht100_glass",60,55,108,108,38.1f,.25f,2,"#2a363c",0);
  // The upper lamp and concentric lens are separate from the dark glass.
  solid(p,o,l,"ht100_lamp",71,64,86,39,38.4f,.35f,1.3f,"#15334a",0);
  solid(p,o,l,"ht100_lens_outer",88,111,52,52,38.4f,.9f,26,"#161b25",0);
  solid(p,o,l,"ht100_lens_ring",94,117,40,40,39.3f,.7f,20,"#342133",0);
  solid(p,o,l,"ht100_lens_inner",99,122,30,30,40,1.2f,15,"#241320",1.2f);
  solid(p,o,l,"ht100_lens_pupil",107,130,14,14,41.2f,.35f,7,"#070b11",0);
  // Six long horizontal slits follow the photographed taper toward the right edge.
  for(int i=0;i<6;i++){float y=213+i*12.1f,w=94-i*.55f;solid(p,o,l,"ht100_slot_"+i,24,y,w,3.7f,38.65f,.4f,1.7f,"#35433f",0);line(p,o,l,27,y+3.1f,w-8,.45f,39.15f,0xa1aaa5);}
  solid(p,o,l,"ht100_button_recess",132,207,73,72,36.8f,.5f,7,"#858f8e",0);
  solid(p,o,l,"ht100_button",135,210,67,66,37.3f,2.4f,6,"#bfc7c3",.6f);
  // Separate two-slot microphone, press-window, and bottom speaker-hole plug.
  for(int i=0;i<2;i++)solid(p,o,l,"ht100_mic_"+i,209,36+i*12,6,3.2f,38.8f,.25f,1.2f,"#253034",0);
  solid(p,o,l,"ht100_button_window",166,215,12,4,39.7f,.2f,2,"#293a40",0);
  solid(p,o,l,"ht100_grille_plug",110,282,20,18,37.1f,1.2f,5,"#a9b2af",.4f);
  line(p,o,l,113,285,14,1,38.35f,0x414d4b);
  // Transparent printed logo, product caption and hairline panel details.
  Surface.rectangle(p,o,0,0,W,H,42,DETAIL,l);
 }
 static void draw(PoseStack p,MultiBufferSource o,int light,boolean frame){GeometryCache.draw(frame?"ht100_frame":"ht100_plain",p,o,light,(q,b)->mesh(q,b,frame));}
 static void pose(Ht100.Unit b,PoseStack p){var offset=com.qwerty.homenet.move.DeviceOffsets.get(b.getLevel(),b.getBlockPos());p.translate(offset.x,offset.y,offset.z);double bottom=.5-Ht100.HEIGHT/2;switch(b.getBlockState().getValue(WallMountedBlock.FACING)){case NORTH->{p.translate(.5,bottom,1);p.mulPose(Axis.YP.rotationDegrees(180));}case SOUTH->p.translate(.5,bottom,0);case EAST->{p.translate(0,bottom,.5);p.mulPose(Axis.YP.rotationDegrees(90));}case WEST->{p.translate(1,bottom,.5);p.mulPose(Axis.YP.rotationDegrees(-90));}default->{}}p.scale(SCALE,SCALE,SCALE);p.translate(-W/2,H-700,0);}
 static class Renderer implements BlockEntityRenderer<Ht100.Unit>{public void render(Ht100.Unit b,float time,PoseStack p,MultiBufferSource out,int light,int overlay){boolean framed=b.getBlockState().getValue(Ht100.FRAMED);String key=framed?"ht100_frame":"ht100_plain";if(DeviceVisibility.hidden(b,GeometryCache.bounds(key),q->pose(b,q)))return;p.pushPose();pose(b,p);draw(p,out,light,framed);if(b.getBlockState().getValue(Ht100.LIT))Surface.rectangle(p,out,71,64,86,39,42.05f,LIGHT,15728880);p.popPose();}}
}
