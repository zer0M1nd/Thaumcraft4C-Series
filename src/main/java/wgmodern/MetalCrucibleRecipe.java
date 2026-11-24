package wgmodern;
import java.util.*;import com.google.gson.*;import net.minecraft.core.RegistryAccess;import net.minecraft.resources.ResourceLocation;import net.minecraft.network.FriendlyByteBuf;import net.minecraft.world.item.*;import net.minecraft.world.item.crafting.*;import net.minecraft.world.level.Level;import tc4revival.aspect.*;import tc4revival.aspect.item.ItemAspects;import tc4revival.content.CrucibleRecipe;
/** Costs use a copy of the ingot's direct aspects; the old global-aspect mutation is never repeated. */
public class MetalCrucibleRecipe extends CrucibleRecipe {
 public final String metal;public final boolean transmutation;private final JsonObject json;private final boolean received;
 private record CostSnapshot(Object generation,AspectAmounts amounts){}
 private volatile CostSnapshot calculated;
 public MetalCrucibleRecipe(ResourceLocation id,JsonObject json,boolean received){super(id,json);this.json=json.deepCopy();this.received=received;metal=json.get("metal").getAsString();transmutation=json.get("transmutation").getAsBoolean();}
 @Override public AspectAmounts costs(){
  if(!transmutation||received)return super.costs();
  var direct=ItemAspects.directResolver();var snapshot=calculated;
  if(snapshot==null||snapshot.generation()!=direct){
   var amounts=ItemAspects.direct(BlastRecipe.tagged("forge:ingots/"+metal,1)).orElseGet(AspectAmounts::new).copy();
   var metalAspect=TCAspects.catalog().require("metallum");amounts.remove(metalAspect,amounts.amount(metalAspect));
   // Recipe packets for different players can encode concurrently on Netty threads.
   // Publish the generation and the fully computed costs together, never the mutable work in progress.
   snapshot=new CostSnapshot(direct,amounts);calculated=snapshot;
  }
  var result=snapshot.amounts().copy();
  var metalAspect=TCAspects.catalog().require("metallum");
  result.add(metalAspect,super.costs().amount(metalAspect));
  return result;
 }
 public boolean available(){return transmutation?WGConfig.TRANSMUTATIONS.get()&&!BlastRecipe.tagged("forge:ingots/"+metal,1).isEmpty()&&!BlastRecipe.tagged("forge:nuggets/"+metal,1).isEmpty():Metallurgy.enabled(metal);}
 @Override public boolean matches(Input input,Level level){return available()&&thaumcraft.api.research.ResearchCategories.enabled(research())&&input.researchKnown(research())&&catalystMatches(input.getItem(0))&&remainder(input.aspects()).isPresent();}
 @Override public Optional<AspectAmounts> remainder(AspectAmounts contents){var fee=costs();if(fee.view().entrySet().stream().anyMatch(e->contents.amount(e.getKey())<e.getValue()))return Optional.empty();var result=contents.copy();fee.view().forEach(result::remove);return Optional.of(result);}
 @Override public ItemStack getResultItem(RegistryAccess r){if(!available())return ItemStack.EMPTY;return transmutation?BlastRecipe.tagged("forge:nuggets/"+metal,3):super.getResultItem(r);}
 @Override public ItemStack assemble(Input input,RegistryAccess r){return getResultItem(r);}
 @Override public RecipeSerializer<?> getSerializer(){return Content.METAL_CRUCIBLE.get();}
 public static class Serializer implements RecipeSerializer<MetalCrucibleRecipe>{public MetalCrucibleRecipe fromJson(ResourceLocation id,JsonObject j){return new MetalCrucibleRecipe(id,j,false);}public MetalCrucibleRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf b){return new MetalCrucibleRecipe(id,JsonParser.parseString(b.readUtf(65536)).getAsJsonObject(),true);}public void toNetwork(FriendlyByteBuf b,MetalCrucibleRecipe r){var json=r.json.deepCopy();json.remove("nugget_transmutation");var fees=new JsonObject();r.costs().view().forEach((a,n)->fees.addProperty(a.tag(),n));json.add("aspects",fees);b.writeUtf(json.toString(),65536);}}
}
