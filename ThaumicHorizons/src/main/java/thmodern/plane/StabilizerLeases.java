package thmodern.plane;
import java.util.*;
/** Device leases are runtime state, deliberately never serialized with a rift. */
public final class StabilizerLeases {
 private final Map<String,Long> leases=new HashMap<>();
 public void renew(String source,long tick){leases.put(source,tick+12);}
 public void remove(String source){leases.remove(source);}
 public int count(long tick){leases.values().removeIf(expiry->expiry<=tick);return leases.size();}
}
