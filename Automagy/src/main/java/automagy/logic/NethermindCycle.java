package automagy.logic;
import java.util.*;
import java.util.function.*;
/** Paid units and the selected variant are retained while waiting for a valid output location. */
public final class NethermindCycle {
 public record Snapshot(String rune,boolean doubleBlaze,int remaining,int power,int age,boolean instabilityChecked,Map<String,Integer> needed){public Snapshot{needed=Map.copyOf(needed);}}
 private String rune="";private boolean doubleBlaze,instabilityChecked;private int remaining,power,age;private final Map<String,Integer> needed=new LinkedHashMap<>();
 public String rune(){return rune;}public boolean doubleBlaze(){return doubleBlaze;}public int remaining(){return remaining;}public int power(){return power;}public int age(){return age;}public boolean selected(){return !rune.isEmpty();}public Map<String,Integer> needed(){return Map.copyOf(needed);}
 public void select(String key,boolean extra,int duration,Map<String,Integer> cost){rune=Objects.requireNonNull(key);doubleBlaze=extra;remaining=Math.max(1,duration);instabilityChecked=false;needed.clear();cost.forEach((id,n)->{if(n>0)needed.put(id,n);});}
 public void clear(){rune="";doubleBlaze=false;remaining=0;power=0;age=0;instabilityChecked=false;needed.clear();}
 /** Callback returns actual centivis extracted, at most the requested 20. */
 public boolean powerTick(boolean creature,IntUnaryOperator draw){age=age==Integer.MAX_VALUE?20:age+1;power=Math.max(0,power-(creature?2:1));if(power==0)power=Math.max(0,Math.min(20,draw.applyAsInt(20)));return power>0;}
 public boolean acquisitionTick(int speed){return age%Math.max(5-Math.max(0,speed),1)==0;}
 /** Stops after the first successfully acquired unit, but tries other aspects if one is absent. */
 public boolean acquire(Predicate<String> drain){for(var entry:needed.entrySet())if(drain.test(entry.getKey())){int count=entry.getValue();if(count==1)needed.remove(entry.getKey());else entry.setValue(count-1);return true;}return false;}
 /** A caller with missing bait must not invoke this; ready stays true until complete/retry. */
 public boolean advance(){if(!selected()||power==0||!needed.isEmpty())return false;if(remaining>0)remaining--;return remaining==0;}
 public void retry(int delay){remaining=Math.max(1,delay);}
 public boolean checkInstabilityOnce(){if(instabilityChecked)return false;instabilityChecked=true;return true;}
 public void complete(){rune="";remaining=0;needed.clear();doubleBlaze=false;instabilityChecked=false;}
 public Snapshot snapshot(){return new Snapshot(rune,doubleBlaze,remaining,power,age,instabilityChecked,needed);}
 public void restore(Snapshot saved){clear();rune=Objects.requireNonNullElse(saved.rune(),"");doubleBlaze=saved.doubleBlaze();remaining=Math.max(0,saved.remaining());power=Math.max(0,Math.min(20,saved.power()));age=Math.max(0,saved.age());instabilityChecked=saved.instabilityChecked();saved.needed().forEach((id,n)->{if(id!=null&&!id.isBlank()&&n!=null&&n>0)needed.put(id,n);});if(rune.isBlank())complete();}
}
