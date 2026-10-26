package com.qwerty.homenet.client;

import com.qwerty.homenet.HomeNet;
import com.qwerty.homenet.client.am.Am10kpRenderer;
import com.qwerty.homenet.client.ha.Ha4100hRenderer;
import com.qwerty.homenet.client.hmc.Hmc7000Renderer;
import com.qwerty.homenet.client.ipap.Ip203apRenderer;
import com.qwerty.homenet.client.kam.Kam305Renderer;
import com.qwerty.homenet.client.klp.Klp203rfRenderer;
import com.qwerty.homenet.client.kvm.Kvm906Renderer;
import com.qwerty.homenet.client.logo.LegacyLogoRenderer;
import com.qwerty.homenet.client.move.MovingModel;
import com.qwerty.homenet.client.move.MovingRenderer;
import com.qwerty.homenet.client.samsung.Sht5500sRenderer;
import com.qwerty.homenet.registry.ModBlockEntities;
import java.util.List;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.minecraftforge.client.event.ModelEvent.ModifyBakingResult;
import net.minecraftforge.client.event.ModelEvent.RegisterAdditional;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(
    modid = "qwertys_homenet",
    bus = Bus.MOD,
    value = {Dist.CLIENT}
)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void registerRenderers(RegisterRenderers event) {
        MovingRenderer.register(event, ModBlockEntities.KLP_203RF.get(), Klp203rfRenderer::new);
        MovingRenderer.register(event, ModBlockEntities.RECEIVER.get(), LegacyLogoRenderer::new);
        MovingRenderer.register(event, ModBlockEntities.WALLPAD.get(), LegacyLogoRenderer::new);
        MovingRenderer.register(event, ModBlockEntities.DOOR_STATION.get(), LegacyLogoRenderer::new);
        MovingRenderer.register(event, ModBlockEntities.KAM_305.get(), Kam305Renderer::new);
        MovingRenderer.register(event, ModBlockEntities.HA_4100H.get(), Ha4100hRenderer::new);
        MovingRenderer.register(event, ModBlockEntities.HMC_7000.get(), Hmc7000Renderer::new);
        MovingRenderer.register(event, ModBlockEntities.SHT_5500S.get(), Sht5500sRenderer::new);
        MovingRenderer.register(event, ModBlockEntities.AM_10KP.get(), Am10kpRenderer::new);
        MovingRenderer.register(event, ModBlockEntities.IP_203AP.get(), Ip203apRenderer::new);
        MovingRenderer.register(event, ModBlockEntities.IP_203APB.get(), c -> new Ip203apRenderer(c, true));
        MovingRenderer.register(event, ModBlockEntities.IP_200PC.get(), c -> new Ip203apRenderer(c, "ip200pc"));
        MovingRenderer.register(event, ModBlockEntities.IP_200PCHB.get(), c -> new Ip203apRenderer(c, "ip200pchb"));
        MovingRenderer.register(event, ModBlockEntities.DP_202H.get(), c -> new Ip203apRenderer(c, "dp202h"));
        MovingRenderer.register(event, ModBlockEntities.KIP_234R2.get(), c -> new Ip203apRenderer(c, "kip234r2"));
        MovingRenderer.register(event, ModBlockEntities.KVM_906.get(), Kvm906Renderer::new);
    }

    @SubscribeEvent
    public static void movingModels(ModifyBakingResult event) {
        event.getModels()
            .replaceAll(
                (id, model) -> (BakedModel)(id instanceof ModelResourceLocation variant
                        && id.getNamespace().equals("qwertys_homenet")
                        && !variant.getVariant().equals("inventory")
                    ? new MovingModel(model)
                    : model)
            );
    }

    @SubscribeEvent
    public static void registerModels(RegisterAdditional event) {
        for (String p : List.of("body", "handset")) {
            event.register(HomeNet.id("block/ha_4100h_" + p));
        }

        for (String part : List.of("skirt", "body", "handset", "screen")) {
            event.register(HomeNet.id("block/hmc_7000_" + part));
        }

        for (String part : List.of("base", "deck", "panel", "handset")) {
            event.register(HomeNet.id("block/sht_5500s_" + part));
        }

        for (String part : List.of("inventory", "hardware", "hardware_open")) {
            event.register(HomeNet.id("block/ip_203ap_" + part));
            event.register(HomeNet.id("block/ip_203apb_" + part));
            event.register(HomeNet.id("block/kip_234r2_" + part));
            event.register(HomeNet.id("block/dp_202h_" + part));
            event.register(HomeNet.id("block/ip_200pchb_" + part));
            event.register(HomeNet.id("block/ip_200pc_" + part));
        }

        for (String part : List.of("body", "handset", "screen")) {
            event.register(HomeNet.id("block/kam_305_" + part));
        }

        event.register(HomeNet.id("block/am_10kp_skirt"));
        event.register(HomeNet.id("block/kam_305_skirt"));
        event.register(HomeNet.id("block/am_10kp_body"));
        event.register(HomeNet.id("block/am_10kp_handset"));
        Kvm906Renderer.MODELS.forEach(event::register);
    }
}
