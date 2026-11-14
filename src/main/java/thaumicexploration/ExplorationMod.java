package thaumicexploration;

import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.*;

@Mod(ExplorationMod.ID)
public final class ExplorationMod {
    public static final String ID = "thaumicexploration";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ID);
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ID);
    public static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, ID);
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, ID);
    public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> MOBS = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ID);
    public static final RegistryObject<net.minecraft.world.entity.EntityType<TaintacleMinion>> MINION=MOBS.register("taintacle_minion",()->net.minecraft.world.entity.EntityType.Builder.of(TaintacleMinion::new,net.minecraft.world.entity.MobCategory.CREATURE).sized(.6F,1).clientTrackingRange(8).build("thaumicexploration:taintacle_minion"));
    public static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ID);
    public static final RegistryObject<net.minecraft.world.inventory.MenuType<ThinkTankMenu>> THINK_MENU = MENUS.register("think_tank", () -> net.minecraftforge.common.extensions.IForgeMenuType.create(ThinkTankMenu::new));
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
    public static final Map<String,RegistryObject<Item>> ITEM = new LinkedHashMap<>();
    public static final Map<String,RegistryObject<Block>> BLOCK = new LinkedHashMap<>();
    public static final RegistryObject<BlockEntityType<MachineEntity>> MACHINE;
    public static final RegistryObject<BlockEntityType<BoundEntity>> BOUND;
    public static final RegistryObject<BlockEntityType<CandleEntity>> CANDLE;
    public static final RegistryObject<Enchantment> BINDING = ENCHANTMENTS.register("binding", () -> new ExplorationEnchantment("binding"));
    public static final RegistryObject<Enchantment> DISARM = ENCHANTMENTS.register("disarm", () -> new ExplorationEnchantment("disarm"));
    public static final RegistryObject<Enchantment> NIGHT_VISION = ENCHANTMENTS.register("night_vision", () -> new ExplorationEnchantment("night_vision"));
    public static final RegistryObject<MobEffect> BOUND_EFFECT = EFFECTS.register("binding", BindingEffect::new);
    public static final RegistryObject<MobEffect> WITHDRAWAL = EFFECTS.register("taint_withdrawal", WithdrawalEffect::new);
    static {
        for (String name : List.of("transmutation_core","transmutation_staff_core","amber_core","amber_staff_core","necro_staff_core","bread_core","sojourner_cap","sojourner_cap_uncharged","mechanist_cap","mechanist_cap_uncharged","charm_no_taint","charm_taint"))
            item(name, () -> new Item(new Item.Properties()));
        item("pure_zombie_brain", () -> new Item(new Item.Properties()));
        item("talisman_food", NourishmentTalisman::new);
        item("boots_meteor", () -> new ExplorationBoots(true));
        item("boots_comet", () -> new ExplorationBoots(false));
        item("stabilizer_belt", () -> new ExplorationCurio("belt", null));
        item("tentacle_ring", () -> new ExplorationCurio("tentacle", null));
        for (String aspect : List.of("aer","terra","ignis","aqua","ordo","perditio"))
            item("discount_ring_" + aspect, () -> new ExplorationCurio("discount", aspect));
        item("taint_berry", () -> new TaintBerry());
        for (DyeColor color : DyeColor.values()) {
            for (String kind : List.of("blank_seal","chest_seal","jar_seal"))
                item(kind + "_" + color.getName(), () -> new SealItem(kind, color));
            String name = "floating_candle_" + color.getName();
            block(name, () -> new FloatingCandle(color));
        }
        for (String name : List.of("think_tank","everfull_urn","everburn_urn","soul_brazier","crucible_souls","replicator","trash_jar"))
            block(name, () -> new MachineBlock(name));
        block("bound_chest", () -> new BoundBlock(false));
        block("bound_jar", () -> new BoundBlock(true));
        MACHINE = ENTITIES.register("machine", () -> BlockEntityType.Builder.of(MachineEntity::new,
                BLOCK.values().stream().map(RegistryObject::get).filter(b -> b instanceof MachineBlock).toArray(Block[]::new)).build(null));
        BOUND = ENTITIES.register("bound", () -> BlockEntityType.Builder.of(BoundEntity::new, block("bound_chest"), block("bound_jar")).build(null));
        CANDLE = ENTITIES.register("floating_candle", () -> BlockEntityType.Builder.of(CandleEntity::new,
                BLOCK.values().stream().map(RegistryObject::get).filter(b -> b instanceof FloatingCandle).toArray(Block[]::new)).build(null));
        TABS.register("exploration", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.thaumicexploration"))
                .icon(() -> new ItemStack(item("amber_core"))).displayItems((parameters, output) -> ITEM.values().forEach(i -> output.accept(i.get()))).build());
    }
    private static void item(String name, java.util.function.Supplier<Item> item) { ITEM.put(name, ITEMS.register(name, item)); }
    private static void block(String name, java.util.function.Supplier<Block> block) {
        var registered = BLOCKS.register(name, block); BLOCK.put(name, registered);
        item(name, () -> new BlockItem(registered.get(), new Item.Properties()));
    }
    public static Item item(String name) { return ITEM.get(name).get(); }
    public static Block block(String name) { return BLOCK.get(name).get(); }
    public static ResourceLocation id(String name) { return new ResourceLocation(ID, name); }
    public static Item core(String name) { return ForgeRegistries.ITEMS.getValue(new ResourceLocation("thaumcraft", name)); }
    public ExplorationMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus); BLOCKS.register(bus); ENTITIES.register(bus); ENCHANTMENTS.register(bus); EFFECTS.register(bus); TABS.register(bus); MENUS.register(bus); MOBS.register(bus);
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event)->event.put(MINION.get(),tc4revival.content.SmallTaintacleEntity.attributes().build()));
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON,ExplorationConfig.SPEC);
        bus.addListener(this::setup);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new ExplorationEvents());
    }
    private void setup(FMLCommonSetupEvent event) { event.enqueueWork(()->{
        WandBehaviors.register();ExplorationConfig.register();
        net.minecraftforge.common.world.ForgeChunkManager.setForcedChunkLoadingCallback(ID,(level,tickets)->{
            for(var pos:java.util.List.copyOf(tickets.getBlockTickets().keySet()))
                if(!(level.getBlockEntity(pos) instanceof MachineEntity machine)||!machine.kind().equals("soul_brazier")||!machine.active)tickets.removeAllTickets(pos);
        });
    }); }
}
