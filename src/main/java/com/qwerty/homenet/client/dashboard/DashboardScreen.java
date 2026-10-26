package com.qwerty.homenet.client.dashboard;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.NativeImage.Format;
import com.qwerty.homenet.data.ComplexOutlines;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.data.ZoneData;
import com.qwerty.homenet.network.DashboardDataPacket;
import com.qwerty.homenet.network.ModNetwork;
import com.qwerty.homenet.network.ZoneEditPacket;
import com.qwerty.homenet.network.ZoneGroupPacket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.MapColor.Brightness;

public class DashboardScreen extends Screen {
    private static final int PANEL_W = 150;
    private static final int ROW_H = 22;
    private static final int[] PALETTE = new int[]{15022389, 16485376, 16635957, 4431943, 44225, 2001125, 6174129, 14162784, 7162945, 5533306, 35195, 8172354};
    private List<ZoneData.Zone> zones;
    private List<ZoneData.Zone> outlineSnapshot;
    private int outlineEdit = -2;
    private final Map<Integer, Integer> zoneColors = new HashMap<>();
    private final Map<Integer, List<ComplexOutlines.Segment>> zoneBorders = new HashMap<>();
    private List<DeviceRegistry.Entry> devices;
    private double centerX;
    private double centerZ;
    private float zoom = 2.0F;
    private DashboardScreen.Mode mode = DashboardScreen.Mode.VIEW;
    private int listScroll;
    private final Set<Integer> groupSelection = new LinkedHashSet<>();
    private String groupName = "";
    private boolean drawing;
    private boolean panning;
    private int dragX1;
    private int dragZ1;
    private int dragX2;
    private int dragZ2;
    private int editId = -1;
    private String editName = "";
    private int editColor = PALETTE[0];
    private int ex1;
    private int ez1;
    private int ex2;
    private int ez2;
    private EditBox nameBox;
    private int deleteId;
    private DynamicTexture tex;
    private ResourceLocation texLoc;
    private int texW;
    private int texH;
    private boolean dirty = true;
    private int refreshTimer;
    private final Map<Long, int[]> chunkCache = new HashMap<>();

    public DashboardScreen(DashboardDataPacket data) {
        super(Component.translatable("item.qwertys_homenet.dashboard"));
        this.zones = data.zones();
        this.devices = data.devices();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            this.centerX = player.getX();
            this.centerZ = player.getZ();
        }
    }

    public void update(DashboardDataPacket data) {
        this.zones = data.zones();
        this.devices = data.devices();
        this.groupSelection.removeIf(id -> this.zones.stream().noneMatch(z -> z.id() == id));
        if (this.mode == DashboardScreen.Mode.VIEW || this.mode == DashboardScreen.Mode.GROUP) {
            this.rebuildWidgets();
        }
    }

    private static MutableComponent tr(String key, Object... args) {
        return Component.translatable("gui.qwertys_homenet.dashboard." + key, args);
    }

    @Override
    protected void init() {
        this.nameBox = null;
        int x = 8;
        int w = 134;
        switch (this.mode) {
            case VIEW:
                this.addRenderableWidget(Button.builder(tr("add_zone"), b -> {
                    this.editId = -1;
                    this.setMode(DashboardScreen.Mode.DRAW);
                }).bounds(x, 24, w, 20).build());
                this.addRenderableWidget(Button.builder(Component.literal("구획 묶기 / 분리"), button -> {
                    this.groupSelection.clear();
                    this.groupName = "";
                    this.listScroll = 0;
                    this.setMode(DashboardScreen.Mode.GROUP);
                }).bounds(x, 48, w, 20).build());
                int y0 = 76;
                int rows = Math.max(1, (this.height - y0 - 56) / 22);
                this.listScroll = Mth.clamp(this.listScroll, 0, Math.max(0, this.zones.size() - rows));

                for (int i = 0; i < rows && i + this.listScroll < this.zones.size(); i++) {
                    ZoneData.Zone z = this.zones.get(i + this.listScroll);
                    int y = y0 + i * 22;
                    int count = this.devicesIn(z);
                    this.addRenderableWidget(
                        Button.builder(
                                Component.literal(
                                    this.font.plainSubstrByWidth(z.name() + (z.complex().isEmpty() ? "" : " / " + z.complex()) + " (" + count + ")", w - 64)
                                ),
                                b -> this.focus(z)
                            )
                            .bounds(x + 8, y, w - 56, 20)
                            .build()
                    );
                    this.addRenderableWidget(Button.builder(Component.literal("✎"), b -> this.startEdit(z)).bounds(x + w - 46, y, 22, 20).build());
                    this.addRenderableWidget(Button.builder(Component.literal("✕"), b -> {
                        this.deleteId = z.id();
                        this.setMode(DashboardScreen.Mode.CONFIRM_DELETE);
                    }).bounds(x + w - 22, y, 22, 20).build());
                }
                break;
            case GROUP:
                this.nameBox = this.addRenderableWidget(new EditBox(this.font, x, 48, w, 18, Component.literal("단지 이름")));
                this.nameBox.setMaxLength(32);
                this.nameBox.setValue(this.groupName);
                this.nameBox.setHint(Component.literal("같은 단지 이름 입력"));
                this.nameBox.setResponder(value -> this.groupName = value);
                int groupRows = Math.max(1, (this.height - 196) / 22);
                this.listScroll = Mth.clamp(this.listScroll, 0, Math.max(0, this.zones.size() - groupRows));

                for (int i = 0; i < groupRows && i + this.listScroll < this.zones.size(); i++) {
                    ZoneData.Zone z = this.zones.get(i + this.listScroll);
                    String text = (this.groupSelection.contains(z.id()) ? "[✓] " : "[ ] ") + z.name() + (z.complex().isEmpty() ? "" : " / " + z.complex());
                    this.addRenderableWidget(Button.builder(Component.literal(this.font.plainSubstrByWidth(text, w - 8)), button -> {
                        if (!this.groupSelection.remove(z.id())) {
                            this.groupSelection.add(z.id());
                        }

                        this.rebuildWidgets();
                    }).bounds(x, 90 + i * 22, w, 20).build());
                }

                this.addRenderableWidget(Button.builder(Component.literal("선택 구획 묶기"), button -> {
                    if (!this.groupSelection.isEmpty() && !this.groupName.isBlank()) {
                        ModNetwork.sendToServer(new ZoneGroupPacket(this.groupName, this.groupSelection.stream().mapToInt(Integer::intValue).toArray(), false));
                        this.setMode(DashboardScreen.Mode.VIEW);
                    }
                }).bounds(x, this.height - 78, w, 20).build());
                this.addRenderableWidget(Button.builder(Component.literal("선택 구획 분리"), button -> {
                    if (!this.groupSelection.isEmpty()) {
                        ModNetwork.sendToServer(new ZoneGroupPacket("", this.groupSelection.stream().mapToInt(Integer::intValue).toArray(), true));
                        this.setMode(DashboardScreen.Mode.VIEW);
                    }
                }).bounds(x, this.height - 54, w, 20).build());
                this.addRenderableWidget(
                    Button.builder(tr("cancel"), button -> this.setMode(DashboardScreen.Mode.VIEW)).bounds(x, this.height - 30, w, 20).build()
                );
                break;
            case DRAW:
                this.addRenderableWidget(
                    Button.builder(tr("cancel"), b -> this.setMode(this.editId < 0 ? DashboardScreen.Mode.VIEW : DashboardScreen.Mode.EDIT))
                        .bounds(x, 24, w, 20)
                        .build()
                );
                break;
            case EDIT:
                this.nameBox = new EditBox(this.font, x, 62, w, 18, tr("name"));
                this.nameBox.setMaxLength(32);
                this.nameBox.setValue(this.editName);
                this.nameBox.setResponder(s -> this.editName = s);
                this.addRenderableWidget(this.nameBox);
                this.addRenderableWidget(Button.builder(tr("color"), b -> {
                    int idx = 0;

                    for (int ix = 0; ix < PALETTE.length; ix++) {
                        if (PALETTE[ix] == this.editColor) {
                            idx = ix;
                        }
                    }

                    this.editColor = PALETTE[(idx + 1) % PALETTE.length];
                }).bounds(x + 20, 100, w - 20, 20).build());
                this.addRenderableWidget(Button.builder(tr("redraw"), b -> this.setMode(DashboardScreen.Mode.DRAW)).bounds(x, 142, w, 20).build());
                this.addRenderableWidget(Button.builder(tr("save"), b -> this.save()).bounds(x, 168, w, 20).build());
                this.addRenderableWidget(Button.builder(tr("cancel"), b -> this.setMode(DashboardScreen.Mode.VIEW)).bounds(x, 192, w, 20).build());
                this.setFocused(this.nameBox);
                break;
            case CONFIRM_DELETE:
                this.addRenderableWidget(Button.builder(tr("delete"), b -> {
                    ModNetwork.sendToServer(new ZoneEditPacket(2, this.deleteId, "", 0, 0, 0, 0, 0));
                    this.setMode(DashboardScreen.Mode.VIEW);
                }).bounds(x, 90, w, 20).build());
                this.addRenderableWidget(Button.builder(tr("cancel"), b -> this.setMode(DashboardScreen.Mode.VIEW)).bounds(x, 114, w, 20).build());
        }

        if (this.texW != this.width - 150 || this.texH != this.height) {
            this.dirty = true;
        }
    }

    private void setMode(DashboardScreen.Mode m) {
        this.mode = m;
        this.drawing = false;
        this.rebuildWidgets();
    }

    private int devicesIn(ZoneData.Zone z) {
        int n = 0;

        for (DeviceRegistry.Entry e : this.devices) {
            if (z.contains(e.pos().getX(), e.pos().getZ()) && this.zoneAt(e.pos()) == z.id()) {
                n++;
            }
        }

        return n;
    }

    private int zoneAt(BlockPos p) {
        ZoneData.Zone best = null;

        for (ZoneData.Zone z : this.zones) {
            if (z.contains(p.getX(), p.getZ()) && (best == null || z.area() < best.area())) {
                best = z;
            }
        }

        return best == null ? 0 : best.id();
    }

    private void focus(ZoneData.Zone z) {
        this.centerX = (z.x1() + z.x2() + 1) / 2.0;
        this.centerZ = (z.z1() + z.z2() + 1) / 2.0;
        int mapW = this.width - 150;
        float fit = Math.min((float)mapW / (z.x2() - z.x1() + 8), (float)this.height / (z.z2() - z.z1() + 8));
        this.zoom = Mth.clamp(fit, 0.25F, 16.0F);
        this.dirty = true;
    }

    private void startEdit(ZoneData.Zone z) {
        this.editId = z.id();
        this.editName = z.name();
        this.editColor = z.color();
        this.ex1 = z.x1();
        this.ez1 = z.z1();
        this.ex2 = z.x2();
        this.ez2 = z.z2();
        this.focus(z);
        this.setMode(DashboardScreen.Mode.EDIT);
    }

    private void save() {
        String name = this.editName.trim().isEmpty() ? tr("default_name", this.zones.size() + 1).getString() : this.editName.trim();
        int action = this.editId < 0 ? 0 : 1;
        ModNetwork.sendToServer(new ZoneEditPacket(action, Math.max(0, this.editId), name, this.editColor, this.ex1, this.ez1, this.ex2, this.ez2));
        this.setMode(DashboardScreen.Mode.VIEW);
    }

    private int mapX0() {
        return 150;
    }

    private int mapW() {
        return this.width - 150;
    }

    private double worldX(double sx) {
        return this.centerX + (sx - this.mapX0() - this.mapW() / 2.0) / this.zoom;
    }

    private double worldZ(double sy) {
        return this.centerZ + (sy - this.height / 2.0) / this.zoom;
    }

    private float screenX(double wx) {
        return (float)(this.mapX0() + this.mapW() / 2.0 + (wx - this.centerX) * this.zoom);
    }

    private float screenY(double wz) {
        return (float)(this.height / 2.0 + (wz - this.centerZ) * this.zoom);
    }

    private boolean overMap(double mx) {
        return mx >= 150.0;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) {
            return true;
        } else if (!this.overMap(mx)) {
            return false;
        } else if (button == 0 && this.mode == DashboardScreen.Mode.DRAW) {
            this.drawing = true;
            this.dragX1 = this.dragX2 = Mth.floor(this.worldX(mx));
            this.dragZ1 = this.dragZ2 = Mth.floor(this.worldZ(my));
            return true;
        } else if (button != 0 && button != 1) {
            return false;
        } else {
            this.panning = true;
            return true;
        }
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (this.drawing) {
            this.dragX2 = Mth.floor(this.worldX(mx));
            this.dragZ2 = Mth.floor(this.worldZ(my));
            return true;
        } else if (this.panning) {
            this.centerX = this.centerX - dx / this.zoom;
            this.centerZ = this.centerZ - dy / this.zoom;
            this.dirty = true;
            return true;
        } else {
            return super.mouseDragged(mx, my, button, dx, dy);
        }
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (this.drawing) {
            this.drawing = false;
            this.ex1 = Math.min(this.dragX1, this.dragX2);
            this.ex2 = Math.max(this.dragX1, this.dragX2);
            this.ez1 = Math.min(this.dragZ1, this.dragZ2);
            this.ez2 = Math.max(this.dragZ1, this.dragZ2);
            if (this.editId < 0) {
                this.editName = "";
                this.editColor = PALETTE[this.zones.size() % PALETTE.length];
            }

            this.setMode(DashboardScreen.Mode.EDIT);
            return true;
        } else {
            this.panning = false;
            return super.mouseReleased(mx, my, button);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (this.overMap(mx)) {
            double wx = this.worldX(mx);
            double wz = this.worldZ(my);
            this.zoom = Mth.clamp(this.zoom * (delta > 0.0 ? 1.25F : 0.8F), 0.25F, 16.0F);
            this.centerX = wx - (mx - this.mapX0() - this.mapW() / 2.0) / this.zoom;
            this.centerZ = wz - (my - this.height / 2.0) / this.zoom;
            this.dirty = true;
            return true;
        } else if (this.mode != DashboardScreen.Mode.VIEW && this.mode != DashboardScreen.Mode.GROUP) {
            return false;
        } else {
            this.listScroll = Math.max(0, this.listScroll - (int)Math.signum(delta));
            this.rebuildWidgets();
            return true;
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256 && this.mode != DashboardScreen.Mode.VIEW) {
            this.setMode(DashboardScreen.Mode.VIEW);
            return true;
        } else if (this.mode != DashboardScreen.Mode.EDIT || key != 257 && key != 335) {
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

        if (++this.refreshTimer >= 60) {
            this.refreshTimer = 0;
            this.chunkCache.clear();
            this.dirty = true;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        super.removed();
        if (this.tex != null) {
            Minecraft.getInstance().getTextureManager().release(this.texLoc);
            this.tex = null;
        }
    }

    private void rebuildMap() {
        int w = Math.max(1, this.mapW());
        int h = Math.max(1, this.height);
        if (this.tex == null || this.texW != w || this.texH != h) {
            if (this.tex != null) {
                Minecraft.getInstance().getTextureManager().release(this.texLoc);
            }

            this.tex = new DynamicTexture(new NativeImage(Format.RGBA, w, h, false));
            this.texLoc = Minecraft.getInstance().getTextureManager().register("qwertys_homenet_dashboard", this.tex);
            this.texW = w;
            this.texH = h;
        }

        NativeImage img = this.tex.getPixels();
        if (img != null) {
            for (int i = 0; i < w; i++) {
                int wx = Mth.floor(this.centerX + (i + 0.5 - w / 2.0) / this.zoom);

                for (int j = 0; j < h; j++) {
                    int wz = Mth.floor(this.centerZ + (j + 0.5 - h / 2.0) / this.zoom);
                    img.setPixelRGBA(i, j, this.colorAt(wx, wz));
                }
            }

            this.tex.upload();
            this.dirty = false;
        }
    }

    private int colorAt(int x, int z) {
        int cx = x >> 4;
        int cz = z >> 4;
        long key = ChunkPos.asLong(cx, cz);
        int[] colors = this.chunkCache.get(key);
        if (colors == null) {
            colors = this.computeChunk(cx, cz);
            if (colors == null) {
                return ((x ^ z) & 8) == 0 ? -14935012 : -14540254;
            }

            this.chunkCache.put(key, colors);
        }

        return colors[(z & 15) * 16 + (x & 15)];
    }

    private int[] computeChunk(int cx, int cz) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && level.hasChunk(cx, cz)) {
            LevelChunk chunk = level.getChunk(cx, cz);
            int[] out = new int[256];
            int[] heights = new int[256];
            MutableBlockPos mp = new MutableBlockPos();

            for (int lz = 0; lz < 16; lz++) {
                for (int lx = 0; lx < 16; lx++) {
                    int y = chunk.getHeight(Types.WORLD_SURFACE, lx, lz);
                    int x = (cx << 4) + lx;
                    int z = (cz << 4) + lz;
                    MapColor color = MapColor.NONE;

                    for (int tries = 0; y > level.getMinBuildHeight() && tries++ < 12; y--) {
                        mp.set(x, y, z);
                        BlockState s = chunk.getBlockState(mp);
                        color = s.getMapColor(level, mp);
                        if (color != MapColor.NONE) {
                            break;
                        }
                    }

                    heights[lz * 16 + lx] = y;
                    Brightness b = Brightness.NORMAL;
                    if (lz > 0) {
                        int north = heights[(lz - 1) * 16 + lx];
                        if (y > north) {
                            b = Brightness.HIGH;
                        } else if (y < north) {
                            b = Brightness.LOW;
                        }
                    }

                    out[lz * 16 + lx] = color == MapColor.NONE ? -15724528 : color.calculateRGBColor(b);
                }
            }

            return out;
        } else {
            return null;
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, -15987182);
        if (this.dirty || this.tex == null) {
            this.rebuildMap();
        }

        if (this.texLoc != null) {
            g.blit(this.texLoc, this.mapX0(), 0, 0.0F, 0.0F, this.texW, this.texH, this.texW, this.texH);
        }

        g.enableScissor(this.mapX0(), 0, this.width, this.height);
        this.ensureZoneBorders();

        for (ZoneData.Zone z : this.zones) {
            if (this.mode != DashboardScreen.Mode.EDIT || z.id() != this.editId) {
                this.drawZoneRect(
                    g, z.x1(), z.z1(), z.x2(), z.z2(), this.zoneColor(z), z.name() + (z.complex().isEmpty() ? "" : " [" + z.complex() + "]"), false
                );
            }
        }

        for (ZoneData.Zone zx : this.zones) {
            if (this.mode != DashboardScreen.Mode.EDIT || zx.id() != this.editId) {
                this.drawZoneBorders(g, zx, this.zoneColor(zx));
            }
        }

        if (this.mode == DashboardScreen.Mode.EDIT) {
            this.drawZoneRect(g, this.ex1, this.ez1, this.ex2, this.ez2, this.editColor, this.editName.isEmpty() ? "?" : this.editName);
        }

        if (this.drawing) {
            this.drawZoneRect(
                g,
                Math.min(this.dragX1, this.dragX2),
                Math.min(this.dragZ1, this.dragZ2),
                Math.max(this.dragX1, this.dragX2),
                Math.max(this.dragZ1, this.dragZ2),
                16777215,
                Math.abs(this.dragX2 - this.dragX1) + 1 + " x " + (Math.abs(this.dragZ2 - this.dragZ1) + 1)
            );
        }

        DeviceRegistry.Entry hover = null;

        for (DeviceRegistry.Entry e : this.devices) {
            float sx = this.screenX(e.pos().getX() + 0.5);
            float sy = this.screenY(e.pos().getZ() + 0.5);
            int r = this.zoom >= 2.0F ? 3 : 2;
            g.fill(Math.round(sx) - r - 1, Math.round(sy) - r - 1, Math.round(sx) + r + 1, Math.round(sy) + r + 1, -16777216);
            g.fill(Math.round(sx) - r, Math.round(sy) - r, Math.round(sx) + r, Math.round(sy) + r, 0xFF000000 | kindColor(e.kind()));
            if (Math.abs(mouseX - sx) <= r + 2 && Math.abs(mouseY - sy) <= r + 2) {
                hover = e;
            }
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            int px = Math.round(this.screenX(player.getX()));
            int py = Math.round(this.screenY(player.getZ()));
            g.fill(px - 3, py - 3, px + 3, py + 3, -16777216);
            g.fill(px - 2, py - 2, px + 2, py + 2, -1);
        }

        g.disableScissor();
        if (this.mode == DashboardScreen.Mode.DRAW) {
            g.drawCenteredString(this.font, tr("draw_hint"), this.mapX0() + this.mapW() / 2, 8, -171);
        }

        if (this.overMap(mouseX)) {
            String c = "X " + Mth.floor(this.worldX(mouseX)) + "  Z " + Mth.floor(this.worldZ(mouseY)) + "   x" + String.format("%.2f", this.zoom);
            g.drawString(this.font, c, this.width - this.font.width(c) - 6, this.height - 12, -2236963);
        }

        g.fill(0, 0, 150, this.height, -267118044);
        g.fill(149, 0, 150, this.height, -12958112);
        g.drawString(this.font, this.title, 8, 8, -1);
        switch (this.mode) {
            case VIEW:
                if (this.zones.isEmpty()) {
                    g.drawWordWrap(this.font, tr("empty"), 8, 56, 134, -6641994);
                }

                int y0 = 76;
                int rows = Math.max(1, (this.height - y0 - 56) / 22);

                for (int i = 0; i < rows && i + this.listScroll < this.zones.size(); i++) {
                    ZoneData.Zone zxx = this.zones.get(i + this.listScroll);
                    int y = y0 + i * 22;
                    g.fill(8, y + 3, 14, y + 17, 0xFF000000 | zxx.color());
                }

                g.drawWordWrap(this.font, tr("help"), 8, this.height - 46, 134, -9536882);
                break;
            case GROUP:
                g.drawString(this.font, "여러 구획 → 하나의 단지", 8, 27, -5649979);
                g.drawString(this.font, "선택 " + this.groupSelection.size() + "개 · 휠로 목록 이동", 8, 73, -3419431);
                break;
            case DRAW:
                g.drawWordWrap(this.font, tr("draw_hint"), 8, 52, 134, -171);
                break;
            case EDIT:
                g.drawString(this.font, tr(this.editId < 0 ? "new_zone" : "edit_zone"), 8, 32, -1);
                g.drawString(this.font, tr("name"), 8, 50, -6641994);
                g.drawString(this.font, tr("color_label"), 8, 88, -6641994);
                g.fill(8, 101, 26, 119, 0xFF000000 | this.editColor);
                g.drawString(this.font, tr("size", this.ex2 - this.ex1 + 1, this.ez2 - this.ez1 + 1), 8, 126, -6641994);
                break;
            case CONFIRM_DELETE:
                ZoneData.Zone zxx = null;

                for (ZoneData.Zone zz : this.zones) {
                    if (zz.id() == this.deleteId) {
                        zxx = zz;
                    }
                }

                g.drawWordWrap(this.font, tr("delete_confirm", zxx == null ? "?" : zxx.name()), 8, 40, 134, -32640);
        }

        super.render(g, mouseX, mouseY, partialTick);
        if (hover != null && this.overMap(mouseX)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("block.qwertys_homenet." + blockKey(hover.kind())));
            if (!hover.unit().isEmpty()) {
                lines.add(tr("unit", hover.unit()));
            }

            lines.add(Component.literal(hover.pos().toShortString()).withStyle(s -> s.withColor(10135222)));
            g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        }
    }

    private int zoneColor(ZoneData.Zone zone) {
        return this.mode == DashboardScreen.Mode.GROUP && this.groupSelection.contains(zone.id())
            ? 16777215
            : this.zoneColors.getOrDefault(zone.id(), zone.color());
    }

    private void ensureZoneBorders() {
        int editing = this.mode == DashboardScreen.Mode.EDIT ? this.editId : -1;
        if (this.outlineSnapshot != this.zones || this.outlineEdit != editing) {
            this.outlineSnapshot = this.zones;
            this.outlineEdit = editing;
            this.zoneBorders.clear();
            this.zoneColors.clear();
            List<ZoneData.Zone> visible = this.zones.stream().filter(z -> z.id() != editing).toList();

            for (ZoneData.Zone zone : visible) {
                this.zoneBorders.put(zone.id(), ComplexOutlines.of(zone, visible));
                this.zoneColors.put(zone.id(), ComplexOutlines.color(zone, this.zones));
            }
        }
    }

    private void drawZoneBorders(GuiGraphics g, ZoneData.Zone zone, int color) {
        for (ComplexOutlines.Segment edge : this.zoneBorders.getOrDefault(zone.id(), List.of())) {
            int x1 = Math.round(this.screenX(edge.x1()));
            int x2 = Math.round(this.screenX(edge.x2()));
            int y1 = Math.round(this.screenY(edge.z1()));
            int y2 = Math.round(this.screenY(edge.z2()));
            int c = 0xFF000000 | color;
            switch (edge.side()) {
                case TOP:
                    g.fill(x1, y1, Math.max(x1 + 1, x2), y1 + 1, c);
                    break;
                case BOTTOM:
                    g.fill(x1, y1 - 1, Math.max(x1 + 1, x2), y1, c);
                    break;
                case LEFT:
                    g.fill(x1, y1, x1 + 1, Math.max(y1 + 1, y2), c);
                    break;
                case RIGHT:
                    g.fill(x1 - 1, y1, x1, Math.max(y1 + 1, y2), c);
            }
        }
    }

    private void drawZoneRect(GuiGraphics g, int x1, int z1, int x2, int z2, int color, String name) {
        this.drawZoneRect(g, x1, z1, x2, z2, color, name, true);
    }

    private void drawZoneRect(GuiGraphics g, int x1, int z1, int x2, int z2, int color, String name, boolean border) {
        int sx1 = Math.round(this.screenX(x1));
        int sy1 = Math.round(this.screenY(z1));
        int sx2 = Math.round(this.screenX(x2 + 1L));
        int sy2 = Math.round(this.screenY(z2 + 1L));
        if (sx2 - sx1 < 2) {
            sx2 = sx1 + 2;
        }

        if (sy2 - sy1 < 2) {
            sy2 = sy1 + 2;
        }

        g.fill(sx1, sy1, sx2, sy2, 1342177280 | color);
        int c = 0xFF000000 | color;
        if (border) {
            g.fill(sx1, sy1, sx2, sy1 + 1, c);
            g.fill(sx1, sy2 - 1, sx2, sy2, c);
            g.fill(sx1, sy1, sx1 + 1, sy2, c);
            g.fill(sx2 - 1, sy1, sx2, sy2, c);
        }

        if (sx2 - sx1 > this.font.width(name) + 4 && sy2 - sy1 > 12) {
            g.drawCenteredString(this.font, name, (sx1 + sx2) / 2, (sy1 + sy2) / 2 - 4, -1);
        }
    }

    private static String blockKey(DeviceRegistry.Kind k) {
        return switch (k) {
            case DOOR_CAMERA -> "door_station";
            default -> k.key();
        };
    }

    private static int kindColor(DeviceRegistry.Kind k) {
        return switch (k) {
            case DOOR_CAMERA -> 15753874;
            case WALLPAD -> 5227511;
            case VIDEO_PHONE -> 8445674;
            case INTERPHONE -> 11457921;
            case GUARD_CONSOLE -> 16758605;
            case LOBBY_PHONE -> 12216520;
            case DOOR_PHONE -> 15037299;
        };
    }

    private static enum Mode {
        VIEW,
        DRAW,
        EDIT,
        CONFIRM_DELETE,
        GROUP;
    }
}
