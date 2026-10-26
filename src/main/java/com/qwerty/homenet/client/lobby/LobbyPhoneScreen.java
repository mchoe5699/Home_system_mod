package com.qwerty.homenet.client.lobby;

import com.mojang.blaze3d.vertex.PoseStack;
import com.qwerty.homenet.HomeNet;
import com.qwerty.homenet.blockentity.LobbyPhoneBlockEntity;
import com.qwerty.homenet.intercom.Intercom;
import com.qwerty.homenet.intercom.IntercomLine;
import com.qwerty.homenet.item.RfCardItem;
import com.qwerty.homenet.lobby.LobbySettings;
import com.qwerty.homenet.network.LobbyKeyPacket;
import com.qwerty.homenet.network.ModNetwork;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;

public class LobbyPhoneScreen extends Screen {
    private static final ResourceLocation TEX = HomeNet.id("textures/gui/lobby_phone.png");
    private static final int DW = 248;
    private static final int DH = 279;
    private static final int TEX_SCALE = 4;
    private static final int LCD_X1 = 55;
    private static final int LCD_Y1 = 45;
    private static final int LCD_X2 = 142;
    private static final int LCD_Y2 = 199;
    private static final float INFO_X1 = 61.0F;
    private static final float INFO_Y1 = 60.0F;
    private static final float INFO_X2 = 135.0F;
    private static final float INFO_Y2 = 102.0F;
    private static final float PANEL_X1 = 57.0F;
    private static final float PANEL_Y1 = 57.0F;
    private static final float PANEL_X2 = 140.0F;
    private static final float PANEL_Y2 = 117.0F;
    private static final int KEY_X0 = 57;
    private static final int KEY_W = 27;
    private static final int KEY_STEP = 28;
    private static final int KEY_H = 16;
    private static final int[] KEY_ROWS = new int[]{120, 141, 162, 183};
    private static final float[] RIGHT_KEYS_Y = new float[]{133.0F, 154.0F, 174.5F, 195.0F};
    private static final int[] RIGHT_KEYS = new int[]{12, 13, 14, 15};
    private static final float NET_X = 134.8F;
    private static final float ICON_Y = 51.8F;
    private static final float DOOR_X = 126.4F;
    private static final float[] DIGIT_SCALE = new float[]{1.0F, 1.3F, 1.6F};
    private static final float CARD_X1 = 64.0F;
    private static final float CARD_Y1 = 207.0F;
    private static final float CARD_X2 = 140.0F;
    private static final float CARD_Y2 = 236.0F;
    private static final int PANEL_BG = -788998;
    private static final int PANEL_LINE = -6380360;
    private static final int PANEL_TEXT = -12959656;
    private static final int INFO_TEXT = -14274452;
    private final BlockPos pos;
    private float s = 1.0F;
    private int left;
    private int top;
    private int pressedSlot = -1;
    private long pressedAt;
    private boolean wasTalking;
    private EditBox msgBox;
    private String msgDraft = "";

    public LobbyPhoneScreen(BlockPos pos) {
        super(Component.translatable("block.qwertys_homenet.lobby_phone"));
        this.pos = pos;
    }

    private LobbyPhoneBlockEntity be() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        } else {
            return level.getBlockEntity(this.pos) instanceof LobbyPhoneBlockEntity b ? b : null;
        }
    }

    private boolean talking(LobbyPhoneBlockEntity be) {
        return be != null && be.getScreen() == LobbyPhoneBlockEntity.Screen.TALKING && be.isConnected();
    }

    @Override
    protected void init() {
        this.s = Math.max(0.5F, Math.min((this.height - 12) / 279.0F, 1.6F));
        int devW = Math.round(248.0F * this.s);
        int devH = Math.round(279.0F * this.s);
        LobbyPhoneBlockEntity be = this.be();
        boolean talk = this.talking(be);
        this.wasTalking = talk;
        int chatW = talk ? 160 : 0;
        this.left = (this.width - devW - chatW) / 2;
        this.top = (this.height - devH) / 2;
        this.msgBox = null;
        if (talk) {
            int cx = this.left + devW + 10;
            int cy = this.top + devH - 60;
            this.msgBox = new EditBox(this.font, cx, cy, 148, 18, Component.translatable("gui.qwertys_homenet.message"));
            this.msgBox.setMaxLength(80);
            this.msgBox.setValue(this.msgDraft);
            this.msgBox.setResponder(t -> this.msgDraft = t);
            this.msgBox.setHint(Component.translatable("gui.qwertys_homenet.message_hint"));
            this.addRenderableWidget(this.msgBox);
            this.addRenderableWidget(
                Button.builder(Component.translatable("gui.qwertys_homenet.send"), b -> this.sendMessage()).bounds(cx, cy + 22, 148, 20).build()
            );
            this.setFocused(this.msgBox);
        }
    }

    private void sendMessage() {
        String text = Intercom.sanitize(this.msgDraft, 80);
        if (!text.isEmpty()) {
            ModNetwork.sendToServer(new LobbyKeyPacket(this.pos, 16, text));
            this.msgDraft = "";
            if (this.msgBox != null) {
                this.msgBox.setValue("");
            }
        }
    }

    private void press(int key, int slot) {
        this.pressedSlot = slot;
        this.pressedAt = System.currentTimeMillis();
        ModNetwork.sendToServer(new LobbyKeyPacket(this.pos, key, ""));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) {
            return true;
        } else if (button != 0) {
            return false;
        } else {
            LobbyPhoneBlockEntity be = this.be();
            if (be == null) {
                return false;
            } else {
                float u = (float)((mx - this.left) / this.s);
                float v = (float)((my - this.top) / this.s);

                for (int slot = 0; slot < 12; slot++) {
                    int kx = 57 + slot % 3 * 28;
                    int ky = KEY_ROWS[slot / 3];
                    if (u >= kx && u < kx + 27 && v >= ky && v < ky + 16) {
                        int key;
                        if (slot < 9) {
                            key = be.getLayout()[slot];
                        } else if (slot == 10) {
                            key = be.getLayout()[9];
                        } else {
                            key = slot == 9 ? 10 : 11;
                        }

                        this.press(key, slot);
                        return true;
                    }
                }

                for (int i = 0; i < 4; i++) {
                    float ky = RIGHT_KEYS_Y[i];
                    if (u >= 150.0F && u <= 200.0F && v >= ky - 9.0F && v <= ky + 9.0F) {
                        this.press(RIGHT_KEYS[i], 12 + i);
                        return true;
                    }
                }

                if (u >= 64.0F && u <= 140.0F && v >= 207.0F && v <= 236.0F) {
                    LocalPlayer player = Minecraft.getInstance().player;
                    if (player != null && player.getMainHandItem().getItem() instanceof RfCardItem) {
                        this.press(17, 16);
                    } else if (player != null) {
                        player.displayClientMessage(Component.translatable("msg.qwertys_homenet.hold_card"), true);
                    }

                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (this.msgBox == null || !this.msgBox.isFocused()) {
            int digit = -1;
            if (key >= 48 && key <= 57) {
                digit = key - 48;
            }

            if (key >= 320 && key <= 329) {
                digit = key - 320;
            }

            if (digit < 0) {
                if (key == 259) {
                    this.press(15, 15);
                    return true;
                } else {
                    return super.keyPressed(key, scan, mods);
                }
            } else {
                LobbyPhoneBlockEntity be = this.be();
                int slot = -1;
                if (be != null) {
                    int[] layout = be.getLayout();

                    for (int i = 0; i < 10; i++) {
                        if (layout[i] == digit) {
                            slot = i < 9 ? i : 10;
                        }
                    }
                }

                this.press(digit, slot);
                return true;
            }
        } else if (key != 257 && key != 335) {
            return super.keyPressed(key, scan, mods);
        } else {
            this.sendMessage();
            return true;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.msgBox != null) {
            this.msgBox.tick();
        }

        LobbyPhoneBlockEntity be = this.be();
        LocalPlayer player = Minecraft.getInstance().player;
        if (be != null && player != null && !(player.distanceToSqr(Vec3.atCenterOf(this.pos)) > 64.0)) {
            if (this.talking(be) != this.wasTalking) {
                this.rebuildWidgets();
            }
        } else {
            this.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        LobbyPhoneBlockEntity be = this.be();
        if (be != null) {
            long gameTime = Minecraft.getInstance().level.getGameTime();
            PoseStack pose = g.pose();
            pose.pushPose();
            pose.translate((float)this.left, (float)this.top, 0.0F);
            pose.scale(this.s, this.s, 1.0F);
            g.blit(TEX, 0, 0, 248, 279, 0.0F, 0.0F, 992, 1116, 992, 1116);
            this.drawLcd(g, be, gameTime);
            this.drawRightKeys(g, be, gameTime, mouseX, mouseY);
            if (this.isPressed(16)) {
                this.fillF(g, 64.0F, 207.0F, 140.0F, 236.0F, 1084274943);
            }

            pose.popPose();
            if (this.talking(be)) {
                this.drawChat(g, be);
            }

            super.render(g, mouseX, mouseY, partialTick);
        }
    }

    private boolean isPressed(int slot) {
        return this.pressedSlot == slot && System.currentTimeMillis() - this.pressedAt < 160L;
    }

    private void drawLcd(GuiGraphics g, LobbyPhoneBlockEntity be, long gameTime) {
        LobbyPhoneBlockEntity.Screen sc = be.getScreen();
        switch (sc) {
            case HELP:
                this.drawHelp(g);
                break;
            case ADMIN_PASSWORD:
                this.drawAdminPassword(g, be);
                break;
            case ADMIN_MENU:
                this.drawAdminMenu(g, be);
                break;
            case ADMIN_EDIT:
                this.drawAdminEdit(g, be, gameTime);
                break;
            case CARD_MENU:
                this.drawMenu(g, "card_menu_title", 3, "card_menu_");
                break;
            case CARD_UNIT_MENU:
                this.drawMenu(g, "card_unit_menu_title", 4, "card_unit_menu_");
                break;
            case PROX_MENU:
                this.drawMenu(g, "prox_menu_title", 2, "prox_menu_");
                break;
            case CARD_MASTER:
            case CARD_REG:
            case CARD_DELETE:
            case CARD_DELETE_ALL:
            case CARD_UNIT_REG:
            case CARD_UNIT_DELETE:
                this.drawCardScreen(g, be, gameTime);
                break;
            default:
                this.drawInfo(g, LobbyLcd.of(be, gameTime), sc == LobbyPhoneBlockEntity.Screen.MESSAGE);
        }

        if (be.isDoorOpen(gameTime)) {
            float is = 0.94F;
            this.text(g, LobbyLcd.icon("\ue001"), 126.4F - 3.75F * is, 51.8F - 4.0F * is, is, -1, false);
        }

        this.drawKeypad(g, be);
        if (!be.isBacklight()) {
            this.fillF(g, 55.0F, 45.0F, 142.0F, 199.0F, -939524096);
        }
    }

    private void drawInfo(GuiGraphics g, LobbyLcd lcd, boolean wrap) {
        this.infoBox(g);
        if (!lcd.topLeft().getString().isEmpty()) {
            this.text(g, lcd.topLeft(), 65.0F, 63.0F, 0.62F, -14274452, false);
        }

        if (!lcd.topRight().getString().isEmpty()) {
            this.textRight(g, lcd.topRight(), 131.0F, 63.5F, 0.52F, -14274452);
        }

        float cx = 98.0F;
        if (wrap) {
            float sc = 0.85F;
            int maxW = Math.round(68.0F / sc);
            List<FormattedCharSequence> lines = this.font.split(lcd.big(), maxW);
            float y0 = 81.0F - lines.size() * 10 * sc / 2.0F;

            for (int i = 0; i < lines.size(); i++) {
                PoseStack pose = g.pose();
                pose.pushPose();
                pose.translate(cx, y0 + i * 10 * sc, 0.0F);
                pose.scale(sc, sc, 1.0F);
                g.drawString(this.font, lines.get(i), -this.font.width(lines.get(i)) / 2, 0, -14274452, false);
                pose.popPose();
            }
        } else {
            this.drawBig(g, lcd, cx);
        }

        if (!lcd.subLeft().getString().isEmpty()) {
            this.text(g, lcd.subLeft(), 61.0F, 108.0F, 0.62F, -1, false);
        }

        if (!lcd.subRight().getString().isEmpty()) {
            this.textRight(g, lcd.subRight(), 135.0F, 108.0F, 0.62F, -1);
        }
    }

    private void drawBig(GuiGraphics g, LobbyLcd lcd, float cx) {
        float bs = lcd.bigScale();
        boolean bitmap = LobbyLcd.isBitmapBig(lcd.big());
        float textW = Math.max(1, this.font.width(lcd.big()) - (bitmap ? 1 : 0));
        float maxW = 66.0F;
        float topPad = lcd.topLeft().getString().isEmpty() ? 4.0F : 10.0F;
        float maxH = 42.0F - topPad - 3.0F;
        bs = Math.min(bs, maxW / textW);
        bs = Math.min(bs, maxH / 8.0F);
        float areaMid = 60.0F + topPad + (42.0F - topPad - 3.0F) / 2.0F;
        float y = areaMid - (bitmap ? 4.0F : 4.6F) * bs;
        this.text(g, lcd.big(), cx, y, bs, -14274452, true);
    }

    private void infoBox(GuiGraphics g) {
        this.fillF(g, 62.0F, 60.0F, 134.0F, 102.0F, -11642736);
        this.fillF(g, 61.0F, 61.0F, 135.0F, 101.0F, -11642736);
        this.gradF(g, 62.0F, 60.5F, 134.0F, 101.5F, -986374, -3222036);
        this.gradF(g, 61.5F, 61.0F, 134.5F, 101.0F, -986374, -3222036);
        this.fillF(g, 62.2F, 60.9F, 133.8F, 61.2F, -1);
    }

    private void panel(GuiGraphics g) {
        this.fillF(g, 57.0F, 57.0F, 140.0F, 117.0F, -788998);
        this.outline(g, 57.0F, 57.0F, 140.0F, 117.0F, -6380360);
    }

    private float adminHeader(GuiGraphics g, boolean secondLine) {
        this.text(g, LobbyLcd.lcd("App Ver : 070.501.73"), 58.5F, 58.5F, 0.36F, -12959656, false);
        this.textRight(g, LobbyLcd.lcd("FW Ver : 02.09"), 138.5F, 58.5F, 0.36F, -12959656);
        if (!secondLine) {
            return 63.0F;
        } else {
            this.textFit(g, LobbyLcd.tr("admin_hint"), 98.5F, 63.0F, 0.36F, 80.0F, -12959656, true);
            return 68.0F;
        }
    }

    private float tableBox(GuiGraphics g, float y1, float y2, int rows) {
        float bx1 = 58.5F;
        float bx2 = 138.5F;
        this.outline(g, bx1, y1, bx2, y2, -6380360);
        float rh = (y2 - y1) / rows;

        for (int i = 1; i < rows; i++) {
            float y = y1 + i * rh;
            this.fillF(g, bx1, y, bx2, y + 0.3F, -6380360);
        }

        return rh;
    }

    private void drawAdminPassword(GuiGraphics g, LobbyPhoneBlockEntity be) {
        this.panel(g);
        float y = this.adminHeader(g, false) + 2.0F;
        float y2 = 114.0F;
        float bx1 = 58.5F;
        float bx2 = 138.5F;
        float cx = 98.5F;
        this.outline(g, bx1, y, bx2, y2, -6380360);
        this.fillF(g, bx1, y + 7.0F, bx2, y + 7.3F, -6380360);
        this.fillF(g, bx1, y2 - 7.0F, bx2, y2 - 6.7F, -6380360);
        this.text(g, LobbyLcd.tr("admin_pw_title"), cx, y + 2.0F, 0.42F, -12959656, true);
        StringBuilder stars = new StringBuilder();

        for (int i = 0; i < be.getSecretLength(); i++) {
            stars.append(i == 0 ? "*" : "  *");
        }

        this.text(g, LobbyLcd.lcd(stars.toString()), cx + 6.0F, (y + y2) / 2.0F - 3.0F, 1.1F, -12959656, true);
        this.text(g, LobbyLcd.tr("enter_password_admin"), cx, y2 - 5.0F, 0.42F, -12959656, true);
    }

    private void drawAdminMenu(GuiGraphics g, LobbyPhoneBlockEntity be) {
        this.panel(g);
        float y = this.adminHeader(g, true) + 1.0F;
        int page = be.getAdminPage();
        float rh = this.tableBox(g, y, 115.0F, 7);
        float div = 102.5F;
        this.fillF(g, div, y, div + 0.3F, 115.0F, -6380360);

        for (int i = 0; i < 7; i++) {
            LobbySettings.Item item = LobbySettings.item(page, i);
            if (item != null) {
                float ry = y + i * rh + (rh - 3.6F) / 2.0F;
                this.textFit(
                    g,
                    Component.literal(i + 1 + " ").append(LobbyLcd.tr("setting." + item.key())).withStyle(st -> st.withFont(LobbyLcd.FONT)),
                    59.5F,
                    ry,
                    0.38F,
                    div - 57.0F - 3.5F,
                    -12959656,
                    false
                );
                Component val = LobbyLcd.lcd(be.displaySetting(page, i));
                float vw = this.font.width(val) * 0.38F;
                float maxVw = 140.0F - div - 4.0F;
                float vs = vw > maxVw ? 0.38F * maxVw / vw : 0.38F;
                this.textRight(g, val, 137.5F, ry, vs, -12959656);
            }
        }
    }

    private void drawAdminEdit(GuiGraphics g, LobbyPhoneBlockEntity be, long gameTime) {
        this.panel(g);
        LobbySettings.Item item = LobbySettings.item(be.getAdminPage(), be.getEditItem());
        float y = this.adminHeader(g, true) + 1.0F;
        if (item != null) {
            float y2 = 115.0F;
            float bx1 = 58.5F;
            float bx2 = 138.5F;
            float cx = 98.5F;
            this.outline(g, bx1, y, bx2, y2, -6380360);
            this.fillF(g, bx1, y + 6.0F, bx2, y + 6.3F, -6380360);
            this.fillF(g, bx1, y2 - 6.0F, bx2, y2 - 5.7F, -6380360);
            this.text(g, LobbyLcd.tr("setting." + item.key()), cx, y + 1.6F, 0.42F, -12959656, true);
            boolean blink = gameTime / 10L % 2L == 0L;
            if (item.kind() == LobbySettings.Kind.LOBBY_NO) {
                this.text(g, LobbyLcd.tr("lobby_no_desc1"), cx, y + 8.0F, 0.38F, -12959656, true);
                String typing = be.getEditValue();
                Component cur = LobbyLcd.lcd(LobbySettings.lobbyNoDisplay(be.getEditDong() + "|" + be.getEditLine()));
                this.text(g, cur, cx, y + 14.0F, 0.8F, -12959656, true);
                this.text(g, LobbyLcd.lcd(typing), cx, y + 23.0F, 1.0F, -12959656, true);
                if (blink) {
                    float tw = this.font.width(LobbyLcd.lcd(typing)) * 1.0F;
                    this.fillF(g, cx + tw / 2.0F + 0.5F, y + 23.0F, cx + tw / 2.0F + 4.0F, y + 30.0F, -12959656);
                }

                this.text(g, LobbyLcd.tr("lobby_no_desc2"), cx, y2 - 15.0F, 0.36F, -12959656, true);
                this.text(g, LobbyLcd.tr("lobby_no_desc3"), cx, y2 - 10.5F, 0.36F, -12959656, true);
            } else {
                String v = be.getEditValue();
                float vs = 1.3F;
                Component val = LobbyLcd.lcd(v);
                float tw = this.font.width(val) * vs;
                float vy = y + 10.0F;
                this.text(g, val, cx, vy, vs, -12959656, true);
                if (blink) {
                    this.fillF(g, cx + tw / 2.0F + 0.6F, vy + 0.5F, cx + tw / 2.0F + 5.0F, vy + 11.0F, -12959656);
                }

                this.textFit(g, LobbyLcd.tr("setting_desc1." + item.key()), cx, y2 - 16.0F, 0.38F, bx2 - bx1 - 2.0F, -12959656, true);
                this.textFit(g, LobbyLcd.tr("setting_desc2." + item.key()), cx, y2 - 11.0F, 0.38F, bx2 - bx1 - 2.0F, -12959656, true);
            }

            this.textFit(g, LobbyLcd.tr("edit_hint"), cx, y2 - 4.4F, 0.38F, bx2 - bx1 - 2.0F, -12959656, true);
        }
    }

    private void drawMenu(GuiGraphics g, String titleKey, int items, String itemPrefix) {
        this.panel(g);
        float y = this.adminHeader(g, true) + 1.0F;
        float rh = this.tableBox(g, y, 115.0F, 8);
        this.text(g, LobbyLcd.tr(titleKey), 98.5F, y + (rh - 3.6F) / 2.0F, 0.4F, -12959656, true);

        for (int i = 0; i < items; i++) {
            this.textFit(g, LobbyLcd.tr(itemPrefix + (i + 1)), 60.0F, y + (i + 1) * rh + (rh - 3.6F) / 2.0F, 0.4F, 77.0F, -12959656, false);
        }

        this.text(g, LobbyLcd.tr("menu_footer"), 98.5F, y + 7.0F * rh + (rh - 3.6F) / 2.0F, 0.38F, -12959656, true);
    }

    private void drawCardScreen(GuiGraphics g, LobbyPhoneBlockEntity be, long gameTime) {
        this.panel(g);
        float y = this.adminHeader(g, true) + 1.0F;
        float rh = this.tableBox(g, y, 115.0F, 8);
        LobbyPhoneBlockEntity.Screen sc = be.getScreen();
        Component[] comps = new Component[6];
        String notice = be.activeNotice(gameTime);
        String title;
        switch (sc) {
            case CARD_MASTER:
            case CARD_REG:
                title = sc == LobbyPhoneBlockEntity.Screen.CARD_MASTER ? "card_master_title" : "card_reg_title";
                comps[0] = LobbyLcd.tr("card_touch");
                comps[1] = LobbyLcd.tr("card_press0");
                if (!be.getPendingCard().isEmpty()) {
                    comps[2] = LobbyLcd.tr("card_number", be.getPendingCard());
                }
                break;
            case CARD_DELETE:
                title = "card_delete_title";
                comps[0] = LobbyLcd.tr("card_touch_please");
                comps[1] = LobbyLcd.tr("card_delete_desc");
                break;
            case CARD_DELETE_ALL:
            default:
                title = "card_delete_all_title";
                comps[0] = LobbyLcd.tr("card_delete_all_desc", be.getCardCount());
                break;
            case CARD_UNIT_REG:
            case CARD_UNIT_DELETE:
                title = sc == LobbyPhoneBlockEntity.Screen.CARD_UNIT_REG ? "card_unit_reg_title" : "card_unit_delete_title";
                comps[0] = LobbyLcd.tr("card_dong_info", be.getCardDong());
                comps[1] = LobbyLcd.tr("card_ho_info", be.getCardHo());
                if (!be.isCardHoSet()) {
                    comps[2] = LobbyLcd.tr("card_check_unit");
                } else {
                    comps[2] = LobbyLcd.tr(sc == LobbyPhoneBlockEntity.Screen.CARD_UNIT_REG ? "card_touch_to_register" : "card_hash_to_delete");
                }

                if (!be.getEditValue().isEmpty()) {
                    comps[3] = LobbyLcd.tr("card_typing", be.getEditValue());
                }
        }

        if (notice != null) {
            comps[4] = LobbyLcd.msg("card_" + notice, be.getNoticeArg());
        }

        this.text(g, LobbyLcd.tr(title), 98.5F, y + (rh - 3.6F) / 2.0F, 0.4F, -12959656, true);

        for (int i = 0; i < 6; i++) {
            if (comps[i] != null) {
                this.textFit(g, comps[i], 60.0F, y + (i + 1) * rh + (rh - 3.6F) / 2.0F, 0.4F, 77.0F, i == 4 ? -14001976 : -12959656, false);
            }
        }

        this.text(g, LobbyLcd.tr("menu_footer"), 98.5F, y + 7.0F * rh + (rh - 3.6F) / 2.0F, 0.38F, -12959656, true);
    }

    private void drawHelp(GuiGraphics g) {
        this.panel(g);
        this.fillF(g, 58.0F, 58.0F, 139.0F, 66.0F, -12956516);
        this.text(g, LobbyLcd.tr("help_title"), 61.0F, 59.6F, 0.55F, -1, false);
        String[] rows = new String[]{"help_unit", "help_guard", "help_cancel", "help_password"};
        float y = 69.0F;

        for (String r : rows) {
            this.fillF(g, 59.0F, y, 84.0F, y + 7.0F, -10983244);
            this.textFit(g, LobbyLcd.tr(r), 71.5F, y + 1.8F, 0.4F, 23.0F, -1, true);
            this.textFit(g, LobbyLcd.tr(r + "_how"), 86.0F, y + 1.8F, 0.4F, 52.0F, -12959656, false);
            y += 11.0F;
        }
    }

    private Component[] bottomLabels(LobbyPhoneBlockEntity be) {
        Component cancel = LobbyLcd.tr("key.cancel");
        Component hash = LobbyLcd.keyDigit("#");
        Component star = LobbyLcd.keyDigit("*");
        Component la = LobbyLcd.lcd("←");
        Component ra = LobbyLcd.lcd("→");

        return switch (be.getScreen()) {
            case ADMIN_PASSWORD, PASSWORD, COMMON_PASSWORD -> new Component[]{cancel, LobbyLcd.tr("key.ok")};
            case ADMIN_MENU, CARD_MENU, CARD_UNIT_MENU, PROX_MENU, CARD_MASTER, CARD_REG, CARD_DELETE, CARD_DELETE_ALL -> new Component[]{la, ra};
            case ADMIN_EDIT -> {
                LobbySettings.Item it = LobbySettings.item(be.getAdminPage(), be.getEditItem());
                yield it != null && it.kind() == LobbySettings.Kind.LOBBY_NO
                    ? new Component[]{LobbyLcd.tr("key.dong"), LobbyLcd.tr("key.line")}
                    : new Component[]{la, ra};
            }
            case CARD_UNIT_REG, CARD_UNIT_DELETE -> new Component[]{star, hash};
            case IDLE -> new Component[]{LobbyLcd.tr("key.help"), (Component)(be.isCommonPasswordUse() ? LobbyLcd.tr("key.common_pw") : hash)};
            case INPUT -> be.isDongStage()
                ? new Component[]{LobbyLcd.tr("key.dong"), LobbyLcd.tr("key.dong")}
                : new Component[]{cancel, LobbyLcd.tr("key.password")};
            default -> new Component[]{cancel, hash};
        };
    }

    private void drawKeypad(GuiGraphics g, LobbyPhoneBlockEntity be) {
        int[] layout = be.getLayout();
        Component[] bottom = this.bottomLabels(be);
        float ds = DIGIT_SCALE[Math.max(0, Math.min(2, be.getDigitSize()))];

        for (int slot = 0; slot < 12; slot++) {
            int kx = 57 + slot % 3 * 28;
            int ky = KEY_ROWS[slot / 3];
            if (this.isPressed(slot)) {
                this.fillF(g, kx, ky, kx + 27, ky + 16, 1623249151);
            }

            float cx = kx + 13.5F;
            float cy = ky + 8.0F;
            g.enableScissor(this.sx(kx), this.sy(ky), this.sx(kx + 27), this.sy(ky + 16));
            if (slot >= 9 && slot != 10) {
                Component label = bottom[slot == 9 ? 0 : 1];
                String raw = label.getString();
                if (!raw.equals("*") && !raw.equals("#")) {
                    String[] parts = raw.split("\n");
                    float ls = parts.length > 1 ? 0.5F : (raw.length() <= 1 ? 1.2F : 0.7F);

                    for (String p : parts) {
                        float pw = this.font.width(LobbyLcd.lcd(p)) * ls;
                        if (pw > 24.0F) {
                            ls *= 24.0F / pw;
                        }
                    }

                    float lineH = 9.5F * ls;
                    float y = cy - parts.length * lineH / 2.0F;

                    for (String px : parts) {
                        this.text(g, LobbyLcd.lcd(px), cx, y, ls, -1, true);
                        y += lineH;
                    }
                } else {
                    this.text(g, label, cx, cy - 5.2F, 1.3F, -1, true);
                }
            } else {
                int d = slot < 9 ? layout[slot] : layout[9];
                Component c = LobbyLcd.keyDigit(String.valueOf(d));
                float w = Math.max(1, this.font.width(c) - 1);
                float scale = Math.min(ds, Math.min(23.0F / w, 1.625F));
                this.text(g, c, cx, cy - 4.0F * scale, scale, -1, true);
            }

            g.disableScissor();
        }
    }

    private void drawRightKeys(GuiGraphics g, LobbyPhoneBlockEntity be, long gameTime, int mouseX, int mouseY) {
        float u = (mouseX - this.left) / this.s;
        float v = (mouseY - this.top) / this.s;

        for (int i = 0; i < 4; i++) {
            float ky = RIGHT_KEYS_Y[i];
            if (this.isPressed(12 + i)) {
                this.fillF(g, 151.0F, ky - 8.0F, 199.0F, ky + 7.0F, 1621145855);
            } else if (u >= 150.0F && u <= 200.0F && v >= ky - 9.0F && v <= ky + 9.0F) {
                this.fillF(g, 151.0F, ky - 8.0F, 199.0F, ky + 7.0F, 620756991);
            }
        }

        if (!be.isKeyLedOn(gameTime)) {
            this.fillF(g, 151.0F, 122.0F, 199.0F, 205.0F, -1342177280);
        }
    }

    private void drawChat(GuiGraphics g, LobbyPhoneBlockEntity be) {
        int devW = Math.round(248.0F * this.s);
        int devH = Math.round(279.0F * this.s);
        int x = this.left + devW + 10;
        int y = this.top + 20;
        int w = 148;
        int bottom = this.top + devH - 66;
        g.fill(x - 4, y - 4, x + w + 4, bottom, -1072687056);
        g.drawString(this.font, Component.translatable("lobby.qwertys_homenet." + (be.isGuardCall() ? "talking_guard" : "talking_unit")), x, y, -9510790, false);
        List<IntercomLine> log = be.getLog();
        int lineY = y + 14;
        int maxLines = Math.max(1, (bottom - lineY - 4) / 11);
        int start = Math.max(0, log.size() - maxLines);

        for (int i = start; i < log.size(); i++) {
            IntercomLine l = log.get(i);
            Component c = Component.literal("[")
                .append(Intercom.sideName(l.side()))
                .append("] ")
                .withStyle(ChatFormatting.AQUA)
                .append(Component.literal(l.name() + ": ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(l.text()).withStyle(ChatFormatting.WHITE));
            g.drawString(this.font, Language.getInstance().getVisualOrder(this.font.substrByWidth(c, w)), x, lineY, -1, false);
            lineY += 11;
        }

        if (log.isEmpty()) {
            g.drawString(this.font, Component.translatable("gui.qwertys_homenet.log_empty"), x, lineY, -6641994, false);
        }
    }

    private int sx(float u) {
        return Math.round(this.left + u * this.s);
    }

    private int sy(float v) {
        return Math.round(this.top + v * this.s);
    }

    private void fillF(GuiGraphics g, float x1, float y1, float x2, float y2, int color) {
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.scale(0.1F, 0.1F, 1.0F);
        g.fill(Math.round(x1 * 10.0F), Math.round(y1 * 10.0F), Math.round(x2 * 10.0F), Math.round(y2 * 10.0F), color);
        pose.popPose();
    }

    private void gradF(GuiGraphics g, float x1, float y1, float x2, float y2, int c1, int c2) {
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.scale(0.1F, 0.1F, 1.0F);
        g.fillGradient(Math.round(x1 * 10.0F), Math.round(y1 * 10.0F), Math.round(x2 * 10.0F), Math.round(y2 * 10.0F), c1, c2);
        pose.popPose();
    }

    private void outline(GuiGraphics g, float x1, float y1, float x2, float y2, int color) {
        float t = 0.3F;
        this.fillF(g, x1, y1, x2, y1 + t, color);
        this.fillF(g, x1, y2 - t, x2, y2, color);
        this.fillF(g, x1, y1, x1 + t, y2, color);
        this.fillF(g, x2 - t, y1, x2, y2, color);
    }

    private void textFit(GuiGraphics g, Component c, float x, float y, float scale, float maxW, int color, boolean center) {
        float w = this.font.width(c) * scale;
        if (w > maxW && w > 0.0F) {
            scale *= maxW / w;
        }

        this.text(g, c, x, y, scale, color, center);
    }

    private void text(GuiGraphics g, Component c, float x, float y, float scale, int color, boolean center) {
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0.0F);
        pose.scale(scale, scale, 1.0F);
        int w = this.font.width(c);
        g.drawString(this.font, c, center ? -w / 2 : 0, 0, color, false);
        pose.popPose();
    }

    private void textRight(GuiGraphics g, Component c, float xRight, float y, float scale, int color) {
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(xRight, y, 0.0F);
        pose.scale(scale, scale, 1.0F);
        g.drawString(this.font, c, -this.font.width(c), 0, color, false);
        pose.popPose();
    }
}
