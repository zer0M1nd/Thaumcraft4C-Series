package ttmodern;

import java.util.*;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.*;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=Tinkerer.ID)
public final class MobEssenceCatalog extends SimpleJsonResourceReloadListener {
    public record Recipe(ResourceLocation entity,List<String> aspects){public Recipe{aspects=List.copyOf(aspects);}public boolean matches(List<String> inputs){return SoulComposition.matches(aspects,inputs);}}
    private static volatile List<Recipe> recipes=List.of();
    public MobEssenceCatalog(){super(new Gson(),"mob_essences");}
    public static List<Recipe> recipes(){return recipes;}
    public static Recipe find(Entity entity){var id=net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());var exact=recipes.stream().filter(r->r.entity().equals(id)).findFirst().orElse(null);if(exact!=null)return exact;
        // Preserve old class inheritance matches for modern split horse types and derived mod mobs.
        String fallback=null;
        if(entity instanceof tc4revival.content.BrainyZombieEntity)fallback="thaumcraft:brainy_zombie";
        else if(entity instanceof net.minecraft.world.entity.animal.horse.AbstractHorse)fallback="minecraft:horse";
        else if(entity instanceof net.minecraft.world.entity.monster.Zombie)fallback="minecraft:zombie";
        else if(entity instanceof net.minecraft.world.entity.animal.Cow)fallback="minecraft:cow";
        else if(entity instanceof net.minecraft.world.entity.monster.Spider)fallback="minecraft:spider";
        else if(entity instanceof net.minecraft.world.entity.monster.Slime)fallback="minecraft:slime";
        if(fallback==null)return null;var target=ResourceLocation.parse(fallback);return recipes.stream().filter(r->r.entity().equals(target)).findFirst().orElse(null);}
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void reload(net.minecraftforge.event.AddReloadListenerEvent event){event.addListener(new MobEssenceCatalog());}
    @Override protected void apply(Map<ResourceLocation,JsonElement> data,ResourceManager manager,ProfilerFiller profiler){
        var next=new LinkedHashMap<ResourceLocation,Recipe>();data.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(file->{for(var raw:file.getValue().getAsJsonObject().getAsJsonArray("entries")){
            var row=raw.getAsJsonObject();var id=ResourceLocation.parse(row.get("entity").getAsString());if(!net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.containsKey(id))throw new IllegalArgumentException("Unknown summon entity "+id);
            var aspects=new ArrayList<String>();for(var a:row.getAsJsonArray("aspects")){String aspect=a.getAsString();if(!MobEssenceItem.ASPECTS.contains(aspect))throw new IllegalArgumentException("Unknown soul aspect "+aspect);aspects.add(aspect);}if(aspects.size()!=3)throw new IllegalArgumentException("Three soul aspects required");
            if(next.putIfAbsent(id,new Recipe(id,aspects))!=null)throw new IllegalArgumentException("Duplicate summon recipe "+id);
        }});if(next.size()>4096)throw new IllegalArgumentException("At most 4096 summoning recipes are supported");recipes=List.copyOf(next.values());
    }
}
