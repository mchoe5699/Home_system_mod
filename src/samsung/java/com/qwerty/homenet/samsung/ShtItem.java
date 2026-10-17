package com.qwerty.homenet.samsung;
import com.qwerty.homenet.data.DeviceRegistry;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.server.level.*;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import java.util.function.Consumer;
/** One paired handheld is issued by each fixed station, not an independent placeable telephone. */
public final class ShtItem extends Item {
 public final boolean cordless=true;
 public ShtItem(){super(new Item.Properties().stacksTo(1));}
 @Override public String getDescriptionId(){return cordless?"item."+Samsung.ID+".sht_101c_cordless":"block."+Samsung.ID+".sht_101c";}
 public static Sht101c station(ServerLevel l,ItemStack stack){if(!stack.hasTag()||!stack.getTag().hasUUID("Station")||!l.dimension().location().toString().equals(stack.getTag().getString("Dimension")))return null;for(var e:DeviceRegistry.get(l).all())if(l.isLoaded(e.pos())&&l.getBlockEntity(e.pos()) instanceof Sht101c b&&b.validHandset(stack))return b;return null;}
 @Override public InteractionResultHolder<ItemStack> use(Level l,Player player,InteractionHand hand){if(!cordless)return super.use(l,player,hand);var s=player.getItemInHand(hand);if(!l.isClientSide&&l instanceof ServerLevel sl&&player instanceof ServerPlayer p){if(player.isShiftKeyDown()){var t=s.getOrCreateTag();t.putBoolean("On",!t.getBoolean("On"));p.displayClientMessage(net.minecraft.network.chat.Component.literal(t.getBoolean("On")?"무선수화기 전원 켜짐":"무선수화기 전원 꺼짐"),true);}else{var b=station(sl,s);if(b==null)p.displayClientMessage(net.minecraft.network.chat.Component.literal("등록된 본체를 찾을 수 없습니다"),true);else if(!b.remoteAllowed(p))p.displayClientMessage(net.minecraft.network.chat.Component.literal("무선수화기 전원·충전량·같은 단지·32블록 범위를 확인하세요"),true);else b.open(p,true);}}return InteractionResultHolder.sidedSuccess(s,l.isClientSide);}
 @Override public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext c){return cordless?InteractionResult.PASS:super.useOn(c);}
 @Override public void inventoryTick(ItemStack s,Level l,Entity e,int slot,boolean selected){if(!cordless||l.isClientSide||!s.hasTag()||!s.getTag().getBoolean("On")||l.getGameTime()%20!=0)return;int charge=Math.max(0,s.getTag().getInt("Charge")-20);s.getTag().putInt("Charge",charge);if(charge==600&&e instanceof ServerPlayer p)p.displayClientMessage(net.minecraft.network.chat.Component.literal("무선수화기 배터리 부족 · 본체에 장착하여 충전하세요"),true);}
 @Override public void initializeClient(Consumer<IClientItemExtensions> consumer){consumer.accept(new IClientItemExtensions(){public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){return ShtClient.itemRenderer();}});}
}
