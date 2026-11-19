package com.ilya3point999k.thaumicconcilium.api;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AspectCostParserTest {
    private static final Set<String> KNOWN=Set.of("aer","terra","forbidden_magic:gula");
    @Test void legacyWhitespaceAndModernNamespacedAspectsHaveOneImmutableCost(){
        var costs=AspectCostParser.parse(" aer 10 , terra\t20, forbidden_magic:gula 3, aer 7 ",KNOWN::contains);
        assertEquals(Map.of("aer",17,"terra",20,"forbidden_magic:gula",3),costs);
        assertThrows(UnsupportedOperationException.class,()->costs.put("aer",0));
    }
    @Test void invalidCostsFailAsAWholeInsteadOfSilentlyDroppingClauses(){
        for(String invalid:List.of("","aer","aer 1,","aer 1,,terra 2","aer 0","aer -1","aer 1.5","aer 2147483648","aer 1 2","unknown 1","Aer 1"))
            assertThrows(IllegalArgumentException.class,()->AspectCostParser.parse(invalid,KNOWN::contains),invalid);
    }
    @Test void duplicateAccumulationCannotWrapIntoAFreeRecipe(){
        assertEquals(Integer.MAX_VALUE,AspectCostParser.parse("aer 2147483646, aer 1",KNOWN::contains).get("aer"));
        assertThrows(IllegalArgumentException.class,()->AspectCostParser.parse("aer 2147483647, aer 1",KNOWN::contains));
    }
}
