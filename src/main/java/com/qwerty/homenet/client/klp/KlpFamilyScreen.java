package com.qwerty.homenet.client.klp;

import com.qwerty.homenet.client.family.FamilyModelScreen;
import com.qwerty.homenet.klp.KlpModel;
import com.qwerty.homenet.network.WallpadDataPacket;
import java.util.Arrays;

public final class KlpFamilyScreen extends FamilyModelScreen {
    public KlpFamilyScreen(WallpadDataPacket p) {
        super(
            p,
            "KLP",
            p.setting("klp_model", "klp_203rf"),
            Arrays.stream(KlpModel.values())
                .map(
                    m -> new FamilyModelScreen.Choice(
                        m.id,
                        m.label,
                        "KOCOM BUS · " + (m.reader.equals("RF") ? "RF 카드" : (m.hasCard() ? "삽입형 카드" : "카드 없음")) + " · " + (m.colorVideo() ? "컬러" : "흑백")
                    )
                )
                .toList()
        );
    }

    @Override
    public void update(WallpadDataPacket p) {
        this.current(p.setting("klp_model", "klp_203rf"));
    }
}
