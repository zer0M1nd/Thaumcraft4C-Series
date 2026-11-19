package thmodern.golem;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.golems.GolemApi;
import thmodern.Horizons;

/** Named profiles and ordered block rules replace legacy material ordinals. */
public final class GolemProfiles {
    public record Profile(ResourceLocation id,GolemApi.Material material,int visCost,List<ResourceLocation> blocks,List<ResourceLocation> tags) {
        public Profile { blocks=List.copyOf(blocks);tags=List.copyOf(tags);if(visCost<0)throw new IllegalArgumentException("Negative animation cost"); }
        public boolean matches(BlockState state){return blocks.contains(net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock()))||tags.stream().anyMatch(tag->state.is(TagKey.create(Registries.BLOCK,tag)));}
    }
    private static final Map<String,Profile> PROFILES=new LinkedHashMap<>();
    public static ResourceLocation id(String name){return ResourceLocation.fromNamespaceAndPath(Horizons.ID,name);}
    public static void register(){
        try(var stream=GolemProfiles.class.getResourceAsStream("/data/thaumichorizons/golem_profiles.json")){
            if(stream==null)throw new IOException("Missing golem profiles");
            try(var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)){
                for(var value:JsonParser.parseReader(reader).getAsJsonArray()){
                    var row=value.getAsJsonObject();String key=row.get("id").getAsString();var id=id("material/"+key);
                    var material=new GolemApi.Material(row.get("health").getAsInt(),row.get("armor").getAsInt(),row.get("speed").getAsFloat(),row.get("fire_resistant").getAsBoolean(),row.get("upgrades").getAsInt(),row.get("carry").getAsInt(),row.get("regeneration_delay").getAsInt(),row.get("strength").getAsInt(),row.get("sinks").getAsBoolean(),id("textures/models/golem_void.png"));
                    if(PROFILES.putIfAbsent(key,new Profile(id,material,row.get("vis_cost").getAsInt(),ids(row,"blocks"),ids(row,"tags")))!=null)throw new IllegalArgumentException("Duplicate profile "+key);
                    GolemApi.registerMaterial(id,material);
                }
            }
        }catch(IOException failure){throw new IllegalStateException("Cannot load Horizons golem definitions",failure);}
        if(!PROFILES.containsKey("rock")||!PROFILES.containsKey("void"))throw new IllegalArgumentException("Missing fallback profiles");
        GolemApi.registerTask(AnimatedGolems.TASK,AnimatedGolems::installGoals);
    }
    private static List<ResourceLocation> ids(JsonObject row,String key){if(!row.has(key))return List.of();var result=new ArrayList<ResourceLocation>();for(var entry:row.getAsJsonArray(key))result.add(ResourceLocation.parse(entry.getAsString()));return result;}
    public static Profile select(BlockState state){return PROFILES.values().stream().filter(p->p.matches(state)).findFirst().orElseGet(()->PROFILES.get("rock"));}
    public static Profile require(String key){return Objects.requireNonNull(PROFILES.get(key),key);}
    private GolemProfiles(){}
}
