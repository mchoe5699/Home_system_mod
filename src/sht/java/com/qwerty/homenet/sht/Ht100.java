package com.qwerty.homenet.sht;

import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.blockentity.CallerBlockEntity;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.intercom.DoorStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** HT-100 door camera with a separate, replaceable mounting surround. */
public final class Ht100 extends WallMountedBlock {
 public static final double HEIGHT=.3,WIDTH=HEIGHT*226/300,DEPTH=HEIGHT*53/300;
 public static final BooleanProperty FRAMED=BooleanProperty.create("framed");
 public static final BooleanProperty LIT=BooleanProperty.create("lit");
 public static final BooleanProperty POWERED=BooleanProperty.create("powered");
 public Ht100(){super(Properties.of().strength(1.5f).noOcclusion(),(WIDTH+.012)*8,(.5-HEIGHT/2-.005)*16,(.5+HEIGHT/2+.005)*16,DEPTH*16);registerDefaultState(stateDefinition.any().setValue(FACING,net.minecraft.core.Direction.NORTH).setValue(FRAMED,true).setValue(LIT,false).setValue(POWERED,false));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,FRAMED,LIT,POWERED);}
 @Override public RenderShape getRenderShape(BlockState s){return RenderShape.ENTITYBLOCK_ANIMATED;}
 @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Unit(p,s);}
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,Product.HT100_TYPE.get(),Unit::tick);}
 @Override public InteractionResult use(BlockState s,Level l,BlockPos at,Player p,InteractionHand hand,BlockHitResult hit){
  ItemStack held=p.getItemInHand(hand);
  if(held.getItem() instanceof com.qwerty.homenet.item.DoorLinkerItem)return InteractionResult.PASS;
  if(held.getItem() instanceof com.qwerty.homenet.item.HomeLinkerItem){
   if(!l.isClientSide&&l.getBlockEntity(at) instanceof Unit u){
    var tag=held.getTag();var receiver=tag!=null&&tag.contains("Wallpad")&&tag.getString("Dim").equals(l.dimension().location().toString())&&l.isLoaded(BlockPos.of(tag.getLong("Wallpad")))?l.getBlockEntity(BlockPos.of(tag.getLong("Wallpad"))):null;
    if(receiver instanceof ReceiverBlockEntity r&&!r.getUnit().isEmpty()&&com.qwerty.homenet.data.ZoneData.connected(l,at,r.getBlockPos())){u.setUnit(DeviceRegistry.normalize(r.getUnit()).equals(DeviceRegistry.normalize(u.unit))?"":r.getUnit());p.displayClientMessage(Component.literal(u.unit.isEmpty()?"HT-100 세대 연결 해제":"HT-100 세대 "+u.unit+" 연결"),true);}else p.displayClientMessage(Component.literal("같은 단지의 세대 내부기기를 홈링커로 먼저 선택하세요"),true);
   }
   return InteractionResult.sidedSuccess(l.isClientSide);
  }
  if(held.is(Product.HT100_FRAME.get())){
   if(!s.getValue(FRAMED)&&!l.isClientSide){l.setBlock(at,s.setValue(FRAMED,true),Block.UPDATE_ALL);if(!p.getAbilities().instabuild)held.shrink(1);}
   return InteractionResult.sidedSuccess(l.isClientSide);
  }
  if(p.isShiftKeyDown()&&held.isEmpty()&&s.getValue(FRAMED)){
   if(!l.isClientSide){l.setBlock(at,s.setValue(FRAMED,false),Block.UPDATE_ALL);if(!p.getAbilities().instabuild)p.addItem(new ItemStack(Product.HT100_FRAME.get()));}
   return InteractionResult.sidedSuccess(l.isClientSide);
  }
  if(button(s,l,at,hit)&&!l.isClientSide&&l.getBlockEntity(at) instanceof Unit u)u.call();
  return InteractionResult.sidedSuccess(l.isClientSide);
 }
 static boolean button(BlockState s,Level l,BlockPos at,BlockHitResult hit){
  var v=hit.getLocation().subtract(net.minecraft.world.phys.Vec3.atLowerCornerOf(at)).subtract(com.qwerty.homenet.move.DeviceOffsets.get(l,at));
  double across=switch(s.getValue(FACING)){case NORTH->.5-v.x;case SOUTH->v.x-.5;case EAST->.5-v.z;case WEST->v.z-.5;default->10;};
  double scale=HEIGHT/300,x=113+across/scale,y=(.5+HEIGHT/2-v.y)/scale;
  return x>=137&&x<=210&&y>=196&&y<=275;
 }
 @Override public void tick(BlockState state,ServerLevel l,BlockPos pos,net.minecraft.util.RandomSource random){if(state.getValue(POWERED))l.setBlock(pos,state.setValue(POWERED,false),Block.UPDATE_ALL);}
 @Override public boolean isSignalSource(BlockState s){return true;}
 @Override public int getSignal(BlockState s,net.minecraft.world.level.BlockGetter l,BlockPos p,net.minecraft.core.Direction d){return s.getValue(POWERED)?15:0;}
 @Override public void onRemove(BlockState s,Level l,BlockPos p,BlockState n,boolean moving){if(!s.is(n.getBlock())&&l.getBlockEntity(p) instanceof Unit u)u.onBroken();super.onRemove(s,l,p,n,moving);}
 public static final class Unit extends CallerBlockEntity {
  String unit="";
  public Unit(BlockPos p,BlockState s){super(Product.HT100_TYPE.get(),p,s);}
  @Override public DeviceRegistry.Kind kind(){return DeviceRegistry.Kind.DOOR_CAMERA;}
  @Override public String callerKey(){return "front_door";}
  @Override public String registeredUnit(){return unit;}
  public void setUnit(String value){unit=value==null?"":value;setChanged();registerDevice();}
  public void call(){if(!(level instanceof ServerLevel))return;if(inCall){hangUpFromCaller();setLamp(false);return;}if(unit.isEmpty()){level.players().stream().filter(p->p.distanceToSqr(worldPosition.getCenter())<64).forEach(p->p.displayClientMessage(Component.literal("HT-100 세대를 홈링커로 연결하세요"),true));return;}if(startCall(unit)==CallResult.OK){setLamp(true);level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(),net.minecraft.sounds.SoundSource.BLOCKS,.6f,1.4f);}}
  void setLamp(boolean value){if(level!=null&&getBlockState().getValue(LIT)!=value)level.setBlock(worldPosition,getBlockState().setValue(LIT,value),Block.UPDATE_ALL);}
  @Override protected void onEnded(DoorStatus why){setLamp(false);}
  @Override protected void onDoorOpenedByReceiver(){setLamp(false);}
  @Override public void syncScreens(){}
  @Override public void onLoad(){super.onLoad();if(level!=null&&!level.isClientSide)com.qwerty.homenet.move.DeviceMoves.applyPending(this);}
  @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(){return new net.minecraft.world.phys.AABB(worldPosition).move(com.qwerty.homenet.move.DeviceOffsets.get(level,worldPosition));}
  @Override public void load(CompoundTag t){super.load(t);unit=t.getString("UnitNo");}
  @Override protected void saveAdditional(CompoundTag t){super.saveAdditional(t);t.putString("UnitNo",unit);}
  static void tick(Level l,BlockPos p,BlockState s,Unit u){u.tickCaller();}
 }
}
