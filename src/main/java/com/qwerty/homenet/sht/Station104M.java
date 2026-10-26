package com.qwerty.homenet.sht;

import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.blockentity.CallerBlockEntity;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.intercom.CallState;
import com.qwerty.homenet.item.AnalogLinkerItem;
import com.qwerty.homenet.item.HomeLinkerItem;
import com.qwerty.homenet.move.DeviceOffsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class Station104M extends WallMountedBlock {
    public static final double HEIGHT = 0.4046;
    public static final double WIDTH = 0.5922;
    public static final double DEPTH = 0.112;

    public Station104M() {
        super(Properties.of().strength(1.5F).noOcclusion(), 4.7376, 4.763199999999999, 11.2368, 1.792);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState s) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
        return new Station104M.Unit(p, s);
    }

    @Override
    public void setPlacedBy(Level l, BlockPos at, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(l, at, state, placer, stack);
        if (!l.isClientSide && l.getBlockEntity(at) instanceof Station104M.Unit u) {
            u.setVariant(Sht104MItem.variant(stack));
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        return l.isClientSide ? null : createTickerHelper(t, Product.TYPE104M.get(), ReceiverBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState s, Level l, BlockPos at, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (!(held.getItem() instanceof HomeLinkerItem) && !(held.getItem() instanceof AnalogLinkerItem)) {
            if (player instanceof ServerPlayer p && l.getBlockEntity(at) instanceof Station104M.Unit u) {
                Vec3 v = hit.getLocation().subtract(Vec3.atLowerCornerOf(at)).subtract(DeviceOffsets.get(l, at));

                double across = switch ((Direction)s.getValue(FACING)) {
                    case NORTH -> 0.5 - v.x;
                    case SOUTH -> v.x - 0.5;
                    case EAST -> 0.5 - v.z;
                    case WEST -> v.z - 0.5;
                    default -> 10.0;
                };
                double x = 296.0 + across / 0.001;
                double y = (0.7023 - v.y) / 0.001;
                if (x >= 40.0 && x < 125.0 && y >= 43.0 && y <= 360.0) {
                    u.replace(p);
                } else if (y >= 376.0 && y <= 390.0 && x >= 257.0 && x <= 270.0) {
                    u.press("contrast", p, false);
                } else if (y >= 376.0 && y <= 390.0 && x >= 285.0 && x <= 298.0) {
                    u.press("brightness", p, false);
                } else if (x >= 458.0 && x < 545.0 && y >= 182.0 && y < 300.0 && !u.variant.equals("C")) {
                    if (!(y < 219.0) || !u.variant.equals("A") && !u.variant.equals("D")) {
                        if (x >= 521.0 && x <= 545.0 && u.variant.equals("B")) {
                            u.press(y < 219.0 ? "wireless" : "intercom", p, false);
                        } else if (x >= 521.0 && x <= 545.0) {
                            u.press(y < 276.0 ? "wireless" : "intercom", p, false);
                        }
                    } else {
                        int index = Math.min(3, Math.max(0, (int)((x - 460.0) / 21.0)));
                        u.press(
                            (u.variant.equals("A")
                                ? new String[]{"playback", "record", "erase", "clock"}
                                : new String[]{"broadcast", "listen", "broadcast_stop", "yes"})[index],
                            p,
                            false
                        );
                    }
                } else if (x >= 171.0 && x < 299.0 && y >= 237.0 && y < 261.0) {
                    u.press(x < 207.0 ? "redial" : (x < 261.0 ? "call" : "function"), p, false);
                } else if (x >= 310.0 && x < 332.0 && y >= 259.0 && y < 289.0) {
                    u.press(y < 274.0 ? "up" : "down", p, false);
                } else if (x >= 171.0 && x < 298.0 && y >= 267.0 && y < 354.0) {
                    int col = (int)((x - 176.0) / 39.0);
                    int row = (int)((y - 267.0) / 22.6);
                    if (col >= 0 && col < 3 && row >= 0 && row < 4) {
                        u.digit("123456789*0#".charAt(row * 3 + col) + "", false);
                    } else {
                        u.open(p, false);
                    }
                } else if (y >= 235.0 && y < 365.0 && x >= 303.0 && x < 345.0) {
                    u.press(y < 300.0 ? "guard" : "front", p, false);
                } else if (x >= 419.0 && x < 444.0 && y >= 193.0 && y < 357.0) {
                    boolean cs = u instanceof Station104CS.Unit;
                    String action = y < 211.0
                        ? "test"
                        : (
                            y >= 254.0 && y < 277.0
                                ? (cs ? "s1_away" : "away")
                                : (
                                    y >= 279.0 && y < 299.0
                                        ? (cs ? "s1_home" : "security")
                                        : (y >= 302.0 && y < 322.0 ? (cs ? "s1_disarm" : "restore") : (y >= 330.0 ? (cs ? "restore" : "emergency") : null))
                                )
                        );
                    if (action != null) {
                        u.press(action, p, false);
                    } else {
                        u.open(p, false);
                    }
                } else {
                    u.open(p, false);
                }
            }

            return InteractionResult.sidedSuccess(l.isClientSide);
        } else {
            return InteractionResult.PASS;
        }
    }

    @Override
    public void onRemove(BlockState s, Level l, BlockPos p, BlockState next, boolean moving) {
        if (!s.is(next.getBlock()) && !(next.getBlock() instanceof Station104M) && l.getBlockEntity(p) instanceof Station104M.Unit u) {
            u.onBroken();
        }

        super.onRemove(s, l, p, next, moving);
    }

    public static class Unit extends Station102 {
        String variant = "C";
        private final ArrayList<Long> recordings = new ArrayList<>();
        private final ArrayList<ArrayList<int[]>> clips = new ArrayList<>();
        private int playback = -1;
        private int recording = -1;
        private BlockPos recordingCamera;
        private final ArrayList<UUID> portableTickets = new ArrayList<>();

        public Unit(BlockPos p, BlockState s) {
            this(Product.TYPE104M.get(), p, s);
        }

        protected Unit(BlockEntityType<?> type, BlockPos p, BlockState s) {
            super(type, p, s);
            this.bus = true;
            this.folded = false;
        }

        String modelName() {
            return "SHT-104M";
        }

        @Override
        protected Map<String, String> settingsForClient() {
            Map<String, String> values = super.settingsForClient();
            values.put("model", this.modelName());
            values.put("variant", this.variant);
            values.put("recordings", this.recordings.size() + "");
            values.put("playback", this.playback + "");
            return values;
        }

        @Override
        public boolean supportsWirelessPortable() {
            return !this.variant.equals("C");
        }

        boolean allowsVariant(String value) {
            return value.matches("[ABCD]");
        }

        void setVariant(String value) {
            if (this.allowsVariant(value)) {
                if (!this.variant.equals(value)) {
                    this.variant = value;
                    if (!this.variant.equals("A")) {
                        this.playback = -1;
                        this.recording = -1;
                        this.recordingCamera = null;
                    }

                    if (this.variant.equals("C")) {
                        this.portableTickets.clear();
                        this.portableRegistered = false;
                        this.portableActive = false;
                    }

                    this.tell(this.modelName() + " " + this.variant + "형 파트 장착");
                    this.update();
                }
            }
        }

        @Override
        public boolean ownsPortable(ItemStack stack) {
            return this.supportsWirelessPortable()
                && stack.is(Product.PORTABLE104M.get())
                && stack.hasTag()
                && stack.getTag().getLong("base") == this.worldPosition.asLong()
                && (this.level == null || stack.getTag().getString("dimension").equals(this.level.dimension().location().toString()))
                && stack.getTag().hasUUID("serial")
                && this.serial.equals(stack.getTag().getUUID("serial"))
                && stack.getTag().hasUUID("ticket")
                && this.portableTickets.contains(stack.getTag().getUUID("ticket"));
        }

        boolean hasPortableTicket(UUID ticket) {
            return this.portableTickets.contains(ticket);
        }

        void unbindPortable(ItemStack stack) {
            if (this.ownsPortable(stack)) {
                this.portableTickets.remove(stack.getTag().getUUID("ticket"));
                this.portableRegistered = !this.portableTickets.isEmpty();
                if (!this.portableRegistered) {
                    this.portableActive = false;
                    this.finish();
                }

                for (String key : new String[]{"serial", "ticket", "base", "dimension", "Sht104PortableNumber"}) {
                    stack.getTag().remove(key);
                }

                this.tell("SHT-104M 무선전화 연결 해제 · " + this.portableTickets.size() + "/3");
            }
        }

        boolean bindPortable(ServerPlayer player, ItemStack stack) {
            if (!this.supportsWirelessPortable() || !this.allowed(player, false)) {
                return false;
            } else if (this.ownsPortable(stack)) {
                this.tell("이미 등록된 SHT-104M 무선전화입니다");
                return true;
            } else if (this.portableTickets.size() >= 3) {
                this.tell("무선전화는 최대 3대까지 등록할 수 있습니다");
                return false;
            } else {
                UUID ticket = UUID.randomUUID();
                this.portableTickets.add(ticket);
                this.portableRegistered = true;
                this.portableTicket = ticket;
                CompoundTag tag = stack.getOrCreateTag();
                tag.putUUID("serial", this.serial);
                tag.putUUID("ticket", ticket);
                tag.putInt("Sht104PortableNumber", this.portableTickets.size());
                tag.putLong("base", this.worldPosition.asLong());
                tag.putString("dimension", player.level().dimension().location().toString());
                tag.putBoolean("power", true);
                if (!tag.contains("battery")) {
                    tag.putInt("battery", 36000);
                }

                this.tell("SHT-104M 무선전화 등록 " + this.portableTickets.size() + "/3");
                return true;
            }
        }

        boolean beginRecording(BlockPos source) {
            if (this.level instanceof ServerLevel server && source != null) {
                int[] frame = Sht104Recording.capture(server, source);
                if (frame == null) {
                    return false;
                } else {
                    if (this.recordings.size() == 8) {
                        this.recordings.remove(0);
                        this.clips.remove(0);
                    }

                    this.recordings.add(this.level.getGameTime());
                    ArrayList<int[]> clip = new ArrayList<>();
                    clip.add(frame);
                    this.clips.add(clip);
                    this.playback = -1;
                    this.recording = this.clips.size() - 1;
                    this.recordingCamera = source;
                    this.update();
                    return true;
                }
            } else {
                return false;
            }
        }

        int[] playbackFrame() {
            if (this.variant.equals("A") && this.playback >= 0 && this.playback < this.clips.size() && !this.clips.get(this.playback).isEmpty()) {
                ArrayList<int[]> frames = this.clips.get(this.playback);
                return frames.get((int)((this.level == null ? 0L : this.level.getGameTime() / 10L) % frames.size()));
            } else {
                return null;
            }
        }

        @Override
        protected void tickExtra() {
            super.tickExtra();
            if (this.recording >= 0 && this.recording < this.clips.size() && this.recordingCamera != null && this.level instanceof ServerLevel server) {
                ArrayList<int[]> clip = this.clips.get(this.recording);
                if (clip.size() < 6 && this.camera() != null) {
                    if (this.level.getGameTime() % 20L == 0L) {
                        int[] frame = Sht104Recording.capture(server, this.recordingCamera);
                        if (frame != null) {
                            clip.add(frame);
                            this.update();
                        }
                    }
                } else {
                    this.recording = -1;
                    this.recordingCamera = null;
                    this.update();
                }
            }
        }

        @Override
        public boolean startRinging(BlockPos at, String key) {
            boolean accepted = super.startRinging(at, key);
            if (accepted && this.variant.equals("A") && key.equals("front_door") && this.beginRecording(at)) {
                this.tell("현관 영상 자동 녹화 · " + this.recordings.size() + "건");
            }

            return accepted;
        }

        private boolean portableDoorRelease() {
            if (this.getCallState() == CallState.CONNECTED
                && this.portableActive
                && this.audio
                && !this.hold
                && (this.incomingKey.equals("front_door") || this.incomingKey.equals("lobby"))) {
                CallerBlockEntity source = this.level != null
                        && this.caller != null
                        && this.level.isLoaded(this.caller)
                        && this.level.getBlockEntity(this.caller) instanceof CallerBlockEntity c
                    ? c
                    : null;
                if (!this.getReceiverDoors().isEmpty() || source != null && !source.getLinkedDoors().isEmpty()) {
                    this.receiverDoors.open(this.level, this.worldPosition);
                    if (source != null) {
                        source.openDoors();
                    }

                    this.tell("문열림 · 통화 유지");
                    return true;
                } else {
                    this.tell("연결된 문이 없습니다");
                    return true;
                }
            } else {
                return false;
            }
        }

        @Override
        void portableDigit(String key) {
            if (!key.equals("*") && !key.equals("#") || !this.portableDoorRelease()) {
                super.portableDigit(key);
            }
        }

        @Override
        void remotePress(String key, ServerPlayer p) {
            if (!key.equals("door") || !this.portableDoorRelease()) {
                super.remotePress(key, p);
            }
        }

        @Override
        public void digit(String key, boolean remote) {
            if (!remote
                && (key.equals("#") || key.equals("*"))
                && this.getCallState() == CallState.CONNECTED
                && (this.incomingKey.equals("front_door") || this.incomingKey.equals("lobby"))
                && this.audio
                && !this.hold
                && !this.portableActive) {
                this.openDoor();
            } else {
                super.digit(key, remote);
            }
        }

        @Override
        public void press(String key, ServerPlayer p, boolean remote) {
            if (remote) {
                super.press(key, p, true);
            } else if (key.equals("function")) {
                if (this.memory && this.editing.isEmpty()) {
                    this.memory = false;
                    this.digits = "";
                    this.tell("기능 종료");
                } else {
                    super.press("memory", p, false);
                }
            } else if (key.equals("call")) {
                if (this.getCallState() != CallState.IDLE) {
                    this.talk();
                } else {
                    super.press("line", p, false);
                }
            } else if (!key.equals("wireless") && !key.equals("intercom")) {
                if (!Set.of("playback", "record", "erase", "clock").contains(key)) {
                    if (!Set.of("broadcast", "listen", "broadcast_stop", "yes").contains(key)) {
                        super.press(key, p, false);
                    } else if (!this.variant.equals("D")) {
                        this.tell("D형 경보방송 파트가 필요합니다");
                    } else {
                        this.tell(switch (key) {
                            case "broadcast" -> "경보방송 표시";
                            case "listen" -> "경보방송 청취";
                            case "broadcast_stop" -> "경보방송 중단";
                            default -> "YES네 선택";
                        });
                    }
                } else if (!this.variant.equals("A")) {
                    this.tell("A형 녹화 파트가 필요합니다");
                } else {
                    switch (key) {
                        case "record":
                            if (!this.beginRecording(this.camera())) {
                                this.tell("녹화할 영상이 없습니다");
                                return;
                            }

                            this.tell("현관 영상 녹화 · " + this.recordings.size() + "건");
                            break;
                        case "playback":
                            if (this.recordings.isEmpty()) {
                                this.tell("녹화 영상이 없습니다");
                                return;
                            }

                            this.playback = (this.playback + 1) % this.recordings.size();
                            this.tell("녹화 재생 " + (this.playback + 1) + "/" + this.recordings.size() + " · " + this.clips.get(this.playback).size() + "프레임");
                            break;
                        case "erase":
                            if (this.playback < 0 || this.playback >= this.recordings.size()) {
                                this.tell("지울 녹화 영상을 선택하세요");
                                return;
                            }

                            this.recordings.remove(this.playback);
                            this.clips.remove(this.playback);
                            this.recording = -1;
                            this.recordingCamera = null;
                            this.playback = -1;
                            this.tell("녹화 영상 삭제 · " + this.recordings.size() + "건 남음");
                            break;
                        case "clock":
                            this.tell(
                                "시간 "
                                    + (this.level.getDayTime() / 1000L + 6L) % 24L
                                    + ":"
                                    + String.format("%02d", this.level.getDayTime() % 1000L * 60L / 1000L)
                            );
                    }

                    this.update();
                }
            } else if (!this.supportsWirelessPortable()) {
                this.tell("C형에는 무선전화가 없습니다");
            } else {
                if (key.equals("wireless")) {
                    if (p.getInventory().getFreeSlot() < 0) {
                        this.tell("인벤토리 공간이 필요합니다");
                        return;
                    }

                    ItemStack item = new ItemStack(Product.PORTABLE104M.get());
                    item.getOrCreateTag().putBoolean("power", true);
                    item.getTag().putInt("battery", 36000);
                    p.getInventory().add(item);
                    this.tell("SHT-104M 무선전화 · 충전기에 놓고 홈링커로 연결하세요");
                } else if (!this.portableRegistered) {
                    this.tell("연결된 무선전화가 없습니다");
                } else {
                    super.press("intercom", p, false);
                }
            }
        }

        @Override
        protected void saveAdditional(CompoundTag t) {
            super.saveAdditional(t);
            t.putString("Sht104Variant", this.variant);
            t.putLongArray("Sht104Recordings", this.recordings.stream().mapToLong(Long::longValue).toArray());
            ListTag list = new ListTag();

            for (ArrayList<int[]> clip : this.clips) {
                CompoundTag item = new CompoundTag();
                ListTag frames = new ListTag();

                for (int[] frame : clip) {
                    frames.add(new IntArrayTag(frame));
                }

                item.put("Frames", frames);
                list.add(item);
            }

            t.put("Sht104Clips", list);
            t.putInt("Sht104Playback", this.playback);
            t.putString("Sht104PortableTickets", String.join(",", this.portableTickets.stream().map(UUID::toString).toList()));
        }

        @Override
        public CompoundTag getUpdateTag() {
            CompoundTag tag = this.saveWithoutMetadata();
            if (this.level != null && !this.level.isClientSide) {
                BlockPos source = this.camera();
                if (source != null) {
                    tag.putLong("Video", source.asLong());
                }

                tag.putInt("Lights", this.lights());
            }

            return tag;
        }

        public ClientboundBlockEntityDataPacket getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }

        @Override
        public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
            CompoundTag tag = packet.getTag();
            if (tag != null) {
                this.load(tag);
            }
        }

        @Override
        public void load(CompoundTag t) {
            super.load(t);
            String value = t.getString("Sht104Variant");
            this.variant = value.matches("[ABCD]") ? value : "C";
            this.recordings.clear();

            for (long time : t.getLongArray("Sht104Recordings")) {
                this.recordings.add(time);
            }

            this.clips.clear();
            ListTag stored = t.getList("Sht104Clips", 10);

            for (int i = 0; i < this.recordings.size(); i++) {
                ArrayList<int[]> clip = new ArrayList<>();
                if (i < stored.size()) {
                    ListTag frames = stored.getCompound(i).getList("Frames", 11);

                    for (int j = 0; j < frames.size(); j++) {
                        int[] frame = frames.getIntArray(j);
                        if (frame.length == 300) {
                            clip.add(frame);
                        }
                    }
                }

                this.clips.add(clip);
            }

            this.playback = t.contains("Sht104Playback") ? t.getInt("Sht104Playback") : -1;
            this.portableTickets.clear();

            for (String text : t.getString("Sht104PortableTickets").split(",")) {
                try {
                    this.portableTickets.add(UUID.fromString(text));
                } catch (IllegalArgumentException var9) {
                }
            }

            this.portableRegistered = !this.portableTickets.isEmpty();
        }

        @Override
        public void lift(ServerPlayer p) {
            super.lift(p);
        }

        @Override
        public void replace(ServerPlayer p) {
            super.replace(p);
        }
    }
}
