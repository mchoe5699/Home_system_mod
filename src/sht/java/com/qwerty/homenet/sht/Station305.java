package com.qwerty.homenet.sht;

import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.intercom.CallState;
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

/** Surface-mounted SHT-305V BUS video phone with a fixed, liftable handset. */
public final class Station305 extends Station102 {
 public Station305(BlockPos p,BlockState s){super(Product.TYPE305.get(),p,s);bus=true;}
 @Override public boolean supportsWirelessPortable(){return false;}
 @Override public void press(String key,ServerPlayer p,boolean remote){
  if(remote)return;
  switch(key){
   case "mode","portable","antenna","line","redial","fire_call","police_call","medical_call","intercom"->tell("SHT-305V 본체에 없는 조작입니다");
   case "door_open"->{if(callState==CallState.CONNECTED&&(incomingKey.equals("front_door")||incomingKey.equals("lobby")))openDoor();else tell("현관과 통화 중일 때 문을 열 수 있습니다");}
   case "gas_fire_test"->{if(!test){tell("시험 버튼을 먼저 누르세요");return;}alarm="fire".equals(alarm)?"gas":"fire";alarmTime=0;tell(("gas".equals(alarm)?"가스":"화재")+" 경보 시험 · 경보정지 후 복귀");}
   case "gas_test"->{if(!test){tell("시험 버튼을 먼저 누르세요");return;}alarm="gas";alarmTime=0;tell("가스 경보 시험 · 경보정지 후 복귀");}
   case "test"->{test=true;tell("경보 자체 시험 · 가스·화재 버튼으로 확인");}
   case "stop"->{super.press(key,p,false);}
   case "restore"->{super.press(key,p,false);test=false;}
   default->super.press(key,p,false);
  }
 }
 @Override protected java.util.Map<String,String> settingsForClient(){var values=super.settingsForClient();values.put("sht305v","1");values.put("mode","BUS");return values;}
 public static final class Mounted extends WallMountedBlock {
  public static final double HEIGHT=.5,WIDTH=HEIGHT*660/600,DEPTH=HEIGHT*125/600;
  public Mounted(){super(Properties.of().strength(1.5f).noOcclusion(),WIDTH*8,(.5-HEIGHT/2)*16,(.5+HEIGHT/2)*16,DEPTH*16);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(com.qwerty.homenet.block.WallpadBlock.RINGING,false));}
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,com.qwerty.homenet.block.WallpadBlock.RINGING);}
  @Override public RenderShape getRenderShape(BlockState s){return RenderShape.ENTITYBLOCK_ANIMATED;}
  @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Station305(p,s);}
  @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,Product.TYPE305.get(),com.qwerty.homenet.blockentity.ReceiverBlockEntity::serverTick);}
  @Override public InteractionResult use(BlockState s,Level l,BlockPos at,Player player,InteractionHand hand,BlockHitResult hit){
   var held=player.getItemInHand(hand);
   if(held.getItem() instanceof com.qwerty.homenet.item.HomeLinkerItem||held.getItem() instanceof com.qwerty.homenet.item.AnalogLinkerItem)return InteractionResult.PASS;
   if(player instanceof ServerPlayer p&&l.getBlockEntity(at) instanceof Station305 b){if(handset(s,l,at,hit))b.replace(p);else b.open(p,false);}
   return InteractionResult.sidedSuccess(l.isClientSide);
  }
  static boolean handset(BlockState s,Level l,BlockPos at,BlockHitResult hit){
   Vec3 v=hit.getLocation().subtract(Vec3.atLowerCornerOf(at)).subtract(com.qwerty.homenet.move.DeviceOffsets.get(l,at));
   double across=switch(s.getValue(FACING)){case NORTH->.5-v.x;case SOUTH->v.x-.5;case EAST->.5-v.z;case WEST->v.z-.5;default->10;};
   double x=330+across/(HEIGHT/600),y=(.5+HEIGHT/2-v.y)/(HEIGHT/600);
   return x>=0&&x<=189&&y>=8&&y<=590;
  }
  @Override public void onRemove(BlockState s,Level l,BlockPos p,BlockState n,boolean moving){if(!s.is(n.getBlock())&&l.getBlockEntity(p) instanceof Station305 b)b.onBroken();super.onRemove(s,l,p,n,moving);}
 }
}
