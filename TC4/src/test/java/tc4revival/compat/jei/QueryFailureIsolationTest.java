package tc4revival.compat.jei;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QueryFailureIsolationTest {
    @Test void brokenSubtypeDoesNotEnterCacheOrBlockValidPotionVariants(){
        var guard=new QueryFailureIsolation<String>();
        var cache=new ClientQueryCache<String,String,String>();
        var calls=new AtomicInteger();var warnings=new AtomicInteger();
        for(int i=0;i<3;i++)assertFalse(guard.run("powder",()->{
            calls.incrementAndGet();throw new NullPointerException("missing effect");
        },(k,e)->warnings.incrementAndGet()));
        for(var potion:List.of("healing","strength"))assertTrue(guard.run(potion,
                ()->cache.offer(potion,potion,false),(k,e)->fail(e)));
        var batch=cache.begin(32);assertEquals(List.of("healing","strength"),batch.values());
        cache.complete(batch.id(),batch.values(),s->s);
        assertTrue(cache.result("powder").isEmpty());
        assertEquals(1,calls.get());assertEquals(1,warnings.get());
        guard.clear();assertTrue(guard.run("powder",()->cache.offer("powder","powder",false),(k,e)->fail(e)));
    }
    @Test void failedCalculationIsDiscardedNotCachedAsEmptyAndQueueContinues(){
        var cache=new ClientQueryCache<String,String,String>();
        cache.offer("bad","bad",false);cache.offer("good","good",false);
        var bad=cache.begin(1);assertTrue(cache.discard(bad.id()));
        assertTrue(cache.result("bad").isEmpty());assertFalse(cache.waiting());
        var good=cache.begin(1);assertFalse(cache.discard(bad.id()));
        assertTrue(cache.complete(good.id(),good.values(),s->s));
        assertEquals(List.of("good"),cache.result("good").orElseThrow());
    }
    @Test void addonLinkageErrorsAreIsolatedButVmFailuresAreNotSwallowed(){
        var guard=new QueryFailureIsolation<String>();
        assertFalse(guard.run("incompatible",()->{throw new NoSuchMethodError();},(k,e)->{}));
        assertThrows(OutOfMemoryError.class,()->guard.run("vm",()->{throw new OutOfMemoryError();},(k,e)->{}));
    }
}
