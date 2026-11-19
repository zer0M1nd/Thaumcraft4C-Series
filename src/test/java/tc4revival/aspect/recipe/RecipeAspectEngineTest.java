package tc4revival.aspect.recipe;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.LegacyAspect;
import tc4revival.aspect.recipe.RecipeAspectData.Ingredient;
import tc4revival.aspect.recipe.RecipeAspectData.Key;
import tc4revival.aspect.recipe.RecipeAspectData.Kind;
import tc4revival.aspect.recipe.RecipeAspectData.Recipe;
import tc4revival.aspect.recipe.RecipeAspectData.Registration;
import static org.junit.jupiter.api.Assertions.*;

class RecipeAspectEngineTest {
    private final AspectCatalog catalog = new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    private Key key(String name) { return new Key("test:" + name, 0); }
    private Registration source(String name, String aspect, int amount) { return new Registration(key(name), Map.of(aspect, amount)); }
    private Recipe recipe(Kind kind, String output, int count, String input, Map<String, Integer> cost) {
        return new Recipe(kind, key(output), count, List.of(Ingredient.direct(key(input))), cost);
    }
    private RecipeAspectEngine engine(List<Registration> initial, Recipe... recipes) {
        return new RecipeAspectEngine(new RecipeAspectData(Map.of(), initial, List.of(recipes)), catalog,Map.of(),(k,v)->v,new InferenceLimits(64,6));
    }

    @Test void craftingChoosesTheSmallestPositiveTotalAndKeepsFirstTie() {
        var engine = engine(List.of(source("earth", "terra", 8), source("water", "aqua", 4), source("fire", "ignis", 4)),
                recipe(Kind.CRAFTING, "out", 1, "earth", Map.of()), recipe(Kind.CRAFTING, "out", 1, "water", Map.of()),
                recipe(Kind.CRAFTING, "out", 1, "fire", Map.of()));
        var result = engine.object(key("out"));
        assertEquals(3, result.total());
        assertEquals(3, result.amount(catalog.require("aqua")));
    }

    @Test void arcaneChoosesLowestPositiveTotalWithSquareRootCostAfterRounding() {
        var engine = engine(List.of(source("earth", "terra", 8), source("water", "aqua", 4)),
                recipe(Kind.ARCANE, "out", 1, "water", Map.of("ignis", 4)),
                recipe(Kind.ARCANE, "out", 2, "earth", Map.of("ignis", 25)));
        var result = engine.object(key("out"));
        assertEquals(0, result.amount(catalog.require("terra")));
        assertEquals(2, result.amount(catalog.require("ignis")));
        assertEquals(3, result.amount(catalog.require("aqua")));
    }

    @Test void crucibleHasPriorityAndKeepsFullCatalystRegardlessOfOutputCount() {
        var engine = engine(List.of(source("earth", "terra", 8), source("water", "aqua", 12)),
                recipe(Kind.CRAFTING, "out", 1, "water", Map.of()), recipe(Kind.ARCANE, "out", 1, "water", Map.of()),
                recipe(Kind.INFUSION, "out", 1, "water", Map.of()),
                recipe(Kind.CRUCIBLE, "out", 2, "earth", Map.of("terra", 9)),
                recipe(Kind.CRUCIBLE, "out", 1, "water", Map.of()));
        var result = engine.object(key("out"));
        assertEquals(9, result.total());
        assertEquals(9, result.amount(catalog.require("terra")));
    }

    @Test void alternativesUseFirstNonemptyListAndInfusionUsesFirstRecipe() {
        var first = new Recipe(Kind.INFUSION, key("out"), 1,
                List.of(new Ingredient(List.of(key("missing"), key("water"), key("earth")), true)), Map.of("ignis", 9));
        var engine = engine(List.of(source("water", "aqua", 4), source("earth", "terra", 8)), first,
                recipe(Kind.INFUSION, "out", 1, "earth", Map.of()));
        var result = engine.object(key("out"));
        assertEquals(3, result.amount(catalog.require("aqua")));
        assertEquals(3, result.amount(catalog.require("ignis")));
    }

    @Test void failedDerivationIsNullOnlyBeforeItsEmptyCacheEntryExists() {
        var engine = engine(List.of());
        assertNull(engine.object(key("missing")));
        assertTrue(engine.object(key("missing")).isEmpty());
    }

    @Test void forksKeepParentSnapshotAndTheirOwnFailedDerivations() {
        var parent = engine(List.of());
        var earlier = parent.fork();
        assertNull(parent.object(key("missing")));
        var later = parent.fork();
        assertTrue(later.object(key("missing")).isEmpty());
        assertNull(earlier.object(key("missing")));
        assertNull(parent.object(key("second")));
        assertNull(later.object(key("second")));
        assertTrue(parent.fork().object(key("second")).isEmpty());

        var sibling = parent.fork();
        assertNull(later.object(key("child_only")));
        assertNull(sibling.object(key("child_only")));
        assertNull(parent.object(key("child_only")));
        assertTrue(later.fork().object(key("child_only")).isEmpty());
    }

    @Test void returnedAndCallerOwnedAmountsCannotChangeSharedSeedsOrDerivedSnapshots() {
        var terra = catalog.require("terra");
        var override = new AspectAmounts().add(terra, 8);
        var data = new RecipeAspectData(Map.of(), List.of(),
                List.of(recipe(Kind.CRAFTING, "out", 1, "earth", Map.of())));
        var parent = new RecipeAspectEngine(data, catalog, Map.of(key("earth"), override),
                (k, v) -> v, new InferenceLimits(64, 6));
        override.add(terra, 20);
        var early = parent.fork();
        parent.object(key("earth")).add(terra, 20);
        parent.object(key("out")).add(terra, 20);
        var late = parent.fork();
        assertEquals(8, early.object(key("earth")).amount(terra));
        assertEquals(6, early.object(key("out")).amount(terra));
        assertEquals(6, late.object(key("out")).amount(terra));
        late.object(key("out")).add(terra, 20);
        assertEquals(6, parent.object(key("out")).amount(terra));
    }

    @Test void inheritedSeedsRetainExactWildcardAndMetadataFallbackOrder() {
        var parent = engine(List.of(
                new Registration(new Key("test:variants", RecipeAspectData.WILDCARD), Map.of("aqua", 4)),
                new Registration(new Key("test:variants", 1), Map.of("terra", 8)),
                new Registration(new Key("test:metas", 5), Map.of("ignis", 12)),
                new Registration(new Key("test:metas", 2), Map.of("aqua", 4))));
        // Include a derived entry so nested forks also exercise merged snapshots.
        assertNull(parent.object(key("missing")));
        var fork = parent.fork().fork();
        assertEquals(8, fork.object(new Key("test:variants", 1)).amount(catalog.require("terra")));
        assertEquals(4, fork.object(new Key("test:variants", 0)).amount(catalog.require("aqua")));
        assertEquals(4, fork.object(new Key("test:metas", RecipeAspectData.WILDCARD)).amount(catalog.require("aqua")));
        assertTrue(fork.object(key("missing")).isEmpty());
    }

    @Test void returnedContainersAndSelfReturningToolsDoNotContributeAspects() {
        var properties = Map.of("test:milk", new RecipeAspectData.ItemProperties(false, true, "test:bucket"),
                "test:self", new RecipeAspectData.ItemProperties(false, true, "test:self"));
        var initial = List.of(new Registration(key("milk"),Map.of("metallum",8,"aqua",4)), source("bucket", "metallum", 8), source("self", "terra", 8));
        var data = new RecipeAspectData(properties, initial,
                List.of(recipe(Kind.CRAFTING, "out", 1, "milk", Map.of()), recipe(Kind.CRAFTING, "self_out", 1, "self", Map.of())));
        var engine = new RecipeAspectEngine(data, catalog,Map.of(),(k,v)->v,new InferenceLimits(64,6));
        assertEquals(0, engine.object(key("out")).amount(catalog.require("metallum")));
        assertEquals(3, engine.object(key("out")).amount(catalog.require("aqua")));
        assertEquals(8, engine.object(key("milk")).amount(catalog.require("metallum")));
        assertNull(engine.object(key("self_out")));
    }

    @Test void containerSubtractionIsPerInputAndExpandsScaledQuantitiesBeforeSubtracting() {
        for(var limits:List.of(new InferenceLimits(256,10),InferenceLimits.DEFAULT)) {
            var properties=Map.of("test:full",new RecipeAspectData.ItemProperties(false,true,"test:empty"));
            var initial=List.of(new Registration(key("full"),Map.of("metallum",2,"aqua",70)),
                    new Registration(key("empty"),Map.of("metallum",8,"aqua",40)),source("metal","metallum",8));
            var recipe=new Recipe(Kind.CRAFTING,key("out"),2,
                    List.of(Ingredient.direct(key("metal")),Ingredient.direct(key("full"))),Map.of());
            var engine=new RecipeAspectEngine(new RecipeAspectData(properties,initial,List.of(recipe)),catalog,Map.of(),(k,v)->v,limits);
            var result=engine.object(key("out"));
            assertEquals(3,result.amount(catalog.require("metallum")),"Remainder must not consume the separate metal input");
            int water=limits.scaling()?limits.compress((limits.expand(70)-limits.expand(40))*.75/2):(int)(30*.75/2);
            assertEquals(water,result.amount(catalog.require("aqua")));
            assertEquals(2,engine.fork().object(key("full")).amount(catalog.require("metallum")),"Shared seed was not mutated");
        }
    }

    @Test void zeroCountDynamicTemplateRetainsFloatCastAndSelectionThreshold() {
        var engine = engine(List.of(source("earth", "terra", 8)), recipe(Kind.CRAFTING, "out", 0, "earth", Map.of()));
        // float infinity casts to MAX_VALUE, which does not beat the old initial candidate threshold.
        assertNull(engine.object(key("out")));
    }

    @Test void crucibleCyclesAndFreshHistoryChainsCannotOverflowTheStack() {
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            var cycle = engine(List.of(), recipe(Kind.CRUCIBLE, "a", 1, "b", Map.of()), recipe(Kind.CRUCIBLE, "b", 1, "a", Map.of()));
            assertTrue(cycle.object(key("a")).isEmpty());
            var chain = new ArrayList<Recipe>();
            for (int index = 0; index < 200; index++) chain.add(recipe(Kind.CRUCIBLE, "part" + index, 1, "part" + (index + 1), Map.of()));
            var deep = engine(List.of(source("part200", "terra", 1)), chain.toArray(Recipe[]::new));
            assertTrue(deep.object(key("part0")).isEmpty());
        });
    }
}
