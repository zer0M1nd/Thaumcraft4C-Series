package thaumicomniverse.compat;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.registries.ForgeRegistries;

public final class AtmRecipes {
    private static final ResourceLocation SOUL = new ResourceLocation("allthetweaks", "dragon_soul");
    private static final ResourceLocation FRAGMENT = new ResourceLocation("mysticalagradditions", "dragon_scale");
    private static final java.util.Map<ShapedRecipe, java.util.Map<Integer,Ingredient>> CHANGED = new java.util.WeakHashMap<>();

    /** Runs after all reload listeners, including KubeJS recipes and afterRecipes. */
    public static void apply(RecipeManager manager, RegistryAccess registries) {
        CHANGED.forEach((recipe,slots)->slots.forEach((slot,original)->recipe.getIngredients().set(slot,original)));
        CHANGED.clear();
        if (!thaumicomniverse.RevivalFeatures.CONFIG.loaded() || !thaumicomniverse.RevivalFeatures.Group.ATM_CERTIFICATE.enabled()) return;
        var certificate = ForgeRegistries.ITEMS.getValue(new ResourceLocation("thaumicomniverse", "thaumaturge_certificate"));
        if (certificate == null) return;
        if (manager.byKey(ResourceLocation.parse("thaumicomniverse:atm/thaumaturge_certificate")).isEmpty()) return;
        for (var recipe : manager.getRecipes()) {
            if (!(recipe instanceof ShapedRecipe shaped) ||
                    !SOUL.equals(ForgeRegistries.ITEMS.getKey(recipe.getResultItem(registries).getItem()))) continue;
            var ingredients = shaped.getIngredients();
            for (int slot = 0; slot < ingredients.size(); slot++) {
                var alternatives = ingredients.get(slot).getItems();
                if (alternatives.length == 0) continue;
                boolean fragmentOnly = java.util.Arrays.stream(alternatives)
                        .allMatch(stack -> FRAGMENT.equals(ForgeRegistries.ITEMS.getKey(stack.getItem())));
                if (fragmentOnly) {
                    CHANGED.computeIfAbsent(shaped,ignored->new java.util.HashMap<>()).put(slot,ingredients.get(slot));
                    ingredients.set(slot, Ingredient.of(certificate));
                }
            }
        }
    }
}
