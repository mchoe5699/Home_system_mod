package com.qwerty.homenet.client.am;

import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.client.ReceiverScreen;
import com.qwerty.homenet.client.logo.LogoRenderer;
import com.qwerty.homenet.intercom.IntercomLine;
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

public class Am10kpScreen extends Screen implements ReceiverScreen {
    private WallpadDataPacket data;
    private int left;
    private int top;
    private float scale;
    private final List<Am10kpScreen.Hit> hits = new ArrayList<>();
    private EditBox message;
    private int forwarding;
    private static final int[] MASK = new int[]{63, 6, 91, 79, 102, 109, 125, 7, 127, 111};
    private static final int[][] SEG = new int[][]{{1, 0, 5, 1}, {5, 1, 6, 5}, {5, 6, 6, 10}, {1, 10, 5, 11}, {0, 6, 1, 10}, {0, 1, 1, 5}, {1, 5, 5, 6}};

    public Am10kpScreen(WallpadDataPacket d) {
        super(Component.literal("KOCOM AM-10KP"));
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

    private void send(ReceiverBlockEntity.Action a, String text, BlockPos target) {
        ModNetwork.sendToServer(new WallpadActionPacket(this.getPos(), a.ordinal(), target, text));
    }

    private void setting(String key, String v) {
        this.send(ReceiverBlockEntity.Action.SET_SETTING, key + "=" + v, BlockPos.ZERO);
    }

    private void mode(String m) {
        this.setting("mode", m);
    }

    @Override
    protected void init() {
        this.scale = Math.min(this.width / 760.0F, this.height / 450.0F);
        this.left = (int)((this.width - 740.0F * this.scale) / 2.0F);
        this.top = (int)((this.height - 425.0F * this.scale) / 2.0F);
        this.message = new EditBox(
            this.font,
            this.left + (int)(510.0F * this.scale),
            this.top + (int)(330.0F * this.scale),
            (int)(210.0F * this.scale),
            (int)(20.0F * this.scale),
            Component.literal("통화 메시지")
        );
        this.message.setMaxLength(80);
        this.addRenderableWidget(this.message);
    }

    private void hit(int x, int y, int w, int h, Runnable a) {
        this.hits.add(new Am10kpScreen.Hit(x, y, w, h, a));
    }

    private void button(GuiGraphics g, String label, int x, int y, int w, Runnable a) {
        g.fill(x, y, x + w, y + 20, -12824228);
        g.drawString(this.font, label, x + 5, y + 6, -920334, false);
        this.hit(x, y, w, 20, a);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        this.renderBackground(g);
        this.hits.clear();
        g.pose().pushPose();
        g.pose().translate((float)this.left, (float)this.top, 0.0F);
        g.pose().scale(this.scale, this.scale, 1.0F);
        g.fill(-8, -8, 744, 428, -283104206);
        g.drawString(this.font, "KOCOM AM-10KP  ·  디지털 BUS", 8, 7, -1972002, false);
        g.blit(Am10kpRenderer.texture(this.data.flag("lifted") ? "face" : "gui"), 0, 35, 0.0F, 0.0F, 500, 323, 500, 323);
        LogoRenderer.gui(g, this.font, this.data, Am10kpRenderer.texture("logo"), 117, 149, 74, 20);

        for (int row = 0; row < 5; row++) {
            String[] pair = this.data.setting("display" + row, "").split("-", 2);
            int y = 35 + (153 + row * 83) / 2;
            this.segmentDigits(g, pair[0], 218, y + 2, 3);
            if (pair.length > 1) {
                this.segmentDigits(g, pair[1], 254, y + 2, 4);
            }

            if (row > 0 && this.data.flag("led" + row)) {
                g.fill(307, 35 + (row == 0 ? 132 : 153 + row * 83 - 25) / 2, 312, 35 + (row == 0 ? 140 : 153 + row * 83 - 17) / 2, -308890);
            }
        }

        for (int i = 0; i < 4; i++) {
            if (this.data.flag("mode" + i)) {
                g.fill((699 + i * 53) / 2, 129, (707 + i * 53) / 2, 133, i == 0 ? -42644 : -11156347);
            }
        }

        this.hit(5, 39, 80, 310, () -> this.mode("handset"));

        for (int ix = 0; ix < 12; ix++) {
            String digit = "123456789*0#".substring(ix, ix + 1);
            this.hit((704 + ix % 3 * 80 - 25) / 2, 35 + (291 + ix / 3 * 72 - 25) / 2, 25, 25, () -> this.setting("key", digit));
        }

        for (int ix = 0; ix < 4; ix++) {
            String m = new String[]{"emergency", "building", "exchange", "mutual"}[ix];
            this.hit((681 + ix * 53) / 2, 133, 26, 24, () -> this.mode(m));
            int line = ix;
            this.hit(300, 35 + (153 + (ix + 1) * 83 + 25) / 2, 21, 18, () -> this.setting("line", Integer.toString(line)));
        }

        this.hit(300, 109, 21, 22, () -> this.mode("building"));
        this.hit(175, 286, 23, 28, () -> this.mode("power"));
        this.hit(115, 281, 17, 37, () -> this.mode("volume"));
        this.hit(139, 281, 16, 37, () -> this.mode("melody"));
        g.drawString(this.font, "등록 " + this.data.setting("registered", "0") + " 세대 · 연결 수 제한 없음", 10, 365, -3353398, false);
        g.drawString(this.font, this.data.setting("status", ""), 10, 383, -1787000, false);
        int x = 510;
        g.drawString(this.font, "수화기 → 통화1~4 버튼으로 응답", x, 38, -2695469, false);
        g.drawString(this.font, "동 입력 → 동 → 호 입력 → # 호출", x, 57, -2695469, false);
        this.button(g, this.data.flag("absent") ? "경비실 부재 해제" : "경비실 부재 전환", x, 77, 210, () -> this.mode("away"));
        this.button(g, this.data.flag("lifted") ? "수화기 내려놓기" : "수화기 들기", x, 102, 210, () -> this.mode("handset"));
        g.drawString(this.font, "세대 번호 등록", x, 132, -2695469, false);
        g.drawString(this.font, "세대 발신 통화 중 **00* 입력", x, 150, -3811649, false);
        g.drawString(this.font, "새 동 → 동 → 새 호 → * 저장", x, 168, -3811649, false);
        g.drawString(this.font, "경비실 번호: 상호 → **00*", x, 190, -3811649, false);
        g.drawString(this.font, "새 번호 → * 저장", x, 207, -3811649, false);
        g.drawString(this.font, "비상 " + this.data.setting("alarms", "0") + "세대 · " + (this.data.flag("muted") ? "경보음 정지" : "경보음 사용"), x, 230, -1467733, false);
        this.button(g, "비상 경보음 정지", x, 249, 210, () -> this.mode("emergency"));
        this.button(g, "부재 전환: " + this.data.setting("forward", "미설정"), x, 274, 210, () -> this.cycleForward());
        g.drawString(this.font, "음량 " + this.data.setting("volume", "2") + " / 멜로디 " + this.data.setting("melody", "1"), x, 304, -3811649, false);
        this.button(g, "통화 메시지 전송", x, 355, 210, () -> {
            this.send(ReceiverBlockEntity.Action.SEND_MESSAGE, this.message.getValue(), BlockPos.ZERO);
            this.message.setValue("");
        });
        List<IntercomLine> logs = this.data.log();
        if (!logs.isEmpty()) {
            IntercomLine l = logs.get(logs.size() - 1);
            g.drawString(this.font, this.font.plainSubstrByWidth(l.name() + ": " + l.text(), 208), x, 385, -2959921, false);
        }

        g.pose().popPose();
        super.render(g, mx, my, partial);
    }

    private void cycleForward() {
        int count = Integer.parseInt(this.data.setting("peer_count", "0"));
        if (count != 0) {
            String entry = this.data.setting("peer" + this.forwarding++ % count, "");

            try {
                String[] xyz = entry.split(":")[0].split(",");
                this.send(
                    ReceiverBlockEntity.Action.SET_SETTING,
                    "forward=1",
                    new BlockPos(Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2]))
                );
            } catch (Exception var4) {
            }
        }
    }

    private void segmentDigits(GuiGraphics g, String s, int x, int y, int count) {
        if (this.data.flag("power")) {
            s = s.replaceAll("[^0-9]", "");
            if (s.length() > count) {
                s = s.substring(s.length() - count);
            }

            s = " ".repeat(count - s.length()) + s;

            for (int i = 0; i < count; i++) {
                if (s.charAt(i) != ' ') {
                    for (int j = 0; j < 7; j++) {
                        int[] a = SEG[j];
                        g.fill(x + i * 8 + a[0], y + a[1], x + i * 8 + a[2], y + a[3], (MASK[s.charAt(i) - '0'] & 1 << j) != 0 ? -4039508 : -13096135);
                    }
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        if (super.mouseClicked(mx, my, b)) {
            return true;
        } else {
            double x = (mx - this.left) / this.scale;
            double y = (my - this.top) / this.scale;

            for (Am10kpScreen.Hit h : this.hits) {
                if (x >= h.x && x < h.x + h.w && y >= h.y && y < h.y + h.h) {
                    h.action.run();
                    return true;
                }
            }

            return false;
        }
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
        if (this.message.isFocused()) {
            return super.keyPressed(code, scan, mods);
        } else if (code == 257 || code == 335) {
            this.setting("key", "#");
            return true;
        } else if (code == 259) {
            this.setting("key", "*");
            return true;
        } else {
            return super.keyPressed(code, scan, mods);
        }
    }

    private record Hit(int x, int y, int w, int h, Runnable action) {
    }
}
