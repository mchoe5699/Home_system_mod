package com.qwerty.homenet.client;

import com.qwerty.homenet.block.DeviceType;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.intercom.CallState;
import com.qwerty.homenet.intercom.Intercom;
import com.qwerty.homenet.intercom.IntercomLine;
import com.qwerty.homenet.intercom.MissedCall;
import com.qwerty.homenet.network.DeviceEntry;
import com.qwerty.homenet.network.ModNetwork;
import com.qwerty.homenet.network.WallpadActionPacket;
import com.qwerty.homenet.network.WallpadDataPacket;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class WallpadScreen extends HomeNetScreen implements ReceiverScreen {
    private static final int PER_PAGE = 8;
    private final BlockPos pos;
    private WallpadDataPacket data;
    private WallpadScreen.Tab tab = WallpadScreen.Tab.HOME;
    private int page;
    private int refreshTimer;
    private String unitDraft;
    private String msgDraft = "";
    private EditBox unitBox;
    private EditBox pwBox;
    private String pwDraft = "";
    private EditBox msgBox;
    private EditBox dialBox;
    private String dialDraft = "";
    private Component notice;
    private long noticeUntil;

    public WallpadScreen(WallpadDataPacket data) {
        super(Component.translatable("block.qwertys_homenet." + DeviceRegistry.Kind.byId(data.kind()).key()), 280, 196);
        this.pos = data.pos();
        this.data = data;
        this.unitDraft = data.unit();
        if (this.callState() != CallState.IDLE || !this.isWallpad()) {
            this.tab = WallpadScreen.Tab.INTERCOM;
        }
    }

    private boolean isWallpad() {
        return DeviceRegistry.Kind.byId(this.data.kind()) == DeviceRegistry.Kind.WALLPAD;
    }

    private boolean isVideo() {
        DeviceRegistry.Kind k = DeviceRegistry.Kind.byId(this.data.kind());
        return k == DeviceRegistry.Kind.WALLPAD || k == DeviceRegistry.Kind.VIDEO_PHONE || k == DeviceRegistry.Kind.GUARD_CONSOLE;
    }

    @Override
    public BlockPos getPos() {
        return this.pos;
    }

    private CallState callState() {
        return CallState.byId(this.data.callState());
    }

    @Override
    public void update(WallpadDataPacket newData) {
        CallState before = this.callState();
        boolean unitChanged = !newData.unit().equals(this.data.unit());
        boolean msgFocused = this.msgBox != null && this.msgBox.isFocused();
        boolean unitFocused = this.unitBox != null && this.unitBox.isFocused();
        boolean dialFocused = this.dialBox != null && this.dialBox.isFocused();
        if (!newData.notice().isEmpty()) {
            this.notice = Component.translatable("gui.qwertys_homenet.wp.notice." + newData.notice());
            this.noticeUntil = System.currentTimeMillis() + 4000L;
        }

        this.data = newData;
        if (unitChanged && !unitFocused) {
            this.unitDraft = newData.unit();
        }

        if (this.callState() == CallState.RINGING && before != CallState.RINGING) {
            this.tab = WallpadScreen.Tab.INTERCOM;
        }

        this.rebuildWidgets();
        if (msgFocused && this.msgBox != null) {
            this.setFocused(this.msgBox);
        }

        if (unitFocused && this.unitBox != null) {
            this.setFocused(this.unitBox);
        }

        if (dialFocused && this.dialBox != null) {
            this.setFocused(this.dialBox);
        }
    }

    private boolean isGuard() {
        return DeviceRegistry.Kind.byId(this.data.kind()) == DeviceRegistry.Kind.GUARD_CONSOLE;
    }

    private boolean fromReceiver() {
        return this.data.outgoing() || Intercom.isReceiverKey(this.data.callerKey());
    }

    private Component peerName() {
        if (this.data.peer().startsWith("#guard:")) {
            return Component.translatable("caller.qwertys_homenet.guard_no", this.data.peer().substring(7));
        } else {
            String var1 = this.data.peer();

            return switch (var1) {
                case "#guard" -> Component.translatable("caller.qwertys_homenet.guard");
                case "#office" -> Component.translatable("caller.qwertys_homenet.office");
                default -> Component.literal(this.data.peer());
            };
        }
    }

    private void dial() {
        String u = this.dialDraft == null ? "" : this.dialDraft.trim();
        if (!u.isEmpty()) {
            this.send(ReceiverBlockEntity.Action.DIAL, BlockPos.ZERO, u);
        }
    }

    private void send(ReceiverBlockEntity.Action action) {
        ModNetwork.sendToServer(new WallpadActionPacket(this.pos, action));
    }

    private void send(ReceiverBlockEntity.Action action, BlockPos target, String text) {
        ModNetwork.sendToServer(new WallpadActionPacket(this.pos, action.ordinal(), target, text));
    }

    @Override
    protected void init() {
        super.init();
        this.unitBox = null;
        this.pwBox = null;
        this.msgBox = null;
        this.dialBox = null;
        WallpadScreen.Tab[] tabs = this.isWallpad()
            ? WallpadScreen.Tab.values()
            : new WallpadScreen.Tab[]{WallpadScreen.Tab.INTERCOM, WallpadScreen.Tab.SETTINGS};
        if (!this.isWallpad() && this.tab == WallpadScreen.Tab.HOME) {
            this.tab = WallpadScreen.Tab.INTERCOM;
        }

        for (int i = 0; i < tabs.length; i++) {
            WallpadScreen.Tab t = tabs[i];
            MutableComponent label = tr("tab." + t.name().toLowerCase(Locale.ROOT), new Object[0]);
            if (t == WallpadScreen.Tab.INTERCOM && (this.callState() != CallState.IDLE || !this.data.missed().isEmpty())) {
                label = label.append(Component.literal(" ●").withStyle(this.callState() == CallState.RINGING ? ChatFormatting.RED : ChatFormatting.YELLOW));
            }

            Button b = Button.builder(label, btn -> {
                this.tab = t;
                this.rebuildWidgets();
            }).bounds(this.left + 10 + i * 88, this.top + 25, 84, 18).build();
            b.active = this.tab != t;
            this.addRenderableWidget(b);
        }

        switch (this.tab) {
            case HOME:
                this.initHome();
                break;
            case INTERCOM:
                this.initIntercom();
                break;
            case SETTINGS:
                this.initSettings();
        }
    }

    private void initHome() {
        List<DeviceEntry> devices = this.data.devices();
        int pages = Math.max(1, (devices.size() + 8 - 1) / 8);
        this.page = Math.min(this.page, pages - 1);

        for (int i = 0; i < 8; i++) {
            int idx = this.page * 8 + i;
            if (idx >= devices.size()) {
                break;
            }

            DeviceEntry e = devices.get(idx);
            int col = i % 2;
            int row = i / 2;
            Button b = Button.builder(this.deviceLabel(e), btn -> this.send(ReceiverBlockEntity.Action.TOGGLE, e.pos(), ""))
                .bounds(this.left + 10 + col * 132, this.top + 50 + row * 24, 128, 20)
                .build();
            b.active = e.online();
            this.addRenderableWidget(b);
        }

        int by = this.top + this.panelH - 30;
        Button prev = Button.builder(Component.literal("◀"), bx -> {
            this.page--;
            this.rebuildWidgets();
        }).bounds(this.left + 10, by, 20, 20).build();
        prev.active = this.page > 0;
        this.addRenderableWidget(prev);
        Button next = Button.builder(Component.literal("▶"), bx -> {
            this.page++;
            this.rebuildWidgets();
        }).bounds(this.left + 66, by, 20, 20).build();
        next.active = this.page < pages - 1;
        this.addRenderableWidget(next);
        this.addRenderableWidget(
            Button.builder(tr("all_lights_off", new Object[0]), bx -> this.send(ReceiverBlockEntity.Action.ALL_LIGHTS_OFF))
                .bounds(this.left + this.panelW - 180, by, 84, 20)
                .build()
        );
        this.addRenderableWidget(
            Button.builder(tr("all_off", new Object[0]), bx -> this.send(ReceiverBlockEntity.Action.ALL_OFF))
                .bounds(this.left + this.panelW - 92, by, 82, 20)
                .build()
        );
    }

    private Component deviceLabel(DeviceEntry e) {
        if (!e.online()) {
            return Component.literal("○ ").append(tr("offline", new Object[0])).withStyle(ChatFormatting.DARK_GRAY);
        } else {
            DeviceType type = DeviceType.byId(e.type());
            Component name = (Component)(e.name().isEmpty() ? type.displayName() : Component.literal(e.name()));
            return Component.literal(e.on() ? "● " : "○ ")
                .withStyle(e.on() ? ChatFormatting.GREEN : ChatFormatting.GRAY)
                .append(name.copy().withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" · ").withStyle(ChatFormatting.GRAY))
                .append(type.stateName(e.on()).copy().withStyle(e.on() ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        }
    }

    private void initIntercom() {
        int by = this.top + this.panelH - 30;
        switch (this.callState()) {
            case RINGING: {
                this.addRenderableWidget(
                    Button.builder(tr("answer", new Object[0]).withStyle(ChatFormatting.GREEN), b -> this.send(ReceiverBlockEntity.Action.ANSWER))
                        .bounds(this.left + 10, by, 84, 20)
                        .build()
                );
                Button open = Button.builder(
                        tr("open_door", new Object[0]).withStyle(ChatFormatting.AQUA), b -> this.send(ReceiverBlockEntity.Action.OPEN_DOOR)
                    )
                    .bounds(this.left + 98, by, 84, 20)
                    .build();
                open.active = !this.fromReceiver();
                this.addRenderableWidget(open);
                this.addRenderableWidget(
                    Button.builder(tr("reject", new Object[0]).withStyle(ChatFormatting.RED), b -> this.send(ReceiverBlockEntity.Action.HANG_UP))
                        .bounds(this.left + 186, by, 84, 20)
                        .build()
                );
                break;
            }
            case DIALING:
                this.addRenderableWidget(
                    Button.builder(tr("cancel_call", new Object[0]).withStyle(ChatFormatting.RED), b -> this.send(ReceiverBlockEntity.Action.HANG_UP))
                        .bounds(this.left + 10, by, 260, 20)
                        .build()
                );
                break;
            case CONNECTED: {
                this.msgBox = new EditBox(this.font, this.left + 11, by - 25, 186, 18, tr("message", new Object[0]));
                this.msgBox.setMaxLength(80);
                this.msgBox.setValue(this.msgDraft);
                this.msgBox.setResponder(s -> this.msgDraft = s);
                this.msgBox.setHint(tr("message_hint", new Object[0]));
                this.addRenderableWidget(this.msgBox);
                this.addRenderableWidget(Button.builder(tr("send", new Object[0]), b -> this.sendMessage()).bounds(this.left + 202, by - 26, 68, 20).build());
                Button open = Button.builder(
                        tr("open_door", new Object[0]).withStyle(ChatFormatting.AQUA), b -> this.send(ReceiverBlockEntity.Action.OPEN_DOOR)
                    )
                    .bounds(this.left + 10, by, 128, 20)
                    .build();
                open.active = !this.fromReceiver();
                this.addRenderableWidget(open);
                this.addRenderableWidget(
                    Button.builder(tr("hang_up", new Object[0]).withStyle(ChatFormatting.RED), b -> this.send(ReceiverBlockEntity.Action.HANG_UP))
                        .bounds(this.left + 142, by, 128, 20)
                        .build()
                );
                break;
            }
            default:
                this.dialBox = new EditBox(this.font, this.left + 11, by - 25, 150, 18, tr("dial", new Object[0]));
                this.dialBox.setMaxLength(16);
                this.dialBox.setValue(this.dialDraft);
                this.dialBox.setResponder(v -> this.dialDraft = v);
                this.dialBox.setHint(tr("dial_hint", new Object[0]));
                this.addRenderableWidget(this.dialBox);
                this.addRenderableWidget(
                    Button.builder(tr("dial_call", new Object[0]).withStyle(ChatFormatting.GREEN), b -> this.dial())
                        .bounds(this.left + 166, by - 26, 104, 20)
                        .build()
                );
                if (this.isGuard()) {
                    this.addRenderableWidget(
                        Button.builder(tr("call_other_guard", new Object[0]), b -> this.send(ReceiverBlockEntity.Action.CALL_GUARD, BlockPos.ZERO, "guard"))
                            .bounds(this.left + 10, by, 84, 20)
                            .build()
                    );
                } else {
                    this.addRenderableWidget(
                        Button.builder(
                                tr("call_guard", new Object[0]).withStyle(ChatFormatting.AQUA),
                                b -> this.send(ReceiverBlockEntity.Action.CALL_GUARD, BlockPos.ZERO, "guard")
                            )
                            .bounds(this.left + 10, by, 84, 20)
                            .build()
                    );
                    this.addRenderableWidget(
                        Button.builder(tr("call_office", new Object[0]), b -> this.send(ReceiverBlockEntity.Action.CALL_GUARD, BlockPos.ZERO, "office"))
                            .bounds(this.left + 98, by, 72, 20)
                            .build()
                    );
                }

                Button clear = Button.builder(tr("clear_missed", new Object[0]), b -> this.send(ReceiverBlockEntity.Action.CLEAR_MISSED))
                    .bounds(this.left + this.panelW - 96, by, 86, 20)
                    .build();
                clear.active = !this.data.missed().isEmpty();
                this.addRenderableWidget(clear);
        }
    }

    private void sendMessage() {
        String text = Intercom.sanitize(this.msgDraft, 80);
        if (!text.isEmpty()) {
            this.send(ReceiverBlockEntity.Action.SEND_MESSAGE, BlockPos.ZERO, text);
            this.msgDraft = "";
            if (this.msgBox != null) {
                this.msgBox.setValue("");
            }
        }
    }

    private void initSettings() {
        this.unitBox = new EditBox(this.font, this.left + 12, this.top + 66, 170, 18, tr("unit", new Object[0]));
        this.unitBox.setMaxLength(16);
        this.unitBox.setValue(this.unitDraft == null ? "" : this.unitDraft);
        this.unitBox.setResponder(s -> this.unitDraft = s);
        this.unitBox.setHint(tr("unit_hint", new Object[0]));
        this.addRenderableWidget(this.unitBox);
        this.addRenderableWidget(
            Button.builder(
                    tr("save", new Object[0]), b -> this.send(ReceiverBlockEntity.Action.SET_UNIT, BlockPos.ZERO, this.unitDraft == null ? "" : this.unitDraft)
                )
                .bounds(this.left + 190, this.top + 65, 80, 20)
                .build()
        );
        this.pwBox = new EditBox(this.font, this.left + 12, this.top + 104, 170, 18, tr("door_pw", new Object[0]));
        this.pwBox.setMaxLength(4);
        this.pwBox.setFilter(s -> s.matches("\\d*"));
        this.pwBox.setValue(this.pwDraft);
        this.pwBox.setResponder(s -> this.pwDraft = s);
        this.pwBox.setHint(this.data.hasDoorPassword() ? tr("door_pw_hint_set", new Object[0]) : tr("door_pw_hint", new Object[0]));
        this.addRenderableWidget(this.pwBox);
        this.addRenderableWidget(Button.builder(tr("save", new Object[0]), b -> this.savePassword()).bounds(this.left + 190, this.top + 103, 80, 20).build());
    }

    private void savePassword() {
        this.send(ReceiverBlockEntity.Action.SET_DOOR_PASSWORD, BlockPos.ZERO, this.pwDraft);
        this.pwDraft = "";
        if (this.pwBox != null) {
            this.pwBox.setValue("");
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        Component header = this.data.unit().isEmpty() ? tr("unit_unset", new Object[0]) : tr("unit_header", new Object[]{this.data.unit()});
        this.drawDevice(g, header, this.clockAndWeather());
        switch (this.tab) {
            case HOME:
                this.renderHome(g);
                break;
            case INTERCOM:
                this.renderIntercom(g);
                break;
            case SETTINGS:
                this.renderSettings(g);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderHome(GuiGraphics g) {
        List<DeviceEntry> devices = this.data.devices();
        if (devices.isEmpty()) {
            g.drawCenteredString(this.font, tr("no_devices", new Object[0]), this.left + this.panelW / 2, this.top + 80, -6641994);
            g.drawCenteredString(this.font, tr("no_devices_hint", new Object[0]), this.left + this.panelW / 2, this.top + 94, -6641994);
        }

        int pages = Math.max(1, (devices.size() + 8 - 1) / 8);
        g.drawCenteredString(this.font, this.page + 1 + "/" + pages, this.left + 48, this.top + this.panelH - 24, -6641994);
    }

    private void renderIntercom(GuiGraphics g) {
        int cx = this.left + this.panelW / 2;
        Component caller = this.data.outgoing() ? this.peerName() : Intercom.sideName(this.data.callerKey());
        if (this.notice != null && System.currentTimeMillis() < this.noticeUntil) {
            g.drawCenteredString(this.font, this.notice, cx, this.top + 26 + 18 + 4, -14003);
        }

        switch (this.callState()) {
            case RINGING:
                boolean blink = System.currentTimeMillis() / 400L % 2L == 0L;
                g.fill(this.left + 20, this.top + 60, this.left + this.panelW - 20, this.top + 120, blink ? -14008486 : -14733240);
                int tx = cx;
                if (this.isVideo() && !this.fromReceiver()) {
                    this.drawCameraView(g, this.left + this.panelW - 86, this.top + 68);
                    tx = this.left + (this.panelW - 92) / 2 + 10;
                }

                g.drawCenteredString(this.font, tr("ringing_title", new Object[0]), tx, this.top + 72, -14003);
                g.drawCenteredString(this.font, tr("ringing_from", new Object[]{caller}), tx, this.top + 92, -1);
                g.drawCenteredString(this.font, tr("ringing_hint", new Object[0]), cx, this.top + 132, -6641994);
                break;
            case DIALING:
                boolean dialingBlink = System.currentTimeMillis() / 400L % 2L == 0L;
                g.fill(this.left + 20, this.top + 60, this.left + this.panelW - 20, this.top + 120, dialingBlink ? -14008486 : -14733240);
                g.drawCenteredString(this.font, tr("dialing_title", new Object[0]), cx, this.top + 72, -14003);
                g.drawCenteredString(this.font, tr("dialing_to", new Object[]{caller}), cx, this.top + 92, -1);
                break;
            case CONNECTED:
                g.drawString(this.font, tr("connected_with", new Object[]{caller}), this.left + 12, this.top + 50, -9510790, false);
                this.renderLog(g, this.data.log(), this.left + 12, this.top + 64, this.panelW - 24, 6);
                break;
            default:
                g.drawString(this.font, tr("idle", new Object[0]), this.left + 12, this.top + 50, -6641994, false);
                g.drawString(this.font, tr("missed_title", new Object[0]), this.left + 12, this.top + 66, -1, false);
                List<MissedCall> missed = this.data.missed();
                if (missed.isEmpty()) {
                    g.drawString(this.font, tr("missed_none", new Object[0]), this.left + 20, this.top + 78, -6641994, false);
                }

                g.drawString(
                    this.font,
                    this.isGuard() ? tr("dial_label_guard", new Object[0]) : tr("dial_label", new Object[0]),
                    this.left + 12,
                    this.top + this.panelH - 66,
                    -1,
                    false
                );

                for (int i = 0; i < Math.min(missed.size(), 4); i++) {
                    MissedCall m = missed.get(i);
                    Component line = Component.literal("• ")
                        .append(Intercom.sideName(m.caller()))
                        .append("   ")
                        .append(tr("day_time", new Object[]{day(m.dayTime()), clock(m.dayTime())}));
                    g.drawString(this.font, line, this.left + 20, this.top + 78 + i * 11, m.caller().startsWith("emergency:") ? -40864 : -14003, false);
                }
        }
    }

    private void drawCameraView(GuiGraphics g, int x, int y) {
        int w = 60;
        int h = 44;
        g.fill(x, y, x + w, y + h, -15723496);
        g.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, -10853264, -14012360);
        g.fill(x + 24, y + 9, x + 36, y + 22, -15065564);
        g.fill(x + 15, y + 24, x + 45, y + h - 1, -15065564);
        g.fill(x + 26, y + 22, x + 34, y + 25, -15065564);
        boolean rec = System.currentTimeMillis() / 500L % 2L == 0L;
        if (rec) {
            g.fill(x + 3, y + 3, x + 6, y + 6, -2080704);
        }

        g.drawString(this.font, "CAM", x + 8, y + 2, -3353376, false);
    }

    static void renderLog(GuiGraphics g, List<IntercomLine> log, int x, int y, int w, int maxLines, Font font) {
        int start = Math.max(0, log.size() - maxLines);

        for (int i = start; i < log.size(); i++) {
            IntercomLine l = log.get(i);
            Component c = Component.literal("[")
                .append(Intercom.sideName(l.side()))
                .append("] ")
                .withStyle(ChatFormatting.AQUA)
                .append(Component.literal(l.name() + ": ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(l.text()).withStyle(ChatFormatting.WHITE));
            g.drawString(font, Language.getInstance().getVisualOrder(font.substrByWidth(c, w)), x, y + (i - start) * 11, -1, false);
        }
    }

    private void renderLog(GuiGraphics g, List<IntercomLine> log, int x, int y, int w, int maxLines) {
        if (log.isEmpty()) {
            g.drawString(this.font, tr("log_empty", new Object[0]), x, y, -6641994, false);
        } else {
            renderLog(g, log, x, y, w, maxLines, this.font);
        }
    }

    private void renderSettings(GuiGraphics g) {
        g.drawString(this.font, tr("unit_label", new Object[0]), this.left + 12, this.top + 54, -1, false);
        g.drawString(this.font, tr("door_pw_label", new Object[0]), this.left + 12, this.top + 92, -1, false);
        g.drawString(this.font, tr("settings_help_1", new Object[0]), this.left + 12, this.top + 130, -6641994, false);
        g.drawString(this.font, tr("settings_help_2", new Object[0]), this.left + 12, this.top + 141, -6641994, false);
        g.drawString(this.font, tr("settings_help_3", new Object[0]), this.left + 12, this.top + 152, -6641994, false);
        g.drawString(this.font, tr("settings_help_4", new Object[0]), this.left + 12, this.top + 163, -6641994, false);
        g.drawString(this.font, tr("device_count", new Object[]{this.data.devices().size()}), this.left + 12, this.top + 177, -6641994, false);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 257 || key == 335) {
            if (this.msgBox != null && this.msgBox.isFocused()) {
                this.sendMessage();
                return true;
            }

            if (this.dialBox != null && this.dialBox.isFocused()) {
                this.dial();
                return true;
            }

            if (this.pwBox != null && this.pwBox.isFocused()) {
                this.savePassword();
                return true;
            }

            if (this.unitBox != null && this.unitBox.isFocused()) {
                this.send(ReceiverBlockEntity.Action.SET_UNIT, BlockPos.ZERO, this.unitDraft == null ? "" : this.unitDraft);
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

        if (this.unitBox != null) {
            this.unitBox.tick();
        }

        if (this.pwBox != null) {
            this.pwBox.tick();
        }

        if (this.tab == WallpadScreen.Tab.HOME && ++this.refreshTimer >= 40) {
            this.refreshTimer = 0;
            this.send(ReceiverBlockEntity.Action.REFRESH);
        }
    }

    private static enum Tab {
        HOME,
        INTERCOM,
        SETTINGS;
    }
}
