package com.qwerty.homenet.sht;

import com.qwerty.homenet.blockentity.CallerBlockEntity;
import com.qwerty.homenet.blockentity.Klp203rfBlockEntity;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.data.ZoneData;
import com.qwerty.homenet.intercom.DoorControl;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class DoorCameraRouting {
    private DoorCameraRouting() {
    }

    public static BlockPos linked(ReceiverBlockEntity receiver, BlockPos standard) {
        if (standard != null) {
            return standard;
        } else if (receiver.getLevel() instanceof ServerLevel level) {
            String unit = DeviceRegistry.normalize(receiver.getUnit());
            if (unit.isEmpty()) {
                return null;
            } else {
                BlockPos closest = null;
                double distance = Double.POSITIVE_INFINITY;

                for (DeviceRegistry.Entry entry : DeviceRegistry.get(level).all()) {
                    BlockPos at = entry.pos();
                    if (level.isLoaded(at)
                        && ZoneData.connected(level, receiver.getBlockPos(), at)
                        && level.getBlockEntity(at) instanceof Ht100s.Unit camera
                        && camera.getBlockState().is(Product.HT100.get())
                        && unit.equals(DeviceRegistry.normalize(camera.registeredUnit()))) {
                        double next = receiver.getBlockPos().distSqr(at);
                        if (next < distance) {
                            closest = at;
                            distance = next;
                        }
                    }
                }

                return closest;
            }
        } else {
            return null;
        }
    }

    static boolean isVideo(Ht100s.Unit camera) {
        return camera.getBlockState().is(Product.HT100.get());
    }

    public static InteractionResult lobbyDoor(ItemStack stack, UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos at = ctx.getClickedPos();
        BlockEntity entity = level.getBlockEntity(at);
        boolean caller = entity instanceof CallerBlockEntity c && c.kind() == DeviceRegistry.Kind.LOBBY_PHONE && !(entity instanceof Klp203rfBlockEntity);
        CompoundTag tag = stack.getTag();
        boolean selected = tag != null && tag.contains("ShtLobbyCaller");
        if (!(entity instanceof ReceiverBlockEntity) && !(entity instanceof Klp203rfBlockEntity)) {
            if (!caller && !selected) {
                return InteractionResult.PASS;
            } else if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            } else if (caller) {
                if (ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown()) {
                    ((CallerBlockEntity)entity).clearDoors();
                    notice(ctx, "로비폰 문 연결을 모두 해제했습니다");
                    return InteractionResult.CONSUME;
                } else {
                    CompoundTag saved = stack.getOrCreateTag();
                    saved.remove("Receiver");
                    saved.remove("Dim");
                    saved.putLong("ShtLobbyCaller", at.asLong());
                    saved.putString("ShtLobbyDim", level.dimension().location().toString());
                    notice(ctx, "로비폰 선택: 연결할 문을 클릭하세요");
                    return InteractionResult.CONSUME;
                }
            } else if (!DoorControl.isDoor(level.getBlockState(at))) {
                return InteractionResult.PASS;
            } else {
                BlockPos source = BlockPos.of(tag.getLong("ShtLobbyCaller"));
                if (tag.getString("ShtLobbyDim").equals(level.dimension().location().toString())
                    && level.isLoaded(source)
                    && ZoneData.connected(level, source, at)
                    && level.getBlockEntity(source) instanceof CallerBlockEntity lobby
                    && lobby.kind() == DeviceRegistry.Kind.LOBBY_PHONE) {
                    CallerBlockEntity.DoorLinkResult result = lobby.toggleDoor(DoorControl.normalize(level.getBlockState(at), at));
                    tag.remove("ShtLobbyCaller");
                    tag.remove("ShtLobbyDim");
                    notice(
                        ctx,
                        result == CallerBlockEntity.DoorLinkResult.LINKED
                            ? "로비폰과 문을 연결했습니다"
                            : (result == CallerBlockEntity.DoorLinkResult.UNLINKED ? "로비폰 문 연결을 해제했습니다" : "로비폰과 문을 연결할 수 없습니다")
                    );
                    return result != CallerBlockEntity.DoorLinkResult.LINKED && result != CallerBlockEntity.DoorLinkResult.UNLINKED
                        ? InteractionResult.FAIL
                        : InteractionResult.CONSUME;
                } else {
                    return InteractionResult.FAIL;
                }
            }
        } else {
            if (!level.isClientSide && selected) {
                tag.remove("ShtLobbyCaller");
                tag.remove("ShtLobbyDim");
            }

            return InteractionResult.PASS;
        }
    }

    private static void notice(UseOnContext ctx, String message) {
        if (ctx.getPlayer() != null) {
            ctx.getPlayer().displayClientMessage(Component.literal(message), true);
        }
    }
}
