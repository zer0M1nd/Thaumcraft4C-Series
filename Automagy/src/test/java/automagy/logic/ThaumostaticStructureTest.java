package automagy.logic;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ThaumostaticStructureTest {
 private static class World implements ThaumostaticStructure.World {
  Set<Integer> pylons=new HashSet<>(List.of(-2,0));boolean loaded=true,sky=true,active=false;
  public int minimumY(){return -64;}public int maximumY(){return 320;}public boolean loaded(int y){return loaded;}
  public boolean pylon(int y){return pylons.contains(y);}public boolean base(int y){return y==-3;}public boolean thaumium(int y){return y==1;}
  public boolean spire(int y){return active&&y==1;}public boolean spireTop(int y){return active&&y==2;}public boolean air(int y){return y==2;}public boolean sky(int y){return sky;}
 }
 @Test void modernNegativeHeightAndSplitPylons(){var w=new World();var result=ThaumostaticStructure.scan(w,-1,false);assertTrue(result.valid());assertEquals(2,result.pylons());assertEquals(1,result.spire());}
 @Test void unloadedSuspendsAndRoofBlocksActivationOnly(){var w=new World();w.loaded=false;assertTrue(ThaumostaticStructure.scan(w,-1,false).unloaded());w.loaded=true;w.sky=false;assertFalse(ThaumostaticStructure.scan(w,-1,false).valid());w.active=true;assertTrue(ThaumostaticStructure.scan(w,-1,true).valid());}
 @Test void missingPylonInvalidatesStructure(){var w=new World();w.pylons.remove(0);assertFalse(ThaumostaticStructure.scan(w,-1,false).valid());}
}
