package com.qwerty.homenet.sht;

import com.qwerty.homenet.blockentity.DoorStationBlockEntity;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.guard.GuardSystem;
import com.qwerty.homenet.intercom.Intercom;
import com.qwerty.homenet.item.DoorLinkerItem;
import com.qwerty.homenet.item.HomeLinkerItem;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
    modid = "homenet_sht101c"
)
public final class DoorDeviceRouting {
    private DoorDeviceRouting() {
    }

    @SubscribeEvent(
        priority = EventPriority.HIGHEST
    )
    public static void click(RightClickBlock event) {
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            Item held = event.getItemStack().getItem();
            if (!(held instanceof HomeLinkerItem) && !(held instanceof DoorLinkerItem)) {
                BlockEntity entity = event.getLevel().getBlockEntity(event.getPos());
                if (entity instanceof DoorStationBlockEntity || entity instanceof Ht100s.Unit) {
                    if (!(entity instanceof Ht100s.Unit)
                        || !Ht100s.button(event.getLevel().getBlockState(event.getPos()), event.getLevel(), event.getPos(), event.getHitVec())) {
                        event.setCanceled(true);
                        event.setCancellationResult(InteractionResult.SUCCESS);
                        if (event.getLevel().isClientSide) {
                            Wire.doorOpen(event.getPos());
                        } else if (event.getEntity() instanceof ServerPlayer player) {
                            open(player, event.getPos());
                        }
                    }
                }
            }
        }
    }

    public static boolean canLink(DoorStationBlockEntity door, BlockPos candidate) {
        if (door.hasReceiver(candidate)) {
            return true;
        } else {
            Level level = door.getLevel();
            if (level != null && level.isLoaded(candidate) && level.getBlockEntity(candidate) instanceof ReceiverBlockEntity target) {
                GuardSystem protocol = target.guardSystem();
                if (protocol == GuardSystem.NONE) {
                    return false;
                } else {
                    for (BlockPos at : door.linkedReceivers()) {
                        if (level.isLoaded(at) && level.getBlockEntity(at) instanceof ReceiverBlockEntity linked && linked.guardSystem() != protocol) {
                            return false;
                        }
                    }

                    return true;
                }
            } else {
                return true;
            }
        }
    }

    public static List<ReceiverBlockEntity> compatibleReceivers(DoorStationBlockEntity door, List<ReceiverBlockEntity> receivers) {
        if (receivers.isEmpty()) {
            return receivers;
        } else {
            GuardSystem selected = receivers.get(0).guardSystem();
            return selected == GuardSystem.NONE ? List.of() : receivers.stream().filter(r -> r.guardSystem() == selected).toList();
        }
    }

    static void open(ServerPlayer player, BlockPos pos) {
        if (player.level().isLoaded(pos) && Intercom.inReach(player, pos)) {
            BlockEntity entity = player.level().getBlockEntity(pos);
            Wire.DoorView view = view(entity);
            if (view != null) {
                Wire.doorView(player, view);
            }
        }
    }

    static Wire.DoorView view(BlockEntity entity) {
        if (entity instanceof Ht100s.Unit phone) {
            return new Wire.DoorView(
                phone.getBlockPos(), phone.model(), phone.unit, phone.unit.isBlank() ? 0 : 1, phone.isInCall(), List.copyOf(phone.getLog())
            );
        } else {
            return entity instanceof DoorStationBlockEntity door
                ? new Wire.DoorView(
                    door.getBlockPos(),
                    door.isDoorPhone() ? "도어폰" : "도어카메라",
                    door.registeredUnit(),
                    door.linkedReceivers().size(),
                    door.isInCall(),
                    List.copyOf(door.getLog())
                )
                : null;
        }
    }

    static void action(ServerPlayer player, Wire.DoorAction request) {
        BlockPos pos = request.pos();
        if (player.level().isLoaded(pos) && Intercom.inReach(player, pos)) {
            BlockEntity entity = player.level().getBlockEntity(pos);
            if (entity instanceof Ht100s.Unit phone) {
                if (request.action().equals("call") || request.action().equals("hang")) {
                    phone.call();
                } else if (request.action().equals("message") && phone.isConnected()) {
                    phone.say(player, phone.model(), request.value());
                }
            } else if (entity instanceof DoorStationBlockEntity door) {
                String var6 = request.action();
                switch (var6) {
                    case "call":
                        door.handleAction(player, DoorStationBlockEntity.Action.CALL, "");
                        break;
                    case "hang":
                        door.handleAction(player, DoorStationBlockEntity.Action.HANG_UP, "");
                        break;
                    case "message":
                        door.handleAction(player, DoorStationBlockEntity.Action.SEND_MESSAGE, request.value());
                }
            }

            Wire.DoorView updated = view(entity);
            if (updated != null) {
                Wire.doorView(player, updated);
            }
        }
    }
}
