package tc4revival.aspect.editor;

import com.google.gson.*;
import java.util.*;
import java.util.function.Predicate;
import tc4revival.aspect.*;

/** Simple registry-wide overrides. An empty map is a deliberate, authoritative definition. */
public class EditorDefinitions {
    public static final int MAX_ENTRIES=16384, MAX_AMOUNT=10000;
    public static final EditorDefinitions EMPTY=new EditorDefinitions(Map.of(),0);
    private final Map<String,Map<String,Integer>> entries;
    private final long order;
    public EditorDefinitions(Map<String,Map<String,Integer>> entries,long order) {
        if(entries.size()>MAX_ENTRIES||order<0)throw new IllegalArgumentException("Too many edits or invalid order");
        var copy=new LinkedHashMap<String,Map<String,Integer>>();
        entries.forEach((key,amounts)->{
            validateKey(key);
            if(amounts.size()>1024)throw new IllegalArgumentException("Too many aspects");
            var values=new LinkedHashMap<String,Integer>();
            amounts.forEach((aspect,n)->{
                if(aspect==null||!aspect.matches("(?:[a-z0-9_.-]+:)?[a-z0-9_/.-]+")||aspect.length()>64||n==null||n<0||n>MAX_AMOUNT)
                    throw new IllegalArgumentException("Invalid aspect quantity");
                if(n>0)values.put(aspect,n);
            });
            copy.put(key,Collections.unmodifiableMap(values));
        });
        this.entries=Collections.unmodifiableMap(copy);this.order=order;
    }
    public Map<String,Map<String,Integer>> entries(){return entries;}
    public long order(){return order;}
    public Optional<Map<String,Integer>> get(String key){return Optional.ofNullable(entries.get(key));}
    public Optional<Map<String,Integer>> tag(String kind,Predicate<String> contains){
        Map<String,Integer> found=null;
        for(var e:entries.entrySet())if(e.getKey().startsWith(kind+"_tag:")&&contains.test(e.getKey().substring(kind.length()+5)))found=e.getValue();
        return Optional.ofNullable(found);
    }
    public Optional<Map<String,Integer>> resolve(String kind,String id,Predicate<String> tags){
        var exact=get(kind+":"+id);return exact.isPresent()?exact:tag(kind,tags);
    }
    public void validateAspects(AspectCatalog catalog){entries.values().forEach(v->v.keySet().forEach(catalog::require));}
    public static AspectAmounts amounts(Map<String,Integer> values,AspectCatalog catalog){
        var result=new AspectAmounts();values.forEach((a,n)->catalog.find(a).ifPresent(aspect->result.add(aspect,n)));return result;
    }
    /** Loading existing data packs is tolerant; submitted editor changes still use strict validation. */
    public EditorDefinitions filtered(AspectCatalog catalog,java.util.function.BiPredicate<String,String> exists){
        var result=new LinkedHashMap<String,Map<String,Integer>>();
        entries.forEach((key,values)->{
            int split=key.indexOf(':');if(!exists.test(key.substring(0,split),key.substring(split+1)))return;
            var known=new LinkedHashMap<String,Integer>();values.forEach((aspect,n)->{if(catalog.find(aspect).isPresent())known.put(aspect,n);});
            result.put(key,known);
        });
        return new EditorDefinitions(result,order);
    }
    public static Map<String,Integer> values(AspectAmounts amounts){
        var values=new LinkedHashMap<String,Integer>();amounts.view().forEach((a,n)->{if(n>0)values.put(a.tag(),n);});return values;
    }
    public static void validateKey(String key){
        if(key==null||key.length()>300||!key.matches("(?:item|block|entity|item_tag|block_tag):[a-z0-9_.-]+:[a-z0-9_/.-]+"))
            throw new IllegalArgumentException("Invalid editor target");
    }
    public JsonObject encode(){
        var root=new JsonObject();root.addProperty("format",1);root.addProperty("order",order);
        var rows=new JsonObject();entries.forEach((key,values)->{var row=new JsonObject();values.forEach(row::addProperty);rows.add(key,row);});root.add("entries",rows);return root;
    }
    public static EditorDefinitions decode(JsonElement input){
        var root=input.getAsJsonObject();if(root.get("format").getAsInt()!=1)throw new IllegalArgumentException("Unsupported editor format");
        var result=new LinkedHashMap<String,Map<String,Integer>>();
        root.getAsJsonObject("entries").entrySet().forEach(e->{var values=new LinkedHashMap<String,Integer>();e.getValue().getAsJsonObject().entrySet().forEach(v->values.put(v.getKey(),v.getValue().getAsBigDecimal().intValueExact()));result.put(e.getKey(),values);});
        return new EditorDefinitions(result,root.get("order").getAsBigDecimal().longValueExact());
    }
    public static EditorDefinitions merge(Collection<JsonElement> files){
        var parsed=files.stream().map(EditorDefinitions::decode).sorted(Comparator.comparingLong(EditorDefinitions::order)).toList();
        var result=new LinkedHashMap<String,Map<String,Integer>>();long order=0;
        for(var file:parsed){order=Math.max(order,file.order());file.entries.forEach((k,v)->{result.remove(k);result.put(k,v);});}
        return new EditorDefinitions(result,order);
    }
}
