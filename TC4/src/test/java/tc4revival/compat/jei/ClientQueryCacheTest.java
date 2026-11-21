package tc4revival.compat.jei;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClientQueryCacheTest {
    @Test void warmHitsAndPriorityPromotionsNeverCopyTheStackAgain(){
        var cache=new ClientQueryCache<String,String,String>();int[] copies={0};
        java.util.function.Supplier<String> copy=()->{copies[0]++;return "potion";};
        cache.offerLazy("potion",copy,false);
        for(int i=0;i<10000;i++)cache.offerLazy("potion",copy,false);
        cache.offerLazy("potion",copy,true); // Promotion reuses queued snapshot.
        for(int i=0;i<10000;i++)cache.offerLazy("potion",copy,true);
        var batch=cache.begin(1);cache.offerLazy("potion",copy,true);
        cache.complete(batch.id(),batch.values(),s->s);
        for(int i=0;i<10000;i++)cache.offerLazy("potion",copy,true);
        assertEquals(1,copies[0]);assertTrue(cache.begin(1).values().isEmpty());
        cache.clear();cache.offerLazy("potion",copy,false);assertEquals(2,copies[0]);
    }
    @Test void repeatedInventoryVisitsDoNotResendPendingActiveOrCompletedStacks(){
        var cache=new ClientQueryCache<String,String,String>();
        for(int i=0;i<100;i++)cache.offer("potion:healing","potion:healing",false);
        var batch=cache.begin(32);assertEquals(1,batch.values().size());
        cache.offer("potion:healing","potion:healing",true);
        assertTrue(cache.complete(batch.id(),List.of("potion:healing"),s->s));
        cache.offer("potion:healing","potion:healing",true);
        assertEquals(List.of("potion:healing"),cache.result("potion:healing").orElseThrow());
        assertTrue(cache.begin(32).values().isEmpty());
    }
    @Test void negativeResultsAreCachedUntilInvalidation(){
        var cache=new ClientQueryCache<String,String,String>();cache.offer("empty","empty",false);
        var first=cache.begin(32);cache.complete(first.id(),List.of(),s->s);
        assertEquals(List.of(),cache.result("empty").orElseThrow());
        cache.offer("empty","empty",true);assertTrue(cache.begin(32).values().isEmpty());
        cache.clear();assertTrue(cache.result("empty").isEmpty());
        cache.offer("empty","empty",false);assertEquals(List.of("empty"),cache.begin(32).values());
    }
    @Test void newInventoryStateHasPriorityWithoutRestartingWarmup(){
        var cache=new ClientQueryCache<String,String,String>();
        for(int i=0;i<1000;i++)cache.offer("item"+i,"item"+i,false);
        var first=cache.begin(32);assertEquals(32,first.values().size());
        cache.offer("enchanted","enchanted",true);cache.offer("item500","item500",true);
        cache.complete(first.id(),first.values(),s->s);
        var next=cache.begin(32);assertEquals(List.of("enchanted","item500","item32"),next.values().subList(0,3));
        assertTrue(cache.result("item0").isPresent());
    }
    @Test void retriesAndStaleRepliesCannotCompleteAnotherBatchOrGeneration(){
        var cache=new ClientQueryCache<String,String,String>();cache.offer("a","a",false);
        var first=cache.begin(32);assertEquals(first,cache.current());
        cache.clear();cache.offer("b","b",false);var second=cache.begin(32);
        assertNotEquals(first.id(),second.id());
        assertFalse(cache.complete(first.id(),List.of("a"),s->s));assertTrue(cache.waiting());
        assertTrue(cache.complete(second.id(),List.of("b"),s->s));
        cache.offer("c","c",false);var third=cache.begin(32);
        assertFalse(cache.complete(second.id(),List.of("b"),s->s));assertEquals(third,cache.current());
        assertTrue(cache.result("a").isEmpty());
    }
    @Test void onlyRequestedKeysAreCachedAndIncompleteRepliesDoNotCountAsSuccess(){
        var cache=new ClientQueryCache<String,String,String>();cache.offer("a","a",false);
        var batch=cache.begin(32);assertTrue(cache.result("a").isEmpty());
        assertThrows(IllegalStateException.class,()->cache.begin(32));
        cache.complete(batch.id(),List.of("other"),s->s);
        assertTrue(cache.result("other").isEmpty());assertEquals(List.of(),cache.result("a").orElseThrow());
    }
}
