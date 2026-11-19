package emtmodern;

import emtmodern.item.ElectricScribingTools;
import emtmodern.item.ElectricTool;
import emtmodern.item.ToolProfile;
import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.*;
import java.util.LinkedHashMap;
import java.util.Map;

@Mod(ElectroMagicTools.MOD_ID)
public class ElectroMagicTools {
    public static final String MOD_ID="emt";
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB,MOD_ID);
    public static final Map<String,RegistryObject<Item>> CONTENT=new LinkedHashMap<>();
    public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,MOD_ID);
    public static final RegistryObject<net.minecraft.world.entity.EntityType<emtmodern.focus.ShieldEntity>> SHIELD_ENTITY=ENTITIES.register("shield",()->net.minecraft.world.entity.EntityType.Builder.<emtmodern.focus.ShieldEntity>of(emtmodern.focus.ShieldEntity::new,net.minecraft.world.entity.MobCategory.MISC).sized(4,4).clientTrackingRange(6).updateInterval(1).noSave().build("emt:shield"));
    public static final RegistryObject<net.minecraft.world.entity.EntityType<emtmodern.focus.LaserEntity>> LASER_ENTITY=ENTITIES.register("laser",()->net.minecraft.world.entity.EntityType.Builder.<emtmodern.focus.LaserEntity>of(emtmodern.focus.LaserEntity::new,net.minecraft.world.entity.MobCategory.MISC).sized(.5f,.5f).clientTrackingRange(8).updateInterval(1).build("emt:laser"));
    public static final RegistryObject<net.minecraft.world.entity.EntityType<emtmodern.focus.EnergyBallEntity>> ENERGY_BALL_ENTITY=ENTITIES.register("energy_ball",()->net.minecraft.world.entity.EntityType.Builder.<emtmodern.focus.EnergyBallEntity>of(emtmodern.focus.EnergyBallEntity::new,net.minecraft.world.entity.MobCategory.MISC).sized(1,1).clientTrackingRange(5).updateInterval(1).build("emt:energy_ball"));
    public static final RegistryObject<net.minecraft.world.entity.EntityType<emtmodern.item.ArcherEntity>> ARCHER_ENTITY=ENTITIES.register("archer",()->net.minecraft.world.entity.EntityType.Builder.<emtmodern.item.ArcherEntity>of(emtmodern.item.ArcherEntity::new,net.minecraft.world.entity.MobCategory.MISC).sized(.7f,1.9f).clientTrackingRange(5).updateInterval(3).build("emt:archer"));
    public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,MOD_ID);
    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,MOD_ID);
    public static final RegistryObject<emtmodern.energy.PortableNodeBlock> PORTABLE_NODE=BLOCKS.register("portable_node",emtmodern.energy.PortableNodeBlock::new);
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<emtmodern.energy.PortableNodeEntity>> PORTABLE_NODE_BE=BLOCK_ENTITIES.register("portable_node",()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(emtmodern.energy.PortableNodeEntity::new,PORTABLE_NODE.get()).build(null));
    public static final RegistryObject<emtmodern.energy.AmberBricksBridge> AMBER_BRICKS=BLOCKS.register("amber_bricks",emtmodern.energy.AmberBricksBridge::new);
    public static final RegistryObject<emtmodern.energy.ElectricCloudBlock> CLOUD=BLOCKS.register("electric_cloud",emtmodern.energy.ElectricCloudBlock::new);
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<emtmodern.energy.ElectricCloudEntity>> CLOUD_BE=BLOCK_ENTITIES.register("electric_cloud",()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(emtmodern.energy.ElectricCloudEntity::new,CLOUD.get()).build(null));
    public static final RegistryObject<Item> CLOUD_ITEM=register("electric_cloud",emtmodern.item.ElectricCloudItem::new);
    public static final RegistryObject<emtmodern.energy.ShieldBlock> SHIELD=BLOCKS.register("shield",emtmodern.energy.ShieldBlock::new);
    public static final RegistryObject<emtmodern.energy.WandChargerBlock> WAND_CHARGER=BLOCKS.register("wand_charger",emtmodern.energy.WandChargerBlock::new);
    public static final RegistryObject<emtmodern.energy.EtherealProcessorBlock> PROCESSOR=BLOCKS.register("ethereal_processor",emtmodern.energy.EtherealProcessorBlock::new);
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<emtmodern.energy.EtherealProcessorEntity>> PROCESSOR_BE=BLOCK_ENTITIES.register("ethereal_processor",()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(emtmodern.energy.EtherealProcessorEntity::new,PROCESSOR.get()).build(null));
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<emtmodern.energy.WandChargerEntity>> WAND_CHARGER_BE=BLOCK_ENTITIES.register("wand_charger",()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(emtmodern.energy.WandChargerEntity::new,WAND_CHARGER.get()).build(null));
    public static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,MOD_ID);
    public static final RegistryObject<net.minecraft.world.inventory.MenuType<emtmodern.energy.EtherealProcessorMenu>> PROCESSOR_MENU=MENUS.register("ethereal_processor",()->net.minecraftforge.common.extensions.IForgeMenuType.create((id,inventory,buffer)->new emtmodern.energy.EtherealProcessorMenu(id,inventory,(emtmodern.energy.EtherealProcessorEntity)inventory.player.level().getBlockEntity(buffer.readBlockPos()))));
    public static final RegistryObject<net.minecraft.world.inventory.MenuType<emtmodern.energy.WandChargerMenu>> WAND_CHARGER_MENU=MENUS.register("wand_charger",()->net.minecraftforge.common.extensions.IForgeMenuType.create((id,inventory,buffer)->new emtmodern.energy.WandChargerMenu(id,inventory,(emtmodern.energy.WandChargerEntity)inventory.player.level().getBlockEntity(buffer.readBlockPos()))));
    public static final Map<String,RegistryObject<emtmodern.energy.SolarBlock>> SOLARS=new LinkedHashMap<>();
    public static final Map<String,RegistryObject<emtmodern.energy.EssentiaGeneratorBlock>> GENERATORS=new LinkedHashMap<>();
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<emtmodern.energy.EssentiaGeneratorBlockEntity>> GENERATOR_BE=BLOCK_ENTITIES.register("essentia_generator",()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(emtmodern.energy.EssentiaGeneratorBlockEntity::new,GENERATORS.values().stream().map(RegistryObject::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<emtmodern.energy.SolarBlockEntity>> SOLAR_BE=BLOCK_ENTITIES.register("solar",()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(emtmodern.energy.SolarBlockEntity::new,SOLARS.values().stream().map(RegistryObject::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));
    public static final RegistryObject<Item> SCRIBING=register("electric_scribing_tools",ElectricScribingTools::new);
    public static final RegistryObject<Item> THAUMIUM_PLATE=register("thaumium_plate",()->new Item(new Item.Properties()));
    static {
        register("portable_node",()->new BlockItem(PORTABLE_NODE.get(),new Item.Properties()));
        register("tainted_mjolnir",emtmodern.item.BrokenThorHammer::new);
        register("mjolnir",emtmodern.item.ThorHammer::new);
        register("supercharged_mjolnir",emtmodern.item.ElectricThorHammer::new);
        register("archer_spawn_egg",()->new net.minecraftforge.common.ForgeSpawnEggItem(ARCHER_ENTITY,0x99111F,939653,new Item.Properties()));
        register("ethereal_processor",()->new BlockItem(PROCESSOR.get(),new Item.Properties()));
        register("legacy_tin_nugget",()->new Item(new Item.Properties()){@Override public Component getName(ItemStack stack){return Component.translatable("item.nuggettin.name");}});
        register("amber_bricks",()->new BlockItem(AMBER_BRICKS.get(),new Item.Properties()));
        for(String material:java.util.List.of("uranium_cluster","crushed_amber","purified_amber","crushed_cinnabar","purified_cinnabar","uu_matter_drop"))register(material,emtmodern.item.LegacyMaterialItem::new);
        register("lightning_summoner",emtmodern.item.LightningSummoner::new);
        register("tainted_feather",emtmodern.item.TaintedFeather::new);
        register("inventory_charging_ring",()->new emtmodern.item.EnergyChargingRing(false));
        register("armor_charging_ring",()->new emtmodern.item.EnergyChargingRing(true));
        register("one_ring",emtmodern.item.OneRing::new);
        register("shield_focus",emtmodern.focus.ShieldFocus::new);
        register("charge_focus",emtmodern.focus.ChargeFocus::new);
        register("explosion_focus",emtmodern.focus.ExplosionFocus::new);
        register("energy_ball_focus",emtmodern.focus.EnergyBallFocus::new);
        register("christmas_focus",emtmodern.focus.ChristmasFocus::new);
        register("wand_charge_focus",emtmodern.focus.WandChargingFocus::new);
        register("wand_charger",()->new BlockItem(WAND_CHARGER.get(),new Item.Properties()));
        register("rockbreaker_drill",emtmodern.item.RockbreakerDrill::new);
        register("stream_chainsaw",emtmodern.item.StreamChainsaw::new);
        register("industrial_electric_hoe",emtmodern.item.IndustrialElectricHoe::new);
        register("electric_growth_hoe",emtmodern.item.ElectricGrowthHoe::new);
        register("shield",()->new BlockItem(SHIELD.get(),new Item.Properties()));
        register("infused_quantum_chestplate",emtmodern.item.InfusedQuantumChestplate::new);
        register("feather_wings",()->new emtmodern.item.FeatherWings("feather"));register("thaumium_wings",emtmodern.item.FeatherWings.Thaumium::new);
        register("nano_wings",()->new emtmodern.item.ElectricWings("nano"));register("quantum_wings",()->new emtmodern.item.ElectricWings("quantum"));
        for(String material:java.util.List.of("feather_mesh","glue","duct_tape","rubber_ball","cardboard","feather_wing_component","thaumium_wing_component"))register(material,()->new Item(new Item.Properties()));
        for(String kind:java.util.List.of("electric","nano","quantum","solar"))register(kind+"_goggles",()->new emtmodern.item.ElectricGoggles(kind));
        for(String kind:java.util.List.of("electric","nano","quantum"))register(kind+"_traveller_boots",()->new emtmodern.item.ElectricTravellerBoots(kind));
        for(String aspect:java.util.List.of("potentia","ignis","auram","arbor","aer")){
            String id=aspect+"_generator";var block=BLOCKS.register(id,()->new emtmodern.energy.EssentiaGeneratorBlock(aspect));GENERATORS.put(id,block);register(id,()->new BlockItem(block.get(),new Item.Properties()));
        }
        for(String kind:emtmodern.energy.SolarRule.KINDS)for(int i=0;i<3;i++){
            int tier=i;String id=(i==0?"":i==1?"double_":"triple_")+kind+"_solar";
            var block=BLOCKS.register(id,()->new emtmodern.energy.SolarBlock(kind,tier));SOLARS.put(id,block);
            register(id,()->new BlockItem(block.get(),new Item.Properties()));
        }
        for(ToolProfile profile:ToolProfile.ALL)register(profile.id(),()->ElectricTool.create(profile));
        TABS.register("main",()->CreativeModeTab.builder().title(Component.literal("Electro-Magic Tools"))
            .icon(()->new ItemStack(SCRIBING.get())).displayItems((parameters,output)->CONTENT.values().forEach(reg->{
                output.accept(reg.get());
                if(reg.get() instanceof IElectricItem){ItemStack charged=new ItemStack(reg.get());ElectricItem.manager.charge(charged,Double.MAX_VALUE,Integer.MAX_VALUE,true,false);output.accept(charged);}
            })).build());
    }
    public static RegistryObject<Item> register(String id,java.util.function.Supplier<Item> factory){
        RegistryObject<Item> item=ITEMS.register(id,factory);CONTENT.put(id,item);return item;
    }
    public ElectroMagicTools(){
        RevivalFeatures.register();IEventBus bus=FMLJavaModLoadingContext.get().getModEventBus();emtmodern.item.LegacyLootTypes.register(bus);emtmodern.item.ThaumiumPlateRecipe.register(bus);ITEMS.register(bus);TABS.register(bus);BLOCKS.register(bus);BLOCK_ENTITIES.register(bus);ENTITIES.register(bus);MENUS.register(bus);net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER,emtmodern.energy.EmtEnergyConfig.SPEC);net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER,emtmodern.energy.EmtFocusConfig.SPEC,"emt-focus-server.toml");net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER,emtmodern.item.RingConfig.SPEC,"emt-rings-server.toml");net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER,emtmodern.item.MaterialProcessing.SPEC,"emt-materials-server.toml");net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER,emtmodern.energy.ProcessorConfig.SPEC,"emt-processor-server.toml");net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER,emtmodern.item.HammerConfig.SPEC,"emt-hammers-server.toml");net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER,emtmodern.item.LegacyToolConfig.SPEC,"emt-tools-server.toml");net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER,emtmodern.item.LegacyResearchConfig.SPEC,"emt-research-server.toml");emtmodern.item.QuantumChestNetwork.register();}
}
