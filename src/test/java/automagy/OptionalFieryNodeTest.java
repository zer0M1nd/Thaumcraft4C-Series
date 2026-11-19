package automagy;
import automagy.logic.FieryNodeRoll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class OptionalFieryNodeTest {
 @Test void legacyDefaultFindsModernForbiddenMagicButExplicitConfigIsRespected(){
  var modern=new tc4revival.aspect.Aspect("forbidden_magic:infernus",0xffffff,java.util.List.of(),"f",771);
  var catalog=new tc4revival.aspect.AspectCatalog(java.util.List.of(modern));
  assertEquals(modern,OptionalAspects.resolve(catalog,"infernus").orElseThrow());
  assertTrue(OptionalAspects.resolve(catalog,"").isEmpty());assertTrue(OptionalAspects.resolve(catalog,"other:nether").isEmpty());
  var legacy=new tc4revival.aspect.Aspect("infernus",0xffffff,java.util.List.of(),"f",771);
  assertEquals(legacy,OptionalAspects.resolve(new tc4revival.aspect.AspectCatalog(java.util.List.of(legacy,modern)),"infernus").orElseThrow());
 }
 @Test void absentOptionalAspectKeepsOriginalRoll(){assertEquals(FieryNodeRoll.roll(100,n->0),FieryNodeRoll.roll(100,n->0,null));}
 @Test void optionalAspectSharesTheExistingAuraBudget(){var result=FieryNodeRoll.roll(100,n->0,"addon:nether");assertEquals(10,result.get("addon:nether"));assertEquals(40,result.get("ignis"));assertEquals(10,result.get("mortuus"));assertEquals(10,result.get("tenebrae"));}
 @Test void reusedFireTagDoesNotDowngradeItsWeight(){assertEquals(FieryNodeRoll.roll(100,n->0),FieryNodeRoll.roll(100,n->0,"ignis"));}
}
