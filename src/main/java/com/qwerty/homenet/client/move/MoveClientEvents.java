package com.qwerty.homenet.client.move;

import com.qwerty.homenet.move.DeviceMoves;
import com.qwerty.homenet.move.DeviceOffsets;
import com.qwerty.homenet.network.DeviceMoveOpenPacket;
import com.qwerty.homenet.network.DeviceOffsetPacket;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
    modid = "qwertys_homenet",
    value = {Dist.CLIENT}
)
public final class MoveClientEvents {
    public static void offset(DeviceOffsetPacket p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            DeviceOffsets.clientUpdate(mc.level, p.pos(), p.x(), p.y(), p.z());
        }
    }

    public static void open(DeviceMoveOpenPacket p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof DeviceMoveScreen screen) {
            screen.update(p);
        } else if (p.open()) {
            mc.setScreen(new DeviceMoveScreen(p));
        }
    }

    @SubscribeEvent
    public static void logout(LoggingOut e) {
        DeviceOffsets.clearClient();
    }

    @SubscribeEvent
    public static void pick(RenderLevelStageEvent e) {
        if (e.getStage() == Stage.AFTER_SKY) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && mc.player != null && mc.getCameraEntity() != null) {
                Entity entity = mc.getCameraEntity();
                Vec3 eye = entity.getEyePosition(e.getPartialTick());
                double reach = mc.player.getBlockReach();
                Vec3 end = eye.add(entity.getViewVector(e.getPartialTick()).scale(reach));
                double closest = mc.hitResult != null && mc.hitResult.getType() != Type.MISS ? eye.distanceToSqr(mc.hitResult.getLocation()) : reach * reach;

                for (Entry<BlockPos, Vec3> entry : DeviceOffsets.client(mc.level).entrySet()) {
                    BlockPos at = entry.getKey();
                    if (!(eye.distanceToSqr(Vec3.atCenterOf(at)) > (reach + 2.0) * (reach + 2.0))
                        && mc.level.hasChunkAt(at)
                        && DeviceMoves.product(mc.level.getBlockState(at))) {
                        BlockHitResult hit = mc.level.getBlockState(at).getShape(mc.level, at).clip(eye, end, at);
                        if (hit != null && eye.distanceToSqr(hit.getLocation()) < closest) {
                            closest = eye.distanceToSqr(hit.getLocation());
                            mc.hitResult = hit;
                            mc.crosshairPickEntity = null;
                        }
                    }
                }
            }
        }
    }
}
