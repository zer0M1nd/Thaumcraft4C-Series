package tc4revival.content;

import net.minecraft.nbt.CompoundTag;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectNbt;
import tc4revival.aspect.TCAspects;

/** The advanced furnace spends network vis to dissolve one thrown item at a time. */
public  class AdvancedAlchemyProcess {
    public static final int CAPACITY=500,MAX_POWER=500;
    public interface Host {
        int drain(Aspect aspect,int requested);
        void changed(boolean sync);
    }
    private AspectAmounts contents=new AspectAmounts();
    private int heat,entropy,water,cooldown,ticks;
    public int heat() { return heat; }
    public int entropy() { return entropy; }
    public int water() { return water; }
    public int cooldown() { return cooldown; }
    public int amount() { return (int)contents.total(); }
    public AspectAmounts contents() { return contents.copy(); }
    public Aspect firstAspect() { return contents.isEmpty() ? null : contents.aspects().get(0); }
    public int amount(Aspect aspect) { return contents.amount(aspect); }
    public void tick(Host host) {
        ticks++;
        if (cooldown>0) cooldown--;
        if (ticks%5!=0) return;
        int previous=heat; heat=Math.max(0,heat-1);
        if (heat<MAX_POWER) heat+=host.drain(TCAspects.catalog().require("ignis"),Math.min(50,MAX_POWER-heat));
        if (entropy<MAX_POWER) entropy+=host.drain(TCAspects.catalog().require("perditio"),Math.min(50,MAX_POWER-entropy));
        if (water<MAX_POWER) water+=host.drain(TCAspects.catalog().require("aqua"),Math.min(50,MAX_POWER-water));
        host.changed(previous/50!=heat/50);
    }
    /** The caller removes exactly one item only after this returns true. */
    public boolean process(AspectAmounts input,Host host) {
        if (cooldown!=0 || input==null || input.isEmpty()) return false;
        long size=input.total();
        if (size<0 || size>CAPACITY-contents.total() || input.view().values().stream().anyMatch(value->value<0)) return false;
        int cost=(int)size;
        if (2*cost>heat || cost>entropy || cost>water) return false;
        heat-=2*cost; entropy-=cost; water-=cost;
        // Delay is computed after paying heat, using the same float truncation as TC4.
        cooldown=(int)(5.0f+Math.max(0.0f,(1.0f-(float)heat/MAX_POWER)*100.0f));
        contents.add(input); host.changed(true); return true;
    }
    public boolean take(Aspect aspect,int amount,Host host) {
        if (aspect==null || amount<=0 || contents.amount(aspect)<amount) return false;
        contents.remove(aspect,amount); host.changed(true); return true;
    }
    public void save(CompoundTag tag) {
        AspectNbt.write(tag,contents); tag.putShort("vis",(short)amount()); tag.putShort("heat",(short)heat);
        tag.putShort("power1",(short)entropy); tag.putShort("power2",(short)water);
        tag.putInt("Cooldown",cooldown); tag.putInt("TickPhase",ticks%5);
    }
    public void load(CompoundTag tag) {
        contents=AspectNbt.read(tag,TCAspects.catalog()).amounts();
        heat=Math.max(0,tag.getShort("heat")); entropy=Math.max(0,tag.getShort("power1")); water=Math.max(0,tag.getShort("power2"));
        cooldown=Math.max(0,tag.getInt("Cooldown")); ticks=Math.floorMod(tag.getInt("TickPhase"),5);
    }
}
