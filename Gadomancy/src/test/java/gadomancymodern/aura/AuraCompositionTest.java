package gadomancymodern.aura;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class AuraCompositionTest {
    @Test public void collectionReachesAllPositionsWithoutOverrun(){
        assertEquals(0,AuraComposition.consumedAt(50));assertEquals(1,AuraComposition.consumedAt(51));assertEquals(729,AuraComposition.consumedAt(500));assertEquals(729,AuraComposition.consumedAt(Integer.MAX_VALUE));
        int previous=0;for(int tick=0;tick<502;tick++){int current=AuraComposition.consumedAt(tick);assertTrue(current>=previous&&current<=729);previous=current;}
    }
    @Test public void candidatesBelowMeanCannotWinAndCountThresholdIsStrict(){
        var aura=new AuraComposition();aura.add("aer",60);aura.add("ignis",30);aura.add("terra",10);
        assertEquals("blank",aura.choose(108,max->0));assertEquals("aer",aura.choose(109,max->max-1));assertEquals(6,aura.orbitals().size());
        aura.add("ignis",30);assertEquals("aer",aura.choose(109,max->0));assertEquals("ignis",aura.choose(109,max->max-1));
    }
    @Test public void halfCapKeepsOriginalExclusiveUpperBoundsAndOverflowIsSafe(){
        int[] bounds={9,19,17,34},minimum={4,9,8,17},maximum={7,17,15,33};
        for(int i=0;i<bounds.length;i++){assertEquals(minimum[i],AuraComposition.randomHalfCost(bounds[i],max->0));assertEquals(maximum[i],AuraComposition.randomHalfCost(bounds[i],max->max-1));}
        var aura=new AuraComposition();for(String aspect:AuraComposition.PRIMALS)aura.add(aspect,Integer.MAX_VALUE);assertDoesNotThrow(()->aura.choose(729,max->max-1));
    }
}
