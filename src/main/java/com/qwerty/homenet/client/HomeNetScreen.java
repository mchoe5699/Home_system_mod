package com.qwerty.homenet.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public abstract class HomeNetScreen extends Screen {
    protected static final int BEZEL = -1775894;
    protected static final int BEZEL_EDGE = -4670268;
    protected static final int SCREEN = -15788254;
    protected static final int HEADER = -14996672;
    protected static final int ACCENT = -12604939;
    protected static final int TEXT = -1;
    protected static final int TEXT_DIM = -6641994;
    protected static final int TEXT_WARN = -14003;
    protected static final int TEXT_OK = -9510790;
    protected final int panelW;
    protected final int panelH;
    protected int left;
    protected int top;

    protected HomeNetScreen(Component title, int panelW, int panelH) {
        super(title);
        this.panelW = panelW;
        this.panelH = panelH;
    }

    protected static MutableComponent tr(String key, Object... args) {
        return Component.translatable("gui.qwertys_homenet." + key, args);
    }

    @Override
    protected void init() {
        this.left = (this.width - this.panelW) / 2;
        this.top = (this.height - this.panelH) / 2;
    }

    protected void drawDevice(GuiGraphics g, Component headerLeft, Component headerRight) {
        g.fill(this.left - 1, this.top - 1, this.left + this.panelW + 1, this.top + this.panelH + 1, -4670268);
        g.fill(this.left, this.top, this.left + this.panelW, this.top + this.panelH, -1775894);
        g.fill(this.left + 5, this.top + 5, this.left + this.panelW - 5, this.top + this.panelH - 5, -15788254);
        g.fill(this.left + 5, this.top + 5, this.left + this.panelW - 5, this.top + 20, -14996672);
        g.fill(this.left + 5, this.top + 20, this.left + this.panelW - 5, this.top + 21, -12604939);
        g.drawString(this.font, headerLeft, this.left + 10, this.top + 9, -1, false);
        if (headerRight != null) {
            int w = this.font.width(headerRight);
            g.drawString(this.font, headerRight, this.left + this.panelW - 10 - w, this.top + 9, -6641994, false);
        }
    }

    protected static String clock(long dayTime) {
        long t = Math.floorMod(dayTime, 24000L);
        int hours = (int)((t / 1000L + 6L) % 24L);
        int minutes = (int)(t % 1000L * 60L / 1000L);
        return String.format("%02d:%02d", hours, minutes);
    }

    protected static long day(long dayTime) {
        return dayTime / 24000L + 1L;
    }

    protected Component clockAndWeather() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return Component.empty();
        } else {
            String weather = level.isThundering() ? "thunder" : (level.isRaining() ? "rain" : "clear");
            return Component.literal(clock(level.getDayTime()) + "  ").append(tr("weather." + weather));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
