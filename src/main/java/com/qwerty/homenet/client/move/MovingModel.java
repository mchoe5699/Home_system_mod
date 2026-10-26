package com.qwerty.homenet.client.move;

import com.qwerty.homenet.move.DeviceOffsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

public final class MovingModel extends BakedModelWrapper<BakedModel> {
    public static final ModelProperty<Vec3> OFFSET = new ModelProperty<>();

    public MovingModel(BakedModel parent) {
        super(parent);
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter l, BlockPos p, BlockState s, ModelData d) {
        return this.originalModel.getModelData(l, p, s, d).derive().with(OFFSET, DeviceOffsets.get(l, p)).build();
    }

    @Override
    public List<BakedQuad> getQuads(BlockState s, Direction side, RandomSource r, ModelData data, RenderType type) {
        List<BakedQuad> quads = this.originalModel.getQuads(s, side, r, data, type);
        Vec3 v = data.get(OFFSET);
        if (v != null && !v.equals(Vec3.ZERO)) {
            ArrayList<BakedQuad> result = new ArrayList<>(quads.size());

            for (BakedQuad q : quads) {
                int[] a = (int[])q.getVertices().clone();
                int stride = a.length / 4;

                for (int i = 0; i < 4; i++) {
                    int j = i * stride;
                    a[j] = Float.floatToRawIntBits(Float.intBitsToFloat(a[j]) + (float)v.x);
                    a[j + 1] = Float.floatToRawIntBits(Float.intBitsToFloat(a[j + 1]) + (float)v.y);
                    a[j + 2] = Float.floatToRawIntBits(Float.intBitsToFloat(a[j + 2]) + (float)v.z);
                }

                result.add(new BakedQuad(a, q.getTintIndex(), q.getDirection(), q.getSprite(), q.isShade(), q.hasAmbientOcclusion()));
            }

            return result;
        } else {
            return quads;
        }
    }
}
