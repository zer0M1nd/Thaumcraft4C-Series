package tc4revival.content;

import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;
import tc4revival.aspect.Aspect;
import static org.junit.jupiter.api.Assertions.*;

class EssentiaTubeEmptySourceTest {
    @Test void emptyUnsidedFallbackNeverRequestsAnUnspecifiedAspect() {
        int[] queries={0};
        class Endpoint implements EssentiaTransport {
            final int strength;
            Endpoint(int strength){this.strength=strength;}
            public boolean connectable(Direction face){return true;}
            public boolean canInput(Direction face){return false;}
            public boolean canOutput(Direction face){return strength==0;}
            public Aspect suctionType(Direction face){return null;}
            public int suction(Direction face){return strength;}
            public int minimumSuction(){return 0;}
            public Aspect essentiaType(Direction face){if(face==null)queries[0]++;return null;}
            public int essentiaAmount(Direction face){return 0;}
            public int takeEssentia(Aspect aspect,int amount,Direction face){fail("Empty source must not receive an extraction");return 0;}
            public int addEssentia(Aspect aspect,int amount,Direction face){return 0;}
        }
        var empty=new Endpoint(0);var sink=new Endpoint(32);
        EssentiaTubeFlow.Host host=new EssentiaTubeFlow.Host() {
            final RandomSource random=RandomSource.create(42);
            public EssentiaTransport neighbor(Direction side){return side==Direction.WEST?empty:side==Direction.EAST?sink:null;}
            public RandomSource random(){return random;}
            public boolean powered(){return false;}
            public int bellows(){return 0;}
            public void changed(){}
            public void valveSound(){}
            public void creak(){}
            public void vent(Aspect type){}
        };
        var tube=new EssentiaTubeFlow(EssentiaTubeFlow.Kind.NORMAL,host);
        for(int tick=0;tick<100;tick++)tube.tick();
        assertTrue(queries[0]>0);
        assertEquals(0,tube.essentiaAmount(null));
    }
}
