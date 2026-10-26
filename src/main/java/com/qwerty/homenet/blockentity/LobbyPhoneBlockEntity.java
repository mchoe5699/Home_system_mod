package com.qwerty.homenet.blockentity;

import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.intercom.DoorStatus;
import com.qwerty.homenet.intercom.IntercomLine;
import com.qwerty.homenet.intercom.UnitAddress;
import com.qwerty.homenet.item.RfCardItem;
import com.qwerty.homenet.lobby.LobbySettings;
import com.qwerty.homenet.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class LobbyPhoneBlockEntity extends CallerBlockEntity {
    public static final int KEY_LEFT = 10;
    public static final int KEY_RIGHT = 11;
    public static final int KEY_SECURITY = 12;
    public static final int KEY_CALL = 13;
    public static final int KEY_GUARD = 14;
    public static final int KEY_CANCEL = 15;
    public static final int KEY_MESSAGE = 16;
    public static final int KEY_CARD = 17;
    private static final int INPUT_TIMEOUT = 600;
    private static final int ADMIN_TIMEOUT = 2400;
    private static final int MESSAGE_TICKS = 50;
    private static final int NOTICE_TICKS = 50;
    private static final int ADMIN_ARM_TICKS = 100;
    private static final int[] DEFAULT_LAYOUT = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 0};
    private final Map<String, String> cfg = LobbySettings.defaults();
    private final Map<String, LobbyPhoneBlockEntity.Card> cards = new LinkedHashMap<>();
    private LobbyPhoneBlockEntity.Screen screen = LobbyPhoneBlockEntity.Screen.IDLE;
    private String input = "";
    private String inputDong = "";
    private boolean dongStage;
    private String unitInput = "";
    private String secret = "";
    private String bigLabel = "";
    private boolean guardCall;
    private int[] layout = (int[])DEFAULT_LAYOUT.clone();
    private int digitSize = 2;
    private int adminPage;
    private int editItem = -1;
    private String editValue = "";
    private String editDong = "";
    private String editLine = "";
    private String msgKey = "";
    private String msgArg = "";
    private LobbyPhoneBlockEntity.Screen msgReturn = LobbyPhoneBlockEntity.Screen.IDLE;
    private long stateSince;
    private long lastInput;
    private boolean backlight = true;
    private long doorOpenUntil;
    private long keyLedUntil;
    private boolean adminArmed;
    private long adminArmedAt;
    private int failCount;
    private String cardDong = "0000";
    private String cardHo = "0000";
    private boolean cardHoSet;
    private String pendingCard = "";
    private String noticeKey = "";
    private String noticeArg = "";
    private long noticeUntil;
    private int secretLen;
    private boolean clientConnected;
    private int cardCount;
    private final Random random = new Random();

    public LobbyPhoneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LOBBY_PHONE.get(), pos, state);
    }

    @Override
    public DeviceRegistry.Kind kind() {
        return DeviceRegistry.Kind.LOBBY_PHONE;
    }

    @Override
    protected int openTicks() {
        return Math.max(1, this.cfgInt("open_time")) * 20;
    }

    public String cfg(String key) {
        String v = this.cfg.get(key);
        return v == null ? "" : v;
    }

    private int cfgInt(String key) {
        try {
            return Integer.parseInt(this.cfg(key));
        } catch (NumberFormatException var3) {
            return 0;
        }
    }

    public String lobbyDong() {
        String d = this.cfg("lobby_no").split("\\|", -1)[0].replaceFirst("^0+(?=.)", "");
        return "0".equals(d) ? "" : d;
    }

    public boolean matchesNumber(String number) {
        String n = strip(number);
        if (n.isEmpty()) {
            return false;
        } else {
            String[] p = this.cfg("lobby_no").split("\\|", -1);
            String dong = strip(p.length > 0 ? p[0] : "");
            String line = strip(p.length > 1 ? p[1] : "");
            return n.equals(dong) || n.equals(line) || n.equals(dong + line) || n.equals(dong + (line.length() < 2 ? "0" + line : line));
        }
    }

    private static String strip(String s) {
        return s.replaceAll("[^0-9]", "").replaceFirst("^0+(?=.)", "");
    }

    public boolean acceptGuardCall(BlockPos guard) {
        if (this.isInCall()) {
            return false;
        } else {
            this.wake();
            this.guardCall = true;
            this.bigLabel = "경비";
            return this.acceptIncoming(guard);
        }
    }

    public boolean isParkingLobby() {
        return this.cfgInt("parking_lobby") == 1;
    }

    public boolean isCommonPasswordUse() {
        return this.cfgInt("common_password_use") == 1;
    }

    public void press(ServerPlayer player, int key, String text) {
        if (this.level instanceof ServerLevel) {
            long t = this.now();
            this.lastInput = t;
            this.backlight = true;
            this.keyLedUntil = t + this.cfgInt("key_led_time") * 20L;
            if (key == 16) {
                if (this.screen == LobbyPhoneBlockEntity.Screen.TALKING && this.isConnected()) {
                    this.say(player, "lobby", text);
                }
            } else if (key == 17) {
                String id = RfCardItem.cardId(player.getMainHandItem());
                if (id != null) {
                    this.tapCard(id);
                }
            } else {
                this.keyTone();
                boolean armedNow = this.adminArmed && t - this.adminArmedAt <= 100L;
                this.adminArmed = false;
                switch (this.screen) {
                    case IDLE:
                        this.pressIdle(key, armedNow);
                        break;
                    case INPUT:
                        this.pressInput(key);
                        break;
                    case PASSWORD:
                    case COMMON_PASSWORD:
                        this.pressPassword(key);
                        break;
                    case CALLING:
                    case TALKING:
                        if (key == 10 || key == 15) {
                            this.hangUpFromLobby();
                        }
                        break;
                    case HELP:
                        if (key == 10 || key == 15) {
                            this.goIdle();
                        }
                        break;
                    case ADMIN_PASSWORD:
                        this.pressAdminPassword(key);
                        break;
                    case ADMIN_MENU:
                        this.pressAdminMenu(key);
                        break;
                    case ADMIN_EDIT:
                        this.pressAdminEdit(key);
                        break;
                    case MESSAGE:
                        if (key == 15 || key == 10) {
                            this.leaveMessage();
                        }
                        break;
                    case CARD_MENU:
                        this.pressCardMenu(key);
                        break;
                    case CARD_MASTER:
                    case CARD_REG:
                        this.pressCardRegisterSpecial(key);
                        break;
                    case CARD_UNIT_MENU:
                        this.pressCardUnitMenu(key);
                        break;
                    case CARD_UNIT_REG:
                    case CARD_UNIT_DELETE:
                        this.pressCardUnitInput(key);
                        break;
                    case CARD_DELETE:
                        this.pressCardBack(key, LobbyPhoneBlockEntity.Screen.CARD_UNIT_MENU);
                        break;
                    case CARD_DELETE_ALL:
                        if (key == 0) {
                            int n = this.cards.size();
                            this.cards.clear();
                            this.setChanged();
                            this.notice("all_deleted", String.valueOf(n));
                        } else {
                            this.pressCardBack(key, LobbyPhoneBlockEntity.Screen.CARD_UNIT_MENU);
                        }
                        break;
                    case PROX_MENU:
                        this.pressProxMenu(key);
                }

                this.sync();
            }
        }
    }

    private void pressIdle(int key, boolean armedNow) {
        if (isDigit(key)) {
            this.setScreen(LobbyPhoneBlockEntity.Screen.INPUT);
            this.dongStage = this.isParkingLobby();
            this.inputDong = "";
            this.input = String.valueOf(key);
            this.digitTone(key);
        } else {
            switch (key) {
                case 10:
                    this.setScreen(LobbyPhoneBlockEntity.Screen.HELP);
                    break;
                case 11:
                    if (this.isCommonPasswordUse() || !this.linkedGuardPassword().isEmpty()) {
                        this.secret = "";
                        this.failCount = 0;
                        this.setScreen(LobbyPhoneBlockEntity.Screen.COMMON_PASSWORD);
                    }
                    break;
                case 12:
                    this.toggleLayout();
                    break;
                case 13:
                    this.adminArmed = true;
                    this.adminArmedAt = this.now();
                    break;
                case 14:
                    if (armedNow) {
                        this.secret = "";
                        this.setScreen(LobbyPhoneBlockEntity.Screen.ADMIN_PASSWORD);
                    } else {
                        this.startGuardCall();
                    }
                    break;
                case 15:
                    this.digitSize = (this.digitSize + 1) % 3;
            }
        }
    }

    private void pressInput(int key) {
        if (isDigit(key)) {
            if (this.input.length() < 4) {
                this.input = this.input + key;
            }

            this.digitTone(key);
        } else {
            switch (key) {
                case 10:
                case 11:
                    if (this.dongStage) {
                        if (UnitAddress.component(this.input)) {
                            this.inputDong = this.input;
                            this.input = "";
                            this.dongStage = false;
                        }
                    } else if (key == 10) {
                        this.goIdle();
                    } else if (!this.input.isEmpty()) {
                        this.unitInput = this.composeUnit();
                        this.secret = "";
                        this.failCount = 0;
                        this.setScreen(LobbyPhoneBlockEntity.Screen.PASSWORD);
                    }
                    break;
                case 12:
                    this.toggleLayout();
                    break;
                case 13:
                    if (!this.dongStage && !this.input.isEmpty()) {
                        this.startUnitCall(this.composeUnit(), this.input);
                    }
                    break;
                case 14:
                    this.startGuardCall();
                    break;
                case 15:
                    this.goIdle();
            }
        }
    }

    private void pressPassword(int key) {
        if (isDigit(key)) {
            if (this.secret.length() < 4) {
                this.secret = this.secret + key;
            }

            this.digitTone(key);
        } else {
            switch (key) {
                case 10:
                case 15:
                    this.goIdle();
                    break;
                case 11:
                    this.checkPassword();
                    break;
                case 12:
                    this.toggleLayout();
                case 13:
                default:
                    break;
                case 14:
                    this.startGuardCall();
            }
        }
    }

    private void pressAdminPassword(int key) {
        if (isDigit(key)) {
            if (this.secret.length() < 4) {
                this.secret = this.secret + key;
            }

            if (this.secret.length() == 4) {
                this.checkAdminPassword();
            }
        } else {
            switch (key) {
                case 10:
                case 15:
                    this.goIdle();
                    break;
                case 11:
                    this.checkAdminPassword();
            }
        }
    }

    private void checkAdminPassword() {
        if (this.secret.length() == 4 && this.secret.equals(this.cfg("system_password"))) {
            this.adminPage = 0;
            this.setScreen(LobbyPhoneBlockEntity.Screen.ADMIN_MENU);
        } else {
            this.setScreen(LobbyPhoneBlockEntity.Screen.ADMIN_PASSWORD);
        }

        this.secret = "";
    }

    private void pressAdminMenu(int key) {
        if (key == 0) {
            this.setScreen(LobbyPhoneBlockEntity.Screen.CARD_MENU);
        } else if (key == 9) {
            this.setScreen(LobbyPhoneBlockEntity.Screen.PROX_MENU);
        } else if (isDigit(key)) {
            LobbySettings.Item item = LobbySettings.item(this.adminPage, key - 1);
            if (item != null) {
                if (item.key().equals("guard_password")) {
                    this.showMessage("guard_pw_readonly", "", LobbyPhoneBlockEntity.Screen.ADMIN_MENU);
                } else if (item.kind() == LobbySettings.Kind.READONLY) {
                    this.showMessage("unsupported", "", LobbyPhoneBlockEntity.Screen.ADMIN_MENU);
                } else {
                    this.editItem = key - 1;
                    this.editValue = "";
                    if (item.kind() == LobbySettings.Kind.LOBBY_NO) {
                        String[] p = this.cfg("lobby_no").split("\\|", -1);
                        this.editDong = p.length > 0 ? p[0] : "";
                        this.editLine = p.length > 1 ? p[1] : "";
                    } else if (item.kind() != LobbySettings.Kind.PASSWORD) {
                        this.editValue = this.cfg(item.key());
                    }

                    this.setScreen(LobbyPhoneBlockEntity.Screen.ADMIN_EDIT);
                }
            }
        } else {
            switch (key) {
                case 10:
                    this.adminPage = (this.adminPage + LobbySettings.pageCount() - 1) % LobbySettings.pageCount();
                    break;
                case 11:
                    this.adminPage = (this.adminPage + 1) % LobbySettings.pageCount();
                    break;
                case 15:
                    this.goIdle();
            }
        }
    }

    private void pressAdminEdit(int key) {
        LobbySettings.Item item = LobbySettings.item(this.adminPage, this.editItem);
        if (item == null) {
            this.setScreen(LobbyPhoneBlockEntity.Screen.ADMIN_MENU);
        } else if (isDigit(key)) {
            if (this.editValue.length() < item.maxInputLength()) {
                this.editValue = this.editValue + key;
            }
        } else {
            boolean lobbyNo = item.kind() == LobbySettings.Kind.LOBBY_NO;
            switch (key) {
                case 10:
                    if (lobbyNo) {
                        if (!this.editValue.isEmpty()) {
                            this.editDong = LobbySettings.pad(this.editValue, 4);
                        }

                        this.editValue = "";
                    }
                    break;
                case 11:
                    if (lobbyNo) {
                        if (!this.editValue.isEmpty()) {
                            this.editLine = LobbySettings.pad(this.editValue, 2);
                        }

                        this.editValue = "";
                    }
                case 12:
                default:
                    break;
                case 13:
                    if (!this.editValue.isEmpty()) {
                        this.editValue = this.editValue.substring(0, this.editValue.length() - 1);
                    }
                    break;
                case 14:
                    if (lobbyNo) {
                        if (!UnitAddress.component(this.editDong)) {
                            this.showMessage("out_of_range", "", LobbyPhoneBlockEntity.Screen.ADMIN_EDIT);
                            return;
                        }

                        this.cfg.put("lobby_no", this.editDong + "|" + this.editLine);
                        this.setChanged();
                        this.setScreen(LobbyPhoneBlockEntity.Screen.ADMIN_MENU);
                        return;
                    }

                    String err = LobbySettings.validate(item, this.editValue);
                    if (err != null) {
                        this.showMessage(err, "", LobbyPhoneBlockEntity.Screen.ADMIN_EDIT);
                    } else {
                        String v = item.kind() == LobbySettings.Kind.NUMBER ? String.valueOf(Integer.parseInt(this.editValue)) : this.editValue;
                        this.cfg.put(item.key(), v);
                        this.setChanged();
                        this.setScreen(LobbyPhoneBlockEntity.Screen.ADMIN_MENU);
                    }
                    break;
                case 15:
                    this.setScreen(LobbyPhoneBlockEntity.Screen.ADMIN_MENU);
            }
        }
    }

    private void pressCardBack(int key, LobbyPhoneBlockEntity.Screen parent) {
        if (key == 13 || key == 14) {
            this.setScreen(parent);
        } else if (key == 15) {
            this.goIdle();
        }
    }

    private void pressCardMenu(int key) {
        switch (key) {
            case 1:
                this.pendingCard = "";
                this.setScreen(LobbyPhoneBlockEntity.Screen.CARD_MASTER);
                break;
            case 2:
                this.pendingCard = "";
                this.setScreen(LobbyPhoneBlockEntity.Screen.CARD_REG);
                break;
            case 3:
                this.setScreen(LobbyPhoneBlockEntity.Screen.CARD_UNIT_MENU);
                break;
            default:
                this.pressCardBack(key, LobbyPhoneBlockEntity.Screen.ADMIN_MENU);
        }
    }

    private void pressCardRegisterSpecial(int key) {
        if (key == 0) {
            if (!this.pendingCard.isEmpty()) {
                String type = this.screen == LobbyPhoneBlockEntity.Screen.CARD_MASTER ? "master" : "reg";
                this.cards.put(this.pendingCard, new LobbyPhoneBlockEntity.Card(type, "", ""));
                this.setChanged();
                this.notice("registered", this.pendingCard);
                this.pendingCard = "";
            }
        } else {
            this.pressCardBack(key, LobbyPhoneBlockEntity.Screen.CARD_MENU);
        }
    }

    private void pressCardUnitMenu(int key) {
        switch (key) {
            case 1:
                this.resetCardUnitInput();
                this.setScreen(LobbyPhoneBlockEntity.Screen.CARD_UNIT_REG);
                break;
            case 2:
                this.setScreen(LobbyPhoneBlockEntity.Screen.CARD_DELETE);
                break;
            case 3:
                this.resetCardUnitInput();
                this.setScreen(LobbyPhoneBlockEntity.Screen.CARD_UNIT_DELETE);
                break;
            case 4:
                this.setScreen(LobbyPhoneBlockEntity.Screen.CARD_DELETE_ALL);
                break;
            default:
                this.pressCardBack(key, LobbyPhoneBlockEntity.Screen.CARD_MENU);
        }
    }

    private void resetCardUnitInput() {
        this.cardDong = "0000";
        this.cardHo = "0000";
        this.cardHoSet = false;
        this.editValue = "";
    }

    private void pressCardUnitInput(int key) {
        if (isDigit(key)) {
            if (this.editValue.length() < 4) {
                this.editValue = this.editValue + key;
            }
        } else {
            switch (key) {
                case 10:
                    if (UnitAddress.component(this.editValue)) {
                        this.cardDong = LobbySettings.pad(this.editValue, 4);
                    }

                    this.editValue = "";
                    break;
                case 11:
                    if (UnitAddress.component(this.editValue)) {
                        this.cardHo = LobbySettings.pad(this.editValue, 4);
                        this.cardHoSet = true;
                        this.editValue = "";
                    } else if (this.screen == LobbyPhoneBlockEntity.Screen.CARD_UNIT_DELETE && this.cardHoSet) {
                        int n = 0;
                        Iterator<Entry<String, LobbyPhoneBlockEntity.Card>> it = this.cards.entrySet().iterator();

                        while (it.hasNext()) {
                            LobbyPhoneBlockEntity.Card c = it.next().getValue();
                            if ("unit".equals(c.type()) && c.dong().equals(this.cardDong) && c.ho().equals(this.cardHo)) {
                                it.remove();
                                n++;
                            }
                        }

                        this.setChanged();
                        this.notice("unit_deleted", String.valueOf(n));
                    }
                    break;
                case 12:
                default:
                    this.pressCardBack(key, LobbyPhoneBlockEntity.Screen.CARD_UNIT_MENU);
                    break;
                case 13:
                    if (!this.editValue.isEmpty()) {
                        this.editValue = this.editValue.substring(0, this.editValue.length() - 1);
                    } else {
                        this.setScreen(LobbyPhoneBlockEntity.Screen.CARD_UNIT_MENU);
                    }
            }
        }
    }

    private void pressProxMenu(int key) {
        switch (key) {
            case 1:
                this.adminPage = 3;
                this.editItem = 0;
                this.editValue = this.cfg("prox_use");
                this.setScreen(LobbyPhoneBlockEntity.Screen.ADMIN_EDIT);
                break;
            case 2:
                this.cfg.put("prox_data", "40");
                this.setChanged();
                this.showMessage("prox_auto", "40", LobbyPhoneBlockEntity.Screen.PROX_MENU);
                this.melody(1.3F);
                break;
            default:
                this.pressCardBack(key, LobbyPhoneBlockEntity.Screen.ADMIN_MENU);
        }
    }

    public void tapCard(String id) {
        if (this.level instanceof ServerLevel) {
            long t = this.now();
            this.lastInput = t;
            this.backlight = true;
            LobbyPhoneBlockEntity.Card card = this.cards.get(id);
            switch (this.screen) {
                case IDLE:
                case INPUT:
                case PASSWORD:
                case COMMON_PASSWORD:
                case HELP:
                    if (card == null) {
                        this.beep(false);
                    } else if ("unit".equals(card.type())) {
                        this.beep(true);
                        this.openDoor();
                        if (this.screen != LobbyPhoneBlockEntity.Screen.IDLE) {
                            this.goIdle();
                        }
                    } else if ("master".equals(card.type())) {
                        this.beep(true);
                        this.setScreen(LobbyPhoneBlockEntity.Screen.CARD_MENU);
                    } else {
                        this.beep(true);
                        this.resetCardUnitInput();
                        this.setScreen(LobbyPhoneBlockEntity.Screen.CARD_UNIT_REG);
                    }
                    break;
                case CALLING:
                case TALKING:
                case ADMIN_PASSWORD:
                case ADMIN_MENU:
                case ADMIN_EDIT:
                case MESSAGE:
                case CARD_MENU:
                case CARD_UNIT_MENU:
                case CARD_UNIT_DELETE:
                default:
                    this.beep(false);
                    break;
                case CARD_MASTER:
                case CARD_REG:
                    this.pendingCard = id;
                    this.notice("card_touched", id);
                    this.beep(true);
                    break;
                case CARD_UNIT_REG:
                    if (this.cardHoSet && UnitAddress.component(this.cardDong) && UnitAddress.component(this.cardHo)) {
                        this.cards.put(id, new LobbyPhoneBlockEntity.Card("unit", this.cardDong, this.cardHo));
                        this.setChanged();
                        this.notice("unit_registered", id);
                        this.beep(true);
                    } else {
                        this.notice("check_unit", "");
                        this.beep(false);
                    }
                    break;
                case CARD_DELETE:
                    if (this.cards.remove(id) != null) {
                        this.setChanged();
                        this.notice("deleted", id);
                        this.beep(true);
                    } else {
                        this.notice("not_registered", id);
                        this.beep(false);
                    }
            }

            this.sync();
        }
    }

    private void notice(String key, String arg) {
        this.noticeKey = key;
        this.noticeArg = arg == null ? "" : arg;
        this.noticeUntil = this.now() + 50L;
    }

    private static boolean isDigit(int key) {
        return key >= 0 && key <= 9;
    }

    private String composeUnit() {
        String d = this.isParkingLobby() ? this.inputDong : this.lobbyDong();
        return d.isEmpty() ? this.input : d + "-" + this.input;
    }

    private void toggleLayout() {
        if (Arrays.equals(this.layout, DEFAULT_LAYOUT)) {
            List<Integer> digits = new ArrayList<>();

            for (int i = 0; i < 10; i++) {
                digits.add(i);
            }

            Collections.shuffle(digits, this.random);

            for (int i = 0; i < 10; i++) {
                this.layout[i] = digits.get(i);
            }
        } else {
            this.layout = (int[])DEFAULT_LAYOUT.clone();
        }
    }

    private void goIdle() {
        this.input = "";
        this.inputDong = "";
        this.unitInput = "";
        this.secret = "";
        this.dongStage = false;
        this.guardCall = false;
        this.bigLabel = "";
        this.editValue = "";
        this.pendingCard = "";
        this.noticeUntil = 0L;
        this.layout = (int[])DEFAULT_LAYOUT.clone();
        this.setScreen(LobbyPhoneBlockEntity.Screen.IDLE);
    }

    private void setScreen(LobbyPhoneBlockEntity.Screen s) {
        this.screen = s;
        this.stateSince = this.now();
        if (s != LobbyPhoneBlockEntity.Screen.CARD_MASTER && s != LobbyPhoneBlockEntity.Screen.CARD_REG) {
            this.noticeUntil = 0L;
        }
    }

    private void showMessage(String key, String arg, LobbyPhoneBlockEntity.Screen returnTo) {
        this.msgKey = key;
        this.msgArg = arg == null ? "" : arg;
        this.msgReturn = returnTo;
        this.setScreen(LobbyPhoneBlockEntity.Screen.MESSAGE);
    }

    private void leaveMessage() {
        if (this.msgReturn == LobbyPhoneBlockEntity.Screen.IDLE) {
            this.goIdle();
        } else {
            this.setScreen(this.msgReturn);
        }
    }

    public String linkedGuardPassword() {
        if (this.level instanceof ServerLevel sl) {
            String no = DeviceRegistry.digits(this.cfg("guard_no"));
            if (no.isEmpty()) {
                return "";
            } else {
                for (BlockPos p : DeviceRegistry.get(sl).guardsByNumber(sl, this.worldPosition, no)) {
                    if (sl.isLoaded(p) && sl.getBlockEntity(p) instanceof ReceiverBlockEntity g && g.hasDoorPassword()) {
                        return g.getDoorPassword();
                    }
                }

                return "";
            }
        } else {
            return "";
        }
    }

    private void checkPassword() {
        if (this.secret.length() >= 4) {
            boolean ok;
            if (this.screen == LobbyPhoneBlockEntity.Screen.COMMON_PASSWORD) {
                String common = this.cfg("common_password");
                String guard = this.linkedGuardPassword();
                ok = this.isCommonPasswordUse() && !common.isEmpty() && common.equals(this.secret) || !guard.isEmpty() && guard.equals(this.secret);
            } else {
                String pw = this.secret;
                ok = this.receiversOf(this.unitInput).stream().anyMatch(r -> r.checkDoorPassword(pw));
            }

            this.secret = "";
            if (ok) {
                this.failCount = 0;
                this.openDoor();
                this.goIdle();
            } else {
                this.failCount++;
                if (this.failCount >= 3) {
                    this.failCount = 0;
                    this.startGuardCall();
                } else {
                    this.setScreen(this.screen);
                }
            }
        }
    }

    private void startGuardCall() {
        if (this.level instanceof ServerLevel) {
            this.guardCall = true;
            String guardNo = this.cfg("guard_no");
            String unit = guardNo.isEmpty() ? "경비실" : guardNo;
            if (this.receiversOf(unit).isEmpty() && !"경비실".equals(unit) && !this.receiversOf("경비실").isEmpty()) {
                unit = "경비실";
            }

            CallerBlockEntity.CallResult r = this.startCall(unit);
            if (r == CallerBlockEntity.CallResult.NO_UNIT) {
                this.showMessage("no_guard", "", LobbyPhoneBlockEntity.Screen.IDLE);
            } else {
                this.afterStart(r, unit, guardNo.isEmpty() ? "경비" : guardNo);
            }
        }
    }

    private void startUnitCall(String unit, String label) {
        if (!UnitAddress.valid(unit)) {
            this.showMessage("check_unit", "", LobbyPhoneBlockEntity.Screen.IDLE);
        } else {
            this.guardCall = false;
            this.afterStart(this.startCall(unit), unit, label);
        }
    }

    private void afterStart(CallerBlockEntity.CallResult r, String unit, String label) {
        switch (r) {
            case OK:
                this.bigLabel = label;
                this.setScreen(LobbyPhoneBlockEntity.Screen.CALLING);
                this.melody(1.0F);
                break;
            case NO_UNIT:
                this.showMessage("no_unit", unit, LobbyPhoneBlockEntity.Screen.IDLE);
                break;
            case NO_SIGNAL:
                this.showMessage("no_signal", unit, LobbyPhoneBlockEntity.Screen.IDLE);
                break;
            case BUSY:
                this.showMessage("busy", unit, LobbyPhoneBlockEntity.Screen.IDLE);
        }
    }

    private void hangUpFromLobby() {
        this.hangUpFromCaller();
        this.goIdle();
    }

    private void openDoor() {
        this.openDoors();
    }

    @Override
    public void openDoors() {
        super.openDoors();
        this.doorOpenUntil = this.now() + this.openTicks();
        this.melody(1.6F);
    }

    @Override
    public String callerKey() {
        return "lobby";
    }

    @Override
    protected void onConnected() {
        this.setScreen(LobbyPhoneBlockEntity.Screen.TALKING);
    }

    @Override
    protected void onDoorOpenedByReceiver() {
        this.goIdle();
    }

    @Override
    protected void onEnded(DoorStatus reason) {
        if (reason != DoorStatus.REJECTED && reason != DoorStatus.NO_ANSWER && reason != DoorStatus.TIMEOUT && reason != DoorStatus.ENDED) {
            this.goIdle();
        } else {
            this.showMessage(reason.name().toLowerCase(Locale.ROOT), "", LobbyPhoneBlockEntity.Screen.IDLE);
        }
    }

    @Override
    public void syncScreens() {
        this.sync();
    }

    public void wake() {
        this.lastInput = this.now();
        this.keyLedUntil = this.lastInput + this.cfgInt("key_led_time") * 20L;
        if (!this.backlight) {
            this.backlight = true;
            this.sync();
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LobbyPhoneBlockEntity be) {
        long t = level.getGameTime();
        long inState = t - be.stateSince;
        boolean changed = false;
        be.tickCaller();
        if ((be.screen == LobbyPhoneBlockEntity.Screen.CALLING || be.screen == LobbyPhoneBlockEntity.Screen.TALKING) && !be.isInCall()) {
            be.goIdle();
            changed = true;
        }

        switch (be.screen) {
            case INPUT:
            case PASSWORD:
            case COMMON_PASSWORD:
            case HELP:
                if (t - be.lastInput >= 600L) {
                    be.goIdle();
                    changed = true;
                }
                break;
            case CALLING:
            case TALKING:
            case ADMIN_PASSWORD:
            case ADMIN_MENU:
            case ADMIN_EDIT:
            default:
                if (be.screen.isAdmin() && t - be.lastInput >= 2400L) {
                    be.goIdle();
                    changed = true;
                }
                break;
            case MESSAGE:
                if (inState >= 50L) {
                    be.leaveMessage();
                    changed = true;
                }
        }

        if (t % 10L == 0L && be.cfgInt("prox_use") == 1) {
            double range = Math.max(0.5, be.cfgInt("prox_data") / 20.0);
            Vec3 c = Vec3.atCenterOf(pos);
            boolean near = level.players().stream().anyMatch(p -> p.distanceToSqr(c) <= range * range);
            if (near) {
                be.lastInput = Math.max(be.lastInput, t - 1L);
                be.keyLedUntil = Math.max(be.keyLedUntil, t + be.cfgInt("key_led_time") * 20L);
                if (!be.backlight) {
                    be.backlight = true;
                    changed = true;
                }
            }
        }

        boolean always = be.cfgInt("backlight_always") == 1;
        long blTicks = Math.max(1, be.cfgInt("backlight_time")) * 20L;
        if (!always && be.backlight && be.screen == LobbyPhoneBlockEntity.Screen.IDLE && t - be.lastInput >= blTicks) {
            be.backlight = false;
            changed = true;
        } else if (always && !be.backlight) {
            be.backlight = true;
            changed = true;
        }

        if (t == be.doorOpenUntil || t == be.noticeUntil || t == be.keyLedUntil) {
            changed = true;
        }

        if (changed) {
            be.sync();
        }
    }

    private float vol(String key) {
        return this.cfgInt(key) / 5.0F;
    }

    private void keyTone() {
        if (this.level != null && this.cfgInt("key_tone_volume") > 0) {
            this.level.playSound(null, this.worldPosition, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.BLOCKS, 0.5F * this.vol("key_tone_volume"), 1.9F);
        }
    }

    private void digitTone(int digit) {
        if (this.level != null && this.cfgInt("digit_voice") == 1 && this.cfgInt("key_tone_volume") > 0) {
            float[] pitch = new float[]{0.7F, 0.75F, 0.8F, 0.85F, 0.9F, 0.95F, 1.0F, 1.05F, 1.1F, 1.2F};
            this.level
                .playSound(null, this.worldPosition, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 0.3F * this.vol("key_tone_volume"), pitch[digit]);
        }
    }

    private void melody(float pitch) {
        if (this.level != null && this.cfgInt("melody_volume") > 0) {
            this.level.playSound(null, this.worldPosition, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.6F * this.vol("melody_volume"), pitch);
        }
    }

    private void beep(boolean ok) {
        if (this.level != null) {
            this.level.playSound(null, this.worldPosition, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 0.6F, ok ? 1.6F : 0.5F);
        }
    }

    public LobbyPhoneBlockEntity.Screen getScreen() {
        return this.screen;
    }

    public String getInput() {
        return this.input;
    }

    public String getInputDong() {
        return this.inputDong;
    }

    public boolean isDongStage() {
        return this.dongStage;
    }

    public int getSecretLength() {
        return this.secretLen;
    }

    public String getBigLabel() {
        return this.bigLabel;
    }

    public boolean isGuardCall() {
        return this.guardCall;
    }

    public int[] getLayout() {
        return this.layout;
    }

    public int getDigitSize() {
        return this.digitSize;
    }

    public int getAdminPage() {
        return this.adminPage;
    }

    public int getEditItem() {
        return this.editItem;
    }

    public String getEditValue() {
        return this.editValue;
    }

    public String getEditDong() {
        return this.editDong;
    }

    public String getEditLine() {
        return this.editLine;
    }

    public String getMsgKey() {
        return this.msgKey;
    }

    public String getMsgArg() {
        return this.msgArg;
    }

    public long getStateSince() {
        return this.stateSince;
    }

    public boolean isBacklight() {
        return this.backlight;
    }

    public String getCardDong() {
        return this.cardDong;
    }

    public String getCardHo() {
        return this.cardHo;
    }

    public boolean isCardHoSet() {
        return this.cardHoSet;
    }

    public String getPendingCard() {
        return this.pendingCard;
    }

    public int getCardCount() {
        return this.cardCount;
    }

    public boolean isDoorOpen(long gameTime) {
        return gameTime < this.doorOpenUntil;
    }

    public boolean isKeyLedOn(long gameTime) {
        return this.cfgInt("key_led_always") == 1 || gameTime < this.keyLedUntil;
    }

    @Nullable
    public String activeNotice(long gameTime) {
        return gameTime < this.noticeUntil && !this.noticeKey.isEmpty() ? this.noticeKey : null;
    }

    public String getNoticeArg() {
        return this.noticeArg;
    }

    public String displaySetting(int page, int index) {
        LobbySettings.Item item = LobbySettings.item(page, index);
        if (item == null) {
            return "";
        } else {
            String v = this.cfg(item.key());

            return switch (item.kind()) {
                case LOBBY_NO -> LobbySettings.lobbyNoDisplay(v);
                case DIGITS -> v.isEmpty() && item.key().equals("guard_no") ? "경비실" : v;
                case PASSWORD -> v.isEmpty() ? "-" : v;
                default -> item.key().equals("guard_password") && v.isEmpty() ? "-" : v;
            };
        }
    }

    private void sync() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 2);
        }
    }

    private void writeSync(CompoundTag tag) {
        tag.putInt("Screen", this.screen.ordinal());
        tag.putString("Input", this.input);
        tag.putString("InputDong", this.inputDong);
        tag.putBoolean("DongStage", this.dongStage);
        tag.putInt("SecretLen", this.secret.length());
        tag.putString("Big", this.bigLabel);
        tag.putBoolean("Guard", this.guardCall);
        tag.putIntArray("Layout", this.layout);
        tag.putInt("DigitSize", this.digitSize);
        tag.putInt("AdminPage", this.adminPage);
        tag.putInt("EditItem", this.editItem);
        LobbySettings.Item item = LobbySettings.item(this.adminPage, this.editItem);
        boolean maskEdit = item != null && item.kind() == LobbySettings.Kind.PASSWORD && this.screen != LobbyPhoneBlockEntity.Screen.ADMIN_EDIT;
        tag.putString("EditValue", maskEdit ? "" : this.editValue);
        tag.putString("EditDong", this.editDong);
        tag.putString("EditLine", this.editLine);
        tag.putString("MsgKey", this.msgKey);
        tag.putString("MsgArg", this.msgArg);
        tag.putLong("Since", this.stateSince);
        tag.putBoolean("Backlight", this.backlight);
        tag.putLong("DoorUntil", this.doorOpenUntil);
        tag.putLong("LedUntil", this.keyLedUntil);
        tag.putBoolean("Connected", this.isConnected());
        tag.putBoolean("InCall", this.inCall);
        tag.putString("CardDong", this.cardDong);
        tag.putString("CardHo", this.cardHo);
        tag.putBoolean("CardHoSet", this.cardHoSet);
        tag.putString("Pending", this.pendingCard);
        tag.putString("Notice", this.noticeKey);
        tag.putString("NoticeArg", this.noticeArg);
        tag.putLong("NoticeUntil", this.noticeUntil);
        tag.putInt("Cards", this.cards.size());
        ListTag lines = new ListTag();

        for (IntercomLine l : this.log) {
            CompoundTag c = new CompoundTag();
            c.putString("S", l.side());
            c.putString("N", l.name());
            c.putString("T", l.text());
            lines.add(c);
        }

        tag.put("Log", lines);
        CompoundTag c = new CompoundTag();
        boolean admin = this.screen.isAdmin() && this.screen != LobbyPhoneBlockEntity.Screen.ADMIN_PASSWORD
            || this.screen == LobbyPhoneBlockEntity.Screen.MESSAGE && this.msgReturn.isAdmin() && this.msgReturn != LobbyPhoneBlockEntity.Screen.ADMIN_PASSWORD;

        for (LobbySettings.Item[] page : LobbySettings.PAGES) {
            for (LobbySettings.Item it : page) {
                boolean guardPw = it.key().equals("guard_password");
                if (it.kind() != LobbySettings.Kind.PASSWORD && !guardPw || admin) {
                    c.putString(it.key(), guardPw ? this.linkedGuardPassword() : this.cfg(it.key()));
                }
            }
        }

        tag.put("Cfg", c);
    }

    private void readSync(CompoundTag tag) {
        this.screen = LobbyPhoneBlockEntity.Screen.byId(tag.getInt("Screen"));
        this.input = tag.getString("Input");
        this.inputDong = tag.getString("InputDong");
        this.dongStage = tag.getBoolean("DongStage");
        this.secretLen = tag.getInt("SecretLen");
        this.bigLabel = tag.getString("Big");
        this.guardCall = tag.getBoolean("Guard");
        int[] l = tag.getIntArray("Layout");
        this.layout = l.length == 10 ? l : (int[])DEFAULT_LAYOUT.clone();
        this.digitSize = tag.getInt("DigitSize");
        this.adminPage = tag.getInt("AdminPage");
        this.editItem = tag.getInt("EditItem");
        this.editValue = tag.getString("EditValue");
        this.editDong = tag.getString("EditDong");
        this.editLine = tag.getString("EditLine");
        this.msgKey = tag.getString("MsgKey");
        this.msgArg = tag.getString("MsgArg");
        this.stateSince = tag.getLong("Since");
        this.backlight = tag.getBoolean("Backlight");
        this.doorOpenUntil = tag.getLong("DoorUntil");
        this.keyLedUntil = tag.getLong("LedUntil");
        this.clientConnected = tag.getBoolean("Connected");
        this.inCall = tag.getBoolean("InCall");
        this.cardDong = tag.getString("CardDong");
        this.cardHo = tag.getString("CardHo");
        this.cardHoSet = tag.getBoolean("CardHoSet");
        this.pendingCard = tag.getString("Pending");
        this.noticeKey = tag.getString("Notice");
        this.noticeArg = tag.getString("NoticeArg");
        this.noticeUntil = tag.getLong("NoticeUntil");
        this.cardCount = tag.getInt("Cards");
        this.log.clear();
        ListTag lines = tag.getList("Log", 10);

        for (int i = 0; i < lines.size(); i++) {
            CompoundTag c = lines.getCompound(i);
            this.log.add(new IntercomLine(c.getString("S"), c.getString("N"), c.getString("T")));
        }

        CompoundTag c = tag.getCompound("Cfg");

        for (String k : c.getAllKeys()) {
            this.cfg.put(k, c.getString(k));
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        this.writeSync(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        this.readSync(tag);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            this.readSync(tag);
        }
    }

    @Override
    public boolean isConnected() {
        return this.level != null && this.level.isClientSide ? this.clientConnected : super.isConnected();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        CompoundTag c = tag.getCompound("Settings");

        for (String k : c.getAllKeys()) {
            if (this.cfg.containsKey(k)) {
                this.cfg.put(k, c.getString(k));
            }
        }

        this.cards.clear();
        ListTag list = tag.getList("RfCards", 10);

        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            this.cards.put(e.getString("Id"), new LobbyPhoneBlockEntity.Card(e.getString("Type"), e.getString("Dong"), e.getString("Ho")));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        CompoundTag c = new CompoundTag();
        this.cfg.forEach(c::putString);
        tag.put("Settings", c);
        ListTag list = new ListTag();
        this.cards.forEach((id, card) -> {
            CompoundTag e = new CompoundTag();
            e.putString("Id", id);
            e.putString("Type", card.type());
            e.putString("Dong", card.dong());
            e.putString("Ho", card.ho());
            list.add(e);
        });
        tag.put("RfCards", list);
    }

    public record Card(String type, String dong, String ho) {
        public static final String MASTER = "master";
        public static final String REG = "reg";
        public static final String UNIT = "unit";
    }

    public static enum Screen {
        IDLE,
        INPUT,
        PASSWORD,
        COMMON_PASSWORD,
        CALLING,
        TALKING,
        HELP,
        ADMIN_PASSWORD,
        ADMIN_MENU,
        ADMIN_EDIT,
        MESSAGE,
        CARD_MENU,
        CARD_MASTER,
        CARD_REG,
        CARD_UNIT_MENU,
        CARD_UNIT_REG,
        CARD_DELETE,
        CARD_UNIT_DELETE,
        CARD_DELETE_ALL,
        PROX_MENU;

        public static LobbyPhoneBlockEntity.Screen byId(int id) {
            LobbyPhoneBlockEntity.Screen[] v = values();
            return id >= 0 && id < v.length ? v[id] : IDLE;
        }

        public boolean isAdmin() {
            return this.ordinal() >= ADMIN_PASSWORD.ordinal() && this != MESSAGE;
        }
    }
}
