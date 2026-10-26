package com.qwerty.homenet.sht;

import com.qwerty.homenet.intercom.IntercomLine;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

final class Sht5500Controls extends Screen {
    final BlockPos pos;
    Wire.GuardView data;
    private int tab;
    private int refreshTicks;
    private EditBox numberBox;
    private EditBox chatBox;
    private static final String[][] FUNCTIONS = new String[][]{
        {"세대", "mode_house"},
        {"경비실", "mode_guard"},
        {"현관", "mode_lobby"},
        {"관리실", "mode_office"},
        {"외출", "mode_away"},
        {"전환", "mode_transfer"},
        {"문열림", "door"},
        {"문닫힘", "door_close"},
        {"경보정지", "stop"},
        {"복귀", "restore"},
        {"야간감시", "night"},
        {"부재확인", "away_list"},
        {"상황확인", "alarm_list"},
        {"음량+", "volume_up"},
        {"음량-", "volume_down"},
        {"프로그램", "program"},
        {"프린트", "print"},
        {"지우기", "backspace"}
    };

    Sht5500Controls(Wire.GuardView view) {
        super(Component.literal("SAMSUNG SHT-5500S"));
        this.data = view;
        this.pos = view.pos();
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
            Wire.guardOpen(this.pos);
        }
    }

    private void send(String action) {
        Wire.guardKey(this.pos, action, "");
    }

    private void key(String digit) {
        Wire.guardKey(this.pos, "digit", digit);
    }

    private void button(String label, String action, int x, int y, int w) {
        this.addRenderableWidget(Button.builder(Component.literal(label), b -> this.send(action)).bounds(x, y, w, 20).build());
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 217;
        int y = Math.max(9, this.height / 2 - 124);
        this.button("수화기 들기", "lift", x, y + 109, 104);
        this.button("수화기 놓기", "hang", x + 110, y + 109, 104);
        this.button("취소", "cancel", x + 220, y + 109, 104);
        String[] names = new String[]{"숫자판", "기능 버튼", "단축 버튼", "설정"};

        for (int i = 0; i < 4; i++) {
            int tabIndex = i;
            this.addRenderableWidget(Button.builder(Component.literal(names[i]), b -> {
                this.tab = tabIndex;
                this.rebuildWidgets();
            }).bounds(x + i * 109, y + 133, 105, 20).build());
        }

        if (this.tab == 0) {
            String[][] digits = new String[][]{{"1", "2", "3"}, {"4", "5", "6"}, {"7", "8", "9"}, {"동", "0", "호출/다음"}};

            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 3; col++) {
                    String label = digits[row][col];
                    int bx = x + col * 145;
                    int by = y + 158 + row * 22;
                    this.addRenderableWidget(Button.builder(Component.literal(label), b -> {
                        if (label.equals("동")) {
                            this.send("dong");
                        } else if (label.equals("호출/다음")) {
                            this.send("call");
                        } else {
                            this.key(label);
                        }
                    }).bounds(bx, by, 140, 20).build());
                }
            }
        }

        if (this.tab == 1) {
            for (int i = 0; i < FUNCTIONS.length; i++) {
                this.button(FUNCTIONS[i][0], FUNCTIONS[i][1], x + i % 4 * 109, y + 158 + i / 4 * 22, 105);
            }
        }

        if (this.tab == 2) {
            for (int slot = 1; slot <= 17; slot++) {
                int selected = slot;
                int bx = x + (slot - 1) % 6 * 72;
                int by = y + 158 + (slot - 1) / 6 * 23;
                this.addRenderableWidget(
                    Button.builder(Component.literal("단" + slot), b -> Wire.guardKey(this.pos, "shortcut", Integer.toString(selected)))
                        .bounds(bx, by, 68, 20)
                        .build()
                );
            }
        }

        this.numberBox = null;
        if (this.tab == 3) {
            this.numberBox = new EditBox(this.font, x, y + 162, 120, 20, Component.literal("경비실 번호"));
            this.numberBox.setMaxLength(4);
            this.numberBox.setValue(this.data.number());
            this.addRenderableWidget(this.numberBox);
            this.addRenderableWidget(
                Button.builder(Component.literal("경비실 번호 저장"), b -> Wire.guardKey(this.pos, "number", this.numberBox.getValue()))
                    .bounds(x + 126, y + 162, 145, 20)
                    .build()
            );
        }

        this.chatBox = new EditBox(this.font, x, y + 254, 333, 20, Component.literal("통화 메시지"));
        this.chatBox.setMaxLength(32);
        this.addRenderableWidget(this.chatBox);
        this.addRenderableWidget(Button.builder(Component.literal("전송"), b -> this.sendChat()).bounds(x + 339, y + 254, 95, 20).build());
    }

    private void sendChat() {
        if (this.chatBox != null && !this.chatBox.getValue().isBlank()) {
            Wire.guardKey(this.pos, "chat", this.chatBox.getValue());
            this.chatBox.setValue("");
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float dt) {
        this.renderBackground(g);
        int x = this.width / 2 - 217;
        int y = Math.max(9, this.height / 2 - 124);
        g.drawString(this.font, this.title, x, y - 16, -790558);
        g.fill(x, y + 20, x + 435, y + 105, -2500407);
        g.fill(x + 4, y + 24, x + 431, y + 101, -1118751);
        g.fill(x + 9, y + 28, x + 82, y + 97, -4671318);
        g.fill(x + 12, y + 31, x + 79, y + 94, -592663);
        g.fill(x + 21, y + 35, x + 70, y + 89, -1447716);
        g.drawString(this.font, "SAMSUNG", x + 95, y + 30, -14140861);
        g.drawString(this.font, "SHT-5500S", x + 95, y + 86, -13351607);
        g.fill(x + 267, y + 27, x + 426, y + 96, -8024704);
        g.fill(x + 270, y + 30, x + 423, y + 93, -5522523);
        String[] lines = this.data.lcd().split("\\n", -1);

        for (int i = 0; i < Math.min(4, lines.length); i++) {
            String line = lines[i];
            if (line.length() > 23) {
                line = line.substring(0, 23);
            }

            g.drawString(this.font, line, x + 276, y + 34 + i * 13, -15258592);
        }

        String[] labels = new String[]{"전원", "화재", "가스", "방범", "비상", "문열림", "외출", "야간"};
        int[] colors = new int[]{-11285162, -1291713, -21963, -12407064, -1816498, -1328328, -30380, -11165456};

        for (int i = 0; i < labels.length; i++) {
            int bx = x + 94 + i % 4 * 42;
            int by = y + 48 + i / 4 * 18;
            boolean on = (this.data.lamps() & 1 << i) != 0;
            g.fill(bx, by, bx + 6, by + 6, on ? colors[i] : -10984616);
            g.drawString(this.font, labels[i], bx + 8, by - 1, -14666454);
        }

        int start = Math.max(0, this.data.log().size() - 5);

        for (int i = start; i < this.data.log().size(); i++) {
            IntercomLine message = this.data.log().get(i);
            String line = message.name() + ": " + message.text();
            if (line.length() > 68) {
                line = line.substring(0, 68);
            }

            g.drawString(this.font, line, x, y + 281 + (i - start) * 11, -1118482);
        }

        super.render(g, mx, my, dt);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if ((this.chatBox == null || !this.chatBox.isFocused()) && (this.numberBox == null || !this.numberBox.isFocused())) {
            if (c >= '0' && c <= '9') {
                this.key(String.valueOf(c));
                return true;
            } else {
                return super.charTyped(c, mods);
            }
        } else {
            return super.charTyped(c, mods);
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (this.chatBox != null && this.chatBox.isFocused()) {
            if (key != 257 && key != 335) {
                return super.keyPressed(key, scan, mods);
            } else {
                this.sendChat();
                return true;
            }
        } else if (this.numberBox != null && this.numberBox.isFocused()) {
            if (key != 257 && key != 335) {
                return super.keyPressed(key, scan, mods);
            } else {
                Wire.guardKey(this.pos, "number", this.numberBox.getValue());
                return true;
            }
        } else if (key == 257 || key == 335) {
            this.send("call");
            return true;
        } else if (key == 259) {
            this.send("backspace");
            return true;
        } else {
            return super.keyPressed(key, scan, mods);
        }
    }
}
