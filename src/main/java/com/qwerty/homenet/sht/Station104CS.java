package com.qwerty.homenet.sht;

import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.item.InsertCardItem;
import com.qwerty.homenet.item.RfCardItem;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class Station104CS extends Station104C {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos at, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        boolean registrar = held.is(Product.S1_CARD.get());
        boolean credential = held.getItem() instanceof InsertCardItem || held.getItem() instanceof RfCardItem;
        if (!registrar && !credential) {
            return super.use(state, level, at, player, hand, hit);
        } else {
            if (player instanceof ServerPlayer server && level.getBlockEntity(at) instanceof Station104CS.Unit unit) {
                if (registrar) {
                    unit.beginEnrollment();
                } else {
                    unit.presentCard(held);
                }
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Station104CS.Unit(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, Product.TYPE104CS.get(), ReceiverBlockEntity::serverTick);
    }

    public static final class Unit extends Station104M.Unit {
        private final Set<String> s1Cards = new HashSet<>();
        private boolean s1Armed;
        private boolean s1Home;
        private long enrollmentDeadline;

        public Unit(BlockPos pos, BlockState state) {
            super(Product.TYPE104CS.get(), pos, state);
        }

        @Override
        String modelName() {
            return "SHT-104CS";
        }

        @Override
        boolean allowsVariant(String value) {
            return value.matches("[ABC]");
        }

        @Override
        protected Map<String, String> settingsForClient() {
            Map<String, String> values = super.settingsForClient();
            values.put("s1_armed", this.s1Armed ? "1" : "0");
            values.put("s1_home", this.s1Home ? "1" : "0");
            values.put("s1_cards", Integer.toString(this.s1Cards.size()));
            values.put("s1_enroll", this.enrollmentOpen() ? "1" : "0");
            return values;
        }

        static boolean withinEnrollment(long now, long deadline) {
            return deadline > 0L && now < deadline;
        }

        boolean enrollmentOpen() {
            return this.level != null && withinEnrollment(this.level.getGameTime(), this.enrollmentDeadline);
        }

        long remainingEnrollmentTicks() {
            return this.enrollmentOpen() ? this.enrollmentDeadline - this.level.getGameTime() : 0L;
        }

        void beginEnrollment() {
            this.enrollmentDeadline = this.level.getGameTime() + 600L;
            this.tell("에스원 카드 등록 대기 · 30초 이내 미등록 카드를 우클릭하세요");
            this.update();
        }

        void presentCard(ItemStack card) {
            String id = Ht512.Unit.cardId(card);
            if (this.enrollmentOpen()) {
                this.enrollmentDeadline = 0L;
                if (this.s1Cards.add(id)) {
                    this.tell("에스원 카드 연동 완료 · " + this.s1Cards.size() + "장");
                } else {
                    this.tell("이미 등록된 에스원 카드");
                }
            } else if (!this.s1Cards.contains(id)) {
                this.tell("등록되지 않은 에스원 카드");
            } else {
                if (!this.s1Armed && this.openDoors()) {
                    this.tell("문이 열려 있어 에스원 경비를 설정할 수 없습니다");
                    return;
                }

                this.s1Armed = !this.s1Armed;
                this.armed = this.s1Armed;
                if (!this.s1Armed && this.alarm.equals("security")) {
                    this.restore();
                }

                this.tell("에스원 경비 " + (this.s1Armed ? "설정" : "해제"));
            }

            this.update();
        }

        private ItemStack heldCredential(ServerPlayer player) {
            ItemStack main = player.getMainHandItem();
            return !(main.getItem() instanceof InsertCardItem) && !(main.getItem() instanceof RfCardItem)
                ? (
                    !(player.getOffhandItem().getItem() instanceof InsertCardItem) && !(player.getOffhandItem().getItem() instanceof RfCardItem)
                        ? ItemStack.EMPTY
                        : player.getOffhandItem()
                )
                : main;
        }

        @Override
        public void press(String key, ServerPlayer player, boolean remote) {
            if (Set.of("s1_away", "s1_home", "s1_disarm").contains(key)) {
                ItemStack card = this.heldCredential(player);
                if (card.isEmpty() || !this.s1Cards.contains(Ht512.Unit.cardId(card))) {
                    this.tell("등록된 에스원 카드를 손에 드세요");
                } else if (!key.equals("s1_disarm") && this.openDoors()) {
                    this.tell("문이 열려 있어 에스원 경비를 설정할 수 없습니다");
                } else {
                    this.s1Armed = !key.equals("s1_disarm");
                    this.s1Home = key.equals("s1_home");
                    this.armed = this.s1Armed;
                    if (!this.s1Armed && this.alarm.equals("security")) {
                        this.restore();
                    }

                    this.tell("에스원 " + (this.s1Armed ? (this.s1Home ? "재택 경비 설정" : "외출 경비 설정") : "경비 해제"));
                    this.update();
                }
            } else if (key.equals("s1_arm")) {
                ItemStack card = this.heldCredential(player);
                if (card.isEmpty()) {
                    this.tell("등록된 일반 카드를 손에 드세요");
                } else {
                    this.presentCard(card);
                }
            } else {
                super.press(key, player, remote);
            }
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.putBoolean("S1Armed", this.s1Armed);
            tag.putBoolean("S1Home", this.s1Home);
            tag.putString("S1Cards", String.join(",", this.s1Cards));
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            if (this.variant.equals("D")) {
                this.variant = "C";
            }

            CompoundTag shared = tag.getCompound("ForgeData");
            this.s1Armed = tag.contains("S1Armed") ? tag.getBoolean("S1Armed") : shared.getBoolean("Sht104FamilyS1Armed");
            this.s1Home = tag.getBoolean("S1Home");
            this.s1Cards.clear();
            String cards = tag.contains("S1Cards") ? tag.getString("S1Cards") : shared.getString("Sht104FamilyS1Cards");
            if (!cards.isEmpty()) {
                this.s1Cards.addAll(Arrays.asList(cards.split(",")));
            }
        }
    }
}
