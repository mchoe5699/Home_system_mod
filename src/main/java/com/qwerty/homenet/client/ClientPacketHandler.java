package com.qwerty.homenet.client;

import com.qwerty.homenet.block.GuardMasterBlock;
import com.qwerty.homenet.client.am.Am10kpScreen;
import com.qwerty.homenet.client.dashboard.DashboardScreen;
import com.qwerty.homenet.client.guard.GuardMasterScreen;
import com.qwerty.homenet.client.ha.Ha4100hConfigScreen;
import com.qwerty.homenet.client.hmc.Hmc7000Screen;
import com.qwerty.homenet.client.ipap.Ip203apScreen;
import com.qwerty.homenet.client.ipap.IpFamilyScreen;
import com.qwerty.homenet.client.kam.Kam305Screen;
import com.qwerty.homenet.client.klp.Klp203rfScreen;
import com.qwerty.homenet.client.klp.KlpFamilyScreen;
import com.qwerty.homenet.client.kvm.Kvm906Screen;
import com.qwerty.homenet.client.kvm.ModelSelectionScreen;
import com.qwerty.homenet.client.logo.LogoScreen;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.network.DashboardDataPacket;
import com.qwerty.homenet.network.DeviceConfigOpenPacket;
import com.qwerty.homenet.network.DoorStationDataPacket;
import com.qwerty.homenet.network.WallpadDataPacket;
import net.minecraft.client.Minecraft;

public final class ClientPacketHandler {
    private ClientPacketHandler() {
    }

    public static void handleWallpad(WallpadDataPacket p) {
        Minecraft mc = Minecraft.getInstance();
        if (p.open() && p.notice().equals("klp_models")) {
            mc.setScreen(new KlpFamilyScreen(p));
        } else if (p.flag("klp203rf")) {
            if (mc.screen instanceof Klp203rfScreen s && s.getPos().equals(p.pos())) {
                s.update(p);
            } else if (p.open()) {
                mc.setScreen(new Klp203rfScreen(p));
            }
        } else if (p.open() && p.notice().equals("ip_family_models")) {
            mc.setScreen(new IpFamilyScreen(p));
        } else if (p.open() && p.notice().equals("ip_family_changed")) {
            mc.setScreen(new Ip203apScreen(p));
        } else if (p.open() && p.notice().equals("logo_editor")) {
            mc.setScreen(new LogoScreen(p));
        } else if (p.open() && p.notice().equals("kvm_models")) {
            mc.setScreen(new ModelSelectionScreen(p));
        } else {
            if (mc.screen instanceof ReceiverScreen s && s.getPos().equals(p.pos())) {
                s.update(p);
            } else if (p.open()) {
                if (p.flag("ip203ap")) {
                    mc.setScreen(new Ip203apScreen(p));
                    return;
                }

                if (p.flag("ha4100h_config")) {
                    mc.setScreen(new Ha4100hConfigScreen(p));
                    return;
                }

                if (p.flag("hmc7000")) {
                    mc.setScreen(new Hmc7000Screen(p));
                    return;
                }

                if (p.flag("kam305")) {
                    mc.setScreen(new Kam305Screen(p));
                    return;
                }

                if (p.flag("am10kp")) {
                    mc.setScreen(new Am10kpScreen(p));
                    return;
                }

                if (p.flag("kvm")) {
                    mc.setScreen(new Kvm906Screen(p));
                    return;
                }

                boolean wallpad = DeviceRegistry.Kind.byId(p.kind()) == DeviceRegistry.Kind.WALLPAD;
                boolean guardMaster = mc.level != null && mc.level.getBlockState(p.pos()).getBlock() instanceof GuardMasterBlock;
                if (guardMaster) {
                    mc.setScreen(new GuardMasterScreen(p));
                } else {
                    mc.setScreen(new WallpadScreen(p));
                }
            }
        }
    }

    public static void handleDoorStation(DoorStationDataPacket p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof DoorStationScreen s && s.getPos().equals(p.pos())) {
            s.update(p);
        } else if (p.open()) {
            mc.setScreen(new DoorStationScreen(p));
        }
    }

    public static void handleDashboard(DashboardDataPacket p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof DashboardScreen s) {
            s.update(p);
        } else if (p.open()) {
            mc.setScreen(new DashboardScreen(p));
        }
    }

    public static void handleDeviceConfig(DeviceConfigOpenPacket p) {
        Minecraft.getInstance().setScreen(new DeviceConfigScreen(p));
    }
}
