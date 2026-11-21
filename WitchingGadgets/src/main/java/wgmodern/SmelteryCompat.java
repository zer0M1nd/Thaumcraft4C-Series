package wgmodern;
import com.google.gson.*;import net.minecraft.resources.ResourceLocation;import net.minecraft.network.FriendlyByteBuf;import net.minecraft.world.Container;import net.minecraft.world.item.crafting.*;import net.minecraftforge.common.crafting.*;import net.minecraftforge.common.crafting.conditions.*;
/** Adapts WG's original ingot units; Tinkers still owns melting and recipe synchronization. */
public class SmelteryCompat {
 public static int fluidAmount(int legacy){return (int)Math.min(Integer.MAX_VALUE,(long)legacy*90/144);}
 public static void register(){CraftingHelper.register(new ConditionSerializer());if(net.minecraftforge.fml.ModList.get().isLoaded("tconstruct"))Content.SERIALIZERS.register("cluster_melting",Serializer::new);}
 public record Condition(String metal) implements ICondition {public ResourceLocation getID(){return new ResourceLocation("witchinggadgets","cluster_melting");}public boolean test(IContext context){return net.minecraftforge.fml.ModList.get().isLoaded("tconstruct")&&WGConfig.SMELTERY_CLUSTER_AMOUNT.get()>0&&!context.getTag(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.FLUID,new ResourceLocation("forge","molten_"+metal))).isEmpty()&&!context.getTag(net.minecraft.tags.ItemTags.create(new ResourceLocation("forge","clusters/"+metal))).isEmpty();}}
 public static class ConditionSerializer implements IConditionSerializer<Condition>{public ResourceLocation getID(){return new ResourceLocation("witchinggadgets","cluster_melting");}public Condition read(JsonObject j){return new Condition(j.get("metal").getAsString());}public void write(JsonObject j,Condition c){j.addProperty("metal",c.metal());}}
 @SuppressWarnings({"unchecked","rawtypes"}) public static class Serializer implements RecipeSerializer<Recipe<Container>> {
  private RecipeSerializer base(){return net.minecraftforge.registries.ForgeRegistries.RECIPE_SERIALIZERS.getValue(new ResourceLocation("tconstruct","melting"));}
  public Recipe<Container> fromJson(ResourceLocation id,JsonObject json){var copy=json.deepCopy();copy.getAsJsonObject("result").addProperty("amount",fluidAmount(WGConfig.SMELTERY_CLUSTER_AMOUNT.get()));return (Recipe<Container>)base().fromJson(id,copy);}
  public Recipe<Container> fromNetwork(ResourceLocation id,FriendlyByteBuf b){return (Recipe<Container>)base().fromNetwork(id,b);}
  public void toNetwork(FriendlyByteBuf b,Recipe<Container> r){base().toNetwork(b,r);}
 }
}
