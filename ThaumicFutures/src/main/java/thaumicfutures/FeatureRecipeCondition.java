package thaumicfutures;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;
import thaumcraft.api.config.FeatureRecipes;

/** Defer world-dependent material alternatives until the SERVER configuration is loaded. */
public record FeatureRecipeCondition(FutureFeatures.Group group) implements ICondition {
    private static final ResourceLocation ID=ResourceLocation.parse("thaumicfutures:feature");
    public static void register(){
        FeatureRecipes.deferCondition(ID);
        // Forge's condition table is not safe for concurrent mod constructors.
        net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus().addListener(
                (net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) ->
                        event.enqueueWork(() -> CraftingHelper.register(new Codec())));
    }
    @Override public ResourceLocation getID(){return ID;}
    @Override public boolean test(IContext context){return group.enabled();}
    private static class Codec implements IConditionSerializer<FeatureRecipeCondition> {
        @Override public ResourceLocation getID(){return ID;}
        @Override public void write(JsonObject json,FeatureRecipeCondition value){json.addProperty("group",value.group.name());}
        @Override public FeatureRecipeCondition read(JsonObject json){return new FeatureRecipeCondition(FutureFeatures.Group.valueOf(json.get("group").getAsString()));}
    }
}
