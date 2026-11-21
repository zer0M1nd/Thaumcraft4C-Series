package tc4revival.research;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResearchDisplayItemsTest {
    private ResearchDisplayItems generated() throws Exception {
        try (var reader = new InputStreamReader(getClass().getResourceAsStream("/data/thaumcraft/research_display_items/legacy.json"), StandardCharsets.UTF_8)) {
            return ResearchDisplayItems.decode(JsonParser.parseReader(reader).getAsJsonObject());
        }
    }
    private ResearchDisplayItems.Entry entry(ResearchDisplayItems data, String item, int metadata) {
        return data.entries().get(new ResearchDisplayItems.Identity(item, metadata, Optional.empty()));
    }
    @Test void everyObservedStackIdentityHasAnExplicitResolutionOrMissingDependency() throws Exception {
        var observed = new HashSet<ResearchDisplayItems.Identity>();
        for (String file : List.of("research-catalog.json", "research-pages.json", "research-page-recipes.json")) {
            try (var reader = new InputStreamReader(getClass().getResourceAsStream("/" + file), StandardCharsets.UTF_8)) {
                collect(JsonParser.parseReader(reader), observed);
            }
        }
        assertEquals(527, observed.size());
        try (var reader = new InputStreamReader(getClass().getResourceAsStream("/research-page-recipes.json"), StandardCharsets.UTF_8)) {
            for (var recipe : ResearchPageRecipes.decode(JsonParser.parseReader(reader).getAsJsonArray()).entries().values()) {
                if (recipe.definition() instanceof ResearchPageRecipes.Infusion infusion)
                    observed.add(ResearchDisplayItems.Identity.of(ResearchInfusionDisplay.output(infusion).stack()));
            }
        }
        var data = generated(); assertEquals(observed, data.entries().keySet());
        assertTrue(data.entries().values().stream().anyMatch(e -> e.missing().isPresent()));
        for (var entry : data.entries().values()) {
            assertEquals(entry.variants().isEmpty(), entry.missing().isPresent());
            for (var variant : entry.variants()) assertNotEquals("minecraft:air", variant.item());
        }
    }
    @Test void vanillaFlatteningAndIronNuggetReuseAreRealItemIdentities() throws Exception {
        var data = generated();
        assertEquals("minecraft:iron_nugget", entry(data, "Thaumcraft:ItemNugget", 0).variants().get(0).item());
        assertEquals("minecraft:gold_nugget", entry(data, "minecraft:gold_nugget", 0).variants().get(0).item());
        assertEquals(List.of("minecraft:oak_planks", "minecraft:spruce_planks", "minecraft:birch_planks", "minecraft:jungle_planks",
                "minecraft:acacia_planks", "minecraft:dark_oak_planks"), entry(data, "minecraft:planks", 32767).variants().stream().map(ResearchDisplayItems.Variant::item).toList());
        assertEquals(List.of("minecraft:coal", "minecraft:charcoal"), entry(data, "minecraft:coal", 32767).variants().stream().map(ResearchDisplayItems.Variant::item).toList());
    }
    @Test void wildcardDurabilityAndSubtypeCyclesRetainDistinctRules() throws Exception {
        var data = generated(); var bow = entry(data, "minecraft:bow", 32767);
        assertTrue(bow.cycleDamage()); assertTrue(bow.cycleCount()); assertEquals(1, bow.variants().size());
        var shards = entry(data, "Thaumcraft:ItemShard", 32767);
        assertFalse(shards.cycleDamage()); assertTrue(shards.cycleCount()); assertEquals(7, shards.variants().size());
        assertEquals("thaumcraft:balanced_shard", shards.variants().get(6).item());
        var glass = entry(data, "minecraft:glass", 32767);
        assertFalse(glass.cycleCount()); assertFalse(glass.cycleDamage()); assertEquals("minecraft:glass", glass.variants().get(0).item());
    }
    @Test void golemMaterialCyclesPreserveAdvancedInfusionPreview() throws Exception {
        var data=generated();
        var cycles=data.entries().values().stream().filter(e -> e.identity().item().equals("Thaumcraft:ItemGolemPlacer") && e.identity().metadata()==32767).toList();
        assertEquals(3,cycles.size());
        var materials=List.of("straw","wood","tallow","clay","flesh","stone","iron","thaumium");
        for (var cycle:cycles) {
            assertTrue(cycle.missing().isEmpty()); assertTrue(cycle.cycleCount()); assertFalse(cycle.cycleDamage());
            assertEquals(materials.stream().map(m -> "thaumcraft:golem_"+m).toList(),cycle.variants().stream().map(ResearchDisplayItems.Variant::item).toList());
            for (var variant:cycle.variants()) {
                if (cycle.identity().nbt().isPresent()) assertEquals("{advanced:1b}",variant.tag().orElseThrow());
                else assertTrue(variant.tag().isEmpty());
            }
        }
    }
    @Test void implementedItemsResolveWithoutPendingPortableTagAdapters() throws Exception {
        var data = generated();
        assertEquals("thaumcraft:alumentum", entry(data, "Thaumcraft:ItemResource", 0).variants().get(0).item());
        var wands = data.entries().values().stream().filter(e -> e.identity().item().equals("Thaumcraft:WandCasting") && e.identity().nbt().isPresent()).toList();
        assertFalse(wands.isEmpty());
        for (var wand : wands) {
            assertTrue(wand.missing().isEmpty());
            assertEquals("thaumcraft:casting_wand", wand.variants().get(0).item());
            assertTrue(wand.variants().get(0).tag().orElseThrow().contains("rod:"));
        }
        var unsupported = data.entries().values().stream().filter(e -> e.missing().filter("MOD_TAG_ADAPTER_REQUIRED"::equals).isPresent()).toList();
        assertTrue(unsupported.isEmpty());
        assertTrue(unsupported.stream().allMatch(e -> e.identity().nbt().isPresent() && e.variants().isEmpty()));
        var key = new tc4revival.aspect.recipe.RecipeAspectData.Key("minecraft:gold_nugget", 0);
        assertEquals(data.find(new ResearchDefinition.Stack(key, 1, false, Optional.empty(), Optional.empty())),
                data.find(new ResearchDefinition.Stack(key, 9, false, Optional.empty(), Optional.of("nuggetGold"))));
    }
    private void collect(JsonElement element, java.util.Set<ResearchDisplayItems.Identity> values) {
        if (element.isJsonArray()) element.getAsJsonArray().forEach(value -> collect(value, values));
        else if (element.isJsonObject()) {
            var row = element.getAsJsonObject();
            if (row.has("item") && row.has("metadata") && row.has("count") && row.has("damageable")) values.add(ResearchDisplayItems.Identity.decode(row));
            row.entrySet().forEach(entry -> collect(entry.getValue(), values));
        }
    }
}
