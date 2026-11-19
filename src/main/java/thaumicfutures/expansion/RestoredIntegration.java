package thaumicfutures.expansion;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import thaumicfutures.FutureFeatures;

/** Hide the superseded furnace without changing its registry or disabling placed blocks. */
@Mod.EventBusSubscriber(modid="thaumicfutures",bus=Mod.EventBusSubscriber.Bus.MOD)
public final class RestoredIntegration {
    public static final ResourceLocation OLD_FURNACE=ResourceLocation.parse("thaumicbases:advanced_alchemical_furnace");
    public static boolean hideOldFurnace(){return FutureFeatures.Group.RESTORATION.enabled();}
    public static void register(){
        PreferredMaterialIngredient.register();
        thaumcraft.api.research.ResearchCategories.registerEnablementFilter(ResourceLocation.parse("thaumicfutures:restored_smelter"),
                key->!key.equals("TB.AdvAlc")||!hideOldFurnace());
        thaumcraft.api.config.FeatureRecipes.register(ResourceLocation.parse("thaumicfutures:replaced_smelter"),
                id->id.toString().equals("thaumicbases:machines/advfurnacerecipe"),()->!hideOldFurnace());
    }
    @SubscribeEvent public static void creative(net.minecraftforge.event.BuildCreativeModeTabContentsEvent event){
        if(!hideOldFurnace())return;
        var iterator=event.getEntries().iterator();
        while(iterator.hasNext())if(OLD_FURNACE.equals(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(iterator.next().getKey().getItem())))iterator.remove();
    }
    private RestoredIntegration(){}
}
