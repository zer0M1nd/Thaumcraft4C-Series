package automagy;
import automagy.logic.LiquidSourceSearch;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LiquidSourceSearchTest {
 @Test void followsFlowToDistantSourceButDoesNotCrossUnloadedBarrier(){assertEquals(5,LiquidSourceSearch.find(0,p->new LiquidSourceSearch.Cell(p>=0&&p<6,p==0||p==5),p->List.of(p+1,p-1),p->p*p).orElseThrow());assertEquals(0,LiquidSourceSearch.find(0,p->new LiquidSourceSearch.Cell(p>=0&&p<3,p==0||p==5),p->List.of(p+1,p-1),p->p*p).orElseThrow());}
 @Test void boundsDepthAndCycles(){assertTrue(LiquidSourceSearch.find(0,p->new LiquidSourceSearch.Cell(true,p==33),p->List.of(p+1,p),p->p*p).isEmpty());assertEquals(32,LiquidSourceSearch.find(0,p->new LiquidSourceSearch.Cell(true,p==32),p->List.of(p+1,p),p->p*p).orElseThrow());}
}
