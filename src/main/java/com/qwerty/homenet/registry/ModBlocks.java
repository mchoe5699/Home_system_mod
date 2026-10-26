package com.qwerty.homenet.registry;

import com.qwerty.homenet.block.Am10kpBlock;
import com.qwerty.homenet.block.CompactInterphoneBlock;
import com.qwerty.homenet.block.ControlBlock;
import com.qwerty.homenet.block.DoorPhoneBlock;
import com.qwerty.homenet.block.DoorStationBlock;
import com.qwerty.homenet.block.DoorbellBlock;
import com.qwerty.homenet.block.Ha4100hBlock;
import com.qwerty.homenet.block.Hmc7000Block;
import com.qwerty.homenet.block.Ip203apBlock;
import com.qwerty.homenet.block.Kam305Block;
import com.qwerty.homenet.block.Klp203rfBlock;
import com.qwerty.homenet.block.Kvm906Block;
import com.qwerty.homenet.block.ReceiverBlock;
import com.qwerty.homenet.block.Sht5500sBlock;
import com.qwerty.homenet.block.SmartLightBlock;
import com.qwerty.homenet.block.WallpadBlock;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.klp.KlpModel;
import com.qwerty.homenet.kvm.KvmModel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "qwertys_homenet");
    public static final RegistryObject<Hmc7000Block> HMC_7000 = BLOCKS.register(
        "hmc_7000", () -> new Hmc7000Block(Properties.of().mapColor(MapColor.QUARTZ).strength(1.5F).sound(SoundType.METAL).noOcclusion())
    );
    public static final RegistryObject<Sht5500sBlock> SHT_5500S = BLOCKS.register(
        "sht_5500s", () -> new Sht5500sBlock(Properties.of().mapColor(MapColor.SAND).strength(1.5F).sound(SoundType.METAL).noOcclusion())
    );
    public static final RegistryObject<Klp203rfBlock> KLP_203RF = BLOCKS.register(
        "klp_203rf", () -> new Klp203rfBlock(Properties.of().mapColor(MapColor.METAL).strength(1.5F).sound(SoundType.METAL).noOcclusion())
    );
    public static final RegistryObject<Klp203rfBlock> KLP_202 = BLOCKS.register(
        "klp_202", () -> new Klp203rfBlock(Properties.of().mapColor(MapColor.METAL).strength(1.5F).sound(SoundType.METAL).noOcclusion(), KlpModel.NO_CARD)
    );
    public static final RegistryObject<Klp203rfBlock> KLP_202R2_EARLY = BLOCKS.register(
        "klp_202r2_early",
        () -> new Klp203rfBlock(Properties.of().mapColor(MapColor.METAL).strength(1.5F).sound(SoundType.METAL).noOcclusion(), KlpModel.EARLY)
    );
    public static final RegistryObject<Klp203rfBlock> KLP_202R2_LATE = BLOCKS.register(
        "klp_202r2_late", () -> new Klp203rfBlock(Properties.of().mapColor(MapColor.METAL).strength(1.5F).sound(SoundType.METAL).noOcclusion(), KlpModel.LATE)
    );
    public static final RegistryObject<Klp203rfBlock> KLP_203 = BLOCKS.register(
        "klp_203", () -> new Klp203rfBlock(Properties.of().mapColor(MapColor.METAL).strength(1.5F).sound(SoundType.METAL).noOcclusion(), KlpModel.BASE)
    );
    public static final RegistryObject<Klp203rfBlock> KLP_203R3S = BLOCKS.register(
        "klp_203r3s", () -> new Klp203rfBlock(Properties.of().mapColor(MapColor.METAL).strength(1.5F).sound(SoundType.METAL).noOcclusion(), KlpModel.R3S)
    );
    public static final RegistryObject<Klp203rfBlock> KLP_204 = BLOCKS.register(
        "klp_204", () -> new Klp203rfBlock(Properties.of().mapColor(MapColor.METAL).strength(1.5F).sound(SoundType.METAL).noOcclusion(), KlpModel.COLOR)
    );
    public static final RegistryObject<Klp203rfBlock> KLP_204RF = BLOCKS.register(
        "klp_204rf", () -> new Klp203rfBlock(Properties.of().mapColor(MapColor.METAL).strength(1.5F).sound(SoundType.METAL).noOcclusion(), KlpModel.COLOR_RF)
    );
    public static final RegistryObject<DoorbellBlock> DOORBELL = BLOCKS.register(
        "doorbell", () -> new DoorbellBlock(Properties.of().mapColor(MapColor.QUARTZ).strength(0.5F).sound(SoundType.METAL).noOcclusion())
    );
    public static final RegistryObject<Ip203apBlock> IP_203AP = BLOCKS.register(
        "ip_203ap", () -> new Ip203apBlock(Properties.of().mapColor(MapColor.QUARTZ).strength(1.0F).sound(SoundType.METAL).noOcclusion().dynamicShape())
    );
    public static final RegistryObject<Ip203apBlock> IP_203APB = BLOCKS.register(
        "ip_203apb", () -> new Ip203apBlock(Properties.of().mapColor(MapColor.QUARTZ).strength(1.0F).sound(SoundType.METAL).noOcclusion().dynamicShape(), true)
    );
    public static final RegistryObject<Ip203apBlock> KIP_234R2 = BLOCKS.register(
        "kip_234r2",
        () -> new Ip203apBlock(Properties.of().mapColor(MapColor.QUARTZ).strength(1.0F).sound(SoundType.METAL).noOcclusion().dynamicShape(), true, true)
    );
    public static final RegistryObject<CompactInterphoneBlock> DP_202H = BLOCKS.register(
        "dp_202h",
        () -> new CompactInterphoneBlock(Properties.of().mapColor(MapColor.QUARTZ).strength(1.0F).sound(SoundType.METAL).noOcclusion().dynamicShape(), "dp202h")
    );
    public static final RegistryObject<CompactInterphoneBlock> IP_200PCHB = BLOCKS.register(
        "ip_200pchb",
        () -> new CompactInterphoneBlock(
            Properties.of().mapColor(MapColor.QUARTZ).strength(1.0F).sound(SoundType.METAL).noOcclusion().dynamicShape(), "ip200pchb"
        )
    );
    public static final RegistryObject<CompactInterphoneBlock> IP_200PC = BLOCKS.register(
        "ip_200pc",
        () -> new CompactInterphoneBlock(
            Properties.of().mapColor(MapColor.QUARTZ).strength(1.0F).sound(SoundType.METAL).noOcclusion().dynamicShape(), "ip200pc"
        )
    );
    public static final RegistryObject<Am10kpBlock> AM_10KP = BLOCKS.register(
        "am_10kp", () -> new Am10kpBlock(Properties.of().mapColor(MapColor.QUARTZ).strength(1.5F).sound(SoundType.METAL).noOcclusion())
    );
    public static final RegistryObject<Kam305Block> KAM_305 = BLOCKS.register(
        "kam_305", () -> new Kam305Block(Properties.of().mapColor(MapColor.QUARTZ).strength(1.5F).sound(SoundType.METAL).noOcclusion())
    );
    public static final RegistryObject<Ha4100hBlock> HA_4100H = BLOCKS.register(
        "ha_4100h", () -> new Ha4100hBlock(Properties.of().mapColor(MapColor.QUARTZ).strength(1.5F).sound(SoundType.METAL).noOcclusion())
    );
    public static final RegistryObject<WallpadBlock> WALLPAD = BLOCKS.register(
        "wallpad",
        () -> new WallpadBlock(
            Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_GRAY)
                .strength(1.0F)
                .sound(SoundType.METAL)
                .noOcclusion()
                .lightLevel(state -> state.getValue(WallpadBlock.RINGING) ? 7 : 2)
        )
    );
    public static final RegistryObject<DoorStationBlock> DOOR_STATION = BLOCKS.register(
        "door_station",
        () -> new DoorStationBlock(Properties.of().mapColor(MapColor.COLOR_GRAY).strength(1.0F).sound(SoundType.METAL).noOcclusion().lightLevel(state -> 1))
    );
    public static final RegistryObject<DoorPhoneBlock> DOOR_PHONE = BLOCKS.register(
        "door_phone",
        () -> new DoorPhoneBlock(Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(1.0F).sound(SoundType.METAL).noOcclusion().dynamicShape())
    );
    public static final RegistryObject<ReceiverBlock> VIDEO_PHONE = BLOCKS.register(
        "video_phone",
        () -> new ReceiverBlock(
            Properties.of()
                .mapColor(MapColor.QUARTZ)
                .strength(1.0F)
                .sound(SoundType.METAL)
                .noOcclusion()
                .lightLevel(s -> s.getValue(WallpadBlock.RINGING) ? 7 : 1),
            DeviceRegistry.Kind.VIDEO_PHONE,
            6.0,
            4.0,
            12.0,
            1.0,
            false
        )
    );
    public static final RegistryObject<Kvm906Block> KVM_906 = BLOCKS.register(
        "kvm_906", () -> new Kvm906Block(Properties.of().mapColor(MapColor.QUARTZ).strength(1.0F).sound(SoundType.METAL).noOcclusion().dynamicShape())
    );
    public static final RegistryObject<Kvm906Block> KVM_904 = kvm("kvm_904", KvmModel.KVM_904);
    public static final RegistryObject<Kvm906Block> KVM_930 = kvm("kvm_930", KvmModel.KVM_930);
    public static final RegistryObject<Kvm906Block> KVM_960 = kvm("kvm_960", KvmModel.KVM_960);
    public static final RegistryObject<Kvm906Block> KVM_931 = kvm("kvm_931", KvmModel.KVM_931);
    public static final RegistryObject<Kvm906Block> KVM_932T = kvm("kvm_932t", KvmModel.KVM_932T);
    public static final RegistryObject<Kvm906Block> KVM_931T = kvm("kvm_931t", KvmModel.KVM_931T);
    public static final RegistryObject<Kvm906Block> KVM_936 = kvm("kvm_936", KvmModel.KVM_936);
    public static final RegistryObject<Kvm906Block> KVM_932 = kvm("kvm_932", KvmModel.KVM_932);
    public static final RegistryObject<Kvm906Block> KVM_931_LAMP = kvm("kvm_931_screen", KvmModel.KVM_931_LAMP);
    public static final RegistryObject<Kvm906Block> KVM_933 = kvm("kvm_933", KvmModel.KVM_933);
    public static final RegistryObject<Kvm906Block> KVM_940 = kvm("kvm_940", KvmModel.KVM_940);
    public static final RegistryObject<Kvm906Block> KVM_940R3 = kvm("kvm_940r3", KvmModel.KVM_940R3);
    public static final RegistryObject<ReceiverBlock> INTERPHONE = BLOCKS.register(
        "interphone",
        () -> new ReceiverBlock(
            Properties.of()
                .mapColor(MapColor.QUARTZ)
                .strength(1.0F)
                .sound(SoundType.METAL)
                .noOcclusion()
                .lightLevel(s -> s.getValue(WallpadBlock.RINGING) ? 5 : 0),
            DeviceRegistry.Kind.INTERPHONE,
            3.0,
            3.0,
            14.0,
            2.0,
            false
        )
    );
    public static final RegistryObject<ReceiverBlock> GUARD_CONSOLE = BLOCKS.register(
        "guard_console",
        () -> new ReceiverBlock(
            Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .strength(1.5F)
                .sound(SoundType.METAL)
                .noOcclusion()
                .lightLevel(s -> s.getValue(WallpadBlock.RINGING) ? 7 : 2),
            DeviceRegistry.Kind.GUARD_CONSOLE,
            7.0,
            0.0,
            7.0,
            12.0,
            true
        )
    );
    public static final RegistryObject<ControlBlock> CONTROL_BLOCK = BLOCKS.register(
        "control_block", () -> new ControlBlock(Properties.of().mapColor(MapColor.METAL).strength(2.0F).sound(SoundType.METAL), true)
    );
    public static final RegistryObject<SmartLightBlock> SMART_LIGHT = BLOCKS.register(
        "smart_light",
        () -> new SmartLightBlock(
            Properties.of()
                .mapColor(MapColor.QUARTZ)
                .strength(0.6F)
                .sound(SoundType.GLASS)
                .lightLevel(state -> state.getValue(BlockStateProperties.POWERED) ? 15 : 0)
        )
    );

    private static RegistryObject<Kvm906Block> kvm(String id, KvmModel model) {
        return BLOCKS.register(
            id, () -> new Kvm906Block(Properties.of().mapColor(MapColor.QUARTZ).strength(1.0F).sound(SoundType.METAL).noOcclusion().dynamicShape(), model)
        );
    }

    private ModBlocks() {
    }
}
