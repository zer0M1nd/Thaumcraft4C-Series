package tc4revival.content;

import net.minecraft.nbt.CompoundTag;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectNbt;
import tc4revival.aspect.TCAspects;

/** Portable current charge and permanent capacity; old items infer capacity from charge. */
public record NodeJarData(AspectAmounts aspects,AuraNodeType type,AuraNodeModifier modifier,String id,AspectAmounts capacity) {
    public NodeJarData(AspectAmounts aspects,AuraNodeType type,AuraNodeModifier modifier,String id) { this(aspects,type,modifier,id,aspects); }
    public NodeJarData { aspects=aspects.copy();capacity=capacity.copy();for(var aspect:aspects.aspects())capacity.merge(aspect,aspects.amount(aspect)); }
    @Override public AspectAmounts capacity() { return capacity.copy(); }
    @Override public AspectAmounts aspects() { return aspects.copy(); }
    public CompoundTag write() {
        var tag=new CompoundTag(); AspectNbt.write(tag,aspects); AspectNbt.write(tag,"AspectsBase",capacity); tag.putInt("nodetype",type.ordinal());
        if (modifier!=null) tag.putInt("nodemod",modifier.ordinal()); tag.putString("nodeid",id); return tag;
    }
    public static NodeJarData read(CompoundTag tag) {
        int type=tag.getInt("nodetype"),modifier=tag.contains("nodemod") ? tag.getInt("nodemod") : -1;
        return new NodeJarData(AspectNbt.read(tag,TCAspects.catalog()).amounts(),type>=0 && type<AuraNodeType.values().length ? AuraNodeType.values()[type] : AuraNodeType.NORMAL,
                modifier>=0 && modifier<AuraNodeModifier.values().length ? AuraNodeModifier.values()[modifier] : null,tag.getString("nodeid"),tag.contains("AspectsBase")?AspectNbt.read(tag,"AspectsBase",TCAspects.catalog()).amounts():AspectNbt.read(tag,TCAspects.catalog()).amounts());
    }
    public static AuraNodeModifier degraded(AuraNodeModifier modifier) {
        return modifier==null ? AuraNodeModifier.PALE : switch(modifier) { case BRIGHT->null; case PALE,FADING->AuraNodeModifier.FADING; };
    }
}
