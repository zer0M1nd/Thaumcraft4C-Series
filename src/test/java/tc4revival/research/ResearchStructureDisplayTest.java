package tc4revival.research;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResearchStructureDisplayTest {
    private ResearchDefinition.Stack stack(String item,int meta,Optional<String> tag) {
        return new ResearchDefinition.Stack(new tc4revival.aspect.recipe.RecipeAspectData.Key(item,meta),1,false,tag,Optional.empty());
    }
    @Test void specialPreviewsAreRestrictedToReviewedLegacyIdentities() {
        assertEquals(ResearchStructureDisplay.EMPTY_SPACE,ResearchStructureDisplay.of(stack("Thaumcraft:blockHole",15,Optional.empty())).orElseThrow());
        assertEquals(ResearchStructureDisplay.LAVA,ResearchStructureDisplay.of(stack("minecraft:lava",0,Optional.empty())).orElseThrow());
        assertEquals(ResearchStructureDisplay.NODE,ResearchStructureDisplay.of(stack("Thaumcraft:blockAiry",5,Optional.empty())).orElseThrow());
        for (var original:java.util.List.of(stack("Thaumcraft:blockHole",0,Optional.empty()),stack("Thaumcraft:blockAiry",0,Optional.empty()),
                stack("minecraft:lava_bucket",0,Optional.empty()),stack("minecraft:portal",0,Optional.empty()),stack("minecraft:end_portal",0,Optional.empty()),
                stack("Thaumcraft:blockAiry",5,Optional.of("{nodeid:test}"))))
            assertTrue(ResearchStructureDisplay.of(original).isEmpty(),original.toString());
    }
}
