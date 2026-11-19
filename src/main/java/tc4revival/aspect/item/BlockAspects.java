package tc4revival.aspect.item;

import com.google.gson.*;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.*;

public class BlockAspects {
    private static BlockAspectTable table;
    private static Map<ResourceLocation,JsonElement> authoredFiles=Map.of();
    public static boolean defined(String id){return table!=null&&table.rules().stream().anyMatch(r->r.block().equals(id));}
    public static void inference(JsonElement additional){
        var files=new TreeMap<ResourceLocation,JsonElement>(authoredFiles);files.put(ResourceLocation.parse("thaumcraft:advanced_inference"),additional);
        table=new BlockAspectTable(files.values(),TCAspects.catalog(),AspectRegistryTargets::block);
        tc4revival.aspect.sync.ServerAspectData.capture("block_aspects",files);
    }
    public static Optional<AspectAmounts> resolve(BlockState state){return table==null?Optional.empty():table.resolve(ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString(),BlockScanTargets.properties(state));}
    public static void addReloadListener(AddReloadListenerEvent event){event.addListener(new SimpleJsonResourceReloadListener(new Gson(),"block_aspects"){
        @Override protected void apply(Map<ResourceLocation,JsonElement> files,ResourceManager manager,ProfilerFiller profiler){
            var candidate=new BlockAspectTable(files.values(),TCAspects.catalog(),AspectRegistryTargets::block);
            for(var rule:candidate.rules()){
                var id=new ResourceLocation(rule.block());if(!ForgeRegistries.BLOCKS.containsKey(id))throw new IllegalArgumentException("Unknown aspect block "+id);
                var definition=ForgeRegistries.BLOCKS.getValue(id).getStateDefinition();
                rule.properties().forEach((key,value)->{var property=definition.getProperty(key);if(property==null||property.getValue(value).isEmpty())throw new IllegalArgumentException("Invalid block aspect state "+id+" "+key+"="+value);});
            }
            authoredFiles=Map.copyOf(files);table=candidate;tc4revival.aspect.sync.ServerAspectData.capture("block_aspects",files);
        }
    });}
    public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event){table=null;}
}
