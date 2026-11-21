package automagy.content;

import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;

/** Immutable item identity for requests; callers never receive the stored mutable tag. */
public final class RequestedItem {
    private final Item item;
    private final CompoundTag tag;
    public RequestedItem(ItemStack stack){
        if(stack.isEmpty())throw new IllegalArgumentException("Cannot request an empty item");
        item=stack.getItem();tag=stack.hasTag()?stack.getTag().copy():null;
    }
    public ItemStack stack(){var result=new ItemStack(item);if(tag!=null)result.setTag(tag.copy());return result;}
    public boolean matches(ItemStack stack){return !stack.isEmpty()&&stack.is(item)&&Objects.equals(tag,stack.getTag());}
    @Override public boolean equals(Object value){return value instanceof RequestedItem other&&item==other.item&&Objects.equals(tag,other.tag);}
    @Override public int hashCode(){return 31*System.identityHashCode(item)+Objects.hashCode(tag);}
}
