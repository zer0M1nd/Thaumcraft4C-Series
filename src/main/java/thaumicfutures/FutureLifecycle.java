package thaumicfutures;
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="thaumicfutures")
public final class FutureLifecycle {
    private static void recipes(net.minecraft.world.item.crafting.RecipeManager recipes){for(var recipe:recipes.getRecipes())if(recipe instanceof FeatureRecipeMutation mutable)mutable.omniverse$applyFeatures();}
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void start(net.minecraftforge.event.server.ServerAboutToStartEvent event){recipes(event.getServer().getRecipeManager());}
    @net.minecraftforge.eventbus.api.SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.HIGHEST) public static void sync(net.minecraftforge.event.OnDatapackSyncEvent event){recipes(event.getPlayerList().getServer().getRecipeManager());}
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void missing(net.minecraftforge.registries.MissingMappingsEvent event){
        LegacyMappings.missing(event);
        for(var mapping:event.getMappings(net.minecraftforge.registries.ForgeRegistries.Keys.ITEMS,"thaumcraft"))if(mapping.getKey().getPath().equals("alchemical_brass_ingot"))mapping.remap(LegacyContent.BRASS.get());
        if(net.minecraftforge.fml.ModList.get().isLoaded("gtceu"))LegacyBrassMaterials.missing(event);
    }
}
