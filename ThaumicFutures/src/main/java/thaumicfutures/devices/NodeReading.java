package thaumicfutures.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import tc4revival.aspect.*;
import tc4revival.content.*;

/** Only observational data is sent; a projection never instantiates or ticks a real node. */
public record NodeReading(BlockPos pos,AspectAmounts capacity,AspectAmounts charge,AuraNodeType type,AuraNodeModifier modifier,boolean live) {
    public static NodeReading of(AuraNodeBlockEntity node) {
        return new NodeReading(node.getBlockPos(),node.base(),node.aspects(),node.nodeType(),node.nodeModifier(),true);
    }
    public static NodeReading read(CompoundTag tag,boolean live) {
        var base=AspectNbt.read(tag,"AspectsBase",TCAspects.catalog()).amounts();
        var charge=AspectNbt.read(tag,TCAspects.catalog()).amounts();
        if(base.isEmpty())base=charge.copy();
        return new NodeReading(new BlockPos(tag.getInt("x"),tag.getInt("y"),tag.getInt("z")),base,charge,
                AuraNodeType.fromId(tag.getByte("type")),AuraNodeModifier.fromId(tag.contains("modifier")?tag.getByte("modifier"):-1),live);
    }
    public CompoundTag save() {
        var tag=new CompoundTag();tag.putInt("x",pos.getX());tag.putInt("y",pos.getY());tag.putInt("z",pos.getZ());
        AspectNbt.write(tag,"AspectsBase",capacity);AspectNbt.write(tag,charge);
        tag.putByte("type",(byte)type.ordinal());tag.putByte("modifier",modifier==null?-1:(byte)modifier.ordinal());tag.putBoolean("Live",live);return tag;
    }
    public boolean contains(Aspect aspect) { return aspect==null || capacity.contains(aspect) || charge.contains(aspect); }
}
