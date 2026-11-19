package automagy.logic;
import java.util.*;
/** Bait is per creature family, including both ordinary and double-blaze outcomes. */
public final class NetherBait {
 private final Map<String,Integer> uses=new TreeMap<>();private final Map<String,Integer> needed=new LinkedHashMap<>();private String active="";private int reward;
 public void select(String family,Map<String,Integer> cost,int count){active=Objects.requireNonNull(family);reward=Math.max(1,count);needed.clear();if(remaining(family)>0)return;cost.forEach((aspect,amount)->{if(amount>0)needed.put(aspect,amount);});if(needed.isEmpty())addReward();}
 public record Snapshot(String active,int reward,Map<String,Integer> uses,Map<String,Integer> needed){public Snapshot{uses=Map.copyOf(uses);needed=Map.copyOf(needed);}}
 public Snapshot snapshot(){return new Snapshot(active,reward,uses,needed);}
 public void restore(Snapshot saved){restore(saved.uses());active=Objects.requireNonNullElse(saved.active(),"");reward=Math.max(0,saved.reward());if(!active.isBlank()&&reward>0)saved.needed().forEach((id,n)->{if(id!=null&&!id.isBlank()&&n!=null&&n>0)needed.put(id,n);});else clearActive();}
 public Map<String,Integer> needed(){return Collections.unmodifiableMap(new LinkedHashMap<>(needed));}
 public Map<String,Integer> stored(){return Map.copyOf(uses);}
 public int remaining(String family){return uses.getOrDefault(family,0);}
 /** Called only after one actual essentia unit has been received. */
 public boolean received(String aspect){int count=needed.getOrDefault(aspect,0);if(count<=0||active.isEmpty())return false;if(count==1)needed.remove(aspect);else needed.put(aspect,count-1);if(needed.isEmpty())addReward();return true;}
 private void addReward(){uses.put(active,(int)Math.min(Integer.MAX_VALUE,(long)remaining(active)+reward));}
 public boolean consume(){int n=remaining(active);if(n<=0)return false;if(n==1)uses.remove(active);else uses.put(active,n-1);return true;}
 public void clearActive(){active="";reward=0;needed.clear();}
 public void restore(Map<String,Integer> saved){clearActive();uses.clear();saved.forEach((id,n)->{if(id!=null&&!id.isBlank()&&n!=null&&n>0)uses.put(id,n);});}
}
