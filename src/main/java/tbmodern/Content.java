package tbmodern;

import java.util.*;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.*;

public final class Content {
    public static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ThaumicBases.ID);
    public static final RegistryObject<net.minecraft.world.inventory.MenuType<Overchanter.Menu>> OVERCHANTER_MENU = MENUS.register("overchanter", () -> new net.minecraft.world.inventory.MenuType<>(Overchanter.Menu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<net.minecraft.world.inventory.MenuType<net.minecraft.world.inventory.AnvilMenu>> THAUMIC_ANVIL_MENU = MENUS.register("thaumic_anvil", () -> new net.minecraft.world.inventory.MenuType<>((id, inv) -> new MagicalAnvil.Menu(id, inv, false), net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<net.minecraft.world.inventory.MenuType<net.minecraft.world.inventory.AnvilMenu>> VOID_ANVIL_MENU = MENUS.register("void_anvil", () -> new net.minecraft.world.inventory.MenuType<>((id, inv) -> new MagicalAnvil.Menu(id, inv, true), net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ThaumicBases.ID);
    public static final RegistryObject<net.minecraft.world.entity.EntityType<RevolverBullet>> REVOLVER_BULLET = ENTITIES.register("revolver_bullet", () -> net.minecraft.world.entity.EntityType.Builder.<RevolverBullet>of(RevolverBullet::new, net.minecraft.world.entity.MobCategory.MISC).sized(.25F,.25F).clientTrackingRange(8).updateInterval(1).build("thaumicbases:revolver_bullet"));
    public static final DeferredRegister<net.minecraft.sounds.SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ThaumicBases.ID);
    public static final RegistryObject<net.minecraft.sounds.SoundEvent> REVOLVER_SHOT = SOUNDS.register("revolver.shot",()->net.minecraft.sounds.SoundEvent.createVariableRangeEvent(new ResourceLocation(ThaumicBases.ID,"revolver.shot")));
    public static final RegistryObject<net.minecraft.sounds.SoundEvent> REVOLVER_RELOAD = SOUNDS.register("revolver.reload",()->net.minecraft.sounds.SoundEvent.createVariableRangeEvent(new ResourceLocation(ThaumicBases.ID,"revolver.reload")));
    public static final RegistryObject<net.minecraft.sounds.SoundEvent> REVOLVER_CLICK = SOUNDS.register("revolver.click",()->net.minecraft.sounds.SoundEvent.createVariableRangeEvent(new ResourceLocation(ThaumicBases.ID,"revolver.click")));
    public static final RegistryObject<net.minecraft.world.inventory.MenuType<Revolver.Menu>> REVOLVER_MENU = MENUS.register("revolver",()->net.minecraftforge.common.extensions.IForgeMenuType.create((id,inventory,buffer)->new Revolver.Menu(id,inventory,buffer.readVarInt())));
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ThaumicBases.ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ThaumicBases.ID);
    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> TILES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ThaumicBases.ID);
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<Overchanter>> OVERCHANTER = TILES.register("overchanter",
        () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(Overchanter::new, block("overchanter")).build(null));
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<AdvancedFurnace>> ADVANCED_FURNACE=TILES.register("advanced_alchemical_furnace",
        ()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(AdvancedFurnace::new,block("advanced_alchemical_furnace")).build(null));
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<EntityDeconstructor>> DECONSTRUCTOR=TILES.register("entity_deconstructor",
        ()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(EntityDeconstructor::new,block("entity_deconstructor")).build(null));
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<WardingFire>> WARDING_FIRE=TILES.register("warding_fire",
        ()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(WardingFire::new,block("campfire"),block("brazier")).build(null));
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<Relocator>> RELOCATOR=TILES.register("relocator",
        ()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(Relocator::new,block("relocator"),block("attracting_relocator")).build(null));
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<NodeLinker>> LINKER = TILES.register("node_linker",
        () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(NodeLinker::new, block("node_linker")).build(null));
    public static final List<String> NODE_MODES = List.of("brightness", "destruction", "efficiency", "hunger", "instability", "purity", "sinister", "speed", "stability", "taint");
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<NodeManipulator>> MANIPULATOR = TILES.register("node_manipulator",
        () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(NodeManipulator::new, block("node_manipulator")).build(null));
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, ThaumicBases.ID);
    public static final Map<String, RegistryObject<Item>> ITEM_IDS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Block>> BLOCK_IDS = new LinkedHashMap<>();
    public static final List<String> CRYSTALS = List.of("crystal_air", "crystal_fire", "crystal_water", "crystal_earth", "crystal_order", "crystal_entropy", "crystal_mixed", "crystal_tainted");
    public static final List<PlantBlock.Definition> PLANTS = List.of(
        new PlantBlock.Definition("plax", 8, 4, "farmland", "plax_seeds", "minecraft:string"),
        new PlantBlock.Definition("metalleat", 8, 8, "farmland", "metalleat_seeds", "minecraft:iron_nugget"),
        new PlantBlock.Definition("lucrite", 8, 16, "farmland", "lucrite_seeds", "minecraft:gold_nugget"),
        new PlantBlock.Definition("knose", 4, 16, "crystal", "knose_seed", ""),
        new PlantBlock.Definition("sweed", 4, 4, "grass", "sweed_seeds", ""),
        new PlantBlock.Definition("lazullia", 8, 16, "farmland", "lazullia_seeds", "minecraft:lapis_lazuli"),
        new PlantBlock.Definition("glieonia", 4, 12, "grass", "glieonia_seed", "minecraft:glowstone_dust"),
        new PlantBlock.Definition("tobacco", 8, 16, "farmland", "tobacco_seeds", "tobacco_leaves"),
        new PlantBlock.Definition("void_plant", 4, 32, "farmland", "void_seed", "thaumcraft:void_seed"),
        new PlantBlock.Definition("ashroom", 4, 8, "solid", "ashroom", "")
    );
    static {
        registerItem("revolver",Revolver::new);
        for(var definition:Bracelet.definitions()) registerItem("bracelet_"+definition.id(),()->new Bracelet(definition));
        for(String kind:List.of("activation","drain","experience","flux")) registerItem("focus_"+kind,()->new Foci(kind));
        for(String kind:List.of("thaumic_anvil","void_anvil")) {
            var anvil = registerBlock(kind,()->new MagicalAnvil(kind.equals("void_anvil")),false);
            registerItem(kind,()->new MagicalAnvil.Item(anvil.get()));
        }
        registerBlock("overchanter",Overchanter.Device::new,true);
        registerBlock("ender_planks",DecorativeBlocks.EnderPlanks::new,true);
        for(String kind : Trees.KINDS) {
            registerBlock(kind+"_sapling",()->new Trees.Sapling(kind),true);
            registerBlock(kind+"_leaves",()->new Trees.Leaves(kind),true);
            if(!kind.equals("golden_oak")) registerBlock(kind+"_log",()->new Trees.Log(kind),true);
        }
        registerBlock("rainbow_cactus",SpecialPlants.RainbowCactus::new,true);
        registerBlock("aurelia",SpecialPlants.Aurelia::new,true);
        registerBlock("aurelia_petal_block",SpecialPlants.Petal::new,true);
        registerBlock("flaxium",SpecialPlants.Flaxium::new,true);
        registerBlock("briar",SpecialPlants.Briar::new,true);
        var redlon=registerBlock("redlon_stem",SpecialPlants.Redlon::new,false);
        registerItem("redlon_seeds",()->new ItemNameBlockItem(redlon.get(),new Item.Properties()));
        registerBlock("advanced_alchemical_furnace",AdvancedFurnace.Device::new,true);
        registerBlock("entity_deconstructor",EntityDeconstructor.Device::new,true);
        registerBlock("campfire",()->new WardingFire.Device(false),true);
        registerBlock("brazier",()->new WardingFire.Device(true),true);
        registerBlock("relocator",()->new Relocator.Device(false),true);
        registerBlock("attracting_relocator",()->new Relocator.Device(true),true);
        // Structure-only block. Inventory, recipes and drops use vanilla crying obsidian.
        registerBlock("crying_obelisk",CryingObelisk::new,false);
        registerItem("herobrines_scythe",HerobrinesScythe::new);
        registerBlock("pyrofluid",PyrofluidBlock::new,false);
        registerItem("pyrofluid_bucket",PyrofluidBlock.Bucket::new);
        registerItem("concentrated_taint",ConcentratedTaint::new);
        for(String kind:List.of("iron","thaumic","void"))registerBlock("spike_"+kind,()->new SpikeBlock(kind),true);
        for(ArmorItem.Type type:List.of(ArmorItem.Type.CHESTPLATE,ArmorItem.Type.LEGGINGS,ArmorItem.Type.BOOTS))
            registerItem("bloody_"+type.getName(),()->new BloodyArmor(type));
        for(String kind : List.of("pile","eldritch","fighting","hunger","knowledge","mining","sanity","tainted","wispy"))
            registerItem("tobacco_"+kind, () -> new TobaccoItems.Dose(kind));
        registerItem("greatwood_pipe", () -> new TobaccoItems.Pipe(false));
        registerItem("silverwood_pipe", () -> new TobaccoItems.Pipe(true));
        registerItem("mortar", TobaccoItems.Mortar::new);
        registerItem("rosehip_syrup", RosehipSyrup::new);
        registerBlock("node_linker", NodeLinkerBlock::new, true);
        registerBlock("node_manipulator", NodeManipulatorBlock::new, true);
        for (String mode : NODE_MODES) registerItem("node_focus_" + mode, () -> new Item(new Item.Properties().stacksTo(1)));
        for (String id : List.of("thauminite_nugget", "thauminite_ingot", "thauminite_wand_cap", "thaumium_wand_core", "void_wand_core", "aurelia_petal", "briar_seedbag", "tobacco_leaves", "bloody_cloth"))
            registerItem(id, () -> new Item(new Item.Properties()));
        for (String crystal : CRYSTALS) {
            String kind = crystal.substring(8);
            registerItem("knose_fragment_" + kind, () -> new KnowledgeFragment(kind));
            registerBlock(crystal, () -> new Block(BlockBehaviour.Properties.of().strength(.7F, 1).sound(SoundType.AMETHYST).lightLevel(s -> 7)), true);
            registerBlock(crystal + "_slab", () -> new SlabBlock(BlockBehaviour.Properties.of().strength(.7F, 1).sound(SoundType.AMETHYST).lightLevel(s -> 7)), true);
        }
        for (String id : List.of("quicksilver_block", "quicksilver_brick", "salis_mundus_block", "thauminite_block", "eldritch_ark", "old_brick", "old_cobble", "old_mossy_cobble", "old_lapis", "old_iron", "old_gold", "old_diamond"))
            registerBlock(id, () -> new Block(BlockBehaviour.Properties.of().strength(id.equals("thauminite_block") ? 2 : id.equals("eldritch_ark") ? 3 : 1)), true);
        for (String id : List.of("eldritch_ark", "old_brick", "old_cobble", "old_mossy_cobble", "old_lapis", "old_iron", "old_gold", "old_diamond"))
            registerBlock(id + "_slab", () -> new SlabBlock(BlockBehaviour.Properties.of().strength(1, 10)), true);
        registerBlock("old_gravel", () -> new GravelBlock(BlockBehaviour.Properties.copy(Blocks.GRAVEL)), true);
        registerBlock("iron_greatwood", () -> new Block(BlockBehaviour.Properties.of().strength(1, 5).sound(SoundType.WOOD)), true);
        registerBlock("void_block", DecorativeBlocks.VoidBlock::new, true);
        for (var plant : PLANTS) {
            var block = registerBlock(plant.id(), () -> new PlantBlock(plant), false);
            registerItem(plant.seed(), () -> new ItemNameBlockItem(block.get(), new Item.Properties()));
        }
        registerItem("thauminite_axe", Equipment.Axe::new);
        registerItem("thauminite_pickaxe", Equipment.Pick::new);
        registerItem("thauminite_shovel", Equipment.Shovel::new);
        registerItem("thauminite_hoe", Equipment.Hoe::new);
        registerItem("thauminite_sword", Equipment.Sword::new);
        registerItem("thauminite_shears", () -> new Equipment.Shears(974));
        registerItem("void_shears", Equipment.VoidShears::new);
        registerItem("void_flint_and_steel", Equipment.VoidLighter::new);
        for (ArmorItem.Type type : ArmorItem.Type.values()) registerItem("thauminite_" + type.getName(), () -> new Equipment.Armor(type));
        TABS.register("main", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.thaumicbases"))
            .icon(() -> new ItemStack(item("thauminite_ingot"))).displayItems((parameters, output) -> ITEM_IDS.values().forEach(item -> output.accept(item.get()))).build());
    }
    public static RegistryObject<Item> registerItem(String id, Supplier<? extends Item> factory) {
        RegistryObject<Item> result = ITEMS.register(id, () -> factory.get());
        if (ITEM_IDS.putIfAbsent(id, result) != null) throw new IllegalArgumentException("Duplicate item " + id);
        return result;
    }
    public static RegistryObject<Block> registerBlock(String id, Supplier<? extends Block> factory, boolean item) {
        RegistryObject<Block> result = BLOCKS.register(id, () -> factory.get());
        if (BLOCK_IDS.putIfAbsent(id, result) != null) throw new IllegalArgumentException("Duplicate block " + id);
        if (item) registerItem(id, () -> id.equals("node_linker")||id.equals("node_manipulator") ? new NodeDeviceItem(result.get()) : new BlockItem(result.get(), new Item.Properties()));
        return result;
    }
    public static Item item(String id) {
        if (id.contains(":")) return Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(new ResourceLocation(id)), id);
        return ITEM_IDS.get(id).get();
    }
    public static Block block(String id) { return BLOCK_IDS.get(id).get(); }
    private Content() {}
}
