package com.qwerty.homenet.samsung;
import com.qwerty.homenet.blockentity.*;
import com.qwerty.homenet.data.ZoneData;
import com.qwerty.homenet.registry.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.lang.reflect.*;
@Mod.EventBusSubscriber(modid=Samsung.ID,value=Dist.CLIENT)
public final class ShtClientCheck {
 private static final BlockPos HOME=new BlockPos(0,80,0),CAMERA=new BlockPos(3,80,0);private static int phase,age;private static volatile int ready;private static volatile Throwable failure;
 private static void check(boolean good,String msg){if(!good)throw new AssertionError(msg);}
 private static double field(Object o,String name)throws Exception{var f=o.getClass().getDeclaredField(name);f.setAccessible(true);return ((Number)f.get(o)).doubleValue();}
 private static void click(ShtScreen s,int x,int y)throws Exception{s.mouseClicked(field(s,"left")+x*field(s,"scale"),field(s,"top")+y*field(s,"scale"),0);s.mouseReleased(0,0,0);}
 private static void server(Minecraft mc,java.util.function.Consumer<net.minecraft.server.MinecraftServer> task){var s=mc.getSingleplayerServer();s.execute(()->{try{task.accept(s);}catch(Throwable t){failure=t;}});}
 private static void use(Minecraft mc,BlockPos pos){mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.NORTH,pos,false));}
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){if(!Boolean.getBoolean("homenet.shtClientCheck")||event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();try{
  if(failure!=null)throw new AssertionError("Server test failed",failure);
  if(phase==0){if(mc.screen==null||mc.getOverlay()!=null)return;phase=1;System.out.println("SHT_CLIENT_BEGIN_NO_SCREENSHOTS");mc.setScreen(null);mc.createWorldOpenFlows().createFreshLevel("sht-check-"+System.currentTimeMillis(),new LevelSettings("SHT check",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),net.minecraft.world.level.WorldDataConfiguration.DEFAULT),new WorldOptions(9921,false,false),r->WorldPresets.createNormalWorldDimensions(r).replaceOverworldGenerator(r,new FlatLevelSource(FlatLevelGeneratorSettings.getDefault(r.lookupOrThrow(Registries.BIOME),r.lookupOrThrow(Registries.STRUCTURE_SET),r.lookupOrThrow(Registries.PLACED_FEATURE)))));return;}
  if(++age>3000)throw new AssertionError("Timed out phase "+phase+" ready "+ready);
  if(phase==1&&mc.player!=null&&mc.getSingleplayerServer()!=null){phase=2;server(mc,s->{var l=s.overworld();var p=s.getPlayerList().getPlayers().get(0);for(var at:BlockPos.betweenClosed(-2,79,-2,5,79,2))l.setBlockAndUpdate(at,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());for(var at:java.util.List.of(HOME,CAMERA))l.setBlockAndUpdate(at.south(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());l.setBlockAndUpdate(HOME,Samsung.SHT.get().defaultBlockState());l.setBlockAndUpdate(CAMERA,ModBlocks.DOOR_STATION.get().defaultBlockState());ZoneData.get(l).add("Samsung client parcel",1,-20,-20,20,20);p.teleportTo(l,1,80,-2,0,0);p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new ItemStack(ModItems.HOME_LINKER.get()));ready=1;});}
  if(phase==2&&ready==1&&mc.player.getMainHandItem().is(ModItems.HOME_LINKER.get())){use(mc,HOME);use(mc,CAMERA);phase=3;}
  if(phase==3&&age%20==0){server(mc,s->{var c=(DoorStationBlockEntity)s.overworld().getBlockEntity(CAMERA);if(c.hasReceiver(HOME)){s.getPlayerList().getPlayers().get(0).setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,ItemStack.EMPTY);ready=2;}});if(ready==2&&mc.player.getMainHandItem().isEmpty()){mc.setScreen(null);use(mc,HOME);phase=4;}}
  if(phase==4&&mc.screen instanceof ShtScreen screen){click(screen,160,300);phase=5;}
  if(phase==5&&age%20==0){server(mc,s->{var a=(Sht101c)s.overworld().getBlockEntity(HOME);if(!a.docked())ready=3;});if(ready==3&&mc.player.getMainHandItem().is(Samsung.CORDLESS.get())){mc.setScreen(null);mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);phase=6;}}
  if(phase==6&&mc.screen instanceof ShtScreen screen){phase=7;server(mc,s->{var c=(DoorStationBlockEntity)s.overworld().getBlockEntity(CAMERA);check(c.startCall("")==CallerBlockEntity.CallResult.OK,"Real camera could not call cordless");ready=4;});}
  if(phase==7&&ready==4&&mc.screen instanceof ShtScreen screen){click(screen,120,218);phase=8;}
  if(phase==8&&age%20==0){server(mc,s->{var a=(Sht101c)s.overworld().getBlockEntity(HOME);var c=(DoorStationBlockEntity)s.overworld().getBlockEntity(CAMERA);if(c.isConnected()&&a.videoCamera()!=null)ready=5;});if(ready==5&&mc.screen instanceof ShtScreen screen){phase=9;screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_T,0,0);screen.charTyped('t',0);for(char c:"삼성 무선 통화 101C".toCharArray())check(screen.charTyped(c,0),"Chat focus rejected text");screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER,0,0);}}
  if(phase==9&&age%20==0){server(mc,s->{var a=(Sht101c)s.overworld().getBlockEntity(HOME);var c=(DoorStationBlockEntity)s.overworld().getBlockEntity(CAMERA);if(c.getLog().stream().anyMatch(v->v.text().equals("삼성 무선 통화 101C"))){c.say(s.getPlayerList().getPlayers().get(0),"front_door","Samsung camera reply");check(a.conversation().stream().anyMatch(v->v.text().equals("Samsung camera reply")),"Return chat missing");ready=6;}});if(ready==6){System.out.println("SHT_CLIENT_CHECKS_PASSED: real Home Linker packets -> physical handset pickup -> cordless item screen -> door call answer -> Korean typing/Enter -> two-way chat; no screenshots");phase=10;mc.stop();}}
 }catch(Throwable t){t.printStackTrace();System.out.println("SHT_CLIENT_CHECKS_FAILED phase="+phase);System.exit(1);}}
}
