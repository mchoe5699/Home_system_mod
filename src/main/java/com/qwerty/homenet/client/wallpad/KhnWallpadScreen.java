package com.qwerty.homenet.client.wallpad;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.qwerty.homenet.HomeNet;
import com.qwerty.homenet.block.DeviceType;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.client.ReceiverScreen;
import com.qwerty.homenet.intercom.CallState;
import com.qwerty.homenet.intercom.Intercom;
import com.qwerty.homenet.intercom.IntercomLine;
import com.qwerty.homenet.intercom.MissedCall;
import com.qwerty.homenet.network.DeviceEntry;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
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
import net.minecraft.util.FormattedCharSequence;

public class KhnWallpadScreen extends Screen implements ReceiverScreen {
    private static final ResourceLocation BODY = HomeNet.id("textures/gui/wallpad_body.png");
    private static final ResourceLocation BG = HomeNet.id("textures/gui/wallpad_bg.png");
    private static final ResourceLocation BG_SUB = HomeNet.id("textures/gui/wallpad_bg_sub.png");
    private static final ResourceLocation ICONS = HomeNet.id("textures/gui/wallpad_icons.png");
    private static final ResourceLocation CAMS = HomeNet.id("textures/gui/wallpad_cams.png");
    private static final int BW = 705;
    private static final int BH = 475;
    private static final int SX = 104;
    private static final int SY = 99;
    private static final int SW = 494;
    private static final int SH = 278;
    private static final int W = 400;
    private static final int H = 225;
    private static final float K = 1.235F;
    private static final int[] BUTTON_Y = new int[]{115, 177, 239, 300, 361};
    private static final int BUTTON_X1 = 600;
    private static final int BUTTON_X2 = 646;
    private static final int IC_SECURITY = 0;
    private static final int IC_CONTROL = 1;
    private static final int IC_CALL = 2;
    private static final int IC_INQUIRY = 3;
    private static final int IC_SETTINGS = 4;
    private static final int IC_ENERGY = 5;
    private static final int IC_SUN = 6;
    private static final int IC_CLOUD = 7;
    private static final int IC_RAIN = 8;
    private static final int IC_SNOW = 9;
    private static final int IC_WIFI = 10;
    private static final int IC_PERSON = 11;
    private static final int IC_NOTICE = 12;
    private static final int IC_ELEVATOR = 13;
    private static final int IC_HOME = 14;
    private static final int IC_HELP = 15;
    private static final int IC_SHIELD = 16;
    private static final int IC_WRENCH = 17;
    private static final int IC_PHONE = 18;
    private static final int IC_MAGNIFIER = 19;
    private static final int IC_GAUGE = 20;
    private static final int IC_GEAR = 21;
    private static final int IC_BULB_ON = 22;
    private static final int IC_BULB_OFF = 23;
    private static final int IC_FAN = 24;
    private static final int IC_HEATER = 25;
    private static final int IC_AC = 26;
    private static final int IC_SENSOR_DOOR = 27;
    private static final int IC_SENSOR_WINDOW = 28;
    private static final int IC_GAS = 29;
    private static final int IC_SPEAKER = 30;
    private static final int IC_BRIGHT = 31;
    private static final int IC_LOCK = 32;
    private static final int CAM_DOOR = 0;
    private static final int CAM_LOBBY = 1;
    private static final int CAM_GUARD = 2;
    private static final int CAM_NEIGHBOR = 3;
    private static final int CAM_ELEVATOR = 4;
    private static final int CAM_CCTV = 5;
    private static final int CAM_NONE = 6;
    private static final int WHITE = -1;
    private static final int DARK = -14012616;
    private static final int DIM = -9735296;
    private static final int BLUE = -13722661;
    private static final int RED = -1750977;
    private static final int GREEN = -11746454;
    private static final int YELLOW = -11686;
    private static final KhnWallpadScreen.Page[] MENU = new KhnWallpadScreen.Page[]{
        KhnWallpadScreen.Page.SECURITY,
        KhnWallpadScreen.Page.CONTROL,
        KhnWallpadScreen.Page.CALL,
        KhnWallpadScreen.Page.INQUIRY,
        KhnWallpadScreen.Page.ENERGY,
        KhnWallpadScreen.Page.SETTINGS
    };
    private static final int[] MENU_ICON = new int[]{16, 17, 18, 19, 20, 21};
    private static final String[] MENU_KEY = new String[]{"security", "control", "call", "inquiry", "energy", "settings"};
    private static final String[] CONTROL_TABS = new String[]{"gas", "light", "heating", "vent", "aircon", "other"};
    private static final String[] CALL_TABS = new String[]{"front", "household", "guard", "phone"};
    private static final String[] INQUIRY_TABS = new String[]{"notice", "visitor", "repair", "vote", "fee", "elevator", "cctv", "parcel", "memo", "parking"};
    private static final String[] ENERGY_TABS = new String[]{"realtime", "average", "meter"};
    private static final String[] SETTING_TABS = new String[]{"sound", "password", "sms", "ars", "touch", "admin"};
    private static final String[] ENERGY_KEYS = new String[]{"elec", "water", "gas", "hotwater", "heat"};
    private static final String[] ENERGY_UNITS = new String[]{"kWh", "m³", "m³", "m³", "kWh"};
    private final BlockPos pos;
    private WallpadDataPacket data;
    private long[] lastEnergy = new long[0];
    private final boolean[] energyUp = new boolean[5];
    private KhnWallpadScreen.Page page = KhnWallpadScreen.Page.MAIN;
    private final Map<KhnWallpadScreen.Page, Integer> tabs = new LinkedHashMap<>();
    private int inquiryScroll;
    private int settingScroll;
    private GuiGraphics g;
    private float s;
    private int bx;
    private int by;
    private int mouseSX = -1;
    private int mouseSY = -1;
    private final List<KhnWallpadScreen.Hot> hots = new ArrayList<>();
    private Consumer<String> keyInput;
    private KhnWallpadScreen.Popup popup;
    private KhnWallpadScreen.Keypad keypad;
    private int calYear;
    private int calMonth;
    private String selectedRoom = "";
    private int selectedHeat;
    private int selectedVent;
    private int selectedAc;
    private int selectedGas;
    private int roomPage;
    private String unitInput = "";
    private String phoneInput = "";
    private boolean videoCall;
    private boolean videoBlocked = true;
    private boolean guardOffice;
    private long monitorUntil;
    private long connectedSince;
    private int brightness = 5;
    private int visitorIndex;
    private long confirmDeleteUntil;
    private long confirmClearUntil;
    private int cctvCam;
    private int energySel;
    private int repairSel = -1;
    private final Map<String, String> draft = new LinkedHashMap<>();
    private boolean pwUnlocked;
    private boolean adminUnlocked;
    private String pwAuth = "";
    private String pendingAuth = "";
    private String pw1 = "";
    private String pw2 = "";
    private int fieldFocus;
    private String adminUnit = "";
    private int touchStep;
    private int refreshTimer;
    private EditBox msgBox;
    private String msgDraft = "";
    private static final int CX = 80;
    private static final int CY = 38;
    private static final int CW = 315;
    private static final int CH = 183;
    private int videoX = 84;
    private int videoY = 42;
    private int videoW = 222;
    private int videoH = 153;
    private String adminGuard;
    private static final int[][] TOUCH_POINTS = new int[][]{{30, 30}, {370, 30}, {370, 195}, {30, 195}, {200, 112}};

    public KhnWallpadScreen(WallpadDataPacket data) {
        super(Component.translatable("block.qwertys_homenet.wallpad"));
        this.pos = data.pos();
        this.data = data;
        LocalDate now = LocalDate.now();
        this.calYear = now.getYear();
        this.calMonth = now.getMonthValue();
        this.lastEnergy = (long[])data.energy().clone();
        this.autoNavigate(CallState.IDLE);
        if (this.callState() != CallState.IDLE) {
            this.page = KhnWallpadScreen.Page.CALL;
            String k = data.outgoing() ? (this.isGuardPeer() ? "guard:" : "unit:") : this.key();
            this.tabs.put(KhnWallpadScreen.Page.CALL, k.startsWith("guard:") ? 2 : (k.startsWith("unit:") ? 1 : 0));
        }

        if (this.callState() == CallState.CONNECTED) {
            this.connectedSince = System.currentTimeMillis();
        }
    }

    @Override
    public BlockPos getPos() {
        return this.pos;
    }

    @Override
    public void update(WallpadDataPacket p) {
        CallState before = this.callState();
        long[] old = this.data.energy();
        this.data = p;

        for (int i = 0; i < 5 && i < p.energy().length && i < old.length; i++) {
            if (p.energy()[i] > old[i]) {
                this.energyUp[i] = true;
            }
        }

        if (this.callState() == CallState.CONNECTED && before != CallState.CONNECTED) {
            this.connectedSince = System.currentTimeMillis();
        }

        this.autoNavigate(before);
        if (!p.notice().isEmpty()) {
            this.handleNotice(p.notice());
        }
    }

    private CallState callState() {
        return CallState.byId(this.data.callState());
    }

    private boolean incoming() {
        return this.callState() != CallState.IDLE && !this.data.outgoing();
    }

    private String key() {
        return this.data.callerKey();
    }

    private void autoNavigate(CallState before) {
        CallState now = this.callState();
        if (now == CallState.RINGING && before != CallState.RINGING) {
            this.popup = null;
            this.keypad = null;
            this.page = KhnWallpadScreen.Page.CALL;
            String k = this.key();
            this.tabs.put(KhnWallpadScreen.Page.CALL, k.startsWith("guard:") ? 2 : (k.startsWith("unit:") ? 1 : 0));
            this.videoCall = false;
            this.videoBlocked = true;
        }
    }

    private void send(ReceiverBlockEntity.Action a) {
        ModNetwork.sendToServer(new WallpadActionPacket(this.pos, a));
    }

    private void send(ReceiverBlockEntity.Action a, String text) {
        ModNetwork.sendToServer(new WallpadActionPacket(this.pos, a.ordinal(), BlockPos.ZERO, text));
    }

    private void send(ReceiverBlockEntity.Action a, BlockPos target, String text) {
        ModNetwork.sendToServer(new WallpadActionPacket(this.pos, a.ordinal(), target, text));
    }

    private static String t(String key, Object... args) {
        return I18n.get("gui.qwertys_homenet.wp." + key, args);
    }

    private int tab(KhnWallpadScreen.Page p) {
        return this.tabs.getOrDefault(p, 0);
    }

    private void go(KhnWallpadScreen.Page p) {
        if (this.page != p) {
            this.page = p;
            this.draft.clear();
            this.fieldFocus = 0;
            if (p == KhnWallpadScreen.Page.SETTINGS) {
                this.onSettingsTab(this.tab(KhnWallpadScreen.Page.SETTINGS));
            }
        }
    }

    private void handleNotice(String n) {
        switch (n) {
            case "unlock_pw":
                this.pwUnlocked = true;
                this.pwAuth = this.pendingAuth;
                this.keypad = null;
                this.pw1 = "";
                this.pw2 = "";
                this.fieldFocus = 0;
                return;
            case "unlock_admin":
                this.adminUnlocked = true;
                this.keypad = null;
                this.adminUnit = this.data.unit();
                return;
            case "pw_saved":
                this.pwAuth = this.pw1;
                this.pw1 = "";
                this.pw2 = "";
            default:
                if (n.startsWith("energy_over:")) {
                    int i = parse(n.substring(12), 0);
                    this.info(t("notice.energy_over", t("energy." + ENERGY_KEYS[Math.max(0, Math.min(4, i))])));
                } else {
                    if (n.equals("unlock_fail")) {
                        this.keypad = null;
                    }

                    this.info(t("notice." + n));
                }
        }
    }

    private void info(String text) {
        this.popup = new KhnWallpadScreen.Popup(Arrays.asList(text.split("\n")), t("ok"), null, null, null);
    }

    private void confirm(String text, Runnable onOk) {
        this.popup = new KhnWallpadScreen.Popup(Arrays.asList(text.split("\n")), t("ok"), t("cancel"), onOk, null);
    }

    @Override
    protected void init() {
        this.layout();
        this.msgBox = new EditBox(
            this.font, this.width / 2 - 110, Math.min(this.height - 22, this.by + Math.round(475.0F * this.s) + 4), 170, 16, Component.literal("")
        );
        this.msgBox.setMaxLength(80);
        this.msgBox.setValue(this.msgDraft);
        this.msgBox.setResponder(v -> this.msgDraft = v);
        this.msgBox.setHint(Component.literal(t("message_hint")));
        this.addRenderableWidget(this.msgBox);
        this.addRenderableWidget(
            Button.builder(Component.literal(t("send")), b -> this.sendMessage()).bounds(this.width / 2 + 64, this.msgBox.getY() - 2, 46, 20).build()
        );
    }

    private void layout() {
        int availH = this.height - 34;
        this.s = Math.min((this.width - 16) / 705.0F, availH / 475.0F);
        this.s = Math.min(this.s, 1.6F);
        this.bx = Math.round((this.width - 705.0F * this.s) / 2.0F);
        this.by = Math.max(4, Math.round((availH - 475.0F * this.s) / 2.0F));
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
        if (this.brightness < 5) {
            this.fill(x, y, w, h, (5 - this.brightness) * 30 << 24);
        } else if (this.brightness > 5) {
            this.fill(x, y, w, h, (this.brightness - 5) * 18 << 24 | 16777215);
        }
    }

    private void hot(int x, int y, int w, int h, Runnable r) {
        this.hots.add(new KhnWallpadScreen.Hot(x, y, w, h, r));
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
        int color = selected ? -1 : (enabled ? -14012616 : -7433060);
        this.textFit(label, x + w / 2.0F, y + (h - 6) / 2.0F, color, 0.75F, w - 4);
        if (enabled) {
            this.hot(x, y, w, h, action);
        }
    }

    private void tabRow(int x, int y, int w, String group, String[] keys, int selected, int first, int count, Consumer<Integer> onSelect) {
        this.fill(x, y, w, 16, -1070319036);
        boolean arrows = count < keys.length;
        int ax = arrows ? 12 : 0;
        int tw = (w - ax * 2) / count;
        if (arrows) {
            this.text("<", x + 4, y + 4, first > 0 ? -1 : -10064778, 0.9F);
            this.text(">", x + w - 9, y + 4, first + count < keys.length ? -1 : -10064778, 0.9F);
            if (first > 0) {
                this.hot(x, y, ax, 16, () -> this.scrollTabs(group, -1));
            }

            if (first + count < keys.length) {
                this.hot(x + w - ax, y, ax, 16, () -> this.scrollTabs(group, 1));
            }
        }

        for (int i = 0; i < count && first + i < keys.length; i++) {
            int idx = first + i;
            int tx = x + ax + i * tw;
            boolean sel = idx == selected;
            if (sel) {
                this.g.fillGradient(tx + 2, y + 2, tx + tw - 2, y + 14, -11747851, -14710321);
            }

            this.textFit(t(group + "." + keys[idx]), tx + tw / 2.0F, y + 5, sel ? -1 : -2630429, 0.7F, tw - 6);
            this.hot(tx, y, tw, 16, () -> onSelect.accept(idx));
        }
    }

    private void scrollTabs(String group, int d) {
        if (group.equals("inquiry")) {
            this.inquiryScroll = Math.max(0, Math.min(INQUIRY_TABS.length - 5, this.inquiryScroll + d));
        } else {
            this.settingScroll = Math.max(0, Math.min(SETTING_TABS.length - 5, this.settingScroll + d));
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

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.g = graphics;
        this.renderBackground(graphics);
        this.hots.clear();
        this.keyInput = null;
        this.layout();
        int sxGui = this.bx + Math.round(104.0F * this.s);
        int syGui = this.by + Math.round(99.0F * this.s);
        this.mouseSX = (int)Math.floor(((mouseX - this.bx) / this.s - 104.0F) / 1.235F);
        this.mouseSY = (int)Math.floor(((mouseY - this.by) / this.s - 99.0F) / 1.235F);
        RenderSystem.enableBlend();
        PoseStack ps = graphics.pose();
        ps.pushPose();
        ps.translate((float)this.bx, (float)this.by, 0.0F);
        ps.scale(this.s, this.s, 1.0F);
        graphics.blit(BODY, 0, 0, 705, 475, 0.0F, 0.0F, 1410, 950, 1410, 950);
        this.drawButtonLeds();
        ps.pushPose();
        ps.translate(104.0F, 99.0F, 0.0F);
        ps.scale(1.235F, 1.235F, 1.0F);
        graphics.enableScissor(sxGui, syGui, sxGui + Math.round(494.0F * this.s), syGui + Math.round(278.0F * this.s));
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
            graphics.drawString(this.font, t("message_label"), this.msgBox.getX() - this.font.width(t("message_label")) - 6, this.msgBox.getY() + 4, -1);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private boolean blink() {
        return System.currentTimeMillis() / 400L % 2L == 0L;
    }

    private void drawButtonLeds() {
        CallState cs = this.callState();
        int[] colors = new int[5];
        if (this.data.flag("emergency") && this.blink()) {
            colors[0] = -50384;
        }

        if (this.data.flag("outing")) {
            colors[1] = -12723068;
        }

        if (this.incoming() && this.key().startsWith("guard:") && cs == CallState.RINGING && this.blink()) {
            colors[2] = -12737025;
        }

        if (cs == CallState.CONNECTED || cs == CallState.DIALING || cs == CallState.RINGING && this.blink()) {
            colors[3] = -12737025;
        }

        if (cs == CallState.CONNECTED && this.incoming() && !Intercom.isReceiverKey(this.key())) {
            colors[4] = -12737025;
        }

        for (int i = 0; i < 5; i++) {
            int y = BUTTON_Y[i];
            if (colors[i] != 0) {
                this.g.fill(603, y - 2, 643, y + 2, colors[i]);
                this.g.fill(601, y - 4, 645, y + 4, colors[i] & 16777215 | 1073741824);
            }
        }
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

        if (this.data.flag("alarm") && this.page != KhnWallpadScreen.Page.TOUCH) {
            this.drawAlarmBanner();
        }

        if (this.keypad != null) {
            this.drawKeypad();
        }

        if (this.popup != null) {
            this.drawPopup();
        }
    }

    private int outdoorTemp() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return 20;
        } else {
            int temp = Math.round(level.getBiome(this.pos).value().getBaseTemperature() * 25.0F);
            if (level.isRaining()) {
                temp -= 3;
            }

            return Math.max(-15, Math.min(40, temp));
        }
    }

    private void drawMain() {
        this.g.blit(BG, 0, 0, 400, 225, 0.0F, 0.0F, 800, 450, 800, 450);
        this.fill(0, 0, 132, 225, 1343236693);
        this.fill(132, 0, 1, 225, 1090519039);
        ClientLevel level = Minecraft.getInstance().level;
        boolean cold = level != null && level.getBiome(this.pos).value().getBaseTemperature() < 0.15F;
        String weather;
        int wicon;
        if (level != null && level.isThundering()) {
            weather = t("weather.thunder");
            wicon = 8;
        } else if (level != null && level.isRaining()) {
            weather = cold ? t("weather.snow") : t("weather.rain");
            wicon = cold ? 9 : 8;
        } else if (level != null && level.getDayTime() % 24000L > 12500L && level.getDayTime() % 24000L < 23500L) {
            weather = t("weather.clear_night");
            wicon = 7;
        } else {
            weather = t("weather.clear");
            wicon = 6;
        }

        this.icon(wicon, 8, 8, 40);
        this.shadowText(this.outdoorTemp() + "°", 56.0F, 10.0F, -1, 2.4F);
        this.shadowText(weather, 58.0F, 34.0F, -1, 0.75F);
        LocalDateTime now = LocalDateTime.now();
        String wd = t("wd." + now.getDayOfWeek().getValue() % 7);
        String clock = now.format(DateTimeFormatter.ofPattern("MM.dd HH:mm")) + (now.getHour() < 12 ? " AM" : " PM");
        this.shadowText(wd + " " + clock, 8.0F, 56.0F, -1, 0.72F);
        this.drawCalendar(4, 72);
        this.fill(0, 203, 132, 22, 1879048192);
        boolean net = !this.data.unit().isEmpty();
        this.icon(10, 6, 206, 16, net ? -11746454 : -1);
        this.hot(4, 204, 20, 20, () -> this.info(net ? t("status.net_ok", this.data.unit()) : t("status.net_none")));
        boolean outing = this.data.flag("outing");
        this.icon(11, 38, 206, 16, outing ? -11746454 : -1);
        this.hot(
            36, 204, 20, 20, () -> this.confirm(outing ? t("status.outing_off") : t("status.outing_on"), () -> this.send(ReceiverBlockEntity.Action.OUTING))
        );
        this.icon(12, 70, 206, 16);
        this.hot(68, 204, 20, 20, () -> {
            this.go(KhnWallpadScreen.Page.INQUIRY);
            this.tabs.put(KhnWallpadScreen.Page.INQUIRY, 0);
        });
        this.icon(13, 102, 206, 16);
        this.hot(100, 204, 20, 20, () -> {
            this.go(KhnWallpadScreen.Page.INQUIRY);
            this.tabs.put(KhnWallpadScreen.Page.INQUIRY, 5);
            this.inquiryScroll = 5;
        });
        if (!this.data.missed().isEmpty() || !this.data.visitors().isEmpty() && this.newVisitor()) {
            this.g.fill(114, 205, 118, 209, -1750977);
        }

        this.shadowText("HOME", 340.0F, 9.0F, -1, 0.9F);
        this.text("NET", 366.0F, 11.0F, -2036488, 0.6F);
        int[] xs = new int[]{160, 216, 272, 328};
        KhnWallpadScreen.Page[] row1 = new KhnWallpadScreen.Page[]{
            KhnWallpadScreen.Page.SECURITY, KhnWallpadScreen.Page.CONTROL, KhnWallpadScreen.Page.CALL, KhnWallpadScreen.Page.INQUIRY
        };
        int[] ic1 = new int[]{0, 1, 2, 3};
        String[] l1 = new String[]{"security", "control", "call", "inquiry"};

        for (int i = 0; i < 4; i++) {
            this.mainIcon(xs[i], 52, ic1[i], t("menu." + l1[i]), row1[i]);
        }

        this.mainIcon(xs[0], 122, 4, t("menu.settings"), KhnWallpadScreen.Page.SETTINGS);
        this.mainIcon(xs[1], 122, 5, t("menu.energy"), KhnWallpadScreen.Page.ENERGY);
        if (this.callState() != CallState.IDLE) {
            String st = this.callStatusLine();
            this.fill(140, 192, 254, 14, -1610612736);
            this.textC(st, 267.0F, 195.0F, -11686, 0.7F);
            this.hot(140, 192, 254, 14, () -> this.go(KhnWallpadScreen.Page.CALL));
        }
    }

    private boolean newVisitor() {
        return !this.data.visitors().isEmpty() && System.currentTimeMillis() - this.data.visitors().get(0).dayTime() < 600000L;
    }

    private void mainIcon(int x, int y, int idx, String label, KhnWallpadScreen.Page target) {
        boolean hv = this.hover(x, y, 44, 44);
        if (hv) {
            this.fill(x - 2, y - 2, 48, 48, 1090519039);
        }

        this.icon(idx, x, y, 44);
        float lw = this.width(label, 0.8F);
        this.shadowText(label, x + 22 - lw / 2.0F, y + 48, -1, 0.8F);
        this.hot(x - 4, y - 4, 52, 62, () -> this.go(target));
    }

    private void drawCalendar(int x, int y) {
        LocalDate first = LocalDate.of(this.calYear, this.calMonth, 1);
        LocalDate today = LocalDate.now();
        String header = t("cal.header", this.calMonth, this.calYear);
        this.shadowText("<", x + 4, y + 2, -1, 0.9F);
        this.hot(x, y, 16, 12, () -> this.shiftMonth(-1));
        this.textC(header, x + 62, y + 3, -1, 0.75F);
        this.shadowText(">", x + 116, y + 2, -1, 0.9F);
        this.hot(x + 110, y, 16, 12, () -> this.shiftMonth(1));
        this.fill(x, y + 13, 124, 10, 1358940493);
        String[] wd = new String[]{"M", "T", "W", "T", "F", "S", "S"};

        for (int i = 0; i < 7; i++) {
            this.textC(wd[i], x + 9 + i * 17.7F, y + 15, i == 6 ? -30080 : -1, 0.6F);
        }

        int offset = first.getDayOfWeek().getValue() - 1;
        int days = first.lengthOfMonth();

        for (int d = 1; d <= days; d++) {
            int cell = offset + d - 1;
            float cx = x + 9 + cell % 7 * 17.7F;
            int cy = y + 27 + cell / 7 * 17;
            boolean isToday = today.getYear() == this.calYear && today.getMonthValue() == this.calMonth && today.getDayOfMonth() == d;
            if (isToday) {
                this.fill(Math.round(cx) - 7, cy - 3, 15, 13, -1862270977);
            }

            DayOfWeek dw = first.plusDays(d - 1).getDayOfWeek();
            int c = isToday ? -1750977 : (dw == DayOfWeek.SUNDAY ? -20312 : -1);
            this.textC(String.valueOf(d), cx, cy, c, 0.62F);
        }
    }

    private void shiftMonth(int d) {
        LocalDate m = LocalDate.of(this.calYear, this.calMonth, 1).plusMonths(d);
        this.calYear = m.getYear();
        this.calMonth = m.getMonthValue();
    }

    private String callStatusLine() {
        CallState cs = this.callState();
        if (this.data.outgoing()) {
            return cs == CallState.DIALING ? t("call.dialing", this.peerDisplay()) : t("call.talking_with", this.peerDisplay(), this.talkTime());
        } else {
            String who = Intercom.sideName(this.key()).getString();
            return cs == CallState.RINGING ? t("call.incoming", who) : t("call.talking_with", who, this.talkTime());
        }
    }

    private String talkTime() {
        long sec = Math.max(0L, (System.currentTimeMillis() - this.connectedSince) / 1000L);
        return String.format("%02d:%02d", sec / 60L, sec % 60L);
    }

    private void drawAlarmBanner() {
        if (this.blink()) {
            this.fill(0, 0, 400, 16, -523231200);
            this.textC(this.data.flag("emergency") ? t("alarm.emergency") : t("alarm.test"), 170.0F, 4.0F, -1, 0.8F);
            this.fill(336, 2, 60, 12, -1);
            this.textC(t("security.stop"), 366.0F, 4.0F, -1750977, 0.7F);
            this.hot(336, 0, 64, 16, () -> this.send(ReceiverBlockEntity.Action.ALARM_STOP));
        }
    }

    private void drawSub() {
        this.g.blit(BG_SUB, 0, 0, 400, 225, 0.0F, 0.0F, 800, 450, 800, 450);
        this.icon(14, 4, 2, 14, this.hover(2, 0, 20, 18) ? -7351297 : -1);
        this.hot(0, 0, 24, 18, () -> this.go(KhnWallpadScreen.Page.MAIN));
        this.icon(15, 382, 2, 14, this.hover(378, 0, 22, 18) ? -7351297 : -1);
        this.hot(376, 0, 24, 18, () -> this.info(t("help." + this.page.name().toLowerCase(Locale.ROOT))));

        for (int i = 0; i < MENU.length; i++) {
            int y = 22 + i * 27;
            boolean sel = this.page == MENU[i];
            KhnWallpadScreen.Page target = MENU[i];
            if (sel) {
                this.g.fillGradient(3, y, 74, y + 22, -11747851, -14710321);
            } else {
                this.fill(3, y, 71, 22, this.hover(3, y, 71, 22) ? -1068475800 : -1606926263);
            }

            this.frame(3, y, 71, 22, sel ? -7351297 : -9735040);
            this.icon(MENU_ICON[i], 8, y + 4, 14, -1);
            this.text(t("menu." + MENU_KEY[i]), 27.0F, y + 8, -1, 0.75F);
            this.hot(3, y, 71, 22, () -> this.go(target));
        }

        this.frame(78, 19, 319, 204, -12605475);
        switch (this.page) {
            case SECURITY:
                this.drawSecurity();
                break;
            case CONTROL:
                this.drawControl();
                break;
            case CALL:
                this.drawCall();
                break;
            case INQUIRY:
                this.drawInquiry();
                break;
            case ENERGY:
                this.drawEnergy();
                break;
            case SETTINGS:
                this.drawSettings();
        }
    }

    private void drawSecurity() {
        int tb = this.tab(KhnWallpadScreen.Page.SECURITY);
        this.tabRow(79, 20, 317, "security", new String[]{"set", "test"}, tb, 0, 2, i -> this.tabs.put(KhnWallpadScreen.Page.SECURITY, i));
        this.panel(84, 42, 216, 150);
        if (tb == 0) {
            boolean s1 = this.data.flag("sec1");
            boolean s2 = this.data.flag("sec2");
            this.securityTile(96, 68, 27, t("security.sensor1"), s1, () -> this.send(ReceiverBlockEntity.Action.SET_SETTING, "sec1=" + (s1 ? 0 : 1)));
            this.securityTile(162, 68, 28, t("security.sensor2"), s2, () -> this.send(ReceiverBlockEntity.Action.SET_SETTING, "sec2=" + (s2 ? 0 : 1)));
            this.securityTile(228, 68, 29, t("security.gas"), false, () -> this.info(t("security.gas_ok")));
            String mode = this.data.flag("outing") ? t("security.mode_out") : (!s1 && !s2 ? t("security.mode_off") : t("security.mode_home"));
            this.text(t("security.state", mode), 92.0F, 158.0F, -14012616, 0.75F);
            if (this.data.flag("emergency")) {
                this.text(t("alarm.emergency"), 92.0F, 174.0F, -1750977, 0.75F);
            }
        } else {
            this.icon(30, 100, 68, 40, -14012616);
            this.text(t("security.test_desc1"), 150.0F, 74.0F, -14012616, 0.75F);
            this.text(t("security.test_desc2"), 150.0F, 88.0F, -9735296, 0.7F);
            this.button(150, 114, 90, 22, t("security.test_btn"), this.data.flag("alarm"), true, () -> this.send(ReceiverBlockEntity.Action.ALARM_TEST));
        }

        int rx = 306;
        this.darkPanel(rx, 42, 85, 150);
        this.button(rx + 6, 54, 73, 24, t("security.reset"), false, true, () -> this.send(ReceiverBlockEntity.Action.ALARM_STOP));
        this.button(rx + 6, 86, 73, 24, t("security.stop"), false, true, () -> this.send(ReceiverBlockEntity.Action.ALARM_STOP));
        this.button(rx + 6, 118, 73, 24, t("security.outing"), this.data.flag("outing"), true, () -> this.send(ReceiverBlockEntity.Action.OUTING));
        this.button(rx + 6, 150, 73, 24, t("security.emergency"), this.data.flag("emergency"), true, () -> this.send(ReceiverBlockEntity.Action.EMERGENCY));
        this.text(t("security.hint"), 86.0F, 200.0F, -12959412, 0.65F);
    }

    private void securityTile(int x, int y, int ic, String label, boolean on, Runnable r) {
        if (on) {
            this.g.fillGradient(x - 4, y - 4, x + 56, y + 66, -10237705, -13986858);
        } else {
            this.fill(x - 4, y - 4, 60, 70, this.hover(x - 4, y - 4, 60, 70) ? -1 : -1183758);
        }

        this.frame(x - 4, y - 4, 60, 70, on ? -15044955 : -6643285);
        this.icon(ic, x + 4, y, 44);
        this.textC(label, x + 26, y + 52, on ? -1 : -14012616, 0.7F);
        this.hot(x - 4, y - 4, 60, 70, r);
    }

    private List<DeviceEntry> devices(DeviceType... types) {
        List<DeviceEntry> out = new ArrayList<>();

        for (DeviceEntry e : this.data.devices()) {
            DeviceType dt = DeviceType.byId(e.type());

            for (DeviceType t : types) {
                if (dt == t && e.online()) {
                    out.add(e);
                }
            }
        }

        return out;
    }

    private String deviceName(DeviceEntry e, int idx) {
        return !e.name().isEmpty() ? e.name() : DeviceType.byId(e.type()).displayName().getString() + " " + (idx + 1);
    }

    private void drawControl() {
        int tb = this.tab(KhnWallpadScreen.Page.CONTROL);
        boolean hasOther = !this.devices(DeviceType.OUTLET, DeviceType.DOOR_LOCK, DeviceType.CURTAIN, DeviceType.OTHER).isEmpty();
        int count = hasOther ? 6 : 5;
        if (tb >= count) {
            tb = 0;
        }

        this.tabRow(79, 20, 317, "control", CONTROL_TABS, tb, 0, count, i -> {
            this.tabs.put(KhnWallpadScreen.Page.CONTROL, i);
            this.roomPage = 0;
        });
        switch (tb) {
            case 0:
                this.drawGas();
                break;
            case 1:
                this.drawLights();
                break;
            case 2:
                this.drawClimate(DeviceType.HEATING);
                break;
            case 3:
                this.drawVent();
                break;
            case 4:
                this.drawClimate(DeviceType.AIRCON);
                break;
            default:
                this.drawOther();
        }
    }

    private void noDevice(String typeKey) {
        this.panel(84, 42, 307, 175);
        this.textC(t("control.none", t("control." + typeKey)), 237.5F, 108.0F, -14012616, 0.8F);
        this.textC(t("control.none_hint"), 237.5F, 124.0F, -9735296, 0.65F);
    }

    private int roomButtons(List<String> names, int selected, Consumer<Integer> onSelect) {
        int rx = 306;
        this.darkPanel(rx, 42, 85, 153);
        int pages = Math.max(1, (names.size() + 3) / 4);
        this.roomPage = Math.min(this.roomPage, pages - 1);

        for (int i = 0; i < 4; i++) {
            int idx = this.roomPage * 4 + i;
            if (idx >= names.size()) {
                break;
            }

            this.button(rx + 6, 50 + i * 30, 73, 24, names.get(idx), idx == selected, true, () -> onSelect.accept(idx));
        }

        if (pages > 1) {
            this.button(rx + 6, 166, 34, 14, "<", false, this.roomPage > 0, () -> this.roomPage--);
            this.button(rx + 45, 166, 34, 14, ">", false, this.roomPage < pages - 1, () -> this.roomPage++);
        }

        return Math.min(selected, Math.max(0, names.size() - 1));
    }

    private void drawGas() {
        List<DeviceEntry> gas = this.devices(DeviceType.GAS);
        if (gas.isEmpty()) {
            this.noDevice("gas");
        } else {
            this.selectedGas = Math.min(this.selectedGas, gas.size() - 1);
            DeviceEntry e = gas.get(this.selectedGas);
            this.panel(84, 42, 216, 175);
            this.g.blit(ICONS, 110, 50, 160, 160, 0.0F, 640.0F, 256, 256, 1024, 1024);
            this.g.blit(ICONS, 110, 50, 160, 160, e.on() ? 256.0F : 512.0F, 640.0F, 256, 256, 1024, 1024);
            int rx = 306;
            this.darkPanel(rx, 42, 85, 175);
            String[] lines = t(e.on() ? "control.gas_open" : "control.gas_closed").split("\n");

            for (int i = 0; i < lines.length; i++) {
                this.textC(lines[i], rx + 42, 78 + i * 11, -1, 0.75F);
            }

            this.button(rx + 12, 118, 61, 22, t("control.close"), e.on(), e.on(), () -> this.send(ReceiverBlockEntity.Action.SET_POWER, e.pos(), "0"));
            if (gas.size() > 1) {
                List<String> names = new ArrayList<>();

                for (int i = 0; i < gas.size(); i++) {
                    names.add(this.deviceName(gas.get(i), i));
                }

                for (int i = 0; i < names.size() && i < 3; i++) {
                    int idx = i;
                    this.button(rx + 6, 154 + i * 18, 73, 15, names.get(i), i == this.selectedGas, true, () -> this.selectedGas = idx);
                }
            }

            this.text(t("control.gas_note"), 88.0F, 205.0F, -12959412, 0.6F);
        }
    }

    private static String roomOf(String name) {
        int sp = name.lastIndexOf(32);
        return sp > 0 ? name.substring(0, sp) : name;
    }

    private void drawLights() {
        List<DeviceEntry> lights = this.devices(DeviceType.LIGHT);
        if (lights.isEmpty()) {
            this.noDevice("light");
        } else {
            List<String> rooms = new ArrayList<>();
            Map<String, List<DeviceEntry>> byRoom = new LinkedHashMap<>();
            Map<DeviceEntry, String> labels = new HashMap<>();

            for (int i = 0; i < lights.size(); i++) {
                DeviceEntry e = lights.get(i);
                String name = this.deviceName(e, i);
                String room = e.name().isEmpty() ? t("control.light_all_room") : roomOf(name);
                labels.put(e, name.equals(room) ? name : name.substring(Math.min(name.length(), room.length())).trim());
                if (!byRoom.containsKey(room)) {
                    rooms.add(room);
                }

                byRoom.computeIfAbsent(room, k -> new ArrayList<>()).add(e);
            }

            if (!byRoom.containsKey(this.selectedRoom)) {
                this.selectedRoom = rooms.get(0);
            }

            int sel = rooms.indexOf(this.selectedRoom);
            this.roomButtons(rooms, sel, i -> this.selectedRoom = rooms.get(i));
            List<DeviceEntry> list = byRoom.get(this.selectedRoom);
            this.panel(84, 42, 216, 153);

            for (int i = 0; i < list.size() && i < 12; i++) {
                DeviceEntry e = list.get(i);
                int x = 92 + i % 4 * 52;
                int y = 48 + i / 4 * 48;
                boolean hv = this.hover(x, y, 46, 44);
                if (e.on()) {
                    this.g.fillGradient(x, y, x + 46, y + 44, -10237705, -13986858);
                } else {
                    this.fill(x, y, 46, 44, hv ? -1 : -1183758);
                }

                this.frame(x, y, 46, 44, e.on() ? -15044955 : -6643285);
                this.icon(e.on() ? 22 : 23, x + 9, y + 2, 28);
                this.textFit(labels.get(e), x + 23, y + 33, e.on() ? -1 : -14012616, 0.62F, 42.0F);
                this.hot(x, y, 46, 44, () -> this.send(ReceiverBlockEntity.Action.TOGGLE, e.pos(), ""));
            }

            boolean anyOn = list.stream().anyMatch(DeviceEntry::on);
            int by2 = 199;
            this.fill(84, by2 - 2, 307, 20, -1339543496);
            int sx = 90;
            this.g.fillGradient(sx, by2 + 1, sx + 40, by2 + 15, anyOn ? -10237705 : -1775636, anyOn ? -13986858 : -4603960);
            this.frame(sx, by2 + 1, 40, 14, -10919314);
            this.textC(anyOn ? "ON" : "OFF", sx + 20, by2 + 5, anyOn ? -1 : -14012616, 0.65F);
            this.hot(sx, by2, 40, 16, () -> {
                for (DeviceEntry e : list) {
                    this.send(ReceiverBlockEntity.Action.SET_POWER, e.pos(), anyOn ? "0" : "1");
                }
            });
            this.text(t("control.room_all", this.selectedRoom), sx + 46, by2 + 5, -1, 0.65F);
            this.button(303, by2, 84, 16, t("control.all_off"), false, true, () -> this.send(ReceiverBlockEntity.Action.ALL_LIGHTS_OFF));
        }
    }

    private void drawClimate(DeviceType type) {
        boolean heat = type == DeviceType.HEATING;
        List<DeviceEntry> list = this.devices(type);
        if (list.isEmpty()) {
            this.noDevice(heat ? "heating" : "aircon");
        } else {
            List<String> names = new ArrayList<>();

            for (int i = 0; i < list.size(); i++) {
                names.add(this.deviceName(list.get(i), i));
            }

            int sel = heat ? this.selectedHeat : this.selectedAc;
            sel = this.roomButtons(names, sel, i -> {
                if (heat) {
                    this.selectedHeat = i;
                } else {
                    this.selectedAc = i;
                }
            });
            DeviceEntry e = list.get(sel);
            this.panel(84, 42, 216, 153);
            this.icon(heat ? 25 : 26, 92, 48, 22);
            this.text(t(heat ? "control.heat_title" : "control.ac_title"), 120.0F, 56.0F, -14012616, 0.8F);
            this.textBox(96, 86, t("control.current"), e.roomTemp() + "℃");
            this.textBox(96, 118, t("control.set"), e.setTemp() + "℃");
            this.button(240, 84, 30, 26, "+", false, true, () -> this.send(ReceiverBlockEntity.Action.SET_TEMP, e.pos(), "+1"));
            this.button(240, 116, 30, 26, "−", false, true, () -> this.send(ReceiverBlockEntity.Action.SET_TEMP, e.pos(), "-1"));
            if (heat && e.away()) {
                this.text(t("control.away_on"), 96.0F, 150.0F, -13722661, 0.7F);
            }

            int by2 = 199;
            this.fill(84, by2 - 2, 307, 20, -1339543496);
            this.button(90, by2, 44, 16, t("control.power"), e.on(), true, () -> this.send(ReceiverBlockEntity.Action.SET_POWER, e.pos(), e.on() ? "0" : "1"));
            if (heat) {
                this.button(
                    138, by2, 44, 16, t("control.away"), e.away(), true, () -> this.send(ReceiverBlockEntity.Action.SET_AWAY, e.pos(), e.away() ? "0" : "1")
                );
            }

            this.text(e.on() ? t(heat ? "control.heat_on" : "control.ac_on") : t("control.off"), 190.0F, by2 + 5, e.on() ? -7348064 : -4669238, 0.65F);
        }
    }

    private void textBox(int x, int y, String label, String value) {
        this.fill(x, y, 54, 22, -10919314);
        this.textC(label, x + 27, y + 8, -1, 0.65F);
        this.fill(x + 58, y, 82, 22, -1);
        this.frame(x + 58, y, 82, 22, -7564387);
        this.textC(value, x + 99, y + 5, -14012616, 1.3F);
    }

    private void drawVent() {
        List<DeviceEntry> list = this.devices(DeviceType.VENT);
        if (list.isEmpty()) {
            this.noDevice("vent");
        } else {
            List<String> names = new ArrayList<>();

            for (int i = 0; i < list.size(); i++) {
                names.add(this.deviceName(list.get(i), i));
            }

            this.selectedVent = this.roomButtons(names, this.selectedVent, i -> this.selectedVent = i);
            DeviceEntry e = list.get(this.selectedVent);
            this.panel(84, 42, 216, 153);
            this.icon(24, 92, 48, 22);
            this.text(t("control.vent_title"), 120.0F, 56.0F, -14012616, 0.8F);
            String[] lv = new String[]{t("control.weak"), t("control.mid"), t("control.strong")};

            for (int i = 0; i < 3; i++) {
                int x = 102 + i * 62;
                int y = 88;
                boolean sel = e.on() && e.level() == i + 1;
                if (sel) {
                    this.g.fillGradient(x, y, x + 54, y + 54, -10237705, -13986858);
                } else {
                    this.fill(x, y, 54, 54, this.hover(x, y, 54, 54) ? -1 : -1183758);
                }

                this.frame(x, y, 54, 54, sel ? -15044955 : -6643285);
                int sz = 22 + i * 6;
                this.icon(24, x + 27 - sz / 2, y + 22 - sz / 2, sz);
                this.textC(lv[i], x + 27, y + 43, sel ? -1 : -14012616, 0.65F);
                int level = i + 1;
                this.hot(x, y, 54, 54, () -> this.send(ReceiverBlockEntity.Action.SET_LEVEL, e.pos(), String.valueOf(level)));
            }

            int by2 = 199;
            this.fill(84, by2 - 2, 307, 20, -1339543496);
            this.button(167, by2, 44, 16, t("control.power"), e.on(), true, () -> this.send(ReceiverBlockEntity.Action.SET_POWER, e.pos(), e.on() ? "0" : "1"));
        }
    }

    private void drawOther() {
        List<DeviceEntry> list = this.devices(DeviceType.OUTLET, DeviceType.DOOR_LOCK, DeviceType.CURTAIN, DeviceType.OTHER);
        this.panel(84, 42, 307, 175);

        for (int i = 0; i < list.size() && i < 16; i++) {
            DeviceEntry e = list.get(i);
            DeviceType dt = DeviceType.byId(e.type());
            int x = 90 + i % 2 * 152;
            int y = 48 + i / 2 * 20;
            this.button(
                x,
                y,
                146,
                17,
                this.deviceName(e, i) + "  ·  " + dt.stateName(e.on()).getString(),
                e.on(),
                true,
                () -> this.send(ReceiverBlockEntity.Action.TOGGLE, e.pos(), "")
            );
        }
    }

    private void drawCall() {
        int tb = this.tab(KhnWallpadScreen.Page.CALL);
        this.tabRow(79, 20, 317, "call", CALL_TABS, tb, 0, 4, i -> this.tabs.put(KhnWallpadScreen.Page.CALL, i));
        switch (tb) {
            case 0:
                this.drawFrontDoor();
                break;
            case 1:
                this.drawHousehold();
                break;
            case 2:
                this.drawGuard();
                break;
            default:
                this.drawPhone();
        }

        this.drawCallBar();
    }

    private void drawCallBar() {
        int y = 203;
        this.fill(80, y, 315, 18, -1070319036);
        this.icon(31, 84, y + 3, 12, -1);
        this.slider(100, y + 3, this.brightness, v -> this.brightness = v);
        boolean talking = this.callState() == CallState.CONNECTED;
        String volKey = talking ? "vol_call" : "vol_ring";
        int vol = parse(this.data.setting(volKey, "5"), 5);
        this.icon(30, 246, y + 3, 12, -1);
        this.slider(262, y + 3, vol, v -> this.send(ReceiverBlockEntity.Action.SET_SETTING, volKey + "=" + v));
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
            this.fill(bxx, y + 2, 7, 8, i < value ? -10919314 : -3091238);
            int v = i + 1;
            this.hot(bxx - 1, y, 10, 12, () -> set.accept(v));
        }
    }

    private void videoArea(int camIdx, boolean on) {
        this.fill(this.videoX - 1, this.videoY - 1, this.videoW + 2, this.videoH + 2, -10919314);
        if (on) {
            this.cam(camIdx, this.videoX, this.videoY, this.videoW, this.videoH);
        } else {
            this.fill(this.videoX, this.videoY, this.videoW, this.videoH, 1625877744);
        }
    }

    private void overlayLine(String s1, int color) {
        this.fill(this.videoX, this.videoY, this.videoW, 13, -1610612736);
        this.textFit(s1, this.videoX + this.videoW / 2.0F, this.videoY + 3, color, 0.7F, this.videoW - 6);
    }

    private void drawLog() {
        List<IntercomLine> log = this.data.log();
        int n = Math.min(3, log.size());
        int y = this.videoY + this.videoH - 4 - n * 9;

        for (int i = log.size() - n; i < log.size(); i++) {
            IntercomLine l = log.get(i);
            String line = "[" + Intercom.sideName(l.side()).getString() + "] " + l.name() + ": " + l.text();
            this.fill(this.videoX + 2, y - 1, this.videoW - 4, 9, -1879048192);
            List<FormattedCharSequence> parts = this.font.split(Component.literal(line), (int)((this.videoW - 8) / 0.6F));
            if (!parts.isEmpty()) {
                PoseStack ps = this.g.pose();
                ps.pushPose();
                ps.translate((float)(this.videoX + 4), (float)y, 0.0F);
                ps.scale(0.6F, 0.6F, 1.0F);
                this.g.drawString(this.font, parts.get(0), 0, 0, -1, false);
                ps.popPose();
            }

            y += 9;
        }
    }

    private void drawFrontDoor() {
        CallState cs = this.callState();
        String k = this.key();
        boolean doorCall = this.incoming() && !Intercom.isReceiverKey(k);
        boolean monitoring = !doorCall && System.currentTimeMillis() < this.monitorUntil;
        int camIdx = k.equals("lobby") ? 1 : 0;
        this.videoArea(doorCall ? camIdx : 0, doorCall || monitoring);
        if (doorCall) {
            this.overlayLine(
                cs == CallState.RINGING ? t("call.from_" + (k.equals("lobby") ? "lobby" : "front")) : t("call.talking", this.talkTime()),
                cs == CallState.RINGING ? -11686 : -6491984
            );
            this.drawLog();
        } else if (monitoring) {
            this.overlayLine(t("call.monitoring"), -1);
        } else if (this.callState() != CallState.IDLE) {
            this.overlayLine(this.callStatusLine(), -11686);
        }

        int rx = 312;
        this.darkPanel(rx - 4, 42, 83, 153);
        boolean ringing = doorCall && cs == CallState.RINGING;
        boolean talking = doorCall && cs == CallState.CONNECTED;
        this.button(rx + 2, 52, 71, 26, t("call.talk"), talking || ringing && this.blink(), this.callState() == CallState.IDLE || doorCall, () -> {
            if (ringing) {
                this.send(ReceiverBlockEntity.Action.ANSWER);
            } else if (talking) {
                this.send(ReceiverBlockEntity.Action.HANG_UP);
            } else {
                this.monitorUntil = monitoring ? 0L : System.currentTimeMillis() + 30000L;
            }
        });
        this.button(rx + 2, 84, 71, 26, t("call.open_door"), false, talking, () -> this.send(ReceiverBlockEntity.Action.OPEN_DOOR));
        this.button(rx + 2, 116, 71, 26, t("call.save_video"), false, doorCall, () -> this.send(ReceiverBlockEntity.Action.SAVE_VISITOR));
        if (!talking && !ringing) {
            this.text(t("call.open_note"), rx, 150.0F, -2630429, 0.55F);
        }
    }

    private void drawHousehold() {
        CallState cs = this.callState();
        boolean unitCall = this.incoming() && this.key().startsWith("unit:");
        boolean outUnit = this.data.outgoing() && !this.isGuardPeer();
        boolean ringing = unitCall && cs == CallState.RINGING;
        boolean talking = (unitCall || outUnit) && cs == CallState.CONNECTED;
        boolean dialing = outUnit && cs == CallState.DIALING;
        int lx = 84;
        this.panel(lx, 42, 222, 153);
        if (talking && this.videoCall && !this.videoBlocked) {
            this.cam(3, lx + 2, 44, 218, 149);
            this.overlayLineAt(lx + 2, 44, 218, t("call.video_talking", this.peerName(), this.talkTime()), -6491984);
        } else {
            this.fill(lx + 6, 48, 210, 18, -1);
            this.frame(lx + 6, 48, 210, 18, -7564387);
            String shown = this.unitInput.isEmpty() ? t("call.unit_hint") : this.unitInput;
            this.text(shown, lx + 10, 53.0F, this.unitInput.isEmpty() ? -6643285 : -14012616, 0.8F);
            this.button(lx + 196, 50, 18, 14, "<", false, !this.unitInput.isEmpty(), () -> this.unitInput = backspaceUnit(this.unitInput));
            String[] keys = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", t("call.dong"), "0", t("call.ho")};

            for (int i = 0; i < 12; i++) {
                int x = lx + 8 + i % 3 * 70;
                int y = 70 + i / 3 * 29;
                String kk = keys[i];
                int idx = i;
                this.button(x, y, 64, 25, kk, false, cs == CallState.IDLE, () -> this.unitKey(idx));
            }

            this.keyInput = cs == CallState.IDLE ? c -> {
                if (c.equals("\b")) {
                    this.unitInput = backspaceUnit(this.unitInput);
                } else if (c.matches("\\d") && this.unitInput.length() < 14) {
                    this.unitInput = this.unitInput + c;
                }
            } : null;
            if (ringing || dialing || talking) {
                String st = ringing
                    ? t("call.from_unit", this.peerName())
                    : (dialing ? t("call.dialing_unit", this.peerDisplay()) : t("call.talking_with", this.peerName(), this.talkTime()));
                this.fill(lx + 2, 98, 218, 22, -805306368);
                this.textFit(st, lx + 111, 105.0F, !ringing && !dialing ? -6491984 : -11686, 0.8F, 210.0F);
            }

            if (talking) {
                this.drawLogIn(lx + 2, 126);
            }
        }

        int rx = 312;
        this.darkPanel(rx - 4, 42, 83, 153);
        boolean idle = cs == CallState.IDLE;
        this.button(rx + 2, 52, 71, 26, t("call.voice"), (talking || dialing) && !this.videoCall || ringing && this.blink(), idle || ringing, () -> {
            this.videoCall = false;
            if (ringing) {
                this.send(ReceiverBlockEntity.Action.ANSWER);
            } else {
                this.dialUnit();
            }
        });
        String videoLabel = talking && this.videoCall ? t(this.videoBlocked ? "call.video_unblock" : "call.video_block") : t("call.video");
        this.button(rx + 2, 84, 71, 26, videoLabel, (talking || dialing) && this.videoCall, idle || ringing || talking && this.videoCall, () -> {
            if (talking) {
                this.videoBlocked = !this.videoBlocked;
            } else {
                this.videoCall = true;
                this.videoBlocked = true;
                if (ringing) {
                    this.send(ReceiverBlockEntity.Action.ANSWER);
                } else {
                    this.dialUnit();
                }
            }
        });
        this.button(rx + 2, 116, 71, 26, t("call.end"), false, ringing || talking || dialing, () -> this.send(ReceiverBlockEntity.Action.HANG_UP));
    }

    private void overlayLineAt(int x, int y, int w, String str, int color) {
        this.fill(x, y, w, 13, -1610612736);
        this.textFit(str, x + w / 2.0F, y + 3, color, 0.7F, w - 6);
    }

    private void drawLogIn(int x, int y) {
        List<IntercomLine> log = this.data.log();
        int n = Math.min(3, log.size());

        for (int i = 0; i < n; i++) {
            IntercomLine l = log.get(log.size() - n + i);
            String line = l.name() + ": " + l.text();
            this.fill(x, y + i * 9 - 1, 218, 9, -1879048192);
            this.text(this.font.plainSubstrByWidth(line, 356), x + 2, y + i * 9, -1, 0.6F);
        }
    }

    private boolean isGuardPeer() {
        return this.data.peer().startsWith("#guard") || this.data.peer().equals("#office");
    }

    private String peerDisplay() {
        if (this.data.peer().startsWith("#guard:")) {
            return t("guard_label_no", this.data.peer().substring(7));
        } else {
            String var1 = this.data.peer();

            return switch (var1) {
                case "#guard" -> t("guard_label");
                case "#office" -> t("office_label");
                default -> this.data.peer();
            };
        }
    }

    private String peerName() {
        return this.data.outgoing() ? this.peerDisplay() : Intercom.sideName(this.key()).getString();
    }

    private void unitKey(int idx) {
        if (this.unitInput.length() < 14) {
            if (idx == 9) {
                if (!this.unitInput.isEmpty() && !this.unitInput.contains("동") && Character.isDigit(this.unitInput.charAt(this.unitInput.length() - 1))) {
                    this.unitInput = this.unitInput + "동 ";
                }
            } else if (idx == 11) {
                if (!this.unitInput.isEmpty() && !this.unitInput.endsWith("호") && Character.isDigit(this.unitInput.charAt(this.unitInput.length() - 1))) {
                    this.unitInput = this.unitInput + "호";
                }
            } else {
                if (this.unitInput.endsWith("호")) {
                    return;
                }

                this.unitInput = this.unitInput + (idx == 10 ? "0" : String.valueOf(idx + 1));
            }
        }
    }

    private static String backspaceUnit(String s) {
        if (s.endsWith("동 ")) {
            return s.substring(0, s.length() - 2);
        } else {
            return s.isEmpty() ? s : s.substring(0, s.length() - 1);
        }
    }

    private void dialUnit() {
        String u = this.unitInput.trim();
        String norm = u.replaceAll("[^0-9A-Za-z가-힣]", "").replace("동", "").replace("호", "");
        String own = this.data.unit().replaceAll("[^0-9A-Za-z가-힣]", "").replace("동", "").replace("호", "");
        if (!norm.isEmpty() && !norm.equals(own)) {
            this.send(ReceiverBlockEntity.Action.DIAL, u);
        } else {
            this.popup = new KhnWallpadScreen.Popup(Arrays.asList(t("notice.bad_unit").split("\n")), t("ok"), t("cancel"), () -> this.unitInput = "", null);
        }
    }

    private void drawGuard() {
        CallState cs = this.callState();
        boolean guardCall = this.incoming() && this.key().startsWith("guard:");
        boolean outGuard = this.data.outgoing() && this.isGuardPeer();
        boolean ringing = guardCall && cs == CallState.RINGING;
        boolean talking = (guardCall || outGuard) && cs == CallState.CONNECTED;
        boolean dialing = outGuard && cs == CallState.DIALING;
        this.videoArea(2, talking || ringing);
        if (ringing) {
            this.overlayLine(t("call.from_guard", this.peerName()), -11686);
        } else if (dialing) {
            this.overlayLine(t("call.dialing_guard", this.peerDisplay()), -11686);
        } else if (talking) {
            this.overlayLine(t("call.talking_with", this.peerName(), this.talkTime()), -6491984);
            this.drawLog();
        }

        boolean office = outGuard ? this.data.peer().equals("#office") : this.guardOffice;
        int rx = 312;
        this.darkPanel(rx - 4, 42, 83, 153);
        String home = this.data.setting("guard_no", "");
        this.button(
            rx + 2,
            48,
            71,
            24,
            home.isEmpty() ? t("call.guard_btn") : t("guard_label_no", home),
            !office && (dialing || talking) || ringing && this.blink(),
            cs == CallState.IDLE || ringing,
            () -> {
                this.guardOffice = false;
                if (ringing) {
                    this.send(ReceiverBlockEntity.Action.ANSWER);
                } else {
                    this.send(ReceiverBlockEntity.Action.CALL_GUARD, "guard");
                }
            }
        );
        this.button(rx + 2, 76, 71, 24, t("call.office"), office && (dialing || talking), cs == CallState.IDLE, () -> {
            this.guardOffice = true;
            this.send(ReceiverBlockEntity.Action.CALL_GUARD, "office");
        });
        this.button(rx + 2, 104, 71, 24, t("call.talk"), talking || dialing, cs == CallState.IDLE || ringing, () -> {
            if (ringing) {
                this.send(ReceiverBlockEntity.Action.ANSWER);
            } else {
                this.send(ReceiverBlockEntity.Action.CALL_GUARD, this.guardOffice ? "office" : "guard");
            }
        });
        this.button(rx + 2, 132, 71, 24, t("call.finish"), false, ringing || talking || dialing, () -> this.send(ReceiverBlockEntity.Action.HANG_UP));
    }

    private void drawPhone() {
        int lx = 84;
        this.panel(lx, 42, 222, 153);
        this.fill(lx + 6, 48, 210, 18, -1);
        this.frame(lx + 6, 48, 210, 18, -7564387);
        this.text(this.phoneInput.isEmpty() ? t("call.phone_hint") : this.phoneInput, lx + 10, 53.0F, this.phoneInput.isEmpty() ? -6643285 : -14012616, 0.8F);
        this.button(
            lx + 196, 50, 18, 14, "<", false, !this.phoneInput.isEmpty(), () -> this.phoneInput = this.phoneInput.substring(0, this.phoneInput.length() - 1)
        );
        String[] keys = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#"};

        for (int i = 0; i < 12; i++) {
            String kk = keys[i];
            this.button(lx + 8 + i % 3 * 70, 70 + i / 3 * 29, 64, 25, kk, false, true, () -> {
                if (this.phoneInput.length() < 15) {
                    this.phoneInput = this.phoneInput + kk;
                }
            });
        }

        this.keyInput = c -> {
            if (c.equals("\b")) {
                if (!this.phoneInput.isEmpty()) {
                    this.phoneInput = this.phoneInput.substring(0, this.phoneInput.length() - 1);
                }
            } else if (this.phoneInput.length() < 15) {
                this.phoneInput = this.phoneInput + c;
            }
        };
        int rx = 312;
        this.darkPanel(rx - 4, 42, 83, 153);
        this.button(rx + 2, 52, 71, 26, t("call.talk"), false, !this.phoneInput.isEmpty(), () -> this.info(t("notice.no_line")));
        this.button(rx + 2, 84, 71, 26, t("call.end"), false, true, () -> this.phoneInput = "");
    }

    private void drawInquiry() {
        int tb = this.tab(KhnWallpadScreen.Page.INQUIRY);
        if (tb < this.inquiryScroll) {
            this.inquiryScroll = tb;
        }

        if (tb >= this.inquiryScroll + 5) {
            this.inquiryScroll = tb - 4;
        }

        this.tabRow(79, 20, 317, "inquiry", INQUIRY_TABS, tb, this.inquiryScroll, 5, i -> this.tabs.put(KhnWallpadScreen.Page.INQUIRY, i));
        String var2 = INQUIRY_TABS[tb];
        switch (var2) {
            case "notice":
                this.table(
                    new String[]{t("col.no"), t("col.title"), t("col.writer"), t("col.date")},
                    new float[]{0.1F, 0.45F, 0.2F, 0.25F},
                    List.of(),
                    t("inquiry.notice_empty")
                );
                break;
            case "visitor":
                this.drawVisitors();
                break;
            case "repair":
                this.drawRepair();
                break;
            case "vote":
                this.table(
                    new String[]{t("col.no"), t("col.title"), t("col.period"), t("col.result")},
                    new float[]{0.1F, 0.45F, 0.3F, 0.15F},
                    List.of(),
                    t("inquiry.vote_empty")
                );
                break;
            case "fee":
                this.drawFee();
                break;
            case "elevator":
                this.drawElevator();
                break;
            case "cctv":
                this.drawCctv();
                break;
            case "parcel":
                List<String[]> rows = new ArrayList<>();
                DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

                for (MissedCall m : this.data.records()) {
                    if (m.caller().startsWith("parcel|")) {
                        String[] p = m.caller().split("\\|");
                        rows.add(
                            new String[]{
                                f.format(Instant.ofEpochMilli(m.dayTime()).atZone(ZoneId.systemDefault())),
                                p.length > 2 && p[2].equals("1") ? t("inquiry.parcel_done") : t("inquiry.parcel_wait"),
                                p.length > 1 ? p[1] : ""
                            }
                        );
                    }
                }

                this.table(new String[]{t("col.date"), t("col.received"), t("col.box")}, new float[]{0.4F, 0.25F, 0.35F}, rows, t("inquiry.parcel_empty"));
                break;
            case "memo":
                this.drawMemo();
                break;
            default:
                this.table(new String[]{t("col.tag"), t("col.detail")}, new float[]{0.7F, 0.3F}, List.of(), t("inquiry.parking_empty"));
        }
    }

    private void table(String[] header, float[] cols, List<String[]> rows, String empty) {
        int x = 84;
        int y = 42;
        int w = 307;
        this.panel(x, y, w, 175);
        this.fill(x + 4, y + 4, w - 8, 14, -10919314);
        float cx = x + 4;

        for (int i = 0; i < header.length; i++) {
            float cw = (w - 8) * cols[i];
            this.textFit(header[i], cx + cw / 2.0F, y + 8, -1, 0.65F, cw - 4.0F);
            cx += cw;
        }

        for (int r = 0; r < 8; r++) {
            this.fill(x + 4, y + 18 + (r + 1) * 19, w - 8, 1, 1619036304);
        }

        if (rows.isEmpty()) {
            this.textC(empty, x + w / 2.0F, y + 80, -9735296, 0.75F);
        } else {
            for (int r = 0; r < rows.size() && r < 8; r++) {
                cx = x + 4;

                for (int i = 0; i < header.length; i++) {
                    float cw = (w - 8) * cols[i];
                    this.textFit(rows.get(r)[i], cx + cw / 2.0F, y + 25 + r * 19, -14012616, 0.65F, cw - 4.0F);
                    cx += cw;
                }
            }
        }
    }

    private void drawVisitors() {
        List<MissedCall> v = this.data.visitors();
        this.visitorIndex = Math.max(0, Math.min(this.visitorIndex, v.size() - 1));
        if (v.isEmpty()) {
            this.videoArea(6, false);
            this.textC(t("inquiry.visitor_empty"), this.videoX + this.videoW / 2.0F, this.videoY + 60, -9735296, 0.8F);
        } else {
            MissedCall m = v.get(this.visitorIndex);
            this.videoArea(m.caller().equals("lobby") ? 1 : 0, true);
            this.overlayLine(Intercom.sideName(m.caller()).getString(), -1);
        }

        int by2 = 199;
        this.fill(84, by2, 222, 16, -1070319036);
        if (!v.isEmpty()) {
            String when = DateTimeFormatter.ofPattern("yyyy.MM.dd  HH:mm:ss")
                .format(Instant.ofEpochMilli(v.get(this.visitorIndex).dayTime()).atZone(ZoneId.systemDefault()));
            this.fill(88, by2 + 2, 110, 12, -1512465);
            this.text(when, 92.0F, by2 + 5, -14012616, 0.6F);
            this.button(230, by2 + 2, 14, 12, "<", false, this.visitorIndex > 0, () -> this.visitorIndex--);
            this.textC(this.visitorIndex + 1 + "/" + v.size(), 260.0F, by2 + 5, -1, 0.6F);
            this.button(276, by2 + 2, 14, 12, ">", false, this.visitorIndex < v.size() - 1, () -> this.visitorIndex++);
        }

        int rx = 312;
        this.darkPanel(rx - 4, 42, 83, 153);
        long now = System.currentTimeMillis();
        boolean cDel = now < this.confirmDeleteUntil;
        boolean cClr = now < this.confirmClearUntil;
        this.button(rx + 2, 52, 71, 26, cDel ? t("ok") : t("inquiry.delete"), cDel, !v.isEmpty(), () -> {
            if (cDel) {
                this.send(ReceiverBlockEntity.Action.DELETE_VISITOR, String.valueOf(this.visitorIndex));
                this.confirmDeleteUntil = 0L;
            } else {
                this.confirmDeleteUntil = now + 3000L;
            }
        });
        this.button(rx + 2, 84, 71, 26, cClr ? t("ok") : t("inquiry.clear"), cClr, !v.isEmpty(), () -> {
            if (cClr) {
                this.send(ReceiverBlockEntity.Action.CLEAR_VISITORS);
                this.confirmClearUntil = 0L;
            } else {
                this.confirmClearUntil = now + 3000L;
            }
        });
        if (cDel || cClr) {
            this.text(t("inquiry.confirm_hint"), rx, 118.0F, -11686, 0.55F);
        }
    }

    private void drawRepair() {
        this.panel(84, 42, 307, 175);
        String[] items = new String[]{"elec", "civil", "arch", "comm", "etc", "result"};
        int[] ics = new int[]{23, 17, 14, 10, 12, 19};

        for (int i = 0; i < 6; i++) {
            int x = 120 + i % 3 * 82;
            int y = 52 + i / 3 * 70;
            boolean sel = this.repairSel == i;
            if (sel) {
                this.g.fillGradient(x, y, x + 64, y + 60, -10237705, -13986858);
            } else {
                this.fill(x, y, 64, 60, this.hover(x, y, 64, 60) ? -1 : -1183758);
            }

            this.frame(x, y, 64, 60, sel ? -15044955 : -6643285);
            this.icon(ics[i], x + 18, y + 6, 28, sel ? -1 : -11906464);
            this.textFit(t("repair." + items[i]), x + 32, y + 44, sel ? -1 : -14012616, 0.65F, 60.0F);
            int idx = i;
            this.hot(x, y, 64, 60, () -> {
                if (idx == 5) {
                    this.info(t("repair.no_result"));
                } else {
                    this.repairSel = idx;
                }
            });
        }

        this.button(315, 195, 70, 18, t("inquiry.repair"), false, this.repairSel >= 0 && this.repairSel < 5, () -> {
            this.info(t("repair.done", t("repair." + items[this.repairSel])));
            this.repairSel = -1;
        });
    }

    private void drawFee() {
        long[] e = this.data.energy();
        long[] cur = new long[5];
        long[] prev = new long[5];
        long[] prev2 = new long[5];

        for (int i = 0; i < 5 && e.length >= 15; i++) {
            cur[i] = e[i];
            prev[i] = e[5 + i];
            prev2[i] = e[10 + i];
        }

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{t("fee.common"), won(35000L), won(35000L), won(35000L)});
        rows.add(new String[]{t("fee.elec"), won(cur[0] / 1000L * 120L), won(prev[0] / 1000L * 120L), won(prev2[0] / 1000L * 120L)});
        rows.add(
            new String[]{
                t("fee.water"),
                won(cur[1] / 1000L * 700L + cur[3] / 1000L * 3000L),
                won(prev[1] / 1000L * 700L + prev[3] / 1000L * 3000L),
                won(prev2[1] / 1000L * 700L + prev2[3] / 1000L * 3000L)
            }
        );
        rows.add(
            new String[]{
                t("fee.heat"),
                won(cur[4] / 1000L * 90L + cur[2] / 1000L * 900L),
                won(prev[4] / 1000L * 90L + prev[2] / 1000L * 900L),
                won(prev2[4] / 1000L * 90L + prev2[2] / 1000L * 900L)
            }
        );
        rows.add(new String[]{t("fee.etc"), won(0L), won(0L), won(0L)});
        this.table(
            new String[]{t("col.item"), t("col.this_month"), t("col.last_month"), t("col.prev_month")}, new float[]{0.31F, 0.23F, 0.23F, 0.23F}, rows, ""
        );
    }

    private static String won(long v) {
        return String.format("%,d", v);
    }

    private void drawElevator() {
        this.videoArea(4, true);
        int rx = 312;
        this.darkPanel(rx - 4, 42, 83, 153);
        this.button(rx + 14, 60, 47, 40, "▲", false, true, () -> this.info(t("notice.elevator_called")));
        this.button(rx + 14, 112, 47, 40, "▼", false, true, () -> this.info(t("notice.elevator_called")));
    }

    private void drawCctv() {
        this.videoArea(5, true);
        String[] cams = new String[]{t("cctv.gate"), t("cctv.play1"), t("cctv.play2"), t("cctv.parking")};
        this.overlayLine(cams[this.cctvCam], -1);
        int rx = 312;
        this.darkPanel(rx - 4, 42, 83, 153);

        for (int i = 0; i < cams.length; i++) {
            int idx = i;
            this.button(rx + 2, 50 + i * 28, 71, 24, cams[i], this.cctvCam == i, true, () -> this.cctvCam = idx);
        }
    }

    private void drawMemo() {
        this.table(new String[]{t("col.no"), t("col.memo"), t("col.rec_time")}, new float[]{0.15F, 0.6F, 0.25F}, List.of(), t("inquiry.memo_empty"));
        int y = 195;
        this.fill(88, y, 299, 18, -1070319036);
        String[] ctl = new String[]{"●", "▶", "■"};

        for (int i = 0; i < 3; i++) {
            this.button(200 + i * 26, y + 2, 22, 14, ctl[i], false, true, () -> this.info(t("notice.no_mic")));
        }
    }

    private void drawEnergy() {
        int tb = this.tab(KhnWallpadScreen.Page.ENERGY);
        this.tabRow(79, 20, 317, "energy", ENERGY_TABS, tb, 0, 3, ix -> this.tabs.put(KhnWallpadScreen.Page.ENERGY, ix));
        long[] e = this.data.energy();
        if (e.length < 15) {
            e = new long[15];
        }

        switch (tb) {
            case 0:
                this.panel(84, 42, 307, 175);

                for (int i = 0; i < 5; i++) {
                    int x = 92 + i * 60;
                    this.fill(x, 50, 54, 26, -10919314);
                    this.textC(t("energy." + ENERGY_KEYS[i]), x + 27, 53.0F, -1, 0.75F);
                    this.textC(ENERGY_UNITS[i], x + 27, 65.0F, -3616550, 0.55F);
                    this.g.fillGradient(x, 80, x + 54, 198, -328708, -3880238);
                    this.frame(x, 80, 54, 118, -7564387);
                    boolean up = this.energyUp[i];
                    this.textC(up ? "▲" : "▲", x + 27, 96.0F, up ? -1750977 : -5196098, 1.6F);
                    this.fill(x + 4, 134, 46, 30, -1);
                    this.frame(x + 4, 134, 46, 30, -7564387);
                    long v = e[i] / 1000L;
                    this.textC(String.format("%03d", Math.min(999L, v)), x + 27, 142.0F, -14012616, 1.5F);
                }
                break;
            case 1:
                this.drawEnergyAverage(e);
                break;
            default:
                List<String[]> rows = new ArrayList<>();

                for (int i = 0; i < 5; i++) {
                    rows.add(new String[]{t("energy." + ENERGY_KEYS[i]) + "(" + ENERGY_UNITS[i] + ")", fmt(e[10 + i]), fmt(e[5 + i]), fmt(e[i])});
                }

                this.table(
                    new String[]{t("col.item"), t("col.prev_month"), t("col.last_month"), t("col.this_month")},
                    new float[]{0.31F, 0.23F, 0.23F, 0.23F},
                    rows,
                    ""
                );
                this.text(t("energy.meter_note"), 90.0F, 205.0F, -12959412, 0.58F);
        }
    }

    private static String fmt(long milli) {
        return String.format("%.2f", milli / 1000.0);
    }

    private void drawEnergyAverage(long[] e) {
        this.panel(84, 42, 307, 175);

        for (int i = 0; i < 5; i++) {
            int idx = i;
            this.button(88, 48 + i * 24, 44, 20, t("energy." + ENERGY_KEYS[i]), this.energySel == i, true, () -> this.energySel = idx);
        }

        int gx = 140;
        int gy = 48;
        int gw = 244;
        int gh = 120;
        this.fill(gx, gy, gw, gh, -1);
        this.frame(gx, gy, gw, gh, -7564387);

        for (int r = 1; r < 5; r++) {
            this.fill(gx + 1, gy + r * gh / 5, gw - 2, 1, -1907222);
        }

        int target = parse(this.data.setting("target_" + this.energySel, "0"), 0);
        long[] vals = new long[]{e[this.energySel] / 1000L, e[5 + this.energySel] / 1000L, e[10 + this.energySel] / 1000L, target};
        String[] labels = new String[]{t("energy.this_month"), t("energy.last_month"), t("energy.prev_month"), t("energy.target")};
        int[] colors = new int[]{-13722661, -8405786, -4663314, -1007552};
        long max = 1L;

        for (long v : vals) {
            max = Math.max(max, v);
        }

        for (int i = 0; i < 4; i++) {
            int bw = 34;
            int x = gx + 20 + i * 58;
            int h = (int)(vals[i] * (gh - 30) / max);
            this.fill(x, gy + gh - 16 - h, bw, h, colors[i]);
            this.textC(String.valueOf(vals[i]), x + bw / 2.0F, gy + gh - 26 - h, -14012616, 0.6F);
            this.textC(labels[i], x + bw / 2.0F, gy + gh - 12, -9735296, 0.58F);
        }

        int by2 = 191;
        this.fill(140, by2, 244, 20, -1512465);
        this.text(t("energy.target_line", target, ENERGY_UNITS[this.energySel], vals[0]), 144.0F, by2 + 7, -14012616, 0.6F);
        this.button(
            320,
            by2 + 2,
            62,
            16,
            t("energy.set_target"),
            false,
            true,
            () -> {
                int sel = this.energySel;
                this.keypad = new KhnWallpadScreen.Keypad(
                    t("energy." + ENERGY_KEYS[sel]),
                    false,
                    6,
                    true,
                    v -> this.send(ReceiverBlockEntity.Action.SET_SETTING, "target_" + sel + "=" + (v.isEmpty() ? "0" : v)),
                    null
                );
                this.keypad.value = target > 0 ? String.valueOf(target) : "";
            }
        );
    }

    private void onSettingsTab(int idx) {
        this.draft.clear();
        this.fieldFocus = 0;
        String var2 = SETTING_TABS[idx];
        switch (var2) {
            case "password":
                if (!this.pwUnlocked) {
                    this.keypad = new KhnWallpadScreen.Keypad(t("settings.pw_title"), true, 8, false, v -> {
                        this.pendingAuth = v;
                        this.send(ReceiverBlockEntity.Action.UNLOCK, "pw:" + v);
                    }, () -> this.tabs.put(KhnWallpadScreen.Page.SETTINGS, 0));
                }
                break;
            case "admin":
                if (!this.adminUnlocked) {
                    this.keypad = new KhnWallpadScreen.Keypad(
                        t("settings.admin_title"),
                        true,
                        8,
                        false,
                        v -> this.send(ReceiverBlockEntity.Action.UNLOCK, "admin:" + v),
                        () -> this.tabs.put(KhnWallpadScreen.Page.SETTINGS, 0)
                    );
                }
                break;
            case "sms":
            case "ars":
                if (!this.data.flag("outing")) {
                    this.info(t("settings.outing_only"));
                }
                break;
            case "touch":
                this.page = KhnWallpadScreen.Page.TOUCH;
                this.touchStep = 0;
        }
    }

    private String dv(String key, String def) {
        return this.draft.containsKey(key) ? this.draft.get(key) : this.data.setting(key, def);
    }

    private void drawSettings() {
        int tb = this.tab(KhnWallpadScreen.Page.SETTINGS);
        if (tb < this.settingScroll) {
            this.settingScroll = tb;
        }

        if (tb >= this.settingScroll + 5) {
            this.settingScroll = tb - 4;
        }

        this.tabRow(79, 20, 317, "settings", SETTING_TABS, tb, this.settingScroll, 5, i -> {
            this.tabs.put(KhnWallpadScreen.Page.SETTINGS, i);
            this.onSettingsTab(i);
        });
        String var2 = SETTING_TABS[tb];
        switch (var2) {
            case "sound":
                this.drawSound();
                break;
            case "password":
                this.drawPassword();
                break;
            case "sms":
                this.drawSms();
                break;
            case "ars":
                this.drawArs();
                break;
            case "admin":
                this.drawAdmin();
                break;
            default:
                this.panel(84, 42, 307, 175);
                this.button(187, 108, 100, 24, t("settings.touch_start"), false, true, () -> {
                    this.page = KhnWallpadScreen.Page.TOUCH;
                    this.touchStep = 0;
                });
        }
    }

    private void checkbox(int x, int y, String label, String key) {
        boolean on = "1".equals(this.dv(key, "0"));
        this.fill(x, y, 9, 9, -1);
        this.frame(x, y, 9, 9, -10919314);
        if (on) {
            this.text("✔", x + 1, y + 1, -13722661, 0.75F);
        }

        this.text(label, x + 12, y + 2, -14012616, 0.6F);
        this.hot(x, y - 2, 14 + (int)this.width(label, 0.6F), 13, () -> this.draft.put(key, on ? "0" : "1"));
    }

    private void drawSound() {
        this.panel(84, 42, 307, 175);
        this.fill(90, 48, 30, 14, -10919314);
        this.textC(t("settings.notify"), 105.0F, 51.0F, -1, 0.65F);
        this.checkbox(128, 50, t("settings.notify_notice"), "notify_notice");
        this.checkbox(188, 50, t("settings.notify_parking"), "notify_parking");
        this.checkbox(248, 50, t("settings.notify_parcel"), "notify_parcel");
        this.checkbox(308, 50, t("settings.touch_sound"), "touch_sound");
        String[] keys = new String[]{"vol_call", "vol_ring", "vol_system"};

        for (int i = 0; i < 3; i++) {
            int y = 74 + i * 32;
            this.fill(90, y, 295, 24, -1907222);
            this.frame(90, y, 295, 24, -5196098);
            this.fill(94, y + 4, 64, 16, -10919314);
            this.textC(t("settings." + keys[i]), 126.0F, y + 8, -1, 0.65F);
            this.icon(30, 176, y + 5, 14, -14012616);
            String k = keys[i];
            this.slider(196, y + 6, parse(this.dv(k, "5"), 5), v -> this.draft.put(k, String.valueOf(v)));
        }

        this.button(275, 193, 52, 18, t("settings.reset"), false, true, () -> {
            this.draft.clear();
            this.send(ReceiverBlockEntity.Action.RESET_SOUND);
        });
        this.button(333, 193, 52, 18, t("settings.save"), false, true, this::saveDraft);
    }

    private void saveDraft() {
        for (Entry<String, String> en : this.draft.entrySet()) {
            this.send(ReceiverBlockEntity.Action.SET_SETTING, en.getKey() + "=" + en.getValue());
        }

        this.draft.clear();
        this.info(t("notice.saved"));
    }

    private void sideKeypad(int x, int y, Consumer<String> onKey) {
        String[] keys = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#"};

        for (int i = 0; i < 12; i++) {
            String kk = keys[i];
            this.button(x + i % 3 * 44, y + i / 3 * 34, 40, 30, kk, false, true, () -> onKey.accept(kk));
        }

        this.keyInput = c -> onKey.accept(c);
    }

    private void field(int x, int y, int w, String label, String value, boolean masked, boolean focused, Runnable onFocus) {
        this.fill(x, y, w, 30, -10919314);
        this.text(label, x + 4, y + 3, -1, 0.6F);
        this.fill(x + 3, y + 13, w - 6, 14, focused ? -1 : -1512465);
        this.frame(x + 3, y + 13, w - 6, 14, focused ? -13722661 : -7564387);
        String v = masked ? "*".repeat(value.length()) : value;
        this.text(v + (focused && this.blink() ? "_" : ""), x + 7, y + 17, -14012616, 0.7F);
        this.hot(x, y, w, 30, onFocus);
    }

    private void drawPassword() {
        this.panel(84, 42, 307, 175);
        if (!this.pwUnlocked) {
            this.textC(t("settings.pw_locked"), 237.5F, 108.0F, -9735296, 0.75F);
            this.button(197, 128, 80, 20, t("settings.pw_enter"), false, true, () -> this.onSettingsTab(1));
        } else {
            this.field(92, 52, 120, t("settings.pw_new"), this.pw1, true, this.fieldFocus == 0, () -> this.fieldFocus = 0);
            this.field(92, 90, 120, t("settings.pw_confirm"), this.pw2, true, this.fieldFocus == 1, () -> this.fieldFocus = 1);
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
            this.button(92, 130, 56, 20, t("settings.save"), false, true, () -> {
                if (this.pw1.length() == 4 && this.pw1.equals(this.pw2)) {
                    this.send(ReceiverBlockEntity.Action.SET_DOOR_PASSWORD, this.pwAuth + "|" + this.pw1);
                } else {
                    this.info(t("notice.pw_mismatch"));
                }
            });
            this.button(156, 130, 56, 20, t("settings.edit"), false, true, () -> {
                this.pw1 = "";
                this.pw2 = "";
                this.fieldFocus = 0;
            });
            this.text(t("settings.pw_note1"), 92.0F, 162.0F, -12959412, 0.58F);
            this.text(t("settings.pw_note2"), 92.0F, 172.0F, -12959412, 0.58F);
            this.text(t(this.data.flag("pw_default") ? "settings.pw_state_default" : "settings.pw_state_set"), 92.0F, 186.0F, -13722661, 0.6F);
        }
    }

    private void drawSms() {
        this.panel(84, 42, 307, 175);
        if (!this.data.flag("outing")) {
            this.textC(t("settings.outing_only"), 237.5F, 108.0F, -9735296, 0.75F);
        } else {
            String on = this.dv("sms_on", "");

            for (int i = 0; i < 5; i++) {
                int y = 48 + i * 22;
                int idx = i;
                String k = "sms" + (i + 1);
                this.fill(90, y, 100, 18, this.fieldFocus == i ? -1 : -1512465);
                this.frame(90, y, 100, 18, this.fieldFocus == i ? -13722661 : -7564387);
                this.text(this.dv(k, ""), 93.0F, y + 6, -14012616, 0.65F);
                this.hot(90, y, 100, 18, () -> this.fieldFocus = idx);
                boolean mms = on.length() > i && on.charAt(i) == '1';
                this.button(194, y + 2, 22, 14, "SMS", !mms, true, () -> this.draft.put("sms_on", setFlag(this.dv("sms_on", ""), idx, '0')));
                this.button(218, y + 2, 22, 14, "MMS", mms, true, () -> this.draft.put("sms_on", setFlag(this.dv("sms_on", ""), idx, '1')));
            }

            this.sideKeypad(250, 48, kx -> this.typeInto("sms" + (this.fieldFocus + 1), kx, 15));
            this.button(90, 191, 50, 18, t("settings.save"), false, true, this::saveDraft);
            this.button(146, 191, 50, 18, t("settings.edit"), false, true, () -> this.draft.put("sms" + (this.fieldFocus + 1), ""));
        }
    }

    private static String setFlag(String flags, int i, char c) {
        StringBuilder sb = new StringBuilder(flags);

        while (sb.length() <= i) {
            sb.append('0');
        }

        sb.setCharAt(i, c);
        return sb.toString();
    }

    private void typeInto(String key, String k, int max) {
        String cur = this.dv(key, "");
        if (k.equals("\b")) {
            if (!cur.isEmpty()) {
                this.draft.put(key, cur.substring(0, cur.length() - 1));
            }
        } else if (k.matches("\\d") && cur.length() < max) {
            this.draft.put(key, cur + k);
        }
    }

    private void drawArs() {
        this.panel(84, 42, 307, 175);
        if (!this.data.flag("outing")) {
            this.textC(t("settings.outing_only"), 237.5F, 108.0F, -9735296, 0.75F);
        } else {
            this.fill(90, 46, 80, 12, -10919314);
            this.text(t("settings.ars_phones"), 93.0F, 49.0F, -1, 0.6F);

            for (int i = 0; i < 3; i++) {
                int y = 60 + i * 18;
                int idx = i;
                this.fill(90, y, 80, 15, this.fieldFocus == i ? -1 : -1512465);
                this.frame(90, y, 80, 15, this.fieldFocus == i ? -13722661 : -7564387);
                this.text(this.dv("ars_phone" + (i + 1), ""), 93.0F, y + 5, -14012616, 0.6F);
                this.hot(90, y, 80, 15, () -> this.fieldFocus = idx);
            }

            this.fill(90, 120, 80, 12, -10919314);
            this.text(t("settings.ars_pw"), 93.0F, 123.0F, -1, 0.6F);
            this.fill(90, 134, 80, 15, this.fieldFocus == 3 ? -1 : -1512465);
            this.frame(90, 134, 80, 15, this.fieldFocus == 3 ? -13722661 : -7564387);
            this.text(this.dv("ars_pw", "FFFF"), 93.0F, 139.0F, -14012616, 0.6F);
            this.hot(90, 134, 80, 15, () -> this.fieldFocus = 3);
            this.text(t("settings.ars_auto"), 178.0F, 48.0F, -14012616, 0.6F);
            boolean auto = "1".equals(this.dv("ars_auto", "1"));
            this.button(178, 58, 30, 13, t("settings.ars_out"), auto, true, () -> this.draft.put("ars_auto", "1"));
            this.button(210, 58, 30, 13, t("settings.ars_off"), !auto, true, () -> this.draft.put("ars_auto", "0"));
            this.checkbox(178, 82, t("settings.ars_use"), "ars_use");
            this.text(t("settings.ars_rings"), 178.0F, 102.0F, -14012616, 0.6F);
            int rings = parse(this.dv("ars_rings", "10"), 10);
            this.button(178, 112, 14, 14, "−", false, rings > 1, () -> this.draft.put("ars_rings", String.valueOf(rings - 1)));
            this.textC(rings + t("settings.times"), 205.0F, 116.0F, -14012616, 0.65F);
            this.button(220, 112, 14, 14, "+", false, rings < 20, () -> this.draft.put("ars_rings", String.valueOf(rings + 1)));
            this.sideKeypad(250, 48, k -> {
                if (this.fieldFocus == 3) {
                    String cur = this.dv("ars_pw", "FFFF");
                    if (k.equals("\b")) {
                        this.draft.put("ars_pw", cur.isEmpty() ? cur : cur.substring(0, cur.length() - 1));
                    } else if (k.matches("\\d") && cur.length() < 8) {
                        this.draft.put("ars_pw", (cur.equals("FFFF") ? "" : cur) + k);
                    }
                } else {
                    this.typeInto("ars_phone" + (this.fieldFocus + 1), k, 15);
                }
            });
            this.button(90, 191, 50, 18, t("settings.save"), false, true, this::saveDraft);
            this.button(146, 191, 50, 18, t("settings.edit"), false, true, () -> {
                if (this.fieldFocus == 3) {
                    this.draft.put("ars_pw", "");
                } else {
                    this.draft.put("ars_phone" + (this.fieldFocus + 1), "");
                }
            });
        }
    }

    private void drawAdmin() {
        this.panel(84, 42, 307, 175);
        if (!this.adminUnlocked) {
            this.textC(t("settings.admin_locked"), 237.5F, 108.0F, -9735296, 0.75F);
            this.button(197, 128, 80, 20, t("settings.pw_enter"), false, true, () -> this.onSettingsTab(5));
        } else {
            if (this.adminGuard == null) {
                this.adminGuard = this.data.setting("guard_no", "");
            }

            this.text(t("settings.admin_unit"), 92.0F, 46.0F, -14012616, 0.65F);
            boolean f0 = this.fieldFocus != 1;
            this.fill(92, 56, 140, 16, -1);
            this.frame(92, 56, 140, 16, f0 ? -13722661 : -7564387);
            this.text(this.adminUnit + (f0 && this.blink() ? "_" : ""), 96.0F, 60.0F, -14012616, 0.72F);
            this.hot(92, 56, 140, 16, () -> this.fieldFocus = 0);
            this.text(t("settings.admin_current", this.data.unit().isEmpty() ? "-" : this.data.unit()), 92.0F, 75.0F, -9735296, 0.58F);
            this.button(92, 84, 50, 15, t("settings.save"), false, true, () -> this.send(ReceiverBlockEntity.Action.SET_UNIT, this.adminUnit.trim()));
            this.button(146, 84, 50, 15, t("settings.edit"), false, true, () -> {
                this.adminUnit = "";
                this.fieldFocus = 0;
            });
            this.text(t("settings.admin_guard"), 92.0F, 106.0F, -14012616, 0.58F);
            boolean f1 = this.fieldFocus == 1;
            this.fill(92, 116, 140, 16, -1);
            this.frame(92, 116, 140, 16, f1 ? -13722661 : -7564387);
            this.text(this.adminGuard + (f1 && this.blink() ? "_" : ""), 96.0F, 120.0F, -14012616, 0.72F);
            this.hot(92, 116, 140, 16, () -> this.fieldFocus = 1);
            String curGuard = this.data.setting("guard_no", "");
            this.text(
                t("settings.admin_guard_current", curGuard.isEmpty() ? t("settings.all_guards") : t("guard_label_no", curGuard)),
                92.0F,
                135.0F,
                -9735296,
                0.58F
            );
            this.button(92, 144, 50, 15, t("settings.save"), false, true, () -> {
                this.send(ReceiverBlockEntity.Action.SET_SETTING, "guard_no=" + this.adminGuard);
                this.info(t("notice.saved"));
            });
            this.button(146, 144, 50, 15, t("settings.edit"), false, true, () -> {
                this.adminGuard = "";
                this.fieldFocus = 1;
            });
            String[] keys = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", t("call.dong"), "0", t("call.ho")};

            for (int i = 0; i < 12; i++) {
                int idx = i;
                boolean enabled = this.fieldFocus != 1 || i != 9 && i != 11;
                this.button(248 + i % 3 * 44, 48 + i / 3 * 34, 40, 30, keys[i], false, enabled, () -> this.adminKey(idx));
            }

            this.keyInput = c -> {
                if (this.fieldFocus == 1) {
                    if (c.equals("\b")) {
                        this.adminGuard = this.adminGuard.isEmpty() ? "" : this.adminGuard.substring(0, this.adminGuard.length() - 1);
                    } else if (c.matches("\\d") && this.adminGuard.length() < 6) {
                        this.adminGuard = this.adminGuard + c;
                    }
                } else if (c.equals("\b")) {
                    this.adminUnit = backspaceUnit(this.adminUnit);
                } else if (c.matches("\\d") && this.adminUnit.length() < 14) {
                    this.adminUnit = this.adminUnit + c;
                }
            };
            this.text(t("settings.admin_hint"), 92.0F, 166.0F, -12959412, 0.55F);
            this.text("Model : KOCOM KHN-893N (8Type)  ·  S/W Ver : 3.2.15", 92.0F, 180.0F, -9735296, 0.55F);
            this.text(
                t("settings.devices", this.data.devices().size()) + "  ·  " + t("settings.visitors", this.data.visitors().size()),
                92.0F,
                190.0F,
                -9735296,
                0.55F
            );
        }
    }

    private void adminKey(int idx) {
        if (this.fieldFocus == 1) {
            if (idx != 9 && idx != 11 && this.adminGuard.length() < 6) {
                this.adminGuard = this.adminGuard + (idx == 10 ? "0" : String.valueOf(idx + 1));
            }
        } else {
            String saved = this.unitInput;
            this.unitInput = this.adminUnit;
            this.unitKey(idx);
            this.adminUnit = this.unitInput;
            this.unitInput = saved;
        }
    }

    private void drawTouch() {
        this.fill(0, 0, 400, 225, -1);
        this.textC(t("touch.line1"), 200.0F, 8.0F, -14012616, 0.65F);
        this.textC(t("touch.line2"), 200.0F, 18.0F, -14012616, 0.65F);
        int[] p = TOUCH_POINTS[Math.min(this.touchStep, 4)];
        this.fill(p[0] - 10, p[1], 21, 1, -14012616);
        this.fill(p[0], p[1] - 10, 1, 21, -14012616);
        this.hot(p[0] - 12, p[1] - 12, 25, 25, () -> {
            this.touchStep++;
            if (this.touchStep >= TOUCH_POINTS.length) {
                this.page = KhnWallpadScreen.Page.SETTINGS;
                this.tabs.put(KhnWallpadScreen.Page.SETTINGS, 0);
                this.info(t("notice.touch_done"));
            }
        });
        this.textC(this.touchStep + 1 + " / " + TOUCH_POINTS.length, 200.0F, 211.0F, -9735296, 0.6F);
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

        KhnWallpadScreen.Popup p = this.popup;
        if (p.cancel == null) {
            this.button(x + w / 2 - 30, y + h - 20, 60, 15, p.ok, false, true, () -> {
                this.popup = null;
                if (p.onOk != null) {
                    p.onOk.run();
                }
            });
        } else {
            this.button(x + w / 2 - 64, y + h - 20, 60, 15, p.ok, false, true, () -> {
                this.popup = null;
                if (p.onOk != null) {
                    p.onOk.run();
                }
            });
            this.button(x + w / 2 + 4, y + h - 20, 60, 15, p.cancel, false, true, () -> {
                this.popup = null;
                if (p.onCancel != null) {
                    p.onCancel.run();
                }
            });
        }

        this.keyInput = null;
    }

    private void drawKeypad() {
        this.hots.clear();
        this.fill(0, 0, 400, 225, Integer.MIN_VALUE);
        KhnWallpadScreen.Keypad kp = this.keypad;
        if (kp.energyStyle) {
            int x = 60;
            int y = 50;
            int w = 280;
            int h = 120;
            this.fill(x, y, w, h, -12959412);
            this.frame(x, y, w, h, -14802135);
            this.text("⚡ " + kp.title, x + 12, y + 14, -1, 1.0F);
            this.fill(x + 110, y + 10, 150, 20, -1);
            this.text(kp.value, x + 116, y + 16, -14012616, 0.9F);

            for (int i = 0; i < 10; i++) {
                String d = String.valueOf(i);
                this.button(x + 10 + i % 5 * 44, y + 44 + i / 5 * 34, 40, 30, d, false, true, () -> {
                    if (kp.value.length() < kp.maxLen) {
                        kp.value = kp.value + d;
                    }
                });
            }

            this.button(x + 230, y + 44, 40, 30, t("settings.save"), false, true, () -> {
                this.keypad = null;
                kp.onOk.accept(kp.value);
            });
            this.button(x + 230, y + 78, 40, 30, t("cancel"), false, true, () -> {
                this.keypad = null;
                if (kp.onCancel != null) {
                    kp.onCancel.run();
                }
            });
        } else {
            int x = 130;
            int y = 14;
            int w = 140;
            int h = 198;
            this.fill(x, y, w, h, -11906724);
            this.frame(x, y, w, h, -14802135);
            this.textC(kp.title, x + w / 2.0F, y + 6, -1, 0.8F);
            this.fill(x + 10, y + 18, w - 20, 16, -1);
            this.textC(kp.masked ? "*".repeat(kp.value.length()) : kp.value, x + w / 2.0F, y + 23, -14012616, 0.9F);
            String[] keys = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#"};

            for (int i = 0; i < 12; i++) {
                String d = keys[i];
                this.button(x + 10 + i % 3 * 41, y + 40 + i / 3 * 34, 38, 30, d, false, true, () -> {
                    if (kp.value.length() < kp.maxLen) {
                        kp.value = kp.value + d;
                    }
                });
            }

            this.button(x + 10, y + h - 22, 58, 16, t("ok"), false, true, () -> {
                String v = kp.value;
                kp.value = "";
                kp.onOk.accept(v);
            });
            this.button(x + w - 68, y + h - 22, 58, 16, t("cancel"), false, true, () -> {
                this.keypad = null;
                if (kp.onCancel != null) {
                    kp.onCancel.run();
                }
            });
        }

        this.keyInput = c -> {
            if (c.equals("\b")) {
                if (!kp.value.isEmpty()) {
                    kp.value = kp.value.substring(0, kp.value.length() - 1);
                }
            } else if (kp.value.length() < kp.maxLen && c.matches("\\d")) {
                kp.value = kp.value + c;
            }
        };
    }

    private static int parse(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception var3) {
            return def;
        }
    }

    private void physicalButton(int i) {
        CallState cs = this.callState();
        switch (i) {
            case 0:
                this.send(ReceiverBlockEntity.Action.EMERGENCY);
                break;
            case 1:
                this.send(ReceiverBlockEntity.Action.OUTING);
                break;
            case 2:
                this.page = KhnWallpadScreen.Page.CALL;
                this.tabs.put(KhnWallpadScreen.Page.CALL, 2);
                if (cs == CallState.RINGING && this.incoming() && this.key().startsWith("guard:")) {
                    this.send(ReceiverBlockEntity.Action.ANSWER);
                } else if (cs == CallState.IDLE) {
                    this.send(ReceiverBlockEntity.Action.CALL_GUARD, "guard");
                }
                break;
            case 3:
                if (cs == CallState.RINGING && this.incoming()) {
                    this.page = KhnWallpadScreen.Page.CALL;
                    this.send(ReceiverBlockEntity.Action.ANSWER);
                } else if (cs != CallState.IDLE) {
                    this.send(ReceiverBlockEntity.Action.HANG_UP);
                } else {
                    this.page = KhnWallpadScreen.Page.CALL;
                    this.tabs.put(KhnWallpadScreen.Page.CALL, 0);
                    this.monitorUntil = System.currentTimeMillis() + 30000L;
                }
                break;
            default:
                if (cs == CallState.CONNECTED && this.incoming() && !Intercom.isReceiverKey(this.key())) {
                    this.send(ReceiverBlockEntity.Action.OPEN_DOOR);
                }
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
            if (ux >= 596.0 && ux <= 650.0) {
                for (int i = 0; i < 5; i++) {
                    if (uy >= BUTTON_Y[i] - 10 && uy <= BUTTON_Y[i] + 18) {
                        this.click();
                        this.physicalButton(i);
                        return true;
                    }
                }
            }

            double sx = (ux - 104.0) / 1.235F;
            double sy = (uy - 99.0) / 1.235F;
            if (!(sx < 0.0) && !(sy < 0.0) && !(sx >= 400.0) && !(sy >= 225.0)) {
                for (int ix = this.hots.size() - 1; ix >= 0; ix--) {
                    KhnWallpadScreen.Hot h = this.hots.get(ix);
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

    private void click() {
        if ("1".equals(this.data.setting("touch_sound", "1"))) {
            float vol = parse(this.data.setting("vol_system", "5"), 5) / 10.0F;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.6F, vol * 0.6F));
        }
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
            if ((key == 257 || key == 335) && this.keypad != null) {
                KhnWallpadScreen.Keypad kp = this.keypad;
                if (kp.energyStyle) {
                    this.keypad = null;
                }

                String v = kp.value;
                kp.value = "";
                kp.onOk.accept(v);
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
        final boolean energyStyle;
        String value = "";

        Keypad(String title, boolean masked, int maxLen, boolean energyStyle, Consumer<String> onOk, Runnable onCancel) {
            this.title = title;
            this.masked = masked;
            this.maxLen = maxLen;
            this.energyStyle = energyStyle;
            this.onOk = onOk;
            this.onCancel = onCancel;
        }
    }

    private static enum Page {
        MAIN,
        SECURITY,
        CONTROL,
        CALL,
        INQUIRY,
        ENERGY,
        SETTINGS,
        TOUCH;
    }

    private record Popup(List<String> lines, String ok, String cancel, Runnable onOk, Runnable onCancel) {
    }
}
