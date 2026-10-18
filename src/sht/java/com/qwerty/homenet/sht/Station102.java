package com.qwerty.homenet.sht;
import com.qwerty.homenet.blockentity.CallerBlockEntity;
import com.qwerty.homenet.intercom.CallState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
/** Wired base handset and separately registered portable accessory. The wired handset has no cable geometry. */
public final class Station102 extends Station {
 int heldTicks,transfer,transferTicks;boolean portableRegistered,portableActive;
 public Station102(BlockPos p,BlockState s){super(Product.TYPE102.get(),p,s);folded=false;}
 @Override public boolean wired(){return true;}
 @Override boolean paired(){return true;}
 @Override public boolean owns(ItemStack stack){return portableRegistered&&stack.is(Product.PORTABLE102.get())&&stack.hasTag()&&stack.getTag().getLong("base")==worldPosition.asLong()&&(level==null||stack.getTag().getString("dimension").equals(level.dimension().location().toString()))&&stack.getTag().hasUUID("serial")&&serial.equals(stack.getTag().getUUID("serial"))&&stack.getTag().hasUUID("ticket")&&ticket.equals(stack.getTag().getUUID("ticket"));}
 @Override public boolean allowed(ServerPlayer p,boolean remote){if(!remote)return super.allowed(p,false);if(p.level()!=level||!same(p.blockPosition())||p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(worldPosition).add(com.qwerty.homenet.move.DeviceOffsets.get(level,worldPosition)))>1024)return false;for(int i=0;i<p.getInventory().getContainerSize();i++){var s=p.getInventory().getItem(i);if(owns(s)&&s.getTag().getBoolean("power")&&s.getTag().getInt("battery")>0)return true;}return false;}
 @Override public void lift(ServerPlayer p){
  if(!docked)return;docked=false;
  if(internal==2){acceptInternal(false);update();return;}
  if(internal==3){audio=true;update();return;}
  if(portableActive){tell("휴대장치 통화 중 · 내선으로 전환하세요");return;}
  portableActive=false;
  if(callState==CallState.RINGING||phone==2){audio=false;talk();}
  else if(callState==CallState.CONNECTED||callState==CallState.DIALING||phone>0){audio=true;hold=false;}
  else if(monitor>0){startDoorTalk();}
  else{audio=true;tell("국선 발신음 · 번호 입력");}
  update();
 }
 @Override public void replace(ServerPlayer p){
  if(docked){lift(p);return;}
  docked=true;
  if(internal==3){finish();tell("내선 통화 종료 · 유선 수화기 거치");}
  else if(portableActive||transfer==1){tell("유선 수화기 거치 · 휴대장치 연결 유지");}
  else if(phone==3&&hold){audio=false;tell("국선 보류 · 수화기 거치");}
  else{finish();tell("수화기 거치 · 대기");}
 }
 void startDoorTalk(){var at=linkedCamera();if(at!=null&&level.getBlockEntity(at) instanceof CallerBlockEntity c){c.callFromReceiver(this);audio=true;answer();tell("현관 통화");}else tell("연결된 도어카메라가 없습니다");}
 @Override public boolean startRinging(BlockPos at,String key){boolean accepted=super.startRinging(at,key);if(accepted)tell(portableRegistered?"착신 · 유선 수화기 또는 휴대장치로 응답":"착신 · 유선 수화기를 드세요");return accepted;}
 @Override public void digit(String key,boolean remote){
  if(remote){super.digit(key,true);return;}
  if(memory&&editing.equals("slot")&&key.matches("[0-9*#]")){editing=key;digits="";update();return;}
  super.digit(key,false);
 }
 @Override public void press(String key,ServerPlayer p,boolean remote){
  if(remote){remotePress(key,p);return;}
  if(key.equals("portable")){if(portableRegistered){tell("휴대장치는 별도 충전기에서 들고 거치합니다");return;}var item=new ItemStack(Product.PORTABLE102.get());if(p.getInventory().getFreeSlot()<0)tell("인벤토리 공간이 필요합니다");else{Portable.bind(p,this,item);p.getInventory().add(item);}return;}
  if(java.util.Set.of("speaker","talk","channel").contains(key))return;
  if(key.equals("handset")){replace(p);return;}
  if(portableActive&&(phone>0||getCallState()!=CallState.IDLE)&&java.util.Set.of("front","line","guard","redial","fire_call","police_call","medical_call").contains(key)){tell("휴대장치 통화 중 · 내선으로 본체에 전환하세요");return;}
  if(key.equals("front")){
   if(memory){editing="front";digits="";update();return;}
   if(callState==CallState.CONNECTED&&(incomingKey.equals("front_door")||incomingKey.equals("lobby"))){openDoor();return;}
   finish();monitor=600;
   if(!docked)startDoorTalk();else tell("현관 영상 감시 · 수화기를 들면 통화");return;
  }
  if(key.equals("line")){
   if(memory){editing=editing.equals("slot")?"number":"slot";digits="";tell("단축키 선택 · 국선을 다시 누르면 자택 전화번호");return;}
   if(verify){verify=false;tell("자택 전화번호: "+number);return;}
   if(phone==3&&hold){hold=false;heldTicks=0;audio=!docked;tell("국선 재통화 · 수화기를 드세요");return;}
   if(phone>0||audio&&callState==CallState.IDLE&&monitor==0||status.startsWith("국선 선택")){finish();return;}
   finish();audio=!docked;tell("국선 선택 · 번호 입력 · 응답 후 수화기를 드세요");return;
  }
  if(key.equals("guard")&&!memory&&!verify){audio=!docked;callGuard(p,"");return;}
  if(key.equals("redial")&&!memory&&!verify){finish();audio=!docked;if(!last.isEmpty())dialNumber(last);else tell("재다이얼 번호가 없습니다");return;}
  if(java.util.Set.of("fire_call","police_call","medical_call").contains(key)&&!memory&&!verify){finish();audio=!docked;dialNumber(stored.getOrDefault("B:"+key,""));return;}
  if(key.equals("intercom")&&!memory&&!verify){requestInternal(false);return;}
  super.press(key,p,false);
 }
 @Override public int lights(){int bits=super.lights()&~Indicators.CHARGE;return status.startsWith("국선 선택")?bits|Indicators.LINE:bits;}
 @Override protected void tickExtra(){
  if(internal==1||internal==2){if(++transferTicks>=600){if(transfer!=0){portableActive=transfer==2;hold=false;audio=true;internal=transfer=0;tell("통화전환 응답 없음 · 원래 통화로 복귀");}else finish();transferTicks=0;}}else transferTicks=0;
  if(hold&&phone==3){phoneTime=0;if(++heldTicks>=3600){finish();heldTicks=0;}}
  else heldTicks=0;
  super.tickExtra();
 }

 void requestInternal(boolean remote){
  if(!portableRegistered){tell("등록된 SHT-102C 휴대장치가 없습니다");return;}
  if(transfer!=0){portableActive=transfer==2;internal=transfer=0;hold=false;audio=true;tell("통화전환 취소");return;}
  if(internal==3){finish();return;}
  if(phone==3||getCallState()==CallState.CONNECTED){transfer=remote?2:1;internal=transfer;hold=phone==3;audio=false;transferTicks=0;tell(remote?"본체로 통화전환 · 유선 수화기를 드세요":"휴대장치로 통화전환 · 통화 버튼으로 응답");return;}
  finish();portableActive=remote;internal=remote?2:1;transferTicks=0;tell(remote?"본체 내선 호출 · 유선 수화기로 응답":"휴대장치 내선 호출 · 통화로 응답");
 }
 void acceptInternal(boolean remote){if(transfer!=0){internal=transfer=0;hold=false;portableActive=remote;audio=true;tell(remote?"휴대장치로 통화전환 완료":"본체로 통화전환 완료");}else{internal=3;audio=true;tell("본체 ↔ SHT-102C 휴대장치 내선 통화");}}
 void portablePickup(ServerPlayer p){if(!allowed(p,true))return;if(internal==1)acceptInternal(true);else if(phone==2||getCallState()==CallState.RINGING){portableActive=true;audio=false;talk();}update();}
 void portableReturn(){if(transfer==2){tell("휴대장치 거치 · 본체의 전환 응답 대기");return;}if(portableActive||internal==3)finish();}
 void remotePress(String key,ServerPlayer p){
  if(!portableActive&&transfer!=1&&(phone==3||getCallState()==CallState.CONNECTED||getCallState()==CallState.DIALING)){tell("본체 통화 중 · 본체 내선 버튼으로 전환하세요");return;}
  if(key.equals("intercom")){requestInternal(true);return;}
  if(key.equals("talk")){
   if(internal==1){acceptInternal(true);return;}
   if((phone==3||getCallState()==CallState.CONNECTED)&&!portableActive){tell("본체 통화 중 · 본체 내선 버튼으로 전환하세요");return;}
   portableActive=true;talk();return;
  }
  if(key.equals("onhook")){if(portableActive&&audio){finish();return;}if(phone>0||getCallState()!=CallState.IDLE){remotePress("talk",p);return;}finish();portableActive=true;audio=true;tell("휴대장치 온후크 · 국선 번호 입력");return;}
  if(key.equals("memory")){super.press(key,p,true);return;}
  if(memory){super.press(key,p,true);return;}
  if(key.equals("flash")){if(phone==3){hold=!hold;audio=!hold;tell(hold?"휴대장치 국선 보류":"휴대장치 국선 재통화");}else{phoneTime=-60;tell("포즈 · 번호 입력");}return;}
  if(key.equals("redial")){finish();portableActive=true;audio=true;if(!last.isEmpty())dialNumber(last);else tell("재다이얼 번호가 없습니다");return;}
  if(key.equals("door")||key.equals("guard")){portableActive=true;super.press(key,p,true);return;}
 }
 @Override public void say(ServerPlayer p,String text,boolean remote){if(internal!=3&&remote!=portableActive||!remote&&docked){tell("현재 통화 중인 수화기를 사용하세요");return;}super.say(p,text,remote);}
 @Override void finish(){portableActive=false;transfer=transferTicks=0;super.finish();}
 @Override protected java.util.Map<String,String> settingsForClient(){var s=super.settingsForClient();s.put("portable_registered",portableRegistered?"1":"0");s.put("portable_active",portableActive?"1":"0");return s;}
 @Override protected void saveAdditional(net.minecraft.nbt.CompoundTag t){super.saveAdditional(t);t.putBoolean("PortableRegistered",portableRegistered);}
 @Override public void load(net.minecraft.nbt.CompoundTag t){super.load(t);portableRegistered=t.getBoolean("PortableRegistered");}
}
