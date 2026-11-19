package tc4revival.research;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResearchPagesTest {
    private ResearchPages observed() throws Exception {
        try (var reader = new InputStreamReader(getClass().getResourceAsStream("/research-pages.json"), StandardCharsets.UTF_8)) {
            return BookTestData.pages(JsonParser.parseReader(reader).getAsJsonArray());
        }
    }
    private ResearchPages generated() throws Exception {
        try (var reader = new InputStreamReader(getClass().getResourceAsStream("/data/thaumcraft/research_pages/legacy.json"), StandardCharsets.UTF_8);
             var catalogReader = new InputStreamReader(getClass().getResourceAsStream("/data/thaumcraft/research_catalog/legacy.json"), StandardCharsets.UTF_8)) {
            var root = JsonParser.parseReader(reader).getAsJsonObject(); assertEquals(1, root.get("format").getAsInt());
            var result = BookTestData.pages(root.getAsJsonArray("entries"));
            var aspects = new tc4revival.aspect.AspectCatalog(java.util.Arrays.stream(tc4revival.aspect.LegacyAspect.values())
                    .map(tc4revival.aspect.LegacyAspect::create).toList());
            result.validate(ResearchCatalog.decode(JsonParser.parseReader(catalogReader).getAsJsonObject().getAsJsonArray("categories"), aspects));
            return result;
        }
    }
    @Test void generatedDataPreservesEveryObservedPageAndRecipeDetail() throws Exception {
        var observed = observed(); var bundled = generated();
        var retained = new java.util.HashMap<>(observed.entries());
        retained.remove("BANNERS");
        var modern = new java.util.HashMap<>(bundled.entries());
        assertFalse(modern.containsKey("ENDLESSRESEARCH"));
        var enchantmentPages = modern.get("INFUSIONENCHANTMENT");
        assertEquals(44, enchantmentPages.size());
        assertEquals(retained.get("INFUSIONENCHANTMENT"), enchantmentPages.subList(0, 27));
        modern.put("INFUSIONENCHANTMENT", enchantmentPages.subList(0, 27));
        var copper=modern.get("ENTROPICPROCESSING");
        assertEquals(retained.get("ENTROPICPROCESSING").size()+4,copper.size());
        var copperKeys=copper.stream().filter(p->p.recipe() instanceof ResearchPage.Reference)
                .flatMap(p->((ResearchPage.Reference)p.recipe()).keys().stream()).filter(k->k.startsWith("thaumcraft:crucible/")).toList();
        assertEquals(40,copperKeys.size());assertEquals(40,new java.util.HashSet<>(copperKeys).size());
        for(String key:copperKeys)assertNotNull(getClass().getResource("/data/thaumcraft/recipes/"+key.substring("thaumcraft:".length())+".json"),key);
        modern.put("ENTROPICPROCESSING",copper.subList(0,retained.get("ENTROPICPROCESSING").size()));
        assertEquals(retained.keySet(), modern.keySet());
        retained.forEach((key, pages) -> assertEquals(pages, modern.get(key), key));
        var additions = List.of("depth_strider", "frost_walker", "binding_curse", "soul_speed", "swift_sneak",
                "sweeping", "mending", "vanishing_curse", "luck_of_the_sea", "lure", "loyalty", "impaling",
                "riptide", "channeling", "multishot", "quick_charge", "piercing");
        for (int i = 0; i < additions.size(); i++) {
            String name = additions.get(i);
            String id = "thaumcraft:infusion/enchantment/" + name;
            var expected = ResearchPage.decode(JsonParser.parseString("{\"id\":\"thaumcraft:INFUSIONENCHANTMENT/page/"+(28+i)+"\",\"page_break\":true,\"type\":\"INFUSION_ENCHANTMENT\","
                    + "\"recipe\":{\"kind\":\"reference\",\"class\":\"modern\",\"keys\":[\"" + id + "\"]}}").getAsJsonObject());
            assertEquals(expected, enchantmentPages.get(27 + i), name);
            try (var reader = new InputStreamReader(java.util.Objects.requireNonNull(getClass().getResourceAsStream(
                    "/data/thaumcraft/recipes/infusion/enchantment/" + name + ".json")), StandardCharsets.UTF_8)) {
                var recipe = JsonParser.parseReader(reader).getAsJsonObject();
                assertEquals("thaumcraft:infusion", recipe.get("type").getAsString(), name);
                assertEquals("minecraft:" + name, recipe.get("enchantment").getAsString(), name);
            }
        }
        assertEquals(194, observed.entries().size());
        assertEquals(576, observed.entries().values().stream().mapToInt(List::size).sum());
        var types = observed.entries().values().stream().flatMap(List::stream)
                .collect(Collectors.groupingBy(ResearchPage::type, Collectors.counting()));
        assertEquals(288L, types.get(ResearchPage.Type.TEXT));
        assertEquals(15L, types.get(ResearchPage.Type.TEXT_CONCEALED));
        assertEquals(89L, types.get(ResearchPage.Type.ARCANE_CRAFTING));
        assertEquals(62L, types.get(ResearchPage.Type.INFUSION_CRAFTING));
    }
    @Test void concealedPagesDependOnTheirOwnCompletedResearch() throws Exception {
        var pages = observed();
        for (var entry : pages.entries().entrySet()) {
            var visible = pages.visible(entry.getKey(), key -> false, Map.of());
            assertEquals(entry.getValue().stream().filter(p -> p.type() != ResearchPage.Type.TEXT_CONCEALED).toList(), visible);
            assertEquals(entry.getValue(), pages.visible(entry.getKey(), key -> true, Map.of()));
        }
    }
    @Test void aspectPagesSortTagsIncludeZeroPoolsAndSplitFourPerPage() throws Exception {
        var pages = observed(); int base = pages.require("ASPECTS").size();
        var result = pages.visible("ASPECTS", key -> false, Map.of("terra", 8, "aer", 0, "ordo", 4, "ignis", 3, "aqua", 2));
        assertEquals(base + 2, result.size());
        assertEquals(List.of("aer", "aqua", "ignis", "ordo"), new ArrayList<>(result.get(base).aspects().keySet()));
        assertEquals(0, result.get(base).aspects().get("aer"));
        assertEquals(Map.of("terra", 8), result.get(base + 1).aspects());
        assertThrows(UnsupportedOperationException.class, () -> result.get(base).aspects().clear());
    }
    @Test void sceptresPreserveEmptyGridCellsCostsAndWandNbt() throws Exception {
        var variants = (ResearchPage.Sequence) generated().require("SCEPTRE").get(1).recipe();
        assertEquals(3, variants.elements().size());
        int[] costs = {1, 13, 81}; String[] rods = {"wood", "greatwood", "silverwood"};
        for (int i = 0; i < 3; i++) {
            var recipe = (ResearchPage.Arcane) variants.elements().get(i);
            assertEquals(9, recipe.ingredients().elements().size());
            assertInstanceOf(ResearchPage.Empty.class, recipe.ingredients().elements().get(0));
            assertInstanceOf(ResearchPage.Item.class, recipe.ingredients().elements().get(4));
            assertEquals(4, recipe.ingredients().elements().stream().filter(e -> e instanceof ResearchPage.Empty).count());
            assertTrue(recipe.mirrored()); assertEquals(costs[i], recipe.aspects().get("aer"));
            assertTrue(recipe.output().nbt().orElseThrow().contains("rod:\"" + rods[i] + "\""));
            assertEquals(List.of("aer", "terra", "ignis", "aqua", "ordo", "perditio"), new ArrayList<>(recipe.aspects().keySet()));
        }
    }
    @Test void runicExamplesKeepDynamicUpgradeOutputsAndEscalatingCosts() throws Exception {
        var examples = (ResearchPage.Sequence) observed().require("RUNICAUGMENTATION").get(1).recipe();
        assertEquals(5, examples.elements().size());
        for (int i = 0; i < 5; i++) {
            var recipe = (ResearchPage.RunicExample) examples.elements().get(i);
            assertEquals(32 << i, recipe.aspects().get("potentia"));
            assertEquals(5 + i / 2, recipe.instability());
            assertEquals(i + 2, recipe.ingredients().elements().size());
            assertTrue(recipe.output().nbt().orElseThrow().contains("RS.HARDEN:" + (i + 1) + "b"));
            if (i == 0) assertTrue(recipe.input().nbt().isEmpty());
            else assertTrue(recipe.input().nbt().orElseThrow().contains("RS.HARDEN:" + i + "b"));
        }
    }
    @Test void duplicateEntriesAndUnresolvedRecipesFailInsteadOfShowingBlankPages() throws Exception {
        var source = JsonParser.parseString("[{\"key\":\"A\",\"pages\":[]},{\"key\":\"A\",\"pages\":[]}]").getAsJsonArray();
        assertThrows(IllegalArgumentException.class, () -> BookTestData.pages(source));
        var page = JsonParser.parseString("{\"type\":\"NORMAL_CRAFTING\",\"aspects\":{},\"recipe\":{\"kind\":\"unresolved\"}}").getAsJsonObject();
        assertThrows(IllegalArgumentException.class, () -> ResearchPage.decode(page));
        var generated = generated();
        assertThrows(IllegalArgumentException.class, () -> generated.require("MISSING"));
    }
}
