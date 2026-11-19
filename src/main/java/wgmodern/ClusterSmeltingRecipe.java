package wgmodern;
import com.google.gson.*;import net.minecraft.core.RegistryAccess;import net.minecraft.resources.ResourceLocation;import net.minecraft.network.FriendlyByteBuf;import net.minecraft.world.Container;import net.minecraft.world.item.*;import net.minecraft.world.item.crafting.*;import net.minecraft.world.level.Level;
public class ClusterSmeltingRecipe extends SmeltingRecipe {
 private final String metal;private final JsonObject json;
 public ClusterSmeltingRecipe(ResourceLocation id,JsonObject json){super(id,"witchinggadgets:clusters",CookingBookCategory.MISC,Ingredient.fromJson(json.get("ingredient")),new ItemStack(Items.IRON_INGOT),1,200);this.json=json.deepCopy();metal=json.get("metal").getAsString();}
 @Override public boolean matches(Container input,Level l){return Metallurgy.enabled(metal)&&super.matches(input,l)&&!getResultItem(l.registryAccess()).isEmpty();}
 @Override public ItemStack assemble(Container input,RegistryAccess r){return getResultItem(r);}
 @Override public ItemStack getResultItem(RegistryAccess r){return BlastRecipe.tagged("forge:ingots/"+metal,2);}
 @Override public RecipeSerializer<?> getSerializer(){return Content.CLUSTER_SMELTING.get();}
 public static class Serializer implements RecipeSerializer<ClusterSmeltingRecipe>{public ClusterSmeltingRecipe fromJson(ResourceLocation id,JsonObject j){return new ClusterSmeltingRecipe(id,j);}public ClusterSmeltingRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf b){return fromJson(id,JsonParser.parseString(b.readUtf()).getAsJsonObject());}public void toNetwork(FriendlyByteBuf b,ClusterSmeltingRecipe r){b.writeUtf(r.json.toString());}}
}
