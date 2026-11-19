package thaumicfutures;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.*;

/** Remap only to registrations actually supplied by Futures (including optional integrations). */
public final class LegacyMappings {
    public static void missing(MissingMappingsEvent event){
        remap(event,ForgeRegistries.ITEMS);remap(event,ForgeRegistries.BLOCKS);
        remap(event,ForgeRegistries.BLOCK_ENTITY_TYPES);remap(event,ForgeRegistries.ENTITY_TYPES);
        remap(event,ForgeRegistries.MENU_TYPES);remap(event,ForgeRegistries.RECIPE_SERIALIZERS);
        remap(event,ForgeRegistries.FEATURES);
    }
    private static <T> void remap(MissingMappingsEvent event,IForgeRegistry<T> registry){
        for(var mapping:event.getMappings(registry.getRegistryKey(),"thaumicomniverse")){
            var target=ResourceLocation.fromNamespaceAndPath("thaumicfutures",mapping.getKey().getPath());
            if(registry.containsKey(target))mapping.remap(registry.getValue(target));
        }
    }
    private LegacyMappings(){}
}
