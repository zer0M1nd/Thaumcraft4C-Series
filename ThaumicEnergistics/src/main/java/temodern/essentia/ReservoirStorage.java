package temodern.essentia;

import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;
import tc4revival.content.EssentiaReservoirBlockEntity;
import thaumcraft.api.aspects.IEssentiaStorage;

/** Inventory access to the whole multi-aspect reservoir, independent of its directional pipe port. */
public class ReservoirStorage implements IEssentiaStorage {
    private final EssentiaReservoirBlockEntity reservoir;
    public ReservoirStorage(EssentiaReservoirBlockEntity reservoir) { this.reservoir = reservoir; }
    @Override public AspectAmounts contents() { return reservoir.revealedAspects(); }
    @Override public int capacity(Aspect aspect) {
        return contents().amount(aspect) + Math.max(0, EssentiaReservoirBlockEntity.CAPACITY - reservoir.amount());
    }
    @Override public int insert(Aspect aspect, int amount, boolean simulate) {
        int accepted = Math.min(amount, Math.max(0, EssentiaReservoirBlockEntity.CAPACITY - reservoir.amount()));
        return simulate ? accepted : accepted - reservoir.addToContainer(aspect, accepted);
    }
    @Override public int extract(Aspect aspect, int amount, boolean simulate) {
        int taken = Math.min(amount, contents().amount(aspect));
        return simulate ? taken : reservoir.takeFromContainer(aspect, taken) ? taken : 0;
    }
}
