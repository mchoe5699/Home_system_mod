package com.qwerty.homenet.sht;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
/** The model-only SHT-5500S remains model-only; this screen only stores its address. */
final class AddressControls extends Screen {
 final BlockPos pos;final String current;EditBox unit;
 AddressControls(BlockPos p,String n){super(Component.literal("삼성 동·호 설정"));pos=p;current=n;}
 protected void init(){unit=new EditBox(font,width/2-110,height/2-10,220,20,Component.literal("동-호"));unit.setMaxLength(32);unit.setValue(current);addRenderableWidget(unit);addRenderableWidget(Button.builder(Component.literal("동·호 저장"),b->Wire.send(pos,false,"unit",unit.getValue())).bounds(width/2-110,height/2+20,220,20).build());setInitialFocus(unit);}
 public boolean isPauseScreen(){return false;}
 public void render(GuiGraphics g,int x,int y,float dt){renderBackground(g);g.drawCenteredString(font,title,width/2,height/2-50,0xffffff);g.drawCenteredString(font,"동-호 (예: 101-1001)",width/2,height/2-30,0xdddddd);super.render(g,x,y,dt);}
}
