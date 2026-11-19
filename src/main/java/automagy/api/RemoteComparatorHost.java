package automagy.api;

/** A reader under a remote comparator borrows its eye; the comparator itself stops emitting. */
public interface RemoteComparatorHost {
    boolean hasComparatorSignal();
}
