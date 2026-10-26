package com.qwerty.homenet.client.family;

import java.util.Locale;

public final class FamilySearch {
    public static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[\\s_\\-]", "");
    }

    public static boolean matches(String query, String id, String label, String description) {
        String q = normalize(query);
        return q.isEmpty() || normalize(id + " " + label + " " + description).contains(q);
    }

    private FamilySearch() {
    }
}
