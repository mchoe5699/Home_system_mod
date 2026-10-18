package com.qwerty.homenet.sht;
import com.qwerty.homenet.network.WallpadDataPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
/** The manual's audio-only portable has its own face and hit regions; it has no video display. */
final class PortableControls extends Controls {
 PortableControls(WallpadDataPacket d){super(d,true);}
 @Override net.minecraft.resources.ResourceLocation texture(String p){return Product.id("portable102/"+p);}
 @Override public boolean mouseClicked(double mx,double my,int button){if(super.mouseClicked(mx,my,button))return true;if(button!=0)return false;PortableVisual.load();double u=(mx-x)/scale,v=(my-y)/scale;for(var k:PortableVisual.keys)if(Math.abs(u-k.x())<=k.w()/2+2&&Math.abs(v-k.y())<=k.h()/2+3){if(k.name().startsWith("r")&&k.name().length()==2)send("digit",k.name().substring(1));else send("press",k.name());return true;}return false;}
 @Override public void render(GuiGraphics g,int mx,int my,float dt){super.render(g,mx,my,dt);int battery=0;boolean power=false;if(minecraft.player!=null)for(int i=0;i<minecraft.player.getInventory().getContainerSize();i++){var s=minecraft.player.getInventory().getItem(i);if(s.is(Product.PORTABLE102.get())&&s.hasTag()&&s.getTag().getLong("base")==data.pos().asLong()){battery=s.getTag().getInt("battery");power=s.getTag().getBoolean("power");break;}}g.drawString(font,(power?"전원 ON":"전원 OFF")+" · 배터리 "+Math.round(battery*100f/Portable.CAPACITY)+"%",12,39,0xffddddb8);if(data.flag("portable_active")||java.util.Set.of("1","3").contains(data.setting("intercom","0"))){int lx=x+(int)(76*scale),ly=y+(int)(425*scale);g.fill(lx,ly,lx+Math.max(2,(int)(8*scale)),ly+Math.max(1,(int)(3*scale)),0xffa0ef7a);}}
}
