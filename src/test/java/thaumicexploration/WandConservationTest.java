package thaumicexploration;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class WandConservationTest {
    @Test void noChargeIsCreatedWithoutSurplus(){int[] input={100,200,300,400,500,600};assertArrayEquals(input,WandBehaviors.transmuteAmounts(input,7500));}
    @Test void fullWandsKeepTheirCharge(){int[] input={7500,7500,7500,7500,7500,7500};assertArrayEquals(input,WandBehaviors.transmuteAmounts(input,7500));}
    @Test void surplusPaysFourToOne(){int[] next=WandBehaviors.transmuteAmounts(new int[]{7500,6750,6750,6750,6750,0},7500);assertArrayEquals(new int[]{6750,6787,6787,6787,6787,37},next);}
    @Test void randomizedConversionNeverCreatesVisOrExceedsCapacity(){var random=new Random(237);for(int j=0;j<10000;j++){int cap=100+random.nextInt(20000);int[] input=random.ints(6,0,cap+1).toArray(),copy=input.clone();int[] next=WandBehaviors.transmuteAmounts(input,cap);assertArrayEquals(copy,input);assertTrue(Arrays.stream(next).allMatch(n->n>=0&&n<=cap));assertTrue(Arrays.stream(next).sum()<=Arrays.stream(input).sum());}}
}
