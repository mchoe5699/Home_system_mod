package com.qwerty.homenet.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "qwertys_homenet");
    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register(
        "main",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.qwertys_homenet"))
            .icon(() -> new ItemStack(ModItems.WALLPAD.get()))
            .displayItems((params, output) -> {
                output.accept(ModItems.WALLPAD.get());
                output.accept(ModItems.DOOR_STATION.get());
                output.accept(ModItems.DOOR_PHONE.get());
                output.accept(ModItems.KLP_203RF.get());
                output.accept(ModItems.KLP_202.get());
                output.accept(ModItems.KLP_202R2_EARLY.get());
                output.accept(ModItems.KLP_202R2_LATE.get());
                output.accept(ModItems.KLP_203.get());
                output.accept(ModItems.KLP_203R3S.get());
                output.accept(ModItems.KLP_204.get());
                output.accept(ModItems.KLP_204RF.get());
                output.accept(ModItems.VIDEO_PHONE.get());
                output.accept(ModItems.KVM_906.get());
                output.accept(ModItems.KVM_904.get());
                output.accept(ModItems.KVM_930.get());
                output.accept(ModItems.KVM_960.get());
                output.accept(ModItems.KVM_933.get());
                output.accept(ModItems.KVM_931.get());
                output.accept(ModItems.KVM_931_LAMP.get());
                output.accept(ModItems.KVM_932T.get());
                output.accept(ModItems.KVM_932.get());
                output.accept(ModItems.KVM_936.get());
                output.accept(ModItems.KVM_931T.get());
                output.accept(ModItems.KVM_940.get());
                output.accept(ModItems.KVM_940R3.get());
                output.accept(ModItems.MODEL_CHANGER.get());
                output.accept(ModItems.DEVICE_MOVER.get());
                output.accept(ModItems.ANALOG_LINKER.get());
                output.accept(ModItems.KVM_DESIGNER.get());
                output.accept(ModItems.INTERPHONE.get());
                output.accept(ModItems.DOORBELL.get());
                output.accept(ModItems.IP_203AP.get());
                output.accept(ModItems.IP_203APB.get());
                output.accept(ModItems.KIP_234R2.get());
                output.accept(ModItems.DP_202H.get());
                output.accept(ModItems.IP_200PCHB.get());
                output.accept(ModItems.IP_200PC.get());
                output.accept(ModItems.GUARD_CONSOLE.get());
                output.accept(ModItems.HA_4100H.get());
                output.accept(ModItems.HMC_7000.get());
                output.accept(ModItems.SHT_5500S.get());
                output.accept(ModItems.AM_10KP.get());
                output.accept(ModItems.KAM_305.get());
                output.accept(ModItems.CONTROL_BLOCK.get());
                output.accept(ModItems.SMART_LIGHT.get());
                output.accept(ModItems.RF_CARD.get());
                output.accept(ModItems.INSERT_CARD.get());
                output.accept(ModItems.HOME_LINKER.get());
                output.accept(ModItems.DOOR_LINKER.get());
                output.accept(ModItems.DASHBOARD.get());
            })
            .build()
    );

    private ModCreativeTab() {
    }
}
