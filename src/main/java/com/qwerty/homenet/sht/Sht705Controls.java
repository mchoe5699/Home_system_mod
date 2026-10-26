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

final class Sht705Controls extends Screen {
    WallpadDataPacket data;
    final BlockPos pos;
    private EditBox input;
    private EditBox message;
    private float scale;
    private int left;
    private int top;
    private long erasePressedAt = -1L;
    private final KvmCameraView legacy = new KvmCameraView();

    private static void roundLamp(GuiGraphics g, int cx, int cy, int color) {
        for (int dy = -3; dy <= 3; dy++) {
            int half = (int)Math.floor(Math.sqrt(10 - dy * dy));
            g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }

    Sht705Controls(WallpadDataPacket data) {
        super(Component.literal("SAMSUNG SHT-705"));
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
        this.scale = Math.max(0.3F, Math.min((usable - 24) / 480.0F, (this.height - 222) / 354.0F));
        this.left = (int)((usable - 480.0F * this.scale) / 2.0F);
        this.top = 42;
        this.addRenderableWidget(Button.builder(Component.literal("덮개 열기/닫기"), b -> this.send("press", "cover")).bounds(12, this.height - 147, 113, 18).build());
        this.addRenderableWidget(
            Button.builder(Component.literal("착신음 선택"), b -> this.send("press", "ringtone")).bounds(132, this.height - 147, 100, 18).build()
        );
        this.addRenderableWidget(
            Button.builder(Component.literal("가스밸브 닫기"), b -> this.send("press", "gas_close")).bounds(239, this.height - 147, 112, 18).build()
        );
        this.input = new EditBox(this.font, 12, this.height - 108, this.width - 154, 18, Component.literal("동·호 또는 경비실 번호"));
        this.input.setMaxLength(32);
        this.addRenderableWidget(this.input);
        this.addRenderableWidget(
            Button.builder(Component.literal("지정경비실 입력"), b -> this.send("guard_no", this.input.getValue()))
                .bounds(this.width - 136, this.height - 108, 124, 18)
                .build()
        );
        this.message = new EditBox(this.font, 12, this.height - 78, this.width - 94, 18, Component.literal("통화 메시지"));
        this.message.setMaxLength(80);
        this.addRenderableWidget(this.message);
        this.addRenderableWidget(Button.builder(Component.literal("전송"), b -> {
            this.send("chat", this.message.getValue());
            this.message.setValue("");
        }).bounds(this.width - 76, this.height - 78, 64, 18).build());
        this.addRenderableWidget(
            Button.builder(Component.literal("동·호 설정"), b -> this.send("unit", this.input.getValue()))
                .bounds(this.width - 136, this.height - 52, 124, 18)
                .build()
        );
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        this.renderBackground(g);
        g.drawString(this.font, this.title, 12, 8, -723728);
        g.drawString(this.font, "BUS · " + this.data.unit() + " · " + this.data.setting("status", "대기"), 12, 24, -2301726);
        g.pose().pushPose();
        g.pose().translate((float)this.left, (float)this.top, 0.0F);
        g.pose().scale(this.scale, this.scale, 1.0F);
        g.blit(Product.id("textures/block/sht_705_face.png"), 0, 0, 0.0F, 0.0F, 480, 354, 480, 354);
        g.blit(Product.id("textures/block/sht_705_panel.png"), 0, 0, 0.0F, 0.0F, 480, 354, 480, 354);
        if (!this.data.flag("cover")) {
            g.blit(Product.id("textures/block/sht_705_cover.png"), 0, 0, 0.0F, 0.0F, 480, 354, 480, 354);
        }

        int front = Integer.parseInt(this.data.setting("front_lamps", "0"));
        int[] bits = new int[]{1, 2, 2, 4, 8};

        for (int i = 0; i < Sht705Visual.FRONT_X.length; i++) {
            if ((front & bits[i]) != 0) {
                g.pose().pushPose();
                g.pose().translate(Sht705Visual.mx(Sht705Visual.FRONT_X[i]), 221.0F, 0.0F);
                float ringScale = i == 4 ? 1.2631578F : 1.0526316F;
                g.pose().scale(ringScale, ringScale, 1.0F);
                g.blit(Product.id("textures/block/sht_705_led_ring.png"), -13, -13, 0.0F, 0.0F, 26, 26, 26, 26);
                g.pose().popPose();
            }
        }

        int lights = Integer.parseInt(this.data.setting("lights", "0"));
        int[] status = new int[]{256, 512, 1024, 1};
        int[] colors = new int[]{-1752001, -1721539, -10109846, -1752001};

        for (int ix = 0; ix < 4; ix++) {
            if ((lights & status[ix]) != 0) {
                roundLamp(g, Math.round(Sht705Visual.mx(405.0F)), 78 + ix * 22, colors[ix]);
            }
        }

        int inner = Integer.parseInt(this.data.setting("inner_lamps", "0"));

        for (int ixx = 0; ixx < 7; ixx++) {
            if ((inner & 1 << ixx) != 0) {
                roundLamp(g, Math.round(Sht705Visual.mx(224 + ixx * 24)), 306, ixx == 6 ? -1683383 : -9064734);
            }
        }

        ClientLevel world = Minecraft.getInstance().level;
        Station705.Unit unit = world != null && world.getBlockEntity(this.pos) instanceof Station705.Unit current ? current : null;
        int[] saved = unit == null ? null : unit.recordedFrame();
        int vx = Math.round(Sht705Visual.mx(205.0F));
        if (saved != null) {
            Sht104Recording.gui(g, saved, vx, 67, 154, 116, true);
        } else if (this.data.flag("video")) {
            BlockPos at = new BlockPos(
                Integer.parseInt(this.data.setting("vx", "0")), Integer.parseInt(this.data.setting("vy", "0")), Integer.parseInt(this.data.setting("vz", "0"))
            );
            if (SamsungVideoView.isColorSource(world, at)) {
                int[] frame = SamsungVideoView.frame(at);
                if (frame != null) {
                    for (int y = 0; y < 36; y++) {
                        for (int x = 0; x < 48; x++) {
                            g.fill(vx + x * 154 / 48, 67 + y * 116 / 36, vx + (x + 1) * 154 / 48 + 1, 67 + (y + 1) * 116 / 36 + 1, frame[y * 48 + x]);
                        }
                    }
                }
            } else {
                this.legacy
                    .draw(
                        g,
                        at,
                        vx,
                        67,
                        154,
                        116,
                        Integer.parseInt(this.data.setting("brightness", "2")),
                        Integer.parseInt(this.data.setting("contrast", "2")),
                        VideoDisplays.sourceFormat(world, at, VideoFormat.COLOR)
                    );
            }
        }

        g.pose().popPose();
        g.drawString(
            this.font, "녹화 " + this.data.setting("recordings", "0") + "건 · 착신음 " + this.data.setting("ringtone", "1") + "/21", 12, this.height - 121, -2301726
        );
        IntercomConversation.draw(g, this.font, this.data, this.width, this.height - 160);
        super.render(g, mouseX, mouseY, partial);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (super.mouseClicked(x, y, button)) {
            return true;
        } else if (button != 0 && button != 1) {
            return false;
        } else {
            double cx = (x - this.left) / this.scale;
            double original = cx < 80.0
                ? cx * 94.0 / 80.0
                : (
                    cx < 163.0
                        ? 94.0 + (cx - 80.0) * 109.0 / 83.0
                        : (cx <= 327.0 ? cx + 40.0 : (cx < 400.0 ? 367.0 + (cx - 327.0) * 99.0 / 73.0 : 466.0 + (cx - 400.0) * 94.0 / 80.0))
                );
            String action = Station705.keyAt(original, (y - this.top) / this.scale, this.data.flag("cover"));
            if (action == null) {
                return false;
            } else if (action.equals("meter")) {
                if (button == 1) {
                    this.send("press", "erase_all");
                } else {
                    this.erasePressedAt = System.currentTimeMillis();
                }

                return true;
            } else if (button == 0) {
                this.send("press", action);
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (button == 0 && this.erasePressedAt >= 0L) {
            this.send("press", System.currentTimeMillis() - this.erasePressedAt >= 2000L ? "erase_all" : "meter");
            this.erasePressedAt = -1L;
            return true;
        } else {
            return super.mouseReleased(x, y, button);
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (!this.message.isFocused() || key != 257 && key != 335) {
            return super.keyPressed(key, scan, modifiers);
        } else {
            this.send("chat", this.message.getValue());
            this.message.setValue("");
            return true;
        }
    }
}
