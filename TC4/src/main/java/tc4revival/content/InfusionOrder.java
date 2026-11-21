package tc4revival.content;

import java.util.List;
import java.util.Comparator;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;

/** One orientation for manual and JEI placement; starting angular sector is arbitrary. */
public class InfusionOrder {
    public static List<BlockPos> clockwise(BlockPos center,List<BlockPos> positions) {
        return positions.stream().filter(p->p.getX()!=center.getX() || p.getZ()!=center.getZ())
                .sorted(Comparator.comparingDouble((BlockPos p)->Math.atan2(p.getZ()-center.getZ(),p.getX()-center.getX()))
                        .thenComparingLong(p->p.distManhattan(center)).thenComparingInt(BlockPos::getY)).toList();
    }
    public static <A,B> boolean cyclicMatches(List<A> required,List<B> supplied,BiPredicate<A,B> match) {
        if(required.size()!=supplied.size())return false;
        if(required.isEmpty())return true;
        for(int offset=0;offset<supplied.size();offset++) {
            boolean valid=true;
            for(int i=0;i<required.size();i++)if(!match.test(required.get(i),supplied.get((offset+i)%supplied.size()))) {valid=false;break;}
            if(valid)return true;
        }
        return false;
    }
}
