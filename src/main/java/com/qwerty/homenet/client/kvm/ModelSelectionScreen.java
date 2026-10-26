package com.qwerty.homenet.client.kvm;

import com.qwerty.homenet.client.family.FamilyModelScreen;
import com.qwerty.homenet.kvm.KvmModel;
import com.qwerty.homenet.network.WallpadDataPacket;
import java.util.Arrays;

public final class ModelSelectionScreen extends FamilyModelScreen {
    public ModelSelectionScreen(WallpadDataPacket p) {
        super(
            p,
            KvmModel.saved(p.setting("model", "906")).nativeCase()
                ? (KvmModel.saved(p.setting("model", "906")).is932() ? "KVM-931T / 932T" : "KVM-931 / 932 / 936")
                : "KVM",
            p.setting("model", "906"),
            Arrays.stream(KvmModel.values())
                .filter(m -> KvmModel.saved(p.setting("model", "906")).sameFamily(m))
                .map(m -> new FamilyModelScreen.Choice(m.id, m.label(), description(m)))
                .toList()
        );
    }

    private static String description(KvmModel m) {
        return switch (m) {
            case KVM_906 -> "현관·경비 / STAR · 모든 제조사";
            case KVM_904 -> "현관·문열림 / 경비 기능 없음";
            case KVM_932 -> "BUS · 비상·외출·경비·현관/LP·문열림";
            case KVM_936 -> "BUS · 호 번호 전용 · 경비·로비·현관·문열림";
            case KVM_931T -> "BUS · 국선·보류·재다이얼 · 문열림 없음";
            case KVM_932T -> "BUS · 국선·보류·재다이얼 · 현관/로비·경비";
            case KVM_931_LAMP -> "4선 BUS · 화면 버튼·상태 램프형";
            case KVM_931 -> "4선 BUS · 흑백 CRT · 비상·외출·현관·경비";
            case KVM_930 -> "비상·외출 / BUS · 코콤 경비실기";
            case KVM_960 -> "현관·경비실 / BUS · 코콤 경비실기";
            case KVM_933 -> "현관/LP·문열림 / BUS · 코콤 경비실기";
            case KVM_940 -> "국선 다이얼·현관 개폐 / STAR";
            case KVM_940R3 -> "국선 다이얼·로비 개폐 / KOCOM BUS";
        };
    }

    @Override
    public void update(WallpadDataPacket p) {
        this.current(p.setting("model", "906"));
        if (p.notice().equals("kvm_model_changed")) {
            this.minecraft.setScreen(new Kvm906Screen(p));
        }
    }
}
