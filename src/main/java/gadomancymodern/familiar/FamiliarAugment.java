package gadomancymodern.familiar;

import java.util.*;

/** Extensible augment definitions; author 1.0.7.3's two mutually exclusive groups. */
public class FamiliarAugment {
    public static final Map<String,FamiliarAugment> REGISTRY=new LinkedHashMap<>();
    public static final FamiliarAugment SHOCK=register("shock",true,Map.of("aer",3,"perditio",2));
    public static final FamiliarAugment POISON=register("poison",true,Map.of("aqua",3,"perditio",4));
    public static final FamiliarAugment FIRE=register("fire",true,Map.of("ignis",4,"ordo",2));
    public static final FamiliarAugment WEAKNESS=register("weakness",true,Map.of("perditio",3,"terra",3));
    public static final FamiliarAugment DAMAGE=register("damage",false,Map.of("ignis",3));
    public static final FamiliarAugment RANGE=register("range",false,Map.of("aer",2,"ordo",1));
    public static final FamiliarAugment SPEED=register("speed",false,Map.of("ordo",2,"ignis",1));
    public final String id;public final boolean element;public final Map<String,Integer> costs;
    public FamiliarAugment(String id,boolean element,Map<String,Integer> costs){this.id=id;this.element=element;this.costs=Map.copyOf(costs);}
    public static FamiliarAugment register(String id,boolean element,Map<String,Integer> costs){var value=new FamiliarAugment(id,element,costs);if(REGISTRY.putIfAbsent(id,value)!=null)throw new IllegalArgumentException("Duplicate familiar augment: "+id);return value;}
    public boolean accepts(Map<String,Integer> current,int level){
        if(level<=0||current.getOrDefault(id,0)!=level-1)return false;boolean hasElement=false;
        for(var key:current.keySet()){var other=REGISTRY.get(key);if(other==null||current.get(key)<=0)continue;if(other.element)hasElement=true;if(other!=this&&other.element==element)return false;}
        return element||hasElement;
    }
    public static Map<String,Integer> costs(Map<String,Integer> augments){var costs=new LinkedHashMap<String,Integer>();augments.forEach((id,level)->{var augment=REGISTRY.get(id);if(augment!=null&&level>0)augment.costs.forEach((aspect,amount)->costs.merge(aspect,Math.multiplyExact(level,amount),Math::addExact));});return costs;}
    public static double damage(Map<String,Integer> augments){return 1+1.1*augments.getOrDefault("damage",0);}
    public static double range(Map<String,Integer> augments){return 6+1.8*augments.getOrDefault("range",0);}
    public static int delay(Map<String,Integer> augments){return 32-6*augments.getOrDefault("speed",0);}
}
