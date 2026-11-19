package tc4revival.research;

import java.util.*;
import tc4revival.aspect.*;

/** Persisted per player, independent of a table's inventory and location. */
public record ResearchPreferences(Sort sort,boolean descending,boolean autoRefill) {
    public enum Sort { DEFAULT, ORDER, QUANTITY }
    public static final ResearchPreferences DEFAULT=new ResearchPreferences(Sort.DEFAULT,false,false);
    public int packed() { return sort.ordinal()|(descending?4:0)|(autoRefill?8:0); }
    public static ResearchPreferences unpack(int value) { return new ResearchPreferences(Sort.values()[Math.min(2,value&3)],(value&4)!=0,(value&8)!=0); }
    public List<Aspect> sort(PlayerKnowledge knowledge,AspectCatalog catalog) {
        var order=new HashMap<String,Integer>();
        Comparator<Aspect> comparator=switch(sort) {
            case DEFAULT -> Comparator.comparing(Aspect::tag);
            case ORDER -> Comparator.comparingInt((Aspect aspect)->order(aspect,catalog,order));
            case QUANTITY -> Comparator.comparingInt(aspect->knowledge.knownAspects().getOrDefault(aspect,0));
        };
        if(descending)comparator=comparator.reversed();
        return knowledge.knownAspects().keySet().stream().sorted(comparator.thenComparing(Aspect::tag)).toList();
    }
    public static int order(Aspect aspect,AspectCatalog catalog,Map<String,Integer> cache) {
        var existing=cache.get(aspect.tag());if(existing!=null)return existing;
        int value=aspect.components().isEmpty()?0:1+aspect.components().stream().map(catalog::require).mapToInt(parent->order(parent,catalog,cache)).max().orElse(0);
        cache.put(aspect.tag(),value);return value;
    }
}
