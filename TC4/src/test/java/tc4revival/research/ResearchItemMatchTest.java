package tc4revival.research;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.recipe.RecipeAspectData.Key;
import static org.junit.jupiter.api.Assertions.*;

class ResearchItemMatchTest {
    private ResearchDefinition.Stack trigger(String id, int meta, boolean damageable) {
        return new ResearchDefinition.Stack(new Key(id, meta), 1, damageable, Optional.empty(), Optional.empty());
    }
    @Test void directIdentityRequiresNbtEqualityAndIgnoresDamageOnlyForDamageablePair() {
        var bow = trigger("minecraft:bow", 0, true);
        assertTrue(ResearchItemMatch.matches(bow, new Key("minecraft:bow", 50), true, true, false));
        assertFalse(ResearchItemMatch.matches(bow, new Key("minecraft:bow", 50), true, false, false));
        assertFalse(ResearchItemMatch.matches(bow, new Key("minecraft:bow", 50), false, true, false));
        assertFalse(ResearchItemMatch.matches(bow, new Key("minecraft:fishing_rod", 0), true, true, false));
    }
    @Test void wildcardWorksOnEitherSideButNeverBypassesNbt() {
        var wool = trigger("minecraft:wool", 32767, false);
        for (int color = 0; color < 16; color++) assertTrue(ResearchItemMatch.matches(wool, new Key("minecraft:wool", color), false, true, false));
        assertFalse(ResearchItemMatch.matches(wool, new Key("minecraft:wool", 1), false, false, false));
        assertTrue(ResearchItemMatch.matches(trigger("minecraft:wool", 2, false), new Key("minecraft:wool", 32767), false, true, false));
    }
    @Test void materialTagMatchesStillRequireNbtEquality() {
        var mercury = trigger("Thaumcraft:ItemResource", 3, false);
        assertFalse(ResearchItemMatch.matches(mercury, new Key("minecraft:iron_nugget", 0), false, false, true));
        assertTrue(ResearchItemMatch.matches(mercury, new Key("minecraft:iron_nugget", 0), false, true, true));
        assertFalse(ResearchItemMatch.matches(mercury, new Key("minecraft:iron_nugget", 0), false, true, false));
        assertFalse(ResearchItemMatch.matches(mercury, null, false, false, true));
        assertTrue(ResearchItemMatch.matches(mercury, null, false, true, true));
    }
    @Test void absentIdentityCannotDirectMatchAndSubtypeDifferenceIsMeaningful() {
        var exact = trigger("Thaumcraft:ItemResource", 3, false);
        assertFalse(ResearchItemMatch.matches(exact, null, false, true, false));
        assertFalse(ResearchItemMatch.matches(exact, new Key("Thaumcraft:ItemResource", 5), false, true, false));
        assertTrue(ResearchItemMatch.matches(exact, new Key("Thaumcraft:ItemResource", 3), false, true, false));
    }
}
