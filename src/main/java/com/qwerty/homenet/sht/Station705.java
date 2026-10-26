package com.qwerty.homenet.sht;

import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.intercom.CallState;
import com.qwerty.homenet.item.AnalogLinkerItem;
import com.qwerty.homenet.item.HomeLinkerItem;
import com.qwerty.homenet.move.DeviceOffsets;
import java.util.ArrayList;
import java.util.Map;
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

public final class Station705 extends WallMountedBlock {
    public static final double WIDTH = 0.48;
    public static final double HEIGHT = 0.354;
    public static final double DEPTH = 0.032;

    public Station705() {
        super(Properties.of().strength(1.5F).noOcclusion(), 3.84, 5.168, 10.832, 0.512);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos at, BlockState state) {
        return new Station705.Unit(at, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, Product.TYPE705.get(), ReceiverBlockEntity::serverTick);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return false;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (!(held.getItem() instanceof HomeLinkerItem) && !(held.getItem() instanceof AnalogLinkerItem)) {
            if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof Station705.Unit unit) {
                Vec3 point = hit.getLocation().subtract(Vec3.atLowerCornerOf(pos)).subtract(DeviceOffsets.get(level, pos));

                double across = switch ((Direction)state.getValue(FACING)) {
                    case NORTH -> 0.5 - point.x;
                    case SOUTH -> point.x - 0.5;
                    case EAST -> 0.5 - point.z;
                    case WEST -> point.z - 0.5;
                    default -> 10.0;
                };
                String action = keyAt(280.0 + across * 1000.0, (0.677 - point.y) * 1000.0, unit.coverOpen);
                if (action == null) {
                    unit.open(server, false);
                } else {
                    unit.press(server.isShiftKeyDown() && action.equals("meter") ? "erase_all" : action, server, false);
                }
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        } else {
            return InteractionResult.PASS;
        }
    }

    static String keyAt(double x, double y, boolean coverOpen) {
        if (x >= 142.0 && x <= 415.0 && y >= 284.0 && y <= 354.0) {
            if (!coverOpen) {
                return "cover";
            } else if (x >= 166.0 && x <= 210.0 && y >= 315.0 && y <= 333.0) {
                return x < 188.0 ? "minus" : "plus";
            } else {
                if (y >= 315.0 && y <= 331.0) {
                    int i = (int)Math.round((x - 224.0) / 24.0);
                    String[] actions = new String[]{"intercom", "record", "meter", "function", "security", "stop_restore", "test"};
                    if (i >= 0 && i < actions.length && Math.abs(x - (224 + i * 24)) < 9.0) {
                        return actions[i];
                    }
                }

                return "cover";
            }
        } else {
            if (y >= 209.0 && y <= 234.0) {
                int[] centers = new int[]{211, 244, 275, 308, 350};
                String[] actions = new String[]{"guard", "door_open", "talk_front", "away", "emergency"};

                for (int i = 0; i < centers.length; i++) {
                    if (Math.abs(x - centers[i]) < 12.0) {
                        return actions[i];
                    }
                }
            }

            return null;
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof Station705.Unit unit) {
            unit.onBroken();
        }

        super.onRemove(state, level, pos, next, moving);
    }

    public static final class Unit extends Station102 {
        boolean coverOpen;
        boolean functionMode;
        boolean gasClosed;
        int ringtone;
        int alarmTest;
        int playback = -1;
        private final ArrayList<int[]> recordings = new ArrayList<>();

        public Unit(BlockPos at, BlockState state) {
            super(Product.TYPE705.get(), at, state);
            this.bus = true;
            this.docked = true;
            this.folded = false;
        }

        @Override
        public boolean supportsWirelessPortable() {
            return true;
        }

        @Override
        public void lift(ServerPlayer player) {
            this.tell("SHT-705 핸즈프리 통화기");
        }

        @Override
        public void replace(ServerPlayer player) {
            this.lift(player);
        }

        @Override
        public void say(ServerPlayer player, String text, boolean remote) {
            boolean seated = this.docked;
            this.docked = false;

            try {
                super.say(player, text, remote);
            } finally {
                this.docked = seated;
            }
        }

        @Override
        protected Map<String, String> settingsForClient() {
            Map<String, String> data = super.settingsForClient();
            data.put("model", "SHT-705");
            data.put("cover", this.coverOpen ? "1" : "0");
            data.put("function_mode", this.functionMode ? "1" : "0");
            data.put("recordings", Integer.toString(this.recordings.size()));
            data.put("playback", Integer.toString(this.playback));
            data.put("ringtone", Integer.toString(this.ringtone + 1));
            data.put("gas_closed", this.gasClosed ? "1" : "0");
            data.put("front_lamps", Integer.toString(this.frontLamps()));
            data.put("inner_lamps", Integer.toString(this.innerLamps()));
            return data;
        }

        int[] recordedFrame() {
            return this.playback >= 0 && this.playback < this.recordings.size() ? this.recordings.get(this.playback) : null;
        }

        private void record(BlockPos camera) {
            if (this.level instanceof ServerLevel server) {
                int[] image = Sht104Recording.capture(server, camera);
                if (image == null) {
                    this.tell("녹화할 영상이 없습니다");
                } else {
                    if (this.recordings.size() == 32) {
                        this.recordings.remove(0);
                    }

                    this.recordings.add(image);
                    this.playback = -1;
                    this.tell("영상 녹화 " + this.recordings.size() + "건");
                }
            }
        }

        @Override
        protected BlockPos ring(BlockPos source, String key, int depth) {
            return this.away && key.equals("front_door") && depth <= 3 && this.source(source) && this.switchboard() instanceof ReceiverBlockEntity receiver
                ? receiver.ring(source, key)
                : super.ring(source, key, depth);
        }

        @Override
        public boolean startRinging(BlockPos source, String key) {
            boolean success = super.startRinging(source, key);
            if (success && this.away && key.equals("front_door")) {
                this.record(source);
            }

            return success;
        }

        @Override
        public void press(String key, ServerPlayer player, boolean remote) {
            if (!remote) {
                switch (key) {
                    case "cover":
                        this.coverOpen = !this.coverOpen;
                        this.tell(this.coverOpen ? "하단 덮개 열림" : "하단 덮개 닫힘");
                        return;
                    case "guard":
                        if (this.callState != CallState.RINGING && this.callState != CallState.CONNECTED) {
                            this.audio = true;
                            this.pressCore("guard", player, false);
                            return;
                        }

                        this.talk();
                        return;
                    case "door_open":
                        if (this.callState != CallState.CONNECTED || !this.audio || !this.incomingKey.equals("front_door") && !this.incomingKey.equals("lobby")
                            )
                         {
                            this.tell("현관·공동현관 통화 중에 문을 열 수 있습니다");
                        } else {
                            this.openDoor();
                        }

                        return;
                    case "talk_front":
                        if (this.callState != CallState.IDLE) {
                            this.talk();
                            return;
                        } else if (this.monitor > 0) {
                            this.startDoorTalk();
                            return;
                        } else {
                            if (this.cameraLink() == null) {
                                this.tell("연결된 현관 카메라가 없습니다");
                                return;
                            }

                            this.monitor = 600;
                            this.tell("현관 영상 확인 · 다시 누르면 통화");
                            return;
                        }
                    case "away":
                        super.press("away", player, false);
                        return;
                    case "emergency":
                        super.press("emergency", player, false);
                        return;
                    case "intercom":
                        if (this.internal == 1) {
                            this.finish();
                            this.tell("내선 호출 종료");
                        } else {
                            this.requestInternal(false);
                        }

                        return;
                    case "record":
                        BlockPos camera = this.camera();
                        if (this.callState == CallState.CONNECTED && camera != null) {
                            this.record(camera);
                            return;
                        } else {
                            if (this.recordings.isEmpty()) {
                                this.tell("녹화된 영상이 없습니다");
                                return;
                            }

                            this.playback = this.playback < 0
                                ? this.recordings.size() - 1
                                : (this.playback - 1 + this.recordings.size()) % this.recordings.size();
                            this.tell("녹화 영상 재생 " + (this.playback + 1) + "/" + this.recordings.size());
                            return;
                        }
                    case "meter":
                        if (this.playback >= 0) {
                            this.recordings.remove(this.playback);
                            this.playback = -1;
                            this.tell("선택한 영상 삭제");
                        } else {
                            this.tell("화면에 표시된 저장 영상이 없습니다");
                        }

                        return;
                    case "erase_all":
                        this.recordings.clear();
                        this.playback = -1;
                        this.tell("저장 영상 전체 삭제");
                        return;
                    case "function":
                        if (this.playback >= 0) {
                            this.playback = -1;
                            this.tell("녹화 영상 재생 종료");
                            return;
                        }

                        this.functionMode = !this.functionMode;
                        this.tell(this.functionMode ? "기능 설정 · 현관 영상 중 +/-로 밝기 조절" : "기능 설정 종료");
                        return;
                    case "security":
                        super.press("security", player, false);
                        return;
                    case "stop_restore":
                        super.press(this.mute ? "restore" : "stop", player, false);
                        return;
                    case "plus":
                    case "minus":
                        if (this.functionMode && (this.callState != CallState.IDLE || this.monitor > 0)) {
                            this.brightness = Math.max(0, Math.min(4, this.brightness + (key.equals("plus") ? 1 : -1)));
                            this.tell("화면 밝기 " + this.brightness);
                        } else {
                            super.press(key.equals("plus") ? "up" : "down", player, false);
                        }

                        return;
                    case "test":
                        this.alarmTest = (this.alarmTest + 1) % 3;
                        if (this.alarmTest == 0) {
                            this.restore();
                            return;
                        }

                        this.test = true;
                        this.alarm = this.alarmTest == 1 ? "fire" : "gas";
                        this.alarmTime = 0;
                        this.tell(this.alarmTest == 1 ? "화재 시험" : "가스 시험");
                        return;
                    case "ringtone":
                        this.ringtone = (this.ringtone + 1) % 21;
                        this.tell("착신음 " + (this.ringtone + 1) + "/21");
                        return;
                    case "gas_close":
                        this.gasClosed = true;
                        this.tell("가스밸브 닫힘 표시");
                        return;
                    default:
                        super.press(key, player, false);
                }
            }
        }

        int frontLamps() {
            int bits = this.lights();
            int result = 0;
            if ((bits & 64) != 0) {
                result |= 1;
            }

            if ((bits & 32) != 0) {
                result |= 2;
            }

            if ((bits & 8) != 0) {
                result |= 4;
            }

            if ((bits & 16) != 0) {
                result |= 8;
            }

            return result;
        }

        int innerLamps() {
            int result = 0;
            if (this.internal > 0) {
                result |= 1;
            }

            if (!this.recordings.isEmpty()) {
                result |= 2;
            }

            if (this.functionMode) {
                result |= 8;
            }

            if (this.armed) {
                result |= 16;
            }

            if (this.mute) {
                result |= 32;
            }

            if (this.test) {
                result |= 64;
            }

            return result;
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.putBoolean("705Cover", this.coverOpen);
            tag.putBoolean("705Function", this.functionMode);
            tag.putBoolean("705Gas", this.gasClosed);
            tag.putInt("705Tune", this.ringtone);
            tag.putInt("705Test", this.alarmTest);
            tag.putInt("705Playback", this.playback);
            ListTag frames = new ListTag();

            for (int[] frame : this.recordings) {
                frames.add(new IntArrayTag(frame));
            }

            tag.put("705Frames", frames);
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            this.coverOpen = tag.getBoolean("705Cover");
            this.functionMode = tag.getBoolean("705Function");
            this.gasClosed = tag.getBoolean("705Gas");
            this.ringtone = tag.getInt("705Tune");
            this.alarmTest = tag.getInt("705Test");
            this.playback = tag.contains("705Playback") ? tag.getInt("705Playback") : -1;
            this.recordings.clear();
            ListTag frames = tag.getList("705Frames", 11);

            for (int i = 0; i < frames.size(); i++) {
                int[] frame = frames.getIntArray(i);
                if (frame.length == 300) {
                    this.recordings.add(frame);
                }
            }
        }

        @Override
        public CompoundTag getUpdateTag() {
            CompoundTag tag = this.saveWithoutMetadata();
            BlockPos camera = this.camera();
            if (camera != null) {
                tag.putLong("Video", camera.asLong());
            }

            tag.putInt("Lights", this.lights());
            return tag;
        }

        public ClientboundBlockEntityDataPacket getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }

        @Override
        public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
            if (packet.getTag() != null) {
                this.load(packet.getTag());
            }
        }
    }
}
