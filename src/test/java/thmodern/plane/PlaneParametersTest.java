package thmodern.plane;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class PlaneParametersTest {
 @Test void radiusAndEmptyEnvironmentAreFinite(){var empty=new PlaneParameters(Map.of());assertEquals(32,empty.radius());assertEquals(50,empty.noise());assertEquals(0,empty.life());assertEquals("minecraft:forest",empty.biome());assertEquals(127,new PlaneParameters(Map.of("aer",Integer.MAX_VALUE,"terra",Integer.MAX_VALUE)).radius());assertEquals(75,new PlaneParameters(Map.of("aer",100)).radius());}
 @Test void climatePrioritiesOverrideNumericalTemperature(){assertEquals("minecraft:desert",new PlaneParameters(Map.of("ignis",100)).biome());assertEquals("minecraft:snowy_plains",new PlaneParameters(Map.of("gelum",100)).biome());assertEquals("thaumcraft:eerie",new PlaneParameters(Map.of("ignis",100,"alienis",1,"praecantatio",1)).biome());assertEquals("thaumcraft:tainted_land",new PlaneParameters(Map.of("vitium",1,"alienis",100)).biome());}
 @Test void noiseLifeAndColorPreserveLegacyWeights(){assertEquals(0,new PlaneParameters(Map.of("ordo",10)).noise());assertEquals(100,new PlaneParameters(Map.of("perditio",10)).noise());assertEquals(100,new PlaneParameters(Map.of("victus",10)).life());assertEquals(0,new PlaneParameters(Map.of("mortuus",10)).life());assertEquals(0x7f007f,new PlaneParameters(Map.of("aer",1,"terra",1)).color(a->a.equals("aer")?0xff0000:0x0000ff));}
 @Test void effectsAndConcentricShellsKeepOrderAndDuplicates(){var plan=new PlaneParameters(Map.of("motus",1,"iter",1,"alienis",1,"tenebrae",1,"vitreus",1,"gelum",1));assertEquals(java.util.List.of("minecraft:speed","minecraft:speed"),plan.effects());assertEquals(java.util.List.of("eldritch","obsidian","eldritch","glass","packed_ice"),plan.shells());}
}

