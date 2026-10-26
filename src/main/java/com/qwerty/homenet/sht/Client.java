package com.qwerty.homenet.sht;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.client.kvm.KvmCameraView;
import com.qwerty.homenet.move.DeviceOffsets;
import com.qwerty.homenet.video.VideoFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(
    modid = "homenet_sht101c",
    bus = Bus.MOD,
    value = {Dist.CLIENT}
)
public class Client {
    static BlockEntityWithoutLevelRenderer renderer;

    @SubscribeEvent
    public static void reload(RegisterClientReloadListenersEvent e) {
        e.registerReloadListener((ResourceManagerReloadListener)manager -> {
            GeometryCache.clear();
            Sht308Mesh.clear();
            Sht305Mesh.clear();
            Hi101Mesh.clear();
            Ht512Visual.keys = null;
            DeviceVisibility.clear();
            Surface.models.clear();
            Surface.solids = null;
            PortableVisual.parts = null;
            PortableVisual.keys = null;
        });
    }

    static void address(Wire.Address a) {
        Minecraft.getInstance().setScreen(new AddressControls(a.pos(), a.unit()));
    }

    static void family(BlockPos pos) {
        Minecraft.getInstance().setScreen(new Sht104FamilyScreen(pos));
    }

    static void guard(Wire.GuardView v) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof Sht5500Controls s && s.pos.equals(v.pos())) {
            s.data = v;
        } else {
            mc.setScreen(new Sht5500Controls(v));
        }
    }

    static void receive(Wire.View v) {
        Minecraft mc = Minecraft.getInstance();
        String model = v.state().setting("model", "");
        boolean sht104 = model.equals("SHT-104M") || model.equals("SHT-104C") || model.equals("SHT-104CS");
        if (!v.state().open() || !v.accessory() && (!v.remote() || !v.state().flag("wired"))) {
            if (v.state().open() && sht104) {
                mc.setScreen(new Sht104MControls(v.state()));
            } else if (v.state().open() && model.equals("SHT-705")) {
                mc.setScreen(new Sht705Controls(v.state()));
            } else if (v.state().open()) {
                mc.setScreen(
                    (Screen)(!model.equals("SHT-308")
                            && !model.equals("SHT-305V")
                            && !model.equals("SHT-305")
                            && !model.equals("SHT-307")
                            && !model.equals("HI-101")
                        ? new Controls(v.state(), v.remote())
                        : new ModelControls(v.state()))
                );
            } else if (mc.screen instanceof Sht104MControls c && c.pos.equals(v.state().pos())) {
                c.data = v.state();
            } else if (mc.screen instanceof Sht705Controls c && c.pos.equals(v.state().pos())) {
                c.data = v.state();
            } else if (mc.screen instanceof ModelControls c && c.data.pos().equals(v.state().pos())) {
                c.data = v.state();
            } else if (mc.screen instanceof Controls c && c.data.pos().equals(v.state().pos())) {
                c.data = v.state();
            }
        } else {
            mc.setScreen(new PortableControls(v.state()));
        }
    }

    @SubscribeEvent
    public static void renderers(RegisterRenderers e) {
        e.registerBlockEntityRenderer(Product.HT512_TYPE.get(), c -> new Ht512Visual.Renderer());
        e.registerBlockEntityRenderer(Product.HT513_TYPE.get(), c -> new Ht513Visual.Renderer());
        e.registerBlockEntityRenderer(Product.HT100S_TYPE.get(), c -> new Ht100sVisual.Renderer());
        e.registerBlockEntityRenderer(Product.TYPE104M.get(), c -> new Sht104MVisual.Renderer());
        e.registerBlockEntityRenderer(Product.TYPE104C.get(), c -> new Sht104MVisual.RendererC());
        e.registerBlockEntityRenderer(Product.TYPE104CS.get(), c -> new Sht104MVisual.RendererCS());
        e.registerBlockEntityRenderer(Product.TYPE705.get(), c -> new Sht705Visual.Renderer());
        e.registerBlockEntityRenderer(Product.TYPE308.get(), c -> new Sht308Visual.Renderer());
        e.registerBlockEntityRenderer(Product.TYPE307.get(), c -> new Sht308Visual.Renderer());
        e.registerBlockEntityRenderer(Product.TYPE305.get(), c -> new Sht305Visual.Renderer());
        e.registerBlockEntityRenderer(Product.TYPE305_PLAIN.get(), c -> new Sht305Visual.Renderer());
        e.registerBlockEntityRenderer(Product.TYPE_HI101.get(), c -> new Hi101Visual.Renderer());
        e.registerBlockEntityRenderer(Product.CHARGER_TYPE.get(), c -> new PortableVisual.Renderer());
        e.registerBlockEntityRenderer(Product.TYPE.get(), c -> new Client.Renderer());
        e.registerBlockEntityRenderer(Product.TYPE102.get(), c -> new Client.Renderer());
        e.registerBlockEntityRenderer(Product.TYPE101N.get(), c -> new Client.Renderer());
    }

    static void devicePose(Station b, PoseStack p) {
        Vec3 offset = DeviceOffsets.get(b.getLevel(), b.getBlockPos());
        p.translate(offset.x, offset.y, offset.z);
        switch ((Direction)b.getBlockState().getValue(WallMountedBlock.FACING)) {
            case NORTH:
                p.translate(0.5, 0.3, 1.0);
                p.mulPose(Axis.YP.rotationDegrees(180.0F));
                break;
            case SOUTH:
                p.translate(0.5, 0.3, 0.0);
                break;
            case EAST:
                p.translate(0.0, 0.3, 0.5);
                p.mulPose(Axis.YP.rotationDegrees(90.0F));
                break;
            case WEST:
                p.translate(1.0, 0.3, 0.5);
                p.mulPose(Axis.YP.rotationDegrees(-90.0F));
        }

        p.scale(6.067961E-4F, 6.067961E-4F, 6.067961E-4F);
        p.translate(-523.5, 0.0, 0.0);
    }

    static IClientItemExtensions itemExtension() {
        return new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (Client.renderer == null) {
                    Minecraft mc = Minecraft.getInstance();
                    Client.renderer = new BlockEntityWithoutLevelRenderer(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels()) {
                        @Override
                        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack p, MultiBufferSource out, int light, int overlay) {
                            if (stack.is(Product.HI101_ITEM.get())) {
                                p.pushPose();
                                p.translate(0.5, 0.22, 0.5);
                                p.scale(8.85E-4F, 8.85E-4F, 8.85E-4F);
                                p.translate(-108.0F, -300.0F, 0.0F);
                                Hi101Visual.draw(p, out, light, true);
                                p.popPose();
                            } else if (stack.is(Product.BASE305_ITEM.get()) || stack.is(Product.BASE305_PLAIN_ITEM.get())) {
                                p.pushPose();
                                p.translate(0.5, 0.14, 0.5);
                                p.scale(8.85E-4F, 8.85E-4F, 8.85E-4F);
                                p.translate(-210.0F, -300.0F, 0.0F);
                                Sht305Visual.draw(p, out, light, true, false, stack.is(Product.BASE305_ITEM.get()));
                                p.popPose();
                            } else if (stack.is(Product.BASE308_ITEM.get()) || stack.is(Product.BASE307_ITEM.get())) {
                                p.pushPose();
                                p.translate(0.5, 0.14, 0.5);
                                p.scale(8.85E-4F, 8.85E-4F, 8.85E-4F);
                                p.translate(-210.0F, -300.0F, 0.0F);
                                Sht308Visual.draw(p, out, light, true, stack.is(Product.BASE307_ITEM.get()));
                                p.popPose();
                            } else if (stack.is(Product.BASE705_ITEM.get())) {
                                p.pushPose();
                                p.translate(0.5, 0.323, 0.5);
                                p.scale(0.001F, 0.001F, 0.001F);
                                p.translate(-240.0F, -346.0F, 0.0F);
                                Sht705Visual.draw(p, out, light, false);
                                p.popPose();
                            } else if (stack.is(Product.BASE104M_ITEM.get()) || stack.is(Product.BASE104C_ITEM.get()) || stack.is(Product.BASE104CS_ITEM.get())
                                )
                             {
                                p.pushPose();
                                p.translate(0.5, 0.29769999999999996, 0.5);
                                p.scale(0.001F, 0.001F, 0.001F);
                                p.translate(-296.0F, -295.0F, 0.0F);
                                Sht104MVisual.draw(
                                    p,
                                    out,
                                    light,
                                    true,
                                    Sht104MItem.variant(stack),
                                    !stack.is(Product.BASE104M_ITEM.get()),
                                    stack.is(Product.BASE104CS_ITEM.get())
                                );
                                p.popPose();
                            } else if (stack.is(Product.HT100S_ITEM.get()) || stack.is(Product.HT100_ITEM.get())) {
                                p.pushPose();
                                p.translate(0.5, 0.2, 0.5);
                                p.scale(0.0017F, 0.0017F, 0.0017F);
                                p.translate(-115.0F, -400.0F, 0.0F);
                                Ht100sVisual.draw(p, out, light, stack.is(Product.HT100_ITEM.get()));
                                p.popPose();
                            } else if (stack.is(Product.HT513_ITEM.get())) {
                                p.pushPose();
                                p.translate(0.5, 0.13, 0.5);
                                p.scale(9.5628423E-4F, 9.5628423E-4F, 9.5628423E-4F);
                                p.translate(-222.5F, -90.0F, 0.0F);
                                Ht513Visual.draw(p, out, light);
                                p.popPose();
                            } else if (stack.is(Product.HT512_ITEM.get())) {
                                p.pushPose();
                                p.translate(0.5, 0.13, 0.5);
                                p.scale(0.0012F, 0.0012F, 0.0012F);
                                p.translate(-232.0F, -90.0F, 0.0F);
                                Ht512Visual.draw(p, out, light);
                                p.popPose();
                            } else if (stack.is(Product.PORTABLE102.get()) || stack.is(Product.PORTABLE104M.get())) {
                                p.pushPose();
                                p.translate(0.4, 0.15, 0.4);
                                p.scale(8.0E-4F, 8.0E-4F, 8.0E-4F);
                                p.translate(0.0F, 200.0F, 0.0F);
                                PortableVisual.handset(
                                    p,
                                    out,
                                    light,
                                    stack.hasTag() && (stack.getTag().getBoolean("ringing") || stack.getTag().getBoolean("talking")),
                                    stack.hasTag() && stack.getTag().getBoolean("intercom"),
                                    stack.is(Product.PORTABLE104M.get())
                                );
                                p.popPose();
                            } else if (stack.is(Product.CHARGER_ITEM.get())) {
                                p.pushPose();
                                p.translate(0.3, 0.1, 0.3);
                                p.scale(0.001F, 0.001F, 0.001F);
                                PortableVisual.stand(p, out, light, true, false, false);
                                p.popPose();
                            } else {
                                boolean radio = stack.is(Product.RADIO.get());
                                boolean n = stack.is(Product.BASE101N_ITEM.get());
                                Surface.select(stack.is(Product.BASE102_ITEM.get()) || n);
                                p.pushPose();
                                p.translate(radio ? 0.36 : -0.02, radio ? 0.45 : 0.18, 0.5);
                                float s = radio ? 7.8E-4F : 9.0E-4F;
                                p.scale(s, s, s);
                                if (n) {
                                    Surface.draw101N(p, out, light, true);
                                } else {
                                    Surface.draw(p, out, light, radio, true, !stack.is(Product.BASE102_ITEM.get()));
                                }

                                p.popPose();
                            }
                        }
                    };
                }

                return Client.renderer;
            }
        };
    }

    static class Renderer implements BlockEntityRenderer<Station> {
        KvmCameraView video = new KvmCameraView();

        public boolean shouldRenderOffScreen(Station b) {
            return false;
        }

        public void render(Station b, float time, PoseStack p, MultiBufferSource out, int light, int overlay) {
            Surface.select(b.wired());
            boolean n = b instanceof Station101N;
            String mesh = n ? "station/101n/" + b.docked : Surface.meshKey(false, b.docked, b.folded);
            if (!DeviceVisibility.hidden(b, GeometryCache.bounds(mesh), pose -> Client.devicePose(b, pose))) {
                p.pushPose();
                Client.devicePose(b, p);
                if (n) {
                    Surface.draw101N(p, out, light, b.docked);
                } else {
                    Surface.draw(p, out, light, false, b.docked, b.folded);
                }

                Surface.bodyPose(p);
                if (n) {
                    Surface.patch(p, out, 405.0F, 582.0F, 49.0F, 18.0F, 72.4F, 15855583, light);
                    p.pushPose();
                    p.translate(409.0, 113.0, 72.5);
                    p.scale(1.0F, -1.0F, 1.0F);
                    Minecraft.getInstance().font.drawInBatch("재발신", 0.0F, 0.0F, 3356730, false, p.last().pose(), out, DisplayMode.NORMAL, 0, light);
                    p.popPose();
                }

                BlockPos cam = b.camera();
                if (cam != null) {
                    p.pushPose();
                    p.scale(16.0F, 16.0F, 16.0F);
                    this.video.drawWall(p, out, cam, 770.0F, 382.0F, 578.0F, 526.0F, 66.0F, b.brightness, b.contrast, VideoFormat.MONOCHROME);
                    p.popPose();
                }

                for (Indicators.Lamp lamp : Indicators.forModel(b.wired())) {
                    if ((b.lights() & lamp.bit()) != 0) {
                        Surface.indicator(p, out, lamp);
                    }
                }

                String logo = b.productLogo.text();
                if (!logo.equals("SAMSUNG")) {
                    Surface.patch(p, out, 306.0F, 131.0F, 143.0F, 26.0F, 77.0F, 15855583, light);
                    p.pushPose();
                    p.translate(310.0F, 563.0F, 78.0F);
                    p.scale(0.7F, -0.7F, 0.7F);
                    Minecraft.getInstance().font.drawInBatch(logo, 0.0F, 0.0F, 5464144, false, p.last().pose(), out, DisplayMode.NORMAL, 0, light);
                    p.popPose();
                }

                p.popPose();
                p.popPose();
            }
        }
    }
}
