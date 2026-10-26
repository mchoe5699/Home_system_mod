package com.qwerty.homenet.item;

import com.qwerty.homenet.blockentity.CallerBlockEntity;
import com.qwerty.homenet.blockentity.Klp203rfBlockEntity;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.intercom.DoorControl;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class DoorLinkerItem extends Item {
    public DoorLinkerItem(Properties properties) {
        super(properties);
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
            boolean door = DoorControl.isDoor(level.getBlockState(pos));
            if (!(be instanceof ReceiverBlockEntity) && !(be instanceof Klp203rfBlockEntity) && !door) {
                return InteractionResult.PASS;
            } else if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            } else if (be instanceof Klp203rfBlockEntity caller) {
                if (player.isShiftKeyDown()) {
                    caller.clearDoors();
                    return InteractionResult.CONSUME;
                } else {
                    CompoundTag data = stack.getOrCreateTag();
                    data.putLong("Receiver", pos.asLong());
                    data.putString("Dim", level.dimension().location().toString());
                    player.displayClientMessage(Component.literal("로비폰 선택: 연결할 문을 클릭하세요."), true);
                    return InteractionResult.CONSUME;
                }
            } else if (be instanceof ReceiverBlockEntity receiver) {
                if (receiver.kind() == DeviceRegistry.Kind.GUARD_CONSOLE) {
                    return InteractionResult.PASS;
                } else if (player.isShiftKeyDown()) {
                    receiver.clearReceiverDoors();
                    player.displayClientMessage(Component.literal("내부 기기의 문 연결을 모두 해제했습니다."), true);
                    return InteractionResult.CONSUME;
                } else {
                    CompoundTag tag = stack.getOrCreateTag();
                    tag.putLong("Receiver", pos.asLong());
                    tag.putString("Dim", level.dimension().location().toString());
                    player.displayClientMessage(Component.literal("내부 기기를 선택했습니다. 연결할 문을 선택하세요."), true);
                    return InteractionResult.CONSUME;
                }
            } else {
                CompoundTag tag = stack.getTag();
                if (tag != null && tag.contains("Receiver") && tag.getString("Dim").equals(level.dimension().location().toString())) {
                    BlockPos source = BlockPos.of(tag.getLong("Receiver"));
                    if (!level.isLoaded(source)) {
                        return InteractionResult.FAIL;
                    } else if (level.getBlockEntity(source) instanceof Klp203rfBlockEntity callerx) {
                        CallerBlockEntity.DoorLinkResult result = callerx.toggleDoor(pos);
                        player.displayClientMessage(Component.literal("로비폰 문 링크: " + result), true);
                        return result != CallerBlockEntity.DoorLinkResult.LINKED && result != CallerBlockEntity.DoorLinkResult.UNLINKED
                            ? InteractionResult.FAIL
                            : InteractionResult.CONSUME;
                    } else if (level.getBlockEntity(source) instanceof ReceiverBlockEntity receiverx) {
                        String var20 = receiverx.toggleReceiverDoor(pos);

                        player.displayClientMessage(Component.literal(switch (var20) {
                            case "linked" -> "내부 기기에 문을 연결했습니다.";
                            case "unlinked" -> "문 연결을 해제했습니다.";
                            case "full" -> "문은 최대 16개까지 연결할 수 있습니다.";
                            case "outside_zone" -> "문과 기기가 같은 단지 구획 안에 있어야 합니다.";
                            case "too_far" -> "문은 내부 기기에서 32블록 이내에 있어야 합니다.";
                            default -> "연결할 수 없는 문입니다.";
                        }), true);
                        return !var20.equals("linked") && !var20.equals("unlinked") ? InteractionResult.FAIL : InteractionResult.CONSUME;
                    } else {
                        return InteractionResult.FAIL;
                    }
                } else {
                    player.displayClientMessage(Component.literal("먼저 월패드·비디오폰·인터폰을 선택하세요."), true);
                    return InteractionResult.FAIL;
                }
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                stack.setTag(null);
            }

            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        } else {
            return InteractionResultHolder.pass(stack);
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("Receiver");
    }
}
