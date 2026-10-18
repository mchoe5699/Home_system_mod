package com.qwerty.homenet.sht;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
/** Tabletop charging stand: the complete docked assembly occupies exactly .4 by .4 blocks. */
public final class Charger extends Block implements EntityBlock {
 public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
 Charger(){super(Properties.of().strength(1.2f).noOcclusion());registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
 protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
 public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
 public RenderShape getRenderShape(BlockState s){return RenderShape.ENTITYBLOCK_ANIMATED;}
 public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,net.minecraft.world.phys.shapes.CollisionContext c){boolean full=l.getBlockEntity(p) instanceof Dock d&&!d.stack.isEmpty();return com.qwerty.homenet.move.DeviceOffsets.shape(Block.box(4.8,0,4.8,11.2,full?14.5:2.4,11.2),l,p);}
 public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Dock(p,s);}
 @SuppressWarnings("unchecked") public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return !l.isClientSide&&t==Product.CHARGER_TYPE.get()?(level,pos,state,be)->((Dock)be).tick():null;}
 public InteractionResult use(BlockState s,Level l,BlockPos at,Player player,InteractionHand hand,BlockHitResult hit){if(player instanceof ServerPlayer p&&l.getBlockEntity(at) instanceof Dock d){var held=p.getItemInHand(hand);
  if(d.stack.isEmpty()&&held.is(Product.PORTABLE102.get())){var b=Portable.base(l,held);if(b!=null)b.portableReturn();d.stack=held.split(1);d.sync();}
  else if(held.isEmpty()&&!d.stack.isEmpty()){var item=d.stack.copy();if(p.getInventory().add(item)){var b=Portable.base(l,d.stack);d.stack=ItemStack.EMPTY;d.sync();if(b!=null)b.portablePickup(p);}else p.displayClientMessage(net.minecraft.network.chat.Component.literal("인벤토리 공간이 필요합니다"),true);}
 }return InteractionResult.sidedSuccess(l.isClientSide);}
 public void onRemove(BlockState s,Level l,BlockPos p,BlockState n,boolean moving){if(!s.is(n.getBlock())&&l.getBlockEntity(p) instanceof Dock d&&!l.isClientSide){Containers.dropItemStack(l,p.getX()+.5,p.getY()+.2,p.getZ()+.5,d.stack);d.stack=ItemStack.EMPTY;}super.onRemove(s,l,p,n,moving);}
 public static final class Dock extends BlockEntity {
  @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(){return new net.minecraft.world.phys.AABB(worldPosition).move(com.qwerty.homenet.move.DeviceOffsets.get(level,worldPosition));}
  ItemStack stack=ItemStack.EMPTY;
  Dock(BlockPos p,BlockState s){super(Product.CHARGER_TYPE.get(),p,s);}
  @Override public void onLoad(){super.onLoad();if(level!=null&&!level.isClientSide)com.qwerty.homenet.move.DeviceMoves.applyPending(this);}
  void tick(){if(level.getGameTime()%20!=0||stack.isEmpty())return;if(refresh())sync();}
  boolean refresh(){if(stack.isEmpty())return false;var before=stack.getOrCreateTag().copy();var t=stack.getOrCreateTag();int battery=t.getInt("battery");t.putInt("battery",Math.min(Portable.CAPACITY,battery+120));var base=Portable.base(level,stack);boolean ringing=base!=null&&base.same(worldPosition)&&base.getBlockPos().distSqr(worldPosition)<=1024&&t.getBoolean("power")&&base.portableRinging();t.putBoolean("intercom",base!=null&&base.internal>0&&ringing);t.putBoolean("ringing",ringing);t.putBoolean("talking",false);if(ringing)com.qwerty.homenet.intercom.CallSounds.note(level,worldPosition,net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BELL.value(),base.ring*.25f,1.1f);return !before.equals(t);}
  void sync(){setChanged();level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
  protected void saveAdditional(CompoundTag t){super.saveAdditional(t);t.put("Portable",stack.save(new CompoundTag()));}
  public void load(CompoundTag t){super.load(t);stack=ItemStack.of(t.getCompound("Portable"));}
  public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
  public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
 }
}
