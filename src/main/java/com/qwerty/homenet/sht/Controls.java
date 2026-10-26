package com.qwerty.homenet.sht;

import com.qwerty.homenet.client.IntercomConversation;
import com.qwerty.homenet.client.kvm.KvmCameraView;
import com.qwerty.homenet.network.WallpadDataPacket;
import com.qwerty.homenet.video.VideoFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class Controls extends Screen {
    static final int BASE_X = 74;
    static final int BASE_Y = 54;
    static final int BASE_W = 896;
    static final int BASE_H = 653;
    static final int BASE_ATLAS_H = 720;
    public WallpadDataPacket data;
    boolean radio;
    EditBox text;
    EditBox setting;
    float scale;
    int x;
    int y;
    boolean ignoreT;
    final KvmCameraView video = new KvmCameraView();

    boolean wired() {
        return this.data.flag("wired");
    }

    boolean model101N() {
        return this.data.setting("model", "").equals("SHT-101N");
    }

    ResourceLocation texture(String path) {
        return Product.id((this.wired() ? "sht102/" : "") + path);
    }

    Controls(WallpadDataPacket d, boolean r) {
        this(
            d,
            r,
            Component.literal(
                r
                    ? (d.setting("model", "").equals("SHT-101N") ? "SHT-101N 휴대장치" : (d.flag("wired") ? "SHT-102C 휴대장치" : "SHT-101C 무선수화기"))
                    : (d.setting("model", "").equals("SHT-101N") ? "SAMSUNG SHT-101N" : (d.flag("wired") ? "SAMSUNG SHT-102C" : "SAMSUNG SHT-101C"))
            )
        );
    }

    Controls(WallpadDataPacket d, boolean r, Component label) {
        super(label);
        this.data = d;
        this.radio = r;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    void send(String a, String v) {
        Wire.send(this.data.pos(), this.radio, a, v);
    }

    @Override
    protected void init() {
        int usable = this.width - IntercomConversation.width(this.width) - 30;
        this.scale = Math.min(usable / (this.radio ? 240.0F : 896.0F), (this.height - 155) / (this.radio ? 950.0F : 653.0F));
        this.x = (int)((usable - (this.radio ? 240 : 896) * this.scale) / 2.0F);
        this.y = 52;
        if (!this.radio) {
            this.addRenderableWidget(
                Button.builder(Component.literal("지정경비실 입력"), b -> this.send("guard_no", this.setting.getValue()))
                    .bounds(12, this.height - 108, 126, 18)
                    .build()
            );
        }

        this.text = new EditBox(this.font, 12, this.height - 84, this.width - 94, 18, Component.literal("통화 메시지"));
        this.text.setMaxLength(80);
        this.text.setHint(Component.literal("T: 메시지 입력 · Enter: 전송"));
        this.addRenderableWidget(this.text);
        this.addRenderableWidget(Button.builder(Component.literal("전송"), b -> {
            this.send("chat", this.text.getValue());
            this.text.setValue("");
        }).bounds(this.width - 76, this.height - 84, 64, 18).build());
        if (!this.radio) {
            String[] labels = this.wired()
                ? new String[]{"유선 수화기 들기 / 걸기", "BUS / STAR", this.model101N() ? "재발신" : "안테나 접기 / 펴기", "휴대장치 받기"}
                : new String[]{"수화기 들기 / 걸기", "BUS / STAR", "안테나 접기 / 펴기"};
            String[] actions = this.wired()
                ? new String[]{"handset", "mode", this.model101N() ? "redial" : "antenna", "portable"}
                : new String[]{"handset", "mode", "antenna"};

            for (int i = 0; i < labels.length; i++) {
                String a = actions[i];
                this.addRenderableWidget(
                    Button.builder(Component.literal(labels[i]), b -> this.send("press", a))
                        .bounds(12 + i * (this.width - 24) / labels.length, this.height - 61, (this.width - 24) / labels.length - 3, 18)
                        .build()
                );
            }

            this.setting = new EditBox(this.font, 12, this.height - 38, this.width - 235, 18, Component.literal("설정값"));
            this.setting.setMaxLength(32);
            this.addRenderableWidget(this.setting);
            this.addRenderableWidget(
                Button.builder(Component.literal("로고"), b -> this.send("logo", this.setting.getValue()))
                    .bounds(this.width - 213, this.height - 38, 80, 18)
                    .build()
            );
            this.addRenderableWidget(
                Button.builder(Component.literal("동·호 설정"), b -> this.send("unit", this.setting.getValue()))
                    .bounds(this.width - 128, this.height - 38, 116, 18)
                    .build()
            );
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float dt) {
        Surface.select(this.wired());
        this.renderBackground(g);
        g.drawString(this.font, this.title, 12, 8, -1118482);
        g.drawString(this.font, this.data.setting("mode", "BUS") + " · " + this.data.unit() + " · " + this.data.setting("status", "대기"), 12, 25, -1843272);
        g.pose().pushPose();
        g.pose().translate((float)this.x, (float)this.y, 0.0F);
        g.pose().scale(this.scale, this.scale, 1.0F);
        int ox = this.radio ? 0 : 74;
        int oy = this.radio ? 0 : 54;
        g.blit(
            this.texture(this.radio ? "textures/radio_gui.png" : (this.data.flag("docked") ? "textures/base_docked.png" : "textures/base_empty.png")),
            0,
            0,
            ox,
            oy,
            this.radio ? 240 : 896,
            this.radio ? 950 : 653,
            this.radio && this.wired() ? 240 : 1042,
            this.radio ? 950 : 720
        );
        if (!this.radio) {
            if (this.data.flag("video")) {
                BlockPos at = new BlockPos(
                    Integer.parseInt(this.data.setting("vx", "0")),
                    Integer.parseInt(this.data.setting("vy", "0")),
                    Integer.parseInt(this.data.setting("vz", "0"))
                );
                this.video
                    .draw(
                        g,
                        at,
                        578 - ox,
                        174 - oy,
                        192,
                        144,
                        Integer.parseInt(this.data.setting("brightness", "2")),
                        Integer.parseInt(this.data.setting("contrast", "2")),
                        VideoFormat.MONOCHROME
                    );
            }

            String logo = this.data.setting("product_logo_text", "SAMSUNG");
            if (!logo.equals("SAMSUNG")) {
                g.fill(306 - ox, 131 - oy, 449 - ox, 157 - oy, -921633);
                g.drawString(this.font, logo, 310 - ox, 137 - oy, -11313072, false);
            }

            int lights = Integer.parseInt(this.data.setting("lights", "0"));

            for (Indicators.Lamp lamp : Indicators.forModel(this.wired())) {
                if ((lights & lamp.bit()) != 0) {
                    int color = 0xFF000000 | lamp.rgb();
                    if (lamp.round()) {
                        for (int row = 0; row < lamp.h(); row++) {
                            double dy = (row + 0.5 - lamp.h() / 2.0) / (lamp.h() / 2.0);
                            int half = (int)Math.round(lamp.w() / 2.0 * Math.sqrt(Math.max(0.0, 1.0 - dy * dy)));
                            g.fill(
                                lamp.x() + lamp.w() / 2 - half - ox, lamp.y() + row - oy, lamp.x() + lamp.w() / 2 + half - ox, lamp.y() + row + 1 - oy, color
                            );
                        }

                        g.blit(this.texture("textures/base_ink.png"), lamp.x() - ox, lamp.y() - oy, lamp.x(), lamp.y(), lamp.w(), lamp.h(), 1042, 720);
                    } else {
                        g.fill(lamp.x() - ox, lamp.y() - oy, lamp.x() + lamp.w() - ox, lamp.y() + lamp.h() - oy, color);
                    }
                }
            }
        }

        if (!this.radio && this.model101N()) {
            g.fill(475 - ox, 54 - oy, 501 - ox, 84 - oy, -921633);
            g.fill(406 - ox, 581 - oy, 455 - ox, 602 - oy, -921633);
            g.drawString(this.font, "재발신", 409 - ox, 587 - oy, -13420486, false);
        }

        g.pose().popPose();
        IntercomConversation.draw(g, this.font, this.data, this.width, this.height - 100);
        super.render(g, mx, my, dt);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        if (super.mouseClicked(mx, my, b)) {
            return true;
        } else if (b != 0) {
            return false;
        } else {
            this.setFocused(null);
            double u = (mx - this.x) / this.scale + (this.radio ? 0 : 74);
            double v = (my - this.y) / this.scale + (this.radio ? 0 : 54);
            Surface.select(this.wired());

            for (Surface.Key k : Surface.buttons) {
                if (k.group().equals(this.radio ? "radio" : "base") && Math.abs(u - k.x()) <= k.w() / 2.0F + 2.0F && Math.abs(v - k.y()) <= k.h() / 2.0F + 3.0F
                    )
                 {
                    if ((k.name().startsWith("n") || k.name().startsWith("r")) && k.name().length() == 2) {
                        this.send("digit", k.name().substring(1));
                    } else {
                        this.send("press", this.model101N() && k.name().equals("intercom") ? "redial" : k.name());
                    }

                    return true;
                }
            }

            if (!this.radio && u >= 145.0 && u <= 296.0 && v >= 126.0 && v <= 640.0) {
                this.send("press", "handset");
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (!this.text.isFocused() || key != 257 && key != 335) {
            if (!this.text.isFocused() && (this.setting == null || !this.setting.isFocused()) && (key == 84 || key == 257)) {
                this.setFocused(this.text);
                this.text.setFocused(true);
                this.ignoreT = key == 84;
                return true;
            } else {
                return super.keyPressed(key, scan, mods);
            }
        } else {
            this.send("chat", this.text.getValue());
            this.text.setValue("");
            return true;
        }
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (this.ignoreT) {
            this.ignoreT = false;
            if (c == 't' || c == 'T') {
                return true;
            }
        }

        if (!this.text.isFocused() && (this.setting == null || !this.setting.isFocused()) && "123456789*0#".indexOf(c) >= 0) {
            this.send("digit", c + "");
            return true;
        } else {
            return super.charTyped(c, mods);
        }
    }
}
