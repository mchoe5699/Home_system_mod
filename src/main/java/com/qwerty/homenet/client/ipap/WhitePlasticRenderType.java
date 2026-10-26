package com.qwerty.homenet.client.ipap;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard.TextureStateShard;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.resources.ResourceLocation;

public final class WhitePlasticRenderType extends RenderType {
    private static final Map<ResourceLocation, RenderType> TYPES = new HashMap<>();

    private WhitePlasticRenderType() {
        super("ip_white_plastic", DefaultVertexFormat.NEW_ENTITY, Mode.QUADS, 256, false, false, () -> {}, () -> {});
    }

    public static RenderType get(ResourceLocation texture) {
        return TYPES.computeIfAbsent(
            texture,
            t -> create(
                "ip_white_plastic",
                DefaultVertexFormat.NEW_ENTITY,
                Mode.QUADS,
                256,
                false,
                false,
                CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                    .setTextureState(new TextureStateShard(t, false, false))
                    .setTransparencyState(NO_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(NO_LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .createCompositeState(true)
            )
        );
    }
}
