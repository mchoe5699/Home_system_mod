package com.qwerty.homenet.move;

import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.intercom.Intercom;
import com.qwerty.homenet.network.DeviceOffsetPacket;
import com.qwerty.homenet.network.ModNetwork;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.level.BlockEvent.BreakEvent;
import net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent;
import net.minecraftforge.event.level.ChunkWatchEvent.Watch;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.network.PacketDistributor;

@EventBusSubscriber(
    modid = "qwertys_homenet"
)
public final class DeviceOffsets {
    public static final String KEY = "HomeNetOffsetMm";
    private static final Map<ResourceKey<Level>, Map<BlockPos, Vec3>> CLIENT = new ConcurrentHashMap<>();

    public static Map<BlockPos, Vec3> client(Level l) {
        return CLIENT.computeIfAbsent(l.dimension(), k -> new ConcurrentHashMap<>());
    }

    public static void clearClient() {
        CLIENT.clear();
    }

    public static Vec3 get(BlockGetter l, BlockPos p) {
        BlockEntity b = l.getBlockEntity(p);
        if (b == null) {
            return Vec3.ZERO;
        } else {
            if (b.getLevel() != null && b.getLevel().isClientSide) {
                Vec3 v = client(b.getLevel()).get(p);
                if (v != null) {
                    return v;
                }
            }

            int[] a = millimetres(b);
            return new Vec3(a[0] / 1000.0, a[1] / 1000.0, a[2] / 1000.0);
        }
    }

    public static int[] millimetres(BlockEntity b) {
        if (b == null) {
            return new int[]{0, 0, 0};
        } else {
            int[] a = b.getPersistentData().getIntArray("HomeNetOffsetMm");
            if (a.length == 3) {
                return a;
            } else {
                int[] legacy = b.getPersistentData().getIntArray("HomeNetOffset");
                return legacy.length == 3 ? new int[]{legacy[0] * 100, legacy[1] * 100, legacy[2] * 100} : new int[]{0, 0, 0};
            }
        }
    }

    public static VoxelShape shape(VoxelShape s, BlockGetter l, BlockPos p) {
        Vec3 v = get(l, p);
        return v.equals(Vec3.ZERO) ? s : s.move(v.x, v.y, v.z);
    }

    public static void clientUpdate(Level l, BlockPos p, int x, int y, int z) {
        Vec3 v = new Vec3(x / 1000.0, y / 1000.0, z / 1000.0);
        if (v.equals(Vec3.ZERO)) {
            client(l).remove(p);
        } else {
            client(l).put(p.immutable(), v);
        }

        BlockEntity b = l.getBlockEntity(p);
        if (b != null) {
            b.getPersistentData().putIntArray("HomeNetOffsetMm", new int[]{x, y, z});
            b.requestModelDataUpdate();
        }

        l.sendBlockUpdated(p, l.getBlockState(p), l.getBlockState(p), 8);
    }

    public static DeviceOffsetPacket packet(ServerLevel l, BlockPos p) {
        BlockEntity b = l.getBlockEntity(p);
        int[] a = millimetres(b);
        return new DeviceOffsetPacket(p, a.length == 3 ? a[0] : 0, a.length == 3 ? a[1] : 0, a.length == 3 ? a[2] : 0);
    }

    public static void sync(ServerLevel l, BlockPos p) {
        ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> l.getChunkAt(p)), packet(l, p));
    }

    public static DeviceOffsets.Result nudge(ServerPlayer player, BlockPos from, Direction direction, int tenths) {
        return tenths >= 1 && tenths <= 10 ? nudgeMm(player, from, direction, tenths * 100) : new DeviceOffsets.Result(from, "이동량은 0.1~1칸입니다.");
    }

    public static DeviceOffsets.Result nudgeMm(ServerPlayer player, BlockPos from, Direction direction, int mm) {
        ServerLevel l = player.serverLevel();
        if (mm >= 1 && mm <= 1000 && l.isLoaded(from) && DeviceMoves.product(l.getBlockState(from)) && Intercom.inReach(player, from)) {
            BlockEntity source = l.getBlockEntity(from);
            if (source != null && !DeviceMoves.busy(source)) {
                Vec3 old = get(l, from);
                int[] a = new int[]{
                    (int)Math.round(old.x * 1000.0) + direction.getStepX() * mm,
                    (int)Math.round(old.y * 1000.0) + direction.getStepY() * mm,
                    (int)Math.round(old.z * 1000.0) + direction.getStepZ() * mm
                };
                int[] shift = new int[]{Math.floorDiv(a[0] + 500, 1000), Math.floorDiv(a[1] + 500, 1000), Math.floorDiv(a[2] + 500, 1000)};
                BlockPos to = from.offset(shift[0], shift[1], shift[2]);

                for (int i = 0; i < 3; i++) {
                    a[i] -= shift[i] * 1000;
                }

                if (l.isLoaded(to)
                    && l.getWorldBorder().isWithinBounds(to)
                    && !l.isOutsideBuildHeight(to)
                    && l.mayInteract(player, from)
                    && l.mayInteract(player, to)
                    && player.mayUseItemAt(to, direction, player.getMainHandItem())) {
                    if (!to.equals(from) && !l.getBlockState(to).isAir()) {
                        return new DeviceOffsets.Result(from, "다른 블록이 있는 위치로 이동할 수 없습니다.");
                    } else {
                        BlockState state = l.getBlockState(from);
                        VoxelShape proposed = state.getShape(l, from)
                            .move(-old.x, -old.y, -old.z)
                            .move(to.getX() + a[0] / 1000.0, to.getY() + a[1] / 1000.0, to.getZ() + a[2] / 1000.0);
                        AABB bounds = proposed.bounds().deflate(1.0E-5);

                        for (BlockPos p : BlockPos.betweenClosed(
                            BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ), BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ)
                        )) {
                            if (!p.equals(from)) {
                                if (!l.isLoaded(p)) {
                                    return new DeviceOffsets.Result(from, "주변 청크가 로딩되어야 합니다.");
                                }

                                VoxelShape collision = l.getBlockState(p).getCollisionShape(l, p).move(p.getX(), p.getY(), p.getZ());
                                if (Shapes.joinIsNotEmpty(Shapes.create(bounds), collision, BooleanOp.AND)) {
                                    return new DeviceOffsets.Result(from, "벽이나 다른 제품과 겹치는 위치입니다.");
                                }
                            }
                        }

                        if (!MinecraftForge.EVENT_BUS.post(new BreakEvent(l, from, state, player))
                            && !ForgeEventFactory.onBlockPlace(player, BlockSnapshot.create(l.dimension(), l, to), direction)) {
                            if (!to.equals(from)) {
                                Direction facing = state.hasProperty(WallMountedBlock.FACING) ? state.getValue(WallMountedBlock.FACING) : Direction.NORTH;
                                String result = DeviceMoves.move(l, from, to, facing);
                                if (!result.equals("moved")) {
                                    return new DeviceOffsets.Result(from, result);
                                }
                            }

                            BlockEntity moved = l.getBlockEntity(to);
                            moved.getPersistentData().putIntArray("HomeNetOffsetMm", a);
                            moved.setChanged();
                            if (!from.equals(to)) {
                                sync(l, from);
                            }

                            sync(l, to);
                            l.sendBlockUpdated(to, state, state, 3);
                            return new DeviceOffsets.Result(to, mm + "mm 이동했습니다.");
                        } else {
                            return new DeviceOffsets.Result(from, "이 위치의 이동 권한이 없습니다.");
                        }
                    }
                } else {
                    return new DeviceOffsets.Result(from, "이동할 위치나 설치 권한을 확인하세요.");
                }
            } else {
                return new DeviceOffsets.Result(from, "동작을 끝낸 뒤 이동하세요.");
            }
        } else {
            return new DeviceOffsets.Result(from, "제품과 이동량(1~1000mm)을 확인하세요.");
        }
    }

    @SubscribeEvent
    public static void watch(Watch e) {
        ServerLevel l = e.getLevel();
        LevelChunk chunk = l.getChunkSource().getChunkNow(e.getPos().x, e.getPos().z);
        if (chunk != null) {
            for (BlockEntity b : chunk.getBlockEntities().values()) {
                if (DeviceMoves.product(b.getBlockState())) {
                    ModNetwork.sendTo(e.getPlayer(), packet(l, b.getBlockPos()));
                }
            }
        }
    }

    @SubscribeEvent
    public static void placed(EntityPlaceEvent e) {
        if (e.getLevel() instanceof ServerLevel l && DeviceMoves.product(e.getPlacedBlock())) {
            sync(l, e.getPos());
        }
    }

    @SubscribeEvent
    public static void broken(BreakEvent e) {
        if (e.getLevel() instanceof ServerLevel l && DeviceMoves.product(e.getState())) {
            l.getServer().tell(new TickTask(l.getServer().getTickCount() + 1, () -> {
                if (!DeviceMoves.product(l.getBlockState(e.getPos()))) {
                    sync(l, e.getPos());
                }
            }));
        }
    }

    public record Result(BlockPos pos, String message) {
    }
}
