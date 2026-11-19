package ttmodern;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class SoulCompositionTest {
    @Test void orderDoesNotChangeRecipe(){assertTrue(SoulComposition.matches(List.of("bestia","terra","bestia"),List.of("terra","bestia","bestia")));}
    @Test void duplicateCountsAreSignificant(){assertFalse(SoulComposition.matches(List.of("bestia","terra","bestia"),List.of("terra","bestia","terra")));}
    @Test void threeIndependentInputsAreRequired(){assertFalse(SoulComposition.matches(List.of("bestia","terra","bestia"),List.of("bestia","terra")));}
    @Test void matchingDoesNotMutateInputs(){var inputs=new ArrayList<>(List.of("terra","bestia","bestia"));SoulComposition.matches(List.of("bestia","terra","bestia"),inputs);assertEquals(List.of("terra","bestia","bestia"),inputs);}
}
