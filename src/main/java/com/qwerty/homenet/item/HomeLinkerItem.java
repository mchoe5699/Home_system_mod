package com.qwerty.homenet.item;

import com.qwerty.homenet.blockentity.DoorStationBlockEntity;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.data.ZoneData;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public class HomeLinkerItem extends Item {
    private static final String TAG_POS = "Wallpad";
    private static final String TAG_DIM = "Dim";

    public HomeLinkerItem(Properties props) {
        super(props);
    }

    private static MutableComponent msg(String key, Object... args) {
        return Component.translatable("msg.qwertys_homenet." + key, args);
    }

    @Nullable
    private static BlockPos selected(ItemStack stack, Level level) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("Wallpad")) {
            return !tag.getString("Dim").equals(level.dimension().location().toString()) ? null : BlockPos.of(tag.getLong("Wallpad"));
        } else {
            return null;
        }
    }

    private static boolean indoor(ReceiverBlockEntity receiver) {
        return switch (receiver.kind()) {
            case WALLPAD, VIDEO_PHONE, INTERPHONE -> true;
            default -> false;
        };
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        return this.useOn(ctx);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        } else {
            BlockPos pos = ctx.getClickedPos();
            BlockEntity be = level.getBlockEntity(pos);
            boolean indoor = be instanceof ReceiverBlockEntity r && indoor(r);
            if (!indoor && !(be instanceof DoorStationBlockEntity)) {
                if (!level.isClientSide) {
                    player.displayClientMessage(Component.literal("홈 링커는 세대 내부 기기와 도어카메라·도어폰만 연결합니다. 경비실은 전용 경비실 링크 도구를 사용하세요."), true);
                }

                return InteractionResult.FAIL;
            } else if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            } else {
                ItemStack stack = ctx.getItemInHand();
                if (be instanceof ReceiverBlockEntity wp && indoor(wp)) {
                    CompoundTag tag = stack.getOrCreateTag();
                    tag.putLong("Wallpad", pos.asLong());
                    tag.putString("Dim", level.dimension().location().toString());
                    String unit = wp.getUnit().isEmpty() ? "-" : wp.getUnit();
                    player.displayClientMessage(msg("linker.selected", unit, pos.toShortString()).withStyle(ChatFormatting.AQUA), true);
                    ding(level, pos, 1.5F);
                    return InteractionResult.CONSUME;
                } else {
                    BlockPos wpPos = selected(stack, level);
                    ReceiverBlockEntity receiver = wpPos != null && level.isLoaded(wpPos) && level.getBlockEntity(wpPos) instanceof ReceiverBlockEntity rx
                        ? rx
                        : null;
                    if (receiver == null || !indoor(receiver)) {
                        player.displayClientMessage(msg("linker.no_wallpad").withStyle(ChatFormatting.RED), true);
                        return InteractionResult.FAIL;
                    } else if (!ZoneData.connected(level, wpPos, pos)) {
                        player.displayClientMessage(Component.literal("연결 실패: 두 기기를 단지 대시보드에서 같은 단지 구획에 포함하세요. 서로 떨어진 구획은 같은 단지로 묶어야 합니다."), true);
                        return InteractionResult.FAIL;
                    } else if (!receiver.supportsDoorCommunication()) {
                        player.displayClientMessage(Component.literal("이 모델은 도어폰·도어카메라 통신을 지원하지 않습니다."), true);
                        return InteractionResult.FAIL;
                    } else {
                        DoorStationBlockEntity door = (DoorStationBlockEntity)be;
                        if (!pos.closerThan(wpPos, 64.0)) {
                            player.displayClientMessage(Component.literal("기기 링크는 64블록 이내에서 가능합니다."), true);
                            return InteractionResult.FAIL;
                        } else if (!door.hasReceiver(wpPos) && door.linkedReceivers().size() >= 32) {
                            player.displayClientMessage(Component.literal("현관 기기당 최대 32개 수신기를 연결할 수 있습니다."), true);
                            return InteractionResult.FAIL;
                        } else {
                            boolean linked = door.toggleReceiver(wpPos);
                            player.displayClientMessage(Component.literal(linked ? "선택한 내부 기기에 직접 연결했습니다." : "내부 기기 연결을 해제했습니다."), true);
                            ding(level, pos, linked ? 1.8F : 0.8F);
                            receiver.syncScreens();
                            return InteractionResult.CONSUME;
                        }
                    }
                }
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && stack.hasTag()) {
            if (!level.isClientSide) {
                stack.setTag(null);
                player.displayClientMessage(msg("linker.cleared"), true);
            }

            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        } else {
            return InteractionResultHolder.pass(stack);
        }
    }

    private static void ding(Level level, BlockPos pos, float pitch) {
        level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.4F, pitch);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("Wallpad");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("Wallpad")) {
            tooltip.add(msg("linker.tooltip_selected", BlockPos.of(tag.getLong("Wallpad")).toShortString()).withStyle(ChatFormatting.AQUA));
        }

        tooltip.add(msg("linker.tooltip_1").withStyle(ChatFormatting.GRAY));
        tooltip.add(msg("linker.tooltip_2").withStyle(ChatFormatting.GRAY));
        tooltip.add(msg("linker.tooltip_3").withStyle(ChatFormatting.GRAY));
    }
}
