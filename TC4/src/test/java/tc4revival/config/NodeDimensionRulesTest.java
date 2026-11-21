package tc4revival.config;

import java.util.List;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NodeDimensionRulesTest {
    @Test void allDimensionsDefaultToOneWithExplicitOverrides(){
        var rules=new NodeDimensionRules(List.of("twilightforest:twilight_forest=2","minecraft:the_nether=0","custom:moon=0.25"));
        assertEquals(2,rules.multiplier("twilightforest:twilight_forest"));
        assertEquals(0,rules.multiplier("minecraft:the_nether"));
        assertEquals(.25,rules.multiplier("custom:moon"));
        for(var id:List.of("minecraft:overworld","minecraft:the_end","thaumcraft:outer_lands","unknown:world"))assertEquals(1,rules.multiplier(id));
    }
    @Test void invalidRulesAreRejectedAndLastDuplicateWins(){
        for(Object bad:List.of("foo","foo=1","minecraft:world=-1","minecraft:world=NaN","minecraft:world=Infinity","bad id:world=1",7))assertFalse(NodeDimensionRules.valid(bad),bad.toString());
        assertEquals(3,new NodeDimensionRules(List.of("test:world=2","test:world=3")).multiplier("test:world"));
    }
    @Test void ordinaryRatePreservesRandomStreamAndZeroConsumesNothing(){
        var old=RandomSource.create(92);var modern=RandomSource.create(92);
        for(int i=0;i<1000;i++)assertEquals(old.nextInt(36)==0,NodeDimensionRules.roll(modern,36,1));
        assertFalse(NodeDimensionRules.roll(modern,36,0));assertEquals(old.nextInt(),modern.nextInt());
    }
    @Test void multiplierControlsProbabilityAndSaturates(){
        var random=RandomSource.create(100);int count=0;
        for(int i=0;i<36000;i++)if(NodeDimensionRules.roll(random,36,2))count++;
        assertTrue(count>1800&&count<2200,"Twilight should average two nodes per 36 candidate chunks: "+count);
        for(int i=0;i<100;i++)assertTrue(NodeDimensionRules.roll(random,36,100));
    }
}
