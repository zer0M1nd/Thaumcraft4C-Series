package gadomancymodern.aura;
import java.util.*;
import java.util.function.IntUnaryOperator;
/** Six primal weights gathered from a 9x9x9 environment, without client authority. */
public class AuraComposition {
    public static final List<String> PRIMALS=List.of("aer","ignis","aqua","terra","ordo","perditio");
    public static final int REQUIRED_BLOCKS=109;
    protected final Map<String,Integer> amounts=new LinkedHashMap<>();
    public AuraComposition(){PRIMALS.forEach(a->amounts.put(a,0));}
    public void add(String aspect,int amount){if(amount>0&&amounts.containsKey(aspect))amounts.put(aspect,(int)Math.min(Integer.MAX_VALUE,(long)amounts.get(aspect)+amount));}
    public int amount(String aspect){return amounts.getOrDefault(aspect,0);}
    public List<String> ranked(){return PRIMALS.stream().filter(a->amount(a)>0).sorted(Comparator.<String>comparingInt(this::amount).reversed()).toList();}
    public long total(){return amounts.values().stream().mapToLong(Integer::longValue).sum();}
    public List<String> orbitals(){
        var sorted=ranked();if(sorted.isEmpty())return Collections.nCopies(6,"aer");
        var seats=new ArrayList<String>();int left=6;
        for(String a:sorted){int count=Math.min(left,(int)Math.ceil(left*(amount(a)/(double)total())));left-=count;for(int i=0;i<count;i++)seats.add(a);if(left==0)break;}
        while(seats.size()<6)seats.add(sorted.get(0));return seats;
    }
    public String choose(int blocks,IntUnaryOperator random){
        var sorted=ranked();if(blocks<REQUIRED_BLOCKS||sorted.isEmpty())return "blank";
        double average=total()/(double)sorted.size();var eligible=sorted.stream().filter(a->amount(a)>=average).toList();
        // Source weights are bounded on input; keep sampling safe for edited save data too.
        long total=eligible.stream().mapToLong(this::amount).sum();double scale=Math.max(1,total/(double)(Integer.MAX_VALUE-6));
        int sum=eligible.stream().mapToInt(a->Math.max(1,(int)(amount(a)/scale))).sum(),draw=random.applyAsInt(sum);
        for(String a:eligible){draw-=Math.max(1,(int)(amount(a)/scale));if(draw<0)return a;}return eligible.get(eligible.size()-1);
    }
    public static int consumedAt(int elapsed){return (int)Math.max(0,Math.min(729,(Math.max(0,(long)elapsed-50)*729/450)));}
    public static int randomHalfCost(int bound,IntUnaryOperator random){int half=bound/2;return half<=0?0:half+random.applyAsInt(half);}
}
