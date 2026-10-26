package com.qwerty.homenet.sht;

import com.qwerty.homenet.analog.AnalogBus;
import com.qwerty.homenet.analog.AnalogLobbyEndpoint;
import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.blockentity.CallerBlockEntity;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.data.ZoneData;
import com.qwerty.homenet.guard.GuardSystem;
import com.qwerty.homenet.intercom.DoorControl;
import com.qwerty.homenet.intercom.Intercom;
import com.qwerty.homenet.item.InsertCardItem;
import com.qwerty.homenet.item.RfCardItem;
import com.qwerty.homenet.move.DeviceMoves;
import com.qwerty.homenet.move.DeviceOffsets;
import com.qwerty.homenet.video.VideoFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class Ht512 extends WallMountedBlock {
    public static final double HEIGHT = 0.583333375;
    public static final double WIDTH = 0.44371587868852463;
    public static final double DEPTH = 0.01912568442622951;

    public Ht512() {
        this(0.44371587868852463, 0.01912568442622951);
    }

    protected Ht512(double width, double depth) {
        super(Properties.of().strength(1.5F).noOcclusion(), width * 8.0, 3.3333329999999997, 12.666667, depth * 16.0);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos at, BlockState state) {
        return new Ht512.Unit(at, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        return l.isClientSide ? null : createTickerHelper(t, Product.HT512_TYPE.get(), (level, pos, state, unit) -> unit.serverTick());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos at, Player player, InteractionHand hand, BlockHitResult hit) {
        if (holdingLinker(player, hand)) {
            return InteractionResult.PASS;
        } else if (!(player.getItemInHand(hand).getItem() instanceof InsertCardItem) && !player.getItemInHand(hand).is(Product.S1_CARD.get())) {
            if (level.isClientSide) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Ht512Visual.open(at));
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        } else {
            if (player instanceof ServerPlayer sp && level.getBlockEntity(at) instanceof Ht512.Unit unit) {
                unit.insert(sp, player.getItemInHand(hand));
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    @Override
    public void onRemove(BlockState s, Level l, BlockPos p, BlockState next, boolean moving) {
        if (!s.is(next.getBlock()) && l.getBlockEntity(p) instanceof Ht512.Unit u) {
            u.onBroken();
        }

        super.onRemove(s, l, p, next, moving);
    }

    public static class Unit extends CallerBlockEntity implements AnalogLobbyEndpoint {
        String dong = "101";
        String lobby = "01";
        String guard = "";
        String adminPin = "0825";
        String accessPin = "1234";
        String roomMin = "00";
        String roomMax = "99";
        String input = "";
        String segmentValue = "";
        String screen = "대기";
        String menu = "";
        String cardAction = "";
        boolean secret;
        boolean guardDial;
        final Set<String> cards = new HashSet<>();
        int idleTicks;
        int doorTicks;
        int openSeconds = 7;
        boolean callLamp;
        boolean heldOpen;

        public Unit(BlockPos pos, BlockState state) {
            this(Product.HT512_TYPE.get(), pos, state);
        }

        protected Unit(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state);
        }

        @Override
        public DeviceRegistry.Kind kind() {
            return DeviceRegistry.Kind.LOBBY_PHONE;
        }

        @Override
        public String registeredUnit() {
            return this.dong + "-" + this.lobby;
        }

        @Override
        public String callerKey() {
            return "lobby";
        }

        @Override
        protected int openTicks() {
            return this.openSeconds * 20;
        }

        @Override
        public void syncScreens() {
            if (this.level != null && !this.level.isClientSide) {
                this.callLamp = this.isInCall();
            }

            this.setChanged();
            if (this.level != null) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            }
        }

        @Override
        public void openDoors() {
            super.openDoors();
            if (this.level != null && !this.level.isClientSide) {
                this.doorTicks = this.openTicks();
                this.syncScreens();
            }
        }

        @Override
        public boolean supports(AnalogBus bus) {
            return bus == AnalogBus.COMMON_8_WIRE;
        }

        @Override
        public String manufacturer() {
            return "SAMSUNG";
        }

        @Override
        public String videoNumber() {
            return this.registeredUnit();
        }

        @Override
        public BlockPos switchboardPosition() {
            return null;
        }

        @Override
        public BlockPos cameraPosition() {
            return this.worldPosition;
        }

        public VideoFormat videoFormat() {
            return VideoFormat.MONOCHROME;
        }

        @Override
        public AABB getRenderBoundingBox() {
            return new AABB(this.worldPosition).move(DeviceOffsets.get(this.level, this.worldPosition));
        }

        @Override
        public void onLoad() {
            super.onLoad();
            if (this.level != null && !this.level.isClientSide) {
                DeviceMoves.applyPending(this);
            }
        }

        @Override
        public List<ReceiverBlockEntity> receiversOf(String unit) {
            return super.receiversOf(unit)
                .stream()
                .filter(r -> r.guardSystem() == GuardSystem.BUS && ZoneData.connected(this.level, this.worldPosition, r.getBlockPos()))
                .toList();
        }

        void message(String text) {
            this.screen = text;
            this.syncScreens();
        }

        void serverTick() {
            this.tickCaller();
            if (this.heldOpen && this.level instanceof ServerLevel && this.level.getGameTime() % 20L == 0L) {
                this.openDoors();
            }

            if (this.doorTicks > 0 && --this.doorTicks == 0) {
                this.syncScreens();
            }

            if (++this.idleTicks > 200) {
                if (!this.menu.isEmpty() || !this.input.isEmpty() || !this.segmentValue.isEmpty() && !this.isInCall()) {
                    this.menu = "";
                    this.input = "";
                    this.segmentValue = "";
                    this.secret = false;
                    this.guardDial = false;
                    this.message("대기");
                }

                this.idleTicks = 0;
            }
        }

        public void key(ServerPlayer player, String key) {
            if (Intercom.inReach(player, this.worldPosition)) {
                this.idleTicks = 0;
                if (this.isInCall() && key.equals("#")) {
                    this.hangUpFromCaller();
                    this.message("통화 종료");
                } else if (key.equals("경비실")) {
                    this.menu = "";
                    this.guardDial = true;
                    this.input = "";
                    this.segmentValue = "0000";
                    this.message("경비실 : 0000 · 호출");
                } else if (key.equals("호출")) {
                    if (this.menu.isEmpty()) {
                        if (this.guardDial) {
                            this.callGuard();
                        } else if (this.input.isEmpty()) {
                            this.message("호실번호를 입력하세요");
                        } else {
                            String room = this.input;
                            this.segmentValue = room;
                            this.input = "";
                            if (room.length() > 4) {
                                this.message("ERR");
                            } else {
                                int last = Integer.parseInt(room.substring(Math.max(0, room.length() - 2)));
                                if (last >= Integer.parseInt(this.roomMin) && last <= Integer.parseInt(this.roomMax)) {
                                    CallerBlockEntity.CallResult result = this.startCall(this.dong + "-" + room);
                                    this.message(
                                        result == CallerBlockEntity.CallResult.OK
                                            ? room + "호 호출중"
                                            : (result == CallerBlockEntity.CallResult.BUSY ? "BUSY" : "ERR")
                                    );
                                } else {
                                    this.message("호출 범위 밖");
                                }
                            }
                        }
                    }
                } else if (key.equals("#")) {
                    if (this.menu.equals("prefix")) {
                        this.reset();
                    } else if (!this.menu.equals("secret")) {
                        if (this.menu.startsWith("edit:")) {
                            this.saveEdit();
                        } else if (!this.menu.isEmpty()) {
                            this.reset();
                        } else if (!this.input.isEmpty()) {
                            this.reset();
                        } else {
                            this.menu = "prefix";
                            this.segmentValue = "    ";
                            this.message("* → 비밀번호 4자리");
                        }
                    } else {
                        if (this.input.length() == 4 && this.input.equals(this.accessPin)) {
                            this.openDoors();
                            this.doorTicks = this.openTicks();
                            this.message("문열림 · " + this.openSeconds + "초");
                        } else {
                            this.message("비밀번호 오류");
                        }

                        this.resetInput();
                    }
                } else if (key.equals("*")) {
                    if (this.menu.equals("prefix")) {
                        this.menu = "secret";
                        this.secret = true;
                        this.input = "";
                        this.segmentValue = "    ";
                        this.message("비밀번호 4자리");
                    }
                } else if (key.matches("[0-9]")) {
                    if (this.menu.equals("secret")) {
                        if (this.input.length() < 4) {
                            this.input = this.input + key;
                        }

                        this.segmentValue = " ".repeat(4 - this.input.length()) + "-".repeat(this.input.length());
                        if (this.input.length() == 4 && this.input.equals(this.adminPin)) {
                            this.menu = "admin";
                            this.input = "";
                            this.secret = false;
                            this.message("관리 1 비밀번호 / 2 번호 / 3 카드 / 4 문 조작");
                        } else {
                            this.message("비밀번호 입력");
                        }
                    } else if (this.menu.equals("admin")) {
                        this.menu = switch (key) {
                            case "1" -> "password";
                            case "2" -> "numbers";
                            case "3" -> "cards";
                            case "4" -> "door_menu";
                            default -> "admin";
                        };
                        String var8 = this.menu;

                        this.message(switch (var8) {
                            case "password" -> "1 관리 / 2 출입";
                            case "numbers" -> "1 동 / 2 로비 / 3 경비 / 4 시작호 / 5 끝호";
                            case "cards" -> "1 등록 / 2 삭제 / 3 전체삭제";
                            case "door_menu" -> "문 조작 1 열림시간 / 2 계속 개문 / 3 자동운전";
                            default -> "1 비밀번호 / 2 번호 / 3 카드 / 4 문 조작";
                        });
                    } else if (this.menu.equals("door_menu")) {
                        if (key.equals("1")) {
                            this.menu = "edit:door_time";
                            this.input = "";
                            this.message("문열림 시간(초) 입력 후 #");
                        } else if (key.equals("2")) {
                            this.heldOpen = true;
                            this.openDoors();
                            this.message("계속 개문 상태");
                            this.resetInput();
                        } else if (key.equals("3")) {
                            this.heldOpen = false;
                            if (this.level instanceof ServerLevel server) {
                                for (BlockPos at : this.getLinkedDoors()) {
                                    if (server.isLoaded(at)) {
                                        DoorControl.setOpen(server, at, false);
                                    }
                                }
                            }

                            this.message("자동운전 상태");
                            this.resetInput();
                        }
                    } else if (this.menu.equals("password")) {
                        if (key.equals("1") || key.equals("2")) {
                            this.menu = "edit:" + (key.equals("1") ? "admin" : "access");
                            this.input = "";
                            this.message("새 4자리 입력 후 #");
                        }
                    } else if (this.menu.equals("numbers")) {
                        String field = switch (key) {
                            case "1" -> "dong";
                            case "2" -> "lobby";
                            case "3" -> "guard";
                            case "4" -> "min";
                            case "5" -> "max";
                            default -> "";
                        };
                        if (!field.isEmpty()) {
                            this.menu = "edit:" + field;
                            this.input = "";
                            this.message("번호 입력 후 #");
                        }
                    } else if (this.menu.equals("cards")) {
                        if (key.equals("3")) {
                            this.cards.clear();
                            this.reset();
                            this.message("카드 전체삭제");
                        } else if (key.equals("1") || key.equals("2")) {
                            this.cardAction = key.equals("1") ? "add" : "remove";
                            this.menu = "card";
                            ItemStack held = player.getMainHandItem().getItem() instanceof InsertCardItem || player.getMainHandItem().is(Product.S1_CARD.get())
                                ? player.getMainHandItem()
                                : (
                                    !(player.getOffhandItem().getItem() instanceof InsertCardItem) && !player.getOffhandItem().is(Product.S1_CARD.get())
                                        ? ItemStack.EMPTY
                                        : player.getOffhandItem()
                                );
                            if (!held.isEmpty()) {
                                this.insert(player, held);
                            } else {
                                this.message("카드를 손에 들거나 삽입하세요");
                            }
                        }
                    } else if (this.menu.startsWith("edit:")) {
                        if (this.input.length() < 8) {
                            this.input = this.input + key;
                        }

                        this.message(
                            "입력: " + (!this.menu.equals("edit:admin") && !this.menu.equals("edit:access") ? this.input : "*".repeat(this.input.length()))
                        );
                    } else {
                        if (this.input.length() < 4) {
                            this.input = this.input + key;
                        }

                        this.segmentValue = this.input;
                        this.message("호실 " + this.input);
                    }
                }
            }
        }

        void callGuard() {
            this.guardDial = false;
            this.input = "";
            if (this.level instanceof ServerLevel sl) {
                if (this.guard.isEmpty()) {
                    this.message("지정 경비실 번호를 설정하세요");
                } else {
                    List<ReceiverBlockEntity> list = DeviceRegistry.get(sl)
                        .guardsByNumber(sl, this.worldPosition, this.guard)
                        .stream()
                        .filter(p -> ZoneData.connected(sl, this.worldPosition, p) && sl.isLoaded(p))
                        .map(sl::getBlockEntity)
                        .filter(b -> b instanceof ReceiverBlockEntity)
                        .map(b -> (ReceiverBlockEntity)b)
                        .filter(r -> r.guardSystem() == GuardSystem.BUS)
                        .toList();
                    if (list.isEmpty()) {
                        this.message("경비실 연결 실패");
                    } else {
                        CallerBlockEntity.CallResult result = this.startReceivers(list);
                        this.message(result == CallerBlockEntity.CallResult.OK ? "경비실 호출중" : "경비실 통화중");
                    }
                }
            }
        }

        void saveEdit() {
            String field = this.menu.substring(5);
            String value = this.input;
            boolean good = value.matches("[0-9]{1,8}");
            if (field.equals("admin") || field.equals("access")) {
                good = value.matches("[0-9]{4}") && !value.equals(field.equals("admin") ? this.accessPin : this.adminPin);
            }

            if (field.equals("min") || field.equals("max")) {
                good = value.matches("[0-9]{2}");
            }

            if (field.equals("door_time")) {
                good = value.matches("[0-9]{1,2}") && Integer.parseInt(value) >= 1;
            }

            if (!good) {
                this.message("설정값 오류");
            } else {
                switch (field) {
                    case "admin":
                        this.adminPin = value;
                        break;
                    case "access":
                        this.accessPin = value;
                        break;
                    case "dong":
                        this.dong = value;
                        break;
                    case "lobby":
                        this.lobby = value;
                        break;
                    case "guard":
                        this.guard = value;
                        break;
                    case "min":
                        this.roomMin = value;
                        break;
                    case "max":
                        this.roomMax = value;
                        break;
                    case "door_time":
                        this.openSeconds = Integer.parseInt(value);
                }

                this.reset();
                this.message("설정 저장");
                this.registerDevice();
            }
        }

        void resetInput() {
            this.menu = "";
            this.input = "";
            this.segmentValue = "";
            this.secret = false;
            this.guardDial = false;
        }

        void reset() {
            this.resetInput();
            this.message("대기");
        }

        void insert(ServerPlayer player, ItemStack stack) {
            String id = cardId(stack);
            if (id != null) {
                if (this.menu.equals("card")) {
                    if (this.cardAction.equals("add")) {
                        this.cards.add(id);
                        this.message("카드 등록");
                    } else {
                        this.cards.remove(id);
                        this.message("카드 삭제");
                    }

                    this.resetInput();
                } else {
                    if (this.cards.contains(id)) {
                        this.openDoors();
                        this.doorTicks = this.openTicks();
                        this.message("카드 인증 · 문열림");
                    } else {
                        this.message("등록되지 않은 카드");
                    }
                }
            }
        }

        static String cardId(ItemStack stack) {
            if (stack.is(Product.S1_CARD.get())) {
                return S1Card.id(stack);
            } else if (stack.getItem() instanceof InsertCardItem) {
                CompoundTag tag = stack.getOrCreateTag();
                if (!tag.contains("CardId")) {
                    tag.putString("CardId", UUID.randomUUID().toString());
                }

                return tag.getString("CardId");
            } else {
                return RfCardItem.cardId(stack);
            }
        }

        public String display() {
            return this.secret ? "*".repeat(this.input.length()) : this.screen;
        }

        String segmentText() {
            if (this.segmentValue.isEmpty()) {
                return "----";
            } else {
                return this.segmentValue.length() > 4 ? this.segmentValue.substring(this.segmentValue.length() - 4) : this.segmentValue;
            }
        }

        @Override
        protected void saveAdditional(CompoundTag t) {
            super.saveAdditional(t);
            t.putString("Dong", this.dong);
            t.putString("Lobby", this.lobby);
            t.putString("Guard", this.guard);
            t.putString("AdminPin", this.adminPin);
            t.putString("AccessPin", this.accessPin);
            t.putString("RoomMin", this.roomMin);
            t.putString("RoomMax", this.roomMax);
            t.putString("Display", this.screen);
            t.putString("Segment", this.segmentValue);
            t.putInt("DoorTicks", this.doorTicks);
            t.putBoolean("CallLamp", this.callLamp);
            t.putInt("DoorOpenSeconds", this.openSeconds);
            t.putBoolean("DoorHeldOpen", this.heldOpen);
            t.putString("Cards", String.join(",", this.cards));
        }

        @Override
        public void load(CompoundTag t) {
            super.load(t);
            if (t.contains("Dong")) {
                this.dong = t.getString("Dong");
            }

            if (t.contains("Lobby")) {
                this.lobby = t.getString("Lobby");
            }

            this.guard = t.getString("Guard");
            if (t.contains("AdminPin")) {
                this.adminPin = t.getString("AdminPin");
            }

            if (t.contains("AccessPin")) {
                this.accessPin = t.getString("AccessPin");
            }

            if (t.contains("RoomMin")) {
                this.roomMin = t.getString("RoomMin");
            }

            if (t.contains("RoomMax")) {
                this.roomMax = t.getString("RoomMax");
            }

            this.screen = t.getString("Display");
            if (t.contains("Input")) {
                this.input = t.getString("Input");
            }

            this.segmentValue = t.getString("Segment");
            this.doorTicks = t.getInt("DoorTicks");
            this.callLamp = t.getBoolean("CallLamp");
            this.openSeconds = t.contains("DoorOpenSeconds") ? Math.max(1, Math.min(99, t.getInt("DoorOpenSeconds"))) : 7;
            this.heldOpen = t.getBoolean("DoorHeldOpen");
            this.cards.clear();
            if (!t.getString("Cards").isEmpty()) {
                this.cards.addAll(Arrays.asList(t.getString("Cards").split(",")));
            }
        }

        @Override
        public CompoundTag getUpdateTag() {
            CompoundTag t = super.getUpdateTag();
            t.putString("Display", this.screen);
            t.putString("Input", this.input);
            t.putString("Segment", this.segmentValue);
            t.putInt("DoorTicks", this.doorTicks);
            t.putBoolean("CallLamp", this.isInCall());
            return t;
        }

        public ClientboundBlockEntityDataPacket getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }
    }
}
