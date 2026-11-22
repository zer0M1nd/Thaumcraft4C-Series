package thaumicomniverse.compat.create;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;
import thaumicomniverse.ThaumicOmniverse;

public class ArcaneCreateContent {
    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,ThaumicOmniverse.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> TILES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,ThaumicOmniverse.MOD_ID);
    public static final DeferredRegister<com.simibubi.create.api.contraption.ContraptionType> STRUCTURES=DeferredRegister.create(com.simibubi.create.api.registry.CreateRegistries.CONTRAPTION_TYPE,ThaumicOmniverse.MOD_ID);
    public static final RegistryObject<com.simibubi.create.api.contraption.ContraptionType> STRUCTURE=STRUCTURES.register("arcane_mobile",()->new com.simibubi.create.api.contraption.ContraptionType(ArcaneMovingStructure::new));
    public static final RegistryObject<com.simibubi.create.api.contraption.ContraptionType> WAND_STRUCTURE=STRUCTURES.register("wand_mobile",()->new com.simibubi.create.api.contraption.ContraptionType(WandMovingStructure::new));
    public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,ThaumicOmniverse.MOD_ID);
    public static final RegistryObject<net.minecraft.world.entity.EntityType<ArcaneMobileEntity>> MOBILE=ENTITIES.register("arcane_mobile",()->net.minecraft.world.entity.EntityType.Builder.<ArcaneMobileEntity>of(ArcaneMobileEntity::new,net.minecraft.world.entity.MobCategory.MISC).sized(1,1).clientTrackingRange(16).updateInterval(1).build("thaumicomniverse:arcane_mobile"));
    public static final RegistryObject<net.minecraft.world.entity.EntityType<WandContraptionEntity>> WAND_MOBILE=ENTITIES.register("wand_mobile",()->net.minecraft.world.entity.EntityType.Builder.<WandContraptionEntity>of(WandContraptionEntity::new,net.minecraft.world.entity.MobCategory.MISC).sized(1,1).clientTrackingRange(16).updateInterval(1).build("thaumicomniverse:wand_mobile"));
    public static final RegistryObject<KineticFocusItem> KINETIC_FOCUS=ThaumicOmniverse.ITEMS.register("focus_kinetic",KineticFocusItem::new);
    public static final RegistryObject<RelocationFocusItem> RELOCATION_FOCUS=ThaumicOmniverse.ITEMS.register("focus_relocation",RelocationFocusItem::new);
    public static final RegistryObject<Block> CASING=BLOCKS.register("thaumic_casing",()->new Block(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).strength(3,6).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> MAGNETIC_CASING=BLOCKS.register("thaumic_magnetic_casing",()->new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(3,6).requiresCorrectToolForDrops()));
    public static final RegistryObject<Item> THAUMIUM_SHEET=ThaumicOmniverse.THAUMIUM_SHEET;
    public static final RegistryObject<Item> VOID_SHEET=ThaumicOmniverse.VOID_SHEET;
    public static final RegistryObject<ThaumicCrushingWheel> CRUSHING_WHEEL=BLOCKS.register("thaumic_crushing_wheel",ThaumicCrushingWheel::new);
    public static final RegistryObject<BlockEntityType<ThaumicCrushingWheel.WheelTile>> CRUSHING_WHEEL_TILE=TILES.register("thaumic_crushing_wheel",()->BlockEntityType.Builder.of(ThaumicCrushingWheel.WheelTile::new,CRUSHING_WHEEL.get()).build(null));
    public static final RegistryObject<ThaumicCrushingWheel.Controller> CRUSHING_CONTROLLER=BLOCKS.register("thaumic_crushing_controller",ThaumicCrushingWheel.Controller::new);
    public static final RegistryObject<BlockEntityType<ThaumicCrushingWheel.ControllerTile>> CRUSHING_CONTROLLER_TILE=TILES.register("thaumic_crushing_controller",()->BlockEntityType.Builder.of(ThaumicCrushingWheel.ControllerTile::new,CRUSHING_CONTROLLER.get()).build(null));
    public static final RegistryObject<Item> INCOMPLETE_CRUSHING_WHEEL=ThaumicOmniverse.ITEMS.register("incomplete_thaumic_crushing_wheel",()->new com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem(new Item.Properties()));
    public static final RegistryObject<ArcaneMotor> MOTOR=BLOCKS.register("arcane_motor",ArcaneMotor::new);
    public static final RegistryObject<BlockEntityType<ArcaneMotor.Tile>> MOTOR_TILE=TILES.register("arcane_motor",()->BlockEntityType.Builder.of(ArcaneMotor.Tile::new,MOTOR.get()).build(null));
    public static final RegistryObject<ThaumicCrank> CRANK=BLOCKS.register("thaumic_hand_crank",ThaumicCrank::new);
    public static final RegistryObject<BlockEntityType<ThaumicCrank.Tile>> CRANK_TILE=TILES.register("thaumic_hand_crank",()->BlockEntityType.Builder.of(ThaumicCrank.Tile::new,CRANK.get()).build(null));
    public static final RegistryObject<ArcaneBurner> BURNER=BLOCKS.register("arcane_blaze_burner",ArcaneBurner::new);
    public static final RegistryObject<BlockEntityType<ArcaneBurner.Tile>> BURNER_TILE=TILES.register("arcane_blaze_burner",()->BlockEntityType.Builder.of(ArcaneBurner.Tile::new,BURNER.get()).build(null));
    public static final RegistryObject<Item> EMPTY_BURNER=ThaumicOmniverse.ITEMS.register("empty_arcane_blaze_burner",()->new ArcaneBurner.BurnerItem(false));
    public static final RegistryObject<Item> FILLED_BURNER=ThaumicOmniverse.ITEMS.register("arcane_blaze_burner",()->new ArcaneBurner.BurnerItem(true));
    public static final RegistryObject<Item> SANDPAPER=ThaumicOmniverse.ITEMS.register("void_sandpaper",VoidSandpaper::new);
    public static final RegistryObject<PortableEssentiaInterface> INTERFACE=BLOCKS.register("portable_essentia_interface",PortableEssentiaInterface::new);
    public static final RegistryObject<BlockEntityType<PortableEssentiaInterface.Tile>> INTERFACE_TILE=TILES.register("portable_essentia_interface",()->BlockEntityType.Builder.of(PortableEssentiaInterface.Tile::new,INTERFACE.get()).build(null));
    public static final RegistryObject<WandCharger> CHARGER=BLOCKS.register("wand_charger",WandCharger::new);
    public static final RegistryObject<BlockEntityType<WandCharger.Tile>> CHARGER_TILE=TILES.register("wand_charger",()->BlockEntityType.Builder.of(WandCharger.Tile::new,CHARGER.get()).build(null));
    public static final RegistryObject<Item> MAGIC_HAND=ThaumicOmniverse.ITEMS.register("magic_hand",MagicHand::new);
    public static final RegistryObject<VoidBacktank.Block> BACKTANK=BLOCKS.register("void_backtank",VoidBacktank.Block::new);
    public static final RegistryObject<VoidBacktank> BACKTANK_ITEM=ThaumicOmniverse.ITEMS.register("void_backtank",VoidBacktank::new);
    public static final RegistryObject<com.simibubi.create.content.equipment.armor.BacktankItem.BacktankBlockItem> BACKTANK_PLACEABLE=ThaumicOmniverse.ITEMS.register("void_backtank_placeable",()->new com.simibubi.create.content.equipment.armor.BacktankItem.BacktankBlockItem(BACKTANK.get(),BACKTANK_ITEM::get,new Item.Properties().stacksTo(1)));
    public static final RegistryObject<BlockEntityType<VoidBacktank.Tile>> BACKTANK_TILE=TILES.register("void_backtank",()->BlockEntityType.Builder.of(VoidBacktank.Tile::new,BACKTANK.get()).build(null));
    public static final RegistryObject<EssentiaSwitch> SWITCH=BLOCKS.register("essentia_stockpile_switch",EssentiaSwitch::new);
    public static final RegistryObject<BlockEntityType<EssentiaSwitch.Tile>> SWITCH_TILE=TILES.register("essentia_stockpile_switch",()->BlockEntityType.Builder.of(EssentiaSwitch.Tile::new,SWITCH.get()).build(null));
    public static final RegistryObject<SuspensionCoupler> COUPLER=BLOCKS.register("arcane_suspension_coupler",SuspensionCoupler::new);
    public static final RegistryObject<ArcaneMagnet> MAGNET=BLOCKS.register("arcane_kinetic_magnet",ArcaneMagnet::new);
    public static final RegistryObject<BlockEntityType<ArcaneMagnet.Tile>> MAGNET_TILE=TILES.register("arcane_kinetic_magnet",()->BlockEntityType.Builder.of(ArcaneMagnet.Tile::new,MAGNET.get()).build(null));
    public static final RegistryObject<FloatingDepot> DEPOT=BLOCKS.register("arcane_floating_depot",FloatingDepot::new);
    public static final RegistryObject<BlockEntityType<FloatingDepot.Tile>> DEPOT_TILE=TILES.register("arcane_floating_depot",()->BlockEntityType.Builder.of(FloatingDepot.Tile::new,DEPOT.get()).build(null));
    public static final RegistryObject<EssentiaSpout> SPOUT=BLOCKS.register("essentia_spout",EssentiaSpout::new);
    public static final RegistryObject<BlockEntityType<EssentiaSpout.Tile>> SPOUT_TILE=TILES.register("essentia_spout",()->BlockEntityType.Builder.of(EssentiaSpout.Tile::new,SPOUT.get()).build(null));
    public static final RegistryObject<Item> INCOMPLETE_INFUSION=ThaumicOmniverse.ITEMS.register("incomplete_essentia_infusion",()->new com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem(new Item.Properties()));
    public static void register(IEventBus bus){
        for(var entry:java.util.List.of(CASING,MAGNETIC_CASING,MOTOR,CRANK,INTERFACE,CHARGER,SWITCH,COUPLER,MAGNET,SPOUT,CRUSHING_WHEEL))ThaumicOmniverse.ITEMS.register(entry.getId().getPath(),()->new BlockItem(entry.get(),new Item.Properties()));
        ThaumicOmniverse.ITEMS.register("arcane_floating_depot",()->new FloatingDepot.PlacementItem(DEPOT.get()));
        BLOCKS.register(bus);TILES.register(bus);STRUCTURES.register(bus);ENTITIES.register(bus);
    }
}
