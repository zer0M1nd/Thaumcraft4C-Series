package automagy;
import automagy.logic.ExperienceLevels;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ExperienceLevelsTest {
 @Test void usesModernLevelBoundaries(){assertEquals(0,ExperienceLevels.base(0));assertEquals(352,ExperienceLevels.base(16));assertEquals(394,ExperienceLevels.base(17));assertEquals(1507,ExperienceLevels.base(31));assertEquals(1628,ExperienceLevels.base(32));}
 @Test void inverseUsesSameModernBoundaries(){for(int level:new int[]{0,1,16,17,30,31,32,50,100})assertEquals(level,ExperienceLevels.level(ExperienceLevels.base(level)),1e-9);assertEquals(16.5,ExperienceLevels.level(373),1e-9);assertTrue(Double.isFinite(ExperienceLevels.level(Integer.MAX_VALUE)));}
 @Test void fractionalProgressAndCorruptBoundsDoNotOverflow(){assertEquals(367,ExperienceLevels.total(16,.375f,42));assertEquals(Integer.MAX_VALUE,ExperienceLevels.base(Integer.MAX_VALUE));assertEquals(0,ExperienceLevels.total(-1,-1,10));}
}
