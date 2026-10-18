package com.qwerty.homenet.sht;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Modal help over the existing controller: its packets, tick and call session stay alive. */
@Mod.EventBusSubscriber(modid=Product.ID,value=Dist.CLIENT)
public final class UsageHelp {
 private UsageHelp(){}
 static final Set<String> SCREENS=Set.of("WallpadScreen","DoorStationScreen","Am10kpScreen","GuardMasterScreen","Ha4100hConfigScreen","Hmc7000Screen","Ip203apScreen","Kam305Screen","Klp203rfScreen","Kvm906Screen","LobbyPhoneScreen","KhnWallpadScreen","DeviceConfigScreen");
 private static final Map<Screen,State> STATES=new WeakHashMap<>();
 static final class State { boolean open;int scroll,max;Button button;List<FormattedCharSequence> lines=List.of();int wrap=-1; }
 static boolean supports(Screen s){return s instanceof Controls||s instanceof AddressControls||s.getClass().getName().startsWith("com.qwerty.homenet.client.")&&SCREENS.contains(s.getClass().getSimpleName());}
 static State state(Screen s){return STATES.get(s);}
 @SubscribeEvent public static void init(ScreenEvent.Init.Post e){var s=e.getScreen();if(!supports(s))return;var st=STATES.computeIfAbsent(s,k->new State());st.wrap=-1;st.button=Button.builder(Component.literal("사용법"),b->{st.open=true;st.scroll=0;}).bounds(Math.max(2,s.width-66),3,62,20).build();e.addListener(st.button);}
 static boolean active(Screen s){var st=state(s);return st!=null&&st.open;}
 static void close(Screen s){state(s).open=false;}
 @SubscribeEvent public static void render(ScreenEvent.Render.Post e){var s=e.getScreen();var st=state(s);if(st==null)return;var g=e.getGuiGraphics();var font=Minecraft.getInstance().font;
  // Some physical face controllers render without Screen.render, so render this widget last.
  g.pose().pushPose();g.pose().translate(0,0,500);st.button.render(g,e.getMouseX(),e.getMouseY(),e.getPartialTick());
  if(st.open){int left=8,top=28,right=s.width-8,bottom=s.height-8,wrap=Math.max(20,right-left-28);
   if(st.wrap!=wrap){var lines=new ArrayList<FormattedCharSequence>();for(String paragraph:UsageText.forScreen(s)){lines.addAll(font.split(Component.literal(paragraph),wrap));lines.add(FormattedCharSequence.EMPTY);}st.lines=List.copyOf(lines);st.wrap=wrap;}
   int visible=Math.max(1,(bottom-top-56)/12);st.max=Math.max(0,st.lines.size()-visible);st.scroll=Math.max(0,Math.min(st.max,st.scroll));
   g.fill(0,0,s.width,s.height,0xb0000000);g.fill(left,top,right,bottom,0xff202832);g.fill(left,top,right,top+2,0xffb9d3df);
   g.drawString(font,"사용법 · "+UsageText.title(s),left+10,top+10,0xfff4e7bb,false);
   g.enableScissor(left+8,top+27,right-8,bottom-26);for(int i=0;i<visible&&st.scroll+i<st.lines.size();i++)g.drawString(font,st.lines.get(st.scroll+i),left+12,top+28+i*12,0xffeeeeee,false);g.disableScissor();
   g.drawString(font,"휠 / ↑↓ / PageUp·Down · Esc: 돌아가기",left+10,bottom-17,0xffb9cbd6,false);
   g.drawString(font,(st.scroll+1)+" / "+Math.max(1,st.max+1),Math.max(left+10,right-65),top+10,0xffb9cbd6,false);
   st.button.setMessage(Component.literal("돌아가기"));st.button.render(g,e.getMouseX(),e.getMouseY(),e.getPartialTick());
  }else st.button.setMessage(Component.literal("사용법"));g.pose().popPose();
 }
 @SubscribeEvent public static void click(ScreenEvent.MouseButtonPressed.Pre e){var st=state(e.getScreen());if(st==null)return;if(active(e.getScreen())){if(st.button.isMouseOver(e.getMouseX(),e.getMouseY()))close(e.getScreen());e.setCanceled(true);}else if(st.button.isMouseOver(e.getMouseX(),e.getMouseY())){st.button.mouseClicked(e.getMouseX(),e.getMouseY(),e.getButton());e.setCanceled(true);}}
 @SubscribeEvent public static void scroll(ScreenEvent.MouseScrolled.Pre e){if(active(e.getScreen())){var st=state(e.getScreen());st.scroll=Math.max(0,Math.min(st.max,st.scroll-(int)Math.signum(e.getScrollDelta())*3));e.setCanceled(true);}}
 @SubscribeEvent public static void key(ScreenEvent.KeyPressed.Pre e){if(!active(e.getScreen()))return;var st=state(e.getScreen());switch(e.getKeyCode()){case GLFW.GLFW_KEY_ESCAPE->close(e.getScreen());case GLFW.GLFW_KEY_UP->st.scroll--;case GLFW.GLFW_KEY_DOWN->st.scroll++;case GLFW.GLFW_KEY_PAGE_UP->st.scroll-=10;case GLFW.GLFW_KEY_PAGE_DOWN,GLFW.GLFW_KEY_SPACE->st.scroll+=10;case GLFW.GLFW_KEY_HOME->st.scroll=0;case GLFW.GLFW_KEY_END->st.scroll=st.max;default->{}}st.scroll=Math.max(0,Math.min(st.max,st.scroll));e.setCanceled(true);}
 @SubscribeEvent public static void character(ScreenEvent.CharacterTyped.Pre e){if(active(e.getScreen()))e.setCanceled(true);}
 @SubscribeEvent public static void release(ScreenEvent.MouseButtonReleased.Pre e){if(active(e.getScreen()))e.setCanceled(true);}
 @SubscribeEvent public static void drag(ScreenEvent.MouseDragged.Pre e){if(active(e.getScreen()))e.setCanceled(true);}
 // Let key releases reach the original screen, to release any previously held dial key.
}
