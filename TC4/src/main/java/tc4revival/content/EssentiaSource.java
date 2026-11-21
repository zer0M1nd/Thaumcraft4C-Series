package tc4revival.content;

import tc4revival.aspect.Aspect;

/** A reservoir from which TC4 devices may draw essentia without a pipe connection. */
public interface EssentiaSource {
    boolean takeFromContainer(Aspect aspect,int amount);
    boolean contains(Aspect aspect,int amount);
    /** Read-only availability. Proxies expose their reservoirs so overlapping routes are not counted twice. */
    default java.util.List<? extends EssentiaSource> availabilitySources() { return java.util.List.of(this); }
    default Object availabilityIdentity() { return this; }
    default int availableAmount(Aspect aspect) {
        if (!contains(aspect,1)) return 0;
        int low=1,high=2;
        while (contains(aspect,high)) {
            low=high;if(high==Integer.MAX_VALUE)return high;
            high=(int)Math.min(Integer.MAX_VALUE,(long)high*2);
        }
        while ((long)high-low>1) {int mid=low+(high-low)/2;if(contains(aspect,mid))low=mid;else high=mid;}
        return low;
    }
    default boolean mirror() { return false; }
    default net.minecraft.core.GlobalPos mirrorDestination() { return null; }
}
