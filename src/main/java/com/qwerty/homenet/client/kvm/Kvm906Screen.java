package com.qwerty.homenet.client.kvm;

import com.mojang.blaze3d.systems.RenderSystem;
import com.qwerty.homenet.HomeNet;
import com.qwerty.homenet.blockentity.Kvm906BlockEntity;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.client.ReceiverScreen;
import com.qwerty.homenet.client.logo.LogoRenderer;
import com.qwerty.homenet.intercom.CallState;
import com.qwerty.homenet.kvm.KvmDesign;
import com.qwerty.homenet.kvm.KvmModel;
import com.qwerty.homenet.network.ModNetwork;
import com.qwerty.homenet.network.WallpadActionPacket;
import com.qwerty.homenet.network.WallpadDataPacket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class Kvm906Screen extends Screen implements ReceiverScreen {
    private final BlockPos pos;
    private WallpadDataPacket data;
    private final KvmDesign design = new KvmDesign();
    private final KvmCameraView cameraView = new KvmCameraView();
    private boolean editing;
    private int page;
    private float scale;
    private int left;
    private int top;
    private final Map<String, EditBox> fields = new LinkedHashMap<>();
    private final List<Kvm906Screen.Hit> hits = new ArrayList<>();

    public Kvm906Screen(WallpadDataPacket data) {
        super(Component.literal("KOCOM KVM-906"));
        this.pos = data.pos();
        this.data = data;
        this.design.restore(data.settings());
        this.editing = data.notice().equals("kvm_edit");
    }

    @Override
    public BlockPos getPos() {
        return this.pos;
    }

    @Override
    public void update(WallpadDataPacket packet) {
        String oldModel = this.design.model().id;
        this.data = packet;
        this.design.restore(packet.settings());
        if (!oldModel.equals(this.design.model().id)) {
            this.rebuildWidgets();
        }

        if (!this.data.flag("unit_edit") && this.fields.containsKey("unit")) {
            this.fields.get("unit").setValue(this.data.unit());
        }

        if (packet.notice().equals("kvm_edit") && !this.editing) {
            this.editing = true;
            this.rebuildWidgets();
        }
    }

    private static String tr(String key) {
        return Component.translatable("gui.qwertys_homenet.kvm." + key).getString();
    }

    private void send(ReceiverBlockEntity.Action action, String text) {
        ModNetwork.sendToServer(new WallpadActionPacket(this.pos, action.ordinal(), BlockPos.ZERO, text));
    }

    private void setting(String key, String value) {
        this.send(ReceiverBlockEntity.Action.SET_SETTING, key + "=" + value);
    }

    private void cycle(String key, String... options) {
        int i = List.of(options).indexOf(this.design.get(key));
        this.setting(key, options[(i + 1) % options.length]);
    }

    @Override
    protected void init() {
        this.scale = Math.min(1.25F, Math.min((this.width - 12) / 442.0F, (this.height - 12) / 270.0F));
        this.left = Math.round((this.width - 442.0F * this.scale) / 2.0F);
        this.top = Math.round((this.height - 270.0F * this.scale) / 2.0F);
        this.rebuildWidgets();
    }

    private void field(String key, String value, int y, int max) {
        EditBox box = new EditBox(
            this.font,
            this.left + Math.round(240.0F * this.scale),
            this.top + Math.round(y * this.scale),
            Math.round(172.0F * this.scale),
            Math.round(18.0F * this.scale),
            Component.literal(tr(key))
        );
        box.setMaxLength(max);
        box.setValue(value);
        this.fields.put(key, this.addRenderableWidget(box));
    }

    @Override
    protected void rebuildWidgets() {
        this.clearWidgets();
        this.fields.clear();
        if (!this.editing) {
            if (this.design.model().guard()) {
                this.field("unit", this.data.unit(), 62, 16);
                this.fields.get("unit").setEditable(this.data.flag("unit_edit"));
            }

            this.field("message", "", 205, 128);
            if (this.design.model().keypad()) {
                this.field("phone_number", this.data.setting("phone_number", ""), 100, 16);
            }
        } else if (this.page == 0) {
            this.field("logo_text", this.design.get("logo_text"), 90, 32);
            this.field("logo_texture", this.design.get("logo_texture"), 136, 64);
            this.field("cover_texture", this.design.get("cover_texture"), 202, 64);
        } else if (this.page == 1) {
            this.field("body_color", this.design.get("body_color"), 64, 6);
            this.field("cover_color", this.design.get("cover_color"), 110, 6);
            this.field("model_name", this.design.get("model_name"), 168, 32);
            this.fields.get("model_name").setEditable(this.design.family());
        } else if (this.design.model().guard()) {
            this.field("unit", this.data.unit(), 218, 16);
            this.fields.get("unit").setEditable(this.data.flag("unit_edit"));
        }
    }

    private void applyField(String key) {
        EditBox box = this.fields.get(key);
        if (box != null) {
            if (key.equals("unit")) {
                this.send(ReceiverBlockEntity.Action.SET_UNIT, box.getValue());
            } else if (key.equals("message")) {
                this.send(ReceiverBlockEntity.Action.SEND_MESSAGE, box.getValue());
                box.setValue("");
            } else {
                this.setting(key, box.getValue());
            }
        }
    }

    private void label(GuiGraphics g, String value, int x, int y, int color) {
        g.drawString(this.font, value, x, y, color, false);
    }

    private void button(GuiGraphics g, String text, int x, int y, int w, int h, Runnable action) {
        rounded(g, x, y, w, h, 3, -13023919);
        this.label(g, text, x + 4, y + (h - 8) / 2, -1118482);
        this.hits.add(new Kvm906Screen.Hit(x, y, w, h, action));
    }

    private void apply(GuiGraphics g, String key, int y) {
        this.button(g, tr("apply"), 414, y, 26, 18, () -> this.applyField(key));
    }

    private static void rounded(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        for (int row = 0; row < h; row++) {
            double dy = row < r ? r - row - 0.5 : (row >= h - r ? row - (h - r) + 0.5 : 0.0);
            int inset = dy == 0.0 ? 0 : (int)Math.ceil(r - Math.sqrt(Math.max(0.0, r * r - dy * dy)));
            g.fill(x + inset, y + row, x + w - inset, y + row + 1, color);
        }
    }

    private void image(GuiGraphics g, ResourceLocation location, int x, int y, int w, int h, int color) {
        RenderSystem.setShaderColor((color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, 1.0F);
        g.blit(location, x, y, w, h, 0.0F, 0.0F, 256, 256, 256, 256);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void image(GuiGraphics g, ResourceLocation location, int x, int y, int w, int h, int color, int tw, int th) {
        RenderSystem.setShaderColor((color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, 1.0F);
        g.blit(location, x, y, w, h, 0.0F, 0.0F, tw, th, tw, th);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void hardwareLabel(GuiGraphics g, String text, int x, int y, int maxWidth) {
        g.pose().pushPose();
        g.pose().translate((float)x, (float)y, 0.0F);
        float fit = Math.min(1.0F, (float)maxWidth / Math.max(1, this.font.width(text)));
        g.pose().scale(fit, fit, 1.0F);
        this.label(g, text, 0, 0, -10987696);
        g.pose().popPose();
    }

    private void device931(GuiGraphics g) {
        this.image(
            g,
            Kvm931Renderer.texture(
                this.design.model() == KvmModel.KVM_931_LAMP
                    ? (this.data.flag("offhook") ? "gui_lamp_open" : "gui_lamp")
                    : (this.data.flag("offhook") ? "gui_open" : "gui")
            ),
            0,
            24,
            220,
            220,
            Integer.parseInt(this.design.get("body_color"), 16),
            500,
            500
        );
        if (this.data.flag("screen")) {
            boolean visible = false;

            try {
                String raw = this.data.setting("camera", "");
                if (!raw.isEmpty()) {
                    visible = this.cameraView
                        .draw(
                            g,
                            BlockPos.of(Long.parseLong(raw)),
                            86,
                            64,
                            70,
                            50,
                            this.design.number("brightness"),
                            this.design.number("contrast"),
                            this.design.model().videoFormat()
                        );
                }
            } catch (NumberFormatException var8) {
            }

            if (!visible) {
                g.fill(86, 64, 156, 114, -8946054);
                this.hardwareLabel(g, tr("no_signal"), 91, 83, 60);
            }

            for (int y = 65; y < 114; y += 3) {
                g.fill(86, y, 156, y + 1, 419430400);
            }
        }

        if (this.data.flag("power")) {
            this.image(g, Kvm931Renderer.texture("led_power"), 178, 124, 10, 3, 16722256, 64, 16);
        }

        boolean blink = this.minecraft.level != null && this.minecraft.level.getGameTime() % 20L < 10L;
        String[] functions = new String[]{"emergency", "away", "monitor", "guard"};

        for (int i = 0; i < 4; i++) {
            String mode = this.data.setting("led_" + functions[i], "off");
            int y = 24 + Math.round((310 + i * 40) * 0.44F);
            if (mode.equals("on") || mode.equals("blink") && blink) {
                this.image(g, Kvm931Renderer.texture("led_round"), 174, y - 1, 2, 2, i < 2 ? 16726325 : 3595346, 32, 32);
            }

            int slot = i + (this.design.model() == KvmModel.KVM_931_LAMP ? 1 : 0);
            this.hits.add(new Kvm906Screen.Hit(176, y - 6, 14, 13, () -> this.setting("button", Integer.toString(slot))));
        }

        if (this.design.model() == KvmModel.KVM_931_LAMP) {
            this.hits.add(new Kvm906Screen.Hit(176, 137, 14, 13, () -> this.setting("button", "0")));
        }

        LogoRenderer.gui(g, this.font, this.data, Kvm931Renderer.texture("logo"), 79, 133, 25, 6);
        this.hits.add(new Kvm906Screen.Hit(21, 46, 47, 180, () -> this.send(ReceiverBlockEntity.Action.PICKUP, "")));
        this.label(g, tr("handset_hint"), 3, 251, -3814448);
    }

    private void device932(GuiGraphics g) {
        this.image(
            g,
            Kvm931Renderer.modelTexture(this.design.model().id, this.data.flag("offhook") ? "gui_open" : "gui"),
            0,
            24,
            220,
            220,
            Integer.parseInt(this.design.get("body_color"), 16),
            500,
            500
        );
        if (this.data.flag("screen")) {
            boolean visible = false;

            try {
                String raw = this.data.setting("camera", "");
                if (!raw.isEmpty()) {
                    visible = this.cameraView
                        .draw(
                            g,
                            BlockPos.of(Long.parseLong(raw)),
                            86,
                            58,
                            75,
                            57,
                            this.design.number("brightness"),
                            this.design.number("contrast"),
                            this.design.model().videoFormat()
                        );
                }
            } catch (NumberFormatException var10) {
            }

            if (!visible) {
                g.fill(86, 58, 161, 115, -8946054);
                this.hardwareLabel(g, tr("no_signal"), 91, 80, 60);
            }

            for (int y = 59; y < 115; y += 3) {
                g.fill(86, y, 161, y + 1, 419430400);
            }
        }

        boolean phase = this.minecraft.level != null && this.minecraft.level.getGameTime() % 20L < 10L;
        if (this.data.flag("power")) {
            this.image(g, Kvm931Renderer.texture("led_round"), 162, 142, 3, 3, 16722256, 32, 32);
        }

        String[] fs = new String[]{"away", "monitor", "guard"};

        for (int i = 0; i < 3; i++) {
            String mode = this.data.setting("led_" + fs[i], "off");
            if (mode.equals("on") || mode.equals("blink") && phase) {
                this.image(g, Kvm931Renderer.texture("led_round"), 174, 87 + i * 13, 3, 3, 3595346, 32, 32);
            }
        }

        if (this.data.flag("emergency")) {
            this.image(g, Kvm931Renderer.texture("932:emergency"), 175, 61, 15, 15, 16777215, 80, 80);
        }

        if (this.data.setting("led_telephone", "").equals("on") || this.data.setting("led_telephone", "").equals("blink") && phase) {
            this.image(g, Kvm931Renderer.texture("led_power"), 157, 159, 8, 2, 5758305, 64, 16);
        }

        if (this.data.flag("phone_held")) {
            this.image(g, Kvm931Renderer.texture("led_power"), 171, 159, 8, 2, 14853696, 64, 16);
        }

        int[] ys = new int[]{100, 145, 175, 205, 270};

        for (int ix = 0; ix < (this.design.model() == KvmModel.KVM_931T ? 4 : 5); ix++) {
            int i = ix;
            int y = 24 + Math.round(ys[ix] * 0.44F);
            this.hits.add(new Kvm906Screen.Hit(175, y - 7, 15, 14, () -> this.setting("button", Integer.toString(i))));
        }

        for (int ix = 0; ix < 3; ix++) {
            int slot = (this.design.model() == KvmModel.KVM_931T ? 4 : 5) + ix;
            int x = Math.round((365 + 32 * ix) * 0.44F);
            this.hits.add(new Kvm906Screen.Hit(x - 6, 156, 12, 8, () -> this.setting("button", Integer.toString(slot))));
        }

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 3; col++) {
                String key = "123456789*0#".substring(row * 3 + col, row * 3 + col + 1);
                int x = Math.round((365 + 32 * col) * 0.44F);
                int y = 24 + Math.round((340 + 30 * row) * 0.44F);
                this.hits.add(new Kvm906Screen.Hit(x - 5, y - 5, 11, 11, () -> this.setting("dial_key", key)));
            }
        }

        LogoRenderer.gui(g, this.font, this.data, Kvm931Renderer.texture("logo"), 79, 134, 25, 6);
        this.hits.add(new Kvm906Screen.Hit(21, 46, 47, 180, () -> this.send(ReceiverBlockEntity.Action.PICKUP, "")));
        this.label(g, tr("handset_hint"), 3, 251, -3814448);
    }

    private void deviceBusFamily(GuiGraphics g) {
        boolean room = this.design.model().roomOnly();
        this.image(
            g,
            Kvm931Renderer.modelTexture(this.design.model().id, this.data.flag("offhook") ? "gui_open" : "gui"),
            0,
            24,
            220,
            220,
            Integer.parseInt(this.design.get("body_color"), 16),
            500,
            500
        );
        if (this.data.flag("screen")) {
            boolean visible = false;

            try {
                String raw = this.data.setting("camera", "");
                if (!raw.isEmpty()) {
                    visible = this.cameraView
                        .draw(
                            g,
                            BlockPos.of(Long.parseLong(raw)),
                            86,
                            64,
                            70,
                            50,
                            this.design.number("brightness"),
                            this.design.number("contrast"),
                            this.design.model().videoFormat()
                        );
                }
            } catch (NumberFormatException var9) {
            }

            if (!visible) {
                g.fill(86, 64, 156, 114, -8946054);
                this.hardwareLabel(g, tr("no_signal"), 91, 83, 60);
            }

            for (int y = 65; y < 114; y += 3) {
                g.fill(86, y, 156, y + 1, 419430400);
            }
        }

        boolean phase = this.minecraft.level != null && this.minecraft.level.getGameTime() % 20L < 10L;
        if (this.data.flag("power")) {
            this.image(g, Kvm931Renderer.texture("led_power"), 178, 124, 10, 3, 16722256, 64, 16);
            if (room) {
                this.image(g, Kvm931Renderer.texture("led_round"), 177, 138, 11, 11, 16722256, 32, 32);
            }
        }

        if (!room && this.data.flag("emergency")) {
            this.image(g, Kvm931Renderer.texture("led_round"), 177, 138, 11, 11, 16722256, 32, 32);
        }

        String[] fs = room ? new String[]{"guard", "lobby", "monitor", "door"} : new String[]{"away", "guard", "monitor", "door"};

        for (int i = 0; i < 4; i++) {
            String mode = this.data.setting("led_" + fs[i], "off");
            int y = 24 + Math.round((307 + 37 * i) * 0.44F);
            if (mode.equals("on") || mode.equals("blink") && phase) {
                this.image(g, Kvm931Renderer.texture("led_round"), 174, y - 1, 2, 2, 3595346, 32, 32);
            }
        }

        int[] ys = room ? new int[]{307, 344, 381, 418} : new int[]{270, 307, 344, 381, 418};

        for (int ix = 0; ix < ys.length; ix++) {
            int slot = ix;
            int i = ix;
            int y = 24 + Math.round(ys[ix] * 0.44F);
            this.hits.add(new Kvm906Screen.Hit(176, y - 6, 14, 13, () -> this.setting("button", Integer.toString(slot))));
        }

        LogoRenderer.gui(g, this.font, this.data, Kvm931Renderer.texture("logo"), 79, 133, 25, 6);
        this.hits.add(new Kvm906Screen.Hit(21, 46, 47, 180, () -> this.send(ReceiverBlockEntity.Action.PICKUP, "")));
        this.label(g, tr("handset_hint"), 3, 251, -3814448);
    }

    private void device(GuiGraphics g) {
        if (this.design.model().manual932() || this.design.model().roomOnly()) {
            this.deviceBusFamily(g);
        } else if (this.design.model().is932()) {
            this.device932(g);
        } else if (this.design.model().is931()) {
            this.device931(g);
        } else {
            KvmModel model = this.design.model();
            boolean apartment = model.indicators;
            int color = Integer.parseInt(this.design.get("body_color"), 16);
            this.image(g, HomeNet.id("textures/gui/kvm_body.png"), 0, 24, 220, 220, color, 880, 880);
            int cover = 0xFF000000 | Integer.parseInt(this.design.get("cover_color"), 16);
            rounded(g, 73, apartment ? 33 : 36, 139, apartment ? 87 : 78, 5, cover);
            int sy = apartment ? 49 : 52;
            int sh = apartment ? 55 : 47;
            rounded(g, 79, sy - 6, 89, sh + 13, 3, -10130576);
            rounded(g, 82, sy - 3, 82, sh + 6, 2, -14341071);
            g.fill(85, sy, 161, sy + sh, -5854297);
            if (this.data.flag("screen")) {
                boolean visible = false;

                try {
                    String raw = this.data.setting("camera", "");
                    if (!raw.isEmpty()) {
                        visible = this.cameraView
                            .draw(
                                g,
                                BlockPos.of(Long.parseLong(raw)),
                                85,
                                sy,
                                76,
                                sh,
                                this.design.number("brightness"),
                                this.design.number("contrast"),
                                this.design.model().videoFormat()
                            );
                    }
                } catch (NumberFormatException var15) {
                }

                if (!visible) {
                    g.fill(85, sy, 161, sy + sh, -8946054);
                    this.label(g, tr("no_signal"), 87, sy + 18, -2039584);
                }

                for (int y = sy + 1; y < sy + sh; y += 3) {
                    g.fill(85, y, 161, y + 1, 385875968);
                }
            } else {
                for (int y = sy; y < sy + sh; y++) {
                    int c = 154 + (y - sy) / 2;
                    g.fill(85, y, 161, y + 1, 0xFF000000 | c << 16 | c << 8 | c);
                }
            }

            String style = this.design.get("cover");
            ResourceLocation decor = style.equals("custom")
                ? Kvm906Renderer.customTexture(this.design.get("cover_texture"), "custom_cover")
                : Kvm906Renderer.texture("cover_" + style + "_" + model.id);
            this.image(g, decor, 171, apartment ? 39 : 43, 38, apartment ? 75 : 66, 16777215, 152, 264);
            boolean flash = this.minecraft.level != null && this.minecraft.level.getGameTime() % 10L < 5L;
            if (apartment) {
                for (String function : model.keypad()
                    ? List.of("power", "telephone", "monitor", "guard", "door")
                    : List.of("emergency", "away", "monitor", "guard", "door")) {
                    String state = this.data.setting("led_" + function, "off");
                    if (state.equals("on") || state.equals("blink") && flash) {
                        this.image(g, Kvm906Renderer.texture("led_" + function + "_" + model.id), 171, 39, 38, 75, 16777215, 152, 264);
                    }
                }
            }

            if (this.data.flag("product_logo_configured")) {
                LogoRenderer.gui(g, this.font, this.data, Kvm906Renderer.texture("logo"), 78, apartment ? 125 : 120, 46, 11);
            } else {
                String var21 = this.design.get("logo");
                switch (var21) {
                    case "default":
                        this.image(
                            g,
                            Kvm906Renderer.texture(model.id.equals("906") ? "logo" : "logo_" + model.id),
                            78,
                            apartment ? 125 : 120,
                            model.id.equals("906") ? 33 : 52,
                            model.id.equals("906") ? 8 : 16,
                            16777215,
                            320,
                            100
                        );
                        break;
                    case "custom":
                        this.image(
                            g,
                            Kvm906Renderer.customTexture(this.design.get("logo_texture"), "custom_logo"),
                            78,
                            apartment ? 125 : 120,
                            46,
                            11,
                            16777215,
                            256,
                            64
                        );
                        break;
                    case "text":
                        this.hardwareLabel(g, this.design.get("logo_text"), 78, apartment ? 125 : 120, 60);
                }
            }

            if (!this.data.flag("offhook")) {
                this.image(g, HomeNet.id("textures/gui/kvm_handset.png"), 7, 30, 51, 204, color, 204, 816);
            }

            this.hits.add(new Kvm906Screen.Hit(5, 29, 56, 207, () -> this.send(ReceiverBlockEntity.Action.PICKUP, "")));
            if (!model.keypad()) {
                int lampX = (int)Math.round((11.52 - this.design.lampX()) / 0.032);
                int lampY = (int)Math.round((11.52 - this.design.lampY()) / 0.032) + 24;
                rounded(g, lampX - (apartment ? 6 : 10), lampY - 5, apartment ? 12 : 20, 10, 5, -14211028);
                boolean alarmPower = model == KvmModel.KVM_933 && this.data.flag("emergency");
                if (this.data.flag("power") && (!alarmPower || flash)) {
                    if (apartment) {
                        this.image(g, Kvm906Renderer.texture("power_led"), lampX - 4, lampY - 4, 8, 8, 16777215, 32, 32);
                    } else {
                        g.fill(lampX - 3, lampY, lampX + 3, lampY + 1, -2806706);
                    }
                }

                this.hardwareLabel(g, model == KvmModel.KVM_933 ? "비상/전원" : "전 원", lampX - 46, lampY - 3, 36);
                if (model == KvmModel.KVM_933) {
                    this.hits.add(new Kvm906Screen.Hit(lampX - 7, lampY - 6, 14, 12, () -> this.send(ReceiverBlockEntity.Action.EMERGENCY, "")));
                }
            }

            for (int slot = 0; slot < model.keys.size(); slot++) {
                int i = slot;
                int x = (int)Math.round((11.52 - this.design.buttonX(slot)) / 0.032);
                int y = (int)Math.round((11.52 - this.design.buttonY(slot)) / 0.032) + 24;
                if (!model.keypad()) {
                    rounded(g, x - 13, y - 6, 26, 12, 4, -5264998);
                    rounded(g, x - 11, y - 5, 22, 10, 3, 0xFF000000 | color);
                }

                if (model.keypad()) {
                    g.blit(Kvm906Renderer.texture("key_" + model.id + "_" + slot), x - 14, y - 6, 0.0F, 0.0F, 28, 12, 28, 12);
                } else {
                    this.hardwareLabel(g, model.keyLabel(model.keys.get(slot)), x - 42, y - 3, 30);
                }

                this.hits.add(new Kvm906Screen.Hit(x - 14, y - 6, 28, 13, () -> this.setting("button", Integer.toString(i))));
            }

            if (model.keypad()) {
                for (int index = 0; index < 12; index++) {
                    String digit = "123456789*0#".substring(index, index + 1);
                    int xx = 149 + index % 3 * 20;
                    int yx = 168 + index / 3 * 18;
                    rounded(g, xx - 8, yx - 8, 16, 16, 8, -5590862);
                    rounded(g, xx - 7, yx - 7, 14, 14, 7, 0xFF000000 | color);
                    this.label(g, digit, xx - 3, yx - 3, -13287356);
                    this.hits.add(new Kvm906Screen.Hit(xx - 8, yx - 8, 16, 13, () -> this.setting("dial_key", digit)));
                }
            }

            this.label(g, tr("handset_hint"), 3, 251, -3814448);
        }
    }

    private void controls(GuiGraphics g) {
        this.label(g, this.design.get("model_name"), 240, 28, -1118482);
        this.label(
            g,
            !this.design.model().guard()
                ? "문열림 전용 · 경비 기능 없음"
                : (this.data.flag("unit_edit") ? tr("unit") : (this.data.flag("room_only") ? "호 번호: 경비실기 설정" : "동·호수: 경비실기 설정")),
            240,
            50,
            -3419947
        );
        if (this.data.flag("unit_edit")) {
            this.apply(g, "unit", 62);
        }

        if (this.design.model().keypad()) {
            this.telephoneControls(g);
        } else {
            String state = CallState.byId(this.data.callState()).name().toLowerCase(Locale.ROOT);
            String callSource = this.data.setting("call_source", "");

            String role = switch (callSource) {
                case "guard" -> "경비실";
                case "lobby" -> "로비";
                case "camera" -> "현관";
                case "unit" -> "세대";
                default -> "통화";
            };
            this.label(g, CallState.byId(this.data.callState()) == CallState.CONNECTED ? role + " 통화 중" : tr("state_" + state), 240, 89, -4728640);
            this.label(g, tr(this.data.flag("offhook") ? "offhook" : "onhook"), 240, 103, -3419947);
            this.button(
                g, tr(this.data.flag("power") ? "power_off" : "power_on"), 240, 120, 196, 18, () -> this.setting("power", this.data.flag("power") ? "0" : "1")
            );
            String[] adjust = new String[]{"volume", "brightness", "contrast"};

            for (int i = 0; i < adjust.length; i++) {
                String key = adjust[i];
                int y = 145 + i * 17;
                this.label(g, tr(key) + "  " + this.design.get(key), 240, y + 3, -3419947);
                this.button(g, "−", 365, y, 28, 15, () -> this.setting(key, Integer.toString(Math.max(0, this.design.number(key) - 1))));
                this.button(g, "+", 402, y, 28, 15, () -> this.setting(key, Integer.toString(Math.min(4, this.design.number(key) + 1))));
                this.hits.add(new Kvm906Screen.Hit(94 + i * 23, 241, 13, 5, () -> this.setting(key, Integer.toString((this.design.number(key) + 1) % 5))));
            }

            this.label(g, tr("message"), 240, 194, -3419947);
            this.apply(g, "message", 205);
            if (this.design.model().keys.contains("door")) {
                if (this.data.flag("can_open_door")) {
                    this.button(
                        g,
                        "연결된 문 열기 (" + this.data.setting("linked_doors", "0") + ")",
                        240,
                        230,
                        196,
                        18,
                        () -> this.send(ReceiverBlockEntity.Action.OPEN_DOOR, "")
                    );
                } else {
                    this.label(g, this.design.model() == KvmModel.KVM_904 ? "문열림: 도어카메라 통화 중에만" : "문열림: 로비폰 통화 중에만", 240, 234, -3419947);
                }
            }

            this.label(g, this.data.setting("guard_system", "NONE") + " · 경비실기: " + this.data.setting("board", "미연결"), 240, 252, -4085898);
        }
    }

    private void telephoneControls(GuiGraphics g) {
        this.label(g, "게임 내 국선 번호", 240, 88, -3419947);
        this.apply(g, "phone_number", 100);
        String role = this.data.setting("phone_state", "IDLE");

        String status = switch (role) {
            case "RINGING" -> "국선 착신";
            case "DIALING" -> "국선 발신 중";
            case "CONNECTED" -> this.data.flag("phone_active") ? "국선 통화 중" : "국선 보류";
            default -> "국선 대기";
        };
        String intercom = this.data.setting("call_source", "");

        role = switch (intercom) {
            case "guard" -> "경비실";
            case "lobby" -> "로비";
            case "camera" -> "현관";
            case "unit" -> "세대";
            default -> "현관/경비";
        };
        intercom = CallState.byId(this.data.callState()) == CallState.CONNECTED
            ? role + " 통화 중"
            : (CallState.byId(this.data.callState()) == CallState.RINGING ? role + " 호출" : role + " 대기");
        this.label(g, status + " · " + (this.data.flag("phone_active") ? "국선" : intercom), 240, 123, -4728640);
        this.label(g, "다이얼: " + this.data.setting("phone_buffer", ""), 240, 140, -3419947);
        this.button(g, "후크 / 국선 전환", 240, 157, 112, 18, () -> this.setting("hook", "1"));
        this.button(g, "발신 (#)", 358, 157, 78, 18, () -> this.send(ReceiverBlockEntity.Action.DIAL, this.data.setting("phone_buffer", "")));
        if (this.design.model().away()) {
            this.button(g, this.data.flag("power") ? "전원 끄기" : "전원 켜기", 240, 178, 62, 18, () -> this.setting("power", this.data.flag("power") ? "0" : "1"));
            this.button(g, this.data.flag("away") ? "외출 해제" : "외출", 374, 178, 62, 18, () -> this.send(ReceiverBlockEntity.Action.OUTING, ""));
        } else {
            this.button(g, this.data.flag("power") ? "전원 끄기" : "전원 켜기", 240, 178, 196, 18, () -> this.setting("power", this.data.flag("power") ? "0" : "1"));
        }

        this.apply(g, "message", 205);
        if (this.data.flag("can_open_door")) {
            this.button(g, "개폐 (" + this.data.setting("linked_doors", "0") + ")", 240, 230, 196, 18, () -> this.send(ReceiverBlockEntity.Action.OPEN_DOOR, ""));
        } else {
            this.label(g, this.design.model().lobby() ? "개폐: 로비폰 통화 중에만" : "개폐: 도어카메라 통화 중에만", 240, 234, -3419947);
        }

        this.label(g, this.data.setting("guard_system", "NONE") + " · 경비실기: " + this.data.setting("board", "미연결"), 240, 252, -4085898);
    }

    private void editor(GuiGraphics g) {
        for (int i = 0; i < 3; i++) {
            int p = i;
            this.button(g, tr(new String[]{"print", "shell", "keys"}[i]), 240 + i * 67, 26, 63, 18, () -> {
                this.page = p;
                this.rebuildWidgets();
            });
        }

        if (this.page == 0) {
            this.button(g, tr("logo") + ": " + tr(this.design.get("logo")), 240, 51, 196, 18, () -> this.cycle("logo", "default", "none", "text", "custom"));
            this.label(g, tr("logo_text"), 240, 78, -3419947);
            this.apply(g, "logo_text", 90);
            this.label(g, tr("logo_texture"), 240, 124, -3419947);
            this.apply(g, "logo_texture", 136);
            if (!this.design.model().indicators) {
                this.button(
                    g,
                    tr("cover") + ": " + tr(this.design.get("cover")),
                    240,
                    167,
                    196,
                    18,
                    () -> this.cycle("cover", "stripes", "house", "squares", "triangle", "plain", "custom")
                );
            } else {
                this.label(g, "화면 옆 비상·외출·현관·경비 표시기", 240, 172, -3419947);
            }

            this.label(g, tr("cover_texture"), 240, 190, -3419947);
            this.apply(g, "cover_texture", 202);
            this.label(g, tr("pack_hint"), 240, 231, -4085898);
        } else if (this.page == 1) {
            this.label(g, tr("body_color"), 240, 52, -3419947);
            this.apply(g, "body_color", 64);
            this.label(g, tr("cover_color"), 240, 98, -3419947);
            this.apply(g, "cover_color", 110);
            this.label(g, "현재 모델", 240, 156, -3419947);
            if (this.design.model().is932()) {
                this.button(g, "전화벨 음량: " + this.design.get("phone_bell"), 240, 199, 196, 18, () -> this.cycle("phone_bell", "0", "1", "2", "3", "4"));
            } else {
                this.label(g, tr("color_hint"), 240, 199, -4085898);
            }

            if (this.design.model().keypad()) {
                this.button(
                    g,
                    "국선 방식: " + (this.data.setting("phone_mode", "tone").equals("tone") ? "전자식" : "기계식"),
                    240,
                    226,
                    196,
                    18,
                    () -> this.setting("phone_mode", this.data.setting("phone_mode", "tone").equals("tone") ? "pulse" : "tone")
                );
            } else {
                this.label(g, tr(this.design.family() ? "custom_profile" : "tool_hint"), 240, 226, -4085898);
            }
        } else {
            this.label(g, "모델 변경 아이템으로 기기를 선택", 240, 57, -4085898);
            this.label(g, "목록을 스크롤하고 형제모델 클릭", 240, 72, -4085898);

            for (int i = 0; i < this.design.model().keys.size(); i++) {
                this.label(
                    g,
                    i + 1 + ". " + this.design.model().keyLabel(this.design.model().keys.get(i)),
                    240 + (this.design.model().is932() ? i / 4 * 98 : 0),
                    98 + (this.design.model().is932() ? i % 4 * 14 : i * 21),
                    -3419947
                );
            }

            if (this.design.model() == KvmModel.KVM_933) {
                this.label(g, "비상/전원 표시등 클릭: 비상 토글", 240, 190, -4085898);
            }

            if (this.design.model().keypad()) {
                for (int index = 0; index < 3; index++) {
                    String key = new String[]{"volume", "brightness", "contrast"}[index];
                    int y = 158 + index * 17;
                    this.label(g, tr(key) + " " + this.design.get(key), 240, y + 3, -3419947);
                    this.button(g, "−", 365, y, 28, 15, () -> this.setting(key, Integer.toString(Math.max(0, this.design.number(key) - 1))));
                    this.button(
                        g, "+", 402, y, 28, 15, () -> this.setting(key, Integer.toString(Math.min(key.equals("volume") ? 3 : 4, this.design.number(key) + 1)))
                    );
                }
            }

            this.label(g, this.data.flag("room_only") ? "호 번호" : tr("unit"), 240, 206, -3419947);
            if (this.data.flag("unit_edit")) {
                this.apply(g, "unit", 218);
            }

            this.label(g, this.data.setting("guard_system", "NONE") + " · 경비실기: " + this.data.setting("board", "미연결"), 240, 250, -4085898);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        this.hits.clear();
        g.pose().pushPose();
        g.pose().translate((float)this.left, (float)this.top, 0.0F);
        g.pose().scale(this.scale, this.scale, 1.0F);
        rounded(g, -4, -2, 448, 274, 6, -299882448);
        this.label(g, "KOCOM " + this.design.model().label(), 2, 4, -1118482);
        this.button(g, tr(this.editing ? "operate" : "edit"), 240, 2, 196, 18, () -> {
            this.editing = !this.editing;
            this.rebuildWidgets();
        });
        this.device(g);
        if (this.editing) {
            this.editor(g);
        } else {
            this.controls(g);
        }

        g.pose().popPose();
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (super.mouseClicked(mouseX, mouseY, mouseButton)) {
            return true;
        } else if (mouseButton != 0) {
            return false;
        } else {
            double x = (mouseX - this.left) / this.scale;
            double y = (mouseY - this.top) / this.scale;

            for (Kvm906Screen.Hit hit : this.hits) {
                if (x >= hit.x && x < hit.x + hit.w && y >= hit.y && y < hit.y + hit.h) {
                    hit.action.run();
                    return true;
                }
            }

            return false;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        this.fields.values().forEach(EditBox::tick);
        if (this.minecraft == null || this.minecraft.level == null || !(this.minecraft.level.getBlockEntity(this.pos) instanceof Kvm906BlockEntity)) {
            this.onClose();
        }
    }

    private record Hit(int x, int y, int w, int h, Runnable action) {
    }
}
