package com.qwerty.homenet.client.klp;

import net.minecraft.client.gui.GuiGraphics;

public final class KlpSegments {
    private static final int[] DIGITS = new int[]{63, 6, 91, 79, 102, 109, 125, 7, 127, 111};
    public static final int[][] BARS = new int[][]{
        {4, 0, 24, 3}, {25, 4, 28, 19}, {25, 22, 28, 37}, {4, 38, 24, 41}, {0, 22, 3, 37}, {0, 4, 3, 19}, {4, 19, 24, 22}
    };

    public static int mask(char c) {
        return c >= 48 && c <= 57
            ? DIGITS[c - 48]
            : (
                c == 45
                    ? 64
                    : (
                        c == 61
                            ? 72
                            : (c == 89 ? 110 : (c == 69 ? 121 : (c == 83 ? 109 : (c == 114 ? 80 : (c == 79 ? 63 : (c == 80 ? 115 : (c == 78 ? 84 : 0)))))))
                    )
            );
    }

    public static String four(String text) {
        String s = "    " + text;
        return s.substring(s.length() - 4);
    }

    public static void gui(GuiGraphics g, String text, int x, int y) {
        String s = four(text);

        for (int i = 0; i < 4; i++) {
            for (int k = 0; k < 7; k++) {
                int[] b = BARS[k];
                boolean on = (mask(s.charAt(i)) & 1 << k) != 0;
                g.fill(x + i * 36 + b[0], y + b[1], x + i * 36 + b[2], y + b[3], on ? -51675 : -13232106);
            }
        }
    }

    private KlpSegments() {
    }
}
