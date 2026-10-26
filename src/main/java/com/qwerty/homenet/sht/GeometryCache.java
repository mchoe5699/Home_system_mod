package com.qwerty.homenet.sht;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

final class GeometryCache {
    private static final Map<String, List<GeometryCache.Batch>> MESHES = new HashMap<>();
    private static final Map<String, AABB> BOUNDS = new HashMap<>();
    static int builds;

    static void clear() {
        MESHES.clear();
        BOUNDS.clear();
        builds = 0;
    }

    static AABB bounds(String key) {
        AABB result = BOUNDS.get(key);
        if (result != null) {
            return result;
        } else {
            List<GeometryCache.Batch> mesh = MESHES.get(key);
            if (mesh == null) {
                return null;
            } else {
                double minX = Double.POSITIVE_INFINITY;
                double minY = minX;
                double minZ = minX;
                double maxX = Double.NEGATIVE_INFINITY;
                double maxY = maxX;
                double maxZ = maxX;

                for (GeometryCache.Batch batch : mesh) {
                    if (batch.type == RenderType.entityCutoutNoCull(Product.id("textures/solid.png"))) {
                        for (int i = 0; i < batch.data.length; i += 11) {
                            float[] a = batch.data;
                            minX = Math.min(minX, (double)a[i]);
                            minY = Math.min(minY, (double)a[i + 1]);
                            minZ = Math.min(minZ, (double)a[i + 2]);
                            maxX = Math.max(maxX, (double)a[i]);
                            maxY = Math.max(maxY, (double)a[i + 1]);
                            maxZ = Math.max(maxZ, (double)a[i + 2]);
                        }
                    }
                }

                if (!Double.isFinite(minX)) {
                    return null;
                } else {
                    result = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
                    BOUNDS.put(key, result);
                    return result;
                }
            }
        }
    }

    static void draw(String key, PoseStack pose, MultiBufferSource out, int light, BiConsumer<PoseStack, MultiBufferSource> build) {
        List<GeometryCache.Batch> mesh = MESHES.get(key);
        if (mesh == null) {
            LinkedHashMap<RenderType, GeometryCache.Collector> collectors = new LinkedHashMap<>();
            build.accept(new PoseStack(), type -> collectors.computeIfAbsent(type, t -> new GeometryCache.Collector()));
            mesh = new ArrayList<>(collectors.size());

            for (Entry<RenderType, GeometryCache.Collector> e : collectors.entrySet()) {
                mesh.add(new GeometryCache.Batch(e.getKey(), compact(e.getKey(), e.getValue())));
            }

            MESHES.put(key, mesh);
            builds++;
        }

        Pose transform = pose.last();
        Vector3f position = new Vector3f();
        Vector3f normal = new Vector3f();

        for (GeometryCache.Batch batch : mesh) {
            VertexConsumer consumer = out.getBuffer(batch.type);
            float[] a = batch.data;

            for (int i = 0; i < a.length; i += 11) {
                int color = Float.floatToRawIntBits(a[i + 8]);
                int storedLight = Float.floatToRawIntBits(a[i + 9]);
                transform.pose().transformPosition(a[i], a[i + 1], a[i + 2], position);
                transform.normal().transform(a[i + 5], a[i + 6], a[i + 7], normal);
                consumer.vertex(
                    position.x,
                    position.y,
                    position.z,
                    (color >>> 24) / 255.0F,
                    (color >>> 16 & 0xFF) / 255.0F,
                    (color >>> 8 & 0xFF) / 255.0F,
                    (color & 0xFF) / 255.0F,
                    a[i + 3],
                    a[i + 4],
                    Float.floatToRawIntBits(a[i + 10]),
                    storedLight == 0 ? light : storedLight,
                    normal.x,
                    normal.y,
                    normal.z
                );
            }
        }
    }

    private static float[] compact(RenderType type, GeometryCache.Collector source) {
        if (type != RenderType.entityCutoutNoCull(Product.id("textures/solid.png"))) {
            return Arrays.copyOf(source.data, source.size);
        } else {
            LinkedHashMap<GeometryCache.Plane, List<GeometryCache.Rect>> planes = new LinkedHashMap<>();
            GeometryCache.Collector result = new GeometryCache.Collector();
            float[] a = source.data;

            for (int i = 0; i < source.size; i += 44) {
                float x0 = Float.POSITIVE_INFINITY;
                float x1 = Float.NEGATIVE_INFINITY;
                float y0 = x0;
                float y1 = x1;
                boolean flat = true;

                for (int j = 0; j < 4; j++) {
                    int at = i + j * 11;
                    x0 = Math.min(x0, a[at]);
                    x1 = Math.max(x1, a[at]);
                    y0 = Math.min(y0, a[at + 1]);
                    y1 = Math.max(y1, a[at + 1]);

                    for (int k = 2; k < 11; k++) {
                        if (a[at + k] != a[i + k] && Float.floatToRawIntBits(a[at + k]) != Float.floatToRawIntBits(a[i + k])) {
                            flat = false;
                        }
                    }
                }

                int corners = 0;

                for (int j = 0; j < 4; j++) {
                    float x = a[i + j * 11];
                    float y = a[i + j * 11 + 1];
                    if (x != x0 && x != x1 || y != y0 && y != y1) {
                        flat = false;
                    }

                    corners |= 1 << ((x == x1 ? 1 : 0) | (y == y1 ? 2 : 0));
                }

                if (flat && corners == 15 && x0 != x1 && y0 != y1) {
                    int pattern = 0;

                    for (int j = 0; j < 4; j++) {
                        pattern |= ((a[i + j * 11] == x1 ? 1 : 0) | (a[i + j * 11 + 1] == y1 ? 2 : 0)) << j * 2;
                    }

                    GeometryCache.Plane plane = new GeometryCache.Plane(
                        Float.floatToRawIntBits(a[i + 2]),
                        Float.floatToRawIntBits(a[i + 5]),
                        Float.floatToRawIntBits(a[i + 6]),
                        Float.floatToRawIntBits(a[i + 7]),
                        Float.floatToRawIntBits(a[i + 3]),
                        Float.floatToRawIntBits(a[i + 4]),
                        Float.floatToRawIntBits(a[i + 8]),
                        Float.floatToRawIntBits(a[i + 9]),
                        Float.floatToRawIntBits(a[i + 10]),
                        pattern
                    );
                    planes.computeIfAbsent(plane, kx -> new ArrayList<>()).add(new GeometryCache.Rect(a, i, x0, x1, y0, y1));
                } else {
                    result.append(a, i, 44);
                }
            }

            for (List<GeometryCache.Rect> rectangles : planes.values()) {
                for (GeometryCache.Rect rect : merge(merge(rectangles, true), false)) {
                    float minX = Float.POSITIVE_INFINITY;
                    float minY = minX;
                    float maxX = Float.NEGATIVE_INFINITY;
                    float maxY = maxX;

                    for (int j = 0; j < 4; j++) {
                        minX = Math.min(minX, rect.sample[j * 11]);
                        maxX = Math.max(maxX, rect.sample[j * 11]);
                        minY = Math.min(minY, rect.sample[j * 11 + 1]);
                        maxY = Math.max(maxY, rect.sample[j * 11 + 1]);
                    }

                    for (int j = 0; j < 4; j++) {
                        rect.sample[j * 11] = rect.sample[j * 11] == minX ? rect.x0 : rect.x1;
                        rect.sample[j * 11 + 1] = rect.sample[j * 11 + 1] == minY ? rect.y0 : rect.y1;
                    }

                    result.append(rect.sample, 0, 44);
                }
            }

            return Arrays.copyOf(result.data, result.size);
        }
    }

    private static List<GeometryCache.Rect> merge(List<GeometryCache.Rect> rectangles, boolean horizontal) {
        LinkedHashMap<GeometryCache.Span, List<GeometryCache.Rect>> groups = new LinkedHashMap<>();

        for (GeometryCache.Rect r : rectangles) {
            groups.computeIfAbsent(horizontal ? new GeometryCache.Span(r.y0, r.y1) : new GeometryCache.Span(r.x0, r.x1), k -> new ArrayList<>()).add(r);
        }

        ArrayList<GeometryCache.Rect> result = new ArrayList<>();

        for (List<GeometryCache.Rect> group : groups.values()) {
            group.sort(Comparator.comparingDouble(rx -> horizontal ? rx.x0 : rx.y0));
            GeometryCache.Rect previous = null;

            for (GeometryCache.Rect r : group) {
                if (previous != null && (horizontal ? previous.x1 == r.x0 : previous.y1 == r.y0)) {
                    if (horizontal) {
                        previous.x1 = r.x1;
                    } else {
                        previous.y1 = r.y1;
                    }
                } else {
                    result.add(r);
                    previous = r;
                }
            }
        }

        return result;
    }

    private record Batch(RenderType type, float[] data) {
    }

    private static final class Collector implements VertexConsumer {
        float[] data = new float[11264];
        int size;
        float x;
        float y;
        float z;
        float u;
        float v;
        float nx;
        float ny;
        float nz;
        int color;
        int light;
        int overlay;

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            this.x = (float)x;
            this.y = (float)y;
            this.z = (float)z;
            return this;
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a) {
            this.color = r << 24 | g << 16 | b << 8 | a;
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            this.u = u;
            this.v = v;
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            this.overlay = u | v << 16;
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            this.light = u | v << 16;
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            this.nx = x;
            this.ny = y;
            this.nz = z;
            return this;
        }

        void append(float[] input, int offset, int count) {
            while (this.size + count > this.data.length) {
                this.data = Arrays.copyOf(this.data, this.data.length * 2);
            }

            System.arraycopy(input, offset, this.data, this.size, count);
            this.size += count;
        }

        @Override
        public void endVertex() {
            if (this.size + 11 > this.data.length) {
                this.data = Arrays.copyOf(this.data, this.data.length * 2);
            }

            this.data[this.size++] = this.x;
            this.data[this.size++] = this.y;
            this.data[this.size++] = this.z;
            this.data[this.size++] = this.u;
            this.data[this.size++] = this.v;
            this.data[this.size++] = this.nx;
            this.data[this.size++] = this.ny;
            this.data[this.size++] = this.nz;
            this.data[this.size++] = Float.intBitsToFloat(this.color);
            this.data[this.size++] = Float.intBitsToFloat(this.light);
            this.data[this.size++] = Float.intBitsToFloat(this.overlay);
        }

        @Override
        public void defaultColor(int r, int g, int b, int a) {
            this.color(r, g, b, a);
        }

        @Override
        public void unsetDefaultColor() {
        }
    }

    private record Plane(int z, int nx, int ny, int nz, int u, int v, int color, int light, int overlay, int winding) {
    }

    private static final class Rect {
        float x0;
        float x1;
        float y0;
        float y1;
        final float[] sample;

        Rect(float[] a, int offset, float x0, float x1, float y0, float y1) {
            this.sample = Arrays.copyOfRange(a, offset, offset + 44);
            this.x0 = x0;
            this.x1 = x1;
            this.y0 = y0;
            this.y1 = y1;
        }
    }

    private record Span(float lo, float hi) {
    }
}
