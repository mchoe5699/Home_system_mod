package com.qwerty.homenet.sht;

import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.*;

/** Exposed, BUS-only SHT-305V with a liftable fixed handset. */
public final class Station305 extends Station102 {
 public Station305(BlockPos pos, BlockState state){super(Product.TYPE305.get(),pos,state);bus=true;}
 @Override public boolean supportsWirelessPortable(){return false;}
 @Override public void press(String key,ServerPlayer player,boolean remote){
  if(remote)return;
  if(key.equals("test")){test=true;tell("경보 시험 · 가스·화재 버튼으로 확인");return;}
  if(key.equals("gas_fire_test")){if(!test){tell("시험 버튼을 먼저 누르세요");return;}alarm="fire".equals(alarm)?"gas":"fire";alarmTime=0;tell(("fire".equals(alarm)?"화재":"가스")+" 경보 시험");return;}
  if(key.equals("mode")||key.equals("portable")||key.equals("antenna"))return;
  if(key.equals("restore"))test=false;
  super.press(key,player,false);
 }
 @Override protected java.util.Map<String,String> settingsForClient(){var map=super.settingsForClient();map.put("sht305v","1");map.put("mode","BUS");return map;}
 public static class Mounted extends WallMountedBlock {
  public static final double HEIGHT=.5,WIDTH=.52,DEPTH=.108;
  public Mounted(){this(WIDTH,DEPTH);}
  protected Mounted(double width,double depth){super(Properties.of().strength(1.5f).noOcclusion(),width*8,4,12,depth*16);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(com.qwerty.homenet.block.WallpadBlock.RINGING,false));}
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,com.qwerty.homenet.block.WallpadBlock.RINGING);}
  @Override public RenderShape getRenderShape(BlockState s){return RenderShape.ENTITYBLOCK_ANIMATED;}
  @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Station305(p,s);}
  @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,Product.TYPE305.get(),ReceiverBlockEntity::serverTick);}
  @Override public InteractionResult use(BlockState s,Level l,BlockPos at,Player player,InteractionHand hand,BlockHitResult hit){
   var held=player.getItemInHand(hand);if(held.getItem() instanceof com.qwerty.homenet.item.HomeLinkerItem||held.getItem() instanceof com.qwerty.homenet.item.AnalogLinkerItem)return InteractionResult.PASS;
   if(player instanceof ServerPlayer p&&l.getBlockEntity(at) instanceof Station305 b){Vec3 v=hit.getLocation().subtract(Vec3.atLowerCornerOf(at)).subtract(com.qwerty.homenet.move.DeviceOffsets.get(l,at));double across=switch(s.getValue(FACING)){case NORTH->.5-v.x;case SOUTH->v.x-.5;case EAST->.5-v.z;case WEST->v.z-.5;default->10;};if(across<-.13)b.replace(p);else b.open(p,false);}
   return InteractionResult.sidedSuccess(l.isClientSide);
  }
  @Override public void onRemove(BlockState s,Level l,BlockPos p,BlockState n,boolean moving){if(!s.is(n.getBlock())&&l.getBlockEntity(p) instanceof Station305 b)b.onBroken();super.onRemove(s,l,p,n,moving);}
 }
 public static final class CanvasMounted extends Mounted {
  public static final double WIDTH=HEIGHT*668/600;
  public CanvasMounted(){super(WIDTH,DEPTH);}
 }
}
