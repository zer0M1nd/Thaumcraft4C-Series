package tc4revival.aspect.item;

import java.util.List;
import java.util.Map;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.recipe.RecipeAspectData.Key;

public  class PotionAspectRules {
    public record Effect(int id, int amplifier, int duration) {
        public Effect {
            if (id < 0 || amplifier < 0 || amplifier > 255 || duration < 0) throw new IllegalArgumentException("Invalid legacy potion effect");
        }
    }
    public record Definition(boolean splash, boolean hasEffectList, List<Effect> effects) {
        public Definition {
            effects = List.copyOf(effects);
            if (!hasEffectList && !effects.isEmpty()) throw new IllegalArgumentException("Null effect list cannot have members");
        }
    }
    private final Map<Integer, Definition> definitions;
    private final AspectCatalog catalog;
    public PotionAspectRules(Map<Integer, Definition> definitions, AspectCatalog catalog) {
        this.definitions = Map.copyOf(definitions);
        this.catalog = catalog;
    }
    public Map<Integer, Definition> definitions() { return definitions; }
    public AspectAmounts decorate(Key key, AspectAmounts source) {
        if (!key.item().equals("minecraft:potion")) return source;
        var definition = definitions.get(key.metadata());
        if (definition == null) throw new IllegalArgumentException("Unsupported legacy potion metadata: " + key.metadata());
        var result = source == null ? new AspectAmounts() : source.copy();
        result.merge(catalog.require("aqua"), 1);
        if (definition.hasEffectList()) {
            if (definition.splash()) result.merge(catalog.require("perditio"), 2);
            for (var effect : definition.effects()) {
                int level = effect.amplifier() + 1;
                result.merge(catalog.require("praecantatio"), level * 2);
                String aspect = switch (effect.id()) {
                    case 1 -> "motus"; case 2, 4 -> "vinculum"; case 3 -> "instrumentum";
                    case 5 -> "telum"; case 6, 10 -> "sano"; case 7, 17, 18 -> "mortuus";
                    case 8 -> "volatus"; case 9 -> "alienis"; case 11 -> "tutamen";
                    case 13 -> "aer"; case 14, 16 -> "sensus"; case 15 -> "tenebrae";
                    case 19 -> "venenum"; default -> null;
                };
                if (effect.id() == 12) {
                    result.merge(catalog.require("tutamen"), level);
                    result.merge(catalog.require("ignis"), level * 2);
                } else if (aspect != null) result.merge(catalog.require(aspect), level * 3);
            }
        }
        var capped = new AspectAmounts();
        result.view().forEach((aspect, amount) -> capped.merge(aspect, Math.min(64, amount)));
        return capped;
    }
    public static String modernEffect(int oldId) {
        return switch (oldId) {
            case 1 -> "minecraft:speed"; case 2 -> "minecraft:slowness";
            case 3 -> "minecraft:haste"; case 4 -> "minecraft:mining_fatigue";
            case 5 -> "minecraft:strength"; case 6 -> "minecraft:instant_health";
            case 7 -> "minecraft:instant_damage"; case 8 -> "minecraft:jump_boost";
            case 9 -> "minecraft:nausea"; case 10 -> "minecraft:regeneration";
            case 11 -> "minecraft:resistance"; case 12 -> "minecraft:fire_resistance";
            case 13 -> "minecraft:water_breathing"; case 14 -> "minecraft:invisibility";
            case 15 -> "minecraft:blindness"; case 16 -> "minecraft:night_vision";
            case 17 -> "minecraft:hunger"; case 18 -> "minecraft:weakness";
            case 19 -> "minecraft:poison"; default -> null;
        };
    }
}
