package tc4revival.research;

import com.google.gson.*;
import java.util.*;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import tc4revival.aspect.TCAspects;

/** One atomic catalog/page snapshot, shared by gameplay and the synchronized book. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="thaumcraft")
public class ResearchDefinitions extends SimplePreparableReloadListener<JsonObject> {
    public record Snapshot(ResearchCatalog catalog, ResearchPages pages, ResearchPageRecipes recipes, JsonObject json) {}
    private static volatile Snapshot server, client;
    private static JsonObject rawServer;
    private static boolean configurationReady;
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void tagsUpdated(net.minecraftforge.event.TagsUpdatedEvent event) {
        // Tags bind after reload listeners. Rebuild optional-metal gates before the new snapshot is sent.
        if (event.getUpdateCause() == net.minecraftforge.event.TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD
                && configurationReady && rawServer != null) publish(rawServer);
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void starting(net.minecraftforge.event.server.ServerAboutToStartEvent event) {
        configurationReady=true;
        if(rawServer!=null)publish(rawServer);
    }
    private static JsonObject tutorials(JsonObject json, boolean enabled) {
        return TutorialResearch.materialize(json, enabled, row -> net.minecraftforge.common.crafting.CraftingHelper
                .processConditions(row, "conditions", net.minecraftforge.common.crafting.conditions.ICondition.IContext.EMPTY));
    }
    private static void publish(JsonObject json) {
        var contributed=thaumcraft.api.research.ResearchDataContributors.apply(                thaumcraft.api.research.ResearchDataContributors.Phase.BEFORE_PUBLICATION,json);        contributed = tutorials(contributed, tc4revival.config.TCConfig.SERVER.revival.bookTutorials.get());        var resolved=thaumcraft.api.research.ResearchPrerequisites.materialize(contributed);
        resolved=ResearchAmendments.materialize(resolved,net.minecraftforge.fml.ModList.get()::isLoaded,
                entry->net.minecraftforge.common.crafting.CraftingHelper.processConditions(entry,"conditions",net.minecraftforge.common.crafting.conditions.ICondition.IContext.EMPTY));
        resolved=ResearchPageConditions.materialize(resolved,page->net.minecraftforge.common.crafting.CraftingHelper
                .processConditions(page,"conditions",net.minecraftforge.common.crafting.conditions.ICondition.IContext.EMPTY));
        resolved.add("wandAttributes",thaumcraft.api.wands.WandAttributes.refreshServer());server=decode(resolved);
    }
    public static Snapshot server() { return server; }
    public static Snapshot display() {
        return net.minecraftforge.fml.util.thread.EffectiveSide.get().isServer()?server:client;
    }
    public static void clearClient() { client=null; }
    public static void clearServer() { server=null;rawServer=null;configurationReady=false; }
    public static void accept(JsonObject json) {
        var next=decode(json);
        thaumcraft.api.wands.WandAttributes.accept(json.has("wandAttributes")?json.getAsJsonObject("wandAttributes"):new JsonObject());client=next;
    }
    public static Snapshot decode(JsonObject json) {
        return decode(json,TCAspects.catalog());
    }
    public static Snapshot decode(JsonObject json,tc4revival.aspect.AspectCatalog aspects) {
        // Multiple files may add research to an existing category, but cannot redefine its artwork.
        var categories=new LinkedHashMap<String,ResearchCatalog.Category>();
        for(var element:json.getAsJsonArray("categories")) {
            var category=ResearchCatalog.decodeCategory(element.getAsJsonObject());
            var old=categories.get(category.key());
            if(old!=null) {
                if(!old.icon().equals(category.icon())||!old.background().equals(category.background()))
                    throw new IllegalArgumentException("Conflicting research category artwork: "+category.key());
                var entries=new ArrayList<>(old.research());entries.addAll(category.research());
                // An addon may repeat another category's artwork to insert research.                // Prefer its defining namespace when that source is present among the files.                String owner=category.modId().equals(ResearchOrigins.namespace(category.icon()))?category.modId():old.modId();                category=new ResearchCatalog.Category(old.key(),old.icon(),old.background(),entries,owner);
            }
            categories.put(category.key(),category);
        }
        var catalog=new ResearchCatalog(List.copyOf(categories.values()),aspects);
        for(var entry:catalog.all()) {
            for(var parent:entry.parents())catalog.require(parent);
            for(var parent:entry.hiddenParents())catalog.require(parent);
            for(var parent:entry.parentsAny())catalog.require(parent);
            for(var trigger:entry.triggers().items().orElse(List.of())) {
                trigger.ore().ifPresent(ResearchClues::materialTag);ResearchClues.readNbt(trigger.nbt());
            }
        }
        var pages=ResearchPages.decode(json.getAsJsonArray("pages"));pages.validate(catalog);
        for(var list:pages.entries().values())for(var page:list) {
            page.aspects().keySet().forEach(aspects::require);
            page.research().ifPresent(catalog::require);
        }
        var recipes=ResearchPageRecipes.decode(json.getAsJsonArray("recipes"));recipes.validate(pages);
        return new Snapshot(catalog,pages,recipes,json.deepCopy());
    }
    @Override protected JsonObject prepare(ResourceManager manager,ProfilerFiller profiler) {
        var json=new JsonObject();
        json.add("categories",read(manager,"research_catalog","categories"));
        json.add("pages",read(manager,"research_pages","entries"));
        json.add("recipes",read(manager,"research_page_recipes","entries"));
        json.add("additions",read(manager,"research_page_additions","entries"));
        json.add("changes",read(manager,"research_changes","entries"));
        json.add("replacements",read(manager,"research_text_replacements","entries"));
        var contributed=thaumcraft.api.research.ResearchDataContributors.apply(thaumcraft.api.research.ResearchDataContributors.Phase.BEFORE_ADDITIONS,json);
        return thaumcraft.api.research.ResearchDataContributors.apply(
                thaumcraft.api.research.ResearchDataContributors.Phase.AFTER_ADDITIONS, ResearchPageAdditions.apply(contributed));
    }
    private static JsonArray read(ResourceManager manager,String folder,String field) {
        var array=new JsonArray();
        manager.listResources(folder,id->id.getPath().endsWith(".json")).entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).forEach(entry->{
            try(var reader=entry.getValue().openAsReader()) {
                var root=JsonParser.parseReader(reader).getAsJsonObject();
                if(root.get("format").getAsInt()!=1)throw new IllegalArgumentException("Unsupported research format");
                // Old addon/resource-pack pages predate component IDs. Migrate only
                // those pages at the resource boundary; explicit IDs stay strict.
                if(folder.equals("research_pages"))
                    tc4revival.data.BookDefinitionMigration.upgradeMissingIds(root.getAsJsonArray(field),entry.getKey().getNamespace());
                if(folder.equals("research_catalog"))                    ResearchOrigins.annotate(root.getAsJsonArray(field),entry.getKey().getNamespace());                array.addAll(root.getAsJsonArray(field));
            } catch(Exception e) { throw new IllegalArgumentException("Invalid research file: "+entry.getKey(),e); }
        });
        return array;
    }
    @Override protected void apply(JsonObject json,ResourceManager manager,ProfilerFiller profiler) {
        // Forge loads the world config after the first reload. Keep the raw lessons for that world,
        // but expose only the default book until its own setting is available.
        if (configurationReady) publish(json);
        else server = decode(tutorials(json, false));
        rawServer=json.deepCopy();
    }
}
