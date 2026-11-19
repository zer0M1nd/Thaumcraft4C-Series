package automagy.logic;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class NetherSpireSiteTest {
 static class World implements NetherSpireSite.Column{
  Set<Integer> lava=new HashSet<>(List.of(20,21,22));Set<Integer> blocked=new HashSet<>();int max=128;
  public int minimumY(){return 0;}public int maximumY(){return max;}public boolean lavaLayer(int y){return lava.contains(y);}public boolean airLayer(int y){return !lava.contains(y)&&!blocked.contains(y);}
 }
 @Test void threeFullLavaLayersAndThirtyAir(){var w=new World();assertEquals(23,NetherSpireSite.find(w).orElseThrow());w.blocked.add(52);assertTrue(NetherSpireSite.find(w).isEmpty());}
 @Test void rejectsThinLavaAndOutOfBounds(){var w=new World();w.lava.remove(20);assertTrue(NetherSpireSite.find(w).isEmpty());w.lava.add(20);w.max=52;assertTrue(NetherSpireSite.find(w).isEmpty());}
 @Test void obstructedLowerPoolDoesNotHideHigherPool(){var w=new World();w.blocked.add(30);w.lava.addAll(List.of(60,61,62));assertEquals(63,NetherSpireSite.find(w).orElseThrow());}
 @Test void fieryNodeGuaranteedFireAndOneDeathFamily(){for(int seed=0;seed<100;seed++){var rng=new Random(seed);var result=FieryNodeRoll.roll(200,rng::nextInt);assertTrue(result.get("ignis")>=20);assertNotEquals(result.containsKey("mortuus"),result.containsKey("exanimis"));assertTrue(result.values().stream().allMatch(v->v>0));}}
}
