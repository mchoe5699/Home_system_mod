package com.qwerty.homenet.sht;

import com.qwerty.homenet.block.WallMountedBlock;
import com.qwerty.homenet.block.WallpadBlock;
import com.qwerty.homenet.blockentity.ReceiverBlockEntity;
import com.qwerty.homenet.item.AnalogLinkerItem;
import com.qwerty.homenet.item.HomeLinkerItem;
import com.qwerty.homenet.move.DeviceOffsets;
import com.qwerty.homenet.registry.ModCreativeTab;
import com.qwerty.homenet.registry.ModItems;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab.TabVisibility;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod("homenet_sht101c")
public final class Product {
    public static final String ID = "homenet_sht101c";
    static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "homenet_sht101c");
    static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "homenet_sht101c");
    static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "homenet_sht101c");
    static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "homenet_sht101c");
    public static final RegistryObject<Item> S1_CARD = ITEMS.register("s1_security_card", S1Card::new);
    public static final RegistryObject<Ht512> HT512 = BLOCKS.register("ht_512", Ht512::new);
    public static final RegistryObject<Item> HT512_ITEM = ITEMS.register("ht_512", () -> new BlockItem(HT512.get(), new Properties()) {
        @Override
        public void initializeClient(Consumer<IClientItemExtensions> c) {
            c.accept(Client.itemExtension());
        }
    });
    public static final RegistryObject<BlockEntityType<Ht512.Unit>> HT512_TYPE = ENTITIES.register(
        "ht512", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(Ht512.Unit::new, HT512.get()).build(null)
    );
    public static final RegistryObject<Ht513> HT513 = BLOCKS.register("ht_513", Ht513::new);
    public static final RegistryObject<Item> HT513_ITEM = ITEMS.register("ht_513", () -> new BlockItem(HT513.get(), new Properties()) {
        @Override
        public void initializeClient(Consumer<IClientItemExtensions> consumer) {
            consumer.accept(Client.itemExtension());
        }
    });
    public static final RegistryObject<BlockEntityType<Ht513.Unit>> HT513_TYPE = ENTITIES.register(
        "ht513", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(Ht513.Unit::new, HT513.get()).build(null)
    );
    public static final RegistryObject<Ht100s> HT100S = BLOCKS.register("ht_100s", Ht100s::new);
    public static final RegistryObject<Item> HT100S_ITEM = ITEMS.register("ht_100s", () -> new BlockItem(HT100S.get(), new Properties()) {
        @Override
        public void initializeClient(Consumer<IClientItemExtensions> c) {
            c.accept(Client.itemExtension());
        }
    });
    public static final RegistryObject<Ht100s> HT100 = BLOCKS.register("ht_100", Ht100s::new);
    public static final RegistryObject<Item> HT100_ITEM = ITEMS.register("ht_100", () -> new BlockItem(HT100.get(), new Properties()) {
        @Override
        public void initializeClient(Consumer<IClientItemExtensions> c) {
            c.accept(Client.itemExtension());
        }
    });
    public static final RegistryObject<BlockEntityType<Ht100s.Unit>> HT100S_TYPE = ENTITIES.register(
        "ht100s", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(Ht100s.Unit::new, HT100S.get(), HT100.get()).build(null)
    );
    public static final RegistryObject<Station104M> STATION104M = BLOCKS.register("sht_104m", Station104M::new);
    public static final RegistryObject<Item> BASE104M_ITEM = ITEMS.register("sht_104m", () -> new Sht104MItem(STATION104M.get()));
    public static final RegistryObject<BlockEntityType<Station104M.Unit>> TYPE104M = ENTITIES.register(
        "station104m", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(Station104M.Unit::new, STATION104M.get()).build(null)
    );
    public static final RegistryObject<Station104C> STATION104C = BLOCKS.register("sht_104c", Station104C::new);
    public static final RegistryObject<Item> BASE104C_ITEM = ITEMS.register("sht_104c", () -> new Sht104MItem(STATION104C.get(), "SHT-104C"));
    public static final RegistryObject<BlockEntityType<Station104C.Unit>> TYPE104C = ENTITIES.register(
        "station104c", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(Station104C.Unit::new, STATION104C.get()).build(null)
    );
    public static final RegistryObject<Station104CS> STATION104CS = BLOCKS.register("sht_104cs", Station104CS::new);
    public static final RegistryObject<Item> BASE104CS_ITEM = ITEMS.register("sht_104cs", () -> new Sht104MItem(STATION104CS.get(), "SHT-104CS"));
    public static final RegistryObject<BlockEntityType<Station104CS.Unit>> TYPE104CS = ENTITIES.register(
        "station104cs", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(Station104CS.Unit::new, STATION104CS.get()).build(null)
    );
    public static final RegistryObject<Station705> STATION705 = BLOCKS.register("sht_705", Station705::new);
    public static final RegistryObject<Item> BASE705_ITEM = ITEMS.register("sht_705", () -> new BlockItem(STATION705.get(), new Properties()) {
        @Override
        public void initializeClient(Consumer<IClientItemExtensions> c) {
            c.accept(Client.itemExtension());
        }
    });
    public static final RegistryObject<BlockEntityType<Station705.Unit>> TYPE705 = ENTITIES.register(
        "station705", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(Station705.Unit::new, STATION705.get()).build(null)
    );
    public static final RegistryObject<Station308> STATION308 = BLOCKS.register("sht_308", Station308::new);
    public static final RegistryObject<Item> BASE308_ITEM = ITEMS.register("sht_308", () -> new BlockItem(STATION308.get(), new Properties()) {
        @Override
        public void initializeClient(Consumer<IClientItemExtensions> c) {
            c.accept(Client.itemExtension());
        }
    });
    public static final RegistryObject<BlockEntityType<Station308.Unit>> TYPE308 = ENTITIES.register(
        "station308", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(Station308.Unit::new, STATION308.get()).build(null)
    );
    public static final RegistryObject<Station308> STATION307 = BLOCKS.register("sht_307", () -> new Station308(true));
    public static final RegistryObject<Item> BASE307_ITEM = ITEMS.register("sht_307", () -> new BlockItem(STATION307.get(), new Properties()) {
        @Override
        public void initializeClient(Consumer<IClientItemExtensions> c) {
            c.accept(Client.itemExtension());
        }
    });
    public static final RegistryObject<BlockEntityType<Station308.Unit>> TYPE307 = ENTITIES.register(
        "station307",
        () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of((p, s) -> new Station308.Unit(p, s, true), STATION307.get()).build(null)
    );
    public static final RegistryObject<Station305> STATION305 = BLOCKS.register("sht_305v", Station305::new);
    public static final RegistryObject<Item> BASE305_ITEM = ITEMS.register("sht_305v", () -> new BlockItem(STATION305.get(), new Properties()) {
        @Override
        public void initializeClient(Consumer<IClientItemExtensions> c) {
            c.accept(Client.itemExtension());
        }
    });
    public static final RegistryObject<BlockEntityType<Station305.Unit>> TYPE305 = ENTITIES.register(
        "station305", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(Station305.Unit::new, STATION305.get()).build(null)
    );
    public static final RegistryObject<Station305> STATION305_PLAIN = BLOCKS.register("sht_305", () -> new Station305(false));
    public static final RegistryObject<Item> BASE305_PLAIN_ITEM = ITEMS.register("sht_305", () -> new BlockItem(STATION305_PLAIN.get(), new Properties()) {
        @Override
        public void initializeClient(Consumer<IClientItemExtensions> c) {
            c.accept(Client.itemExtension());
        }
    });
    public static final RegistryObject<BlockEntityType<Station305.Unit>> TYPE305_PLAIN = ENTITIES.register(
        "station305_plain",
        () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of((p, s) -> new Station305.Unit(p, s, false), STATION305_PLAIN.get()).build(null)
    );
    public static final RegistryObject<StationHi101> HI101 = BLOCKS.register("hi_101", StationHi101::new);
    public static final RegistryObject<Item> HI101_ITEM = ITEMS.register("hi_101", () -> new BlockItem(HI101.get(), new Properties()) {
        @Override
        public void initializeClient(Consumer<IClientItemExtensions> c) {
            c.accept(Client.itemExtension());
        }
    });
    public static final RegistryObject<BlockEntityType<StationHi101.Unit>> TYPE_HI101 = ENTITIES.register(
        "hi101", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(StationHi101.Unit::new, HI101.get()).build(null)
    );
    public static final RegistryObject<Product.StationBlock> STATION = BLOCKS.register("sht_101c", Product.StationBlock::new);
    public static final RegistryObject<Item> BASE_ITEM = ITEMS.register("sht_101c", () -> new BlockItem(STATION.get(), new Properties()) {
        @Override
        public void initializeClient(Consumer<IClientItemExtensions> c) {
            c.accept(Client.itemExtension());
        }
    });
    public static final RegistryObject<Product.StationBlock> STATION102 = BLOCKS.register("sht_102c", () -> new Product.StationBlock(true));
    public static final RegistryObject<Station101N.Block> STATION101N = BLOCKS.register("sht_101n", Station101N.Block::new);
    public static final RegistryObject<Item> BASE101N_ITEM = ITEMS.register("sht_101n", () -> new BlockItem(STATION101N.get(), new Properties()) {
        @Override
        public void initializeClient(Consumer<IClientItemExtensions> c) {
            c.accept(Client.itemExtension());
        }
    });
    public static final RegistryObject<Item> BASE102_ITEM = ITEMS.register("sht_102c", () -> new BlockItem(STATION102.get(), new Properties()) {
        @Override
        public void initializeClient(Consumer<IClientItemExtensions> c) {
            c.accept(Client.itemExtension());
        }
    });
    public static final RegistryObject<Portable> PORTABLE102 = ITEMS.register("sht_102c_portable", Portable::new);
    public static final RegistryObject<Portable> PORTABLE104M = ITEMS.register("sht_104m_portable", Portable::new);
    public static final RegistryObject<Charger> CHARGER = BLOCKS.register("sht_102c_charger", Charger::new);
    public static final RegistryObject<Item> CHARGER_ITEM = ITEMS.register("sht_102c_charger", () -> new BlockItem(CHARGER.get(), new Properties()) {
        @Override
        public void initializeClient(Consumer<IClientItemExtensions> c) {
            c.accept(Client.itemExtension());
        }
    });
    public static final RegistryObject<BlockEntityType<Charger.Dock>> CHARGER_TYPE = ENTITIES.register(
        "charger102", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(Charger.Dock::new, CHARGER.get()).build(null)
    );
    public static final RegistryObject<Item> RADIO = ITEMS.register(
        "wireless_handset",
        () -> new Item(new Properties().stacksTo(1)) {
            @Override
            public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
                if (!slotChanged && oldStack.getItem() == newStack.getItem()) {
                    CompoundTag a = oldStack.getTag();
                    CompoundTag b = newStack.getTag();
                    return a != null && b != null
                        ? !Objects.equals(a.get("serial"), b.get("serial")) || !Objects.equals(a.get("ticket"), b.get("ticket"))
                        : !Objects.equals(a, b);
                } else {
                    return true;
                }
            }

            @Override
            public InteractionResultHolder<ItemStack> use(Level l, Player p, InteractionHand hand) {
                ItemStack stack = p.getItemInHand(hand);
                if (p instanceof ServerPlayer s) {
                    Station base = Station.find(s, stack);
                    if (base != null) {
                        if (p.isShiftKeyDown()) {
                            stack.getOrCreateTag().putBoolean("power", !stack.getOrCreateTag().getBoolean("power"));
                        } else {
                            base.open(s, true);
                        }
                    }
                }

                return InteractionResultHolder.sidedSuccess(stack, l.isClientSide);
            }

            @Override
            public void initializeClient(Consumer<IClientItemExtensions> c) {
                c.accept(Client.itemExtension());
            }
        }
    );
    public static final RegistryObject<BlockEntityType<Station>> TYPE = ENTITIES.register(
        "station", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(Station::new, STATION.get()).build(null)
    );
    public static final RegistryObject<BlockEntityType<Station102>> TYPE102 = ENTITIES.register(
        "station102", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(Station102::new, STATION102.get()).build(null)
    );
    public static final RegistryObject<BlockEntityType<Station101N>> TYPE101N = ENTITIES.register(
        "station101n", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(Station101N::new, STATION101N.get()).build(null)
    );

    public static ResourceLocation id(String path) {
        return new ResourceLocation("homenet_sht101c", path);
    }

    private static boolean commonItem(String path) {
        return Set.of(
                "model_changer",
                "device_mover",
                "analog_linker",
                "control_block",
                "smart_light",
                "rf_card",
                "insert_card",
                "home_linker",
                "door_linker",
                "dashboard",
                "door_station",
                "door_phone",
                "video_phone",
                "interphone",
                "guard_console",
                "wallpad",
                "doorbell"
            )
            .contains(path);
    }

    private static boolean companyItem(String path) {
        return path.equals("sht_5500s")
            || path.equals("hmc_7000")
            || path.equals("ha_4100h")
            || path.equals("kvm_designer")
            || path.startsWith("kvm_")
            || path.startsWith("klp_")
            || path.startsWith("kip_")
            || path.startsWith("kam_")
            || path.startsWith("am_")
            || path.startsWith("ip_")
            || path.startsWith("dp_");
    }

    private static void splitOriginalInventory(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() == ModCreativeTab.MAIN.get()) {
            ArrayList<ItemStack> remove = new ArrayList<>();

            for (Entry<ItemStack, TabVisibility> entry : event.getEntries()) {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(entry.getKey().getItem());
                if (id != null && id.getNamespace().equals("qwertys_homenet") && companyItem(id.getPath())) {
                    remove.add(entry.getKey());
                }
            }

            for (ItemStack stack : remove) {
                event.getEntries().remove(stack);
            }

            event.accept(S1_CARD);
        }
    }

    public Product() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        TABS.register(
            "samsung",
            () -> CreativeModeTab.builder().title(Component.literal("SAMSUNG 홈오토메이션")).icon(() -> new ItemStack(BASE_ITEM.get())).displayItems((a, o) -> {
                o.accept(HT512_ITEM.get());
                o.accept(HT513_ITEM.get());
                o.accept(HT100S_ITEM.get());
                o.accept(HT100_ITEM.get());
                o.accept(BASE_ITEM.get());
                o.accept(BASE101N_ITEM.get());
                o.accept(BASE102_ITEM.get());
                o.accept(BASE104M_ITEM.get());
                o.accept(BASE104C_ITEM.get());
                o.accept(BASE104CS_ITEM.get());
                o.accept(BASE705_ITEM.get());
                o.accept(BASE308_ITEM.get());
                o.accept(BASE307_ITEM.get());
                o.accept(BASE305_ITEM.get());
                o.accept(BASE305_PLAIN_ITEM.get());
                o.accept(HI101_ITEM.get());
                o.accept(PORTABLE102.get());
                o.accept(PORTABLE104M.get());
                o.accept(CHARGER_ITEM.get());
                o.accept(ModItems.SHT_5500S.get());
            }).build()
        );
        TABS.register(
            "hyundai",
            () -> CreativeModeTab.builder()
                .title(Component.literal("HYUNDAI 홈오토메이션"))
                .icon(() -> new ItemStack(ModItems.HMC_7000.get()))
                .displayItems((a, o) -> {
                    for (Item item : ForgeRegistries.ITEMS.getValues()) {
                        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
                        if (id != null && id.getNamespace().equals("qwertys_homenet") && Set.of("hmc_7000", "ha_4100h").contains(id.getPath())) {
                            o.accept(item);
                        }
                    }
                })
                .build()
        );
        TABS.register(
            "kocom",
            () -> CreativeModeTab.builder()
                .title(Component.literal("KOCOM 홈오토메이션"))
                .icon(() -> new ItemStack(ModItems.KVM_906.get()))
                .displayItems(
                    (a, o) -> {
                        for (Item item : ForgeRegistries.ITEMS.getValues()) {
                            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
                            if (id != null
                                && id.getNamespace().equals("qwertys_homenet")
                                && companyItem(id.getPath())
                                && !Set.of("sht_5500s", "hmc_7000", "ha_4100h").contains(id.getPath())) {
                                o.accept(item);
                            }
                        }
                    }
                )
                .build()
        );
        TABS.register(bus);
        bus.addListener(Product::splitOriginalInventory);
        Wire.init();
        MovementSupport.init();
    }

    public static class StationBlock extends WallMountedBlock {
        final boolean wired;

        public StationBlock() {
            this(false);
        }

        public StationBlock(boolean wired) {
            super(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(1.5F).noOcclusion(), 4.36, 4.7, 11.2, 1.0);
            this.wired = wired;
            this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WallpadBlock.RINGING, false));
        }

        @Override
        protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
            b.add(FACING, WallpadBlock.RINGING);
        }

        @Override
        public RenderShape getRenderShape(BlockState s) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
            return (BlockEntity)(this.wired ? new Station102(p, s) : new Station(p, s));
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
            return l.isClientSide
                ? null
                : (
                    this.wired
                        ? createTickerHelper(t, Product.TYPE102.get(), ReceiverBlockEntity::serverTick)
                        : createTickerHelper(t, Product.TYPE.get(), ReceiverBlockEntity::serverTick)
                );
        }

        @Override
        public InteractionResult use(BlockState s, Level l, BlockPos at, Player player, InteractionHand hand, BlockHitResult hit) {
            ItemStack held = player.getItemInHand(hand);
            if (!(held.getItem() instanceof HomeLinkerItem) && !(held.getItem() instanceof AnalogLinkerItem)) {
                if (player instanceof ServerPlayer p && l.getBlockEntity(at) instanceof Station b) {
                    if (held.is(Product.PORTABLE102.get()) || held.is(Product.PORTABLE104M.get())) {
                        b.tell("홈링커로 내부기기를 선택한 뒤 휴대장치가 놓인 충전기를 클릭하세요");
                    } else if (held.is(Product.RADIO.get())) {
                        b.replace(p);
                    } else if (!b.docked && cradle(s, l, at, hit)) {
                        b.replace(p);
                    } else {
                        b.open(p, false);
                    }
                }

                return InteractionResult.sidedSuccess(l.isClientSide);
            } else {
                return InteractionResult.PASS;
            }
        }

        public static boolean cradle(BlockState s, Level l, BlockPos at, BlockHitResult hit) {
            Vec3 v = hit.getLocation().subtract(Vec3.atLowerCornerOf(at)).subtract(DeviceOffsets.get(l, at));

            double across = switch ((Direction)s.getValue(FACING)) {
                case NORTH -> 0.5 - v.x;
                case SOUTH -> v.x - 0.5;
                case EAST -> 0.5 - v.z;
                case WEST -> v.z - 0.5;
                default -> 10.0;
            };
            double x = 523.5 + across / 6.067961165048543E-4;
            double y = 700.0 - (v.y - 0.3) / 6.067961165048543E-4;
            return x >= 145.0 && x <= 294.0 && y >= 126.0 && y <= 640.0;
        }

        @Override
        public void onRemove(BlockState s, Level l, BlockPos p, BlockState n, boolean moving) {
            if (!s.is(n.getBlock()) && l.getBlockEntity(p) instanceof Station b) {
                b.onBroken();
            }

            super.onRemove(s, l, p, n, moving);
        }
    }
}
