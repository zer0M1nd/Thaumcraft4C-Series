package thmodern.plane;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class PlaneDepositsTest {
 @Test void unsupportedAspectsLeaveUnselectedProbability(){var p=new PlaneParameters(Map.of("terra",1,"motus",3));assertEquals("terra",PlaneDeposits.select(p,PlaneDeposits.POCKETS,.1).orElseThrow());assertTrue(PlaneDeposits.select(p,PlaneDeposits.POCKETS,.5).isEmpty());assertTrue(PlaneDeposits.select(new PlaneParameters(Map.of()),PlaneDeposits.CAVES,0).isEmpty());}
 @Test void depositsRetainSpecialMaterials(){var random=new Random(12);assertEquals("thaumcraft:liquid_death",PlaneDeposits.pocket("venenum",random));assertEquals("thaumcraft:cinnabar_ore",PlaneDeposits.pocket("permutatio",random));assertEquals("thaumcraft:air_infused_stone",PlaneDeposits.pocket("aer",random));assertEquals("thaumcraft:flesh_block",PlaneDeposits.pocket("fames",random));assertEquals("minecraft:skeleton_skull",PlaneDeposits.cave("exanimis",random));assertEquals("thaumcraft:water_crystal_cluster",PlaneDeposits.cave("aqua",random));}
 @Test void seededMappingsCoverAllBranchesAndRemainRepeatable(){var first=new Random(951);var second=new Random(951);for(int i=0;i<100;i++){for(var aspect:PlaneDeposits.POCKETS)assertEquals(PlaneDeposits.pocket(aspect,first),PlaneDeposits.pocket(aspect,second));for(var aspect:PlaneDeposits.CAVES)assertEquals(PlaneDeposits.cave(aspect,first),PlaneDeposits.cave(aspect,second));}}
}
