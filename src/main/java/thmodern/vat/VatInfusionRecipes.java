package thmodern.vat;

import java.util.*;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thmodern.Horizons;

@Mod.EventBusSubscriber(modid=Horizons.ID)
public final class VatInfusionRecipes extends SimpleJsonResourceReloadListener {
    private static volatile List<VatInfusionRecipe> recipes=List.of();
    private VatInfusionRecipes(){super(new Gson(),"vat_infusions");}
    public static List<VatInfusionRecipe> all(){return recipes;}
    @SubscribeEvent public static void reload(AddReloadListenerEvent event){event.addListener(new VatInfusionRecipes());}
    @Override protected void apply(Map<ResourceLocation,JsonElement> values,ResourceManager resources,ProfilerFiller profiler){
        var next=new ArrayList<VatInfusionRecipe>();values.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry->{
            try{next.add(new VatInfusionRecipe(entry.getKey(),entry.getValue().getAsJsonObject()));}
            catch(RuntimeException invalid){throw new JsonParseException("Invalid vat infusion "+entry.getKey(),invalid);}
        });recipes=List.copyOf(next);
    }
}
