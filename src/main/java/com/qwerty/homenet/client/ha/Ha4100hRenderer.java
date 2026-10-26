package com.qwerty.homenet.client.ha;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.qwerty.homenet.HomeNet;
import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.blockentity.Ha4100hBlockEntity;
import com.qwerty.homenet.client.kvm.KvmCameraView;
import com.qwerty.homenet.client.logo.LogoRenderer;
import com.qwerty.homenet.video.VideoFormat;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraftforge.client.model.data.ModelData;

public final class Ha4100hRenderer implements BlockEntityRenderer<Ha4100hBlockEntity> {
    private final Ha4100hSurfaces surfaces = new Ha4100hSurfaces();
    private final List<Ha4100hConfigScreen.Part> lamps = Ha4100hConfigScreen.layout("lamps");
    private final KvmCameraView camera = new KvmCameraView();

    public Ha4100hRenderer(Context c) {
    }

    private static float x(float px) {
        return 12.0F - px / 125.0F;
    }

    private static float y(float py) {
        return 11.136F - py / 125.0F;
    }

    public void render(Ha4100hBlockEntity b, float partial, PoseStack p, MultiBufferSource out, int light, int overlay) {
        p.pushPose();
        p.translate(0.5, 0.0, 0.5);

        p.mulPose(Axis.YN.rotationDegrees(switch ((Direction)b.getBlockState().getValue(WallMountedBlock.FACING)) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        }));
        p.translate(-0.5, 0.0, -0.5);
        Minecraft mc = Minecraft.getInstance();

        for (String part : List.of("body")) {
            mc.getBlockRenderer()
                .getModelRenderer()
                .renderModel(
                    p.last(),
                    out.getBuffer(RenderType.cutout()),
                    b.getBlockState(),
                    mc.getModelManager().getModel(HomeNet.id("block/ha_4100h_" + part)),
                    1.0F,
                    1.0F,
                    1.0F,
                    light,
                    overlay,
                    ModelData.EMPTY,
                    RenderType.cutout()
                );
        }

        this.surfaces.render(p, out, light, b.lifted());
        boolean phase = b.getLevel() == null || b.getLevel().getGameTime() % 20L < 10L;

        for (Ha4100hConfigScreen.Part lamp : this.lamps) {
            if (b.lamp(lamp.name()) == 1 || b.lamp(lamp.name()) == 2 && phase) {
                this.lamp(p, out, lamp);
            }
        }

        BlockPos video = b.videoCamera();
        if (b.screenOn() && video != null) {
            this.camera.drawWall(p, out, video, x(884.0F), y(287.0F), x(620.0F), y(111.0F), 15.805F, b.brightness(), b.contrast(), VideoFormat.MONOCHROME);
        }

        this.slider(p, out, 637, b.contrast(), light);
        this.slider(p, out, 682, b.brightness(), light);

        for (float[] r : new float[][]{{282.0F, 47.0F, 380.0F, 67.0F}, {589.0F, 46.0F, 693.0F, 66.0F}, {714.0F, 323.0F, 786.0F, 337.0F}}) {
            LogoRenderer.wallLogo(
                b.productLogo, p, out, x(r[2]), y(r[3]), x(r[0]), y(r[1]), 15.615F, HomeNet.id("textures/block/ha4100h/logo.png"), light, 11058603
            );
        }

        p.popPose();
    }

    private void slider(PoseStack p, MultiBufferSource out, int y, int step, int light) {
        float a = 825 + step * 12;
        float b = a + 25.0F;
        VertexConsumer v = out.getBuffer(RenderType.entityCutoutNoCull(HomeNet.id("textures/block/ha4100h/video_white.png")));

        for (float[] q : new float[][]{
            {a, y - 8, 15.57F, 0.0F, 0.0F}, {a, y + 8, 15.57F, 0.0F, 1.0F}, {b, y + 8, 15.57F, 1.0F, 1.0F}, {b, y - 8, 15.57F, 1.0F, 0.0F}
        }) {
            v.vertex(p.last().pose(), x(q[0]) / 16.0F, y(q[1]) / 16.0F, q[2] / 16.0F)
                .color(250, 253, 246, 255)
                .uv(q[3], q[4])
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(p.last().normal(), 0.0F, 0.0F, -1.0F)
                .endVertex();
        }
    }

    private void lamp(PoseStack p, MultiBufferSource out, Ha4100hConfigScreen.Part l) {
        float z = l.name().equals("emergency")
            ? 15.595F
            : (Set.of("stop", "test", "security", "guard", "doorphone", "door", "away", "phone").contains(l.name()) ? 15.583F : 15.614F);
        VertexConsumer v = out.getBuffer(RenderType.entityTranslucentEmissive(HomeNet.id("textures/block/ha4100h/light_" + l.name() + ".png")));

        for (float[] q : new float[][]{
            {l.x() - l.w() / 2.0F, l.y() - l.h() / 2.0F, 0.0F, 0.0F},
            {l.x() - l.w() / 2.0F, l.y() + l.h() / 2.0F, 0.0F, 1.0F},
            {l.x() + l.w() / 2.0F, l.y() + l.h() / 2.0F, 1.0F, 1.0F},
            {l.x() + l.w() / 2.0F, l.y() - l.h() / 2.0F, 1.0F, 0.0F}
        }) {
            v.vertex(p.last().pose(), x(q[0]) / 16.0F, y(q[1]) / 16.0F, z / 16.0F)
                .color(255, 255, 255, 255)
                .uv(q[2], q[3])
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880)
                .normal(p.last().normal(), 0.0F, 0.0F, -1.0F)
                .endVertex();
        }
    }
}
