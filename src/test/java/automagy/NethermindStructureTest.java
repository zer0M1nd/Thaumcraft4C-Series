package automagy;
import automagy.logic.NethermindStructure;
import static automagy.logic.NethermindStructure.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
class NethermindStructureTest {
 static final Rune BASIC=new Rune("basic",true,false,false,6),ROCK=new Rune("rock",false,false,false,6),SPEED=new Rune("speed",false,true,false,6),BLAZE=new Rune("blaze",false,false,true,6),GHAST=new Rune("ghast",false,false,true,8);
 static final class Fixture implements World {
  final Map<Offset,Rune> runes=new HashMap<>();final Set<Offset> unloaded=new HashSet<>(),blocked=new HashSet<>();int lureDepth=6,lureState=2;
  Fixture(){for(var p:RUNES)runes.put(p,BASIC);}void first(int n,Rune r){for(int i=0;i<n;i++)runes.put(RUNES.get(i),r);}
  public boolean loaded(int x,int y,int z){return !unloaded.contains(new Offset(x,y,z));}public Rune rune(int x,int y,int z){return runes.get(new Offset(x,y,z));}public int lure(int x,int y,int z){return x==0&&z==0&&y==-lureDepth?lureState:0;}public boolean obstructs(int x,int y,int z){return blocked.contains(new Offset(x,y,z));}
 }
 @Test void geometryIncludesTwentyRunesAndSixteenLureStones(){assertEquals(20,RUNES.size());assertEquals(20,new HashSet<>(RUNES).size());assertEquals(4,RUNES.stream().filter(p->p.y()==-1).count());assertEquals(16,new HashSet<>(LURE_RING).size());assertTrue(scan(new Fixture()).valid());}
 @Test void missingAndUnloadedRunesHaveDifferentOutcomes(){var f=new Fixture();f.runes.remove(RUNES.get(0));assertEquals("missing_rune",scan(f).problem());f.unloaded.add(RUNES.get(1));assertTrue(scan(f).unloaded());}
 @Test void blockAndCreatureRunesCannotMixInEitherOrder(){var f=new Fixture();f.first(4,BLAZE);f.runes.put(RUNES.get(4),ROCK);assertEquals("mixed_runes",scan(f).problem());f.runes.put(RUNES.get(0),ROCK);f.runes.put(RUNES.get(4),BLAZE);assertEquals("mixed_runes",scan(f).problem());}
 @Test void creatureModeRequiresFourRunesAndAdequateVerticalRoom(){var f=new Fixture();f.first(3,BLAZE);assertEquals("insufficient_creature_runes",scan(f).problem());f.first(4,BLAZE);assertEquals(6,scan(f).lureDepth());f.runes.put(RUNES.get(0),GHAST);assertEquals("lure_too_close",scan(f).problem());f.lureDepth=8;assertTrue(scan(f).valid());f.blocked.add(new Offset(2,-7,2));assertEquals("obstructed_shaft",scan(f).problem());f.blocked.clear();f.unloaded.add(new Offset(-2,-3,1));assertTrue(scan(f).unloaded());}
 @Test void inactiveAndOutOfRangeLuresDoNotActivate(){var f=new Fixture();f.first(4,BLAZE);f.lureState=1;assertEquals("inactive_lure",scan(f).problem());f.lureState=2;f.lureDepth=11;assertEquals("missing_lure",scan(f).problem());}
 @Test void speedCapAndSelectionRetainBasicFallback(){var f=new Fixture();f.first(20,SPEED);assertEquals(12,scan(f).speedRunes());assertEquals(36,duration(360,12));assertEquals(360,duration(360,0));for(int i=0;i<4;i++)assertEquals(i>=3,chooseBasic(3,i));assertFalse(chooseBasic(4,3));}
 @Test void symmetryAndNitorReduceGrowthWithoutHealingExistingInstability(){assertEquals(35,stability(new int[]{4,4,4,4},8));assertEquals(15,stability(new int[]{4,4,4,3},0));assertEquals(500,instability(500,12,35,0,9));assertEquals(1000,instability(990,0,0,200,9));assertEquals(280,instability(0,0,0,200,0));}
}
