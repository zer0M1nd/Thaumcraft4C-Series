package tc4revival.content;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectCatalog;

/** Growth and crossbreeding decisions independent of block updates and rendering. */
public  class ManaPodGrowth {
    private ManaPodGrowth() {}
    public record Result(int age,Aspect aspect) {}
    /** Neighbours must arrive north, south, west, east, omitting empty pods. */
    public static Result advance(int age,Aspect current,List<Aspect> neighbours,AspectCatalog catalog,IntUnaryOperator random) {
        if (age<0 || age>7) throw new IllegalArgumentException("Invalid mana pod age "+age);
        int next=Math.min(7,age+1);
        if (next<=2) return new Result(next,current);
        if (next==3) {
            var parents=new AspectAmounts();
            if (current!=null) parents.add(current,1);
            for (var neighbour:neighbours) if (neighbour!=null) parents.add(neighbour,1);
            if (parents.size()>1) {
                var choices=new ArrayList<Aspect>();
                for (var first:parents.aspects()) {
                    choices.add(first);
                    for (var second:parents.aspects()) if (!first.equals(second)) {
                        catalog.combine(first,second).ifPresent(combined->{ choices.add(combined); choices.add(combined); });
                    }
                }
                current=choices.get(random.applyAsInt(choices.size()));
            }
            if (current==null && !parents.isEmpty()) current=parents.byAmount().get(0);
        }
        if (current==null) current=random.applyAsInt(8)==0 ? catalog.require("herba")
                : catalog.primals().get(random.applyAsInt(catalog.primals().size()));
        return new Result(next,current);
    }
    public static int harvestCount(int age,float roll) {
        if (age<2) return 0;
        return age==7 && roll>.33f ? 2 : 1;
    }
}
