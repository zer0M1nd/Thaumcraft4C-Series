package tc4revival.aspect.recipe;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Semantic recipe data; legacy keys preserve exact and wildcard values without registering old items. */
public record RecipeAspectData(Map<String, ItemProperties> items, List<Registration> registrations, List<Recipe> recipes) {
    public static final int WILDCARD = Short.MAX_VALUE;
    public RecipeAspectData {
        items = Map.copyOf(items);
        registrations = List.copyOf(registrations);
        recipes = List.copyOf(recipes);
    }

    public record Key(String item, int metadata) {
        public Key {
            if (item == null || !validItem(item)) throw new IllegalArgumentException("Invalid semantic item key");
        }
    }
    static boolean validItem(String id) {
        int colon=id.indexOf(':'); if(colon<1 || colon==id.length()-1)return false;
        for(int i=0;i<id.length();i++) {
            if(i==colon)continue; char c=id.charAt(i);
            if(c>='a'&&c<='z'||c>='A'&&c<='Z'||c>='0'&&c<='9'||c=='_'||c=='.'||c=='-'||c=='/'&&i>colon)continue;
            return false;
        }
        return true;
    }
    public record ItemProperties(boolean damageable, boolean hasSubtypes, String container) {}
    public record Registration(Key key, Map<String, Integer> aspects) {
        public Registration { aspects = quantities(aspects); }
    }
    public enum Kind { CRAFTING, ARCANE, INFUSION, CRUCIBLE }
    public record Ingredient(List<Key> alternatives, boolean chooseFirstWithAspects) {
        public Ingredient { alternatives = List.copyOf(alternatives); }
        public static Ingredient direct(Key key) { return new Ingredient(List.of(key), false); }
    }
    public record Recipe(Kind kind, Key output, int outputCount, List<Ingredient> ingredients, Map<String, Integer> cost) {
        public Recipe {
            // Old dynamic map recipes expose a zero-count template; this is calculation data, not craft output.
            if (outputCount < 0) throw new IllegalArgumentException("Recipe output count cannot be negative");
            ingredients = List.copyOf(ingredients);
            cost = quantities(cost);
        }
    }
    private static Map<String, Integer> quantities(Map<String, Integer> values) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }
}
