package com.qwerty.homenet.sht;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent.Init.Post;
import net.minecraftforge.client.event.ScreenEvent.MouseButtonPressed.Pre;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
    modid = "homenet_sht101c",
    value = {Dist.CLIENT}
)
public final class UsageHelp {
    static final Set<String> SCREENS = Set.of(
        "WallpadScreen",
        "DoorStationScreen",
        "Am10kpScreen",
        "GuardMasterScreen",
        "Ha4100hConfigScreen",
        "Hmc7000Screen",
        "Ip203apScreen",
        "Kam305Screen",
        "Klp203rfScreen",
        "Kvm906Screen",
        "LobbyPhoneScreen",
        "KhnWallpadScreen",
        "DeviceConfigScreen"
    );
    private static final Map<Screen, UsageHelp.State> STATES = new WeakHashMap<>();

    private UsageHelp() {
    }

    static boolean supports(Screen s) {
        return s instanceof Ht512Visual.ModelScreen
            || s instanceof Controls
            || s instanceof AddressControls
            || s instanceof Sht5500Controls
            || s.getClass().getName().startsWith("com.qwerty.homenet.client.") && SCREENS.contains(s.getClass().getSimpleName());
    }

    static UsageHelp.State state(Screen s) {
        return STATES.get(s);
    }

    @SubscribeEvent
    public static void init(Post e) {
        Screen s = e.getScreen();
        if (supports(s)) {
            UsageHelp.State st = STATES.computeIfAbsent(s, k -> new UsageHelp.State());
            st.wrap = -1;
            st.button = Button.builder(Component.literal("사용법"), b -> {
                st.open = true;
                st.scroll = 0;
            }).bounds(Math.max(2, s.width - 66), 3, 62, 20).build();
            e.addListener(st.button);
        }
    }

    static boolean active(Screen s) {
        UsageHelp.State st = state(s);
        return st != null && st.open;
    }

    static void close(Screen s) {
        state(s).open = false;
    }

    @SubscribeEvent
    public static void render(net.minecraftforge.client.event.ScreenEvent.Render.Post e) {
        Screen s = e.getScreen();
        UsageHelp.State st = state(s);
        if (st != null) {
            GuiGraphics g = e.getGuiGraphics();
            Font font = Minecraft.getInstance().font;
            g.pose().pushPose();
            g.pose().translate(0.0F, 0.0F, 500.0F);
            st.button.render(g, e.getMouseX(), e.getMouseY(), e.getPartialTick());
            if (st.open) {
                int left = 8;
                int top = 28;
                int right = s.width - 8;
                int bottom = s.height - 8;
                int wrap = Math.max(20, right - left - 28);
                if (st.wrap != wrap) {
                    ArrayList<FormattedCharSequence> lines = new ArrayList<>();

                    for (String paragraph : UsageText.forScreen(s)) {
                        lines.addAll(font.split(Component.literal(paragraph), wrap));
                        lines.add(FormattedCharSequence.EMPTY);
                    }

                    st.lines = List.copyOf(lines);
                    st.wrap = wrap;
                }

                int visible = Math.max(1, (bottom - top - 56) / 12);
                st.max = Math.max(0, st.lines.size() - visible);
                st.scroll = Math.max(0, Math.min(st.max, st.scroll));
                g.fill(0, 0, s.width, s.height, -1342177280);
                g.fill(left, top, right, bottom, -14669774);
                g.fill(left, top, right, top + 2, -4598817);
                g.drawString(font, "사용법 · " + UsageText.title(s), left + 10, top + 10, -727109, false);
                g.enableScissor(left + 8, top + 27, right - 8, bottom - 26);

                for (int i = 0; i < visible && st.scroll + i < st.lines.size(); i++) {
                    g.drawString(font, st.lines.get(st.scroll + i), left + 12, top + 28 + i * 12, -1118482, false);
                }

                g.disableScissor();
                g.drawString(font, "휠 / ↑↓ / PageUp·Down · Esc: 돌아가기", left + 10, bottom - 17, -4600874, false);
                g.drawString(font, st.scroll + 1 + " / " + Math.max(1, st.max + 1), Math.max(left + 10, right - 65), top + 10, -4600874, false);
                st.button.setMessage(Component.literal("돌아가기"));
                st.button.render(g, e.getMouseX(), e.getMouseY(), e.getPartialTick());
            } else {
                st.button.setMessage(Component.literal("사용법"));
            }

            g.pose().popPose();
        }
    }

    @SubscribeEvent
    public static void click(Pre e) {
        UsageHelp.State st = state(e.getScreen());
        if (st != null) {
            if (active(e.getScreen())) {
                if (st.button.isMouseOver(e.getMouseX(), e.getMouseY())) {
                    close(e.getScreen());
                }

                e.setCanceled(true);
            } else if (st.button.isMouseOver(e.getMouseX(), e.getMouseY())) {
                st.button.mouseClicked(e.getMouseX(), e.getMouseY(), e.getButton());
                e.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void scroll(net.minecraftforge.client.event.ScreenEvent.MouseScrolled.Pre e) {
        if (active(e.getScreen())) {
            UsageHelp.State st = state(e.getScreen());
            st.scroll = Math.max(0, Math.min(st.max, st.scroll - (int)Math.signum(e.getScrollDelta()) * 3));
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void key(net.minecraftforge.client.event.ScreenEvent.KeyPressed.Pre e) {
        if (active(e.getScreen())) {
            UsageHelp.State st = state(e.getScreen());
            switch (e.getKeyCode()) {
                case 32:
                case 267:
                    st.scroll += 10;
                    break;
                case 256:
                    close(e.getScreen());
                    break;
                case 264:
                    st.scroll++;
                    break;
                case 265:
                    st.scroll--;
                    break;
                case 266:
                    st.scroll -= 10;
                    break;
                case 268:
                    st.scroll = 0;
                    break;
                case 269:
                    st.scroll = st.max;
            }

            st.scroll = Math.max(0, Math.min(st.max, st.scroll));
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void character(net.minecraftforge.client.event.ScreenEvent.CharacterTyped.Pre e) {
        if (active(e.getScreen())) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void release(net.minecraftforge.client.event.ScreenEvent.MouseButtonReleased.Pre e) {
        if (active(e.getScreen())) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void drag(net.minecraftforge.client.event.ScreenEvent.MouseDragged.Pre e) {
        if (active(e.getScreen())) {
            e.setCanceled(true);
        }
    }

    static final class State {
        boolean open;
        int scroll;
        int max;
        Button button;
        List<FormattedCharSequence> lines = List.of();
        int wrap = -1;
    }
}
