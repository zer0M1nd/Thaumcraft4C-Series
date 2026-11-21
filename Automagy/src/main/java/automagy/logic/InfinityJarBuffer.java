package automagy.logic;
/** Legacy refill timer protects withdrawals during refill and when the last unit is taken. */
public final class InfinityJarBuffer {
 public record Snapshot(String aspect,int amount,int refill){}
 private String aspect="";private int amount,refill;
 public String aspect(){return aspect;}public int amount(){return amount;}
 public void select(String selected){aspect=selected;amount=1;refill=0;}
 public int insert(String offered,int count){if(offered==null||offered.isEmpty()||count<=0||amount>0&&!aspect.equals(offered))return count;aspect=offered;amount+=Math.min(count,64-amount);return 0;}
 public boolean contains(String requested,int count){return requested!=null&&aspect.equals(requested)&&count>=0&&count<=amount;}
 public boolean extract(String requested,int count){if(!contains(requested,count))return false;amount-=count;if(refill>0||amount==0)amount+=count;return true;}
 public void tick(boolean roll){if(!roll)return;if(!aspect.isEmpty()&&amount<64){if(amount<32)refill=64-amount;if(refill>0)amount++;}if(refill>0)refill--;}
 public Snapshot snapshot(){return new Snapshot(aspect,amount,refill);}
 public void restore(Snapshot s){aspect=s.aspect==null?"":s.aspect;amount=aspect.isEmpty()?0:Math.max(0,Math.min(64,s.amount));refill=Math.max(0,Math.min(64,s.refill));}
}
