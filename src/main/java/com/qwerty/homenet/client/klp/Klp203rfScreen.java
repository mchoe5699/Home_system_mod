package com.qwerty.homenet.client.klp;

import com.qwerty.homenet.HomeNet;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.client.ReceiverScreen;
import com.qwerty.homenet.client.logo.LogoRenderer;
import com.qwerty.homenet.intercom.IntercomLine;
import com.qwerty.homenet.network.ModNetwork;
import com.qwerty.homenet.network.WallpadActionPacket;
import com.qwerty.homenet.network.WallpadDataPacket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

public class Klp203rfScreen extends Screen implements ReceiverScreen {
    private WallpadDataPacket data;
    private final BlockPos pos;
    private int left;
    private int top;
    private float scale;
    private EditBox dong;
    private EditBox line;
    private EditBox guard;
    private EditBox range;
    private EditBox pin;
    private EditBox message;
    private String dongDraft;
    private String lineDraft;
    private String guardDraft;
    private String rangeDraft;
    private String pinDraft = "";
    private String messageDraft = "";
    private final Map<Integer, String> mouseHeld = new HashMap<>();
    private final Set<Integer> keyboardHeld = new HashSet<>();

    public Klp203rfScreen(WallpadDataPacket p) {
        super(Component.literal("KOCOM " + p.setting("klp_label", "KLP-203RF")));
        this.data = p;
        this.pos = p.pos();
    }

    @Override
    public BlockPos getPos() {
        return this.pos;
    }

    @Override
    public void update(WallpadDataPacket p) {
        boolean rebuild = this.data.flag("auth") != p.flag("auth")
            || this.data.flag("connected") != p.flag("connected")
            || this.data.callState() != p.callState()
            || !this.data.setting("klp_model", "").equals(p.setting("klp_model", ""));
        this.data = p;
        if (rebuild) {
            this.rebuildWidgets();
        }
    }

    private ResourceLocation texture(String name) {
        return HomeNet.id("textures/block/" + this.data.setting("klp_folder", "klp203rf") + "/" + name + ".png");
    }

    private void send(String key, String value) {
        ModNetwork.sendToServer(new WallpadActionPacket(this.pos, ReceiverBlockEntity.Action.SET_SETTING.ordinal(), BlockPos.ZERO, key + "=" + value));
    }

    private void action(ReceiverBlockEntity.Action a, String text) {
        ModNetwork.sendToServer(new WallpadActionPacket(this.pos, a.ordinal(), BlockPos.ZERO, text));
    }

    private EditBox field(String value, int x, int y, int w, int max, Consumer<String> responder) {
        EditBox b = new EditBox(
            this.font,
            this.left + (int)(x * this.scale),
            this.top + (int)(y * this.scale),
            (int)(w * this.scale),
            (int)(21.0F * this.scale),
            Component.literal("설정 입력")
        );
        b.setMaxLength(max);
        b.setValue(value);
        b.setResponder(responder);
        this.addRenderableWidget(b);
        return b;
    }

    private void button(String label, int x, int y, int w, Runnable action) {
        this.addRenderableWidget(
            Button.builder(Component.literal(label), b -> action.run())
                .bounds(this.left + (int)(x * this.scale), this.top + (int)(y * this.scale), (int)(w * this.scale), (int)(22.0F * this.scale))
                .build()
        );
    }

    @Override
    protected void init() {
        this.scale = Math.min(this.width / 840.0F, this.height / 490.0F);
        this.left = (int)((this.width - 820.0F * this.scale) / 2.0F);
        this.top = (int)((this.height - 470.0F * this.scale) / 2.0F);
        this.dong = this.line = this.guard = this.range = this.pin = this.message = null;
        if (this.data.flag("auth")) {
            if (this.dongDraft == null) {
                this.dongDraft = this.data.setting("dong", "101");
                this.lineDraft = this.data.setting("line", "1");
                this.guardDraft = this.data.setting("guard", "10");
                this.rangeDraft = this.data.setting("range", "01~99");
            }

            this.dong = this.field(this.dongDraft, 342, 105, 180, 4, s -> this.dongDraft = s);
            this.line = this.field(this.lineDraft, 554, 105, 180, 4, s -> this.lineDraft = s);
            this.guard = this.field(this.guardDraft, 342, 160, 180, 4, s -> this.guardDraft = s);
            this.range = this.field(this.rangeDraft, 554, 160, 180, 5, s -> this.rangeDraft = s);
            this.pin = this.field(this.pinDraft, 342, 215, 180, 4, s -> this.pinDraft = s);
            this.pin.setFormatter((s, i) -> FormattedCharSequence.forward("-".repeat(s.length()), Style.EMPTY));
            this.button(
                "설정 저장",
                342,
                261,
                180,
                () -> {
                    this.send(
                        "save",
                        this.dong.getValue()
                            + "|"
                            + this.line.getValue()
                            + "|"
                            + this.guard.getValue()
                            + "|"
                            + this.range.getValue()
                            + "|"
                            + this.pin.getValue()
                            + "|save"
                    );
                    this.pinDraft = "";
                    this.pin.setValue("");
                }
            );
            this.button("관리자 종료", 554, 261, 180, () -> this.send("exit", ""));
            if (!this.data.setting("klp_reader", "RF").equals("none")) {
                this.button("손에 든 카드 등록", 342, 327, 180, () -> this.send("card", "add"));
                this.button("손에 든 카드 삭제", 554, 327, 180, () -> this.send("card", "remove"));
            }
        } else {
            if (this.data.flag("connected")) {
                this.message = this.field(this.messageDraft, 342, 317, 330, 80, s -> this.messageDraft = s);
                this.button("전송", 682, 317, 70, this::sendMessage);
            }

            if (this.data.callState() != 0) {
                this.button("통화 종료", 342, 366, 180, () -> this.action(ReceiverBlockEntity.Action.HANG_UP, ""));
            }
        }

        this.button("닫기", 691, 427, 90, this::onClose);
    }

    private void sendMessage() {
        if (this.message != null && !this.message.getValue().isBlank()) {
            this.action(ReceiverBlockEntity.Action.SEND_MESSAGE, this.message.getValue());
            this.messageDraft = "";
            this.message.setValue("");
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        this.renderBackground(g);
        g.pose().pushPose();
        g.pose().translate((float)this.left, (float)this.top, 0.0F);
        g.pose().scale(this.scale, this.scale, 1.0F);
        g.fill(0, 0, 820, 470, -14669263);
        g.drawString(this.font, "KOCOM " + this.data.setting("klp_label", "KLP-203RF"), 13, 12, -1183506, false);
        g.pose().pushPose();
        g.pose().translate(12.0F, 35.0F, 0.0F);
        g.pose().scale(0.5F, 0.5F, 1.0F);
        g.blit(this.texture("gui"), 0, 0, 0.0F, 0.0F, 600, 784, 600, 784);
        LogoRenderer.gui(g, this.font, this.data, this.texture("logo"), 94, 48, 104, 24);
        KlpSegments.gui(g, this.data.setting("display", "-"), 341, 140);
        if (this.data.flag("visitor_light")) {
            for (int x : new int[]{112, 273}) {
                for (int j = 0; j < 3; j++) {
                    g.fill(x, 143 + j * 14, x + 18, 156 + j * 14, this.data.flag("color_video") ? -2083 : -7457939);
                }
            }
        }

        for (int i = 0; i < 3; i++) {
            boolean on = this.data.flag(i == 0 ? "home_led" : (i == 1 ? "guard_led" : "talk_led"));
            g.fill(
                364 + i * 54,
                198,
                369 + i * 54,
                202,
                !on || !this.data.flag("connected") && i != 2 && (this.minecraft.level == null || this.minecraft.level.getGameTime() % 20L >= 10L)
                    ? -13887465
                    : -51933
            );
        }

        g.pose().popPose();
        int color = -1709592;
        g.drawString(this.font, this.data.flag("auth") ? "관리자 설정" : "로비폰 조작", 342, 47, color, false);
        g.drawString(this.font, this.data.setting("status", ""), 342, this.data.flag("auth") ? 382 : 82, -3810883, false);
        if (this.data.flag("auth")) {
            g.drawString(this.font, "동 번호 (1~9999)", 342, 88, color, false);
            g.drawString(this.font, "라인 번호 (1~9999)", 554, 88, color, false);
            g.drawString(this.font, "경비번호", 342, 143, color, false);
            g.drawString(this.font, "호수 뒤 두 자리 (00~99)", 554, 143, color, false);
            g.drawString(this.font, "관리 비밀번호 (4자리)", 342, 198, color, false);
            g.drawString(this.font, "빈칸이면 기존 암호 유지", 554, 220, -5326143, false);
            g.drawString(
                this.font,
                this.data.setting("klp_reader", "RF").equals("none")
                    ? "카드 기능 없음"
                    : (this.data.setting("klp_reader", "RF").equals("insert") ? "삽입형 카드" : "RF 카드") + " · 등록 " + this.data.setting("cards", "0") + "장",
                342,
                304,
                color,
                false
            );
        } else {
            g.drawString(this.font, "세대번호 → 세대호출", 342, 121, color, false);
            g.drawString(this.font, "경비실 버튼으로 지정 경비실 호출", 342, 144, color, false);
            g.drawString(this.font, "호수 # 비밀번호 # / 경비번호 # 출입암호 #", 342, 178, color, false);
            g.drawString(this.font, "세대 통화 중 ##00# → 10초 → 새 암호 4자리 #", 342, 201, -5456699, false);
            g.drawString(this.font, "관리자: 세대호출(C) + 경비실(G) 3초 유지", 342, 244, color, false);
            g.drawString(this.font, "두 버튼은 마우스 왼쪽·오른쪽으로도 동시 유지", 342, 267, -5456699, false);

            for (int i = 0; i < Math.min(4, this.data.log().size()); i++) {
                IntercomLine l = this.data.log().get(Math.max(0, this.data.log().size() - 4) + i);
                g.drawString(this.font, this.font.plainSubstrByWidth(l.name() + ": " + l.text(), 420), 342, 395 + i * 10, -2959143, false);
            }
        }

        g.drawString(this.font, this.data.flag("linked") ? "KOCOM BUS · 연결됨" : "KOCOM BUS · 경비번호 / 링크 확인", 13, 444, -4414101, false);
        g.pose().popPose();
        super.render(g, mx, my, partial);
    }

    private int hit(double mx, double my) {
        double x = ((mx - this.left) / this.scale - 12.0) * 2.0;
        double y = ((my - this.top) / this.scale - 35.0) * 2.0;

        for (int i = 0; i < 14; i++) {
            int dx = 16;
            int cx = i < 12 ? 350 + dx + i % 3 * 58 : 350 + dx;
            int cy = i < 12 ? 344 + i / 3 * 58 : 572 + (i - 12) * 66;
            if (Math.hypot(x - cx, y - cy) <= 25.0) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        int i = this.hit(x, y);
        if (i >= 0 && !this.data.flag("auth")) {
            if (i < 12 && button == 0) {
                this.send("key", new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#"}[i]);
            } else if (i >= 12 && (button == 0 || button == 1)) {
                String key = i == 12 ? "home" : "guard";
                this.mouseHeld.put(button, key);
                this.send("down", key);
            }

            return true;
        } else {
            return super.mouseClicked(x, y, button);
        }
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        String held = this.mouseHeld.remove(button);
        if (held != null) {
            this.send("up", held);
            return true;
        } else {
            return super.mouseReleased(x, y, button);
        }
    }

    private boolean editing() {
        return this.getFocused() instanceof EditBox;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (this.editing()) {
            if (key == 257 && this.message != null && this.message.isFocused()) {
                this.sendMessage();
                return true;
            } else {
                return super.keyPressed(key, scan, mods);
            }
        } else {
            if (!this.data.flag("auth")) {
                if (key == 67 || key == 71) {
                    if (this.keyboardHeld.add(key)) {
                        this.send("down", key == 67 ? "home" : "guard");
                    }

                    return true;
                }

                String value = (mods & 1) != 0 && key == 51
                    ? "#"
                    : (
                        (mods & 1) != 0 && key == 56
                            ? "*"
                            : (
                                key >= 48 && key <= 57
                                    ? key - 48 + ""
                                    : (key >= 320 && key <= 329 ? key - 320 + "" : (key == 259 || key == 332 ? "*" : (key != 257 && key != 335 ? "" : "#")))
                            )
                    );
                if (!value.isEmpty()) {
                    this.send("key", value);
                    return true;
                }
            }

            return super.keyPressed(key, scan, mods);
        }
    }

    @Override
    public boolean keyReleased(int key, int scan, int mods) {
        if (this.keyboardHeld.remove(key)) {
            this.send("up", key == 67 ? "home" : "guard");
            return true;
        } else {
            return super.keyReleased(key, scan, mods);
        }
    }

    @Override
    public void removed() {
        this.send("cancel_hold", "");
        this.send("exit", "");
        this.mouseHeld.clear();
        this.keyboardHeld.clear();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
