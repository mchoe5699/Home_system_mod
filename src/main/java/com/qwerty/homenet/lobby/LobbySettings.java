package com.qwerty.homenet.lobby;

import java.util.LinkedHashMap;
import java.util.Map;

public final class LobbySettings {
    public static final String APP_VER = "App Ver : 070.501.73";
    public static final String FW_VER = "FW Ver : 02.09";
    public static final LobbySettings.Item[][] PAGES = new LobbySettings.Item[][]{
        {
                new LobbySettings.Item("lobby_no", LobbySettings.Kind.LOBBY_NO, 0, 0, "0101|13"),
                new LobbySettings.Item("guard_no", LobbySettings.Kind.DIGITS, 0, 8, ""),
                new LobbySettings.Item("manage_line", LobbySettings.Kind.READONLY, 0, 0, "01 - 10 라인"),
                new LobbySettings.Item("system_password", LobbySettings.Kind.PASSWORD, 4, 4, "0000"),
                new LobbySettings.Item("network_type", LobbySettings.Kind.READONLY, 0, 0, "0"),
                new LobbySettings.Item("parking_lookup", LobbySettings.Kind.READONLY, 0, 0, "0"),
                new LobbySettings.Item("rf_server", LobbySettings.Kind.READONLY, 0, 0, "010.254.254.100")
        },
        {
                new LobbySettings.Item("parking_lobby", LobbySettings.Kind.NUMBER, 0, 1, "0"),
                new LobbySettings.Item("rf_server_use", LobbySettings.Kind.READONLY, 0, 0, "1"),
                new LobbySettings.Item("ip_address", LobbySettings.Kind.READONLY, 0, 0, "010.101.254.013"),
                new LobbySettings.Item("subnet_mask", LobbySettings.Kind.READONLY, 0, 0, "255.255.000.000"),
                new LobbySettings.Item("gateway", LobbySettings.Kind.READONLY, 0, 0, "010.101.000.254"),
                new LobbySettings.Item("complex_server", LobbySettings.Kind.READONLY, 0, 0, "010.254.254.001"),
                new LobbySettings.Item("sip_server", LobbySettings.Kind.READONLY, 0, 0, "010.254.254.001")
        },
        {
                new LobbySettings.Item("guard_password", LobbySettings.Kind.READONLY, 0, 4, ""),
                new LobbySettings.Item("common_password", LobbySettings.Kind.PASSWORD, 0, 4, ""),
                new LobbySettings.Item("elevator_mode", LobbySettings.Kind.READONLY, 0, 0, "0"),
                new LobbySettings.Item("monitor_mode", LobbySettings.Kind.READONLY, 0, 0, "0"),
                new LobbySettings.Item("door_state", LobbySettings.Kind.READONLY, 0, 0, "0"),
                new LobbySettings.Item("sensor1", LobbySettings.Kind.NUMBER, 0, 2, "1"),
                new LobbySettings.Item("sensor2", LobbySettings.Kind.NUMBER, 0, 2, "2")
        },
        {
                new LobbySettings.Item("prox_use", LobbySettings.Kind.NUMBER, 0, 1, "1"),
                new LobbySettings.Item("prox_data", LobbySettings.Kind.NUMBER, 0, 255, "40"),
                new LobbySettings.Item("light_use", LobbySettings.Kind.NUMBER, 0, 1, "1"),
                new LobbySettings.Item("light_button_data", LobbySettings.Kind.NUMBER, 12, 255, "138"),
                new LobbySettings.Item("light_camera_data", LobbySettings.Kind.NUMBER, 12, 255, "250"),
                new LobbySettings.Item("key_led_always", LobbySettings.Kind.NUMBER, 0, 1, "1"),
                new LobbySettings.Item("key_led_time", LobbySettings.Kind.NUMBER, 1, 20, "10")
        },
        {
                new LobbySettings.Item("backlight_always", LobbySettings.Kind.NUMBER, 0, 1, "0"),
                new LobbySettings.Item("backlight_time", LobbySettings.Kind.NUMBER, 1, 20, "20"),
                new LobbySettings.Item("digit_voice", LobbySettings.Kind.NUMBER, 0, 1, "1"),
                new LobbySettings.Item("open_time", LobbySettings.Kind.NUMBER, 1, 8, "3"),
                new LobbySettings.Item("guard_volume", LobbySettings.Kind.NUMBER, 1, 5, "3"),
                new LobbySettings.Item("unit_volume", LobbySettings.Kind.NUMBER, 1, 5, "3"),
                new LobbySettings.Item("melody_volume", LobbySettings.Kind.NUMBER, 0, 5, "3")
        },
        {
                new LobbySettings.Item("key_tone_volume", LobbySettings.Kind.NUMBER, 0, 5, "3"),
                new LobbySettings.Item("video_out", LobbySettings.Kind.READONLY, 0, 0, "255"),
                new LobbySettings.Item("common_password_use", LobbySettings.Kind.NUMBER, 0, 1, "1"),
                new LobbySettings.Item("dsp_unit", LobbySettings.Kind.READONLY, 0, 0, "01 03 00 20 ..."),
                new LobbySettings.Item("dsp_guard", LobbySettings.Kind.READONLY, 0, 0, "01 03 00 20 ..."),
                new LobbySettings.Item("dsp_melody", LobbySettings.Kind.READONLY, 0, 0, "FF FF FF FF ..."),
                new LobbySettings.Item("dsp_keytone", LobbySettings.Kind.READONLY, 0, 0, "FF FF FF FF ...")
        }
    };

    private LobbySettings() {
    }

    public static int pageCount() {
        return PAGES.length;
    }

    public static LobbySettings.Item item(int page, int index) {
        return page >= 0 && page < PAGES.length && index >= 0 && index < PAGES[page].length ? PAGES[page][index] : null;
    }

    public static Map<String, String> defaults() {
        Map<String, String> m = new LinkedHashMap<>();

        for (LobbySettings.Item[] page : PAGES) {
            for (LobbySettings.Item it : page) {
                m.put(it.key(), it.def());
            }
        }

        return m;
    }

    public static String validate(LobbySettings.Item item, String value) {
        switch (item.kind()) {
            case NUMBER:
                if (value.isEmpty()) {
                    return "out_of_range";
                } else {
                    try {
                        int v = Integer.parseInt(value);
                        return v >= item.min() && v <= item.max() ? null : "out_of_range";
                    } catch (NumberFormatException var3) {
                        return "out_of_range";
                    }
                }
            case PASSWORD:
                if (!value.matches("\\d*")) {
                    return "out_of_range";
                } else {
                    if (value.isEmpty()) {
                        return item.min() == 0 ? null : "need_4_digits";
                    }

                    return value.length() != 4 ? "need_4_digits" : null;
                }
            case LOBBY_NO:
            default:
                return null;
            case DIGITS:
                if (!value.matches("\\d*")) {
                    return "out_of_range";
                } else {
                    return value.length() > item.max() ? "out_of_range" : null;
                }
        }
    }

    public static String lobbyNoDisplay(String v) {
        String[] p = v.split("\\|", -1);
        String dong = p.length > 0 ? p[0] : "";
        String line = p.length > 1 ? p[1] : "";
        return dong + " 동   " + line + " 라인";
    }

    public static String pad(String digits, int len) {
        String d = digits.length() > len ? digits.substring(digits.length() - len) : digits;
        return "0".repeat(len - d.length()) + d;
    }

    public record Item(String key, LobbySettings.Kind kind, int min, int max, String def) {
        public int maxInputLength() {
            return switch (this.kind) {
                case NUMBER -> String.valueOf(this.max).length();
                case PASSWORD -> 4;
                case LOBBY_NO -> 4;
                default -> this.max;
            };
        }
    }

    public static enum Kind {
        NUMBER,
        DIGITS,
        PASSWORD,
        LOBBY_NO,
        READONLY;
    }
}
