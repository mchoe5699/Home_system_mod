package com.qwerty.homenet.registry;

import com.qwerty.homenet.item.AnalogLinkerItem;
import com.qwerty.homenet.item.DashboardItem;
import com.qwerty.homenet.item.DeviceMoverItem;
import com.qwerty.homenet.item.DoorLinkerItem;
import com.qwerty.homenet.item.HomeLinkerItem;
import com.qwerty.homenet.item.InsertCardItem;
import com.qwerty.homenet.item.KvmDesignItem;
import com.qwerty.homenet.item.ModelChangerItem;
import com.qwerty.homenet.item.RfCardItem;
import com.qwerty.homenet.kvm.KvmModel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "qwertys_homenet");
    public static final RegistryObject<Item> DOORBELL = ITEMS.register("doorbell", () -> new BlockItem(ModBlocks.DOORBELL.get(), new Properties()));
    public static final RegistryObject<Item> IP_203AP = ITEMS.register("ip_203ap", () -> new BlockItem(ModBlocks.IP_203AP.get(), new Properties()));
    public static final RegistryObject<Item> IP_203APB = ITEMS.register("ip_203apb", () -> new BlockItem(ModBlocks.IP_203APB.get(), new Properties()));
    public static final RegistryObject<Item> KIP_234R2 = ITEMS.register("kip_234r2", () -> new BlockItem(ModBlocks.KIP_234R2.get(), new Properties()));
    public static final RegistryObject<Item> DP_202H = ITEMS.register("dp_202h", () -> new BlockItem(ModBlocks.DP_202H.get(), new Properties()));
    public static final RegistryObject<Item> IP_200PCHB = ITEMS.register("ip_200pchb", () -> new BlockItem(ModBlocks.IP_200PCHB.get(), new Properties()));
    public static final RegistryObject<Item> IP_200PC = ITEMS.register("ip_200pc", () -> new BlockItem(ModBlocks.IP_200PC.get(), new Properties()));
    public static final RegistryObject<Item> KAM_305 = ITEMS.register("kam_305", () -> new BlockItem(ModBlocks.KAM_305.get(), new Properties()));
    public static final RegistryObject<Item> HA_4100H = ITEMS.register("ha_4100h", () -> new BlockItem(ModBlocks.HA_4100H.get(), new Properties()));
    public static final RegistryObject<Item> HMC_7000 = ITEMS.register("hmc_7000", () -> new BlockItem(ModBlocks.HMC_7000.get(), new Properties()));
    public static final RegistryObject<Item> SHT_5500S = ITEMS.register("sht_5500s", () -> new BlockItem(ModBlocks.SHT_5500S.get(), new Properties()));
    public static final RegistryObject<Item> AM_10KP = ITEMS.register("am_10kp", () -> new BlockItem(ModBlocks.AM_10KP.get(), new Properties()));
    public static final RegistryObject<Item> KVM_906 = ITEMS.register("kvm_906", () -> new BlockItem(ModBlocks.KVM_906.get(), new Properties()));
    public static final RegistryObject<Item> KVM_DESIGNER = ITEMS.register("kvm_designer", () -> new KvmDesignItem(new Properties().stacksTo(1), false));
    public static final RegistryObject<Item> KVM_904 = ITEMS.register("kvm_904", () -> new BlockItem(ModBlocks.KVM_904.get(), new Properties()));
    public static final RegistryObject<Item> KVM_930 = ITEMS.register("kvm_930", () -> new BlockItem(ModBlocks.KVM_930.get(), new Properties()));
    public static final RegistryObject<Item> KVM_960 = ITEMS.register("kvm_960", () -> new BlockItem(ModBlocks.KVM_960.get(), new Properties()));
    public static final RegistryObject<Item> KVM_931 = ITEMS.register("kvm_931", () -> new BlockItem(ModBlocks.KVM_931.get(), new Properties()));
    public static final RegistryObject<Item> KVM_932T = ITEMS.register("kvm_932t", () -> new BlockItem(ModBlocks.KVM_932T.get(), new Properties()));
    public static final RegistryObject<Item> KVM_931T = ITEMS.register("kvm_931t", () -> new BlockItem(ModBlocks.KVM_931T.get(), new Properties()));
    public static final RegistryObject<Item> KVM_936 = ITEMS.register("kvm_936", () -> new BlockItem(ModBlocks.KVM_936.get(), new Properties()));
    public static final RegistryObject<Item> KVM_932 = ITEMS.register("kvm_932", () -> new BlockItem(ModBlocks.KVM_932.get(), new Properties()));
    public static final RegistryObject<Item> KVM_931_LAMP = ITEMS.register(
        "kvm_931_screen", () -> new BlockItem(ModBlocks.KVM_931_LAMP.get(), new Properties())
    );
    public static final RegistryObject<Item> KVM_933 = ITEMS.register("kvm_933", () -> new BlockItem(ModBlocks.KVM_933.get(), new Properties()));
    public static final RegistryObject<Item> KVM_940 = ITEMS.register("kvm_940", () -> new BlockItem(ModBlocks.KVM_940.get(), new Properties()));
    public static final RegistryObject<Item> KVM_940R3 = ITEMS.register("kvm_940r3", () -> new BlockItem(ModBlocks.KVM_940R3.get(), new Properties()));
    public static final RegistryObject<Item> MODEL_CHANGER = ITEMS.register("model_changer", () -> new ModelChangerItem(new Properties().stacksTo(1)));
    public static final RegistryObject<Item> ANALOG_LINKER = ITEMS.register("analog_linker", () -> new AnalogLinkerItem(new Properties().stacksTo(1)));
    public static final RegistryObject<Item> WALLPAD = ITEMS.register("wallpad", () -> new BlockItem(ModBlocks.WALLPAD.get(), new Properties()));
    public static final RegistryObject<Item> DOOR_STATION = ITEMS.register("door_station", () -> new BlockItem(ModBlocks.DOOR_STATION.get(), new Properties()));
    public static final RegistryObject<Item> DEVICE_MOVER = ITEMS.register("device_mover", () -> new DeviceMoverItem(new Properties().stacksTo(1)));
    public static final RegistryObject<Item> KLP_202 = ITEMS.register("klp_202", () -> new BlockItem(ModBlocks.KLP_202.get(), new Properties()));
    public static final RegistryObject<Item> KLP_202R2_EARLY = ITEMS.register(
        "klp_202r2_early", () -> new BlockItem(ModBlocks.KLP_202R2_EARLY.get(), new Properties())
    );
    public static final RegistryObject<Item> KLP_202R2_LATE = ITEMS.register(
        "klp_202r2_late", () -> new BlockItem(ModBlocks.KLP_202R2_LATE.get(), new Properties())
    );
    public static final RegistryObject<Item> KLP_203 = ITEMS.register("klp_203", () -> new BlockItem(ModBlocks.KLP_203.get(), new Properties()));
    public static final RegistryObject<Item> KLP_203R3S = ITEMS.register("klp_203r3s", () -> new BlockItem(ModBlocks.KLP_203R3S.get(), new Properties()));
    public static final RegistryObject<Item> KLP_204 = ITEMS.register("klp_204", () -> new BlockItem(ModBlocks.KLP_204.get(), new Properties()));
    public static final RegistryObject<Item> KLP_204RF = ITEMS.register("klp_204rf", () -> new BlockItem(ModBlocks.KLP_204RF.get(), new Properties()));
    public static final RegistryObject<Item> INSERT_CARD = ITEMS.register("insert_card", () -> new InsertCardItem(new Properties().stacksTo(1)));
    public static final RegistryObject<Item> KLP_203RF = ITEMS.register("klp_203rf", () -> new BlockItem(ModBlocks.KLP_203RF.get(), new Properties()));
    public static final RegistryObject<Item> DOOR_PHONE = ITEMS.register("door_phone", () -> new BlockItem(ModBlocks.DOOR_PHONE.get(), new Properties()));
    public static final RegistryObject<Item> VIDEO_PHONE = ITEMS.register("video_phone", () -> new BlockItem(ModBlocks.VIDEO_PHONE.get(), new Properties()));
    public static final RegistryObject<Item> INTERPHONE = ITEMS.register("interphone", () -> new BlockItem(ModBlocks.INTERPHONE.get(), new Properties()));
    public static final RegistryObject<Item> GUARD_CONSOLE = ITEMS.register(
        "guard_console", () -> new BlockItem(ModBlocks.GUARD_CONSOLE.get(), new Properties())
    );
    public static final RegistryObject<Item> CONTROL_BLOCK = ITEMS.register(
        "control_block", () -> new BlockItem(ModBlocks.CONTROL_BLOCK.get(), new Properties())
    );
    public static final RegistryObject<Item> SMART_LIGHT = ITEMS.register("smart_light", () -> new BlockItem(ModBlocks.SMART_LIGHT.get(), new Properties()));
    public static final RegistryObject<RfCardItem> RF_CARD = ITEMS.register("rf_card", () -> new RfCardItem(new Properties().stacksTo(1)));
    public static final RegistryObject<DoorLinkerItem> DOOR_LINKER = ITEMS.register("door_linker", () -> new DoorLinkerItem(new Properties().stacksTo(1)));
    public static final RegistryObject<DashboardItem> DASHBOARD = ITEMS.register("dashboard", () -> new DashboardItem(new Properties().stacksTo(1)));
    public static final RegistryObject<HomeLinkerItem> HOME_LINKER = ITEMS.register("home_linker", () -> new HomeLinkerItem(new Properties().stacksTo(1)));

    public static Item kvmItem(KvmModel model) {
        return switch (model) {
            case KVM_904 -> (Item)KVM_904.get();
            case KVM_930 -> (Item)KVM_930.get();
            case KVM_960 -> (Item)KVM_960.get();
            case KVM_931_LAMP -> (Item)KVM_931_LAMP.get();
            case KVM_932 -> (Item)KVM_932.get();
            case KVM_936 -> (Item)KVM_936.get();
            case KVM_931T -> (Item)KVM_931T.get();
            case KVM_932T -> (Item)KVM_932T.get();
            case KVM_931 -> (Item)KVM_931.get();
            case KVM_933 -> (Item)KVM_933.get();
            case KVM_940 -> (Item)KVM_940.get();
            case KVM_940R3 -> (Item)KVM_940R3.get();
            default -> (Item)KVM_906.get();
        };
    }

    private ModItems() {
    }
}
