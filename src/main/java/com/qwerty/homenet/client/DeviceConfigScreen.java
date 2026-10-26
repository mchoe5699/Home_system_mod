package com.qwerty.homenet.client;

import com.qwerty.homenet.block.DeviceType;
import com.qwerty.homenet.network.DeviceConfigOpenPacket;
import com.qwerty.homenet.network.DeviceConfigPacket;
import com.qwerty.homenet.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class DeviceConfigScreen extends HomeNetScreen {
    private final BlockPos pos;
    private String name;
    private DeviceType type;
    private boolean on;
    private EditBox nameBox;

    public DeviceConfigScreen(DeviceConfigOpenPacket p) {
        super(Component.translatable("gui.qwertys_homenet.device_config"), 220, 140);
        this.pos = p.pos();
        this.name = p.name();
        this.type = DeviceType.byId(p.type());
        this.on = p.on();
    }

    @Override
    protected void init() {
        super.init();
        this.nameBox = new EditBox(this.font, this.left + 12, this.top + 40, this.panelW - 24, 18, tr("device_name", new Object[0]));
        this.nameBox.setMaxLength(20);
        this.nameBox.setValue(this.name);
        this.nameBox.setResponder(s -> this.name = s);
        this.nameBox.setHint(this.type.displayName());
        this.addRenderableWidget(this.nameBox);
        this.addRenderableWidget(Button.builder(tr("device_type", new Object[]{this.type.displayName()}), b -> {
            this.type = this.type.next();
            this.rebuildWidgets();
        }).bounds(this.left + 12, this.top + 66, this.panelW - 24, 20).build());
        this.addRenderableWidget(Button.builder(tr("test_toggle", new Object[]{this.type.stateName(this.on)}), b -> {
            ModNetwork.sendToServer(new DeviceConfigPacket(this.pos, this.name, this.type.ordinal(), true));
            this.on = !this.on;
            this.rebuildWidgets();
        }).bounds(this.left + 12, this.top + 92, 94, 20).build());
        this.addRenderableWidget(
            Button.builder(tr("save", new Object[0]), b -> this.save()).bounds(this.left + this.panelW - 106, this.top + 92, 94, 20).build()
        );
    }

    private void save() {
        ModNetwork.sendToServer(new DeviceConfigPacket(this.pos, this.name, this.type.ordinal(), false));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        this.drawDevice(g, this.title, null);
        g.drawString(this.font, tr("device_name", new Object[0]), this.left + 12, this.top + 28, -6641994, false);
        g.drawString(this.font, tr("device_link_hint", new Object[0]), this.left + 12, this.top + 120, -6641994, false);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key != 257 && key != 335) {
            return super.keyPressed(key, scan, mods);
        } else {
            this.save();
            return true;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.nameBox != null) {
            this.nameBox.tick();
        }
    }
}
