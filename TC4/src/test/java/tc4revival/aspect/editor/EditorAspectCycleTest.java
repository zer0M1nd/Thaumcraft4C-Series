package tc4revival.aspect.editor;

import java.util.*;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.*;
import static org.junit.jupiter.api.Assertions.*;

class EditorAspectCycleTest {
    @Test void addonCyclesShareOneTierAndDependentAspectsRemainAboveThem(){
        var definitions=List.of(aspect("aer"),aspect("terra"),aspect("base","aer","terra"),
            aspect("first","second","base"),aspect("second","first","aer"),
            aspect("higher","first","base"),aspect("self","self","aer"));
        // Expedition intentionally permits cycles; this catalog models its public graph contract.
        AspectCatalog catalog=new AspectCatalog(List.of()){
            @Override public List<Aspect> all(){return definitions;}
            @Override public Aspect require(String tag){return definitions.stream().filter(a->a.tag().equals(tag)).findFirst().orElseThrow();}
        };
        assertEquals(0,EditorAspectLayout.tier(catalog.require("aer"),catalog));
        assertEquals(1,EditorAspectLayout.tier(catalog.require("base"),catalog));
        assertEquals(2,EditorAspectLayout.tier(catalog.require("first"),catalog));
        assertEquals(2,EditorAspectLayout.tier(catalog.require("second"),catalog));
        assertEquals(3,EditorAspectLayout.tier(catalog.require("higher"),catalog));
        assertEquals(1,EditorAspectLayout.tier(catalog.require("self"),catalog));
        for(int mode=0;mode<6;mode++){
            var rows=EditorAspectLayout.rows(catalog,mode,3);
            assertEquals(definitions.size(),rows.stream().mapToInt(List::size).sum());
            for(var row:rows){assertTrue(row.size()<=3);if(mode>=4)assertEquals(1,row.stream().map(a->EditorAspectLayout.tier(a,catalog)).distinct().count());}
        }
    }
    private static Aspect aspect(String tag,String... components){return new Aspect(tag,0,List.of(components),"f",1);}
}
