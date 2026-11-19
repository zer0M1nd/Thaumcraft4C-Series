package thmodern.vat;

import java.util.*;

/** A bounded reservation, not a general-purpose essentia store. Changing a job cannot refund its fuel. */
public final class VatEssentia {
    private final LinkedHashMap<String,Integer> required=new LinkedHashMap<>(),stored=new LinkedHashMap<>();
    public void demand(Map<String,Integer> costs){
        var checked=new LinkedHashMap<String,Integer>();
        if(costs.size()>128)throw new IllegalArgumentException("Too many aspects");
        costs.forEach((key,value)->{if(key==null||key.isBlank()||value==null||value<=0||value>32767)throw new IllegalArgumentException("Invalid essentia cost");checked.put(key,value);});
        required.clear();required.putAll(checked);
        stored.keySet().retainAll(required.keySet());stored.replaceAll((key,value)->Math.min(value,required.get(key)));
    }
    public Map<String,Integer> required(){return Collections.unmodifiableMap(required);}
    public Map<String,Integer> stored(){return Collections.unmodifiableMap(stored);}
    public String wanted(){for(var entry:required.entrySet())if(amount(entry.getKey())<entry.getValue())return entry.getKey();return null;}
    public int amount(String key){return stored.getOrDefault(key,0);}
    public int accept(String key,int amount){
        if(amount<=0||!required.containsKey(key))return 0;
        int accepted=Math.min(amount,required.get(key)-amount(key));
        if(accepted>0)stored.merge(key,accepted,Integer::sum);return accepted;
    }
    public boolean paid(){return !required.isEmpty()&&wanted()==null;}
    public boolean consume(String key,int count){if(count<=0||amount(key)<count)return false;int left=amount(key)-count;if(left==0)stored.remove(key);else stored.put(key,left);return true;}
    public boolean consumeAll(){if(!paid())return false;clear();return true;}
    public void clear(){required.clear();stored.clear();}
}
