package thaumicomniverse.compat.create.airship;

import com.simibubi.create.api.contraption.*;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.actors.trainControls.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import thaumicomniverse.ThaumicOmniverse;
import thaumicomniverse.compat.create.ArcaneCreateContent;

public final class AirshipContent {
    private static BlockBehaviour.Properties metal(){return BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(3,6);}
    public static final RegistryObject<AirshipBlocks.NodeBlock> STATION=ArcaneCreateContent.BLOCKS.register("airship_station",()->new AirshipBlocks.NodeBlock(true));
    public static final RegistryObject<AirshipBlocks.NodeBlock> WAYPOINT=ArcaneCreateContent.BLOCKS.register("airship_waypoint",()->new AirshipBlocks.NodeBlock(false));
    public static final RegistryObject<Block> CASING=ArcaneCreateContent.BLOCKS.register("airship_casing",AirshipBlocks.CasingBlock::new);
    public static final RegistryObject<Block> CORE=ArcaneCreateContent.BLOCKS.register("airship_core",AirshipBlocks.CoreBlock::new);
    public static final RegistryObject<Item> SCHEDULE=ThaumicOmniverse.ITEMS.register("airship_schedule",AirshipScheduleItem::new);
    public static final RegistryObject<ControlsBlock> CONTROLS=ArcaneCreateContent.BLOCKS.register("airship_controls",()->new ControlsBlock(metal()));
    public static final RegistryObject<Block> FUEL=ArcaneCreateContent.BLOCKS.register("airship_fuel_interface",()->new Block(metal()));
    public static final RegistryObject<AirshipSignal> SIGNAL=ArcaneCreateContent.BLOCKS.register("airship_signal",AirshipSignal::new);
    public static final RegistryObject<BlockEntityType<AirshipBlocks.NodeTile>> NODE_TILE=ArcaneCreateContent.TILES.register("airship_node",()->BlockEntityType.Builder.of(AirshipBlocks.NodeTile::new,STATION.get(),WAYPOINT.get()).build(null));
    public static final RegistryObject<BlockEntityType<AirshipSignal.Tile>> SIGNAL_TILE=ArcaneCreateContent.TILES.register("airship_signal",()->BlockEntityType.Builder.of(AirshipSignal.Tile::new,SIGNAL.get()).build(null));
    public static final RegistryObject<ContraptionType> STRUCTURE=ArcaneCreateContent.STRUCTURES.register("airship",()->new ContraptionType(AirshipStructure::new));
    public static final RegistryObject<EntityType<AirshipEntity>> ENTITY=ArcaneCreateContent.ENTITIES.register("airship",()->EntityType.Builder.<AirshipEntity>of(AirshipEntity::new,MobCategory.MISC).sized(1,1).clientTrackingRange(16).updateInterval(1).noSave().fireImmune().build("thaumicomniverse:airship"));
    public static void register(IEventBus bus){
        for(var block:java.util.List.of(STATION,WAYPOINT,CASING,CORE,CONTROLS,FUEL,SIGNAL))ThaumicOmniverse.ITEMS.register(block.getId().getPath(),()->new BlockItem(block.get(),new Item.Properties()));
        AirshipNetwork.register();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(AirshipManager::tick);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(AirshipManager::save);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(AirshipManager::stopped);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(AirshipPassengers::logout);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(AirshipBlocks::selectAnchor);
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent e)->e.enqueueWork(AirshipContent::setup));
        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->AirshipClient.register(bus));
    }
    private static void setup(){
        MovingInteractionBehaviour.REGISTRY.register(CONTROLS.get(),new ControlsInteractionBehaviour());
        com.simibubi.create.api.behaviour.movement.MovementBehaviour.REGISTRY.register(CONTROLS.get(),new AirshipControls());
        BlockMovementChecks.registerAttachedCheck((state,level,pos,face)->state.is(CORE.get())&&face.getAxis().isHorizontal()?BlockMovementChecks.CheckResult.SUCCESS:BlockMovementChecks.CheckResult.PASS);
        BlockMovementChecks.registerMovementAllowedCheck((state,level,pos)->state.getBlock() instanceof AirshipBlocks.NodeBlock||state.is(SIGNAL.get())?BlockMovementChecks.CheckResult.FAIL:BlockMovementChecks.CheckResult.PASS);
    }
}
