package com.qwerty.homenet.sht;

import com.qwerty.homenet.analog.AnalogLobbyEndpoint;
import com.qwerty.homenet.blockentity.DoorStationBlockEntity;
import com.qwerty.homenet.blockentity.Ha4100hBlockEntity;
import com.qwerty.homenet.blockentity.Kam305BlockEntity;
import com.qwerty.homenet.blockentity.Kvm906BlockEntity;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.blockentity.Sht5500sBlockEntity;
import com.qwerty.homenet.blockentity.WallpadBlockEntity;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.data.ZoneData;
import com.qwerty.homenet.intercom.CallState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
    modid = "homenet_sht101c"
)
public final class WorldVideo {
    private static final Map<UUID, List<WorldVideo.Signal>> LAST = new HashMap<>();
    private static final Map<UUID, String> DIMENSIONS = new HashMap<>();

    public static boolean supports(ReceiverBlockEntity b) {
        return b instanceof Station104M.Unit
            || !(b instanceof Station)
                && !(b instanceof Ha4100hBlockEntity)
                && !(b instanceof Sht5500sBlockEntity)
                && (b instanceof Kvm906BlockEntity || b.kind() == DeviceRegistry.Kind.VIDEO_PHONE || b instanceof WallpadBlockEntity);
    }

    public static BlockPos camera(ReceiverBlockEntity b) {
        if (!(supports(b) && b.getLevel() instanceof ServerLevel l)) {
            return null;
        } else if (b instanceof Station104M.Unit station) {
            return station.camera();
        } else if (b instanceof Kvm906BlockEntity k && !k.power()) {
            return null;
        } else {
            BlockPos at = b.getCallState() == CallState.IDLE ? null : b.getCaller();
            if (at == null && b instanceof Kvm906BlockEntity k && k.screenOn()) {
                at = b.linkedCamera();
            }

            if (at != null && l.isLoaded(at) && ZoneData.connected(l, b.getBlockPos(), at)) {
                BlockEntity source = l.getBlockEntity(at);
                if (source instanceof Ht100s.Unit camera) {
                    return DoorCameraRouting.isVideo(camera) ? at : null;
                } else if (source instanceof DoorStationBlockEntity door) {
                    return door.isDoorPhone() ? null : at;
                } else if (source instanceof AnalogLobbyEndpoint lobby) {
                    BlockPos exchange = b.getAnalogStation();
                    if (exchange != null && l.getBlockEntity(exchange) instanceof Kam305BlockEntity kam && !kam.allowsLobbyVideo(b.getUnit(), lobby)) {
                        return null;
                    } else {
                        BlockPos camera = lobby.cameraPosition();
                        return camera != null && l.isLoaded(camera) && ZoneData.connected(l, b.getBlockPos(), camera) ? camera : null;
                    }
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    @SubscribeEvent
    public static void tick(LevelTickEvent e) {
        if (e.phase == Phase.END && e.level instanceof ServerLevel l && l.getGameTime() % 20L == 0L) {
            ArrayList<WorldVideo.Signal> signals = new ArrayList<>();

            for (DeviceRegistry.Entry entry : DeviceRegistry.get(l).all()) {
                if (entry.kind().receiver && l.isLoaded(entry.pos()) && l.getBlockEntity(entry.pos()) instanceof ReceiverBlockEntity b) {
                    BlockPos camera = camera(b);
                    if (camera != null) {
                        signals.add(new WorldVideo.Signal(entry.pos(), camera));
                    }
                }
            }

            for (ServerPlayer player : l.players()) {
                List<WorldVideo.Signal> nearby = signals.stream()
                    .filter(s -> player.distanceToSqr(Vec3.atCenterOf(s.receiver())) <= 4096.0)
                    .limit(4096L)
                    .toList();
                String dim = l.dimension().location().toString();
                UUID id = player.getUUID();
                if (!nearby.equals(LAST.get(id)) || !dim.equals(DIMENSIONS.get(id))) {
                    Wire.video(player, dim, nearby);
                    LAST.put(id, nearby);
                    DIMENSIONS.put(id, dim);
                }
            }

            PlayerList online = l.getServer().getPlayerList();
            LAST.keySet().removeIf(idx -> online.getPlayer(idx) == null);
            DIMENSIONS.keySet().retainAll(LAST.keySet());
        }
    }

    public record Signal(BlockPos receiver, BlockPos camera) {
    }
}
