package tc4revival.aspect.sync;

import java.util.*;
import java.util.function.BiPredicate;
import tc4revival.aspect.*;
import tc4revival.aspect.item.*;
import tc4revival.aspect.recipe.*;
import net.minecraft.world.item.ItemStack;

/** A self-contained evaluator over one synchronized resource/tag snapshot. No server static data reads. */
public class AspectCalculationData implements ItemAspectAccess {
    private final AspectCatalog catalog;
    private final tc4revival.aspect.editor.AdvancedAspectSources advancedSources;
    private final ItemAspectResolver direct,equipment;
    private final tc4revival.aspect.editor.EditorDefinitions editor;
    private final BlockScanMapping blocks;
    private final BlockAspectTable blockAspects;
    private final tc4revival.aspect.editor.EditorEntityDefaults entities;
    private final PotionAspectData potions;
    private final LegacyItemIdentity identity;
    private final Map<String,RecipeAspectData.Key> eggs;
    private final ItemBaseAspectSession session;
    private final RecipeAspectEngine engine;
    private final Map<RecipeAspectData.Key,Optional<AspectAmounts>> objects=new HashMap<>();
    public AspectCalculationData(AspectDataSet files,AspectCatalog catalog,BiPredicate<String,String> hasTag){
        this(files,catalog,hasTag,tc4revival.aspect.recipe.InferenceLimits.server());
    }
    public AspectCalculationData(AspectDataSet files,AspectCatalog catalog,BiPredicate<String,String> hasTag,
            tc4revival.aspect.recipe.InferenceLimits limits){
        this.catalog=catalog;
        advancedSources=new tc4revival.aspect.editor.AdvancedAspectSources(files,catalog);
        entities=new tc4revival.aspect.editor.EditorEntityDefaults(files,catalog,id->net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.containsKey(net.minecraft.resources.ResourceLocation.parse(id)));
        var directRules=new ArrayList<ItemAspectRule>();files.directory("item_aspects").values().forEach(json->directRules.addAll(ItemAspectJson.decodeKnown(json,catalog,AspectRegistryTargets::item)));
        editor=tc4revival.aspect.editor.EditorDefinitions.merge(files.directory("aspect_editor").values()).filtered(catalog,AspectRegistryTargets::editor);
        direct=new tc4revival.aspect.editor.EditorItemResolver(new ItemAspectResolver(directRules,catalog,false,limits,hasTag),editor,catalog,hasTag);
        var blockFiles=new TreeMap<net.minecraft.resources.ResourceLocation,com.google.gson.JsonElement>();
        files.directory("block_scan_targets").forEach((id,json)->blockFiles.put(net.minecraft.resources.ResourceLocation.parse(id),json));
        blocks=BlockScanTargets.validatedMapping(blockFiles);
        blockAspects=new BlockAspectTable(files.directory("block_aspects").values(),catalog,AspectRegistryTargets::block);
        var bonuses=new ArrayList<ItemAspectRule>();files.directory("item_bonus_aspects").values().forEach(json->bonuses.addAll(ItemAspectJson.decodeKnown(json,catalog,AspectRegistryTargets::item)));
        equipment=new ItemAspectResolver(bonuses,catalog,true,limits,hasTag);
        var potionFiles=files.directory("potion_aspects");
        if(potionFiles.size()!=1||!potionFiles.containsKey("thaumcraft:legacy"))throw new IllegalArgumentException("Missing potion aspect snapshot");
        potions=PotionAspectJson.decode(potionFiles.get("thaumcraft:legacy"));
        var recipeFiles=files.directory("recipe_aspect_inputs");
        if(!recipeFiles.containsKey("thaumcraft:legacy"))throw new IllegalArgumentException("Missing recipe aspect snapshot");
        var inputs=new ArrayList<RecipeAspectData>();inputs.add(RecipeAspectJson.decode(recipeFiles.get("thaumcraft:legacy")));
        recipeFiles.forEach((id,json)->{if(!id.equals("thaumcraft:legacy"))inputs.add(RecipeAspectJson.decode(json));});
        var recipes=RuntimeCraftingAspects.withoutPresetCrafting(RecipeAspectInputs.merge(inputs));
        var craftingFiles=files.directory("crafting_aspect_inputs");
        var crafting=craftingFiles.containsKey("thaumcraft:runtime")?RecipeAspectJson.decode(craftingFiles.get("thaumcraft:runtime")):null;
        var items=new LinkedHashMap<String,RecipeAspectData.Key>();var groups=new LinkedHashMap<RecipeAspectData.Key,Integer>();var spawnEggs=new LinkedHashMap<String,RecipeAspectData.Key>();
        files.directory("legacy_item_mappings").values().forEach(json->{
            var mapping=LegacyItemMappings.decode(json);merge(items,mapping.items());merge(groups,mapping.groups());merge(spawnEggs,mapping.spawnEggs());
        });
        identity=new LegacyItemIdentity(items,groups,recipes);eggs=Map.copyOf(spawnEggs);
        var potionRules=new PotionAspectRules(potions.definitions(),catalog);
        java.util.function.BiFunction<RecipeAspectData.Key,AspectAmounts,AspectAmounts> decorate=(key,amounts)->key.item().equals("minecraft:potion")?potionRules.decorate(key,amounts):amounts;
        session=new ItemBaseAspectSession(identity,recipes,catalog,direct,hasTag,decorate,crafting,limits);
        engine=new RecipeAspectEngine(recipes,catalog,Map.of(),decorate,limits);
    }
    private static <K,V> void merge(Map<K,V> into,Map<K,V> from){from.forEach((k,v)->{if(into.putIfAbsent(k,v)!=null)throw new IllegalArgumentException("Duplicate aspect mapping "+k);});}
    public AspectCatalog catalog(){return catalog;}
    public tc4revival.aspect.editor.AdvancedAspectSources advancedSources(){return advancedSources;}
    public tc4revival.aspect.editor.EditorDefinitions editor(){return editor;}
    public BlockScanMapping blocks(){return blocks;}
    public BlockAspectTable blockAspects(){return blockAspects;}
    public tc4revival.aspect.editor.EditorEntityDefaults entities(){return entities;}
    public ItemAspectResolver direct(){return direct;}
    public ItemAspectResolver equipment(){return equipment;}
    public ItemBaseAspectSession baseSession(){return session;}
    public LegacyItemIdentity identity(){return identity;}
    public Map<String,RecipeAspectData.Key> spawnEggs(){return eggs;}
    public Optional<RecipeAspectData.Key> potionKey(ItemStack stack){return PotionAspects.key(stack,potions);}
    public synchronized AspectAmounts recipeObject(RecipeAspectData.Key key){return objects.computeIfAbsent(key,k->Optional.ofNullable(engine.fork().object(k))).map(AspectAmounts::copy).orElse(null);}
}
