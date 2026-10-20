package com.qwerty.homenet.sht;
import com.qwerty.homenet.blockentity.CallerBlockEntity;
import com.qwerty.homenet.intercom.CallState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
/** Wired base handset and separately registered portable accessory. The wired handset has no cable geometry. */
public class Station102 extends Station {
 int heldTicks;
 public Station102(BlockPos p,BlockState s){this(Product.TYPE102.get(),p,s);}
 protected Station102(net.minecraft.world.level.block.entity.BlockEntityType<?> type,BlockPos p,BlockState s){super(type,p,s);folded=false;}
 @Override public boolean wired(){return true;}
 @Override boolean paired(){return true;}
 @Override public boolean owns(ItemStack stack){return ownsPortable(stack);}
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
  if(key.equals("*")&&callState==CallState.CONNECTED&&(incomingKey.equals("lobby")||incomingKey.equals("front_door"))&&audio&&!hold&&(remote?portableActive:!docked&&!portableActive)){openDoor();return;}
  if(remote){super.digit(key,true);return;}
  if(memory&&editing.equals("slot")&&key.matches("[0-9*#]")){editing=key;digits="";update();return;}
  super.digit(key,false);
 }
 @Override public void press(String key,ServerPlayer p,boolean remote){
  if(remote){remotePress(key,p);return;}
  if(key.equals("door"))key="front";
  if(java.util.Set.of("speaker","talk","channel").contains(key))return;
  if(key.equals("handset")){replace(p);return;}
  if(portableActive&&(phone>0||getCallState()!=CallState.IDLE)&&java.util.Set.of("front","line","guard","redial","fire_call","police_call","medical_call").contains(key)){tell("휴대장치 통화 중 · 내선으로 본체에 전환하세요");return;}
  if(key.equals("front")){
   if(memory){editing="front";digits="";update();return;}
   if(callState==CallState.CONNECTED&&(incomingKey.equals("front_door")||incomingKey.equals("lobby"))){tell("현관 영상 · 문열림은 통화 중 *");return;}
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
  if(hold&&phone==3){phoneTime=0;if(++heldTicks>=3600){finish();heldTicks=0;}}
  else heldTicks=0;
  super.tickExtra();
 }

 @Override public void say(ServerPlayer p,String text,boolean remote){if(internal!=3&&remote!=portableActive||!remote&&docked){tell("현재 통화 중인 수화기를 사용하세요");return;}super.say(p,text,remote);}
}
