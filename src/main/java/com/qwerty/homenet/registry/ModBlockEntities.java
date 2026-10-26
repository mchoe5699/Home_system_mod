package com.qwerty.homenet.registry;

import com.qwerty.homenet.blockentity.Am10kpBlockEntity;
import com.qwerty.homenet.blockentity.CompactInterphoneBlockEntity;
import com.qwerty.homenet.blockentity.DeviceBlockEntity;
import com.qwerty.homenet.blockentity.DoorStationBlockEntity;
import com.qwerty.homenet.blockentity.DoorbellBlockEntity;
import com.qwerty.homenet.blockentity.GuardMasterBlockEntity;
import com.qwerty.homenet.blockentity.Ha4100hBlockEntity;
import com.qwerty.homenet.blockentity.Hmc7000BlockEntity;
import com.qwerty.homenet.blockentity.Ip203apBlockEntity;
import com.qwerty.homenet.blockentity.Ip203apbBlockEntity;
import com.qwerty.homenet.blockentity.Kam305BlockEntity;
import com.qwerty.homenet.blockentity.Kip234r2BlockEntity;
import com.qwerty.homenet.blockentity.Klp203rfBlockEntity;
import com.qwerty.homenet.blockentity.Kvm906BlockEntity;
import com.qwerty.homenet.blockentity.LobbyPhoneBlockEntity;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.blockentity.Sht5500sBlockEntity;
import com.qwerty.homenet.blockentity.WallpadBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "qwertys_homenet");
    public static final RegistryObject<BlockEntityType<Klp203rfBlockEntity>> KLP_203RF = BLOCK_ENTITIES.register(
        "klp_203rf",
        () -> Builder.of(
                Klp203rfBlockEntity::new,
                ModBlocks.KLP_203RF.get(),
                ModBlocks.KLP_202.get(),
                ModBlocks.KLP_202R2_EARLY.get(),
                ModBlocks.KLP_202R2_LATE.get(),
                ModBlocks.KLP_203.get(),
                ModBlocks.KLP_203R3S.get(),
                ModBlocks.KLP_204.get(),
                ModBlocks.KLP_204RF.get()
            )
            .build(null)
    );
    public static final RegistryObject<BlockEntityType<DoorbellBlockEntity>> DOORBELL = BLOCK_ENTITIES.register(
        "doorbell", () -> Builder.of(DoorbellBlockEntity::new, ModBlocks.DOORBELL.get()).build(null)
    );
    public static final RegistryObject<BlockEntityType<Ip203apBlockEntity>> IP_203AP = BLOCK_ENTITIES.register(
        "ip_203ap", () -> Builder.of(Ip203apBlockEntity::new, ModBlocks.IP_203AP.get()).build(null)
    );
    public static final RegistryObject<BlockEntityType<Ip203apBlockEntity>> IP_203APB = BLOCK_ENTITIES.register(
        "ip_203apb", () -> Builder.<Ip203apBlockEntity>of(Ip203apbBlockEntity::new, ModBlocks.IP_203APB.get()).build(null)
    );
    public static final RegistryObject<BlockEntityType<Ip203apBlockEntity>> KIP_234R2 = BLOCK_ENTITIES.register(
        "kip_234r2", () -> Builder.<Ip203apBlockEntity>of(Kip234r2BlockEntity::new, ModBlocks.KIP_234R2.get()).build(null)
    );
    public static final RegistryObject<BlockEntityType<Ip203apBlockEntity>> DP_202H = BLOCK_ENTITIES.register(
        "dp_202h", () -> Builder.<Ip203apBlockEntity>of((p, s) -> new CompactInterphoneBlockEntity(ModBlockEntities.DP_202H.get(), p, s), ModBlocks.DP_202H.get()).build(null)
    );
    public static final RegistryObject<BlockEntityType<Ip203apBlockEntity>> IP_200PCHB = BLOCK_ENTITIES.register(
        "ip_200pchb", () -> Builder.<Ip203apBlockEntity>of((p, s) -> new CompactInterphoneBlockEntity(ModBlockEntities.IP_200PCHB.get(), p, s), ModBlocks.IP_200PCHB.get()).build(null)
    );
    public static final RegistryObject<BlockEntityType<Ip203apBlockEntity>> IP_200PC = BLOCK_ENTITIES.register(
        "ip_200pc", () -> Builder.<Ip203apBlockEntity>of((p, s) -> new CompactInterphoneBlockEntity(ModBlockEntities.IP_200PC.get(), p, s), ModBlocks.IP_200PC.get()).build(null)
    );
    public static final RegistryObject<BlockEntityType<Ha4100hBlockEntity>> HA_4100H = BLOCK_ENTITIES.register(
        "ha_4100h", () -> Builder.of(Ha4100hBlockEntity::new, ModBlocks.HA_4100H.get()).build(null)
    );
    public static final RegistryObject<BlockEntityType<Hmc7000BlockEntity>> HMC_7000 = BLOCK_ENTITIES.register(
        "hmc_7000", () -> Builder.of(Hmc7000BlockEntity::new, ModBlocks.HMC_7000.get()).build(null)
    );
    public static final RegistryObject<BlockEntityType<Sht5500sBlockEntity>> SHT_5500S = BLOCK_ENTITIES.register(
        "sht_5500s", () -> Builder.of(Sht5500sBlockEntity::new, ModBlocks.SHT_5500S.get()).build(null)
    );
    public static final RegistryObject<BlockEntityType<Kam305BlockEntity>> KAM_305 = BLOCK_ENTITIES.register(
        "kam_305", () -> Builder.of(Kam305BlockEntity::new, ModBlocks.KAM_305.get()).build(null)
    );
    public static final RegistryObject<BlockEntityType<Am10kpBlockEntity>> AM_10KP = BLOCK_ENTITIES.register(
        "am_10kp", () -> Builder.of(Am10kpBlockEntity::new, ModBlocks.AM_10KP.get()).build(null)
    );
    public static final RegistryObject<BlockEntityType<Kvm906BlockEntity>> KVM_906 = BLOCK_ENTITIES.register(
        "kvm_906",
        () -> Builder.of(
                Kvm906BlockEntity::new,
                ModBlocks.KVM_906.get(),
                ModBlocks.KVM_904.get(),
                ModBlocks.KVM_930.get(),
                ModBlocks.KVM_960.get(),
                ModBlocks.KVM_933.get(),
                ModBlocks.KVM_931.get(),
                ModBlocks.KVM_931_LAMP.get(),
                ModBlocks.KVM_932T.get(),
                ModBlocks.KVM_932.get(),
                ModBlocks.KVM_936.get(),
                ModBlocks.KVM_931T.get(),
                ModBlocks.KVM_940.get(),
                ModBlocks.KVM_940R3.get()
            )
            .build(null)
    );
    public static final RegistryObject<BlockEntityType<WallpadBlockEntity>> WALLPAD = BLOCK_ENTITIES.register(
        "wallpad", () -> Builder.of(WallpadBlockEntity::new, ModBlocks.WALLPAD.get()).build(null)
    );
    public static final RegistryObject<BlockEntityType<DoorStationBlockEntity>> DOOR_STATION = BLOCK_ENTITIES.register(
        "door_station", () -> Builder.of(DoorStationBlockEntity::new, ModBlocks.DOOR_STATION.get(), ModBlocks.DOOR_PHONE.get()).build(null)
    );
    public static final RegistryObject<BlockEntityType<ReceiverBlockEntity>> RECEIVER = BLOCK_ENTITIES.register(
        "receiver",
        () -> Builder.of(ReceiverBlockEntity::new, ModBlocks.VIDEO_PHONE.get(), ModBlocks.INTERPHONE.get(), ModBlocks.GUARD_CONSOLE.get()).build(null)
    );
    public static final RegistryObject<BlockEntityType<GuardMasterBlockEntity>> GUARD_MASTER = BLOCK_ENTITIES.register(
        "guard_master", () -> Builder.of(GuardMasterBlockEntity::new).build(null)
    );
    public static final RegistryObject<BlockEntityType<LobbyPhoneBlockEntity>> LOBBY_PHONE = BLOCK_ENTITIES.register(
        "lobby_phone", () -> Builder.of(LobbyPhoneBlockEntity::new).build(null)
    );
    public static final RegistryObject<BlockEntityType<DeviceBlockEntity>> DEVICE = BLOCK_ENTITIES.register(
        "device", () -> Builder.of(DeviceBlockEntity::new, ModBlocks.CONTROL_BLOCK.get(), ModBlocks.SMART_LIGHT.get()).build(null)
    );

    private ModBlockEntities() {
    }
}
