package thaumcraft.api.aspects;

import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;

/** Integer essentia units. Returned amounts mean accepted/extracted, NEVER remainder.
 * Simulation must not mutate inventory, labels, counters or emit events. Implementations run on the server thread.
 * Storage insertion never silently destroys overflow (including void jars).
 */
public interface IEssentiaStorage {
    AspectAmounts contents();
    int capacity(Aspect aspect);
    /** Physical capacity, counted once regardless of aspect filters. Legacy single-tank
     * providers retain a conservative default; shared pools and aggregates should override. */
    default int totalCapacity() {
        long capacity=contents().total();
        for(var aspect:tc4revival.aspect.TCAspects.catalog().all())capacity=Math.max(capacity,capacity(aspect));
        return (int)Math.min(Integer.MAX_VALUE,capacity);
    }
    int insert(Aspect aspect,int amount,boolean simulate);
    int extract(Aspect aspect,int amount,boolean simulate);
}
