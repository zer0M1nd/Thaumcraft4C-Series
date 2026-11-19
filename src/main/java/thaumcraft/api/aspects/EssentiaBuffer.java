package thaumcraft.api.aspects;

import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.nbt.CompoundTag;
import tc4revival.aspect.*;

/** Reusable multi-aspect buffer with a SHARED total capacity. Its owner persists and synchronizes changes. */
public  class EssentiaBuffer implements IEssentiaStorage {
    private final int limit;
    private final Predicate<tc4revival.aspect.Aspect> filter;
    private final Runnable changed;
    private AspectAmounts values=new AspectAmounts();
    public EssentiaBuffer(int capacity,Predicate<tc4revival.aspect.Aspect> filter,Runnable changed){if(capacity<1)throw new IllegalArgumentException("Positive capacity required");limit=capacity;this.filter=Objects.requireNonNull(filter);this.changed=Objects.requireNonNull(changed);}
    public AspectAmounts contents(){return values.copy();}
    @Override public int totalCapacity(){return limit;}
    public int capacity(tc4revival.aspect.Aspect aspect){return filter.test(aspect)?values.amount(aspect)+(int)(limit-values.total()):0;}
    private static void check(tc4revival.aspect.Aspect aspect,int amount){Objects.requireNonNull(aspect);if(amount<0)throw new IllegalArgumentException("Negative transfer");}
    public int insert(tc4revival.aspect.Aspect aspect,int amount,boolean simulate){check(aspect,amount);if(!filter.test(aspect))return 0;int accepted=(int)Math.min(amount,limit-values.total());if(!simulate&&accepted>0){values.add(aspect,accepted);changed.run();}return accepted;}
    public int extract(tc4revival.aspect.Aspect aspect,int amount,boolean simulate){check(aspect,amount);int taken=Math.min(amount,values.amount(aspect));if(!simulate&&taken>0){values.remove(aspect,taken);changed.run();}return taken;}
    public CompoundTag save(){var tag=new CompoundTag();AspectNbt.write(tag,values);return tag;}
    /** Invalid/oversized input is rejected before changing the buffer. No silent overflow deletion. */
    public void load(CompoundTag tag){for(var entry:tag.getList("Aspects",net.minecraft.nbt.Tag.TAG_COMPOUND))if(((CompoundTag)entry).getInt("amount")<0)throw new IllegalArgumentException("Negative persisted essentia");var decoded=AspectNbt.read(tag,TCAspects.catalog());if(!decoded.problems().isEmpty())throw new IllegalArgumentException("Invalid buffer data: "+decoded.problems());var result=decoded.amounts();if(result.total()>limit||result.view().values().stream().anyMatch(n->n<0))throw new IllegalArgumentException("Buffer data exceeds capacity");values=result.copy();}
}
