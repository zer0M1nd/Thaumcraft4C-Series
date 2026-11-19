package tc4revival.content;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EssentiaAvailabilityTest {
 @Test void exactReadOnlyAvailabilityAcrossEntireIntegerRange(){
  for(int amount:new int[]{0,1,2,63,64,65,1024,1000000,Integer.MAX_VALUE}){
   int[] calls={0};EssentiaSource source=new EssentiaSource(){
    public boolean contains(tc4revival.aspect.Aspect aspect,int n){calls[0]++;return n<=amount;}
    public boolean takeFromContainer(tc4revival.aspect.Aspect aspect,int n){throw new AssertionError("Preview consumed essentia");}
   };
   assertEquals(amount,source.availableAmount(null));assertTrue(calls[0]<=64);
  }
 }
}
