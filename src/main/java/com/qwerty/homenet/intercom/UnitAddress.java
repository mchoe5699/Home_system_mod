package com.qwerty.homenet.intercom;

public final class UnitAddress {
    public static final int MIN = 1;
    public static final int MAX = 9999;

    private UnitAddress() {
    }

    public static boolean component(String value) {
        return value != null && value.matches("[0-9]{1,4}") && Integer.parseInt(value) >= 1;
    }

    public static String canonical(String raw) {
        if (raw == null) {
            return "";
        } else {
            String value = raw.trim().replace("동", "-").replace("호", "").replaceAll("\\s+", "");
            String[] parts = value.split("-", -1);
            if (parts.length == 2 && component(parts[0]) && component(parts[1])) {
                return Integer.parseInt(parts[0]) + "-" + Integer.parseInt(parts[1]);
            } else {
                return parts.length == 1 && component(value) ? String.valueOf(Integer.parseInt(value)) : "";
            }
        }
    }

    public static boolean valid(String value) {
        return !canonical(value).isEmpty();
    }

    public static boolean full(String value) {
        return canonical(value).contains("-");
    }
}
