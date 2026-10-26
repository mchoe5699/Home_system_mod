package com.qwerty.homenet.sht;

import com.qwerty.homenet.analog.AnalogBus;
import com.qwerty.homenet.analog.AnalogLobbyEndpoint;
import com.qwerty.homenet.blockentity.CallerBlockEntity;
import com.qwerty.homenet.blockentity.Ha4100hBlockEntity;
import com.qwerty.homenet.blockentity.Kvm906BlockEntity;
import com.qwerty.homenet.blockentity.LobbyPhoneBlockEntity;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.blockentity.WallpadBlockEntity;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.data.ZoneData;
import com.qwerty.homenet.guard.GuardExchange;
import com.qwerty.homenet.guard.GuardSystem;
import com.qwerty.homenet.guard.GuardUnitAssignments;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
    modid = "homenet_sht101c"
)
public final class GuardRouting {
    private static final ThreadLocal<ReceiverBlockEntity> CALLER = new ThreadLocal<>();

    private GuardRouting() {
    }

    public static GuardSystem haSystem(Ha4100hBlockEntity device) {
        return device.getPersistentData().getBoolean("ShtHaStar") ? GuardSystem.STAR : GuardSystem.BUS;
    }

    public static AnalogBus haBus(Ha4100hBlockEntity device) {
        return device.getPersistentData().getBoolean("ShtHaStar") ? AnalogBus.DEDICATED : AnalogBus.COMMON_8_WIRE;
    }

    static String preferred(ReceiverBlockEntity b) {
        String explicit = DeviceRegistry.digits(b.getPersistentData().getString("ShtPreferredGuard"));
        if (!explicit.isEmpty()) {
            return explicit;
        } else if (b instanceof Station s && !s.guardNumber.isBlank()) {
            return DeviceRegistry.digits(s.guardNumber);
        } else if (b instanceof WallpadBlockEntity w && !w.setting("guard_no").isBlank()) {
            return DeviceRegistry.digits(w.setting("guard_no"));
        } else {
            BlockPos at = b.getAnalogStation();
            return at != null && b.getLevel() != null && b.getLevel().isLoaded(at) && b.getLevel().getBlockEntity(at) instanceof ReceiverBlockEntity g
                ? DeviceRegistry.digits(g.getUnit())
                : "";
        }
    }

    public static BlockPos kvmStation(Kvm906BlockEntity b) {
        if (b.getLevel() instanceof ServerLevel level) {
            String wanted = preferred(b);
            if (wanted.isEmpty()) {
                return null;
            } else {
                for (BlockPos at : DeviceRegistry.get(level).guardsByNumber(level, b.getBlockPos(), wanted)) {
                    if (level.isLoaded(at)
                        && ZoneData.connected(level, b.getBlockPos(), at)
                        && level.getBlockEntity(at) instanceof GuardExchange g
                        && b.supportsSwitchboard(g)) {
                        return at;
                    }
                }

                return null;
            }
        } else {
            return null;
        }
    }

    public static GuardExchange kvmSwitchboard(ReceiverBlockEntity receiver) {
        if (!(receiver instanceof Kvm906BlockEntity b)) {
            return null;
        } else {
            BlockPos at = kvmStation(b);
            return at != null && b.getLevel().getBlockEntity(at) instanceof GuardExchange g ? g : null;
        }
    }

    public static boolean lobbyMatches(GuardSystem system, AnalogLobbyEndpoint lobby) {
        return system == GuardSystem.BUS && (lobby.supports(AnalogBus.COMMON_8_WIRE) || lobby.supports(AnalogBus.COMMON_4_WIRE))
            || system == GuardSystem.STAR && lobby.supports(AnalogBus.DEDICATED);
    }

    public static BlockPos kvmLobbyStation(Kvm906BlockEntity receiver, BlockPos lobbyPos) {
        if (receiver.getLevel() != null && receiver.getLevel().isLoaded(lobbyPos) && ZoneData.connected(receiver.getLevel(), receiver.getBlockPos(), lobbyPos)) {
            return receiver.getLevel().getBlockEntity(lobbyPos) instanceof AnalogLobbyEndpoint lobby && lobbyMatches(receiver.guardSystem(), lobby)
                ? lobby.switchboardPosition()
                : null;
        } else {
            return null;
        }
    }

    public static String lobbyGuardNumber(LobbyPhoneBlockEntity lobby, String key) {
        String explicit = DeviceRegistry.digits(lobby.getPersistentData().getString("ShtPreferredGuard"));
        return explicit.isEmpty() ? lobby.cfg(key) : explicit;
    }

    public static List<DeviceRegistry.Entry> installedEntries(DeviceRegistry registry, ServerLevel level) {
        for (DeviceRegistry.Entry entry : registry.all()) {
            if (level.isLoaded(entry.pos())) {
                BlockEntity entity = level.getBlockEntity(entry.pos());
                boolean present = entry.kind().receiver ? entity instanceof ReceiverBlockEntity : entity instanceof CallerBlockEntity;
                if (!present || (entry.kind().receiver ? ((ReceiverBlockEntity)entity).kind() : ((CallerBlockEntity)entity).kind()) != entry.kind()) {
                    registry.remove(entry.pos());
                }
            }
        }

        return registry.all();
    }

    public static List<ReceiverBlockEntity> lobbyGuardReceivers(CallerBlockEntity source, String unused) {
        if (source instanceof LobbyPhoneBlockEntity lobby && lobby.getLevel() instanceof ServerLevel sl) {
            String number = DeviceRegistry.digits(lobbyGuardNumber(lobby, "guard_no"));
            if (number.isEmpty()) {
                return List.of();
            } else {
                ArrayList<ReceiverBlockEntity> out = new ArrayList<>();

                for (BlockPos at : DeviceRegistry.get(sl).guardsByNumber(sl, lobby.getBlockPos(), number)) {
                    if (sl.isLoaded(at)
                        && ZoneData.connected(sl, lobby.getBlockPos(), at)
                        && sl.getBlockEntity(at) instanceof ReceiverBlockEntity guard
                        && guard instanceof GuardExchange
                        && !(lobby instanceof AnalogLobbyEndpoint endpoint && !lobbyMatches(guard.guardSystem(), endpoint))) {
                        out.add(guard);
                    }
                }

                return out;
            }
        } else {
            return List.of();
        }
    }

    public static List<ReceiverBlockEntity> lobbyApartmentReceivers(CallerBlockEntity source, List<ReceiverBlockEntity> receivers) {
        if (source.kind() != DeviceRegistry.Kind.LOBBY_PHONE) {
            return receivers;
        } else {
            String number = source instanceof LobbyPhoneBlockEntity lobby
                ? DeviceRegistry.digits(lobbyGuardNumber(lobby, "guard_no"))
                : (
                    source instanceof Ht512.Unit ht
                        ? DeviceRegistry.digits(ht.guard)
                        : DeviceRegistry.digits(source.getPersistentData().getString("ShtPreferredGuard"))
                );
            return number.isEmpty()
                ? List.of()
                : receivers.stream()
                    .filter(r -> ZoneData.connected(source.getLevel(), source.getBlockPos(), r.getBlockPos()) && number.equals(preferred(r)))
                    .toList();
        }
    }

    public static List<?> noLobbyFallback(LobbyPhoneBlockEntity lobby, String unused) {
        return List.of();
    }

    public static boolean routePreferredCall(ReceiverBlockEntity receiver, ServerPlayer player, String mode) {
        if (!"guard".equals(mode)) {
            return false;
        } else {
            String number = preferred(receiver);
            if (number.isEmpty()) {
                receiver.noticeTo(player, "지정 경비실 번호를 설정하세요");
                return true;
            } else {
                CALLER.set(receiver);

                try {
                    receiver.callGuard(player, "no:" + number);
                } finally {
                    CALLER.remove();
                }

                return true;
            }
        }
    }

    public static List<BlockPos> numberedGuardList(DeviceRegistry registry, ServerLevel level, BlockPos from, String number) {
        ReceiverBlockEntity receiver = CALLER.get();
        List<BlockPos> matches = registry.guardsByNumber(level, from, number);
        if (receiver == null) {
            return matches;
        } else {
            String selected = preferred(receiver);
            if (!selected.isEmpty() && selected.equals(DeviceRegistry.digits(number))) {
                return receiver.guardSystem() != GuardSystem.BUS
                    ? matches.stream().filter(at -> level.isLoaded(at) && ZoneData.connected(level, from, at)).toList()
                    : matches.stream()
                        .filter(
                            at -> level.isLoaded(at)
                                && ZoneData.connected(level, from, at)
                                && level.getBlockEntity(at) instanceof GuardExchange guard
                                && guard.guardSystem() == GuardSystem.BUS
                                && receiver.supportsSwitchboard(guard)
                        )
                        .toList();
            } else {
                return List.of();
            }
        }
    }

    public static BlockPos linkedForCall(ReceiverBlockEntity receiver, String mode) {
        return mode.startsWith("no:") ? null : receiver.getAnalogStation();
    }

    public static String randomUnit(GuardUnitAssignments unused, ServerLevel level, BlockPos pos, boolean roomOnly) {
        DeviceRegistry registry = DeviceRegistry.get(level);

        for (int attempt = 0; attempt < 128; attempt++) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            String candidate = roomOnly ? Integer.toString(random.nextInt(101, 9999)) : random.nextInt(101, 999) + "-" + random.nextInt(101, 9999);
            if (registry.receivers(level, pos, candidate).isEmpty()) {
                return candidate;
            }
        }

        return Integer.toString((int)(Math.abs(pos.asLong()) % 899999L) + 100000);
    }

    @SubscribeEvent
    public static void tick(LevelTickEvent event) {
        if (event.phase == Phase.END && event.level instanceof ServerLevel level && level.getGameTime() % 200L == 0L) {
            List<DeviceRegistry.Entry> entries = installedEntries(DeviceRegistry.get(level), level);
            List<DeviceRegistry.Entry> guards = entries.stream().filter(e -> e.kind() == DeviceRegistry.Kind.GUARD_CONSOLE && level.isLoaded(e.pos())).toList();

            for (DeviceRegistry.Entry entry : entries) {
                if (entry.kind().receiver
                    && entry.kind() != DeviceRegistry.Kind.GUARD_CONSOLE
                    && level.isLoaded(entry.pos())
                    && level.getBlockEntity(entry.pos()) instanceof ReceiverBlockEntity receiver
                    && !(receiver instanceof Kvm906BlockEntity)) {
                    BlockPos linked = receiver.getAnalogStation();
                    if (linked != null && !ZoneData.connected(level, entry.pos(), linked)) {
                        receiver.unlinkSwitchboard();
                        linked = null;
                    }

                    String wanted = preferred(receiver);
                    if (!wanted.isEmpty() || receiver.guardSystem() != GuardSystem.BUS) {
                        if (linked != null
                            && !wanted.isEmpty()
                            && level.isLoaded(linked)
                            && level.getBlockEntity(linked) instanceof ReceiverBlockEntity existing
                            && !DeviceRegistry.digits(existing.getUnit()).equals(wanted)) {
                            receiver.unlinkSwitchboard();
                            linked = null;
                        }

                        BlockPos chosen = null;
                        double best = Double.POSITIVE_INFINITY;

                        for (DeviceRegistry.Entry candidate : guards) {
                            if ((wanted.isEmpty() || DeviceRegistry.digits(candidate.unit()).equals(wanted))
                                && ZoneData.connected(level, entry.pos(), candidate.pos())
                                && level.getBlockEntity(candidate.pos()) instanceof GuardExchange guard
                                && receiver.supportsSwitchboard(guard)) {
                                int rank = !wanted.isEmpty() && DeviceRegistry.digits(candidate.unit()).equals(wanted)
                                    ? 0
                                    : (candidate.pos().equals(linked) ? 1 : 2);
                                double score = rank * 1.0E9 + entry.pos().distSqr(candidate.pos());
                                if (score < best) {
                                    best = score;
                                    chosen = candidate.pos();
                                }
                            }
                        }

                        if (chosen != null && !chosen.equals(linked)) {
                            receiver.bindSwitchboard(chosen);
                        }
                    }
                }
            }
        }
    }
}
