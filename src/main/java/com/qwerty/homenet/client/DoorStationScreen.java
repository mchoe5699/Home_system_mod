package com.qwerty.homenet.client;

import com.qwerty.homenet.blockentity.DoorStationBlockEntity;
import com.qwerty.homenet.intercom.DoorStatus;
import com.qwerty.homenet.intercom.Intercom;
import com.qwerty.homenet.network.DoorStationActionPacket;
import com.qwerty.homenet.network.DoorStationDataPacket;
import com.qwerty.homenet.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class DoorStationScreen extends HomeNetScreen {
    private static final String[] KEYS = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "-", "0", "←"};
    private final BlockPos pos;
    private DoorStationDataPacket data;
    private String dialDraft = "";
    private String msgDraft = "";
    private EditBox dialBox;
    private EditBox msgBox;

    public DoorStationScreen(DoorStationDataPacket data) {
        super(Component.translatable("block.qwertys_homenet.door_station"), 220, 214);
        this.pos = data.pos();
        this.data = data;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public void update(DoorStationDataPacket newData) {
        boolean msgFocused = this.msgBox != null && this.msgBox.isFocused();
        boolean dialFocused = this.dialBox != null && this.dialBox.isFocused();
        this.data = newData;
        this.rebuildWidgets();
        if (msgFocused && this.msgBox != null) {
            this.setFocused(this.msgBox);
        }

        if (dialFocused && this.dialBox != null) {
            this.setFocused(this.dialBox);
        }
    }

    private void send(DoorStationBlockEntity.Action action, String text) {
        ModNetwork.sendToServer(new DoorStationActionPacket(this.pos, action.ordinal(), text));
    }

    @Override
    protected void init() {
        super.init();
        this.dialBox = null;
        this.msgBox = null;
        int cx = this.left + this.panelW / 2;
        int by = this.top + this.panelH - 30;
        if (this.data.inCall()) {
            if (this.data.connected()) {
                this.msgBox = new EditBox(this.font, this.left + 11, by - 25, 136, 18, tr("message", new Object[0]));
                this.msgBox.setMaxLength(80);
                this.msgBox.setValue(this.msgDraft);
                this.msgBox.setResponder(s -> this.msgDraft = s);
                this.msgBox.setHint(tr("message_hint", new Object[0]));
                this.addRenderableWidget(this.msgBox);
                this.addRenderableWidget(Button.builder(tr("send", new Object[0]), b -> this.sendMessage()).bounds(this.left + 152, by - 26, 58, 20).build());
            }

            this.addRenderableWidget(
                Button.builder(tr("hang_up", new Object[0]).withStyle(ChatFormatting.RED), b -> this.send(DoorStationBlockEntity.Action.HANG_UP, ""))
                    .bounds(cx - 60, by, 120, 20)
                    .build()
            );
        } else {
            if (this.data.lobby()) {
                this.dialBox = new EditBox(this.font, cx - 60, this.top + 62, 120, 18, tr("unit", new Object[0]));
                this.dialBox.setMaxLength(16);
                this.dialBox.setValue(this.dialDraft);
                this.dialBox.setResponder(s -> this.dialDraft = s);
                this.dialBox.setHint(tr("dial_hint", new Object[0]));
                this.addRenderableWidget(this.dialBox);
                int kx = cx - 52;
                int ky = this.top + 86;

                for (int i = 0; i < KEYS.length; i++) {
                    String k = KEYS[i];
                    int col = i % 3;
                    int row = i / 3;
                    this.addRenderableWidget(Button.builder(Component.literal(k), b -> this.pressKey(k)).bounds(kx + col * 36, ky + row * 22, 32, 20).build());
                }

                this.addRenderableWidget(
                    Button.builder(tr("call", new Object[0]).withStyle(ChatFormatting.GREEN), b -> this.call()).bounds(cx - 60, by, 120, 20).build()
                );
            } else {
                this.addRenderableWidget(
                    Button.builder(tr("call_unit", new Object[0]).withStyle(ChatFormatting.GREEN), b -> this.call())
                        .bounds(cx - 60, this.top + 110, 120, 24)
                        .build()
                );
            }
        }
    }

    private void pressKey(String k) {
        if (k.equals("←")) {
            if (!this.dialDraft.isEmpty()) {
                this.dialDraft = this.dialDraft.substring(0, this.dialDraft.length() - 1);
            }
        } else if (this.dialDraft.length() < 16) {
            this.dialDraft = this.dialDraft + k;
        }

        if (this.dialBox != null) {
            this.dialBox.setValue(this.dialDraft);
        }
    }

    private void call() {
        this.send(DoorStationBlockEntity.Action.CALL, this.data.lobby() ? this.dialDraft : "");
    }

    private void sendMessage() {
        String text = Intercom.sanitize(this.msgDraft, 80);
        if (!text.isEmpty()) {
            this.send(DoorStationBlockEntity.Action.SEND_MESSAGE, text);
            this.msgDraft = "";
            if (this.msgBox != null) {
                this.msgBox.setValue("");
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        Component header = this.data.lobby()
            ? tr("lobby_title", new Object[0])
            : tr("front_door_title", new Object[]{this.data.linkedUnit().isEmpty() ? "-" : this.data.linkedUnit()});
        this.drawDevice(g, header, null);
        int cx = this.left + this.panelW / 2;
        g.fill(cx - 4, this.top + 26, cx + 4, this.top + 34, -14012616);
        g.fill(cx - 2, this.top + 28, cx + 2, this.top + 32, this.data.inCall() ? -12604939 : -15657701);
        DoorStatus status = DoorStatus.byId(this.data.status());

        int color = switch (status) {
            case CONNECTED, OPENED -> -9510790;
            case CALLING -> -14003;
            case IDLE, ENDED -> -6641994;
            default -> -34182;
        };
        Component statusText = (Component)(status == DoorStatus.IDLE
            ? (this.data.lobby() ? tr("lobby_idle", new Object[0]) : tr("front_door_idle", new Object[0]))
            : status.display(this.data.statusArg()));
        g.drawCenteredString(this.font, statusText, cx, this.top + 44, color);
        if (this.data.inCall() && this.data.connected()) {
            if (this.data.log().isEmpty()) {
                g.drawString(this.font, tr("log_empty", new Object[0]), this.left + 12, this.top + 62, -6641994, false);
            } else {
                WallpadScreen.renderLog(g, this.data.log(), this.left + 12, this.top + 62, this.panelW - 24, 8, this.font);
            }
        } else if (this.data.inCall()) {
            g.drawCenteredString(this.font, tr("waiting_answer", new Object[0]), cx, this.top + 100, -6641994);
        } else if (!this.data.lobby()) {
            g.drawCenteredString(this.font, tr("front_door_hint", new Object[0]), cx, this.top + 80, -6641994);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 257 || key == 335) {
            if (this.msgBox != null && this.msgBox.isFocused()) {
                this.sendMessage();
                return true;
            }

            if (!this.data.inCall()) {
                this.call();
                return true;
            }
        }

        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.msgBox != null) {
            this.msgBox.tick();
        }

        if (this.dialBox != null) {
            this.dialBox.tick();
        }
    }
}
