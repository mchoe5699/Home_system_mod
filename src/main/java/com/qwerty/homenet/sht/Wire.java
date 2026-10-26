package com.qwerty.homenet.sht;

import com.qwerty.homenet.blockentity.CallerBlockEntity;
import com.qwerty.homenet.blockentity.Ha4100hBlockEntity;
import com.qwerty.homenet.blockentity.LobbyPhoneBlockEntity;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.intercom.CallState;
import com.qwerty.homenet.intercom.Intercom;
import com.qwerty.homenet.intercom.IntercomLine;
import com.qwerty.homenet.network.WallpadDataPacket;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class Wire {
    static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(Product.id("station"), () -> "9", "9"::equals, "9"::equals);

    static void doorOpen(BlockPos pos) {
        CHANNEL.sendToServer(new Wire.DoorOpen(pos));
    }

    static void doorAction(BlockPos pos, String action, String value) {
        CHANNEL.sendToServer(new Wire.DoorAction(pos, action, value));
    }

    static void doorView(ServerPlayer p, Wire.DoorView view) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), view);
    }

    static void guardOpen(BlockPos pos) {
        CHANNEL.sendToServer(new Wire.GuardOpen(pos));
    }

    static void change104(BlockPos pos, String model, String variant) {
        CHANNEL.sendToServer(new Wire.Sht104FamilyChange(pos, model, variant));
    }

    static void guardPref(BlockPos pos, String number, boolean query) {
        CHANNEL.sendToServer(new Wire.GuardPref(pos, number, query));
    }

    static void haMode(BlockPos pos, boolean query) {
        CHANNEL.sendToServer(new Wire.HaMode(pos, query));
    }

    static void applyHaMode(Wire.HaMode request, ServerPlayer player) {
        Level level = player.level();
        BlockPos pos = request.pos();
        if (level.isLoaded(pos) && Intercom.inReach(player, pos) && level.getBlockEntity(pos) instanceof Ha4100hBlockEntity device) {
            if (!request.query()) {
                if (device.getCallState() != CallState.IDLE) {
                    player.sendSystemMessage(Component.literal("통화 종료 후 통신 방식을 변경하세요"));
                    return;
                }

                device.unlinkSwitchboard();
                CompoundTag tag = device.getPersistentData();
                tag.putBoolean("ShtHaStar", !tag.getBoolean("ShtHaStar"));
                device.setChanged();
                device.syncScreens();
            }

            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Wire.HaModeView(pos, device.getPersistentData().getBoolean("ShtHaStar")));
        }
    }

    static void applyGuardPref(Wire.GuardPref request, ServerPlayer player) {
        Level level = player.level();
        BlockPos pos = request.pos();
        if (level.isLoaded(pos) && Intercom.inReach(player, pos)) {
            BlockEntity entity = level.getBlockEntity(pos);
            boolean indoor = entity instanceof ReceiverBlockEntity;
            boolean lobby = entity instanceof CallerBlockEntity c && c.kind() == DeviceRegistry.Kind.LOBBY_PHONE;
            if (indoor || lobby) {
                CompoundTag tag = entity.getPersistentData();
                if (!request.query()) {
                    String entered = request.number();
                    if (entered.isBlank() && entity instanceof Ht512.Unit ht) {
                        entered = ht.input;
                    }

                    String number = DeviceRegistry.digits(entered);
                    if (number.isEmpty()) {
                        player.sendSystemMessage(Component.literal("공용 입력란에 지정 경비실 번호를 입력하세요"));
                        return;
                    }

                    if (number.length() > 8) {
                        number = number.substring(0, 8);
                    }

                    if (entity instanceof ReceiverBlockEntity guard && guard.kind() == DeviceRegistry.Kind.GUARD_CONSOLE) {
                        guard.setUnit(player, number);
                    } else {
                        tag.putString("ShtPreferredGuard", number);
                    }

                    if (entity instanceof Ht512.Unit ht) {
                        ht.guard = number;
                    }

                    entity.setChanged();
                    if (entity instanceof ReceiverBlockEntity r) {
                        r.syncScreens();
                    } else if (entity instanceof CallerBlockEntity cx) {
                        cx.syncScreens();
                    }
                }

                String numberx = entity instanceof ReceiverBlockEntity guard && guard.kind() == DeviceRegistry.Kind.GUARD_CONSOLE
                    ? guard.getUnit()
                    : tag.getString("ShtPreferredGuard");
                if (numberx.isEmpty() && entity instanceof ReceiverBlockEntity r) {
                    numberx = GuardRouting.preferred(r);
                } else if (numberx.isEmpty() && entity instanceof LobbyPhoneBlockEntity l) {
                    numberx = l.cfg("guard_no");
                } else if (numberx.isEmpty() && entity instanceof Ht512.Unit ht) {
                    numberx = ht.guard;
                }

                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Wire.GuardPrefView(pos, numberx));
            }
        }
    }

    static void guardKey(BlockPos pos, String key, String value) {
        CHANNEL.sendToServer(new Wire.GuardKey(pos, key, value));
    }

    static void guardView(ServerPlayer p, Sht5500s g) {
        CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> p),
            new Wire.GuardView(
                g.getBlockPos(),
                g.getUnit(),
                g.display(),
                g.forward(),
                g.muted(),
                g.night(),
                g.volume(),
                g.alertCount(),
                g.awayCount(),
                g.lamps(),
                g.lcd(),
                g.conversation()
            )
        );
    }

    static void lobbyKey(BlockPos at, String key) {
        CHANNEL.sendToServer(new Wire.LobbyKey(at, key));
    }

    static void address(ServerPlayer p, BlockPos at, String unit) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new Wire.Address(at, unit));
    }

    static void video(ServerPlayer p, String dimension, List<WorldVideo.Signal> signals) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new Wire.Video(dimension, signals));
    }

    static void init() {
        CHANNEL.messageBuilder(Wire.Sht104FamilyChange.class, 15, NetworkDirection.PLAY_TO_SERVER)
            .encoder(Wire.Sht104FamilyChange::encode)
            .decoder(Wire.Sht104FamilyChange::decode)
            .consumerMainThread((p, c) -> {
                ServerPlayer player = c.get().getSender();
                if (player != null) {
                    Sht104Family.change(player, p.pos(), p.model(), p.variant());
                }
            })
            .add();
        CHANNEL.messageBuilder(Wire.HaMode.class, 13, NetworkDirection.PLAY_TO_SERVER)
            .encoder(Wire.HaMode::encode)
            .decoder(Wire.HaMode::decode)
            .consumerMainThread((p, c) -> {
                ServerPlayer player = c.get().getSender();
                if (player != null) {
                    applyHaMode(p, player);
                }
            })
            .add();
        CHANNEL.messageBuilder(Wire.HaModeView.class, 14, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(Wire.HaModeView::encode)
            .decoder(Wire.HaModeView::decode)
            .consumerMainThread((p, c) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> GuardPreferenceUi.showHaMode(p)))
            .add();
        CHANNEL.messageBuilder(Wire.Address.class, 3, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(Wire.Address::encode)
            .decoder(Wire.Address::decode)
            .consumerMainThread((p, c) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Client.address(p)))
            .add();
        CHANNEL.messageBuilder(Wire.Video.class, 2, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(Wire.Video::encode)
            .decoder(Wire.Video::decode)
            .consumerMainThread((p, c) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> VideoDisplays.receive(p)))
            .add();
        CHANNEL.messageBuilder(Wire.Input.class, 0, NetworkDirection.PLAY_TO_SERVER)
            .encoder(Wire.Input::encode)
            .decoder(Wire.Input::decode)
            .consumerMainThread((p, c) -> {
                ServerPlayer player = c.get().getSender();
                if (player != null) {
                    apply(p, player);
                }
            })
            .add();
        CHANNEL.messageBuilder(Wire.View.class, 1, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(Wire.View::encode)
            .decoder(Wire.View::decode)
            .consumerMainThread((p, c) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Client.receive(p)))
            .add();
        CHANNEL.messageBuilder(Wire.LobbyKey.class, 4, NetworkDirection.PLAY_TO_SERVER)
            .encoder(Wire.LobbyKey::encode)
            .decoder(Wire.LobbyKey::decode)
            .consumerMainThread((p, c) -> {
                ServerPlayer player = c.get().getSender();
                if (player != null && player.level().isLoaded(p.pos()) && player.level().getBlockEntity(p.pos()) instanceof Ht512.Unit unit) {
                    unit.key(player, p.key());
                }
            })
            .add();
        CHANNEL.messageBuilder(Wire.GuardKey.class, 5, NetworkDirection.PLAY_TO_SERVER)
            .encoder(Wire.GuardKey::encode)
            .decoder(Wire.GuardKey::decode)
            .consumerMainThread((p, c) -> {
                ServerPlayer player = c.get().getSender();
                if (player != null && player.level().isLoaded(p.pos()) && player.level().getBlockEntity(p.pos()) instanceof Sht5500s g) {
                    g.action(player, p.key(), p.value());
                }
            })
            .add();
        CHANNEL.messageBuilder(Wire.GuardView.class, 6, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(Wire.GuardView::encode)
            .decoder(Wire.GuardView::decode)
            .consumerMainThread((p, c) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Client.guard(p)))
            .add();
        CHANNEL.messageBuilder(Wire.GuardPref.class, 7, NetworkDirection.PLAY_TO_SERVER)
            .encoder(Wire.GuardPref::encode)
            .decoder(Wire.GuardPref::decode)
            .consumerMainThread((p, c) -> {
                ServerPlayer player = c.get().getSender();
                if (player != null) {
                    applyGuardPref(p, player);
                }
            })
            .add();
        CHANNEL.messageBuilder(Wire.GuardPrefView.class, 8, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(Wire.GuardPrefView::encode)
            .decoder(Wire.GuardPrefView::decode)
            .consumerMainThread((p, c) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> GuardPreferenceUi.show(p)))
            .add();
        CHANNEL.messageBuilder(Wire.GuardOpen.class, 9, NetworkDirection.PLAY_TO_SERVER)
            .encoder(Wire.GuardOpen::encode)
            .decoder(Wire.GuardOpen::decode)
            .consumerMainThread((p, c) -> {
                ServerPlayer player = c.get().getSender();
                if (player != null) {
                    Sht5500s.openAt(player, p.pos());
                }
            })
            .add();
        CHANNEL.messageBuilder(Wire.DoorOpen.class, 10, NetworkDirection.PLAY_TO_SERVER)
            .encoder(Wire.DoorOpen::encode)
            .decoder(Wire.DoorOpen::decode)
            .consumerMainThread((p, c) -> {
                ServerPlayer player = c.get().getSender();
                if (player != null) {
                    DoorDeviceRouting.open(player, p.pos());
                }
            })
            .add();
        CHANNEL.messageBuilder(Wire.DoorAction.class, 11, NetworkDirection.PLAY_TO_SERVER)
            .encoder(Wire.DoorAction::encode)
            .decoder(Wire.DoorAction::decode)
            .consumerMainThread((p, c) -> {
                ServerPlayer player = c.get().getSender();
                if (player != null) {
                    DoorDeviceRouting.action(player, p);
                }
            })
            .add();
        CHANNEL.messageBuilder(Wire.DoorView.class, 12, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(Wire.DoorView::encode)
            .decoder(Wire.DoorView::decode)
            .consumerMainThread((p, c) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> DoorDeviceControls.receive(p)))
            .add();
    }

    public static void apply(Wire.Input in, ServerPlayer p) {
        if (in.remote || !in.action.equals("unit") || !SamsungAddress.set(p, in.pos, in.value)) {
            if (p.level().isLoaded(in.pos) && p.level().getBlockEntity(in.pos) instanceof Station s && s.allowed(p, in.remote)) {
                if (!in.accessory || in.remote && Portable.carried(p, s)) {
                    String var5 = in.action;
                    switch (var5) {
                        case "press":
                            if (in.remote) {
                                if (!in.accessory && !s.wired()) {
                                    s.radioPress(in.value, p);
                                } else {
                                    s.remotePress(in.value, p);
                                }
                            } else {
                                s.press(in.value, p, false);
                            }
                            break;
                        case "digit":
                            if (in.accessory) {
                                s.portableDigit(in.value);
                            } else {
                                s.digit(in.value, in.remote);
                            }
                            break;
                        case "chat":
                            if (in.accessory) {
                                s.portableSay(p, in.value);
                            } else {
                                s.say(p, in.value, in.remote);
                            }
                            break;
                        case "unit":
                            if (!in.remote) {
                                s.setUnit(p, in.value);
                            }
                            break;
                        case "guard_no":
                            if (!in.remote) {
                                s.setGuardNumber(in.value);
                            }
                            break;
                        case "logo":
                            if (!in.remote) {
                                s.editLogo("logo_text=" + Intercom.sanitize(in.value, 32));
                            }
                    }

                    s.update();
                }
            }
        }
    }

    static void show(ServerPlayer p, WallpadDataPacket data, boolean remote, boolean accessory) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new Wire.View(data, remote, accessory));
    }

    static void sendPortable(BlockPos p, String action, String value) {
        CHANNEL.sendToServer(new Wire.Input(p, true, action, value, true));
    }

    static void send(BlockPos p, boolean remote, String action, String value) {
        CHANNEL.sendToServer(new Wire.Input(p, remote, action, value));
    }

    record Address(BlockPos pos, String unit) {
        static void encode(Wire.Address p, FriendlyByteBuf b) {
            b.writeBlockPos(p.pos);
            b.writeUtf(p.unit, 32);
        }

        static Wire.Address decode(FriendlyByteBuf b) {
            return new Wire.Address(b.readBlockPos(), b.readUtf(32));
        }
    }

    public record DoorAction(BlockPos pos, String action, String value) {
        static void encode(Wire.DoorAction p, FriendlyByteBuf b) {
            b.writeBlockPos(p.pos);
            b.writeUtf(p.action, 16);
            b.writeUtf(p.value, 80);
        }

        static Wire.DoorAction decode(FriendlyByteBuf b) {
            return new Wire.DoorAction(b.readBlockPos(), b.readUtf(16), b.readUtf(80));
        }
    }

    public record DoorOpen(BlockPos pos) {
        static void encode(Wire.DoorOpen p, FriendlyByteBuf b) {
            b.writeBlockPos(p.pos);
        }

        static Wire.DoorOpen decode(FriendlyByteBuf b) {
            return new Wire.DoorOpen(b.readBlockPos());
        }
    }

    public record DoorView(BlockPos pos, String model, String unit, int linked, boolean inCall, List<IntercomLine> log) {
        static void encode(Wire.DoorView p, FriendlyByteBuf b) {
            b.writeBlockPos(p.pos);
            b.writeUtf(p.model, 32);
            b.writeUtf(p.unit, 32);
            b.writeVarInt(p.linked);
            b.writeBoolean(p.inCall);
            b.writeVarInt(Math.min(20, p.log.size()));

            for (IntercomLine line : p.log.stream().skip(Math.max(0, p.log.size() - 20)).toList()) {
                line.write(b);
            }
        }

        static Wire.DoorView decode(FriendlyByteBuf b) {
            BlockPos pos = b.readBlockPos();
            String model = b.readUtf(32);
            String unit = b.readUtf(32);
            int linked = b.readVarInt();
            boolean inCall = b.readBoolean();
            int count = b.readVarInt();
            if (count >= 0 && count <= 20) {
                ArrayList<IntercomLine> log = new ArrayList<>();

                for (int i = 0; i < count; i++) {
                    log.add(IntercomLine.read(b));
                }

                return new Wire.DoorView(pos, model, unit, linked, inCall, log);
            } else {
                throw new IllegalArgumentException("door log");
            }
        }
    }

    record GuardKey(BlockPos pos, String key, String value) {
        static void encode(Wire.GuardKey p, FriendlyByteBuf b) {
            b.writeBlockPos(p.pos);
            b.writeUtf(p.key, 24);
            b.writeUtf(p.value, 32);
        }

        static Wire.GuardKey decode(FriendlyByteBuf b) {
            return new Wire.GuardKey(b.readBlockPos(), b.readUtf(24), b.readUtf(32));
        }
    }

    public record GuardOpen(BlockPos pos) {
        static void encode(Wire.GuardOpen p, FriendlyByteBuf b) {
            b.writeBlockPos(p.pos);
        }

        static Wire.GuardOpen decode(FriendlyByteBuf b) {
            return new Wire.GuardOpen(b.readBlockPos());
        }
    }

    public record GuardPref(BlockPos pos, String number, boolean query) {
        static void encode(Wire.GuardPref p, FriendlyByteBuf b) {
            b.writeBlockPos(p.pos);
            b.writeUtf(p.number, 16);
            b.writeBoolean(p.query);
        }

        static Wire.GuardPref decode(FriendlyByteBuf b) {
            return new Wire.GuardPref(b.readBlockPos(), b.readUtf(16), b.readBoolean());
        }
    }

    public record GuardPrefView(BlockPos pos, String number) {
        static void encode(Wire.GuardPrefView p, FriendlyByteBuf b) {
            b.writeBlockPos(p.pos);
            b.writeUtf(p.number, 16);
        }

        static Wire.GuardPrefView decode(FriendlyByteBuf b) {
            return new Wire.GuardPrefView(b.readBlockPos(), b.readUtf(16));
        }
    }

    record GuardView(
        BlockPos pos,
        String number,
        String status,
        String forward,
        boolean muted,
        boolean night,
        int volume,
        int alerts,
        int away,
        int lamps,
        String lcd,
        List<IntercomLine> log
    ) {
        static void encode(Wire.GuardView p, FriendlyByteBuf b) {
            b.writeBlockPos(p.pos);
            b.writeUtf(p.number, 16);
            b.writeUtf(p.status, 128);
            b.writeUtf(p.forward, 16);
            b.writeBoolean(p.muted);
            b.writeBoolean(p.night);
            b.writeVarInt(p.volume);
            b.writeVarInt(p.alerts);
            b.writeVarInt(p.away);
            b.writeVarInt(p.lamps);
            b.writeUtf(p.lcd, 256);
            List<IntercomLine> lines = p.log.stream().skip(Math.max(0, p.log.size() - 12)).toList();
            b.writeVarInt(lines.size());

            for (IntercomLine line : lines) {
                line.write(b);
            }
        }

        static Wire.GuardView decode(FriendlyByteBuf b) {
            BlockPos pos = b.readBlockPos();
            String number = b.readUtf(16);
            String status = b.readUtf(128);
            String forward = b.readUtf(16);
            boolean muted = b.readBoolean();
            boolean night = b.readBoolean();
            int volume = b.readVarInt();
            int alerts = b.readVarInt();
            int away = b.readVarInt();
            int lamps = b.readVarInt();
            String lcd = b.readUtf(256);
            int count = b.readVarInt();
            if (count >= 0 && count <= 12) {
                ArrayList<IntercomLine> log = new ArrayList<>();

                for (int i = 0; i < count; i++) {
                    log.add(IntercomLine.read(b));
                }

                return new Wire.GuardView(pos, number, status, forward, muted, night, volume, alerts, away, lamps, lcd, log);
            } else {
                throw new IllegalArgumentException("guard log");
            }
        }
    }

    record HaMode(BlockPos pos, boolean query) {
        static void encode(Wire.HaMode p, FriendlyByteBuf b) {
            b.writeBlockPos(p.pos);
            b.writeBoolean(p.query);
        }

        static Wire.HaMode decode(FriendlyByteBuf b) {
            return new Wire.HaMode(b.readBlockPos(), b.readBoolean());
        }
    }

    record HaModeView(BlockPos pos, boolean star) {
        static void encode(Wire.HaModeView p, FriendlyByteBuf b) {
            b.writeBlockPos(p.pos);
            b.writeBoolean(p.star);
        }

        static Wire.HaModeView decode(FriendlyByteBuf b) {
            return new Wire.HaModeView(b.readBlockPos(), b.readBoolean());
        }
    }

    public record Input(BlockPos pos, boolean remote, String action, String value, boolean accessory) {
        public Input(BlockPos p, boolean r, String a, String v) {
            this(p, r, a, v, false);
        }

        static void encode(Wire.Input p, FriendlyByteBuf b) {
            b.writeBlockPos(p.pos);
            b.writeBoolean(p.remote);
            b.writeUtf(p.action, 32);
            b.writeUtf(p.value, 160);
            b.writeBoolean(p.accessory);
        }

        static Wire.Input decode(FriendlyByteBuf b) {
            return new Wire.Input(b.readBlockPos(), b.readBoolean(), b.readUtf(32), b.readUtf(160), b.readBoolean());
        }
    }

    record LobbyKey(BlockPos pos, String key) {
        static void encode(Wire.LobbyKey p, FriendlyByteBuf b) {
            b.writeBlockPos(p.pos);
            b.writeUtf(p.key, 16);
        }

        static Wire.LobbyKey decode(FriendlyByteBuf b) {
            return new Wire.LobbyKey(b.readBlockPos(), b.readUtf(16));
        }
    }

    public record Sht104FamilyChange(BlockPos pos, String model, String variant) {
        static void encode(Wire.Sht104FamilyChange p, FriendlyByteBuf b) {
            b.writeBlockPos(p.pos);
            b.writeUtf(p.model, 16);
            b.writeUtf(p.variant, 1);
        }

        static Wire.Sht104FamilyChange decode(FriendlyByteBuf b) {
            return new Wire.Sht104FamilyChange(b.readBlockPos(), b.readUtf(16), b.readUtf(1));
        }
    }

    record Video(String dimension, List<WorldVideo.Signal> signals) {
        static void encode(Wire.Video p, FriendlyByteBuf b) {
            b.writeUtf(p.dimension, 128);
            b.writeVarInt(p.signals.size());

            for (WorldVideo.Signal s : p.signals) {
                b.writeBlockPos(s.receiver());
                b.writeBlockPos(s.camera());
            }
        }

        static Wire.Video decode(FriendlyByteBuf b) {
            String dim = b.readUtf(128);
            int count = b.readVarInt();
            if (count >= 0 && count <= 4096) {
                ArrayList<WorldVideo.Signal> list = new ArrayList<>();

                for (int i = 0; i < count; i++) {
                    list.add(new WorldVideo.Signal(b.readBlockPos(), b.readBlockPos()));
                }

                return new Wire.Video(dim, list);
            } else {
                throw new IllegalArgumentException("video count");
            }
        }
    }

    record View(WallpadDataPacket state, boolean remote, boolean accessory) {
        static void encode(Wire.View p, FriendlyByteBuf b) {
            WallpadDataPacket.encode(p.state, b);
            b.writeBoolean(p.remote);
            b.writeBoolean(p.accessory);
        }

        static Wire.View decode(FriendlyByteBuf b) {
            return new Wire.View(WallpadDataPacket.decode(b), b.readBoolean(), b.readBoolean());
        }
    }
}
