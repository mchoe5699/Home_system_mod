package com.qwerty.homenet.item;

import com.qwerty.homenet.move.DeviceMoves;
import com.qwerty.homenet.network.DeviceMoveOpenPacket;
import com.qwerty.homenet.network.ModNetwork;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class DeviceMoverItem extends Item {
    public DeviceMoverItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext c) {
        if (!DeviceMoves.product(c.getLevel().getBlockState(c.getClickedPos()))) {
            return InteractionResult.PASS;
        } else {
            if (c.getPlayer() instanceof ServerPlayer p) {
                ModNetwork.sendTo(p, new DeviceMoveOpenPacket(c.getClickedPos(), "이동량과 방향을 선택하세요."));
            }

            return InteractionResult.sidedSuccess(c.getLevel().isClientSide);
        }
    }

    @Override
    public void appendHoverText(ItemStack s, Level l, List<Component> t, TooltipFlag f) {
        t.add(Component.literal("제품 클릭: 0.1~1칸씩 위치 조정"));
        t.add(Component.literal("설정·연결을 보존하며 상하·동서남북 이동"));
    }
}
