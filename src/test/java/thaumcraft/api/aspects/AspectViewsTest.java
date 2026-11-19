package thaumcraft.api.aspects;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import tc4revival.aspect.*;
import tc4revival.aspect.Aspect;
import tc4revival.research.PlayerKnowledge;

class AspectViewsTest {
    @Test void twoReadersNeverChangeRawMetadataFingerprintOrStoredAmounts(){
        var hidden=new Aspect("api_test:hidden",0x123456,List.of(),"",771);
        var catalog=new AspectCatalog(java.util.stream.Stream.concat(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create),java.util.stream.Stream.of(hidden)).toList());
        var raw=new PlayerKnowledge(catalog);raw.discover(hidden);raw.setPool(hidden,19);
        String before=new com.google.gson.Gson().toJson(hidden);
        AspectViews.register("api_test:projection",0,(reader,tag)->!tag.equals(hidden.tag())||reader.research().contains("recognition"),
            new AspectViews.Mask("unknown","unknown","contents",net.minecraft.resources.ResourceLocation.parse("api_test:unknown"),net.minecraft.resources.ResourceLocation.parse("api_test:hidden_unknown"),0x888888));
        var first=AspectViews.knowledge(raw);assertFalse(first.knows(hidden));assertTrue(raw.knows(hidden));assertEquals(19,raw.poolAmount(hidden));
        var amounts=new AspectAmounts().add(hidden,27);assertTrue(AspectViews.project(AspectViews.Reader.of(raw),amounts,AspectViews.Purpose.NODE).isEmpty());
        var recognized=raw.copy();recognized.rememberResearch("recognition");
        assertEquals(27,AspectViews.project(AspectViews.Reader.of(recognized),amounts,AspectViews.Purpose.NODE).amount(hidden));
        assertEquals(19,AspectViews.knowledge(recognized).poolAmount(hidden));
        assertEquals(before,new com.google.gson.Gson().toJson(hidden));assertEquals(0x123456,hidden.color());assertEquals(27,amounts.amount(hidden));
        first.setPool(hidden,99);assertEquals(19,raw.poolAmount(hidden));
    }
}
