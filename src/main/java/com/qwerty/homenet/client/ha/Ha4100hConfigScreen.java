package com.qwerty.homenet.client.ha;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.qwerty.homenet.HomeNet;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.client.IntercomConversation;
import com.qwerty.homenet.client.ReceiverScreen;
import com.qwerty.homenet.client.kvm.KvmCameraView;
import com.qwerty.homenet.client.logo.LogoRenderer;
import com.qwerty.homenet.intercom.CallState;
import com.qwerty.homenet.network.ModNetwork;
import com.qwerty.homenet.network.WallpadActionPacket;
import com.qwerty.homenet.network.WallpadDataPacket;
import com.qwerty.homenet.video.VideoFormat;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class Ha4100hConfigScreen extends Screen implements ReceiverScreen {
    private final Consumer<WallpadActionPacket> outbound;
    private WallpadDataPacket data;
    private float scale;
    private int left;
    private int top;
    private EditBox message;
    private boolean holdingEmergency;
    private boolean fired;
    private boolean suppressShortcutChar;
    private long emergencySince;
    private long lastHandsetPress;
    private final List<Ha4100hConfigScreen.Part> keys = layout("keys");
    private final List<Ha4100hConfigScreen.Part> lamps = layout("lamps");
    private final KvmCameraView camera = new KvmCameraView();

    public static List<Ha4100hConfigScreen.Part> layout(String group) {
        try {
            ArrayList var9;
            try (BufferedReader r = Minecraft.getInstance().getResourceManager().openAsReader(HomeNet.id("ha_4100h_geometry.json"))) {
                ArrayList<Ha4100hConfigScreen.Part> result = new ArrayList<>();

                for (JsonElement e : JsonParser.parseReader(r).getAsJsonObject().getAsJsonArray(group)) {
                    JsonObject o = e.getAsJsonObject();
                    result.add(
                        new Ha4100hConfigScreen.Part(
                            o.get("name").getAsString(), o.get("x").getAsInt(), o.get("y").getAsInt(), o.get("w").getAsInt(), o.get("h").getAsInt()
                        )
                    );
                }

                var9 = result;
            }

            return var9;
        } catch (IOException var8) {
            throw new IllegalStateException("HA layout missing", var8);
        }
    }

    public Ha4100hConfigScreen(WallpadDataPacket d) {
        this(d, ModNetwork::sendToServer);
    }

    public Ha4100hConfigScreen(WallpadDataPacket d, Consumer<WallpadActionPacket> outbound) {
        super(Component.literal("HYUNDAI HA-4100H"));
        this.data = d;
        this.outbound = outbound;
    }

    @Override
    public BlockPos getPos() {
        return this.data.pos();
    }

    @Override
    public void update(WallpadDataPacket d) {
        this.data = d;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void send(ReceiverBlockEntity.Action action, String value) {
        this.outbound.accept(new WallpadActionPacket(this.getPos(), action.ordinal(), BlockPos.ZERO, value));
    }

    private void button(String value) {
        if (value.equals("handset")) {
            long now = System.nanoTime();
            if (this.lastHandsetPress == 0L || now - this.lastHandsetPress >= 250000000L) {
                this.lastHandsetPress = now;
                this.send(ReceiverBlockEntity.Action.SET_SETTING, "button=handset");
            }
        } else {
            this.send(ReceiverBlockEntity.Action.SET_SETTING, "button=" + value);
        }
    }

    private void key(String value) {
        this.send(ReceiverBlockEntity.Action.SET_SETTING, "key=" + value);
    }

    @Override
    protected void init() {
        this.scale = Math.min((this.width - 40 - IntercomConversation.width(this.width)) / 1000.0F, (this.height - 150) / 784.0F);
        this.left = (int)((this.width - IntercomConversation.width(this.width) - 20 - 1000.0F * this.scale) / 2.0F);
        this.top = 50;
        this.message = new EditBox(this.font, 12, this.height - 72, Math.max(60, this.width - 100), 18, Component.literal("통화 메시지"));
        this.message.setMaxLength(80);
        this.message.setHint(Component.literal("T 또는 클릭: 통화 메시지 입력"));
        this.addRenderableWidget(this.message);
        this.addRenderableWidget(Button.builder(Component.literal("전송"), b -> {
            this.send(ReceiverBlockEntity.Action.SEND_MESSAGE, this.message.getValue());
            this.message.setValue("");
        }).bounds(this.width - 80, this.height - 72, 68, 18).build());
        int hx = this.left + (int)(62.0F * this.scale);
        int hy = this.top + (int)(35.0F * this.scale);
        int hw = Math.max(1, (int)Math.ceil(182.0F * this.scale));
        int hh = Math.max(1, (int)Math.ceil(715.0F * this.scale));
        this.addWidget(new Button(hx, hy, hw, hh, Component.literal("수화기 들기 / 놓기"), b -> this.button("handset"), narration -> narration.get()) {
            @Override
            protected boolean isValidClickButton(int mouse) {
                return mouse == 0 || mouse == 1;
            }
        });
        String[] labels = new String[]{"로고 편집", "세대 등록", "수화기", "전원", "스피커 음량", "벨 음량"};
        String[] actions = new String[]{"logo", "register", "handset", "power", "speaker", "bell"};
        int bw = Math.max(40, (this.width - 32) / 6);

        for (int i = 0; i < 6; i++) {
            String action = actions[i];
            this.addRenderableWidget(Button.builder(Component.literal(labels[i]), b -> {
                if (action.equals("register")) {
                    this.send(ReceiverBlockEntity.Action.SET_SETTING, "register");
                } else {
                    this.button(action);
                }
            }).bounds(12 + i * bw, this.height - 46, bw - 4, 18).build());
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.holdingEmergency && !this.fired && System.currentTimeMillis() - this.emergencySince >= 1000L) {
            this.fired = true;
            this.button("emergency");
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        this.renderBackground(g);
        g.drawString(this.font, this.title.getString() + " · " + this.data.setting("ha_controls_version", "구버전 서버"), 12, 8, -1118482);
        int infoWidth = this.width - IntercomConversation.width(this.width) - 32;
        g.drawString(this.font, this.font.plainSubstrByWidth(this.data.setting("status", ""), infoWidth), 12, 22, -2373742);
        String links = "전원 "
            + (this.data.flag("power") ? "켜짐" : "꺼짐")
            + " · 경비실 "
            + (this.data.flag("linked") ? "연결됨" : "미연결")
            + " · 현관 "
            + (this.data.flag("camera_linked") ? "연결됨" : "미연결")
            + (!this.data.flag("zoned") ? " · 단지 미지정" : "");
        g.drawString(this.font, this.font.plainSubstrByWidth(links, infoWidth), 12, 35, -3286578);
        g.pose().pushPose();
        g.pose().translate((float)this.left, (float)this.top, 0.0F);
        g.pose().scale(this.scale, this.scale, 1.0F);
        g.blit(tex(this.data.flag("lifted") ? "gui_open" : "gui"), 0, 0, 0.0F, 0.0F, 1000, 784, 1000, 784);

        for (int[] r : new int[][]{{282, 47, 98, 20}, {589, 46, 104, 20}, {714, 323, 72, 14}}) {
            LogoRenderer.gui(g, this.font, this.data, tex("logo"), r[0], r[1], r[2], r[3]);
        }

        boolean phase = Minecraft.getInstance().level == null || Minecraft.getInstance().level.getGameTime() % 20L < 10L;

        for (Ha4100hConfigScreen.Part l : this.lamps) {
            int mode = Integer.parseInt(this.data.setting("ha_lamp_" + l.name, "0"));
            if (mode == 1 || mode == 2 && phase) {
                g.blit(tex("light_" + l.name), l.x - l.w / 2, l.y - l.h / 2, 0.0F, 0.0F, l.w, l.h, l.w, l.h);
            }
        }

        if (this.data.flag("screen")) {
            if (this.data.flag("camera")) {
                BlockPos at = new BlockPos(
                    Integer.parseInt(this.data.setting("camera_x", "0")),
                    Integer.parseInt(this.data.setting("camera_y", "0")),
                    Integer.parseInt(this.data.setting("camera_z", "0"))
                );
                this.camera
                    .draw(
                        g,
                        at,
                        620,
                        111,
                        264,
                        176,
                        Integer.parseInt(this.data.setting("brightness", "2")),
                        Integer.parseInt(this.data.setting("contrast", "2")),
                        VideoFormat.MONOCHROME
                    );
            } else {
                g.fill(620, 111, 884, 287, -7762551);
            }
        }

        for (int[] slider : new int[][]{
            {637, Integer.parseInt(this.data.setting("contrast", "2"))}, {682, Integer.parseInt(this.data.setting("brightness", "2"))}
        }) {
            g.fill(819, slider[0] - 10, 904, slider[0] + 10, -5062486);
            g.fill(824, slider[0] - 6, 899, slider[0] + 6, -11245235);
            int x = 825 + slider[1] * 12;
            g.fill(x, slider[0] - 8, x + 25, slider[0] + 8, -328202);
        }

        g.pose().popPose();
        g.drawString(
            this.font,
            this.font
                .plainSubstrByWidth(
                    "세대 "
                        + this.data.unit()
                        + " · "
                        + (this.data.callState() != 0 ? CallState.byId(this.data.callState()).name() : this.data.setting("phone_state", "IDLE"))
                        + " "
                        + this.data.setting("digits", ""),
                    this.width - 24
                ),
            12,
            this.height - 90,
            -3286578
        );
        g.drawString(this.font, "T 채팅 · H 수화기 · 비상 1초 · 동호는 경비실에서", 12, this.height - 20, -4668232);
        IntercomConversation.draw(g, this.font, this.data, this.width, this.height - 100);
        super.render(g, mx, my, partial);
    }

    private static ResourceLocation tex(String name) {
        return HomeNet.id("textures/block/ha4100h/" + name + ".png");
    }

    @Override
    public boolean mouseClicked(double x, double y, int mouse) {
        if (super.mouseClicked(x, y, mouse)) {
            return true;
        } else if (mouse != 0) {
            return false;
        } else {
            this.setFocused(null);
            double u = (x - this.left) / this.scale;
            double v = (y - this.top) / this.scale;
            if (Math.hypot(u - 448.0, v - 88.0) < 29.0) {
                this.holdingEmergency = true;
                this.fired = false;
                this.emergencySince = System.currentTimeMillis();
                return true;
            } else if (u >= 819.0 && u <= 903.0 && Math.abs(v - 637.0) < 14.0) {
                this.button("contrast");
                return true;
            } else if (u >= 819.0 && u <= 903.0 && Math.abs(v - 682.0) < 14.0) {
                this.button("brightness");
                return true;
            } else {
                for (Ha4100hConfigScreen.Part k : this.keys) {
                    if (Math.abs(u - k.x) <= k.w / 2.0 + 3.0 && Math.abs(v - k.y) <= k.h / 2.0 + 4.0) {
                        if (k.name.startsWith("digit_")) {
                            String key = k.name.substring(6);
                            this.key(key.equals("star") ? "*" : (key.equals("hash") ? "#" : key));
                        } else {
                            this.button(k.name);
                        }

                        return true;
                    }
                }

                return false;
            }
        }
    }

    @Override
    public boolean mouseReleased(double x, double y, int mouse) {
        if (this.holdingEmergency) {
            if (!this.fired && System.currentTimeMillis() - this.emergencySince >= 1000L) {
                this.button("emergency");
            }

            this.holdingEmergency = false;
            return true;
        } else {
            return super.mouseReleased(x, y, mouse);
        }
    }

    @Override
    public boolean keyPressed(int k, int scan, int modifiers) {
        if (this.message.isFocused() || k != 84 && k != 257 && k != 335) {
            if (!this.message.isFocused() || k != 257 && k != 335) {
                if (!this.message.isFocused() && k == 72) {
                    this.button("handset");
                    return true;
                } else {
                    return super.keyPressed(k, scan, modifiers);
                }
            } else {
                this.send(ReceiverBlockEntity.Action.SEND_MESSAGE, this.message.getValue());
                this.message.setValue("");
                return true;
            }
        } else {
            this.setFocused(this.message);
            this.message.setFocused(true);
            this.suppressShortcutChar = k == 84;
            return true;
        }
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (this.suppressShortcutChar) {
            this.suppressShortcutChar = false;
            if (c == 't' || c == 'T') {
                return true;
            }
        }

        if (!this.message.isFocused() && "123456789*0#".indexOf(c) >= 0) {
            this.key(c + "");
            return true;
        } else {
            return super.charTyped(c, modifiers);
        }
    }

    public record Part(String name, int x, int y, int w, int h) {
    }
}
