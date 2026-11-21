package wgmodern;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Modern atlases do not automatically discover the old plural items/ and blocks/ paths. */
@Mod.EventBusSubscriber(modid="witchinggadgets",bus=Mod.EventBusSubscriber.Bus.MOD)
public class LegacyAtlasData implements DataProvider {
    private final PackOutput output;
    public LegacyAtlasData(PackOutput output){this.output=output;}

    @SubscribeEvent public static void gather(GatherDataEvent event) {
        event.getGenerator().addProvider(event.includeClient(),new LegacyAtlasData(event.getGenerator().getPackOutput()));
    }

    @Override public CompletableFuture<?> run(CachedOutput cache) {
        try(var stream=LegacyAtlasData.class.getResourceAsStream("/data/witchinggadgets/thaumcraft/original_resources.json")) {
            if(stream==null)throw new IOException("Witching Gadgets original resource manifest is missing");
            var mapping=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonObject("mapping");
            var ids=new TreeSet<String>();
            String prefix="assets/witchinggadgets/textures/";
            for(String target:mapping.keySet()) {
                if(!target.startsWith(prefix)||!target.endsWith(".png"))continue;
                String path=target.substring(prefix.length(),target.length()-4);
                if(path.startsWith("items/")||path.startsWith("blocks/"))ids.add("witchinggadgets:"+path);
            }
            // These three icons come from the separate original Traveller's Gear resource source.
            for(String gear:new String[]{"cloak","pauldrons","vambraces"})
                ids.add("witchinggadgets:tg/items/simplegear_"+gear);
            var sources=new JsonArray();
            for(String id:ids) {
                var source=new JsonObject();
                source.addProperty("type","minecraft:single");
                source.addProperty("resource",id);
                sources.add(source);
            }
            var atlas=new JsonObject();atlas.add("sources",sources);
            // A single source also loads its resource pack's matching .png.mcmeta, preserving animation.
            return DataProvider.saveStable(cache,atlas,output.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
                    .resolve("minecraft/atlases/blocks.json"));
        } catch(IOException|RuntimeException error) {
            return CompletableFuture.failedFuture(error);
        }
    }

    @Override public String getName(){return "Witching Gadgets legacy item and block atlas sprites";}
}
