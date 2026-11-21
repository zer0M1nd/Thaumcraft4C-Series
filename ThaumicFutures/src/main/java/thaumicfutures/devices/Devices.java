package thaumicfutures.devices;

import net.minecraft.world.entity.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;
import thaumicfutures.LegacyContent;

public final class Devices {
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,"thaumicfutures");
    private static final DeferredRegister<BlockEntityType<?>> TILES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,"thaumicfutures");
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,"thaumicfutures");
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,"thaumicfutures");
    public static final RegistryObject<DioptraBlock> DIOPTRA=BLOCKS.register("dioptra",DioptraBlock::new);
    public static final RegistryObject<PatternCrafterBlock> CRAFTER=BLOCKS.register("arcane_pattern_crafter",PatternCrafterBlock::new);
    public static final RegistryObject<BlockEntityType<Dioptra>> DIOPTRA_TILE=TILES.register("dioptra",()->BlockEntityType.Builder.of(Dioptra::new,DIOPTRA.get()).build(null));
    public static final RegistryObject<BlockEntityType<PatternCrafter>> CRAFTER_TILE=TILES.register("arcane_pattern_crafter",()->BlockEntityType.Builder.of(PatternCrafter::new,CRAFTER.get()).build(null));
    public static final RegistryObject<EntityType<AutomatedCrossbow>> CROSSBOW=ENTITIES.register("automated_crossbow",()->EntityType.Builder.<AutomatedCrossbow>of(AutomatedCrossbow::new,MobCategory.MISC).sized(.95f,1.25f).clientTrackingRange(10).updateInterval(1).build("thaumicfutures:automated_crossbow"));
    public static final RegistryObject<CrossbowPlacer> CROSSBOW_ITEM=LegacyContent.ITEMS.register("automated_crossbow",CrossbowPlacer::new);
    public static final RegistryObject<MenuType<CrossbowMenu>> CROSSBOW_MENU=MENUS.register("automated_crossbow",()->IForgeMenuType.create(CrossbowMenu::new));
    public static void register(IEventBus bus) {
        LegacyContent.ITEMS.register("dioptra",()->new BlockItem(DIOPTRA.get(),new Item.Properties()));
        LegacyContent.ITEMS.register("arcane_pattern_crafter",()->new BlockItem(CRAFTER.get(),new Item.Properties()));
        BLOCKS.register(bus);TILES.register(bus);ENTITIES.register(bus);MENUS.register(bus);
        bus.addListener((EntityAttributeCreationEvent event)->event.put(CROSSBOW.get(),AutomatedCrossbow.attributes().build()));
        thaumcraft.api.resources.ExternalResourceConverters.register(net.minecraft.resources.ResourceLocation.parse("thaumicfutures:tc5_devices"),DeviceResources::convert);
    }
    private Devices() {}
}
