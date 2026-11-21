package thaumicfutures.compat;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.Deserializers;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class EvilCraftCompat {
    private EvilCraftCompat() {}
    public static void register(DeferredRegister<Item> items) {
        items.register("blood_wand_cap", () -> new Item(new Item.Properties()));
        items.register("veined_scribing_tools", VeinedScribingToolsItem::new);
        MinecraftForge.EVENT_BUS.addListener(EvilCraftCompat::loot);
    }
    private static void loot(LootTableLoadEvent event) {
        var id = event.getName();
        if (!id.getNamespace().equals("thaumcraft") || !id.getPath().matches("bags/items_[0-2]")) return;
        int rarity = id.getPath().charAt(id.getPath().length() - 1) - '0';
        var gson = Deserializers.createLootTableSerializer().create();
        var definition = gson.toJsonTree(event.getTable()).getAsJsonObject();
        var pools = definition.getAsJsonArray("pools");
        if (pools == null || pools.isEmpty()) return;
        var entries = pools.get(0).getAsJsonObject().getAsJsonArray("entries");
        add(entries, "dark_gem", new int[]{1, 2, 5}, new int[]{100, 80, 40});
        add(entries, "dark_gem_crushed", new int[]{1, 2, 5}, new int[]{80, 60, 20});
        if (rarity > 0) add(entries, "garmonbozia", new int[]{1, 2, 3}, new int[]{5, 3, 1});
        // Forge's pool deserializer needs its load context even when rebuilding an existing table.
        // custom=true suppresses a recursive LootTableLoadEvent for this derived table.
        event.setTable(net.minecraftforge.common.ForgeHooks.loadLootTable(gson, id, definition, true));
    }
    private static void add(com.google.gson.JsonArray entries, String item, int[] counts, int[] weights) {
        String id = "evilcraft:" + item;
        if (!ForgeRegistries.ITEMS.containsKey(new ResourceLocation(id))) return;
        for (int i = 0; i < counts.length; i++) {
            var entry = new JsonObject();
            entry.addProperty("type", "minecraft:item"); entry.addProperty("name", id); entry.addProperty("weight", weights[i]);
            var count = new JsonObject(); count.addProperty("function", "minecraft:set_count"); count.addProperty("count", counts[i]);
            var functions = new com.google.gson.JsonArray(); functions.add(count); entry.add("functions", functions);
            var conditions=new com.google.gson.JsonArray();
            conditions.add(thaumicfutures.FeatureLootCondition.json(thaumicfutures.FutureFeatures.Group.EVILCRAFT));
            entry.add("conditions",conditions);
            entries.add(entry);
        }
    }
}
