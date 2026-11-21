package automagy.logic;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class EssentiaCostSettingTest {
 @Test void duplicateOverrideAndZeroRemoval(){assertEquals(Map.of("aer",2),EssentiaCostSetting.parse(List.of("ignis=4","aer=1","ignis=0"," aer = 2 ")));}
 @Test void malformedAndOverflowRejected(){for(String value:List.of("ignis=-1","ignis=65536","Ignis=1","ignis=1=2","ignis=99999999999","=3"))assertTrue(EssentiaCostSetting.entry(value).isEmpty(),value);}
 @Test void emptyMeansFreeAndResultsAreImmutable(){var result=EssentiaCostSetting.parse(List.of());assertTrue(result.isEmpty());assertThrows(UnsupportedOperationException.class,()->result.put("ignis",1));}
}
