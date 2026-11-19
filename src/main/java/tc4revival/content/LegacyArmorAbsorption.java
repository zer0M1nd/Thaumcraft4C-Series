package tc4revival.content;

import java.util.*;

/** Forge 1.7 armor uses damage scaled by 25 for durability caps and descending priority groups. */
public  class LegacyArmorAbsorption {
    public record Piece(int slot,int priority,double ratio,int maximum) {}
    public record Result(float remaining,Map<Integer,Integer> wear) { public Result { wear=Map.copyOf(wear); } }
    private LegacyArmorAbsorption() {}
    public static Result apply(float damage,List<Piece> input) {
        var pieces=new ArrayList<>(input);
        pieces.sort((a,b)->a.priority()!=b.priority()?b.priority()-a.priority():(int)(threshold(a)-threshold(b)));
        double remaining=damage*25d;
        var wear=new LinkedHashMap<Integer,Integer>();
        for (int begin=0;begin<pieces.size() && remaining>0;) {
            int end=begin+1;
            while (end<pieces.size() && pieces.get(end).priority()==pieces.get(begin).priority()) end++;
            double[] ratios=new double[end-begin];
            for (int i=0;i<ratios.length;i++) ratios[i]=pieces.get(begin+i).ratio();
            int cursor=0; boolean exhausted=false;
            while (cursor<ratios.length) {
                double total=Arrays.stream(ratios).sum();
                if (total<=1) {
                    for (int i=cursor;i<ratios.length;i++) ratios[i]=Math.min(ratios[i],pieces.get(begin+i).maximum()/remaining);
                    break;
                }
                boolean capped=false;
                for (int i=cursor;i<ratios.length;i++) {
                    double normalized=ratios[i]/total,cap=pieces.get(begin+i).maximum()/remaining;
                    if (normalized>cap) { ratios[i]=cap; cursor=i+1; capped=true; break; }
                    ratios[i]=normalized;
                }
                if (!capped) { exhausted=true; break; }
            }
            double absorbed=0;
            for (int i=0;i<ratios.length;i++) {
                double share=remaining*ratios[i]; absorbed+=ratios[i];
                if (share>0) wear.put(pieces.get(begin+i).slot(),Math.max(1,(int)(share/25)));
            }
            remaining-=remaining*absorbed; begin=end;
            // Legacy normalization zeros later priorities even if floating-point residual damage remains.
            if (exhausted) break;
        }
        return new Result((float)(remaining/25),wear);
    }
    private static double threshold(Piece piece) { return piece.ratio()==0?0:piece.maximum()*100d/piece.ratio(); }
}
