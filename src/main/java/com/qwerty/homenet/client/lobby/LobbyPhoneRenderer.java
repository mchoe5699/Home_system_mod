package com.qwerty.homenet.client.lobby;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.qwerty.homenet.block.LobbyPhoneBlock;
import com.qwerty.homenet.blockentity.LobbyPhoneBlockEntity;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

public class LobbyPhoneRenderer implements BlockEntityRenderer<LobbyPhoneBlockEntity> {
    private static final float K = 0.034408603F;
    private static final float LEFT_PX = 12.266666F;
    private static final float TOP_PX = 12.8F;
    private static final float FRONT_PX = 15.43F;
    private static final float INFO_X1 = 61.0F;
    private static final float INFO_Y1 = 60.0F;
    private static final float INFO_X2 = 135.0F;
    private static final float INFO_Y2 = 102.0F;
    private static final float DOOR_X = 126.4F;
    private static final float ICON_Y = 51.8F;
    private static final int DARK = -14274452;
    private final Font font;

    public LobbyPhoneRenderer(Context ctx) {
        this.font = ctx.getFont();
    }

    public void render(LobbyPhoneBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (be.getLevel() != null && be.isBacklight()) {
            long gameTime = be.getLevel().getGameTime();
            Direction facing = be.getBlockState().getValue(LobbyPhoneBlock.FACING);
            pose.pushPose();
            pose.translate(0.5, 0.5, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
            pose.translate(-0.5, -0.5, -0.5);
            pose.translate(0.76666665F, 0.8F, 0.9633750190734863);
            pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            float unit = 0.0021505377F;
            pose.scale(unit, -unit, unit);
            if (be.isDoorOpen(gameTime)) {
                float is = 0.94F;
                this.draw(pose, buffers, LobbyLcd.icon("\ue001"), 126.4F - 3.75F * is, 51.8F - 4.0F * is, is, -1, false);
            }

            LobbyLcd lcd = LobbyLcd.of(be, gameTime);
            LobbyPhoneBlockEntity.Screen sc = be.getScreen();
            float cx = 98.0F;
            if (!lcd.topLeft().getString().isEmpty()) {
                this.draw(pose, buffers, lcd.topLeft(), 65.0F, 63.0F, 0.62F, -14274452, false);
            }

            if (!lcd.topRight().getString().isEmpty()) {
                float w = this.font.width(lcd.topRight()) * 0.52F;
                this.draw(pose, buffers, lcd.topRight(), 131.0F - w, 63.5F, 0.52F, -14274452, false);
            }

            if (sc == LobbyPhoneBlockEntity.Screen.MESSAGE) {
                float s = 0.85F;
                int maxW = Math.round(68.0F / s);
                List<FormattedCharSequence> lines = this.font.split(lcd.big(), maxW);
                float y0 = 81.0F - lines.size() * 10 * s / 2.0F;

                for (int i = 0; i < lines.size(); i++) {
                    FormattedCharSequence line = lines.get(i);
                    pose.pushPose();
                    pose.translate(cx, y0 + i * 10 * s, 0.0F);
                    pose.scale(s, s, s);
                    Matrix4f m = pose.last().pose();
                    this.font.drawInBatch(line, -this.font.width(line) / 2.0F, 0.0F, -14274452, false, m, buffers, DisplayMode.POLYGON_OFFSET, 0, 15728880);
                    pose.popPose();
                }
            } else {
                float bs = lcd.bigScale();
                boolean bitmap = LobbyLcd.isBitmapBig(lcd.big());
                float textW = Math.max(1, this.font.width(lcd.big()) - (bitmap ? 1 : 0));
                float topPad = lcd.topLeft().getString().isEmpty() ? 4.0F : 10.0F;
                bs = Math.min(bs, 66.0F / textW);
                bs = Math.min(bs, (42.0F - topPad - 3.0F) / 8.0F);
                float areaMid = 60.0F + topPad + (42.0F - topPad - 3.0F) / 2.0F;
                this.draw(pose, buffers, lcd.big(), cx, areaMid - (bitmap ? 4.0F : 4.6F) * bs, bs, -14274452, true);
            }

            if (!lcd.subLeft().getString().isEmpty()) {
                this.draw(pose, buffers, lcd.subLeft(), 61.0F, 108.0F, 0.62F, -1, false);
            }

            if (!lcd.subRight().getString().isEmpty()) {
                float w = this.font.width(lcd.subRight()) * 0.62F;
                this.draw(pose, buffers, lcd.subRight(), 135.0F - w, 108.0F, 0.62F, -1, false);
            }

            pose.popPose();
        }
    }

    private void draw(PoseStack pose, MultiBufferSource buffers, Component c, float x, float y, float scale, int color, boolean center) {
        pose.pushPose();
        pose.translate(x, y, 0.0F);
        pose.scale(scale, scale, scale);
        float w = this.font.width(c);
        Matrix4f m = pose.last().pose();
        this.font.drawInBatch(c, center ? -w / 2.0F : 0.0F, 0.0F, color, false, m, buffers, DisplayMode.POLYGON_OFFSET, 0, 15728880);
        pose.popPose();
    }

    @Override
    public int getViewDistance() {
        return 24;
    }
}
