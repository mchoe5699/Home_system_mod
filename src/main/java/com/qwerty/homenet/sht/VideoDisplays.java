package com.qwerty.homenet.sht;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.blockentity.Kvm906BlockEntity;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.blockentity.WallpadBlockEntity;
import com.qwerty.homenet.client.kvm.Kvm906Renderer;
import com.qwerty.homenet.client.kvm.KvmCameraView;
import com.qwerty.homenet.client.logo.LegacyLogoRenderer;
import com.qwerty.homenet.client.move.MovingRenderer;
import com.qwerty.homenet.kvm.KvmModel;
import com.qwerty.homenet.registry.ModBlockEntities;
import com.qwerty.homenet.video.VideoFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(
    modid = "homenet_sht101c",
    bus = Bus.MOD,
    value = {Dist.CLIENT}
)
public final class VideoDisplays {
    private static final Map<BlockPos, BlockPos> SIGNALS = new HashMap<>();
    private static Object level;

    static void receive(Wire.Video packet) {
        ClientLevel l = Minecraft.getInstance().level;
        SIGNALS.clear();
        level = l;
        if (l != null && l.dimension().location().toString().equals(packet.dimension())) {
            for (WorldVideo.Signal s : packet.signals()) {
                SIGNALS.put(s.receiver(), s.camera());
            }
        }
    }

    static BlockPos camera(BlockPos receiver) {
        return SIGNALS.get(receiver);
    }

    static VideoFormat sourceFormat(Level level, BlockPos camera, VideoFormat receiver) {
        return level != null
                && camera != null
                && level.isLoaded(camera)
                && level.getBlockEntity(camera) instanceof Ht512.Unit lobby
                && lobby.videoFormat() == VideoFormat.MONOCHROME
            ? VideoFormat.MONOCHROME
            : receiver;
    }

    @SubscribeEvent(
        priority = EventPriority.LOWEST
    )
    public static void register(RegisterRenderers e) {
        MovingRenderer.register(e, ModBlockEntities.KVM_906.get(), c -> new VideoDisplays.Display<>(new Kvm906Renderer(c)));
        MovingRenderer.register(e, ModBlockEntities.RECEIVER.get(), c -> new VideoDisplays.Display<>(new LegacyLogoRenderer<>(c)));
        MovingRenderer.register(e, ModBlockEntities.WALLPAD.get(), c -> new VideoDisplays.Display<>(new LegacyLogoRenderer<>(c)));
    }

    static float classic(float v) {
        return 8.0F + (v - 8.0F) / 1.1F;
    }

    static float nativeX(float v) {
        return 8.0F + (12.06F - v / 500.0F * 8.12F - 8.0F) * 0.9F;
    }

    static float nativeY(float v) {
        return 8.0F + (12.0F - v / 500.0F * 8.0F - 8.0F) * 0.9F;
    }

    static class Display<T extends ReceiverBlockEntity> implements BlockEntityRenderer<T> {
        final BlockEntityRenderer<T> original;
        final Map<BlockPos, KvmCameraView> views = new LinkedHashMap<BlockPos, KvmCameraView>(16, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Entry<BlockPos, KvmCameraView> e) {
                return this.size() > 64;
            }
        };

        Display(BlockEntityRenderer<T> original) {
            this.original = original;
        }

        public boolean shouldRenderOffScreen(T b) {
            return this.original.shouldRenderOffScreen(b);
        }

        @Override
        public int getViewDistance() {
            return this.original.getViewDistance();
        }

        public void render(T b, float tick, PoseStack p, MultiBufferSource out, int light, int overlay) {
            this.original.render(b, tick, p, out, light, overlay);
            if (VideoDisplays.level == b.getLevel() && WorldVideo.supports(b)) {
                BlockPos camera = VideoDisplays.SIGNALS.get(b.getBlockPos());
                if (camera != null) {
                    p.pushPose();
                    p.translate(0.5, 0.0, 0.5);

                    float turn = switch ((Direction)b.getBlockState().getValue(WallMountedBlock.FACING)) {
                        case EAST -> 90.0F;
                        case SOUTH -> 180.0F;
                        case WEST -> 270.0F;
                        default -> 0.0F;
                    };
                    p.mulPose(Axis.YN.rotationDegrees(turn));
                    p.translate(-0.5, 0.0, -0.5);
                    VideoFormat format = VideoFormat.COLOR;
                    float x1;
                    float y1;
                    float x2;
                    float y2;
                    float z;
                    if (b instanceof Kvm906BlockEntity k) {
                        KvmModel m = k.design().model();
                        format = m.videoFormat();
                        if (m.nativeCase()) {
                            boolean is932 = m.is932();
                            x1 = VideoDisplays.nativeX(is932 ? 365.0F : 355.0F);
                            x2 = VideoDisplays.nativeX(196.0F);
                            y1 = VideoDisplays.nativeY(is932 ? 207.0F : 205.0F);
                            y2 = VideoDisplays.nativeY(is932 ? 78.0F : 91.0F);
                            z = 15.6535F;
                        } else {
                            float scale = (float)m.size();
                            p.translate(0.5, 0.5, 1.0);
                            p.scale(scale, scale, scale);
                            p.translate(-0.5, -0.5, -1.0);
                            x1 = VideoDisplays.classic(6.368F);
                            x2 = VideoDisplays.classic(8.8F);
                            y1 = VideoDisplays.classic(11.52F - (m.indicators ? 81 : 75) * 0.032F);
                            y2 = VideoDisplays.classic(11.52F - (m.indicators ? 25 : 28) * 0.032F);
                            z = 15.047487F;
                        }
                    } else if (b instanceof WallpadBlockEntity) {
                        x1 = 3.8F;
                        y1 = 4.8F;
                        x2 = 12.2F;
                        y2 = 11.2F;
                        z = 15.075F;
                    } else {
                        x1 = 5.75F;
                        y1 = 4.875F;
                        x2 = 13.25F;
                        y2 = 11.1875F;
                        z = 14.995F;
                        format = VideoFormat.MONOCHROME;
                    }

                    this.views
                        .computeIfAbsent(camera, key -> new KvmCameraView())
                        .drawWall(p, out, camera, x1, y1, x2, y2, z, 2, 2, VideoDisplays.sourceFormat(b.getLevel(), camera, format));
                    p.popPose();
                }
            }
        }
    }
}
