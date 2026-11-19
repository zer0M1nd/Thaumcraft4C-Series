package thmodern.node;

import java.util.*;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.*;
import static org.junit.jupiter.api.Assertions.*;

class RecombinationTest {
    private final Aspect a=new Aspect("aer",0,List.of(),"",1),b=new Aspect("ignis",0,List.of(),"",1),
            compound=new Aspect("lux",0,List.of("aer","ignis"),"",1),
            higher=new Aspect("higher",0,List.of("lux","aer"),"",1);
    private final AspectCatalog catalog=new AspectCatalog(List.of(a,b,compound,higher));
    private AspectAmounts amounts(Aspect... aspects){var result=new AspectAmounts();for(var aspect:aspects)result.add(aspect,2);return result;}
    @Test void favorsTwoPrimalsOverHigherPair(){
        var receiver=amounts(a,compound);var donor=amounts(a,b);
        assertEquals(List.of(new Recombination.Plan(a,b,compound)),Recombination.candidates(catalog,receiver,receiver,donor,donor));
    }
    @Test void preservesComponentOwnershipWhenCatalogOrderIsReversed(){
        var receiver=amounts(b);var donor=amounts(a);
        assertEquals(new Recombination.Plan(b,a,compound),Recombination.candidates(catalog,receiver,receiver,donor,donor).get(0));
    }
    @Test void depletedDonorCannotManufactureNewVis(){
        var receiver=amounts(a);var donor=amounts(b);
        assertTrue(Recombination.candidates(catalog,receiver,receiver,donor,new AspectAmounts().add(b,0)).isEmpty());
        assertEquals(2,receiver.amount(a));assertEquals(2,donor.amount(b));
    }
    @Test void exhaustedReceiverCannotManufactureCompoundCapacity(){
        assertTrue(Recombination.candidates(catalog,amounts(a),new AspectAmounts(),amounts(b),amounts(b)).isEmpty());
    }
    @Test void fallsBackToPrimalAndCompound(){
        var receiver=amounts(a);var donor=amounts(compound);
        assertEquals(List.of(new Recombination.Plan(a,compound,higher)),Recombination.candidates(catalog,receiver,receiver,donor,donor));
    }
    @Test void guardsIntegerOverflowWithoutChangingInputs(){
        var receiver=amounts(a).add(compound,Integer.MAX_VALUE);var donor=amounts(b);
        assertTrue(Recombination.candidates(catalog,receiver,receiver,donor,donor).isEmpty());
        assertEquals(Integer.MAX_VALUE,receiver.amount(compound));
    }
}
