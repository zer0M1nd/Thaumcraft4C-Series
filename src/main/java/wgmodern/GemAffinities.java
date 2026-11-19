package wgmodern;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
public class GemAffinities {
 public record Definition(Set<String> affinities,Set<String> aversions){}
 public static final Map<ResourceLocation,Definition> TAGS=new LinkedHashMap<>();
 static {register("forge:gems/diamond","ordo perditio","");register("forge:gems/emerald","ordo terra","aqua perditio");register("forge:gems/quartz","perditio ignis","ordo aqua");register("forge:gems/certus_quartz","ordo aer","perditio ignis");register("forge:gems/topaz","aer ignis","perditio aqua");register("forge:gems/amber","aer terra","perditio");register("forge:gems/peridot","terra","perditio ignis");register("forge:gems/malachite","terra aqua","perditio ignis");register("forge:gems/ruby","ignis","perditio aqua");register("forge:gems/sapphire","aqua","perditio ignis");register("forge:gems/tanzanite","aqua","perditio ignis");register("forge:gems/primordial","aer ignis aqua terra ordo perditio","");}
 public static void register(String tag,String positive,String negative){TAGS.put(new ResourceLocation(tag),new Definition(words(positive),words(negative)));}
 private static Set<String> words(String s){return s.isBlank()?Set.of():Set.of(s.split(" "));}
 public static Definition find(ItemStack s,net.minecraft.world.level.Level level){if(level!=null){var match=level.getRecipeManager().getAllRecipesFor(Content.GEM_AFFINITIES.get()).stream().filter(r->r.ingredient.test(s)).min(java.util.Comparator.comparingInt((GemAffinityRecipe r)->r.priority).thenComparing(r->r.id));if(match.isPresent())return match.get().enabled?match.get().definition:null;}return find(s);}
 public static Definition find(ItemStack s){for(var entry:TAGS.entrySet())if(s.is(TagKey.create(Registries.ITEM,entry.getKey())))return entry.getValue();return null;}
}
