package com.qwerty.homenet.move;

import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.blockentity.Am10kpBlockEntity;
import com.qwerty.homenet.blockentity.CallerBlockEntity;
import com.qwerty.homenet.blockentity.HyundaiBusConsoleBlockEntity;
import com.qwerty.homenet.blockentity.Ip203apBlockEntity;
import com.qwerty.homenet.blockentity.Klp203rfBlockEntity;
import com.qwerty.homenet.blockentity.Kvm906BlockEntity;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.guard.GuardUnitAssignments;
import com.qwerty.homenet.intercom.DoorControl;
import com.qwerty.homenet.kvm.KvmPhoneDirectory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent;
import net.minecraftforge.event.level.ChunkEvent.Load;
import net.minecraftforge.event.level.ChunkEvent.Unload;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

@EventBusSubscriber(
    modid = "qwertys_homenet"
)
public final class DeviceMoves extends SavedData {
    private final List<DeviceMoves.Move> history = new ArrayList<>();
    private static final String REV = "HomeNetMoveRevision";
    private static final Set<String> POSITIONS = new HashSet<>(Set.of("Pos", "P", "Receiver", "Station", "AnalogStation", "Forward"));
    private static final Set<String> ARRAYS = new HashSet<>(Set.of("Doors", "ReceiverDoors", "Receivers"));
    private static final Map<ServerLevel, Set<Long>> LOADED = new WeakHashMap<>();

    public static DeviceMoves get(ServerLevel l) {
        return l.getDataStorage().computeIfAbsent(DeviceMoves::load, DeviceMoves::new, "qwertys_homenet_moves");
    }

    public static DeviceMoves load(CompoundTag t) {
        DeviceMoves d = new DeviceMoves();

        for (Tag row : t.getList("Moves", 10)) {
            CompoundTag n = (CompoundTag)row;
            d.history.add(new DeviceMoves.Move(BlockPos.of(n.getLong("From")), BlockPos.of(n.getLong("To"))));
        }

        return d;
    }

    @Override
    public CompoundTag save(CompoundTag t) {
        ListTag rows = new ListTag();

        for (DeviceMoves.Move m : this.history) {
            CompoundTag n = new CompoundTag();
            n.putLong("From", m.from.asLong());
            n.putLong("To", m.to.asLong());
            rows.add(n);
        }

        t.put("Moves", rows);
        return t;
    }

    public static void registerPositionField(String key) {
        POSITIONS.add(key);
    }

    public static void registerPositionArray(String key) {
        ARRAYS.add(key);
    }

    public static boolean product(BlockState s) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(s.getBlock());
        return id != null && id.getNamespace().equals("qwertys_homenet");
    }

    private static boolean replace(Tag tag, DeviceMoves.Move m) {
        boolean changed = false;
        if (tag instanceof CompoundTag t) {
            for (String key : new HashSet<>(t.getAllKeys())) {
                Tag value = t.get(key);
                if (value instanceof LongTag && POSITIONS.contains(key) && t.getLong(key) == m.from.asLong()) {
                    t.putLong(key, m.to.asLong());
                    changed = true;
                } else if (value instanceof LongArrayTag && ARRAYS.contains(key)) {
                    long[] a = t.getLongArray(key);
                    boolean update = false;

                    for (int i = 0; i < a.length; i++) {
                        if (a[i] == m.from.asLong()) {
                            a[i] = m.to.asLong();
                            update = true;
                        }
                    }

                    if (update) {
                        t.putLongArray(key, a);
                        changed = true;
                    }
                } else if (value instanceof CompoundTag || value instanceof ListTag) {
                    changed |= replace(value, m);
                }
            }
        } else if (tag instanceof ListTag) {
            for (Tag value : (ListTag)tag) {
                changed |= replace(value, m);
            }
        }

        return changed;
    }

    public static boolean busy(BlockEntity b) {
        if (b instanceof CallerBlockEntity c
            && (c.isInCall() || b.getBlockState().hasProperty(BlockStateProperties.POWERED) && b.getBlockState().getValue(BlockStateProperties.POWERED))) {
            return true;
        } else if (b instanceof ReceiverBlockEntity r
            && (r.isBusy() || r.getReceiverDoors().stream().anyMatch(p -> b.getLevel() instanceof ServerLevel sl && DoorControl.isOpen(sl, p)))) {
            return true;
        } else if (b instanceof Ip203apBlockEntity p && p.lifted()) {
            return true;
        } else if (b instanceof Kvm906BlockEntity k && (k.offHook() || k.telephone().busy())) {
            return true;
        } else {
            return !(b instanceof HyundaiBusConsoleBlockEntity h && (h.audioActive() || h.occupiedChannels() > 0 || h.alarmCount() > 0))
                ? b instanceof Am10kpBlockEntity a && (a.audioActive() || a.occupiedChannels() > 0 || a.alarmCount() > 0)
                : true;
        }
    }

    public static boolean applyPending(BlockEntity b) {
        if (b.getLevel() instanceof ServerLevel l && product(b.getBlockState())) {
            DeviceMoves d = get(l);
            int since = (int)Math.max(0L, Math.min((long)d.history.size(), b.getPersistentData().getLong("HomeNetMoveRevision")));
            CompoundTag tag = b.saveWithoutMetadata();
            boolean changed = false;

            for (int i = since; i < d.history.size(); i++) {
                changed |= replace(tag, d.history.get(i));
            }

            if (changed) {
                b.load(tag);
            }

            b.getPersistentData().putLong("HomeNetMoveRevision", d.history.size());
            b.setChanged();
            if (changed) {
                l.sendBlockUpdated(b.getBlockPos(), b.getBlockState(), b.getBlockState(), 3);
            }

            return changed;
        } else {
            return false;
        }
    }

    private static Collection<BlockEntity> loaded(ServerLevel l, BlockPos from, BlockPos to) {
        HashSet<Long> chunks = new HashSet<>(LOADED.getOrDefault(l, Set.of()));
        chunks.add(new ChunkPos(from).toLong());
        chunks.add(new ChunkPos(to).toLong());

        for (DeviceRegistry.Entry e : DeviceRegistry.get(l).all()) {
            if (l.isLoaded(e.pos())) {
                chunks.add(new ChunkPos(e.pos()).toLong());
            }
        }

        LinkedHashMap<BlockPos, BlockEntity> result = new LinkedHashMap<>();

        for (long key : chunks) {
            ChunkPos at = new ChunkPos(key);
            LevelChunk c = l.getChunkSource().getChunkNow(at.x, at.z);
            if (c != null) {
                for (BlockEntity b : c.getBlockEntities().values()) {
                    if (product(b.getBlockState())) {
                        result.put(b.getBlockPos(), b);
                    }
                }
            }
        }

        return result.values();
    }

    public static String move(ServerLevel l, BlockPos from, BlockPos to, Direction facing) {
        if (!from.equals(to) && l.isLoaded(from) && l.isLoaded(to) && l.getWorldBorder().isWithinBounds(to) && !l.isOutsideBuildHeight(to)) {
            BlockState state = l.getBlockState(from);
            if (!product(state)) {
                return "선택한 제품이 없습니다.";
            } else if (!l.getBlockState(to).isAir()) {
                return "빈 공간으로만 이동할 수 있습니다.";
            } else {
                BlockEntity source = l.getBlockEntity(from);
                if (source != null && busy(source)) {
                    return "통화·문열림·경보 동작을 끝낸 뒤 이동하세요.";
                } else {
                    BlockState next = state;
                    if (state.getBlock() instanceof WallMountedBlock && facing.getAxis().isHorizontal()) {
                        next = state.setValue(WallMountedBlock.FACING, facing);
                    }

                    DeviceMoves.Move m = new DeviceMoves.Move(from.immutable(), to.immutable());
                    LinkedHashMap<BlockEntity, CompoundTag> affected = new LinkedHashMap<>();
                    List<BlockEntity> peers = List.copyOf(loaded(l, from, to));

                    for (BlockEntity b : peers) {
                        if (b != source) {
                            CompoundTag tag = b.saveWithoutMetadata();
                            if (replace(tag, m)) {
                                if (busy(b)) {
                                    return "연결된 기기의 통화를 끝낸 뒤 이동하세요.";
                                }

                                affected.put(b, tag);
                            }
                        }
                    }

                    CompoundTag saved = source == null ? null : source.saveWithoutMetadata();
                    if (saved != null) {
                        replace(saved, m);
                    }

                    l.removeBlockEntity(from);
                    l.setBlock(from, Blocks.AIR.defaultBlockState(), 67);
                    if (l.setBlock(to, next, 3) && (source == null || l.getBlockEntity(to) != null)) {
                        BlockEntity moved = l.getBlockEntity(to);
                        if (moved != null && saved != null) {
                            moved.load(saved);
                            moved.setChanged();
                        }

                        DeviceMoves d = get(l);
                        d.history.add(m);
                        d.setDirty();
                        if (moved != null) {
                            moved.getPersistentData().putLong("HomeNetMoveRevision", d.history.size());
                        }

                        DeviceRegistry.get(l).relocate(from, to);
                        GuardUnitAssignments.get(l).relocate(from, to);
                        KvmPhoneDirectory.get(l).relocate(from, to);

                        for (Entry<BlockEntity, CompoundTag> e : affected.entrySet()) {
                            BlockEntity bx = e.getKey();
                            bx.load(e.getValue());
                            bx.getPersistentData().putLong("HomeNetMoveRevision", d.history.size());
                            bx.setChanged();
                            l.sendBlockUpdated(bx.getBlockPos(), bx.getBlockState(), bx.getBlockState(), 3);
                        }

                        for (BlockEntity bx : peers) {
                            if (bx != source && !bx.isRemoved()) {
                                bx.getPersistentData().putLong("HomeNetMoveRevision", d.history.size());
                                bx.setChanged();
                            }
                        }

                        if (moved != null) {
                            l.sendBlockUpdated(to, next, next, 3);
                        }

                        if (moved instanceof Klp203rfBlockEntity panel) {
                            panel.publish();
                        }

                        return "moved";
                    } else {
                        l.removeBlockEntity(to);
                        l.setBlock(to, Blocks.AIR.defaultBlockState(), 3);
                        l.setBlock(from, state, 3);
                        if (saved != null && l.getBlockEntity(from) != null) {
                            l.getBlockEntity(from).load(source.saveWithoutMetadata());
                        }

                        return "이동하지 못해 원래 위치로 복원했습니다.";
                    }
                }
            }
        } else {
            return "이동할 위치를 확인하세요.";
        }
    }

    private static void deferred(ServerLevel l, Runnable r) {
        l.getServer().tell(new TickTask(l.getServer().getTickCount() + 1, r));
    }

    @SubscribeEvent
    public static void chunkLoad(Load e) {
        if (e.getLevel() instanceof ServerLevel l && e.getChunk() instanceof LevelChunk c) {
            deferred(l, () -> {
                LOADED.computeIfAbsent(l, ignored -> new HashSet<>()).add(c.getPos().toLong());

                for (BlockEntity b : new ArrayList<>(c.getBlockEntities().values())) {
                    if (!b.isRemoved()) {
                        applyPending(b);
                    }
                }
            });
        }
    }

    @SubscribeEvent
    public static void chunkUnload(Unload e) {
        if (e.getLevel() instanceof ServerLevel l) {
            deferred(l, () -> {
                Set<Long> set = LOADED.get(l);
                if (set != null) {
                    set.remove(e.getChunk().getPos().toLong());
                }
            });
        }
    }

    @SubscribeEvent
    public static void placed(EntityPlaceEvent e) {
        if (e.getLevel() instanceof ServerLevel l) {
            BlockEntity b = l.getBlockEntity(e.getPos());
            if (b != null) {
                applyPending(b);
            }
        }
    }

    private record Move(BlockPos from, BlockPos to) {
    }
}
