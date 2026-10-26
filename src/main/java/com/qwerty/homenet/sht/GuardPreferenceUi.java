package com.qwerty.homenet.sht;

import com.qwerty.homenet.blockentity.CallerBlockEntity;
import com.qwerty.homenet.blockentity.Ha4100hBlockEntity;
import com.qwerty.homenet.blockentity.LobbyPhoneBlockEntity;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.client.ReceiverScreen;
import com.qwerty.homenet.client.guard.GuardMasterScreen;
import com.qwerty.homenet.client.lobby.LobbyPhoneScreen;
import com.qwerty.homenet.data.DeviceRegistry;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent.Init.Post;
import net.minecraftforge.client.event.ScreenEvent.MouseButtonPressed.Pre;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
    modid = "homenet_sht101c",
    value = {Dist.CLIENT}
)
public final class GuardPreferenceUi {
    private static final Map<Screen, GuardPreferenceUi.Row> ROWS = new WeakHashMap<>();

    private GuardPreferenceUi() {
    }

    private static BlockPos position(Screen screen) {
        if (screen instanceof ReceiverScreen receiver) {
            return receiver.getPos();
        } else {
            for (Class<?> type = screen.getClass(); type != null && type != Screen.class; type = type.getSuperclass()) {
                for (String name : new String[]{"pos", "at"}) {
                    try {
                        Field field = type.getDeclaredField(name);
                        if (field.getType() == BlockPos.class) {
                            field.setAccessible(true);
                            return (BlockPos)field.get(screen);
                        }
                    } catch (ReflectiveOperationException var7) {
                    }
                }
            }

            return null;
        }
    }

    private static String commonInput(Screen screen) {
        if (screen instanceof LobbyPhoneScreen) {
            BlockPos at = position(screen);
            Minecraft mc = Minecraft.getInstance();
            if (at != null && mc.level != null && mc.level.getBlockEntity(at) instanceof LobbyPhoneBlockEntity lobby && !lobby.getInput().isBlank()) {
                return lobby.getInput();
            }
        }

        if (screen.getFocused() instanceof EditBox focused && !focused.getValue().isBlank()) {
            return focused.getValue();
        } else {
            EditBox best = null;
            int bestScore = -1;

            for (Class<?> type = screen.getClass(); type != null && type != Screen.class; type = type.getSuperclass()) {
                for (Field field : type.getDeclaredFields()) {
                    try {
                        field.setAccessible(true);
                        Object value = field.get(screen);
                        if (value instanceof EditBox box) {
                            int score = score(field.getName());
                            if (score > bestScore && !box.getValue().isBlank()) {
                                best = box;
                                bestScore = score;
                            }
                        } else if (value instanceof Map<?, ?> fields) {
                            for (Entry<?, ?> entry : fields.entrySet()) {
                                if (entry.getValue() instanceof EditBox boxx) {
                                    int score = score(String.valueOf(entry.getKey()));
                                    if (score > bestScore && !boxx.getValue().isBlank()) {
                                        best = boxx;
                                        bestScore = score;
                                    }
                                }
                            }
                        }
                    } catch (ReflectiveOperationException var16) {
                    }
                }
            }

            if (best != null) {
                return best.getValue();
            } else {
                for (Class<?> type = screen.getClass(); type != null && type != Screen.class; type = type.getSuperclass()) {
                    for (String name : new String[]{"dialDraft", "input", "digits", "draft"}) {
                        try {
                            Field field = type.getDeclaredField(name);
                            field.setAccessible(true);
                            if (field.get(screen) instanceof String text && !text.isBlank() && text.matches("[0-9]{1,8}")) {
                                return text;
                            }
                        } catch (ReflectiveOperationException var15) {
                        }
                    }
                }

                return "";
            }
        }
    }

    private static int score(String name) {
        name = name.toLowerCase(Locale.ROOT);
        if (name.contains("guard")) {
            return 8;
        } else if (name.contains("number") || name.contains("unit") || name.contains("setting")) {
            return 7;
        } else if (name.contains("dong") || name.contains("dial")) {
            return 6;
        } else if (name.contains("search") || name.contains("input")) {
            return 5;
        } else {
            return !name.contains("message") && !name.contains("msg") && !name.contains("text") ? 1 : 3;
        }
    }

    private static GuardPreferenceUi.Row ensure(Screen screen) {
        GuardPreferenceUi.Row existing = ROWS.get(screen);
        if (existing != null) {
            return existing;
        } else if (!(screen instanceof Controls)
            && !(screen instanceof ModelControls)
            && !(screen instanceof Sht5500Controls)
            && !(screen instanceof GuardMasterScreen)) {
            Minecraft client = Minecraft.getInstance();
            if (client.level == null) {
                return null;
            } else {
                BlockPos pos = position(screen);
                if (pos != null && client.level.isLoaded(pos)) {
                    BlockEntity entity = client.level.getBlockEntity(pos);
                    boolean indoor = entity instanceof ReceiverBlockEntity;
                    boolean lobby = entity instanceof CallerBlockEntity caller && caller.kind() == DeviceRegistry.Kind.LOBBY_PHONE;
                    if (!indoor && !lobby) {
                        return null;
                    } else {
                        boolean console = entity instanceof ReceiverBlockEntity receiver && receiver.kind() == DeviceRegistry.Kind.GUARD_CONSOLE;
                        Button button = Button.builder(
                                Component.literal(console ? "경비실 번호 설정" : "지정경비실 입력"), b -> Wire.guardPref(pos, commonInput(screen), false)
                            )
                            .bounds(8, screen.height - 24, 126, 20)
                            .build();
                        GuardPreferenceUi.Row row = new GuardPreferenceUi.Row(pos, button);
                        if (entity instanceof Ha4100hBlockEntity) {
                            row.modeButton = Button.builder(Component.literal("통신 방식: BUS"), b -> Wire.haMode(pos, false))
                                .bounds(8, screen.height - 48, 126, 20)
                                .build();
                            Wire.haMode(pos, true);
                        }

                        ROWS.put(screen, row);
                        Wire.guardPref(pos, "", true);
                        return row;
                    }
                } else {
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    @SubscribeEvent
    public static void init(Post event) {
        ensure(event.getScreen());
    }

    @SubscribeEvent
    public static void render(net.minecraftforge.client.event.ScreenEvent.Render.Post event) {
        GuardPreferenceUi.Row row = ensure(event.getScreen());
        if (row != null) {
            row.button.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
            if (row.modeButton != null) {
                row.modeButton.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
            }

            event.getGuiGraphics()
                .drawString(
                    Minecraft.getInstance().font, "경비실 번호: " + (row.selected.isEmpty() ? "미설정" : row.selected), 140, event.getScreen().height - 18, -1118482
                );
        }
    }

    @SubscribeEvent
    public static void click(Pre event) {
        GuardPreferenceUi.Row row = ensure(event.getScreen());
        if (row != null && event.getButton() == 0) {
            Button hit = row.modeButton != null && row.modeButton.isMouseOver(event.getMouseX(), event.getMouseY())
                ? row.modeButton
                : (row.button.isMouseOver(event.getMouseX(), event.getMouseY()) ? row.button : null);
            if (hit != null) {
                hit.onPress();
                event.setCanceled(true);
            }
        }
    }

    static void show(Wire.GuardPrefView view) {
        GuardPreferenceUi.Row row = ROWS.get(Minecraft.getInstance().screen);
        if (row != null && row.pos.equals(view.pos())) {
            row.selected = view.number();
        }
    }

    static void showHaMode(Wire.HaModeView view) {
        GuardPreferenceUi.Row row = ROWS.get(Minecraft.getInstance().screen);
        if (row != null && row.pos.equals(view.pos()) && row.modeButton != null) {
            row.modeButton.setMessage(Component.literal("통신 방식: " + (view.star() ? "STAR" : "BUS")));
        }
    }

    static boolean hasButton(Screen screen) {
        return ROWS.containsKey(screen);
    }

    static boolean hasModeButton(Screen screen) {
        GuardPreferenceUi.Row row = ROWS.get(screen);
        return row != null && row.modeButton != null;
    }

    private static final class Row {
        final BlockPos pos;
        final Button button;
        Button modeButton;
        String selected = "";

        Row(BlockPos pos, Button button) {
            this.pos = pos;
            this.button = button;
        }
    }
}
