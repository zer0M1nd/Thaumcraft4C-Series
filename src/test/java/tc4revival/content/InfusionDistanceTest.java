package tc4revival.content;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InfusionDistanceTest {
    private boolean[][] match(String expected,String actual) {
        return InfusionDistance.matches(expected.chars().boxed().toList(),actual.chars().boxed().toList(),Objects::equals);
    }
    @Test void unorderedCountsSubstitutionOnceAndNeverReusesDuplicateItems() {
        assertEquals(1,InfusionDistance.unordered(match("ABC","ABD"),3));
        assertEquals(2,InfusionDistance.unordered(match("AAA","AB"),2));
        assertEquals(2,InfusionDistance.unordered(match("ABC","A"),1));
        assertEquals(3,InfusionDistance.unordered(match("","ABC"),3));
    }
    @Test void overlappingTagsUseMaximumMatchingInsteadOfGreedyFirstMatch() {
        boolean[][] tags={{true,true},{true,false},{false,false}};
        assertArrayEquals(new int[]{1,0,-1},InfusionDistance.assignment(tags,2));
        assertEquals(1,InfusionDistance.unordered(tags,2));
    }
    @Test void orderAllowsRotationButNotReflectionAndUsesUnitEdits() {
        assertEquals(0,InfusionDistance.ordered(match("ABCD","CDAB"),4,9));
        assertEquals(2,InfusionDistance.ordered(match("ABCD","DCBA"),4,9));
        assertEquals(1,InfusionDistance.ordered(match("ABCD","ABXD"),4,9));
        assertEquals(1,InfusionDistance.ordered(match("ABC","AC"),2,9));
        assertEquals(1,InfusionDistance.ordered(match("ABC","AXBC"),4,9));
    }
    @Test void prunedSearchAgreesWithFullSearchForEveryAffordableDistance() {
        var random=new Random(7);
        for(int trial=0;trial<800;trial++) {
            int n=random.nextInt(12),m=random.nextInt(12);var matrix=new boolean[n][m];
            for(var row:matrix)for(int j=0;j<m;j++)row[j]=random.nextBoolean();
            int full=InfusionDistance.ordered(matrix,m,99);
            for(int limit=0;limit<=12;limit++) {
                int bounded=InfusionDistance.ordered(matrix,m,limit);
                if(full<=limit)assertEquals(full,bounded);else assertTrue(bounded>limit);
            }
        }
    }
}
