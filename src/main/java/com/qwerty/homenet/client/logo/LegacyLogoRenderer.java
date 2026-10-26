package com.qwerty.homenet.client.logo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.qwerty.homenet.HomeNet;
import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.blockentity.CallerBlockEntity;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.blockentity.WallpadBlockEntity;
import com.qwerty.homenet.logo.ProductLogo;
import com.qwerty.homenet.registry.ModBlocks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

public class LegacyLogoRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    public LegacyLogoRenderer(Context c) {
    }

    @Override
    public void render(T b, float t, PoseStack p, MultiBufferSource out, int light, int overlay) {
        ProductLogo logo = b instanceof ReceiverBlockEntity r ? r.productLogo : (b instanceof CallerBlockEntity c ? c.productLogo : null);
        if (logo != null && logo.configured() && !logo.mode().equals("default")) {
            float depth = b instanceof WallpadBlockEntity ? 15.089F : (b instanceof CallerBlockEntity ? 14.99F : 13.99F);
            float y = b instanceof WallpadBlockEntity ? 4.82F : (b instanceof CallerBlockEntity ? 4.3F : 3.4F);
            p.pushPose();
            p.translate(0.5, 0.0, 0.5);

            p.mulPose(Axis.YN.rotationDegrees(switch ((Direction)b.getBlockState().getValue(WallMountedBlock.FACING)) {
                case EAST -> 90.0F;
                case SOUTH -> 180.0F;
                case WEST -> 270.0F;
                default -> 0.0F;
            }));
            p.translate(-0.5, 0.0, -0.5);
            if (b.getBlockState().getBlock() == ModBlocks.GUARD_CONSOLE.get()) {
                LogoRenderer.desk((ReceiverBlockEntity)b, p, out, 6.9F, 3.2F, 9.1F, 3.56F, 3.01F, HomeNet.id("textures/logo/legacy_default.png"), light);
            } else {
                LogoRenderer.wallLogo(logo, p, out, 6.9F, y, 9.1F, y + 0.36F, depth, HomeNet.id("textures/logo/legacy_default.png"), light);
            }

            p.popPose();
        }
    }
}
