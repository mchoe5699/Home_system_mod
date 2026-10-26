package com.qwerty.homenet.client.move;

import com.qwerty.homenet.network.DeviceMoveOpenPacket;
import com.qwerty.homenet.network.DeviceNudgePacket;
import com.qwerty.homenet.network.ModNetwork;
import java.math.BigDecimal;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public final class DeviceMoveScreen extends Screen {
    private BlockPos pos;
    private String message;
    private String draft = "0.1";
    private boolean mmMode;
    private boolean pending;
    private EditBox amount;
    private int held = -1;
    private int heldKey = -1;
    private int cooldown;
    private int pendingTicks;

    public DeviceMoveScreen(DeviceMoveOpenPacket p) {
        super(Component.literal("제품 위치 조정"));
        this.update(p);
    }

    public void update(DeviceMoveOpenPacket p) {
        this.pos = p.pos();
        this.message = p.message();
        this.pending = false;
        this.pendingTicks = 0;
        if (!this.message.contains("이동했습니다")) {
            this.stop();
        }
    }

    private void stop() {
        this.held = this.heldKey = -1;
    }

    public static int parseMillimetres(String text, boolean mm) {
        try {
            BigDecimal n = new BigDecimal(text.strip());
            int value = n.multiply(BigDecimal.valueOf(mm ? 1L : 1000L)).intValueExact();
            return value >= (mm ? 1 : 100) && value <= 1000 ? value : -1;
        } catch (Exception var4) {
            return -1;
        }
    }

    private void move(int d) {
        if (!this.pending) {
            int step = parseMillimetres(this.draft, this.mmMode);
            if (step < 0) {
                this.message = this.mmMode ? "1~1000 사이의 정수 mm를 입력하세요." : "0.1~1칸을 입력하세요. (최소 단위 0.001칸)";
                this.stop();
            } else {
                this.pending = true;
                this.pendingTicks = 0;
                ModNetwork.sendToServer(new DeviceNudgePacket(this.pos, d, step));
            }
        }
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 150;
        int y = this.height / 2 - 110;
        this.addRenderableWidget(
            Button.builder(
                    Component.literal(this.mmMode ? "이동 모드: mm" : "이동 모드: 칸"),
                    b -> {
                        this.stop();
                        int value = parseMillimetres(this.draft, this.mmMode);
                        this.mmMode = !this.mmMode;
                        this.draft = value < 0
                            ? (this.mmMode ? "100" : "0.1")
                            : (this.mmMode ? value + "" : BigDecimal.valueOf(value, 3).stripTrailingZeros().toPlainString());
                        this.rebuildWidgets();
                    }
                )
                .bounds(x, y + 25, 145, 23)
                .build()
        );
        this.amount = new EditBox(this.font, x + 155, y + 25, 145, 23, Component.literal("이동량 직접 입력"));
        this.amount.setMaxLength(12);
        this.amount.setValue(this.draft);
        this.amount.setResponder(v -> {
            this.draft = v;
            this.stop();
        });
        this.addRenderableWidget(this.amount);
        String[] names = new String[]{"↓ 아래 −Y", "↑ 위 +Y", "↥ 북 −Z", "↧ 남 +Z", "← 서 −X", "→ 동 +X"};

        for (int i = 0; i < 6; i++) {
            int d = i;
            this.addRenderableWidget(Button.builder(Component.literal(names[i]), b -> {
                this.setFocused(null);
                this.held = d;
                this.heldKey = -1;
                this.cooldown = 8;
                this.move(d);
            }).bounds(x + i % 2 * 155, y + 87 + i / 2 * 27, 145, 23).build());
        }

        this.addRenderableWidget(Button.builder(Component.literal("완료"), b -> this.onClose()).bounds(x, y + 201, 300, 20).build());
    }

    @Override
    public void tick() {
        super.tick();
        if (this.pending && ++this.pendingTicks > 40) {
            this.pending = false;
            this.stop();
            this.message = "응답을 기다린 뒤 다시 이동하세요.";
        }

        if (this.held >= 0 && --this.cooldown <= 0 && !this.pending) {
            this.cooldown = 3;
            this.move(this.held);
        }
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        this.stop();
        return super.mouseReleased(x, y, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (this.getFocused() == this.amount) {
            return super.keyPressed(key, scan, mods);
        } else {
            int d = switch (key) {
                case 262 -> 5;
                case 263 -> 4;
                case 264 -> 0;
                case 265 -> 1;
                case 266 -> 2;
                case 267 -> 3;
                default -> -1;
            };
            if (d < 0) {
                return super.keyPressed(key, scan, mods);
            } else {
                if (this.held < 0) {
                    this.held = d;
                    this.heldKey = key;
                    this.cooldown = 8;
                    this.move(d);
                }

                return true;
            }
        }
    }

    @Override
    public boolean keyReleased(int key, int scan, int mods) {
        if (key == 257 || key == 335 || key == 32) {
            this.stop();
        }

        if (key == this.heldKey) {
            this.stop();
            return true;
        } else {
            return super.keyReleased(key, scan, mods);
        }
    }

    @Override
    public void removed() {
        this.stop();
        super.removed();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float t) {
        this.renderBackground(g);
        int x = this.width / 2 - 150;
        int y = this.height / 2 - 110;
        g.fill(x - 12, y - 15, x + 312, y + 234, -14669263);
        g.drawString(this.font, "제품 위치 조정 · 이동량 직접 입력", x, y, -1);
        g.drawString(this.font, this.mmMode ? "1~1000mm · 1칸 = 1000mm" : "0.1~1칸 · 예: 0.1 / 0.25 / 1", x, y + 56, -3419175);
        g.drawString(this.font, "화살표 길게 누르기: 연속 이동", x, y + 71, -3419175);
        g.drawString(this.font, this.font.plainSubstrByWidth(this.message, 300), x, y + 172, -3810883);
        g.drawString(this.font, "방향키: 좌우·상하 / PageUp·Down: 북·남", x, y + 186, -5392193);
        super.render(g, mx, my, t);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
