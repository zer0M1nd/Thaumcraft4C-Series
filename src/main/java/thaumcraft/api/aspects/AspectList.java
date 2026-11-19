package thaumcraft.api.aspects;

import tc4revival.aspect.AspectAmounts;
/** Mutable quantity builder; conversion methods copy to keep ownership explicit. */
public  class AspectList {
    private AspectAmounts values=new AspectAmounts();
    public AspectList() {}
    public AspectList(net.minecraft.world.item.ItemStack stack) { values=tc4revival.aspect.item.ItemStackAspects.resolve(stack).orElseGet(AspectAmounts::new).copy(); }
    public static AspectList from(AspectAmounts amounts) { var result=new AspectList();result.values=amounts.copy();return result; }
    public AspectAmounts unwrap() { return values.copy(); }
    public AspectList copy() { return from(values); }
    public int size() { return values.size(); }
    public int visSize() { return Math.toIntExact(values.total()); }
    public int getAmount(Aspect aspect) { return values.amount(aspect.unwrap()); }
    public Aspect[] getAspects() { return values.aspects().stream().map(Aspect::wrap).toArray(Aspect[]::new); }
    public Aspect[] getAspectsSortedAmount() { return values.byAmount().stream().map(Aspect::wrap).toArray(Aspect[]::new); }
    public AspectList add(Aspect aspect,int amount) { values.add(aspect.unwrap(),amount);return this; }
    public AspectList add(AspectList list) { values.add(list.values);return this; }
    public AspectList merge(Aspect aspect,int amount) { values.merge(aspect.unwrap(),amount);return this; }
    public boolean reduce(Aspect aspect,int amount) { return values.reduce(aspect.unwrap(),amount); }
    public AspectList remove(Aspect aspect,int amount) { values.remove(aspect.unwrap(),amount);return this; }
    public AspectList remove(Aspect aspect) { values.remove(aspect.unwrap());return this; }
    public void writeToNBT(net.minecraft.nbt.CompoundTag tag) { tc4revival.aspect.AspectNbt.write(tag,values); }
    public void readFromNBT(net.minecraft.nbt.CompoundTag tag) { values=tc4revival.aspect.AspectNbt.read(tag,tc4revival.aspect.TCAspects.catalog()).amounts(); }
}
