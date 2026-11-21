package thmodern.vat;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VatEssentiaTest {
    @Test void cloneRequiresEveryAspectNotOnlyLife(){
        var vat=new VatEssentia();var costs=new LinkedHashMap<String,Integer>();costs.put("victus",4);costs.put("cognitio",16);vat.demand(costs);
        assertEquals(4,vat.accept("victus",4));assertFalse(vat.consumeAll());assertEquals("cognitio",vat.wanted());
        assertEquals(16,vat.accept("cognitio",100));assertTrue(vat.consumeAll());assertTrue(vat.stored().isEmpty());assertFalse(vat.consumeAll());
    }
    @Test void changingJobCannotRecycleRemovedAspects(){
        var vat=new VatEssentia();vat.demand(Map.of("victus",4,"sano",2));vat.accept("victus",4);vat.accept("sano",2);
        vat.demand(Map.of("victus",1));assertEquals(1,vat.amount("victus"));assertEquals(0,vat.amount("sano"));
        vat.demand(Map.of("sano",2));assertEquals(0,vat.amount("sano"));assertFalse(vat.paid());
    }
    @Test void feedingConsumesOneMeasuredUnit(){
        var vat=new VatEssentia();vat.demand(Map.of("fames",1));vat.accept("fames",1);assertTrue(vat.consume("fames",1));assertFalse(vat.consume("fames",1));
        assertEquals("fames",vat.wanted());
    }
    @Test void rejectsInvalidInputWithoutDamagingPreviousReservation(){
        var vat=new VatEssentia();vat.demand(Map.of("victus",4));vat.accept("victus",2);
        assertThrows(IllegalArgumentException.class,()->vat.demand(Map.of("victus",-1)));
        assertEquals(2,vat.amount("victus"));assertEquals(0,vat.accept("victus",-1));assertEquals(2,vat.accept("victus",Integer.MAX_VALUE));assertEquals(0,vat.accept("sano",1));
    }
}
