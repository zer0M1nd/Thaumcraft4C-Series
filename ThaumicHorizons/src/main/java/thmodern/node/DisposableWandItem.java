package thmodern.node;

import net.minecraft.world.item.ItemStack;
import tc4revival.content.CastingWandItem;
import tc4revival.content.WandParts;

/** Initialization is separate from every subsequent recharge route. */
public final class DisposableWandItem extends CastingWandItem {
    public static final String PART="thaumichorizons:disposable";
    @Override public WandParts.Rod rod(ItemStack stack){return WandParts.Rod.require(PART);}
    @Override public WandParts.Cap cap(ItemStack stack){return WandParts.Cap.require(PART);}
    @Override public boolean rechargeable(ItemStack stack){return false;}
    public ItemStack full(){return create(rod(ItemStack.EMPTY),cap(ItemStack.EMPTY),false,true);}
    @Override public ItemStack getDefaultInstance(){return full();}
}
