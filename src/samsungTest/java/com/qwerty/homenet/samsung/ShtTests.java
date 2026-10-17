package com.qwerty.homenet.samsung;
import com.qwerty.homenet.blockentity.*;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity.Action;
import com.qwerty.homenet.data.*;
import com.qwerty.homenet.guard.*;
import com.qwerty.homenet.analog.*;
import com.qwerty.homenet.intercom.*;
import com.qwerty.homenet.registry.ModBlocks;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import java.util.*;
@GameTestHolder(Samsung.ID) @PrefixGameTestTemplate(false)
public class ShtTests {
 @BeforeBatch(batch="defaultBatch") public static void zone(ServerLevel l){ZoneData.get(l).add("Samsung tests",0x488899,-100000,-100000,100000,100000);}
 static Sht101c unit(GameTestHelper h,int x){h.setBlock(new BlockPos(x,1,3),Samsung.SHT.get());var b=(Sht101c)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(x,1,3)));b.onLoad();return b;}
 static ServerPlayer player(GameTestHelper h,Sht101c b){var p=FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.nameUUIDFromBytes(("sht:"+b.getBlockPos()).getBytes(java.nio.charset.StandardCharsets.UTF_8)),"SamsungTest"));p.setPos(b.getBlockPos().getX()+.5,b.getBlockPos().getY()+1,b.getBlockPos().getZ()+.5);return p;}
 static void program(Sht101c b,ServerPlayer p,String slot,String value){b.press("memory",p,false);if(slot.matches("[0-9*#]"))b.key(slot,false);else b.press(slot,p,false);for(char c:value.toCharArray())b.key(""+c,false);if(b.snapshot(false).flag("programming"))b.press("memory",p,false);}
 static void ticks(GameTestHelper h,Sht101c b,int n){for(int i=0;i<n;i++)ReceiverBlockEntity.serverTick(h.getLevel(),b.getBlockPos(),b.getBlockState(),b);}
 static final class Exchange extends ReceiverBlockEntity implements GuardExchange {
  ReceiverBlockEntity home;String assigned="101-1001";boolean safety;
  Exchange(BlockPos at,BlockState s){super(Samsung.BE.get(),at,s);}
  public DeviceRegistry.Kind kind(){return DeviceRegistry.Kind.GUARD_CONSOLE;}
  public String modelId(){return "SAMSUNG test exchange";}public GuardSystem guardSystem(){return GuardSystem.BUS;}public String manufacturer(){return "SAMSUNG";}public boolean supports(AnalogBus b){return b==AnalogBus.COMMON_8_WIRE;}
  public void onLinked(BlockPos at,String unit){assigned=unit;}public void onUnlinked(BlockPos at){}public String assignedUnit(BlockPos at){return assigned;}
  public void setEmergency(BlockPos at,String n,boolean on){safety=on;}public void setAway(BlockPos at,String n,boolean on){}
  public void setSafetyEvent(BlockPos at,String n,String type,boolean on){safety=on;}
  public void callFrom(ReceiverBlockEntity r,ServerPlayer p){home=r;callState=CallState.RINGING;}
  public void answer(){if(home!=null&&home.acceptSwitchboardCall(worldPosition))callState=CallState.CONNECTED;}
  public void handleAction(ServerPlayer p,Action a,BlockPos t,String text){if(a==Action.SEND_MESSAGE&&home!=null&&callState==CallState.CONNECTED)Intercom.say((ServerLevel)level,this,home,p,"경비실",text);}
  public boolean startRinging(BlockPos at,String key){return super.startRinging(at,key);}
 }
 static Exchange exchange(GameTestHelper h){var p=h.absolutePos(new BlockPos(1,1,1));h.setBlock(new BlockPos(1,1,1),Samsung.SHT.get());var e=new Exchange(p,h.getLevel().getBlockState(p));h.getLevel().setBlockEntity(e);e.onLoad();return e;}
 @GameTest(template="empty") public static void busGuardCallsAndMessagesUseSamsungCircuit(GameTestHelper h){var a=unit(h,3);var p=player(h,a);var g=exchange(h);h.assertTrue(a.bindSwitchboard(g.getBlockPos()),"Samsung BUS link failed");a.press("guard",p,false);h.assertTrue(g.getCallState()==CallState.RINGING&&a.getCallState()==CallState.DIALING,"Base guard selection did not signal exchange");a.press("talk",p,true);g.answer();h.assertTrue(a.getCallState()==CallState.CONNECTED,"Cordless guard audio did not connect");a.message(p,"from Samsung handset",true);g.handleAction(p,Action.SEND_MESSAGE,BlockPos.ZERO,"guard reply");h.assertTrue(g.conversation().stream().anyMatch(l->l.text().equals("from Samsung handset"))&&a.conversation().stream().anyMatch(l->l.text().equals("guard reply")),"Guard message route not bidirectional");h.succeed();}
 @GameTest(template="empty") public static void digitalRejectsKocomAndModeChangeDropsBinding(GameTestHelper h){var a=unit(h,3);var p=player(h,a);h.setBlock(new BlockPos(1,1,1),ModBlocks.AM_10KP.get());var g=h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(1,1,1)));h.assertTrue(!a.bindSwitchboard(g.getBlockPos()),"Samsung BUS accepted KOCOM");var e=exchange(h);a.bindSwitchboard(e.getBlockPos());a.press("mode",p,false);h.assertTrue(!a.busMode()&&a.getAnalogStation()==null&&a.guardSystem()==GuardSystem.STAR,"Mode switch retained BUS connection");h.assertTrue(!a.bindSwitchboard(e.getBlockPos()),"STAR accepted BUS exchange");h.succeed();}
 @GameTest(template="empty") public static void realCameraCallAnswerChatAndFifteenSecondTimeout(GameTestHelper h){var a=unit(h,3);var p=player(h,a);h.setBlock(new BlockPos(6,1,3),ModBlocks.DOOR_STATION.get());var c=(DoorStationBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(6,1,3)));h.assertTrue(c.toggleReceiver(a.getBlockPos()),"Camera home link failed");h.assertTrue(c.startCall("")==CallerBlockEntity.CallResult.OK,"Camera could not ring Samsung");h.assertTrue(c.getBlockPos().equals(a.videoCamera()),"Incoming monochrome monitor missing");a.press("talk",p,true);h.assertTrue(c.isConnected()&&a.getCallState()==CallState.CONNECTED,"Cordless did not answer camera");a.message(p,"door talk",true);h.assertTrue(c.getLog().stream().anyMatch(l->l.text().equals("door talk")),"Camera chat missing");a.press("talk",p,true);c.startCall("");ticks(h,a,301);h.assertTrue(a.getCallState()==CallState.IDLE&&!c.isInCall(),"15-second door call did not expire");h.succeed();}
 @GameTest(template="empty") public static void wirelessHandsetOwnershipRangeAndDocking(GameTestHelper h){var a=unit(h,3);var b=unit(h,5);var p=player(h,a);p.getInventory().clearContent();a.take(p);var stack=p.getInventory().items.stream().filter(s->s.is(Samsung.CORDLESS.get())).findFirst().orElse(ItemStack.EMPTY);h.assertTrue(a.validHandset(stack)&&a.remoteAllowed(p)&&!b.validHandset(stack),"Cordless pairing token invalid");h.assertTrue(!a.docked(),"Docked mesh remained after removal");p.setPos(a.getBlockPos().getX()+40,a.getBlockPos().getY(),a.getBlockPos().getZ());h.assertTrue(!a.remoteAllowed(p),"Out-of-range cordless accepted");player(h,a);a.dock(p,stack);h.assertTrue(a.docked()&&stack.isEmpty(),"Handset docking or duplicate prevention failed");h.succeed();}
 @GameTest(template="empty") public static void physicalEmptyCradleWorksOnEveryWall(GameTestHelper h){
  var a=unit(h,3);var p=player(h,a);var at=a.getBlockPos();
  for(var facing:java.util.List.of(Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST)){
   p.getInventory().clearContent();a.take(p);var handset=p.getInventory().getItem(0);p.getInventory().setItem(0,ItemStack.EMPTY);p.getInventory().setItem(12,handset);
   var state=a.getBlockState().setValue(com.qwerty.homenet.block.WallMountedBlock.FACING,facing);double along=(160-500)*(.5/930);
   double x=facing==Direction.NORTH?.5-along:facing==Direction.SOUTH?.5+along:.5;
   double z=facing==Direction.EAST?.5-along:facing==Direction.WEST?.5+along:.5;
   var hit=new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(at.getX()+x,at.getY()+.30+(740-300)*(.5/930),at.getZ()+z),facing,at,false);
   h.assertTrue(ShtBlock.cradleHit(state,h.getLevel(),at,hit),"Cradle hit failed on "+facing);
   Samsung.SHT.get().use(state,h.getLevel(),at,p,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
   h.assertTrue(a.docked()&&handset.isEmpty(),"Physical cradle did not dock on "+facing);
  }h.succeed();
 }
 @GameTest(template="empty") public static void emptyCradleClickDocksInventoryHandsetAndEndsCall(GameTestHelper h){
  var a=unit(h,3);var other=unit(h,5);var p=player(h,a);p.getInventory().clearContent();
  ShtNetwork.dispatch(new ShtNetwork.Control(a.getBlockPos(),false,"button","take"),p);
  var stack=p.getInventory().items.stream().filter(v->a.validHandset(v)).findFirst().orElse(ItemStack.EMPTY);
  h.assertTrue(!stack.isEmpty()&&!a.docked(),"Cradle click did not take handset");
  a.press("intercom",p,false);a.press("talk",p,true);
  ShtNetwork.dispatch(new ShtNetwork.Control(other.getBlockPos(),false,"button","take"),p);
  ShtNetwork.dispatch(new ShtNetwork.Control(other.getBlockPos(),false,"button","take"),p);
  h.assertTrue(!a.docked()&&other.docked(),"Other base consumed wrong handset");
  p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,ItemStack.EMPTY);
  // Move the valid handset to a non-selected inventory slot, as when the base is opened empty-handed.
  p.getInventory().setItem(12,stack);
  ShtNetwork.dispatch(new ShtNetwork.Control(a.getBlockPos(),false,"button","take"),p);
  h.assertTrue(a.docked()&&stack.isEmpty()&&a.snapshot(false).setting("intercom","1").equals("0"),"Empty cradle did not dock owned handset and end audio");
  h.assertTrue(a.folded(),"Base antenna must default to folded");h.succeed();
 }
 @GameTest(template="empty") public static void memoryTelephoneHoldAndMessages(GameTestHelper h){var a=unit(h,3);var b=unit(h,5);var p=player(h,a);program(a,p,"speaker","50101");program(b,p,"speaker","50102");h.assertTrue(a.dialPhone("50102")&&b.phoneState()==2,"Registered PSTN simulation did not ring");a.press("talk",p,true);b.press("talk",p,true);a.message(p,"phone message",true);h.assertTrue(b.conversation().stream().anyMatch(l->l.text().equals("phone message")),"PSTN message missing");a.key("#",true);h.assertTrue(a.phoneState()==3&&b.phoneState()==3,"In-call DTMF redialled or hung up");a.press("hold",p,false);int before=b.conversation().size();a.message(p,"held message",true);h.assertTrue(b.conversation().size()==before,"Held telephone transmitted audio");a.press("talk",p,true);a.message(p,"resumed",true);h.assertTrue(b.conversation().stream().anyMatch(l->l.text().equals("resumed")),"Held telephone did not resume");a.press("talk",p,true);h.assertTrue(a.phoneState()==0&&b.phoneState()==0,"Telephone ended on only one side");program(a,p,"3","50102");a.press("speaker",p,false);a.press("memory",p,false);a.key("3",false);h.assertTrue(b.phoneState()==2,"Manual base speed recall sequence failed");a.press("talk",p,true);b.press("talk",p,true);a.press("talk",p,true);a.press("talk",p,true);a.press("talk",p,true);h.assertTrue(a.phoneState()==0,"Empty telephone selection could not be released");h.succeed();}
 @GameTest(template="empty") public static void alarmTrialMuteRestoreAndPersistentProgramming(GameTestHelper h){var a=unit(h,3);var p=player(h,a);var g=exchange(h);a.bindSwitchboard(g.getBlockPos());program(a,p,"guard","23");program(a,p,"hold","23");a.trigger("fire");ticks(h,a,600);h.assertTrue(g.safety&&a.alarmActive(),"30-second internal alarm did not notify guard");a.press("stop",p,false);h.assertTrue(a.alarmActive(),"Alarm stop erased indicator");a.press("restore",p,false);h.assertTrue(!a.alarmActive()&&!g.safety,"Alarm restore retained guard event");a.press("test",p,false);a.key("2",false);ticks(h,a,601);h.assertTrue(!g.safety,"Gas trial notified real guard");var restored=new Sht101c(a.getBlockPos(),a.getBlockState());restored.load(a.saveWithoutMetadata());h.assertTrue(restored.snapshot(false).settings().get("mode").equals("BUS"),"Mode persistence lost");a.press("restore",p,false);h.succeed();}
 @GameTest(template="empty") public static void complexBoundaryBlocksCameraAndCordless(GameTestHelper h){var a=unit(h,3);var p=player(h,a);p.getInventory().clearContent();a.take(p);var outside=a.getBlockPos().east(2);ZoneData.get(h.getLevel()).add("separate Samsung parcel",1,outside.getX(),outside.getZ(),outside.getX(),outside.getZ());p.setPos(outside.getX()+.5,outside.getY(),outside.getZ()+.5);h.assertTrue(!a.remoteAllowed(p),"Cordless escaped complex boundary");h.assertTrue(!a.startRinging(outside,"front_door"),"Cross-complex caller accepted");h.succeed();}
 @GameTest(template="empty") public static void baseAndCordlessIntercom(GameTestHelper h){var a=unit(h,3);var p=player(h,a);a.press("intercom",p,false);a.press("talk",p,true);a.message(p,"cordless to base",true);a.message(p,"base to cordless",false);h.assertTrue(a.conversation().stream().anyMatch(l->l.text().equals("cordless to base"))&&a.conversation().stream().anyMatch(l->l.text().equals("base to cordless")),"Base/handset intercom missing");a.press("talk",p,true);h.succeed();}
 @GameTest(template="empty") public static void authenticatedPacketPathAndAutomaticAlarmPhone(GameTestHelper h){
  var a=unit(h,3);var b=unit(h,5);var p=player(h,a);p.getInventory().clearContent();var g=exchange(h);a.bindSwitchboard(g.getBlockPos());a.take(p);
  ShtNetwork.dispatch(new ShtNetwork.Control(a.getBlockPos(),false,"button","guard"),p);
  ShtNetwork.dispatch(new ShtNetwork.Control(a.getBlockPos(),true,"button","talk"),p);g.answer();
  h.assertTrue(a.getCallState()==CallState.CONNECTED,"Validated remote packet did not connect");
  ShtNetwork.dispatch(new ShtNetwork.Control(a.getBlockPos(),true,"message","packet radio chat"),p);h.assertTrue(g.conversation().stream().anyMatch(l->l.text().equals("packet radio chat")),"Validated radio chat packet lost");a.press("talk",p,true);
  program(b,p,"speaker","50999");program(a,p,"speaker","50111");program(a,p,"*","50999");a.trigger("gas");ticks(h,a,620);
  h.assertTrue(b.phoneState()==2,"Alarm did not dial stored report number");b.press("talk",p,true);ticks(h,a,441);
  h.assertTrue(b.conversation().stream().anyMatch(l->l.text().equals("gas 경보"))&&b.conversation().stream().anyMatch(l->l.text().equals("발생지 전화번호 50111")),"Recorded alarm/home-number sequence missing");h.succeed();
 }
 @GameTest(template="empty") public static void manualTwoDigitPairingAndChannelPersistence(GameTestHelper h){
  var a=unit(h,3);var p=player(h,a);program(a,p,"guard","23");
  a.press("memory",p,true);a.key("*",true);a.key("#",true);a.key("2",true);a.key("3",true);
  a.press("memory",p,false);a.press("hold",p,false);a.key("2",false);a.key("3",false);
  a.press("memory",p,true);a.press("intercom",p,false);
  h.assertTrue(a.snapshot(false).flag("paired")&&!a.snapshot(false).flag("programming"),"Manual fixed/cordless pairing sequence failed");
  a.press("channel",p,true);var copy=new Sht101c(a.getBlockPos(),a.getBlockState());copy.load(a.saveWithoutMetadata());h.assertTrue(copy.snapshot(false).setting("channel","0").equals("2"),"Radio channel not saved");h.succeed();
 }
 @GameTest(template="empty") public static void analogStarSignalsRealExistingGuardWithoutChangingIt(GameTestHelper h){
  var a=unit(h,3);var p=player(h,a);a.press("mode",p,false);a.setUnit(p,"811-102");h.setBlock(new BlockPos(1,1,1),ModBlocks.GUARD_CONSOLE.get());var g=(ReceiverBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(1,1,1)));g.onLoad();a.press("guard",p,true);
  h.assertTrue(a.getCallState()==CallState.DIALING&&g.getCallState()==CallState.RINGING,"STAR call did not reach the real guard console");g.answer();h.assertTrue(a.getCallState()==CallState.CONNECTED,"Real STAR guard answer failed");a.message(p,"Samsung STAR",true);h.assertTrue(g.conversation().stream().anyMatch(l->l.text().equals("Samsung STAR")),"STAR message not delivered");h.succeed();
 }
}
