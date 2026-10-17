package com.qwerty.homenet.samsung;

import com.qwerty.homenet.blockentity.*;
import com.qwerty.homenet.analog.*;
import com.qwerty.homenet.guard.*;
import com.qwerty.homenet.data.*;
import com.qwerty.homenet.intercom.*;
import com.qwerty.homenet.network.WallpadDataPacket;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Samsung-only fixed/cordless controller. No KOCOM or Hyundai product handler is overridden. */
public final class Sht101c extends ReceiverBlockEntity {
 public static final double RADIO_RANGE=32;
 private UUID identity=UUID.randomUUID(),token=UUID.randomUUID();
 private boolean bus=true,docked=true,folded=true,armed,away,muted,trial,tone=true,radioAudio;
 private int ringVolume=2,voiceVolume=2,brightness=2,contrast=2,monitor,awayDelay,restoreTimer,alarmAge,alarmCycles,localIntercom;
 private String route="",status="대기",homeNumber="",access="00",handsetPin="00",fixedPin="00",slot="",buffer="",lastDial="",alarm="";
 private boolean programming,checking;private int phone;private BlockPos phonePeer;private boolean held;
 private final Map<String,String> memory=new HashMap<>();private final Map<String,Boolean> sensorEdges=new HashMap<>();
 private BlockPos clientCamera;private int clientLamps;
 private final Map<UUID,Boolean> viewers=new HashMap<>();private int ticks,phoneTicks,reportTicks,radioChannel=1;private boolean reported,autoReport;
 public Sht101c(BlockPos p,BlockState s){super(Samsung.BE.get(),p,s);productLogo.set("logo_text","SAMSUNG");}
 @Override public GuardSystem guardSystem(){return bus?GuardSystem.BUS:GuardSystem.STAR;}
 @Override public String manufacturer(){return "SAMSUNG";}
 @Override public AnalogBus analogBus(){return bus?AnalogBus.COMMON_8_WIRE:AnalogBus.DEDICATED;}
 @Override public boolean canSetUnitLocally(){return !bus;}
 @Override public boolean supportsSwitchboard(GuardExchange b){return super.supportsSwitchboard(b)&&b.supports(analogBus());}
 public UUID identity(){return identity;}public boolean docked(){return docked;}public boolean folded(){return folded;}
 public boolean busMode(){return bus;}public int phoneState(){return phone;}public String status(){return status;}public boolean alarmActive(){return !alarm.isEmpty();}
 public boolean scope(BlockPos p){return p!=null&&level!=null&&ZoneData.connected(level,worldPosition,p);}
 public void changed(){setChanged();if(level!=null&&!level.isClientSide){level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);syncScreens();}}
 private void note(String s){status=s;changed();}
 private boolean paired(){return handsetPin.equals(access)&&fixedPin.equals(access);}
 public boolean validHandset(ItemStack stack){return !docked&&stack.is(Samsung.CORDLESS.get())&&stack.hasTag()&&stack.getTag().hasUUID("Station")&&identity.equals(stack.getTag().getUUID("Station"))&&stack.getTag().hasUUID("Token")&&token.equals(stack.getTag().getUUID("Token"));}
 public boolean remoteAllowed(ServerPlayer p){if(!scope(p.blockPosition())||p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(worldPosition).add(com.qwerty.homenet.move.DeviceOffsets.get(level,worldPosition)))>RADIO_RANGE*RADIO_RANGE)return false;for(var s:p.getInventory().items)if(validHandset(s)&&s.getTag().getInt("Charge")>0&&s.getTag().getBoolean("On"))return true;return false;}
 public boolean canOperate(ServerPlayer p,boolean remote){return p.level()==level&&(remote?remoteAllowed(p):Intercom.inReach(p,worldPosition));}
 public void toggleHandset(ServerPlayer p){if(docked){take(p);return;}for(int i=0;i<p.getInventory().getContainerSize();i++){var stack=p.getInventory().getItem(i);if(validHandset(stack)){dock(p,stack);return;}}note("등록된 무선수화기를 인벤토리에 가지고 있어야 거치할 수 있습니다");}
 public void take(ServerPlayer p){if(!docked){note("무선수화기가 이미 분리되어 있습니다");return;}ItemStack s=new ItemStack(Samsung.CORDLESS.get());var t=s.getOrCreateTag();t.putUUID("Station",identity);t.putUUID("Token",token);t.putLong("Base",worldPosition.asLong());t.putString("Dimension",level.dimension().location().toString());t.putInt("Charge",36000);t.putBoolean("On",true);t.putInt("Channel",radioChannel);if(!p.getInventory().add(s)){note("인벤토리에 빈 공간이 필요합니다");return;}docked=false;changed();}
 public void dock(ServerPlayer p,ItemStack s){if(!validHandset(s)){note("이 본체에 등록된 무선수화기가 아닙니다");return;}endAudio();s.shrink(1);docked=true;if(Boolean.TRUE.equals(viewers.get(p.getUUID())))viewers.remove(p.getUUID());changed();}
 @Override public void openFor(ServerPlayer p){open(p,false);}
 public void open(ServerPlayer p,boolean remote){if(!canOperate(p,remote))return;viewers.put(p.getUUID(),remote);ShtNetwork.data(p,snapshot(true),remote);}
 public WallpadDataPacket snapshot(boolean open){return super.buildPacket(open);}
 @Override public void syncScreens(){if(!(level instanceof ServerLevel sl))return;for(var e:new ArrayList<>(viewers.entrySet())){var p=sl.getServer().getPlayerList().getPlayer(e.getKey());if(p==null||!canOperate(p,e.getValue())){viewers.remove(e.getKey());continue;}ShtNetwork.data(p,snapshot(false),e.getValue());}}
 @Override protected Map<String,String> settingsForClient(){var m=super.settingsForClient();m.put("sht101c","1");m.put("mode",bus?"BUS":"STAR");m.put("docked",docked?"1":"0");m.put("folded",folded?"1":"0");m.put("status",status);m.put("armed",armed?"1":"0");m.put("away",away?"1":"0");m.put("muted",muted?"1":"0");m.put("alarm",alarm);m.put("phone",""+phone);m.put("channel",""+radioChannel);m.put("held",held?"1":"0");m.put("voice_volume",""+voiceVolume);m.put("ring_volume",""+ringVolume);m.put("radio_audio",radioAudio?"1":"0");m.put("intercom",""+localIntercom);m.put("digits",buffer);m.put("programming",programming?"1":"0");m.put("brightness",""+brightness);m.put("contrast",""+contrast);m.put("paired",paired()?"1":"0");m.put("charging",docked?"1":"0");m.put("guard_lamp",callState!=CallState.IDLE&&!incomingKey.equals("front_door")?"1":"0");m.put("door_lamp",callState!=CallState.IDLE&&incomingKey.equals("front_door")?"1":"0");m.put("speaker_lamp",phone>0||localIntercom>0?"1":"0");m.put("zoned",level instanceof ServerLevel sl&&ZoneData.get(sl).zoneAt(worldPosition)!=0?"1":"0");var c=videoCamera();if(c!=null){m.put("camera","1");m.put("camera_x",""+c.getX());m.put("camera_y",""+c.getY());m.put("camera_z",""+c.getZ());}return m;}
 private boolean allowedSource(BlockPos from){if(!scope(from)||!level.isLoaded(from))return false;var b=level.getBlockEntity(from);if(b instanceof DoorStationBlockEntity)return hasLinkedCamera(from);if(b instanceof GuardExchange g)return supportsSwitchboard(g)&&from.equals(analogStation);if(b instanceof ReceiverBlockEntity r&&r.kind()==DeviceRegistry.Kind.GUARD_CONSOLE)return !bus&&r.guardSystem()==GuardSystem.STAR;if(b instanceof AnalogLobbyEndpoint l)return bus?l.manufacturer().equalsIgnoreCase("SAMSUNG")&&l.supports(AnalogBus.COMMON_8_WIRE):false;return !bus&&b instanceof LobbyPhoneBlockEntity;}
 @Override protected BlockPos ring(BlockPos from,String key,int depth){
  if(depth>=4||!allowedSource(from))return null;
  if(away&&!key.equals("front_door")&&level.getBlockEntity(from) instanceof CallerBlockEntity){
   if(bus&&switchboard() instanceof ReceiverBlockEntity g)return g.ring(from,key);
   if(!bus&&level instanceof ServerLevel sl)for(var at:DeviceRegistry.get(sl).guards(sl,worldPosition,false))if(scope(at)&&sl.isLoaded(at)&&sl.getBlockEntity(at) instanceof ReceiverBlockEntity g&&g.kind()==DeviceRegistry.Kind.GUARD_CONSOLE&&g.guardSystem()==GuardSystem.STAR)return g.ring(from,key);
  }
  return super.ring(from,key,depth);
 }
 @Override public boolean startRinging(BlockPos from,String key){if(!allowedSource(from))return false;
  boolean ok=super.startRinging(from,key);if(ok){route=key.equals("front_door")?"door":"guard";status="착신 · 무선수화기 통화 버튼을 누르세요";changed();}return ok;}
 @Override public boolean acceptSwitchboardCall(BlockPos p){if(!radioAudio||callState!=CallState.DIALING||!p.equals(caller)||switchboard()==null)return false;callState=CallState.CONNECTED;callTicks=0;note("경비실 통화 연결됨");return true;}
 @Override public void callGuard(ServerPlayer p,String ignored){if(!paired()){note("본체·수화기 암호를 먼저 일치시키세요");return;}endPhone();if(callState!=CallState.IDLE)super.hangUp();route="guard";
  if(bus){var g=switchboard();if(g==null){note("삼성 BUS 경비실기가 연결되지 않았습니다");return;}caller=g.intercomPosition();incomingKey=g.callerKey();outgoing=false;callState=CallState.DIALING;callTicks=0;g.callFrom(this,p);}
  else if(level instanceof ServerLevel sl){var all=DeviceRegistry.get(sl).guards(sl,worldPosition,false).stream().filter(at->sl.isLoaded(at)&&sl.getBlockEntity(at) instanceof ReceiverBlockEntity r&&r.kind()==DeviceRegistry.Kind.GUARD_CONSOLE&&r.guardSystem()==GuardSystem.STAR).toList();if(all.isEmpty()){note("같은 단지의 STAR 경비실기가 없습니다");return;}startOutgoing(p,all,"경비실");}
  note(callState==CallState.IDLE?"경비실 호출을 시작할 수 없습니다":"경비실 호출 중 · 무선수화기 통화 버튼을 누르세요");
 }
 @Override public void answer(){if(!paired()||!radioAudio)return;super.answer();changed();}
 public BlockPos videoCamera(){if(level!=null&&level.isClientSide)return clientCamera;if(callState!=CallState.IDLE&&caller!=null&&allowedSource(caller)&&level.getBlockEntity(caller) instanceof CallerBlockEntity)return caller;return monitor>0?linkedCamera():null;}
 @Override protected float ringVolume(){return ringVolume*.25f;}
 @Override protected void resetCall(){super.resetCall();radioAudio=false;if(level!=null&&!level.isClientSide)changed();}
 @Override public void endCallerSession(BlockPos p,boolean missed){super.endCallerSession(p,missed);if(callState==CallState.IDLE){radioAudio=false;status="통화 종료 · 대기";}changed();}
 private void endAudio(){super.hangUp();endPhone();localIntercom=0;radioAudio=false;route="";monitor=0;status="통화 종료 · 대기";changed();}
 private Sht101c peer(){return scope(phonePeer)&&level.isLoaded(phonePeer)&&level.getBlockEntity(phonePeer) instanceof Sht101c p?p:null;}
 private void clearPhone(){phone=0;phonePeer=null;phoneTicks=0;held=false;}
 private void endPhone(){var p=peer();if(p!=null&&worldPosition.equals(p.phonePeer)){p.clearPhone();p.radioAudio=false;p.changed();}clearPhone();}
 public boolean dialPhone(String number){if(number==null||!number.matches("[0-9]{1,18}")||!(level instanceof ServerLevel sl))return false;endPhone();for(var e:DeviceRegistry.get(sl).all())if(!e.pos().equals(worldPosition)&&scope(e.pos())&&sl.isLoaded(e.pos())&&sl.getBlockEntity(e.pos()) instanceof Sht101c p&&p.homeNumber.equals(number)){if(p.phone!=0){note("전화 통화 중");return false;}phone=1;phoneTicks=0;phonePeer=p.worldPosition;p.phone=2;p.phonePeer=worldPosition;p.phoneTicks=0;lastDial=number;route="phone";p.changed();note("국선 발신 · 무선수화기 통화 버튼");return true;}note("등록된 게임 내 전화번호가 없습니다");return false;}
 private void talk(){if(radioAudio&&phone==0&&callState==CallState.IDLE&&localIntercom==0&&route.equals("phone")){endAudio();return;}if(!paired()){note("본체·수화기 암호가 일치하지 않습니다");return;}if(callState==CallState.RINGING){radioAudio=true;answer();return;}if(localIntercom==3){localIntercom=0;radioAudio=false;note("내선 통화 종료");return;}if(localIntercom==1||localIntercom==2){localIntercom=3;radioAudio=true;note("본체 ↔ 무선수화기 내선 통화");return;}if(callState==CallState.CONNECTED){if(radioAudio)endAudio();else{radioAudio=true;note("통화 연결됨");}return;}if(radioAudio&&phone==3){endAudio();return;}if(phone==2){var p=peer();if(p==null){endPhone();return;}phone=p.phone=3;phoneTicks=p.phoneTicks=0;radioAudio=true;p.changed();note("국선 통화 연결됨");return;}if(phone==1||phone==3){radioAudio=true;held=false;note(phone==3?"국선 보류 해제":"국선 발신 중");return;}if(callState==CallState.DIALING){radioAudio=true;changed();return;}radioAudio=true;route="phone";buffer="";note("국선 발신음 · 번호를 입력하세요");}
 private void memoryButton(boolean remote){if(!programming&&phone==0&&route.equals("phone")){route="speed";buffer="";note("기억된 단축번호를 누르세요");return;}if(programming){commit(remote);return;}programming=true;checking=false;slot="";buffer="";note("기억할 기능 또는 단축번호를 누르세요");}
 private void commit(boolean remote){String value=buffer;if(slot.equals("pin")||slot.equals("pair")){if(value.length()!=2){note("암호번호는 두 자리입니다");return;}if(slot.equals("pair"))handsetPin=value;else access=value;}
  else if(slot.equals("fixed")){if(value.length()!=2){note("본체 비밀번호는 두 자리입니다");return;}fixedPin=value;}
  else if(slot.equals("home"))homeNumber=value;else if(slot.equals("tone"))tone=!value.equals("0");else if(!slot.isEmpty()){if((slot.equals("*")||slot.equals("#"))&&Set.of("112","113","119").contains(value)){note("자동통보에는 관공서 번호를 등록할 수 없습니다");return;}memory.put((remote?"remote:":"base:")+slot,value);}
  programming=false;slot="";buffer="";note("기억되었습니다");}
 private void selectProgram(String name,boolean remote){if(programming&&slot.isEmpty()){slot=switch(name){case "speaker"->"home";case "guard"->"pin";case "hold"->"fixed";case "redial"->"tone";default->name;};note("번호를 입력한 뒤 메모리 버튼");}}
 public void key(String k,boolean remote){if(!k.matches("[0-9*#]"))return;
  if(checking){if(k.equals("1")||k.equals("2")){trial=true;alarm=k.equals("1")?"fire":"gas";alarmAge=0;note("경보 시험 · 복귀로 종료");}else note("기억번호: "+memory.getOrDefault("base:"+k,"없음"));checking=false;return;}
  if(programming){if(slot.isEmpty()){slot=k;note("전화번호를 입력하세요");}else if(remote&&slot.equals("*")&&k.equals("#")&&buffer.isEmpty())slot="pair";else if(k.matches("[0-9]")&&buffer.length()<(remote?16:18))buffer+=k;if((slot.equals("pair")||slot.equals("fixed"))&&buffer.length()==2){commit(remote);return;}changed();return;}
  if(route.equals("speed")){dialPhone(memory.getOrDefault((remote?"remote:":"base:")+k,""));return;}
  if(route.equals("phone")&&radioAudio){if(phone>0){if(k.equals("*"))tone=true;var target=peer();if(target!=null)CallSounds.note(level,target.worldPosition,SoundEvents.NOTE_BLOCK_BIT.value(),target.voiceVolume*.25f,.7f+"123456789*0#".indexOf(k)*.06f);return;}if(k.equals("#"))dialPhone(buffer);else if(k.equals("*")&&phone>0){tone=true;note("전자식 국선 선택");}else if(buffer.length()<18&&k.matches("[0-9]")){buffer+=k;phoneTicks=-25;changed();}}
 }
 public void press(String key,ServerPlayer p,boolean remote){
  if(key.equals("take")){toggleHandset(p);return;}if(key.equals("antenna")){folded=!folded;changed();return;}
  if(key.equals("mode")){if(callState!=CallState.IDLE||phone!=0||!docked){note("통화 종료·수화기 장착 후 방식을 변경하세요");return;}unlinkSwitchboard();bus=!bus;note(bus?"삼성 디지털 BUS 방식":"아날로그 STAR 방식");return;}
  if(key.equals("memory")){memoryButton(remote);return;}
  if(programming){if(key.equals("intercom")&&slot.isEmpty()&&paired()){programming=false;buffer="";note("삐 · 본체와 무선수화기 암호 일치");return;}if(key.equals("intercom")&&slot.equals("fixed")){commit(false);note(paired()?"삐 · 본체와 무선수화기 암호 일치":"본체와 무선수화기 암호를 일치시키세요");}else selectProgram(key,remote);return;}
  if(checking){if(key.equals("speaker"))note("자택 전화번호: "+homeNumber);else if(key.equals("guard"))note("암호번호: "+access);else if(key.equals("restore"))restore();checking=false;return;}
  switch(key){
   case "talk"->talk();
   case "guard"->{if(remote)radioAudio=true;callGuard(p,"");}
   case "door"->{if(callState==CallState.CONNECTED&&incomingKey.equals("lobby")){openDoor();note("공동현관 문열림");}else if(callState==CallState.CONNECTED&&incomingKey.equals("front_door")){openDoor();}else{if(phone>0)endPhone();if(callState!=CallState.IDLE)super.hangUp();route="door";monitor=600;var c=linkedCamera();if(c!=null&&level.getBlockEntity(c) instanceof CallerBlockEntity cam)cam.callFromReceiver(this);else note("홈 링커로 연결한 도어카메라가 없습니다");if(remote){radioAudio=true;if(callState==CallState.RINGING)answer();}}}
   case "speaker"->{if(localIntercom==2){localIntercom=3;note("본체 ↔ 수화기 내선 통화");}else if(localIntercom==3){localIntercom=0;note("내선 통화 종료");}else{if(route.equals("phone")&&phone==0&&!radioAudio){route="";buffer="";note("국선 선택 해제");}else{super.hangUp();route="phone";buffer="";note("국선 선택 · 번호 입력 후 무선수화기 통화");}}}
   case "intercom"->{endPhone();super.hangUp();localIntercom=remote?2:1;radioAudio=false;note(remote?"본체 내선 호출 · 스피커로 응답":"무선수화기 내선 호출 · 통화로 응답");}
   case "hold"->{if(phone==3){held=!held;radioAudio=!held;note(held?"국선 보류":"국선 재통화");}}
   case "redial"->{super.hangUp();dialPhone(lastDial);}
   case "speed"->{route="speed";note("단축번호를 누르세요");}
   case "quick_fire","quick_police","quick_medical"->{super.hangUp();dialPhone(memory.getOrDefault("base:"+key,""));}
   case "security"->{if(armed){armed=false;}else if(doorsOpen()){note("문이 열려 방범을 설정할 수 없습니다");return;}else armed=true;changed();}
   case "away"->{if(away){away=false;armed=false;awayDelay=0;}else if(doorsOpen()){note("문이 열려 외출을 설정할 수 없습니다");return;}else{away=true;awayDelay=1200;}changed();}
   case "test"->{checking=true;note("시험: 1 화재 · 2 가스 · 스피커/경비실/단축번호 확인");}
   case "stop"->{if(autoReport){endPhone();autoReport=false;}muted=true;restoreTimer=6000;note("경보정지 · 표시등 유지 · 5분 뒤 자동 복귀");}
   case "restore"->restore();
   case "emergency"->trigger("emergency");
   case "up"->{if(callState!=CallState.IDLE||phone==3)voiceVolume=Math.min(3,voiceVolume+1);else ringVolume=Math.min(3,ringVolume+1);changed();}
   case "down"->{if(callState!=CallState.IDLE||phone==3)voiceVolume=Math.max(0,voiceVolume-1);else ringVolume=Math.max(0,ringVolume-1);changed();}
   case "brightness"->{brightness=(brightness+1)%5;changed();}case "contrast"->{contrast=(contrast+1)%5;changed();}
   case "channel"->{radioChannel=radioChannel%16+1;for(var item:p.getInventory().items)if(validHandset(item))item.getOrCreateTag().putInt("Channel",radioChannel);note("무선 채널 "+radioChannel+"로 전환했습니다");CallSounds.note(level,worldPosition,SoundEvents.NOTE_BLOCK_BIT.value(),.2f,1.4f);}
   case "pair"->{if(!docked&&fixedPin.equals(access)&&handsetPin.equals(access))note("삐 · 본체와 수화기 암호 일치");else note("메모리 → 경비실/보류 및 수화기 메모리 → * → #로 같은 암호 등록");}
  }
 }
 private boolean doorsOpen(){return receiverDoors.positions().stream().anyMatch(p->scope(p)&&level instanceof ServerLevel sl&&DoorControl.isOpen(sl,p));}
 public void trigger(String type){if(type.equals("security")&&!armed||!alarm.isEmpty())return;alarm=type;alarmAge=0;alarmCycles=0;reported=false;muted=false;trial=false;changed();}
 private void restore(){if(autoReport){endPhone();autoReport=false;}if(reported&&switchboard()!=null)switchboard().setSafetyEvent(worldPosition,unit,alarm,false);alarm="";alarmAge=0;reported=false;muted=false;trial=false;restoreTimer=0;checking=false;note("경보 복귀 · 대기");}
 private void alarmTick(){if(alarm.isEmpty())return;alarmAge++;if(!muted&&alarmAge%20==1)CallSounds.note(level,worldPosition,SoundEvents.NOTE_BLOCK_BELL.value(),.6f,alarm.equals("gas")?.6f:1.4f);
  if(!trial&&!muted&&alarmAge==600){var g=switchboard();if(g!=null){g.setSafetyEvent(worldPosition,unit,alarm,true);reported=true;}else if(!bus&&level instanceof ServerLevel sl)for(var pos:DeviceRegistry.get(sl).guards(sl,worldPosition,false))if(scope(pos)&&sl.isLoaded(pos)&&sl.getBlockEntity(pos) instanceof ReceiverBlockEntity b&&b.kind()==DeviceRegistry.Kind.GUARD_CONSOLE&&b.guardSystem()==GuardSystem.STAR)b.addAlert("SHT-101C "+unit+" · "+alarm);}
  if(!trial&&!muted&&alarmAge>=600&&alarmCycles<6&&phone==0&&ticks%20==0){String number=memory.getOrDefault("base:"+(alarmCycles++%2==0?"*":"#"),"");if(!number.isEmpty()&&dialPhone(number)){autoReport=true;reportTicks=0;status="경보 자동통보: "+alarm+" · 자택 "+homeNumber;}}
  if(autoReport&&phone==3){reportTicks++;var other=peer();if(other!=null&&(reportTicks==1||reportTicks==200||reportTicks==240)){other.addLine(new IntercomLine("경보",homeNumber,reportTicks==200?"발생지 전화번호 "+homeNumber:alarm+" 경보"));other.changed();}if(reportTicks>=440){endPhone();autoReport=false;}}
  else if(autoReport&&phone==0)autoReport=false;
  if(restoreTimer>0&&--restoreTimer==0)restore();
 }
 @Override protected void tickExtra(){ticks++;if(monitor>0)monitor--;if(awayDelay>0&&--awayDelay==0){armed=true;changed();}
  if(level.getGameTime()%5==0){Direction front=getBlockState().getValue(com.qwerty.homenet.block.WallMountedBlock.FACING);Direction[] sides={Direction.DOWN,front.getClockWise(),front.getCounterClockWise(),Direction.UP};String[] names={"fire","gas","security","emergency"};for(int i=0;i<4;i++){var at=worldPosition.relative(sides[i]);boolean active=scope(at)&&level.getSignal(at,sides[i])>0;if(active&&!sensorEdges.getOrDefault(names[i],false))trigger(names[i]);sensorEdges.put(names[i],active);}}
  alarmTick();if(callState==CallState.RINGING&&incomingKey.equals("front_door")&&callTicks>=299){super.hangUp();changed();}
  if(phone!=0){phoneTicks++;var p=peer();if(p==null||!worldPosition.equals(p.phonePeer)||phoneTicks>=600&&phone!=3){endPhone();changed();}else if(phone==2&&phoneTicks%40==1)CallSounds.incoming(level,worldPosition,false,phoneTicks-1,ringVolume*.25f);else if(held&&phoneTicks%40==1)CallSounds.note(level,p.worldPosition,SoundEvents.NOTE_BLOCK_CHIME.value(),.3f,1.1f);}
  else if(phoneTicks<0&&++phoneTicks==0&&!buffer.isEmpty())dialPhone(buffer);
  if(localIntercom>0&&localIntercom<3&&ticks%40==1)CallSounds.note(level,worldPosition,SoundEvents.NOTE_BLOCK_CHIME.value(),.3f,1.5f);
  if(ticks%10==0)syncScreens();
 }
 public void message(ServerPlayer p,String text,boolean remote){text=Intercom.sanitize(text,80);if(text.isEmpty())return;
  if(phone==3&&!held&&radioAudio){var other=peer();if(other!=null&&other.phone==3&&!other.held&&other.radioAudio&&level instanceof ServerLevel sl)Intercom.say(sl,this,other,p,"국선 "+homeNumber,text);else note("상대방이 보류 중입니다");}
  else if(localIntercom==3){addLine(new IntercomLine(remote?"무선수화기":"본체",p.getGameProfile().getName(),text));changed();}
  else if(callState==CallState.CONNECTED&&radioAudio)super.sendMessage(p,text);else note("통화 연결 후 메시지를 보낼 수 있습니다");
 }
 @Override public void handleAction(ServerPlayer p,Action a,BlockPos target,String text){if(a==Action.SEND_MESSAGE)message(p,text,false);else if(a==Action.PICKUP)talk();else if(a==Action.HANG_UP)endAudio();else if(a==Action.CALL_GUARD)press("guard",p,false);else if(a==Action.OPEN_DOOR)press("door",p,false);else if(a==Action.SET_UNIT){if(canSetUnitLocally())super.handleAction(p,a,target,text);else note("BUS 세대번호는 삼성 경비실기에서 설정합니다");}else if(a==Action.SET_SETTING){if(text.startsWith("button="))press(text.substring(7),p,false);else if(text.startsWith("key="))key(text.substring(4),false);}else super.handleAction(p,a,target,text);}
 @Override public void onBroken(){endAudio();super.onBroken();viewers.clear();}
 @Override protected void saveAdditional(CompoundTag t){super.saveAdditional(t);t.putUUID("ShtIdentity",identity);t.putUUID("ShtToken",token);t.putBoolean("ShtBus",bus);t.putBoolean("ShtDocked",docked);t.putBoolean("ShtFolded",folded);t.putBoolean("ShtArmed",armed);t.putBoolean("ShtAway",away);t.putBoolean("ShtTone",tone);t.putString("ShtHomeNumber",homeNumber);t.putString("ShtAccess",access);t.putString("ShtHandsetPin",handsetPin);t.putString("ShtFixedPin",fixedPin);t.putString("ShtLastDial",lastDial);t.putInt("ShtChannel",radioChannel);t.putInt("ShtRing",ringVolume);t.putInt("ShtVoice",voiceVolume);t.putInt("ShtBrightness",brightness);t.putInt("ShtContrast",contrast);var m=new CompoundTag();memory.forEach(m::putString);t.put("ShtMemory",m);}
 @Override public void load(CompoundTag t){super.load(t);clientCamera=t.contains("ShtClientCamera")?BlockPos.of(t.getLong("ShtClientCamera")):null;clientLamps=t.getInt("ShtClientLamps");if(t.hasUUID("ShtIdentity"))identity=t.getUUID("ShtIdentity");if(t.hasUUID("ShtToken"))token=t.getUUID("ShtToken");bus=!t.contains("ShtBus")||t.getBoolean("ShtBus");docked=!t.contains("ShtDocked")||t.getBoolean("ShtDocked");folded=!t.contains("ShtFolded")||t.getBoolean("ShtFolded");armed=t.getBoolean("ShtArmed");away=t.getBoolean("ShtAway");tone=!t.contains("ShtTone")||t.getBoolean("ShtTone");homeNumber=t.getString("ShtHomeNumber");access=t.contains("ShtAccess")?t.getString("ShtAccess"):"00";handsetPin=t.contains("ShtHandsetPin")?t.getString("ShtHandsetPin"):"00";fixedPin=t.contains("ShtFixedPin")?t.getString("ShtFixedPin"):"00";lastDial=t.getString("ShtLastDial");radioChannel=t.contains("ShtChannel")?Math.max(1,Math.min(16,t.getInt("ShtChannel"))):1;ringVolume=clamp(t,"ShtRing",3);voiceVolume=clamp(t,"ShtVoice",3);brightness=clamp(t,"ShtBrightness",4);contrast=clamp(t,"ShtContrast",4);memory.clear();var m=t.getCompound("ShtMemory");m.getAllKeys().forEach(k->memory.put(k,m.getString(k)));}
 private int clamp(CompoundTag t,String n,int max){return t.contains(n)?Math.max(0,Math.min(max,t.getInt(n))):2;}
 public int lamps(){if(level!=null&&level.isClientSide)return clientLamps;return (docked?1:0)|(armed?2:0)|(away?4:0)|(!alarm.isEmpty()?8:0)|(callState!=CallState.IDLE?16:0)|(phone>0||localIntercom>0?32:0);}
 @Override public CompoundTag getUpdateTag(){var t=super.getUpdateTag();var c=videoCamera();if(c!=null)t.putLong("ShtClientCamera",c.asLong());t.putInt("ShtClientLamps",lamps());return t;}
 @Override public void onLoad(){super.onLoad();if(level!=null&&!level.isClientSide){clearPhone();localIntercom=0;radioAudio=false;viewers.clear();}}
}
