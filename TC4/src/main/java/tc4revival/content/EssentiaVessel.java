package tc4revival.content;

import tc4revival.aspect.Aspect;

public interface EssentiaVessel {
    boolean takeFromContainer(Aspect aspect,int amount);
    boolean contains(Aspect aspect,int amount);
    Aspect aspect();
    int amount();
    int capacity();
    boolean accepts(Aspect aspect);
    /** Returns the amount that did not fit. */
    int addToContainer(Aspect aspect,int amount);
    boolean acceptsPhials();
}
