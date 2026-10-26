package com.qwerty.homenet.sht;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.move.DeviceOffsets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

final class Ht513Visual {
    static final float W = 445.0F;
    static final float H = 610.0F;
    static final float S = 9.5628423E-4F;
    static final float[] XS = new float[]{256.0F, 304.0F, 352.0F};
    static final float[] YS = new float[]{358.0F, 395.0F, 431.0F, 469.0F};

    private static Surface.Solid solid(String name, float x, float y, float w, float h, float z, float depth, float radius, String color) {
        return new Surface.Solid(name, x, y, w, h, z, depth, radius, color, "", 0.0F);
    }

    static void draw(PoseStack p, MultiBufferSource out, int light) {
        GeometryCache.draw("ht513", p, out, light, Ht513Visual::mesh);
    }

    private static void mesh(PoseStack p, MultiBufferSource out) {
        int light = 15728880;
        Surface.patch(p, out, 0.0F, 0.0F, 162.4F, 610.0F, 0.08F, 2570351, light);
        Surface.patch(p, out, 162.4F, 0.0F, 282.6F, 610.0F, 0.08F, 10265256, light);
        Surface.solid(solid("ht513_one_piece_housing", 0.0F, 0.0F, 445.0F, 610.0F, 0.0F, 31.0F, 1.5F, "#a6aaad"), p, out, light);
        navyBoxSides(p, out, light);
        curvedBlueEnds(p, out, light);
        curvedFront(p, out, Product.id("ht513/face.png"), 31.15F, light, false);
        Surface.rim(
            p,
            out,
            light,
            solid("ht513_acrylic_edge", 190.5F, 48.5F, 228.0F, 230.0F, 31.45F, 0.0F, 3.0F, "#b1b8b8"),
            solid("ht513_acrylic_inner", 192.0F, 50.0F, 225.0F, 227.0F, 31.2F, 0.0F, 2.0F, "#0d111b"),
            11647160
        );

        for (float x : new float[]{59.0F, 69.0F, 79.0F, 89.0F}) {
            Surface.patch(p, out, x, 105.0F, 5.0F, 70.0F, 31.22F, 1055285, light);
        }

        for (int i = 0; i < 12; i++) {
            float x = 24.0F + i * 9.5F;
            Surface.solid(solid("ht513_curved_card_lip", x, 524.0F, 9.6F, 11.0F, 31.2F + bulge(x + 4.75F), 2.7F, 4.8F, "#435990"), p, out, light);
        }

        Surface.solid(solid("ht513_card_bump", 74.0F, 520.0F, 15.0F, 15.0F, 33.0F + bulge(81.5F), 2.2F, 7.5F, "#647bc2"), p, out, light);

        for (float cy : YS) {
            for (float cx : XS) {
                Surface.solid(solid("ht513_key_socket", cx - 16.6F, cy - 16.6F, 33.2F, 33.2F, 31.3F, 0.7F, 16.6F, "#30353a"), p, out, light);
                Surface.solid(solid("ht513_key_rim", cx - 15.5F, cy - 15.5F, 31.0F, 31.0F, 32.0F, 1.1F, 15.5F, "#aeb8b9"), p, out, light);
                Surface.solid(solid("ht513_key_top", cx - 11.6F, cy - 11.6F, 23.2F, 23.2F, 33.1F, 1.3F, 11.6F, "#dbe0dd"), p, out, light);
            }
        }

        for (float cx : new float[]{256.0F, 352.0F}) {
            Surface.solid(solid("ht513_function_socket", cx - 26.0F, 521.0F, 52.0F, 31.0F, 31.3F, 0.7F, 15.0F, "#333b3f"), p, out, light);
            Surface.solid(solid("ht513_function_rim", cx - 24.5F, 522.5F, 49.0F, 28.0F, 32.0F, 1.1F, 14.0F, "#b5bec0"), p, out, light);
            Surface.solid(solid("ht513_function_top", cx - 18.0F, 527.0F, 36.0F, 19.0F, 33.1F, 1.3F, 9.0F, "#dce1dd"), p, out, light);
        }

        curvedFront(p, out, Product.id("ht513/ink.png"), 31.75F, light, true);
    }

    private static float bulge(float x) {
        return !(x <= 0.0F) && !(x >= 162.4F) ? 5.0F * (float)Math.sin(Math.PI * x / 162.4F) : 0.0F;
    }

    private static void curvedBlueEnds(PoseStack p, MultiBufferSource out, int light) {
        VertexConsumer buffer = out.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));

        for (float y : new float[]{0.0F, 610.0F}) {
            for (int i = 0; i < 16; i++) {
                float x = i * 162.4F / 16.0F;
                float nx = (i + 1) * 162.4F / 16.0F;

                for (float[] v : new float[][]{{x, y, 31.0F}, {nx, y, 31.0F}, {nx, y, 31.0F + bulge(nx)}, {x, y, 31.0F + bulge(x)}}) {
                    Surface.vertex(buffer, p.last(), v[0], v[1], v[2], 0.0F, y == 0.0F ? -1.0F : 1.0F, 0.0F, 2702200, light, 0.5F, 0.5F);
                }
            }
        }
    }

    private static void curvedFront(PoseStack p, MultiBufferSource out, ResourceLocation texture, float depth, int light, boolean ink) {
        VertexConsumer buffer = out.getBuffer(RenderType.entityTranslucent(texture));

        for (int i = 0; i < 16; i++) {
            float x = i * 162.4F / 16.0F;
            float nx = (i + 1) * 162.4F / 16.0F;
            float z = depth + bulge(x);
            float nz = depth + bulge(nx);

            for (float[] v : new float[][]{
                {x, 610.0F, z, x / 445.0F, 1.0F}, {nx, 610.0F, nz, nx / 445.0F, 1.0F}, {nx, 0.0F, nz, nx / 445.0F, 0.0F}, {x, 0.0F, z, x / 445.0F, 0.0F}
            }) {
                Surface.vertex(buffer, p.last(), v[0], v[1], v[2], 0.0F, 0.0F, 1.0F, 16777215, light, v[3], v[4]);
            }
        }

        float greyDepth = ink ? 34.5F : depth;

        for (float[] v : new float[][]{
            {162.4F, 610.0F, 0.3649438F, 1.0F}, {445.0F, 610.0F, 1.0F, 1.0F}, {445.0F, 0.0F, 1.0F, 0.0F}, {162.4F, 0.0F, 0.3649438F, 0.0F}
        }) {
            Surface.vertex(buffer, p.last(), v[0], v[1], greyDepth, 0.0F, 0.0F, 1.0F, 16777215, light, v[2], v[3]);
        }
    }

    private static void navyBoxSides(PoseStack p, MultiBufferSource out, int light) {
        VertexConsumer b = out.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));

        for (float y : new float[]{-0.02F, 610.02F}) {
            for (float[] v : new float[][]{{0.0F, y, 0.0F}, {162.4F, y, 0.0F}, {162.4F, y, 31.0F}, {0.0F, y, 31.0F}}) {
                Surface.vertex(b, p.last(), v[0], v[1], v[2], 0.0F, y < 0.0F ? -1.0F : 1.0F, 0.0F, 2702200, light, 0.5F, 0.5F);
            }
        }

        for (float[] v : new float[][]{{-0.02F, 0.0F, 0.0F}, {-0.02F, 610.0F, 0.0F}, {-0.02F, 610.0F, 31.0F}, {-0.02F, 0.0F, 31.0F}}) {
            Surface.vertex(b, p.last(), v[0], v[1], v[2], -1.0F, 0.0F, 0.0F, 2702200, light, 0.5F, 0.5F);
        }
    }

    static void pose(Ht513.Unit unit, PoseStack p) {
        Vec3 offset = DeviceOffsets.get(unit.getLevel(), unit.getBlockPos());
        p.translate(offset.x, offset.y, offset.z);
        double bottom = 0.20833331249999998;
        switch ((Direction)unit.getBlockState().getValue(WallMountedBlock.FACING)) {
            case NORTH:
                p.translate(0.5, bottom, 1.0);
                p.mulPose(Axis.YP.rotationDegrees(180.0F));
                break;
            case SOUTH:
                p.translate(0.5, bottom, 0.0);
                break;
            case EAST:
                p.translate(0.0, bottom, 0.5);
                p.mulPose(Axis.YP.rotationDegrees(90.0F));
                break;
            case WEST:
                p.translate(1.0, bottom, 0.5);
                p.mulPose(Axis.YP.rotationDegrees(-90.0F));
        }

        p.scale(9.5628423E-4F, 9.5628423E-4F, 9.5628423E-4F);
        p.translate(-222.5F, -90.0F, 0.0F);
    }

    static void open(BlockPos at) {
        Minecraft.getInstance().setScreen(new Ht513Visual.ModelScreen(at));
    }

    static final class ModelScreen extends Screen {
        private final BlockPos at;

        ModelScreen(BlockPos at) {
            super(Component.literal("SAMSUNG HT-513"));
            this.at = at;
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        private Ht513.Unit unit() {
            ClientLevel level = Minecraft.getInstance().level;
            return level != null && level.getBlockEntity(this.at) instanceof Ht513.Unit u ? u : null;
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
            this.renderBackground(g);
            g.drawString(this.font, this.title, 12, 8, -1118482);
            Ht513.Unit unit = this.unit();
            g.drawString(this.font, "BUS · " + (unit == null ? "대기" : unit.display()), 12, 27, -1843272);
            float scale = Math.min((this.width - 32) / 445.0F, (this.height - 68) / 610.0F);
            g.pose().pushPose();
            g.pose().translate((this.width - 445.0F * scale) / 2.0F, 52.0F, 0.0F);
            g.pose().scale(scale, scale, 1.0F);
            g.blit(Product.id("ht513/gui.png"), 0, 0, 0.0F, 0.0F, 445, 610, 445, 610);
            Ht512Visual.segments(g, Ht512Visual.digits(unit), 267, 230);
            if (unit != null && unit.callLamp) {
                g.fill(348, 233, 353, 238, -43194);
            }

            if (unit != null && unit.doorTicks > 0) {
                g.fill(348, 251, 353, 256, -43194);
            }

            g.pose().popPose();
            super.render(g, mouseX, mouseY, partial);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0) {
                return super.mouseClicked(mouseX, mouseY, button);
            } else {
                float scale = Math.min((this.width - 32) / 445.0F, (this.height - 68) / 610.0F);
                double x = (mouseX - (this.width - 445.0F * scale) / 2.0F) / scale;
                double y = (mouseY - 52.0) / scale;

                for (int row = 0; row < 4; row++) {
                    for (int col = 0; col < 3; col++) {
                        if (Math.abs(x - Ht513Visual.XS[col]) <= 18.0 && Math.abs(y - Ht513Visual.YS[row]) <= 18.0) {
                            Wire.lobbyKey(this.at, "123456789*0#".charAt(row * 3 + col) + "");
                            return true;
                        }
                    }
                }

                if (y >= 521.0 && y <= 552.0 && x >= 230.0 && x <= 282.0) {
                    Wire.lobbyKey(this.at, "경비실");
                    return true;
                } else if (y >= 521.0 && y <= 552.0 && x >= 326.0 && x <= 378.0) {
                    Wire.lobbyKey(this.at, "호출");
                    return true;
                } else {
                    return super.mouseClicked(mouseX, mouseY, button);
                }
            }
        }

        @Override
        public boolean keyPressed(int key, int scan, int mods) {
            String value = switch (key) {
                case 48, 320 -> "0";
                case 49, 321 -> "1";
                case 50, 322 -> "2";
                case 51, 323 -> "3";
                case 52, 324 -> "4";
                case 53, 325 -> "5";
                case 54, 326 -> "6";
                case 55, 327 -> "7";
                case 56, 328 -> "8";
                case 57, 329 -> "9";
                case 257, 335 -> "호출";
                case 259 -> "#";
                default -> "";
            };
            if (!value.isEmpty()) {
                Wire.lobbyKey(this.at, value);
                return true;
            } else {
                return super.keyPressed(key, scan, mods);
            }
        }
    }

    static final class Renderer implements BlockEntityRenderer<Ht513.Unit> {
        public void render(Ht513.Unit unit, float partial, PoseStack p, MultiBufferSource out, int light, int overlay) {
            if (!DeviceVisibility.hidden(unit, GeometryCache.bounds("ht513"), pose -> Ht513Visual.pose(unit, pose))) {
                p.pushPose();
                Ht513Visual.pose(unit, p);
                Ht513Visual.draw(p, out, light);
                Ht512Visual.segments(p, out, Ht512Visual.digits(unit), 267.0F, 230.0F, 31.7F);
                if (unit.callLamp) {
                    Ht512Visual.lamp(p, out, 348.0F, 233.0F, 31.8F);
                }

                if (unit.doorTicks > 0) {
                    Ht512Visual.lamp(p, out, 348.0F, 251.0F, 31.8F);
                }

                p.popPose();
            }
        }
    }
}
