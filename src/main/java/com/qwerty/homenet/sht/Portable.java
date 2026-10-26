package com.qwerty.homenet.sht;

import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.data.DeviceRegistry;
import com.qwerty.homenet.intercom.CallSounds;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public final class Portable extends Item {
    static final int CAPACITY = 36000;

    static boolean supports(ReceiverBlockEntity r) {
        return r instanceof Station s
            && s.manufacturer().equalsIgnoreCase("SAMSUNG")
            && s.supportsWirelessPortable()
            && Set.of(DeviceRegistry.Kind.WALLPAD, DeviceRegistry.Kind.VIDEO_PHONE, DeviceRegistry.Kind.INTERPHONE).contains(s.kind());
    }

    static boolean isPortable(ItemStack stack) {
        return stack.is(Product.PORTABLE102.get()) || stack.is(Product.PORTABLE104M.get());
    }

    static boolean compatible(Station station, ItemStack stack) {
        return station instanceof Station104M.Unit ? stack.is(Product.PORTABLE104M.get()) : stack.is(Product.PORTABLE102.get());
    }

    static boolean carried(ServerPlayer p, Station s) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack item = p.getInventory().getItem(i);
            if (s.ownsPortable(item) && item.getTag().getBoolean("power") && item.getTag().getInt("battery") > 0) {
                return true;
            }
        }

        return false;
    }

    Portable() {
        super(new Properties().stacksTo(1));
    }

    static Station base(Level l, ItemStack stack) {
        if (isPortable(stack) && stack.hasTag()) {
            MovementSupport.relocatePortable(l, stack);
            CompoundTag t = stack.getTag();
            if (t.getString("dimension").equals(l.dimension().location().toString()) && t.contains("base")) {
                BlockPos at = BlockPos.of(t.getLong("base"));
                return l.isLoaded(at) && l.getBlockEntity(at) instanceof Station s && supports(s) && s.ownsPortable(stack) ? s : null;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    static boolean bind(ServerPlayer p, Station s, ItemStack stack) {
        return !s.allowed(p, false) ? false : bindFromLinker(p, s, stack);
    }

    static boolean bindFromLinker(ServerPlayer p, Station s, ItemStack stack) {
        if (!supports(s) || p.level() != s.getLevel() || !compatible(s, stack)) {
            return false;
        } else if (s instanceof Station104M.Unit model104) {
            return model104.bindPortable(p, stack);
        } else if (s.ownsPortable(stack)) {
            s.tell("등록된 삼성 휴대장치입니다");
            return true;
        } else if (s.portableRegistered && !p.isShiftKeyDown()) {
            s.tell("이미 휴대장치가 등록되어 있습니다 · 재등록은 웅크리고 클릭");
            return false;
        } else {
            if (s.portableRegistered) {
                s.portableTicket = UUID.randomUUID();
            }

            s.portableRegistered = true;
            CompoundTag t = stack.getOrCreateTag();
            t.putUUID("serial", s.serial);
            t.putUUID("ticket", s.portableTicket);
            t.putLong("base", s.getBlockPos().asLong());
            t.putString("dimension", p.level().dimension().location().toString());
            t.putBoolean("power", true);
            if (!t.contains("battery")) {
                t.putInt("battery", 36000);
            }

            s.tell("삼성 휴대장치 연결 완료");
            return true;
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level l, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer p) {
            if (player.isShiftKeyDown()) {
                CompoundTag t = stack.getOrCreateTag();
                t.putBoolean("power", !t.getBoolean("power"));
            } else {
                Station s = base(l, stack);
                if (s != null) {
                    s.openPortable(p);
                } else {
                    p.displayClientMessage(Component.literal("홈링커로 삼성 무선 지원 내부기기와 휴대장치 충전기를 연결하세요"), true);
                }
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, l.isClientSide);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level l, Entity entity, int slot, boolean selected) {
        if (!l.isClientSide && l.getGameTime() % 20L == 0L && entity instanceof ServerPlayer p) {
            refresh(stack, l, p);
        }
    }

    static void refresh(ItemStack stack, Level l, ServerPlayer p) {
        CompoundTag t = stack.getOrCreateTag();
        Station s = base(l, stack);
        boolean powered = t.getBoolean("power") && t.getInt("battery") > 0;
        if (powered) {
            t.putInt("battery", Math.max(0, t.getInt("battery") - 20));
        }

        boolean inRange = powered && s != null && s.allowed(p, true);
        boolean ringing = inRange && s.portableRinging();
        t.putBoolean("intercom", inRange && s.internal > 0);
        t.putBoolean("ringing", ringing);
        t.putBoolean("talking", inRange && s.audio && (s.portableActive || s.internal == 3));
        if (ringing) {
            CallSounds.note(l, p.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), s.ring * 0.25F, 1.1F);
        }

        if (s != null && (s.portableActive || s.internal == 3) && !inRange) {
            s.finish();
            s.tell("휴대장치 전원·배터리·통신 범위 확인");
        }
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack a, ItemStack b, boolean slot) {
        if (!slot && a.getItem() == b.getItem()) {
            CompoundTag x = a.getTag();
            CompoundTag y = b.getTag();
            return x != null && y != null
                ? !Objects.equals(x.get("serial"), y.get("serial")) || !Objects.equals(x.get("ticket"), y.get("ticket"))
                : !Objects.equals(x, y);
        } else {
            return true;
        }
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> c) {
        c.accept(Client.itemExtension());
    }
}
