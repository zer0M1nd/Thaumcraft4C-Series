package tc4revival.aspect.item;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.TC4Revival;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.TCAspects;
import tc4revival.aspect.recipe.RecipeAspects;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="thaumcraft")
public  class ItemAspects {
    private static volatile ItemAspectResolver direct;
    private static java.util.List<ItemAspectRule> authoredRules=java.util.List.of();
    private static Map<ResourceLocation,JsonElement> authoredFiles=Map.of();
    public static synchronized void inference(java.util.List<ItemAspectRule> additional){
        var rules=new ArrayList<>(authoredRules);rules.addAll(additional);
        direct=new tc4revival.aspect.editor.EditorItemResolver(new ItemAspectResolver(rules,TCAspects.catalog(),false,tc4revival.aspect.recipe.InferenceLimits.server(),AspectRegistryTargets.itemTags()),
            tc4revival.aspect.editor.EditorResources.server(),TCAspects.catalog(),
            (id,tag)->ForgeRegistries.ITEMS.getValue(new ResourceLocation(id)).builtInRegistryHolder().is(ItemTags.create(new ResourceLocation(tag))));
        cached=null;
        var files=new java.util.TreeMap<ResourceLocation,JsonElement>(authoredFiles);
        files.put(ResourceLocation.parse("thaumcraft:advanced_inference"),ItemAspectJson.encode(additional));
        tc4revival.aspect.sync.ServerAspectData.capture("item_aspects",files);
    }
    private record SessionCache(LegacyItemIdentity identity,tc4revival.aspect.recipe.RecipeAspectData recipes,
            tc4revival.aspect.AspectCatalog catalog,ItemAspectResolver direct,PotionAspectData potions,tc4revival.aspect.recipe.RecipeAspectData crafting,ItemBaseAspectSession session) {}
    private static SessionCache cached;
    @net.minecraftforge.eventbus.api.SubscribeEvent public static synchronized void tagsUpdated(net.minecraftforge.event.TagsUpdatedEvent event) {
        cached=null;
        if(direct!=null)direct.clearTagCache();
        ItemStackAspects.tagsUpdated();
    }
    private ItemAspects() {}

    public static void addReloadListener(AddReloadListenerEvent event) { event.addListener(new Reload()); }
    public static synchronized void stopped(ServerStoppedEvent event) { direct = null;cached=null;tc4revival.aspect.editor.EditorResources.clear(); }

    public static ItemAspectResolver directResolver() {
        var current = direct;
        if (current == null) throw new IllegalStateException("Item aspect data has not loaded");
        return current;
    }

    public static Optional<AspectAmounts> direct(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) return Optional.empty();
        return directResolver().resolve(id.toString(), tag -> stack.is(ItemTags.create(new ResourceLocation(tag))),stack.getTag());
    }

    /** Reuse base calculations until definitions or tags change; stack/NBT bonuses remain per-stack. */
    public static synchronized ItemBaseAspectSession baseSession() {
        var identity=LegacyItemMappings.identity();var recipes=RecipeAspects.data();var catalog=TCAspects.catalog();var resolver=directResolver();var potions=PotionAspects.data();
        var crafting=tc4revival.aspect.recipe.RuntimeCraftingAspects.data();
        if(cached!=null&&cached.identity()==identity&&cached.recipes()==recipes&&cached.catalog()==catalog&&cached.direct()==resolver&&cached.potions()==potions&&cached.crafting()==crafting)return cached.session();
        var session=new ItemBaseAspectSession(identity, recipes, catalog, resolver,
                (id, tag) -> ForgeRegistries.ITEMS.getValue(new ResourceLocation(id)).builtInRegistryHolder()
                        .is(ItemTags.create(new ResourceLocation(tag))), PotionAspects::decorate,crafting);
        cached=new SessionCache(identity,recipes,catalog,resolver,potions,crafting,session);return session;
    }

    private static  class Reload extends SimpleJsonResourceReloadListener {
        Reload() { super(new Gson(), "item_aspects"); }
        @Override protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            var rules = new ArrayList<ItemAspectRule>();
            files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> rules.addAll(ItemAspectJson.decodeKnown(entry.getValue(), TCAspects.catalog(), AspectRegistryTargets::item)));
            direct = new tc4revival.aspect.editor.EditorItemResolver(new ItemAspectResolver(rules,TCAspects.catalog(),false,tc4revival.aspect.recipe.InferenceLimits.server(),AspectRegistryTargets.itemTags()),
                    tc4revival.aspect.editor.EditorResources.load(manager),TCAspects.catalog(),
                    (id,tag)->ForgeRegistries.ITEMS.getValue(new ResourceLocation(id)).builtInRegistryHolder().is(ItemTags.create(new ResourceLocation(tag))));
            authoredRules=java.util.List.copyOf(rules);authoredFiles=Map.copyOf(files);
            tc4revival.aspect.sync.ServerAspectData.capture("item_aspects",files);
            TC4Revival.LOGGER.info("Loaded {} direct item aspect rules for {} items", direct.ruleCount(), direct.itemCount());
        }
    }
}
