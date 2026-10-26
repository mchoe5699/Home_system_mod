package com.qwerty.homenet.client;

import com.qwerty.homenet.block.GuardMasterBlock;
import com.qwerty.homenet.block.Hmc7000Block;
import com.qwerty.homenet.block.Kam305Block;
import com.qwerty.homenet.network.GuardAnglePacket;
import com.qwerty.homenet.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent.Key;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(
    modid = "qwertys_homenet",
    bus = Bus.FORGE,
    value = {Dist.CLIENT}
)
public final class ClientEvents {
    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onKey(Key event) {
        if (event.getAction() != 0) {
            if ((event.getModifiers() & 2) != 0) {
                int key = event.getKey();
                int delta;
                if (key != 61 && key != 334) {
                    if (key != 45 && key != 333) {
                        return;
                    }

                    delta = -1;
                } else {
                    delta = 1;
                }

                Minecraft mc = Minecraft.getInstance();
                if (mc.screen == null && mc.player != null && mc.level != null) {
                    if (mc.hitResult instanceof BlockHitResult hit && hit.getType() == Type.BLOCK) {
                        boolean kam = mc.level.getBlockState(hit.getBlockPos()).getBlock() instanceof Kam305Block
                            || mc.level.getBlockState(hit.getBlockPos()).getBlock() instanceof Hmc7000Block;
                        if (kam || mc.level.getBlockState(hit.getBlockPos()).getBlock() instanceof GuardMasterBlock && mc.player.getMainHandItem().isEmpty()) {
                            ModNetwork.sendToServer(new GuardAnglePacket(hit.getBlockPos(), delta));
                        }
                    }
                }
            }
        }
    }

    private static boolean ctrl() {
        return Screen.hasControlDown();
    }
}
