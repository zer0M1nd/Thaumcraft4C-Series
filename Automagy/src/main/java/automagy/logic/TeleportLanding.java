package automagy.logic;
import java.util.OptionalDouble;
import java.util.function.*;
/** Find the same vertically grounded position as a living entity teleport, retaining fractional Y. */
public final class TeleportLanding {
 public static OptionalDouble find(double requestedY,int minimumY,IntPredicate supports,DoublePredicate clear){
  if(!Double.isFinite(requestedY)||requestedY<minimumY||requestedY>Integer.MAX_VALUE-1)return OptionalDouble.empty();
  int blockY=(int)Math.floor(requestedY);double y=requestedY;
  while(blockY>minimumY){if(supports.test(blockY-1))return clear.test(y)?OptionalDouble.of(y):OptionalDouble.empty();blockY--;y--;}
  return OptionalDouble.empty();
 }
 private TeleportLanding(){}
}
