package automagy.logic;

import java.util.*;
import java.util.function.IntUnaryOperator;
/** The controller stores three independent eight-unit buffers, not one shared tank. */
public final class ThaumostaticCycle {
    public static final List<String> ASPECTS=List.of("potentia","aer","volatus");
    public record Snapshot(Map<String,Integer> amounts,int rangeTicks,int rateTicks,int distributionTicks){
        public Snapshot {amounts=Map.copyOf(amounts);}
    }
    private final Map<String,Integer> amounts=new LinkedHashMap<>();
    private int rangeTicks,rateTicks,distributionTicks=30;
    public int amount(String aspect){return amounts.getOrDefault(aspect,0);}
    public int insert(String aspect,int requested){if(requested!=1||!ASPECTS.contains(aspect)||amount(aspect)>=8)return 0;amounts.put(aspect,amount(aspect)+1);return 1;}
    public int extract(String aspect,int requested,boolean active){if(active||requested<=0||amount(aspect)<requested)return 0;set(aspect,amount(aspect)-requested);return requested;}
    private void set(String aspect,int amount){if(amount==0)amounts.remove(aspect);else amounts.put(aspect,amount);}
    public int range(int pylons){return Math.max(0,Math.min(6,pylons))*(rangeTicks>0?15:4);}
    public boolean rangeBoost(){return rangeTicks>0;}public boolean rateBoost(){return rateTicks>0;}
    public void activated(){distributionTicks=30;}
    /** Recipient callbacks receive at most one unit and return the amount actually accepted. */
    public boolean tick(boolean powered,int pylons,List<IntUnaryOperator> recipients){
        if(!powered)return false;
        boolean transferred=false;
        if(distributionTicks>0)distributionTicks--;
        if(distributionTicks==0){
            distributionTicks=30-(rateTicks>0?Math.max(0,Math.min(6,pylons))*4:0);
            for(var recipient:recipients){if(amount("potentia")<1)break;
                int accepted=recipient.applyAsInt(1);
                if(accepted<0||accepted>1)throw new IllegalStateException("Invalid harness transfer amount");
                if(accepted==1){set("potentia",amount("potentia")-1);transferred=true;}
            }
        }
        if(rangeTicks>0)rangeTicks--;
        if(rangeTicks==0&&amount("potentia")>0&&amount("aer")>0){set("aer",amount("aer")-1);rangeTicks=1200;}
        if(rateTicks>0)rateTicks--;
        if(rateTicks==0&&amount("potentia")>0&&amount("volatus")>0){set("volatus",amount("volatus")-1);rateTicks=1200;}
        return transferred;
    }
    public Snapshot snapshot(){return new Snapshot(amounts,rangeTicks,rateTicks,distributionTicks);}
    public void restore(Snapshot state){amounts.clear();for(String aspect:ASPECTS){int amount=Math.max(0,Math.min(8,state.amounts.getOrDefault(aspect,0)));if(amount>0)amounts.put(aspect,amount);}rangeTicks=Math.max(0,Math.min(1200,state.rangeTicks));rateTicks=Math.max(0,Math.min(1200,state.rateTicks));distributionTicks=Math.max(0,Math.min(30,state.distributionTicks));}
}
