package tc4revival.research;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.recipe.RecipeAspectData.Key;
import tc4revival.research.ResearchDefinition.*;

/** Stable category order and declarative research insertion order. */
public  class ResearchCatalog {
    public record Category(String key, String icon, String background, List<ResearchDefinition> research, String modId) {
        public Category(String key, String icon, String background, List<ResearchDefinition> research) {
            this(key, icon, background, research, ResearchOrigins.namespace(icon));
        }
        public Category {
            research = List.copyOf(research);
            if (modId == null || !modId.matches("[a-z0-9_.-]+"))
                throw new IllegalArgumentException("Invalid research category source modid");
        }
    }
    private final List<Category> categories;
    private final Map<String, ResearchDefinition> definitions;
    public ResearchCatalog(List<Category> categories, AspectCatalog aspects) {
        var legacyOrder = List.of("BASICS", "THAUMATURGY", "ALCHEMY", "ARTIFICE", "GOLEMANCY", "ELDRITCH");
        // Stable sorting keeps addon-defined order, after every base category.
        this.categories = categories.stream().sorted(java.util.Comparator.comparingInt(c -> {
            int index = legacyOrder.indexOf(c.key()); return index < 0 ? legacyOrder.size() : index;
        })).toList();
        var keys = new java.util.HashSet<String>();
        var entries = new LinkedHashMap<String, ResearchDefinition>();
        for (var category : categories) {
            if (!keys.add(category.key())) throw new IllegalArgumentException("Duplicate research category " + category.key());
            for (var entry : category.research()) {
                if (!entry.category().equals(category.key())) throw new IllegalArgumentException("Mismatched research category");
                if (entries.putIfAbsent(entry.key(), entry) != null) throw new IllegalArgumentException("Duplicate research " + entry.key());
                entry.aspects().keySet().forEach(aspects::require);
                entry.noteSeeds().inner().forEach(aspects::require);
                entry.noteSeeds().negative().forEach(aspects::require);
                entry.noteLayout().ifPresent(layout -> layout.cells().values().forEach(cell -> {
                    if (cell.aspect() != null) aspects.require(cell.aspect());
                }));
                entry.triggers().aspects().ifPresent(tags -> tags.forEach(aspects::require));
            }
        }
        definitions = Collections.unmodifiableMap(entries);
        for(var entry:all()) {
            if(entry.mirror())original(entry.key()); // Reject cycles and dangling mirrors at data load time.
            entry.reference().parentMirrors().forEach((parent,mirror)->{
                var target=require(mirror);
                if(!entry.parents().contains(parent) && !entry.parentsAny().contains(parent))
                    throw new IllegalArgumentException("Display link does not name a parent: "+entry.key()+" / "+parent);
                if(!target.category().equals(entry.category()) || !original(parent).key().equals(original(mirror).key()))
                    throw new IllegalArgumentException("Invalid parent display target: "+entry.key()+" / "+mirror);
            });
        }
    }
    public List<Category> categories() { return thaumcraft.api.research.ResearchPresentation.order(categories); }
    public List<ResearchDefinition> all() { return List.copyOf(definitions.values()); }
    public Optional<ResearchDefinition> find(String key) { return Optional.ofNullable(definitions.get(key)); }
    public ResearchDefinition require(String key) {
        return find(key).orElseThrow(() -> new IllegalArgumentException("Unknown research " + key));
    }
    /** Mirrors have independent layout identities but a single gameplay identity. */
    public ResearchDefinition original(String key) {
        var entry=require(key);var visited=new java.util.HashSet<String>();
        while(entry.mirror()) {
            if(!visited.add(entry.key()))throw new IllegalArgumentException("Cyclic research mirror: "+key);
            entry=require(entry.reference().target().orElseThrow());
        }
        return entry;
    }
    public static ResearchCatalog decode(JsonArray source, AspectCatalog aspects) {
        var categories = new ArrayList<Category>();
        for (var element : source) {
            categories.add(decodeCategory(element.getAsJsonObject()));
        }
        return new ResearchCatalog(categories, aspects);
    }
    static Category decodeCategory(JsonObject category) {
        var entries=new ArrayList<ResearchDefinition>();
        for(var value:category.getAsJsonArray("research"))entries.add(decodeEntry(value.getAsJsonObject()));
        return new Category(string(category,"key"),string(category,"icon"),string(category,"background"),entries,
                ResearchOrigins.modId(category, ResearchOrigins.namespace(string(category,"icon"))));
    }
    private static ResearchDefinition decodeEntry(JsonObject value) {
        var flags = EnumSet.noneOf(Flag.class);
        String[] fields = {"isSpecial", "isSecondary", "isRound", "isStub", "isVirtual", "isConcealed", "isHidden", "isLost", "isAutoUnlock", "isTutorial"};
        for (int i = 0; i < fields.length; i++) if (value.has(fields[i]) && value.get(fields[i]).getAsBoolean()) flags.add(Flag.values()[i]);
        var amounts = new LinkedHashMap<String, Integer>();
        var quantities = value.has("aspects")?value.get("aspects"):new JsonObject();
        if (quantities.isJsonArray()) {
            for (var quantity : quantities.getAsJsonArray()) {
                var row = quantity.getAsJsonObject();
                if (amounts.putIfAbsent(string(row, "aspect"), row.get("amount").getAsInt()) != null) {
                    throw new IllegalArgumentException("Duplicate research aspect");
                }
            }
        } else {
            // The read-only legacy observer uses ordered object members; runtime DataGen uses arrays.
            quantities.getAsJsonObject().entrySet().forEach(entry -> amounts.put(entry.getKey(), entry.getValue().getAsInt()));
        }
        var display = new Display(value.get("displayColumn").getAsInt(), value.get("displayRow").getAsInt(),
                optional(value, "icon_resource").map(JsonElement::getAsString), optional(value, "icon_item").map(ResearchCatalog::stack),
                value.get("pageCount").getAsInt(), array(value,"icons",icon->{
                    var json=icon.getAsJsonObject();
                    return new Icon(optional(json,"resource").map(JsonElement::getAsString),
                            optional(json,"item").map(item->stack(item.isJsonObject()?item:icon)));
                }).orElse(List.of()));
        var triggers = new Triggers(array(value, "itemTriggers", ResearchCatalog::stack),
                array(value, "entityTriggers", JsonElement::getAsString), array(value, "aspectTriggers", JsonElement::getAsString));
        return new ResearchDefinition(string(value, "key"), string(value, "category"), amounts,
                array(value, "parents", JsonElement::getAsString).orElse(List.of()),
                array(value, "parentsHidden", JsonElement::getAsString).orElse(List.of()),
                array(value, "siblings", JsonElement::getAsString).orElse(List.of()), flags,
                net.minecraft.util.GsonHelper.getAsInt(value,"complexity",1), net.minecraft.util.GsonHelper.getAsInt(value,"warp",0), display, triggers,
                array(value,"parentsAny",JsonElement::getAsString).orElse(List.of()),
                new NoteSeeds(array(value,"innerAspects",JsonElement::getAsString).orElse(List.of()),
                        array(value,"negativeAspects",JsonElement::getAsString).orElse(List.of())),
                new Reference(optional(value,"mirrorOf").map(JsonElement::getAsString), stringMap(value,"parentMirrors")),
                value.has("noteLayout") ? Optional.of(ResearchNoteLayout.decode(value.get("noteLayout"))) : Optional.empty(),
                ResearchOrigins.modId(value, ResearchOrigins.namespace(string(value,"key"))),
                array(value,"suggestedResearch",JsonElement::getAsString).orElse(List.of()));
    }
    private static Map<String,String> stringMap(JsonObject value,String key) {
        var result=new LinkedHashMap<String,String>();
        if(value.has(key))value.getAsJsonObject(key).entrySet().forEach(e->result.put(e.getKey(),e.getValue().getAsString()));
        return result;
    }
    static Stack stack(JsonElement source) {
        var value = source.getAsJsonObject();
        return new Stack(new Key(string(value, "item"), net.minecraft.util.GsonHelper.getAsInt(value,"metadata",0)), net.minecraft.util.GsonHelper.getAsInt(value,"count",1),
                net.minecraft.util.GsonHelper.getAsBoolean(value,"damageable",false), optional(value, "nbt").map(JsonElement::getAsString),
                optional(value, "ore").map(JsonElement::getAsString));
    }
    private static String string(JsonObject value, String field) { return value.get(field).getAsString(); }
    private static Optional<JsonElement> optional(JsonObject value, String field) {
        return Optional.ofNullable(value.get(field)).filter(element -> !element.isJsonNull());
    }
    private static <T> Optional<List<T>> array(JsonObject value, String field, Function<JsonElement, T> decode) {
        return optional(value, field).map(element -> {
            var entries = new ArrayList<T>();
            element.getAsJsonArray().forEach(item -> entries.add(decode.apply(item)));
            return List.copyOf(entries);
        });
    }
}
