package tc4revival.content;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.*;
import static org.junit.jupiter.api.Assertions.*;

class SoulCrucibleTest {
    private final AspectCatalog catalog = AspectDefinitions.bundled();
    private CrucibleContents contents() { var value = new CrucibleContents(catalog); value.fill(1000,true); return value; }
    private void tick(CrucibleContents value, boolean soul, int count) {
        var random = RandomSource.create(17);
        for (int i=0;i<count;i++) value.tick(true,soul,0,random,()->{});
    }
    @Test void boilsTwiceAsFastIncludingBellows() {
        var normal=contents();var soul=contents();tick(normal,false,75);tick(soul,true,75);
        assertEquals(75,normal.heat());assertEquals(150,soul.heat());assertFalse(soul.boiling());
        tick(soul,true,1);assertTrue(soul.boiling());
        for(int bellows=0;bellows<=4;bellows++) {
            normal=contents();soul=contents();normal.tick(true,false,bellows,RandomSource.create(1),()->{});soul.tick(true,true,bellows,RandomSource.create(1),()->{});
            assertEquals(2*normal.heat(),soul.heat());
        }
    }
    @Test void firstDelayIsUnchangedThenFiveTimesAsManyDecayEvents() {
        var normal=contents();var soul=contents();var air=new AspectAmounts().add(catalog.require("aer"),100);
        normal.dissolve(air);soul.dissolve(air);tick(normal,false,250);tick(soul,true,250);
        assertEquals(100,normal.aspects().total());assertEquals(100,soul.aspects().total());
        tick(normal,false,1);tick(soul,true,1);assertEquals(99,normal.aspects().total());assertEquals(99,soul.aspects().total());
        tick(normal,false,505);tick(soul,true,505);
        assertEquals(94,normal.aspects().total());assertEquals(74,soul.aspects().total());
        assertEquals(988,normal.water());assertEquals(948,soul.water());
    }
    @Test void saveLoadPreservesGraceAndAddingIngredientsRestartsIt() {
        var soul=contents();soul.dissolve(new AspectAmounts().add(catalog.require("aer"),20));tick(soul,true,200);
        var tag=new CompoundTag();soul.save(tag);var loaded=contents();assertTrue(loaded.load(tag).isEmpty());
        tick(loaded,true,50);assertEquals(20,loaded.aspects().total());tick(loaded,true,1);assertEquals(19,loaded.aspects().total());
        tick(loaded,true,18);tag=new CompoundTag();loaded.save(tag);soul.load(tag);tick(soul,true,3);assertEquals(18,soul.aspects().total());
        soul.dissolve(new AspectAmounts().add(catalog.require("aqua"),1));tick(soul,true,250);assertEquals(19,soul.aspects().total());
    }
    @Test void compoundDecompositionAlsoRunsFiveTimesAfterGrace() {
        var soul=contents();soul.dissolve(new AspectAmounts().add(catalog.require("potentia"),80));
        tick(soul,true,251);assertEquals(79,soul.aspects().amount(catalog.require("potentia")));
        tick(soul,true,101);assertEquals(988,soul.water());
    }
}
