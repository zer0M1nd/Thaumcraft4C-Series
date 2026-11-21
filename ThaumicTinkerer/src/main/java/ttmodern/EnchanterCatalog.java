package ttmodern;

import com.google.gson.*;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

/** Reloadable server authority for Osmotic Enchanter costs; never trust client-supplied costs. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=Tinkerer.ID)
public final class EnchanterCatalog extends SimpleJsonResourceReloadListener {
    public record Entry(ResourceLocation enchantment,String research,ResourceLocation icon,Map<String,Integer> base,List<String> researchAlternatives){
        public Entry { base=Map.copyOf(base);researchAlternatives=List.copyOf(researchAlternatives); }
        public Entry(ResourceLocation enchantment,String research,ResourceLocation icon,Map<String,Integer> base){this(enchantment,research,icon,base,List.of());}
        public boolean unlocked(java.util.function.Predicate<String> complete){
            return research.isEmpty()||complete.test(research)||researchAlternatives.stream().anyMatch(complete);
        }
        public Map<String,Integer> cost(int rank){
            var type=ForgeRegistries.ENCHANTMENTS.getValue(enchantment);if(type==null||rank<type.getMinLevel()||rank>type.getMaxLevel())throw new IllegalArgumentException("Invalid enchantment rank");
            var result=new TreeMap<String,Integer>();base.forEach((key,value)->result.put(key,(int)(value*rank*(1d+rank*.2d))));return Map.copyOf(result);
        }
    }
    private static volatile Map<ResourceLocation,Entry> entries=Map.of();
    public EnchanterCatalog(){super(new Gson(),"osmotic_enchantments");}
    public static Map<ResourceLocation,Entry> entries(){
        if(!net.minecraftforge.fml.ModList.get().isLoaded("thaumicbases"))return entries;
        var result=new LinkedHashMap<ResourceLocation,Entry>(ttmodern.compat.BasesEnchantments.entries());
        result.putAll(entries); // Authored datapack overrides remain authoritative.
        return Collections.unmodifiableMap(result);
    }
    public static Entry find(Enchantment enchantment){return entries().get(ForgeRegistries.ENCHANTMENTS.getKey(enchantment));}
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void reload(net.minecraftforge.event.AddReloadListenerEvent event){event.addListener(new EnchanterCatalog());}
    @Override protected void apply(Map<ResourceLocation,JsonElement> resources,ResourceManager manager,ProfilerFiller profiler){
        var next=new LinkedHashMap<ResourceLocation,Entry>();
        resources.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(file->{for(var value:file.getValue().getAsJsonObject().getAsJsonArray("entries")){
            var row=value.getAsJsonObject();var id=ResourceLocation.parse(row.get("enchantment").getAsString());if(!ForgeRegistries.ENCHANTMENTS.containsKey(id))throw new IllegalArgumentException("Unknown osmotic enchantment "+id);
            var costs=new TreeMap<String,Integer>();row.getAsJsonObject("aspects").entrySet().forEach(a->{int amount=a.getValue().getAsInt();if(amount<1||amount>100000)throw new IllegalArgumentException("Invalid enchantment cost");costs.put(a.getKey(),amount);});
            if(costs.isEmpty())throw new IllegalArgumentException("Empty enchantment cost");next.put(id,new Entry(id,row.has("research")?row.get("research").getAsString():"",ResourceLocation.parse(row.get("icon").getAsString()),Map.copyOf(costs),row.has("research_alternatives")?java.util.stream.StreamSupport.stream(row.getAsJsonArray("research_alternatives").spliterator(),false).map(JsonElement::getAsString).toList():List.of()));
        }});entries=Collections.unmodifiableMap(next);
    }
}
