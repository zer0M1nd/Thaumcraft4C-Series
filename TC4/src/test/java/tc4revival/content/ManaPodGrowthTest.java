package tc4revival.content;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.LegacyAspect;
import static org.junit.jupiter.api.Assertions.*;

class ManaPodGrowthTest {
    private final AspectCatalog catalog=new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    @Test void crossbreedingKeepsOrderedPairWeightsAndOnlyRunsAtAgeThree() {
        var air=catalog.require("aer"); var fire=catalog.require("ignis"); var light=catalog.require("lux");
        var expected=List.of(air,light,light,fire,light,light);
        for (int index=0;index<expected.size();index++) {
            final int selection=index;
            var result=ManaPodGrowth.advance(2,air,List.of(fire,fire),catalog,bound->{ assertEquals(6,bound); return selection; });
            assertEquals(3,result.age()); assertEquals(expected.get(index),result.aspect());
        }
        assertEquals(air,ManaPodGrowth.advance(3,air,List.of(fire),catalog,bound->fail("No further crossbreeding")).aspect());
    }
    @Test void earlyGrowthAndSingleParentDoNotConsumeRandomness() {
        var water=catalog.require("aqua");
        assertNull(ManaPodGrowth.advance(1,null,List.of(water),catalog,bound->fail("Not old enough")).aspect());
        assertEquals(water,ManaPodGrowth.advance(2,null,List.of(water,water),catalog,bound->fail("A single distinct parent is inherited")).aspect());
    }
    @Test void unassignedOlderPodsInitializeAndMatureHarvestUsesStrictThreshold() {
        assertEquals(catalog.require("herba"),ManaPodGrowth.advance(7,null,List.of(),catalog,bound->{ assertEquals(8,bound); return 0; }).aspect());
        assertEquals(0,ManaPodGrowth.harvestCount(1,1));
        assertEquals(1,ManaPodGrowth.harvestCount(6,1));
        assertEquals(1,ManaPodGrowth.harvestCount(7,.33f));
        assertEquals(2,ManaPodGrowth.harvestCount(7,Math.nextUp(.33f)));
    }
}
