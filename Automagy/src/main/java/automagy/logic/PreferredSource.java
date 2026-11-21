package automagy.logic;
import java.util.*;
import java.util.function.BiPredicate;
/** Remembers a successful source per resource; a failed attempt is never repeated in one request. */
public final class PreferredSource<K,A>{
    private final Map<A,K> previous=new HashMap<>();
    public Optional<K> take(A aspect,List<K> candidates,BiPredicate<K,A> extractOne){
        K preferred=previous.remove(aspect);Set<K> attempted=new HashSet<>();
        if(preferred!=null&&candidates.contains(preferred)){attempted.add(preferred);if(extractOne.test(preferred,aspect)){previous.put(aspect,preferred);return Optional.of(preferred);}}
        for(K candidate:candidates)if(attempted.add(candidate)&&extractOne.test(candidate,aspect)){previous.put(aspect,candidate);return Optional.of(candidate);}
        return Optional.empty();
    }
}
