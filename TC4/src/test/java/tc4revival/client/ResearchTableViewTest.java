package tc4revival.client;

import java.util.Arrays;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.LegacyAspect;
import tc4revival.research.ResearchHex;
import tc4revival.research.ResearchNote;
import static org.junit.jupiter.api.Assertions.*;

class ResearchTableViewTest {
    @Test void everyBoardCellAndNearCenterPointMapsBackToTheSameHex() {
        for (int q = -4; q <= 4; q++) for (int r = -4; r <= 4; r++) {
            var hex = new ResearchHex(q, r); var pixel = ResearchTableView.pixel(hex);
            for (double dx : new double[] {-3, 0, 3}) for (double dy : new double[] {-3, 0, 3})
                assertEquals(hex, ResearchTableView.hex(pixel.x() + dx, pixel.y() + dy));
        }
    }
    @Test void pagesMoveOneColumnWithFiveRowsPerColumn() {
        assertEquals(0, ResearchTableView.aspectIndex(0, 0, 0));
        assertEquals(4, ResearchTableView.aspectIndex(0, 15, 79));
        assertEquals(5, ResearchTableView.aspectIndex(0, 16, 0));
        assertEquals(10, ResearchTableView.aspectIndex(2, 0, 0));
        assertEquals(-1, ResearchTableView.aspectIndex(0, 80, 0));
        assertEquals(-1, ResearchTableView.aspectIndex(0, 0, -1));
        assertEquals(1, ResearchTableView.lastPage(25)); // TC4 permits a partly filled next page here.
        assertEquals(5, ResearchTableView.lastPage(48));
    }
    @Test void highlightsFollowOnlyKnownDirectConnectionsFromAnchors() {
        var aspects = new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
        var cells = new LinkedHashMap<ResearchHex, ResearchNote.Cell>();
        cells.put(new ResearchHex(0, 0), new ResearchNote.Cell(1, "terra"));
        cells.put(new ResearchHex(1, 0), new ResearchNote.Cell(2, "vitreus"));
        cells.put(new ResearchHex(2, 0), new ResearchNote.Cell(2, "ordo"));
        cells.put(new ResearchHex(3, 0), new ResearchNote.Cell(2, "aer"));
        var note = new ResearchNote("THAUMIUM", 0, false, 0, cells);
        assertEquals(2, ResearchTableView.connections(note, aspects, key -> true).edges().size());
        assertTrue(ResearchTableView.connections(note, aspects, key -> !key.equals("vitreus")).edges().isEmpty());
        assertFalse(ResearchTableView.connections(note, aspects, key -> true).lit().contains(new ResearchHex(3, 0)));
    }
}
