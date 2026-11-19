package gadomancymodern.node;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import tc4revival.aspect.*;
import tc4revival.content.*;
import net.minecraft.util.RandomSource;
public class NodeManipulationsTest {
    static Aspect aer=new Aspect("aer",0xffffff,List.of(),"",771),terra=new Aspect("terra",0x00ff00,List.of(),"",771),victus=new Aspect("victus",0xff0000,List.of("aer","terra"),"",771);static AspectCatalog catalog=new AspectCatalog(List.of(aer,terra,victus));
    static NodeManipulations.State state(AspectAmounts values){return new NodeManipulations.State(catalog,values,values,AuraNodeType.NORMAL,null,false,true);}
    @Test public void combiningTwoEqualComponentsCannotReadTheMutatedShorterList(){var state=state(new AspectAmounts().add(aer,10).add(terra,10));assertTrue(NodeManipulations.RESULTS.get("gadomancy:combine").apply(state,RandomSource.create(1)));assertEquals(1,state.base.size());assertTrue(state.base.amount(victus)>=10&&state.base.amount(victus)<=13);assertEquals(state.base.amount(victus),state.charge.amount(victus));}
    @Test public void splittingRebuildsBothCapacitiesAndChargeAtOriginalAmount(){var state=state(new AspectAmounts().add(victus,12));state.charge.remove(victus,11);assertTrue(NodeManipulations.RESULTS.get("gadomancy:break_compound").apply(state,RandomSource.create(2)));assertEquals(12,state.base.amount(aer));assertEquals(12,state.charge.amount(terra));assertFalse(state.base.contains(victus));}
    @Test public void brightnessStepsAndInitialGrowingRetryQuirkArePreserved(){var state=state(new AspectAmounts().add(aer,12));state.modifier=AuraNodeModifier.FADING;var up=NodeManipulations.RESULTS.get("gadomancy:brighter");for(int i=0;i<3;i++)assertTrue(up.apply(state,RandomSource.create()));assertEquals(AuraNodeModifier.BRIGHT,state.modifier);assertFalse(up.accepts(state));var growing=NodeManipulations.RESULTS.get("gadomancy:growing");assertFalse(growing.apply(state,RandomSource.create()));assertTrue(state.growing);assertTrue(growing.apply(state,RandomSource.create()));}
}
