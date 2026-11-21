package tc4revival.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Balanced subset selection, then counterclockwise placement as viewed from above. */
public class InfusionPlacementOrder {
    private static double angle(BlockPos center,BlockPos p) { return Math.atan2(-(p.getZ()-center.getZ()),p.getX()-center.getX()); }
    private static double positive(double a) { double turn=Math.PI*2;return (a%turn+turn)%turn; }
    private static double distance(double a,double b) { double d=positive(a-b);return Math.min(d,Math.PI*2-d); }
    private static BlockPos mirror(BlockPos center,BlockPos p) { return new BlockPos(2*center.getX()-p.getX(),p.getY(),2*center.getZ()-p.getZ()); }
    public static List<BlockPos> select(BlockPos center,List<BlockPos> candidates,int count,Direction facing) {
        if(count<0||count>candidates.size())throw new IllegalArgumentException("Insufficient pedestals");
        var available=new HashSet<>(candidates);var remaining=new ArrayList<>(candidates);var selected=new ArrayList<BlockPos>();
        double firstAngle=Math.atan2(-facing.getStepZ(),facing.getStepX());
        Comparator<BlockPos> tie=Comparator.comparingLong((BlockPos p)->p.distManhattan(center)).thenComparingInt(BlockPos::getY).thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ);
        while(selected.size()<count) {
            var paired=remaining.stream().filter(p->available.contains(mirror(center,p))).toList();
            var pool=paired.isEmpty()?List.copyOf(remaining):paired;
            BlockPos next;
            if(selected.isEmpty())next=pool.stream().min(Comparator.comparingDouble((BlockPos p)->distance(angle(center,p),firstAngle)).thenComparing(tie)).orElseThrow();
            else {
                var opposite=mirror(center,selected.get(selected.size()-1));
                if(pool.contains(opposite))next=opposite;
                else next=pool.stream().min(Comparator.comparingDouble((BlockPos p)->-selected.stream().mapToDouble(s->distance(angle(center,p),angle(center,s))).min().orElse(0)).thenComparing(tie)).orElseThrow();
            }
            selected.add(next);remaining.remove(next);
        }
        return List.copyOf(selected);
    }
    public static List<BlockPos> counterclockwise(BlockPos center,List<BlockPos> selected) {
        if(selected.isEmpty())return List.of();double start=angle(center,selected.get(0));
        return selected.stream().sorted(Comparator.comparingDouble(p->positive(angle(center,p)-start))).toList();
    }
}
