package automagy.logic;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScribePowerTest {
    @Test void partialNetworkSupplyBuysOnlyItsActualDuration(){
        ScribePower power=new ScribePower();AtomicInteger calls=new AtomicInteger();
        power.tick(true,request->{assertEquals(10,request);calls.incrementAndGet();return 3;});
        assertEquals(6,power.remaining());
        for(int i=0;i<5;i++)power.tick(true,request->{fail("Must spend stored charge first");return 0;});
        assertEquals(1,power.remaining());
        power.tick(true,request->{calls.incrementAndGet();return 0;});
        assertFalse(power.powered());assertEquals(2,calls.get());
    }
    @Test void disconnectedDeviceSpendsPaidChargeWithoutDraining(){
        ScribePower power=new ScribePower();power.restore(2);
        for(int i=0;i<4;i++)power.tick(false,request->{fail("Disconnected drain");return 10;});
        assertFalse(power.powered());
        power.restore(Integer.MAX_VALUE);assertEquals(20,power.remaining());
        power.restore(-1);assertEquals(0,power.remaining());
    }
}
