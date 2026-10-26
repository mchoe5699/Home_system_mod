package com.qwerty.homenet.intercom;

import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.data.ZoneData;
import com.qwerty.homenet.guard.GuardExchange;
import com.qwerty.homenet.kocom.KocomCallRoute;
import com.qwerty.homenet.move.DeviceOffsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;

public final class Intercom {
    public static final double NEAR_RANGE = 8.0;
    public static final double NOTIFY_RANGE = 24.0;
    public static final double USE_RANGE = 8.0;

    private Intercom() {
    }

    public static List<ServerPlayer> playersNear(ServerLevel level, BlockPos pos, double range) {
        Vec3 c = interactionCentre(level, pos);
        double r2 = range * range;
        return level.players().stream().filter(p -> p.distanceToSqr(c) <= r2).collect(Collectors.toList());
    }

    private static Vec3 interactionCentre(BlockGetter level, BlockPos pos) {
        return KocomCallRoute.owns(level.getBlockEntity(pos)) ? KocomCallRoute.centre(level, pos) : Vec3.atCenterOf(pos).add(DeviceOffsets.get(level, pos));
    }

    public static boolean inReach(ServerPlayer player, BlockPos pos) {
        return player.distanceToSqr(interactionCentre(player.level(), pos)) <= 64.0;
    }

    public static Component sideName(String side) {
        int i = side.indexOf(58);
        if (i > 0) {
            String kind = side.substring(0, i);
            String arg = side.substring(i + 1);
            byte var5 = -1;
            switch (kind.hashCode()) {
                case 98705061:
                    if (kind.equals("guard")) {
                        var5 = 0;
                    }
                default:
                    return switch (var5) {
                        case 0 -> arg.isEmpty() ? Component.translatable("caller.qwertys_homenet.guard") : Component.literal(arg);
                        default -> Component.translatable("caller.qwertys_homenet." + kind + "_of", arg.isEmpty() ? "-" : arg);
                    };
            }
        } else {
            return Component.translatable("caller.qwertys_homenet." + side);
        }
    }

    public static boolean isReceiverKey(String key) {
        return key.startsWith("unit:") || key.startsWith("guard:");
    }

    public static String sanitize(String text, int max) {
        String t = text == null ? "" : text.replaceAll("[\\p{Cntrl}§]", "").trim();
        return t.length() > max ? t.substring(0, max) : t;
    }

    public static void say(ServerLevel level, ReceiverBlockEntity wallpad, IntercomCaller door, ServerPlayer sender, String side, String rawText) {
        if (ZoneData.connected(level, wallpad.getBlockPos(), door.intercomPosition())) {
            String text = sanitize(rawText, 80);
            if (!text.isEmpty()) {
                IntercomLine line = new IntercomLine(side, sender.getGameProfile().getName(), text);
                if (wallpad instanceof GuardExchange exchange) {
                    exchange.addLineFrom(door.intercomPosition(), line);
                } else {
                    wallpad.addLine(line);
                }

                if (door instanceof GuardExchange exchange) {
                    exchange.addLineFrom(wallpad.getBlockPos(), line);
                } else {
                    door.addLine(line);
                }

                Component msg = Component.translatable("chat.qwertys_homenet.intercom", sideName(side), line.name(), line.text())
                    .withStyle(ChatFormatting.AQUA);
                Set<ServerPlayer> targets = new LinkedHashSet<>();
                targets.addAll(playersNear(level, wallpad.getBlockPos(), 8.0));
                targets.addAll(playersNear(level, door.intercomPosition(), 8.0));

                for (ServerPlayer p : targets) {
                    p.sendSystemMessage(msg);
                }

                wallpad.syncScreens();
                door.syncScreens();
            }
        }
    }
}
