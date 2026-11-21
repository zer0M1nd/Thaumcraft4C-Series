package tc4revival.client;

import java.util.*;
import net.minecraft.world.item.ItemStack;
import tc4revival.research.*;

/** First recipe output in prerequisite order; independent of what this player has unlocked. */
public final class BookItemResearch {
    private BookItemResearch() {}
    private static ResearchDefinitions.Snapshot snapshot;
    private static long generation=-1;
    private static final Map<String,String> cache=new HashMap<>();
    public static String first(ItemStack stack) {
        var current=ResearchDefinitions.display();
        if(stack.isEmpty()||current==null)return "";
        if(snapshot!=current||generation!=ClientLiveBookRecipes.generation()) {
            snapshot=current;generation=ClientLiveBookRecipes.generation();cache.clear();
        }
        String identity=stack.getItem()+":"+stack.getTag();
        return cache.computeIfAbsent(identity,key->find(stack,current));
    }
    private static String find(ItemStack stack,ResearchDefinitions.Snapshot definitions) {
        var catalog=ResearchCatalogs.bundled();var depths=new HashMap<String,Integer>();
        var keys=new ArrayList<>(definitions.pages().entries().keySet());
        keys.sort(Comparator.comparingInt(key->depth(key,catalog,depths,new HashSet<>())));
        String fallback="";
        for(String key:keys)for(var page:definitions.pages().require(key)) {
            var outputs=new ArrayList<ItemStack>();
            if(LiveBookRecipes.supported(page.type()))ClientLiveBookRecipes.get(page).forEach(view->outputs.add(view.output()));
            page.output().flatMap(value->ResearchDisplayItems.bundled().resolve(value,0)).ifPresent(outputs::add);
            for(var output:outputs)if(output.is(stack.getItem())) {
                if(ItemStack.isSameItemSameTags(output,stack))return key;
                if(fallback.isEmpty())fallback=key;
            }
        }
        return fallback;
    }
    private static int depth(String key,ResearchCatalog catalog,Map<String,Integer> memo,Set<String> active) {
        if(memo.containsKey(key))return memo.get(key);
        if(!active.add(key))return 0;
        var row=catalog.find(key);int value=0;
        if(row.isPresent()) {
            var parents=new ArrayList<>(row.get().parents());parents.addAll(row.get().hiddenParents());
            for(String parent:parents)value=Math.max(value,1+depth(parent,catalog,memo,active));
            if(!row.get().parentsAny().isEmpty())value=Math.max(value,1+row.get().parentsAny().stream()
                    .mapToInt(parent->depth(parent,catalog,memo,active)).min().orElse(0));
        }
        active.remove(key);memo.put(key,value);return value;
    }
}
