package wgmodern;
import com.google.gson.JsonObject;import java.util.*;import net.minecraft.resources.ResourceLocation;import net.minecraft.network.FriendlyByteBuf;import net.minecraft.core.RegistryAccess;import net.minecraft.world.inventory.CraftingContainer;import net.minecraft.world.item.*;import net.minecraft.world.item.crafting.*;import net.minecraft.world.level.Level;
public class PhotoDevelopingRecipe extends tc4revival.content.ArcaneRecipe {
 public PhotoDevelopingRecipe(ResourceLocation id,JsonObject json){super(id,json);}
 private record Ingredients(ItemStack plate,int count){}
 private Ingredients ingredients(CraftingContainer grid){ItemStack plate=ItemStack.EMPTY;int papers=0,ink=0;for(int i=0;i<Math.min(9,grid.getContainerSize());i++){var s=grid.getItem(i);if(s.isEmpty())continue;if(s.is(Content.PLATE.get())){if(!plate.isEmpty()||!PhotoData.valid(PhotoData.of(s)))return null;plate=s;}else if(s.is(Items.PAPER))papers++;else if(s.is(net.minecraft.tags.ItemTags.create(new ResourceLocation("forge","dyes/black"))))ink++;else return null;}return !plate.isEmpty()&&papers>0&&papers==ink?new Ingredients(plate,papers):null;}
 @Override public boolean matches(CraftingContainer grid,Level level){return ingredients(grid)!=null;}
 @Override public ItemStack assemble(CraftingContainer grid,RegistryAccess registries){var values=ingredients(grid);if(values==null)return ItemStack.EMPTY;var out=PhotoData.create(true,PhotoData.of(values.plate()));out.setCount(values.count());return out;}
 @Override public Map<tc4revival.aspect.Aspect,Integer> visCost(CraftingContainer grid){var values=ingredients(grid);var result=new LinkedHashMap<tc4revival.aspect.Aspect,Integer>();if(values!=null)for(String a:List.of("aer","aqua","ordo"))result.put(tc4revival.aspect.TCAspects.catalog().require(a),values.count()*5);return result;}
 @Override public RecipeSerializer<?> getSerializer(){return Content.PHOTO_RECIPE.get();}
 @Override public JsonObject displayDefinition(){
  var json=super.displayDefinition();var scan=new net.minecraft.nbt.CompoundTag();
  scan.putString("identity","item:minecraft:stone");scan.putString("kind","OBJECT");
  scan.putString("name",net.minecraft.network.chat.Component.Serializer.toJson(net.minecraft.network.chat.Component.translatable("block.minecraft.stone")));
  scan.put("item",new ItemStack(Items.STONE).save(new net.minecraft.nbt.CompoundTag()));
  var inputs=new com.google.gson.JsonArray();
  for(var stack:List.of(PhotoData.create(false,scan),new ItemStack(Items.PAPER),new ItemStack(Items.BLACK_DYE)))inputs.add(stack.save(new net.minecraft.nbt.CompoundTag()).toString());
  json.add("display_inputs",inputs);json.addProperty("display_width",0);return json;
 }
 public static class Serializer implements RecipeSerializer<PhotoDevelopingRecipe>{public PhotoDevelopingRecipe fromJson(ResourceLocation id,JsonObject json){return new PhotoDevelopingRecipe(id,json);}public PhotoDevelopingRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf b){return new PhotoDevelopingRecipe(id,com.google.gson.JsonParser.parseString(b.readUtf(1048576)).getAsJsonObject());}public void toNetwork(FriendlyByteBuf b,PhotoDevelopingRecipe recipe){b.writeUtf(recipe.displayDefinition().toString(),1048576);}}
}
