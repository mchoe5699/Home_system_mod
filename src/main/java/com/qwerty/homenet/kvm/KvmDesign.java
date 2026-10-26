package com.qwerty.homenet.kvm;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class KvmDesign {
    private final Map<String, String> values = new LinkedHashMap<>();

    public KvmDesign() {
        this.values.put("model", "906");
        this.values.put("logo", "default");
        this.values.put("logo_text", "MY HOME");
        this.values.put("logo_texture", "qwertys_homenet:textures/block/kvm/custom_logo.png");
        this.values.put("cover", "stripes");
        this.values.put("cover_texture", "qwertys_homenet:textures/block/kvm/custom_cover.png");
        this.values.put("body_color", "FFFFFF");
        this.values.put("cover_color", "202C3E");
        this.values.put("phone_bell", "2");
        this.values.put("volume", "2");
        this.values.put("brightness", "2");
        this.values.put("contrast", "2");
    }

    public KvmModel model() {
        return KvmModel.saved(this.values.get("model"));
    }

    public String get(String key) {
        if (key.equals("model_name")) {
            return this.model().label();
        } else if (key.equals("buttons")) {
            return Integer.toString(this.model().keys.size());
        } else if (key.equals("layout")) {
            return "right";
        } else if (key.startsWith("key") && key.matches("key[0-9]")) {
            int slot = key.charAt(3) - '0';
            return slot < this.model().keys.size() ? this.model().keys.get(slot) : "none";
        } else {
            return this.values.getOrDefault(key, "");
        }
    }

    public int number(String key) {
        return Integer.parseInt(this.get(key));
    }

    public boolean family() {
        return false;
    }

    public Map<String, String> snapshot() {
        return new LinkedHashMap<>(this.values);
    }

    public void select(KvmModel model) {
        KvmModel old = this.model();
        if (this.get("body_color").equals(old.bodyColor())) {
            this.values.put("body_color", model.bodyColor());
        }

        if (this.get("cover").equals(old.cover)) {
            this.values.put("cover", model.cover);
        }

        this.values.put("model", model.id);
        if (model.indicators) {
            this.values.put("cover", model.cover);
            this.values.put("cover_color", "151C21");
        }
    }

    public boolean set(String key, String raw) {
        if (raw != null && this.values.containsKey(key) && !key.equals("model")) {
            String value = raw.strip();

            boolean valid = switch (key) {
                case "logo_text" -> !value.isEmpty() && value.length() <= 32 && value.codePoints().noneMatch(c -> Character.isISOControl(c) || c == 167);
                case "logo" -> Set.of("default", "none", "text", "custom").contains(value);
                case "cover" -> !this.model().indicators && Set.of("stripes", "house", "plain", "custom", "squares", "triangle").contains(value);
                case "phone_bell", "volume", "brightness", "contrast" -> value.matches("[0-4]");
                case "body_color", "cover_color" -> value.matches("[0-9a-fA-F]{6}");
                case "logo_texture", "cover_texture" -> validTexture(value);
                default -> false;
            };
            if (valid) {
                this.values.put(key, value);
            }

            return valid;
        } else {
            return false;
        }
    }

    public static boolean validTexture(String value) {
        return value != null
            && value.length() <= 64
            && value.matches("[a-z0-9_.-]+:textures/[a-z0-9_./-]+\\.png")
            && !value.contains("..")
            && !value.contains("//");
    }

    public void restore(Map<String, String> saved) {
        this.values.clear();
        this.values.putAll((new KvmDesign()).values);
        this.select(KvmModel.saved(saved.get("model")));
        saved.forEach(this::set);
        if (Set.of("F2EDDC", "F0F2F0").contains(this.get("body_color").toUpperCase(Locale.ROOT))) {
            this.values.put("body_color", "FFFFFF");
        }
    }

    public double buttonX(int slot) {
        return 11.52 - (this.model().indicators ? 188 : 184) * 0.032;
    }

    public double buttonY(int slot) {
        return 11.52 - (this.model().keypad() ? 43 + slot * 19 : (this.model().indicators ? 138 + slot * 20 : 172 + slot * 21)) * 0.032;
    }

    public double lampX() {
        return 11.52 - (this.model().indicators ? 188 : 184) * 0.032;
    }

    public double lampY() {
        return 11.52 - (this.model().keypad() ? 24 : (this.model().indicators ? 119 : 151)) * 0.032;
    }
}
