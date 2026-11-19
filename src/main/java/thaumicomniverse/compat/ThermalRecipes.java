package thaumicomniverse.compat;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.*;
import thaumicomniverse.RevivalFeatures;

/** Optional replacements are applied after loading so disabled defaults retain upstream recipes. */
public final class ThermalRecipes {
    private record Change(Recipe<?> original,Recipe<?> replacement) {}
    private static final Map<RecipeManager,Map<ResourceLocation,Change>> CHANGED=new WeakHashMap<>();
    public static void apply(RecipeManager manager) {
        var recipes=new LinkedHashMap<ResourceLocation,Recipe<?>>();
        manager.getRecipes().forEach(r->recipes.put(r.getId(),r));
        var previous=CHANGED.remove(manager);
        if(previous!=null)previous.forEach((id,change)->{if(recipes.get(id)==change.replacement())recipes.put(id,change.original());});
        var changed=new HashMap<ResourceLocation,Change>();
        for(String name:List.of("dragonsteel_ingot","abyssal_ingot")) {
            boolean enabled=name.startsWith("dragon")?RevivalFeatures.THERMAL_DRAGONSTEEL.get():RevivalFeatures.THERMAL_ABYSSAL.get();
            if(!enabled)continue;
            var id=ResourceLocation.parse("thermal_extra:machine/smelter/"+name);var original=recipes.get(id);
            if(original==null)continue;
            try(var stream=ThermalRecipes.class.getResourceAsStream("/data/thaumicomniverse/optional_recipes/thermal/"+name+".json")) {
                if(stream==null)throw new IllegalStateException("Missing optional Thermal recipe: "+name);
                var json=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
                var replacement=RecipeManager.fromJson(id,json);recipes.put(id,replacement);changed.put(id,new Change(original,replacement));
            } catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
        }
        if(previous!=null||!changed.isEmpty())manager.replaceRecipes(recipes.values());
        if(!changed.isEmpty())CHANGED.put(manager,changed);
    }
    private ThermalRecipes() {}
}
