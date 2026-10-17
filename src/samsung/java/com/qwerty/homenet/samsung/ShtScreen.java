package com.qwerty.homenet.samsung;
import com.qwerty.homenet.network.WallpadDataPacket;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.*;
public final class ShtScreen extends Screen {
 private WallpadDataPacket data;private final boolean remote;private EditBox chat,setting;private String edit="logo";private float scale;private int left,top;private boolean shortcutChar;
 private final com.qwerty.homenet.client.kvm.KvmCameraView video=new com.qwerty.homenet.client.kvm.KvmCameraView();
 public ShtScreen(WallpadDataPacket d,boolean remote){super(Component.literal(remote?"SHT-101C 무선수화기":"SAMSUNG SHT-101C"));data=d;this.remote=remote;}
 public BlockPos getPos(){return data.pos();}public void update(WallpadDataPacket d){data=d;}public boolean isPauseScreen(){return false;}
 private void send(String key,String value){ShtNetwork.send(getPos(),remote,key,value);}private void press(String name){send("button",name);}
 protected void init(){int convo=com.qwerty.homenet.client.IntercomConversation.width(width);scale=Math.min((width-convo-45)/(remote?300f:1000f),(height-170)/740f);left=(int)((width-convo-(remote?240:1000)*scale)/2);top=58;
  chat=new EditBox(font,12,height-92,width-96,18,Component.literal("통화 메시지"));chat.setMaxLength(80);chat.setHint(Component.literal("T 또는 클릭: 메시지 입력 · Enter 전송"));addRenderableWidget(chat);
  addRenderableWidget(Button.builder(Component.literal("전송"),b->{send("message",chat.getValue());chat.setValue("");}).bounds(width-78,height-92,66,18).build());
  String[] names=remote?new String[]{"통화","경비실","문/열림","호출","채널"}:new String[]{"수화기 들기 / 걸기","BUS / STAR","안테나 접기","암호 일치 확인"};String[] actions=remote?new String[]{"talk","guard","door","intercom","channel"}:new String[]{"take","mode","antenna","pair"};int w=(width-24)/names.length;
  for(int i=0;i<names.length;i++){String action=actions[i];addRenderableWidget(Button.builder(Component.literal(names[i]),b->press(action)).bounds(12+i*w,height-68,w-3,18).build());}
  if(!remote){setting=new EditBox(font,12,height-44,Math.max(50,width-245),18,Component.literal("설정값"));setting.setMaxLength(32);setting.setHint(Component.literal("로고 또는 STAR 동·호 번호"));addRenderableWidget(setting);
   addRenderableWidget(Button.builder(Component.literal("로고 변경"),b->send("logo",setting.getValue())).bounds(width-225,height-44,98,18).build());addRenderableWidget(Button.builder(Component.literal("STAR 동·호 설정"),b->send("unit",setting.getValue())).bounds(width-122,height-44,110,18).build());}
 }
 public void render(GuiGraphics g,int mx,int my,float partial){renderBackground(g);g.drawString(font,title,12,8,0xffeeeeee);g.drawString(font,font.plainSubstrByWidth(data.setting("status",""),width-24),12,22,0xffd7c799);g.drawString(font,data.setting("mode","BUS")+" · 세대 "+data.unit()+(remote?" · 같은 단지 / 32블록":" · 흑백 CRT")+(data.flag("zoned")?"":" · 단지 미지정"),12,36,0xffd6dfd0);
  g.pose().pushPose();g.pose().translate(left,top,0);g.pose().scale(scale,scale,1);if(remote)g.blit(Samsung.id("textures/handset.png"),0,0,0,0,240,680,240,680);else{g.blit(Samsung.id(data.flag("docked")?"textures/gui_docked.png":"textures/gui_empty.png"),0,0,0,0,1000,740,1000,740);String logo=data.setting("product_logo_text","SAMSUNG");if(!logo.equals("SAMSUNG")){g.fill(254,86,428,116,0xfff3efdd);g.drawString(font,logo,265,96,0xff3d4942,false);}if(data.flag("camera")){BlockPos at=new BlockPos(Integer.parseInt(data.setting("camera_x","0")),Integer.parseInt(data.setting("camera_y","0")),Integer.parseInt(data.setting("camera_z","0")));video.draw(g,at,578,124,224,193,Integer.parseInt(data.setting("brightness","2")),Integer.parseInt(data.setting("contrast","2")),com.qwerty.homenet.video.VideoFormat.MONOCHROME);}for(int[] a:new int[][]{{440,232},{440,210},{440,245},{440,290},{440,513},{440,558},{440,603}}){boolean on=a[1]==232||a[1]==210&&data.flag("charging")||a[1]==245&&data.flag("armed")||a[1]==290&&data.flag("away")||a[1]==513&&data.flag("guard_lamp")||a[1]==558&&data.flag("door_lamp")||a[1]==603&&data.flag("speaker_lamp");if(on)g.fill(a[0],a[1],a[0]+12,a[1]+4,0xffa4c877);}}
  g.pose().popPose();com.qwerty.homenet.client.IntercomConversation.draw(g,font,data,width,height-110);super.render(g,mx,my,partial);
  if(!remote){float u=(mx-left)/scale,v=(my-top)/scale;for(var k:ShtMesh.keys())if(Math.abs(u-k.x())<k.w()/2+3&&Math.abs(v-k.y())<k.h()/2+5)g.renderTooltip(font,Component.literal(k.label()),mx,my);}
 }
 public boolean mouseClicked(double x,double y,int button){if(super.mouseClicked(x,y,button))return true;if(button!=0)return false;setFocused(null);double u=(x-left)/scale,v=(y-top)/scale;
  if(remote){if(u<0||u>240)return false;if(v>=205&&v<=235){press("talk");return true;}for(int r=0;r<4;r++)for(int c=0;c<3;c++)if(Math.hypot(u-(65+c*55),v-(290+r*48))<21){send("key","123456789*0#".substring(r*3+c,r*3+c+1));return true;}String[][] names={{"channel","memory","redial"},{"intercom","door","guard"}};for(int r=0;r<2;r++)for(int c=0;c<3;c++)if(Math.abs(u-(65+c*55))<25&&Math.abs(v-(490+r*32))<13){press(names[r][c]);return true;}}
  else{for(var k:ShtMesh.keys())if(Math.abs(u-k.x())<k.w()/2+2&&Math.abs(v-k.y())<k.h()/2+4){if(k.name().startsWith("digit_"))send("key",k.name().substring(6));else press(k.name());return true;}if(u>=86&&u<=234&&v>96&&v<667){press("take");return true;}}
  return false;
 }
 public boolean keyPressed(int key,int scan,int mods){if(!chat.isFocused()&&(setting==null||!setting.isFocused())&&(key==GLFW.GLFW_KEY_T||key==GLFW.GLFW_KEY_ENTER)){setFocused(chat);chat.setFocused(true);shortcutChar=key==GLFW.GLFW_KEY_T;return true;}if(chat.isFocused()&&(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER)){send("message",chat.getValue());chat.setValue("");return true;}return super.keyPressed(key,scan,mods);}
 public boolean charTyped(char c,int mods){if(shortcutChar){shortcutChar=false;if(c=='t'||c=='T')return true;}if(!chat.isFocused()&&(setting==null||!setting.isFocused())&&"0123456789*#".indexOf(c)>=0){send("key",""+c);return true;}return super.charTyped(c,mods);}
}
