package tc4revival.research;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.function.Predicate;

/** Resolve explicit page conditions on the server before synchronizing the book. */
public final class ResearchPageConditions {
    private ResearchPageConditions() {}

    public static JsonObject materialize(JsonObject source, Predicate<JsonObject> enabled) {
        var result = source.deepCopy();
        var counts = new HashMap<String, Integer>();
        for (var element : result.getAsJsonArray("pages")) {
            var entry = element.getAsJsonObject();
            var pages = new JsonArray();
            for (var value : entry.getAsJsonArray("pages")) {
                var page = value.getAsJsonObject();
                if (!page.has("conditions") || enabled.test(page)) {
                    page.remove("conditions");
                    if(page.has("group")) {
                        var group=page.getAsJsonObject("group");var entries=new JsonArray();
                        for(var member:group.getAsJsonArray("entries")) {
                            var row=member.getAsJsonObject();
                            if(!row.has("conditions")||enabled.test(row)){row.remove("conditions");entries.add(row);}
                        }
                        group.add("entries",entries);
                    }
                    pages.add(page);
                }
            }
            entry.add("pages", pages);
            counts.put(entry.get("key").getAsString(), pages.size());
        }
        for (var category : result.getAsJsonArray("categories"))
            for (var element : category.getAsJsonObject().getAsJsonArray("research")) {
                var entry = element.getAsJsonObject();
                var count = counts.get(entry.get("key").getAsString());
                if (count != null) entry.addProperty("pageCount", count);
            }
        return result;
    }
}
