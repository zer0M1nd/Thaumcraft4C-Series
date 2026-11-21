package tc4revival.research;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import tc4revival.research.ResearchPage.*;
import tc4revival.research.ResearchPageRecipes.*;

class ResearchPageRecipesTest {
    @Test void liveRecipeBindingsDoNotRequireDuplicateLegacyDisplayDefinitions() {
        var recipes=ResearchPageRecipes.decode(new com.google.gson.JsonArray());
        var pages=BookTestData.pages(JsonParser.parseString("""
                [{"key":"test:RESEARCH","pages":[{"type":"ARCANE_CRAFTING","recipe":
                {"kind":"reference","class":"modern","keys":["test:crafting/example"]}}]}]
                """).getAsJsonArray());
        assertDoesNotThrow(()->recipes.validate(pages));
    }
    @Test void liveBindingsStillRejectInvalidIdsAndLegacyBindingsRequireDefinitions() {
        var recipes=ResearchPageRecipes.decode(new com.google.gson.JsonArray());
        for(String reference:List.of("\"class\":\"modern\",\"keys\":[\"test:UPPERCASE\"]",
                "\"class\":\"modern\",\"keys\":[\"unqualified\"]", "\"class\":\"legacy\",\"keys\":[\"test:missing\"]")) {
            var pages=BookTestData.pages(JsonParser.parseString("[{\"key\":\"test:RESEARCH\",\"pages\":[{\"type\":\"ARCANE_CRAFTING\",\"recipe\":{\"kind\":\"reference\","+reference+"}}]}]").getAsJsonArray());
            assertThrows(IllegalArgumentException.class,()->recipes.validate(pages));
        }
    }
    @Test void compoundReferenceDisplaysEveryAlternativeAndWrapsWithoutChangingItsIdentity() {
        var keys=List.of("gadomancy:activate_aura_core_aer","gadomancy:activate_aura_core_ignis",
                "gadomancy:activate_aura_core_aqua","gadomancy:activate_aura_core_terra",
                "gadomancy:activate_aura_core_ordo","gadomancy:activate_aura_core_perditio",
                "gadomancy:activate_aura_core_balanced");
        var reference=new Reference(keys,"modern");
        var observed=new java.util.ArrayList<String>();
        for(int i=0;i<7;i++) {
            observed.add(reference.displayKey(i*5000L));
            assertEquals(keys.get(i),reference.displayKey(i*5000L+4999));
        }
        assertEquals(keys,observed);
        assertEquals(keys.get(0),reference.displayKey(35000));
        assertEquals(keys,reference.keys());
        assertEquals("InfusionAltar",new Reference(List.of("InfusionAltar"),"legacy").displayKey(Long.MAX_VALUE));
    }
    private com.google.gson.JsonElement read(String path) throws Exception {
        try (var reader = new InputStreamReader(getClass().getResourceAsStream(path), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader);
        }
    }
    private ResearchPageRecipes generated() throws Exception {
        return ResearchPageRecipes.decode(read("/data/thaumcraft/research_page_recipes/legacy.json").getAsJsonObject().getAsJsonArray("entries"));
    }
    @Test void everyNamedReferenceResolvesAndGenerationPreservesAllObservedDetails() throws Exception {
        var recipes = generated();
        assertEquals(347, recipes.entries().size());
        assertEquals(ResearchPageRecipes.decode(read("/research-page-recipes.json").getAsJsonArray()).entries(), recipes.entries());
        recipes.validate(BookTestData.pages(read("/research-pages.json").getAsJsonArray()));
        assertEquals(24, recipes.entries().values().stream().filter(e -> e.definition() instanceof Enchanting).count());
        assertEquals(9, recipes.entries().values().stream().filter(e -> e.definition() instanceof Compound).count());
    }
    @Test void infusionModifiesTheInputTagInsteadOfReplacingItWithAFixedStack() throws Exception {
        var recipe = (Infusion) generated().require("HelmGoggles").definition();
        assertInstanceOf(Item.class, recipe.input());
        assertEquals(new Sequence(List.of(new Literal("goggles"), new Nbt("net.minecraft.nbt.NBTTagByte", "1b"))), recipe.output());
        var golem = (Infusion) generated().require("AdvancedGolem").definition();
        assertEquals(new Literal("advanced"), ((Sequence) golem.output()).elements().get(0));
    }
    @Test void compoundStructuresKeepTheirActualDimensionsEmptyCellsAndWandNbt() throws Exception {
        var altar = (Compound) generated().require("InfusionAltar").definition();
        assertEquals(3, altar.width()); assertEquals(3, altar.height()); assertEquals(3, altar.depth());
        assertEquals(27, altar.blocks().elements().size()); assertInstanceOf(Empty.class, altar.blocks().elements().get(1));
        assertEquals(List.of("ignis", "terra", "ordo", "aer", "perditio", "aqua"), List.copyOf(altar.aspects().keySet()));
        var book = (Compound) generated().require("Thaumonomicon").definition();
        assertEquals(2, book.blocks().elements().size());
        assertTrue(((Item) book.blocks().elements().get(0)).stack().nbt().orElseThrow().contains("rod:\"wood\""));
    }
    @Test void jarLabelsRequireMatchingIngredientNbtAndKeepTheirFixedOutput() throws Exception {
        var recipe = (Crafting) generated().require("JarLabel0").definition();
        assertTrue(recipe.matchesInputNbt()); assertEquals(0, recipe.width());
        assertTrue(((Item) recipe.ingredients().elements().get(1)).stack().nbt().orElseThrow().contains("amount:8"));
        assertTrue(((Item) recipe.output()).stack().nbt().orElseThrow().contains("amount:0"));
        assertFalse(((Crafting) generated().require("Table").definition()).matchesInputNbt());
    }
    @Test void enchantmentBaseCostsAndOreNamesRemainExplicit() throws Exception {
        var recipes = generated(); var protection = (Enchanting) recipes.require("InfEnch0").definition();
        assertEquals(0, protection.enchantmentId()); assertEquals(4, protection.maxLevel());
        assertEquals(1, protection.baseXp()); assertEquals(1, protection.instability());
        var table = (Crafting) recipes.require("Table").definition();
        assertTrue(table.ingredients().elements().stream().anyMatch(e -> e instanceof Ore ore && ore.names().contains("plankWood")));
        assertThrows(UnsupportedOperationException.class, () -> recipes.entries().clear());
    }
    @Test void missingReferencesAndInvalidCompoundDimensionsAreRejected() throws Exception {
        var recipes = generated();
        var pages = BookTestData.pages(JsonParser.parseString("[{\"key\":\"X\",\"pages\":[{\"type\":\"NORMAL_CRAFTING\",\"aspects\":{},\"recipe\":{\"kind\":\"reference\",\"keys\":[\"MISSING\"],\"class\":\"x\"}}]}]").getAsJsonArray());
        assertThrows(IllegalArgumentException.class, () -> recipes.validate(pages));
        assertThrows(IllegalArgumentException.class, () -> new Compound(java.util.Map.of(), 3, 3, 3, new Sequence(List.of())));
    }
}
