package gadomancymodern.automation;
import com.google.gson.*;
import gadomancymodern.Gadomancy;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import tc4revival.content.ArcaneRecipe;

/** Resolve only the author's warded-glass configuration alternate, leaving datapack edits intact. */
public class PackagerRecipe extends ArcaneRecipe {
    protected final ArcaneRecipe ordinaryGlass;
    protected final JsonObject raw;
    public PackagerRecipe(ResourceLocation id,JsonObject json){super(id,json);raw=json.deepCopy();ordinaryGlass=new ArcaneRecipe(id,ordinary(json));}
    public static JsonObject ordinary(JsonObject source){var json=source.deepCopy();var glass=json.getAsJsonObject("key").getAsJsonObject("G");if(glass.has("item")&&glass.get("item").getAsString().equals("thaumcraft:warded_glass"))glass.addProperty("item","minecraft:glass");return json;}
    @Override public boolean matches(CraftingContainer grid,Level level){return tc4revival.config.TCConfig.SERVER.wardedStone.get()?super.matches(grid,level):ordinaryGlass.matches(grid,level);}
    @Override public JsonObject displayDefinition(){return tc4revival.config.TCConfig.SERVER.wardedStone.get()?raw.deepCopy():ordinary(raw);}
    @Override public RecipeSerializer<?> getSerializer(){return Gadomancy.PACKAGER_RECIPE.get();}
    public static class Serializer implements RecipeSerializer<PackagerRecipe>{
        public PackagerRecipe fromJson(ResourceLocation id,JsonObject json){return new PackagerRecipe(id,json);}
        public PackagerRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buffer){return fromJson(id,JsonParser.parseString(buffer.readUtf(1048576)).getAsJsonObject());}
        public void toNetwork(FriendlyByteBuf buffer,PackagerRecipe recipe){buffer.writeUtf(recipe.raw.toString(),1048576);}
    }
}
