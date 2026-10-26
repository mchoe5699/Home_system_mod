package com.qwerty.homenet.item;

import com.qwerty.homenet.blockentity.Ip203apBlockEntity;
import com.qwerty.homenet.blockentity.Klp203rfBlockEntity;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.data.ZoneData;
import com.qwerty.homenet.guard.GuardExchange;
import com.qwerty.homenet.guard.GuardSystem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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

public class AnalogLinkerItem extends Item {
    public AnalogLinkerItem(Properties properties) {
        super(properties);
    }

    public static List<BlockPos> selected(ItemStack stack, Level level) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getString("Dim").equals(level.dimension().location().toString())
            ? new ArrayList<>(Arrays.stream(tag.getLongArray("Selected")).mapToObj(BlockPos::of).distinct().toList())
            : new ArrayList<>();
    }

    private static boolean isConsole(BlockEntity be) {
        return be instanceof GuardExchange || Ip203apBlockEntity.isStarConsole(be);
    }

    public static boolean connectBatch(Level level, List<BlockPos> sources, BlockPos target) {
        if (!sources.isEmpty() && !sources.stream().anyMatch(pos -> !ZoneData.connected(level, pos, target)) && level.isLoaded(target)) {
            BlockEntity targetEntity = level.getBlockEntity(target);
            if (Ip203apBlockEntity.isStarConsole(targetEntity) && !(targetEntity instanceof GuardExchange)) {
                ArrayList<Ip203apBlockEntity> phones = new ArrayList<>();

                for (BlockPos pos : new LinkedHashSet<>(sources)) {
                    if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof Ip203apBlockEntity phone)) {
                        return false;
                    }

                    if (phone.guardSystem() != GuardSystem.STAR) {
                        return false;
                    }

                    phones.add(phone);
                }

                for (Ip203apBlockEntity phone : phones) {
                    if (!phone.bindSwitchboard(target)) {
                        return false;
                    }
                }

                return true;
            } else if (!(targetEntity instanceof GuardExchange board)) {
                return false;
            } else {
                ArrayList<ReceiverBlockEntity> receivers = new ArrayList<>();
                ArrayList<Klp203rfBlockEntity> panels = new ArrayList<>();

                for (BlockPos pos : new LinkedHashSet<>(sources)) {
                    if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof Klp203rfBlockEntity panel) {
                        if (!panel.canBind(target) || panel.isInCall()) {
                            return false;
                        }

                        panels.add(panel);
                    } else {
                        if (!level.isLoaded(pos)
                            || !(
                                level.getBlockEntity(pos) instanceof ReceiverBlockEntity receiver
                                    && receiver.kind() != DeviceRegistry.Kind.GUARD_CONSOLE
                                    && receiver.supportsSwitchboard(board)
                            )) {
                            return false;
                        }

                        receivers.add(receiver);
                    }
                }

                for (ReceiverBlockEntity receiver : receivers) {
                    if (!receiver.bindSwitchboard(target)) {
                        return false;
                    }
                }

                for (Klp203rfBlockEntity panel : panels) {
                    if (!panel.bindSwitchboard(target)) {
                        return false;
                    }
                }

                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        } else {
            BlockPos pos = ctx.getClickedPos();
            BlockEntity be = level.getBlockEntity(pos);
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            } else {
                List<BlockPos> selected = selected(stack, level);
                if (player.isShiftKeyDown()) {
                    if (!isConsole(be)) {
                        player.displayClientMessage(Component.literal("웅크린 상태에서는 경비실기만 선택할 수 있습니다."), true);
                        return InteractionResult.CONSUME;
                    } else {
                        boolean success = connectBatch(level, selected, pos);
                        player.displayClientMessage(
                            Component.literal(
                                success
                                    ? selected.size() + "개 기기를 경비실기에 연결했습니다. 경비실 전용 번호 모델에는 무작위 동·호수를 부여했습니다."
                                    : "연결하지 않았습니다. 선택 기기가 같은 단지 구획 안에 있는지와 STAR/BUS·제조사 호환성을 확인하세요."
                            ),
                            true
                        );
                        if (success) {
                            stack.setTag(null);
                        }

                        return success ? InteractionResult.CONSUME : InteractionResult.FAIL;
                    }
                } else if (isConsole(be)) {
                    player.displayClientMessage(Component.literal("비디오폰을 먼저 선택한 뒤 웅크리고 경비실기를 클릭하세요."), true);
                    return InteractionResult.CONSUME;
                } else if (be instanceof Klp203rfBlockEntity
                    || be instanceof ReceiverBlockEntity receiver && receiver.kind() != DeviceRegistry.Kind.GUARD_CONSOLE) {
                    if (be instanceof ReceiverBlockEntity receiverx && receiverx.guardSystem() == GuardSystem.NONE) {
                        player.displayClientMessage(Component.literal("이 제품에는 경비실 연결 기능이 없습니다."), true);
                        return InteractionResult.FAIL;
                    } else {
                        if (selected.remove(pos)) {
                            player.displayClientMessage(Component.literal("선택 해제: " + selected.size() + "개"), true);
                        } else {
                            selected.add(pos.immutable());
                            player.displayClientMessage(Component.literal("기기 선택: " + selected.size() + "개. 추가 기기를 선택한 뒤 웅크리고 경비실기를 클릭하세요."), true);
                        }

                        CompoundTag tag = stack.getOrCreateTag();
                        tag.putString("Dim", level.dimension().location().toString());
                        tag.putLongArray("Selected", selected.stream().mapToLong(BlockPos::asLong).toArray());
                        return InteractionResult.CONSUME;
                    }
                } else {
                    player.displayClientMessage(Component.literal("서서 비디오폰 선택·해제 → 웅크리고 경비실기 클릭"), true);
                    return InteractionResult.CONSUME;
                }
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getLongArray("Selected").length > 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        int count = stack.hasTag() ? stack.getTag().getLongArray("Selected").length : 0;
        tooltip.add(Component.literal("선택 기기 " + count + "개 / 세대수 제한 없음"));
        tooltip.add(Component.literal("비디오폰 여러 대 선택·재클릭 해제 → 웅크리고 경비실기 클릭"));
        tooltip.add(Component.literal("웅크린 상태에서는 경비실기만 선택 가능"));
    }
}
