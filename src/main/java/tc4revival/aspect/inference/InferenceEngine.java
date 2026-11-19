package tc4revival.aspect.inference;

import java.util.*;

/** Independently implemented bounded, recipe-first inference. No Minecraft or JEI client dependency. */
public final class InferenceEngine {
    public record Amount(String key,double count){public Amount{if(!Double.isFinite(count)||count<=0||count>1000000)throw new IllegalArgumentException("Invalid ingredient count");}}
    public record Recipe(String id,Amount output,List<List<Amount>> inputs){
        public Recipe{inputs=inputs.stream().map(List::copyOf).toList();if(inputs.isEmpty()||inputs.stream().anyMatch(List::isEmpty))throw new IllegalArgumentException("Incomplete recipe");}
    }
    /** Rounds use snapshots: recipe file order cannot accelerate one chain and starve another. */
    public static Map<String,Map<String,Integer>> infer(Map<String,Map<String,Integer>> seeds,List<Recipe> recipes,int rounds,double decay){
        return infer(seeds,recipes,rounds,decay,tc4revival.aspect.recipe.InferenceLimits.DEFAULT,values -> {
            var result=new TreeMap<String,Integer>();
            values.entrySet().stream().sorted(Map.Entry.<String,Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(10).forEach(e->result.put(e.getKey(),Math.min(256,e.getValue())));return result;
        });
    }
    public static Map<String,Map<String,Integer>> infer(Map<String,Map<String,Integer>> seeds,List<Recipe> recipes,int rounds,double decay,
            java.util.function.UnaryOperator<Map<String,Integer>> limiter){
        return infer(seeds,recipes,rounds,decay,new tc4revival.aspect.recipe.InferenceLimits(64,10),limiter);
    }
    public static Map<String,Map<String,Integer>> infer(Map<String,Map<String,Integer>> seeds,List<Recipe> recipes,int rounds,double decay,
            tc4revival.aspect.recipe.InferenceLimits limits,java.util.function.UnaryOperator<Map<String,Integer>> limiter){
        if(rounds<1||rounds>64||!Double.isFinite(decay)||decay<=0||decay>1)throw new IllegalArgumentException("Invalid inference settings");
        var known=new HashMap<String,Map<String,Integer>>();seeds.forEach((k,v)->{if(!v.isEmpty())known.put(k,Map.copyOf(v));});
        // A known entry never changes during this run. Expand and sum its cost once,
        // rather than once per occurrence in every recipe's ingredient alternatives.
        var costs=new HashMap<String,Double>();
        known.forEach((key,value)->costs.put(key,cost(value,limits)));
        var ordered=recipes.stream().sorted(Comparator.comparing(Recipe::id)).toList();
        for(int round=0;round<rounds;round++){
            var next=new TreeMap<String,Map<String,Integer>>();
            for(var recipe:ordered){
                if(known.containsKey(recipe.output().key()))continue;
                var sum=new TreeMap<String,Double>();boolean complete=true;
                for(var slot:recipe.inputs()){
                    Amount chosen=null;double lowest=Double.POSITIVE_INFINITY;
                    for(var alternative:slot){
                        var unitCost=costs.get(alternative.key());if(unitCost==null)continue;
                        double cost=unitCost*alternative.count();
                        if(cost<lowest){lowest=cost;chosen=alternative;}
                    }
                    if(chosen==null){complete=false;break;}
                    double count=chosen.count();known.get(chosen.key()).forEach((a,n)->{if(n>0)sum.merge(a,(limits.scaling()?limits.expand(n):n)*count,Double::sum);});
                }
                if(!complete||sum.isEmpty())continue;
                var result=new TreeMap<String,Integer>();sum.forEach((a,n)->result.put(a,limits.scaling()?limits.compress(n*decay/recipe.output().count()):(int)Math.min(10000,Math.max(1,Math.ceil(n*decay/recipe.output().count())))));
                result.values().removeIf(n->n<=0);
                if(result.isEmpty())continue;
                var limited=limiter.apply(result);
                var previous=next.get(recipe.output().key());
                if(previous==null||total(limited)<total(previous))next.put(recipe.output().key(),Map.copyOf(limited));
            }
            if(next.isEmpty())break;known.putAll(next);
            next.forEach((key,value)->{if(!value.isEmpty())costs.put(key,cost(value,limits));});
        }
        return Collections.unmodifiableMap(new TreeMap<>(known));
    }
    private static double cost(Map<String,Integer> values,tc4revival.aspect.recipe.InferenceLimits limits){
        return values.values().stream().mapToDouble(n->limits.scaling()?limits.expand(n):n).sum();
    }
    private static long total(Map<String,Integer> amounts){return amounts.values().stream().mapToLong(Integer::longValue).sum();}
    private InferenceEngine(){}
}
