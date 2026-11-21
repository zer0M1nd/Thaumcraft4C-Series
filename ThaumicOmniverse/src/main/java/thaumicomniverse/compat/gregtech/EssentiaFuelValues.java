package thaumicomniverse.compat.gregtech;
import java.util.Map;
/** Relative energy from GTNH GoodGenerator essentia.json; values here are EU per unit before the 2x scaling. */
public final class EssentiaFuelValues {
    public static final Map<String,Integer> ENERGY=thaumicomniverse.expansion.EssentiaFuelCatalog.ENERGY;
    public static int ticks(int energy){return ticks(energy,thaumicomniverse.RevivalFeatures.CONFIG.loaded()?thaumicomniverse.RevivalFeatures.ESSENTIA_FUEL_DURATION.get():4);}
    /** GT may generate recipes before a world's SERVER config is loaded. Keep its registered recipe identities. */
    public static void apply(net.minecraft.world.item.crafting.RecipeManager manager){
        for(var entry:manager.getRecipes()){
            if(!(entry instanceof com.gregtechceu.gtceu.api.recipe.GTRecipe recipe)||!entry.getId().getNamespace().equals("thaumicomniverse"))continue;
            String prefix="essentia_fuels/gt/fuels/",path=entry.getId().getPath();
            if(!path.startsWith(prefix))continue;
            var energy=ENERGY.get(path.substring(prefix.length()));
            if(energy!=null)recipe.duration=ticks(energy);
        }
    }
    public static int ticks(int energy,double multiplier){return thaumicomniverse.expansion.EssentiaFuelCatalog.generatorTicks(energy,multiplier);}
    private EssentiaFuelValues(){}
}
