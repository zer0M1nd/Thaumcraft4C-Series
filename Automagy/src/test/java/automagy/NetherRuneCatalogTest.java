package automagy;
import automagy.logic.NetherRuneCatalog;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class NetherRuneCatalogTest {
 @Test void basicRockCostsTwiceTheDedicatedRune(){var rock=NetherRuneCatalog.BUILTINS.get("rock");var basic=NetherRuneCatalog.basic(false);assertEquals(rock.duration(),basic.duration());rock.cost().forEach((aspect,n)->assertEquals(n*2,basic.cost().get(aspect)));assertEquals("minecraft:lava",NetherRuneCatalog.basic(true).output());}
 @Test void creatureDefinitionsCarryBothCostsAndSpatialRequirements(){assertEquals(13,NetherRuneCatalog.BUILTINS.size());for(var rune:NetherRuneCatalog.BUILTINS.values())if(rune.creature()){assertEquals(360,rune.duration());assertFalse(rune.baitCost().isEmpty());assertTrue(rune.baitUses()>0);}assertEquals(8,NetherRuneCatalog.BUILTINS.get("ghast").lureDistance());assertEquals(375,NetherRuneCatalog.BUILTINS.get("wisp").instability());assertTrue(NetherRuneCatalog.BUILTINS.get("celerity").speed());}
}
