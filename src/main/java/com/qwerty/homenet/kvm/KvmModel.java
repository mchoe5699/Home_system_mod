package com.qwerty.homenet.kvm;

import com.qwerty.homenet.analog.AnalogBus;
import com.qwerty.homenet.guard.GuardSystem;
import com.qwerty.homenet.video.VideoFormat;
import java.util.List;
import java.util.Locale;

public enum KvmModel {
    KVM_906("906", AnalogBus.DEDICATED, "stripes", List.of("monitor", "guard"), false),
    KVM_904("904", AnalogBus.NONE, "squares", List.of("monitor", "door"), false),
    KVM_930("930", AnalogBus.COMMON_4_WIRE, "indicators", List.of("emergency", "away", "monitor", "guard"), true),
    KVM_960("960", AnalogBus.COMMON_4_WIRE, "triangle", List.of("monitor", "guard"), false),
    KVM_933("933", AnalogBus.COMMON_8_WIRE, "indicators", List.of("away", "monitor", "guard", "door"), true),
    KVM_940("940", AnalogBus.DEDICATED, "indicators", List.of("monitor", "guard", "door"), true),
    KVM_931("931", AnalogBus.COMMON_4_WIRE, "indicators", List.of("emergency", "away", "monitor", "guard"), true),
    KVM_931_LAMP("931_screen", AnalogBus.COMMON_4_WIRE, "indicators", List.of("screen", "emergency", "away", "monitor", "guard"), true),
    KVM_932T("932t", AnalogBus.COMMON_8_WIRE, "indicators", List.of("emergency", "away", "monitor", "guard", "door", "line", "hold", "redial"), true),
    KVM_932("932", AnalogBus.COMMON_8_WIRE, "indicators", List.of("emergency", "away", "guard", "monitor", "door"), true),
    KVM_936("936", AnalogBus.COMMON_8_WIRE, "indicators", List.of("guard", "lobby", "monitor", "door"), true),
    KVM_931T("931t", AnalogBus.COMMON_8_WIRE, "indicators", List.of("emergency", "away", "monitor", "guard", "line", "hold", "redial"), true),
    KVM_940R3("940r3", AnalogBus.COMMON_8_WIRE, "indicators", List.of("monitor", "guard", "door"), true);

    public final String id;
    public final AnalogBus bus;
    public final String cover;
    public final List<String> keys;
    public final boolean indicators;

    private KvmModel(String id, AnalogBus bus, String cover, List<String> keys, boolean indicators) {
        this.id = id;
        this.bus = bus;
        this.cover = cover;
        this.keys = keys;
        this.indicators = indicators;
    }

    public boolean is931() {
        return this == KVM_931 || this == KVM_931_LAMP;
    }

    public boolean is932() {
        return this == KVM_932T || this == KVM_931T;
    }

    public boolean nativeCase() {
        return this.is931Family() || this.is932();
    }

    public boolean is931Family() {
        return this.is931() || this == KVM_932 || this == KVM_936;
    }

    public boolean manual932() {
        return this == KVM_932;
    }

    public boolean roomOnly() {
        return this == KVM_936;
    }

    public boolean busPanel() {
        return this.is932() || this.manual932() || this.roomOnly();
    }

    public boolean sameFamily(KvmModel other) {
        return other != null && (this.is932() ? other.is932() : (this.is931Family() ? other.is931Family() : !other.nativeCase()));
    }

    public String label() {
        return this == KVM_931_LAMP ? "KVM-931 (화면 버튼형)" : "KVM-" + this.id.toUpperCase(Locale.ROOT);
    }

    public boolean guard() {
        return this.bus != AnalogBus.NONE;
    }

    public boolean apartment() {
        return this.bus == AnalogBus.COMMON_4_WIRE || this.bus == AnalogBus.COMMON_8_WIRE;
    }

    public boolean emergency() {
        return this == KVM_930 || this.nativeCase() && !this.roomOnly() || this == KVM_933;
    }

    public boolean keypad() {
        return this.is932() || this == KVM_940 || this == KVM_940R3;
    }

    public boolean lobby() {
        return this.busPanel() || this == KVM_933 || this == KVM_940R3;
    }

    public boolean away() {
        return !this.roomOnly() && (this == KVM_930 || this.nativeCase() || this.lobby());
    }

    public VideoFormat videoFormat() {
        return VideoFormat.MONOCHROME;
    }

    public String bodyColor() {
        return "FFFFFF";
    }

    public double size() {
        return this.nativeCase() ? 0.9 : 1.125;
    }

    public double heightPixels() {
        return this.nativeCase() ? 8.0 * this.size() : 6.4 * this.size();
    }

    public double widthPixels() {
        return this.nativeCase() ? 8.12 * this.size() : this.heightPixels();
    }

    public boolean multiSource() {
        return this.keypad() || this.is931Family();
    }

    public double depthPixels() {
        return this.nativeCase() ? 1.03 * this.size() : 1.410909090909091 * this.size();
    }

    public static KvmModel byId(String id) {
        for (KvmModel model : values()) {
            if (model.id.equals(id)) {
                return model;
            }
        }

        return null;
    }

    public static KvmModel saved(String id) {
        KvmModel m = byId(id);
        return m == null ? KVM_906 : m;
    }

    public GuardSystem guardSystem() {
        return this == KVM_904 ? GuardSystem.NONE : (this != KVM_906 && this != KVM_940 ? GuardSystem.BUS : GuardSystem.STAR);
    }

    public String manufacturer() {
        return "KOCOM";
    }

    public String keyLabel(String key) {
        return switch (key) {
            case "lobby" -> "로 비";
            case "line" -> "국선";
            case "hold" -> "보류";
            case "redial" -> "재다이얼";
            case "screen" -> "화 면";
            case "monitor" -> this.keypad() ? (this == KVM_940R3 ? "현관/로비" : "현관") : (this == KVM_933 ? "현관/LP" : "현 관");
            case "guard" -> this.keypad() ? "경비" : (this != KVM_960 && !this.indicators ? "경 비" : "경비실");
            case "door" -> this.keypad() ? "개폐" : "문열림";
            case "away" -> "외 출";
            case "emergency" -> "비 상";
            default -> "";
        };
    }
}
