package automagy.logic;
import java.util.*;
import java.util.function.IntUnaryOperator;
/** Legacy weighted distribution; merge means maximum, while the low-Ignis bonus is additive. */
public final class FieryNodeRoll {
    private FieryNodeRoll(){}
    public static Map<String,Integer> roll(int biomeAura,IntUnaryOperator random){return roll(biomeAura,random,null);}
    public static Map<String,Integer> roll(int biomeAura,IntUnaryOperator random,String optionalNetherAspect){
        int half=Math.max(1,biomeAura/2),value=random.applyAsInt(half)+half;
        var weights=new TreeMap<String,Integer>();weights.put("ignis",2);weights.put(random.applyAsInt(2)==0?"mortuus":"exanimis",1);if(random.applyAsInt(2)==0)weights.put("tenebrae",1);
        if(optionalNetherAspect!=null&&!optionalNetherAspect.isBlank())weights.merge(optionalNetherAspect,1,Math::max);
        var shares=new LinkedHashMap<String,Integer>();float total=0;
        for(var e:weights.entrySet()){int share=e.getValue()==2?50+random.applyAsInt(25):25+random.applyAsInt(50);shares.put(e.getKey(),share);total+=share;}
        for(var e:shares.entrySet())weights.put(e.getKey(),Math.max(weights.get(e.getKey()),(int)(e.getValue()/total*value)));
        if(weights.get("ignis")<50)weights.put("ignis",weights.get("ignis")+20+random.applyAsInt(20));
        return Collections.unmodifiableMap(weights);
    }
}
