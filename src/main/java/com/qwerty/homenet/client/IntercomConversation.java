package com.qwerty.homenet.client;

import com.qwerty.homenet.intercom.IntercomLine;
import com.qwerty.homenet.network.WallpadDataPacket;
import java.util.ArrayList;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public final class IntercomConversation {
    private IntercomConversation() {
    }

    public static int width(int screen) {
        return Math.min(180, Math.max(110, screen / 3));
    }

    public static void draw(GuiGraphics g, Font font, WallpadDataPacket data, int screen, int bottom) {
        int w = width(screen);
        int x = screen - w - 12;
        int top = 28;
        g.fill(x - 4, top - 4, screen - 8, bottom, -1340596192);
        g.drawString(font, "통화 메시지", x, top, -2693675);
        ArrayList<FormattedCharSequence> lines = new ArrayList<>();

        for (IntercomLine line : data.log()) {
            lines.addAll(font.split(Component.literal(line.name() + ": " + line.text()), w - 8));
        }

        int count = Math.max(0, (bottom - top - 20) / 11);
        int start = Math.max(0, lines.size() - count);

        for (int i = start; i < lines.size(); i++) {
            g.drawString(font, lines.get(i), x, top + 18 + (i - start) * 11, -1118482);
        }
    }
}
