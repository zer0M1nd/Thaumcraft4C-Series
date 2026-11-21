package thaumicomniverse.compat.railcraft;

import net.minecraft.nbt.CompoundTag;
import tc4revival.aspect.*;
import thaumcraft.api.aspects.IEssentiaStorage;

/** A single-aspect reservoir; simulation has no callbacks or NBT changes. */
public final class RailEssentiaTank implements IEssentiaStorage {
    private final int limit;
    private final Runnable changed;
    private Aspect aspect;
    private int amount;
    public RailEssentiaTank(int limit,Runnable changed){this.limit=limit;this.changed=changed;}
    public Aspect aspect(){return aspect;}
    public int amount(){return amount;}
    public int limit(){return limit;}
    @Override public AspectAmounts contents(){return aspect==null?new AspectAmounts():new AspectAmounts().add(aspect,amount);}
    @Override public int capacity(Aspect type){return type!=null&&(amount==0||type.equals(aspect))?limit:0;}
    @Override public int insert(Aspect type,int count,boolean simulate){
        if(count<0)throw new IllegalArgumentException("Negative essentia");
        int accepted=Math.min(count,Math.max(0,capacity(type)-amount));
        if(!simulate&&accepted>0){aspect=type;amount+=accepted;changed.run();}return accepted;
    }
    @Override public int extract(Aspect type,int count,boolean simulate){
        if(count<0)throw new IllegalArgumentException("Negative essentia");
        int taken=type!=null&&type.equals(aspect)?Math.min(count,amount):0;
        if(!simulate&&taken>0){amount-=taken;if(amount==0)aspect=null;changed.run();}return taken;
    }
    public CompoundTag save(){var tag=new CompoundTag();AspectNbt.write(tag,contents());return tag;}
    public void load(CompoundTag tag){
        var contents=AspectNbt.read(tag,TCAspects.catalog()).amounts();aspect=null;amount=0;
        if(contents.size()==1){var type=contents.aspects().get(0);int value=Math.min(limit,Math.max(0,contents.amount(type)));if(value>0){aspect=type;amount=value;}}
    }
}
