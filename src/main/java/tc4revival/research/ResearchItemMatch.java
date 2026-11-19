package tc4revival.research;

import tc4revival.aspect.recipe.RecipeAspectData;
import tc4revival.aspect.recipe.RecipeAspectData.Key;

/** Legacy trigger comparison after the platform adapter has resolved tags and structural NBT equality. */
public  class ResearchItemMatch {
    private ResearchItemMatch() {}
    public static boolean matches(ResearchDefinition.Stack trigger, Key observed, boolean damageable,
                                  boolean equalNbt, boolean matchesMaterialTag) {
        if (matchesMaterialTag) return equalNbt;
        if (observed == null || !trigger.key().item().equals(observed.item()) || !equalNbt) return false;
        return trigger.key().metadata() == observed.metadata() || (trigger.damageable() && damageable)
                || trigger.key().metadata() == RecipeAspectData.WILDCARD || observed.metadata() == RecipeAspectData.WILDCARD;
    }
}
