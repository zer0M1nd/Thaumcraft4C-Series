package thmodern.blood;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.ToIntFunction;

/** Pure recipe arithmetic, independent of world state and mutable registry ordinals. */
public final class BloodFormula {
    public record Effect(String id,int duration,int amplifier,int weight){}
    public record Result(List<Effect> effects,int color){public Result{effects=List.copyOf(effects);}}
    private final LinkedHashMap<String,Map<String,Integer>> weights=new LinkedHashMap<>();
    private final List<String> order;
    public final int maximum;
    public BloodFormula(JsonObject definition){
        maximum=definition.get("maximum_essentia").getAsInt();if(maximum<1||maximum>64)throw new IllegalArgumentException("Invalid formula limit");
        order=new ArrayList<>();definition.getAsJsonArray("effect_order").forEach(e->order.add(e.getAsString()));
        for(var aspect:definition.getAsJsonObject("weights").entrySet()){
            var map=new LinkedHashMap<String,Integer>();
            for(var entry:aspect.getValue().getAsJsonObject().entrySet()){
                int n=entry.getValue().getAsInt();if(n<=0||n>100||!order.contains(entry.getKey()))throw new IllegalArgumentException("Invalid weight");map.put(entry.getKey(),n);
            }
            weights.put(aspect.getKey(),Map.copyOf(map));
        }
    }
    public static BloodFormula bundled(){
        try(var stream=BloodFormula.class.getResourceAsStream("/data/thaumichorizons/blood_effect_weights.json");var reader=new InputStreamReader(Objects.requireNonNull(stream),StandardCharsets.UTF_8)){return new BloodFormula(JsonParser.parseReader(reader).getAsJsonObject());}
        catch(IOException e){throw new IllegalStateException(e);}
    }
    public Map<String,Integer> weights(String aspect){return weights.getOrDefault(aspect,Map.of());}
    public boolean supports(String aspect){return weights.containsKey(aspect);}
    public Set<String> aspects(){return Collections.unmodifiableSet(weights.keySet());}
    public Result calculate(Map<String,Integer> selected,ToIntFunction<String> color){
        int total=0,red=0,green=0,blue=0;var scores=new LinkedHashMap<String,Integer>();
        for(var entry:selected.entrySet()){
            int quantity=entry.getValue();if(quantity<0||quantity>maximum)throw new IllegalArgumentException("Invalid amount");
            if(quantity==0)continue;
            if(!supports(entry.getKey())||(total+=quantity)>maximum)throw new IllegalArgumentException("Unsupported or oversized formula");
            int rgb=color.applyAsInt(entry.getKey());red+=(rgb>>16&255)*quantity;green+=(rgb>>8&255)*quantity;blue+=(rgb&255)*quantity;
            weights.get(entry.getKey()).forEach((effect,weight)->scores.merge(effect,weight*quantity,Integer::sum));
        }
        if(total==0)return new Result(List.of(),0);
        var effects=scores.entrySet().stream().sorted(Comparator.<Map.Entry<String,Integer>>comparingInt(Map.Entry::getValue).reversed().thenComparingInt(e->order.indexOf(e.getKey())))
                .limit((total+1)/2).map(e->new Effect(e.getKey(),e.getKey().equals("minecraft:instant_health")||e.getKey().equals("minecraft:instant_damage")?1:e.getValue()*100,e.getValue()/30,e.getValue())).toList();
        return new Result(effects,(red/total<<16)|(green/total<<8)|blue/total);
    }
}
