package tc4revival.aspect.recipe;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import tc4revival.TC4Revival;
import tc4revival.aspect.TCAspects;
import tc4revival.aspect.item.PotionAspects;

/** Loads semantic calculation inputs. This does not register gameplay crafting recipes or legacy items. */
public  class RecipeAspects {
    private static volatile RecipeAspectData data;
    private record Cache(RecipeAspectData data,tc4revival.aspect.AspectCatalog catalog,tc4revival.aspect.item.PotionAspectData potions,RecipeAspectEngine template,
            Map<RecipeAspectData.Key,java.util.Optional<tc4revival.aspect.AspectAmounts>> results) {}
    private static Cache cached;
    private RecipeAspects() {}
    public static void addReloadListener(AddReloadListenerEvent event) { event.addListener(new Reload()); }
    public static synchronized void stopped(ServerStoppedEvent event) { data = null;cached=null;RuntimeCraftingAspects.invalidate(); }
    public static RecipeAspectData data() {
        var current = data;
        if (current == null) throw new IllegalStateException("Recipe aspect data has not loaded");
        return current;
    }
    public static RecipeAspectEngine newEngine() { return new RecipeAspectEngine(data(), TCAspects.catalog(), Map.of(), PotionAspects::decorate); }
    public static synchronized tc4revival.aspect.AspectAmounts object(RecipeAspectData.Key key) {
        var data=data();var catalog=TCAspects.catalog();var potions=PotionAspects.data();
        if(cached==null||cached.data()!=data||cached.catalog()!=catalog||cached.potions()!=potions)
            cached=new Cache(data,catalog,potions,newEngine(),new java.util.LinkedHashMap<>());
        return cached.results().computeIfAbsent(key,k->java.util.Optional.ofNullable(cached.template().fork().object(k)))
                .map(tc4revival.aspect.AspectAmounts::copy).orElse(null);
    }

    private static  class Reload extends SimpleJsonResourceReloadListener {
        Reload() { super(new Gson(), "recipe_aspect_inputs"); }
        @Override protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            var id = new ResourceLocation(TC4Revival.MOD_ID, "legacy");
            if (!files.containsKey(id)) throw new IllegalArgumentException("Expected one complete legacy recipe aspect definition");
            var inputs=new java.util.ArrayList<RecipeAspectData>();
            inputs.add(RecipeAspectJson.decode(files.get(id)));
            files.entrySet().stream().filter(e->!e.getKey().equals(id)).sorted(Map.Entry.comparingByKey())
                    .forEach(e->inputs.add(RecipeAspectJson.decode(e.getValue())));
            var candidate = RuntimeCraftingAspects.withoutPresetCrafting(RecipeAspectInputs.merge(inputs));
            new RecipeAspectEngine(candidate, TCAspects.catalog());
            data = candidate;
            RuntimeCraftingAspects.invalidate();
            tc4revival.aspect.sync.ServerAspectData.capture("crafting_aspect_inputs",Map.of());
            tc4revival.aspect.sync.ServerAspectData.capture("recipe_aspect_inputs",files);
            TC4Revival.LOGGER.info("Loaded recipe aspect inputs: {} semantic items, {} registrations, {} calculation recipes",
                    candidate.items().size(), candidate.registrations().size(), candidate.recipes().size());
        }
    }
}
