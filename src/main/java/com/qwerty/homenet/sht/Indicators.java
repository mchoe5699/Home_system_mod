package com.qwerty.homenet.sht;

import java.util.List;

public final class Indicators {
    public static final int POWER = 1;
    public static final int CHARGE = 2;
    public static final int ARMED = 4;
    public static final int AWAY = 8;
    public static final int EMERGENCY = 16;
    public static final int DOOR = 32;
    public static final int GUARD = 64;
    public static final int LINE = 128;
    public static final int FIRE = 256;
    public static final int GAS = 512;
    public static final int SECURITY = 1024;
    public static final List<Indicators.Lamp> LAMPS = List.of(
        new Indicators.Lamp(1, 460, 249, 12, 4, 16737619, false),
        new Indicators.Lamp(2, 460, 229, 12, 4, 10477932, false),
        new Indicators.Lamp(4, 460, 268, 12, 4, 11001713, false),
        new Indicators.Lamp(8, 460, 305, 12, 4, 11001713, false),
        new Indicators.Lamp(32, 460, 533, 12, 4, 11001713, false),
        new Indicators.Lamp(64, 460, 496, 12, 4, 11001713, false),
        new Indicators.Lamp(128, 460, 573, 12, 4, 11001713, false),
        new Indicators.Lamp(256, 318, 182, 20, 20, 16737875, true),
        new Indicators.Lamp(512, 366, 182, 20, 20, 16113496, true),
        new Indicators.Lamp(1024, 410, 182, 20, 20, 8906620, true)
    );

    public static List<Indicators.Lamp> forModel(boolean wired) {
        return !wired
            ? LAMPS
            : List.of(
                new Indicators.Lamp(1, 321, 305, 10, 10, 15100806, true),
                new Indicators.Lamp(4, 460, 268, 12, 4, 11001713, false),
                new Indicators.Lamp(8, 460, 305, 12, 4, 11001713, false),
                new Indicators.Lamp(32, 460, 533, 12, 4, 11001713, false),
                new Indicators.Lamp(64, 460, 496, 12, 4, 11001713, false),
                new Indicators.Lamp(128, 460, 573, 12, 4, 11001713, false),
                new Indicators.Lamp(256, 310, 182, 20, 20, 16737875, true),
                new Indicators.Lamp(512, 360, 182, 20, 20, 16113496, true),
                new Indicators.Lamp(1024, 410, 182, 20, 20, 8906620, true)
            );
    }

    private Indicators() {
    }

    public record Lamp(int bit, int x, int y, int w, int h, int rgb, boolean round) {
    }
}
