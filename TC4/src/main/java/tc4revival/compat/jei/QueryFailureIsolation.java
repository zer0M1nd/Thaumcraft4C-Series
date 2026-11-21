package tc4revival.compat.jei;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

/** Failed identities are separate from result caches and retried after data/connection invalidation. */
public class QueryFailureIsolation<K> {
    private final Set<K> failed=new HashSet<>();
    public boolean run(K key,Runnable query,BiConsumer<K,Throwable> report){
        if(failed.contains(key))return false;
        try{query.run();return true;}
        catch(RuntimeException|LinkageError error){failed.add(key);report.accept(key,error);return false;}
    }
    public void clear(){failed.clear();}
}
