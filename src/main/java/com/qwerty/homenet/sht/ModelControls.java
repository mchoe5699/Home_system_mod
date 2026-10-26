package com.qwerty.homenet.sht;

import com.qwerty.homenet.client.IntercomConversation;
import com.qwerty.homenet.client.kvm.KvmCameraView;
import com.qwerty.homenet.network.WallpadDataPacket;
import com.qwerty.homenet.video.VideoFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

final class ModelControls extends Screen {
    WallpadDataPacket data;
    private final boolean videoPhone;
    private final KvmCameraView video = new KvmCameraView();
    private EditBox message;
    private EditBox unit;
    private float scale;
    private int left;
    private int top;

    ModelControls(WallpadDataPacket d) {
        super(Component.literal("SAMSUNG " + d.setting("model", "인터폰")));
        this.data = d;
        this.videoPhone = d.setting("model", "").equals("SHT-308")
            || d.setting("model", "").equals("SHT-305V")
            || d.setting("model", "").equals("SHT-305")
            || d.setting("model", "").equals("SHT-307");
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void send(String action, String value) {
        Wire.send(this.data.pos(), false, action, value);
    }

    @Override
    protected void init() {
        int usable = this.width - IntercomConversation.width(this.width) - 24;
        int modelWidth = this.videoPhone ? 420 : 216;
        this.scale = Math.min(1.5F, Math.min((usable - 30.0F) / modelWidth, (this.height - 175.0F) / 400.0F));
        this.scale = Math.max(0.25F, this.scale);
        this.left = (int)((usable - modelWidth * this.scale) / 2.0F);
        this.top = 49;
        this.addRenderableWidget(
            Button.builder(Component.literal("지정경비실 입력"), b -> this.send("guard_no", this.unit.getValue())).bounds(12, this.height - 106, 126, 18).build()
        );
        this.message = new EditBox(this.font, 12, this.height - 82, this.width - 94, 18, Component.literal("통화 메시지"));
        this.message.setMaxLength(80);
        this.message.setHint(Component.literal("통화 메시지"));
        this.addRenderableWidget(this.message);
        this.addRenderableWidget(Button.builder(Component.literal("전송"), b -> {
            this.send("chat", this.message.getValue());
            this.message.setValue("");
        }).bounds(this.width - 76, this.height - 82, 64, 18).build());
        this.addRenderableWidget(
            Button.builder(Component.literal("수화기 들기 / 내려놓기"), b -> this.send("press", "handset")).bounds(12, this.height - 57, 190, 18).build()
        );
        this.unit = new EditBox(this.font, 12, this.height - 34, this.width - 160, 18, Component.literal("동·호"));
        this.unit.setMaxLength(32);
        this.addRenderableWidget(this.unit);
        this.addRenderableWidget(
            Button.builder(Component.literal("동·호 설정"), b -> this.send("unit", this.unit.getValue()))
                .bounds(this.width - 138, this.height - 34, 126, 18)
                .build()
        );
    }

    private void rect(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        g.fill(x0, y0, x1, y1, color);
    }

    private void centered(GuiGraphics g, String value, int x, int y, int color) {
        g.drawString(this.font, value, x - this.font.width(value) / 2, y, color, false);
    }

    private void disk(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int half = (int)Math.sqrt(r * r - dy * dy);
            this.rect(g, cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }

    private void rounded(GuiGraphics g, int x0, int y0, int x1, int y1, int radius, int color) {
        for (int y = y0; y < y1; y++) {
            int inset = 0;
            int d = Math.min(y - y0, y1 - 1 - y);
            if (d < radius) {
                inset = radius - (int)Math.sqrt(radius * radius - (radius - 1 - d) * (radius - 1 - d));
            }

            this.rect(g, x0 + inset, y, x1 - inset, y + 1, color);
        }
    }

    private void draw308(GuiGraphics g) {
        this.rounded(g, 0, 13, 118, 387, 10, -2699081);
        this.rounded(g, 119, 0, 420, 400, 15, -2567751);
        this.rounded(g, 123, 4, 416, 396, 11, -1119790);
        this.rect(g, 338, 179, 341, 386, -3752281);
        this.rounded(g, 342, 181, 415, 396, 9, -2238527);
        if (this.data.flag("docked")) {
            this.rounded(g, 9, 8, 100, 388, 23, -988459);
        } else {
            this.rounded(g, 12, 20, 98, 104, 8, -2764870);
            this.rounded(g, 22, 28, 87, 96, 5, -1909563);
            this.rounded(g, 18, 112, 98, 253, 7, -2041147);

            for (int i = 0; i < 11; i++) {
                this.rect(g, 37, 126 + i * 11, 80, 128 + i * 11, -11183790);
            }

            this.rounded(g, 19, 269, 100, 386, 8, -3422543);
            this.rounded(g, 28, 279, 91, 378, 5, -2041145);
        }

        this.rounded(g, 138, 22, 408, 177, 6, -15724267);
        this.rounded(g, 152, 37, 313, 163, 5, -14408406);
        this.rounded(g, 162, 45, 305, 153, 3, -11447714);
        if (this.data.flag("video")) {
            BlockPos at = new BlockPos(
                Integer.parseInt(this.data.setting("vx", "0")), Integer.parseInt(this.data.setting("vy", "0")), Integer.parseInt(this.data.setting("vz", "0"))
            );
            this.video
                .draw(
                    g,
                    at,
                    162,
                    45,
                    143,
                    108,
                    Integer.parseInt(this.data.setting("brightness", "2")),
                    Integer.parseInt(this.data.setting("contrast", "2")),
                    VideoFormat.MONOCHROME
                );
        }

        for (int i = 0; i < 18; i++) {
            this.rect(g, 343, 42 + i * 6, 393, 44 + i * 6, -12499648);
        }

        boolean star307 = this.data.setting("model", "").equals("SHT-307");
        this.centered(g, star307 ? "EZcom SHT-307" : "SAMSUNG", 168, 193, -13354688);
        this.centered(g, star307 ? "SAMSUNG" : "iTAN  SHT-308", 177, 374, -13617084);
        if (!star307) {
            this.centered(g, "VIDEO DOOR PHONE", 190, 387, -13617084);
        }

        int lights = Integer.parseInt(this.data.setting("lights", "0"));
        String[] names = this.data.setting("model", "").equals("SHT-307") ? new String[]{"전원", "경비실", "현관", ""} : new String[]{"비상", "경비실", "외출", "현관"};
        int[] ys = new int[]{225, 263, 301, 346};

        for (int i = 0; i < 4; i++) {
            this.centered(g, names[i], 385, ys[i] - 25, -13420477);
            int bit = i == 0 ? 16 : (i == 1 ? 64 : 8);
            boolean lit = i < 3 && (lights & bit) != 0;
            if (i == 0 && star307) {
                lit = (lights & 32) != 0;
            }

            this.disk(g, 385, ys[i], i == 3 ? 15 : 12, i == 3 ? -725285 : (lit ? -1224384 : -11839897));
        }
    }

    private void draw305(GuiGraphics g) {
        this.rounded(g, 0, 13, 118, 387, 10, -2699081);
        this.rounded(g, 119, 0, 420, 400, 15, -2567751);
        this.rounded(g, 123, 4, 416, 396, 11, -1119790);
        this.rect(g, 345, 179, 348, 388, -3752281);
        this.rounded(g, 349, 181, 415, 396, 9, -2238527);
        if (this.data.flag("docked")) {
            this.rounded(g, 9, 8, 100, 388, 23, -988459);
        } else {
            this.rounded(g, 12, 20, 98, 104, 8, -2764870);
            this.rounded(g, 22, 28, 87, 96, 5, -1909563);
            this.rounded(g, 18, 112, 98, 253, 7, -2041147);

            for (int i = 0; i < 11; i++) {
                this.rect(g, 37, 126 + i * 11, 80, 128 + i * 11, -11183790);
            }

            this.rounded(g, 19, 269, 100, 386, 8, -3422543);
            this.rounded(g, 28, 279, 91, 378, 5, -2041145);
        }

        this.rounded(g, 138, 22, 408, 177, 6, -15724267);
        this.rounded(g, 152, 37, 313, 163, 5, -14408406);
        this.rounded(g, 162, 45, 305, 153, 3, -11447714);
        if (this.data.flag("video")) {
            BlockPos at = new BlockPos(
                Integer.parseInt(this.data.setting("vx", "0")), Integer.parseInt(this.data.setting("vy", "0")), Integer.parseInt(this.data.setting("vz", "0"))
            );
            this.video
                .draw(
                    g,
                    at,
                    162,
                    45,
                    143,
                    108,
                    Integer.parseInt(this.data.setting("brightness", "2")),
                    Integer.parseInt(this.data.setting("contrast", "2")),
                    VideoFormat.MONOCHROME
                );
        }

        int lights = Integer.parseInt(this.data.setting("lights", "0"));
        String alarm = this.data.setting("alarm", "");
        String[] status = new String[]{"화재", "가스", "방범", "비상"};
        String[] alarmIds = new String[]{"fire", "gas", "security", "emergency"};
        int[] colors = new int[]{-5352100, -6449071, -12217440, -5263283};
        this.centered(g, "전원", 361, 28, -4868947);
        this.centered(g, "사용중", 388, 28, -4868947);

        for (int i = 0; i < 4; i++) {
            if (i != 0 || !this.data.setting("model", "").equals("SHT-305")) {
                int y = 36 + i * 33;
                this.rounded(g, 347, y, 401, y + 30, 3, -10132371);
                this.rounded(g, 350, y + 2, 398, y + 28, 2, -15855595);
                this.draw305StatusIcon(g, i, 358, y + 15, alarm.equals(alarmIds[i]) ? -1096402 : colors[i]);
                this.centered(g, status[i].substring(0, 1), 388, y + 6, -3881274);
                this.centered(g, status[i].substring(1), 388, y + 16, -3881274);
            }
        }

        this.centered(g, "SAMSUNG", 168, 193, -13354688);
        this.centered(g, "iTAN  " + (this.data.setting("model", "").equals("SHT-305") ? "SHT-305" : "SHT-305V"), 180, 374, -13617084);
        this.centered(g, "VIDEO DOOR PHONE", 190, 387, -13617084);
        String[] main = new String[]{"경비실", "외출", "현관", "문열림"};
        int[] mainY = new int[]{226, 263, 300, 347};

        for (int ix = 0; ix < 4; ix++) {
            this.centered(g, main[ix], 310, mainY[ix] - 24, -13420477);
            int bit = ix == 0 ? 64 : (ix == 1 ? 8 : 32);
            this.disk(g, 310, mainY[ix], ix == 3 ? 15 : 12, ix == 3 ? -725285 : ((lights & bit) != 0 ? -1224384 : -11839897));
        }

        String[] side = new String[]{"비상", "시험", this.data.setting("model", "").equals("SHT-305") ? "가스" : "가스/화재", "방범설정", "복귀"};

        for (int ix = 0; ix < side.length; ix++) {
            int y = 201 + ix * 26;
            this.centered(g, side[ix], 387, y - 13, -13420477);

            boolean lit = switch (ix) {
                case 0 -> alarm.equals("emergency");
                case 1 -> this.data.flag("self_test");
                case 2 -> alarm.equals("fire") || alarm.equals("gas");
                case 3 -> (lights & 4) != 0;
                default -> false;
            };
            this.disk(g, 367, y, 2, lit ? -1098714 : -12302521);
            this.rounded(g, 377, y - 3, 398, y + 4, 3, -725542);
        }

        this.centered(g, "경보정지", 387, 333, -13420477);
        this.disk(g, 387, 350, 15, -725285);
    }

    private void draw305StatusIcon(GuiGraphics g, int kind, int x, int y, int color) {
        switch (kind) {
            case 0:
                this.disk(g, x, y + 4, 5, color);
                this.rect(g, x - 4, y - 5, x - 1, y + 4, color);
                this.rect(g, x, y - 9, x + 3, y + 4, color);
                this.rect(g, x + 4, y - 3, x + 7, y + 4, color);
                this.disk(g, x, y + 5, 2, -15855595);
                break;
            case 1:
                this.rect(g, x - 8, y + 4, x + 8, y + 7, color);
                this.rect(g, x - 3, y - 5, x + 4, y + 5, color);
                this.rect(g, x - 7, y - 7, x + 8, y - 4, color);
                break;
            case 2:
                this.rect(g, x - 9, y - 9, x - 6, y + 9, color);
                this.disk(g, x + 3, y - 5, 3, color);
                this.rect(g, x, y - 1, x + 6, y + 6, color);
                this.rect(g, x - 2, y + 5, x + 2, y + 10, color);
                this.rect(g, x + 4, y + 5, x + 8, y + 10, color);
                break;
            default:
                this.rect(g, x - 2, y - 10, x + 2, y + 10, color);
                this.rect(g, x - 10, y - 2, x + 10, y + 2, color);
                this.rect(g, x - 7, y - 7, x - 4, y - 4, color);
                this.rect(g, x + 4, y - 7, x + 7, y - 4, color);
                this.rect(g, x - 7, y + 4, x - 4, y + 7, color);
                this.rect(g, x + 4, y + 4, x + 7, y + 7, color);
        }
    }

    private void drawHi101(GuiGraphics g) {
        this.rounded(g, 0, 0, 216, 400, 8, -3422539);
        this.rounded(g, 3, 3, 213, 397, 7, -1580595);
        this.rect(g, 104, 8, 107, 392, -3554644);
        this.rounded(g, 109, 5, 213, 397, 7, -790820);
        this.rounded(g, 115, 9, 209, 97, 6, -724769);
        this.rect(g, 116, 94, 209, 99, -2764869);
        this.rounded(g, 115, 102, 209, 198, 5, -659233);
        this.rect(g, 116, 193, 209, 200, -3291470);
        this.rounded(g, 115, 203, 209, 298, 5, -659233);
        this.rect(g, 116, 293, 209, 300, -3423056);
        this.rounded(g, 113, 304, 211, 394, 7, -1120043);
        if (this.data.flag("docked")) {
            this.rounded(g, 7, 11, 103, 391, 14, -659491);
        } else {
            this.rounded(g, 11, 18, 99, 105, 8, -3685970);
            this.rounded(g, 21, 29, 89, 97, 5, -1712181);
            this.rounded(g, 22, 111, 100, 288, 6, -3685454);

            for (int i = 0; i < 11; i++) {
                this.rect(g, 35, 128 + i * 13, 93, 131 + i * 13, -11446706);
            }

            this.rounded(g, 16, 302, 101, 390, 9, -3685970);
            this.rounded(g, 25, 310, 94, 382, 6, -1646646);
        }

        this.centered(g, "SAMSUNG", 165, 37, -13156793);
        this.centered(g, "경비실/문열림", 145, 322, -13354686);
        this.centered(g, "외출", 185, 322, -13354686);

        for (int x : new int[]{145, 185}) {
            this.disk(g, x, 350, 15, -5001828);
            this.disk(g, x, 350, 13, -461853);
            this.disk(g, x - 1, 348, 10, -1119784);
        }

        int lights = Integer.parseInt(this.data.setting("lights", "0"));
        this.disk(g, 185, 310, 3, (lights & 8) != 0 ? -1234654 : -13419970);
        this.centered(g, "iTAN", 137, 377, -13617084);
        this.centered(g, "HI-101", 164, 377, -13617084);
        this.centered(g, "INTER PHONE", 168, 389, -13617084);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float dt) {
        this.renderBackground(g);
        g.drawString(this.font, this.title, 12, 9, -986902);
        g.drawString(this.font, this.data.setting("mode", "BUS") + " · " + this.data.unit() + " · " + this.data.setting("status", "대기"), 12, 26, -1515065);
        g.pose().pushPose();
        g.pose().translate((float)this.left, (float)this.top, 0.0F);
        g.pose().scale(this.scale, this.scale, 1.0F);
        if (this.data.setting("model", "").equals("SHT-305V") || this.data.setting("model", "").equals("SHT-305")) {
            this.draw305(g);
        } else if (this.videoPhone) {
            this.draw308(g);
        } else {
            this.drawHi101(g);
        }

        g.pose().popPose();
        IntercomConversation.draw(g, this.font, this.data, this.width, this.height - 100);

        for (Renderable widget : this.renderables) {
            widget.render(g, mx, my, dt);
        }
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
            if (y < 0.0 || y > 400.0) {
                return false;
            } else {
                if (this.videoPhone) {
                    if (x >= 0.0 && x <= 115.0) {
                        this.send("press", "handset");
                        return true;
                    }

                    if (this.data.setting("model", "").equals("SHT-305V") || this.data.setting("model", "").equals("SHT-305")) {
                        if (x >= 287.0 && x <= 336.0 && y >= 205.0 && y <= 375.0) {
                            this.send("press", y < 245.0 ? "guard" : (y < 282.0 ? "away" : (y < 326.0 ? "front" : "open")));
                            return true;
                        }

                        if (x >= 357.0 && x <= 414.0 && y >= 185.0 && y <= 375.0) {
                            this.send(
                                "press",
                                y < 214.0
                                    ? "emergency"
                                    : (y < 240.0 ? "test" : (y < 266.0 ? "gas_fire" : (y < 292.0 ? "security" : (y < 329.0 ? "restore" : "stop"))))
                            );
                            return true;
                        }

                        return false;
                    }

                    if (x >= 353.0 && x <= 415.0) {
                        String action = this.data.setting("model", "").equals("SHT-307")
                            ? (y < 244.0 ? "" : (y < 282.0 ? "guard" : (y < 326.0 ? "front" : "")))
                            : (y < 244.0 ? "emergency" : (y < 282.0 ? "guard" : (y < 326.0 ? "away" : "front")));
                        if (y >= 195.0 && !action.isEmpty()) {
                            this.send("press", action);
                            return true;
                        }
                    }
                } else {
                    if (x >= 0.0 && x <= 106.0) {
                        this.send("press", "handset");
                        return true;
                    }

                    if (y >= 332.0 && y <= 369.0) {
                        if (x >= 127.0 && x <= 163.0) {
                            this.send("press", "guard_door");
                            return true;
                        }

                        if (x >= 167.0 && x <= 203.0) {
                            this.send("press", "away");
                            return true;
                        }
                    }
                }

                return false;
            }
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (!this.message.isFocused() || key != 257 && key != 335) {
            if (!this.message.isFocused() && !this.unit.isFocused() && key == 84) {
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
