package com.qwerty.homenet.sht;

import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.intercom.CallState;
import com.qwerty.homenet.intercom.Intercom;
import com.qwerty.homenet.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
    modid = "homenet_sht101c"
)
final class Sht104Family {
    private Sht104Family() {
    }

    static boolean member(Block block) {
        return block instanceof Station104M;
    }

    static Block block(String name) {
        return (Block)(switch (name) {
            case "SHT-104M" -> (Station104M)Product.STATION104M.get();
            case "SHT-104C" -> (Station104C)Product.STATION104C.get();
            case "SHT-104CS" -> (Station104CS)Product.STATION104CS.get();
            default -> null;
        });
    }

    @SubscribeEvent(
        priority = EventPriority.HIGHEST
    )
    public static void useTool(RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        if (member(level.getBlockState(pos).getBlock()) && event.getItemStack().is(ModItems.MODEL_CHANGER.get())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
            if (level.isClientSide) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Client.family(pos));
            }
        }
    }

    static boolean validVariant(String name, String variant) {
        return variant != null && variant.matches(name.equals("SHT-104CS") ? "[ABC]" : "[ABCD]");
    }

    static boolean change(ServerPlayer player, BlockPos pos, String name, String variant) {
        Level level = player.level();
        Block target = block(name);
        if (!(
            target != null
                && validVariant(name, variant)
                && level.isLoaded(pos)
                && Intercom.inReach(player, pos)
                && (player.getMainHandItem().is(ModItems.MODEL_CHANGER.get()) || player.getOffhandItem().is(ModItems.MODEL_CHANGER.get()))
                && level.getBlockEntity(pos) instanceof Station104M.Unit old
        )) {
            return false;
        } else if (old.getCallState() != CallState.IDLE) {
            player.displayClientMessage(Component.literal("통화를 끝낸 뒤 형제모델을 변경하세요"), true);
            return false;
        } else if (level.getBlockState(pos).is(target)) {
            old.setVariant(variant);
            old.syncScreens();
            return true;
        } else {
            CompoundTag saved = old.saveWithoutMetadata();
            if (old instanceof Station104CS.Unit) {
                CompoundTag shared = saved.getCompound("ForgeData");
                shared.putString("Sht104FamilyS1Cards", saved.getString("S1Cards"));
                shared.putBoolean("Sht104FamilyS1Armed", saved.getBoolean("S1Armed"));
                saved.put("ForgeData", shared);
            }

            Direction facing = old.getBlockState().getValue(WallMountedBlock.FACING);
            level.setBlockAndUpdate(pos, target.defaultBlockState().setValue(WallMountedBlock.FACING, facing));
            if (level.getBlockEntity(pos) instanceof Station104M.Unit replacement) {
                replacement.load(saved);
                replacement.setVariant(variant);
                replacement.setChanged();
                replacement.syncScreens();
                player.displayClientMessage(Component.literal(name + " " + variant + "형으로 변경했습니다"), true);
                return true;
            } else {
                return false;
            }
        }
    }
}
