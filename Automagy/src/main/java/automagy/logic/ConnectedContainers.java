package automagy.logic;

import java.util.*;
import java.util.function.*;

/** Bounded depth-first traversal with stable seed and neighbor order; invalid/unloaded cells are barriers. */
public final class ConnectedContainers {
    public static <K> List<K> discover(List<K> seeds,int limit,Predicate<K> readable,Function<K,List<K>> neighbors){
        if(limit<=0)return List.of();
        Set<K> visited=new HashSet<>();List<K> result=new ArrayList<>();Deque<K> pending=new ArrayDeque<>();
        for(K seed:seeds){pending.push(seed);
            while(!pending.isEmpty()){
                K pos=pending.pop();if(!visited.add(pos)||!readable.test(pos))continue;
                result.add(pos);if(result.size()>=limit)return List.copyOf(result);
                var adjacent=neighbors.apply(pos);for(int i=adjacent.size()-1;i>=0;i--)if(!visited.contains(adjacent.get(i)))pending.push(adjacent.get(i));
            }
        }return List.copyOf(result);
    }
    private ConnectedContainers(){}
}
