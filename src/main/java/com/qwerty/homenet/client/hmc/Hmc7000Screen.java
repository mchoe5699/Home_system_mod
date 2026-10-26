package com.qwerty.homenet.client.hmc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.qwerty.homenet.HomeNet;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.client.IntercomConversation;
import com.qwerty.homenet.client.ReceiverScreen;
import com.qwerty.homenet.client.logo.LogoRenderer;
import com.qwerty.homenet.network.ModNetwork;
import com.qwerty.homenet.network.WallpadActionPacket;
import com.qwerty.homenet.network.WallpadDataPacket;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class Hmc7000Screen extends Screen implements ReceiverScreen {
    private WallpadDataPacket data;
    private float scale;
    private int left;
    private int top;
    private EditBox message;
    private EditBox id;
    private final List<Hmc7000Screen.Key> keys = new ArrayList<>();
    private final List<Hmc7000Screen.Key> dials = new ArrayList<>();

    public Hmc7000Screen(WallpadDataPacket data) {
        super(Component.literal("HYUNDAI HMC-7000"));
        this.data = data;

        try {
            try (BufferedReader reader = Minecraft.getInstance().getResourceManager().openAsReader(HomeNet.id("hmc_7000_layout.json"))) {
                JsonObject layout = JsonParser.parseReader(reader).getAsJsonObject();

                for (String group : List.of("keys", "dial")) {
                    for (JsonElement entry : layout.getAsJsonArray(group)) {
                        JsonObject v = entry.getAsJsonObject();
                        (group.equals("keys") ? this.keys : this.dials)
                            .add(
                                new Hmc7000Screen.Key(
                                    v.get("key").getAsString(), v.get("x").getAsInt(), v.get("y").getAsInt(), v.get("w").getAsInt(), v.get("h").getAsInt()
                                )
                            );
                    }
                }
            }
        } catch (IOException var11) {
            throw new IllegalStateException("Missing HMC-7000 key layout", var11);
        }
    }

    @Override
    public BlockPos getPos() {
        return this.data.pos();
    }

    @Override
    public void update(WallpadDataPacket packet) {
        this.data = packet;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void send(ReceiverBlockEntity.Action a, String text) {
        ModNetwork.sendToServer(new WallpadActionPacket(this.getPos(), a.ordinal(), BlockPos.ZERO, text));
    }

    private void key(String value) {
        this.send(ReceiverBlockEntity.Action.SET_SETTING, "key=" + value);
    }

    private void button(String value) {
        if (value.equals("building") && this.data.flag("clock_edit")) {
            this.send(ReceiverBlockEntity.Action.SET_SETTING, "next=1");
        } else {
            this.send(ReceiverBlockEntity.Action.SET_SETTING, "button=" + value);
        }
    }

    @Override
    protected void init() {
        this.scale = Math.min((this.width - 40 - IntercomConversation.width(this.width)) / 1024.0F, (this.height - 108) / 1280.0F);
        this.left = (int)((this.width - IntercomConversation.width(this.width) - 20 - 1024.0F * this.scale) / 2.0F);
        this.top = 26;
        this.message = new EditBox(this.font, 12, this.height - 49, Math.max(100, this.width - 105), 18, Component.literal("통화 메시지"));
        this.message.setMaxLength(80);
        this.addRenderableWidget(this.message);
        this.addRenderableWidget(Button.builder(Component.literal("전송"), b -> {
            this.send(ReceiverBlockEntity.Action.SEND_MESSAGE, this.message.getValue());
            this.message.setValue("");
        }).bounds(this.width - 86, this.height - 49, 74, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("로고 변경"), b -> this.button("logo")).bounds(this.width - 86, this.height - 25, 74, 18).build());
        this.id = new EditBox(this.font, 12, this.height - 25, 72, 18, Component.literal("관리실 번호"));
        this.id.setMaxLength(4);
        this.id.setValue(this.data.unit());
        this.addRenderableWidget(this.id);
        this.addRenderableWidget(
            Button.builder(Component.literal("관리실 번호 저장"), b -> this.send(ReceiverBlockEntity.Action.SET_UNIT, this.id.getValue()))
                .bounds(90, this.height - 25, 119, 18)
                .build()
        );
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        this.renderBackground(g);
        g.drawString(this.font, "HYUNDAI HMC-7000", 12, 8, -1118482);
        g.drawString(this.font, this.font.plainSubstrByWidth(this.data.setting("status", ""), Math.max(20, this.width - 230)), 220, 8, -4731472);
        g.pose().pushPose();
        g.pose().translate((float)this.left, (float)this.top, 0.0F);
        g.pose().scale(this.scale, this.scale, 1.0F);
        g.blit(Hmc7000Renderer.texture(this.data.flag("lifted") ? "open" : "gui"), 0, 0, 0.0F, 0.0F, 1024, 1280, 1024, 1280);
        LogoRenderer.gui(g, this.font, this.data, Hmc7000Renderer.texture("logo"), 352, 1194, 309, 52);
        g.fill(480, 114, 862, 194, this.data.flag("power") ? -7167615 : -9865125);
        if (this.data.flag("power")) {
            g.pose().pushPose();
            g.pose().translate(488.0F, 125.0F, 0.0F);
            g.pose().scale(2.5F, 2.5F, 1.0F);

            for (int i = 0; i < 2; i++) {
                g.drawString(this.font, this.font.plainSubstrByWidth(this.data.setting("lcd" + i, ""), 146), 0, i * 14, -14140894, false);
            }

            g.pose().popPose();
        }

        for (int i = 0; i < Hmc7000Renderer.LIGHTS.length; i++) {
            if (this.data.flag("led" + i)) {
                int[] xy = Hmc7000Renderer.LIGHTS[i];
                g.fill(xy[0] - 6, xy[1] - 6, xy[0] + 6, xy[1] + 6, i < 4 ? -1680835 : -10371173);
            }
        }

        g.pose().popPose();
        IntercomConversation.draw(g, this.font, this.data, this.width, this.height - 65);
        super.render(g, mx, my, partial);
    }

    @Override
    public boolean mouseClicked(double x, double y, int mouse) {
        if (super.mouseClicked(x, y, mouse)) {
            return true;
        } else if (mouse != 0) {
            return false;
        } else {
            this.setFocused(null);
            double u = (x - this.left) / this.scale;
            double v = (y - this.top) / this.scale;
            if (u > 20.0 && u < 302.0 && v > 50.0 && v < 1200.0) {
                this.button("handset");
                return true;
            } else {
                for (Hmc7000Screen.Key k : this.keys) {
                    if (Math.abs(u - k.x) <= k.w / 2.0 + 7.0 && Math.abs(v - k.y) <= k.h / 2.0 + 10.0) {
                        this.button(k.key);
                        return true;
                    }
                }

                for (Hmc7000Screen.Key kx : this.dials) {
                    if (Math.abs(u - kx.x) <= kx.w / 2.0 + 7.0 && Math.abs(v - kx.y) <= kx.h / 2.0 + 7.0) {
                        this.key(kx.key);
                        return true;
                    }
                }

                return false;
            }
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (!this.message.isFocused() || key != 257 && key != 335) {
            if (this.message.isFocused() || this.id.isFocused()) {
                return super.keyPressed(key, scan, modifiers);
            } else if (key >= 48 && key <= 57) {
                this.key(key - 48 + "");
                return true;
            } else if (key >= 320 && key <= 329) {
                this.key(key - 320 + "");
                return true;
            } else if (key == 257 || key == 335) {
                this.button("call");
                return true;
            } else if (key == 262) {
                this.button("building");
                return true;
            } else if (key == 259) {
                this.key("*");
                return true;
            } else if (key == 72) {
                this.button("handset");
                return true;
            } else {
                return super.keyPressed(key, scan, modifiers);
            }
        } else {
            this.send(ReceiverBlockEntity.Action.SEND_MESSAGE, this.message.getValue());
            this.message.setValue("");
            return true;
        }
    }

    @Override
    public boolean charTyped(char value, int modifiers) {
        if (this.message.isFocused() || this.id.isFocused() || value != '*' && value != '#') {
            return super.charTyped(value, modifiers);
        } else {
            this.key(value + "");
            return true;
        }
    }

    private record Key(String key, int x, int y, int w, int h) {
    }
}
