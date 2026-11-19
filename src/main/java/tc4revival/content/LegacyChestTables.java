package tc4revival.content;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Container;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

/** Server data-pack pools used by old structure chests. Publication is atomic on successful reload. */
public  class LegacyChestTables {
    private static volatile Map<ResourceLocation,LegacyChestContents> tables=Map.of();
    private LegacyChestTables() {}
    public static void addReloadListener(AddReloadListenerEvent event) { event.addListener(new Reload()); }
    public static void stopped(ServerStoppedEvent event) { tables=Map.of(); }
    public static LegacyChestContents require(ResourceLocation id) {
        var result=tables.get(id);
        if (result==null) throw new IllegalStateException("Legacy chest pool has not loaded: "+id);
        return result;
    }
    public static void fill(ResourceLocation id,Container inventory,int rounds,IntUnaryOperator random) { require(id).fill(inventory,rounds,random); }
    public static void fill(ResourceLocation id,Container inventory,int rounds,net.minecraft.util.RandomSource random) { require(id).fill(inventory,rounds,random); }
    private static  class Reload extends SimpleJsonResourceReloadListener {
        Reload() { super(new Gson(),"legacy_chest_loot"); }
        @Override protected void apply(Map<ResourceLocation,JsonElement> files,ResourceManager resources,ProfilerFiller profiler) {
            var loaded=new java.util.LinkedHashMap<ResourceLocation,LegacyChestContents>();
            files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry->loaded.put(entry.getKey(),LegacyChestContents.decode(entry.getValue().getAsJsonObject())));
            tables=Map.copyOf(loaded);
        }
    }
}
