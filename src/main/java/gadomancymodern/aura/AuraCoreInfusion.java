package gadomancymodern.aura;

import com.google.gson.*;
import gadomancymodern.Gadomancy;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import tc4revival.content.InfusionRecipe;

/** Original randomized cost, sampled only when a real altar craft begins. */
public class AuraCoreInfusion extends InfusionRecipe {
    public AuraCoreInfusion(ResourceLocation id,JsonObject definition){super(id,definition);}
    @Override public Plan planForCraft(ItemStack center,Level world){
        Plan preview=plan(center);var costs=preview.aspects();
        for(var aspect:costs.aspects()){
            int sampled=AuraComposition.randomHalfCost(costs.amount(aspect),world.random::nextInt);
            costs.remove(aspect);costs.add(aspect,sampled);
        }
        return new Plan(preview.components(),costs,preview.instability(),preview.xp(),preview.enchanting(),preview.output());
    }
    @Override public RecipeSerializer<?> getSerializer(){return Gadomancy.AURA_RECIPE.get();}
    public static class Serializer implements RecipeSerializer<AuraCoreInfusion>{
        @Override public AuraCoreInfusion fromJson(ResourceLocation id,JsonObject json){return new AuraCoreInfusion(id,json);}
        @Override public AuraCoreInfusion fromNetwork(ResourceLocation id,FriendlyByteBuf buffer){return fromJson(id,JsonParser.parseString(buffer.readUtf(1048576)).getAsJsonObject());}
        @Override public void toNetwork(FriendlyByteBuf buffer,AuraCoreInfusion recipe){buffer.writeUtf(recipe.displayDefinition().toString(),1048576);}
    }
}
