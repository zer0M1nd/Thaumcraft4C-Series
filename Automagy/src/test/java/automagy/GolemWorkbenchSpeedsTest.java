package automagy;
import automagy.api.GolemWorkbenchSpeeds;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GolemWorkbenchSpeedsTest {
 @Test void externalMaterialsDoNotInheritTheirPlaceholderWoodSpeed(){assertEquals(4,GolemWorkbenchSpeeds.base(null,"WOOD"));assertEquals(2,GolemWorkbenchSpeeds.base("test:unregistered","WOOD"));GolemWorkbenchSpeeds.register("test:fast",6);assertEquals(6,GolemWorkbenchSpeeds.base("test:fast","WOOD"));}
 @Test void invalidSpeedsAndDuplicateRegistrationAreRejected(){assertThrows(IllegalArgumentException.class,()->GolemWorkbenchSpeeds.register("invalid",2));assertThrows(IllegalArgumentException.class,()->GolemWorkbenchSpeeds.register("test:bad",65));GolemWorkbenchSpeeds.register("test:unique",3);assertThrows(IllegalArgumentException.class,()->GolemWorkbenchSpeeds.register("test:unique",4));}
}
