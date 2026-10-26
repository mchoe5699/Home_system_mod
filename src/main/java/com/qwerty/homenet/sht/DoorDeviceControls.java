package com.qwerty.homenet.sht;

import com.qwerty.homenet.intercom.IntercomLine;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class DoorDeviceControls extends Screen {
    private Wire.DoorView data;
    private EditBox message;
    private int refreshTicks;

    private DoorDeviceControls(Wire.DoorView data) {
        super(Component.literal(data.model()));
        this.data = data;
    }

    static void receive(Wire.DoorView view) {
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof DoorDeviceControls screen && screen.data.pos().equals(view.pos())) {
            screen.data = view;
        } else {
            client.setScreen(new DoorDeviceControls(view));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (++this.refreshTicks >= 20) {
            this.refreshTicks = 0;
            Wire.doorOpen(this.data.pos());
        }
    }

    @Override
    protected void init() {
        int x = Math.max(4, (this.width - 360) / 2);
        int y = Math.max(8, (this.height - 210) / 2);
        this.message = new EditBox(this.font, x + 135, y + 148, 150, 20, Component.literal("통화 메시지"));
        this.message.setMaxLength(80);
        this.message.setHint(Component.literal("통화 메시지"));
        this.addRenderableWidget(this.message);
        this.addRenderableWidget(Button.builder(Component.literal("전송"), b -> {
            Wire.doorAction(this.data.pos(), "message", this.message.getValue());
            this.message.setValue("");
        }).bounds(x + 289, y + 148, 67, 20).build());
        this.addRenderableWidget(
            Button.builder(
                    Component.literal(this.data.inCall() ? "통화 종료" : "호출"), b -> Wire.doorAction(this.data.pos(), this.data.inCall() ? "hang" : "call", "")
                )
                .bounds(x, y + 148, 126, 20)
                .build()
        );
        this.addRenderableWidget(Button.builder(Component.literal("닫기"), b -> this.onClose()).bounds(x, y + 174, 126, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        this.renderBackground(g);
        int x = Math.max(4, (this.width - 360) / 2);
        int y = Math.max(8, (this.height - 210) / 2);
        g.drawString(this.font, this.data.model(), x, y - 12, -1120040);
        if (!this.data.model().equals("도어폰") && !this.data.model().equals("도어카메라")) {
            g.fill(x, y, x + 128, y + 128, -3684169);
            g.fill(x + 18, y + 12, x + 110, y + 80, -14669529);
            g.drawCenteredString(this.font, this.data.model(), x + 64, y + 97, -14339795);
            g.fill(x + 88, y + 91, x + 101, y + 104, -12694200);
        } else {
            String texture = this.data.model().equals("도어폰") ? "door_phone_front" : "door_station_front";
            ResourceLocation image = new ResourceLocation("qwertys_homenet", "textures/block/" + texture + ".png");
            g.blit(image, x, y, 0.0F, 0.0F, 128, 128, 128, 128);
        }

        g.drawString(this.font, "연결 " + this.data.linked() + "개 · " + (this.data.inCall() ? "통화 중" : "대기"), x, y + 131, -1645872);
        if (!this.data.unit().isBlank()) {
            g.drawString(this.font, "세대 " + this.data.unit(), x, y + 205, -2566975);
        }

        g.fill(x + 135, y, x + 356, y + 137, -14670044);
        g.drawString(this.font, "통화 기록", x + 143, y + 7, -922926);
        List<IntercomLine> lines = this.data.log();
        int first = Math.max(0, lines.size() - 8);

        for (int i = first; i < lines.size(); i++) {
            IntercomLine line = lines.get(i);
            String message = line.name() + ": " + line.text();
            if (message.length() > 30) {
                message = message.substring(0, 30) + "…";
            }

            g.drawString(this.font, message, x + 143, y + 22 + (i - first) * 13, -2695724);
        }

        super.render(g, mx, my, partial);
    }
}
