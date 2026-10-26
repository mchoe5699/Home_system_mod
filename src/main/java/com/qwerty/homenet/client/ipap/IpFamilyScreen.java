package com.qwerty.homenet.client.ipap;

import com.qwerty.homenet.client.family.FamilyModelScreen;
import com.qwerty.homenet.network.WallpadDataPacket;
import java.util.List;

public final class IpFamilyScreen extends FamilyModelScreen {
    private static final List<FamilyModelScreen.Choice> CHOICES = List.of(
        new FamilyModelScreen.Choice("ip203ap", "IP-203AP", "STAR · 현관·경비"),
        new FamilyModelScreen.Choice("ip203apb", "IP-203APB", "BUS · 현관·경비·비상·부재"),
        new FamilyModelScreen.Choice("kip234r2", "KIP-234R2", "BUS · 현관·로비·경비·열쇠"),
        new FamilyModelScreen.Choice("ip200pc", "IP-200PC", "STAR · 경비·초인종"),
        new FamilyModelScreen.Choice("ip200pchb", "IP-200PCHB", "BUS · 경비·초인종·비상"),
        new FamilyModelScreen.Choice("dp202h", "DP-202H", "독립형 · 현관·문열림")
    );

    private static String model(WallpadDataPacket p) {
        return p.setting("family", p.flag("kip234r2") ? "kip234r2" : (p.flag("ip203apb") ? "ip203apb" : "ip203ap"));
    }

    public IpFamilyScreen(WallpadDataPacket p) {
        super(p, "IP-203AP", model(p), CHOICES);
    }

    @Override
    public void update(WallpadDataPacket p) {
        this.current(model(p));
        if (p.notice().equals("ip_family_changed")) {
            this.minecraft.setScreen(new Ip203apScreen(p));
        }
    }
}
