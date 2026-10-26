package com.qwerty.homenet.sht;

import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.blockentity.Sht5500sBlockEntity;
import com.qwerty.homenet.intercom.CallState;
import com.qwerty.homenet.intercom.Intercom;
import com.qwerty.homenet.intercom.UnitAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
    modid = "homenet_sht101c"
)
public final class SamsungAddress {
    static boolean legacy(ReceiverBlockEntity r) {
        return !(r instanceof Station) && !(r instanceof Sht5500sBlockEntity) && r.manufacturer().equalsIgnoreCase("SAMSUNG");
    }

    @SubscribeEvent
    public static void open(RightClickBlock e) {
        if (e.getHand() == InteractionHand.MAIN_HAND && e.getItemStack().isEmpty()) {
            if (e.getLevel().getBlockEntity(e.getPos()) instanceof ReceiverBlockEntity r && legacy(r)) {
                e.setCanceled(true);
                e.setCancellationResult(InteractionResult.SUCCESS);
                if (e.getEntity() instanceof ServerPlayer p && Intercom.inReach(p, e.getPos())) {
                    Wire.address(p, r.getBlockPos(), r.getUnit());
                }
            }
        }
    }

    static boolean set(ServerPlayer p, BlockPos at, String raw) {
        if (p.level().isLoaded(at)
            && Intercom.inReach(p, at)
            && p.level().getBlockEntity(at) instanceof ReceiverBlockEntity r
            && legacy(r)
            && r.getCallState() == CallState.IDLE) {
            String unit = UnitAddress.canonical(raw);
            if (unit.isEmpty() && !raw.isBlank()) {
                r.noticeTo(p, "동-호 형식으로 입력하세요");
                return false;
            } else {
                CompoundTag tag = r.saveWithoutMetadata();
                tag.putString("Unit", unit);
                r.load(tag);
                r.setChanged();
                r.onLoad();
                r.syncScreens();
                Wire.address(p, at, r.getUnit());
                return true;
            }
        } else {
            return false;
        }
    }
}
