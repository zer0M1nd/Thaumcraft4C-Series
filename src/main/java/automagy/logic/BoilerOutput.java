package automagy.logic;

import java.util.*;
import java.util.random.RandomGenerator;

/** Two passes: refill every typed alembic, then seed at most one empty alembic. */
public final class BoilerOutput {
    public interface Vessel<A>{A aspect();A filter();int amount();boolean acceptOne(A aspect);}
    public static <A> int distribute(Map<A,Integer> buffer,List<? extends Vessel<A>> alembics,RandomGenerator random){
        Set<A> excluded=new HashSet<>();int moved=0;
        for(int i=0;i<Math.min(5,alembics.size());i++){
            var vessel=alembics.get(i);A aspect=vessel.aspect();
            if(aspect!=null&&take(buffer,aspect,vessel)){excluded.add(aspect);moved++;}
        }
        for(int i=0;i<Math.min(5,alembics.size());i++){
            var vessel=alembics.get(i);if(vessel.aspect()!=null&&vessel.amount()!=0)continue;
            A aspect=vessel.filter();
            if(aspect==null){var choices=buffer.entrySet().stream().filter(e->e.getValue()>0&&!excluded.contains(e.getKey())).map(Map.Entry::getKey).toList();if(choices.isEmpty())continue;aspect=choices.get(random.nextInt(choices.size()));}
            if(take(buffer,aspect,vessel)){moved++;break;}
        }return moved;
    }
    private static <A> boolean take(Map<A,Integer> buffer,A aspect,Vessel<A> target){
        int stored=buffer.getOrDefault(aspect,0);if(stored<=0||!target.acceptOne(aspect))return false;
        if(stored==1)buffer.remove(aspect);else buffer.put(aspect,stored-1);return true;
    }
    private BoilerOutput(){}
}
