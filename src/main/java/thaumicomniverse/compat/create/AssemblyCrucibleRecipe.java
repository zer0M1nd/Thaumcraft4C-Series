package thaumicomniverse.compat.create;

import com.google.gson.JsonObject;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import java.util.Optional;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import tc4revival.content.CrucibleRecipe;

/** Both thrown ingredients and inventory automation resolve the same sequence step. */
public final class AssemblyCrucibleRecipe extends CrucibleRecipe {
    private final ArcaneProcessingRecipe step;
    private AssemblyCrucibleRecipe(ArcaneProcessingRecipe step, ItemStack catalyst) {
        super(step.getId(), definition(step, catalyst)); this.step = step;
    }
    private static JsonObject definition(ArcaneProcessingRecipe step, ItemStack stack) {
        var json = new JsonObject(); json.addProperty("research", step.research());
        var item = new JsonObject(); item.addProperty("item", net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString());
        json.add("catalyst", item); json.add("result", item.deepCopy());
        var aspects = new JsonObject(); step.aspects().view().forEach((a, n) -> aspects.addProperty(a.tag(), n)); json.add("aspects", aspects);
        return json;
    }
    public static Optional<CrucibleRecipe> find(Level level, Input input) {
        if (!thaumicomniverse.RevivalFeatures.Group.CREATE.enabled() || input.getItem(0).isEmpty()) return Optional.empty();
        return SequencedAssemblyRecipe.getRecipe(level, input.getItem(0), ArcaneAssembly.CRUCIBLE.type.get(), ArcaneProcessingRecipe.class)
                .map(step -> new AssemblyCrucibleRecipe(step, input.getItem(0))).filter(recipe -> recipe.matches(input, level)).map(recipe -> recipe);
    }
    @Override public ItemStack assemble(Input input, RegistryAccess registries) {
        var results = step.rollResults(); return results.isEmpty() ? ItemStack.EMPTY : results.get(0).copy();
    }
}
