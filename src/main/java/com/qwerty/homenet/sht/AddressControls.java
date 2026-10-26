package com.qwerty.homenet.sht;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

final class AddressControls extends Screen {
    final BlockPos pos;
    final String current;
    EditBox unit;

    AddressControls(BlockPos p, String n) {
        super(Component.literal("삼성 동·호 설정"));
        this.pos = p;
        this.current = n;
    }

    @Override
    protected void init() {
        this.unit = new EditBox(this.font, this.width / 2 - 110, this.height / 2 - 10, 220, 20, Component.literal("동-호"));
        this.unit.setMaxLength(32);
        this.unit.setValue(this.current);
        this.addRenderableWidget(this.unit);
        this.addRenderableWidget(
            Button.builder(Component.literal("동·호 저장"), b -> Wire.send(this.pos, false, "unit", this.unit.getValue()))
                .bounds(this.width / 2 - 110, this.height / 2 + 20, 220, 20)
                .build()
        );
        this.setInitialFocus(this.unit);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int x, int y, float dt) {
        this.renderBackground(g);
        g.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 50, 16777215);
        g.drawCenteredString(this.font, "동-호 (예: 101-1001)", this.width / 2, this.height / 2 - 30, 14540253);
        super.render(g, x, y, dt);
    }
}
