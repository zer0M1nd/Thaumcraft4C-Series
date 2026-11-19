package automagy;
import automagy.logic.RunicBeaconBase;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RunicBeaconBaseTest {
    private static final RunicBeaconBase.Cell METAL=new RunicBeaconBase.Cell(true,true,false);
    private static final RunicBeaconBase.Cell RUNE=new RunicBeaconBase.Cell(true,false,true);
    private static final RunicBeaconBase.Cell AIR=new RunicBeaconBase.Cell(true,false,false);
    @Test void ordinaryBeaconsAreNotOverridden(){assertEquals(-1,RunicBeaconBase.levels((x,d,z)->METAL));}
    @Test void mixedRunicFirstTierAcceptsFourOrdinaryTiers(){assertEquals(4,RunicBeaconBase.levels((x,d,z)->d==1&&x==0&&z==0?RUNE:METAL));}
    @Test void runesCannotReplaceDeeperTiers(){assertEquals(1,RunicBeaconBase.levels((x,d,z)->d<=2?RUNE:METAL));}
    @Test void holeInFirstTierDoesNotActivateBeacon(){assertEquals(-1,RunicBeaconBase.levels((x,d,z)->x==1&&z==1?AIR:RUNE));}
    @Test void missingChunkStopsAtLastCompleteTier(){assertEquals(2,RunicBeaconBase.levels((x,d,z)->d==1?RUNE:d==2?METAL:new RunicBeaconBase.Cell(false,false,false)));}
}
