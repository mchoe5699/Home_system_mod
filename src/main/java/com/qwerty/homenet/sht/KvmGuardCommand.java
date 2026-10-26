package com.qwerty.homenet.sht;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.qwerty.homenet.blockentity.Kvm906BlockEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
    modid = "homenet_sht101c"
)
public final class KvmGuardCommand {
    private KvmGuardCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher()
            .register(
                (LiteralArgumentBuilder)Commands.literal("shtguard")
                    .then(
                        Commands.argument("number", StringArgumentType.word())
                            .executes(
                                ctx -> {
                                    ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayerOrException();
                                    if (player.pick(5.0, 0.0F, false) instanceof BlockHitResult block
                                        && player.level().getBlockEntity(block.getBlockPos()) instanceof Kvm906BlockEntity phone) {
                                        String number = StringArgumentType.getString(ctx, "number");
                                        if (!number.matches("[0-9]{1,4}")) {
                                            player.displayClientMessage(Component.literal("경비실 번호는 숫자 1~4자리입니다"), true);
                                            return 0;
                                        } else {
                                            phone.getPersistentData().putString("ShtPreferredGuard", number);
                                            if (phone.getUnit().isBlank() && player.level() instanceof ServerLevel server) {
                                                String assigned = GuardRouting.randomUnit(null, server, phone.getBlockPos(), phone.roomOnlyUnit());
                                                CompoundTag tag = phone.saveWithoutMetadata();
                                                tag.putString("Unit", assigned);
                                                phone.load(tag);
                                                phone.onLoad();
                                            }

                                            phone.setChanged();
                                            phone.syncScreens();
                                            player.displayClientMessage(Component.literal("지정 경비실 번호: " + number + " · 세대 " + phone.getUnit()), true);
                                            return 1;
                                        }
                                    } else {
                                        player.displayClientMessage(Component.literal("코콤 비디오폰을 바라보고 번호를 입력하세요"), true);
                                        return 0;
                                    }
                                }
                            )
                    )
            );
    }
}
