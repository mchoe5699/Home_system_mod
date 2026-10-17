package com.qwerty.homenet.sht;
import com.qwerty.homenet.network.WallpadDataPacket;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import org.lwjgl.glfw.GLFW;
public class Controls extends Screen {
 static final int BASE_X=74,BASE_Y=54,BASE_W=896,BASE_H=653,BASE_ATLAS_H=720;
 public WallpadDataPacket data;boolean radio;EditBox text,setting;float scale;int x,y;boolean ignoreT;
 final com.qwerty.homenet.client.kvm.KvmCameraView video=new com.qwerty.homenet.client.kvm.KvmCameraView();
 Controls(WallpadDataPacket d,boolean r){super(Component.literal(r?"SHT-101C 무선수화기":"SAMSUNG SHT-101C"));data=d;radio=r;}
 public boolean isPauseScreen(){return false;}
 void send(String a,String v){Wire.send(data.pos(),radio,a,v);}
 protected void init(){int usable=width-com.qwerty.homenet.client.IntercomConversation.width(width)-30;scale=Math.min(usable/(radio?240f:(float)BASE_W),(height-155)/(radio?950f:(float)BASE_H));x=(int)((usable-(radio?240:BASE_W)*scale)/2);y=52;
  text=new EditBox(font,12,height-84,width-94,18,Component.literal("통화 메시지"));text.setMaxLength(80);text.setHint(Component.literal("T: 메시지 입력 · Enter: 전송"));addRenderableWidget(text);addRenderableWidget(Button.builder(Component.literal("전송"),b->{send("chat",text.getValue());text.setValue("");}).bounds(width-76,height-84,64,18).build());
  if(!radio){String[] labels={"수화기 들기 / 걸기","BUS / STAR","안테나 접기 / 펴기"};String[] actions={"handset","mode","antenna"};for(int i=0;i<3;i++){String a=actions[i];addRenderableWidget(Button.builder(Component.literal(labels[i]),b->send("press",a)).bounds(12+i*(width-24)/3,height-61,(width-24)/3-3,18).build());}setting=new EditBox(font,12,height-38,width-235,18,Component.literal("설정값"));setting.setMaxLength(32);addRenderableWidget(setting);addRenderableWidget(Button.builder(Component.literal("로고"),b->send("logo",setting.getValue())).bounds(width-213,height-38,80,18).build());addRenderableWidget(Button.builder(Component.literal("STAR 동·호"),b->send("unit",setting.getValue())).bounds(width-128,height-38,116,18).build());}
 }
 public void render(GuiGraphics g,int mx,int my,float dt){renderBackground(g);g.drawString(font,title,12,8,0xffeeeeee);g.drawString(font,data.setting("mode","BUS")+" · "+data.unit()+" · "+data.setting("status","대기"),12,25,0xffe3dfb8);g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(scale,scale,1);int ox=radio?0:BASE_X,oy=radio?0:BASE_Y;g.blit(Product.id(radio?"textures/radio_gui.png":data.flag("docked")?"textures/base_docked.png":"textures/base_empty.png"),0,0,ox,oy,radio?240:BASE_W,radio?950:BASE_H,1042,radio?950:BASE_ATLAS_H);
  if(!radio){if(data.flag("video")){var at=new BlockPos(Integer.parseInt(data.setting("vx","0")),Integer.parseInt(data.setting("vy","0")),Integer.parseInt(data.setting("vz","0")));video.draw(g,at,578-ox,174-oy,192,144,Integer.parseInt(data.setting("brightness","2")),Integer.parseInt(data.setting("contrast","2")),com.qwerty.homenet.video.VideoFormat.MONOCHROME);}String logo=data.setting("product_logo_text","SAMSUNG");if(!logo.equals("SAMSUNG")){g.fill(306-ox,131-oy,449-ox,157-oy,0xfff1efdf);g.drawString(font,logo,310-ox,137-oy,0xff536050,false);}int lights=Integer.parseInt(data.setting("lights","0"));for(var lamp:Indicators.LAMPS)if((lights&lamp.bit())!=0){
   int color=0xff000000|lamp.rgb();
   if(lamp.round()){
    for(int row=0;row<lamp.h();row++){double dy=(row+.5-lamp.h()/2d)/(lamp.h()/2d);int half=(int)Math.round(lamp.w()/2d*Math.sqrt(Math.max(0,1-dy*dy)));g.fill(lamp.x()+lamp.w()/2-half-ox,lamp.y()+row-oy,lamp.x()+lamp.w()/2+half-ox,lamp.y()+row+1-oy,color);}
    g.blit(Product.id("textures/base_ink.png"),lamp.x()-ox,lamp.y()-oy,lamp.x(),lamp.y(),lamp.w(),lamp.h(),1042,BASE_ATLAS_H);
   }else g.fill(lamp.x()-ox,lamp.y()-oy,lamp.x()+lamp.w()-ox,lamp.y()+lamp.h()-oy,color);
  }}
  g.pose().popPose();com.qwerty.homenet.client.IntercomConversation.draw(g,font,data,width,height-100);super.render(g,mx,my,dt);}
 public boolean mouseClicked(double mx,double my,int b){if(super.mouseClicked(mx,my,b))return true;if(b!=0)return false;setFocused(null);double u=(mx-x)/scale+(radio?0:BASE_X),v=(my-y)/scale+(radio?0:BASE_Y);Surface.load();for(var k:Surface.buttons)if(k.group().equals(radio?"radio":"base")&&Math.abs(u-k.x())<=k.w()/2+2&&Math.abs(v-k.y())<=k.h()/2+3){if((k.name().startsWith("n")||k.name().startsWith("r"))&&k.name().length()==2)send("digit",k.name().substring(1));else send("press",k.name());return true;}if(!radio&&u>=145&&u<=296&&v>=126&&v<=640){send("press","handset");return true;}return false;}
 public boolean keyPressed(int key,int scan,int mods){if(text.isFocused()&&(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER)){send("chat",text.getValue());text.setValue("");return true;}if(!text.isFocused()&&(setting==null||!setting.isFocused())&&(key==GLFW.GLFW_KEY_T||key==GLFW.GLFW_KEY_ENTER)){setFocused(text);text.setFocused(true);ignoreT=key==GLFW.GLFW_KEY_T;return true;}return super.keyPressed(key,scan,mods);}
 public boolean charTyped(char c,int mods){if(ignoreT){ignoreT=false;if(c=='t'||c=='T')return true;}if(!text.isFocused()&&(setting==null||!setting.isFocused())&&"123456789*0#".indexOf(c)>=0){send("digit",""+c);return true;}return super.charTyped(c,mods);}
}
