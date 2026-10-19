package com.qwerty.homenet.sht;

import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.blockentity.CallerBlockEntity;
import com.qwerty.homenet.data.DeviceRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** HT-100S door phone, sold with its mounting surround in one installable block. */
public final class Ht100s extends WallMountedBlock {
 public static final double HEIGHT=.15,WIDTH=HEIGHT*230/300,FRAME_WIDTH=HEIGHT*242/300,FRAME_HEIGHT=HEIGHT*310/300,DEPTH=HEIGHT*91/300;
 public Ht100s(){super(Properties.of().strength(1.5f).noOcclusion(),FRAME_WIDTH*8,(.5-FRAME_HEIGHT/2)*16,(.5+FRAME_HEIGHT/2)*16,DEPTH*16);registerDefaultState(stateDefinition.any().setValue(FACING,net.minecraft.core.Direction.NORTH));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
 @Override public RenderShape getRenderShape(BlockState s){return RenderShape.ENTITYBLOCK_ANIMATED;}
 @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Unit(p,s);}
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,Product.HT100S_TYPE.get(),Unit::tick);}
 @Override public InteractionResult use(BlockState s,Level l,BlockPos at,Player player,InteractionHand hand,BlockHitResult hit){
  ItemStack held=player.getItemInHand(hand);
  if(held.getItem() instanceof com.qwerty.homenet.item.HomeLinkerItem||held.getItem() instanceof com.qwerty.homenet.item.DoorLinkerItem)return InteractionResult.PASS;
  if(!l.isClientSide&&l.getBlockEntity(at) instanceof Unit u){if(button(s,l,at,hit))u.call();else player.displayClientMessage(Component.literal("HT-100S · 홈링커로 세대 내부기기 연결 · 오른쪽 아래 버튼으로 호출"),true);}
  return InteractionResult.sidedSuccess(l.isClientSide);
 }
 static boolean button(BlockState s,Level l,BlockPos at,BlockHitResult hit){
  Vec3 v=hit.getLocation().subtract(Vec3.atLowerCornerOf(at)).subtract(com.qwerty.homenet.move.DeviceOffsets.get(l,at));
  double across=switch(s.getValue(FACING)){case NORTH->.5-v.x;case SOUTH->v.x-.5;case EAST->.5-v.z;case WEST->v.z-.5;default->10;};
  double x=115+across/(HEIGHT/300),y=(.5+HEIGHT/2-v.y)/(HEIGHT/300);
  return x>=141&&x<=209&&y>=203&&y<=275;
 }
 @Override public void onRemove(BlockState s,Level l,BlockPos p,BlockState n,boolean moving){if(!s.is(n.getBlock())&&l.getBlockEntity(p) instanceof Unit u)u.onBroken();super.onRemove(s,l,p,n,moving);}
 public static final class Unit extends CallerBlockEntity {
  String unit="";
  public Unit(BlockPos p,BlockState s){super(Product.HT100S_TYPE.get(),p,s);}
  @Override public DeviceRegistry.Kind kind(){return DeviceRegistry.Kind.DOOR_PHONE;}
  @Override public String callerKey(){return "front_door";}
  @Override public String registeredUnit(){return unit;}
  public void setUnit(String value){unit=value==null?"":value;setChanged();registerDevice();}
  public void call(){if(!(level instanceof ServerLevel))return;if(inCall){hangUpFromCaller();return;}if(unit.isBlank()){nearby("HT-100S를 홈링커로 세대 내부기기에 연결하세요");return;}var result=startCall(unit);if(result==CallResult.OK)level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(),net.minecraft.sounds.SoundSource.BLOCKS,.6f,1.4f);else nearby("HT-100S 호출 실패: "+switch(result){case NO_UNIT,NO_SIGNAL->"연결된 세대 내부기기를 찾을 수 없습니다";case BUSY->"통화 중입니다";default->"호출할 수 없습니다";});}
  private void nearby(String message){level.players().stream().filter(p->p.distanceToSqr(worldPosition.getCenter())<64).forEach(p->p.displayClientMessage(Component.literal(message),true));}
  @Override public void syncScreens(){}
  @Override public void onLoad(){super.onLoad();if(level!=null&&!level.isClientSide)com.qwerty.homenet.move.DeviceMoves.applyPending(this);}
  @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(){return new net.minecraft.world.phys.AABB(worldPosition).move(com.qwerty.homenet.move.DeviceOffsets.get(level,worldPosition));}
  @Override public void load(CompoundTag tag){super.load(tag);unit=tag.getString("UnitNo");}
  @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putString("UnitNo",unit);}
  static void tick(Level l,BlockPos p,BlockState s,Unit u){u.tickCaller();}
 }
}
