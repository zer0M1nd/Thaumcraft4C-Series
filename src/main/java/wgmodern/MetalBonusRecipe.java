package wgmodern;
import com.google.gson.*;import net.minecraft.core.RegistryAccess;import net.minecraft.network.FriendlyByteBuf;import net.minecraft.resources.ResourceLocation;import net.minecraft.util.RandomSource;import net.minecraft.world.SimpleContainer;import net.minecraft.world.item.*;import net.minecraft.world.item.crafting.*;import net.minecraft.world.level.Level;import tc4revival.content.InfernalBonusRecipe;
public class MetalBonusRecipe extends InfernalBonusRecipe {
 private final JsonObject json;private final String metal;private final Ingredient input;
 public MetalBonusRecipe(ResourceLocation id,JsonObject json){super(id,json);this.json=json.deepCopy();metal=json.get("metal").getAsString();input=Ingredient.fromJson(json.get("ingredient"));}
 @Override public ItemStack getResultItem(RegistryAccess r){return BlastRecipe.tagged("forge:nuggets/"+metal,1);}
 @Override public ItemStack assemble(SimpleContainer c,RegistryAccess r){return getResultItem(r);}
 @Override public boolean matches(SimpleContainer c,Level l){return Metallurgy.enabled(metal)&&input.test(c.getItem(0))&&!getResultItem(l.registryAccess()).isEmpty();}
 @Override public ItemStack roll(int bellows,RandomSource random){int count=0;if(bellows<=0){if(random.nextInt(4)==0)count++;}else for(int i=0;i<Math.min(3,bellows);i++)if(random.nextFloat()<.44f)count++;return count==0?ItemStack.EMPTY:BlastRecipe.tagged("forge:nuggets/"+metal,count);}
 @Override public RecipeSerializer<?> getSerializer(){return Content.METAL_BONUS.get();}
 public static class Serializer implements RecipeSerializer<MetalBonusRecipe>{public MetalBonusRecipe fromJson(ResourceLocation id,JsonObject j){return new MetalBonusRecipe(id,j);}public MetalBonusRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf b){return fromJson(id,JsonParser.parseString(b.readUtf()).getAsJsonObject());}public void toNetwork(FriendlyByteBuf b,MetalBonusRecipe r){b.writeUtf(r.json.toString());}}
}
