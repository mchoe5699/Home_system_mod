package com.qwerty.homenet.sht;

import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.data.ZoneData;
import com.qwerty.homenet.intercom.Intercom;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class Ht100sLinker {
    private Ht100sLinker() {
    }

    public static InteractionResult use(ItemStack linker, UseOnContext context) {
        InteractionResult portable = PortableLinker.use(linker, context);
        if (portable != InteractionResult.PASS) {
            return portable;
        } else {
            Level level = context.getLevel();
            if (!(level.getBlockEntity(context.getClickedPos()) instanceof Ht100s.Unit)) {
                return InteractionResult.PASS;
            } else if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            } else {
                if (context.getPlayer() instanceof ServerPlayer player) {
                    connect(player, linker, context.getClickedPos());
                }

                return InteractionResult.CONSUME;
            }
        }
    }

    static boolean connect(ServerPlayer player, ItemStack linker, BlockPos target) {
        Level level = player.level();
        if (Intercom.inReach(player, target) && level.isLoaded(target) && level.getBlockEntity(target) instanceof Ht100s.Unit phone) {
            CompoundTag tag = linker.getTag();
            BlockPos source = tag != null && tag.contains("Wallpad") && tag.getString("Dim").equals(level.dimension().location().toString())
                ? BlockPos.of(tag.getLong("Wallpad"))
                : null;
            if (!(source != null && level.isLoaded(source) && level.getBlockEntity(source) instanceof ReceiverBlockEntity receiver)) {
                notice(player, "홈링커로 세대 내부기기를 먼저 선택하세요");
                return false;
            } else if (!receiver.supportsDoorCommunication()) {
                notice(player, "이 내부기기는 도어폰·도어카메라 통신을 지원하지 않습니다");
                return false;
            } else if (receiver.getUnit().isBlank()) {
                notice(player, "선택한 세대 내부기기의 동·호 번호를 먼저 설정하세요");
                return false;
            } else if (!ZoneData.connected(level, source, target)) {
                notice(player, phone.model() + "와 세대 내부기기는 같은 단지에 있어야 합니다");
                return false;
            } else if (!source.closerThan(target, 64.0)) {
                notice(player, "홈링커는 64블록 이내에서 연결할 수 있습니다");
                return false;
            } else {
                String unit = receiver.getUnit();
                phone.getPersistentData().putString("ShtDoorProtocol", receiver.guardSystem().name());
                phone.setUnit(DeviceRegistry.normalize(unit).equals(DeviceRegistry.normalize(phone.unit)) ? "" : unit);
                notice(player, phone.unit.isEmpty() ? phone.model() + " 세대 연결 해제" : phone.model() + " 세대 " + phone.unit + " 연결");
                return true;
            }
        } else {
            return false;
        }
    }

    private static void notice(ServerPlayer p, String message) {
        p.displayClientMessage(Component.literal(message), true);
    }
}
