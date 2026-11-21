package thaumicfutures.expansion;

import java.util.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;
import thaumicfutures.LegacyContent;

/** Content adapted from Thaumcraft 6; original artwork is provided by the external resource pack. */
public final class RestoredContent {
    /** Ownership queries must not initialize Forge registries during configuration discovery. */
    public static final class Ownership {
        public static final Set<String> ITEM_IDS=new LinkedHashSet<>(Set.of("thaumic_sponge","wet_thaumic_sponge","creative_essentia_tank"));
        private Ownership() {}
    }
    public static final Set<String> ITEM_IDS=Ownership.ITEM_IDS;
    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,LegacyContent.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,LegacyContent.MOD_ID);
    public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> PROJECTILES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,LegacyContent.MOD_ID);
    public static final DeferredRegister<net.minecraft.world.level.levelgen.feature.Feature<?>> FEATURES=DeferredRegister.create(ForgeRegistries.FEATURES,LegacyContent.MOD_ID);
    public static final Map<tc4revival.content.ShardType,RegistryObject<PlantedCrystal>> PLANTED_CRYSTALS=new EnumMap<>(tc4revival.content.ShardType.class);
    static{
        for(var type:tc4revival.content.ShardType.values())if(type!=tc4revival.content.ShardType.BALANCED)
            PLANTED_CRYSTALS.put(type,BLOCKS.register("planted_"+type.material()+"_crystal",()->new PlantedCrystal(type)));
    }
    public static final RegistryObject<BlockEntityType<PlantedCrystal.Tile>> CRYSTAL_ENTITY=ENTITIES.register("planted_crystal",
            ()->BlockEntityType.Builder.of(PlantedCrystal.Tile::new,PLANTED_CRYSTALS.values().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));
    public static final RegistryObject<RestoredOreFeature> QUARTZ_FEATURE=FEATURES.register("quartz_ore",RestoredOreFeature::new);
    public static final RegistryObject<Block> QUARTZ_ORE=block("quartz_ore",()->new DropExperienceBlock(Block.Properties.copy(Blocks.IRON_ORE),net.minecraft.util.valueproviders.UniformInt.of(2,5)));
    public static final RegistryObject<Block> DEEPSLATE_QUARTZ_ORE=block("deepslate_quartz_ore",()->new DropExperienceBlock(Block.Properties.copy(Blocks.DEEPSLATE_IRON_ORE),net.minecraft.util.valueproviders.UniformInt.of(2,5)));
    public static final RegistryObject<ThaumicSpongeBlock> SPONGE=BLOCKS.register("thaumic_sponge",ThaumicSpongeBlock::new);
    public static final RegistryObject<Block> WET_SPONGE=BLOCKS.register("wet_thaumic_sponge",()->new Block(Block.Properties.copy(Blocks.WET_SPONGE)));
    public static final RegistryObject<CreativeEssentiaTankBlock> CREATIVE_TANK=BLOCKS.register("creative_essentia_tank",CreativeEssentiaTankBlock::new);
    public static final RegistryObject<RestoredSmelter.Device> THAUMIUM_SMELTER=block("thaumium_smelter",RestoredSmelter.Device::new);
    public static final RegistryObject<RestoredSmelter.Device> VOID_SMELTER=block("void_smelter",RestoredSmelter.Device::new);
    public static final RegistryObject<SlurryPump> SLURRY_PUMP=block("slurry_pump",SlurryPump::new);
    public static final RegistryObject<EverfullUrn> EVERFULL_URN=block("everfull_urn",EverfullUrn::new);
    public static final RegistryObject<BlockEntityType<EverfullUrn.Tile>> URN_ENTITY=ENTITIES.register("everfull_urn",
            ()->BlockEntityType.Builder.of(EverfullUrn.Tile::new,EVERFULL_URN.get()).build(null));
    public static final RegistryObject<Item> BRASS_NUGGET=item("alchemical_brass_nugget",()->new Item(new Item.Properties()));
    public static final RegistryObject<Block> BRASS_BLOCK=block("alchemical_brass_block",()->new Block(Block.Properties.copy(Blocks.IRON_BLOCK)));
    public static final RegistryObject<Item> RARE_EARTH=item("rare_earth",()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> MORPHIC_RESONATOR=item("morphic_resonator",()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> VIS_RESONATOR=item("vis_resonator",()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> RABBIT_NUGGET=item("rabbit_nugget",()->new Item(new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(1).saturationMod(.1f).meat().fast().build())));
    public static final RegistryObject<Item> MUTTON_NUGGET=item("mutton_nugget",()->new Item(new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(1).saturationMod(.1f).meat().fast().build())));
    public static final RegistryObject<RestoredAccessories> CLOUD_RING=item("cloud_ring",RestoredAccessories::new);
    public static final RegistryObject<RestoredAccessories> UNDYING_CHARM=item("undying_charm",RestoredAccessories::new);
    public static final RegistryObject<RestoredAccessories> VOIDSEER_PEARL=item("voidseer_pearl",RestoredAccessories::new);
    public static final RegistryObject<ParadoxMatter> PARADOX_MATTER=item("paradox_matter",ParadoxMatter::new);
    public static final RegistryObject<net.minecraft.world.entity.EntityType<ParadoxMatter.Projectile>> PARADOX_ENTITY=PROJECTILES.register("paradox_matter",
            ()->net.minecraft.world.entity.EntityType.Builder.<ParadoxMatter.Projectile>of(ParadoxMatter.Projectile::new,net.minecraft.world.entity.MobCategory.MISC).sized(.25f,.25f).clientTrackingRange(4).updateInterval(1).build("thaumicfutures:paradox_matter"));
    public static final RegistryObject<EssentiaTransceiver> ESSENTIA_EXTRACTOR=block("essentia_extractor",()->new EssentiaTransceiver(true));
    public static final RegistryObject<EssentiaTransceiver> ESSENTIA_INJECTOR=block("essentia_injector",()->new EssentiaTransceiver(false));
    public static final RegistryObject<BlockEntityType<EssentiaTransceiver.Tile>> TRANSCEIVER_ENTITY=ENTITIES.register("essentia_transceiver",
            ()->BlockEntityType.Builder.of(EssentiaTransceiver.Tile::new,ESSENTIA_EXTRACTOR.get(),ESSENTIA_INJECTOR.get()).build(null));
    public static final Map<DyeColor,RegistryObject<DyedNitor>> NITORS=new EnumMap<>(DyeColor.class);
    static{for(var color:DyeColor.values())NITORS.put(color,block(color.getName()+"_nitor",()->new DyedNitor(color)));}
    public static final RegistryObject<BlockEntityType<DyedNitor.Tile>> NITOR_ENTITY=ENTITIES.register("dyed_nitor",
            ()->BlockEntityType.Builder.of(DyedNitor.Tile::new,NITORS.values().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));
    public static final Map<String,RegistryObject<Item>> CURIOS=new LinkedHashMap<>(),NOTES=new LinkedHashMap<>();
    static{
        ResearchCurio.TYPES.forEach((name,aspects)->CURIOS.put(name,item("curio_"+name,()->new ResearchCurio(aspects))));
        CelestialNotes.TYPES.forEach((name,aspects)->NOTES.put(name,item("celestial_notes_"+name,()->new ResearchCurio(aspects))));
    }
    public static final RegistryObject<BlockEntityType<RestoredSmelter>> SMELTER_ENTITY=ENTITIES.register("restored_smelter",
            ()->BlockEntityType.Builder.of(RestoredSmelter::new,THAUMIUM_SMELTER.get(),VOID_SMELTER.get()).build(null));
    public static final RegistryObject<BlockEntityType<SlurryPump.Tile>> PUMP_ENTITY=ENTITIES.register("slurry_pump",
            ()->BlockEntityType.Builder.of(SlurryPump.Tile::new,SLURRY_PUMP.get()).build(null));
    public static final RegistryObject<BlockEntityType<CreativeEssentiaTankEntity>> CREATIVE_TANK_ENTITY=ENTITIES.register("creative_essentia_tank",
            ()->BlockEntityType.Builder.of(CreativeEssentiaTankEntity::new,CREATIVE_TANK.get()).build(null));
    public static final RegistryObject<Item> SPONGE_ITEM=LegacyContent.ITEMS.register("thaumic_sponge",()->new ThaumicSpongeItem(SPONGE.get()));
    public static final RegistryObject<Item> WET_SPONGE_ITEM=LegacyContent.ITEMS.register("wet_thaumic_sponge",()->new BlockItem(WET_SPONGE.get(),new Item.Properties()));
    public static final RegistryObject<Item> CREATIVE_TANK_ITEM=LegacyContent.ITEMS.register("creative_essentia_tank",
            ()->new BlockItem(CREATIVE_TANK.get(),new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static <T extends Block> RegistryObject<T> block(String name,java.util.function.Supplier<T> factory){
        var block=BLOCKS.register(name,factory);ITEM_IDS.add(name);
        LegacyContent.ITEMS.register(name,()->new BlockItem(block.get(),new Item.Properties()));return block;
    }
    public static <T extends Item> RegistryObject<T> item(String name,java.util.function.Supplier<T> factory){
        ITEM_IDS.add(name);return LegacyContent.ITEMS.register(name,factory);
    }
    public static void register(IEventBus bus){
        RestoredInfusion.register();
        ExpansionNetwork.register();
        BLOCKS.register(bus);
        ENTITIES.register(bus);
        PROJECTILES.register(bus);
        FEATURES.register(bus);
    }
    private RestoredContent(){}
}
