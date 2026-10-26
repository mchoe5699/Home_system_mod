package com.qwerty.homenet.client.kvm;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.qwerty.homenet.HomeNet;
import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.blockentity.Kvm906BlockEntity;
import com.qwerty.homenet.client.logo.LogoRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.data.ModelData;

public final class Kvm931Renderer {
    public static ResourceLocation modelTexture(String model, String name) {
        return HomeNet.id("textures/block/kvm" + model + "/" + name + ".png");
    }

    public static ResourceLocation texture(String name) {
        return HomeNet.id("textures/block/" + (name.startsWith("932:") ? "kvm932t/" + name.substring(4) : "kvm931/" + name) + ".png");
    }

    private static void mesh(String name, Kvm906BlockEntity be, PoseStack p, MultiBufferSource out, int color, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        mc.getBlockRenderer()
            .getModelRenderer()
            .renderModel(
                p.last(),
                out.getBuffer(RenderType.cutout()),
                be.getBlockState(),
                mc.getModelManager().getModel(HomeNet.id("block/kvm_" + be.design().model().id + "_" + name)),
                (color >> 16 & 0xFF) / 255.0F,
                (color >> 8 & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F,
                light,
                overlay,
                ModelData.EMPTY,
                RenderType.cutout()
            );
    }

    public static void renderModel(Kvm906BlockEntity be, PoseStack p, MultiBufferSource out, int light, int overlay) {
        p.pushPose();
        p.translate(0.5, 0.0, 0.5);

        p.mulPose(Axis.YN.rotationDegrees(switch ((Direction)be.getBlockState().getValue(WallMountedBlock.FACING)) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        }));
        p.translate(-0.5, 0.0, -0.5);
        int color = Integer.parseInt(be.design().get("body_color"), 16);
        mesh("body", be, p, out, color, light, overlay);
        mesh("frame", be, p, out, 16777215, light, overlay);
        mesh("button", be, p, out, color, light, overlay);
        mesh("lamp", be, p, out, 16777215, light, overlay);
        if (!be.offHook()) {
            mesh("handset", be, p, out, color, light, overlay);
        }

        if (!be.design().model().manual932() && !be.design().model().roomOnly()) {
            if (be.design().model().is932()) {
                mesh("keypad", be, p, out, color, light, overlay);
                render932(be, p, out, light);
                p.popPose();
            } else {
                boolean phase = be.getLevel() != null && be.getLevel().getGameTime() % 20L < 10L;
                if (be.power()) {
                    quad(p, out, 404.0F, 227.0F, 426.0F, 232.0F, 15.115F, 15935824, "led_power");
                }

                String[] functions = new String[]{"emergency", "away", "monitor", "guard"};

                for (int i = 0; i < 4; i++) {
                    String mode = be.indicator(functions[i]);
                    if (mode.equals("on") || mode.equals("blink") && phase) {
                        quad(p, out, 395.0F, 308 + i * 40, 399.0F, 312 + i * 40, 15.13F, i < 2 ? 15809333 : 3595346, "led_round");
                    }
                }

                if (be.screenOn()) {
                    quad(p, out, 196.0F, 91.0F, 355.0F, 205.0F, 15.235F, 12240316, "white");
                }

                LogoRenderer.wall(be, p, out, x(234.0F), y(261.0F), x(179.0F), y(247.0F), z(15.065F), texture("logo"), light);
                p.popPose();
            }
        } else {
            renderBusFamily(be, p, out, light);
            p.popPose();
        }
    }

    private static void renderBusFamily(Kvm906BlockEntity be, PoseStack p, MultiBufferSource out, int light) {
        boolean phase = be.getLevel() != null && be.getLevel().getGameTime() % 20L < 10L;
        boolean room = be.design().model().roomOnly();
        if (be.indicator("power").equals("on")) {
            quad(p, out, 404.0F, 227.0F, 426.0F, 232.0F, 15.115F, 15935824, "led_power");
            if (room) {
                quad(p, out, 403.0F, 258.0F, 427.0F, 282.0F, 14.995F, 15935824, "led_round");
            }
        }

        if (!room && be.emergency()) {
            quad(p, out, 403.0F, 258.0F, 427.0F, 282.0F, 14.97F, 15935824, "led_round");
        }

        String[] functions = room ? new String[]{"guard", "lobby", "monitor", "door"} : new String[]{"away", "guard", "monitor", "door"};

        for (int i = 0; i < 4; i++) {
            String mode = be.indicator(functions[i]);
            if (mode.equals("on") || mode.equals("blink") && phase) {
                quad(p, out, 395.0F, 305 + i * 37, 399.0F, 309 + i * 37, 15.13F, 3595346, "led_round");
            }
        }

        if (be.screenOn()) {
            quad(p, out, 196.0F, 91.0F, 355.0F, 205.0F, 15.235F, 12240316, "white");
        }

        LogoRenderer.wall(be, p, out, x(234.0F), y(261.0F), x(179.0F), y(247.0F), z(15.065F), texture("logo"), light);
    }

    private static void render932(Kvm906BlockEntity be, PoseStack p, MultiBufferSource out, int light) {
        boolean phase = be.getLevel() != null && be.getLevel().getGameTime() % 20L < 10L;
        if (be.power()) {
            quad(p, out, 369.0F, 268.0F, 373.0F, 272.0F, 15.045F, 15935824, "led_round");
        }

        String[] fs = new String[]{"away", "monitor", "guard"};

        for (int i = 0; i < 3; i++) {
            String mode = be.indicator(fs[i]);
            if (mode.equals("on") || mode.equals("blink") && phase) {
                quad(p, out, 397.0F, 143 + i * 30, 401.0F, 147 + i * 30, 14.995F, 3595346, "led_round");
            }
        }

        if (be.emergency()) {
            quad(p, out, 398.0F, 83.0F, 432.0F, 117.0F, 14.945F, 16777215, "932:emergency");
        }

        if (be.indicator("telephone").equals("on") || be.indicator("telephone").equals("blink") && phase) {
            quad(p, out, 356.0F, 307.0F, 374.0F, 309.0F, 14.99F, 5758305, "led_power");
        }

        if (be.telephone().held()) {
            quad(p, out, 388.0F, 307.0F, 406.0F, 309.0F, 14.99F, 14853696, "led_power");
        }

        if (be.screenOn()) {
            quad(p, out, 196.0F, 78.0F, 365.0F, 207.0F, 15.235F, 12240316, "white");
        }

        LogoRenderer.wall(be, p, out, x(234.0F), y(265.0F), x(179.0F), y(251.0F), z(15.065F), texture("logo"), light);
    }

    private static float x(float px) {
        return 8.0F + (12.06F - px / 500.0F * 8.12F - 8.0F) * 0.9F;
    }

    private static float y(float py) {
        return 8.0F + (12.0F - py / 500.0F * 8.0F - 8.0F) * 0.9F;
    }

    private static float z(float value) {
        return 16.0F + (value - 16.0F) * 0.45F;
    }

    private static void quad(PoseStack p, MultiBufferSource out, float a, float b, float c, float d, float z, int rgb, String mask) {
        VertexConsumer v = out.getBuffer(RenderType.entityTranslucentEmissive(texture(mask)));
        vertex(v, p, x(a), y(b), z(z), 0.0F, 0.0F, rgb);
        vertex(v, p, x(a), y(d), z(z), 0.0F, 1.0F, rgb);
        vertex(v, p, x(c), y(d), z(z), 1.0F, 1.0F, rgb);
        vertex(v, p, x(c), y(b), z(z), 1.0F, 0.0F, rgb);
    }

    private static void vertex(VertexConsumer v, PoseStack p, float x, float y, float z, float u, float t, int rgb) {
        v.vertex(p.last().pose(), x / 16.0F, y / 16.0F, z / 16.0F)
            .color(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, 255)
            .uv(u, t)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(15728880)
            .normal(p.last().normal(), 0.0F, 0.0F, -1.0F)
            .endVertex();
    }

    private Kvm931Renderer() {
    }
}
