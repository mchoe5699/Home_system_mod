package com.qwerty.homenet.sht;

import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.data.ZoneData;
import com.qwerty.homenet.intercom.Intercom;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class PortableLinker {
    static final String DOCK = "SamsungPortableDock";

    static BlockPos selected(ItemStack linker, String key, Level l) {
        CompoundTag t = linker.getTag();
        return t != null && t.contains(key) && t.getString("Dim").equals(l.dimension().location().toString()) ? BlockPos.of(t.getLong(key)) : null;
    }

    static void notice(ServerPlayer p, String text) {
        p.displayClientMessage(Component.literal(text), true);
    }

    static boolean connect(ServerPlayer p, ItemStack linker, BlockPos receiver, BlockPos dock) {
        Level l = p.level();
        if (receiver != null && dock != null && l.isLoaded(receiver) && l.isLoaded(dock) && (Intercom.inReach(p, dock) || Intercom.inReach(p, receiver))) {
            if (!(l.getBlockEntity(receiver) instanceof ReceiverBlockEntity r && Portable.supports(r))) {
                notice(p, "무선 휴대장치를 지원하는 삼성 세대내 기기를 선택하세요");
                return false;
            } else if (!ZoneData.connected(l, receiver, dock) || receiver.distSqr(dock) > 1024.0) {
                notice(p, "같은 단지의 32블록 이내 기기끼리 연결하세요");
                return false;
            } else if (l.getBlockEntity(dock) instanceof Charger.Dock d && !d.stack.isEmpty()) {
                Station s = (Station)r;
                if (!s.ownsPortable(d.stack)) {
                    if (s.portableRegistered && !(s instanceof Station104M.Unit) && !p.isShiftKeyDown()) {
                        notice(p, "이미 휴대장치가 연결되어 있습니다 · 교체 연결은 웅크리고 클릭");
                        return false;
                    } else {
                        boolean ok = Portable.bindFromLinker(p, s, d.stack);
                        if (ok) {
                            d.sync();
                            notice(p, "삼성 내부기기와 휴대장치 연결 완료");
                        }

                        return ok;
                    }
                } else {
                    if (s instanceof Station104M.Unit model104) {
                        model104.unbindPortable(d.stack);
                    } else {
                        if (s.portableActive || s.transfer != 0 || s.internal == 3) {
                            s.finish();
                        }

                        s.portableRegistered = false;
                        s.portableTicket = UUID.randomUUID();
                        s.update();

                        for (String k : new String[]{"serial", "ticket", "base", "dimension"}) {
                            d.stack.getOrCreateTag().remove(k);
                        }
                    }

                    d.sync();
                    notice(p, "삼성 휴대장치 연결 해제");
                    return true;
                }
            } else {
                notice(p, "충전기에 휴대장치를 먼저 놓으세요");
                return false;
            }
        } else {
            return false;
        }
    }

    static InteractionResult use(ItemStack linker, UseOnContext context) {
        Level l = context.getLevel();
        BlockPos at = context.getClickedPos();
        BlockEntity be = l.getBlockEntity(at);
        BlockPos dock = selected(linker, "SamsungPortableDock", l);
        if (be instanceof Charger.Dock || be instanceof ReceiverBlockEntity && dock != null) {
            if (l.isClientSide) {
                return InteractionResult.SUCCESS;
            } else if (context.getPlayer() instanceof ServerPlayer p) {
                if (be instanceof Charger.Dock) {
                    BlockPos receiver = selected(linker, "Wallpad", l);
                    if (receiver != null) {
                        connect(p, linker, receiver, at);
                    } else {
                        CompoundTag t = linker.getOrCreateTag();
                        t.putLong("SamsungPortableDock", at.asLong());
                        t.putString("Dim", l.dimension().location().toString());
                        notice(p, "휴대장치 충전기 선택 · 삼성 무선 지원 내부기기를 클릭하세요");
                    }

                    return InteractionResult.CONSUME;
                } else {
                    if (connect(p, linker, at, dock)) {
                        linker.getOrCreateTag().remove("SamsungPortableDock");
                        linker.getTag().putLong("Wallpad", at.asLong());
                    }

                    return InteractionResult.CONSUME;
                }
            } else {
                return InteractionResult.FAIL;
            }
        } else {
            return InteractionResult.PASS;
        }
    }
}
