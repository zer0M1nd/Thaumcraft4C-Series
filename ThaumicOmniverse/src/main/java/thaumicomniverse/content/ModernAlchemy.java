package thaumicomniverse.content;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;
import tc4revival.content.EssentiaTubeFlow;
import thaumicomniverse.ThaumicOmniverse;

/** Omniverse owns the active transport system; TC4 only supplies the extensible suction pipes. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="thaumicomniverse")
public final class ModernAlchemy {
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,"thaumicomniverse");
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,"thaumicomniverse");
    public static final RegistryObject<ConvenientTubeBlock> CONVENIENT_TUBE=BLOCKS.register("convenient_essentia_tube",()->new ConvenientTubeBlock(EssentiaTubeFlow.Kind.NORMAL));
    public static final RegistryObject<ConvenientTubeBlock> CONVENIENT_FILTER=BLOCKS.register("convenient_essentia_filter",()->new ConvenientTubeBlock(EssentiaTubeFlow.Kind.FILTER));
    public static final RegistryObject<ConvenientTubeBlock> CONVENIENT_RESTRICTOR=BLOCKS.register("convenient_essentia_restrictor",()->new ConvenientTubeBlock(EssentiaTubeFlow.Kind.RESTRICTED));
    public static final RegistryObject<BlockEntityType<ConvenientTubeBlockEntity>> CONVENIENT_TUBE_ENTITY=ENTITIES.register("convenient_essentia_tube",()->BlockEntityType.Builder.of(ConvenientTubeBlockEntity::new,CONVENIENT_TUBE.get(),CONVENIENT_FILTER.get(),CONVENIENT_RESTRICTOR.get()).build(null));
    public static final RegistryObject<EssentiaServoItem> ESSENTIA_SERVO=ThaumicOmniverse.ITEMS.register("essentia_servo",EssentiaServoItem::new);
    public static void register(IEventBus bus) {
        for(var block:java.util.List.of(CONVENIENT_TUBE,CONVENIENT_FILTER,CONVENIENT_RESTRICTOR))
            ThaumicOmniverse.ITEMS.register(block.getId().getPath(),()->new ConvenientTubeItem(block.get()));
        BLOCKS.register(bus); ENTITIES.register(bus);
    }
    /** Forge remaps saved items and placed blocks. Legacy block entity NBT also needs LegacyConvenientTubeMixin. */
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void missingMappings(MissingMappingsEvent event) {
        for(var mapping:event.getMappings(ForgeRegistries.Keys.ITEMS,"thaumcraft")) {
            if(!moved(mapping.getKey().getPath()))continue;
            var item=ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("thaumicomniverse",mapping.getKey().getPath()));
            if(item!=null)mapping.remap(item);
        }
        for(var mapping:event.getMappings(ForgeRegistries.Keys.BLOCKS,"thaumcraft")) {
            if(!mapping.getKey().getPath().startsWith("convenient_essentia_")||!moved(mapping.getKey().getPath()))continue;
            var block=ForgeRegistries.BLOCKS.getValue(new net.minecraft.resources.ResourceLocation("thaumicomniverse",mapping.getKey().getPath()));
            if(block!=null)mapping.remap(block);
        }
        for(var mapping:event.getMappings(ForgeRegistries.Keys.BLOCK_ENTITY_TYPES,"thaumcraft"))
            if(mapping.getKey().getPath().equals("convenient_essentia_tube"))mapping.remap(CONVENIENT_TUBE_ENTITY.get());
    }
    public static boolean moved(String id) {
        return java.util.Set.of("essentia_servo","convenient_essentia_tube","convenient_essentia_filter","convenient_essentia_restrictor").contains(id);
    }
    private ModernAlchemy() {}
}
