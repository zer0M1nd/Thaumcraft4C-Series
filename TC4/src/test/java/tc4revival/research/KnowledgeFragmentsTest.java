package tc4revival.research;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.LegacyAspect;
import static org.junit.jupiter.api.Assertions.*;

class KnowledgeFragmentsTest {
    private final AspectCatalog aspects = new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());

    @Test void grantsOneOrTwoInLegacyDeclarationOrderWithoutDiscoveryBonus() {
        var knowledge = new PlayerKnowledge(aspects);
        var draws = new int[] {0};
        var rewards = KnowledgeFragments.consume(knowledge, aspects, bound -> {
            assertEquals(2, bound);
            return draws[0]++ % 2;
        });
        assertEquals(6, draws[0]);
        assertEquals(List.of("aer", "terra", "ignis", "aqua", "ordo", "perditio"),
                rewards.stream().map(reward -> reward.aspect().tag()).toList());
        for (int index = 0; index < rewards.size(); index++) {
            assertEquals(1 + index % 2, knowledge.pool(aspects.primals().get(index)));
        }
        assertTrue(knowledge.researchMarkers().isEmpty());
        assertEquals(6, knowledge.knownAspects().size());
    }

    @Test void grantsPastShortRangeWithFullIntegerStorageAndSaturatedCompatibility() {
        var knowledge = new PlayerKnowledge(aspects);
        aspects.primals().forEach(aspect -> knowledge.setPool(aspect, Short.MAX_VALUE));
        KnowledgeFragments.consume(knowledge, aspects, bound -> 1);
        aspects.primals().forEach(aspect -> {
            assertEquals(32769, knowledge.knownAspects().get(aspect));
            assertEquals(32769, knowledge.poolAmount(aspect));
            assertEquals(Short.MAX_VALUE, knowledge.pool(aspect));
        });
        KnowledgeFragments.consume(knowledge, aspects, bound -> 0);
        aspects.primals().forEach(aspect -> assertEquals(32770, knowledge.knownAspects().get(aspect)));
    }

    @Test void invalidLastDrawCannotPartiallyGrantPoints() {
        var knowledge = new PlayerKnowledge(aspects);
        for (int invalid : List.of(-1, 2)) {
            var draws = new int[] {0};
            assertThrows(IllegalArgumentException.class, () -> KnowledgeFragments.consume(knowledge, aspects,
                    bound -> ++draws[0] == 6 ? invalid : 1));
            aspects.primals().forEach(aspect -> assertEquals(0, knowledge.pool(aspect)));
        }
    }
}
