package tc4revival.aspect.item;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectCatalog;

/** Direct registrations only. Recipe-derived values and stack-specific rules are separate layers. */
public  class ItemAspectResolver {
    private final Map<String, List<ItemAspectRule>> byItem;
    private final AspectCatalog catalog;
    private final int ruleCount;
    private final boolean additive;
    private final tc4revival.aspect.recipe.InferenceLimits limits;
    private final List<ItemAspectRule> tagRules;
    private final Map<String,List<Integer>> byTag;
    private final java.util.function.BiPredicate<String,String> membership;
    private final Map<String,List<ItemAspectRule>> tagCandidates = new java.util.concurrent.ConcurrentHashMap<>();

    public ItemAspectResolver(List<ItemAspectRule> rules, AspectCatalog catalog) {
        this(rules,catalog,false);
    }
    public ItemAspectResolver(List<ItemAspectRule> rules, AspectCatalog catalog,boolean additive) {
        this(rules,catalog,additive,tc4revival.aspect.recipe.InferenceLimits.server());
    }
    public ItemAspectResolver(List<ItemAspectRule> rules,AspectCatalog catalog,boolean additive,tc4revival.aspect.recipe.InferenceLimits limits) {
        this(rules,catalog,additive,limits,null);
    }
    public ItemAspectResolver(List<ItemAspectRule> rules,AspectCatalog catalog,boolean additive,tc4revival.aspect.recipe.InferenceLimits limits,java.util.function.BiPredicate<String,String> membership) {
        this.limits=limits;
        this.membership=membership;
        this.additive=additive;
        this.catalog = catalog;
        ruleCount = rules.size();
        var index = new LinkedHashMap<String, List<ItemAspectRule>>();
        var tags = new ArrayList<ItemAspectRule>();
        for (var source : rules) {
            var rule = new ItemAspectRule(source.items(), source.requiredTag(),
                    source.aspects().stream().filter(q -> catalog.find(q.aspect()).isPresent()).toList(), source.generated(),source.nbt());
            rule.amounts(catalog,limits);
            if(rule.items().contains("*"))tags.add(rule);
            else for (String item : rule.items()) index.computeIfAbsent(item, key -> new ArrayList<>()).add(rule);
        }
        var frozen = new LinkedHashMap<String, List<ItemAspectRule>>();
        index.forEach((item, values) -> frozen.put(item, List.copyOf(values)));
        byItem = Map.copyOf(frozen);
        tagRules=List.copyOf(tags);
        var tagIndex=new LinkedHashMap<String,List<Integer>>();
        for(int i=0;i<tagRules.size();i++)tagIndex.computeIfAbsent(tagRules.get(i).requiredTag(),key->new ArrayList<>()).add(i);
        tagIndex.replaceAll((tag,indices)->List.copyOf(indices));
        byTag=Map.copyOf(tagIndex);
    }

    public int ruleCount() { return ruleCount; }
    public int itemCount() { return byItem.size(); }
    public boolean contains(String item) { return byItem.containsKey(item)||membership!=null&&!rules(item).isEmpty(); }
    /** Registry-backed callers invalidate this index when the tag snapshot changes. */
    public void clearTagCache() { tagCandidates.clear(); }
    public boolean hasNbtRules(String item) { return rules(item).stream().anyMatch(r->!r.nbt().isEmpty()); }
    public List<ItemAspectRule> rules(String item) {
        if(tagRules.isEmpty())return byItem.getOrDefault(item,List.of());
        if(membership!=null)return tagCandidates.computeIfAbsent(item,id->{
            var matching=new ArrayList<>(byItem.getOrDefault(id,List.of()));
            if(membership instanceof ItemTagLookup lookup){
                // Retain authoring order, including several rules for the same tag.
                var indices=new java.util.TreeSet<Integer>();
                for(var tag:lookup.tags(id))indices.addAll(byTag.getOrDefault(tag,List.of()));
                for(int index:indices)matching.add(tagRules.get(index));
            }else for(var rule:tagRules)if(membership.test(id,rule.requiredTag()))matching.add(rule);
            return List.copyOf(matching);
        });
        var rules=new ArrayList<>(byItem.getOrDefault(item,List.of()));rules.addAll(tagRules);return rules;
    }
    public net.minecraft.nbt.CompoundTag nbtCacheKey(String item,net.minecraft.nbt.CompoundTag tag) {
        var result=new net.minecraft.nbt.CompoundTag();for(var rule:rules(item))rule.nbt().project(tag,result);return result;
    }
    public String nbtIdentity(String item,Predicate<String> tags,net.minecraft.nbt.CompoundTag tag) {
        return rules(item).stream().filter(r->!r.nbt().isEmpty()&&r.matches(item,tags,tag)).map(r->r.nbt().canonical()).distinct().sorted().collect(java.util.stream.Collectors.joining(";"));
    }

    public Optional<AspectAmounts> editorOverride(String item,Predicate<String> hasTag){return Optional.empty();}
    public Optional<AspectAmounts> editorOverride(String item,Predicate<String> hasTag,net.minecraft.nbt.CompoundTag tag){return editorOverride(item,hasTag);}
    public Optional<ItemAspectRule> tagRule(String item,String tag){
        return combinedRule(item,rules(item).stream().filter(r->tag.equals(r.requiredTag())).toList());
    }
    public Optional<ItemAspectRule> selectedRule(String item,Predicate<String> hasTag){
        return selectedRule(item,hasTag,null);
    }
    public Optional<ItemAspectRule> selectedRule(String item,Predicate<String> hasTag,net.minecraft.nbt.CompoundTag tag){
        return combinedRule(item,rules(item).stream().filter(r->r.matches(item,hasTag,tag)).toList());
    }
    private Optional<ItemAspectRule> combinedRule(String item,List<ItemAspectRule> matching){
        if(matching.isEmpty())return Optional.empty();
        if(matching.size()==1)return Optional.of(matching.get(0));
        var amounts=combine(matching);var representative=matching.stream().filter(r->r.requiredTag()==null&&!r.generated()).findFirst().orElse(matching.get(matching.size()-1));
        return Optional.of(new ItemAspectRule(List.of(item),representative.requiredTag(),amounts.view().entrySet().stream()
                .map(e->new ItemAspectRule.Quantity(e.getKey().tag(),e.getValue())).toList(),matching.stream().allMatch(ItemAspectRule::generated)));
    }
    private AspectAmounts combine(List<ItemAspectRule> matching){
        var result=new AspectAmounts();
        for(var rule:matching){var amounts=rule.amounts(catalog,limits);if(additive)result.add(amounts);else result.merge(amounts);}
        return matching.stream().allMatch(ItemAspectRule::generated)
                ?tc4revival.aspect.recipe.GeneratedAspectLimit.apply(result,catalog,limits):result;
    }

    public Optional<AspectAmounts> resolve(String item, Predicate<String> hasTag) {
        return resolve(item,hasTag,null);
    }
    public Optional<AspectAmounts> resolve(String item, Predicate<String> hasTag,net.minecraft.nbt.CompoundTag tag) {
        var matching=rules(item).stream().filter(r->r.matches(item,hasTag,tag)).toList();
        return matching.isEmpty()?Optional.empty():Optional.of(combine(matching));
    }
}
