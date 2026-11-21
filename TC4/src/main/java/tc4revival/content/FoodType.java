package tc4revival.content;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;

/** The four furnace meat bonuses and their combined food; these are not metal nuggets. */
public enum FoodType {
    CHICKEN("chicken_nugget", "nuggetchicken", "ItemNuggetChicken"),
    BEEF("beef_nugget", "nuggetbeef", "ItemNuggetBeef"),
    PORK("pork_nugget", "nuggetpork", "ItemNuggetPork"),
    FISH("fish_nugget", "nuggetfish", "ItemNuggetFish"),
    TRIPLE_MEAT_TREAT("triple_meat_treat", "tripletreat", "TripleMeatTreat");

    private final String path, texture, legacyName;
    FoodType(String path, String texture, String legacyName) {
        this.path = path; this.texture = texture; this.legacyName = legacyName;
    }
    public String path() { return path; }
    public String texture() { return texture; }
    public String legacyName() { return legacyName; }
    public Item create() {
        var food = this == TRIPLE_MEAT_TREAT
                ? new FoodProperties.Builder().nutrition(6).saturationMod(0.8f).meat().alwaysEat()
                    .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 100, 0), 0.66f).build()
                : new FoodProperties.Builder().nutrition(1).saturationMod(0.3f).build();
        return new LegacyFoodItem(new Item.Properties().food(food), this == TRIPLE_MEAT_TREAT ? 32 : 10);
    }
}
