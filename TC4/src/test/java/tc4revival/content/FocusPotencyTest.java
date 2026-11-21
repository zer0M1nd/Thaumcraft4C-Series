package tc4revival.content;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FocusPotencyTest {
    @Test void integralEffectsDoNotChangeTheRandomStream() {
        var random=RandomSource.create(1);var baseline=RandomSource.create(1);
        for(int amount=0;amount<30;amount++)assertEquals(amount,FocusPotency.discrete(amount,random));
        assertEquals(baseline.nextLong(),random.nextLong());
    }
    @Test void fractionalEffectsPreserveTheirExpectedValue() {
        var random=RandomSource.create(175);long total=0;
        for(int i=0;i<20000;i++) {int count=FocusPotency.discrete(7.5f,random);assertTrue(count==7||count==8);total+=count;}
        assertEquals(7.5,total/20000d,.02);
    }
}
