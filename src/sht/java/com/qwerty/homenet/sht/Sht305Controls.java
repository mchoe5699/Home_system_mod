package com.qwerty.homenet.sht;

import com.qwerty.homenet.network.WallpadDataPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** BUS controls corresponding to the front-panel switches, with video and fire/gas testing. */
final class Sht305Controls extends Screen {
 WallpadDataPacket data;
 final com.qwerty.homenet.client.kvm.KvmCameraView video=new com.qwerty.homenet.client.kvm.KvmCameraView();
 Sht305Controls(WallpadDataPacket data){super(Component.literal("SAMSUNG SHT-305V"));this.data=data;}
 @Override public boolean isPauseScreen(){return false;}
 void button(String label,String action,int x,int y,int w){addRenderableWidget(Button.builder(Component.literal(label),b->Wire.send(data.pos(),false,"press",action)).bounds(x,y,w,20).build());}
 @Override protected void init(){int x=Math.max(12,(width-680)/2),y=56;
  button("수화기 들기 / 걸기","handset",x,y+225,254);
  String[][] keys={{"현관","front"},{"비상","emergency"},{"경비실","guard"},{"시험","test"},{"외출","away"},{"가스·화재 시험","gas_fire_test"},{"문열림","door_open"},{"방범설정","security"},{"경보정지","stop"},{"복귀","restore"}};
  for(int i=0;i<keys.length;i++)button(keys[i][0],keys[i][1],x+275+(i%2)*143,y+(i/2)*32,137);
  button("화면 밝기","brightness",x+275,y+174,137);button("화면 명암","contrast",x+418,y+174,137);
 }
 @Override public void render(GuiGraphics g,int mx,int my,float dt){renderBackground(g);int x=Math.max(12,(width-680)/2),y=56;
  g.drawString(font,title,x,14,0xfff0e8d7);g.drawString(font,"BUS · "+data.unit()+" · "+data.setting("status","대기"),x,33,0xffd6d1bf);
  g.fill(x,y,x+254,y+212,0xffe6ddca);g.fill(x+9,y+9,x+245,y+197,0xff191a20);
  if(data.flag("video"))try{BlockPos at=new BlockPos(Integer.parseInt(data.setting("vx","0")),Integer.parseInt(data.setting("vy","0")),Integer.parseInt(data.setting("vz","0")));video.draw(g,at,x+15,y+17,224,163,Integer.parseInt(data.setting("brightness","2")),Integer.parseInt(data.setting("contrast","2")),com.qwerty.homenet.video.VideoFormat.MONOCHROME);}catch(NumberFormatException ignored){}
  g.drawString(font,"SHT-305V · VIDEO DOOR PHONE",x+15,y+184,0xffc4c2c7);
  com.qwerty.homenet.client.IntercomConversation.draw(g,font,data,width,height-100);super.render(g,mx,my,dt);
 }
}
