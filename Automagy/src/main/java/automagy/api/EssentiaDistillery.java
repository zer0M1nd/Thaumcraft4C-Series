package automagy.api;
/** Server-thread controls for fuel helpers; changes must persist and notify clients. */
public interface EssentiaDistillery {
    int burnTime(); void burnTime(int ticks);
    boolean speedBoosted(); void speedBoosted(boolean boosted);
    int storedEssentia(); int essentiaCapacity();
}
