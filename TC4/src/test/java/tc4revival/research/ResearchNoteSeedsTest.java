package tc4revival.research;

import com.google.gson.JsonParser;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.LegacyAspect;
import tc4revival.client.ResearchTableView;
import static org.junit.jupiter.api.Assertions.*;

class ResearchNoteSeedsTest {
    @Test void randomizedNotesShuffleAllSeedTypesAndAngularOriginWithoutLosingConstraints() {
        var def=definition(",\"innerAspects\":[\"aer\",\"ignis\"],\"negativeAspects\":[\"aqua\",\"perditio\"]",3);
        var layouts=new HashSet<Map<ResearchHex,ResearchNote.Cell>>();
        var outerPositions=new HashSet<ResearchHex>();
        for(int seed=0;seed<100;seed++) {
            var note=notes.generate(def,new Random(seed)::nextInt,true).orElseThrow();
            assertEquals(note,notes.generate(def,new Random(seed)::nextInt,true).orElseThrow());
            layouts.add(note.grid());
            var inner=new ArrayList<ResearchHex>();
            var counts=new HashMap<String,Integer>();
            note.grid().forEach((hex,cell)->{
                if(!cell.fixed())return;
                counts.merge(cell.aspect(),1,Integer::sum);
                boolean outside=cell.aspect().equals("terra")||cell.aspect().equals("ordo");
                assertEquals(outside,hex.radius()==4);
                if(outside)outerPositions.add(hex);else inner.add(hex);
            });
            assertEquals(Map.of("terra",1,"ordo",1,"aer",1,"ignis",1,"aqua",1,"perditio",1),counts);
            for(var a:inner)for(var b:inner)if(!a.equals(b))assertTrue(a.distance(b)>1);
            assertEquals(2,note.grid().values().stream().filter(ResearchNote.Cell::negative).count());
        }
        assertTrue(layouts.size()>90);
        assertTrue(outerPositions.size()>12);
        ResearchNoteTestSeeds.set(3,4);
        var injected=notes.generate(def,new Random(12)::nextInt,true).orElseThrow();
        assertEquals(5,injected.grid().values().stream().filter(ResearchNote.Cell::negative).count());
        assertEquals(8,injected.grid().values().stream().filter(ResearchNote.Cell::anchor).count());
    }
    @Test void ordinaryNotesRotateAndShuffleWhileLegacyModeRetainsItsFixedOrigin() {
        var def=definition("",1);
        var positions=new HashSet<ResearchHex>();
        for(int seed=0;seed<80;seed++) {
            var note=notes.generate(def,new Random(seed)::nextInt,true).orElseThrow();
            note.grid().forEach((hex,cell)->{if("terra".equals(cell.aspect()))positions.add(hex);});
            assertEquals(2,note.grid().values().stream().filter(ResearchNote.Cell::anchor).count());
        }
        assertEquals(12,positions.size());
        assertEquals(notes.generate(def,new Random(42)::nextInt),notes.generate(def,new Random(42)::nextInt,false));
    }
    @org.junit.jupiter.api.AfterEach void resetTestOverride() { ResearchNoteTestSeeds.set(0,0); }
    @Test void testOverrideAddsRandomSeedsPreservesCatalogAndResetsWithoutChangingRandomSequence() {
        var def=definition(",\"innerAspects\":[\"aer\"],\"negativeAspects\":[\"aqua\"]",1);
        var original=notes.generate(def,new Random(42)::nextInt).orElseThrow();
        ResearchNoteTestSeeds.set(3,4);
        var note=notes.generate(def,new Random(42)::nextInt).orElseThrow();
        assertEquals(4,note.grid().values().stream().filter(ResearchNote.Cell::negative).count());
        assertEquals(7,note.grid().values().stream().filter(ResearchNote.Cell::anchor).count());
        assertEquals(1,def.noteSeeds().inner().size());
        assertEquals(1,def.noteSeeds().negative().size());
        assertNotEquals(note,notes.generate(def,new Random(43)::nextInt).orElseThrow());
        ResearchNoteTestSeeds.set(0,0);
        assertEquals(original,notes.generate(def,new Random(42)::nextInt).orElseThrow());
    }
    @Test void testOverrideExpandsSmallNotesAndKeepsEveryExtraSeedInside() {
        ResearchNoteTestSeeds.set(32,5);
        var note=notes.generate(definition("",0),new Random(9)::nextInt).orElseThrow();
        assertEquals(32,note.grid().values().stream().filter(ResearchNote.Cell::negative).count());
        assertEquals(7,note.grid().values().stream().filter(ResearchNote.Cell::anchor).count());
        int radius=note.grid().keySet().stream().mapToInt(ResearchHex::radius).max().orElseThrow();
        assertEquals(37,note.grid().entrySet().stream().filter(e->e.getValue().fixed()&&e.getKey().radius()<radius).count());
        var tag=new CompoundTag();ResearchNoteNbt.write(tag,note);
        assertEquals(note,ResearchNoteNbt.read(tag,aspects).note().orElseThrow());
        assertThrows(IllegalArgumentException.class,()->ResearchNoteTestSeeds.set(-1,0));
        assertThrows(IllegalArgumentException.class,()->ResearchNoteTestSeeds.set(0,33));
    }
    private final AspectCatalog aspects = new AspectCatalog(Arrays.stream(LegacyAspect.values()).map(LegacyAspect::create).toList());
    private final ResearchNotes notes = new ResearchNotes(aspects);
    private ResearchDefinition definition(String extra, int complexity) {
        return ResearchCatalog.decode(JsonParser.parseString("""
                [{"key":"TEST","icon":"test","background":"test","research":[{
                "key":"TEST","category":"TEST","aspects":{"terra":1,"ordo":1},
                "displayColumn":0,"displayRow":0,"pageCount":0,"complexity":%d%s}]}]
                """.formatted(complexity, extra)).getAsJsonArray(), aspects).require("TEST");
    }
    @Test void absentSeedsRetainOriginalDefaultsAndMalformedTagsAreRejected() {
        assertTrue(definition("", 1).noteSeeds().empty());
        assertThrows(IllegalArgumentException.class, () -> definition(",\"negativeAspects\":[\"missing\"]", 1));
    }
    @Test void allSeedsSurviveGenerationAndNbtWithoutOverlappingOrUsingOuterRing() {
        var def = definition(",\"innerAspects\":[\"aer\",\"ignis\"],\"negativeAspects\":[\"aqua\",\"perditio\"]", 3);
        for (int seed=0; seed<100; seed++) {
            var note = notes.generate(def,new Random(seed)::nextInt).orElseThrow();
            assertEquals(4,note.grid().values().stream().filter(ResearchNote.Cell::anchor).count());
            assertEquals(2,note.grid().values().stream().filter(ResearchNote.Cell::negative).count());
            var inner = note.grid().entrySet().stream().filter(e -> e.getValue().fixed() && e.getKey().radius()<4).toList();
            assertEquals(4,inner.size());
            for (int i=0;i<inner.size();i++) for (int j=i+1;j<inner.size();j++)
                assertTrue(inner.get(i).getKey().distance(inner.get(j).getKey())>1);
            var tag=new CompoundTag();ResearchNoteNbt.write(tag,note);
            assertEquals(note,ResearchNoteNbt.read(tag,aspects).note().orElseThrow());
        }
    }
    @Test void excessInnerSeedsFailExplicitlyRatherThanReplacingAnchors() {
        // The original minimum radius is two, with seven inner cells.
        String seeds=String.join(",",java.util.Collections.nCopies(8,"\"aer\""));
        var def=definition(",\"innerAspects\":["+seeds+"]",0);
        assertThrows(IllegalArgumentException.class,()->notes.generate(def,new Random(0)::nextInt));
    }
    @Test void denseSeedsKeepNonadjacentPlacementWhenHexagonCapacityAllowsIt() {
        for(int complexity=0;complexity<=3;complexity++) {
            int radius=Math.max(1,complexity)+1,maxIndependent=new int[]{3,3,7,13}[complexity];
            for(int count=1;count<=maxIndependent;count++) {
                String seeds=String.join(",",java.util.Collections.nCopies(count,"\"aer\""));
                var note=notes.generate(definition(",\"innerAspects\":["+seeds+"]",complexity),new Random(3)::nextInt).orElseThrow();
                var inner=note.grid().entrySet().stream().filter(e->e.getValue().fixed()&&e.getKey().radius()<radius).map(Map.Entry::getKey).toList();
                assertEquals(count,inner.size());
                for(var a:inner)for(var b:inner)assertNotEquals(1,a.distance(b),"Avoidable adjacency at complexity "+complexity+", seeds "+count);
            }
        }
    }
    @Test void crowdedAuthoredSeedsAreNotLostToHoleRemoval() {
        String seeds=String.join(",",java.util.Collections.nCopies(37,"\"aer\""));
        var note=notes.generate(definition(",\"innerAspects\":["+seeds+"]",3),new Random(9)::nextInt).orElseThrow();
        assertEquals(39,note.grid().values().stream().filter(ResearchNote.Cell::anchor).count());
    }
    @Test void negativeConnectionBlocksCompletionButUnconnectedNegativeSurvivesCompletion() {
        var grid=new LinkedHashMap<ResearchHex,ResearchNote.Cell>();
        grid.put(new ResearchHex(-1,0),new ResearchNote.Cell(1,"terra"));
        grid.put(new ResearchHex(0,0),new ResearchNote.Cell(2,"vitreus"));
        grid.put(new ResearchHex(1,0),new ResearchNote.Cell(1,"ordo"));
        var negative=new ResearchHex(0,1);
        grid.put(negative,new ResearchNote.Cell(3,"terra"));
        var note=new ResearchNote("TEST",0,false,0,grid);
        assertTrue(notes.complete(note,t->true).isEmpty());
        assertTrue(ResearchTableView.connections(note,aspects,t->true).edges().stream().anyMatch(e->e.to().equals(negative)));
        assertFalse(ResearchTableView.connections(note,aspects,t->true).edges().stream().anyMatch(e->e.from().equals(negative)));
        grid.put(negative,new ResearchNote.Cell(3,"ignis"));
        assertTrue(notes.complete(new ResearchNote("TEST",0,false,0,grid),t->true).orElseThrow().grid().get(negative).negative());
    }
}
