package ttmodern;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EnchanterResearchTest {
 @Test void legacyAndExistingSaveResearchBothUnlockWithoutAncestors(){
  var e=new EnchanterCatalog.Entry(new ResourceLocation("test:ench"),"legacy",new ResourceLocation("test:icon"),Map.of("aer",1),List.of("modern"));
  assertFalse(e.unlocked(k->false));assertTrue(e.unlocked("legacy"::equals));assertTrue(e.unlocked("modern"::equals));assertFalse(e.unlocked("other"::equals));
 }
 @Test void oldPublicConstructorKeepsUngatedDefinitionsAndImmutableCosts(){
  var source=new HashMap<String,Integer>();source.put("aer",2);
  var e=new EnchanterCatalog.Entry(new ResourceLocation("test:ench"),"",new ResourceLocation("test:icon"),source);
  source.clear();assertEquals(Map.of("aer",2),e.base());assertTrue(e.unlocked(k->false));assertTrue(e.researchAlternatives().isEmpty());
 }
}
