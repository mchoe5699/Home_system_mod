package com.qwerty.homenet.guard;

import java.util.Locale;

public enum GuardSystem {
    NONE,
    STAR,
    BUS;

    public boolean compatible(String manufacturer, GuardSystem exchangeSystem, String exchangeManufacturer) {
        if (this == NONE || this != exchangeSystem) {
            return false;
        } else if (this == STAR) {
            return true;
        } else {
            String own = normalize(manufacturer);
            String other = normalize(exchangeManufacturer);
            return !own.isEmpty() && own.equals(other);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
