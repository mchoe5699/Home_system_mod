package com.qwerty.homenet.client.lobby;

import com.qwerty.homenet.HomeNet;
import com.qwerty.homenet.blockentity.LobbyPhoneBlockEntity;
import java.time.LocalDateTime;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

public record LobbyLcd(Component topLeft, Component topRight, Component big, float bigScale, Component subLeft, Component subRight) {
    public static final ResourceLocation FONT = HomeNet.id("lcd");
    public static final ResourceLocation KEY_FONT = HomeNet.id("lcd_key");
    public static final ResourceLocation BIG_FONT = HomeNet.id("lcd_big");
    public static final ResourceLocation ICON_FONT = HomeNet.id("lcd_icons");
    public static final String ICON_NETWORK = "\ue000";
    public static final String ICON_DOOR = "\ue001";
    private static final Component EMPTY = Component.empty();

    public static MutableComponent lcd(Component c) {
        return c.copy().withStyle(s -> s.withFont(FONT));
    }

    public static MutableComponent lcd(String s) {
        return Component.literal(s).withStyle(st -> st.withFont(FONT));
    }

    public static MutableComponent tr(String key, Object... args) {
        return lcd(Component.translatable("lobby.qwertys_homenet." + key, args));
    }

    public static MutableComponent msg(String key, String arg) {
        return lcd(Component.translatable("lobby_msg.qwertys_homenet." + key, arg));
    }

    public static MutableComponent keyDigit(String s) {
        return Component.literal(s).withStyle(st -> st.withFont(KEY_FONT));
    }

    public static MutableComponent icon(String s) {
        return Component.literal(s).withStyle(st -> st.withFont(ICON_FONT));
    }

    public static MutableComponent big(String s) {
        return s.matches("[0-9:*\\-]+") ? Component.literal(s).withStyle(st -> st.withFont(BIG_FONT)) : lcd(s);
    }

    public static boolean isBitmapBig(Component c) {
        return c.getString().matches("[0-9:*\\-]+");
    }

    public static LobbyLcd of(LobbyPhoneBlockEntity be, long gameTime) {
        LobbyPhoneBlockEntity.Screen s = be.getScreen();

        return switch (s) {
            case IDLE -> idle();
            case INPUT -> {
                Component sub;
                if (be.isParkingLobby()) {
                    sub = be.isDongStage() ? tr("dong", be.getInput()) : tr("dong_ho", be.getInputDong(), be.getInput());
                } else if (!be.lobbyDong().isEmpty()) {
                    sub = tr("dong_ho", be.lobbyDong(), be.getInput());
                } else {
                    sub = tr("ho", be.getInput());
                }

                yield new LobbyLcd(EMPTY, EMPTY, big(be.getInput()), 3.4F, sub, EMPTY);
            }
            case PASSWORD, COMMON_PASSWORD -> {
                int n = be.getSecretLength();
                Component b = n == 0 ? tr("password") : big("*".repeat(n));
                yield new LobbyLcd(EMPTY, EMPTY, b, n == 0 ? 2.6F : 3.4F, tr("enter_password"), EMPTY);
            }
            case CALLING, TALKING -> {
                long secs = Math.max(0L, (gameTime - be.getStateSince()) / 20L);
                Component timer = lcd(String.format("%02d:%02d", secs / 60L, secs % 60L));
                String what = be.isGuardCall() ? "guard" : "unit";
                Component sub = tr((s == LobbyPhoneBlockEntity.Screen.CALLING ? "calling_" : "talking_") + what);
                yield new LobbyLcd(EMPTY, EMPTY, big(be.getBigLabel()), 3.4F, sub, timer);
            }
            case MESSAGE -> new LobbyLcd(EMPTY, EMPTY, msg(be.getMsgKey(), be.getMsgArg()), 1.0F, EMPTY, EMPTY);
            case HELP -> new LobbyLcd(EMPTY, EMPTY, tr("help_title"), 1.8F, EMPTY, EMPTY);
            default -> new LobbyLcd(EMPTY, EMPTY, tr("admin_title"), 1.6F, EMPTY, EMPTY);
        };
    }

    public static LobbyLcd idle() {
        LocalDateTime now = LocalDateTime.now();
        int h = now.getHour();
        int h12 = h % 12 == 0 ? 12 : h % 12;
        Component ampm = tr(h < 12 ? "am" : "pm");
        Component date = tr("date", now.getMonthValue(), now.getDayOfMonth(), tr("dow." + now.getDayOfWeek().getValue()));
        Component time = big(h12 + ":" + String.format("%02d", now.getMinute()));
        return new LobbyLcd(ampm, date, time, 3.1F, EMPTY, EMPTY);
    }
}
