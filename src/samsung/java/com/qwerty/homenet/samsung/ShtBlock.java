package com.qwerty.homenet.samsung;
import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.move.DeviceOffsets;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
public final class ShtBlock extends WallMountedBlock {
 public ShtBlock(){super(Properties.of().strength(1.5f).noOcclusion(),.538*8,4.8,4.8+.400*16,1.50);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(BlockStateProperties.POWERED,false));}
 protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(FACING,BlockStateProperties.POWERED);}
 public RenderShape getRenderShape(BlockState s){return RenderShape.ENTITYBLOCK_ANIMATED;}
 public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return DeviceOffsets.shape(super.getShape(s,l,p,c),l,p);}
 public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Sht101c(p,s);}
 public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,Samsung.BE.get(),ReceiverBlockEntity::serverTick);}
 public InteractionResult use(BlockState s,Level l,BlockPos at,Player player,InteractionHand hand,BlockHitResult hit){
  if(player.getItemInHand(hand).getItem() instanceof com.qwerty.homenet.item.HomeLinkerItem||player.getItemInHand(hand).getItem() instanceof com.qwerty.homenet.item.AnalogLinkerItem)return InteractionResult.PASS;
  if(!l.isClientSide&&player instanceof ServerPlayer p&&l.getBlockEntity(at) instanceof Sht101c b){if(player.getItemInHand(hand).is(Samsung.CORDLESS.get()))b.dock(p,player.getItemInHand(hand));else b.openFor(p);}
  return InteractionResult.sidedSuccess(l.isClientSide);
 }
 public void onRemove(BlockState s,Level l,BlockPos p,BlockState n,boolean moving){if(!s.is(n.getBlock())&&l.getBlockEntity(p) instanceof Sht101c b)b.onBroken();super.onRemove(s,l,p,n,moving);}
}
