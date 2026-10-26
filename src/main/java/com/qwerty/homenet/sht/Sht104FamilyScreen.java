package com.qwerty.homenet.sht;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

final class Sht104FamilyScreen extends Screen {
    private final BlockPos pos;
    private String selectedModel;

    Sht104FamilyScreen(BlockPos pos) {
        super(Component.literal("SHT-104 형제모델"));
        this.pos = pos;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        if (this.selectedModel == null) {
            String[] models = new String[]{"SHT-104M", "SHT-104C", "SHT-104CS"};

            for (int i = 0; i < models.length; i++) {
                String model = models[i];
                this.addRenderableWidget(Button.builder(Component.literal(model), button -> {
                    this.selectedModel = model;
                    this.rebuildWidgets();
                }).bounds(this.width / 2 - 100, this.height / 2 - 29 + i * 28, 200, 23).build());
            }
        } else {
            String[] labels = new String[]{"A (녹화·재생)", "B (무선전화·내선)", "C (기본형)", "D (경보방송)"};
            int count = this.selectedModel.equals("SHT-104CS") ? 3 : 4;

            for (int i = 0; i < count; i++) {
                String variant = String.valueOf((char)(65 + i));
                this.addRenderableWidget(Button.builder(Component.literal(labels[i]), button -> {
                    Wire.change104(this.pos, this.selectedModel, variant);
                    this.onClose();
                }).bounds(this.width / 2 - 100, this.height / 2 - 43 + i * 27, 200, 23).build());
            }

            this.addRenderableWidget(Button.builder(Component.literal("모델 다시 선택"), button -> {
                this.selectedModel = null;
                this.rebuildWidgets();
            }).bounds(this.width / 2 - 100, this.height / 2 - 43 + count * 27, 200, 23).build());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 58, -987418);
        ClientLevel level = Minecraft.getInstance().level;
        String installed = level != null && level.getBlockEntity(this.pos) instanceof Station104M.Unit unit ? unit.modelName() : "없음";
        graphics.drawCenteredString(
            this.font,
            this.selectedModel == null ? "설치 모델: " + installed + " · 모델을 선택하세요" : this.selectedModel + " · 타입을 선택하세요",
            this.width / 2,
            this.height / 2 - 65,
            -3548180
        );
        super.render(graphics, mouseX, mouseY, partial);
    }
}
