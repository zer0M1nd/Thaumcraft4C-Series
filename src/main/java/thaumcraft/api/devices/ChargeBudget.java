package thaumcraft.api.devices;

import java.util.*;
import java.util.function.IntUnaryOperator;

/** A source/tick owns this budget, shared by charging, repair, native slots and external adapters. */
public final class ChargeBudget<K> {
    private final int limit;
    private final Map<K,Integer> spent=new HashMap<>();
    private long tick=Long.MIN_VALUE;
    public ChargeBudget(int limit){if(limit<0)throw new IllegalArgumentException("Negative charge budget");this.limit=limit;}
    private void at(long now){if(now!=tick){tick=now;spent.clear();}}
    /** Pure allowance query: this does not promise upstream vis availability or reserve anything. */
    public int allowance(long now,K key,int requested){at(now);return Math.min(Math.max(0,requested),limit-spent.getOrDefault(key,0));}
    public int pay(long now,K key,int requested,IntUnaryOperator drain){
        int allowed=allowance(now,key,requested);if(allowed==0)return 0;
        // Reserve before calling external code, so reentrant payments cannot exceed the tick budget.
        int previous=spent.getOrDefault(key,0);spent.put(key,previous+allowed);
        int actual=drain.applyAsInt(allowed);
        if(actual<0||actual>allowed)throw new IllegalStateException("Invalid vis source payment: "+actual);
        spent.put(key,spent.get(key)-(allowed-actual));return actual;
    }
}
