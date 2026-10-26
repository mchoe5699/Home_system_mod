package com.qwerty.homenet.client.guard;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.qwerty.homenet.HomeNet;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.client.ReceiverScreen;
import com.qwerty.homenet.intercom.CallState;
import com.qwerty.homenet.intercom.Intercom;
import com.qwerty.homenet.intercom.IntercomLine;
import com.qwerty.homenet.intercom.MissedCall;
import com.qwerty.homenet.network.ModNetwork;
import com.qwerty.homenet.network.WallpadActionPacket;
import com.qwerty.homenet.network.WallpadDataPacket;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

public class GuardMasterScreen extends Screen implements ReceiverScreen {
    private static final ResourceLocation BODY = HomeNet.id("textures/gui/guard_master_body.png");
    private static final ResourceLocation HANDSET = HomeNet.id("textures/gui/guard_master_handset.png");
    private static final ResourceLocation BG = HomeNet.id("textures/gui/wallpad_bg.png");
    private static final ResourceLocation BG_SUB = HomeNet.id("textures/gui/wallpad_bg_sub.png");
    private static final ResourceLocation ICONS = HomeNet.id("textures/gui/wallpad_icons.png");
    private static final ResourceLocation CAMS = HomeNet.id("textures/gui/wallpad_cams.png");
    private static final int BW = 580;
    private static final int BH = 510;
    private static final int SX = 187;
    private static final int SY = 36;
    private static final int SW = 353;
    private static final int SH = 199;
    private static final int W = 400;
    private static final int H = 225;
    private static final float K = 0.8825F;
    private static final int HX = 8;
    private static final int HY = 6;
    private static final int HW = 118;
    private static final int HH = 470;
    private static final int[] KEY_X = new int[]{213, 266, 319, 378};
    private static final int[] KEY_Y = new int[]{300, 336, 372, 408};
    private static final String[][] KEYS = new String[][]{{"1", "2", "3", "세대"}, {"4", "5", "6", "경비"}, {"7", "8", "9", "로비"}, {"*", "0", "#", "문열림"}};
    private static final int JOG_X = 478;
    private static final int JOG_Y = 358;
    private static final int JOG_R = 58;
    private static final int JOG_IN = 20;
    private static final int[] FN_X = new int[]{240, 290, 342, 404};
    private static final int FN_Y = 262;
    private static final int IC_SECURITY = 0;
    private static final int IC_CALL = 2;
    private static final int IC_INQUIRY = 3;
    private static final int IC_SETTINGS = 4;
    private static final int IC_SUN = 6;
    private static final int IC_CLOUD = 7;
    private static final int IC_RAIN = 8;
    private static final int IC_SNOW = 9;
    private static final int IC_WIFI = 10;
    private static final int IC_NOTICE = 12;
    private static final int IC_HOME = 14;
    private static final int IC_HELP = 15;
    private static final int IC_SHIELD = 16;
    private static final int IC_PHONE = 18;
    private static final int IC_MAGNIFIER = 19;
    private static final int IC_GEAR = 21;
    private static final int IC_SENSOR_DOOR = 27;
    private static final int IC_SENSOR_WINDOW = 28;
    private static final int IC_GAS = 29;
    private static final int IC_SPEAKER = 30;
    private static final int IC_LOCK = 32;
    private static final int CAM_DOOR = 0;
    private static final int CAM_LOBBY = 1;
    private static final int CAM_GUARD = 2;
    private static final int CAM_NEIGHBOR = 3;
    private static final int WHITE = -1;
    private static final int DARK = -14012616;
    private static final int DIM = -9735296;
    private static final int BLUE = -13722661;
    private static final int RED = -1750977;
    private static final int GREEN = -11746454;
    private static final int YELLOW = -11686;
    private static final GuardMasterScreen.Page[] MENU = new GuardMasterScreen.Page[]{
        GuardMasterScreen.Page.SECURITY, GuardMasterScreen.Page.CALL, GuardMasterScreen.Page.INQUIRY, GuardMasterScreen.Page.SETTINGS
    };
    private static final int[] MENU_ICON = new int[]{16, 18, 19, 21};
    private static final String[] MENU_KEY = new String[]{"security", "call", "inquiry", "settings"};
    private static final String[] SEC_TABS = new String[]{"alarm", "list"};
    private static final String[] CALL_TABS = new String[]{"unit", "guard", "lobby", "phone"};
    private static final String[] INQ_TABS = new String[]{"absent", "bypass", "safety", "parcel"};
    private static final String[] SET_TABS = new String[]{"sound", "password", "touch", "admin"};
    private final BlockPos pos;
    private WallpadDataPacket data;
    private GuardMasterScreen.Page page = GuardMasterScreen.Page.MAIN;
    private final Map<GuardMasterScreen.Page, Integer> tabs = new LinkedHashMap<>();
    private GuiGraphics g;
    private float s;
    private int bx;
    private int by;
    private int mouseSX = -1;
    private int mouseSY = -1;
    private final List<GuardMasterScreen.Hot> hots = new ArrayList<>();
    private Consumer<String> keyInput;
    private Runnable confirmAction;
    private GuardMasterScreen.Popup popup;
    private GuardMasterScreen.Keypad keypad;
    private int calYear;
    private int calMonth;
    private String input = "";
    private int listPage;
    private long connectedSince;
    private int touchStep;
    private boolean pwUnlocked;
    private boolean adminUnlocked;
    private String pwAuth = "";
    private String pendingAuth = "";
    private String pw1 = "";
    private String pw2 = "";
    private int fieldFocus;
    private String adminAddr = "";
    private final Map<String, String> draft = new LinkedHashMap<>();
    private long keyFlashUntil;
    private String keyFlash = "";
    private float jogAngle;
    private int refreshTimer;
    private EditBox msgBox;
    private String msgDraft = "";
    private static final int CX = 80;
    private static final int CY = 38;
    private static final int CW = 315;
    private static final int CH = 183;
    private int parcelFilter;
    private static final int[][] TOUCH_POINTS = new int[][]{{30, 30}, {370, 30}, {370, 195}, {30, 195}, {200, 112}};

    public GuardMasterScreen(WallpadDataPacket data) {
        super(Component.translatable("block.qwertys_homenet.guard_master"));
        this.pos = data.pos();
        this.data = data;
        LocalDate now = LocalDate.now();
        this.calYear = now.getYear();
        this.calMonth = now.getMonthValue();
        if (this.callState() != CallState.IDLE) {
            this.page = GuardMasterScreen.Page.CALL;
            this.tabs.put(GuardMasterScreen.Page.CALL, tabForKey(data.outgoing() ? data.peer() : this.key()));
            if (this.callState() == CallState.CONNECTED) {
                this.connectedSince = System.currentTimeMillis();
            }
        }
    }

    @Override
    public BlockPos getPos() {
        return this.pos;
    }

    @Override
    public void update(WallpadDataPacket p) {
        CallState before = this.callState();
        this.data = p;
        CallState now = this.callState();
        if (now == CallState.CONNECTED && before != CallState.CONNECTED) {
            this.connectedSince = System.currentTimeMillis();
        }

        if (now == CallState.RINGING && before != CallState.RINGING) {
            this.popup = null;
            this.keypad = null;
            this.page = GuardMasterScreen.Page.CALL;
            this.tabs.put(GuardMasterScreen.Page.CALL, tabForKey(this.key()));
        }

        if (now == CallState.CONNECTED && before == CallState.IDLE) {
            this.page = GuardMasterScreen.Page.CALL;
            this.tabs.put(GuardMasterScreen.Page.CALL, tabForKey(this.data.outgoing() ? this.data.peer() : this.key()));
        }

        if (!p.notice().isEmpty()) {
            this.handleNotice(p.notice());
        }
    }

    private static int tabForKey(String k) {
        if (k.startsWith("guard:") || k.startsWith("#guard") || k.equals("#office")) {
            return 1;
        } else {
            return !k.equals("lobby") && !k.equals("front_door") && !k.startsWith("#lobby") ? 0 : 2;
        }
    }

    private CallState callState() {
        return CallState.byId(this.data.callState());
    }

    private String key() {
        return this.data.callerKey();
    }

    private boolean incoming() {
        return this.callState() != CallState.IDLE && !this.data.outgoing();
    }

    private void send(ReceiverBlockEntity.Action a) {
        ModNetwork.sendToServer(new WallpadActionPacket(this.pos, a));
    }

    private void send(ReceiverBlockEntity.Action a, String text) {
        ModNetwork.sendToServer(new WallpadActionPacket(this.pos, a.ordinal(), BlockPos.ZERO, text));
    }

    private static String t(String key, Object... args) {
        return I18n.get("gui.qwertys_homenet.gm." + key, args);
    }

    private static String wp(String key, Object... args) {
        return I18n.get("gui.qwertys_homenet.wp." + key, args);
    }

    private int tab(GuardMasterScreen.Page p) {
        return this.tabs.getOrDefault(p, 0);
    }

    private void go(GuardMasterScreen.Page p) {
        if (this.page != p) {
            this.page = p;
            this.input = "";
            this.listPage = 0;
            this.draft.clear();
            if (p == GuardMasterScreen.Page.SETTINGS) {
                this.onSettingsTab(this.tab(GuardMasterScreen.Page.SETTINGS));
            }
        }
    }

    private void setTab(GuardMasterScreen.Page p, int i) {
        this.tabs.put(p, i);
        this.input = "";
        this.listPage = 0;
        if (p == GuardMasterScreen.Page.SETTINGS) {
            this.onSettingsTab(i);
        }
    }

    private void handleNotice(String n) {
        switch (n) {
            case "unlock_pw":
                this.pwUnlocked = true;
                this.pwAuth = this.pendingAuth;
                this.keypad = null;
                return;
            case "unlock_admin":
                this.adminUnlocked = true;
                this.keypad = null;
                this.adminAddr = digits(this.data.unit());
                return;
            case "pw_saved":
                this.pwAuth = this.pw1;
                this.pw1 = "";
                this.pw2 = "";
                break;
            case "unlock_fail":
                this.keypad = null;
        }

        String k = "gui.qwertys_homenet.gm.notice." + n;
        this.info(I18n.exists(k) ? I18n.get(k) : wp("notice." + n));
    }

    private void info(String text) {
        this.popup = new GuardMasterScreen.Popup(Arrays.asList(text.split("\n")), wp("ok"), null, null);
    }

    private void confirm(String text, Runnable onOk) {
        this.popup = new GuardMasterScreen.Popup(Arrays.asList(text.split("\n")), wp("ok"), wp("cancel"), onOk);
    }

    @Override
    protected void init() {
        this.layout();
        this.msgBox = new EditBox(
            this.font, this.width / 2 - 110, Math.min(this.height - 22, this.by + Math.round(510.0F * this.s) + 4), 170, 16, Component.literal("")
        );
        this.msgBox.setMaxLength(80);
        this.msgBox.setValue(this.msgDraft);
        this.msgBox.setResponder(v -> this.msgDraft = v);
        this.msgBox.setHint(Component.literal(wp("message_hint")));
        this.addRenderableWidget(this.msgBox);
        this.addRenderableWidget(
            Button.builder(Component.literal(wp("send")), b -> this.sendMessage()).bounds(this.width / 2 + 64, this.msgBox.getY() - 2, 46, 20).build()
        );
    }

    private void layout() {
        int availH = this.height - 34;
        this.s = Math.min((this.width - 16) / 580.0F, availH / 510.0F);
        this.s = Math.min(this.s, 1.5F);
        this.bx = Math.round((this.width - 580.0F * this.s) / 2.0F);
        this.by = Math.max(4, Math.round((availH - 510.0F * this.s) / 2.0F));
    }

    private void sendMessage() {
        String text = Intercom.sanitize(this.msgDraft, 80);
        if (!text.isEmpty() && this.callState() == CallState.CONNECTED) {
            this.send(ReceiverBlockEntity.Action.SEND_MESSAGE, text);
            this.msgDraft = "";
            this.msgBox.setValue("");
        }
    }

    private void fill(int x, int y, int w, int h, int c) {
        this.g.fill(x, y, x + w, y + h, c);
    }

    private void frame(int x, int y, int w, int h, int c) {
        this.g.fill(x, y, x + w, y + 1, c);
        this.g.fill(x, y + h - 1, x + w, y + h, c);
        this.g.fill(x, y, x + 1, y + h, c);
        this.g.fill(x + w - 1, y, x + w, y + h, c);
    }

    private void text(String str, float x, float y, int color, float scale) {
        PoseStack ps = this.g.pose();
        ps.pushPose();
        ps.translate(x, y, 0.0F);
        ps.scale(scale, scale, 1.0F);
        this.g.drawString(this.font, str, 0, 0, color, false);
        ps.popPose();
    }

    private void shadowText(String str, float x, float y, int color, float scale) {
        PoseStack ps = this.g.pose();
        ps.pushPose();
        ps.translate(x, y, 0.0F);
        ps.scale(scale, scale, 1.0F);
        this.g.drawString(this.font, str, 0, 0, color, true);
        ps.popPose();
    }

    private float width(String str, float scale) {
        return this.font.width(str) * scale;
    }

    private void textC(String str, float cx, float y, int color, float scale) {
        this.text(str, cx - this.width(str, scale) / 2.0F, y, color, scale);
    }

    private void textFit(String str, float cx, float y, int color, float scale, float maxW) {
        float sc = scale;
        float w = this.width(str, scale);
        if (w > maxW) {
            sc = scale * maxW / w;
        }

        this.textC(str, cx, y + (scale - sc) * 4.0F, color, sc);
    }

    private void icon(int idx, int x, int y, int size) {
        this.g.blit(ICONS, x, y, size, size, idx % 8 * 128, idx / 8 * 128, 128, 128, 1024, 1024);
    }

    private void icon(int idx, int x, int y, int size, int color) {
        RenderSystem.setShaderColor((color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, (color >>> 24 & 0xFF) / 255.0F);
        this.icon(idx, x, y, size);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void cam(int idx, int x, int y, int w, int h) {
        this.g.blit(CAMS, x, y, w, h, idx % 2 * 512, idx / 2 * 288, 480, 270, 1024, 1024);
    }

    private void hot(int x, int y, int w, int h, Runnable r) {
        this.hots.add(new GuardMasterScreen.Hot(x, y, w, h, r));
    }

    private boolean hover(int x, int y, int w, int h) {
        return this.mouseSX >= x && this.mouseSX < x + w && this.mouseSY >= y && this.mouseSY < y + h;
    }

    private void button(int x, int y, int w, int h, String label, boolean selected, boolean enabled, Runnable action) {
        boolean hv = enabled && this.hover(x, y, w, h);
        int top = selected ? -12077328 : (enabled ? (hv ? -854793 : -1775636) : -3551789);
        int bottom = selected ? -14711095 : (enabled ? (hv ? -3551530 : -4603960) : -4998977);
        this.g.fillGradient(x, y, x + w, y + h, top, bottom);
        this.frame(x, y, w, h, selected ? -15308900 : -7564387);
        this.fill(x + 1, y + 1, w - 2, 1, selected ? 1442840575 : -1711276033);
        this.textFit(label, x + w / 2.0F, y + (h - 6) / 2.0F, selected ? -1 : (enabled ? -14012616 : -7433060), 0.75F, w - 4);
        if (enabled) {
            this.hot(x, y, w, h, action);
        }
    }

    private void tabRow(String group, String[] keys, int selected, Consumer<Integer> onSelect) {
        int x = 79;
        int y = 20;
        int w = 317;
        this.fill(x, y, w, 16, -1070319036);
        int tw = w / keys.length;

        for (int i = 0; i < keys.length; i++) {
            int tx = x + i * tw;
            int idx = i;
            boolean sel = i == selected;
            if (sel) {
                this.g.fillGradient(tx + 2, y + 2, tx + tw - 2, y + 14, -11747851, -14710321);
            }

            this.textFit(t(group + "." + keys[i]), tx + tw / 2.0F, y + 5, sel ? -1 : -2630429, 0.7F, tw - 6);
            this.hot(tx, y, tw, 16, () -> onSelect.accept(idx));
        }
    }

    private void panel(int x, int y, int w, int h) {
        this.fill(x, y, w, h, -1930103047);
        this.frame(x, y, w, h, -1056964609);
    }

    private void darkPanel(int x, int y, int w, int h) {
        this.fill(x, y, w, h, -1339543496);
        this.frame(x, y, w, h, -10919314);
    }

    private boolean blink() {
        return System.currentTimeMillis() / 400L % 2L == 0L;
    }

    private static int parse(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception var3) {
            return def;
        }
    }

    private static String digits(String s) {
        return s.replaceAll("[^0-9]", "").replaceFirst("^0+(?=.)", "");
    }

    private static String when(long epoch) {
        return DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm").format(Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.g = graphics;
        this.renderBackground(graphics);
        this.hots.clear();
        this.keyInput = null;
        this.confirmAction = null;
        this.layout();
        this.mouseSX = (int)Math.floor(((mouseX - this.bx) / this.s - 187.0F) / 0.8825F);
        this.mouseSY = (int)Math.floor(((mouseY - this.by) / this.s - 36.0F) / 0.8825F);
        int sxGui = this.bx + Math.round(187.0F * this.s);
        int syGui = this.by + Math.round(36.0F * this.s);
        RenderSystem.enableBlend();
        PoseStack ps = graphics.pose();
        ps.pushPose();
        ps.translate((float)this.bx, (float)this.by, 0.0F);
        ps.scale(this.s, this.s, 1.0F);
        graphics.blit(BODY, 0, 0, 580, 510, 0.0F, 0.0F, 1160, 1020, 1160, 1020);
        this.drawHardware((mouseX - this.bx) / this.s, (mouseY - this.by) / this.s);
        ps.pushPose();
        ps.translate(187.0F, 36.0F, 0.0F);
        ps.scale(0.8825F, 0.8825F, 1.0F);
        graphics.enableScissor(sxGui, syGui, sxGui + Math.round(353.0F * this.s), syGui + Math.round(199.0F * this.s));
        this.drawScreen();
        graphics.disableScissor();
        ps.popPose();
        ps.popPose();
        boolean talking = this.callState() == CallState.CONNECTED;
        this.msgBox.visible = talking;

        for (GuiEventListener child : this.children()) {
            if (child instanceof Button b) {
                b.visible = talking;
            }
        }

        if (talking) {
            graphics.drawString(this.font, wp("message_label"), this.msgBox.getX() - this.font.width(wp("message_label")) - 6, this.msgBox.getY() + 4, -1);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawHardware(double mx, double my) {
        CallState cs = this.callState();
        boolean offHook = cs == CallState.CONNECTED || cs == CallState.DIALING;
        if (!offHook) {
            int dx = cs == CallState.RINGING && System.currentTimeMillis() / 60L % 2L == 0L ? 1 : 0;
            this.g.blit(HANDSET, 8 + dx, 6, 118, 470, 0.0F, 0.0F, 236, 940, 236, 940);
        }

        if (mx >= 8.0 && mx < 126.0 && my >= 6.0 && my < 476.0) {
            this.fill(8, 6, 118, 470, 419430399);
        }

        if (System.currentTimeMillis() < this.keyFlashUntil) {
            for (int r = 0; r < 4; r++) {
                for (int c = 0; c < 4; c++) {
                    if (KEYS[r][c].equals(this.keyFlash)) {
                        int w = c == 3 ? 50 : 40;
                        this.fill(KEY_X[c] - w / 2, KEY_Y[r] - 13, w, 26, 1430103551);
                    }
                }
            }
        }

        int[] leds = new int[4];
        if (this.data.flag("absent")) {
            leds[1] = -26066;
        }

        if (this.adminUnlocked) {
            leds[2] = -12723068;
        }

        boolean alert = this.hasUnconfirmedAlert();
        leds[3] = alert && this.blink() ? -50384 : -12723068;
        if (cs != CallState.IDLE && this.blink()) {
            leds[0] = -12737025;
        }

        for (int i = 0; i < 4; i++) {
            if (leds[i] != 0) {
                this.fill(FN_X[i] - 9, 260, 18, 3, leds[i]);
                this.fill(FN_X[i] - 11, 258, 22, 7, leds[i] & 16777215 | 805306368);
            }
        }

        double a = Math.toRadians(this.jogAngle);
        int px = 478 + (int)Math.round(Math.cos(a) * 49.0);
        int py = 358 + (int)Math.round(Math.sin(a) * 49.0);
        this.fill(px - 2, py - 2, 4, 4, -6642512);
    }

    private boolean hasUnconfirmedAlert() {
        for (MissedCall m : this.data.records()) {
            if (m.caller().startsWith("alert|") && m.caller().endsWith("|0")) {
                return true;
            }
        }

        return false;
    }

    private void drawScreen() {
        switch (this.page) {
            case MAIN:
                this.drawMain();
                break;
            case TOUCH:
                this.drawTouch();
                break;
            default:
                this.drawSub();
        }

        if (this.keypad != null) {
            this.drawKeypad();
        }

        if (this.popup != null) {
            this.drawPopup();
        }
    }

    private void drawMain() {
        this.g.blit(BG, 0, 0, 400, 225, 0.0F, 0.0F, 800, 450, 800, 450);
        this.fill(0, 0, 132, 225, 1343236693);
        ClientLevel level = Minecraft.getInstance().level;
        int temp = 20;
        int wicon = 6;
        String weather = wp("weather.clear");
        if (level != null) {
            float base = level.getBiome(this.pos).value().getBaseTemperature();
            temp = Math.max(-15, Math.min(40, Math.round(base * 25.0F) - (level.isRaining() ? 3 : 0)));
            if (level.isThundering()) {
                wicon = 8;
                weather = wp("weather.thunder");
            } else if (level.isRaining()) {
                wicon = base < 0.15F ? 9 : 8;
                weather = base < 0.15F ? wp("weather.snow") : wp("weather.rain");
            } else if (level.getDayTime() % 24000L > 12500L && level.getDayTime() % 24000L < 23500L) {
                wicon = 7;
                weather = wp("weather.clear_night");
            }
        }

        this.icon(wicon, 8, 8, 40);
        this.shadowText(temp + "℃", 54.0F, 12.0F, -1, 2.0F);
        this.shadowText(weather, 58.0F, 34.0F, -1, 0.75F);
        LocalDateTime now = LocalDateTime.now();
        this.shadowText(
            wp("wd." + now.getDayOfWeek().getValue() % 7) + " " + now.format(DateTimeFormatter.ofPattern("MM.dd HH:mm")) + (now.getHour() < 12 ? " AM" : " PM"),
            8.0F,
            56.0F,
            -1,
            0.72F
        );
        this.drawCalendar(4, 72);
        this.fill(0, 203, 132, 22, 1879048192);
        this.icon(10, 6, 206, 16, digits(this.data.unit()).isEmpty() ? -1 : -11746454);
        this.hot(4, 204, 20, 20, () -> this.info(t("status.addr", this.data.setting("guard_label", this.data.unit()))));
        this.icon(16, 38, 206, 16, this.hasUnconfirmedAlert() && this.blink() ? -1750977 : -1);
        this.hot(36, 204, 20, 20, () -> {
            this.go(GuardMasterScreen.Page.SECURITY);
            this.setTab(GuardMasterScreen.Page.SECURITY, 1);
        });
        this.icon(32, 70, 206, 16);
        this.hot(68, 204, 20, 20, () -> {
            this.go(GuardMasterScreen.Page.INQUIRY);
            this.setTab(GuardMasterScreen.Page.INQUIRY, 2);
        });
        this.icon(12, 102, 206, 16);
        this.hot(100, 204, 20, 20, () -> {
            this.go(GuardMasterScreen.Page.INQUIRY);
            this.setTab(GuardMasterScreen.Page.INQUIRY, 3);
        });
        this.shadowText("HOMENET", 326.0F, 8.0F, -1, 1.0F);
        this.text("Guard Master", 340.0F, 18.0F, -2036488, 0.55F);
        if (this.data.flag("absent")) {
            this.fill(150, 6, 120, 14, -1056990674);
            this.textC(t("absent_banner", this.data.setting("absent_fwd", "")), 210.0F, 9.0F, -1, 0.65F);
        }

        this.mainIcon(170, 56, 0, wp("menu.security"), GuardMasterScreen.Page.SECURITY);
        this.mainIcon(240, 56, 2, wp("menu.call"), GuardMasterScreen.Page.CALL);
        this.mainIcon(310, 56, 3, wp("menu.inquiry"), GuardMasterScreen.Page.INQUIRY);
        this.mainIcon(170, 128, 4, wp("menu.settings"), GuardMasterScreen.Page.SETTINGS);
        this.fill(140, 192, 254, 14, -1610612736);
        String line = this.callState() != CallState.IDLE ? this.callStatusLine() : t("main_hint");
        this.textFit(line, 267.0F, 195.0F, this.callState() != CallState.IDLE ? -11686 : -1, 0.65F, 248.0F);
        if (this.callState() != CallState.IDLE) {
            this.hot(140, 192, 254, 14, () -> this.go(GuardMasterScreen.Page.CALL));
        }
    }

    private void mainIcon(int x, int y, int idx, String label, GuardMasterScreen.Page target) {
        if (this.hover(x, y, 44, 44)) {
            this.fill(x - 2, y - 2, 48, 48, 1090519039);
        }

        this.icon(idx, x, y, 44);
        this.shadowText(label, x + 22 - this.width(label, 0.8F) / 2.0F, y + 48, -1, 0.8F);
        this.hot(x - 4, y - 4, 52, 62, () -> this.go(target));
    }

    private void drawCalendar(int x, int y) {
        LocalDate first = LocalDate.of(this.calYear, this.calMonth, 1);
        LocalDate today = LocalDate.now();
        this.shadowText("<", x + 4, y + 2, -1, 0.9F);
        this.hot(x, y, 16, 12, () -> this.shiftMonth(-1));
        this.textC(wp("cal.header", this.calMonth, this.calYear), x + 62, y + 3, -1, 0.75F);
        this.shadowText(">", x + 116, y + 2, -1, 0.9F);
        this.hot(x + 110, y, 16, 12, () -> this.shiftMonth(1));
        this.fill(x, y + 13, 124, 10, 1358940493);
        String[] wd = new String[]{"M", "T", "W", "T", "F", "S", "S"};

        for (int i = 0; i < 7; i++) {
            this.textC(wd[i], x + 9 + i * 17.7F, y + 15, i == 6 ? -30080 : -1, 0.6F);
        }

        int offset = first.getDayOfWeek().getValue() - 1;

        for (int d = 1; d <= first.lengthOfMonth(); d++) {
            int cell = offset + d - 1;
            float cx = x + 9 + cell % 7 * 17.7F;
            int cy = y + 27 + cell / 7 * 17;
            boolean isToday = today.getYear() == this.calYear && today.getMonthValue() == this.calMonth && today.getDayOfMonth() == d;
            if (isToday) {
                this.fill(Math.round(cx) - 7, cy - 3, 15, 13, -1862270977);
            }

            DayOfWeek dw = first.plusDays(d - 1).getDayOfWeek();
            this.textC(String.valueOf(d), cx, cy, isToday ? -1750977 : (dw == DayOfWeek.SUNDAY ? -20312 : -1), 0.62F);
        }
    }

    private void shiftMonth(int d) {
        LocalDate m = LocalDate.of(this.calYear, this.calMonth, 1).plusMonths(d);
        this.calYear = m.getYear();
        this.calMonth = m.getMonthValue();
    }

    private String peerDisplay(String p) {
        if (p.startsWith("#guard:")) {
            return wp("guard_label_no", p.substring(7));
        } else if (p.startsWith("#lobby:")) {
            return t("lobby_no", p.substring(7));
        } else {
            return switch (p) {
                case "#guard" -> wp("guard_label");
                case "#office" -> wp("office_label");
                default -> p;
            };
        }
    }

    private String peerName() {
        return this.data.outgoing() ? this.peerDisplay(this.data.peer()) : Intercom.sideName(this.key()).getString();
    }

    private String callStatusLine() {
        CallState cs = this.callState();
        if (this.data.outgoing()) {
            return cs == CallState.DIALING ? wp("call.dialing", this.peerName()) : wp("call.talking_with", this.peerName(), this.talkTime());
        } else {
            return cs == CallState.RINGING ? wp("call.incoming", this.peerName()) : wp("call.talking_with", this.peerName(), this.talkTime());
        }
    }

    private String talkTime() {
        long sec = Math.max(0L, (System.currentTimeMillis() - this.connectedSince) / 1000L);
        return String.format("%02d:%02d", sec / 60L, sec % 60L);
    }

    private void drawSub() {
        this.g.blit(BG_SUB, 0, 0, 400, 225, 0.0F, 0.0F, 800, 450, 800, 450);
        this.icon(14, 4, 2, 14, this.hover(2, 0, 20, 18) ? -7351297 : -1);
        this.hot(0, 0, 24, 18, () -> this.go(GuardMasterScreen.Page.MAIN));
        this.icon(15, 382, 2, 14, this.hover(378, 0, 22, 18) ? -7351297 : -1);
        this.hot(376, 0, 24, 18, () -> this.info(t("help." + this.page.name().toLowerCase(Locale.ROOT))));

        for (int i = 0; i < MENU.length; i++) {
            int y = 22 + i * 27;
            boolean sel = this.page == MENU[i];
            GuardMasterScreen.Page target = MENU[i];
            if (sel) {
                this.g.fillGradient(3, y, 74, y + 22, -11747851, -14710321);
            } else {
                this.fill(3, y, 71, 22, this.hover(3, y, 71, 22) ? -1068475800 : -1606926263);
            }

            this.frame(3, y, 71, 22, sel ? -7351297 : -9735040);
            this.icon(MENU_ICON[i], 8, y + 4, 14, -1);
            this.text(wp("menu." + MENU_KEY[i]), 27.0F, y + 8, -1, 0.75F);
            this.hot(3, y, 71, 22, () -> this.go(target));
        }

        this.frame(78, 19, 319, 204, -12605475);
        switch (this.page) {
            case SECURITY:
                this.drawSecurity();
                break;
            case CALL:
                this.drawCall();
                break;
            case INQUIRY:
                this.drawInquiry();
                break;
            case SETTINGS:
                this.drawSettings();
        }
    }

    private List<int[]> alertIdx() {
        List<int[]> out = new ArrayList<>();
        int i = 0;

        for (MissedCall m : this.data.records()) {
            if (m.caller().startsWith("alert|")) {
                int[] var10001 = new int[]{this.data.records().indexOf(m), 0};
                var10001[1] = i++;
                out.add(var10001);
            }
        }

        return out;
    }

    private void drawSecurity() {
        int tb = this.tab(GuardMasterScreen.Page.SECURITY);
        this.tabRow("sec", SEC_TABS, tb, i -> this.setTab(GuardMasterScreen.Page.SECURITY, i));
        List<MissedCall> alerts = new ArrayList<>();

        for (MissedCall m : this.data.records()) {
            if (m.caller().startsWith("alert|")) {
                alerts.add(m);
            }
        }

        if (tb == 0) {
            this.panel(84, 42, 222, 157);
            boolean emergency = alerts.stream().anyMatch(mx -> mx.caller().endsWith("|0"));
            this.secTile(94, 52, 27, t("sec.sensor1"), false);
            this.secTile(164, 52, 28, t("sec.sensor234"), false);
            this.secTile(234, 52, 29, t("sec.gas"), false);
            this.secTile(94, 130, 30, t("sec.emergency"), emergency && this.blink());
            if (emergency) {
                MissedCall last = alerts.stream().filter(mx -> mx.caller().endsWith("|0")).findFirst().orElse(alerts.get(0));
                String u = last.caller().split("\\|")[1];
                this.text(t("sec.from", u), 164.0F, 142.0F, -1750977, 0.75F);
                this.text(when(last.dayTime()), 164.0F, 154.0F, -14012616, 0.6F);
            } else {
                this.text(t("sec.normal"), 164.0F, 146.0F, -9735296, 0.7F);
            }

            int rx = 312;
            this.darkPanel(rx - 4, 42, 83, 157);
            this.button(rx + 2, 52, 71, 26, t("sec.stop"), false, emergency, () -> {
                for (int[] a : this.alertIdx()) {
                    String c = this.data.records().get(a[0]).caller();
                    if (c.endsWith("|0")) {
                        this.send(ReceiverBlockEntity.Action.CONFIRM_ALERT, String.valueOf(a[1]));
                    }
                }
            });
            int y = 201;
            this.fill(80, y, 315, 18, -1070319036);
            this.icon(18, 86, y + 3, 12, emergency ? -11746454 : -6642512);
            this.text(t("sec.call_hint"), 104.0F, y + 6, -1, 0.6F);
            if (!alerts.isEmpty()) {
                String u = alerts.get(0).caller().split("\\|")[1];
                this.hot(80, y, 160, 18, () -> this.send(ReceiverBlockEntity.Action.DIAL, u));
            }
        } else {
            this.drawRecordTable(
                new String[]{t("col.kind"), t("col.unit"), t("col.time"), t("col.check")},
                new float[]{0.18F, 0.3F, 0.34F, 0.18F},
                alerts,
                mx -> {
                    String[] p = mx.caller().split("\\|");
                    return new String[]{
                        t("sec.emergency"), p.length > 1 ? p[1] : "", when(mx.dayTime()), p.length > 2 && p[2].equals("1") ? t("checked") : t("unchecked")
                    };
                },
                i -> this.send(ReceiverBlockEntity.Action.CONFIRM_ALERT, String.valueOf(i)),
                "alert",
                false
            );
        }
    }

    private void secTile(int x, int y, int ic, String label, boolean on) {
        if (on) {
            this.g.fillGradient(x, y, x + 60, y + 70, -34194, -2999762);
        } else {
            this.fill(x, y, 60, 70, -1183758);
        }

        this.frame(x, y, 60, 70, on ? -6411744 : -6643285);
        this.icon(ic, x + 8, y + 4, 44);
        this.textFit(label, x + 30, y + 56, on ? -1 : -14012616, 0.65F, 56.0F);
    }

    private void drawRecordTable(
        String[] header, float[] cols, List<MissedCall> rows, Function<MissedCall, String[]> fmt, Consumer<Integer> onRow, String clearKind, boolean withInput
    ) {
        int x = 84;
        int y = 42 + (withInput ? 20 : 0);
        int w = withInput ? 222 : 307;
        int perPage = withInput ? 5 : 7;
        int pages = Math.max(1, (rows.size() + perPage - 1) / perPage);
        this.listPage = Math.max(0, Math.min(this.listPage, pages - 1));
        int h = perPage * 17 + 18;
        this.panel(x, y, w, h);
        this.fill(x + 3, y + 3, w - 6, 13, -10919314);
        float cx = x + 3;

        for (int i = 0; i < header.length; i++) {
            float cw = (w - 6) * cols[i];
            this.textFit(header[i], cx + cw / 2.0F, y + 6, -1, 0.62F, cw - 4.0F);
            cx += cw;
        }

        if (rows.isEmpty()) {
            this.textC(t("empty"), x + w / 2.0F, y + 50, -9735296, 0.7F);
        }

        for (int r = 0; r < perPage; r++) {
            int idx = this.listPage * perPage + r;
            int ry = y + 17 + r * 17;
            this.fill(x + 3, ry + 16, w - 6, 1, 1619036304);
            if (idx < rows.size()) {
                String[] cells = fmt.apply(rows.get(idx));
                boolean unread = cells.length > 3 && cells[cells.length - 1].equals(t("unchecked"));
                if (this.hover(x + 3, ry, w - 6, 16)) {
                    this.fill(x + 3, ry, w - 6, 16, 1077782015);
                }

                cx = x + 3;

                for (int i = 0; i < header.length && i < cells.length; i++) {
                    float cw = (w - 6) * cols[i];
                    this.textFit(cells[i], cx + cw / 2.0F, ry + 5, unread ? -16777216 : -14012616, unread ? 0.66F : 0.6F, cw - 4.0F);
                    cx += cw;
                }

                this.hot(x + 3, ry, w - 6, 16, () -> onRow.accept(idx));
            }
        }

        int py = y + h + 3;
        this.fill(x, py, w, 16, -1070319036);
        this.button(x + w / 2 - 50, py + 2, 14, 12, "<", false, this.listPage > 0, () -> this.listPage--);
        this.textC(String.format("%02d/%02d", this.listPage + 1, pages), x + w / 2.0F, py + 5, -1, 0.6F);
        this.button(x + w / 2 + 36, py + 2, 14, 12, ">", false, this.listPage < pages - 1, () -> this.listPage++);
        if (clearKind != null) {
            this.button(
                x + w - 34,
                py + 1,
                32,
                14,
                t("clear_all"),
                false,
                !rows.isEmpty(),
                () -> this.confirm(t("clear_confirm"), () -> this.send(ReceiverBlockEntity.Action.CLEAR_RECORDS, clearKind))
            );
        }
    }

    private void inputField(String hint) {
        int x = 84;
        int y = 42;
        this.fill(x, y, 222, 17, -1);
        this.frame(x, y, 222, 17, -13722661);
        this.text(this.input.isEmpty() ? hint : this.input + (this.blink() ? "_" : ""), x + 5, y + 5, this.input.isEmpty() ? -6643285 : -14012616, 0.75F);
        int bxx = x + 202;
        this.fill(bxx, y + 3, 16, 11, -1775636);
        this.frame(bxx, y + 3, 16, 11, -7564387);
        this.textC("×", bxx + 8, y + 5, -14012616, 0.7F);
        this.hot(bxx, y, 20, 17, () -> this.input = this.input.isEmpty() ? "" : backspace(this.input));
    }

    private static String backspace(String s) {
        return s.endsWith("동 ") ? s.substring(0, s.length() - 2) : s.substring(0, s.length() - 1);
    }

    private List<MissedCall> history(int type) {
        List<MissedCall> out = new ArrayList<>();

        for (MissedCall m : this.data.records()) {
            if (m.caller().startsWith("hist|")) {
                String k = m.caller().substring(m.caller().indexOf(124, 5) + 1);
                if (tabForKey(k) == type && (type != 0 || !k.startsWith("#") && !k.equals("lobby"))) {
                    out.add(m);
                }
            }
        }

        return out;
    }

    private void drawCall() {
        int tb = this.tab(GuardMasterScreen.Page.CALL);
        this.tabRow("call", CALL_TABS, tb, i -> this.setTab(GuardMasterScreen.Page.CALL, i));
        CallState cs = this.callState();
        boolean mine = cs != CallState.IDLE && tabForKey(this.data.outgoing() ? this.data.peer() : this.key()) == tb && tb != 3;
        boolean ringing = mine && cs == CallState.RINGING;
        boolean talking = mine && cs == CallState.CONNECTED;
        boolean dialing = mine && cs == CallState.DIALING;
        if (!talking && !ringing) {
            if (tb == 3) {
                this.inputField(t("call.phone_hint"));
                this.drawRecordTable(new String[]{t("col.number"), t("col.time")}, new float[]{0.5F, 0.5F}, List.of(), m -> new String[0], i -> {}, null, true);
            } else {
                String[] hints = new String[]{t("call.unit_hint"), t("call.guard_hint"), t("call.lobby_hint")};
                this.inputField(hints[tb]);
                List<MissedCall> hist = this.history(tb);
                String[] header = tb == 0 ? new String[]{"", t("col.dong"), t("col.ho"), t("col.time")} : new String[]{"", t("col.number"), t("col.time")};
                float[] cols = tb == 0 ? new float[]{0.1F, 0.2F, 0.2F, 0.5F} : new float[]{0.1F, 0.4F, 0.5F};
                this.drawRecordTable(header, cols, hist, m -> this.histCells(m, tb), i -> this.input = this.histInput(hist.get(i), tb), "hist", true);
                if (dialing) {
                    this.fill(84, 98, 222, 24, -805306368);
                    this.textFit(wp("call.dialing", this.peerName()), 195.0F, 106.0F, -11686, 0.8F, 214.0F);
                }
            }
        } else {
            int camIdx = tb == 0 ? 3 : (tb == 1 ? 2 : (this.key().equals("front_door") ? 0 : 1));
            this.fill(83, 41, 224, 159, -10919314);
            this.cam(camIdx, 84, 42, 222, 157);
            this.fill(84, 42, 222, 13, -1610612736);
            this.textFit(
                ringing ? wp("call.incoming", this.peerName()) : wp("call.talking_with", this.peerName(), this.talkTime()),
                195.0F,
                45.0F,
                ringing ? -11686 : -6491984,
                0.7F,
                216.0F
            );
            this.drawLog(86, 169);
        }

        int rx = 312;
        this.darkPanel(rx - 4, 42, 83, 157);
        boolean idle = cs == CallState.IDLE;
        this.button(rx + 2, 50, 71, 26, wp("call.talk"), talking || dialing || ringing && this.blink(), idle || ringing, () -> {
            if (ringing) {
                this.send(ReceiverBlockEntity.Action.ANSWER);
            } else {
                this.confirmCall();
            }
        });
        this.button(rx + 2, 82, 71, 26, t("call.end"), false, cs != CallState.IDLE, () -> this.send(ReceiverBlockEntity.Action.HANG_UP));
        if (tb == 2) {
            this.button(rx + 2, 114, 71, 26, t("call.open"), false, talking, () -> this.send(ReceiverBlockEntity.Action.OPEN_DOOR));
        }

        if (tb == 3) {
            this.button(rx + 2, 114, 71, 26, t("call.redial"), false, true, () -> this.info(wp("notice.no_line")));
        }

        int y = 201;
        this.fill(80, y, 315, 18, -1070319036);
        this.icon(30, 246, y + 3, 12, -1);
        int vol = parse(this.data.setting("vol_call", "8"), 8);
        this.slider(262, y + 3, vol, v -> this.send(ReceiverBlockEntity.Action.SET_SETTING, "vol_call=" + v));
        this.confirmAction = this::confirmCall;
        this.keyInput = c -> {
            if (c.equals("\b")) {
                this.input = this.input.isEmpty() ? "" : backspace(this.input);
            } else if (c.matches("[0-9*#]") && this.input.length() < 16) {
                this.input = this.input + c;
            }
        };
    }

    private String[] histCells(MissedCall m, int tb) {
        String[] p = m.caller().split("\\|", 3);
        String dir = p.length > 1 ? p[1] : "";

        String mark = switch (dir) {
            case "in" -> "↙";
            case "out" -> "↗";
            case "miss" -> "✕";
            default -> "→";
        };
        String k = p.length > 2 ? p[2] : "";
        String who = k.startsWith("#")
            ? this.peerDisplay(k)
            : (!k.startsWith("unit:") && !k.startsWith("guard:") ? Intercom.sideName(k).getString() : k.substring(k.indexOf(58) + 1));
        if (tb == 0) {
            String[] dh = splitUnit(who);
            return new String[]{mark, dh[0], dh[1], when(m.dayTime())};
        } else {
            return new String[]{mark, who, when(m.dayTime())};
        }
    }

    private String histInput(MissedCall m, int tb) {
        String[] p = m.caller().split("\\|", 3);
        String k = p.length > 2 ? p[2] : "";
        if (tb == 0) {
            String[] dh = splitUnit(k.startsWith("unit:") ? k.substring(5) : k);
            return dh[1].isEmpty() ? dh[0] : dh[0] + "동 " + dh[1] + "호";
        } else {
            return digits(k);
        }
    }

    private static String[] splitUnit(String u) {
        String[] parts = u.split("[^0-9]+");
        List<String> nums = new ArrayList<>();

        for (String x : parts) {
            if (!x.isEmpty()) {
                nums.add(x);
            }
        }

        return nums.size() >= 2 ? new String[]{nums.get(0), nums.get(1)} : new String[]{nums.isEmpty() ? u : nums.get(0), ""};
    }

    private void confirmCall() {
        if (this.callState() == CallState.IDLE) {
            int tb = this.tab(GuardMasterScreen.Page.CALL);
            switch (tb) {
                case 0:
                    if (this.input.isEmpty()) {
                        return;
                    }

                    if (!this.input.contains("동")) {
                        this.input = this.input + "동 ";
                        return;
                    }

                    String u = this.input.endsWith("호") ? this.input : this.input.trim() + "호";
                    this.send(ReceiverBlockEntity.Action.DIAL, u);
                    this.input = "";
                    break;
                case 1:
                    this.send(ReceiverBlockEntity.Action.CALL_GUARD, this.input.isEmpty() ? "guard" : "no:" + digits(this.input));
                    this.input = "";
                    break;
                case 2:
                    if (digits(this.input).isEmpty()) {
                        this.info(t("notice.need_lobby"));
                        return;
                    }

                    this.send(ReceiverBlockEntity.Action.CALL_LOBBY, digits(this.input));
                    this.input = "";
                    break;
                default:
                    this.info(wp("notice.no_line"));
            }
        }
    }

    private void drawLog(int x, int y) {
        List<IntercomLine> log = this.data.log();
        int n = Math.min(3, log.size());

        for (int i = 0; i < n; i++) {
            IntercomLine l = log.get(log.size() - n + i);
            String line = l.name() + ": " + l.text();
            this.fill(x, y + i * 9 - 1, 218, 9, -1879048192);
            this.text(this.font.plainSubstrByWidth(line, 356), x + 2, y + i * 9, -1, 0.6F);
        }
    }

    private void slider(int x, int y, int value, Consumer<Integer> set) {
        this.fill(x, y, 128, 12, -723465);
        this.frame(x, y, 128, 12, -7564387);
        this.textC("−", x + 6, y + 2, -14012616, 0.8F);
        this.textC("+", x + 122, y + 2, -14012616, 0.8F);
        this.hot(x, y, 12, 12, () -> set.accept(Math.max(1, value - 1)));
        this.hot(x + 116, y, 12, 12, () -> set.accept(Math.min(10, value + 1)));

        for (int i = 0; i < 10; i++) {
            int bxx = x + 16 + i * 10;
            int v = i + 1;
            this.fill(bxx, y + 2, 7, 8, i < value ? -10919314 : -3091238);
            this.hot(bxx - 1, y, 10, 12, () -> set.accept(v));
        }
    }

    private void drawInquiry() {
        int tb = this.tab(GuardMasterScreen.Page.INQUIRY);
        this.tabRow("inq", INQ_TABS, tb, ix -> this.setTab(GuardMasterScreen.Page.INQUIRY, ix));
        int rx = 312;
        switch (tb) {
            case 0: {
                this.inputField(t("inq.fwd_hint"));
                this.panel(84, 62, 222, 153);
                boolean absent = this.data.flag("absent");
                String fwd = this.data.setting("absent_fwd", "");
                this.text(t("inq.absent_desc1"), 90.0F, 70.0F, -14012616, 0.65F);
                this.text(t("inq.absent_desc2"), 90.0F, 81.0F, -9735296, 0.6F);
                this.fill(90, 100, 210, 40, absent ? -7230 : -1512465);
                this.text(absent ? t("inq.absent_on") : t("inq.absent_off"), 96.0F, 106.0F, absent ? -4957696 : -14012616, 0.75F);
                if (absent) {
                    this.text(fwd.isEmpty() ? t("inq.absent_nofwd") : t("inq.fwd_to", wp("guard_label_no", fwd)), 96.0F, 122.0F, -14012616, 0.65F);
                }

                this.darkPanel(rx - 4, 42, 83, 175);
                this.button(rx + 2, 50, 71, 26, t("inq.absent_set"), absent, true, () -> this.send(ReceiverBlockEntity.Action.ABSENT_SET, digits(this.input)));
                this.button(rx + 2, 82, 71, 26, t("inq.absent_clear"), false, absent, () -> this.send(ReceiverBlockEntity.Action.ABSENT_CLEAR));
                this.numberInput();
                break;
            }
            case 1: {
                this.inputField(t("inq.fwd_hint"));
                this.panel(84, 62, 222, 153);
                String fwd = this.data.setting("busy_fwd", "");
                this.text(t("inq.bypass_desc1"), 90.0F, 70.0F, -14012616, 0.65F);
                this.text(t("inq.bypass_desc2"), 90.0F, 81.0F, -9735296, 0.6F);
                this.fill(90, 100, 210, 40, fwd.isEmpty() ? -1512465 : -2757377);
                this.text(fwd.isEmpty() ? t("inq.bypass_off") : t("inq.bypass_on", wp("guard_label_no", fwd)), 96.0F, 112.0F, -14012616, 0.72F);
                this.darkPanel(rx - 4, 42, 83, 175);
                this.button(
                    rx + 2, 50, 71, 26, t("inq.bypass_set"), !fwd.isEmpty(), true, () -> this.send(ReceiverBlockEntity.Action.BUSY_FWD_SET, digits(this.input))
                );
                this.button(rx + 2, 82, 71, 26, t("inq.bypass_clear"), false, !fwd.isEmpty(), () -> this.send(ReceiverBlockEntity.Action.BUSY_FWD_CLEAR));
                this.numberInput();
                break;
            }
            case 2:
                this.drawRecordTable(
                    new String[]{t("col.unread"), t("col.unit"), t("col.time"), t("col.check")},
                    new float[]{0.2F, 0.3F, 0.3F, 0.2F},
                    List.of(),
                    mx -> new String[0],
                    ix -> {},
                    null,
                    false
                );
                this.text(t("inq.safety_note"), 88.0F, 209.0F, -12959412, 0.55F);
                break;
            default:
                this.inputField(t("inq.parcel_hint"));
                List<MissedCall> all = new ArrayList<>();
                List<Integer> idx = new ArrayList<>();
                int i = 0;

                for (MissedCall m : this.data.records()) {
                    if (m.caller().startsWith("parcel|")) {
                        String[] p = m.caller().split("\\|");

                        boolean keep = switch (this.parcelFilter) {
                            case 1 -> p.length > 1 && digits(p[1]).contains(digits(this.input)) && !digits(this.input).isEmpty();
                            case 2 -> p.length > 2 && p[2].equals("0");
                            default -> true;
                        };
                        if (keep) {
                            all.add(m);
                            idx.add(i);
                        }

                        i++;
                    }
                }

                this.drawRecordTable(
                    new String[]{t("col.kind"), t("col.unit"), t("col.time")},
                    new float[]{0.25F, 0.35F, 0.4F},
                    all,
                    mx -> {
                        String[] px = mx.caller().split("\\|");
                        return new String[]{
                            px.length > 2 && px[2].equals("1") ? t("inq.parcel_done") : t("inq.parcel_wait"), px.length > 1 ? px[1] : "", when(mx.dayTime())
                        };
                    },
                    r -> this.send(ReceiverBlockEntity.Action.PARCEL_DONE, String.valueOf(idx.get(r))),
                    "parcel",
                    true
                );
                this.darkPanel(rx - 4, 42, 83, 175);
                this.button(rx + 2, 50, 71, 26, t("inq.parcel_add"), false, true, () -> {
                    String[] dh = splitUnit(this.input);
                    if (dh[1].isEmpty()) {
                        this.info(t("notice.need_unit"));
                    } else {
                        this.send(ReceiverBlockEntity.Action.PARCEL_ADD, dh[0] + "동 " + dh[1] + "호");
                        this.input = "";
                    }
                });
                this.button(rx + 2, 82, 71, 26, t("inq.parcel_search"), this.parcelFilter == 1, true, () -> this.parcelFilter = this.parcelFilter == 1 ? 0 : 1);
                this.button(rx + 2, 114, 71, 26, t("inq.parcel_unrecv"), this.parcelFilter == 2, true, () -> this.parcelFilter = this.parcelFilter == 2 ? 0 : 2);
                this.confirmAction = () -> {
                    if (!this.input.contains("동") && !this.input.isEmpty()) {
                        this.input = this.input + "동 ";
                    }
                };
                this.keyInput = c -> {
                    if (c.equals("\b")) {
                        this.input = this.input.isEmpty() ? "" : backspace(this.input);
                    } else if (c.matches("[0-9]") && this.input.length() < 14) {
                        this.input = this.input + c;
                    }
                };
        }
    }

    private void numberInput() {
        this.keyInput = c -> {
            if (c.equals("\b")) {
                this.input = this.input.isEmpty() ? "" : backspace(this.input);
            } else if (c.matches("[0-9]") && this.input.length() < 2) {
                this.input = this.input + c;
            }
        };
    }

    private void onSettingsTab(int idx) {
        this.draft.clear();
        this.fieldFocus = 0;
        String var2 = SET_TABS[idx];
        switch (var2) {
            case "password":
                if (!this.pwUnlocked) {
                    this.keypad = new GuardMasterScreen.Keypad(wp("settings.pw_title"), true, 8, v -> {
                        this.pendingAuth = v;
                        this.send(ReceiverBlockEntity.Action.UNLOCK, "pw:" + v);
                    }, () -> this.tabs.put(GuardMasterScreen.Page.SETTINGS, 0));
                }
                break;
            case "admin":
                if (!this.adminUnlocked) {
                    this.keypad = new GuardMasterScreen.Keypad(
                        wp("settings.admin_title"),
                        true,
                        8,
                        v -> this.send(ReceiverBlockEntity.Action.UNLOCK, "admin:" + v),
                        () -> this.tabs.put(GuardMasterScreen.Page.SETTINGS, 0)
                    );
                }
                break;
            case "touch":
                this.page = GuardMasterScreen.Page.TOUCH;
                this.touchStep = 0;
        }
    }

    private void drawSettings() {
        int tb = this.tab(GuardMasterScreen.Page.SETTINGS);
        this.tabRow("set", SET_TABS, tb, i -> this.setTab(GuardMasterScreen.Page.SETTINGS, i));
        String var2 = SET_TABS[tb];
        switch (var2) {
            case "sound":
                this.panel(84, 42, 307, 175);
                int y = 78;
                this.fill(90, y, 295, 24, -1907222);
                this.frame(90, y, 295, 24, -5196098);
                this.fill(94, y + 4, 64, 16, -10919314);
                this.textC(wp("settings.vol_system"), 126.0F, y + 8, -1, 0.65F);
                this.icon(30, 176, y + 5, 14, -14012616);
                String cur = this.draft.getOrDefault("vol_system", this.data.setting("vol_system", "5"));
                this.slider(196, y + 6, parse(cur, 5), v -> this.draft.put("vol_system", String.valueOf(v)));
                this.button(275, 193, 52, 18, wp("settings.reset"), false, true, () -> {
                    this.draft.clear();
                    this.send(ReceiverBlockEntity.Action.RESET_SOUND);
                });
                this.button(333, 193, 52, 18, wp("settings.save"), false, true, () -> {
                    this.draft.forEach((k, v) -> this.send(ReceiverBlockEntity.Action.SET_SETTING, k + "=" + v));
                    this.draft.clear();
                    this.info(wp("notice.saved"));
                });
                break;
            case "password":
                this.drawPassword();
                break;
            case "admin":
                this.drawAdmin();
                break;
            default:
                this.panel(84, 42, 307, 175);
                this.button(187, 108, 100, 24, wp("settings.touch_start"), false, true, () -> {
                    this.page = GuardMasterScreen.Page.TOUCH;
                    this.touchStep = 0;
                });
        }
    }

    private void field(int x, int y, int w, String label, String value, boolean focused, Runnable onFocus) {
        this.fill(x, y, w, 30, -10919314);
        this.text(label, x + 4, y + 3, -1, 0.6F);
        this.fill(x + 3, y + 13, w - 6, 14, focused ? -1 : -1512465);
        this.frame(x + 3, y + 13, w - 6, 14, focused ? -13722661 : -7564387);
        this.text("*".repeat(value.length()) + (focused && this.blink() ? "_" : ""), x + 7, y + 17, -14012616, 0.7F);
        this.hot(x, y, w, 30, onFocus);
    }

    private void sideKeypad(int x, int y, Consumer<String> onKey) {
        String[] keys = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#"};

        for (int i = 0; i < 12; i++) {
            String kk = keys[i];
            this.button(x + i % 3 * 44, y + i / 3 * 34, 40, 30, kk, false, true, () -> onKey.accept(kk));
        }

        this.keyInput = onKey;
    }

    private void drawPassword() {
        this.panel(84, 42, 307, 175);
        if (!this.pwUnlocked) {
            this.textC(wp("settings.pw_locked"), 237.5F, 108.0F, -9735296, 0.75F);
            this.button(197, 128, 80, 20, wp("settings.pw_enter"), false, true, () -> this.onSettingsTab(1));
        } else {
            this.field(92, 52, 120, wp("settings.pw_new"), this.pw1, this.fieldFocus == 0, () -> this.fieldFocus = 0);
            this.field(92, 90, 120, wp("settings.pw_confirm"), this.pw2, this.fieldFocus == 1, () -> this.fieldFocus = 1);
            this.sideKeypad(248, 48, k -> {
                if (k.equals("\b")) {
                    if (this.fieldFocus == 0 && !this.pw1.isEmpty()) {
                        this.pw1 = this.pw1.substring(0, this.pw1.length() - 1);
                    }

                    if (this.fieldFocus == 1 && !this.pw2.isEmpty()) {
                        this.pw2 = this.pw2.substring(0, this.pw2.length() - 1);
                    }
                } else if (k.matches("\\d")) {
                    if (this.fieldFocus == 0 && this.pw1.length() < 4) {
                        this.pw1 = this.pw1 + k;
                        if (this.pw1.length() == 4) {
                            this.fieldFocus = 1;
                        }
                    } else if (this.fieldFocus == 1 && this.pw2.length() < 4) {
                        this.pw2 = this.pw2 + k;
                    }
                }
            });
            this.button(92, 130, 56, 20, wp("settings.save"), false, true, () -> {
                if (this.pw1.length() == 4 && this.pw1.equals(this.pw2)) {
                    this.send(ReceiverBlockEntity.Action.SET_DOOR_PASSWORD, this.pwAuth + "|" + this.pw1);
                } else {
                    this.info(wp("notice.pw_mismatch"));
                }
            });
            this.button(156, 130, 56, 20, wp("settings.edit"), false, true, () -> {
                this.pw1 = "";
                this.pw2 = "";
                this.fieldFocus = 0;
            });
            this.text(t("set.pw_note1"), 92.0F, 162.0F, -12959412, 0.58F);
            this.text(t("set.pw_note2"), 92.0F, 172.0F, -12959412, 0.58F);
        }
    }

    private void drawAdmin() {
        this.panel(84, 42, 307, 175);
        if (!this.adminUnlocked) {
            this.textC(wp("settings.admin_locked"), 237.5F, 108.0F, -9735296, 0.75F);
            this.button(197, 128, 80, 20, wp("settings.pw_enter"), false, true, () -> this.onSettingsTab(3));
        } else {
            this.fill(90, 48, 150, 14, -10919314);
            this.text(t("set.addr_title"), 94.0F, 52.0F, -1, 0.65F);
            this.text(t("set.addr_label"), 92.0F, 70.0F, -14012616, 0.65F);
            this.fill(150, 66, 60, 16, -1);
            this.frame(150, 66, 60, 16, -13722661);
            this.text(this.adminAddr + (this.blink() ? "_" : ""), 155.0F, 70.0F, -14012616, 0.8F);
            this.text(t("set.addr_current", this.data.setting("guard_label", this.data.unit())), 92.0F, 88.0F, -9735296, 0.6F);
            this.text(t("set.addr_note"), 92.0F, 100.0F, -12959412, 0.55F);
            this.button(92, 114, 50, 18, wp("settings.save"), false, true, () -> this.send(ReceiverBlockEntity.Action.SET_UNIT, this.adminAddr));
            this.button(148, 114, 50, 18, wp("settings.edit"), false, true, () -> this.adminAddr = "");
            this.fill(90, 142, 150, 14, -10919314);
            this.text(t("set.ip_title"), 94.0F, 146.0F, -1, 0.65F);
            int a = Math.floorMod(this.pos.hashCode(), 200) + 20;
            this.text("IP Address : 10.0.0." + a, 92.0F, 162.0F, -9735296, 0.6F);
            this.text("Subnet Mask : 255.255.255.0", 92.0F, 172.0F, -9735296, 0.6F);
            this.text("Gateway : 10.0.0.1   ·   Server : 10.0.0.2", 92.0F, 182.0F, -9735296, 0.6F);
            this.text("ASTRO KGP-70K  S/W 1.5.08", 92.0F, 194.0F, -9735296, 0.55F);
            this.sideKeypad(248, 48, k -> {
                if (k.equals("\b")) {
                    this.adminAddr = this.adminAddr.isEmpty() ? "" : this.adminAddr.substring(0, this.adminAddr.length() - 1);
                } else if (k.matches("\\d") && this.adminAddr.length() < 2) {
                    this.adminAddr = this.adminAddr + k;
                }
            });
        }
    }

    private void drawTouch() {
        this.fill(0, 0, 400, 225, -1);
        this.textC(wp("touch.line1"), 200.0F, 8.0F, -14012616, 0.65F);
        this.textC(wp("touch.line2"), 200.0F, 18.0F, -14012616, 0.65F);
        int[] p = TOUCH_POINTS[Math.min(this.touchStep, 4)];
        this.fill(p[0] - 10, p[1], 21, 1, -14012616);
        this.fill(p[0], p[1] - 10, 1, 21, -14012616);
        this.hot(p[0] - 12, p[1] - 12, 25, 25, () -> {
            this.touchStep++;
            if (this.touchStep >= TOUCH_POINTS.length) {
                this.page = GuardMasterScreen.Page.SETTINGS;
                this.tabs.put(GuardMasterScreen.Page.SETTINGS, 0);
                this.info(wp("notice.touch_done"));
            }
        });
    }

    private void drawPopup() {
        this.hots.clear();
        this.fill(0, 0, 400, 225, Integer.MIN_VALUE);
        int w = 230;
        int lines = this.popup.lines.size();
        int h = 46 + lines * 11;
        int x = (400 - w) / 2;
        int y = (225 - h) / 2;
        this.fill(x, y, w, h, -723465);
        this.frame(x, y, w, h, -10919314);
        this.fill(x, y, w, 3, -13722661);
        this.icon(15, x + w / 2 - 7, y + 6, 14, -13722661);

        for (int i = 0; i < lines; i++) {
            this.textFit(this.popup.lines.get(i), x + w / 2.0F, y + 24 + i * 11, -14012616, 0.75F, w - 12);
        }

        GuardMasterScreen.Popup p = this.popup;
        Runnable ok = () -> {
            this.popup = null;
            if (p.onOk != null) {
                p.onOk.run();
            }
        };
        if (p.cancel == null) {
            this.button(x + w / 2 - 30, y + h - 20, 60, 15, p.ok, false, true, ok);
        } else {
            this.button(x + w / 2 - 64, y + h - 20, 60, 15, p.ok, false, true, ok);
            this.button(x + w / 2 + 4, y + h - 20, 60, 15, p.cancel, false, true, () -> this.popup = null);
        }

        this.keyInput = null;
        this.confirmAction = ok;
    }

    private void drawKeypad() {
        this.hots.clear();
        this.fill(0, 0, 400, 225, Integer.MIN_VALUE);
        GuardMasterScreen.Keypad kp = this.keypad;
        int x = 130;
        int y = 14;
        int w = 140;
        int h = 198;
        this.fill(x, y, w, h, -11906724);
        this.frame(x, y, w, h, -14802135);
        this.textC(kp.title, x + w / 2.0F, y + 6, -1, 0.8F);
        this.fill(x + 10, y + 18, w - 20, 16, -1);
        this.textC(kp.masked ? "*".repeat(kp.value.length()) : kp.value, x + w / 2.0F, y + 23, -14012616, 0.9F);
        String[] keys = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "#", "0", "*"};

        for (int i = 0; i < 12; i++) {
            String d = keys[i];
            this.button(x + 10 + i % 3 * 41, y + 40 + i / 3 * 34, 38, 30, d, false, true, () -> {
                if (kp.value.length() < kp.maxLen) {
                    kp.value = kp.value + d;
                }
            });
        }

        Runnable ok = () -> {
            String v = kp.value;
            kp.value = "";
            kp.onOk.accept(v);
        };
        this.button(x + 10, y + h - 22, 58, 16, wp("ok"), false, true, ok);
        this.button(x + w - 68, y + h - 22, 58, 16, wp("cancel"), false, true, () -> {
            this.keypad = null;
            if (kp.onCancel != null) {
                kp.onCancel.run();
            }
        });
        this.keyInput = c -> {
            if (c.equals("\b")) {
                if (!kp.value.isEmpty()) {
                    kp.value = kp.value.substring(0, kp.value.length() - 1);
                }
            } else if (kp.value.length() < kp.maxLen && c.matches("\\d")) {
                kp.value = kp.value + c;
            }
        };
        this.confirmAction = ok;
    }

    private void hardKey(String k) {
        this.keyFlash = k;
        this.keyFlashUntil = System.currentTimeMillis() + 150L;
        switch (k) {
            case "세대":
                this.popup = null;
                this.keypad = null;
                this.go(GuardMasterScreen.Page.CALL);
                this.setTab(GuardMasterScreen.Page.CALL, 0);
                break;
            case "경비":
                this.popup = null;
                this.keypad = null;
                this.go(GuardMasterScreen.Page.CALL);
                this.setTab(GuardMasterScreen.Page.CALL, 1);
                break;
            case "로비":
                this.popup = null;
                this.keypad = null;
                this.go(GuardMasterScreen.Page.CALL);
                this.setTab(GuardMasterScreen.Page.CALL, 2);
                break;
            case "문열림":
                this.send(ReceiverBlockEntity.Action.GUARD_DOOR);
                break;
            case "*":
                if (this.keyInput != null) {
                    this.keyInput.accept("\b");
                }
                break;
            default:
                if (this.keyInput != null) {
                    this.keyInput.accept(k);
                }
        }
    }

    private void fnKey(int i) {
        this.popup = null;
        this.keypad = null;
        switch (i) {
            case 0:
                this.go(GuardMasterScreen.Page.CALL);
                this.setTab(GuardMasterScreen.Page.CALL, 3);
                break;
            case 1:
                this.go(GuardMasterScreen.Page.INQUIRY);
                this.setTab(GuardMasterScreen.Page.INQUIRY, 0);
                break;
            case 2:
                this.go(GuardMasterScreen.Page.SETTINGS);
                this.setTab(GuardMasterScreen.Page.SETTINGS, 3);
                break;
            default:
                for (int[] a : this.alertIdx()) {
                    if (this.data.records().get(a[0]).caller().endsWith("|0")) {
                        this.send(ReceiverBlockEntity.Action.CONFIRM_ALERT, String.valueOf(a[1]));
                    }
                }
        }
    }

    private void jog(int dir) {
        this.jogAngle += dir * 30;
        if (this.page == GuardMasterScreen.Page.MAIN) {
            GuardMasterScreen.Page[] order = new GuardMasterScreen.Page[]{
                GuardMasterScreen.Page.SECURITY, GuardMasterScreen.Page.CALL, GuardMasterScreen.Page.INQUIRY, GuardMasterScreen.Page.SETTINGS
            };
            this.go(order[Math.floorMod(dir > 0 ? 0 : 3, 4)]);
        } else {
            this.listPage = Math.max(0, this.listPage + dir);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) {
            return true;
        } else if (button != 0) {
            return false;
        } else {
            double ux = (mx - this.bx) / this.s;
            double uy = (my - this.by) / this.s;
            if (ux >= 8.0 && ux < 126.0 && uy >= 6.0 && uy < 476.0) {
                this.click();
                if (this.callState() != CallState.IDLE) {
                    this.send(ReceiverBlockEntity.Action.PICKUP);
                } else {
                    this.go(GuardMasterScreen.Page.CALL);
                }

                return true;
            } else {
                for (int r = 0; r < 4; r++) {
                    for (int c = 0; c < 4; c++) {
                        int w = c == 3 ? 50 : 40;
                        if (ux >= KEY_X[c] - w / 2.0 && ux <= KEY_X[c] + w / 2.0 && uy >= KEY_Y[r] - 13 && uy <= KEY_Y[r] + 13) {
                            this.click();
                            this.hardKey(KEYS[r][c]);
                            return true;
                        }
                    }
                }

                double dx = ux - 478.0;
                double dy = uy - 358.0;
                double d = Math.sqrt(dx * dx + dy * dy);
                if (d <= 58.0) {
                    this.click();
                    if (d <= 24.0) {
                        if (this.confirmAction != null) {
                            this.confirmAction.run();
                        }
                    } else {
                        this.jog(dx >= 0.0 ? 1 : -1);
                    }

                    return true;
                } else {
                    for (int i = 0; i < 4; i++) {
                        if (Math.abs(ux - FN_X[i]) <= 14.0 && uy >= 254.0 && uy <= 276.0) {
                            this.click();
                            this.fnKey(i);
                            return true;
                        }
                    }

                    double sx = (ux - 187.0) / 0.8825F;
                    double sy = (uy - 36.0) / 0.8825F;
                    if (!(sx < 0.0) && !(sy < 0.0) && !(sx >= 400.0) && !(sy >= 225.0)) {
                        for (int ix = this.hots.size() - 1; ix >= 0; ix--) {
                            GuardMasterScreen.Hot h = this.hots.get(ix);
                            if (sx >= h.x && sx < h.x + h.w && sy >= h.y && sy < h.y + h.h) {
                                this.click();
                                h.action.run();
                                return true;
                            }
                        }

                        return true;
                    } else {
                        return false;
                    }
                }
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        double ux = (mx - this.bx) / this.s;
        double uy = (my - this.by) / this.s;
        double dx = ux - 478.0;
        double dy = uy - 358.0;
        if (dx * dx + dy * dy <= 5046.0) {
            this.jog(delta > 0.0 ? -1 : 1);
            return true;
        } else {
            return super.mouseScrolled(mx, my, delta);
        }
    }

    private void click() {
        float vol = parse(this.data.setting("vol_system", "5"), 5) / 10.0F;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.6F, vol * 0.6F));
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (this.msgBox != null && this.msgBox.isFocused() && this.msgBox.visible) {
            if (key != 257 && key != 335) {
                return super.keyPressed(key, scan, mods);
            } else {
                this.sendMessage();
                return true;
            }
        } else if (key != 256 || this.popup == null && this.keypad == null) {
            if ((key == 257 || key == 335) && this.confirmAction != null) {
                this.confirmAction.run();
                return true;
            } else if (key == 259 && this.keyInput != null) {
                this.keyInput.accept("\b");
                return true;
            } else {
                return super.keyPressed(key, scan, mods);
            }
        } else {
            if (this.keypad != null && this.keypad.onCancel != null) {
                this.keypad.onCancel.run();
            }

            this.popup = null;
            this.keypad = null;
            return true;
        }
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (this.msgBox != null && this.msgBox.isFocused() && this.msgBox.visible) {
            return super.charTyped(c, mods);
        } else if (this.keyInput == null || !Character.isDigit(c) && c != '*' && c != '#') {
            return super.charTyped(c, mods);
        } else {
            this.keyInput.accept(String.valueOf(c));
            return true;
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.msgBox.tick();
        if (++this.refreshTimer >= 40) {
            this.refreshTimer = 0;
            this.send(ReceiverBlockEntity.Action.REFRESH);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record Hot(int x, int y, int w, int h, Runnable action) {
    }

    private static final class Keypad {
        final String title;
        final boolean masked;
        final int maxLen;
        final Consumer<String> onOk;
        final Runnable onCancel;
        String value = "";

        Keypad(String title, boolean masked, int maxLen, Consumer<String> onOk, Runnable onCancel) {
            this.title = title;
            this.masked = masked;
            this.maxLen = maxLen;
            this.onOk = onOk;
            this.onCancel = onCancel;
        }
    }

    private static enum Page {
        MAIN,
        SECURITY,
        CALL,
        INQUIRY,
        SETTINGS,
        TOUCH;
    }

    private record Popup(List<String> lines, String ok, String cancel, Runnable onOk) {
    }
}
