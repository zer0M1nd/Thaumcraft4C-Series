package tc4revival.research;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.*;
import static org.junit.jupiter.api.Assertions.*;

class ResearchAspectSearchTest {
    private final AspectCatalog catalog = new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    private List<String> search(String text, int mode, Function<Aspect,String> translated) {
        return ResearchAspectSearch.filter(catalog.all(),catalog,text,mode,translated,
                (name,query)->name.toLowerCase(Locale.ROOT).contains(query)).stream().map(Aspect::tag).toList();
    }
    @Test void originalLatinNamesAreCaseInsensitivePrefixes() {
        var result=search("E",0,a->"Earth");
        assertTrue(result.contains("exanimis"));assertFalse(result.contains("aer"));assertFalse(result.contains("terra"));
    }
    @Test void translatedAndCombinedModesHaveIndependentMatching() {
        Function<Aspect,String> names=a->a.tag().equals("terra")?"Earth":"???";
        assertEquals(List.of("terra"),search("e",1,names));
        var both=search("e",2,names);assertTrue(both.contains("terra"));assertTrue(both.contains("exanimis"));
        assertEquals(List.of("terra"),search("地",1,a->a.tag().equals("terra")?"大地":"???"));
    }
    @Test void relatedSearchUnionsDirectNeighborsWithoutLeakingUndiscoveredResults() {
        var seeds=List.of(catalog.require("cognitio"),catalog.require("corpus"));
        var expected=catalog.all().stream().filter(a->seeds.stream().anyMatch(b->a.equals(b)||catalog.areRelated(a,b))).map(Aspect::tag).toList();
        assertEquals(expected,search("~co",0,Aspect::displayName));
        var visible=List.of(catalog.require("humanus"));
        assertEquals(visible,ResearchAspectSearch.filter(visible,catalog,"~co",0,Aspect::displayName,String::contains));
    }
    @Test void optionalMatcherIsUsedForBothTranslatedAndRelatedSearch() {
        var names=(Function<Aspect,String>)(a->a.tag().equals("terra")?"地":"???");
        java.util.function.BiPredicate<String,String> pinyin=(name,q)->name.equals("地")&&q.equals("di");
        assertEquals(List.of(catalog.require("terra")),ResearchAspectSearch.filter(catalog.all(),catalog,"DI",1,names,pinyin));
        var related=ResearchAspectSearch.filter(catalog.all(),catalog,"~di",1,names,pinyin);
        assertTrue(related.contains(catalog.require("terra")));
        assertTrue(related.stream().allMatch(a->a.tag().equals("terra")||catalog.areRelated(a,catalog.require("terra"))));
    }
    @Test void exactNamesTakePrecedenceOverAnotherAspectsPrefix() {
        var all=new java.util.ArrayList<>(catalog.all());
        var luxuria=new Aspect("forbidden_magic:luxuria",0xff00ff,List.of("corpus","fames"),"",1);all.add(luxuria);
        var expanded=new AspectCatalog(all);var lux=expanded.require("lux");
        var exact=ResearchAspectSearch.filter(expanded.all(),expanded,"~lux",0,Aspect::displayName,String::contains);
        assertEquals(expanded.all().stream().filter(a->a.equals(lux)||expanded.areRelated(a,lux)).toList(),exact);
        assertFalse(exact.contains(luxuria));
        var direct=ResearchAspectSearch.filter(expanded.all(),expanded,"lux",0,Aspect::displayName,String::contains);
        assertEquals(List.of(lux),direct);
        var prefix=ResearchAspectSearch.filter(expanded.all(),expanded,"lu",0,Aspect::displayName,String::contains);
        assertTrue(prefix.containsAll(List.of(lux,luxuria)));
    }
    @Test void emptySearchAndBareRelatedPrefixKeepVisibleOrder() {
        var visible=List.of(catalog.require("terra"),catalog.require("aer"));
        for(String input:List.of(""," ","~"))assertEquals(visible,ResearchAspectSearch.filter(visible,catalog,input,0,Aspect::displayName,String::contains));
        assertTrue(search("nothingmatches",2,Aspect::displayName).isEmpty());
    }
}
