package com.qwerty.homenet.client.kam;

import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.client.ReceiverScreen;
import com.qwerty.homenet.client.logo.LogoRenderer;
import com.qwerty.homenet.network.GuardAnglePacket;
import com.qwerty.homenet.network.ModNetwork;
import com.qwerty.homenet.network.WallpadActionPacket;
import com.qwerty.homenet.network.WallpadDataPacket;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class Kam305Screen extends Screen implements ReceiverScreen {
    private WallpadDataPacket data;
    private float scale;
    private int left;
    private int top;
    private EditBox message;
    private final List<Kam305Screen.Hit> hits = new ArrayList<>();
    private long entering;

    public Kam305Screen(WallpadDataPacket d) {
        super(Component.literal("KOCOM KAM-305"));
        this.data = d;
    }

    @Override
    public BlockPos getPos() {
        return this.data.pos();
    }

    @Override
    public void update(WallpadDataPacket d) {
        this.data = d;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void action(ReceiverBlockEntity.Action a, String text) {
        ModNetwork.sendToServer(new WallpadActionPacket(this.getPos(), a.ordinal(), BlockPos.ZERO, text));
    }

    private void setting(String key, String value) {
        this.action(ReceiverBlockEntity.Action.SET_SETTING, key + "=" + value);
    }

    private void press(String b) {
        if (b.equals("enter") && this.data.setting("page", "").equals("dial")) {
            if (this.entering == 0L) {
                this.entering = System.currentTimeMillis();
                this.setting("button", "enter_down");
            }
        } else {
            this.setting("button", b);
        }
    }

    @Override
    protected void init() {
        this.scale = Math.min(this.width / 610.0F, this.height / 420.0F);
        this.left = (int)((this.width - 580.0F * this.scale) / 2.0F);
        this.top = (int)((this.height - 400.0F * this.scale) / 2.0F);
        this.message = new EditBox(
            this.font,
            this.left + (int)(8.0F * this.scale),
            this.top + (int)(378.0F * this.scale),
            (int)(480.0F * this.scale),
            (int)(18.0F * this.scale),
            Component.literal("통화 메시지")
        );
        this.message.setMaxLength(80);
        this.addRenderableWidget(this.message);
    }

    private void hit(int x, int y, int w, int h, Runnable a) {
        this.hits.add(new Kam305Screen.Hit(x, y, w, h, a));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        this.renderBackground(g);
        this.hits.clear();
        if (this.entering > 0L && System.currentTimeMillis() - this.entering >= 700L) {
            this.entering = 0L;
            this.setting("button", "enter_up");
        }

        this.message.visible = this.data.flag("lifted") || this.data.flag("handsfree");
        if (!this.message.visible) {
            this.message.setFocused(false);
        }

        g.pose().pushPose();
        g.pose().translate((float)this.left, (float)this.top, 0.0F);
        g.pose().scale(this.scale, this.scale, 1.0F);
        g.blit(Kam305Renderer.texture(this.data.flag("lifted") ? "face" : "gui"), 0, 0, 0.0F, 0.0F, 580, 358, 580, 358);
        if (this.data.flag("lifted")) {
            g.blit(Kam305Renderer.texture("screen_face"), 293, 22, 0.0F, 0.0F, 260, 201, 260, 201);
        }

        LogoRenderer.gui(g, this.font, this.data, Kam305Renderer.texture("logo"), 355, 186, 137, 21);
        g.fill(326, 60, 521, 160, !this.data.flag("power") && this.data.setting("factory", "0").equals("0") ? -11049408 : -6443679);
        if (this.data.flag("power") || !this.data.setting("factory", "0").equals("0")) {
            g.pose().pushPose();
            g.pose().translate(326.0F, 60.0F, 0.0F);
            g.pose().scale(0.8125F, 0.78125F, 1.0F);
            boolean standby = this.data.setting("lcd2", "").equals("KOCOM");
            int selected = Integer.parseInt(this.data.setting("selected_row", "-1"));
            String page = this.data.setting("page", "");
            boolean call = page.equals("calls") || page.equals("lobby") || page.equals("lobby_ring");

            for (int i = 0; i < 8; i++) {
                String line = this.data.setting("lcd" + i, "");
                int y = 5 + i * 15;
                if (standby && i == 2) {
                    if (this.data.flag("product_logo_configured")) {
                        LogoRenderer.gui(g, this.font, this.data, Kam305Renderer.texture("lcd_logo"), 19, 37, 202, 42);
                    } else {
                        g.blit(Kam305Renderer.texture("lcd_logo"), 19, 37, 0.0F, 0.0F, 202, 42, 202, 42);
                    }
                } else if (i == selected && page.equals("form")) {
                    int start = Math.max(0, Math.min(line.length(), Integer.parseInt(this.data.setting("highlight_start", "0"))));
                    int length = Math.max(0, Math.min(line.length() - start, Integer.parseInt(this.data.setting("highlight_length", "0"))));
                    String label = line.substring(0, start);
                    String value = line.substring(start, start + length);
                    String suffix = line.substring(start + length);
                    int valueX = 5 + this.font.width(label);
                    g.drawString(this.font, label, 5, y, -14470628, false);
                    g.fill(valueX - 2, y - 1, Math.min(238, valueX + Math.max(16, this.font.width(value) + 4)), y + 11, -14470628);
                    g.drawString(this.font, value, valueX, y, -6443679, false);
                    g.drawString(this.font, suffix, valueX + this.font.width(value), y, -14470628, false);
                } else if (i != selected || !page.equals("admin") && !page.equals("guard")) {
                    int x = !call || i != 2 && i != 3
                        ? (
                            (!standby || i != 0 && i != 6) && (!call || i != 0) && (!page.equals("form") || i != 0)
                                ? 5
                                : Math.max(5, (240 - this.font.width(line)) / 2)
                        )
                        : 112;
                    g.drawString(this.font, this.font.plainSubstrByWidth(line, !call || i != 2 && i != 3 ? 230 : 118), x, y, -14470628, false);
                } else {
                    g.fill(3, y - 1, 237, y + 11, -14470628);
                    g.drawString(this.font, this.font.plainSubstrByWidth(line, 230), 5, y, -6443679, false);
                }
            }

            if (call) {
                g.blit(Kam305Renderer.texture("lcd_phone"), 15, 40, 0.0F, 0.0F, 78, 51, 78, 51);
            }

            if (page.equals("pin")) {
                g.renderOutline(8, 56, 220, 23, -14470628);
            }

            if (page.equals("dial")) {
                g.renderOutline(8, 56, 220, 28, -14470628);
            }

            g.pose().popPose();
        }

        this.hit(7, 8, 95, 334, () -> this.press("handset"));

        for (int ix = 0; ix < 12; ix++) {
            String k = "123456789*0#".substring(ix, ix + 1);
            this.hit((int)((269 + ix % 3 * 81 - 28) * 0.58), (int)((267 + ix / 3 * 76 - 23) * 0.58), 33, 28, () -> this.setting("key", k));
        }

        for (int ix = 0; ix < 4; ix++) {
            String b = new String[]{"exchange", "absent", "admin", "settings"}[ix];
            this.hit((int)((260 + ix * 54 - 22) * 0.58), 105, 27, 28, () -> this.press(b));
        }

        for (int ix = 0; ix < 8; ix++) {
            String b = new String[]{"enter", "up", "f1", "emergency", "door", "down", "f2", "f3"}[ix];
            this.hit((int)((578 + ix % 4 * 106 - 34) * 0.58), (int)((460 + ix / 4 * 73 - 20) * 0.58), 40, 29, () -> {
                if (b.equals("door")) {
                    this.action(ReceiverBlockEntity.Action.OPEN_DOOR, "");
                } else {
                    this.press(b);
                }
            });
        }

        if (this.data.flag("lifted") || this.data.flag("handsfree")) {
            g.fill(495, 378, 577, 396, -12365231);
            g.drawString(this.font, "통화 전송", 501, 383, -1643295, false);
            this.hit(495, 378, 82, 18, () -> {
                this.action(ReceiverBlockEntity.Action.SEND_MESSAGE, this.message.getValue());
                this.message.setValue("");
            });
        }

        g.pose().popPose();
        super.render(g, mx, my, partial);
    }

    @Override
    public void onClose() {
        this.setting("button", "close");
        super.onClose();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        if (super.mouseClicked(mx, my, b)) {
            return true;
        } else {
            double x = (mx - this.left) / this.scale;
            double y = (my - this.top) / this.scale;

            for (Kam305Screen.Hit h : this.hits) {
                if (x >= h.x && x < h.x + h.w && y >= h.y && y < h.y + h.h) {
                    h.a.run();
                    return true;
                }
            }

            return false;
        }
    }

    @Override
    public boolean mouseReleased(double x, double y, int b) {
        if (this.entering > 0L) {
            this.setting("button", "enter_cancel");
        }

        this.entering = 0L;
        return super.mouseReleased(x, y, b);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (!this.message.isFocused() && "0123456789*#".indexOf(c) >= 0) {
            this.setting("key", Character.toString(c));
            return true;
        } else {
            return super.charTyped(c, mods);
        }
    }

    @Override
    public boolean keyPressed(int code, int scan, int mods) {
        if (!hasControlDown() || code != 61 && code != 334 && code != 45 && code != 333) {
            if (!this.message.isFocused()) {
                if (code == 257 || code == 335) {
                    this.press("enter");
                    return true;
                }

                if (code == 265) {
                    this.press("up");
                    return true;
                }

                if (code == 264) {
                    this.press("down");
                    return true;
                }

                if (code == 290) {
                    this.press("f1");
                    return true;
                }

                if (code == 291) {
                    this.press("f2");
                    return true;
                }

                if (code == 292) {
                    this.press("f3");
                    return true;
                }
            }

            return super.keyPressed(code, scan, mods);
        } else {
            ModNetwork.sendToServer(new GuardAnglePacket(this.getPos(), code != 45 && code != 333 ? 1 : -1));
            return true;
        }
    }

    @Override
    public boolean keyReleased(int key, int scan, int mods) {
        if (key == 257 || key == 335) {
            if (this.entering > 0L) {
                this.setting("button", "enter_cancel");
            }

            this.entering = 0L;
        }

        return super.keyReleased(key, scan, mods);
    }

    private record Hit(int x, int y, int w, int h, Runnable a) {
    }
}
