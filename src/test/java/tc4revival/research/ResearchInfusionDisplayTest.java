package tc4revival.research;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.recipe.RecipeAspectData.Key;
import tc4revival.research.ResearchPage.*;
import static org.junit.jupiter.api.Assertions.*;

class ResearchInfusionDisplayTest {
    private final ResearchDefinition.Stack input = new ResearchDefinition.Stack(new Key("Thaumcraft:ItemHelmetFortress", 32767),
            1, true, Optional.of("{mask:0,display:{Name:\"Original\"}}"), Optional.empty());

    private ResearchPageRecipes.Infusion infusion(Recipe output) {
        return new ResearchPageRecipes.Infusion("TEST", new Item(input), output, new Sequence(List.of()), Map.of(), 3);
    }

    @Test void propertyOutputPreservesInputAndOtherTagsWithOriginalNumericTypes() {
        for (var value : List.of(new Nbt("net.minecraft.nbt.NBTTagByte", "1b"), new Nbt("net.minecraft.nbt.NBTTagInt", "2"))) {
            var recipe = infusion(new Sequence(List.of(new Literal("mask"), value)));
            var output = ResearchInfusionDisplay.output(recipe).stack();
            var tag = output.tag().orElseThrow();
            assertTrue(tag.contains("mask", value.value().equals("1b") ? Tag.TAG_BYTE : Tag.TAG_INT));
            assertEquals("Original", tag.getCompound("display").getString("Name"));
            assertEquals(input.key(), output.key()); assertEquals(input.count(), output.count());
            assertEquals(0, input.tag().orElseThrow().getInt("mask"));
            tag.putInt("mask", 99);
            assertNotEquals(99, ResearchInfusionDisplay.output(recipe).stack().tag().orElseThrow().getInt("mask"));
        }
    }

    @Test void itemOutputAndRunicExampleKeepExplicitOutput() {
        var output = new Item(input);
        assertSame(output, ResearchInfusionDisplay.output(infusion(output)));
        var emptyRecipes = ResearchPageRecipes.decode(new com.google.gson.JsonArray());
        var example = new RunicExample("RUNIC", input, input, new Sequence(List.of()), Map.of("aer", 8), 6);
        var definition = ResearchInfusionDisplay.definition(example, emptyRecipes);
        assertEquals(output, ResearchInfusionDisplay.output(definition));
        assertEquals(6, definition.instability()); assertEquals(Map.of("aer", 8), definition.aspects());
    }

    @Test void malformedPropertyDescriptorIsRejected() {
        for (var recipe : List.<Recipe>of(new Empty(), new Sequence(List.of()),
                new Sequence(List.of(new Literal(3), new Nbt("net.minecraft.nbt.NBTTagByte", "1b")))))
            assertThrows(IllegalArgumentException.class, () -> ResearchInfusionDisplay.output(infusion(recipe)));
    }

    @Test void everyObservedInfusionProducesAnIndependentDisplayDescription() throws Exception {
        try (var reader = new java.io.InputStreamReader(java.util.Objects.requireNonNull(getClass()
                .getResourceAsStream("/research-page-recipes.json")), java.nio.charset.StandardCharsets.UTF_8)) {
            var recipes = ResearchPageRecipes.decode(com.google.gson.JsonParser.parseReader(reader).getAsJsonArray());
            int properties = 0;
            for (var entry : recipes.entries().values()) {
                if (!(entry.definition() instanceof ResearchPageRecipes.Infusion infusion)) continue;
                var output = ResearchInfusionDisplay.output(infusion);
                assertTrue(output.stack().count() > 0, entry.key());
                if (infusion.output() instanceof Sequence) {
                    properties++;
                    assertTrue(output.stack().nbt().isPresent(), entry.key());
                    assertEquals(((Item) infusion.input()).stack().key(), output.stack().key());
                }
            }
            assertTrue(properties > 0, "Fixture must cover property-changing infusion recipes");
        }
    }
}
