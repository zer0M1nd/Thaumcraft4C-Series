package tc4revival.research;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tc4revival.research.ResearchPage.*;

/** Recipe display definitions with legacy identities; this is not a crafting registry. */
public  class ResearchPageRecipes {
    public sealed interface Definition permits Crafting, Infusion, Crucible, Enchanting, Compound {}
    public record Crafting(String kind, Recipe output, Sequence ingredients, String research,
            Map<String, Integer> aspects, int width, int height, boolean mirrored, boolean matchesInputNbt) implements Definition {
        public Crafting {
            aspects = ResearchPage.ordered(aspects);
            if (width < 0 || height < 0 || width > 3 || height > 3 || (width == 0) != (height == 0)
                    || (width > 0 && width * height != ingredients.elements().size()))
                throw new IllegalArgumentException("Invalid named crafting grid");
        }
    }
    public record Infusion(String research, Recipe input, Recipe output, Sequence ingredients,
            Map<String, Integer> aspects, int instability) implements Definition {
        public Infusion { aspects = ResearchPage.ordered(aspects); }
    }
    public record Crucible(String research, Recipe catalyst, Recipe output, Map<String, Integer> aspects) implements Definition {
        public Crucible { aspects = ResearchPage.ordered(aspects); }
    }
    public record Enchanting(String research, int enchantmentId, String enchantmentName, int maxLevel,
            int baseXp, int instability, Map<String, Integer> aspects, Sequence ingredients) implements Definition {
        public Enchanting { aspects = ResearchPage.ordered(aspects); }
    }
    public record Compound(Map<String, Integer> aspects, int width, int height, int depth, Sequence blocks) implements Definition {
        public Compound {
            aspects = ResearchPage.ordered(aspects);
            if (width < 1 || height < 1 || depth < 1 || (long) width * height * depth != blocks.elements().size())
                throw new IllegalArgumentException("Invalid compound display dimensions");
        }
    }
    public record Entry(String key, String legacyClass, Definition definition) {}
    private final Map<String, Entry> entries;
    private ResearchPageRecipes(Map<String, Entry> entries) { this.entries = Collections.unmodifiableMap(entries); }
    public Map<String, Entry> entries() { return entries; }
    public Entry require(String key) {
        var result = entries.get(key);
        if (result == null) throw new IllegalArgumentException("Missing page recipe " + key);
        return thaumcraft.api.research.ResearchRecipeViews.recipe(result);
    }
    public static ResearchPageRecipes decode(JsonArray source) {
        var entries = new LinkedHashMap<String, Entry>();
        for (var element : source) {
            var row = element.getAsJsonObject(); String key = row.get("key").getAsString();
            var entry = new Entry(key, row.get("class").getAsString(), definition(row.getAsJsonObject("data")));
            if (entries.putIfAbsent(key, entry) != null) throw new IllegalArgumentException("Duplicate page recipe " + key);
        }
        return new ResearchPageRecipes(entries);
    }
    private static Definition definition(JsonObject data) {
        String kind = data.get("kind").getAsString();
        if (kind.equals("compound")) {
            var values = ((Sequence) ResearchPage.recipe(data.get("structure"))).elements();
            if (values.size() != 5) throw new IllegalArgumentException("Invalid compound descriptor");
            return new Compound(((AspectAmounts) values.get(0)).aspects(), dimension(values.get(1)), dimension(values.get(2)),
                    dimension(values.get(3)), (Sequence) values.get(4));
        }
        var costs = data.has("aspects") ? ResearchPage.amounts(data.get("aspects")) : Map.<String, Integer>of();
        String research = data.has("research") ? data.get("research").getAsString() : "";
        var output = ResearchPage.recipe(data.get("output"));
        if (kind.equals("crucible")) return new Crucible(research, ResearchPage.recipe(data.get("catalyst")), output, costs);
        var ingredients = (Sequence) ResearchPage.recipe(data.get("ingredients"));
        if (kind.equals("infusion")) return new Infusion(research, ResearchPage.recipe(data.get("input")), output,
                ingredients, costs, data.get("instability").getAsInt());
        if (kind.equals("infusion_enchantment")) {
            var enchantment = data.getAsJsonObject("enchantment");
            return new Enchanting(research, enchantment.get("id").getAsInt(), enchantment.get("name").getAsString(),
                    enchantment.get("max_level").getAsInt(), data.get("recipeXP").getAsInt(), data.get("instability").getAsInt(), costs, ingredients);
        }
        if (!List.of("crafting_shaped", "crafting_shapeless", "crafting_ore", "arcane_shaped", "arcane_shapeless").contains(kind))
            throw new IllegalArgumentException("Unsupported named page recipe " + kind);
        return new Crafting(kind, output, ingredients, research, costs,
                data.has("width") ? data.get("width").getAsInt() : 0, data.has("height") ? data.get("height").getAsInt() : 0,
                !data.has("mirrored") || data.get("mirrored").getAsBoolean(), data.has("matches_input_nbt") && data.get("matches_input_nbt").getAsBoolean());
    }
    private static int dimension(Recipe value) { return ((java.math.BigDecimal) ((Literal) value).value()).intValueExact(); }
    public void validate(ResearchPages pages) {
        pages.entries().values().forEach(list -> list.forEach(page -> validate(page.recipe())));
    }
    private void validate(Recipe recipe) {
        if (recipe instanceof Reference reference) for (String key : reference.keys()) {
            // A modern binding resolves through RecipeManager after reload, without a duplicate display recipe.
            if (reference.legacyClass().equals("modern")) {
                if (!key.contains(":") || net.minecraft.resources.ResourceLocation.tryParse(key) == null)
                    throw new IllegalArgumentException("Invalid modern page recipe ID " + key);
                continue;
            }
            if (!require(key).legacyClass().equals(reference.legacyClass())) throw new IllegalArgumentException("Page recipe class mismatch " + key);
        }
        else if (recipe instanceof Sequence sequence) sequence.elements().forEach(this::validate);
    }
    public static ResearchPageRecipes bundled() { var snapshot=ResearchDefinitions.display();return snapshot==null?Bundled.VALUE:snapshot.recipes(); }
    private static  class Bundled {
        static final ResearchPageRecipes VALUE = read();
        static ResearchPageRecipes read() {
            try (var reader = new java.io.InputStreamReader(java.util.Objects.requireNonNull(ResearchPageRecipes.class
                    .getResourceAsStream("/data/thaumcraft/research_page_recipes/legacy.json")), java.nio.charset.StandardCharsets.UTF_8)) {
                var root = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                if (root.get("format").getAsInt() != 1) throw new IllegalArgumentException("Unsupported page recipes format");
                var result = decode(root.getAsJsonArray("entries")); result.validate(ResearchPages.bundled()); return result;
            } catch (java.io.IOException failure) { throw new IllegalStateException("Missing bundled page recipes", failure); }
        }
    }
}
