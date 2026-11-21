package tc4revival.content;

import com.google.gson.*;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

public  class GolemFishingLoot {
    public record Entry(ItemStack stack,int weight,float damage,boolean enchant) {
        public Entry {
            stack=stack.copy();
            if (stack.isEmpty() || weight<=0 || !Float.isFinite(damage) || damage<0 || damage>1
                    || damage>0 && (int)(stack.getMaxDamage()*damage)<1) throw new IllegalArgumentException("Invalid golem fishing reward");
        }
        @Override public ItemStack stack() { return stack.copy(); }
    }
    private static volatile Map<String,List<Entry>> pools=Map.of();
    private GolemFishingLoot() {}
    public static Map<String,List<Entry>> decode(JsonObject root) {
        var result=new HashMap<String,List<Entry>>();
        for (var name:List.of("junk","treasure","fish")) {
            var entries=new ArrayList<Entry>(); long total=0;
            for (var value:root.getAsJsonArray(name)) {
                var row=value.getAsJsonObject();
                var entry=new Entry(LegacyLootTemplates.decode(row.getAsJsonObject("stack")),row.get("weight").getAsInt(),row.get("damage").getAsFloat(),row.get("enchant").getAsBoolean());
                entries.add(entry); total+=entry.weight();
            }
            if (entries.isEmpty() || total>Integer.MAX_VALUE) throw new IllegalArgumentException("Invalid golem fishing pool "+name);
            result.put(name,List.copyOf(entries));
        }
        return Map.copyOf(result);
    }
    public static ItemStack select(String pool,RandomSource random) {
        var entries=pools.get(pool);
        if (entries==null) throw new IllegalStateException("Missing golem fishing pool "+pool);
        int roll=random.nextInt(entries.stream().mapToInt(Entry::weight).sum());
        for (var entry:entries) {
            if (roll>=entry.weight()) { roll-=entry.weight(); continue; }
            var stack=entry.stack();
            if (entry.damage()>0) {
                int limit=(int)(entry.damage()*stack.getMaxDamage());
                int wear=stack.getMaxDamage()-random.nextInt(random.nextInt(limit)+1);
                stack.setDamageValue(Math.max(1,Math.min(limit,wear)));
            }
            return entry.enchant()?LegacyRandomEnchantments.apply(stack,30,random):stack;
        }
        throw new IllegalStateException("Invalid fishing selection");
    }
    public static List<ItemStack> nativeLoot(net.minecraft.server.level.ServerLevel level,String pool,GolemBobberEntity bobber,int luck) {
        var id=switch(pool) {case "junk"->net.minecraft.world.level.storage.loot.BuiltInLootTables.FISHING_JUNK;case "treasure"->net.minecraft.world.level.storage.loot.BuiltInLootTables.FISHING_TREASURE;default->net.minecraft.world.level.storage.loot.BuiltInLootTables.FISHING_FISH;};
        var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,bobber.position())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL,new ItemStack(net.minecraft.world.item.Items.FISHING_ROD))
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,bobber)
                .withLuck(luck).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.FISHING);
        return level.getServer().getLootData().getLootTable(id).getRandomItems(params);
    }
    public static void reload(net.minecraftforge.event.AddReloadListenerEvent event) {
        event.addListener(new SimpleJsonResourceReloadListener(new Gson(),"golem_fishing") {
            @Override protected void apply(Map<ResourceLocation,JsonElement> files,ResourceManager resources,net.minecraft.util.profiling.ProfilerFiller profiler) {
                var data=files.get(ResourceLocation.parse("thaumcraft:default"));
                if (data==null) throw new IllegalArgumentException("Missing golem fishing definition");
                pools=decode(data.getAsJsonObject());
            }
        });
    }
}
