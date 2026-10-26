package com.qwerty.homenet.blockentity;

import com.qwerty.homenet.analog.AnalogBus;
import com.qwerty.homenet.analog.AnalogLobbyEndpoint;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.data.ZoneData;
import com.qwerty.homenet.guard.GuardExchange;
import com.qwerty.homenet.guard.GuardSystem;
import com.qwerty.homenet.intercom.CallSounds;
import com.qwerty.homenet.intercom.DoorControl;
import com.qwerty.homenet.intercom.DoorStatus;
import com.qwerty.homenet.intercom.Intercom;
import com.qwerty.homenet.intercom.IntercomCaller;
import com.qwerty.homenet.intercom.MissedCall;
import com.qwerty.homenet.intercom.UnitAddress;
import com.qwerty.homenet.registry.ModBlockEntities;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class Kam305BlockEntity extends Am10kpBlockEntity {
    private int angle = 0;
    private int menuIndex;
    private int recordPage;
    private int doorSeconds = 3;
    private int voiceVolume = 2;
    private int relayId;
    private String page = "main";
    private String input = "";
    private String pin = "ffff";
    private String language = "ko";
    private String dialGroup = "SD";
    private String notice = "";
    private String field = "";
    private boolean handsfree;
    private long enterStarted = -1L;
    private UUID enterPlayer;
    private String recoverySequence = "";
    private boolean monitorMode;
    private boolean setupId;
    private boolean officeMode;
    private boolean selfEmergency;
    private UUID administrator;
    private long adminUntil;
    private long clockOffset = 946684800L;
    private BlockPos lobbyPeer;
    private int lobbyTicks;
    private final LinkedHashMap<String, BlockPos> lobbies = new LinkedHashMap<>();
    private final Map<String, String> lobbyPins = new HashMap<>();
    private final Map<String, String> videoRanges = new HashMap<>();
    private final Map<BlockPos, Set<String>> safetyTypes = new HashMap<>();
    private final List<MissedCall> history = new ArrayList<>();
    private final List<MissedCall> alarmHistory = new ArrayList<>();
    private final Map<BlockPos, Long> closingDoors = new HashMap<>();
    private String[] clientLcd = new String[8];
    private int factoryTicks;
    private boolean factoryRequested;
    private boolean clockInitialized;
    private final Map<BlockPos, Kam305BlockEntity.DeviceSlot> deviceSlots = new HashMap<>();
    private final List<BlockPos> deviceResults = new ArrayList<>();
    private boolean filteredDevices;
    private final List<BlockPos> peerResults = new ArrayList<>();
    private String queriedModel = "";
    private String queriedAddress = "";
    private int deviceChoice;
    private int deviceIndex;
    private String deviceAction = "status";
    private BlockPos editingDevice;
    private int formCursor;
    private String[] formValues = new String[0];
    private boolean formTyping;
    private String returnPage = "admin";
    private static final String[] MENU = new String[]{"부재 설정", "세대번호 관리", "세대암호 관리", "로비 관리", "경비실 관리", "시각 관리", "감시모드 설정", "비상데이터 관리", "언어 설정", "음량 조절"};
    private static final String[] GUARD_MENU = new String[]{
        "경비실 현재번호 수정", "경비실 비밀번호 수정", "동[로비] ID 관리", "주장치 상호 ID 관리", "세대 버전 정보", "중계기 라인 체크", "경비실 마스터 초기화", "중계기 ID 변경", "세대통신 검사 입력", "공동현관문 제어", "로비 영상 제어"
    };
    private int clientHighlightStart = -1;
    private int clientHighlightLength = -1;
    private int clientSelection = -1;
    private String clientPage = "main";

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide && !this.clockInitialized) {
            this.clockOffset = 946684800L - this.level.getGameTime() / 20L;
            this.clockInitialized = true;
            this.setChanged();
        }
    }

    private void startFactoryReset() {
        if (this.selfEmergency) {
            this.selfEmergency = false;
            this.broadcastEmergency();
        }

        this.hangUp();

        for (BlockPos pos : new ArrayList<>(this.homes.keySet())) {
            ReceiverBlockEntity r = this.home(pos);
            if (r != null && this.worldPosition.equals(r.getAnalogStation())) {
                r.unlinkSwitchboard();
            } else {
                this.onUnlinked(pos);
            }
        }

        this.homes.clear();
        this.lobbies.clear();
        this.lobbyPins.clear();
        this.videoRanges.clear();
        this.deviceSlots.clear();
        this.history.clear();
        this.alarmHistory.clear();
        this.alarms.clear();
        this.safetyTypes.clear();
        this.guardAlerts.clear();
        this.unit = "99";
        this.pin = "ffff";
        this.absentTarget = null;
        this.absent = this.setupId = this.officeMode = this.monitorMode = false;
        this.volume = this.voiceVolume = 2;
        this.relayId = 0;
        this.language = "ko";
        this.angle = 0;
        this.clockOffset = 946684800L - this.level.getGameTime() / 20L;
        this.clockInitialized = true;
        this.factoryRequested = false;
        this.factoryTicks = 700;
        this.power = false;
        this.page = "factory";
        this.register();
        this.changed();
    }

    private Kam305BlockEntity.DeviceSlot slot(BlockPos p) {
        return this.deviceSlots.getOrDefault(p, new Kam305BlockEntity.DeviceSlot(this.homes.containsKey(p) ? 0 : 1, this.relayId, 1, 1, "ffff"));
    }

    private String deviceNumber(BlockPos p) {
        if (this.homes.containsKey(p)) {
            return this.homes.get(p);
        } else {
            for (Entry<String, BlockPos> e : this.lobbies.entrySet()) {
                if (e.getValue().equals(p) && !e.getKey().endsWith("-0")) {
                    return e.getKey();
                }
            }

            return "";
        }
    }

    private void searchSlots() {
        int main = Integer.parseInt(this.formValue(0));
        int card = Integer.parseInt(this.formValue(1));
        int channel = Integer.parseInt(this.formValue(2));
        this.deviceResults.clear();

        for (BlockPos p : this.allDevicePositions()) {
            Kam305BlockEntity.DeviceSlot s = this.slot(p);
            if (s.main == main && s.card == card && s.channel == channel) {
                this.deviceResults.add(p);
            }
        }

        this.filteredDevices = true;
        this.deviceIndex = 0;
        this.page = "device_list";
    }

    private List<BlockPos> allDevicePositions() {
        ArrayList<BlockPos> result = new ArrayList<>(this.homes.keySet());

        for (Entry<String, BlockPos> e : this.lobbies.entrySet()) {
            if (!e.getKey().endsWith("-0") && !result.contains(e.getValue())) {
                result.add(e.getValue());
            }
        }

        return result;
    }

    private boolean deleteRegisteredDevice(ServerPlayer p, BlockPos pos) {
        if (this.homes.containsKey(pos)) {
            return this.manageDevices(p, "DELETE|" + this.homes.get(pos) + "|CONFIRM");
        } else if (!this.validLobby(pos)) {
            return false;
        } else {
            ArrayList<String> ids = new ArrayList<>();

            for (Entry<String, BlockPos> e : this.lobbies.entrySet()) {
                if (e.getValue().equals(pos) && !e.getKey().endsWith("-0")) {
                    ids.add(e.getKey());
                }
            }

            ids.forEach(id -> {
                this.lobbies.remove(id);
                this.lobbyPins.remove(id);
            });
            this.deviceSlots.remove(pos);
            return !ids.isEmpty();
        }
    }

    private void queryPeers() {
        this.peerResults.clear();
        int main = Integer.parseInt(this.formValue(0));
        if (this.level instanceof ServerLevel sl) {
            for (DeviceRegistry.Entry e : DeviceRegistry.get(sl).all()) {
                Am10kpBlockEntity b = this.board(e.pos());
                if (b != null && (b instanceof Kam305BlockEntity k ? k.relayId : 0) == main) {
                    this.peerResults.add(e.pos());
                }
            }
        }

        this.peerResults.sort(Comparator.comparingInt(p -> {
            try {
                return Integer.parseInt(this.board(p).getUnit());
            } catch (Exception var3) {
                return 0;
            }
        }));
        this.deviceIndex = 0;
        this.page = "peer_list";
    }

    private boolean commitPeerForm() {
        try {
            if (this.field.equals("peer_search")) {
                this.queryPeers();
                return true;
            }

            if (this.field.equals("peer_change") && !this.peerResults.isEmpty()) {
                Am10kpBlockEntity b = this.board(this.peerResults.get(Math.floorMod(this.deviceIndex, this.peerResults.size())));
                String id = this.formValue(0);
                if (b != null && id.matches("[0-9]{1,3}") && b.occupiedChannels() <= 0 && b.guardPeer == null && (this.byId(id) == null || this.byId(id) == b)) {
                    b.unit = id;
                    b.register();
                    b.changed();
                    this.page = "peer_list";
                    return true;
                }

                return false;
            }
        } catch (Exception var3) {
        }

        this.status = "번호·연결 상태를 확인하세요";
        return false;
    }

    private void queryVersion() {
        this.queriedAddress = this.formValue(1) + "-" + this.formValue(2);
        this.queriedModel = "통신 불량";
        int type = Integer.parseInt(this.formValue(0));
        if (type == 0) {
            for (Entry<BlockPos, String> e : this.homes.entrySet()) {
                if (normalize(e.getValue()).equals(normalize(this.queriedAddress)) && this.home(e.getKey()) instanceof Kvm906BlockEntity k) {
                    this.queriedModel = k.design().model().label();
                }
            }
        } else if (type == 1) {
            Am10kpBlockEntity b = this.byId(this.formValue(1));
            if (b != null) {
                this.queriedModel = b.modelId();
            }
        } else if (type == 2) {
            BlockPos pos = this.lobbies.get(normalize(this.queriedAddress));
            if (pos != null && this.validLobby(pos)) {
                this.queriedModel = "KOCOM 로비폰";
            }
        } else if (type == 3 && Integer.parseInt(this.formValue(1)) == this.relayId) {
            this.queriedModel = "KAM-305 주장치";
        }

        this.page = "version_result";
    }

    private List<BlockPos> devicePositions() {
        return (List<BlockPos>)(this.filteredDevices ? new ArrayList<>(this.deviceResults) : this.allDevicePositions());
    }

    private void deviceMenu(ServerPlayer p) {
        switch (this.deviceChoice) {
            case 0:
                this.deviceAction = "delete";
                this.beginForm("device_search", "device_menu");
                break;
            case 1:
                this.deviceAction = "register";
                this.filteredDevices = false;
                this.page = "device_list";
                this.deviceIndex = 0;
                break;
            case 2:
                this.deviceAction = "status";
                this.beginForm("device_search", "device_menu");
                break;
            case 3:
                this.beginForm("device_range", "device_menu");
                break;
            case 4:
                this.beginForm("device_clear", "device_menu");
        }

        this.changed();
    }

    private void selectDevice(ServerPlayer p) {
        if (this.authenticated(p)) {
            List<BlockPos> items = this.devicePositions();
            if (items.isEmpty()) {
                this.status = "등록된 장치기 없음";
                this.changed();
            } else {
                this.editingDevice = items.get(Math.floorMod(this.deviceIndex, items.size()));
                if (this.deviceAction.equals("delete")) {
                    this.beginForm("device_delete", "device_list");
                } else if (this.deviceAction.equals("register")) {
                    this.beginForm("device_register", "device_list");
                } else {
                    this.page = "device_status";
                }

                this.changed();
            }
        }
    }

    private boolean saveDeviceForm(ServerPlayer p) {
        if (!this.authenticated(p)) {
            return false;
        } else {
            String name = this.field;

            try {
                if (name.equals("device_search")) {
                    this.searchSlots();
                    if (!this.deviceAction.equals("status") && this.formValues.length > 3 && this.formValue(3).equals("2")) {
                        this.beginForm("device_scope_delete", "device_list");
                    }

                    return true;
                }

                if (name.equals("device_scope_delete")) {
                    if (!this.formValue(0).equals("1")) {
                        return false;
                    }

                    for (BlockPos pos : new ArrayList<>(this.deviceResults)) {
                        this.deleteRegisteredDevice(p, pos);
                    }

                    this.deviceResults.clear();
                    return true;
                }

                if (name.equals("device_register")) {
                    int group = bounded(this.formValue(0), 0, 2);
                    int main = bounded(this.formValue(2), 0, 99);
                    int card = bounded(this.formValue(3), 0, 99);
                    int channel = bounded(this.formValue(4), 1, 2);
                    if (main == this.relayId && group != 2) {
                        ReceiverBlockEntity target = this.home(this.editingDevice);
                        String address = target != null && target.roomOnlyUnit() ? this.formValue(6) : normalize(this.formValue(5) + "-" + this.formValue(6));
                        if (group == 0) {
                            if (target == null) {
                                throw new IllegalArgumentException();
                            }

                            String pos = this.editingDevice.getX() + "," + this.editingDevice.getY() + "," + this.editingDevice.getZ();
                            if (!this.manageDevices(p, "REGISTER|" + pos + "|" + address + "|" + this.formValue(1))) {
                                return false;
                            }

                            if (this.board(target.getAnalogStation()) instanceof Kam305BlockEntity k) {
                                k.deviceSlots.put(this.editingDevice, new Kam305BlockEntity.DeviceSlot(group, main, card, channel, this.formValue(7)));
                            }
                        } else {
                            if (!this.validLobby(this.editingDevice) || !this.formValue(1).equals(this.unit)) {
                                throw new IllegalArgumentException();
                            }

                            this.lobbies.entrySet().removeIf(e -> e.getValue().equals(this.editingDevice) && !e.getKey().endsWith("-0"));
                            this.lobbies.put(address, this.editingDevice);
                            this.deviceSlots.put(this.editingDevice, new Kam305BlockEntity.DeviceSlot(group, main, card, channel, ""));
                        }

                        return true;
                    }

                    throw new IllegalArgumentException();
                }

                if (name.equals("device_delete")) {
                    if (!this.formValue(0).equals("1")) {
                        return false;
                    }

                    return this.deleteRegisteredDevice(p, this.editingDevice);
                }

                if (name.equals("device_range")) {
                    int main = bounded(this.formValue(2), 0, 99);
                    int card = bounded(this.formValue(3), 0, 99);
                    int channel = bounded(this.formValue(4), 1, 2);
                    int sf = bounded(this.formValue(5), 0, 99);
                    int ef = bounded(this.formValue(6), sf, 99);
                    int sl = bounded(this.formValue(7), 1, 99);
                    int el = bounded(this.formValue(8), sl, 99);
                    int dong = bounded(this.formValue(9), 1, 9999);
                    if (main == this.relayId && this.formValue(0).equals("0") && this.formValue(1).equals(this.unit)) {
                        ArrayList<BlockPos> targets = new ArrayList<>();

                        for (BlockPos posx : this.homes.keySet()) {
                            Kam305BlockEntity.DeviceSlot meta = this.slot(posx);
                            if (meta.main == main && meta.card == card && meta.channel == channel) {
                                targets.add(posx);
                            }
                        }

                        if (targets.size() > (ef - sf + 1) * (el - sl + 1)) {
                            throw new IllegalArgumentException();
                        }

                        for (BlockPos posxx : targets) {
                            ReceiverBlockEntity r = this.home(posxx);
                            if (r == null || r.isBusy()) {
                                throw new IllegalArgumentException();
                            }
                        }

                        int i = 0;

                        for (BlockPos posxxx : targets) {
                            String n = dong + "-" + ((sf + i / (el - sl + 1)) * 100 + sl + i % (el - sl + 1));
                            if (this.home(posxxx).roomOnlyUnit()) {
                                n = n.substring(n.indexOf("-") + 1);
                            }

                            if (!this.home(posxxx).assignUnitFromSwitchboard(this.worldPosition, n)) {
                                throw new IllegalArgumentException();
                            }

                            this.homes.put(posxxx, n);
                            i++;
                        }

                        return true;
                    }

                    throw new IllegalArgumentException();
                }

                if (name.equals("device_clear")) {
                    if (this.formValue(0).equals(this.relayId + "") && this.formValue(1).equals("1")) {
                        return this.manageDevices(p, "CLEAR|CONFIRM");
                    }

                    return false;
                }
            } catch (Exception var16) {
                this.status = "입력·연결 상태를 확인하세요";
            }

            return false;
        }
    }

    private String[] formLabels() {
        String var1 = this.field;

        return switch (var1) {
            case "peer_search" -> new String[]{"주장치 ID"};
            case "peer_change" -> new String[]{"변경할 경비/상호 ID"};
            case "version_lookup" -> new String[]{"장치기 종류", "동/경비실 번호", "호 번호"};
            case "forward" -> new String[]{"변경 우회경비실 번호"};
            case "house" -> new String[]{"변경할 동 번호", "변경할 호 번호", "관할 경비실 번호", "세대 옵션 번호"};
            case "house_pin" -> new String[]{"변경 세대 비밀번호"};
            case "guard_id" -> new String[]{"변경 경비실 번호", "SETUP ID"};
            case "guard_pin" -> new String[]{"변경 경비실 비밀번호"};
            case "monitor" -> new String[]{"감시 모드"};
            case "language" -> new String[]{"언어"};
            case "volume" -> new String[]{"벨소리 크기", "통화음 크기"};
            case "clock" -> new String[]{"년", "월", "일", "시", "분", "초"};
            case "relay" -> new String[]{"주장치 ID"};
            case "door_time" -> new String[]{"공동현관문 열림 시간"};
            case "reset" -> new String[]{"경비실 비밀번호", "초기화 확인"};
            case "lobby" -> new String[]{"동 번호", "로비 번호", "비밀번호"};
            case "device_search" -> this.deviceAction.equals("status")
                ? new String[]{"주장치 ID", "카드번호", "채널번호"}
                : new String[]{"주장치 ID", "카드번호", "채널번호", "검색/전체삭제"};
            case "device_scope_delete" -> new String[]{"전체 삭제 확인"};
            case "device_register" -> new String[]{"그룹", "경비 번호", "주장치", "카드번호", "채널번호", "동 번호", "호 번호", "세대 옵션 번호"};
            case "device_delete" -> new String[]{"삭제 확인"};
            case "device_clear" -> new String[]{"주장치 ID", "전체 삭제 확인"};
            case "device_range" -> new String[]{"그룹", "경비 번호", "주장치", "카드번호", "채널번호", "시작 층", "끝 층", "시작 라인", "끝 라인", "지정할 동 번호"};
            case "video" -> new String[]{"시작 동", "시작 층", "시작 라인", "끝 동", "끝 층", "끝 라인", "영상 번호"};
            default -> new String[]{"입력"};
        };
    }

    private void beginForm(String name, String parent) {
        this.field = name;
        this.returnPage = parent;
        this.page = "form";
        this.input = "";
        this.formCursor = 0;
        this.formTyping = false;
        this.formValues = new String[this.formLabels().length];
        Arrays.fill(this.formValues, "");
        String var3 = this.field;
        switch (var3) {
            case "lobby":
                List<String> ids = this.lobbies.keySet().stream().filter(id -> !id.endsWith("-0")).toList();
                if (!ids.isEmpty()) {
                    String id = ids.get(Math.floorMod(this.deviceIndex, ids.size()));
                    String[] n = id.split("-");
                    this.formValues[0] = n[0];
                    this.formValues[1] = n[1];
                    this.formValues[2] = this.lobbyPins.getOrDefault(id, "");
                }
                break;
            case "house": {
                ReceiverBlockEntity r = this.activeHome();
                if (r != null) {
                    String[] a = normalize(r.getUnit()).split("-");
                    if (a.length == 2) {
                        this.formValues[0] = a[0];
                        this.formValues[1] = a[1];
                    } else if (r.roomOnlyUnit()) {
                        this.formValues[0] = "";
                        this.formValues[1] = r.getUnit();
                        this.formCursor = 1;
                    }

                    Am10kpBlockEntity origin = this.board(r.getAnalogStation());
                    this.formValues[2] = origin == null ? this.unit : origin.getUnit();
                }

                this.formValues[3] = "ffff";
                break;
            }
            case "guard_id":
                this.formValues[0] = this.unit;
                this.formValues[1] = this.setupId ? "1" : "0";
                break;
            case "peer_search":
                this.formValues[0] = this.relayId + "";
                break;
            case "peer_change":
                this.formValues[0] = this.peerResults.isEmpty()
                    ? ""
                    : this.board(this.peerResults.get(Math.floorMod(this.deviceIndex, this.peerResults.size()))).getUnit();
                break;
            case "version_lookup":
                this.formValues = new String[]{"0", "0", "0"};
                break;
            case "guard_pin":
                this.formValues[0] = "";
                break;
            case "monitor":
                this.formValues[0] = this.monitorMode ? "1" : "0";
                break;
            case "language":
                this.formValues[0] = this.language;
                break;
            case "volume":
                this.formValues[0] = this.volume + "";
                this.formValues[1] = this.voiceVolume + "";
                break;
            case "clock":
                LocalDateTime t = LocalDateTime.ofEpochSecond(this.level.getGameTime() / 20L + this.clockOffset, 0, ZoneOffset.UTC);
                this.formValues = new String[]{
                    t.getYear() + "", t.getMonthValue() + "", t.getDayOfMonth() + "", t.getHour() + "", t.getMinute() + "", t.getSecond() + ""
                };
                break;
            case "house_pin": {
                ReceiverBlockEntity r = this.activeHome();
                this.formValues[0] = r == null ? "" : r.getDoorPassword();
                break;
            }
            case "device_search":
                this.formValues[0] = this.relayId + "";
                this.formValues[1] = "1";
                this.formValues[2] = "1";
                if (this.formValues.length > 3) {
                    this.formValues[3] = "1";
                }
                break;
            case "device_scope_delete":
                this.formValues[0] = "0";
                break;
            case "device_register":
                String[] n = this.deviceNumber(this.editingDevice).isEmpty() ? new String[]{"0", "0"} : this.deviceNumber(this.editingDevice).split("-");
                if (n.length == 1) {
                    n = new String[]{"", n[0]};
                }

                Kam305BlockEntity.DeviceSlot meta = this.slot(this.editingDevice);
                this.formValues = new String[]{meta.group + "", this.unit, this.relayId + "", meta.card + "", meta.channel + "", n[0], n[1], meta.option};
                break;
            case "device_delete":
                this.formValues[0] = "0";
                break;
            case "device_clear":
                this.formValues[0] = this.relayId + "";
                this.formValues[1] = "0";
                break;
            case "device_range":
                this.formValues = new String[]{"0", this.unit, this.relayId + "", "1", "1", "1", "1", "1", "1", "0"};
                break;
            case "relay":
                this.formValues[0] = this.relayId + "";
                break;
            case "door_time":
                this.formValues[0] = this.doorSeconds + "";
                break;
            case "forward":
                this.formValues[0] = this.board(this.absentTarget) == null ? "0" : this.board(this.absentTarget).getUnit();
                break;
            case "reset":
                this.formValues[0] = !this.pin.equals("ffff") && !this.pin.isEmpty() ? "" : this.pin;
                this.formValues[1] = "0";
                this.factoryRequested = false;
                if (this.pin.equals("ffff") || this.pin.isEmpty()) {
                    this.formCursor = 1;
                }
        }

        this.status = "입력[0~9] 선택[확인] 나가기[*]";
    }

    private String formValue(int i) {
        return i < this.formValues.length ? this.formValues[i] : "";
    }

    private int fieldLimit() {
        String var1 = this.field;

        return switch (var1) {
            case "house" -> this.formCursor == 2 ? 3 : 4;
            case "guard_id", "forward" -> 3;
            case "house_pin", "guard_pin" -> 8;
            case "clock" -> this.formCursor == 0 ? 4 : 2;
            case "video" -> this.formCursor != 0 && this.formCursor != 3 && this.formCursor != 6 ? 2 : 4;
            default -> 4;
        };
    }

    private boolean choiceField() {
        return Set.of("monitor", "language", "volume", "door_time").contains(this.field)
            || this.field.equals("guard_id") && this.formCursor == 1
            || this.field.equals("reset") && this.formCursor == 1
            || this.field.equals("device_register") && this.formCursor == 0
            || this.field.equals("device_range") && this.formCursor == 0
            || this.field.equals("device_search") && this.formCursor == 3
            || this.field.equals("device_scope_delete")
            || this.field.equals("device_delete")
            || this.field.equals("device_clear") && this.formCursor == 1
            || this.field.equals("version_lookup") && this.formCursor == 0;
    }

    private boolean skipDongField(int index) {
        return this.field.equals("house") && index == 0 && this.activeHome() != null && this.activeHome().roomOnlyUnit()
            || this.field.equals("device_register") && index == 5 && this.home(this.editingDevice) != null && this.home(this.editingDevice).roomOnlyUnit();
    }

    private void editFormKey(String key) {
        if (!this.skipDongField(this.formCursor)) {
            if (this.formValues.length == 0) {
                this.beginForm(this.field, this.returnPage);
            }

            if (!this.choiceField()) {
                if (!this.formTyping) {
                    this.formValues[this.formCursor] = "";
                    this.formTyping = true;
                }

                if (this.formValues[this.formCursor].length() < this.fieldLimit()) {
                    this.formValues[this.formCursor] = this.formValues[this.formCursor] + key;
                }

                this.input = this.formValues[this.formCursor];
            }
        }
    }

    private void changeForm(int direction) {
        if (this.formValues.length != 0) {
            String old = this.formValue(this.formCursor);
            if (this.field.equals("language")) {
                List<String> langs = List.of("ko", "en", "zh");
                this.formValues[0] = langs.get(Math.floorMod(langs.indexOf(old) + direction, 3));
            } else if (!this.choiceField() && !this.field.equals("clock")) {
                while (true) {
                    this.formCursor = Math.floorMod(this.formCursor + direction, this.formValues.length);
                    if (!this.skipDongField(this.formCursor)) {
                        this.formTyping = false;
                        break;
                    }
                }
            } else {
                int value;
                try {
                    value = Integer.parseInt(old);
                } catch (Exception var6) {
                    value = 0;
                }

                int lo = this.field.equals("device_search")
                    ? 1
                    : (this.field.equals("door_time") ? 1 : (this.field.equals("clock") ? (this.formCursor < 3 ? 1 : 0) : 0));
                int hi = this.field.equals("door_time")
                    ? 10
                    : (
                        this.field.equals("volume")
                            ? 2
                            : (
                                this.field.equals("version_lookup")
                                    ? 4
                                    : (
                                        this.field.equals("device_search") || this.field.equals("device_register") || this.field.equals("device_range")
                                            ? 2
                                            : (this.field.equals("clock") ? new int[]{2099, 12, 31, 23, 59, 59}[this.formCursor] : 1)
                                    )
                            )
                    );
                if (this.field.equals("clock") && this.formCursor == 0) {
                    lo = 2000;
                }

                this.formValues[this.formCursor] = Math.max(lo, Math.min(hi, value + direction)) + "";
            }

            this.input = this.formValues[this.formCursor];
            this.changed();
        }
    }

    private void confirmForm(ServerPlayer p) {
        if (!this.authenticated(p)) {
            this.status = "관리자 인증 필요";
        } else {
            if (this.field.equals("reset")) {
                if (this.formCursor == 0) {
                    if (!this.formValue(0).equals(this.pin)) {
                        this.status = "비밀번호 불일치";
                        this.changed();
                        return;
                    }

                    this.formCursor = 1;
                    this.changed();
                    return;
                }

                if (!this.formValue(1).equals("1")) {
                    this.page = "guard";
                    this.field = "";
                    this.factoryRequested = false;
                    this.changed();
                    return;
                }

                if (this.factoryRequested) {
                    this.startFactoryReset();
                    return;
                }
            }

            if (this.field.equals("guard_id") && this.formCursor == 0) {
                String oldField = this.field;
                String parent = this.returnPage;
                if (this.saveForm(p, this.formValue(0) + "|" + (this.setupId ? "1" : "0"))) {
                    this.field = oldField;
                    this.returnPage = parent;
                    this.page = "form";
                    this.formCursor = 1;
                    this.formTyping = false;
                }

                this.changed();
            } else if (this.formCursor < this.formValues.length - 1) {
                this.formCursor++;
                if (this.skipDongField(this.formCursor)) {
                    this.formCursor++;
                }

                this.formTyping = false;
                this.input = this.formValues[this.formCursor];
                this.changed();
            } else if (this.field.startsWith("peer_")) {
                this.commitPeerForm();
                this.changed();
            } else if (this.field.equals("version_lookup")) {
                try {
                    this.queryVersion();
                } catch (Exception var10) {
                    this.status = "입력 값을 확인하세요";
                }

                this.changed();
            } else if (this.field.startsWith("device_")) {
                String submittedField = this.field;
                if (this.saveDeviceForm(p) && !submittedField.equals("device_search")) {
                    this.page = "saved";
                    this.notice = "장치기 관리 완료";
                }

                this.changed();
            } else {
                try {
                    String parent = this.field;

                    String value = switch (parent) {
                        case "house" -> (
                                this.activeHome() != null && this.activeHome().roomOnlyUnit() ? this.formValue(1) : this.formValue(0) + "-" + this.formValue(1)
                            )
                            + "|"
                            + this.formValue(2);
                        case "guard_id", "volume" -> this.formValue(0) + "|" + this.formValue(1);
                        case "clock" -> String.format(
                            "%04d-%02d-%02d %02d:%02d:%02d",
                            Integer.parseInt(this.formValue(0)),
                            Integer.parseInt(this.formValue(1)),
                            Integer.parseInt(this.formValue(2)),
                            Integer.parseInt(this.formValue(3)),
                            Integer.parseInt(this.formValue(4)),
                            Integer.parseInt(this.formValue(5))
                        );
                        case "reset" -> this.formValue(0) + "|" + (this.formValue(1).equals("1") ? "RESET" : "CANCEL");
                        case "video" -> this.formValue(0)
                            + ","
                            + this.formValue(1)
                            + ","
                            + this.formValue(2)
                            + "-"
                            + this.formValue(3)
                            + ","
                            + this.formValue(4)
                            + ","
                            + this.formValue(5)
                            + "|"
                            + this.formValue(6);
                        case "lobby" -> {
                            String address = normalize(this.formValue(0) + "-" + this.formValue(1));
                            BlockPos pos = this.lobbies.get(address);
                            if (pos == null && this.lobbyPeer != null) {
                                pos = this.lobbyPeer;
                            }

                            if (pos == null && this.level instanceof ServerLevel sl) {
                                for (DeviceRegistry.Entry e : DeviceRegistry.get(sl).all()) {
                                    if (this.validLobby(e.pos())) {
                                        pos = e.pos();
                                        break;
                                    }
                                }
                            }

                            yield pos == null ? "" : address + "|" + this.formValue(2) + "|" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
                        }
                        default -> this.formValue(0);
                    };
                    parent = this.returnPage;
                    String savedField = this.field;
                    if (this.saveForm(p, value) && !savedField.equals("reset")) {
                        this.page = "saved";
                        this.returnPage = parent;
                        this.notice = savedField.equals("house") ? "세대 번호 변경 완료" : "변경 완료";
                    }
                } catch (Exception var11) {
                    this.status = "입력 값을 확인하세요";
                }

                this.changed();
            }
        }
    }

    public Kam305BlockEntity(BlockPos p, BlockState s) {
        super(ModBlockEntities.KAM_305.get(), p, s);
        this.unit = "99";
        Arrays.fill(this.clientLcd, "");
    }

    @Override
    public String modelId() {
        return "KAM-305";
    }

    @Override
    public int linkedCount() {
        return this.homes.size() + (int)this.lobbies.keySet().stream().filter(id -> !id.endsWith("-0")).count();
    }

    @Override
    public boolean canLink(BlockPos p) {
        return true;
    }

    @Override
    protected int channelLimit() {
        return 3;
    }

    @Override
    protected int guardLimit() {
        return 40;
    }

    @Override
    protected int alarmCapacity() {
        return 500;
    }

    @Override
    public boolean audioActive() {
        return this.lifted || this.handsfree;
    }

    public int screenAngle() {
        return this.angle;
    }

    public void adjustAngle(int delta) {
        this.angle = Math.max(0, Math.min(65, this.angle + Integer.signum(delta)));
        this.changed();
    }

    private boolean authenticated(ServerPlayer p) {
        return p != null && p.getUUID().equals(this.administrator) && this.level.getGameTime() <= this.adminUntil;
    }

    private long recordTime() {
        return this.clockOffset + (this.level == null ? 0L : this.level.getGameTime() / 20L);
    }

    private void remember(String type, String group, String address, String event) {
        MissedCall r = new MissedCall(type + "|" + group + "|" + address + "|" + event, this.recordTime());
        this.history.add(0, r);

        while (this.history.size() > 500) {
            this.history.remove(this.history.size() - 1);
        }

        this.setChanged();
    }

    @Override
    protected void forward(Am10kpBlockEntity.Channel c) {
        int count = this.occupiedChannels();
        super.forward(c);
        if (this.occupiedChannels() > count) {
            Am10kpBlockEntity origin = this.board(c.origin);
            this.remember("ROU", c.frontDoor == null ? "SD" : "LB", origin == null ? "" : origin.assignedUnit(c.home), "수신");
            this.page = "calls";
            this.changed();
        }
    }

    @Override
    public void callFrom(ReceiverBlockEntity r, ServerPlayer p) {
        super.callFrom(r, p);
        if (this.find(r.getBlockPos()) >= 0) {
            Am10kpBlockEntity.Channel c = this.channels[this.find(r.getBlockPos())];
            if (!c.redirected && this.level instanceof ServerLevel sl) {
                for (DeviceRegistry.Entry e : DeviceRegistry.get(sl).all()) {
                    if (this.board(e.pos()) instanceof Kam305BlockEntity k
                        && k != this
                        && k.setupId
                        && k.relayId == this.relayId
                        && k.power
                        && !k.absent
                        && k.free() >= 0) {
                        k.forward(c);
                        k.page = "calls";
                        k.changed();
                        this.status = "SETUP 우회 -> " + k.getUnit();
                        break;
                    }
                }
            }

            this.remember(c.redirected ? "ROU" : "STD", "SD", r.getUnit(), "수신");
            this.page = "calls";
        }
    }

    @Override
    public boolean answerLine(int i) {
        boolean ok = super.answerLine(i);
        if (ok) {
            this.page = "calls";
        }

        return ok;
    }

    @Override
    public boolean onAnswered(BlockPos p) {
        boolean ok = super.onAnswered(p);
        if (ok) {
            this.page = "calls";
        }

        return ok;
    }

    @Override
    public void setSafetyEvent(BlockPos p, String n, String type, boolean active) {
        if (type.equals("BS") && this.home(p) instanceof Kvm906BlockEntity) {
            this.setEmergency(p, n, active);
        } else if (Set.of("BS", "FIR", "GAS", "BB1", "BB2", "BB3").contains(type) && this.valid(p) && !(this.home(p) instanceof Kvm906BlockEntity)) {
            Set<String> types = this.safetyTypes.computeIfAbsent(p.immutable(), ignored -> new LinkedHashSet<>());
            boolean old = types.contains(type);
            if (active) {
                if (this.alarms.containsKey(p) || this.alarms.size() < this.alarmCapacity()) {
                    types.add(type);
                    this.alarms.put(p.immutable(), n);
                    if (!old) {
                        this.muted = false;
                        this.page = "alarms";
                    }
                }
            } else {
                types.remove(type);
                if (types.isEmpty()) {
                    this.safetyTypes.remove(p);
                    this.alarms.remove(p);
                }
            }

            if (old != types.contains(type)) {
                this.alarmHistory.add(0, new MissedCall(type + "|" + n + "|" + (active ? "발생" : "해제"), this.recordTime()));

                while (this.alarmHistory.size() > 500) {
                    this.alarmHistory.remove(this.alarmHistory.size() - 1);
                }
            }

            this.changed();
        }
    }

    @Override
    public void onUnlinked(BlockPos p) {
        this.safetyTypes.remove(p);
        this.deviceSlots.remove(p);
        super.onUnlinked(p);
    }

    @Override
    public void setEmergency(BlockPos p, String n, boolean active) {
        if (this.valid(p) && !(this.home(p) instanceof Kvm906BlockEntity)) {
            this.setSafetyEvent(p, n, "BS", active);
        } else {
            boolean old = this.alarms.containsKey(p);
            super.setEmergency(p, n, active);
            if (old != this.alarms.containsKey(p)) {
                MissedCall r = new MissedCall("BS|" + n + "|" + (active ? "발생" : "해제"), this.recordTime());
                this.alarmHistory.add(0, r);

                while (this.alarmHistory.size() > 500) {
                    this.alarmHistory.remove(this.alarmHistory.size() - 1);
                }

                if (active) {
                    this.page = "alarms";
                    this.recordPage = 0;
                }

                this.changed();
            }
        }
    }

    @Override
    public BlockPos routeFrontDoor(ReceiverBlockEntity r, BlockPos camera) {
        if (this.absent && this.board(this.absentTarget) instanceof Kam305BlockEntity target && !target.officeMode) {
            this.status = "영상 우회는 관리실에만 가능";
            this.changed();
            return null;
        } else {
            BlockPos result = super.routeFrontDoor(r, camera);
            if (result != null) {
                this.remember("ROU", "SD", r.getUnit(), "현관 우회");
            }

            return result;
        }
    }

    @Override
    public void hangUp() {
        boolean exchanged = this.transfer;
        super.hangUp();
        this.handsfree = false;
        this.enterStarted = -1L;
        if (this.lobbyPeer != null && this.level.getBlockEntity(this.lobbyPeer) instanceof IntercomCaller lp) {
            lp.onReceiverHangUp(this.worldPosition, DoorStatus.ENDED);
        }

        this.lobbyPeer = null;
        if (exchanged) {
            this.remember("EXC", "SD", "", "교환 완료");
        }

        this.page = "main";
        this.input = "";
        this.field = "";
        this.administrator = null;
        this.changed();
    }

    @Override
    public boolean hasCallerSession(BlockPos p) {
        return p.equals(this.lobbyPeer) || super.hasCallerSession(p);
    }

    @Override
    public void endCallerSession(BlockPos p, boolean missed) {
        if (p.equals(this.lobbyPeer)) {
            this.lobbyPeer = null;
            this.page = "main";
            this.changed();
        } else {
            super.endCallerSession(p, missed);
        }
    }

    @Override
    public void onReceiverHangUp(BlockPos p, DoorStatus reason) {
        if (p.equals(this.lobbyPeer)) {
            this.lobbyPeer = null;
            this.page = "main";
            this.changed();
        } else {
            super.onReceiverHangUp(p, reason);
        }
    }

    @Override
    public boolean startRinging(BlockPos from, String key) {
        if (ZoneData.connected(this.level, this.worldPosition, from)
            && this.power
            && this.lobbyPeer == null
            && this.level.getBlockEntity(from) instanceof AnalogLobbyEndpoint lp
            && this.worldPosition.equals(lp.switchboardPosition())
            && lp.supports(AnalogBus.COMMON_8_WIRE)
            && this.guardSystem().compatible(this.manufacturer(), GuardSystem.BUS, lp.manufacturer())) {
            this.lobbyPeer = from.immutable();
            this.lobbyTicks = 0;
            this.page = "lobby_ring";
            this.status = "로비 호출";
            this.remember("STD", "LB", from.toShortString(), "수신");
            this.changed();
            return true;
        } else {
            return false;
        }
    }

    private void answerLobby() {
        if (this.lifted
            && this.lobbyPeer != null
            && this.level.getBlockEntity(this.lobbyPeer) instanceof IntercomCaller lp
            && lp.onAnswered(this.worldPosition)) {
            this.page = "lobby";
            this.status = "로비 통화";
            this.changed();
        }
    }

    @Override
    protected void sendMessage(ServerPlayer p, String text) {
        if (this.lifted
            && this.lobbyPeer != null
            && this.page.equals("lobby")
            && this.level instanceof ServerLevel sl
            && this.level.getBlockEntity(this.lobbyPeer) instanceof IntercomCaller lp) {
            Intercom.say(sl, this, lp, p, "guard:" + this.unit, text);
        } else {
            super.sendMessage(p, text);
        }
    }

    private void dial() {
        if (!this.lifted) {
            this.handsfree = true;
        }

        if (!this.guardAlerts.isEmpty() && this.recordPage >= this.alarms.size()) {
            Am10kpBlockEntity target = this.board(
                new ArrayList<>(this.guardAlerts.keySet()).get(Math.floorMod(this.recordPage - this.alarms.size(), this.guardAlerts.size()))
            );
            if (target != null) {
                this.mutual = true;
                this.digits = target.getUnit();
                super.dialDigits();
                this.page = "calls";
            }
        } else if (!this.alarms.isEmpty()) {
            super.dialDigits();
            this.page = "calls";
        } else if (this.dialGroup.equals("LB")) {
            BlockPos p = this.lobbies.get(this.input);
            if (p == null && this.input.matches("[0-9]{8}")) {
                p = this.lobbies.get(Integer.parseInt(this.input.substring(0, 4)) + "-" + Integer.parseInt(this.input.substring(4)));
            }

            if (p != null && this.validLobby(p) && this.level.getBlockEntity(p) instanceof CallerBlockEntity lp && lp.acceptIncoming(this.worldPosition)) {
                this.lobbyPeer = p;
                this.page = "lobby";
                this.lobbyTicks = 0;
                this.remember("STD", "LB", this.input, "발신");
                this.input = "";
                this.changed();
            } else {
                this.status = "등록된 로비 없음 / 통화 중";
            }
        } else {
            if (!this.dialGroup.equals("GB") && !this.dialGroup.equals("SH")) {
                this.mutual = false;
                String[] parts = this.input.split("-", 2);
                if (parts.length == 2) {
                    this.building = parts[0];
                    this.digits = parts[1];
                } else if (this.input.matches("[0-9]{8}")) {
                    this.building = this.input.substring(0, 4);
                    this.digits = this.input.substring(4);
                } else {
                    this.building = "";
                    this.digits = this.input;
                }
            } else {
                this.mutual = true;
                this.digits = (this.input.matches("[0-9]{8}") ? this.input.substring(4) : this.input).replaceFirst("^0+(?!$)", "");
                this.building = "";
            }

            int beforeLines = this.occupiedChannels();
            BlockPos beforePeer = this.guardPeer;
            super.dialDigits();
            if (this.occupiedChannels() > beforeLines || this.guardPeer != null && !this.guardPeer.equals(beforePeer)) {
                this.remember(this.transfer ? "EXC" : "STD", this.dialGroup, this.input, "발신");
                this.page = "calls";
                this.input = "";
            }

            this.changed();
        }
    }

    @Override
    protected String selectedAlarmUnit() {
        return this.alarms.isEmpty() ? "" : new ArrayList<>(this.alarms.values()).get(Math.floorMod(this.recordPage, this.alarms.size()));
    }

    @Override
    protected boolean mayCallGuardDuringAlarm() {
        return this.recordPage >= this.alarms.size() && !this.guardAlerts.isEmpty();
    }

    public void registerLobbyPanel(Klp203rfBlockEntity panel) {
        String id = panel.lobbyAddress();
        String pin = null;

        for (Entry<String, BlockPos> e : new HashMap<>(this.lobbies).entrySet()) {
            if (e.getValue().equals(panel.getBlockPos()) && !e.getKey().endsWith("-0")) {
                if (pin == null) {
                    pin = this.lobbyPins.get(e.getKey());
                }

                if (!e.getKey().equals(id)) {
                    this.lobbies.remove(e.getKey());
                    this.lobbyPins.remove(e.getKey());
                }
            }
        }

        boolean update = !panel.getBlockPos().equals(this.lobbies.get(id));
        this.lobbies.put(id, panel.getBlockPos().immutable());
        if (pin != null) {
            this.lobbyPins.putIfAbsent(id, pin);
        }

        if (update) {
            this.changed();
        }
    }

    public void removeLobbyPanel(BlockPos pos) {
        boolean removed = false;

        for (Entry<String, BlockPos> e : new HashMap<>(this.lobbies).entrySet()) {
            if (e.getValue().equals(pos)) {
                this.lobbies.remove(e.getKey());
                this.lobbyPins.remove(e.getKey());
                removed = true;
            }
        }

        if (removed) {
            this.changed();
        }
    }

    public boolean checkPanelPassword(Klp203rfBlockEntity panel, String attempted) {
        if (panel.exchange() == this && panel.guardNumber().equals(this.unit)) {
            String pin = panel.getBlockPos().equals(this.lobbies.get(panel.lobbyAddress())) ? this.lobbyPins.get(panel.lobbyAddress()) : null;
            if (pin == null) {
                pin = this.lobbyPins.get(panel.dong() + "-0");
            }

            return pin != null && pin.equals(attempted);
        } else {
            return false;
        }
    }

    public String lobbyPanelSettings(BlockPos pos) {
        return this.level != null && this.level.getBlockEntity(pos) instanceof Klp203rfBlockEntity panel ? panel.configSummary() : "";
    }

    private boolean validLobby(BlockPos p) {
        return ZoneData.connected(this.level, this.worldPosition, p)
            && this.level.isLoaded(p)
            && this.level.getBlockEntity(p) instanceof AnalogLobbyEndpoint lp
            && this.worldPosition.equals(lp.switchboardPosition())
            && lp.supports(AnalogBus.COMMON_8_WIRE)
            && this.guardSystem().compatible(this.manufacturer(), GuardSystem.BUS, lp.manufacturer());
    }

    private ReceiverBlockEntity activeHome() {
        if (this.selected >= 0
            && this.channels[this.selected] != null
            && this.channels[this.selected].connected
            && this.channels[this.selected].frontDoor == null) {
            ReceiverBlockEntity r = this.home(this.channels[this.selected].home);
            return r != null && r.hasCallerSession(this.channels[this.selected].origin) ? r : null;
        } else {
            return null;
        }
    }

    private void openLobbyDoor() {
        if (this.lobbyPeer != null
            && this.page.equals("lobby")
            && this.validLobby(this.lobbyPeer)
            && this.level.getBlockEntity(this.lobbyPeer) instanceof CallerBlockEntity lp
            && lp.isConnected()) {
            if (this.level instanceof ServerLevel sl) {
                for (BlockPos p : lp.getLinkedDoors()) {
                    if (sl.isLoaded(p)) {
                        DoorControl.setOpen(sl, p, true);
                        this.closingDoors.put(p, sl.getGameTime() + this.doorSeconds * 20);
                    }
                }
            }

            if (this.monitorMode) {
                this.remember(
                    "OPN",
                    "LB",
                    this.lobbies
                        .entrySet()
                        .stream()
                        .filter(e -> e.getValue().equals(this.lobbyPeer) && !e.getKey().endsWith("-0"))
                        .map(Entry::getKey)
                        .findFirst()
                        .orElse(""),
                    "문열림"
                );
            }

            if (lp instanceof Klp203rfBlockEntity panel) {
                panel.onDoorOpened(this.worldPosition);
            } else {
                lp.onReceiverHangUp(this.worldPosition, DoorStatus.ENDED);
            }

            this.lobbyPeer = null;
            this.page = "main";
            this.changed();
        } else {
            this.status = "로비 통화 중에만 개폐 가능";
            this.changed();
        }
    }

    public void button(String name, ServerPlayer player) {
        if (this.factoryTicks <= 0 || name.equals("close")) {
            label366:
            switch (name) {
                case "power":
                    if (this.selfEmergency) {
                        this.selfEmergency = false;
                        this.broadcastEmergency();
                    }

                    super.mode("power");
                    break;
                case "close":
                    this.administrator = null;
                    this.enterStarted = -1L;
                    if (Set.of("admin", "guard", "form", "pin", "saved", "notice").contains(this.page)) {
                        this.page = this.occupiedChannels() > 0 ? "calls" : "main";
                    }
                    break;
                case "handset":
                    this.administrator = null;
                    if (this.lifted) {
                        this.hangUp();
                    } else {
                        this.lifted = true;
                        this.handsfree = false;
                        if (this.page.equals("lobby_ring")) {
                            this.answerLobby();
                        } else {
                            this.page = this.occupiedChannels() <= 0 && this.guardPeer == null ? "dial" : "calls";
                            if (this.guardPeer != null) {
                                super.mode("mutual");
                            } else {
                                for (int ix = 0; ix < this.channelLimit(); ix++) {
                                    if (this.channels[ix] != null && !this.channels[ix].connected && this.channels[ix].incoming) {
                                        this.answerLine(ix);
                                        break label366;
                                    }
                                }
                            }
                        }
                    }
                    break;
                case "admin":
                    this.page = "pin";
                    this.input = "";
                    this.field = "admin";
                    this.administrator = null;
                    this.status = "경비실 비밀번호 입력";
                    if (this.pin.isEmpty() || this.pin.equals("ffff")) {
                        this.authenticate(player, this.pin);
                    }
                    break;
                case "absent":
                    this.page = "pin";
                    this.input = "";
                    this.field = "absence";
                    this.administrator = null;
                    this.status = "부재기능을 실행합니다. 비밀번호를 입력하세요!";
                    if (this.pin.isEmpty() || this.pin.equals("ffff")) {
                        this.authenticate(player, this.pin);
                    }
                    break;
                case "settings":
                    this.page = this.page.equals("settings") ? "main" : "settings";
                    break;
                case "exchange":
                    super.mode("exchange");
                    this.page = "dial";
                    this.input = "";
                    this.dialGroup = "SD";
                    break;
                case "f1":
                    String var10 = this.dialGroup;

                    this.dialGroup = switch (var10) {
                        case "SD" -> "GB";
                        case "GB" -> "SH";
                        case "SH" -> "LB";
                        default -> "SD";
                    };
                    this.page = "dial";
                    this.input = "";
                    break;
                case "f2":
                    if (!this.page.equals("records") && !this.page.equals("archive")) {
                        this.page = "clock";
                    } else {
                        this.returnPage = this.page;
                        this.page = "notice";
                        this.notice = "프린터 연결 없음";
                    }
                    break;
                case "f3":
                    this.page = "records";
                    this.recordPage = 0;
                    break;
                case "up":
                case "down":
                    int d = name.equals("up") ? -1 : 1;
                    if (this.page.equals("form")) {
                        this.changeForm(-d);
                        return;
                    }

                    if (this.page.equals("peer_list")) {
                        this.deviceIndex = Math.floorMod(this.deviceIndex + d, Math.max(1, this.peerResults.size()));
                    } else if (this.page.equals("device_menu")) {
                        this.deviceChoice = Math.floorMod(this.deviceChoice + d, 5);
                    } else if (this.page.equals("device_list") || this.page.equals("lobby_ids")) {
                        this.deviceIndex = Math.floorMod(
                            this.deviceIndex + d, Math.max(1, this.page.equals("lobby_ids") ? this.lobbies.size() : this.devicePositions().size())
                        );
                    } else if (this.page.equals("absence") && !this.officeMode) {
                        this.absent = name.equals("up");
                        this.status = this.absent ? "경비실 부재 설정" : "경비실 부재 해제";
                    } else if (this.page.equals("admin") || this.page.equals("guard")) {
                        this.menuIndex = Math.floorMod(this.menuIndex + d, this.page.equals("guard") ? GUARD_MENU.length : MENU.length);
                    } else if (this.page.equals("calls")) {
                        for (int j = 1; j <= 3; j++) {
                            int ix = Math.floorMod(this.selected + d * j, 3);
                            if (this.channels[ix] != null) {
                                if (this.lifted) {
                                    this.answerLine(ix);
                                } else {
                                    this.selected = ix;
                                }
                                break label366;
                            }
                        }
                    } else {
                        int count = this.page.equals("alarms")
                            ? this.alarmCount()
                            : (
                                this.page.equals("devices")
                                    ? (this.homes.size() + 4) / 5
                                    : (this.page.equals("archive") ? (this.alarmHistory.size() + 4) / 5 : (this.history.size() + 4) / 5)
                            );
                        this.recordPage = Math.max(0, Math.min(Math.max(0, count - 1), this.recordPage + d));
                    }
                    break;
                case "enter":
                    if (this.page.equals("pin")) {
                        this.authenticate(player, this.input);
                    } else if (this.page.equals("admin") && this.authenticated(player)) {
                        this.selectAdmin(this.menuIndex);
                    } else if (this.page.equals("guard") && this.authenticated(player)) {
                        this.selectGuard(this.menuIndex);
                    } else if (this.page.equals("calls")) {
                        if (this.guardPeer != null) {
                            super.mode("mutual");
                            return;
                        }

                        if (this.selected < 0) {
                            for (int i = 0; i < 3; i++) {
                                if (this.channels[i] != null) {
                                    this.selected = i;
                                    break;
                                }
                            }
                        }

                        this.answerLine(this.selected);
                    } else if (this.page.equals("lobby_ring")) {
                        this.answerLobby();
                    } else if (this.page.equals("alarms")) {
                        this.dial();
                    } else if (this.page.equals("dial")) {
                        this.dial();
                    } else if (this.page.equals("form")) {
                        this.confirmForm(player);
                    } else if (this.page.equals("saved") || this.page.equals("notice")) {
                        this.page = this.returnPage;
                        this.field = "";
                    } else if (this.page.equals("absence")) {
                        this.page = "main";
                        this.field = "";
                    } else if (this.page.equals("peers") && this.authenticated(player)) {
                        this.beginForm("peer_search", "guard");
                    } else if (this.page.equals("peer_list") && this.authenticated(player)) {
                        this.beginForm("peer_change", "peer_list");
                    } else if (this.page.equals("version") && this.authenticated(player)) {
                        this.beginForm("version_lookup", "guard");
                    } else if (this.page.equals("device_menu") && this.authenticated(player)) {
                        this.deviceMenu(player);
                    } else if (this.page.equals("device_list")) {
                        this.selectDevice(player);
                    } else if (this.page.equals("lobby_ids") && this.authenticated(player) && !this.lobbies.isEmpty()) {
                        String id = new ArrayList<>(this.lobbies.keySet()).get(Math.floorMod(this.deviceIndex, this.lobbies.size()));
                        this.lobbies.remove(id);
                        this.lobbyPins.remove(id);
                    } else if (this.page.equals("devices")) {
                        this.manageDevices(player, this.input);
                    }
                    break;
                case "emergency":
                    if (this.selfEmergency) {
                        this.selfEmergency = false;
                        this.broadcastEmergency();
                    } else if (this.alarms.isEmpty() && this.guardAlerts.isEmpty()) {
                        this.selfEmergency = true;
                        this.broadcastEmergency();
                    } else {
                        this.muted = true;
                        this.status = "비상음 정지 / 상황 유지";
                    }
                    break;
                case "cancel":
                    this.input = "";
                    if (this.transfer) {
                        super.cancelTransfer();
                        this.page = "calls";
                    } else {
                        this.page = this.authenticated(player) ? "admin" : "main";
                    }

                    this.field = "";
            }

            this.changed();
        }
    }

    private void authenticate(ServerPlayer p, String value) {
        if (p != null) {
            if (value.equals(this.pin)) {
                this.administrator = p.getUUID();
                this.adminUntil = Long.MAX_VALUE;
                if (this.field.equals("absence")) {
                    if (this.officeMode) {
                        this.status = "관리실은 부재 사용 불가";
                        this.page = "notice";
                        this.notice = this.status;
                        this.returnPage = "main";
                    } else {
                        this.status = "UP/DOWN 키로 설정/해제 선택";
                        this.page = "absence";
                    }
                } else {
                    this.page = "admin";
                    this.menuIndex = 0;
                    this.status = "관리자 메뉴";
                }
            } else {
                this.status = "비밀번호 불일치";
            }

            this.input = "";
            this.changed();
        }
    }

    private void selectAdmin(int i) {
        this.field = new String[]{"forward", "house", "house_pin", "lobby", "", "clock", "monitor", "", "language", "volume"}[i];
        if (i == 4) {
            this.page = "guard";
            this.menuIndex = 0;
        } else if (i == 7) {
            this.page = "archive";
            this.recordPage = 0;
        } else {
            this.beginForm(this.field, "admin");
        }
    }

    private void selectGuard(int i) {
        this.field = new String[]{"guard_id", "guard_pin", "lobby_delete", "peers", "version", "linecheck", "reset", "relay", "devices", "door_time", "video"}[i];

        this.page = switch (i) {
            case 2 -> "lobby_ids";
            case 3 -> "peers";
            case 4 -> "version";
            case 5 -> "linecheck";
            default -> "form";
            case 8 -> "device_menu";
        };
        this.input = "";
        if (this.page.equals("form")) {
            this.beginForm(this.field, "guard");
        }

        this.status = this.formHint();
    }

    private String formHint() {
        String var1 = this.field;

        return switch (var1) {
            case "forward" -> "우회 경비실 번호";
            case "house" -> "동·호·관할 경비번호 입력";
            case "house_pin" -> "새 세대 암호 (통화 중)";
            case "lobby" -> "동·로비 번호와 암호 입력";
            case "clock" -> "yyyy-MM-dd HH:mm";
            case "monitor" -> "1 설정 / 0 해제";
            case "language" -> "ko / en / zh";
            case "volume" -> "벨소리와 통화음 순서로 조절";
            case "guard_id" -> "번호 저장 후 SETUP ID 선택";
            case "guard_pin" -> "새 비밀번호 (4~16자)";
            case "lobby_delete" -> "삭제할 동-로비";
            case "reset" -> "비밀번호 입력 후 초기화 확인";
            case "relay" -> "주장치 ID 0~99";
            case "door_time" -> "문열림 시간 1~10초";
            case "video" -> "동,층,라인 시작-끝|영상번호";
            default -> "";
        };
    }

    private Am10kpBlockEntity byId(String id) {
        if (id.equals(this.unit)) {
            return this;
        } else {
            if (this.level instanceof ServerLevel sl) {
                for (DeviceRegistry.Entry e : DeviceRegistry.get(sl).all()) {
                    Am10kpBlockEntity b = this.board(e.pos());
                    if (b != null && b.getUnit().equals(id)) {
                        return b;
                    }
                }
            }

            return null;
        }
    }

    public boolean saveForm(ServerPlayer p, String value) {
        if (this.authenticated(p) && this.page.equals("form")) {
            String[] a = value.split("\\|", -1);

            try {
                String e = this.field;
                switch (e) {
                    case "forward":
                        if (a[0].equals("0")) {
                            this.absentTarget = null;
                            break;
                        } else {
                            Am10kpBlockEntity b = this.byId(a[0]);
                            if (b != null && b != this) {
                                this.absentTarget = b.getBlockPos();
                                break;
                            }

                            throw new IllegalArgumentException();
                        }
                    case "house": {
                        ReceiverBlockEntity r = this.activeHome();
                        if (r != null && a.length >= 2 && r.validAssignedUnit(a[0])) {
                            Am10kpBlockEntity destination = this.byId(a[1]);
                            if (destination != null && r.supportsSwitchboard(destination)) {
                                if (destination.homes
                                    .entrySet()
                                    .stream()
                                    .anyMatch(
                                        exx -> !((BlockPos)exx.getKey()).equals(r.getBlockPos()) && normalize((String)exx.getValue()).equals(normalize(a[0]))
                                    )) {
                                    throw new IllegalArgumentException();
                                }

                                String n = normalize(a[0]);
                                if (!destination.getBlockPos().equals(r.getAnalogStation()) && !r.bindSwitchboard(destination.getBlockPos())) {
                                    throw new IllegalArgumentException();
                                }

                                if (!r.assignUnitFromSwitchboard(destination.getBlockPos(), n)) {
                                    throw new IllegalArgumentException();
                                }

                                destination.onLinked(r.getBlockPos(), n);
                                break;
                            }

                            throw new IllegalArgumentException();
                        }

                        throw new IllegalArgumentException();
                                        }
case "house_pin":
                        ReceiverBlockEntity r = this.activeHome();
                        if (r != null && value.matches("[0-9]{4,8}")) {
                            r.assignDoorPasswordFromSwitchboard(r.getAnalogStation(), value);
                            break;
                        }

                        throw new IllegalArgumentException();
                    case "guard_id": {
                        if (this.officeMode && !a[0].equals("0")) {
                            throw new IllegalArgumentException();
                        }

                        if (!a[0].matches("[0-9]{1,3}")) {
                            throw new IllegalArgumentException();
                        }

                        Am10kpBlockEntity b = this.byId(a[0]);
                        if (b != null && b != this) {
                            throw new IllegalArgumentException();
                        }

                        this.unit = a[0];
                        this.setupId = a.length > 1 && a[1].equals("1");
                        this.officeMode = this.unit.equals("0");
                        this.register();
                        break;
                                        }
case "guard_pin":
                        if (value.length() >= 4 && value.length() <= 16 && !value.contains("|")) {
                            this.pin = value;
                            break;
                        }

                        throw new IllegalArgumentException();
                    case "monitor":
                        this.monitorMode = value.equals("1");
                        break;
                    case "relay":
                        this.relayId = bounded(value, 0, 99);
                        this.deviceSlots.replaceAll((pos, m) -> new Kam305BlockEntity.DeviceSlot(m.group, this.relayId, m.card, m.channel, m.option));
                        break;
                    case "door_time":
                        this.doorSeconds = bounded(value, 1, 10);
                        break;
                    case "volume":
                        if (a.length != 2) {
                            throw new IllegalArgumentException();
                        }

                        this.volume = bounded(a[0], 0, 2);
                        this.voiceVolume = bounded(a[1], 0, 2);
                        break;
                    case "language":
                        if (!Set.of("ko", "en", "zh").contains(value)) {
                            throw new IllegalArgumentException();
                        }

                        this.language = value;
                        break;
                    case "clock":
                        LocalDateTime date = LocalDateTime.parse(
                            value, DateTimeFormatter.ofPattern(value.length() > 16 ? "yyyy-MM-dd HH:mm:ss" : "yyyy-MM-dd HH:mm")
                        );
                        this.clockInitialized = true;
                        this.clockOffset = date.toEpochSecond(ZoneOffset.UTC) - this.level.getGameTime() / 20L;
                        if (this.level instanceof ServerLevel sl) {
                            for (DeviceRegistry.Entry ex : DeviceRegistry.get(sl).all()) {
                                if (this.board(ex.pos()) instanceof Kam305BlockEntity b && b.relayId == this.relayId) {
                                    b.clockOffset = this.clockOffset;
                                    b.clockInitialized = true;
                                    b.changed();
                                }
                            }
                        }
                        break;
                    case "lobby":
                        if (a.length == 3 && a[0].matches("[0-9]{1,4}-[0-9]{1,4}") && a[1].matches("[0-9]{4,8}")) {
                            String[] xyz = a[2].split(",");
                            if (xyz.length != 3) {
                                throw new IllegalArgumentException();
                            }

                            BlockPos pos = new BlockPos(Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2]));
                            String id = normalize(a[0]);
                            if (!this.validLobby(pos)) {
                                throw new IllegalArgumentException();
                            }

                            if (!id.endsWith("-0") && this.level.getBlockEntity(pos) instanceof Klp203rfBlockEntity panel) {
                                panel.applyBoardAddress(this, id);
                                this.removeLobbyPanel(pos);
                            }

                            this.lobbies.put(id, pos);
                            this.lobbyPins.put(id, a[1]);
                            break;
                        }

                        throw new IllegalArgumentException();
                    case "lobby_delete":
                        this.lobbies.remove(value);
                        this.lobbyPins.remove(value);
                        break;
                    case "video":
                        if (a.length != 2
                            || !a[0].matches("[0-9]{1,4},[0-9]{1,2},[0-9]{1,2}-[0-9]{1,4},[0-9]{1,2},[0-9]{1,2}")
                            || !a[1].matches("[0-9]{1,4}")
                            || this.videoRanges.size() >= 60 && !this.videoRanges.containsKey(a[1])) {
                            throw new IllegalArgumentException();
                        }

                        if (!validRange(a[0])) {
                            throw new IllegalArgumentException();
                        }

                        for (Entry<String, String> exx : this.videoRanges.entrySet()) {
                            if (!exx.getKey().equals(a[1]) && overlap(a[0], exx.getValue())) {
                                throw new IllegalArgumentException();
                            }
                        }

                        this.videoRanges.put(a[1], a[0]);
                        break;
                    case "reset":
                        if (a.length == 2 && a[0].equals(this.pin) && a[1].equals("RESET")) {
                            this.unit = "99";
                            this.pin = "ffff";
                            this.absentTarget = null;
                            this.absent = false;
                            this.setupId = false;
                            this.officeMode = false;
                            this.volume = this.voiceVolume = 2;
                            this.relayId = 0;
                            this.history.clear();
                            this.register();
                            this.administrator = null;
                            this.page = "main";
                            this.field = "";
                            break;
                        }

                        throw new IllegalArgumentException();
                    default:
                        throw new IllegalArgumentException();
                }
            } catch (Exception var12) {
                this.status = "입력·연결·통화 상태를 확인하세요";
                this.changed();
                return false;
            }

            this.input = "";
            this.status = "저장 완료";
            if (!this.field.equals("reset")) {
                this.page = "admin";
                this.field = "";
            }

            this.changed();
            return true;
        } else {
            this.status = "관리자 인증과 설정 화면 필요";
            this.changed();
            return false;
        }
    }

    private boolean manageDevices(ServerPlayer p, String value) {
        if (!this.authenticated(p)) {
            this.status = "관리자 인증 필요";
            this.changed();
            return false;
        } else {
            try {
                String[] a = value.split("\\|", -1);
                if (a[0].equals("DELETE") && a.length == 3 && a[2].equals("CONFIRM")) {
                    BlockPos pos = this.homes
                        .entrySet()
                        .stream()
                        .filter(ex -> normalize((String)ex.getValue()).equals(normalize(a[1])))
                        .map(Entry::getKey)
                        .findFirst()
                        .orElseThrow();
                    ReceiverBlockEntity r = this.home(pos);
                    if (r != null) {
                        r.unlinkSwitchboard();
                    } else {
                        this.onUnlinked(pos);
                    }
                } else if (a[0].equals("CLEAR") && a.length == 2 && a[1].equals("CONFIRM")) {
                    for (BlockPos pos : new ArrayList<>(this.homes.keySet())) {
                        ReceiverBlockEntity r = this.home(pos);
                        if (r != null) {
                            r.unlinkSwitchboard();
                        } else {
                            this.onUnlinked(pos);
                        }
                    }
                } else if (a[0].equals("REGISTER") && a.length == 4) {
                    String[] xyz = a[1].split(",");
                    BlockPos posx = new BlockPos(Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2]));
                    ReceiverBlockEntity r = this.home(posx);
                    Am10kpBlockEntity target = this.byId(a[3]);
                    if (r == null
                        || target == null
                        || !r.supportsSwitchboard(target)
                        || !r.validAssignedUnit(a[2])
                        || !target.canLink(posx)
                        || target.homes
                            .entrySet()
                            .stream()
                            .anyMatch(ex -> !((BlockPos)ex.getKey()).equals(posx) && normalize((String)ex.getValue()).equals(normalize(a[2])))) {
                        throw new IllegalArgumentException();
                    }

                    if (!r.bindSwitchboard(target.getBlockPos())) {
                        throw new IllegalArgumentException();
                    }

                    String n = normalize(a[2]);
                    r.assignUnitFromSwitchboard(target.getBlockPos(), n);
                    target.onLinked(posx, n);
                } else {
                    if (!a[0].equals("RANGE") || a.length != 6) {
                        throw new IllegalArgumentException();
                    }

                    int dong = bounded(a[1], 1, 9999);
                    int sf = bounded(a[2], 0, 99);
                    int ef = bounded(a[3], sf, 99);
                    int sl = bounded(a[4], 1, 99);
                    int el = bounded(a[5], sl, 99);
                    if ((ef - sf + 1) * (el - sl + 1) < this.homes.size()) {
                        throw new IllegalArgumentException();
                    }

                    int i = 0;

                    for (BlockPos posxx : new ArrayList<>(this.homes.keySet())) {
                        ReceiverBlockEntity rx = this.home(posxx);
                        if (rx == null || rx.isBusy()) {
                            throw new IllegalArgumentException();
                        }
                    }

                    for (BlockPos posxxx : new ArrayList<>(this.homes.keySet())) {
                        String n = dong + "-" + ((sf + i / (el - sl + 1)) * 100 + sl + i % (el - sl + 1));
                        i++;
                        if (this.home(posxxx).roomOnlyUnit()) {
                            n = n.substring(n.indexOf("-") + 1);
                        }

                        if (!this.home(posxxx).assignUnitFromSwitchboard(this.worldPosition, n)) {
                            throw new IllegalArgumentException();
                        }

                        this.homes.put(posxxx, n);
                    }
                }

                this.status = "장치기 관리 완료";
                this.input = "";
                this.changed();
                return true;
            } catch (Exception var13) {
                this.status = "REGISTER/DELETE/RANGE/CLEAR 입력 확인";
                this.changed();
                return false;
            }
        }
    }

    private static int[] range(String s) {
        return Arrays.stream(s.split("[-,]")).mapToInt(Integer::parseInt).toArray();
    }

    private static boolean validRange(String s) {
        int[] r = range(s);
        return r.length == 6 && r[0] <= r[3] && r[1] <= r[4] && r[2] <= r[5] && r[2] > 0;
    }

    private static boolean overlap(String a, String b) {
        int[] x = range(a);
        int[] y = range(b);
        return x[0] <= y[3] && y[0] <= x[3] && x[1] <= y[4] && y[1] <= x[4] && x[2] <= y[5] && y[2] <= x[5];
    }

    public boolean allowsLobbyVideo(String unit, AnalogLobbyEndpoint source) {
        if (this.videoRanges.isEmpty()) {
            return true;
        } else {
            String value = this.videoRanges.get(source.videoNumber());
            if (value == null) {
                return false;
            } else {
                try {
                    String[] p = normalize(unit).split("-");
                    if (p.length == 1) {
                        int ho = Integer.parseInt(p[0]);
                        int[] r = range(value);
                        return ho / 100 >= r[1] && ho / 100 <= r[4] && ho % 100 >= r[2] && ho % 100 <= r[5];
                    } else {
                        int dong = Integer.parseInt(p[0]);
                        int ho = Integer.parseInt(p[1]);
                        int[] r = range(value);
                        return dong >= r[0] && dong <= r[3] && ho / 100 >= r[1] && ho / 100 <= r[4] && ho % 100 >= r[2] && ho % 100 <= r[5];
                    }
                } catch (Exception var8) {
                    return false;
                }
            }
        }
    }

    public boolean checkLobbyPassword(String dong, String ho, String attempted) {
        String p = this.lobbyPins.getOrDefault(normalize(dong + "-0"), this.lobbyPins.get(normalize(dong + "-" + ho)));
        return p != null && p.equals(attempted);
    }

    private static int bounded(String n, int min, int max) {
        int x = Integer.parseInt(n);
        if (x >= min && x <= max) {
            return x;
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void key(String k) {
        if (this.power && this.factoryTicks <= 0 && k != null && k.length() == 1) {
            if (k.equals("*")) {
                this.input = "";
                boolean exchanging = this.transfer;
                if (this.transfer) {
                    super.cancelTransfer();
                }

                if (exchanging) {
                    this.page = "calls";
                    this.changed();
                } else if (!this.handsfree || this.lifted || !this.page.equals("calls") && !this.page.equals("lobby")) {
                    if (this.page.equals("form") || this.page.equals("saved") || this.page.equals("notice")) {
                        this.page = this.returnPage;
                    } else if (this.page.equals("guard")) {
                        this.page = "admin";
                    } else {
                        this.page = "main";
                    }

                    if (this.page.equals("main")) {
                        this.administrator = null;
                    }

                    this.field = "";
                    this.changed();
                } else {
                    this.hangUp();
                }
            } else {
                this.recoverySequence = this.recoverySequence + k;
                if (this.recoverySequence.length() > 4) {
                    this.recoverySequence = this.recoverySequence.substring(this.recoverySequence.length() - 4);
                }

                if (this.page.equals("form") && this.field.equals("reset") && this.formCursor == 1 && this.recoverySequence.equals("#074")) {
                    this.factoryRequested = true;
                    this.changed();
                } else if (this.page.equals("records") && this.recoverySequence.equals("#074")) {
                    this.page = "recovery";
                    this.input = "";
                    this.changed();
                } else if (!k.equals("#")) {
                    if (k.matches("[0-9]") && this.page.equals("device_menu")) {
                        this.deviceChoice = Math.max(0, Math.min(4, Integer.parseInt(k) - 1));
                        this.changed();
                    } else if (k.matches("[0-9]") && this.page.equals("form")) {
                        this.editFormKey(k);
                        this.changed();
                    } else {
                        if (k.matches("[0-9]") && this.input.length() < 48) {
                            if (!this.page.equals("admin") && !this.page.equals("guard")) {
                                this.input = this.input + k;
                                if (this.page.equals("main")) {
                                    this.page = "dial";
                                }
                            } else {
                                this.menuIndex = k.equals("0") ? 9 : Integer.parseInt(k) - 1;
                            }
                        }

                        this.changed();
                    }
                }
            }
        }
    }

    @Override
    public void handleAction(ServerPlayer p, ReceiverBlockEntity.Action action, BlockPos target, String text) {
        if (action == ReceiverBlockEntity.Action.SET_SETTING && text != null) {
            int i = text.indexOf(61);
            if (i < 0) {
                return;
            }

            String k = text.substring(0, i);
            String v = text.substring(i + 1);
            switch (k) {
                case "button":
                    if (v.equals("enter_down") && this.page.equals("dial")) {
                        this.enterStarted = this.level.getGameTime();
                        this.enterPlayer = p.getUUID();
                    } else if (v.equals("enter_up")) {
                        if (this.enterStarted >= 0L && p.getUUID().equals(this.enterPlayer) && this.level.getGameTime() - this.enterStarted >= 14L) {
                            this.button("enter", p);
                        }

                        this.enterStarted = -1L;
                    } else if (v.equals("enter_cancel")) {
                        this.enterStarted = -1L;
                    } else if (!v.equals("enter") || !this.page.equals("dial")) {
                        this.button(v, p);
                    }
                    break;
                case "key":
                    this.key(v);
                    break;
                case "input":
                    this.input = Intercom.sanitize(v, 48);
                    if (this.page.equals("form") && this.formValues.length > 0) {
                        this.formValues[this.formCursor] = this.input;
                        this.formTyping = true;
                    }

                    this.changed();
                    break;
                case "pin":
                    this.authenticate(p, v);
                    break;
                case "save":
                    if (this.page.equals("devices")) {
                        this.manageDevices(p, v);
                    } else {
                        this.saveForm(p, v);
                    }
                    break;
                case "line":
                    try {
                        this.answerLine(Integer.parseInt(v));
                    } catch (NumberFormatException var11) {
                    }
                    break;
                case "page":
                    if (Set.of("records", "clock", "settings", "calls", "alarms", "dial").contains(v)) {
                        this.page = v;
                        this.recordPage = 0;
                        this.changed();
                    }
            }
        } else if (action == ReceiverBlockEntity.Action.PICKUP) {
            this.button("handset", p);
        } else if (action == ReceiverBlockEntity.Action.HANG_UP) {
            this.hangUp();
        } else if (action == ReceiverBlockEntity.Action.SEND_MESSAGE) {
            this.sendMessage(p, text);
        } else if (action == ReceiverBlockEntity.Action.OPEN_DOOR) {
            this.openLobbyDoor();
        }
    }

    @Override
    public void setGuardEmergency(BlockPos p, String id, boolean active) {
        boolean old = this.guardAlerts.containsKey(p);
        super.setGuardEmergency(p, id, active);
        if (old != this.guardAlerts.containsKey(p)) {
            this.alarmHistory.add(0, new MissedCall("GB|" + id + "|" + (active ? "발생" : "해제"), this.recordTime()));

            while (this.alarmHistory.size() > 500) {
                this.alarmHistory.remove(this.alarmHistory.size() - 1);
            }

            if (active) {
                this.page = "alarms";
            }

            this.changed();
        }
    }

    private void broadcastEmergency() {
        if (this.level instanceof ServerLevel sl) {
            for (DeviceRegistry.Entry e : DeviceRegistry.get(sl).all()) {
                if (sl.isLoaded(e.pos()) && !e.pos().equals(this.worldPosition) && sl.getBlockEntity(e.pos()) instanceof GuardExchange target) {
                    target.setGuardEmergency(this.worldPosition, this.unit, this.selfEmergency);
                }
            }
        }

        this.status = this.selfEmergency ? "경비실 자체 비상 전파" : "경비실 비상 해제";
        this.changed();
    }

    public boolean selfEmergency() {
        return this.selfEmergency;
    }

    private static String two(String n) {
        try {
            return String.format("%02d", Integer.parseInt(n));
        } catch (Exception var2) {
            return "00";
        }
    }

    private static String four(String n) {
        try {
            return String.format("%04d", Integer.parseInt(n));
        } catch (Exception var2) {
            return "0000";
        }
    }

    private void manualFormLayout(String[] lines) {
        if (this.page.equals("form")) {
            ReceiverBlockEntity receiver = this.activeHome();
            if (this.field.equals("video")) {
                Arrays.fill(lines, "");
                lines[0] = "< 로비 영상 제어 입력 >";
                lines[1] = "           동       층   라인";
                lines[2] = "시작 : " + four(this.formValue(0)) + " " + two(this.formValue(1)) + " " + two(this.formValue(2));
                lines[4] = "끝 : " + four(this.formValue(3)) + " " + two(this.formValue(4)) + " " + two(this.formValue(5));
                lines[5] = "번호 : " + four(this.formValue(6));
                lines[7] = "입력[0~9] 선택[확인] 나가기[*]";
            } else if (this.field.equals("reset")) {
                Arrays.fill(lines, "");
                lines[0] = this.factoryRequested ? "< 공장 초기화 확인 >" : "< 경비실 마스터 초기화 >";
                if (this.formCursor == 0) {
                    lines[2] = "비밀번호를 입력하세요!";
                    lines[4] = "입력 : " + "*".repeat(this.formValue(0).length());
                } else {
                    lines[2] = this.factoryRequested ? "모든 설정 및 기록을 초기화합니다." : "경비실 설정을 초기화합니다.";
                    lines[3] = "!!! 경고 !!!";
                    lines[5] = "초기화 : " + (this.formValue(1).equals("1") ? "예" : "아니오");
                }

                lines[7] = "선택[UP/DOWN] 확인[ENTER] 취소[*]";
            } else if (this.field.equals("device_register") || this.field.equals("device_range")) {
                Arrays.fill(lines, "");
                lines[0] = "< 장치기 등록 >";

                int group;
                try {
                    group = Integer.parseInt(this.formValue(0));
                } catch (Exception var7) {
                    group = 0;
                }

                lines[1] = "그룹 : "
                    + new String[]{"세대", "로비", "서버"}[Math.max(0, Math.min(2, group))]
                    + " 경비 : "
                    + this.formValue(1)
                    + " 주장치 : "
                    + this.formValue(2);
                lines[2] = "카드번호 : " + this.formValue(3) + " 채널번호 : " + this.formValue(4);
                if (!this.field.equals("device_register")) {
                    lines[3] = "시작 층 : " + this.formValue(5) + " 끝 층 : " + this.formValue(6);
                    lines[4] = "시작라인 : " + this.formValue(7) + " 끝라인 : " + this.formValue(8);
                    lines[5] = "지정할 동 번호 : " + four(this.formValue(9));
                } else {
                    lines[3] = (
                            this.home(this.editingDevice) != null && this.home(this.editingDevice).roomOnlyUnit()
                                ? ""
                                : "동 번호 : " + four(this.formValue(5)) + " "
                        )
                        + "호 번호 : "
                        + four(this.formValue(6));
                    lines[4] = "세대 옵션 번호 : " + this.formValue(7);
                }

                lines[7] = "입력[0~9] 선택[확인] 나가기[*]";
            } else if (this.field.equals("device_search")) {
                Arrays.fill(lines, "");
                lines[0] = "< 장치기 검색/삭제 >";
                lines[1] = "주장치 : " + this.formValue(0) + "번";
                lines[2] = "카드번호 : " + this.formValue(1) + " 채널번호 : " + this.formValue(2);
                lines[4] = "1. 장치기를 검색합니다.";
                if (!this.deviceAction.equals("status")) {
                    lines[5] = "2. 장치기를 전체 삭제합니다.";
                }

                lines[7] = "입력[0~9] 선택[확인] 나가기[*]";
            } else if (this.field.equals("house")) {
                Arrays.fill(lines, "");
                lines[0] = "< 세대 번호 변경 >";
                lines[2] = "현재 주소 : " + (receiver == null ? "통화 중에만 변경 가능" : this.formattedAddress(receiver.getUnit(), "SD"));
                lines[3] = "변경할 주소 : " + (receiver != null && receiver.roomOnlyUnit() ? "" : four(this.formValue(0)) + "동 ") + four(this.formValue(1)) + "호";
                lines[4] = "변경할 경비 번호 : " + this.formValue(2) + "번";
                lines[5] = "세대 옵션 번호 : " + this.formValue(3);
                lines[7] = "입력[0~9] 선택[확인] 나가기[*]";
            } else if (this.field.equals("house_pin")) {
                Arrays.fill(lines, "");
                lines[0] = "< 세대 비밀번호 변경 >";
                lines[1] = "현재 세대 : " + (receiver == null ? "통화 중에만 변경 가능" : this.formattedAddress(receiver.getUnit(), "SD"));
                lines[2] = "현재 비밀번호 : " + (receiver == null ? "" : receiver.getDoorPassword());
                lines[4] = "변경 비밀번호 : " + this.formValue(0);
                lines[7] = "입력[0~9] 선택[확인] 나가기[*]";
            } else if (this.field.equals("lobby")) {
                Arrays.fill(lines, "");
                lines[0] = "< 로비 관리 >";
                lines[1] = "동 번호 : " + four(this.formValue(0)) + "동 " + four(this.formValue(1)) + "호";
                lines[2] = "비밀번호 : " + this.formValue(2);
                BlockPos configured = this.lobbies.get(normalize(this.formValue(0) + "-" + this.formValue(1)));
                if (configured != null) {
                    lines[3] = this.lobbyPanelSettings(configured);
                }

                lines[4] = this.lobbies.containsKey(normalize(this.formValue(0) + "-" + this.formValue(1))) ? "기존 번호를 수정합니다." : "새 번호를 등록합니다.";
                lines[7] = "입력[0~9] 선택[확인] 나가기[*]";
            } else if (this.field.equals("forward") || this.field.equals("guard_id") || this.field.equals("guard_pin")) {
                Arrays.fill(lines, "");
                lines[0] = "< " + this.formTitle() + " >";
                String current = this.field.equals("forward")
                    ? (this.board(this.absentTarget) == null ? "0" : this.board(this.absentTarget).getUnit())
                    : (this.field.equals("guard_pin") ? this.pin : this.unit);
                lines[1] = "현재 " + (this.field.equals("guard_pin") ? "비밀번호" : "경비실 번호") + " : " + current;
                lines[3] = "변경 " + (this.field.equals("guard_pin") ? "비밀번호" : "경비실 번호") + " : " + this.formValue(0);
                lines[4] = "입력 : " + this.formValue(0);
                if (this.field.equals("guard_id") && this.formCursor == 1) {
                    lines[0] = "< SETUP ID 선택 >";
                    lines[3] = "SETUP ID : " + (this.formValue(1).equals("1") ? "설정" : "해제");
                    lines[4] = "UP/DOWN 선택";
                }

                lines[7] = "입력[0~9] 선택[확인] 나가기[*]";
            } else if (this.field.equals("volume")) {
                Arrays.fill(lines, "");
                lines[0] = "< 음량 조절 >";
                lines[1] = "01. 벨소리 크기 조절";
                lines[2] = volumeBar(this.formValue(0));
                lines[3] = "02. 통화음 크기 조절";
                lines[4] = volumeBar(this.formValue(1));
                lines[7] = "선택[UP/DOWN] 나가기[*]";
            } else if (this.field.equals("monitor")) {
                Arrays.fill(lines, "");
                lines[0] = "< 경비실 감시 모드 설정 >";
                lines[1] = "감시 모드를 설정합니다.";
                lines[3] = "감시 모드 : " + (this.formValue(0).equals("1") ? "설정" : "해제");
                lines[6] = "UP/DOWN 키를 사용하여";
                lines[7] = "[설정]/[해제]를 선택하세요!";
            } else if (this.field.equals("clock")) {
                Arrays.fill(lines, "");
                lines[0] = "< 시간 관리 >";
                String[] v = new String[6];

                for (int i = 0; i < 6; i++) {
                    v[i] = this.formValue(i);
                }

                String day = "?";

                try {
                    day = LocalDate.of(Integer.parseInt(v[0]), Integer.parseInt(v[1]), Integer.parseInt(v[2]))
                        .getDayOfWeek()
                        .getDisplayName(TextStyle.SHORT, Locale.KOREAN);
                } catch (Exception var6) {
                }

                lines[3] = v[0] + "." + v[1] + "." + v[2] + " [" + day + "] " + v[3] + ":" + v[4] + ":" + v[5];
                lines[7] = "수정[UP/DOWN] 다음[확인] 취소[*]";
            }
        }
    }

    private static String volumeBar(String v) {
        int n;
        try {
            n = Integer.parseInt(v);
        } catch (Exception var3) {
            n = 0;
        }

        return "■".repeat(Math.max(0, Math.min(3, n + 1))) + "□".repeat(Math.max(0, 2 - n));
    }

    private int selectedFormRow() {
        String var1 = this.field;

        return switch (var1) {
            case "video" -> this.formCursor < 3 ? 2 : (this.formCursor < 6 ? 4 : 5);
            case "reset" -> this.formCursor == 0 ? 4 : 5;
            case "device_register" -> this.formCursor < 3 ? 1 : (this.formCursor < 5 ? 2 : (this.formCursor < 7 ? 3 : 4));
            case "device_range" -> this.formCursor < 3 ? 1 : (this.formCursor < 5 ? 2 : (this.formCursor < 7 ? 3 : (this.formCursor < 9 ? 4 : 5)));
            case "device_search" -> this.formCursor == 0 ? 1 : (this.formCursor < 3 ? 2 : (this.formValue(3).equals("2") ? 5 : 4));
            case "house" -> this.formCursor < 2 ? 3 : this.formCursor + 2;
            case "house_pin" -> 4;
            case "lobby" -> this.formCursor < 2 ? 1 : 2;
            case "forward", "guard_pin" -> 4;
            case "guard_id" -> this.formCursor == 0 ? 4 : 3;
            case "volume" -> this.formCursor == 0 ? 2 : 4;
            case "monitor", "clock" -> 3;
            default -> 1 + this.formCursor % 5;
        };
    }

    public int lcdHighlightStart() {
        if (this.level != null && this.level.isClientSide) {
            return this.clientHighlightStart;
        } else if (!this.page.equals("form")) {
            return -1;
        } else {
            String line = this.lcd()[this.selectedFormRow()];
            if (this.field.equals("video")) {
                int at = line.indexOf(":") + 2;
                return at + (this.formCursor == 6 ? 0 : new int[]{0, 5, 8}[this.formCursor % 3]);
            } else if (!this.field.equals("device_register") && !this.field.equals("device_range")) {
                if (this.field.equals("device_search")) {
                    if (this.formCursor == 3) {
                        return 0;
                    } else {
                        String label = new String[]{"주장치 : ", "카드번호 : ", "채널번호 : "}[this.formCursor];
                        return Math.max(0, line.indexOf(label) + label.length());
                    }
                } else if ((!this.field.equals("house") || this.formCursor != 1) && (!this.field.equals("lobby") || this.formCursor != 1)) {
                    if (!this.field.equals("clock")) {
                        int colon = line.indexOf(":");
                        return colon < 0 ? 0 : Math.min(line.length(), colon + 2);
                    } else {
                        int start = 0;
                        String[] parts = new String[]{
                            this.formValue(0), this.formValue(1), this.formValue(2), this.formValue(3), this.formValue(4), this.formValue(5)
                        };
                        if (this.formCursor < 3) {
                            for (int i = 0; i < this.formCursor; i++) {
                                start += parts[i].length() + 1;
                            }
                        } else {
                            start = line.indexOf("] ") + 2;

                            for (int i = 3; i < this.formCursor; i++) {
                                start += parts[i].length() + 1;
                            }
                        }

                        return start;
                    }
                } else {
                    return Math.max(0, line.lastIndexOf(" ") + 1);
                }
            } else {
                String[] labels = this.field.equals("device_register")
                    ? new String[]{"그룹 : ", "경비 : ", "주장치 : ", "카드번호 : ", "채널번호 : ", "동 번호 : ", "호 번호 : ", "세대 옵션 번호 : "}
                    : new String[]{"그룹 : ", "경비 : ", "주장치 : ", "카드번호 : ", "채널번호 : ", "시작 층 : ", "끝 층 : ", "시작라인 : ", "끝라인 : ", "지정할 동 번호 : "};
                String label = labels[this.formCursor];
                return Math.max(0, line.indexOf(label) + label.length());
            }
        }
    }

    public int lcdHighlightLength() {
        if (this.level != null && this.level.isClientSide) {
            return this.clientHighlightLength;
        } else if (!this.page.equals("form")) {
            return -1;
        } else if (this.field.equals("video")) {
            return this.formCursor != 0 && this.formCursor != 3 && this.formCursor != 6 ? 2 : 4;
        } else if (!this.field.equals("device_register") && !this.field.equals("device_range")) {
            if (this.field.equals("device_search") && this.formCursor < 3) {
                return Math.max(1, this.formValue(this.formCursor).length());
            } else if ((!this.field.equals("house") || this.formCursor >= 2) && (!this.field.equals("lobby") || this.formCursor >= 2)) {
                if (this.field.equals("house") && this.formCursor == 2) {
                    return this.formValue(2).length();
                } else if (this.field.equals("clock")) {
                    return this.formValue(this.formCursor).length();
                } else {
                    String line = this.lcd()[this.selectedFormRow()];
                    return Math.max(1, line.length() - Math.max(0, this.lcdHighlightStart()));
                }
            } else {
                return 4;
            }
        } else if (this.formCursor == 0) {
            return 2;
        } else {
            return (!this.field.equals("device_register") || this.formCursor != 5 && this.formCursor != 6)
                    && (!this.field.equals("device_range") || this.formCursor != 9)
                ? Math.max(1, this.formValue(this.formCursor).length())
                : 4;
        }
    }

    public int lcdSelectedRow() {
        return this.level != null && this.level.isClientSide
            ? this.clientSelection
            : (!this.page.equals("admin") && !this.page.equals("guard") ? (this.page.equals("form") ? this.selectedFormRow() : -1) : this.menuIndex % 6);
    }

    public String lcdPage() {
        return this.level != null && this.level.isClientSide ? this.clientPage : this.page;
    }

    public String[] lcd() {
        if (this.level != null && this.level.isClientSide) {
            return this.clientLcd;
        } else {
            String[] lines = new String[8];
            Arrays.fill(lines, "");
            if (this.factoryTicks > 0) {
                lines[0] = "< 공장 초기화 >";
                lines[3] = "초기화 진행 중...";
                lines[5] = "잠시 기다리세요 : " + (this.factoryTicks + 19) / 20 + "초";
                return lines;
            } else if (!this.power) {
                return lines;
            } else {
                if (this.page.equals("admin") || this.page.equals("guard")) {
                    String[] options = this.page.equals("admin") ? MENU : GUARD_MENU;
                    int begin = this.menuIndex / 6 * 6;

                    for (int i = 0; i < 6 && begin + i < options.length; i++) {
                        lines[i] = String.format("%02d. %s", begin + i + 1, options[begin + i]);
                    }

                    lines[7] = "선택[번호 Or UP/DOWN] 나가기[*]";
                } else if (this.page.equals("settings")) {
                    lines[0] = "< Master현재 설정 >";
                    lines[1] = "01.현재 경비실 번호 : " + this.unit;
                    lines[2] = "02.현재 우회 경비실 번호 : " + (this.board(this.absentTarget) == null ? "0" : this.board(this.absentTarget).getUnit());
                    lines[3] = "03.벨음량 : " + this.volume + ", 통화음량 : " + this.voiceVolume;
                    lines[4] = String.format("04.주장치 ID : %02d", this.relayId);
                    lines[5] = "05.마스터 모드 : " + (this.absent ? "부재" : "일반대기");
                    lines[6] = "06.Ver 0.0.1 (06.02.07)";
                } else if (this.page.equals("dial")) {
                    lines[0] = "KOCOM";
                    lines[1] = "(주)코콤 무인경비 시스템";
                    lines[4] = this.formattedAddress(this.input, this.dialGroup);
                    lines[6] = this.dialGroup.equals("LB") ? "로비 번호 선택" : "";
                } else if (!this.page.equals("calls") && !this.page.equals("lobby") && !this.page.equals("lobby_ring")) {
                    if (this.page.equals("form")) {
                        lines[0] = "< " + this.formTitle() + " >";
                        String[] labels = this.formLabels();
                        int begin = this.formCursor / 5 * 5;

                        for (int i = 0; i < 5 && begin + i < labels.length; i++) {
                            int at = begin + i;
                            String v = this.formValue(at);
                            if (this.field.equals("version_lookup") && at == 0) {
                                v = new String[]{"세대", "경비실", "로비", "주장치", "세대카드"}[Integer.parseInt(v)];
                            } else if (this.field.equals("language")) {
                                v = switch (v) {
                                    case "en" -> "영어";
                                    case "zh" -> "중국어";
                                    default -> "한국어";
                                };
                            } else if (this.field.equals("monitor") || this.field.equals("guard_id") && at == 1 || this.field.equals("reset") && at == 1) {
                                v = v.equals("1") ? "설정" : "해제";
                            }

                            lines[i + 1] = labels[at] + " : " + v;
                        }

                        if (!this.status.equals("입력[0~9] 선택[확인] 나가기[*]") && !this.status.equals(this.formHint())) {
                            lines[6] = this.status;
                        }

                        lines[7] = "입력[0~9] 선택[확인] 나가기[*]";
                    } else if (this.page.equals("recovery")) {
                        lines[0] = "< 경비실 비밀번호 확인 >";
                        lines[3] = "현재 비밀번호 : " + this.pin;
                        lines[7] = "나가기[*]";
                    } else if (this.page.equals("pin")) {
                        lines[0] = "KOCOM";
                        lines[1] = this.field.equals("absence") ? "부재기능을 실행합니다." : "관리자 메뉴를 실행합니다.";
                        lines[2] = "비밀번호를 입력하세요!";
                        lines[4] = "*".repeat(Math.min(16, this.input.length()));
                        lines[7] = "선택[확인] 나가기[*]";
                    } else if (this.page.equals("absence")) {
                        lines[0] = "< 경비실 부재 설정 >";
                        lines[1] = this.absent ? "경비실 부재기능 동작 중..." : "경비실 부재기능 해제";
                        lines[2] = "현재 우회 경비실 번호 : " + (this.board(this.absentTarget) == null ? "0" : this.board(this.absentTarget).getUnit()) + "번";
                        lines[4] = this.absent ? "OUT" : "IN";
                        lines[7] = "UP/DOWN 설정/해제 나가기[*]";
                    } else if (this.page.equals("notice")) {
                        lines[0] = "< 알림 >";
                        lines[3] = this.notice;
                        lines[7] = "선택[확인] 나가기[*]";
                    } else if (this.page.equals("saved")) {
                        lines[0] = "< " + this.notice + " >";
                        ReceiverBlockEntity r = this.activeHome();
                        if (r != null) {
                            lines[2] = "현재 주소 : " + this.formattedAddress(r.getUnit(), "SD");
                        }

                        lines[4] = "변경 완료";
                        lines[7] = "변경 완료 or Hook off";
                    } else if (!this.page.equals("records") && !this.page.equals("archive")) {
                        if (this.page.equals("alarms")) {
                            lines[0] = "< 비상 상황 발생 >";
                            int j = 1;

                            for (Entry<BlockPos, String> alarm : this.alarms.entrySet()) {
                                for (String type : this.safetyTypes.getOrDefault(alarm.getKey(), Set.of("BS"))) {
                                    if (j < 6) {
                                        lines[j++] = type + " " + this.formattedAddress(alarm.getValue(), "SD") + " 발생";
                                    }
                                }
                            }

                            for (String n : this.guardAlerts.values()) {
                                if (j < 6) {
                                    lines[j++] = "GB " + n + " 경비실 비상";
                                }
                            }

                            lines[7] = this.muted ? "비상음 정지 / 상황 유지" : "비상/정지 : 경보음 정지";
                        } else if (this.page.equals("device_menu")) {
                            lines[0] = "< 세대통신 검사 입력 >";
                            String[] a = new String[]{"장치기 삭제/검색", "장치기 등록", "장치기 상태/검색", "범위 지정 등록", "세대카드 전체 삭제"};

                            for (int i = 0; i < 5; i++) {
                                lines[i + 1] = (i == this.deviceChoice ? ">" : " ") + (i + 1) + ". " + a[i];
                            }

                            lines[7] = "입력[0~9] 선택[확인] 나가기[*]";
                        } else if (this.page.equals("lobby_ids")) {
                            lines[0] = "동    호    비밀번호";
                            ArrayList<String> ids = new ArrayList<>(this.lobbies.keySet());
                            int start = this.deviceIndex / 5 * 5;

                            for (int i = 0; i < 5 && start + i < ids.size(); i++) {
                                String id = ids.get(start + i);
                                lines[i + 1] = (start + i == this.deviceIndex ? ">" : " ") + id + " " + this.lobbyPins.getOrDefault(id, "");
                            }

                            lines[7] = "삭제[확인] 나가기[*]";
                        } else if (this.page.equals("device_status")) {
                            lines[0] = "< 장치기 상태 정보 >";
                            ReceiverBlockEntity r = this.home(this.editingDevice);
                            lines[1] = this.homes.getOrDefault(this.editingDevice, "");
                            lines[2] = r == null ? "통신 불량" : "통신 정상";
                            lines[3] = "부재 : " + (this.away.contains(this.editingDevice) ? "1" : "0");
                            lines[4] = "비상 : " + (this.alarms.containsKey(this.editingDevice) ? "1" : "0");
                            lines[7] = "나가기[*]";
                        } else if (this.page.equals("linecheck")) {
                            lines[0] = "< 중계기 라인 체크 >";
                            lines[2] = "주장치 ID : " + this.relayId;
                            lines[3] = "등록 장치기 : " + this.homes.size();
                            long healthy = this.homes.keySet().stream().filter(pos -> this.home(pos) != null).count();
                            lines[4] = "응답 장치기 : " + healthy;
                            lines[5] = "통신 불량 : " + (this.homes.size() - healthy);
                            lines[7] = "나가기[*]";
                        } else if (this.page.equals("devices") || this.page.equals("device_list")) {
                            lines[0] = "< 장치기 상태/검색 >";
                            int begin = this.page.equals("device_list") ? this.deviceIndex / 5 * 5 : this.recordPage * 5;
                            List<BlockPos> items = this.devicePositions();

                            for (int i = 0; i < 5 && begin + i < items.size(); i++) {
                                BlockPos pos = items.get(begin + i);
                                ReceiverBlockEntity r = this.home(pos);
                                lines[i + 1] = (begin + i == this.deviceIndex ? ">" : " ")
                                    + this.deviceNumber(pos)
                                    + " "
                                    + (r == null ? (this.validLobby(pos) ? "로비 정상" : "통신 불량") : (r.isBusy() ? "통화중" : (this.away.contains(pos) ? "부재" : "정상")));
                            }

                            lines[7] = "UP/DOWN 선택[확인] 나가기[*]";
                        } else if (this.page.equals("peer_list")) {
                            lines[0] = "< 주장치 경비/상호 ID >";
                            int start = this.deviceIndex / 5 * 5;

                            for (int i = 0; i < 5 && start + i < this.peerResults.size(); i++) {
                                Am10kpBlockEntity b = this.board(this.peerResults.get(start + i));
                                lines[i + 1] = (start + i == this.deviceIndex ? ">" : " ") + (b == null ? "통신 불량" : b.getUnit() + " " + b.modelId());
                            }

                            lines[7] = "UP/DOWN 수정[확인] 나가기[*]";
                        } else if (this.page.equals("version_result")) {
                            lines[0] = "< 장치기 버전 정보 >";
                            lines[2] = "주소 : " + this.queriedAddress;
                            lines[3] = this.queriedModel;
                            lines[4] = this.queriedModel.equals("통신 불량") ? "통신 실패" : "HomeNet 1.0.0";
                            lines[7] = "나가기[*]";
                        } else if (this.page.equals("peers")) {
                            lines[0] = "< 주장치 상호 ID검색 >";
                            lines[1] = "주장치 ID : " + this.relayId;
                            int j = 2;
                            if (this.level instanceof ServerLevel sl) {
                                for (DeviceRegistry.Entry e : DeviceRegistry.get(sl).all()) {
                                    if (this.board(e.pos()) != null && j < 7) {
                                        lines[j++] = this.board(e.pos()).getUnit() + " " + this.board(e.pos()).modelId();
                                    }
                                }
                            }

                            lines[7] = "선택[확인] 나가기[*]";
                        } else if (this.page.equals("version")) {
                            lines[0] = "< 장치기 버전 정보 >";
                            lines[2] = "경비실 : " + this.unit;
                            lines[3] = "Ver 0.0.1 (06.02.07)";
                            lines[7] = "선택[확인] 나가기[*]";
                        } else if (this.page.equals("clock")) {
                            lines[0] = "< 현재 시각 >";
                            lines[3] = this.timeText();
                            lines[7] = "나가기[*]";
                        } else {
                            lines[0] = "U-홈네트워크 전문회사";
                            lines[2] = "KOCOM";
                            lines[6] = this.timeText();
                            if (this.setupId && this.ticks / 10 % 2 == 0) {
                                lines[7] = "#" + this.unit;
                            }

                            if (this.monitorMode) {
                                lines[7] = lines[7] + " 감시";
                            }

                            if (this.absent) {
                                lines[7] = lines[7] + " 부재";
                            }
                        }
                    } else {
                        List<MissedCall> items = this.page.equals("records") ? this.history : this.alarmHistory;
                        lines[0] = this.page.equals("records") ? "방향  동    호    시간   통화방식" : "동   호   상태   상황   날짜";

                        for (int i = 0; i < 5 && this.recordPage * 5 + i < items.size(); i++) {
                            MissedCall r = items.get(this.recordPage * 5 + i);
                            String[] a = r.caller().split("\\|");
                            LocalDateTime t = LocalDateTime.ofEpochSecond(r.dayTime(), 0, ZoneOffset.UTC);
                            String when = t.format(DateTimeFormatter.ofPattern("MM/dd HH:mm"));
                            lines[i + 1] = this.page.equals("archive") && a.length >= 3
                                ? a[1].replace("-", " ") + " " + a[0] + " " + (a[2].equals("발생") ? "ON" : "OFF") + " " + when
                                : (
                                    a.length >= 4
                                        ? (a[3].equals("수신") ? ">" : "<") + a[1] + " " + a[2].replace("-", " ") + " " + when + " " + a[0]
                                        : r.caller().replace("|", " ")
                                );
                        }

                        lines[7] = String.format(
                            "%sPAGE UP/DOWN [%03d/%03d]", this.page.equals("records") ? "통화기록" : "보안관리", this.recordPage, Math.max(1, (items.size() + 4) / 5)
                        );
                    }
                } else {
                    Am10kpBlockEntity.Channel c = this.selected >= 0 ? this.channels[this.selected] : null;
                    boolean connected = c != null && c.connected || this.guardConnected || this.page.equals("lobby");
                    lines[0] = connected ? "통화 연결 되었습니다" : "< 통화 연결 중 입니다 >";
                    String address = c == null
                        ? (this.guardPeer != null && this.board(this.guardPeer) != null ? this.board(this.guardPeer).getUnit() : "")
                        : this.number(c);
                    lines[2] = this.formattedAddress(address, this.guardPeer != null ? "GB" : (this.lobbyPeer != null ? "LB" : "SD"));
                    lines[3] = "시간 " + (c == null ? this.guardTicks / 20 : c.age / 20) + "초";
                    lines[6] = this.timeText();
                    lines[7] = this.transfer ? "교환 통화" : "";
                }

                this.manualFormLayout(lines);
                return this.translateLcd(lines);
            }
        }
    }

    private String[] translateLcd(String[] lines) {
        if (this.language.equals("ko")) {
            return lines;
        } else {
            int column = this.language.equals("zh") ? 2 : 1;
            String[][] words = new String[][]{
                {"경비실 부재기능 동작 중...", "Guard is absent...", "保安室离开功能运行中"},
                {"관리자 메뉴를 실행합니다.", "Administrator menu.", "执行管理员菜单"},
                {"U-홈네트워크 전문회사", "U-HOME NETWORK COMPANY", "U-家庭网络专业公司"},
                {"주장치 상호 ID 관리", "Master peer IDs", "主机对讲号码管理"},
                {"부재기능을 실행합니다.", "Activate absence mode.", "执行离开功能"},
                {"비밀번호를 입력하세요!", "Enter password!", "请输入密码"},
                {"현재 우회 경비실 번호", "Forwarding guard ID", "当前转接保安室号码"},
                {"경비실 현재번호 수정", "Guard ID change", "保安室号码修改"},
                {"경비실 비밀번호 수정", "Guard password", "保安室密码修改"},
                {"동[로비] ID 관리", "Building/lobby IDs", "楼栋大厅号码管理"},
                {"경비실 마스터 초기화", "Guard master reset", "保安主机初始化"},
                {"통화 연결 되었습니다", "Call connected", "通话已连接"},
                {"통화 연결 중 입니다", "Connecting call", "正在连接通话"},
                {"변경 경비실 비밀번호", "New guard password", "新保安室密码"},
                {"세대통신 검사 입력", "Household diagnostics", "住户通信检测"},
                {"변경 세대 비밀번호", "New household password", "新住户密码"},
                {"세대카드 전체 삭제", "Clear household card", "删除全部住户卡"},
                {"중계기 라인 체크", "Relay line check", "中继线路检测"},
                {"중계기 ID 변경", "Relay ID change", "中继号码修改"},
                {"경비실 부재 설정", "Guard absence setup", "保安室离开设置"},
                {"현재 경비실 번호", "Current guard ID", "当前保安室号码"},
                {"관할 경비실 번호", "Guard ID", "所属保安室号码"},
                {"장치기 상태/검색", "Device state/search", "设备状态查询"},
                {"장치기 삭제/검색", "Device delete/search", "设备删除查询"},
                {"장치기 상태 정보", "Device status", "设备状态"},
                {"프린터 연결 없음", "Printer not connected", "打印机未连接"},
                {"비상데이터 관리", "Alarm records", "报警记录"},
                {"세대 버전 정보", "Device version", "设备版本"},
                {"공동현관문 제어", "Entrance door control", "公共大门控制"},
                {"로비 영상 제어", "Lobby video control", "大厅视频控制"},
                {"세대 번호 변경", "Household address change", "住户号码修改"},
                {"변경할 동 번호", "New building", "新楼栋号码"},
                {"변경할 호 번호", "New room", "新房间号码"},
                {"세대 옵션 번호", "Household option", "住户选项号码"},
                {"범위 지정 등록", "Range registration", "范围登记"},
                {"비상 상황 발생", "Alarm event", "报警发生"},
                {"세대번호 관리", "Household address", "住户号码管理"},
                {"세대암호 관리", "Household password", "住户密码管理"},
                {"감시모드 설정", "Monitoring mode", "监控模式设置"},
                {"현재 비밀번호", "Current password", "当前密码"},
                {"경비실 관리", "Guard management", "保安室管理"},
                {"마스터 모드", "Master mode", "主机模式"},
                {"벨소리 크기", "Ring volume", "铃声音量"},
                {"통화음 크기", "Call volume", "通话音量"},
                {"문열림 시간", "Door open seconds", "开门时间"},
                {"초기화 확인", "Confirm reset", "确认初始化"},
                {"주장치 ID", "Master ID", "主机号码"},
                {"장치기 등록", "Device registration", "设备登记"},
                {"부재 설정", "Absence setup", "离开设置"},
                {"로비 관리", "Lobby management", "大厅管理"},
                {"시각 관리", "Clock settings", "时间设置"},
                {"언어 설정", "Language", "语言设置"},
                {"음량 조절", "Volume", "音量"},
                {"변경 완료", "Change complete", "修改完成"},
                {"시간 관리", "Clock settings", "时间设置"},
                {"현재 시각", "Current time", "当前时间"},
                {"감시 모드", "Monitor mode", "监控模式"},
                {"통신 불량", "Communication failure", "通信故障"},
                {"통신 정상", "Communication OK", "通信正常"},
                {"통화음량", "Call volume", "通话音量"},
                {"일반대기", "Standby", "正常待机"},
                {"통화기록", "Call history", "通话记录"},
                {"보안관리", "Alarm history", "报警记录"},
                {"벨음량", "Ring volume", "铃声音量"},
                {"나가기", "Exit", "退出"},
                {"한국어", "Korean", "韩语"},
                {"중국어", "Chinese", "中文"},
                {"경비실", "Guard", "保安室"},
                {"선택", "Select", "选择"},
                {"입력", "Input", "输入"},
                {"확인", "Confirm", "确认"},
                {"수정", "Edit", "修改"},
                {"감시", "Monitor", "监控"},
                {"비상", "Alarm", "报警"},
                {"부재", "Absent", "离开"},
                {"설정", "On", "设置"},
                {"해제", "Off", "解除"},
                {"저장", "Save", "保存"},
                {"시간", "Time", "时间"},
                {"영어", "English", "英语"},
                {"세대", "Household", "住户"},
                {"로비", "Lobby", "大厅"}
            };

            for (int i = 0; i < lines.length; i++) {
                for (String[] word : words) {
                    lines[i] = lines[i].replace(word[0], word[column]);
                }
            }

            return lines;
        }
    }

    private String formattedAddress(String value, String group) {
        if (!group.equals("GB") && !group.equals("SH")) {
            String a = value.replace("-", "");
            if (a.isEmpty()) {
                return "";
            } else if (!value.contains("-") && UnitAddress.component(value)) {
                return value + "호";
            } else if (value.contains("-")) {
                String[] p = value.split("-");

                try {
                    return String.format("%04d동 %04d호", Integer.parseInt(p[0]), Integer.parseInt(p[1]));
                } catch (Exception var6) {
                    return value;
                }
            } else {
                return a.length() > 4 ? a.substring(0, 4) + "동 " + a.substring(4) + "호" : a;
            }
        } else {
            return value.isEmpty() ? "" : value + "번";
        }
    }

    private String formTitle() {
        String var1 = this.field;

        return switch (var1) {
            case "peer_search" -> "주장치 상호 ID검색";
            case "peer_change" -> "상호/경비 ID 수정";
            case "version_lookup" -> "장치기 버전 정보";
            case "device_search" -> "장치기 검색/삭제";
            case "device_scope_delete" -> "선택 카드 전체 삭제";
            case "device_register" -> "장치기 등록";
            case "device_delete" -> "장치기 삭제 확인";
            case "device_clear" -> "세대카드 전체 삭제";
            case "device_range" -> "범위 지정 등록";
            case "forward" -> "우회 경비실 번호 변경";
            case "house" -> "세대 번호 변경";
            case "house_pin" -> "세대 비밀번호 변경";
            case "guard_id" -> "경비실 현재번호 수정";
            case "guard_pin" -> "경비실 비밀번호 수정";
            case "lobby" -> "로비 관리";
            case "monitor" -> "경비실 감시 모드 설정";
            case "clock" -> "시간 관리";
            case "language" -> "언어 설정";
            case "volume" -> "음량 조절";
            case "door_time" -> "공동현관문 제어 설정";
            case "video" -> "로비 영상 제어 입력";
            case "reset" -> "경비실 마스터 초기화";
            case "relay" -> "중계기 ID 변경";
            default -> "설정";
        };
    }

    private String timeText() {
        return LocalDateTime.ofEpochSecond((this.level == null ? 0L : this.level.getGameTime() / 20L) + this.clockOffset, 0, ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern(this.page.equals("clock") ? "yyyy.MM.dd '['E']' HH:mm:ss" : "yyyy.MM.dd '['E']' HH:mm", Locale.KOREAN));
    }

    @Override
    protected List<MissedCall> records() {
        return List.copyOf(this.history);
    }

    @Override
    protected List<MissedCall> visitors() {
        return List.copyOf(this.alarmHistory);
    }

    @Override
    protected Map<String, String> settingsForClient() {
        Map<String, String> s = super.settingsForClient();
        s.remove("am10kp");
        s.put("kam305", "1");
        s.put("handsfree", this.handsfree ? "1" : "0");
        s.put("factory", Integer.toString(this.factoryTicks));
        s.put("angle", Integer.toString(this.angle));
        s.put("page", this.page);
        s.put("cursor", Integer.toString(this.formCursor));
        s.put("selected_row", Integer.toString(this.lcdSelectedRow()));
        s.put("highlight_start", Integer.toString(this.lcdHighlightStart()));
        s.put("highlight_length", Integer.toString(this.lcdHighlightLength()));
        s.put("field", this.field);
        s.put("hint", this.formHint());
        s.put("input", !this.page.equals("pin") && !this.field.endsWith("pin") ? this.input : "");
        s.put("group", this.dialGroup);
        s.put("door_time", Integer.toString(this.doorSeconds));
        s.put("guard_alarms", Integer.toString(this.guardAlerts.size()));
        s.put("self_alarm", this.selfEmergency ? "1" : "0");
        s.put("registered", Integer.toString(this.linkedCount()));
        String[] lines = this.lcd();

        for (int i = 0; i < 8; i++) {
            s.put("lcd" + i, lines[i]);
        }

        return s;
    }

    public static void tick(Level l, BlockPos p, BlockState s, Kam305BlockEntity b) {
        if (!l.isClientSide && b.factoryTicks > 0) {
            b.factoryTicks--;
            if (b.factoryTicks == 0) {
                b.power = true;
                b.page = "main";
                b.changed();
            } else if (b.factoryTicks % 20 == 0) {
                b.changed();
            }
        } else {
            Am10kpBlockEntity.tick(l, p, s, b);
            if (b.guardPeer != null && b.page.equals("main")) {
                b.page = "calls";
            }

            if (b.lobbyPeer != null) {
                b.lobbyTicks++;
                if (b.page.equals("lobby_ring")) {
                    CallSounds.incoming(l, p, true, b.lobbyTicks - 1, (b.volume + 1) * 0.22F);
                }

                if (!b.validLobby(b.lobbyPeer) || b.lobbyTicks > (b.page.equals("lobby_ring") ? 600 : 3600)) {
                    b.hangUp();
                }
            }

            if (l instanceof ServerLevel sl) {
                for (Entry<BlockPos, Long> e : new HashMap<>(b.closingDoors).entrySet()) {
                    if (sl.getGameTime() >= e.getValue() && sl.isLoaded(e.getKey())) {
                        DoorControl.setOpen(sl, e.getKey(), false);
                        b.closingDoors.remove(e.getKey());
                    }
                }
            }

            if (b.ticks % 20 == 0) {
                b.changed();
            }
        }
    }

    @Override
    public void onBroken() {
        if (this.selfEmergency) {
            this.selfEmergency = false;
            this.broadcastEmergency();
        }

        if (this.level instanceof ServerLevel sl) {
            for (BlockPos p : this.closingDoors.keySet()) {
                if (sl.isLoaded(p)) {
                    DoorControl.setOpen(sl, p, false);
                }
            }
        }

        super.onBroken();
    }

    @Override
    protected void saveAdditional(CompoundTag t) {
        super.saveAdditional(t);
        t.putInt("Angle", this.angle);
        t.putInt("AngleSchema", 2);
        t.putInt("RecordTimeSchema", 2);
        t.putInt("FactoryTicks", this.factoryTicks);
        t.putBoolean("ClockInitialized", this.clockInitialized);
        t.putBoolean("SelfEmergency", this.selfEmergency);
        t.putString("Pin", this.pin);
        t.putString("Language", this.language);
        t.putInt("DoorTime", this.doorSeconds);
        t.putInt("VoiceVolume", this.voiceVolume);
        t.putInt("RelayId", this.relayId);
        t.putBoolean("Monitor", this.monitorMode);
        t.putBoolean("SetupId", this.setupId);
        t.putBoolean("Office", this.officeMode);
        t.putLong("ClockOffset", this.clockOffset);
        writeRecords(t, "History", this.history);
        writeRecords(t, "AlarmHistory", this.alarmHistory);
        ListTag list = new ListTag();
        this.lobbies.forEach((n, p) -> {
            CompoundTag c = new CompoundTag();
            c.putString("N", n);
            c.putLong("P", p.asLong());
            c.putString("Pin", this.lobbyPins.getOrDefault(n, ""));
            list.add(c);
        });
        t.put("Lobbies", list);
        CompoundTag ranges = new CompoundTag();
        this.videoRanges.forEach(ranges::putString);
        t.put("VideoRanges", ranges);
        ListTag slots = new ListTag();
        this.deviceSlots.forEach((pos, m) -> {
            CompoundTag c = new CompoundTag();
            c.putLong("P", pos.asLong());
            c.putInt("G", m.group);
            c.putInt("M", m.main);
            c.putInt("C", m.card);
            c.putInt("H", m.channel);
            c.putString("O", m.option);
            slots.add(c);
        });
        t.put("DeviceSlots", slots);
    }

    private static void writeRecords(CompoundTag t, String key, List<MissedCall> records) {
        ListTag list = new ListTag();

        for (MissedCall r : records) {
            CompoundTag c = new CompoundTag();
            c.putString("K", r.caller());
            c.putLong("T", r.dayTime());
            list.add(c);
        }

        t.put(key, list);
    }

    private void readRecords(CompoundTag t, String key, List<MissedCall> records) {
        records.clear();
        ListTag list = t.getList(key, 10);

        for (int i = 0; i < Math.min(500, list.size()); i++) {
            CompoundTag c = list.getCompound(i);
            if (!key.equals("History") || c.getString("K").matches("^(STD|ROU|EXC|OPN)\\|.*")) {
                records.add(new MissedCall(c.getString("K"), t.getInt("RecordTimeSchema") == 2 ? c.getLong("T") : this.clockOffset + c.getLong("T") / 20L));
            }
        }
    }

    @Override
    public void load(CompoundTag t) {
        super.load(t);
        this.angle = Math.max(
            0, Math.min(65, t.contains("Angle") ? (t.getInt("AngleSchema") == 2 ? t.getInt("Angle") : Math.max(0, 65 - t.getInt("Angle"))) : 0)
        );
        this.pin = t.contains("Pin") ? t.getString("Pin") : "ffff";
        this.language = t.contains("Language") ? t.getString("Language") : "ko";
        this.doorSeconds = Math.max(1, Math.min(10, t.contains("DoorTime") ? t.getInt("DoorTime") : 3));
        this.voiceVolume = t.contains("VoiceVolume") ? t.getInt("VoiceVolume") : 2;
        this.relayId = t.getInt("RelayId");
        this.monitorMode = t.getBoolean("Monitor");
        this.setupId = t.getBoolean("SetupId");
        this.officeMode = t.getBoolean("Office");
        this.clockOffset = t.contains("ClockOffset") ? t.getLong("ClockOffset") : 946684800L;
        this.clockInitialized = t.contains("ClockInitialized") ? t.getBoolean("ClockInitialized") : t.contains("ClockOffset");
        this.factoryTicks = Math.max(0, Math.min(700, t.getInt("FactoryTicks")));
        this.readRecords(t, "History", this.history);
        this.readRecords(t, "AlarmHistory", this.alarmHistory);
        this.lobbies.clear();
        this.lobbyPins.clear();

        for (Tag item : t.getList("Lobbies", 10)) {
            CompoundTag c = (CompoundTag)item;
            String id = normalize(c.getString("N"));
            this.lobbies.put(id, BlockPos.of(c.getLong("P")));
            this.lobbyPins.put(id, c.getString("Pin"));
        }

        this.videoRanges.clear();
        CompoundTag ranges = t.getCompound("VideoRanges");

        for (String k : ranges.getAllKeys()) {
            if (this.videoRanges.size() < 60) {
                this.videoRanges.put(k, ranges.getString(k));
            }
        }

        this.deviceSlots.clear();

        for (Tag entry : t.getList("DeviceSlots", 10)) {
            CompoundTag c = (CompoundTag)entry;
            BlockPos pos = BlockPos.of(c.getLong("P"));
            if (this.homes.containsKey(pos) || this.lobbies.containsValue(pos)) {
                this.deviceSlots.put(pos, new Kam305BlockEntity.DeviceSlot(c.getInt("G"), c.getInt("M"), c.getInt("C"), c.getInt("H"), c.getString("O")));
            }
        }

        this.filteredDevices = false;
        this.page = this.factoryTicks > 0 ? "factory" : "main";
        this.administrator = null;
        this.lobbyPeer = null;
        this.selfEmergency = t.getBoolean("SelfEmergency");
        this.safetyTypes.clear();
        this.guardAlerts.clear();
        this.handsfree = false;
        this.enterStarted = -1L;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag t = super.getUpdateTag();
        t.putInt("Angle", this.angle);
        t.putInt("FactoryTicks", this.factoryTicks);
        t.putInt("LCDSelection", this.lcdSelectedRow());
        t.putInt("LCDHighlightStart", this.lcdHighlightStart());
        t.putInt("LCDHighlightLength", this.lcdHighlightLength());
        t.putString("LCDPage", this.page);
        String[] lines = this.lcd();

        for (int i = 0; i < 8; i++) {
            t.putString("LCD" + i, lines[i]);
        }

        return t;
    }

    @Override
    public void handleUpdateTag(CompoundTag t) {
        super.handleUpdateTag(t);
        this.angle = t.getInt("Angle");
        this.factoryTicks = t.getInt("FactoryTicks");
        this.clientSelection = t.getInt("LCDSelection");
        this.clientHighlightStart = t.getInt("LCDHighlightStart");
        this.clientHighlightLength = t.getInt("LCDHighlightLength");
        this.clientPage = t.getString("LCDPage");

        for (int i = 0; i < 8; i++) {
            this.clientLcd[i] = t.getString("LCD" + i);
        }
    }

    private record DeviceSlot(int group, int main, int card, int channel, String option) {
    }
}
