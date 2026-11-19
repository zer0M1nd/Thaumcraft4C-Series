package automagy.logic;

import java.util.*;

/** Server-thread request accounting; claiming work does not mean the items have arrived. */
public final class RequestLedger<K,D> {
    public record Request<K,D>(K item,D destination,long amount) {}
    public record Claim<K,D>(long id,K item,D destination,int amount) {}
    private record Key<K,D>(K item,D destination) {}
    private final LinkedHashMap<Key<K,D>,Long> requests=new LinkedHashMap<>();
    private final LinkedHashMap<Long,Claim<K,D>> claims=new LinkedHashMap<>();
    private long nextId=1;
    public long requested(K item,D destination){return requests.getOrDefault(new Key<>(item,destination),0L);}
    public long inTransit(K item,D destination){return claims.values().stream().filter(c->c.item.equals(item)&&c.destination.equals(destination)).mapToLong(Claim::amount).sum();}
    public long available(K item,D destination){return Math.max(0,requested(item,destination)-inTransit(item,destination));}
    public void request(K item,D destination,long amount){
        Objects.requireNonNull(item);Objects.requireNonNull(destination);if(amount<=0)return;
        var key=new Key<>(item,destination);long old=requests.getOrDefault(key,0L);
        requests.put(key,old>Long.MAX_VALUE-amount?Long.MAX_VALUE:old+amount);
    }
    public Optional<Claim<K,D>> claim(K item,D destination,int capacity){
        int amount=(int)Math.min(Math.max(0,capacity),available(item,destination));if(amount==0)return Optional.empty();
        long id=nextId;
        while(id<=0||claims.containsKey(id)){id=id==Long.MAX_VALUE?1:id+1;}
        nextId=id==Long.MAX_VALUE?1:id+1;var claim=new Claim<>(id,item,destination,amount);claims.put(id,claim);return Optional.of(claim);
    }
    /** Remove the entire reservation; reduce demand only by what was actually delivered. */
    public int deliveryLimit(long task){
        var claim=claims.get(task);return claim==null?0:(int)Math.min(claim.amount,requested(claim.item,claim.destination));
    }
    public int finish(long task,int delivered){
        Claim<K,D> claim=claims.remove(task);if(claim==null)return 0;
        int accepted=(int)Math.max(0,Math.min(requested(claim.item,claim.destination),Math.min(claim.amount,delivered)));var key=new Key<>(claim.item,claim.destination);
        long remaining=Math.max(0,requests.getOrDefault(key,0L)-accepted);
        if(remaining==0)requests.remove(key);else requests.put(key,remaining);return accepted;
    }
    public void fail(long task){claims.remove(task);}
    /** Cancellation does not erase in-flight ownership; callers must return already collected items. */
    public long cancel(K item,D destination,long amount){
        if(amount<=0)return 0;var key=new Key<>(item,destination);long old=requests.getOrDefault(key,0L),removed=Math.min(old,amount);
        if(old==removed)requests.remove(key);else requests.put(key,old-removed);return removed;
    }
    public List<Request<K,D>> requests(){return requests.entrySet().stream().map(e->new Request<>(e.getKey().item,e.getKey().destination,e.getValue())).toList();}
    public List<Claim<K,D>> claims(){return List.copyOf(claims.values());}
    /** Restore only tasks whose carriers are restored too. Unknown reservations must be released. */
    public void restore(List<Request<K,D>> savedRequests,List<Claim<K,D>> savedClaims,Set<Long> liveTasks){
        requests.clear();claims.clear();nextId=1;
        for(var entry:savedRequests)request(entry.item,entry.destination,entry.amount);
        for(var claim:savedClaims){
            if(claim.id<=0||claim.amount<=0||!liveTasks.contains(claim.id)||claims.containsKey(claim.id))continue;
            Objects.requireNonNull(claim.item);Objects.requireNonNull(claim.destination);claims.put(claim.id,claim);
            if(claim.id>=nextId)nextId=claim.id==Long.MAX_VALUE?1:claim.id+1;
        }
    }
}
