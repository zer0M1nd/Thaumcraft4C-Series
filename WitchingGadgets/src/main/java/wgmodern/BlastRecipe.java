package wgmodern;

import java.util.*;
import com.google.gson.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

/** Reloadable recipes with tag outputs, so optional ores do not impose another mod dependency. */
public class BlastRecipe implements Recipe<Container> {
    public final ResourceLocation id;
    public final Ingredient input;
    public final int count,time;
    public final boolean special;
    private final JsonObject json;
    public BlastRecipe(ResourceLocation id,JsonObject json){this.id=id;this.json=json.deepCopy();input=Ingredient.fromJson(json.get("ingredient"));count=integer(json,"count",1);time=integer(json,"duration",440);special=json.has("special")&&json.get("special").getAsBoolean();if(count<1||time<1)throw new IllegalArgumentException("Invalid blast recipe "+id);}
    static int integer(JsonObject j,String key,int fallback){return j.has(key)?j.get(key).getAsInt():fallback;}
    public static ItemStack tagged(String tag,int count){var key=TagKey.create(Registries.ITEM,new ResourceLocation(tag));return ForgeRegistries.ITEMS.tags().getTag(key).stream().findFirst().map(i->new ItemStack(i,count)).orElse(ItemStack.EMPTY);}
    public static ItemStack output(JsonObject data){if(data==null)return ItemStack.EMPTY;var result=data.has("tag")?tagged(data.get("tag").getAsString(),integer(data,"count",1)):ShapedRecipe.itemStackFromJson(data);if(result.isEmpty()&&data.has("fallback_tag"))result=tagged(data.get("fallback_tag").getAsString(),integer(data,"fallback_count",1));return result;}
    public ItemStack output(){return output(json.getAsJsonObject("result"));}
    public ItemStack bonus(){return output(json.getAsJsonObject("bonus"));}
    public boolean matches(ItemStack stack){return stack.getCount()>=count&&input.test(stack)&&!output().isEmpty();}
    public static Optional<BlastRecipe> find(Level l,ItemStack stack){var explicit=l.getRecipeManager().getAllRecipesFor(Content.BLAST_RECIPES.get()).stream().filter(r->r.matches(stack)).sorted(Comparator.comparing(r->r.id)).findFirst();if(explicit.isPresent())return explicit;return stack.getTags().filter(t->t.location().getNamespace().equals("forge")&&t.location().getPath().startsWith("clusters/")&&WGConfig.TRIPLING_CLUSTERS.get().contains(t.location().getPath().substring(9))).sorted(Comparator.comparing(t->t.location().toString())).map(t->automaticCluster(t.location().getPath().substring(9))).filter(r->r.matches(stack)).findFirst();}
    private static final Map<String,BlastRecipe> AUTOMATIC_CLUSTERS=new HashMap<>();
    private static BlastRecipe automaticCluster(String metal){return AUTOMATIC_CLUSTERS.computeIfAbsent(metal,key->{var j=new JsonObject();var input=new JsonObject();input.addProperty("tag","forge:clusters/"+key);j.add("ingredient",input);var output=new JsonObject();output.addProperty("tag","forge:ingots/"+key);output.addProperty("count",3);j.add("result",output);var bonus=new JsonObject();bonus.addProperty("tag","forge:nuggets/"+key);j.add("bonus",bonus);return new BlastRecipe(new ResourceLocation("witchinggadgets","automatic_cluster/"+key),j);});}
    public static void clearAutomatic(){AUTOMATIC_CLUSTERS.clear();}
    @Override public boolean matches(Container c,Level l){return c.getContainerSize()>0&&matches(c.getItem(0));}
    @Override public ItemStack assemble(Container c,RegistryAccess r){return output();}
    @Override public ItemStack getResultItem(RegistryAccess r){return output();}
    @Override public boolean canCraftInDimensions(int x,int y){return x*y>0;}
    @Override public boolean isSpecial(){return true;}
    @Override public ResourceLocation getId(){return id;}
    @Override public RecipeType<?> getType(){return Content.BLAST_RECIPES.get();}
    @Override public RecipeSerializer<?> getSerializer(){return Content.BLAST_SERIALIZER.get();}
    public static class Serializer implements RecipeSerializer<BlastRecipe>{
        @Override public BlastRecipe fromJson(ResourceLocation id,JsonObject j){return new BlastRecipe(id,j);}
        @Override public BlastRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf b){return fromJson(id,JsonParser.parseString(b.readUtf(65536)).getAsJsonObject());}
        @Override public void toNetwork(FriendlyByteBuf b,BlastRecipe r){b.writeUtf(r.json.toString(),65536);}
    }
}
