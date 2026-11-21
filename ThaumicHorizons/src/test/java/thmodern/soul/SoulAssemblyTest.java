package thmodern.soul;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SoulAssemblyTest {
    @Test void bitsRequireEnergyAndKeepRemainder(){var state=new SoulAssembly();assertEquals(0,state.accept(10));state.load(9500,0,1000);assertEquals(250,state.accept(250));assertEquals(1,state.souls());assertEquals(150,state.bits());assertEquals(750,state.energy());}
    @Test void clampsPacketToEnergyAndOutputCapacity(){var state=new SoulAssembly();state.load(9599,15,3000);assertEquals(1,state.accept(Integer.MAX_VALUE));assertEquals(16,state.souls());assertEquals(2999,state.energy());assertEquals(0,state.accept(1));assertTrue(state.takeSoul());assertEquals(20,state.accept(20));}
    @Test void respectsTopUpThresholdAndRejectsNegativeInput(){var state=new SoulAssembly();assertEquals(0,state.addEssentia(-1));for(int i=0;i<3;i++)assertEquals(1,state.addEssentia(100));assertEquals(0,state.addEssentia(1));state.accept(1);assertEquals(1,state.addEssentia(1));assertEquals(3999,state.energy());assertEquals(0,state.accept(-1));}
}
