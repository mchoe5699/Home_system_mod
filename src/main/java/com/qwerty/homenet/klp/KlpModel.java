package com.qwerty.homenet.klp;

import com.qwerty.homenet.block.Klp203rfBlock;
import com.qwerty.homenet.registry.ModBlocks;

public enum KlpModel {
    RF("klp_203rf", "klp203rf", "KLP-203RF", "RF"),
    NO_CARD("klp_202", "klp202", "KLP-202", "none"),
    EARLY("klp_202r2_early", "klp202r2early", "KLP-202R2 초기형", "insert"),
    LATE("klp_202r2_late", "klp202r2late", "KLP-202R2 후기형", "insert"),
    BASE("klp_203", "klp203", "KLP-203", "none"),
    R3S("klp_203r3s", "klp203r3s", "KLP-203R3S", "none"),
    COLOR("klp_204", "klp204", "KLP-204", "none"),
    COLOR_RF("klp_204rf", "klp204rf", "KLP-204RF", "RF");

    public final String id;
    public final String folder;
    public final String label;
    public final String reader;

    private KlpModel(String id, String folder, String label, String reader) {
        this.id = id;
        this.folder = folder;
        this.label = label;
        this.reader = reader;
    }

    public boolean colorVideo() {
        return this == COLOR || this == COLOR_RF;
    }

    public boolean hasCard() {
        return !this.reader.equals("none");
    }

    public Klp203rfBlock block() {
        return switch (this) {
            case RF -> (Klp203rfBlock)ModBlocks.KLP_203RF.get();
            case NO_CARD -> (Klp203rfBlock)ModBlocks.KLP_202.get();
            case EARLY -> (Klp203rfBlock)ModBlocks.KLP_202R2_EARLY.get();
            case LATE -> (Klp203rfBlock)ModBlocks.KLP_202R2_LATE.get();
            case BASE -> (Klp203rfBlock)ModBlocks.KLP_203.get();
            case R3S -> (Klp203rfBlock)ModBlocks.KLP_203R3S.get();
            case COLOR -> (Klp203rfBlock)ModBlocks.KLP_204.get();
            case COLOR_RF -> (Klp203rfBlock)ModBlocks.KLP_204RF.get();
        };
    }

    public static KlpModel byId(String id) {
        for (KlpModel m : values()) {
            if (m.id.equals(id)) {
                return m;
            }
        }

        return null;
    }
}
