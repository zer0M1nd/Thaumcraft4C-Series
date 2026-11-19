package tc4revival.aspect.editor;

import java.util.*;
import java.util.function.Predicate;
import tc4revival.aspect.*;
import tc4revival.aspect.item.*;
import tc4revival.aspect.entity.*;
import tc4revival.aspect.sync.AspectDataSet;

/** Track origin from the synchronized resource identity, not from generated/empty quantities. */
public class AdvancedAspectSources {
    public static final String RESOURCE="thaumcraft:advanced_inference";
    private final ItemAspectResolver authoredItems, inferredItems;
    private final BlockAspectTable authoredBlocks, inferredBlocks;
    private final Set<String> authoredEntities=new HashSet<>(),inferredEntities=new HashSet<>();
    public AdvancedAspectSources(AspectDataSet files,AspectCatalog catalog) {
        var authored=new ArrayList<ItemAspectRule>();var inferred=new ArrayList<ItemAspectRule>();
        files.directory("item_aspects").forEach((id,json)->(id.equals(RESOURCE)?inferred:authored).addAll(ItemAspectJson.decodeKnown(json,catalog,key->true)));
        authoredItems=new ItemAspectResolver(authored,catalog);inferredItems=new ItemAspectResolver(inferred,catalog);
        var blocks=files.directory("block_aspects");
        authoredBlocks=new BlockAspectTable(blocks.entrySet().stream().filter(e->!e.getKey().equals(RESOURCE)).map(Map.Entry::getValue).toList(),catalog);
        inferredBlocks=new BlockAspectTable(blocks.entrySet().stream().filter(e->e.getKey().equals(RESOURCE)).map(Map.Entry::getValue).toList(),catalog);
        files.directory("entity_aspects").forEach((id,json)->EntityAspectJson.decode(json).forEach(rule->(id.equals(RESOURCE)?inferredEntities:authoredEntities).add(rule.entity())));
    }
    public boolean item(String id,Predicate<String> tags,net.minecraft.nbt.CompoundTag nbt) {
        return authoredItems.selectedRule(id,tags,nbt).isEmpty()&&inferredItems.selectedRule(id,tags,nbt).isPresent();
    }
    public boolean block(String id,Map<String,String> properties) {
        return authoredBlocks.resolve(id,properties).isEmpty()&&inferredBlocks.resolve(id,properties).isPresent();
    }
    public boolean entity(String id) {return !authoredEntities.contains(id)&&inferredEntities.contains(id);}
}
