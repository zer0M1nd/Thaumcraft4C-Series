package tc4revival.research;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import tc4revival.research.ResearchDefinition.Stack;

/** Ordered book display data. Recipe references do not imply implemented crafting recipes. */
public record ResearchPage(Type type, Optional<String> text, Optional<String> research,
        Optional<String> image, Map<String, Integer> aspects, Optional<Stack> output, Recipe recipe,
        String id, JsonElement component, JsonObject properties) {
    public enum Type { TEXT, TEXT_CONCEALED, IMAGE, CRUCIBLE_CRAFTING, ARCANE_CRAFTING, ASPECTS,
        NORMAL_CRAFTING, INFUSION_CRAFTING, COMPOUND_CRAFTING, INFUSION_ENCHANTMENT, SMELTING, CUSTOM,
        DIVIDER, ILLUSTRATION, ITEM }
    /** Java API convenience; data-pack components always supply an explicit stable id. */
    public ResearchPage(Type type, Optional<String> text, Optional<String> research, Optional<String> image,
            Map<String,Integer> aspects, Optional<Stack> output, Recipe recipe) {
        this(type,text,research,image,aspects,output,recipe,"runtime:"+java.util.UUID.randomUUID(),
                translation(text.orElse("")),new JsonObject());
    }
    public ResearchPage withId(String identity) {
        return new ResearchPage(type,text,research,image,aspects,output,recipe,identity,component,properties);
    }
    public boolean flow() { return type==Type.TEXT || type==Type.TEXT_CONCEALED || type==Type.DIVIDER
            || type==Type.ILLUSTRATION || type==Type.ITEM; }
    public boolean pageBreak() { return properties.has("page_break") && properties.get("page_break").getAsBoolean(); }
    public static JsonObject translation(String key) { var json=new JsonObject();json.addProperty("translate",key);return json; }
    @Override public JsonElement component() { return component.deepCopy(); }
    @Override public JsonObject properties() { return properties.deepCopy(); }
    public interface Recipe {}
    /** Named addon page, carried through server data reload and login synchronization. */
    public record Custom(net.minecraft.resources.ResourceLocation id, JsonObject data) implements Recipe {
        public Custom { java.util.Objects.requireNonNull(id); data = data.deepCopy(); }
        @Override public JsonObject data() { return data.deepCopy(); }
    }
    public record Nbt(String type, String value) implements Recipe {}
    public record Ore(List<String> names, List<Recipe> elements) implements Recipe {
        public Ore { names = List.copyOf(names); elements = List.copyOf(elements); }
    }
    public record Empty() implements Recipe {}
    public record Reference(List<String> keys, String legacyClass) implements Recipe {
        public Reference { keys = List.copyOf(keys); if (keys.isEmpty()) throw new IllegalArgumentException("Empty recipe reference"); }
        public String displayKey(long elapsedMillis) { return keys.get((int)Math.floorMod(elapsedMillis/5000,keys.size())); }
    }
    public record Item(Stack stack) implements Recipe {}
    /** Empty elements are significant shaped-grid cells. */
    public record Sequence(List<Recipe> elements) implements Recipe {
        public Sequence { elements = List.copyOf(elements); }
    }
    public record Literal(Object value) implements Recipe {
        public Literal {
            if (!(value instanceof String || value instanceof Number || value instanceof Boolean))
                throw new IllegalArgumentException("Non-scalar recipe literal");
        }
    }
    public record AspectAmounts(Map<String, Integer> aspects) implements Recipe {
        public AspectAmounts { aspects = ordered(aspects); }
    }
    public record Arcane(String research, int width, int height, boolean mirrored, Stack output,
            Sequence ingredients, Map<String, Integer> aspects) implements Recipe {
        public Arcane {
            aspects = ordered(aspects);
            if (width < 1 || height < 1 || width > 3 || height > 3 || width * height != ingredients.elements().size())
                throw new IllegalArgumentException("Invalid arcane display grid");
        }
    }
    public record RunicExample(String research, Stack input, Stack output, Sequence ingredients,
            Map<String, Integer> aspects, int instability) implements Recipe {
        public RunicExample { aspects = ordered(aspects); }
    }
    public ResearchPage {
        aspects = ordered(aspects);
        if(id==null || id.isBlank())throw new IllegalArgumentException("Book component requires an id");
        component=component==null?new com.google.gson.JsonPrimitive(""):component.deepCopy();
        properties=properties.deepCopy();
    }
    static Map<String, Integer> ordered(Map<String, Integer> source) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
    static Map<String, Integer> amounts(JsonElement value) {
        var result = new LinkedHashMap<String, Integer>();
        if (value.isJsonArray()) for (var element : value.getAsJsonArray()) {
            var row = element.getAsJsonObject();
            if (result.putIfAbsent(row.get("aspect").getAsString(), row.get("amount").getAsInt()) != null)
                throw new IllegalArgumentException("Duplicate page aspect");
        } else value.getAsJsonObject().entrySet().forEach(entry -> result.put(entry.getKey(), entry.getValue().getAsInt()));
        return result;
    }
    static Optional<String> string(JsonObject value, String field) {
        return Optional.ofNullable(value.get(field)).filter(e -> !e.isJsonNull()).map(JsonElement::getAsString);
    }
    public static ResearchPage decode(JsonObject value) {
        String name = value.get("type").getAsString();
        String id=string(value,"id").filter(s->!s.isBlank()).orElseThrow(()->new IllegalArgumentException("Book component requires an id"));
        var properties=value.deepCopy();
        for(String field:List.of("id","type","text","research","image","aspects","recipeOutput","recipe","data"))properties.remove(field);
        for(String field:List.of("width","height","size","texture_width","texture_height"))if(properties.has(field)&&(properties.get(field).getAsInt()<1||properties.get(field).getAsInt()>4096))
            throw new IllegalArgumentException("Invalid book "+field);
        if(properties.has("scale")&&(!Float.isFinite(properties.get("scale").getAsFloat())||properties.get("scale").getAsFloat()<=0))
            throw new IllegalArgumentException("Invalid book scale");
        if(name.equals("ITEM")&&properties.has("items")&&(properties.getAsJsonArray("items").isEmpty()||properties.getAsJsonArray("items").size()>64))
            throw new IllegalArgumentException("Invalid book item row");
        var content=value.has("text")?value.get("text"):new com.google.gson.JsonPrimitive("");
        Optional<String> key=content.isJsonObject()?string(content.getAsJsonObject(),"translate"):Optional.empty();
        if (name.contains(":")) return new ResearchPage(Type.CUSTOM, key, string(value, "research"),
                string(value, "image"), Map.of(), Optional.empty(), new Custom(net.minecraft.resources.ResourceLocation.parse(name),
                value.has("data") ? value.getAsJsonObject("data") : new JsonObject()),id,content,properties);
        return new ResearchPage(Type.valueOf(name), key,
                string(value, "research"), string(value, "image"), amounts(value.has("aspects")?value.get("aspects"):new JsonObject()),
                Optional.ofNullable(value.get("recipeOutput")).filter(e -> !e.isJsonNull()).map(ResearchCatalog::stack),
                recipe(value.get("recipe")),id,content,properties);
    }
    static Recipe recipe(JsonElement element) {
        if (element == null || element.isJsonNull()) return new Empty();
        var value = element.getAsJsonObject();
        return switch (value.get("kind").getAsString()) {
            case "nbt" -> new Nbt(value.get("type").getAsString(), value.get("value").getAsString());
            case "ore" -> {
                var names = new ArrayList<String>(); value.getAsJsonArray("names").forEach(e -> names.add(e.getAsString()));
                var entries = new ArrayList<Recipe>(); value.getAsJsonArray("elements").forEach(e -> entries.add(recipe(e)));
                yield new Ore(names, entries);
            }
            case "reference" -> {
                var keys = new ArrayList<String>(); value.getAsJsonArray("keys").forEach(e -> keys.add(e.getAsString()));
                yield new Reference(keys, value.get("class").getAsString());
            }
            case "stack" -> new Item(ResearchCatalog.stack(value.get("stack")));
            case "sequence" -> {
                var entries = new ArrayList<Recipe>(); value.getAsJsonArray("elements").forEach(e -> entries.add(recipe(e)));
                yield new Sequence(entries);
            }
            case "literal" -> {
                var scalar = value.getAsJsonPrimitive("value");
                yield new Literal(scalar.isBoolean() ? scalar.getAsBoolean() : scalar.isNumber() ? scalar.getAsBigDecimal() : scalar.getAsString());
            }
            case "aspects" -> new AspectAmounts(amounts(value.has("aspects")?value.get("aspects"):new JsonObject()));
            case "inline_arcane" -> new Arcane(value.get("research").getAsString(), value.get("width").getAsInt(),
                    value.get("height").getAsInt(), value.get("mirrored").getAsBoolean(), ResearchCatalog.stack(value.get("output")),
                    (Sequence) recipe(value.get("ingredients")), amounts(value.has("aspects")?value.get("aspects"):new JsonObject()));
            case "runic_augmentation_example" -> new RunicExample(value.get("research").getAsString(),
                    ResearchCatalog.stack(value.get("input")), ResearchCatalog.stack(value.get("output")),
                    (Sequence) recipe(value.get("ingredients")), amounts(value.has("aspects")?value.get("aspects"):new JsonObject()), value.get("instability").getAsInt());
            default -> throw new IllegalArgumentException("Unknown research page recipe kind " + value.get("kind"));
        };
    }
}
