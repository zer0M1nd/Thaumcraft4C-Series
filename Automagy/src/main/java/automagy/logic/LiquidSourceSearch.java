package automagy.logic;
import java.util.*;
import java.util.function.*;
/** Bounded loaded-world traversal; callers classify an unloaded cell as absent. */
public final class LiquidSourceSearch {
 public record Cell(boolean connected,boolean source){}
 private record Step<P>(P pos,int depth){}
 public static <P> Optional<P> find(P start,Function<P,Cell> inspect,Function<P,List<P>> neighbors,ToDoubleFunction<P> distance){
  var pending=new ArrayDeque<Step<P>>();var seen=new HashSet<P>();pending.push(new Step<>(start,0));P best=null;double farthest=-1;
  // Bound both depth and visited cells to prevent a large ocean scan monopolizing a tick.
  while(!pending.isEmpty()&&seen.size()<4096){var step=pending.pop();if(!seen.add(step.pos))continue;var cell=inspect.apply(step.pos);if(!cell.connected)continue;
   if(cell.source){double d=distance.applyAsDouble(step.pos);if(d>farthest){best=step.pos;farthest=d;}}
   if(step.depth<32){var adjacent=neighbors.apply(step.pos);for(int i=adjacent.size()-1;i>=0;i--)if(!seen.contains(adjacent.get(i)))pending.push(new Step<>(adjacent.get(i),step.depth+1));}
  }
  return Optional.ofNullable(best);
 }
 private LiquidSourceSearch(){}
}
