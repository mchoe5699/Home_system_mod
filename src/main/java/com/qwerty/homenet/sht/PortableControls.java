package com.qwerty.homenet.sht;

import com.qwerty.homenet.network.WallpadDataPacket;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

final class PortableControls extends Controls {
    PortableControls(WallpadDataPacket d) {
        super(d, true, Component.literal("삼성 " + d.setting("model", "SHT-104M") + " 무선전화"));
    }

    @Override
    boolean wired() {
        return true;
    }

    @Override
    void send(String a, String v) {
        Wire.sendPortable(this.data.pos(), a, v);
    }

    @Override
    ResourceLocation texture(String p) {
        String model = this.data.setting("model", "");
        return Product.id((!model.equals("SHT-104M") && !model.equals("SHT-104C") ? "portable102/" : "portable104m/") + p);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (GuiEventListener child : this.children()) {
            if (child.mouseClicked(mx, my, button)) {
                return true;
            }
        }

        if (button != 0) {
            return false;
        } else {
            PortableVisual.load();
            double u = (mx - this.x) / this.scale;
            double v = (my - this.y) / this.scale;
            boolean model104 = Set.of("SHT-104M", "SHT-104C", "SHT-104CS").contains(this.data.setting("model", ""));

            for (Surface.Key k : model104 ? PortableVisual.keys104 : PortableVisual.keys) {
                if (Math.abs(u - k.x()) <= k.w() / 2.0F + 2.0F && Math.abs(v - k.y()) <= k.h() / 2.0F + 3.0F) {
                    if (k.name().startsWith("r") && k.name().length() == 2) {
                        this.send("digit", k.name().substring(1));
                    } else {
                        this.send("press", k.name());
                    }

                    return true;
                }
            }

            return false;
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float dt) {
        super.render(g, mx, my, dt);
        int battery = 0;
        int number = 0;
        boolean power = false;
        if (this.minecraft.player != null) {
            for (int i = 0; i < this.minecraft.player.getInventory().getContainerSize(); i++) {
                ItemStack s = this.minecraft.player.getInventory().getItem(i);
                if (Portable.isPortable(s) && s.hasTag() && s.getTag().getLong("base") == this.data.pos().asLong()) {
                    battery = s.getTag().getInt("battery");
                    power = s.getTag().getBoolean("power");
                    number = s.getTag().getInt("Sht104PortableNumber");
                    break;
                }
            }
        }

        g.drawString(
            this.font,
            (power ? "전원 ON" : "전원 OFF") + " · 배터리 " + Math.round(battery * 100.0F / 36000.0F) + "%" + (number > 0 ? " · 무선 " + number + "번" : ""),
            12,
            39,
            -2237000
        );
        if (this.data.flag("portable_ringing") && this.minecraft.level.getGameTime() / 5L % 2L == 0L
            || this.data.flag("portable_active")
            || Set.of("1", "3").contains(this.data.setting("intercom", "0"))) {
            int lx = this.x + (int)((!this.data.setting("intercom", "0").equals("0") ? 148 : 76) * this.scale);
            int ly = this.y + (int)(474.0F * this.scale);
            g.fill(lx, ly, lx + Math.max(2, (int)(8.0F * this.scale)), ly + Math.max(1, (int)(3.0F * this.scale)), -6230150);
        }
    }
}
