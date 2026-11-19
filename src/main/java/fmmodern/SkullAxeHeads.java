package fmmodern;

import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

/** Optional heads are resolved from the registry; neither addon is a required dependency. */
final class SkullAxeHeads {
    private SkullAxeHeads() {}
    private static final Map<String,List<String>> HEADS = Map.ofEntries(
        Map.entry("skeleton",List.of("minecraft:skeleton_skull")),
        Map.entry("wither_skeleton",List.of("minecraft:wither_skeleton_skull")),
        Map.entry("zombie",List.of("minecraft:zombie_head")),
        Map.entry("creeper",List.of("minecraft:creeper_head")),
        Map.entry("player",List.of("minecraft:player_head")),
        Map.entry("piglin",List.of("minecraft:piglin_head")),
        // Prefer Ender IO when both addons supply this head; award only one head per roll.
        Map.entry("enderman",List.of("enderio:enderman_head","tconstruct:enderman_head")),
        Map.entry("blaze",List.of("tconstruct:blaze_head")),
        Map.entry("stray",List.of("tconstruct:stray_head")),
        Map.entry("husk",List.of("tconstruct:husk_head")),
        Map.entry("drowned",List.of("tconstruct:drowned_head")),
        Map.entry("spider",List.of("tconstruct:spider_head")),
        Map.entry("cave_spider",List.of("tconstruct:cave_spider_head")),
        Map.entry("piglin_brute",List.of("tconstruct:piglin_brute_head")),
        Map.entry("zombified_piglin",List.of("tconstruct:zombified_piglin_head")));

    static Item resolve(EntityType<?> type) {
        var id=ForgeRegistries.ENTITY_TYPES.getKey(type);
        if(id==null || !id.getNamespace().equals("minecraft"))return null;
        for(var candidate:HEADS.getOrDefault(id.getPath(),List.of())) {
            var item=ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(candidate));
            if(item!=null && item!=Items.AIR)return item;
        }
        return null;
    }
}
