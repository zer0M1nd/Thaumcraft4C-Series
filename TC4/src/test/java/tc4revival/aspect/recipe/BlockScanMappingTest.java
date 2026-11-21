package tc4revival.aspect.recipe;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.LegacyAspect;
import tc4revival.aspect.item.BlockScanMapping;
import tc4revival.aspect.item.ItemAspectResolver;
import tc4revival.aspect.item.ItemBaseAspectSession;
import tc4revival.aspect.item.LegacyItemIdentity;
import tc4revival.aspect.item.LegacyItemMappings;
import tc4revival.aspect.item.StackAspectBonuses;
import tc4revival.aspect.recipe.RecipeAspectData.Key;
import tc4revival.data.LegacyItemDefinitions;
import static org.junit.jupiter.api.Assertions.*;

class BlockScanMappingTest {
    private final BlockScanMapping mapping = mapping();

    @Test void generatedRulesRoundTripAndHaveConsistentPickedItemIdentities() {
        assertEquals(351, mapping.rules().size());
        assertEquals(343, mapping.rules().stream().map(BlockScanMapping.Rule::block).distinct().count());
        assertEquals(mapping.rules(), BlockScanMapping.decode(BlockScanMapping.encode(mapping.rules())).rules());
        var aliases = aliases();
        for (var rule : mapping.rules()) {
            if (rule.target().modernItem() != null && rule.target().legacyKey() != null) {
                assertEquals(rule.target().legacyKey(), aliases.items().get(rule.target().modernItem()), rule.toString());
            }
        }
    }

    @Test void worldSelectionKeepsTheOldPickAndMetadataRules() {
        key("brown_mushroom_block", "brown_mushroom", 0);
        key("red_mushroom_block", "red_mushroom", 0);
        key("mushroom_stem", "brown_mushroom", 0); // Reviewed identity-loss adaptation.
        key("potted_fern", "tallgrass", 2);
        key("potted_blue_orchid", "red_flower", 1);
        key("oak_wood", "log", 0);
        key("dark_oak_wood", "log2", 1);
        key("coarse_dirt", "dirt", 0);
        key("smooth_stone", "stone_slab", 0);
        key("smooth_sandstone", "stone_slab", 1);
        key("smooth_quartz", "stone_slab", 7);
        key("water", "water", 0);
        key("lava", "lava", 0);
        key("nether_portal", "portal", 0);
        key("end_portal", "end_portal", 0);
        key("spawner", "mob_spawner", 0);
        key("wheat", "wheat_seeds", 0);
        key("cocoa", "dye", 3);
        key("tripwire", "string", 0);
        for (int moisture = 0; moisture < 8; moisture++) {
            assertEquals(new Key("minecraft:dirt", moisture == 1 ? 0 : moisture), mapping.resolve("minecraft:farmland", Map.of("moisture", "" + moisture)).orElseThrow().legacyKey());
        }
        assertEquals(new Key("minecraft:sticky_piston", 0), mapping.resolve("minecraft:piston_head", Map.of("type", "sticky", "facing", "up")).orElseThrow().legacyKey());
        assertEquals(new Key("minecraft:piston", 0), mapping.resolve("minecraft:piston_head", Map.of("type", "normal", "facing", "west")).orElseThrow().legacyKey());
        assertTrue(mapping.resolve("minecraft:farmland", Map.of("moisture", "8")).isEmpty());
    }

    @Test void all349PickedKeysMatchObservedBaseAspectsAndHashesWithoutCulling() {
        var catalog = new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
        var data = LegacyRecipeFixture.data();
        var aliases = aliases();
        var identity = new LegacyItemIdentity(aliases.items(), aliases.groups(), data);
        var observations = new HashMap<Key, com.google.gson.JsonObject>();
        var hashes = new HashMap<Key, Integer>();
        for (var value : LegacyRecipeFixture.read("bonus-vanilla-aspects.json")) observations.put(LegacyRecipeFixture.key(value.getAsJsonObject()), value.getAsJsonObject());
        for (var value : LegacyRecipeFixture.read("scan-vanilla-aspects.json")) hashes.put(LegacyRecipeFixture.key(value.getAsJsonObject()), value.getAsJsonObject().get("initial_hash").getAsInt());
        int checked = 0;
        for (var rule : mapping.rules()) {
            var target = rule.target();
            if (target.legacyKey() == null) continue;
            var observation = observations.get(target.legacyKey());
            assertNotNull(observation, rule.toString());
            AspectAmounts base = target.modernItem() == null ? new RecipeAspectEngine(data, catalog, Map.of(), (key,value)->value, new InferenceLimits(64,6)).object(identity.scanKey(target.legacyKey()))
                    : new ItemBaseAspectSession(identity, data, catalog, new ItemAspectResolver(LegacyItemDefinitions.rules(), catalog), (id, tag) -> true, (key,value)->value, null, new InferenceLimits(64,6)).base(target.modernItem()).orElse(null);
            var actual = new StackAspectBonuses(catalog).apply(base, null, null, List.of());
            assertEquals(new ArrayList<>(LegacyRecipeFixture.quantities(observation.getAsJsonObject("base")).entrySet()), new ArrayList<>(values(actual).entrySet()), rule.toString());
            assertEquals(hashes.get(target.legacyKey()), identity.scanHash(target.legacyKey()), rule.toString());
            checked++;
        }
        assertEquals(349, checked);
    }

    @Test void stateFlatteningMatches102RepresentableActualOldMetadataObservations() {
        int checked = 0;
        for (var value : LegacyRecipeFixture.read("block-picked-metadata.json")) {
            var row = value.getAsJsonObject();
            String oldBlock = row.get("block").getAsString().substring(10);
            int metadata = row.get("metadata").getAsInt();
            assertEquals(metadata, row.get("stored_metadata").getAsInt());
            String modern = null;
            var properties = Map.<String, String>of();
            var woods = List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak");
            switch (oldBlock) {
                case "farmland" -> { if (metadata < 8) { modern = "farmland"; properties = Map.of("moisture", "" + metadata); } }
                case "dirt" -> { if (metadata < 3) modern = List.of("dirt", "coarse_dirt", "podzol").get(metadata); }
                case "log" -> modern = woods.get(metadata & 3) + ((metadata & 12) == 12 ? "_wood" : "_log");
                case "log2" -> { if ((metadata & 3) < 2) modern = woods.get(4 + (metadata & 3)) + ((metadata & 12) == 12 ? "_wood" : "_log"); }
                case "quartz_block" -> { if (metadata < 5) modern = List.of("quartz_block", "chiseled_quartz_block", "quartz_pillar", "quartz_pillar", "quartz_pillar").get(metadata); }
                case "stone_slab", "double_stone_slab" -> modern = List.of("smooth_stone_slab", "sandstone_slab", "petrified_oak_slab", "cobblestone_slab", "brick_slab", "stone_brick_slab", "nether_brick_slab", "quartz_slab").get(metadata & 7);
                case "wooden_slab" -> { if ((metadata & 7) < 6) modern = woods.get(metadata & 7) + "_slab"; }
                case "anvil" -> { if (metadata < 12) modern = List.of("anvil", "chipped_anvil", "damaged_anvil").get(metadata / 4); }
                case "monster_egg" -> { if (metadata < 6) modern = List.of("infested_stone", "infested_cobblestone", "infested_stone_bricks", "infested_mossy_stone_bricks", "infested_cracked_stone_bricks", "infested_chiseled_stone_bricks").get(metadata); }
                default -> fail("Unexpected observation " + oldBlock);
            }
            if (modern == null) continue; // Unused old metadata, not a reason to add new content.
            assertEquals(row.get("picked_metadata").getAsInt(), mapping.resolve("minecraft:" + modern, properties).orElseThrow().legacyKey().metadata(), row.toString());
            checked++;
        }
        assertEquals(102, checked);
    }

    @Test void newContentCannotAcquireAnOldIdentityThroughAPickEquivalent() {
        for (String block : List.of("lava_cauldron", "powder_snow_cauldron", "dirt_path", "potted_bamboo", "potted_azalea_bush", "dragon_head", "piglin_wall_head", "stripped_oak_log", "cherry_log", "deepslate_diamond_ore", "granite", "soul_fire", "candle_cake", "pumpkin", "white_bed", "beetroots", "moving_piston", "smooth_red_sandstone")) {
            assertTrue(mapping.resolve("minecraft:" + block, Map.of()).isEmpty(), block);
        }
        for (String block : List.of("copper_ore", "deepslate_copper_ore")) {
            var target = mapping.resolve("minecraft:" + block, Map.of()).orElseThrow();
            assertNull(target.legacyKey());
            assertEquals("minecraft:" + block, target.modernItem());
        }
    }

    @Test void ambiguousRulesAndInvalidResourceValuesAreRejected() {
        var target = new BlockScanMapping.Target("minecraft:dirt", new Key("minecraft:dirt", 0));
        var rule = new BlockScanMapping.Rule("minecraft:farmland", Map.of("moisture", "1"), target);
        assertThrows(IllegalArgumentException.class, () -> new BlockScanMapping(List.of(rule, rule)));
        assertThrows(IllegalArgumentException.class, () -> new BlockScanMapping(List.of(rule, new BlockScanMapping.Rule(rule.block(), Map.of(), target))));
        var input = new HashMap<String, String>();
        input.put("moisture", "1");
        var immutable = new BlockScanMapping.Rule(rule.block(), input, target);
        input.put("moisture", "7");
        assertTrue(immutable.matches(Map.of("moisture", "1")));
        var json = BlockScanMapping.encode(List.of(rule));
        json.addProperty("format", 1.5);
        assertThrows(ArithmeticException.class, () -> BlockScanMapping.decode(json));
        assertThrows(IllegalArgumentException.class, () -> new BlockScanMapping.Target(null, null));
        assertThrows(IllegalArgumentException.class, () -> new BlockScanMapping.Target("minecraft:air", null));
    }

    private void key(String block, String item, int metadata) {
        assertEquals(new Key("minecraft:" + item, metadata), mapping.resolve("minecraft:" + block, Map.of()).orElseThrow().legacyKey(), block);
    }
    private static Map<String, Integer> values(AspectAmounts amounts) {
        var values = new LinkedHashMap<String, Integer>();
        amounts.view().forEach((aspect, quantity) -> values.put(aspect.tag(), quantity));
        return values;
    }
    private static BlockScanMapping mapping() {
        return BlockScanMapping.decode(read("block_scan_targets"));
    }
    private static LegacyItemMappings.Definition aliases() {
        return LegacyItemMappings.decode(read("legacy_item_mappings"));
    }
    private static com.google.gson.JsonElement read(String folder) {
        try (var reader = new InputStreamReader(BlockScanMappingTest.class.getResourceAsStream("/data/thaumcraft/" + folder + "/vanilla.json"), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader);
        } catch (Exception failure) { throw new IllegalStateException(failure); }
    }
}
