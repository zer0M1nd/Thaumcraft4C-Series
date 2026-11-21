package tc4revival.research;

import com.google.gson.*;
import java.util.*;

/** Ordered, non-destructive additions. Resource ids resolve equal-priority ties deterministically. */
public final class ResearchPageAdditions {
    private ResearchPageAdditions() {}
    public static JsonObject apply(JsonObject source) {
        var result=source.deepCopy();
        if(!result.has("additions"))return result;
        var entries=new LinkedHashMap<String,JsonObject>();
        result.getAsJsonArray("pages").forEach(e->entries.put(e.getAsJsonObject().get("key").getAsString(),e.getAsJsonObject()));
        var additions=new ArrayList<JsonObject>();
        result.getAsJsonArray("additions").forEach(e->additions.add(e.getAsJsonObject()));
        additions.sort(Comparator.<JsonObject>comparingInt(e->e.has("priority")?e.get("priority").getAsInt():0)
                .thenComparing(e->e.get("id").getAsString()));
        var ids=new HashSet<String>();
        var starts=new HashMap<String,JsonArray>();
        var ends=new HashMap<String,JsonArray>();
        for(var addition:additions) {
            String id=addition.get("id").getAsString(),key=addition.get("research").getAsString();
            if(!ids.add(id))throw new IllegalArgumentException("Duplicate book addition "+id);
            // Optional integrations can target an absent research; they must opt in explicitly.
            if(!entries.containsKey(key)) {
                if(addition.has("optional")&&addition.get("optional").getAsBoolean())continue;
                throw new IllegalArgumentException("Unknown book addition target "+key);
            }
            String position=addition.get("position").getAsString();
            var target=switch(position) {case "start"->starts;case "end"->ends;
                default->throw new IllegalArgumentException("Invalid book addition position "+position);};
            var content=target.computeIfAbsent(key,k->new JsonArray());
            for(var element:addition.getAsJsonArray("components")) {
                var component=element.getAsJsonObject().deepCopy();
                if(addition.has("conditions")) {
                    var conditions=new JsonArray();conditions.addAll(addition.getAsJsonArray("conditions"));
                    if(component.has("conditions"))conditions.addAll(component.getAsJsonArray("conditions"));
                    component.add("conditions",conditions);
                }
                content.add(component);
            }
        }
        entries.forEach((key,entry)->{
            var content=new JsonArray();
            if(starts.containsKey(key))content.addAll(starts.get(key));
            var original=entry.getAsJsonArray("pages");
            // A content prepend participates in the first original flow. Explicit added pages keep their break.
            if(!content.isEmpty()&&!original.isEmpty())original.get(0).getAsJsonObject().remove("page_break");
            content.addAll(original);
            if(ends.containsKey(key))content.addAll(ends.get(key));
            entry.add("pages",content);
        });
        result.remove("additions");
        recount(result);
        return result;
    }
    public static void recount(JsonObject json) {
        var counts=new HashMap<String,Integer>();
        json.getAsJsonArray("pages").forEach(e->counts.put(e.getAsJsonObject().get("key").getAsString(),e.getAsJsonObject().getAsJsonArray("pages").size()));
        json.getAsJsonArray("categories").forEach(c->c.getAsJsonObject().getAsJsonArray("research").forEach(e->{
            var row=e.getAsJsonObject();var count=counts.get(row.get("key").getAsString());
            if(count!=null)row.addProperty("pageCount",count);
        }));
    }
}
