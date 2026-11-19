package gadomancymodern.client;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Two pseudo-random alphabetic words from the current translations, as in the author's jar. */
@Mod.EventBusSubscriber(modid="gadomancy",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public class JarNetworkNames {
    protected static List<String> values=List.of();
    protected static final Map<UUID,String> NAMES=new HashMap<>();
    @SubscribeEvent public static void reload(net.minecraftforge.client.event.RegisterClientReloadListenersEvent event){event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)manager->{
        var map=new LinkedHashMap<String,String>();var selected=Minecraft.getInstance().getLanguageManager().getSelected();
        for(String language:selected.equals("en_us")?List.of("en_us"):List.of("en_us",selected))for(var entry:manager.listResourceStacks("lang",path->path.getPath().equals("lang/"+language+".json")).entrySet())for(var resource:entry.getValue())try(var reader=resource.openAsReader()){
            com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().entrySet().forEach(value->{if(value.getValue().isJsonPrimitive())map.put(value.getKey(),value.getValue().getAsString());});
        }catch(java.io.IOException|RuntimeException ignored){}
        values=map.values().stream().filter(value->Arrays.stream(value.split(" ")).anyMatch(JarNetworkNames::valid)).toList();NAMES.clear();
    });}
    protected static boolean valid(String value){return value.length()>=4&&value.length()<=8&&value.chars().allMatch(Character::isAlphabetic);}
    public static String name(UUID id){return NAMES.computeIfAbsent(id,key->{
        if(values.isEmpty())return key.toString().substring(0,8);
        var random=new Random(key.getLeastSignificantBits());var result=new StringBuilder();int count=0;
        // Bound retry count: a resource pack must not be able to hang tooltip rendering indefinitely.
        for(int tries=0;tries<1000&&count<2;tries++){var words=values.get(random.nextInt(values.size())).split(" ");var word=words[random.nextInt(words.length)];if(!valid(word))continue;word=word.toLowerCase(Locale.ROOT);result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));count++;}
        return result.isEmpty()?key.toString().substring(0,8):result.toString();
    });}
}
