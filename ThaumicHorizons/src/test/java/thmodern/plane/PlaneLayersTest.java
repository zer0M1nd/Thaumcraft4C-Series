package thmodern.plane;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class PlaneLayersTest {
 @Test void orderedLayersReserveFleshThicknessAndStoneUndersoil(){
  var p=new PlaneParameters(Map.of("aqua",40,"corpus",40,"terra",40));var layers=PlaneLayers.of(p);
  assertEquals(List.of("minecraft:water","thaumcraft:flesh_block","minecraft:dirt","minecraft:stone"),layers.stream().map(PlaneLayers.Layer::block).toList());
  assertEquals(128,layers.get(0).height());assertEquals(108,layers.get(1).height());assertEquals(78,layers.get(2).height());assertEquals(73,layers.get(3).height());
 }
 @Test void fallbackAndUnstableEarthUseCorrectMaterials(){
  assertEquals("thaumichorizons:dust",PlaneLayers.of(new PlaneParameters(Map.of("aer",100))).get(0).block());
  assertEquals("minecraft:cobblestone",PlaneLayers.of(new PlaneParameters(Map.of("terra",100,"perditio",1))).get(1).block());
  assertEquals(0,PlaneLayers.of(new PlaneParameters(Map.of("aqua",100))).get(0).noise());
 }
 @Test void sphericalColumnsRemainInsideWorldAndPlaneExtent(){
  assertEquals(1,PlaneLayers.bottom(127,0,0));assertEquals(255,PlaneLayers.top(127,0,0,999));
  assertTrue(PlaneLayers.bottom(32,33,0)>PlaneLayers.top(32,33,0,128));
  assertEquals(128,PlaneLayers.bottom(32,32,0));assertEquals(128,PlaneLayers.top(32,32,0,999));
  assertEquals(0,PlaneLayers.solidity(new PlaneParameters(Map.of("vacuos",100))));
  assertEquals(.75,PlaneLayers.solidity(new PlaneParameters(Map.of("ordo",100))));
 }
}
