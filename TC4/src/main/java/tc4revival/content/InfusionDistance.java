package tc4revival.content;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;

/** Unit insert/delete/replace distance. Matching is injective even for overlapping tags. */
public class InfusionDistance {
    public static <A,B> boolean[][] matches(List<A> expected, List<B> actual, BiPredicate<A,B> predicate) {
        var result = new boolean[expected.size()][actual.size()];
        for (int i=0;i<expected.size();i++) for (int j=0;j<actual.size();j++)
            result[i][j] = predicate.test(expected.get(i), actual.get(j));
        return result;
    }
    /** Result maps each expected ingredient to one actual item, or -1. */
    public static int[] assignment(boolean[][] matches, int actualSize) {
        int[] owners = new int[actualSize]; Arrays.fill(owners, -1);
        for (int i=0;i<matches.length;i++) augment(i, matches, owners, new boolean[actualSize]);
        int[] result = new int[matches.length]; Arrays.fill(result, -1);
        for (int j=0;j<actualSize;j++) if (owners[j]>=0) result[owners[j]]=j;
        return result;
    }
    private static boolean augment(int expected, boolean[][] matches, int[] owners, boolean[] seen) {
        for (int j=0;j<owners.length;j++) if (!seen[j] && matches[expected][j]) {
            seen[j]=true;
            if (owners[j]<0 || augment(owners[j], matches, owners, seen)) { owners[j]=expected; return true; }
        }
        return false;
    }
    public static int unordered(boolean[][] matches, int actualSize) {
        int matched=0; for (int index : assignment(matches, actualSize)) if (index>=0) matched++;
        return Math.max(matches.length, actualSize)-matched;
    }
    /** The existing altar accepts cyclic rotations, but never reverses the sequence. */
    public static int ordered(boolean[][] matches, int actualSize, int limit) {
        int n=matches.length, best=Math.max(n,actualSize);
        if (n==0 || actualSize==0) return best;
        if (Math.abs(n-actualSize)>limit) return limit+1;
        int[] previous=new int[actualSize+1], current=new int[actualSize+1];
        for (int offset=0;offset<actualSize;offset++) {
            for (int j=0;j<=actualSize;j++) previous[j]=j;
            boolean pruned=false;
            for (int i=1;i<=n;i++) {
                current[0]=i; int minimum=i;
                for (int j=1;j<=actualSize;j++) {
                    current[j]=Math.min(previous[j-1]+(matches[i-1][(offset+j-1)%actualSize]?0:1),
                            Math.min(previous[j]+1,current[j-1]+1));
                    minimum=Math.min(minimum,current[j]);
                }
                int[] swap=previous; previous=current; current=swap;
                if (minimum>Math.min(best,limit)) { pruned=true; break; }
            }
            if (!pruned) best=Math.min(best,previous[actualSize]);
            if (best==Math.abs(n-actualSize)) break;
        }
        return best;
    }
    private InfusionDistance() {}
}
