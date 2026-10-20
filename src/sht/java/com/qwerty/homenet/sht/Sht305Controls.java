package com.qwerty.homenet.sht;

import com.qwerty.homenet.network.WallpadDataPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** Functional panel for the physical controls shown in the SHT-305V manual. */
final class Sht305Controls extends Screen {
 WallpadDataPacket data;
 EditBox message;
 final com.qwerty.homenet.client.kvm.KvmCameraView video=new com.qwerty.homenet.client.kvm.KvmCameraView();
 Sht305Controls(WallpadDataPacket packet){super(Component.literal("SAMSUNG SHT-305V · BUS 비디오폰"));data=packet;}
 @Override public boolean isPauseScreen(){return false;}
 void press(String key){Wire.send(data.pos(),false,"press",key);}
 void key(String label,String action,int x,int y,int w){addRenderableWidget(Button.builder(Component.literal(label),b->press(action)).bounds(x,y,w,19).build());}
 @Override protected void init(){
  int left=Math.max(12,(width-760)/2),top=49,right=left+300,col=Math.max(108,Math.min(150,(width-right-24)/2-5));
  key("수화기 들기 / 걸기","handset",left,top+220,245);
  String[][] controls={{"현관 보기","front"},{"문열림","door_open"},{"경비실","guard"},{"방범설정","security"},{"외출","away"},{"비상","emergency"},{"시험","test"},{"가스·화재 시험","gas_fire_test"},{"경보정지","stop"},{"복귀","restore"},{"화면 밝기","brightness"},{"화면 명암","contrast"},{"음량 +","up"},{"음량 -","down"}};
  for(int i=0;i<controls.length;i++)key(controls[i][0],controls[i][1],right+(i%2)*(col+6),top+(i/2)*28,col);
  message=new EditBox(font,left,height-57,Math.max(130,width-left-105),19,Component.literal("통화 메시지"));message.setMaxLength(80);message.setHint(Component.literal("통화 중 메시지 입력"));addRenderableWidget(message);
  addRenderableWidget(Button.builder(Component.literal("전송"),b->{Wire.send(data.pos(),false,"chat",message.getValue());message.setValue("");}).bounds(width-94,height-57,82,19).build());
 }
 @Override public void render(GuiGraphics g,int mx,int my,float dt){
  renderBackground(g);int left=Math.max(12,(width-760)/2),top=49;
  g.drawString(font,title,left,11,0xfff0e9d8);g.drawString(font,"BUS · "+data.unit()+" · "+data.setting("status","대기"),left,28,0xffd5d2c4);
  g.fill(left,top,left+245,top+207,0xffded6c1);g.fill(left+8,top+8,left+237,top+189,0xff1b1d24);
  if(data.flag("video")){
   try{var at=new BlockPos(Integer.parseInt(data.setting("vx","0")),Integer.parseInt(data.setting("vy","0")),Integer.parseInt(data.setting("vz","0")));
    video.draw(g,at,left+14,top+16,217,165,Integer.parseInt(data.setting("brightness","2")),Integer.parseInt(data.setting("contrast","2")),com.qwerty.homenet.video.VideoFormat.MONOCHROME);
   }catch(NumberFormatException ignored){}
  }
  g.drawString(font,"SHT-305V   VIDEO DOOR PHONE",left+12,top+194,0xff3d4148);
  String alarm=data.setting("alarm","");if(!alarm.isEmpty())g.drawString(font,"경보: "+alarm,left,top+250,0xffffb07e);
  g.drawString(font,"화재·가스 감지 입력은 본체의 센서 신호와 연동됩니다.",left,height-78,0xffc7c1b4);
  com.qwerty.homenet.client.IntercomConversation.draw(g,font,data,width,height-100);
  super.render(g,mx,my,dt);
 }
 @Override public boolean keyPressed(int key,int scan,int mods){if(message.isFocused()&&(key==257||key==335)){Wire.send(data.pos(),false,"chat",message.getValue());message.setValue("");return true;}return super.keyPressed(key,scan,mods);}
}
