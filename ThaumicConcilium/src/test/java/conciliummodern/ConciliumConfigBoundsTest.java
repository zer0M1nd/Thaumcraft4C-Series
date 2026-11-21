package conciliummodern;

import java.util.*;
import net.minecraftforge.common.ForgeConfigSpec;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConciliumConfigBoundsTest {
    private static ForgeConfigSpec.ValueSpec spec(ForgeConfigSpec.ConfigValue<?> value){return ConciliumMobConfig.SPEC.getSpec().get(value.getPath());}
    @Test void percentagesCannotExceedCertaintyOrBecomeNegative(){
        for(var value:List.of(ConciliumMobConfig.MAD_REPLACEMENT,ConciliumMobConfig.PALADIN_REPLACEMENT,ConciliumMobConfig.RIFT_CHANCE)){
            assertTrue(spec(value).test(0));assertTrue(spec(value).test(100));assertFalse(spec(value).test(-1));assertFalse(spec(value).test(101));
        }
        assertEquals(20,ConciliumMobConfig.MAD_REPLACEMENT.getDefault());assertEquals(20,ConciliumMobConfig.PALADIN_REPLACEMENT.getDefault());assertEquals(100,ConciliumMobConfig.RIFT_CHANCE.getDefault());
    }
    @Test void spawnWeightAndIntervalAllowExplicitDisableWithoutOverflow(){
        for(var value:List.of(ConciliumMobConfig.RANGER_WEIGHT,ConciliumMobConfig.MAD_WEIGHT,ConciliumMobConfig.QUICKSILVER_WEIGHT,ConciliumMobConfig.THAUMATURGE_WEIGHT)){
            assertEquals(5,value.getDefault());assertTrue(spec(value).test(0));assertTrue(spec(value).test(1000000));assertFalse(spec(value).test(-1));assertFalse(spec(value).test(1000001));
        }
        var interval=spec(ConciliumMobConfig.NETHER_EXPLORER_RATE);assertEquals(300,interval.getDefault());assertTrue(interval.test(0));assertTrue(interval.test(Integer.MAX_VALUE));assertFalse(interval.test(-1));
    }
    @Test void biomeBlacklistAcceptsAnEmptyListButRejectsMalformedRegistryKeys(){
        for(var value:List.of(ConciliumMobConfig.RANGER_BIOME_BLACKLIST,ConciliumMobConfig.MAD_BIOME_BLACKLIST,ConciliumMobConfig.QUICKSILVER_BIOME_BLACKLIST,ConciliumMobConfig.THAUMATURGE_BIOME_BLACKLIST)){
            assertTrue(spec(value).test(List.of()));assertTrue(spec(value).test(List.of("minecraft:plains","thaumcraft:magical_forest")));assertFalse(spec(value).test(List.of("bad key")));assertFalse(spec(value).test(List.of(2)));
        }
    }
}
