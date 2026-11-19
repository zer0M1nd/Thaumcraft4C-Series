package tc4revival.content;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public  class HoverGirdleItem extends Item implements ICurioItem, RunicGear {
    public HoverGirdleItem() { super(new Properties().stacksTo(1)); }
    @Override public Rarity getRarity(ItemStack stack) { return Rarity.RARE; }
    @Override public boolean canEquipFromUse(SlotContext context, ItemStack stack) { return false; }
    @Override public void curioTick(SlotContext context, ItemStack stack) {
        if (context.entity().fallDistance > 0) context.entity().fallDistance -= .33f;
    }
}
