package wtmodern;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
class WarpRulesTest {
    @Test void originalEventTableAndCosts(){assertEquals(22,WarpRules.EVENTS.size());assertEquals(22,WarpRules.EVENTS.stream().map(WarpRules.Event::id).distinct().count());assertEquals(70,WarpRules.find("wither").severity());assertEquals(10,WarpRules.find("fakecreeper").severity());assertEquals(10,WarpRules.find("fakeexplosion").severity());assertEquals(3,WarpRules.find("blood").cost());}
    @Test void removalUsesTemporaryThenStickyThenOriginalIntegerPermanentCost(){assertArrayEquals(new int[]{10,5,0},WarpRules.remove(10,5,7,7,2,true));assertArrayEquals(new int[]{9,0,0},WarpRules.remove(10,5,7,15,2,true));assertArrayEquals(new int[]{10,0,0},WarpRules.remove(10,5,7,500,2,false));assertArrayEquals(new int[]{0,0,0},WarpRules.remove(10,5,7,Integer.MAX_VALUE,1,true));}
    @Test void purgeFinishesWithNoEnabledOrEligibleEvents(){assertTrue(WarpRules.purgeQueue(100,List.of(),n->0).isEmpty());assertTrue(WarpRules.purgeQueue(1,WarpRules.EVENTS,n->0).isEmpty());var queue=WarpRules.purgeQueue(100,WarpRules.EVENTS,n->n-1);assertEquals(91,queue.size());assertTrue(queue.stream().allMatch(x->x.equals("fakecreeper")));}
    @Test void selectionDoesNotInventUnavailableEffects(){for(int max=0;max<100;max++){int severity=max;var selected=WarpRules.choose(max,WarpRules.EVENTS,n->0);assertTrue(selected==null||selected.severity()<=severity);}}
    @Test void litmusThresholdsRemainInclusive(){assertEquals("minorwarp",WarpRules.severityKey(10));assertEquals("averagewarp",WarpRules.severityKey(25));assertEquals("majorwarp",WarpRules.severityKey(50));assertEquals("deadlywarp",WarpRules.severityKey(51));}
}
