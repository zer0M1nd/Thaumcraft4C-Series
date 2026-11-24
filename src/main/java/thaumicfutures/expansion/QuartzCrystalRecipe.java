package thaumicfutures.expansion;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import tc4revival.aspect.Aspect;
import tc4revival.content.CrucibleRecipe;
import tc4revival.content.TCItems;
import thaumicfutures.FutureFeatures;
import thaumicfutures.LegacyContent;

/** One sliver crystallizes a pair of a single aspect, including addon aspects. */
public final class QuartzCrystalRecipe extends CrucibleRecipe {
    private final Aspect aspect;

    private QuartzCrystalRecipe(Aspect aspect) {
        super(ResourceLocation.fromNamespaceAndPath("thaumicfutures", "vis_crystal/" + aspect.tag()), definition(aspect));
        this.aspect = aspect;
    }

    static Optional<CrucibleRecipe> forSliver(Level level, Input input) {
        if (!input.getItem(0).is(LegacyContent.QUARTZ_SLIVER.get()) || !FutureFeatures.Group.RESTORATION.enabled())
            return Optional.empty();
        return input.aspects().view().entrySet().stream()
                .filter(entry -> entry.getValue() >= 2)
                .map(entry -> (CrucibleRecipe)new QuartzCrystalRecipe(entry.getKey())).findFirst();
    }

    private static JsonObject definition(Aspect aspect) {
        var recipe = new JsonObject();
        recipe.addProperty("research", "");
        var costs = new JsonObject(); costs.addProperty(aspect.tag(), 2);
        var input = new JsonObject(); input.addProperty("item", "thaumicfutures:quartz_sliver");
        var result = new JsonObject(); result.addProperty("item", "thaumcraft:essence_crystal");
        recipe.add("aspects", costs); recipe.add("catalyst", input); recipe.add("result", result);
        return recipe;
    }

    @Override public ItemStack assemble(Input input, RegistryAccess registries) { return getResultItem(registries); }
    @Override public ItemStack getResultItem(RegistryAccess registries) { return TCItems.ESSENCE_CRYSTAL.get().of(aspect,1); }
}
