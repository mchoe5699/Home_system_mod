package com.qwerty.homenet.sht;
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
import com.qwerty.homenet.registry.*;
import com.qwerty.homenet.blockentity.*;
import com.qwerty.homenet.data.ZoneData;
@Mod.EventBusSubscriber(modid=Product.ID,value=Dist.CLIENT)
public class ClientRun {
 static final BlockPos BASE=new BlockPos(0,80,0),DOOR=new BlockPos(3,80,0);
 static int step,age;static volatile int ready;static volatile Throwable error;
 static void check(boolean value,String text){if(!value)throw new AssertionError(text);}
 static void server(Minecraft mc,java.util.function.Consumer<net.minecraft.server.MinecraftServer> f){mc.getSingleplayerServer().execute(()->{try{f.accept(mc.getSingleplayerServer());}catch(Throwable t){error=t;}});}
 static void block(Minecraft mc,BlockPos p){mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(p),Direction.NORTH,p,false));}
 static void click(Controls c,float px,float py){c.mouseClicked(c.x+(px-(c.radio?0:76))*c.scale,c.y+(py-(c.radio?0:48))*c.scale,0);c.mouseReleased(0,0,0);}
 @SubscribeEvent public static void run(TickEvent.ClientTickEvent e){if(!Boolean.getBoolean("sht.clientTest")||e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();try{
  if(error!=null)throw new AssertionError(error);if(++age>3600)throw new AssertionError("Timed out stage "+step+" ready "+ready);
  switch(step){
   case 0->{if(mc.screen==null||mc.getOverlay()!=null)return;MeshProbe.verify();System.out.println("NEW_SHT_CLIENT_BEGIN_NO_CAPTURE");step=1;mc.setScreen(null);mc.createWorldOpenFlows().createFreshLevel("sht-new-"+System.currentTimeMillis(),new LevelSettings("SHT new test",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),net.minecraft.world.level.WorldDataConfiguration.DEFAULT),new WorldOptions(5623,false,false),r->WorldPresets.createNormalWorldDimensions(r).replaceOverworldGenerator(r,new FlatLevelSource(FlatLevelGeneratorSettings.getDefault(r.lookupOrThrow(Registries.BIOME),r.lookupOrThrow(Registries.STRUCTURE_SET),r.lookupOrThrow(Registries.PLACED_FEATURE)))));}
   case 1->{if(mc.player==null||mc.getSingleplayerServer()==null)return;step=2;server(mc,s->{var l=s.overworld();var p=s.getPlayerList().getPlayers().get(0);for(var at:BlockPos.betweenClosed(-2,79,-2,5,79,2))l.setBlockAndUpdate(at,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());l.setBlockAndUpdate(BASE.south(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());l.setBlockAndUpdate(BASE,Product.STATION.get().defaultBlockState());l.setBlockAndUpdate(DOOR,ModBlocks.DOOR_STATION.get().defaultBlockState());ZoneData.get(l).add("new product client",1,-20,-20,20,20);p.teleportTo(l,1,80,-2,0,0);p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new ItemStack(ModItems.HOME_LINKER.get()));ready=1;});}
   case 2->{if(ready!=1||!mc.player.getMainHandItem().is(ModItems.HOME_LINKER.get()))return;block(mc,BASE);block(mc,DOOR);step=3;}
   case 3->{if(age%20!=0)return;server(mc,s->{var c=(DoorStationBlockEntity)s.overworld().getBlockEntity(DOOR);if(c.hasReceiver(BASE)){s.getPlayerList().getPlayers().get(0).setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,ItemStack.EMPTY);ready=2;}});if(ready==2&&mc.player.getMainHandItem().isEmpty()){mc.setScreen(null);block(mc,BASE);step=4;}}
   case 4->{if(!(mc.screen instanceof Controls c))return;click(c,220,300);step=5;}
   case 5->{if(!mc.player.getMainHandItem().is(Product.RADIO.get()))return;mc.setScreen(null);mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);step=6;}
   case 6->{if(!(mc.screen instanceof Controls c)||!c.radio)return;step=7;server(mc,s->{var camera=(DoorStationBlockEntity)s.overworld().getBlockEntity(DOOR);check(camera.startCall("")==CallerBlockEntity.CallResult.OK,"Camera did not ring");ready=3;});}
   case 7->{if(ready!=3||!(mc.screen instanceof Controls c))return;click(c,92,322);step=8;}
   case 8->{if(age%20!=0)return;server(mc,s->{var camera=(DoorStationBlockEntity)s.overworld().getBlockEntity(DOOR);if(camera.isConnected())ready=4;});if(ready==4&&mc.screen instanceof Controls c){c.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_T,0,0);c.charTyped('t',0);for(char ch:"새 삼성 수화기 통화".toCharArray())check(c.charTyped(ch,0),"Korean edit failed");c.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER,0,0);step=9;}}
   case 9->{if(age%20!=0)return;server(mc,s->{var camera=(DoorStationBlockEntity)s.overworld().getBlockEntity(DOOR);var base=(Station)s.overworld().getBlockEntity(BASE);if(ready<5&&camera.getLog().stream().anyMatch(v->v.text().equals("새 삼성 수화기 통화"))){camera.say(s.getPlayerList().getPlayers().get(0),"front_door","새 카메라 답신");check(base.conversation().stream().anyMatch(v->v.text().equals("새 카메라 답신")),"Camera reply lost");var p=s.getPlayerList().getPlayers().get(0);var radio=p.getMainHandItem();p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,ItemStack.EMPTY);p.getInventory().setItem(12,radio);ready=5;}});if(ready==5&&mc.player.getMainHandItem().isEmpty()){mc.setScreen(null);block(mc,BASE);step=10;}}
   case 10->{if(!(mc.screen instanceof Controls c)||c.radio)return;click(c,220,300);step=11;}
   case 11->{if(!(mc.screen instanceof Controls c)||!c.data.flag("docked"))return;step=12;server(mc,s->{var base=(Station)s.overworld().getBlockEntity(BASE);var p=s.getPlayerList().getPlayers().get(0);check(base.docked&&base.getCallState()==com.qwerty.homenet.intercom.CallState.IDLE,"Cradle did not end call");check(p.getInventory().items.stream().noneMatch(v->v.is(Product.RADIO.get())),"Duplicate handset");ready=6;});}
   case 12->{if(ready!=6)return;System.out.println("NEW_SHT_CLIENT_PASS: actual linker packets, handset pickup, remote screen, door answer, Korean chat and reply, empty-cradle docking, state sync; no screenshots");step=13;mc.setScreen(null);server(mc,s->{VideoProbe.setup(s);ready=7;});}
   case 13->{if(ready!=7||age%20!=0||!VideoProbe.verify(mc))return;step=14;server(mc,s->{for(var at:VideoProbe.positions)((ReceiverBlockEntity)s.overworld().getBlockEntity(at)).hangUp();ready=8;});}
   case 14->{if(ready!=8||age%20!=0||!VideoProbe.cleared(mc))return;System.out.println("ALL_SHT15_CLIENT_CHECKS_PASS");step=15;mc.stop();}
  }
 }catch(Throwable t){t.printStackTrace();System.out.println("NEW_SHT_CLIENT_FAIL "+step);System.exit(1);}}
}
