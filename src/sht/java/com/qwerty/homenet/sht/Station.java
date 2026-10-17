package com.qwerty.homenet.sht;
import com.qwerty.homenet.blockentity.*;
import com.qwerty.homenet.data.*;
import com.qwerty.homenet.analog.*;
import com.qwerty.homenet.guard.*;
import com.qwerty.homenet.intercom.*;
import com.qwerty.homenet.network.WallpadDataPacket;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;
/** Fresh SHT-101C operation controller. Existing product bytecode is never replaced. */
public class Station extends ReceiverBlockEntity {
 public boolean docked=true,bus=true,folded=true;
 UUID serial=UUID.randomUUID(),ticket=UUID.randomUUID();
 String status="대기",digits="",editing="",pin="00",radioPin="00",fixedPin="00",number="",last="",alarm="";
 boolean memory,verify,audio,hold,away,armed,test,mute,tone=true,recall;
 int channel=1,volume=2,ring=2,brightness=2,contrast=2,internal,monitor,alarmTime,awayTime,resetTime,clock,phone,phoneTime,reportStage=-1,reportTimer,reportAudio;
 BlockPos phoneAt,clientVideo;int clientLights;Map<String,String> stored=new HashMap<>();Set<String> edges=new HashSet<>();Map<UUID,Boolean> observers=new HashMap<>();
 public Station(BlockPos p,BlockState s){super(Product.TYPE.get(),p,s);productLogo.set("logo_text","SAMSUNG");}
 public DeviceRegistry.Kind kind(){return DeviceRegistry.Kind.VIDEO_PHONE;}
 public GuardSystem guardSystem(){return bus?GuardSystem.BUS:GuardSystem.STAR;}
 public String manufacturer(){return "SAMSUNG";}
 public AnalogBus analogBus(){return bus?AnalogBus.COMMON_8_WIRE:AnalogBus.DEDICATED;}
 public boolean canSetUnitLocally(){return !bus;}
 public boolean supportsSwitchboard(GuardExchange g){return super.supportsSwitchboard(g)&&g.supports(analogBus());}
 boolean same(BlockPos p){return p!=null&&level!=null&&ZoneData.connected(level,worldPosition,p);}
 boolean paired(){return pin.equals(radioPin)&&pin.equals(fixedPin);}
 public boolean owns(ItemStack s){return !docked&&s.is(Product.RADIO.get())&&s.hasTag()&&s.getTag().hasUUID("serial")&&serial.equals(s.getTag().getUUID("serial"))&&s.getTag().hasUUID("ticket")&&ticket.equals(s.getTag().getUUID("ticket"));}
 static Station find(ServerPlayer p,ItemStack s){if(!s.hasTag()||!s.getTag().hasUUID("serial"))return null;var sl=p.serverLevel();for(var e:DeviceRegistry.get(sl).all())if(sl.isLoaded(e.pos())&&sl.getBlockEntity(e.pos()) instanceof Station b&&b.owns(s))return b;return null;}
 public boolean allowed(ServerPlayer p,boolean remote){if(p.level()!=level)return false;if(!remote)return Intercom.inReach(p,worldPosition);if(!same(p.blockPosition())||p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(worldPosition).add(com.qwerty.homenet.move.DeviceOffsets.get(level,worldPosition)))>1024)return false;for(var s:p.getInventory().items)if(owns(s)&&s.getTag().getBoolean("power")&&s.getTag().getInt("battery")>0)return true;return false;}
 void update(){setChanged();if(level!=null&&!level.isClientSide){level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);syncScreens();}}
 void tell(String s){status=s;update();}
 public void lift(ServerPlayer p){if(!docked)return;var s=new ItemStack(Product.RADIO.get());var t=s.getOrCreateTag();t.putUUID("serial",serial);t.putUUID("ticket",ticket);t.putBoolean("power",true);t.putInt("battery",36000);if(!p.getInventory().add(s)){tell("수화기를 넣을 인벤토리 공간이 필요합니다");return;}docked=false;tell("무선수화기 분리");}
 public void replace(ServerPlayer p){if(docked){lift(p);return;}for(int i=0;i<p.getInventory().getContainerSize();i++){var s=p.getInventory().getItem(i);if(owns(s)){finish();s.shrink(1);docked=true;ticket=UUID.randomUUID();if(Boolean.TRUE.equals(observers.get(p.getUUID())))observers.remove(p.getUUID());tell("수화기 거치 · 충전 중");return;}}tell("이 본체의 등록된 수화기가 인벤토리에 없습니다");}
 public void openFor(ServerPlayer p){open(p,false);}
 public void open(ServerPlayer p,boolean remote){if(!allowed(p,remote))return;observers.put(p.getUUID(),remote);Wire.show(p,buildPacket(true),remote);}
 public WallpadDataPacket snapshot(){return buildPacket(false);}
 public void syncScreens(){if(!(level instanceof ServerLevel sl))return;for(var e:new ArrayList<>(observers.entrySet())){var p=sl.getServer().getPlayerList().getPlayer(e.getKey());if(p==null||!allowed(p,e.getValue()))observers.remove(e.getKey());else Wire.show(p,buildPacket(false),e.getValue());}}
 protected Map<String,String> settingsForClient(){var s=super.settingsForClient();s.put("sht","new");s.put("mode",bus?"BUS":"STAR");s.put("status",status);s.put("docked",docked?"1":"0");s.put("folded",folded?"1":"0");s.put("alarm",alarm);s.put("armed",armed?"1":"0");s.put("away",away?"1":"0");s.put("brightness",""+brightness);s.put("contrast",""+contrast);s.put("channel",""+channel);s.put("phone",""+phone);s.put("intercom",""+internal);s.put("paired",paired()?"1":"0");s.put("lights",""+lights());var v=camera();if(v!=null){s.put("video","1");s.put("vx",""+v.getX());s.put("vy",""+v.getY());s.put("vz",""+v.getZ());}return s;}
 boolean source(BlockPos at){if(!same(at)||!level.isLoaded(at))return false;var b=level.getBlockEntity(at);if(b instanceof DoorStationBlockEntity)return hasLinkedCamera(at);if(b instanceof GuardExchange g)return at.equals(analogStation)&&supportsSwitchboard(g);if(b instanceof ReceiverBlockEntity r)return !bus&&r.kind()==DeviceRegistry.Kind.GUARD_CONSOLE&&r.guardSystem()==GuardSystem.STAR;if(b instanceof AnalogLobbyEndpoint a)return bus&&a.manufacturer().equalsIgnoreCase("SAMSUNG")&&a.supports(AnalogBus.COMMON_8_WIRE);return !bus&&b instanceof LobbyPhoneBlockEntity;}
 protected BlockPos ring(BlockPos at,String key,int depth){if(depth>3||!source(at))return null;if(away&&key.equals("lobby")){var g=switchboard();if(g instanceof ReceiverBlockEntity r)return r.ring(at,key);if(!bus&&level instanceof ServerLevel sl)for(var p:DeviceRegistry.get(sl).guards(sl,worldPosition,false))if(same(p)&&level.getBlockEntity(p) instanceof ReceiverBlockEntity r&&r.guardSystem()==GuardSystem.STAR)return r.ring(at,key);}return super.ring(at,key,depth);}
 public boolean startRinging(BlockPos at,String key){if(!source(at))return false;boolean ok=super.startRinging(at,key);if(ok)tell("착신 · 스피커 또는 무선수화기로 응답");return ok;}
 public void callGuard(ServerPlayer p,String unused){if(!paired()){tell("수화기와 본체의 두 자리 암호를 일치시키세요");return;}boolean activeAudio=audio;endPhone();super.hangUp();audio=activeAudio;internal=0;if(bus){var g=switchboard();if(g==null){tell("연결된 삼성 BUS 경비실기가 없습니다");return;}caller=g.intercomPosition();incomingKey=g.callerKey();callState=CallState.DIALING;outgoing=false;callTicks=0;g.callFrom(this,p);}else if(level instanceof ServerLevel sl){var peers=DeviceRegistry.get(sl).guards(sl,worldPosition,false).stream().filter(at->same(at)&&sl.isLoaded(at)&&sl.getBlockEntity(at) instanceof ReceiverBlockEntity r&&r.kind()==DeviceRegistry.Kind.GUARD_CONSOLE&&r.guardSystem()==GuardSystem.STAR).toList();if(peers.isEmpty()){tell("같은 단지의 STAR 경비실기가 없습니다");return;}startOutgoing(p,peers,"경비실");}tell("경비실 호출 중");}
 public boolean acceptSwitchboardCall(BlockPos p){if(!audio||callState!=CallState.DIALING||!p.equals(caller)||switchboard()==null)return false;callState=CallState.CONNECTED;callTicks=0;tell("경비실 통화");return true;}
 public void answer(){if(paired()&&audio){super.answer();update();}}
 public BlockPos camera(){if(level!=null&&level.isClientSide)return clientVideo;if(callState!=CallState.IDLE&&source(caller)&&level.getBlockEntity(caller) instanceof CallerBlockEntity)return caller;return monitor>0?linkedCamera():null;}
 public int lights(){if(level!=null&&level.isClientSide)return clientLights;return 1|(docked?2:0)|(armed?4:0)|(away?8:0)|(!alarm.isEmpty()?16:0)|(callState!=CallState.IDLE&&incomingKey.equals("front_door")?32:0)|(callState!=CallState.IDLE&&!incomingKey.equals("front_door")?64:0)|(phone>0||internal>0?128:0);}
 protected float ringVolume(){return ring*.25f;}
 void finish(){super.hangUp();endPhone();internal=0;audio=false;monitor=0;digits="";recall=false;tell("대기");}
 protected void resetCall(){super.resetCall();audio=false;update();}
 public void endCallerSession(BlockPos p,boolean missed){super.endCallerSession(p,missed);update();}
 void talk(){if(!paired()){tell("무선수화기 암호가 일치하지 않습니다");return;}if(callState==CallState.RINGING){audio=true;answer();return;}if(callState==CallState.CONNECTED&&audio||phone==3&&audio||internal==3){finish();return;}if(internal==1||internal==2){internal=3;audio=true;tell("본체 ↔ 무선수화기 내선 통화");return;}if(phone==2){var b=phonePeer();if(b!=null){phone=b.phone=3;phoneTime=b.phoneTime=0;audio=true;b.update();}}else if(phone>0){audio=true;hold=false;}else if(callState==CallState.DIALING||callState==CallState.CONNECTED)audio=true;else if(audio)finish();else{audio=true;digits="";tell("국선 발신음 · 번호 입력");}update();}
 Station phonePeer(){return same(phoneAt)&&level.isLoaded(phoneAt)&&level.getBlockEntity(phoneAt) instanceof Station s?s:null;}
 void clearPhone(){phone=0;phoneAt=null;phoneTime=0;hold=false;}
 void endPhone(){var b=phonePeer();if(b!=null&&worldPosition.equals(b.phoneAt)){b.clearPhone();b.audio=false;b.update();}clearPhone();}
 public boolean dialNumber(String n){if(!n.matches("[0-9]{1,18}")||!(level instanceof ServerLevel sl))return false;boolean activeAudio=audio;endPhone();super.hangUp();audio=activeAudio;internal=0;for(var e:DeviceRegistry.get(sl).all())if(!e.pos().equals(worldPosition)&&same(e.pos())&&sl.isLoaded(e.pos())&&sl.getBlockEntity(e.pos()) instanceof Station b&&b.number.equals(n)){if(b.phone>0){tell("통화 중");return false;}phone=1;phoneAt=e.pos();phoneTime=0;b.phone=2;b.phoneAt=worldPosition;b.phoneTime=0;last=n;b.update();tell("국선 호출 중");return true;}tell("게임 내에 등록된 전화번호가 없습니다");return false;}
 public void say(ServerPlayer p,String text,boolean remote){String t=Intercom.sanitize(text,80);if(t.isBlank())return;if(!allowed(p,remote)||remote&&!paired())return;if(internal==3){addLine(new IntercomLine(remote?"무선수화기":"본체",p.getGameProfile().getName(),t));update();}else if(phone==3&&!hold){var b=phonePeer();if(b!=null&&audio){Intercom.say((ServerLevel)level,this,b,p,"전화",t);b.update();update();}}else if(callState==CallState.CONNECTED&&audio){super.sendMessage(p,t);update();}else tell("통화가 연결된 후 메시지를 보낼 수 있습니다");}
 void saveMemory(boolean remote){if(editing.isEmpty())return;if(Set.of("pin","radioPin","fixedPin").contains(editing)&&digits.length()!=2){tell("암호는 두 자리입니다");return;}switch(editing){case "pin"->pin=digits;case "radioPin"->radioPin=digits;case "fixedPin"->fixedPin=digits;case "number"->number=digits;case "tone"->tone=!digits.equals("0");default->{if(Set.of("*","#").contains(editing)&&Set.of("112","113","119").contains(digits)){tell("자동통보 번호로 등록할 수 없습니다");return;}stored.put((remote?"R:":"B:")+editing,digits);}}memory=false;editing="";digits="";tell("기억되었습니다");}
 public void digit(String k,boolean remote){if(!k.matches("[0-9*#]"))return;if(verify){verify=false;if(k.equals("1")||k.equals("2")){test=true;alarm=k.equals("1")?"fire":"gas";alarmTime=0;tell("경보 시험");}else tell("기억 번호: "+stored.getOrDefault("B:"+k,"없음"));return;}if(memory){if(editing.isEmpty()){editing=k;digits="";}else if(remote&&editing.equals("*")&&k.equals("#")&&digits.isEmpty())editing="radioPin";else if(k.matches("[0-9]")&&digits.length()<(remote?16:18))digits+=k;if(Set.of("radioPin","fixedPin").contains(editing)&&digits.length()==2)saveMemory(remote);else update();return;}if(recall){recall=false;dialNumber(stored.getOrDefault((remote?"R:":"B:")+k,""));return;}if(phone==3){if(k.equals("*"))tone=true;return;}if(audio||phone==0&&status.startsWith("국선 선택")){if(k.equals("#")){dialNumber(digits);digits="";}else if(k.matches("[0-9]")&&digits.length()<18){digits+=k;phoneTime=-30;update();}}}
 public void press(String key,ServerPlayer p,boolean remote){
  if(key.equals("handset")){if(!remote)replace(p);return;}
  if(key.equals("mode")){if(remote)return;if(!docked||callState!=CallState.IDLE||phone>0){tell("수화기 거치·통화 종료 후 변경하세요");return;}unlinkSwitchboard();bus=!bus;tell(bus?"삼성 BUS":"아날로그 STAR");return;}
  if(key.equals("antenna")){if(!remote){folded=!folded;update();}return;}
  if(key.equals("memory")){if(memory){saveMemory(remote);}else if(audio||status.startsWith("국선 선택")){recall=true;tell("단축번호 입력");}else{memory=true;editing="";digits="";tell("저장할 기능 또는 단축번호");}return;}
  if(memory){if(key.equals("intercom")&&paired()){memory=false;tell("삐 · 암호 일치");return;}editing=switch(key){case "guard"->"pin";case "hold"->"fixedPin";case "speaker"->"number";case "redial"->"tone";default->key;};digits="";tell("번호 입력 후 메모리");return;}
  if(verify&&!key.equals("restore")){verify=false;tell(key.equals("guard")?"등록 암호: "+pin:key.equals("speaker")?"자택 전화번호: "+number:"기억 확인");return;}
  switch(key){
   case "talk"->talk();
   case "guard"->{if(remote)audio=true;callGuard(p,"");}
   case "door"->{if(callState==CallState.CONNECTED&&incomingKey.equals("lobby")||callState==CallState.CONNECTED&&incomingKey.equals("front_door")){openDoor();}else{super.hangUp();endPhone();internal=0;monitor=600;var at=linkedCamera();if(at!=null&&level.getBlockEntity(at) instanceof CallerBlockEntity c){if(remote){c.callFromReceiver(this);audio=true;answer();}else tell("세대 현관 영상 확인");}else tell("연결된 도어카메라가 없습니다");}}
   case "speaker"->{
    if(internal==2){internal=3;audio=true;tell("본체 ↔ 수화기 내선 통화");}
    else if(internal==3)finish();
    else if(callState!=CallState.IDLE||phone>0)talk();
    else if(monitor>0){var at=linkedCamera();if(at!=null&&level.getBlockEntity(at) instanceof CallerBlockEntity c){c.callFromReceiver(this);audio=true;answer();tell("현관 스피커 통화");}else tell("연결된 도어카메라가 없습니다");}
    else if(audio)finish();
    else{audio=true;digits="";tell("국선 발신음 · 번호 입력");}
   }
   case "intercom"->{finish();internal=remote?2:1;tell(remote?"본체 호출 · 스피커로 응답":"무선수화기 호출 · 통화로 응답");}
   case "hold"->{if(phone==3){hold=true;audio=false;tell("국선 보류");}}
   case "redial"->{boolean speaking=audio;finish();audio=remote&&speaking;if(last.isEmpty()&&remote){audio=true;tell("국선 발신음 · 번호 입력");}else dialNumber(last);}
   case "fire_call","police_call","medical_call"->{finish();dialNumber(stored.getOrDefault("B:"+key,""));}
   case "channel"->{channel=channel%16+1;tell("채널 "+channel);}
   case "security"->{if(!armed&&openDoors()){tell("문이 열려 있습니다");return;}armed=!armed;update();}
   case "away"->{if(!away&&openDoors()){tell("문이 열려 있습니다");return;}away=!away;if(!away){armed=false;awayTime=0;}else awayTime=1200;var g=switchboard();if(g!=null)g.setAway(worldPosition,getUnit(),away);update();}
   case "test"->{verify=true;tell("시험 1: 화재 · 2: 가스 · 스피커/경비실/단축번호 확인");}
   case "stop"->{mute=true;resetTime=6000;reportStage=-1;if(phone>0)endPhone();tell("경보정지 · 표시등 유지");}
   case "restore"->restore();
   case "emergency"->trigger("emergency");
   case "up","down"->{int delta=key.equals("up")?1:-1;if(callState!=CallState.IDLE||phone==3||internal==3)volume=Math.max(0,Math.min(3,volume+delta));else ring=Math.max(0,Math.min(3,ring+delta));update();}
   case "brightness"->{brightness=(brightness+1)%5;update();}
   case "contrast"->{contrast=(contrast+1)%5;update();}
  }
 }
 boolean openDoors(){for(var p:getReceiverDoors())if(level.isLoaded(p)&&same(p)){var s=level.getBlockState(p);if(s.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN)&&s.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN))return true;}return false;}
 public void trigger(String type){if(mute)return;alarm=type;alarmTime=0;test=false;reportStage=-1;reportTimer=0;tell(type+" 경보");}
 void guardAlarm(boolean on){var g=switchboard();if(g!=null)g.setSafetyEvent(worldPosition,getUnit(),alarm,on);else if(!bus&&level instanceof ServerLevel sl)for(var p:DeviceRegistry.get(sl).guards(sl,worldPosition,false))if(same(p)&&sl.isLoaded(p)&&sl.getBlockEntity(p) instanceof ReceiverBlockEntity r&&r.guardSystem()==GuardSystem.STAR){if(r instanceof GuardExchange exchange)exchange.setSafetyEvent(worldPosition,getUnit(),alarm,on);else if(on)r.addAlert(getUnit()+" "+alarm+" 경보");}}
 void restore(){if(!test)guardAlarm(false);alarm="";test=false;mute=false;verify=false;resetTime=0;reportStage=-1;tell("복귀 · 대기");}
 protected void tickExtra(){if((phone==2||internal==1||internal==2)&&clock%40==0)CallSounds.note(level,worldPosition,net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BELL.value(),ring*.25f,1);if(++clock%20==0){if(!docked&&level instanceof ServerLevel sl)for(var p:sl.players())for(var s:p.getInventory().items)if(owns(s)&&s.getTag().getBoolean("power")){int battery=s.getTag().getInt("battery");s.getTag().putInt("battery",Math.max(0,battery-20));if(battery<=20&&audio)finish();}syncScreens();}if(monitor>0&&--monitor==0)update();if(awayTime>0&&--awayTime==0){armed=true;update();}if(resetTime>0&&--resetTime==0)restore();if(callState==CallState.RINGING&&incomingKey.equals("front_door")&&callTicks>=300){super.hangUp();update();}if(phone==0&&digits.length()>0&&!memory&&!recall&&(audio||status.startsWith("국선 선택"))&&++phoneTime==0){String n=digits;digits="";dialNumber(n);}if(phone>0&&++phoneTime>2400)endPhone();
  for(Direction d:Direction.values()){boolean on=level.getSignal(worldPosition.relative(d),d)>0;String event=d==Direction.DOWN?"fire":d==Direction.UP?"emergency":d==Direction.EAST?"gas":"security";String edge=d.name();if(on&&!edges.contains(edge)&&(!event.equals("security")||armed)){trigger(event);edges.add(edge);}else if(!on)edges.remove(edge);}
  if(!alarm.isEmpty()&&!mute){alarmTime++;if(alarmTime%40==1)CallSounds.note(level,worldPosition,net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BELL.value(),ring*.25f,alarm.equals("gas")?.7f:1.2f);if(!test&&alarmTime==600){guardAlarm(true);reportStage=0;reportTimer=0;reportAudio=0;}
   if(!test&&reportStage>=0){if(++reportTimer==1){String n=stored.getOrDefault("B:"+(reportStage%2==0?"*":"#"),"");if(!n.isEmpty())dialNumber(n);}var peer=phonePeer();if(phone==3&&peer!=null){reportAudio++;if(reportAudio==1||reportAudio==241){peer.addLine(new IntercomLine("자동통보","SHT-101C",alarm+" 경보"));peer.update();}if(reportAudio==201){peer.addLine(new IntercomLine("자동통보","SHT-101C","발생지 "+number));peer.update();}}if(reportTimer>=600||reportAudio>=441){endPhone();reportTimer=0;reportAudio=0;if(++reportStage>=6)reportStage=-1;}}
  }
 }
 protected void saveAdditional(CompoundTag t){super.saveAdditional(t);t.putUUID("Serial",serial);t.putUUID("Ticket",ticket);t.putBoolean("Docked",docked);t.putBoolean("Bus",bus);t.putBoolean("Folded",folded);t.putBoolean("Armed",armed);t.putBoolean("Away",away);t.putString("Pin",pin);t.putString("RadioPin",radioPin);t.putString("FixedPin",fixedPin);t.putString("Number",number);t.putString("Last",last);t.putInt("Channel",channel);t.putInt("Ring",ring);t.putInt("Volume",volume);t.putInt("Brightness",brightness);t.putInt("Contrast",contrast);var m=new CompoundTag();stored.forEach(m::putString);t.put("Memory",m);}
 public void load(CompoundTag t){super.load(t);if(t.hasUUID("Serial"))serial=t.getUUID("Serial");if(t.hasUUID("Ticket"))ticket=t.getUUID("Ticket");docked=!t.contains("Docked")||t.getBoolean("Docked");bus=!t.contains("Bus")||t.getBoolean("Bus");folded=!t.contains("Folded")||t.getBoolean("Folded");armed=t.getBoolean("Armed");away=t.getBoolean("Away");pin=t.contains("Pin")?t.getString("Pin"):"00";radioPin=t.contains("RadioPin")?t.getString("RadioPin"):"00";fixedPin=t.contains("FixedPin")?t.getString("FixedPin"):"00";number=t.getString("Number");last=t.getString("Last");channel=t.contains("Channel")?Math.max(1,Math.min(16,t.getInt("Channel"))):1;ring=t.contains("Ring")?Math.min(3,Math.max(0,t.getInt("Ring"))):2;volume=t.contains("Volume")?Math.min(3,Math.max(0,t.getInt("Volume"))):2;brightness=t.contains("Brightness")?Math.min(4,Math.max(0,t.getInt("Brightness"))):2;contrast=t.contains("Contrast")?Math.min(4,Math.max(0,t.getInt("Contrast"))):2;stored.clear();var m=t.getCompound("Memory");m.getAllKeys().forEach(k->stored.put(k,m.getString(k)));clientVideo=t.contains("Video")?BlockPos.of(t.getLong("Video")):null;clientLights=t.getInt("Lights");if(level!=null&&level.isClientSide)alarm=t.getString("RenderAlarm");}
 public CompoundTag getUpdateTag(){var t=super.getUpdateTag();var c=camera();if(c!=null)t.putLong("Video",c.asLong());t.putInt("Lights",lights());t.putString("RenderAlarm",alarm);return t;}
}
