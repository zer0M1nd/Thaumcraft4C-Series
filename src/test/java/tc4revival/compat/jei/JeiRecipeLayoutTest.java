package tc4revival.compat.jei;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JeiRecipeLayoutTest {
    @Test void compactRecipesKeepOutputAndBookAwayFromEveryInput() {
        for(String kind:new String[]{"arcane","infusion","infusion_enchantment","crucible"}) {
            assertTrue(JeiRecipeLayout.height(kind)<=150);
            int size=kind.equals("arcane")?9:JeiRecipeLayout.infusion(kind)?12:1;
            for(int i=0;i<size;i++) {
                var p=JeiRecipeLayout.item(kind,i,size);
                for(var other:new JeiRecipeLayout.Position[]{JeiRecipeLayout.output(kind),JeiRecipeLayout.book(kind)})
                    assertTrue(Math.abs(p.x()-other.x())>=18||Math.abs(p.y()-other.y())>=18);
            }
            var p=JeiRecipeLayout.book(kind);assertTrue(JeiRecipeLayout.overBook(kind,p.x()+8,p.y()+8));
            assertFalse(JeiRecipeLayout.overBook(kind,p.x()-1,p.y()+8));
        }
    }
    @Test void arcaneSlotsKeepRowMajorOrderInsideBookGrid() {
        for(int i=0;i<9;i++) {
            var p=JeiRecipeLayout.item("arcane",i,9);
            assertEquals(16+i%3*32,p.x());
            assertEquals(16+i/3*32,p.y());
            assertTrue(p.x()>=4 && p.x()+16<=108 && p.y()>=4 && p.y()+16<=108);
        }
    }
    @Test void compactInfusionRingRetainsPageBoundsAndOnlyScrollsAfterSixteen() {
        assertEquals(174,JeiRecipeLayout.width("infusion"));
        assertEquals(150,JeiRecipeLayout.height("infusion"));
        for(int size=2;size<=16;size++) {
            assertFalse(JeiRecipeLayout.scrollComponents(size));
            for(int i=0;i<size;i++) {
                var p=JeiRecipeLayout.item("infusion",i,size);
                assertTrue(p.x()>=0 && p.x()+16<=128 && p.y()>=0 && p.y()+16<=106);
            }
            // The requested legacy radius allows corner overlap of rectangular slot hitboxes
            // at high counts; do not enlarge the artwork merely to separate those rectangles.
            for(int i=1;i<size;i++) {
                var a=JeiRecipeLayout.item("infusion",i,size);
                var b=JeiRecipeLayout.item("infusion",0,size);
                assertTrue(Math.abs(a.x()-b.x())>=16 || Math.abs(a.y()-b.y())>=16);
            }
        }
        assertTrue(JeiRecipeLayout.scrollComponents(17));
        assertEquals(new JeiRecipeLayout.Position(48,44),JeiRecipeLayout.item("infusion",0,50));
    }
    @Test void aspectRowsFitBeforeFooterAndItemAspectsStayUnchanged() {
        for(var kind:new String[]{"arcane","crucible","infusion","infusion_enchantment","aspect_combine"}) {
            int count=JeiRecipeLayout.costCapacity(kind);
            for(int i=0;i<count;i++) {
                var p=JeiRecipeLayout.cost(kind,i,count);
                assertTrue(p.x()>=0 && p.x()+18<=JeiRecipeLayout.width(kind));
                assertTrue(p.y()>=0 && p.y()+18<=Math.min(216,JeiRecipeLayout.height(kind)));
            }
        }
        assertEquals(158,JeiRecipeLayout.height("item_aspects"));
        assertEquals(new JeiRecipeLayout.Position(12,24),JeiRecipeLayout.item("item_aspects",0,1));
    }
}
