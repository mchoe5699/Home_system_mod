package com.qwerty.homenet.client.logo;

import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.client.ReceiverScreen;
import com.qwerty.homenet.network.ModNetwork;
import com.qwerty.homenet.network.WallpadActionPacket;
import com.qwerty.homenet.network.WallpadDataPacket;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class LogoScreen extends Screen implements ReceiverScreen {
    private WallpadDataPacket data;
    private EditBox text;
    private EditBox texture;
    private String mode;

    public LogoScreen(WallpadDataPacket p) {
        super(Component.literal("제품 로고"));
        this.data = p;
        this.mode = p.setting("product_logo_mode", "default");
    }

    @Override
    public BlockPos getPos() {
        return this.data.pos();
    }

    @Override
    public void update(WallpadDataPacket p) {
        this.data = p;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void save(String k, String v) {
        ModNetwork.sendToServer(new WallpadActionPacket(this.getPos(), ReceiverBlockEntity.Action.SET_SETTING.ordinal(), BlockPos.ZERO, k + "=" + v));
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 140;
        int y = this.height / 2 - 100;
        this.text = new EditBox(this.font, x, y + 54, 280, 20, Component.literal("로고 문구"));
        this.text.setMaxLength(32);
        this.text.setValue(this.data.setting("product_logo_text", "KOCOM"));
        this.addRenderableWidget(this.text);
        this.texture = new EditBox(this.font, x, y + 101, 280, 20, Component.literal("텍스처 주소"));
        this.texture.setMaxLength(160);
        this.texture.setValue(this.data.setting("product_logo_texture", ""));
        this.addRenderableWidget(this.texture);
        this.addRenderableWidget(Button.builder(Component.literal("방식: " + this.mode), b -> {
            String[] modes = new String[]{"default", "none", "text", "custom"};
            int i = List.of(modes).indexOf(this.mode);
            this.mode = modes[(i + 1) % 4];
            b.setMessage(Component.literal("방식: " + this.mode));
        }).bounds(x, y + 12, 280, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("저장"), b -> {
            this.save("logo_text", this.text.getValue());
            this.save("logo_texture", this.texture.getValue());
            this.save("logo_mode", this.mode);
        }).bounds(x, y + 150, 135, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("닫기"), b -> this.onClose()).bounds(x + 145, y + 150, 135, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int x, int y, float t) {
        this.renderBackground(g);
        int bx = this.width / 2 - 140;
        int by = this.height / 2 - 100;
        g.drawString(this.font, "제품 로고 · 외관 설정", bx, by - 12, -1);
        g.drawString(this.font, "문구 (text 방식)", bx, by + 41, -2038054);
        g.drawString(this.font, "리소스팩 namespace:textures/...png", bx, by + 87, -2038054);
        super.render(g, x, y, t);
    }
}
