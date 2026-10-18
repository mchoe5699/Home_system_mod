package com.qwerty.homenet.sht;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
/** SHT-102C accessory identity is independent of the wired handset and the SHT-101C radio. */
public final class Portable extends Item {
 static boolean supports(com.qwerty.homenet.blockentity.ReceiverBlockEntity r){return r instanceof Station s&&s.manufacturer().equalsIgnoreCase("SAMSUNG")&&s.supportsWirelessPortable()&&java.util.Set.of(com.qwerty.homenet.data.DeviceRegistry.Kind.WALLPAD,com.qwerty.homenet.data.DeviceRegistry.Kind.VIDEO_PHONE,com.qwerty.homenet.data.DeviceRegistry.Kind.INTERPHONE).contains(s.kind());}
 static boolean carried(ServerPlayer p,Station s){for(int i=0;i<p.getInventory().getContainerSize();i++){var item=p.getInventory().getItem(i);if(s.ownsPortable(item)&&item.getTag().getBoolean("power")&&item.getTag().getInt("battery")>0)return true;}return false;}
 static final int CAPACITY=36000;
 Portable(){super(new Properties().stacksTo(1));}
 static Station base(Level l,ItemStack stack){
  if(!stack.is(Product.PORTABLE102.get())||!stack.hasTag())return null;var t=stack.getTag();
  if(!t.getString("dimension").equals(l.dimension().location().toString())||!t.contains("base"))return null;
  var at=BlockPos.of(t.getLong("base"));return l.isLoaded(at)&&l.getBlockEntity(at) instanceof Station s&&supports(s)&&s.ownsPortable(stack)?s:null;
 }
 static boolean bind(ServerPlayer p,Station s,ItemStack stack){
  if(!s.allowed(p,false))return false;return bindFromLinker(p,s,stack);
 }
 static boolean bindFromLinker(ServerPlayer p,Station s,ItemStack stack){
  if(!supports(s)||p.level()!=s.getLevel()||!stack.is(Product.PORTABLE102.get()))return false;
  if(s.ownsPortable(stack)){s.tell("등록된 삼성 휴대장치입니다");return true;}
  if(s.portableRegistered&&!p.isShiftKeyDown()){s.tell("이미 휴대장치가 등록되어 있습니다 · 재등록은 웅크리고 클릭");return false;}
  if(s.portableRegistered)s.portableTicket=java.util.UUID.randomUUID();s.portableRegistered=true;
  var t=stack.getOrCreateTag();t.putUUID("serial",s.serial);t.putUUID("ticket",s.portableTicket);t.putLong("base",s.getBlockPos().asLong());t.putString("dimension",p.level().dimension().location().toString());t.putBoolean("power",true);if(!t.contains("battery"))t.putInt("battery",CAPACITY);
  s.tell("삼성 휴대장치 연결 완료");return true;
 }
 public InteractionResultHolder<ItemStack> use(Level l,Player player,InteractionHand hand){var stack=player.getItemInHand(hand);if(player instanceof ServerPlayer p){
  if(player.isShiftKeyDown()){var t=stack.getOrCreateTag();t.putBoolean("power",!t.getBoolean("power"));}
  else{var s=base(l,stack);if(s!=null)s.openPortable(p);else p.displayClientMessage(net.minecraft.network.chat.Component.literal("홈링커로 삼성 무선 지원 내부기기와 휴대장치 충전기를 연결하세요"),true);}
 }return InteractionResultHolder.sidedSuccess(stack,l.isClientSide);}
 public void inventoryTick(ItemStack stack,Level l,Entity entity,int slot,boolean selected){
  if(l.isClientSide||l.getGameTime()%20!=0||!(entity instanceof ServerPlayer p))return;var t=stack.getOrCreateTag();var s=base(l,stack);
  boolean powered=t.getBoolean("power")&&t.getInt("battery")>0;if(powered)t.putInt("battery",Math.max(0,t.getInt("battery")-20));
  boolean inRange=powered&&s!=null&&s.allowed(p,true);boolean ringing=inRange&&(s.phone==2||s.getCallState()==com.qwerty.homenet.intercom.CallState.RINGING||s.internal==1);
  t.putBoolean("ringing",ringing);t.putBoolean("talking",inRange&&s.audio&&(s.portableActive||s.internal==3));
  if(ringing)com.qwerty.homenet.intercom.CallSounds.note(l,p.blockPosition(),net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BELL.value(),s.ring*.25f,1.1f);
  if(s!=null&&(s.portableActive||s.internal==3)&&!inRange){s.finish();s.tell("휴대장치 전원·배터리·통신 범위 확인");}
 }
 @Override public boolean shouldCauseReequipAnimation(ItemStack a,ItemStack b,boolean slot){if(slot||a.getItem()!=b.getItem())return true;var x=a.getTag();var y=b.getTag();return x==null||y==null?!java.util.Objects.equals(x,y):!java.util.Objects.equals(x.get("serial"),y.get("serial"))||!java.util.Objects.equals(x.get("ticket"),y.get("ticket"));}
 public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> c){c.accept(Client.itemExtension());}
}
