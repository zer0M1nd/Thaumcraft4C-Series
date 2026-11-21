package gadomancymodern.automation;
import com.google.gson.*;
import gadomancymodern.Gadomancy;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import tc4revival.content.ArcaneRecipe;

public class RemoteJarRecipe extends ArcaneRecipe {
    protected final ArcaneRecipe noMirrors;
    protected final JsonObject raw;
    public RemoteJarRecipe(ResourceLocation id,JsonObject json){super(id,json);raw=json.deepCopy();noMirrors=new ArcaneRecipe(id,alternate(json));}
    public static JsonObject alternate(JsonObject source){var json=source.deepCopy();var mirror=json.getAsJsonObject("key").getAsJsonObject("M");if(mirror.has("item")&&mirror.get("item").getAsString().equals("thaumcraft:essentia_mirror"))mirror.addProperty("item","thaumcraft:mirrored_glass");return json;}
    @Override public boolean matches(CraftingContainer grid,Level level){return tc4revival.config.TCConfig.SERVER.allowMirrors.get()?super.matches(grid,level):noMirrors.matches(grid,level);}
    @Override public JsonObject displayDefinition(){return tc4revival.config.TCConfig.SERVER.allowMirrors.get()?raw.deepCopy():alternate(raw);}
    @Override public RecipeSerializer<?> getSerializer(){return Gadomancy.REMOTE_JAR_RECIPE.get();}
    public static class Serializer implements RecipeSerializer<RemoteJarRecipe>{
        public RemoteJarRecipe fromJson(ResourceLocation id,JsonObject json){return new RemoteJarRecipe(id,json);}
        public RemoteJarRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buffer){return fromJson(id,JsonParser.parseString(buffer.readUtf(1048576)).getAsJsonObject());}
        public void toNetwork(FriendlyByteBuf buffer,RemoteJarRecipe recipe){buffer.writeUtf(recipe.raw.toString(),1048576);}
    }
}
