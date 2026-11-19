package thaumcraft.api.aspects;

/** Multi-aspect container. setAspects must persist/synchronize the owning block entity. */
public interface IAspectContainer extends tc4revival.content.EssentiaSource,tc4revival.content.RevealedAspectContainer {
    AspectList getAspects();
    void setAspects(AspectList values);
    boolean accepts(tc4revival.aspect.Aspect aspect);
    int capacity(tc4revival.aspect.Aspect aspect);
    @Override default tc4revival.aspect.AspectAmounts revealedAspects() { return getAspects().unwrap(); }
    @Override default boolean contains(tc4revival.aspect.Aspect aspect,int amount) { return amount>=0&&getAspects().unwrap().amount(aspect)>=amount; }
    @Override default boolean takeFromContainer(tc4revival.aspect.Aspect aspect,int amount) {
        if(amount<0)return false;
        var values=getAspects().unwrap();if(!values.reduce(aspect,amount))return false;
        if(values.amount(aspect)==0)values.remove(aspect);setAspects(AspectList.from(values));return true;
    }
    /** Returns the remainder, as in the original container contract. */
    default int addToContainer(tc4revival.aspect.Aspect aspect,int amount) {
        if(amount<0)throw new IllegalArgumentException("Negative transfer");
        if(!accepts(aspect))return amount;
        var values=getAspects().unwrap();int accepted=Math.min(amount,Math.max(0,capacity(aspect)-values.amount(aspect)));
        if(accepted>0){values.add(aspect,accepted);setAspects(AspectList.from(values));}return amount-accepted;
    }
}
