package com.qwerty.homenet.sht;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.InflaterInputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

final class Sht308Mesh {
    private static final Map<String, float[]> CACHE = new ConcurrentHashMap<>();

    private Sht308Mesh() {
    }

    static void clear() {
        CACHE.clear();
    }

    private static float[] load(String path) {
        try {
            float[] var14;
            try (
                InputStream resource = Minecraft.getInstance().getResourceManager().open(Product.id(path));
                DataInputStream header = new DataInputStream(resource);
            ) {
                if (header.readInt() != 1395863608) {
                    throw new IOException("Invalid SHT-308 mesh: " + path);
                }

                int count = header.readInt();
                if (count < 1 || count > 200000) {
                    throw new IOException("Invalid triangle count: " + count);
                }

                float[] values = new float[count * 3 * 7];
                DataInputStream data = new DataInputStream(new InflaterInputStream(header));

                for (int i = 0; i < count * 3; i++) {
                    int at = i * 7;

                    for (int k = 0; k < 6; k++) {
                        values[at + k] = data.readFloat();
                    }

                    values[at + 6] = Float.intBitsToFloat(data.readInt());
                }

                if (data.read() != -1) {
                    throw new IOException("Trailing SHT-308 mesh data: " + path);
                }

                var14 = values;
            }

            return var14;
        } catch (IOException var13) {
            throw new IllegalStateException("Cannot load SHT-308 mesh " + path, var13);
        }
    }

    static void draw(String filename, PoseStack pose, MultiBufferSource out) {
        float[] a = CACHE.computeIfAbsent(filename.contains("/") ? filename : "sht308/" + filename, Sht308Mesh::load);
        VertexConsumer buffer = out.getBuffer(RenderType.entityCutoutNoCull(Product.id("textures/solid.png")));

        for (int i = 0; i < a.length; i += 21) {
            for (int corner : new int[]{0, 1, 2, 2}) {
                int at = i + corner * 7;
                int color = Float.floatToRawIntBits(a[at + 6]);
                Surface.vertex(buffer, pose.last(), a[at], a[at + 1], a[at + 2], a[at + 3], a[at + 4], a[at + 5], color, 0, 0.5F, 0.5F);
            }
        }
    }
}
