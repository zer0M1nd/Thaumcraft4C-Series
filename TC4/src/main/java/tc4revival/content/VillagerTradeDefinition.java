package tc4revival.content;

import com.google.gson.*;
import java.util.*;

/** Pure data-pack schema; does not need Minecraft registries to validate numeric ranges. */
public record VillagerTradeDefinition(Map<Integer,List<Trade>> levels) {
    public record Stack(List<String> items,int min,int max,String nbt) {}
    public record Trade(Stack buy,Stack buyB,Stack sell,int maxUses,int xp,float multiplier,boolean rewardExp) {}
    public VillagerTradeDefinition { levels=Map.copyOf(levels); }
    private static int integer(JsonObject o,String key,int fallback,int min,int max) {
        int n=o.has(key)?o.get(key).getAsInt():fallback;
        if(n<min||n>max)throw new IllegalArgumentException(key+" outside "+min+".."+max);
        return n;
    }
    private static Stack stack(JsonObject o) {
        var ids=new ArrayList<String>();
        if(o.has("item"))ids.add(o.get("item").getAsString());
        else o.getAsJsonArray("items").forEach(x->ids.add(x.getAsString()));
        if(ids.isEmpty()||ids.stream().anyMatch(s->!s.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")))throw new IllegalArgumentException("Invalid trade item ID");
        int min=integer(o,"min",o.has("count")?o.get("count").getAsInt():1,1,64);
        int max=integer(o,"max",min,min,64);
        return new Stack(List.copyOf(ids),min,max,o.has("nbt")?o.get("nbt").getAsString():null);
    }
    public static VillagerTradeDefinition decode(JsonObject json) {
        if(integer(json,"format",0,1,1)!=1)throw new IllegalArgumentException("Unsupported trade format");
        var result=new LinkedHashMap<Integer,List<Trade>>();
        var levels=json.getAsJsonObject("levels");
        for(var key:levels.keySet())if(!key.matches("[1-5]"))throw new IllegalArgumentException("Invalid villager level: "+key);
        for(int level=1;level<=5;level++) {
            var trades=new ArrayList<Trade>();
            if(levels.has(""+level))for(var value:levels.getAsJsonArray(""+level)) {
                var o=value.getAsJsonObject();
                float multiplier=o.has("price_multiplier")?o.get("price_multiplier").getAsFloat():.05f;
                if(!Float.isFinite(multiplier)||multiplier<0||multiplier>1)throw new IllegalArgumentException("Invalid price multiplier");
                trades.add(new Trade(stack(o.getAsJsonObject("buy")),o.has("buy_b")?stack(o.getAsJsonObject("buy_b")):null,
                        stack(o.getAsJsonObject("sell")),integer(o,"max_uses",12,1,1000000),integer(o,"xp",1,0,1000000),multiplier,
                        !o.has("reward_exp")||o.get("reward_exp").getAsBoolean()));
            }
            result.put(level,List.copyOf(trades));
        }
        return new VillagerTradeDefinition(result);
    }
}
