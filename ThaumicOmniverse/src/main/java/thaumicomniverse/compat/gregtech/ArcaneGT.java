package thaumicomniverse.compat.gregtech;

import com.gregtechceu.gtceu.api.addon.*;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconType;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraftforge.eventbus.api.IEventBus;
import java.util.function.Consumer;

@GTAddon
public final class ArcaneGT implements IGTAddon {
    public static final GTRegistrate REGISTRATE=GTRegistrate.create("thaumicomniverse");
    public static TagPrefix CLUSTER;
    public static final MaterialIconType CLUSTER_ICON=new MaterialIconType("thaumicCluster");
    public static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("thaumicomniverse",path);}
    public static void register(IEventBus bus){
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(()->
            thaumcraft.api.transport.EssentiaConnections.register("thaumicomniverse:gregtech",0,context->{
                if(!(context.neighbor() instanceof com.gregtechceu.gtceu.api.machine.IMachineBlockEntity machine))return thaumcraft.api.transport.EssentiaConnections.Decision.PASS;
                return machine.getMetaMachine() instanceof EssentiaHatch?thaumcraft.api.transport.EssentiaConnections.Decision.CONNECT:thaumcraft.api.transport.EssentiaConnections.Decision.DISCONNECT;
            })));

        ArcaneMaterials.register(bus);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(ArcaneMaterials::missingMappings);
        bus.addGenericListener(com.gregtechceu.gtceu.api.recipe.GTRecipeType.class,
                (com.gregtechceu.gtceu.api.GTCEuAPI.RegisterEvent<ResourceLocation,com.gregtechceu.gtceu.api.recipe.GTRecipeType> event)->ArcaneMachines.registerRecipeType());
        bus.addGenericListener(com.gregtechceu.gtceu.api.machine.MachineDefinition.class,
                (com.gregtechceu.gtceu.api.GTCEuAPI.RegisterEvent<ResourceLocation,com.gregtechceu.gtceu.api.machine.MachineDefinition> event)->ArcaneMachines.registerMachines());
        REGISTRATE.registerEventListeners(bus);
    }
    @Override public GTRegistrate getRegistrate(){return REGISTRATE;}
    @Override public String addonModId(){return "thaumicomniverse";}
    @Override public void registerTagPrefixes(){
        CLUSTER=new TagPrefix("thaumicCluster").idPattern("%s_ore_cluster").defaultTagPath("clusters/%s").unformattedTagPath("clusters")
                .langValue("%s Cluster").materialIconType(CLUSTER_ICON).unificationEnabled(true).generateItem(true)
                .generationCondition(material->material.hasProperty(PropertyKey.ORE));
        ArcaneMaterials.reuseClusters();
    }
    @Override public void initializeAddon(){}
    /** Use the registered GT form even when a TC or addon cluster shares the Forge tag. */
    public static net.minecraft.world.item.ItemStack cluster(String name) {
        for(var material:com.gregtechceu.gtceu.api.GTCEuAPI.materialManager.getRegisteredMaterials())
            if(material.getName().equals(name)&&material.hasProperty(PropertyKey.ORE))
                return com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper.get(CLUSTER,material);
        return net.minecraft.world.item.ItemStack.EMPTY;
    }
    public static Integer materialColor(String name) {
        for(var material:com.gregtechceu.gtceu.api.GTCEuAPI.materialManager.getRegisteredMaterials())
            if(material.getName().equals(name)||material.getResourceLocation().toString().equals(name))return material.getMaterialRGB();
        return null;
    }
    @Override public void registerRecipeCapabilities(){
        GTRegistries.RECIPE_CAPABILITIES.register(EssentiaFuelCapability.CAP.name,EssentiaFuelCapability.CAP);
        EssentiaFuelCapability.registerLookup();
    }
    @Override public void addRecipes(Consumer<FinishedRecipe> provider){ArcaneGTRecipes.generate(provider);}
    @Override public void registerOreVeins(){ArcaneOreVein.register();}
}
