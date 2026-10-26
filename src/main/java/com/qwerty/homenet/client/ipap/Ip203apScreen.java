package com.qwerty.homenet.client.ipap;

import com.qwerty.homenet.blockentity.Ip203apBlockEntity;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.client.ReceiverScreen;
import com.qwerty.homenet.client.logo.LogoRenderer;
import com.qwerty.homenet.network.ModNetwork;
import com.qwerty.homenet.network.WallpadActionPacket;
import com.qwerty.homenet.network.WallpadDataPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public final class Ip203apScreen extends Screen implements ReceiverScreen {
    private WallpadDataPacket data;
    private float scale;
    private int left;
    private int top;
    private EditBox unit;
    private EditBox message;
    private boolean frontHeld;
    private boolean guardHeld;
    private boolean chord;
    private int simultaneousTicks;
    private static final int DX = 18;
    private static final int DY = 38;
    private static final int DW = 207;
    private static final int DH = 470;

    public Ip203apScreen(WallpadDataPacket p) {
        super(Component.literal("KOCOM " + p.setting("family", p.flag("kip234r2") ? "KIP-234R2" : (p.flag("ip203apb") ? "IP-203APB" : "IP-203AP"))));
        this.data = p;
    }

    private boolean kip() {
        return this.data.flag("kip234r2");
    }

    private String family() {
        return this.data.setting("family", this.kip() ? "kip234r2" : (this.bus() ? "ip203apb" : "ip203ap"));
    }

    private boolean compact() {
        return this.data.flag("compact");
    }

    private boolean pc() {
        return this.family().equals("ip200pc");
    }

    private boolean pchb() {
        return this.family().equals("ip200pchb");
    }

    private boolean dp() {
        return this.family().equals("dp202h");
    }

    private String model() {
        return this.pc()
            ? "IP-200PC"
            : (this.pchb() ? "IP-200PCHB" : (this.dp() ? "DP-202H" : (this.kip() ? "KIP-234R2" : (this.bus() ? "IP-203APB" : "IP-203AP"))));
    }

    private boolean bus() {
        return this.data.setting("guard_system", "STAR").equals("BUS");
    }

    @Override
    public BlockPos getPos() {
        return this.data.pos();
    }

    @Override
    public void update(WallpadDataPacket p) {
        this.data = p;
        if (this.unit != null && (this.bus() || !this.unit.isFocused())) {
            this.unit.setValue(this.data.unit());
        }
    }

    private void send(ReceiverBlockEntity.Action a, String t) {
        ModNetwork.sendToServer(new WallpadActionPacket(this.data.pos(), a.ordinal(), BlockPos.ZERO, t));
    }

    private void button(String label, int x, int y, int w, Runnable action) {
        this.addRenderableWidget(
            Button.builder(Component.literal(label), b -> action.run())
                .bounds(
                    this.left + Math.round(x * this.scale),
                    this.top + Math.round(y * this.scale),
                    Math.round(w * this.scale),
                    Math.max(16, Math.round(22.0F * this.scale))
                )
                .build()
        );
    }

    @Override
    protected void init() {
        this.scale = Math.min(1.0F, Math.min((this.width - 16) / 610.0F, (this.height - 16) / 545.0F));
        this.left = Math.round((this.width - 610.0F * this.scale) / 2.0F);
        this.top = Math.round((this.height - 545.0F * this.scale) / 2.0F);
        if (!this.dp()) {
            this.unit = new EditBox(
                this.font,
                this.left + Math.round(270.0F * this.scale),
                this.top + Math.round(112.0F * this.scale),
                Math.round(318.0F * this.scale),
                18,
                Component.literal("동·호수")
            );
            this.unit.setMaxLength(16);
            this.unit.setValue(this.data.unit());
            this.unit.setEditable(!this.bus());
            this.addRenderableWidget(this.unit);
        }

        if (!this.bus() && !this.dp()) {
            this.button("동·호수 저장", 270, 138, 318, () -> this.send(ReceiverBlockEntity.Action.SET_UNIT, this.unit.getValue()));
        }

        this.button("−", 483, 311, 45, () -> this.volume(-5));
        this.button("+", 543, 311, 45, () -> this.volume(5));
        if (this.bus() && !this.compact()) {
            this.button(this.kip() ? "현관 + 경비 0.5초 동시 누름" : "현관 + 경비 동시 누름", 270, 350, 318, this::simultaneous);
        }

        this.message = new EditBox(
            this.font,
            this.left + Math.round(270.0F * this.scale),
            this.top + Math.round(416.0F * this.scale),
            Math.round(254.0F * this.scale),
            18,
            Component.literal("통화 메시지")
        );
        this.message.setMaxLength(180);
        this.addRenderableWidget(this.message);
        this.button("전송", 534, 415, 54, () -> {
            this.send(ReceiverBlockEntity.Action.SEND_MESSAGE, this.message.getValue());
            this.message.setValue("");
        });
    }

    private void simultaneous() {
        if (this.kip()) {
            this.send(ReceiverBlockEntity.Action.SET_SETTING, "front+guard_start");
            this.simultaneousTicks = 12;
        } else {
            this.send(ReceiverBlockEntity.Action.SET_SETTING, "front+guard");
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.simultaneousTicks > 0 && --this.simultaneousTicks == 0) {
            this.send(ReceiverBlockEntity.Action.SET_SETTING, "front+guard_end");
        }
    }

    private void volume(int n) {
        int v = Integer.parseInt(this.data.setting("volume", "70"));
        this.send(
            ReceiverBlockEntity.Action.SET_SETTING,
            "volume=" + (this.kip() ? (n > 0 ? (v < 65 ? 65 : 100) : (v > 65 ? 65 : 30)) : Math.max(0, Math.min(100, v + n)))
        );
    }

    private boolean lit(String lamp) {
        int mode = Integer.parseInt(this.data.setting("lamp_" + lamp, "0"));
        return Ip203apBlockEntity.lampVisible(mode, this.minecraft.level == null ? 0L : this.minecraft.level.getGameTime());
    }

    private void lamp(GuiGraphics g, int px, int py, boolean on, int color) {
        int x = 18 + Math.round(px / 460.0F * 207.0F);
        int y = 38 + Math.round(py / 970.0F * 470.0F);
        g.fill(x - 2, y - 2, x + 3, y + 3, on ? color : -10280414);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        this.renderBackground(g);
        g.pose().pushPose();
        g.pose().translate((float)this.left, (float)this.top, 0.0F);
        g.pose().scale(this.scale, this.scale, 1.0F);
        g.fill(0, 0, 610, 545, -14669520);
        g.fill(8, 30, 238, 514, -13616318);
        g.drawString(this.font, "KOCOM " + this.model(), 12, 12, -1117710, false);
        g.blit(Ip203apRenderer.texture(this.data.flag("lifted") ? "gui_open" : "gui", this.family()), 18, 38, 207, 470, 0.0F, 0.0F, 460, 970, 460, 970);
        LogoRenderer.gui(
            g,
            this.font,
            this.data,
            Ip203apRenderer.texture("logo", this.family()),
            18 + Math.round(139.5F),
            38 + Math.round((!this.compact() && !this.kip() ? (this.bus() ? 694 : 734) : 718) / 970.0F * 470.0F),
            Math.round(49.5F),
            Math.round(11.628866F)
        );
        if (this.bus() && !this.compact()) {
            this.lamp(g, 310, 525, this.lit(this.kip() ? "lobby" : "front"), -1427914);
            this.lamp(g, 310, 585, this.lit("away") || this.kip() && this.lit("guard"), -1427914);
            this.lamp(g, 310, 645, this.lit(this.kip() ? "front" : "guard"), -1427914);
        }

        if (!this.pc() && (this.bus() || this.dp())) {
            this.lamp(g, 375, 290, this.lit("power"), -51915);
        }

        g.fill(270, 12, 588, 38, -13088943);
        g.drawString(this.font, "기기 설정", 278, 20, -1117710, false);
        g.drawString(this.font, this.model() + " · " + (this.dp() ? "독립형" : (this.bus() ? "BUS" : "STAR")), 270, 55, -1117710, false);
        g.drawString(this.font, this.dp() ? "경비실 연결 기능 없음" : (this.bus() ? "동·호수: 경비실기에서 등록" : "동·호수: 세대에서 설정"), 270, 90, -3090728, false);
        if (this.bus()) {
            g.drawString(this.font, "번호 변경은 연결된 경비실기에서", 270, 143, -5128763, false);
        }
        String state = switch (this.data.callState()) {
            case 1 -> "호출 중";
            case 2 -> "통화 중";
            case 3 -> "발신 중";
            default -> "대기";
        };
        String source = this.data.setting("source", "idle");
        g.drawString(
            this.font,
            (source.equals("guard") ? "경비실 " : (source.equals("lobby") ? "로비폰 " : (source.equals("front") ? "현관 " : ""))) + state,
            270,
            205,
            -4727862,
            false
        );
        g.drawString(this.font, this.data.flag("lifted") ? "수화기 들림" : "수화기 놓임", 270, 226, -3090728, false);
        if (this.data.flag("front_waiting")) {
            g.drawString(this.font, "현관 호출 대기", 270, 247, -12176, false);
        }

        if (!this.compact() && this.bus()) {
            g.drawString(
                this.font,
                (this.kip() ? (this.data.flag("can_open_door") ? "열쇠 사용 가능" : "열쇠: 로비 통화 전용") : (this.data.flag("emergency") ? "비상 통보 중" : "비상 해제"))
                    + " · "
                    + (this.data.flag("away") ? "부재 설정" : "부재 해제"),
                270,
                273,
                this.data.flag("emergency") ? -34953 : -4076592,
                false
            );
        }

        if (this.compact()) {
            g.drawString(
                this.font,
                this.pc() ? "초인종: 호출 전용 · 통화 불가" : (this.pchb() ? (this.data.flag("emergency") ? "비상 통보 중" : "초인종: 호출 전용 · 통화 불가") : "열쇠: 수화기를 놓아도 문 열림"),
                270,
                273,
                -4076592,
                false
            );
        }

        g.drawString(this.font, "호출 음량  " + this.data.setting("volume", "70") + "%", 270, 318, -3090728, false);
        g.drawString(this.font, "통화 메시지", 270, 393, -3090728, false);
        g.drawString(
            this.font,
            this.compact()
                ? (this.pchb() ? "H 수화기 · E 비상" : (this.dp() ? "H 수화기 · O 열쇠" : "H 수화기"))
                : "H 수화기 · F 현관 · G 경비" + (this.kip() ? " · O 열쇠" : (this.bus() ? " · E 비상" : "")),
            12,
            525,
            -3682091,
            false
        );
        g.drawString(this.font, this.data.setting("guard_system", "STAR") + " · 경비실기: " + this.data.setting("board", "미연결"), 270, 488, -3625608, false);
        g.pose().popPose();
        super.render(g, mx, my, partial);
    }

    private boolean handsetHit(double x, double y) {
        double wy = 15.3 - y / 500.0 * 9.7;
        if (!(wy < 6.3) && !(wy > 15.18)) {
            double t = (wy - 6.3) / 8.88;
            double r = wy < 7.18 ? 0.88 : (wy > 14.75 ? 0.43 : 0.0);
            double d = wy < 6.3 + r ? 6.3 + r - wy : wy - (15.18 - r);
            double inset = r == 0.0 ? 0.0 : r - Math.sqrt(Math.max(0.0, r * r - d * d));
            double wx = 10.3 - x / 220.0 * 4.6;
            return wx >= 7.69 + 0.11 * Math.sin(Math.PI * t) + inset && wx <= 10.15 - 0.035 * Math.sin(Math.PI * t) - inset;
        } else {
            return false;
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        if (b == 0) {
            double x = ((mx - this.left) / this.scale - 18.0) / 207.0 * 460.0;
            double y = ((my - this.top) / this.scale - 38.0) / 470.0 * 970.0;
            if (this.handsetHit(x / 460.0 * 220.0, y / 970.0 * 500.0)) {
                this.send(ReceiverBlockEntity.Action.PICKUP, "");
                return true;
            }

            if (!this.pc() && (this.bus() || this.dp()) && Math.hypot(x - 376.0, y - 395.0) < 35.0) {
                this.send(!this.kip() && !this.dp() ? ReceiverBlockEntity.Action.EMERGENCY : ReceiverBlockEntity.Action.OPEN_DOOR, "");
                return true;
            }

            if (!this.compact() && Math.hypot(x - (this.bus() ? 388 : 376), y - (this.bus() ? 525 : 550)) < (this.bus() ? 26 : 35)) {
                if (this.bus() && hasShiftDown()) {
                    this.simultaneous();
                } else {
                    this.send(this.kip() ? ReceiverBlockEntity.Action.CALL_GUARD : ReceiverBlockEntity.Action.SET_SETTING, this.kip() ? "" : "front");
                }

                return true;
            }

            if (!this.compact() && Math.hypot(x - (this.bus() ? 388 : 376), y - (this.bus() ? 645 : 694)) < (this.bus() ? 26 : 35)) {
                if (this.bus() && hasShiftDown()) {
                    this.simultaneous();
                } else {
                    this.send(this.kip() ? ReceiverBlockEntity.Action.SET_SETTING : ReceiverBlockEntity.Action.CALL_GUARD, this.kip() ? "front" : "");
                }

                return true;
            }
        }

        return super.mouseClicked(mx, my, b);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double d) {
        double x = (mx - this.left) / this.scale;
        double y = (my - this.top) / this.scale;
        if (x >= 18.0 && x < 34.0 && y >= 38.0 && y < 148.0) {
            this.volume(d > 0.0 ? 5 : -5);
            return true;
        } else {
            return super.mouseScrolled(mx, my, d);
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if ((this.bus() || this.unit == null || !this.unit.isFocused()) && (this.message == null || !this.message.isFocused())) {
            if (key == 72) {
                this.send(ReceiverBlockEntity.Action.PICKUP, "");
                return true;
            } else if ((this.kip() || this.dp()) && key == 79) {
                this.send(ReceiverBlockEntity.Action.OPEN_DOOR, "");
                return true;
            } else if (this.bus() && !this.kip() && key == 69) {
                this.send(ReceiverBlockEntity.Action.EMERGENCY, "");
                return true;
            } else if (!this.compact() || key != 70 && key != 71) {
                if (!this.bus() || key != 70 && key != 71) {
                    if (key == 70) {
                        this.send(ReceiverBlockEntity.Action.SET_SETTING, "front");
                        return true;
                    } else if (key == 71) {
                        this.send(ReceiverBlockEntity.Action.CALL_GUARD, "");
                        return true;
                    } else if (key == 86) {
                        this.volume(5);
                        return true;
                    } else {
                        return super.keyPressed(key, scan, mods);
                    }
                } else {
                    if (key == 70) {
                        this.frontHeld = true;
                    } else {
                        this.guardHeld = true;
                    }

                    if (this.frontHeld && this.guardHeld && !this.chord) {
                        this.chord = true;
                        this.send(ReceiverBlockEntity.Action.SET_SETTING, this.kip() ? "front+guard_start" : "front+guard");
                    }

                    return true;
                }
            } else {
                return true;
            }
        } else {
            return super.keyPressed(key, scan, mods);
        }
    }

    @Override
    public boolean keyReleased(int key, int scan, int mods) {
        if (!this.compact() || key != 70 && key != 71) {
            if (this.bus() && (key == 70 || key == 71)) {
                if (!this.chord && (key == 70 ? this.frontHeld : this.guardHeld)) {
                    this.send(key == 70 ? ReceiverBlockEntity.Action.SET_SETTING : ReceiverBlockEntity.Action.CALL_GUARD, key == 70 ? "front" : "");
                }

                if (this.chord && this.kip()) {
                    this.send(ReceiverBlockEntity.Action.SET_SETTING, "front+guard_end");
                }

                if (key == 70) {
                    this.frontHeld = false;
                } else {
                    this.guardHeld = false;
                }

                if (!this.frontHeld && !this.guardHeld) {
                    this.chord = false;
                }

                return true;
            } else {
                return super.keyReleased(key, scan, mods);
            }
        } else {
            return true;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
