package com.qwerty.homenet.client.ha;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.qwerty.homenet.HomeNet;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;

final class Ha4100hSurfaces {
    private final List<Ha4100hSurfaces.Surface> surfaces = new ArrayList<>();

    Ha4100hSurfaces() {
        try {
            try (BufferedReader reader = Minecraft.getInstance().getResourceManager().openAsReader(HomeNet.id("ha_4100h_surfaces.json"))) {
                for (JsonElement element : JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("surfaces")) {
                    JsonObject obj = element.getAsJsonObject();
                    JsonArray points = obj.getAsJsonArray("vertices");
                    float[][] vertices = new float[points.size()][8];

                    for (int i = 0; i < points.size(); i++) {
                        for (int j = 0; j < 8; j++) {
                            vertices[i][j] = points.get(i).getAsJsonArray().get(j).getAsFloat();
                        }
                    }

                    JsonArray indices = obj.getAsJsonArray("faces");
                    int[][] faces = new int[indices.size()][4];

                    for (int i = 0; i < indices.size(); i++) {
                        for (int j = 0; j < 4; j++) {
                            faces[i][j] = indices.get(i).getAsJsonArray().get(j).getAsInt();
                        }
                    }

                    this.surfaces.add(new Ha4100hSurfaces.Surface(obj.get("texture").getAsString(), vertices, faces));
                }
            }
        } catch (IOException var13) {
            throw new IllegalStateException("Missing HA-4100H surface geometry", var13);
        }
    }

    void render(PoseStack p, MultiBufferSource out, int light, boolean lifted) {
        for (Ha4100hSurfaces.Surface surface : this.surfaces) {
            if (!lifted || !surface.texture.equals("handset")) {
                VertexConsumer buffer = out.getBuffer(RenderType.entityCutoutNoCull(HomeNet.id("textures/block/ha4100h/" + surface.texture + ".png")));

                for (int[] face : surface.faces) {
                    for (int index : face) {
                        float[] v = surface.vertices[index];
                        buffer.vertex(p.last().pose(), v[0] / 16.0F, v[1] / 16.0F, v[2] / 16.0F)
                            .color(255, 255, 255, 255)
                            .uv(v[3], v[4])
                            .overlayCoords(OverlayTexture.NO_OVERLAY)
                            .uv2(light)
                            .normal(p.last().normal(), v[5], v[6], v[7])
                            .endVertex();
                    }
                }
            }
        }
    }

    private record Surface(String texture, float[][] vertices, int[][] faces) {
    }
}
