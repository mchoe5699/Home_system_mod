package com.qwerty.homenet.sht;

import com.qwerty.homenet.client.IntercomConversation;
import com.qwerty.homenet.client.kvm.KvmCameraView;
import com.qwerty.homenet.network.WallpadDataPacket;
import com.qwerty.homenet.video.VideoFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

final class Sht104MControls extends Screen {
    WallpadDataPacket data;
    final BlockPos pos;
    private EditBox input;
    private EditBox message;
    private float scale;
    private int left;
    private int top;
    private final KvmCameraView video = new KvmCameraView();

    Sht104MControls(WallpadDataPacket data) {
        super(Component.literal("SAMSUNG " + data.setting("model", "SHT-104M")));
        this.data = data;
        this.pos = data.pos();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void send(String action, String value) {
        Wire.send(this.pos, false, action, value);
    }

    @Override
    protected void init() {
        int usable = this.width - IntercomConversation.width(this.width) - 24;
        this.scale = Math.min((usable - 24) / 592.0F, (this.height - 160) / 405.0F);
        this.left = (int)((usable - 592.0F * this.scale) / 2.0F);
        this.top = 44;
        if (this.data.setting("model", "").equals("SHT-104CS")) {
            this.addRenderableWidget(
                Button.builder(Component.literal("등록 카드로 경비 설정/해제"), b -> this.send("press", "s1_arm")).bounds(usable - 181, 7, 177, 18).build()
            );
        }

        this.input = new EditBox(this.font, 12, this.height - 38, this.width - 154, 18, Component.literal("설정값"));
        this.input.setMaxLength(32);
        this.addRenderableWidget(this.input);
        this.addRenderableWidget(
            Button.builder(Component.literal("지정경비실 입력"), b -> this.send("guard_no", this.input.getValue()))
                .bounds(this.width - 136, this.height - 38, 124, 18)
                .build()
        );
        this.message = new EditBox(this.font, 12, this.height - 83, this.width - 94, 18, Component.literal("통화 메시지"));
        this.message.setMaxLength(80);
        this.addRenderableWidget(this.message);
        this.addRenderableWidget(Button.builder(Component.literal("전송"), b -> {
            this.send("chat", this.message.getValue());
            this.message.setValue("");
        }).bounds(this.width - 76, this.height - 83, 64, 18).build());
        this.addRenderableWidget(
            Button.builder(Component.literal("수화기 들기 / 걸기"), b -> this.send("press", "handset")).bounds(12, this.height - 60, 175, 18).build()
        );
        this.addRenderableWidget(
            Button.builder(Component.literal("동·호 설정"), b -> this.send("unit", this.input.getValue())).bounds(193, this.height - 60, 115, 18).build()
        );
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float dt) {
        this.renderBackground(g);
        g.drawString(this.font, this.title, 12, 9, -921885);
        g.drawString(this.font, this.data.setting("mode", "BUS") + " · " + this.data.unit() + " · " + this.data.setting("status", "대기"), 12, 25, -2238782);
        if (this.data.setting("model", "").equals("SHT-104CS")) {
            g.drawString(
                this.font,
                "에스원 "
                    + (this.data.flag("s1_enroll") ? "카드 등록 대기 (30초)" : (this.data.flag("s1_armed") ? "경비 설정" : "경비 해제"))
                    + " · 등록 카드 "
                    + this.data.setting("s1_cards", "0")
                    + "장",
                12,
                38,
                -2497300
            );
        }

        boolean s1 = this.data.setting("model", "").equals("SHT-104CS");
        boolean color = s1 || this.data.setting("model", "SHT-104M").equals("SHT-104C");
        String variant = this.data.setting("variant", "C");
        g.pose().pushPose();
        g.pose().translate((float)this.left, (float)this.top, 0.0F);
        g.pose().scale(this.scale, this.scale, 1.0F);
        if (variant.matches("[ABD]")) {
            g.fill(519, -72, 527, 8, -13618894);
            g.fill(518, -76, 528, -64, -12500414);
        }

        g.blit(
            Product.id("textures/block/sht_104" + (s1 ? "cs" : (color ? "c" : "m")) + (this.data.flag("docked") ? "_gui.png" : "_gui_empty.png")),
            0,
            0,
            0.0F,
            0.0F,
            592,
            405,
            592,
            405
        );
        if (variant.matches("[ABD]")) {
            g.blit(Product.id("textures/block/sht_104m_type_" + variant.toLowerCase() + ".png"), 0, 0, 0.0F, 0.0F, 592, 405, 592, 405);
        }

        if (variant.matches("[ABD]")) {
            this.drawPartLabels(g, variant);
        }

        ClientLevel world = Minecraft.getInstance().level;
        Station104M.Unit station = world != null && world.getBlockEntity(this.pos) instanceof Station104M.Unit u ? u : null;
        int[] recorded = station == null ? null : station.playbackFrame();
        if (recorded != null) {
            Sht104Recording.gui(g, recorded, 183, 79, 103, 74, color);
        } else if (this.data.flag("video")) {
            BlockPos at = new BlockPos(
                Integer.parseInt(this.data.setting("vx", "0")), Integer.parseInt(this.data.setting("vy", "0")), Integer.parseInt(this.data.setting("vz", "0"))
            );
            if (color && SamsungVideoView.isColorLobby(world, at)) {
                SamsungVideoView.gui(g, at);
            } else {
                this.video
                    .draw(
                        g,
                        at,
                        183,
                        79,
                        103,
                        74,
                        Integer.parseInt(this.data.setting("brightness", "2")),
                        Integer.parseInt(this.data.setting("contrast", "2")),
                        VideoDisplays.sourceFormat(world, at, color ? VideoFormat.COLOR : VideoFormat.MONOCHROME)
                    );
            }
        }

        g.pose().popPose();
        IntercomConversation.draw(g, this.font, this.data, this.width, this.height - 100);
        super.render(g, mx, my, dt);
    }

    private void drawPartLabels(GuiGraphics g, String variant) {
        if (!variant.equals("A") && !variant.equals("D")) {
            if (variant.equals("B")) {
                this.partLabel(g, "무선전화", 488, 194);
                this.partLabel(g, "내선", 488, 258);
            }
        } else {
            String[] names = variant.equals("A") ? new String[]{"재생", "녹화", "지움", "시계"} : new String[]{"방송", "청취", "중단", "YES"};
            int[] xs = new int[]{471, 492, 513, 534};

            for (int index = 0; index < 4; index++) {
                this.partLabel(g, names[index], xs[index], 176);
            }

            this.partLabel(g, "무선전화", 488, 258);
            this.partLabel(g, "내선", 488, 281);
        }
    }

    private void partLabel(GuiGraphics g, String value, int center, int y) {
        g.drawString(this.font, value, center - this.font.width(value) / 2, y, -14341592, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) {
            return true;
        } else if (button != 0) {
            return false;
        } else {
            double x = (mx - this.left) / this.scale;
            double y = (my - this.top) / this.scale;
            if (x >= 44.0 && x < 122.0 && y >= 44.0 && y < 359.0) {
                this.send("press", "handset");
                return true;
            } else if (y >= 376.0 && y <= 390.0 && x >= 257.0 && x <= 270.0) {
                this.send("press", "contrast");
                return true;
            } else if (y >= 376.0 && y <= 390.0 && x >= 285.0 && x <= 298.0) {
                this.send("press", "brightness");
                return true;
            } else {
                if (x >= 176.0 && x < 291.0 && y >= 267.0 && y < 354.0) {
                    int col = (int)((x - 176.0) / 39.0);
                    int row = (int)((y - 267.0) / 22.6);
                    if (col < 3 && row < 4) {
                        this.send("digit", String.valueOf("123456789*0#".charAt(row * 3 + col)));
                        return true;
                    }
                }

                if (y >= 237.0 && y < 260.0 && x >= 172.0 && x < 298.0) {
                    this.send("press", x < 207.0 ? "redial" : (x < 261.0 ? "call" : "function"));
                    return true;
                } else if (x >= 310.0 && x < 332.0 && y >= 259.0 && y < 289.0) {
                    this.send("press", y < 274.0 ? "up" : "down");
                    return true;
                } else if (x >= 307.0 && x < 340.0 && y >= 292.0 && y < 356.0) {
                    this.send("press", y < 327.0 ? "guard" : "front");
                    return true;
                } else {
                    String variant = this.data.setting("variant", "C");
                    if (x >= 459.0 && x < 545.0 && y >= 180.0 && y < 299.0 && variant.matches("[ABD]")) {
                        if (y < 219.0 && variant.equals("A")) {
                            int index = Math.min(3, Math.max(0, (int)((x - 460.0) / 21.0)));
                            this.send("press", new String[]{"playback", "record", "erase", "clock"}[index]);
                            return true;
                        }

                        if (y < 219.0 && variant.equals("D")) {
                            int index = Math.min(3, Math.max(0, (int)((x - 460.0) / 21.0)));
                            this.send("press", new String[]{"broadcast", "listen", "broadcast_stop", "yes"}[index]);
                            return true;
                        }

                        if (variant.equals("B")) {
                            if (x >= 521.0 && y < 219.0) {
                                this.send("press", "wireless");
                                return true;
                            }

                            if (x >= 521.0 && y >= 248.0 && y < 279.0) {
                                this.send("press", "intercom");
                                return true;
                            }
                        } else if (x >= 521.0 && y >= 248.0 && y < 299.0) {
                            this.send("press", y < 276.0 ? "wireless" : "intercom");
                            return true;
                        }
                    }

                    if (x >= 419.0 && x < 444.0) {
                        boolean cs = this.data.setting("model", "").equals("SHT-104CS");
                        String action = y >= 193.0 && y < 211.0
                            ? "test"
                            : (
                                y >= 254.0 && y < 277.0
                                    ? (cs ? "s1_away" : "away")
                                    : (
                                        y >= 279.0 && y < 299.0
                                            ? (cs ? "s1_home" : "security")
                                            : (
                                                y >= 302.0 && y < 322.0
                                                    ? (cs ? "s1_disarm" : "restore")
                                                    : (y >= 330.0 && y < 357.0 ? (cs ? "restore" : "emergency") : null)
                                            )
                                    )
                            );
                        if (action != null) {
                            this.send("press", action);
                            return true;
                        }
                    }

                    return false;
                }
            }
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (!this.message.isFocused() || key != 257 && key != 335) {
            if (!this.message.isFocused() && !this.input.isFocused() && key == 84) {
                this.setFocused(this.message);
                this.message.setFocused(true);
                return true;
            } else {
                return super.keyPressed(key, scan, mods);
            }
        } else {
            this.send("chat", this.message.getValue());
            this.message.setValue("");
            return true;
        }
    }
}
