package thaumicomniverse.compat.create;

import com.google.gson.*;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import tc4revival.content.InfusionRecipe;
import tc4revival.content.InfusionOutput;

/** A per-interaction adapter. Its rolled output is committed once to TC4's saved infusion process. */
public class AssemblyInfusionRecipe extends InfusionRecipe {
    private final ArcaneProcessingRecipe step;
    private AssemblyInfusionRecipe(ArcaneProcessingRecipe step, ItemStack center) {
        super(step.getId(), definition(step, center)); this.step=step;
    }
    private static JsonObject definition(ArcaneProcessingRecipe step, ItemStack center) {
        var json=new JsonObject();json.addProperty("research",step.research());json.addProperty("instability",step.instability());
        var input=new JsonObject();var ingredient=new JsonObject();ingredient.addProperty("item",net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(center.getItem()).toString());input.add("ingredient",ingredient);json.add("center",input);
        var result=new JsonObject();result.addProperty("item",net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(center.getItem()).toString());json.add("result",result);
        var costs=new JsonObject();step.aspects().view().forEach((a,n)->costs.addProperty(a.tag(),n));json.add("aspects",costs);
        var components=new JsonArray();step.components().forEach(value->{var component=new JsonObject();component.add("ingredient",value.toJson());components.add(component);});json.add("components",components);return json;
    }
    public static Optional<InfusionRecipe> find(Level level, Input input) {
        if(!thaumicomniverse.RevivalFeatures.Group.CREATE.enabled() || input.getItem(0).getCount()!=1)return Optional.empty();
        return SequencedAssemblyRecipe.getRecipe(level,input.getItem(0),ArcaneAssembly.INFUSION.type.get(),ArcaneProcessingRecipe.class)
                .map(step->new AssemblyInfusionRecipe(step,input.getItem(0))).filter(recipe->recipe.matches(input,level)).map(recipe->recipe);
    }
    @Override public Plan planForCraft(Input input,Level level) {
        var preview=super.plan(input);
        var results=step.rollResults();
        // The normal Create sequence may fail at its final chance roll, consuming the input.
        ItemStack result=results.isEmpty()?ItemStack.EMPTY:results.get(0);
        return new Plan(preview.components(),preview.aspects(),preview.instability(),0,false,result.isEmpty()?InfusionOutput.consumed():InfusionOutput.item(result));
    }
}
