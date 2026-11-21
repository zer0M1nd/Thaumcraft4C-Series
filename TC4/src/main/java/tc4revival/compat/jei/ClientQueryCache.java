package tc4revival.compat.jei;

import java.util.*;
import java.util.function.Function;

/** Connection-scoped results, including negative results. Pending and active queries also deduplicate. */
public class ClientQueryCache<K,V,R> {
    public record Batch<V>(int id,List<V> values) {}
    private final Map<K,List<R>> results=new HashMap<>();
    private final LinkedHashMap<K,V> background=new LinkedHashMap<>(),urgent=new LinkedHashMap<>();
    private final LinkedHashMap<K,V> active=new LinkedHashMap<>();
    private int sequence,request;

    public void offer(K key,V value,boolean priority) {
        offerLazy(key,()->value,priority);
    }
    /** Do not copy/serialize a stack again when a pending or completed key already exists. */
    public void offerLazy(K key,java.util.function.Supplier<V> value,boolean priority) {
        if(results.containsKey(key)||active.containsKey(key)||urgent.containsKey(key))return;
        if(priority){var queued=background.remove(key);urgent.put(key,queued!=null?queued:value.get());}
        else if(!background.containsKey(key))background.put(key,value.get());
    }
    public Optional<List<R>> result(K key){return Optional.ofNullable(results.get(key));}
    public boolean waiting(){return !active.isEmpty();}
    public Batch<V> current(){return new Batch<>(request,List.copyOf(active.values()));}
    public Batch<V> begin(int limit) {
        if(waiting())throw new IllegalStateException("Query already in flight");
        if(limit<1)throw new IllegalArgumentException("Empty batch limit");
        take(urgent,limit);take(background,limit);
        if(!active.isEmpty())request=++sequence;
        return current();
    }
    private void take(LinkedHashMap<K,V> source,int limit) {
        var iterator=source.entrySet().iterator();
        while(active.size()<limit&&iterator.hasNext()){
            var entry=iterator.next();active.put(entry.getKey(),entry.getValue());iterator.remove();
        }
    }
    public boolean accepts(int id){return waiting()&&request==id;}
    /** A failed query is not a successful empty result. Release it without populating results. */
    public boolean discard(int id){
        if(!accepts(id))return false;
        active.clear();return true;
    }
    public boolean complete(int id,List<R> rows,Function<R,K> key) {
        if(!accepts(id))return false;
        var grouped=new HashMap<K,List<R>>();
        for(var row:rows){K k=key.apply(row);if(k!=null&&active.containsKey(k))grouped.computeIfAbsent(k,ignored->new ArrayList<>()).add(row);}
        for(var k:active.keySet())results.put(k,List.copyOf(grouped.getOrDefault(k,List.of())));
        active.clear();return true;
    }
    public void clear(){results.clear();background.clear();urgent.clear();active.clear();}
}
