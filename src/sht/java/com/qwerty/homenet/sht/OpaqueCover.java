package com.qwerty.homenet.sht;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Conservative cover sampling: glass, leaves and partial blocks never hide a whole device. */
final class OpaqueCover {
 static boolean covered(Level level,Vec3 eye,AABB box,Entity viewer){
  if(box.inflate(.15).contains(eye))return false;
  if(level.getBlockState(BlockPos.containing(eye)).isSolidRender(level,BlockPos.containing(eye)))return false;
  for(int x=0;x<3;x++)for(int y=0;y<3;y++)for(int z=0;z<3;z++){
   var target=new Vec3(box.minX+(box.maxX-box.minX)*x/2,box.minY+(box.maxY-box.minY)*y/2,box.minZ+(box.maxZ-box.minZ)*z/2);
   var context=new ClipContext(eye,target,ClipContext.Block.VISUAL,ClipContext.Fluid.NONE,viewer){
    @Override public VoxelShape getBlockShape(BlockState state,BlockGetter world,BlockPos pos){return state.isSolidRender(world,pos)?Shapes.block():Shapes.empty();}
   };
   if(level.clip(context).getType()==HitResult.Type.MISS)return false;
  }
  return true;
 }
}
