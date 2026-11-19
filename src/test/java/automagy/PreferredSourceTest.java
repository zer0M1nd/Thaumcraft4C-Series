package automagy;
import automagy.logic.PreferredSource;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PreferredSourceTest {
    @Test void depletedPreferredSourceFallsBackWithoutDoubleExtraction(){
        var selector=new PreferredSource<String,String>();var stock=new HashMap<>(Map.of("a",0,"b",1,"c",2));var visits=new ArrayList<String>();
        java.util.function.BiPredicate<String,String> take=(key,aspect)->{visits.add(key);int n=stock.get(key);if(n==0)return false;stock.put(key,n-1);return true;};
        assertEquals("b",selector.take("water",List.of("a","b","c"),take).orElseThrow());visits.clear();
        assertEquals("c",selector.take("water",List.of("a","b","c"),take).orElseThrow());assertEquals(List.of("b","a","c"),visits);assertEquals(1,stock.get("c"));
    }
    @Test void removedCachedSourceIsNeverQueried(){
        var selector=new PreferredSource<Integer,String>();selector.take("water",List.of(1,2),(k,a)->true);
        var visits=new ArrayList<Integer>();assertEquals(2,selector.take("water",List.of(2),(k,a)->{visits.add(k);return true;}).orElseThrow());assertEquals(List.of(2),visits);
    }
}
