package thaumicomniverse;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import thaumicomniverse.compat.FeatureRecipeMutation;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="thaumicomniverse")
public final class FeatureRecipeLifecycle {
    public static void apply(RecipeManager manager, RegistryAccess registries) {
        if(net.minecraftforge.fml.ModList.get().isLoaded("alchemistry"))thaumicomniverse.compat.ChemistryRecipes.apply(manager);
        if(net.minecraftforge.fml.ModList.get().isLoaded("industrialforegoing"))thaumicomniverse.compat.IndustrialLaserRecipes.restoreOreOutputs(manager);
        thaumicomniverse.research.UniversalEnchantments.recipes(manager);
        thaumicomniverse.compat.UniversalAlchemy.recipes(manager,registries);
        if (!RevivalFeatures.CONFIG.loaded()) return;
        if(thaumicomniverse.compat.AvaritiaCompat.loaded())thaumicomniverse.compat.AvaritiaRecipes.applyCatalysts(manager,registries);
        if(net.minecraftforge.fml.ModList.get().isLoaded("thermal_extra"))thaumicomniverse.compat.ThermalRecipes.apply(manager);
        if(net.minecraftforge.fml.ModList.get().isLoaded("gtceu")){
            thaumicomniverse.compat.gregtech.ArcaneOreProcessing.apply(manager);
            thaumicomniverse.compat.gregtech.EssentiaFuelValues.apply(manager);
        }
        thaumicomniverse.compat.AtmRecipes.apply(manager,registries);
        for(var recipe:manager.getRecipes()) if(recipe instanceof FeatureRecipeMutation mutable) mutable.omniverse$applyFeatures();
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void starting(ServerAboutToStartEvent event) { apply(event.getServer().getRecipeManager(),event.getServer().registryAccess()); }
    @SubscribeEvent(priority=EventPriority.HIGH)
    public static void started(net.minecraftforge.event.server.ServerStartedEvent event) {
        thaumicomniverse.compat.UniversalAlchemy.completeOreRecipes(event.getServer().overworld());
        if(net.minecraftforge.fml.ModList.get().isLoaded("create"))thaumicomniverse.compat.create.ArcaneCreateRecipes.apply(event.getServer().getRecipeManager(),event.getServer().registryAccess());
    }
    // Dynamic recipes must exist before HIGH-priority aspect inference and normal sync.
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void sync(OnDatapackSyncEvent event) {
        var server=event.getPlayerList().getServer();apply(server.getRecipeManager(),server.registryAccess());
        thaumicomniverse.compat.UniversalAlchemy.completeOreRecipes(server.overworld());
        if(net.minecraftforge.fml.ModList.get().isLoaded("create"))thaumicomniverse.compat.create.ArcaneCreateRecipes.apply(server.getRecipeManager(),server.registryAccess());
    }
}
