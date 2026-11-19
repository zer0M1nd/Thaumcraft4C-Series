package tc4revival.aspect.item;

import com.google.gson.*;
import java.util.*;
import tc4revival.aspect.*;

/** Explicit static block-state aspects; multiple matching base lists merge by maximum. */
public class BlockAspectTable {
    public record Rule(String block,Map<String,String> properties,AspectAmounts values){}
    private final Map<String,List<Rule>> rules=new HashMap<>();
    public BlockAspectTable(Collection<JsonElement> files,AspectCatalog catalog){
        this(files,catalog,id->true);
    }
    public BlockAspectTable(Collection<JsonElement> files,AspectCatalog catalog,java.util.function.Predicate<String> exists){
        this(files,catalog,exists,AspectModConditions::loaded);
    }
    public BlockAspectTable(Collection<JsonElement> files,AspectCatalog catalog,java.util.function.Predicate<String> exists,java.util.function.Predicate<String> modLoaded){
        for(var json:files){var root=json.getAsJsonObject();if(root.get("format").getAsInt()!=1)throw new IllegalArgumentException("Unsupported block aspect format");
            if(!AspectModConditions.matches(root,modLoaded))continue;
            for(var entry:root.getAsJsonArray("entries")){var row=entry.getAsJsonObject();String id=row.get("block").getAsString();
                if(!AspectModConditions.matches(row,modLoaded))continue;
                if(!id.matches("[a-z0-9_.-]+:[a-z0-9/._-]+"))throw new IllegalArgumentException("Invalid block ID");
                if(!exists.test(id))continue;
                var props=new LinkedHashMap<String,String>();if(row.has("properties"))row.getAsJsonObject("properties").entrySet().forEach(e->props.put(e.getKey(),e.getValue().getAsString()));
                var values=new AspectAmounts();row.getAsJsonObject("aspects").entrySet().forEach(e->{if(catalog.find(e.getKey()).isEmpty())return;int n=e.getValue().getAsBigDecimal().intValueExact();if(n<0||n>10000)throw new IllegalArgumentException("Invalid aspect amount");values.add(catalog.require(e.getKey()),n);});
                rules.computeIfAbsent(id,k->new ArrayList<>()).add(new Rule(id,Map.copyOf(props),values));
            }
        }
    }
    public List<Rule> rules(){return rules.values().stream().flatMap(List::stream).toList();}
    public Optional<AspectAmounts> resolve(String id,Map<String,String> state){
        AspectAmounts result=null;for(var rule:rules.getOrDefault(id,List.of()))if(rule.properties().entrySet().stream().allMatch(e->e.getValue().equals(state.get(e.getKey())))){
            if(result==null)result=new AspectAmounts();result.merge(rule.values());
        }
        return Optional.ofNullable(result);
    }
}
