package tc4revival.aspect.sync;

import java.util.*;

/** Only authored differences from installed mods cross the network. Empty strings are removal markers. */
public final class AspectInputDelta {
    public static final String SETTINGS="inference_settings";
    public static boolean generated(String key){
        return key.equals("crafting_aspect_inputs/thaumcraft:runtime")
                || Set.of("item_aspects/thaumcraft:advanced_inference","block_aspects/thaumcraft:advanced_inference",
                "entity_aspects/thaumcraft:advanced_inference").contains(key);
    }
    public static Map<String,String> between(Map<String,String> bundled,Map<String,String> effective){
        var result=new TreeMap<String,String>();
        effective.forEach((key,value)->{if(!generated(key)&&!Objects.equals(value,bundled.get(key)))result.put(key,value);});
        bundled.keySet().forEach(key->{if(!generated(key)&&!effective.containsKey(key))result.put(key,"");});
        return result;
    }
    public static Map<String,String> apply(Map<String,String> bundled,Map<String,String> delta){
        var result=new TreeMap<>(bundled);result.keySet().removeIf(AspectInputDelta::generated);
        delta.forEach((key,value)->{
            if(key.equals(SETTINGS))return;
            AspectDataSet.validateKey(key);
            if(generated(key))throw new IllegalArgumentException("Generated aspect data cannot be synchronized");
            if(value.isEmpty())result.remove(key);else result.put(key,value);
        });
        return new AspectDataSet(result).files();
    }
    private AspectInputDelta(){}
}
