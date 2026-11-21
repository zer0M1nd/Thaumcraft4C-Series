package tc4revival.aspect.editor;

import java.util.*;
import java.util.function.*;
import tc4revival.aspect.*;
import tc4revival.aspect.item.*;

/** Preserve native direct > tag > recipe priority, with editor overrides at each layer. */
public class EditorItemResolver extends ItemAspectResolver {
    private final ItemAspectResolver original;
    private final EditorDefinitions definitions;
    private final AspectCatalog catalog;
    private final BiPredicate<String,String> membership;
    public EditorItemResolver(ItemAspectResolver original,EditorDefinitions definitions,AspectCatalog catalog,BiPredicate<String,String> membership){
        super(List.of(),catalog);this.original=original;this.definitions=definitions;this.catalog=catalog;this.membership=membership;
    }
    public ItemAspectResolver original(){return original;}
    public EditorDefinitions definitions(){return definitions;}
    @Override public Optional<ItemAspectRule> tagRule(String id,String tag){return original.tagRule(id,tag);}
    @Override public int ruleCount(){return original.ruleCount()+definitions.entries().size();}
    @Override public int itemCount(){return original.itemCount();}
    @Override public void clearTagCache(){original.clearTagCache();}
    @Override public boolean contains(String id){return original.contains(id)||editorOverride(id,tag->membership.test(id,tag)).isPresent();}
    @Override public boolean hasNbtRules(String id){return original.hasNbtRules(id);}
    @Override public List<ItemAspectRule> rules(String id){return original.rules(id);}
    @Override public net.minecraft.nbt.CompoundTag nbtCacheKey(String id,net.minecraft.nbt.CompoundTag tag){return original.nbtCacheKey(id,tag);}
    @Override public String nbtIdentity(String id,Predicate<String> tags,net.minecraft.nbt.CompoundTag tag){return original.nbtIdentity(id,tags,tag);}
    @Override public Optional<ItemAspectRule> selectedRule(String id,Predicate<String> tags){return original.selectedRule(id,tags);}
    @Override public Optional<ItemAspectRule> selectedRule(String id,Predicate<String> tags,net.minecraft.nbt.CompoundTag tag){return original.selectedRule(id,tags,tag);}
    @Override public Optional<AspectAmounts> editorOverride(String id,Predicate<String> tags){
        return editorOverride(id,tags,null);
    }
    @Override public Optional<AspectAmounts> editorOverride(String id,Predicate<String> tags,net.minecraft.nbt.CompoundTag nbt){
        var exact=definitions.get("item:"+id);if(exact.isPresent())return exact.map(v->EditorDefinitions.amounts(v,catalog));
        var tagEdit=definitions.tag("item",tags);
        if(tagEdit.isEmpty())return Optional.empty();
        var nativeRule=original.selectedRule(id,tags,nbt);
        if(nativeRule.isPresent()&&nativeRule.get().requiredTag()==null&&!nativeRule.get().generated())return Optional.empty();
        return tagEdit.map(v->EditorDefinitions.amounts(v,catalog));
    }
    @Override public Optional<AspectAmounts> resolve(String id,Predicate<String> tags){
        var edited=editorOverride(id,tags);return edited.isPresent()?edited:original.resolve(id,tags);
    }
    @Override public Optional<AspectAmounts> resolve(String id,Predicate<String> tags,net.minecraft.nbt.CompoundTag tag){
        var edited=editorOverride(id,tags,tag);return edited.isPresent()?edited:original.resolve(id,tags,tag);
    }
}
