package com.qwerty.homenet.client.family;

import com.qwerty.homenet.client.ReceiverScreen;
import com.qwerty.homenet.network.ModNetwork;
import com.qwerty.homenet.network.ModelChangePacket;
import com.qwerty.homenet.network.WallpadDataPacket;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public abstract class FamilyModelScreen extends Screen implements ReceiverScreen {
    protected final BlockPos pos;
    private final String family;
    private final List<FamilyModelScreen.Choice> choices;
    private String current;
    private int left;
    private int top;
    private int panelW;
    private int panelH;
    private int visible;
    private int scroll;
    private static final int ROW = 44;
    private static final int LIST_TOP = 82;
    private EditBox search;
    private String query = "";
    private List<FamilyModelScreen.Choice> filtered = List.of();

    protected FamilyModelScreen(WallpadDataPacket data, String family, String current, List<FamilyModelScreen.Choice> choices) {
        super(Component.literal("형제모델 변경"));
        this.pos = data.pos();
        this.family = family;
        this.current = current;
        this.choices = List.copyOf(choices);
        this.filtered = this.choices;
    }

    @Override
    public final BlockPos getPos() {
        return this.pos;
    }

    protected final void current(String id) {
        this.current = id;
    }

    @Override
    protected final void init() {
        this.panelW = Math.max(140, Math.min(380, this.width - 16));
        this.panelH = Math.max(170, Math.min(360, this.height - 16));
        this.left = (this.width - this.panelW) / 2;
        this.top = (this.height - this.panelH) / 2;
        this.visible = Math.max(1, (this.panelH - 128) / 44);
        this.search = this.addRenderableWidget(new EditBox(this.font, this.left + 12, this.top + 48, this.panelW - 24, 20, Component.literal("모델명·기능 검색")));
        this.search.setMaxLength(80);
        this.search.setHint(Component.literal("모델명·기능 검색"));
        this.search.setValue(this.query);
        this.search.setResponder(value -> {
            this.query = value;
            this.filtered = this.choices.stream().filter(c -> FamilySearch.matches(value, c.id(), c.label(), c.description())).toList();
            this.scroll = 0;
            this.clampScroll();
        });
        this.addRenderableWidget(
            Button.builder(Component.literal("닫기"), button -> this.onClose()).bounds(this.left + this.panelW - 76, this.top + this.panelH - 26, 64, 20).build()
        );
        this.setInitialFocus(this.search);
        this.clampScroll();
    }

    private void clampScroll() {
        this.scroll = Math.max(0, Math.min(Math.max(0, this.filtered.size() - this.visible), this.scroll));
    }

    private String fit(String text) {
        return this.font.plainSubstrByWidth(text, this.panelW - 38);
    }

    @Override
    public final void render(GuiGraphics g, int x, int y, float partial) {
        this.renderBackground(g);
        g.fill(this.left, this.top, this.left + this.panelW, this.top + this.panelH, -299947981);
        g.drawString(this.font, this.fit("형제모델 변경 · " + this.family), this.left + 12, this.top + 10, -1, false);
        String label = this.choices.stream().filter(c -> c.id().equals(this.current)).map(FamilyModelScreen.Choice::label).findFirst().orElse(this.current);
        g.drawString(this.font, this.fit("현재 모델: " + label), this.left + 12, this.top + 29, -5649979, false);
        int rows = Math.min(this.visible, this.filtered.size() - this.scroll);

        for (int i = 0; i < rows; i++) {
            FamilyModelScreen.Choice c = this.filtered.get(this.scroll + i);
            int rowY = this.top + 82 + i * 44;
            boolean selected = c.id().equals(this.current);
            boolean hover = x >= this.left + 8 && x < this.left + this.panelW - 12 && y >= rowY && y < rowY + 40;
            g.fill(this.left + 8, rowY, this.left + this.panelW - 12, rowY + 40, selected ? -13081242 : (hover ? -12167578 : -13024175));
            g.drawString(this.font, this.fit(c.label()), this.left + 18, rowY + 6, -1, false);
            g.drawString(this.font, this.fit(c.description()), this.left + 18, rowY + 23, -3419431, false);
        }

        if (this.filtered.isEmpty()) {
            g.drawString(this.font, "검색 결과 없음", this.left + 18, this.top + 82 + 12, -3419431, false);
        }

        if (this.filtered.size() > this.visible) {
            int track = this.visible * 44;
            int thumb = Math.max(12, track * this.visible / this.filtered.size());
            int offset = this.scroll * (track - thumb) / (this.filtered.size() - this.visible);
            g.fill(this.left + this.panelW - 7, this.top + 82, this.left + this.panelW - 4, this.top + 82 + track, -15130583);
            g.fill(this.left + this.panelW - 7, this.top + 82 + offset, this.left + this.panelW - 4, this.top + 82 + offset + thumb, -8276311);
        }

        g.drawString(
            this.font, this.font.plainSubstrByWidth("검색 · 휠 이동 · 모델 클릭", this.panelW - 104), this.left + 12, this.top + this.panelH - 18, -3095918, false
        );
        super.render(g, x, y, partial);
    }

    @Override
    public final boolean mouseScrolled(double x, double y, double delta) {
        if (!(x < this.left) && !(x >= this.left + this.panelW) && !(y < this.top) && !(y >= this.top + this.panelH)) {
            this.scroll = this.scroll - (int)Math.signum(delta);
            this.clampScroll();
            return true;
        } else {
            return super.mouseScrolled(x, y, delta);
        }
    }

    @Override
    public final boolean mouseClicked(double x, double y, int button) {
        if (button == 0 && x >= this.left + 8 && x < this.left + this.panelW - 12 && y >= this.top + 82 && y < this.top + 82 + this.visible * 44) {
            int offset = (int)(y - this.top - 82.0);
            int index = this.scroll + offset / 44;
            if (offset % 44 < 40 && index < this.filtered.size()) {
                ModNetwork.sendToServer(new ModelChangePacket(this.pos, this.filtered.get(index).id()));
                return true;
            }
        }

        return super.mouseClicked(x, y, button);
    }

    @Override
    public final boolean isPauseScreen() {
        return false;
    }

    public record Choice(String id, String label, String description) {
    }
}
