package tc4revival.research;

import com.google.gson.JsonArray;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/** Static pages plus the old player's conditional and discovered-aspect pages. */
public  class ResearchPages {
    private final Map<String, List<ResearchPage>> entries;
    private ResearchPages(Map<String, List<ResearchPage>> entries) { this.entries = Collections.unmodifiableMap(entries); }
    public Map<String, List<ResearchPage>> entries() { return entries; }
    public List<ResearchPage> require(String key) {
        var pages = entries.get(key);
        if (pages == null) throw new IllegalArgumentException("Missing research pages " + key);
        return pages;
    }
    public static ResearchPages decode(JsonArray source) {
        var entries = new LinkedHashMap<String, List<ResearchPage>>();
        var ids = new java.util.HashSet<String>();
        for (var element : source) {
            var value = element.getAsJsonObject();
            var pages = new ArrayList<ResearchPage>();
            value.getAsJsonArray("pages").forEach(page -> {
                var decoded=ResearchPage.decode(page.getAsJsonObject());
                BookComponentIds.add(ids,decoded.id());
                if(decoded.type()==ResearchPage.Type.TEXT||decoded.type()==ResearchPage.Type.TEXT_CONCEALED)
                    BookComponentIds.text(ids,decoded.component(),decoded.id()+"/text");
                pages.add(decoded);
            });
            if (entries.putIfAbsent(value.get("key").getAsString(), List.copyOf(pages)) != null)
                throw new IllegalArgumentException("Duplicate research pages");
        }
        return new ResearchPages(entries);
    }
    public void validate(ResearchCatalog catalog) {
        for (var key : entries.keySet()) catalog.require(key);
        for (var entry : catalog.all()) {
            if (entry.mirror()) {
                if (!entries.getOrDefault(entry.key(), List.of()).isEmpty())
                    throw new IllegalArgumentException("Research mirror cannot own pages " + entry.key());
            } else if (require(entry.key()).size() != entry.display().pageCount())
                throw new IllegalArgumentException("Research page count mismatch " + entry.key());
        }
    }
    /** Caller supplies discovered tags including zero-pool aspects; sorting is by tag, not translated name. */
    public List<ResearchPage> visible(String key, Predicate<String> completed, Map<String, Integer> discovered) {
        var result = new ArrayList<ResearchPage>();
        for (var page : require(key)) if (BookVisibility.visible(page.properties(),completed)
                && (page.type()!=ResearchPage.Type.TEXT_CONCEALED || page.research().map(completed::test).orElse(true))) {
            var properties=page.properties();
            if(properties.has("group")&&page.recipe() instanceof ResearchPage.Custom custom) {
                var group=properties.getAsJsonObject("group");int size=group.get("size").getAsInt();
                if(size<1||size>64)throw new IllegalArgumentException("Invalid book group size");
                var values=group.getAsJsonArray("entries").asList().stream().map(com.google.gson.JsonElement::getAsJsonObject)
                        .filter(e->BookVisibility.visible(e,completed)).toList();
                for(int start=0;start<values.size();start+=size) {
                    var data=custom.data();var selected=new JsonArray();
                    for(int i=start;i<Math.min(start+size,values.size());i++)selected.add(values.get(i).get("value"));
                    data.add(group.get("field").getAsString(),selected);
                    result.add(new ResearchPage(page.type(),page.text(),page.research(),page.image(),page.aspects(),page.output(),
                            new ResearchPage.Custom(custom.id(),data),page.id()+"/"+values.get(start).get("id").getAsString(),page.component(),properties));
                }
            }else result.add(page);
        }
        if (key.equals("ASPECTS")) {
            var sorted = discovered.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList();
            for (int start = 0; start < sorted.size(); start += 4) {
                var amounts = new LinkedHashMap<String, Integer>();
                for (int i = start; i < Math.min(start + 4, sorted.size()); i++) amounts.put(sorted.get(i).getKey(), sorted.get(i).getValue());
                result.add(new ResearchPage(ResearchPage.Type.ASPECTS, Optional.empty(), Optional.empty(), Optional.empty(),
                        amounts, Optional.empty(), new ResearchPage.Empty()).withId("thaumcraft:aspects/"+sorted.get(start).getKey()));
            }
        }
        return thaumcraft.api.research.ResearchRecipeViews.visible(key,result);
    }
    public static ResearchPages bundled() { var snapshot=ResearchDefinitions.display();return snapshot==null?Bundled.VALUE:snapshot.pages(); }
    private static  class Bundled {
        static final ResearchPages VALUE = read();
        private static ResearchPages read() {
            try (var reader = new java.io.InputStreamReader(java.util.Objects.requireNonNull(ResearchPages.class
                    .getResourceAsStream("/data/thaumcraft/research_pages/legacy.json")), java.nio.charset.StandardCharsets.UTF_8)) {
                var root = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                if (root.get("format").getAsInt() != 1) throw new IllegalArgumentException("Unsupported research pages format");
                var result = decode(root.getAsJsonArray("entries")); result.validate(ResearchCatalogs.bundled()); return result;
            } catch (java.io.IOException failure) { throw new IllegalStateException("Missing research pages", failure); }
        }
    }
}
