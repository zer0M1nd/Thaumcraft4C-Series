package tc4revival.research;

import java.util.*;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.*;
import static org.junit.jupiter.api.Assertions.*;

class ResearchBrowserLayoutTest {
    @Test void normalGeometryAndTabPositionsRemainExact(){
        var l=ResearchBrowserLayout.of(1280,720,false);
        assertEquals(256,l.width());assertEquals(230,l.height());assertEquals(224,l.viewportWidth());assertEquals(196,l.viewportHeight());
        assertEquals(18,l.tabsPerPage());assertEquals(-24,l.tabX(8));assertEquals(256,l.tabX(9));assertEquals(192,l.tabY(17));
    }
    @Test void maximumUsesScreenAreaAndAddsUnscaledTabRows(){
        var l=ResearchBrowserLayout.of(640,360,true);
        assertEquals(576,l.width());assertEquals(344,l.height());assertEquals(26,l.tabsPerPage());
        assertEquals(24,l.tabY(1));assertEquals(576,l.tabX(13));
        assertTrue(ResearchBrowserLayout.of(960,540,true).tabsPerPage()>l.tabsPerPage());
    }
    @Test void enlargedViewCanHitNodesOutsideOldViewport(){
        var aspects=new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
        var entry=new ResearchDefinition("R","BASICS",Map.of(),List.of(),List.of(),List.of(),Set.of(),0,0,
                new ResearchDefinition.Display(0,0,Optional.empty(),Optional.empty(),1),new ResearchDefinition.Triggers(Optional.empty(),Optional.empty(),Optional.empty()));
        var category=new ResearchCatalog.Category("BASICS","i","b",List.of(entry));
        var node=new ResearchBrowser.Node(entry,300,220,ResearchBrowser.State.AVAILABLE,true,ResearchBrowser.Frame.NORMAL);
        var bounds=new ResearchBrowser.Bounds(0,0,1000,1000);
        assertTrue(new ResearchBrowser.View(category,bounds,List.of(node),List.of()).hit(310,230,0,0).isEmpty());
        assertEquals(node,new ResearchBrowser.View(category,bounds,List.of(node),List.of(),544,310).hit(310,230,0,0).orElseThrow());
    }
}
