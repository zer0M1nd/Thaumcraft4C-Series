package tc4revival.content;

import java.util.*;
import com.google.gson.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.RandomSource;

public  class PechTrades {
    public record Entry(int value,ItemStack stack,boolean prechargedAmulet) {
        public Entry(int value,ItemStack stack) {this(value,stack,false);}
        public Entry { stack=stack.copy(); }
        @Override public ItemStack stack() { return stack.copy(); }
        public ItemStack generate(RandomSource random) { return prechargedAmulet?LegacyLootTemplates.prechargedAmulet(random::nextInt):stack(); }
    }
    private static volatile List<List<Entry>> pools=List.of();
    public static List<List<Entry>> decode(JsonObject root) {
        var result=new ArrayList<List<Entry>>();
        for (var profession:root.getAsJsonArray("professions")) {
            var rows=new ArrayList<Entry>();
            for (var data:profession.getAsJsonArray()) { var row=data.getAsJsonObject(); int value=row.get("value").getAsInt(); var stack=LegacyLootTemplates.decode(row.getAsJsonObject("stack"));
                if (value<1 || value>5 || stack.isEmpty()) throw new IllegalArgumentException("Invalid Pech trade"); rows.add(new Entry(value,stack,net.minecraft.util.GsonHelper.getAsBoolean(row.getAsJsonObject("stack"),"precharged_amulet",false))); }
            for (int value=1;value<=5;value++) { final int cost=value; if (rows.stream().noneMatch(row->row.value()==cost)) throw new IllegalArgumentException("Missing Pech value tier "+cost); }
            result.add(List.copyOf(rows));
        }
        if (result.size()!=3) throw new IllegalArgumentException("Pech requires all three professions"); return List.copyOf(result);
    }
    public static ItemStack select(int profession,int value,RandomSource random) {
        var eligible=pools.get(profession).stream().filter(row->row.value()==value).toList();
        return eligible.isEmpty()?ItemStack.EMPTY:eligible.get(random.nextInt(eligible.size())).generate(random);
    }
    public static ItemStack dungeon(RandomSource random) {
        var eligible=LegacyChestTables.require(ResourceLocation.parse("thaumcraft:dungeon")).entries().stream().filter(row->row.weight()<=5 && row.maximum()<=1).toList();
        if(eligible.isEmpty())return ItemStack.EMPTY;
        var entry=eligible.get(random.nextInt(eligible.size()));
        return entry.generate(random);
    }
    public static void reload(net.minecraftforge.event.AddReloadListenerEvent event) { event.addListener(new SimpleJsonResourceReloadListener(new Gson(),"pech_trades") {
        @Override protected void apply(Map<ResourceLocation,JsonElement> files,ResourceManager resources,net.minecraft.util.profiling.ProfilerFiller profiler) {
            var data=files.get(ResourceLocation.parse("thaumcraft:default")); if (data==null) throw new IllegalArgumentException("Missing Pech trades"); pools=decode(data.getAsJsonObject());
        }
    }); }
}
