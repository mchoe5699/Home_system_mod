package com.qwerty.homenet.logo;

import com.qwerty.homenet.kvm.KvmDesign;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;

public final class ProductLogo {
    private String mode = "default";
    private String text = "KOCOM";
    private String texture = "";
    private boolean configured;

    public String mode() {
        return this.mode;
    }

    public String text() {
        return this.text;
    }

    public String texture() {
        return this.texture;
    }

    public boolean configured() {
        return this.configured;
    }

    public boolean set(String key, String value) {
        if (value != null && value.length() <= 160) {
            switch (key) {
                case "logo_mode":
                    if (!Set.of("default", "none", "text", "custom").contains(value)) {
                        return false;
                    }

                    this.mode = value;
                    break;
                case "logo_text":
                    if (value.length() > 32 || value.chars().anyMatch(c -> c < 32)) {
                        return false;
                    }

                    this.text = value;
                    break;
                case "logo_texture":
                    if (!value.isEmpty() && !KvmDesign.validTexture(value)) {
                        return false;
                    }

                    this.texture = value;
                    break;
                default:
                    return false;
            }

            this.configured = true;
            return true;
        } else {
            return false;
        }
    }

    public void write(CompoundTag t) {
        CompoundTag n = new CompoundTag();
        n.putString("Mode", this.mode);
        n.putString("Text", this.text);
        n.putString("Texture", this.texture);
        n.putBoolean("Configured", this.configured);
        t.put("ProductLogo", n);
    }

    public void read(CompoundTag t) {
        CompoundTag n = t.getCompound("ProductLogo");
        this.configured = false;
        this.mode = "default";
        this.text = "KOCOM";
        this.texture = "";
        if (n.contains("Mode")) {
            this.set("logo_mode", n.getString("Mode"));
        }

        if (n.contains("Text")) {
            this.set("logo_text", n.getString("Text"));
        }

        if (n.contains("Texture")) {
            this.set("logo_texture", n.getString("Texture"));
        }

        this.configured = n.getBoolean("Configured");
    }

    public void settings(Map<String, String> m) {
        m.put("product_logo_mode", this.mode);
        m.put("product_logo_text", this.text);
        m.put("product_logo_texture", this.texture);
        m.put("product_logo_configured", this.configured ? "1" : "0");
    }
}
