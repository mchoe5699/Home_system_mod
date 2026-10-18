package com.qwerty.homenet.sht;

import com.qwerty.homenet.block.WallMountedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;

/** Model-only digital BUS entrance station. No invented calling or door-release behavior. */
public final class Ht512 extends WallMountedBlock {
 public static final double HEIGHT=9.333334/16,WIDTH=HEIGHT*464/610,DEPTH=HEIGHT*20/610;
 public Ht512(){super(Properties.of().strength(1.5f).noOcclusion(),WIDTH*8,(.5-HEIGHT/2)*16,(.5+HEIGHT/2)*16,DEPTH*16);registerDefaultState(stateDefinition.any().setValue(FACING,net.minecraft.core.Direction.NORTH));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
 @Override public RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
 @Override public BlockEntity newBlockEntity(BlockPos at,BlockState state){return new Unit(at,state);}
 @Override public InteractionResult use(BlockState state,Level level,BlockPos at,Player player,InteractionHand hand,BlockHitResult hit){
  if(holdingLinker(player,hand))return InteractionResult.PASS;
  if(level.isClientSide)net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->Ht512Visual.open());
  return InteractionResult.sidedSuccess(level.isClientSide);
 }
 public static final class Unit extends BlockEntity {
  public Unit(BlockPos pos,BlockState state){super(Product.HT512_TYPE.get(),pos,state);}
  @Override public void onLoad(){super.onLoad();if(level!=null&&!level.isClientSide)com.qwerty.homenet.move.DeviceMoves.applyPending(this);}
  @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(){return new net.minecraft.world.phys.AABB(worldPosition).move(com.qwerty.homenet.move.DeviceOffsets.get(level,worldPosition));}
 }
}
