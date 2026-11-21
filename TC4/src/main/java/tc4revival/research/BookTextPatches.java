package tc4revival.research;

import com.google.gson.*;
import java.util.*;
import java.util.function.Consumer;

/** Small, guarded literal diffs. Resource-pack translations take precedence over stale corrections. */
public record BookTextPatches(Set<String> languages, List<String> protect, List<Patch> patches) {
    public record Patch(Set<String> keys, String find, String replacement, int count) {
        public String apply(String key,String source,Consumer<String> warning) {
            if(!keys.contains(key))return source;
            int hits=0;
            for(int i=0;(i=source.indexOf(find,i))>=0;i+=find.length())hits++;
            if(hits==0)return source;
            if(hits!=count) {warning.accept("Book text diff for "+key+": expected "+count+" occurrences, found "+hits+"; skipped");return source;}
            return source.replace(find,replacement);
        }
    }
    private static List<String> strings(JsonArray array) {var values=new ArrayList<String>();for(var value:array)values.add(value.getAsString());return List.copyOf(values);}
    public static BookTextPatches decode(JsonObject json) {
        if(json.get("format").getAsInt()!=1)throw new IllegalArgumentException("Unsupported book text rules format");
        var languages=Set.copyOf(strings(json.getAsJsonArray("languages")));
        if(languages.isEmpty()||languages.stream().anyMatch(s->!ChineseBookTypography.chinese(s)))throw new IllegalArgumentException("Book typography rules only support zh_cn/zh_tw");
        var protect=json.has("protect")?strings(json.getAsJsonArray("protect")):List.<String>of();
        var patches=new ArrayList<Patch>();
        if(json.has("patches"))for(var entry:json.getAsJsonArray("patches")) {
            var p=entry.getAsJsonObject();var keys=Set.copyOf(strings(p.getAsJsonArray("keys")));
            String find=p.get("find").getAsString();int count=p.get("count").getAsInt();
            if(find.isEmpty()||keys.isEmpty()||count<1)throw new IllegalArgumentException("Book text diffs need keys, nonempty find and positive count");
            patches.add(new Patch(keys,find,p.get("replace").getAsString(),count));
        }
        return new BookTextPatches(languages,protect,List.copyOf(patches));
    }
    public String apply(String locale,String key,String source,Consumer<String> warning) {
        if(languages.contains(locale))for(var patch:patches)source=patch.apply(key,source,warning);
        return source;
    }
}
