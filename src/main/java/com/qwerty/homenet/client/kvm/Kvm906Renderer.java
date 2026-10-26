package com.qwerty.homenet.client.kvm;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.qwerty.homenet.HomeNet;
import com.qwerty.homenet.block.Kvm906Block;
import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.blockentity.Kvm906BlockEntity;
import com.qwerty.homenet.client.logo.LogoRenderer;
import com.qwerty.homenet.kvm.KvmDesign;
import com.qwerty.homenet.kvm.KvmModel;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.data.ModelData;

public class Kvm906Renderer implements BlockEntityRenderer<Kvm906BlockEntity> {
    private static final List<String> PARTS = List.of("body", "cover", "frame", "handset", "button", "lamp", "keypad");
    public static final List<ResourceLocation> MODELS = Arrays.stream(KvmModel.values())
        .flatMap(m -> PARTS.stream().map(part -> HomeNet.id("block/kvm_" + m.id + "_" + part)))
        .toList();

    public Kvm906Renderer(Context context) {
    }

    public static ResourceLocation texture(String path) {
        return HomeNet.id("textures/block/kvm/" + path + ".png");
    }

    public static ResourceLocation customTexture(String value, String fallback) {
        ResourceLocation location = KvmDesign.validTexture(value) ? ResourceLocation.tryParse(value) : null;
        return location != null && Minecraft.getInstance().getResourceManager().getResource(location).isPresent() ? location : texture(fallback);
    }

    private void mesh(int index, int color, Kvm906BlockEntity be, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        mc.getBlockRenderer()
            .getModelRenderer()
            .renderModel(
                pose.last(),
                buffers.getBuffer(RenderType.cutout()),
                be.getBlockState(),
                mc.getModelManager().getModel(HomeNet.id("block/kvm_" + be.design().model().id + "_" + PARTS.get(index))),
                (color >> 16 & 0xFF) / 255.0F,
                (color >> 8 & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F,
                light,
                overlay,
                ModelData.EMPTY,
                RenderType.cutout()
            );
    }

    public void render(Kvm906BlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (be.design().model().nativeCase()) {
            Kvm931Renderer.renderModel(be, pose, buffers, light, overlay);
        } else if (be.getBlockState().getBlock() instanceof Kvm906Block) {
            KvmDesign design = be.design();
            int body = Integer.parseInt(design.get("body_color"), 16);
            int cover = Integer.parseInt(design.get("cover_color"), 16);

            float rotation = switch ((Direction)be.getBlockState().getValue(WallMountedBlock.FACING)) {
                case EAST -> 90.0F;
                case SOUTH -> 180.0F;
                case WEST -> 270.0F;
                default -> 0.0F;
            };
            pose.pushPose();
            pose.translate(0.5, 0.0, 0.5);
            pose.mulPose(Axis.YN.rotationDegrees(rotation));
            pose.translate(-0.5, 0.0, -0.5);
            float extra = (float)design.model().size();
            pose.translate(0.5, 0.5, 1.0);
            pose.scale(extra, extra, extra);
            pose.translate(-0.5, -0.5, -1.0);
            this.mesh(0, body, be, pose, buffers, light, overlay);
            this.mesh(1, cover, be, pose, buffers, light, overlay);
            this.mesh(2, 16777215, be, pose, buffers, light, overlay);
            if (!be.offHook()) {
                this.mesh(3, body, be, pose, buffers, light, overlay);
            }

            for (int slot = 0; slot < design.number("buttons"); slot++) {
                pose.pushPose();
                pose.translate(
                    coordinate(design.buttonX(slot)) / 16.0,
                    coordinate(design.buttonY(slot)) / 16.0,
                    (double)(depth(design.model().keypad() ? 14.2F : 14.4F) / 16.0F)
                );
                this.mesh(4, design.model().keypad() ? 2437435 : body, be, pose, buffers, light, overlay);
                pose.popPose();
                String label = design.model().keyLabel(design.get("key" + slot));
                if (design.model().keypad()) {
                    quad(
                        pose,
                        buffers,
                        texture("key_" + design.model().id + "_" + slot),
                        174.0,
                        43 + slot * 19 - 6,
                        202.0,
                        43 + slot * 19 + 6,
                        14.15F,
                        16777215,
                        light,
                        overlay
                    );
                } else {
                    text(
                        pose,
                        buffers,
                        label,
                        design.buttonX(slot) + (design.model().keypad() ? 0.3 : 0.72),
                        design.buttonY(slot) + 0.11,
                        design.model().keypad() ? 14.15 : 14.445,
                        design.model().keypad() ? 8.0E-4F : 9.5E-4F,
                        design.model().keypad() ? 12898514 : 5591886,
                        light
                    );
                }
            }

            boolean flash = be.getLevel() != null && be.getLevel().getGameTime() % 10L < 5L;
            if (!design.model().keypad()) {
                pose.pushPose();
                pose.translate(coordinate(design.lampX()) / 16.0, coordinate(design.lampY()) / 16.0, (double)(depth(14.4F) / 16.0F));
                this.mesh(5, 16777215, be, pose, buffers, light, overlay);
                pose.popPose();
                boolean alarmLamp = design.model() == KvmModel.KVM_933 && be.emergency();
                if (be.power() && (!alarmLamp || flash)) {
                    if (design.model().indicators) {
                        quad(
                            pose,
                            buffers,
                            texture("power_led"),
                            px(design.lampX()) - 5.0,
                            py(design.lampY()) - 5.0,
                            px(design.lampX()) + 5.0,
                            py(design.lampY()) + 5.0,
                            14.375F,
                            16777215,
                            15728880,
                            overlay
                        );
                    } else {
                        quad(
                            pose,
                            buffers,
                            texture("white"),
                            px(design.lampX()) - 4.0,
                            py(design.lampY()) - 1.0,
                            px(design.lampX()) + 4.0,
                            py(design.lampY()) + 1.0,
                            14.375F,
                            14365005,
                            15728880,
                            overlay
                        );
                    }
                }

                text(
                    pose,
                    buffers,
                    design.model() == KvmModel.KVM_933 ? "비상/전원" : "전 원",
                    design.lampX() + 0.72,
                    design.lampY() + 0.11,
                    14.445,
                    9.5E-4F,
                    5591886,
                    light
                );
            } else {
                this.mesh(6, body, be, pose, buffers, light, overlay);
                quad(pose, buffers, texture("dial_" + design.model().id), 141.0, 137.0, 205.0, 209.0, 14.395F, 16777215, light, overlay);
            }

            String style = design.get("cover");
            ResourceLocation coverPrint = style.equals("custom")
                ? customTexture(design.get("cover_texture"), "custom_cover")
                : texture("cover_" + style + "_" + design.model().id);
            quad(
                pose,
                buffers,
                coverPrint,
                171.0,
                design.model().indicators ? 15.0 : 19.0,
                209.0,
                design.model().indicators ? 90.0 : 85.0,
                14.224F,
                16777215,
                light,
                overlay
            );
            if (design.model().indicators) {
                for (String function : design.model().keypad()
                    ? List.of("power", "telephone", "monitor", "guard", "door")
                    : List.of("emergency", "away", "monitor", "guard", "door")) {
                    String stateLed = be.indicator(function);
                    if (stateLed.equals("on") || stateLed.equals("blink") && flash) {
                        quad(pose, buffers, texture("led_" + function + "_" + design.model().id), 171.0, 15.0, 209.0, 90.0, 14.21F, 16777215, 15728880, overlay);
                    }
                }
            }

            if (be.productLogo.configured()) {
                LogoRenderer.wall(
                    be,
                    pose,
                    buffers,
                    (float)coordinate(7.552),
                    (float)coordinate(8.096),
                    (float)coordinate(9.024),
                    (float)coordinate(8.448),
                    depth(14.445F),
                    texture("logo"),
                    light
                );
            } else {
                String var21 = design.get("logo");
                switch (var21) {
                    case "default":
                        quad(
                            pose,
                            buffers,
                            texture(design.model().id.equals("906") ? "logo" : "logo_" + design.model().id),
                            78.0,
                            96.0,
                            design.model().id.equals("906") ? 111.0 : 130.0,
                            design.model().id.equals("906") ? 104.0 : 112.0,
                            14.445F,
                            16777215,
                            light,
                            overlay
                        );
                        break;
                    case "custom":
                        quad(
                            pose,
                            buffers,
                            customTexture(design.get("logo_texture"), "custom_logo"),
                            78.0,
                            96.0,
                            124.0,
                            107.0,
                            14.445F,
                            16777215,
                            light,
                            overlay
                        );
                        break;
                    case "text":
                        text(pose, buffers, design.get("logo_text"), 9.024, 8.416, 14.445, 0.0015F, 4343371, light);
                }
            }

            if (be.screenOn()) {
                quad(
                    pose,
                    buffers,
                    texture("white"),
                    85.0,
                    design.model().indicators ? 25.0 : 28.0,
                    161.0,
                    design.model().indicators ? 81.0 : 75.0,
                    14.405F,
                    12240062,
                    15728880,
                    overlay
                );
            }

            for (int i = 0; i < 3; i++) {
                text(pose, buffers, new String[]{"음량", "밝기", "명암"}[i], 11.52 - (93 + i * 23) * 0.032, 4.58, 14.5, 8.0E-4F, 5591886, light);
            }

            pose.popPose();
        }
    }

    private static double coordinate(double v) {
        return 8.0 + (v - 8.0) * 0.9090909090909091;
    }

    private static float depth(float z) {
        return 16.0F - (16.0F - z) * 0.59532034F;
    }

    private static double px(double x) {
        return (11.52 - x) / 0.032;
    }

    private static double py(double y) {
        return (11.52 - y) / 0.032;
    }

    private static void text(PoseStack pose, MultiBufferSource buffers, String text, double x, double y, double z, float scale, int color, int light) {
        pose.pushPose();
        pose.translate(coordinate(x) / 16.0, coordinate(y) / 16.0, (double)(depth((float)z) / 16.0F));
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        float fit = Math.min(scale, 0.12F / Math.max(1, Minecraft.getInstance().font.width(text)));
        pose.scale(fit * 0.90909094F, -fit * 0.90909094F, fit * 0.90909094F);
        Minecraft.getInstance().font.drawInBatch(text, 0.0F, 0.0F, color, false, pose.last().pose(), buffers, DisplayMode.NORMAL, 0, light);
        pose.popPose();
    }

    private static void quad(
        PoseStack pose, MultiBufferSource buffers, ResourceLocation texture, double l, double t, double r, double b, float z, int color, int light, int overlay
    ) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        z = depth(z);
        float x1 = (float)coordinate(11.52 - l * 0.032) / 16.0F;
        float x2 = (float)coordinate(11.52 - r * 0.032) / 16.0F;
        float y1 = (float)coordinate(11.52 - t * 0.032) / 16.0F;
        float y2 = (float)coordinate(11.52 - b * 0.032) / 16.0F;
        vertex(consumer, pose, x1, y1, z / 16.0F, 0.0F, 0.0F, color, light, overlay);
        vertex(consumer, pose, x1, y2, z / 16.0F, 0.0F, 1.0F, color, light, overlay);
        vertex(consumer, pose, x2, y2, z / 16.0F, 1.0F, 1.0F, color, light, overlay);
        vertex(consumer, pose, x2, y1, z / 16.0F, 1.0F, 0.0F, color, light, overlay);
    }

    private static void vertex(VertexConsumer v, PoseStack p, float x, float y, float z, float u, float t, int color, int light, int overlay) {
        v.vertex(p.last().pose(), x, y, z)
            .color(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, 255)
            .uv(u, t)
            .overlayCoords(overlay)
            .uv2(light)
            .normal(p.last().normal(), 0.0F, 0.0F, -1.0F)
            .endVertex();
    }
}
