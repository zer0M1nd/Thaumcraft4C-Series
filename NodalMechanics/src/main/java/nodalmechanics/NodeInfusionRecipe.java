package nodalmechanics;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import tc4revival.content.AuraNodeModifier;
import tc4revival.content.AuraNodeType;
import tc4revival.content.InfusionOutput;
import tc4revival.content.InfusionRecipe;
import tc4revival.content.NodeJarData;
import tc4revival.content.TCItems;

/** One immutable datapack recipe handles all matrix compositions. */
public class NodeInfusionRecipe extends InfusionRecipe {
    public NodeInfusionRecipe(ResourceLocation id, JsonObject json) { super(id, json); }
    @Override public boolean acceptsCenter(ItemStack central) {
        return central.is(NodalMechanics.MATRIX.get()) && !MatrixItem.counts(central).isEmpty();
    }
    private Plan result(ItemStack central, int type, int modifier, String nodeId) {
        var base = super.plan(central);
        var output = new ItemStack(TCItems.NODE_JAR.get());
        output.setTag(new NodeJarData(MatrixItem.amounts(central, NodalRules.CAPACITY_PER_PHIAL),
                AuraNodeType.values()[type], modifier < 0 ? null : AuraNodeModifier.values()[modifier], nodeId).write());
        return new Plan(base.components(), MatrixItem.amounts(central, NodalRules.ESSENTIA_PER_PHIAL),
                base.instability(), 0, false, InfusionOutput.item(output));
    }
    @Override public Plan plan(ItemStack central) { return result(central, 0, -1, "nodalmechanics-preview"); }
    @Override public Plan planForCraft(ItemStack central, Level level) {
        int type = NodalRules.type(level.random.nextInt(100));
        int presence = level.random.nextInt(100);
        int modifier = NodalRules.modifier(presence, presence < 60 ? level.random.nextInt(50) : 0);
        return result(central, type, modifier, java.util.UUID.randomUUID().toString());
    }
    @Override public ItemStack assemble(Input input, RegistryAccess access) { return plan(input.getItem(0)).output().apply(input.getItem(0)); }
    @Override public RecipeSerializer<?> getSerializer() { return NodalMechanics.NODE_RECIPE.get(); }
    public static class Serializer implements RecipeSerializer<NodeInfusionRecipe> {
        @Override public NodeInfusionRecipe fromJson(ResourceLocation id, JsonObject json) { return new NodeInfusionRecipe(id, json); }
        @Override public NodeInfusionRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return fromJson(id, com.google.gson.JsonParser.parseString(buffer.readUtf(1048576)).getAsJsonObject());
        }
        @Override public void toNetwork(FriendlyByteBuf buffer, NodeInfusionRecipe recipe) { buffer.writeUtf(recipe.displayDefinition().toString(), 1048576); }
    }
}
