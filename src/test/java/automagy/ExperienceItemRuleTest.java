package automagy;
import automagy.logic.ExperienceItemRule;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ExperienceItemRuleTest {
 @Test void standardAndConfiguredPhialsRetainContainerSemantics(){var vanilla=ExperienceItemRule.parse("minecraft:experience_bottle=8,minecraft:glass_bottle",95).orElseThrow();assertEquals(8,vanilla.experience());assertEquals("minecraft:glass_bottle",vanilla.remainder());var own=ExperienceItemRule.parse("automagy:experience_phial=phial,*",120).orElseThrow();assertEquals(120,own.experience());assertTrue(own.craftingRemainder());assertEquals("",own.remainder());}
 @Test void malformedRulesCannotCauseNegativeOrTruncatedExperience(){for(String line:new String[]{"thing","a:b=-1","a:b=0","a:b=32768","a:b=2147483648","a:b=3,wrong","a:b=3,c:d,e:f","a:b=3=4","a:b,5=13"})assertTrue(ExperienceItemRule.parse(line,95).isEmpty(),line);assertTrue(ExperienceItemRule.parse("optional_mod:berries=13",95).isPresent());}
}
